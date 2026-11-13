package app.fogwalk.ui.settings

import app.fogwalk.ui.GameIcon
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.*
import androidx.compose.material.icons.Icons
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.PaddingValues
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
import app.fogwalk.explore.HealthSteps
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
                val json = Backup.export(container.db, container.settings.now())
                context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(json.toByteArray()) }
            }.isSuccess
            withContext(Dispatchers.Main) { Toast.makeText(context, if (ok) exported else failed, Toast.LENGTH_SHORT).show() }
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingImport = uri }

    var community by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.settings), onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 12.dp).navigationBarsPadding().padding(bottom = 24.dp)) {
            if (!canNotify) {
                Spacer(Modifier.height(8.dp))
                UCard(onClick = { if (Build.VERSION.SDK_INT >= 33) askPermission.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                    Text(stringResource(R.string.perm_title), style = UType.Strong.copy(color = U.c.accent))
                    Text(stringResource(R.string.perm_text), style = UType.Small)
                }
            }

            // Reminders: when the game may call, and what keeps a walk alive with the screen off.
            SectionTitle(stringResource(R.string.set_notifications))
            UCard(padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                Line(Icons.Rounded.Notifications, stringResource(R.string.set_remind), stringResource(R.string.set_remind_text)) {
                    Toggle(settings.remind) { v -> update { it.copy(remind = v) } }
                }
                if (settings.remind) {
                    val time = LocalTime.of(settings.remindAt / 60, settings.remindAt % 60)
                    Line(null, stringResource(R.string.set_remind_time), null, onClick = { pickTime = true }) {
                        Text(timeFormat().format(time), style = UType.Strong.copy(color = U.c.accent))
                        Chevron()
                    }
                }
                Rule()
                Line(Icons.Rounded.Inventory2, stringResource(R.string.set_nearby), stringResource(R.string.set_nearby_text)) {
                    Toggle(settings.nearbyAlerts) { v -> update { it.copy(nearbyAlerts = v) } }
                }
                Rule()
                Line(Icons.Rounded.Bedtime, stringResource(R.string.autostart_title), stringResource(R.string.autostart_text), onClick = {
                    context.startActivity(Intent(AndroidSettings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                }) { Chevron() }
            }

            // How the game looks: three short choices and the fog's density.
            SectionTitle(stringResource(R.string.set_look))
            UCard(padding = PaddingValues(horizontal = 14.dp, vertical = 12.dp)) {
                Choice(Icons.Rounded.LightMode, stringResource(R.string.set_appearance), listOf(
                    stringResource(R.string.choice_auto) to (settings.theme == ThemeMode.SYSTEM),
                    stringResource(R.string.theme_light) to (settings.theme == ThemeMode.LIGHT),
                    stringResource(R.string.theme_dark) to (settings.theme == ThemeMode.DARK),
                )) { i -> update { it.copy(theme = listOf(ThemeMode.SYSTEM, ThemeMode.LIGHT, ThemeMode.DARK)[i]) } }
                Spacer(Modifier.height(14.dp))
                val current = Language.current()
                Choice(Icons.Rounded.Public, stringResource(R.string.language), listOf(
                    stringResource(R.string.choice_auto) to (current == Language.SYSTEM),
                    "Русский" to (current == Language.RU),
                    "English" to (current == Language.EN),
                )) { i -> Language.set(listOf(Language.SYSTEM, Language.RU, Language.EN)[i]) }
                Spacer(Modifier.height(14.dp))
                Choice(Icons.Rounded.Route, stringResource(R.string.set_units), listOf(
                    stringResource(R.string.units_metric) to (settings.units == Units.METRIC),
                    stringResource(R.string.units_imperial) to (settings.units == Units.IMPERIAL),
                )) { i -> update { it.copy(units = if (i == 0) Units.METRIC else Units.IMPERIAL) } }
                Spacer(Modifier.height(14.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GameIcon(Icons.Rounded.Cloud, null, Modifier.size(26.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.set_fog_density), style = UType.Strong, modifier = Modifier.weight(1f))
                    Text("${(density * 100).toInt()}%", style = UType.Small)
                }
                Slider(
                    value = density, onValueChange = { density = it }, valueRange = 0.6f..1f,
                    onValueChangeFinished = { update { it.copy(fogDensity = density) } },
                    colors = SliderDefaults.colors(thumbColor = U.c.accent, activeTrackColor = U.c.accent, inactiveTrackColor = U.c.surface2),
                )
            }

            // Steps and the phone's touch.
            SectionTitle(stringResource(R.string.set_feel))
            UCard(padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                Line(Icons.Rounded.Bolt, stringResource(R.string.set_haptics), stringResource(R.string.set_haptics_text)) {
                    Toggle(settings.haptics) { v -> update { it.copy(haptics = v) } }
                }
                if (container.steps.available) {
                    Rule()
                    Line(Icons.AutoMirrored.Rounded.DirectionsWalk, stringResource(R.string.set_steps), stringResource(R.string.set_steps_text)) {
                        Toggle(settings.steps && canCount) { v ->
                            if (v && !canCount && Build.VERSION.SDK_INT >= 29) askSteps.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                            else update { it.copy(steps = v) }
                        }
                    }
                }
                if (container.health.available || container.health.installable) {
                    Rule()
                    HealthSource(settings)
                }
            }

            // The rest: the backup, the story, the people who make the game.
            SectionTitle(stringResource(R.string.set_more))
            UCard(padding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                Line(Icons.Rounded.Map, stringResource(R.string.backup_save), stringResource(R.string.backup_save_text), onClick = {
                    export.launch("fogwalk-${LocalDate.now()}.json")
                }) { Chevron() }
                Rule()
                Line(Icons.Rounded.Backpack, stringResource(R.string.backup_load), stringResource(R.string.backup_load_text), onClick = {
                    import.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
                }) { Chevron() }
                Rule()
                Line(Icons.AutoMirrored.Rounded.MenuBook, stringResource(R.string.story_again), null, onClick = { update { it.copy(storySeen = false) } }) { Chevron() }
                Rule()
                Line(Icons.Rounded.RecordVoiceOver, stringResource(R.string.community_title), stringResource(R.string.community_row), onClick = { community = true }) { Chevron() }
            }

            // Who we are, in small print at the foot.
            Spacer(Modifier.height(18.dp))
            Text(
                stringResource(R.string.about_footer, BuildConfig.VERSION_NAME) + "\n" + stringResource(R.string.about_map),
                style = UType.Small.copy(fontSize = 12.sp, lineHeight = 17.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            )
        }
    }

    if (community) CommunitySheet { community = false }

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
                        Backup.import(container.db, text, container.settings)
                        container.explorer.load()
                    }.isSuccess
                    withContext(Dispatchers.Main) { Toast.makeText(context, if (ok) restored else failed, Toast.LENGTH_SHORT).show() }
                }
            },
            onDismiss = { pendingImport = null },
        )
    }
}

/** Steps from Health Connect: what health apps, watches and bands counted, all day long. */
@Composable
private fun HealthSource(settings: Settings) {
    val context = LocalContext.current
    val container = context.container
    val health = container.health
    val on = settings.healthFrom >= 0
    // null while asking Health Connect whether reading is still allowed.
    var allowed by remember { mutableStateOf<Boolean?>(null) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { container.scope.launch { allowed = health.allowed() } }
    fun use(v: Boolean) = container.scope.launch {
        container.settings.update { it.copy(healthFrom = if (v) java.time.LocalDate.now().toEpochDay() else -1) }
        container.syncHealth()
    }
    val denied = stringResource(R.string.health_denied)
    val install = stringResource(R.string.health_install)
    val ask = rememberLauncherForActivityResult(HealthSteps.requestContract()) { got ->
        allowed = HealthSteps.READ in got
        if (allowed == true) use(true) else Toast.makeText(context, denied, Toast.LENGTH_LONG).show()
    }
    fun pick() {
        if (health.available) ask.launch(setOf(HealthSteps.READ))
        else {
            Toast.makeText(context, install, Toast.LENGTH_LONG).show()
            runCatching { context.startActivity(health.storeIntent()) }
        }
    }
    Line(Icons.Rounded.Speed, stringResource(R.string.set_health), stringResource(R.string.set_health_text)) {
        Toggle(on && allowed != false) { v -> if (!v) use(false) else if (on && allowed == true) Unit else pick() }
    }
    if (on && allowed == false) {
        Spacer(Modifier.height(4.dp))
        Text(denied, style = UType.Muted.copy(color = U.c.accent))
        Text(
            stringResource(if (health.available) R.string.health_allow else R.string.health_get),
            style = UType.Button.copy(color = U.c.accent),
            modifier = Modifier.clickable { pick() }.padding(vertical = 6.dp),
        )
    }
}

/** The community's links in a sheet of their own. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
private fun CommunitySheet(onClose: () -> Unit) {
    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onClose, containerColor = U.c.bg,
        sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.navigationBarsPadding().padding(horizontal = 12.dp).padding(bottom = 16.dp)) { CommunityLinks() }
    }
}

/** A fine rule between the lines of a card. */
@Composable
private fun Rule() = HorizontalDivider(Modifier.padding(start = 40.dp), color = U.c.line.copy(alpha = 0.7f))

/** A line of a settings card: a painted icon, a name with a short word under it, and what it holds at the end. */
@Composable
private fun Line(icon: ImageVector?, title: String, sub: String?, onClick: (() -> Unit)? = null, end: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(40.dp)) { if (icon != null) GameIcon(icon, null, Modifier.size(26.dp)) }
        Column(Modifier.weight(1f)) {
            Text(title, style = UType.Strong.copy(fontSize = 15.sp))
            if (sub != null) Text(sub, style = UType.Small.copy(fontSize = 13.sp, lineHeight = 17.sp))
        }
        Spacer(Modifier.width(8.dp))
        end()
    }
}

@Composable
private fun Toggle(checked: Boolean, onChange: (Boolean) -> Unit) =
    Switch(checked = checked, onCheckedChange = onChange, colors = SwitchDefaults.colors(checkedTrackColor = U.c.accent))

@Composable
private fun Chevron() = Text("›", style = UType.Strong.copy(fontSize = 22.sp, color = U.c.accent), modifier = Modifier.padding(start = 6.dp))

/** A setting with a few short answers: its name, and the answers side by side, the chosen one filled. */
@Composable
private fun Choice(icon: ImageVector, title: String, options: List<Pair<String, Boolean>>, onPick: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        GameIcon(icon, null, Modifier.size(26.dp))
        Spacer(Modifier.width(10.dp))
        Text(title, style = UType.Strong)
    }
    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth().clip(CircleShape).background(U.c.surface2).padding(3.dp)) {
        options.forEachIndexed { i, (label, on) ->
            Text(
                label, maxLines = 1,
                style = UType.Strong.copy(fontSize = 14.sp, color = if (on) U.c.onAccent else U.c.muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                modifier = Modifier.weight(1f).clip(CircleShape).background(if (on) U.c.accent else Color.Transparent)
                    .clickable { onPick(i) }.padding(vertical = 8.dp),
            )
        }
    }
}
