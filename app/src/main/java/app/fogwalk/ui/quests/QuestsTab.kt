package app.fogwalk.ui.quests

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fogwalk.R
import app.fogwalk.container
import app.fogwalk.data.Entry
import app.fogwalk.data.Units
import app.fogwalk.domain.Goal
import app.fogwalk.domain.Quest
import app.fogwalk.domain.Streaks
import app.fogwalk.ui.GameData
import app.fogwalk.ui.Go
import app.fogwalk.ui.MapFocus
import app.fogwalk.ui.Tab
import app.fogwalk.ui.components.Bar
import app.fogwalk.ui.components.SectionTitle
import app.fogwalk.ui.components.UCard
import app.fogwalk.ui.content
import app.fogwalk.ui.distance
import app.fogwalk.ui.locale
import app.fogwalk.ui.map.CoinIcon
import app.fogwalk.ui.theme.U
import app.fogwalk.ui.theme.UType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle

@Composable
fun QuestsTab(data: GameData, go: Go, focus: MutableStateFlow<MapFocus?>) {
    val imperial = data.settings.units == Units.IMPERIAL
    val now = LocalDateTime.now(data.zone)
    val left = java.time.Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay())
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 24.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        Text(stringResource(R.string.tab_quests), style = UType.Title)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.quests_reset, left.toHours().toInt(), (left.toMinutes() % 60).toInt()), style = UType.Sub)

        StreakCard(data)

        SectionTitle(stringResource(R.string.quests_today))
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            data.quests.forEach { q ->
                QuestCard(q, data, imperial, onShow = if (q.goal == Goal.BEACON && data.settings.beacon.day == data.today) {
                    {
                        focus.value = MapFocus(data.settings.beacon.lat, data.settings.beacon.lon, 16.0)
                        go.tab(Tab.MAP)
                    }
                } else null)
            }
        }

        SectionTitle(stringResource(R.string.quests_week))
        QuestCard(data.weekly, data, imperial, weekly = true)
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.caches_refill), style = UType.Muted, modifier = Modifier.padding(horizontal = 4.dp))
    }
}

@Composable
private fun StreakCard(data: GameData) {
    val st = data.streak
    val l = locale()
    Spacer(Modifier.height(18.dp))
    UCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(56.dp).clip(CircleShape).background(if (st.todayDone) U.c.flameSoft else U.c.surface2), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.LocalFireDepartment, null, Modifier.size(32.dp), tint = if (st.current > 0) U.c.flame else U.c.faint)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(pluralStringResource(R.plurals.day_streak, st.current, st.current), style = UType.Title.copy(fontSize = 22.sp))
                Text(
                    if (st.todayDone) stringResource(R.string.streak_safe) else stringResource(R.string.streak_need, Streaks.CELLS),
                    style = UType.Muted,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        // The last seven days, today on the right.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            for (i in 6 downTo 0) {
                val d = data.today - i
                val row = data.days.lastOrNull { it.day == d }
                val on = Streaks.active(row)
                val shielded = d in data.settings.shielded
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(34.dp).clip(CircleShape)
                            .background(if (on) U.c.flame else if (shielded) U.c.skySoft else U.c.surface2)
                            .then(if (i == 0) Modifier.border(2.dp, U.c.flame.copy(alpha = 0.5f), CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        when {
                            on -> Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = Color.White)
                            shielded -> Icon(Icons.Rounded.Shield, null, Modifier.size(16.dp), tint = U.c.sky)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(LocalDate.ofEpochDay(d).dayOfWeek.getDisplayName(TextStyle.NARROW_STANDALONE, l), style = UType.Small)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Shield, null, Modifier.size(16.dp), tint = U.c.sky)
            Spacer(Modifier.width(6.dp))
            Text(
                pluralStringResource(R.plurals.shields, data.settings.shields, data.settings.shields) + " · " + stringResource(R.string.best_streak, st.best),
                style = UType.Muted,
            )
        }
    }
}

@Composable
private fun QuestCard(q: Quest, data: GameData, imperial: Boolean, weekly: Boolean = false, onShow: (() -> Unit)? = null) {
    val context = LocalContext.current
    val c = content()
    val progress = data.progress(q)
    val done = progress >= q.target
    val claimed = q.ref in data.claimed
    val (icon, color, soft) = when (q.goal) {
        Goal.CELLS -> Triple(Icons.Rounded.TravelExplore, U.c.accent, U.c.accentSoft)
        Goal.METERS -> Triple(Icons.AutoMirrored.Rounded.DirectionsWalk, U.c.sage, U.c.sageSoft)
        Goal.CACHES -> Triple(Icons.Rounded.Inventory2, U.c.gold, U.c.goldSoft)
        Goal.RARE -> Triple(Icons.Rounded.AutoAwesome, U.c.violet, U.c.violetSoft)
        Goal.BEACON -> Triple(Icons.Rounded.Flare, U.c.flame, U.c.flameSoft)
    }
    UCard(onClick = onShow) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(soft), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(22.dp), tint = color)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.quest(q), style = UType.Strong.copy(fontSize = 15.sp))
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CoinIcon(Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${q.coins}", style = UType.Small.copy(color = U.c.gold))
                    Spacer(Modifier.width(10.dp))
                    Text(stringResource(R.string.plus_xp, q.xp), style = UType.Small.copy(color = U.c.accent))
                    if (weekly) {
                        Spacer(Modifier.width(10.dp))
                        Text(stringResource(R.string.days_left, 7 - Math.floorMod(data.today + 3, 7L).toInt()), style = UType.Small)
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
            when {
                claimed -> Box(Modifier.size(36.dp).clip(CircleShape).background(U.c.accentSoft), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, stringResource(R.string.done), Modifier.size(20.dp), tint = U.c.accent)
                }
                done -> Text(
                    stringResource(R.string.claim),
                    style = UType.Button.copy(color = U.c.onAccent),
                    modifier = Modifier.clip(CircleShape).background(U.c.accent).clickable {
                        val container = context.container
                        container.scope.launch { container.db.dao().grant(Entry(q.ref, System.currentTimeMillis(), q.coins, q.xp)) }
                    }.padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
        }
        if (!claimed && q.goal != Goal.BEACON) {
            Spacer(Modifier.height(12.dp))
            Bar(progress.toFloat() / q.target, color = color, track = soft)
            Spacer(Modifier.height(4.dp))
            Text(
                if (q.goal == Goal.METERS) "${distance(progress.toDouble().coerceAtMost(q.target.toDouble()), imperial)} / ${distance(q.target.toDouble(), imperial)}"
                else "${progress.coerceAtMost(q.target)} / ${q.target}",
                style = UType.Small,
            )
        } else if (!claimed && onShow != null) {
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.show_on_map), style = UType.Small.copy(color = U.c.flame))
        }
    }
}
