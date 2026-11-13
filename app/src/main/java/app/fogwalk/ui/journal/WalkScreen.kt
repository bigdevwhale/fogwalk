package app.fogwalk.ui.journal

import app.fogwalk.ui.GameIcon
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.ui.res.pluralStringResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import app.fogwalk.R
import app.fogwalk.data.Units
import app.fogwalk.data.Waypoint
import app.fogwalk.ui.GameData
import app.fogwalk.container
import app.fogwalk.domain.Races
import app.fogwalk.ui.components.PrimaryButton
import app.fogwalk.ui.components.TopBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import kotlinx.coroutines.launch
import androidx.compose.material.icons.rounded.Timer
import app.fogwalk.ui.components.UCard
import app.fogwalk.ui.share.Postcard
import app.fogwalk.ui.share.PostcardDialog
import app.fogwalk.ui.share.ShareButton
import app.fogwalk.ui.components.ConfirmDialog
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.fogwalk.ui.dateFormat
import app.fogwalk.ui.timeFormat
import app.fogwalk.ui.distance
import app.fogwalk.ui.locale
import app.fogwalk.ui.map.nightTiles
import app.fogwalk.ui.map.softTiles
import app.fogwalk.ui.theme.U
import app.fogwalk.ui.theme.UType
import kotlinx.coroutines.delay
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import java.time.Instant
import java.time.format.FormatStyle

/** One journey: its route on the old map and its numbers. */
@Composable
fun WalkScreen(id: Long, data: GameData, onBack: () -> Unit, onRace: (Long) -> Unit) {
    val w = data.walks.firstOrNull { it.id == id }
    val imperial = data.settings.units == Units.IMPERIAL
    val l = locale()
    val day = dateFormat(FormatStyle.FULL)
    val time = timeFormat()
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        val title = w?.let { day.format(Instant.ofEpochMilli(it.start).atZone(data.zone)).replaceFirstChar { c -> c.titlecase(l) } }
        val pinned = id in data.settings.pinned
        var sharing by remember { mutableStateOf(false) }
        var deleting by remember { mutableStateOf(false) }
        // The journey under way can't be taken out while it is still being written.
        val ongoing = w != null && data.settings.walking && data.walks.firstOrNull()?.id == id
        if (deleting) ConfirmDialog(
            stringResource(R.string.walk_delete_q), stringResource(R.string.walk_delete_text), stringResource(R.string.walk_delete),
            onConfirm = {
                deleting = false
                val container = context.container
                container.scope.launch {
                    container.settings.update { s -> s.copy(pinned = s.pinned - id, race = s.race?.takeIf { it.walk != id }) }
                    container.db.dao().deleteWalk(id)
                }
                onBack()
            },
            onDismiss = { deleting = false }, danger = true,
        )
        if (sharing) PostcardDialog(Postcard.Walk(id), data) { sharing = false }
        TopBar(title ?: stringResource(R.string.walks_title), onBack) {
            if (w != null) ShareButton { sharing = true }
            if (w != null) IconButton(onClick = {
                val container = context.container
                container.scope.launch { container.settings.update { it.copy(pinned = if (id in it.pinned) it.pinned - id else it.pinned + id) } }
            }) {
                GameIcon(
                    if (pinned) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    stringResource(if (pinned) R.string.walk_unpin else R.string.walk_pin),
                    tint = if (pinned) U.c.gold else U.c.muted,
                )
            }
            if (w != null && !ongoing) IconButton(onClick = { deleting = true }) {
                GameIcon(Icons.Rounded.DeleteOutline, stringResource(R.string.walk_delete), tint = U.c.muted)
            }
        }
        if (w == null) return@Column
        var version by remember { mutableIntStateOf(0) }
        val route = rememberRoute(id, version)
        /** The stretch picked to bring the mist back over (#46), as route point indices; null while not picking. */
        var cut by remember { mutableStateOf<ClosedFloatingPointRange<Float>?>(null) }
        Column(
            Modifier.fillMaxSize().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 24.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(380.dp).clip(U.RMd).border(1.dp, U.c.gilt.copy(alpha = 0.55f), U.RMd).background(U.c.surface2)) {
                if (route != null && route.size >= 2) RouteMap(route, cut?.let { it.start.roundToInt()..it.endInclusive.roundToInt() })
                else if (route != null) Text(
                    stringResource(R.string.walk_no_route), style = UType.Muted,
                    modifier = Modifier.padding(20.dp),
                )
            }
            Spacer(Modifier.height(12.dp))
            UCard {
                val zone = data.zone
                Text(
                    time.format(Instant.ofEpochMilli(w.start).atZone(zone)) + " – " + time.format(Instant.ofEpochMilli(w.end).atZone(zone)),
                    style = UType.Eyebrow,
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Figure(distance(w.meters, imperial), stringResource(R.string.walk_distance), Modifier.weight(1f))
                    Figure(duration(w.end - w.start), stringResource(R.string.walks_time), Modifier.weight(1f))
                    Figure(pace(w.end - w.start, w.meters, imperial), stringResource(R.string.walk_pace), Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Figure("+" + "%,d".format(w.cells), stringResource(R.string.walk_cells), Modifier.weight(1f))
                    Figure(w.caches.toString(), stringResource(R.string.walk_chests), Modifier.weight(1f))
                    if (w.steps > 0) Figure("%,d".format(w.steps), stringResource(R.string.walk_steps), Modifier.weight(1f))
                    else Spacer(Modifier.weight(1f))
                }
            }
            if (route != null && route.size >= 2) {
                Spacer(Modifier.height(12.dp))
                val racing = data.settings.race?.walk == id
                when {
                    racing -> PrimaryButton(
                        stringResource(R.string.race_cancel), { context.container.explorer.cancelRace() }, Modifier.fillMaxWidth(),
                        color = U.c.ink, icon = Icons.Rounded.Close,
                    )
                    w.meters < Races.MIN_ROUTE_M -> Text(
                        stringResource(R.string.race_short, distance(Races.MIN_ROUTE_M, imperial)), style = UType.Muted,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                    else -> {
                        PrimaryButton(stringResource(R.string.race_challenge), { onRace(id) }, Modifier.fillMaxWidth(), color = U.c.sage, icon = Icons.Rounded.Timer)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.race_challenge_text), style = UType.Small, modifier = Modifier.padding(horizontal = 4.dp))
                    }
                }
                Spacer(Modifier.height(12.dp))
                val picked = cut
                if (picked == null) Text(
                    stringResource(R.string.refog_open), style = UType.Button.copy(color = U.c.muted),
                    modifier = Modifier.clip(U.RSm).clickable { cut = (route.lastIndex / 3f)..(route.lastIndex * 2 / 3f) }.padding(horizontal = 4.dp, vertical = 6.dp),
                ) else RefogCard(id, route, picked, data, onChange = { cut = it }, onDone = {
                    cut = null
                    version++
                })
            }
        }
    }
}

/**
 * Bringing the mist back over a stretch a GPS glitch drew (#46): the slider picks it on the route, the map
 * shows it in flame, and the cells first charted in that time go back under the mist.
 */
@Composable
private fun RefogCard(
    walk: Long, route: List<Waypoint>, picked: ClosedFloatingPointRange<Float>, data: GameData,
    onChange: (ClosedFloatingPointRange<Float>?) -> Unit, onDone: () -> Unit,
) {
    val context = LocalContext.current
    val container = context.container
    val time = timeFormat()
    val a = picked.start.roundToInt().coerceIn(0, route.lastIndex)
    val b = picked.endInclusive.roundToInt().coerceIn(0, route.lastIndex)
    // Each cell carries the time of the step that charted it, the same as the step's end on the route.
    val from = route[a].at + 1
    val to = route[b].at
    val cells by produceState<Int?>(null, from, to) {
        value = null
        delay(250)
        value = if (b > a) container.explorer.cellsBetween(from, to) else 0
    }
    var asking by remember { mutableStateOf(false) }
    UCard {
        Text(stringResource(R.string.refog_title), style = UType.Strong)
        Spacer(Modifier.height(2.dp))
        Text(stringResource(R.string.refog_hint), style = UType.Muted)
        Spacer(Modifier.height(8.dp))
        RangeSlider(
            value = picked, onValueChange = onChange, valueRange = 0f..route.lastIndex.toFloat(),
            colors = SliderDefaults.colors(thumbColor = U.c.flame, activeTrackColor = U.c.flame, inactiveTrackColor = U.c.line),
        )
        val zone = data.zone
        Text(
            time.format(Instant.ofEpochMilli(route[a].at).atZone(zone)) + " – " + time.format(Instant.ofEpochMilli(route[b].at).atZone(zone)) +
                " · " + (cells?.let { pluralStringResource(R.plurals.cells, it, it) } ?: "…"),
            style = UType.Eyebrow,
        )
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(stringResource(R.string.refog_do), { asking = true }, Modifier.weight(1f), enabled = (cells ?: 0) > 0, color = U.c.danger)
            PrimaryButton(stringResource(R.string.cancel), { onChange(null) }, Modifier.weight(1f), color = U.c.ink)
        }
    }
    if (asking) ConfirmDialog(
        stringResource(R.string.refog_q), pluralStringResource(R.plurals.refog_text, cells ?: 0, cells ?: 0), stringResource(R.string.refog_do),
        onConfirm = {
            asking = false
            container.scope.launch {
                val n = container.explorer.refog(walk, from, to)
                withContext(Dispatchers.Main) {
                    android.widget.Toast.makeText(context, context.resources.getQuantityString(R.plurals.refog_done, n, n), android.widget.Toast.LENGTH_SHORT).show()
                    onDone()
                }
            }
        },
        onDismiss = { asking = false }, danger = true,
    )
}

@Composable
private fun Figure(value: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = UType.Number.copy(fontSize = 20.sp), maxLines = 1)
        Text(label, style = UType.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Minutes per kilometre (or mile), "—" when there was hardly any distance. */
@Composable
internal fun pace(ms: Long, meters: Double, imperial: Boolean): String {
    val unit = if (imperial) 1609.344 else 1000.0
    if (meters < 100 || ms <= 0) return "—"
    val sec = (ms / 1000.0 / (meters / unit)).toInt()
    return stringResource(if (imperial) R.string.pace_mi else R.string.pace_km, "%d:%02d".format(sec / 60, sec % 60))
}

/** An osmdroid map in the atlas colours, fitted to the route. */
@android.annotation.SuppressLint("ClickableViewAccessibility")
@Composable
private fun RouteMap(route: List<Waypoint>, cut: IntRange?) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val dark = U.c.dark
    val ink = U.c.accent.toArgb()
    val start = U.c.sage.toArgb()
    val end = U.c.gold.toArgb()
    val paper = U.c.surface.toArgb()
    val flame = U.c.flame.toArgb()
    val overlay = remember(route) { RouteOverlay(route.map { GeoPoint(it.lat, it.lon) }, density, ink, start, end, paper, flame) }
    overlay.cut = cut
    val map = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            minZoomLevel = 3.0
            maxZoomLevel = 19.5
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            overlayManager.tilesOverlay.setColorFilter(if (dark) nightTiles() else softTiles())
            overlayManager.tilesOverlay.loadingBackgroundColor = if (dark) 0xFF1E1810.toInt() else 0xFFE8DCBE.toInt()
            val zoom = app.fogwalk.ui.map.OneFingerZoom(this)
            setOnTouchListener { _, e -> zoom.onTouch(e) }
        }
    }
    // A shorter route after the mist came back gets a fresh line.
    DisposableEffect(overlay) {
        map.overlays.add(overlay)
        map.invalidate()
        onDispose { map.overlays.remove(overlay) }
    }
    LaunchedEffect(cut) { map.invalidate() }
    LaunchedEffect(route) {
        // osmdroid ignores where it is asked to look until it has been laid out.
        while (map.width == 0) delay(50)
        val box = BoundingBox.fromGeoPoints(route.map { GeoPoint(it.lat, it.lon) })
        map.zoomToBoundingBox(box.increaseByScale(1.25f), false, (24 * density).toInt())
        if (map.zoomLevelDouble > 18.0) map.controller.setZoom(18.0)
    }
    DisposableEffect(Unit) {
        map.onResume()
        onDispose {
            map.onPause()
            map.onDetach()
        }
    }
    AndroidView({ map }, Modifier.fillMaxSize())
}

/** The route as an inked line on a paper halo, a green dot where it began and a gold one where it ended. */
private class RouteOverlay(
    private val points: List<GeoPoint>,
    private val density: Float,
    private val ink: Int,
    private val start: Int,
    private val end: Int,
    private val paper: Int,
    private val flame: Int,
) : Overlay() {
    /** Route point indices picked to bring the mist back over, drawn in flame. */
    var cut: IntRange? = null
    private val line = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val dot = Paint(Paint.ANTI_ALIAS_FLAG)
    private val path = Path()
    private val px = android.graphics.Point()

    override fun draw(c: Canvas, mv: MapView, shadow: Boolean) {
        if (shadow) return
        val proj = mv.projection
        path.reset()
        points.forEachIndexed { i, p ->
            proj.toPixels(p, px)
            if (i == 0) path.moveTo(px.x.toFloat(), px.y.toFloat()) else path.lineTo(px.x.toFloat(), px.y.toFloat())
        }
        line.color = paper
        line.strokeWidth = 8 * density
        c.drawPath(path, line)
        line.color = ink
        line.strokeWidth = 4.5f * density
        c.drawPath(path, line)
        cut?.takeIf { it.last > it.first }?.let { r ->
            path.reset()
            for (i in r.first.coerceAtLeast(0)..r.last.coerceAtMost(points.lastIndex)) {
                proj.toPixels(points[i], px)
                if (i == r.first) path.moveTo(px.x.toFloat(), px.y.toFloat()) else path.lineTo(px.x.toFloat(), px.y.toFloat())
            }
            line.color = flame
            line.strokeWidth = 6.5f * density
            c.drawPath(path, line)
        }
        marker(c, proj, points.first(), start)
        marker(c, proj, points.last(), end)
    }

    private fun marker(c: Canvas, proj: org.osmdroid.views.Projection, p: GeoPoint, color: Int) {
        proj.toPixels(p, px)
        dot.color = paper
        c.drawCircle(px.x.toFloat(), px.y.toFloat(), 8 * density, dot)
        dot.color = color
        c.drawCircle(px.x.toFloat(), px.y.toFloat(), 5.5f * density, dot)
    }
}
