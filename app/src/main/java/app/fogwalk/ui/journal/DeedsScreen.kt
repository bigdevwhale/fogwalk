package app.fogwalk.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fogwalk.R
import app.fogwalk.domain.Badge
import app.fogwalk.domain.Badges
import app.fogwalk.ui.GameData
import app.fogwalk.ui.components.Bar
import app.fogwalk.ui.components.CelebrateDialog
import app.fogwalk.ui.components.TopBar
import app.fogwalk.ui.components.UCard
import app.fogwalk.ui.content
import app.fogwalk.ui.groupColor
import app.fogwalk.ui.groupIcon
import app.fogwalk.ui.theme.U
import app.fogwalk.ui.theme.UType

/** The book of deeds: every kind of feat as a ladder of tiers, the next rung with its progress. */
@Composable
fun DeedsScreen(data: GameData, onBack: () -> Unit) {
    val c = content()
    var shown by remember { mutableStateOf<Badge?>(null) }
    val done = data.badges.map { it.id }.toSet()
    Column(Modifier.fillMaxSize().background(U.c.bg).statusBarsPadding()) {
        TopBar(stringResource(R.string.badges_count, done.size, Badges.all.size), onBack)
        LazyColumn(
            Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(Badges.groups, key = { it.id }) { g ->
                val tiers = Badges.all.filter { it.group == g.id }
                val have = tiers.count { it.id in done }
                val next = tiers.firstOrNull { it.id !in done }
                val (color, soft) = groupColor(g.id)
                UCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(42.dp).clip(CircleShape).background(soft), contentAlignment = Alignment.Center) {
                            Icon(groupIcon(g.id), null, Modifier.size(22.dp), tint = color)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(c.groupName(g.id), style = UType.Strong)
                            Text(
                                if (next == null) stringResource(R.string.deeds_all_done)
                                else c.badgeText(next),
                                style = UType.Muted,
                            )
                        }
                        Text("$have/${tiers.size}", style = UType.Strong.copy(color = if (next == null) color else U.c.muted))
                    }
                    if (next != null) {
                        Spacer(Modifier.height(10.dp))
                        Bar(next.share(data.facts).toFloat(), color = color, track = soft)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        tiers.forEach { b ->
                            val on = b.id in done
                            Column(
                                Modifier.width(66.dp).clip(U.RSm).clickable { shown = b }.padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    Modifier.size(44.dp).clip(CircleShape).background(if (on) soft else U.c.surface2)
                                        .border(1.5.dp, if (on) U.c.gilt else U.c.line, CircleShape),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(groupIcon(g.id), null, Modifier.size(20.dp), tint = if (on) color else U.c.faint.copy(alpha = 0.5f))
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    c.badgeTitle(b.id), style = UType.Small.copy(fontSize = 10.5.sp, color = if (on) U.c.ink else U.c.faint, textAlign = TextAlign.Center),
                                    maxLines = 2, minLines = 2,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
    shown?.let { b ->
        val (color, soft) = groupColor(b.group)
        val on = b.id in done
        CelebrateDialog(
            icon = groupIcon(b.group), color = if (on) color else U.c.faint, soft = if (on) soft else U.c.surface2,
            eyebrow = stringResource(if (on) R.string.badge_unlocked else R.string.badge_locked),
            title = c.badgeTitle(b.id),
            text = c.badgeText(b) + if (!on) "\n" + stringResource(R.string.badge_progress, (b.share(data.facts) * 100).toInt()) else "",
            confetti = false,
        ) { shown = null }
    }
}
