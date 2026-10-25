package app.fogwalk.ui.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings as AndroidSettings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.fogwalk.BuildConfig
import app.fogwalk.R
import app.fogwalk.container
import app.fogwalk.data.Backup
import app.fogwalk.data.Settings
import app.fogwalk.data.ThemeMode
import app.fogwalk.data.Units
import app.fogwalk.notify.Alarms
import app.fogwalk.ui.Language
import app.fogwalk.ui.timeFormat
import app.fogwalk.ui.components.Chip
import app.fogwalk.ui.components.CommunityLinks
import app.fogwalk.ui.components.ConfirmDialog
import app.fogwalk.ui.components.SectionTitle
import app.fogwalk.ui.components.TimeDialog
import app.fogwalk.ui.components.TopBar
import app.fogwalk.ui.components.UCard
import app.fogwalk.ui.theme.U
import app.fogwalk.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(settings: Settings, onBack: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    fun update(change: (Settings) -> Settings) {
        container.scope.launch {
            container.settings.update(change)
            Alarms.reschedule(context)
        }
    }
    var canNotify by remember { mutableStateOf(Alarms.canNotify(context)) }
    // Re-read on resume so the permission card disappears after granting it in system settings.
    var canCount by remember { mutableStateOf(container.steps.permitted()) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        canNotify = Alarms.canNotify(context)
        canCount = container.steps.permitted()
        container.steps.refresh()
    }
    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { canNotify = it }
    val noSteps = stringResource(R.string.steps_denied)
    val askSteps = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        canCount = granted
        if (granted) update { it.copy(steps = true) } else Toast.makeText(context, noSteps, Toast.LENGTH_LONG).show()
    }
    var pickTime by remember { mutableStateOf(false) }
    var density by remember { mutableFloatStateOf(settings.fogDensity) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }

    val exported = stringResource(R.string.backup_done)
    val failed = stringResource(R.string.backup_failed)
    val restored = stringResource(R.string.backup_restored)
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        container.scope.launch {
            val ok = runCatching {
                val json = Backup.export(container.db)
                context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
            }.isSuccess
            withContext(Dispatchers.Main) { Toast.makeText(context, if (ok) exported else failed, Toast.LENGTH_SHORT).show() }
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingImport = uri }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.settings), onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).navigationBarsPadding().padding(bottom = 24.dp)) {
            if (!canNotify) {
                Spacer(Modifier.height(8.dp))
                UCard(onClick = { if (Build.VERSION.SDK_INT >= 33) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                    Text(stringResource(R.string.perm_title), style = UType.Strong.copy(color = U.c.accent))
                    Spacer(Modifier.height(4.dp))
                    Text(stringResource(R.string.perm_text), style = UType.Muted)
                }
            }

            SectionTitle(stringResource(R.string.set_notifications))
            UCard {
                SwitchRow(stringResource(R.string.set_remind), stringResource(R.string.set_remind_text), settings.remind) { v -> update { it.copy(remind = v) } }
                if (settings.remind) {
                    Spacer(Modifier.height(6.dp))
                    val time = LocalTime.of(settings.remindAt / 60, settings.remindAt % 60)
                    Text(
                        stringResource(R.string.set_remind_at, timeFormat().format(time)),
                        style = UType.Button.copy(color = U.c.accent),
                        modifier = Modifier.clickable { pickTime = true }.padding(vertical = 6.dp),
                    )
                }
                Divider()
                SwitchRow(stringResource(R.string.set_nearby), stringResource(R.string.set_nearby_text), settings.nearbyAlerts) { v -> update { it.copy(nearbyAlerts = v) } }
            }
            Spacer(Modifier.height(10.dp))
            UCard(onClick = {
                context.startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
            }) {
                Text(stringResource(R.string.autostart_title), style = UType.Strong)
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.autostart_text), style = UType.Muted)
            }

            SectionTitle(stringResource(R.string.set_fog))
            UCard {
                Text(stringResource(R.string.set_fog_density), style = UType.Strong)
                Text(stringResource(R.string.set_fog_density_text), style = UType.Muted)
                Slider(
                    value = density, onValueChange = { density = it }, valueRange = 0.6f..1f,
                    onValueChangeFinished = { update { it.copy(fogDensity = density) } },
                    colors = SliderDefaults.colors(thumbColor = U.c.accent, activeTrackColor = U.c.accent, inactiveTrackColor = U.c.accentSoft),
                )
            }

            SectionTitle(stringResource(R.string.set_units))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.units_metric), settings.units == Units.METRIC, { update { it.copy(units = Units.METRIC) } })
                Chip(stringResource(R.string.units_imperial), settings.units == Units.IMPERIAL, { update { it.copy(units = Units.IMPERIAL) } })
            }

            SectionTitle(stringResource(R.string.set_feel))
            UCard {
                SwitchRow(stringResource(R.string.set_haptics), stringResource(R.string.set_haptics_text), settings.haptics) { v -> update { it.copy(haptics = v) } }
                if (container.steps.available) {
                    Divider()
                    SwitchRow(stringResource(R.string.set_steps), stringResource(R.string.set_steps_text), settings.steps && canCount) { v ->
                        if (v && !canCount && Build.VERSION.SDK_INT >= 29) askSteps.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                        else update { it.copy(steps = v) }
                    }
                }
            }

            SectionTitle(stringResource(R.string.set_appearance))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.theme_system), settings.theme == ThemeMode.SYSTEM, { update { it.copy(theme = ThemeMode.SYSTEM) } })
                Chip(stringResource(R.string.theme_light), settings.theme == ThemeMode.LIGHT, { update { it.copy(theme = ThemeMode.LIGHT) } })
                Chip(stringResource(R.string.theme_dark), settings.theme == ThemeMode.DARK, { update { it.copy(theme = ThemeMode.DARK) } })
            }

            SectionTitle(stringResource(R.string.language))
            val current = Language.current()
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(stringResource(R.string.lang_system), current == Language.SYSTEM, { Language.set(Language.SYSTEM) })
                Chip("English", current == Language.EN, { Language.set(Language.EN) })
                Chip("Русский", current == Language.RU, { Language.set(Language.RU) })
            }

            SectionTitle(stringResource(R.string.backup))
            UCard {
                Text(stringResource(R.string.backup_text), style = UType.Muted)
                Spacer(Modifier.height(8.dp))
                Row {
                    TextButton(onClick = { export.launch("fogwalk-${LocalDate.now()}.json") }) {
                        Text(stringResource(R.string.backup_export), color = U.c.accent, style = UType.Button)
                    }
                    TextButton(onClick = { import.launch(arrayOf("application/json", "text/plain", "application/octet-stream")) }) {
                        Text(stringResource(R.string.backup_import), color = U.c.accent, style = UType.Button)
                    }
                }
            }

            CommunityLinks()

            SectionTitle(stringResource(R.string.about))
            UCard {
                Text(stringResource(R.string.about_text), style = UType.Sub)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.about_map), style = UType.Small)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.about_version, BuildConfig.VERSION_NAME), style = UType.Small)
            }
        }
    }

    if (pickTime) TimeDialog(settings.remindAt, onDismiss = { pickTime = false }) { m -> update { it.copy(remindAt = m) } }

    pendingImport?.let { uri ->
        ConfirmDialog(
            title = stringResource(R.string.backup_import_title),
            text = stringResource(R.string.backup_import_text),
            confirm = stringResource(R.string.backup_import),
            onConfirm = {
                container.scope.launch {
                    val ok = runCatching {
                        val text = context.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() }
                        Backup.import(container.db, text)
                        container.explorer.load()
                    }.isSuccess
                    withContext(Dispatchers.Main) { Toast.makeText(context, if (ok) restored else failed, Toast.LENGTH_SHORT).show() }
                }
            },
            onDismiss = { pendingImport = null },
        )
    }
}

@Composable
private fun Divider() = HorizontalDivider(Modifier.padding(vertical = 10.dp), color = U.c.line)

@Composable
private fun SwitchRow(title: String, text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = UType.Strong)
            Spacer(Modifier.height(2.dp))
            Text(text, style = UType.Muted)
        }
        Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = U.c.accent))
    }
}
