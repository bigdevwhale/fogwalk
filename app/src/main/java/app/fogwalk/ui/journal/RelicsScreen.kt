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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fogwalk.R
import app.fogwalk.domain.Relic
import app.fogwalk.domain.Relics
import app.fogwalk.ui.GameData
import app.fogwalk.ui.components.CelebrateDialog
import app.fogwalk.ui.components.TopBar
import app.fogwalk.ui.components.UCard
import app.fogwalk.ui.content
import app.fogwalk.ui.rarityColor
import app.fogwalk.ui.relicIcon
import app.fogwalk.ui.setIcon
import app.fogwalk.ui.theme.U
import app.fogwalk.ui.theme.UType

/** The hoard of relics: every set with what is found and what is still out there. */
@Composable
fun RelicsScreen(data: GameData, onBack: () -> Unit) {
    val c = content()
    var shown by remember { mutableStateOf<Relic?>(null) }
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.relics_count, data.relics.size, Relics.all.size), onBack)
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 24.dp).navigationBarsPadding()) {
            Relics.sets.forEach { set ->
                val relics = Relics.inSet(set)
                val have = relics.count { it.id in data.relics }
                val (color, soft) = rarityColor(Relics.setRarity(set))
                UCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(soft), contentAlignment = Alignment.Center) {
                            Icon(setIcon(set), null, Modifier.size(20.dp), tint = color)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.setName(set), style = UType.Strong)
                            Text(
                                if (have == relics.size) stringResource(R.string.set_title, c.setTitle(set))
                                else stringResource(R.string.set_reward, Relics.setReward(set).first),
                                style = UType.Muted,
                            )
                        }
                        Text("$have/${relics.size}", style = UType.Strong.copy(color = if (have == relics.size) color else U.c.muted))
                    }
                    Spacer(Modifier.height(12.dp))
                    relics.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            row.forEach { r ->
                                val owned = r.id in data.relics
                                Column(
                                    Modifier.weight(1f).clip(U.RSm).background(if (owned) soft else U.c.surface2)
                                        .clickable(enabled = owned) { shown = r }.padding(vertical = 10.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    if (owned) Icon(relicIcon(r.id), null, Modifier.size(26.dp), tint = color)
                                    else Text("?", style = UType.Strong.copy(fontSize = 20.sp, color = U.c.faint))
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        if (owned) c.relicName(r.id) else stringResource(R.string.relic_unknown),
                                        style = UType.Small.copy(color = if (owned) U.c.ink else U.c.faint, textAlign = TextAlign.Center),
                                        maxLines = 2, overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
            }
        }
    }
    shown?.let { r ->
        val (color, soft) = rarityColor(r.rarity)
        val count = data.finds.count { it.relic == r.id }
        CelebrateDialog(
            icon = relicIcon(r.id), color = color, soft = soft,
            eyebrow = c.rarity(r.rarity) + " · " + c.setName(r.set),
            title = c.relicName(r.id),
            text = c.relicLore(r.id) + "\n\n" + stringResource(R.string.relic_found_times, count),
            confetti = false,
        ) { shown = null }
    }
}

/** The relics in brief for the chronicle: how many are found, and the way into the hoard. */
@Composable
internal fun RelicsSummary(data: GameData, onOpen: () -> Unit) {
    val c = content()
    val complete = Relics.completeSets(data.relics)
    UCard(onClick = onOpen) {
        Relics.sets.forEach { set ->
            val relics = Relics.inSet(set)
            val have = relics.count { it.id in data.relics }
            val (color, soft) = rarityColor(Relics.setRarity(set))
            Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(soft), contentAlignment = Alignment.Center) {
                    Icon(setIcon(set), null, Modifier.size(17.dp), tint = color)
                }
                Spacer(Modifier.width(12.dp))
                Text(c.setName(set), style = UType.Strong.copy(fontSize = 15.sp), modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$have/${relics.size}", style = UType.Strong.copy(fontSize = 14.sp, color = if (have == relics.size) color else U.c.muted))
            }
        }
        Text(stringResource(R.string.relics_sets, complete.size, Relics.sets.size), style = UType.Muted)
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.relics_open), style = UType.Button.copy(color = U.c.accent))
    }
}
