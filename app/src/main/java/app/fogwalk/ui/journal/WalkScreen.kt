package app.fogwalk.ui.journal

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.rounded.Timer
import app.fogwalk.ui.components.UCard
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
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** One journey: its route on the old map and its numbers. */
@Composable
fun WalkScreen(id: Long, data: GameData, onBack: () -> Unit, onRace: (Long) -> Unit) {
    val w = data.walks.firstOrNull { it.id == id }
    val imperial = data.settings.units == Units.IMPERIAL
    val l = locale()
    val day = remember(l) { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(l) }
    val time = remember(l) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(l) }
    val context = LocalContext.current
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        val title = w?.let { day.format(Instant.ofEpochMilli(it.start).atZone(data.zone)).replaceFirstChar { c -> c.titlecase(l) } }
        TopBar(title ?: stringResource(R.string.walks_title), onBack)
        if (w == null) return@Column
        val route = rememberRoute(id)
        Column(
            Modifier.fillMaxSize().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 24.dp),
        ) {
            Box(Modifier.fillMaxWidth().height(380.dp).clip(U.RMd).border(1.dp, U.c.gilt.copy(alpha = 0.55f), U.RMd).background(U.c.surface2)) {
                if (route != null && route.size >= 2) RouteMap(route)
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
                    Spacer(Modifier.weight(1f))
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
            }
        }
    }
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
private fun pace(ms: Long, meters: Double, imperial: Boolean): String {
    val unit = if (imperial) 1609.344 else 1000.0
    if (meters < 100 || ms <= 0) return "—"
    val sec = (ms / 1000.0 / (meters / unit)).toInt()
    return stringResource(if (imperial) R.string.pace_mi else R.string.pace_km, "%d:%02d".format(sec / 60, sec % 60))
}

/** An osmdroid map in the atlas colours, fitted to the route. */
@android.annotation.SuppressLint("ClickableViewAccessibility")
@Composable
private fun RouteMap(route: List<Waypoint>) {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val dark = U.c.dark
    val ink = U.c.accent.toArgb()
    val start = U.c.sage.toArgb()
    val end = U.c.gold.toArgb()
    val paper = U.c.surface.toArgb()
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
            overlays.add(RouteOverlay(route.map { GeoPoint(it.lat, it.lon) }, density, ink, start, end, paper))
            val zoom = app.fogwalk.ui.map.OneFingerZoom(this)
            setOnTouchListener { _, e -> zoom.onTouch(e) }
        }
    }
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
) : Overlay() {
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
