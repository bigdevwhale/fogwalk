package app.fogwalk.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import app.fogwalk.R
import app.fogwalk.ui.theme.Sans
import app.fogwalk.ui.theme.U
import app.fogwalk.ui.theme.UType
import kotlinx.coroutines.delay

@Composable
fun UCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, padding: PaddingValues = PaddingValues(16.dp), content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier
            .fillMaxWidth()
            .clip(U.RMd)
            .background(U.c.surface)
            .border(1.dp, U.c.gilt.copy(alpha = 0.55f), U.RMd)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), style = UType.Eyebrow, modifier = modifier.padding(start = 4.dp, top = 22.dp, bottom = 10.dp))
}

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, color: Color = U.c.accent, icon: ImageVector? = null) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(54.dp),
        shape = U.RMd,
        colors = ButtonDefaults.buttonColors(
            containerColor = color,
            contentColor = U.c.onAccent,
            disabledContainerColor = U.c.line,
            disabledContentColor = U.c.faint,
        ),
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = UType.Button)
    }
}

/** Quiet, tinted button for secondary actions. */
@Composable
fun SoftButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = U.c.accent, soft: Color = U.c.accentSoft, icon: ImageVector? = null) {
    Row(
        modifier
            .height(50.dp)
            .clip(U.RMd)
            .background(soft)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(19.dp), tint = color)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = UType.Button, color = color, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun Chip(text: String, selected: Boolean, onClick: () -> Unit, color: Color = U.c.accent) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text, fontFamily = Sans, fontWeight = FontWeight.Medium, fontSize = 14.sp) },
        shape = U.RSm,
        colors = FilterChipDefaults.filterChipColors(
            containerColor = U.c.surface,
            labelColor = U.c.muted,
            selectedContainerColor = color.copy(alpha = 0.16f),
            selectedLabelColor = color,
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = U.c.line,
            selectedBorderColor = color.copy(alpha = 0.5f),
        ),
    )
}

/** Day / Week / Month style switch. */
@Composable
fun <T> Segmented(options: List<T>, selected: T, label: @Composable (T) -> String, onSelect: (T) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .clip(U.RSm)
            .background(U.c.surface2)
            .border(1.dp, U.c.line, U.RSm)
            .padding(4.dp),
    ) {
        options.forEach { o ->
            val on = o == selected
            Box(
                Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedSm)
                    .background(if (on) U.c.surface else Color.Transparent)
                    .then(if (on) Modifier.border(1.dp, U.c.line, RoundedSm) else Modifier)
                    .clickable { onSelect(o) },
                contentAlignment = Alignment.Center,
            ) {
                Text(label(o), style = UType.Button.copy(fontSize = 14.sp), color = if (on) U.c.accent else U.c.muted)
            }
        }
    }
}

private val RoundedSm = androidx.compose.foundation.shape.RoundedCornerShape(9.dp)

/** A glyph on a soft disc. */
@Composable
fun IconDisc(icon: ImageVector, size: Dp = 44.dp, color: Color = U.c.accent, soft: Color = U.c.accentSoft) {
    Box(Modifier.size(size).clip(CircleShape).background(soft), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(size * 0.5f), tint = color)
    }
}

@Composable
fun TopBar(title: String, onBack: (() -> Unit)?, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().height(60.dp).padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), tint = U.c.ink)
            }
        } else Spacer(Modifier.width(14.dp))
        Text(title, style = UType.Strong.copy(fontSize = 19.sp), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        actions()
    }
}

/** A tappable row inside a card: icon, label, value and a chevron. */
@Composable
fun InfoRow(icon: ImageVector, label: String, value: String, tint: Color = U.c.accent, soft: Color = U.c.accentSoft, onClick: (() -> Unit)? = null) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconDisc(icon, 40.dp, tint, soft)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = UType.Muted)
            Spacer(Modifier.height(2.dp))
            Text(value, style = UType.Strong)
        }
        if (onClick != null) Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = U.c.faint)
    }
}

/** Ring that fills toward a goal. */
@Composable
fun ProgressRing(fraction: Float, modifier: Modifier = Modifier, stroke: Dp = 12.dp, color: Color = U.c.accent, track: Color = U.c.accentSoft, content: @Composable () -> Unit = {}) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val w = stroke.toPx()
            val inset = w / 2
            val arc = Size(size.width - w, size.height - w)
            drawArc(track, 135f, 270f, false, Offset(inset, inset), arc, style = Stroke(w, cap = StrokeCap.Round))
            drawArc(color, 135f, 270f * fraction.coerceIn(0.005f, 1f), false, Offset(inset, inset), arc, style = Stroke(w, cap = StrokeCap.Round))
        }
        content()
    }
}

@Composable
fun Bar(fraction: Float, modifier: Modifier = Modifier, color: Color = U.c.accent, track: Color = U.c.accentSoft) {
    LinearProgressIndicator(
        progress = { fraction.coerceIn(0f, 1f) },
        modifier = modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
        color = color,
        trackColor = track,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
    )
}

/** Wall clock that ticks every [stepMs] while the screen is visible. */
@Composable
fun rememberNow(stepMs: Long = 1000): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var visible by remember { mutableStateOf(true) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { visible = true; now = System.currentTimeMillis() }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { visible = false }
    LaunchedEffect(visible, stepMs) {
        while (visible) {
            now = System.currentTimeMillis()
            delay(stepMs - now % stepMs)
        }
    }
    return now
}

@Composable
fun UTextField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, placeholder: String = "", number: Boolean = false, minLines: Int = 1, suffix: String? = null) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = modifier.fillMaxWidth(),
        placeholder = { Text(placeholder, color = U.c.faint) },
        minLines = minLines,
        singleLine = minLines == 1,
        shape = U.RSm,
        suffix = suffix?.let { { Text(it, color = U.c.muted) } },
        keyboardOptions = if (number) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = U.c.surface,
            unfocusedContainerColor = U.c.surface,
            unfocusedBorderColor = U.c.line,
            focusedBorderColor = U.c.accent,
        ),
    )
}

@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit, danger: Boolean = false) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.bg,
        title = { Text(title, style = UType.Strong.copy(fontSize = 19.sp)) },
        text = { Text(text, style = UType.Sub) },
        confirmButton = {
            TextButton(onClick = { onConfirm(); onDismiss() }) {
                Text(confirm, color = if (danger) U.c.danger else U.c.accent, style = UType.Button)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted, style = UType.Button) } },
    )
}

/** Clock-face picker for a reminder; [minutes] after midnight in and out. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(minutes: Int, onDismiss: () -> Unit, onPicked: (Int) -> Unit) {
    val state = rememberTimePickerState(minutes / 60, minutes % 60)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = U.c.bg,
        confirmButton = {
            TextButton(onClick = { onPicked(state.hour * 60 + state.minute); onDismiss() }) { Text(stringResource(R.string.save), color = U.c.accent) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted) } },
        text = { Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state) } },
    )
}

/** A moment worth marking: a badge, a level, a finished challenge. */
@Composable
fun CelebrateDialog(
    icon: ImageVector, color: Color, soft: Color, eyebrow: String, title: String, text: String, confetti: Boolean = true,
    /** A second thing to do about it, shown as the main button: its label and what it does before the dialog closes. */
    action: Pair<String, () -> Unit>? = null,
    onDismiss: () -> Unit,
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().clip(U.RLg).background(U.c.bg).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                IconDisc(icon, 84.dp, color, soft)
                Spacer(Modifier.height(18.dp))
                Text(eyebrow.uppercase(), style = UType.Eyebrow.copy(color = color))
                Spacer(Modifier.height(6.dp))
                Text(title, style = UType.Title.copy(fontSize = 22.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center))
                Spacer(Modifier.height(8.dp))
                Text(text, style = UType.Sub.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center))
                Spacer(Modifier.height(20.dp))
                if (action != null) {
                    PrimaryButton(action.first, { action.second(); onDismiss() }, Modifier.fillMaxWidth(), color = color)
                    Spacer(Modifier.height(4.dp))
                    TextButton(onClick = onDismiss) { Text(stringResource(R.string.nice), color = U.c.muted, style = UType.Button) }
                } else PrimaryButton(stringResource(R.string.nice), onDismiss, Modifier.fillMaxWidth(), color = color)
            }
            if (confetti) Confetti(title, Modifier.matchParentSize())
        }
    }
}

/** Date, then time. [initial] and the result are epoch millis; future days can't be picked unless [future]. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeDialog(initial: Long, onDismiss: () -> Unit, future: Boolean = false, onPicked: (Long) -> Unit) {
    val zone = java.time.ZoneId.systemDefault()
    val start = java.time.Instant.ofEpochMilli(initial).atZone(zone)
    var date by remember { mutableStateOf<java.time.LocalDate?>(null) }
    val today = java.time.LocalDate.now(zone)
    val d = date
    if (d == null) {
        val state = androidx.compose.material3.rememberDatePickerState(
            initialSelectedDateMillis = start.toLocalDate().atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
            selectableDates = object : androidx.compose.material3.SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                    future || !java.time.Instant.ofEpochMilli(utcTimeMillis).atZone(java.time.ZoneOffset.UTC).toLocalDate().isAfter(today)
            },
        )
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = onDismiss,
            confirmButton = {
                TextButton(onClick = {
                    date = state.selectedDateMillis?.let { java.time.Instant.ofEpochMilli(it).atZone(java.time.ZoneOffset.UTC).toLocalDate() } ?: start.toLocalDate()
                }) { Text(stringResource(R.string.next), color = U.c.accent) }
            },
            dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = U.c.muted) } },
            colors = androidx.compose.material3.DatePickerDefaults.colors(containerColor = U.c.bg),
        ) { androidx.compose.material3.DatePicker(state, showModeToggle = false) }
    } else {
        TimeDialog(start.hour * 60 + start.minute, onDismiss) { m ->
            onPicked(d.atTime(m / 60, m % 60).atZone(zone).toInstant().toEpochMilli())
        }
    }
}

/** Five faces, 1..5. */
@Composable
fun ScalePicker(value: Int, onChange: (Int) -> Unit, labels: (Int) -> String, modifier: Modifier = Modifier, color: Color = U.c.accent) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        (1..5).forEach { v ->
            val on = v == value
            Column(
                Modifier
                    .weight(1f)
                    .clip(U.RSm)
                    .background(if (on) color.copy(alpha = 0.16f) else U.c.surface)
                    .border(1.dp, if (on) color.copy(alpha = 0.6f) else U.c.line, U.RSm)
                    .clickable { onChange(v) }
                    .padding(vertical = 10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(labels(v), fontSize = 22.sp)
            }
        }
    }
}
