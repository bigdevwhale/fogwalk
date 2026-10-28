package app.fogwalk.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fogwalk.R
import app.fogwalk.domain.Relic
import app.fogwalk.domain.Relics
import app.fogwalk.domain.Seasons
import app.fogwalk.ui.GameData
import app.fogwalk.ui.components.CelebrateDialog
import app.fogwalk.ui.components.SectionTitle
import app.fogwalk.ui.components.TopBar
import app.fogwalk.ui.components.UCard
import app.fogwalk.ui.content
import app.fogwalk.ui.locale
import app.fogwalk.ui.rarityColor
import app.fogwalk.ui.relicIcon
import app.fogwalk.ui.setIcon
import app.fogwalk.ui.theme.U
import app.fogwalk.ui.theme.UType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** The hoard of relics: every set with what is found, its stars, and what is still out there. */
@Composable
fun RelicsScreen(data: GameData, onBack: () -> Unit) {
    val c = content()
    var shown by remember { mutableStateOf<Relic?>(null) }
    // This season's set first, then the rest in the order they come round.
    val seasonal = remember(data.season) { Relics.seasonal.sortedBy { Seasons.startOf(it, data.today, data.settings.lastLat) } }
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.relics_count, data.relics.size, Relics.all.size), onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            Text(stringResource(R.string.relic_stars_intro), style = UType.Muted, modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp))
            Spacer(Modifier.height(10.dp))
            Relics.core.forEach { set ->
                SetCard(set, data) { shown = it }
                Spacer(Modifier.height(14.dp))
            }
            SectionTitle(stringResource(R.string.seasonal_sets))
            Text(stringResource(R.string.seasonal_intro), style = UType.Muted, modifier = Modifier.padding(horizontal = 4.dp))
            Spacer(Modifier.height(10.dp))
            seasonal.forEach { set ->
                SetCard(set, data) { shown = it }
                Spacer(Modifier.height(14.dp))
            }
        }
    }
    shown?.let { r ->
        val (color, soft) = rarityColor(r.rarity)
        val count = data.copies[r.id] ?: 0
        val next = Relics.toNextStar(count)
        CelebrateDialog(
            icon = relicIcon(r.id), color = color, soft = soft,
            eyebrow = c.rarity(r.rarity) + " · " + c.setName(r.set),
            title = c.relicName(r.id) + "  " + stars(Relics.stars(count)),
            text = c.relicLore(r.id) + "\n\n" + stringResource(R.string.relic_found_times, count) + "\n" +
                if (next > 0) stringResource(R.string.relic_stars_next, next) else stringResource(R.string.relic_stars_max),
            confetti = false,
        ) { shown = null }
    }
}

/** "★★☆☆☆" */
internal fun stars(n: Int): String = "★".repeat(n) + "☆".repeat(Relics.MAX_STARS - n)

@Composable
private fun SetCard(set: String, data: GameData, onRelic: (Relic) -> Unit) {
    val c = content()
    val relics = Relics.inSet(set)
    val have = relics.count { it.id in data.relics }
    val complete = have == relics.size
    val (color, soft) = rarityColor(Relics.setRarity(set))
    val inSeason = data.season == set
    UCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(40.dp).clip(CircleShape).background(soft), contentAlignment = Alignment.Center) {
                Icon(setIcon(set), null, Modifier.size(20.dp), tint = color)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.setName(set), style = UType.Strong)
                Text(
                    if (complete) stringResource(R.string.set_title, c.setTitle(set))
                    else stringResource(R.string.set_reward, Relics.setReward(set).first),
                    style = UType.Muted,
                )
                if (complete) {
                    val star = Relics.setStars(set, data.copies)
                    Text(
                        if (star >= Relics.MAX_STARS) stringResource(R.string.set_stars_max)
                        else stringResource(R.string.set_stars_next, star + 1, Relics.starReward(set, star + 1).first),
                        style = UType.Muted.copy(color = U.c.gold),
                    )
                }
                if (Relics.isSeasonal(set)) SeasonLine(set, data, inSeason)
            }
            Text("$have/${relics.size}", style = UType.Strong.copy(color = if (complete) color else U.c.muted))
        }
        Spacer(Modifier.height(12.dp))
        relics.chunked(3).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { r -> RelicTile(r, data, Modifier.weight(1f)) { onRelic(r) } }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun SeasonLine(set: String, data: GameData, inSeason: Boolean) {
    val c = content()
    val l = locale()
    val fmt = remember(l) { DateTimeFormatter.ofPattern("d MMMM", l) }
    val text = if (inSeason) {
        stringResource(R.string.season_now, LocalDate.ofEpochDay(Seasons.nextStart(data.today) - 1).format(fmt))
    } else {
        stringResource(R.string.season_later, LocalDate.ofEpochDay(Seasons.startOf(set, data.today, data.settings.lastLat)).format(fmt))
    }
    Text(c.seasonName(set) + " · " + text, style = UType.Small.copy(color = if (inSeason) U.c.sage else U.c.faint))
}

@Composable
private fun RelicTile(r: Relic, data: GameData, modifier: Modifier, onClick: () -> Unit) {
    val c = content()
    val (color, soft) = rarityColor(r.rarity)
    val owned = r.id in data.relics
    Column(
        modifier.clip(U.RSm).background(if (owned) soft else U.c.surface2)
            .clickable(enabled = owned, onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Every tile keeps room for an icon, two lines of name and the stars, so the grid stays even.
        Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            if (owned) Icon(relicIcon(r.id), null, Modifier.size(26.dp), tint = color)
            else Text("?", style = UType.Strong.copy(fontSize = 20.sp, color = U.c.faint))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (owned) c.relicName(r.id) else stringResource(R.string.relic_unknown),
            style = UType.Small.copy(color = if (owned) U.c.ink else U.c.faint, textAlign = TextAlign.Center),
            minLines = 2, maxLines = 2, overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(2.dp))
        val n = Relics.stars(data.copies[r.id] ?: 0)
        Text(
            stars(n),
            style = UType.Small.copy(fontSize = 11.sp, color = if (!owned) Color.Transparent else if (n > 0) U.c.gold else U.c.faint.copy(alpha = 0.6f)),
            maxLines = 1,
        )
    }
}

/** The relics in brief for the chronicle: how many are found, and the way into the hoard. */
@Composable
internal fun RelicsSummary(data: GameData, onOpen: () -> Unit) {
    val c = content()
    val complete = Relics.completeSets(data.relics)
    // The all-year sets, and the one in season now.
    val shown = Relics.core + data.season
    UCard(onClick = onOpen) {
        shown.forEach { set ->
            val relics = Relics.inSet(set)
            val have = relics.count { it.id in data.relics }
            val (color, soft) = rarityColor(Relics.setRarity(set))
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(soft), contentAlignment = Alignment.Center) {
                    Icon(setIcon(set), null, Modifier.size(17.dp), tint = color)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.setName(set), style = UType.Strong.copy(fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (Relics.isSeasonal(set)) Text(c.seasonName(set), style = UType.Small.copy(color = U.c.sage))
                }
                if (have == relics.size) {
                    Text(stars(Relics.setStars(set, data.copies)), style = UType.Small.copy(fontSize = 11.sp, color = U.c.gold))
                    Spacer(Modifier.width(8.dp))
                }
                Text("$have/${relics.size}", style = UType.Strong.copy(fontSize = 14.sp, color = if (have == relics.size) color else U.c.muted))
            }
        }
        Text(stringResource(R.string.relics_sets, complete.size, Relics.sets.size), style = UType.Muted)
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.relics_open), style = UType.Button.copy(color = U.c.accent))
    }
}
