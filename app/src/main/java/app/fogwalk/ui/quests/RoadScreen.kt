package app.fogwalk.ui.quests

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fogwalk.R
import app.fogwalk.data.Units
import app.fogwalk.domain.Bonds
import app.fogwalk.domain.Road
import app.fogwalk.ui.ArtImage
import app.fogwalk.ui.GameData
import app.fogwalk.ui.components.TopBar
import app.fogwalk.ui.components.parchment
import app.fogwalk.ui.content
import app.fogwalk.ui.distance
import app.fogwalk.ui.map.Sprites
import app.fogwalk.ui.theme.U
import app.fogwalk.ui.theme.UType
import kotlin.math.sin

/** Where node [n] of the road map stands across the page: the doorstep in the middle, then the road swings side to side. */
private fun across(n: Int): Float = if (n == 0) 0.5f else 0.5f + (if (n % 2 == 1) 1 else -1) * (0.2f + 0.06f * sin(n * 2.1f))

/** A point on the cubic from [a] to [b] that leaves and arrives going straight down, at [t]. */
private fun along(a: Offset, b: Offset, t: Float): Offset {
    val h = (b.y - a.y) / 2
    val p1 = Offset(a.x, a.y + h)
    val p2 = Offset(b.x, b.y - h)
    val u = 1 - t
    return a * (u * u * u) + p1 * (3 * u * u * t) + p2 * (3 * u * t * t) + b * (t * t * t)
}

/**
 * The Great Road as a painted map: from the walker's doorstep down past every station, the walked road in gold,
 * the companion walking it where the walker stands, and the road ahead fading into the Mist. A station tapped
 * tells its chapter and its gift.
 */
@Composable
fun RoadScreen(data: GameData, onBack: () -> Unit) {
    val c = content()
    val imperial = data.settings.units == Units.IMPERIAL
    var picked by remember { mutableStateOf<Int?>(null) }
    val stations = maxOf(Road.stations.size, data.stations + 2)
    // Node 0 is the doorstep, node n is station n - 1.
    val nodes = stations + 1
    val segH = 150.dp
    val top = 64.dp
    val scroll = rememberScrollState()
    val density = LocalDensity.current
    val screenH = LocalConfiguration.current.screenHeightDp.dp
    val leg = Road.leg(data.roadKm)
    var scrolled by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.road_title), onBack)
        Column(Modifier.fillMaxSize().verticalScroll(scroll).navigationBarsPadding()) {
            Column(Modifier.padding(horizontal = 18.dp)) {
                Text(
                    stringResource(R.string.road_walked, distance(data.roadKm * 1000, imperial)) +
                        if (data.todayRoadM >= 10) " · " + stringResource(R.string.road_today, distance(data.todayRoadM, imperial)) else "",
                    style = UType.Strong.copy(color = U.c.gold),
                )
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.road_note), style = UType.Muted)
                Spacer(Modifier.height(12.dp))
                SatchelCard(data)
            }
            BoxWithConstraints(Modifier.fillMaxWidth().height(top * 2 + segH * (nodes - 1)).parchment(0.3f)) {
                val w = maxWidth
                fun x(n: Int): Dp = w * across(n)
                fun y(n: Int): Dp = top + segH * n
                val gold = U.c.gold
                val bed = U.c.line
                val bg = U.c.bg
                val walked = data.stations
                // The road: a pale bed all the way, gold where walked, dashes ahead.
                Canvas(Modifier.fillMaxSize()) {
                    fun p(n: Int) = Offset(x(n).toPx(), y(n).toPx())
                    for (n in 0 until nodes - 1) {
                        val a = p(n)
                        val b = p(n + 1)
                        val h = (b.y - a.y) / 2
                        val path = Path().apply { moveTo(a.x, a.y); cubicTo(a.x, a.y + h, b.x, b.y - h, b.x, b.y) }
                        drawPath(path, bed.copy(alpha = 0.35f), style = Stroke(16.dp.toPx(), cap = StrokeCap.Round))
                        when {
                            n < walked -> drawPath(path, gold, style = Stroke(6.dp.toPx(), cap = StrokeCap.Round))
                            else -> {
                                drawPath(path, bed, style = Stroke(3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 8.dp.toPx()))))
                                if (n == walked) {
                                    // The stretch walked toward the next station, as short gold steps along the curve.
                                    val steps = (leg * 40).toInt()
                                    var prev = a
                                    for (k in 1..steps) {
                                        val q = along(a, b, k / 40f)
                                        drawLine(gold, prev, q, 6.dp.toPx(), cap = StrokeCap.Round)
                                        prev = q
                                    }
                                }
                            }
                        }
                    }
                    // A glow under the caravan.
                    val at = along(p(walked), p(walked + 1), leg)
                    drawCircle(Brush.radialGradient(listOf(gold.copy(alpha = 0.45f), gold.copy(alpha = 0f)), at, 46.dp.toPx()), 46.dp.toPx(), at)
                }

                for (n in 0 until nodes) {
                    val i = n - 1
                    val reached = n == 0 || i < walked
                    val known = reached || i == walked
                    StationNode(
                        art = stationArt(i), reached = reached, next = i == walked,
                        modifier = Modifier.offset(x(n) - 32.dp, y(n) - 32.dp),
                    ) { picked = i }
                    val left = across(n) > 0.5f
                    val labelW = if (left) x(n) - 52.dp else w - x(n) - 52.dp
                    Column(
                        Modifier.offset(if (left) 12.dp else x(n) + 40.dp, y(n) - 26.dp).width(labelW).clickable { picked = i },
                        horizontalAlignment = if (left) Alignment.End else Alignment.Start,
                    ) {
                        Text(
                            if (n == 0) stringResource(R.string.road_start) else distance(Road.at(i) * 1000.0, imperial),
                            style = UType.Small.copy(color = if (reached) U.c.gold else U.c.faint),
                        )
                        Text(
                            if (n == 0) stringResource(R.string.road_title) else if (known) c.stationName(i) else "· · ·",
                            style = UType.Strong.copy(fontSize = 15.sp, color = if (reached) U.c.ink else U.c.muted, textAlign = if (left) TextAlign.End else TextAlign.Start),
                        )
                        if (i == walked) Text(
                            stringResource(R.string.road_gift_label, giftName(Road.gift(i))),
                            style = UType.Small.copy(textAlign = if (left) TextAlign.End else TextAlign.Start),
                        )
                    }
                }

                // The caravan: the companion on the map, or the walker.
                val pal = data.pal.takeIf { Bonds.befriends(it) }?.let { Sprites.ids["companion_$it"] } ?: R.drawable.ui_walker
                val at = with(density) {
                    along(Offset(x(walked).toPx(), y(walked).toPx()), Offset(x(walked + 1).toPx(), y(walked + 1).toPx()), leg).let { o -> o.x.toDp() to o.y.toDp() }
                }
                ArtImage(pal, Modifier.offset(at.first - 30.dp, at.second - 44.dp).size(60.dp))

                // The Mist over the road ahead, past the next station.
                Canvas(Modifier.fillMaxSize()) {
                    val from = (y(walked + 1) + 56.dp).toPx()
                    val fade = segH.toPx()
                    drawRect(Brush.verticalGradient(listOf(bg.copy(alpha = 0f), bg.copy(alpha = 0.9f)), from, from + fade), Offset(0f, from), androidx.compose.ui.geometry.Size(size.width, fade))
                    drawRect(bg.copy(alpha = 0.9f), Offset(0f, from + fade), androidx.compose.ui.geometry.Size(size.width, (size.height - from - fade).coerceAtLeast(0f)))
                    // Wisps along the mist's edge.
                    for (k in 0 until 6) {
                        val cx = size.width * (k + 0.5f) / 6
                        val cy = from + fade * (0.35f + 0.25f * sin(k * 1.7f))
                        drawCircle(Brush.radialGradient(listOf(bg.copy(alpha = 0.7f), bg.copy(alpha = 0f)), Offset(cx, cy), 70.dp.toPx()), 70.dp.toPx(), Offset(cx, cy))
                    }
                }
                Text(
                    stringResource(R.string.road_ahead_mist), style = UType.Muted.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth().offset(y = y(walked + 1) + segH + 40.dp).padding(horizontal = 32.dp),
                )

                LaunchedEffect(scroll.maxValue) {
                    if (scrolled || scroll.maxValue == 0) return@LaunchedEffect
                    scrolled = true
                    val header = with(density) { 180.dp.toPx() }
                    val target = with(density) { at.second.toPx() } + header - with(density) { screenH.toPx() } / 2
                    scroll.scrollTo(target.toInt().coerceIn(0, scroll.maxValue))
                }
            }
        }
    }
    picked?.let { i -> StationDialog(data, i) { picked = null } }
}

/** A station on the map: its painting in a gilt medallion, greyed while the road hasn't reached it. */
@Composable
private fun StationNode(art: Int, reached: Boolean, next: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(64.dp).clip(CircleShape)
            .background(if (reached) U.c.goldSoft else U.c.surface2)
            .border(if (next) 3.dp else 2.dp, if (reached || next) U.c.gilt else U.c.line, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { ArtImage(art, Modifier.size(48.dp), dim = !reached) }
}

/** A station's page: its chapter once reached, its gift, and the Mist's silence for those far ahead. */
@Composable
private fun StationDialog(data: GameData, i: Int, onClose: () -> Unit) {
    val c = content()
    val imperial = data.settings.units == Units.IMPERIAL
    val reached = i < data.stations
    val known = reached || i == data.stations
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Column(
            Modifier.fillMaxWidth().clip(U.RLg).background(U.c.bg).clickable(onClick = onClose).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(96.dp).clip(CircleShape).background(if (reached) U.c.goldSoft else U.c.surface2), contentAlignment = Alignment.Center) {
                ArtImage(stationArt(i), Modifier.size(76.dp), dim = !reached)
            }
            Spacer(Modifier.height(14.dp))
            if (i < 0) {
                Text(stringResource(R.string.road_start), style = UType.Title.copy(fontSize = 22.sp))
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.road_note), style = UType.Sub.copy(textAlign = TextAlign.Center))
                return@Column
            }
            Text(
                stringResource(R.string.road_station_km, distance(Road.at(i) * 1000.0, imperial)).uppercase(),
                style = UType.Eyebrow.copy(color = U.c.gold),
            )
            Spacer(Modifier.height(4.dp))
            Text(if (known) c.stationName(i) else "· · ·", style = UType.Title.copy(fontSize = 22.sp, textAlign = TextAlign.Center))
            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    reached -> c.stationLore(i)
                    known -> stringResource(R.string.road_locked)
                    else -> stringResource(R.string.road_ahead_mist)
                },
                style = UType.Sub.copy(textAlign = TextAlign.Center),
            )
            Spacer(Modifier.height(14.dp))
            val gift = Road.gift(i)
            Row(verticalAlignment = Alignment.CenterVertically) {
                ArtImage(giftArt(gift), Modifier.size(32.dp), dim = !reached)
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(stringResource(R.string.road_gift_label, giftName(gift)), style = UType.Strong.copy(fontSize = 14.sp))
                    Text(stringResource(R.string.pal_gift_got, Road.coins(i), Road.xp(i)), style = UType.Small.copy(color = U.c.gold))
                }
            }
        }
    }
}
