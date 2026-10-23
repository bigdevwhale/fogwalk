package app.fogwalk.ui.journal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fogwalk.R
import app.fogwalk.container
import app.fogwalk.data.Units
import app.fogwalk.data.Walk
import app.fogwalk.data.Waypoint
import app.fogwalk.geo.Grid
import app.fogwalk.ui.GameData
import app.fogwalk.ui.components.TopBar
import app.fogwalk.ui.components.UCard
import app.fogwalk.ui.dateFormat
import app.fogwalk.ui.distance
import app.fogwalk.ui.locale
import app.fogwalk.ui.timeFormat
import app.fogwalk.ui.theme.U
import app.fogwalk.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Journeys worth keeping: a few steps around the flat don't count. */
fun journeys(walks: List<Walk>): List<Walk> = walks.filter { it.meters >= 50 || it.cells > 0 }

/** Every journey, newest first, by month, each with a sketch of its route. */
@Composable
fun WalksScreen(data: GameData, onBack: () -> Unit, onOpen: (Long) -> Unit) {
    val imperial = data.settings.units == Units.IMPERIAL
    val walks = remember(data.walks) { journeys(data.walks) }
    val l = locale()
    val month = remember(l) { DateTimeFormatter.ofPattern("LLLL yyyy", l) }
    val byMonth = remember(walks, l) {
        walks.groupBy { month.format(Instant.ofEpochMilli(it.start).atZone(data.zone)).replaceFirstChar { c -> c.titlecase(l) } }.toList()
    }
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.walks_title), onBack)
        LazyColumn(
            Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                UCard {
                    Row(Modifier.fillMaxWidth()) {
                        Total(walks.size.toString(), stringResource(R.string.walks_count), Modifier.weight(1f))
                        Total(distance(walks.sumOf { it.meters }, imperial), stringResource(R.string.stat_walked), Modifier.weight(1f))
                        Total(duration(walks.sumOf { it.end - it.start }), stringResource(R.string.walks_time), Modifier.weight(1f))
                    }
                }
            }
            if (walks.isEmpty()) item {
                Text(stringResource(R.string.walks_empty), style = UType.Muted, modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp))
            }
            byMonth.forEach { (title, list) ->
                item(key = "m:$title") {
                    Text(title.uppercase(l), style = UType.Eyebrow, modifier = Modifier.padding(start = 4.dp, top = 12.dp))
                }
                items(list, key = { it.id }) { w ->
                    UCard(onClick = { onOpen(w.id) }, padding = PaddingValues(12.dp)) { WalkRow(w, data, imperial) }
                }
            }
        }
    }
}

@Composable
private fun Total(value: String, label: String, modifier: Modifier) {
    Column(modifier) {
        Text(value, style = UType.Number.copy(fontSize = 19.sp), maxLines = 1)
        Text(label, style = UType.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A journey in a list: its route in miniature, when it was and what it brought. */
@Composable
fun WalkRow(w: Walk, data: GameData, imperial: Boolean) {
    val date = dateFormat(FormatStyle.MEDIUM)
    val time = timeFormat()
    Row(verticalAlignment = Alignment.CenterVertically) {
        RouteSketch(w.id, Modifier.size(52.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            val at = Instant.ofEpochMilli(w.start).atZone(data.zone)
            Text(date.format(at) + ", " + time.format(at), style = UType.Body.copy(fontSize = 14.sp), maxLines = 1)
            Text(
                listOfNotNull(
                    distance(w.meters, imperial), duration(w.end - w.start),
                    if (w.steps > 0) pluralStringResource(R.plurals.steps, w.steps, w.steps) else null,
                ).joinToString(" · ") +
                    if (w.caches > 0) " · " + pluralStringResource(R.plurals.caches, w.caches, w.caches) else "",
                style = UType.Muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Text("+" + "%,d".format(w.cells), style = UType.Strong.copy(color = U.c.accent))
    }
}

/** The route of walk [id], loaded from the database. */
@Composable
fun rememberRoute(id: Long): List<Waypoint>? {
    val dao = LocalContext.current.container.db.dao()
    val route by produceState<List<Waypoint>?>(null, id) { value = withContext(Dispatchers.IO) { dao.waypoints(id) } }
    return route
}

/** The route drawn small in a parchment square; a walking figure when none was kept. */
@Composable
private fun RouteSketch(id: Long, modifier: Modifier) {
    val route = rememberRoute(id)
    val ink = U.c.accent
    Box(modifier.clip(U.RSm).background(U.c.surface2), contentAlignment = Alignment.Center) {
        if (route == null) return@Box
        if (route.size < 2) {
            Icon(Icons.AutoMirrored.Rounded.DirectionsWalk, null, Modifier.size(22.dp), tint = U.c.faint)
            return@Box
        }
        val xs = route.map { Grid.mx(it.lon) }
        val ys = route.map { Grid.my(it.lat) }
        Canvas(Modifier.fillMaxSize().padding(7.dp)) {
            val x0 = xs.min()
            val y0 = ys.min()
            val span = maxOf(xs.max() - x0, ys.max() - y0, 1e-9)
            val k = size.minDimension / span
            // Centred in the square, north up.
            val dx = (size.width - (xs.max() - x0) * k) / 2
            val dy = (size.height - (ys.max() - y0) * k) / 2
            val path = Path()
            xs.indices.forEach { i ->
                val x = ((xs[i] - x0) * k + dx).toFloat()
                val y = ((ys[i] - y0) * k + dy).toFloat()
                if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(path, ink, style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawCircle(ink, 2.6.dp.toPx(), Offset(((xs.last() - x0) * k + dx).toFloat(), ((ys.last() - y0) * k + dy).toFloat()))
        }
    }
}

/** "48 min" or "1 h 12 min". */
@Composable
fun duration(ms: Long): String {
    val minutes = (ms / 60_000).coerceAtLeast(0).toInt()
    return if (minutes < 60) stringResource(R.string.minutes_short, minutes)
    else stringResource(R.string.hours_minutes, minutes / 60, minutes % 60)
}
