package app.fogwalk.ui.journal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fogwalk.R
import app.fogwalk.data.Units
import app.fogwalk.domain.Badge
import app.fogwalk.domain.Badges
import app.fogwalk.domain.Sectors
import app.fogwalk.geo.Grid
import app.fogwalk.ui.GameData
import app.fogwalk.ui.Go
import app.fogwalk.ui.MapFocus
import app.fogwalk.ui.Tab
import app.fogwalk.ui.area
import app.fogwalk.ui.badgeColor
import app.fogwalk.ui.badgeIcon
import app.fogwalk.ui.components.Bar
import app.fogwalk.ui.components.CelebrateDialog
import app.fogwalk.ui.components.SectionTitle
import app.fogwalk.ui.components.UCard
import app.fogwalk.ui.content
import app.fogwalk.ui.distance
import app.fogwalk.ui.locale
import app.fogwalk.ui.theme.U
import app.fogwalk.ui.theme.UType
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.sqrt

@Composable
fun JournalTab(data: GameData, go: Go, focus: MutableStateFlow<MapFocus?>) {
    val imperial = data.settings.units == Units.IMPERIAL
    val c = content()
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 24.dp),
    ) {
        Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.tab_journal), style = UType.Title, modifier = Modifier.weight(1f))
            IconButton(onClick = { go.settings() }) { Icon(Icons.Rounded.Settings, stringResource(R.string.settings), tint = U.c.muted) }
        }

        UCard {
            Text(stringResource(R.string.explored).uppercase(), style = UType.Eyebrow)
            Spacer(Modifier.height(6.dp))
            Text(area(data.areaM2, imperial), style = UType.Display)
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.world_share, worldShare(data.areaM2)), style = UType.Muted)
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat(Icons.AutoMirrored.Rounded.DirectionsWalk, distance(data.facts.km * 1000, imperial), stringResource(R.string.stat_walked), U.c.sage, Modifier.weight(1f))
                Stat(Icons.Rounded.Explore, data.facts.sectors.toString(), stringResource(R.string.stat_sectors), U.c.accent, Modifier.weight(1f))
                Stat(Icons.Rounded.Inventory2, data.facts.caches.toString(), stringResource(R.string.stat_caches), U.c.gold, Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth()) {
                Stat(Icons.Rounded.Flare, data.facts.beacons.toString(), stringResource(R.string.stat_beacons), U.c.rose, Modifier.weight(1f))
                Stat(Icons.Rounded.LocalFireDepartment, data.streak.best.toString(), stringResource(R.string.stat_best_streak), U.c.flame, Modifier.weight(1f))
                Stat(Icons.Rounded.Explore, "%,d".format(data.cells), stringResource(R.string.stat_cells), U.c.sky, Modifier.weight(1f))
            }
        }

        SectionTitle(stringResource(R.string.activity))
        UCard { Heatmap(data) }

        SectionTitle(stringResource(R.string.sectors))
        val sectors = data.sectors.first.indices.sortedByDescending { data.sectors.second[it] }.take(8)
        if (sectors.isEmpty()) {
            Text(stringResource(R.string.sectors_empty), style = UType.Muted, modifier = Modifier.padding(horizontal = 4.dp))
        } else UCard {
            sectors.forEachIndexed { n, i ->
                val key = data.sectors.first[i]
                val share = data.sectors.second[i].toDouble() / Grid.SECTOR_CELLS
                Column(
                    Modifier.fillMaxWidth().clip(U.RSm).clickable {
                        focus.value = MapFocus(Grid.centerLat(key, Grid.SECTOR_Z), Grid.centerLon(key, Grid.SECTOR_Z), 14.6)
                        go.tab(Tab.MAP)
                    }.padding(vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(c.sectorName(key), style = UType.Strong.copy(fontSize = 15.sp), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(percent(share), style = UType.Strong.copy(fontSize = 14.sp, color = if (share >= Sectors.CHARTED) U.c.accent else U.c.muted))
                    }
                    Spacer(Modifier.height(6.dp))
                    SectorBar(share)
                }
                if (n < sectors.size - 1) Spacer(Modifier.height(2.dp))
            }
        }

        SectionTitle(stringResource(R.string.badges_count, data.badges.size, Badges.all.size))
        DeedsSummary(data, go)

        val walks = remember(data.walks) { journeys(data.walks) }
        if (walks.isNotEmpty()) {
            SectionTitle(stringResource(R.string.recent_walks))
            UCard(padding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                walks.take(5).forEachIndexed { i, w ->
                    Box(Modifier.fillMaxWidth().clip(U.RSm).clickable { go.walk(w.id) }.padding(vertical = 8.dp)) { WalkRow(w, data, imperial) }
                    Box(Modifier.fillMaxWidth().height(1.dp).background(U.c.line))
                }
                Text(
                    stringResource(R.string.walks_all, walks.size), style = UType.Button.copy(color = U.c.accent),
                    modifier = Modifier.fillMaxWidth().clip(U.RSm).clickable { go.walks() }.padding(vertical = 12.dp, horizontal = 4.dp),
                )
            }
        }
    }
}

/** "0.000012 %": the share of the Earth's surface explored, with enough digits to show something. */
private fun worldShare(m2: Double): String {
    val p = m2 / 1_000_000 / Grid.EARTH_KM2 * 100
    if (p <= 0) return "0"
    var digits = 2
    while (digits < 12 && p * Math.pow(10.0, digits.toDouble()) < 10) digits++
    return String.format(java.util.Locale.ROOT, "%.${digits}f", p)
}

private fun percent(share: Double): String = if (share < 0.01) "<1%" else "${(share * 100).toInt()}%"

@Composable
private fun Stat(icon: ImageVector, value: String, label: String, color: Color, modifier: Modifier) {
    Column(modifier) {
        Icon(icon, null, Modifier.size(18.dp), tint = color)
        Spacer(Modifier.height(4.dp))
        Text(value, style = UType.Number.copy(fontSize = 18.sp), maxLines = 1)
        Text(label, style = UType.Small, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** A sector's progress with ticks at each milestone. */
@Composable
private fun SectorBar(share: Double) {
    val track = U.c.accentSoft
    val fill = U.c.accent
    val tick = U.c.gold
    val max = 0.5
    Canvas(Modifier.fillMaxWidth().height(8.dp)) {
        val r = CornerRadius(size.height / 2)
        drawRoundRect(track, cornerRadius = r)
        drawRoundRect(fill, size = Size(size.width * (share / max).coerceIn(0.02, 1.0).toFloat(), size.height), cornerRadius = r)
        Sectors.milestones.forEach { (at, _, _) ->
            val x = (size.width * at / max).toFloat()
            drawCircle(if (share >= at) Color.White else tick, size.height * 0.28f, Offset(x, size.height / 2))
        }
    }
}

/** Sixteen weeks of new ground, a square per day, darker for more. */
@Composable
private fun Heatmap(data: GameData) {
    val weeks = 16
    val byDay = remember(data.days) { data.days.associate { it.day to it.cells } }
    val max = (byDay.values.maxOrNull() ?: 0).coerceAtLeast(1)
    val monday = data.today - Math.floorMod(data.today + 3, 7L)
    val first = monday - (weeks - 1) * 7
    val empty = U.c.surface2
    val color = U.c.accent
    val todayRing = U.c.ink
    Canvas(Modifier.fillMaxWidth().aspectRatio(weeks / 7f)) {
        val cell = size.width / weeks
        val gap = cell * 0.16f
        for (w in 0 until weeks) for (d in 0 until 7) {
            val day = first + w * 7 + d
            if (day > data.today) continue
            val v = byDay[day] ?: 0
            val k = if (v == 0) 0f else (0.25f + 0.75f * sqrt(v.toFloat() / max))
            val tl = Offset(w * cell + gap / 2, d * cell + gap / 2)
            val sz = Size(cell - gap, cell - gap)
            drawRoundRect(if (v == 0) empty else color.copy(alpha = k), tl, sz, CornerRadius(cell * 0.2f))
            if (day == data.today) drawRoundRect(todayRing, tl, sz, CornerRadius(cell * 0.2f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.5f))
        }
    }
    Spacer(Modifier.height(8.dp))
    Text(
        stringResource(R.string.activity_note, data.days.count { it.cells > 0 }),
        style = UType.Small,
    )
}

/** The deeds closest to done, and the way into the full book. */
@Composable
private fun DeedsSummary(data: GameData, go: Go) {
    val c = content()
    val done = data.badges.map { it.id }.toSet()
    // The next rung of each ladder, nearest first.
    val next = Badges.groups.mapNotNull { g -> Badges.all.firstOrNull { it.group == g.id && it.id !in done } }
        .sortedByDescending { it.share(data.facts) }.take(4)
    UCard(onClick = { go.deeds() }) {
        Text(stringResource(R.string.deeds_next), style = UType.Eyebrow)
        next.forEach { b ->
            val (color, soft) = badgeColor(b)
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(soft), contentAlignment = Alignment.Center) {
                    Icon(badgeIcon(b), null, Modifier.size(20.dp), tint = color)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.badgeTitle(b.id), style = UType.Strong.copy(fontSize = 15.sp))
                    Text(c.badgeText(b), style = UType.Muted)
                    Spacer(Modifier.height(5.dp))
                    Bar(b.share(data.facts).toFloat(), Modifier.height(6.dp), color = color, track = soft)
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.deeds_open), style = UType.Button.copy(color = U.c.accent))
    }
}
