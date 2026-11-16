package app.fogwalk.ui.quests

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fogwalk.R
import app.fogwalk.container
import app.fogwalk.data.Units
import app.fogwalk.domain.Rarity
import app.fogwalk.domain.Road
import app.fogwalk.domain.Shop
import app.fogwalk.domain.Wayside
import app.fogwalk.ui.Art
import app.fogwalk.ui.ArtImage
import app.fogwalk.ui.GameData
import app.fogwalk.ui.Go
import app.fogwalk.ui.components.PrimaryButton
import app.fogwalk.ui.components.UCard
import app.fogwalk.ui.content
import app.fogwalk.ui.distance
import app.fogwalk.ui.theme.U
import app.fogwalk.ui.theme.UType
import kotlinx.coroutines.launch

/** The painting of station [i] of the Great Road; -1 is the walker's own doorstep. */
@DrawableRes
fun stationArt(i: Int): Int = when (i) {
    -1 -> R.drawable.ui_hearth
    0 -> R.drawable.ui_castle
    1 -> R.drawable.ui_drop
    2 -> R.drawable.ui_ship
    3 -> R.drawable.ui_oak
    4 -> R.drawable.item_torch
    5 -> R.drawable.monument_1
    6 -> R.drawable.ui_sprig
    7 -> R.drawable.ui_bell
    8 -> R.drawable.ui_stall
    9 -> R.drawable.ui_mountain
    10 -> R.drawable.ui_chronicle
    11 -> R.drawable.ui_forest
    12 -> R.drawable.monument_3
    13 -> R.drawable.ui_moon_stars
    14 -> R.drawable.ui_waves
    15 -> R.drawable.ui_atlas
    16 -> R.drawable.ui_radiant
    else -> R.drawable.ui_globe
}

/** A station's gift, in words: "a ward of the streak", "an epic relic". */
@Composable
fun giftName(g: Road.Gift): String = when (g) {
    Road.Gift.Ward -> stringResource(R.string.road_gift_ward)
    Road.Gift.Altar -> stringResource(R.string.road_gift_altar)
    is Road.Gift.Relic -> content().relicOf(g.rarity)
}

@DrawableRes
fun giftArt(g: Road.Gift): Int = when (g) {
    Road.Gift.Ward -> Art.WARD
    Road.Gift.Altar -> Art.ALTAR
    is Road.Gift.Relic -> Art.chest(g.rarity)
}

@DrawableRes
fun waysideArt(f: Wayside.Find): Int = when (f.kind) {
    Wayside.Kind.PURSE -> R.drawable.ui_pouch
    Wayside.Kind.TALE -> Art.SCROLL
    Wayside.Kind.STRONGBOX -> R.drawable.chest_common_open
    Wayside.Kind.WARD -> Art.WARD
    Wayside.Kind.RELIC -> Art.chest(f.rarity)
}

/** The caravan on the Great Road: how far it has come, today's stretch and the next station with its gift; tapped, the road's map. */
@Composable
fun RoadCard(data: GameData, go: Go) {
    val c = content()
    val imperial = data.settings.units == Units.IMPERIAL
    val next = data.stations
    UCard(onClick = { go.road() }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(U.c.goldSoft), contentAlignment = Alignment.Center) {
                ArtImage(R.drawable.ui_road, Modifier.size(44.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.road_title), style = UType.Title.copy(fontSize = 20.sp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.road_walked, distance(data.roadKm * 1000, imperial)), style = UType.Small.copy(color = U.c.gold))
                    if (data.todayRoadM >= 10) {
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.road_today, distance(data.todayRoadM, imperial)), style = UType.Small.copy(color = U.c.sage))
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        RoadStrip(Road.leg(data.roadKm), stationArt(next - 1), stationArt(next))
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.road_next, c.stationName(next), distance(Road.at(next) * 1000.0 - data.roadKm * 1000, imperial)),
            style = UType.Strong.copy(fontSize = 15.sp),
        )
        Spacer(Modifier.height(4.dp))
        val gift = Road.gift(next)
        Row(verticalAlignment = Alignment.CenterVertically) {
            ArtImage(giftArt(gift), Modifier.size(24.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(R.string.road_gift_label, giftName(gift)) + " · " + stringResource(R.string.pal_gift_got, Road.coins(next), Road.xp(next)),
                style = UType.Small,
            )
        }
    }
}

/** A road between two stations, the walked part drawn in, the caravan at [share] and the stations' paintings at its ends. */
@Composable
private fun RoadStrip(share: Float, @DrawableRes from: Int, @DrawableRes to: Int) {
    val gold = U.c.gold
    val line = U.c.line
    val surface = U.c.surface
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(U.c.goldSoft), contentAlignment = Alignment.Center) { ArtImage(from, Modifier.size(32.dp)) }
        Canvas(Modifier.weight(1f).height(22.dp).padding(horizontal = 4.dp)) {
            val y = size.height / 2
            val r = 6.dp.toPx()
            val x0 = r
            val x1 = size.width - r
            val at = x0 + (x1 - x0) * share
            drawLine(line, Offset(x0, y), Offset(x1, y), 3.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())))
            drawLine(gold, Offset(x0, y), Offset(at, y), 4.dp.toPx())
            drawCircle(surface, r + 2.dp.toPx(), Offset(at, y))
            drawCircle(gold, r + 2.dp.toPx(), Offset(at, y), style = Stroke(3.dp.toPx()))
            drawCircle(gold, r - 2.dp.toPx(), Offset(at, y))
        }
        Box(Modifier.size(40.dp).clip(CircleShape).background(U.c.surface2), contentAlignment = Alignment.Center) { ArtImage(to, Modifier.size(32.dp), dim = true) }
    }
}

/** The walker's satchel: the road's finds waiting to be taken, or how far the next one is. */
@Composable
fun SatchelCard(data: GameData) {
    val imperial = data.settings.units == Units.IMPERIAL
    var open by rememberSaveable { mutableStateOf(false) }
    val n = data.satchel.size
    UCard(onClick = if (n > 0) ({ open = true }) else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(if (n > 0) U.c.goldSoft else U.c.surface2), contentAlignment = Alignment.Center) {
                ArtImage(R.drawable.gear_satchel, Modifier.size(46.dp), dim = n == 0)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.satchel_title), style = UType.Title.copy(fontSize = 20.sp))
                Text(
                    if (n > 0) stringResource(R.string.satchel_count, n, Wayside.SATCHEL)
                    else stringResource(R.string.satchel_empty, distance(Wayside.toNext(data.roadKm), imperial)),
                    style = UType.Small.copy(color = if (n > 0) U.c.gold else U.c.muted),
                )
            }
            if (n > 0) {
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(R.string.satchel_open), style = UType.Button.copy(color = U.c.onAccent),
                    modifier = Modifier.clip(CircleShape).background(U.c.accent).clickable { open = true }.padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
        }
        if (n > 0) {
            Spacer(Modifier.height(10.dp))
            // A peek at what is inside.
            Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                data.satchel.forEach { f ->
                    Box(
                        Modifier.size(34.dp).clip(CircleShape).background(U.c.surface).border(1.dp, U.c.gilt, CircleShape).padding(3.dp),
                        contentAlignment = Alignment.Center,
                    ) { ArtImage(waysideArt(f), Modifier.size(26.dp)) }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (n >= Wayside.SATCHEL) stringResource(R.string.satchel_full) else stringResource(R.string.satchel_note),
            style = UType.Small.copy(color = if (n >= Wayside.SATCHEL) U.c.flame else U.c.muted),
        )
    }
    if (open && n > 0) SatchelDialog(data) { open = false }
}

/** Everything in the satchel, kilometre by kilometre, and a hand to take it all. */
@Composable
private fun SatchelDialog(data: GameData, onClose: () -> Unit) {
    val context = LocalContext.current
    val c = content()
    val full = data.settings.shields >= Shop.MAX_SHIELDS
    // A second tap of the one that opened the satchel must not empty it unseen.
    val shownAt = androidx.compose.runtime.remember { System.currentTimeMillis() }
    androidx.compose.ui.window.Dialog(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().clip(U.RLg).background(U.c.bg).padding(20.dp)) {
            Text(stringResource(R.string.satchel_title), style = UType.Title.copy(fontSize = 22.sp))
            Spacer(Modifier.height(12.dp))
            Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                data.satchel.forEach { f ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(52.dp).clip(CircleShape).background(U.c.goldSoft), contentAlignment = Alignment.Center) {
                            ArtImage(waysideArt(f), Modifier.size(42.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(R.string.satchel_km, f.km), style = UType.Small.copy(color = U.c.faint))
                            Text(c.waysideFind(f.kind, f.variant), style = UType.Strong.copy(fontSize = 15.sp))
                            val reward = when (f.kind) {
                                Wayside.Kind.WARD -> if (full) stringResource(R.string.satchel_ward_full, Wayside.WARD_COINS) else stringResource(R.string.road_gift_ward)
                                Wayside.Kind.RELIC -> c.relicOf(f.rarity ?: Rarity.COMMON)
                                else -> if (f.coins > 0) stringResource(R.string.pal_gift_got, f.coins, f.xp) else stringResource(R.string.plus_xp, f.xp)
                            }
                            Text(reward, style = UType.Small.copy(color = U.c.gold))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(stringResource(R.string.satchel_take), {
                if (System.currentTimeMillis() - shownAt < 500) return@PrimaryButton
                val container = context.container
                container.scope.launch { container.explorer.openSatchel() }
                onClose()
            }, Modifier.fillMaxWidth(), color = U.c.gold)
        }
    }
}
