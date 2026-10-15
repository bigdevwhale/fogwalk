package app.fogwalk.ui.quests

import android.widget.Toast
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fogwalk.R
import app.fogwalk.container
import app.fogwalk.domain.Oath
import app.fogwalk.domain.OathGoal
import app.fogwalk.domain.Oaths
import app.fogwalk.explore.Bought
import app.fogwalk.ui.GameData
import app.fogwalk.ui.components.Bar
import app.fogwalk.ui.components.ConfirmDialog
import app.fogwalk.ui.components.PrimaryButton
import app.fogwalk.ui.components.UCard
import app.fogwalk.ui.content
import app.fogwalk.ui.distance
import app.fogwalk.ui.map.CoinIcon
import app.fogwalk.ui.theme.U
import app.fogwalk.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The oath of the week: three vows to choose from, or the one sworn and how it goes. */
@Composable
fun OathCard(data: GameData, imperial: Boolean) {
    val o = data.oath
    when {
        o != null -> Sworn(o, data, imperial)
        data.oathOffers.isNotEmpty() -> Offers(data)
        else -> UCard { Text(stringResource(R.string.oath_settled), style = UType.Muted) }
    }
}

@Composable
private fun levelName(level: Int) = stringResource(
    when (level) {
        0 -> R.string.oath_easy
        1 -> R.string.oath_hard
        else -> R.string.oath_heroic
    }
)

@Composable
private fun Sworn(o: Oath, data: GameData, imperial: Boolean) {
    val context = LocalContext.current
    val c = content()
    val done = data.oathKept
    UCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(U.c.roseSoft), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Handshake, null, Modifier.size(22.dp), tint = U.c.rose)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(c.oath(o), style = UType.Strong.copy(fontSize = 15.sp))
                Text(stringResource(R.string.oath_staked, o.stake, levelName(o.level)), style = UType.Small)
            }
            if (done) {
                Text(
                    stringResource(R.string.oath_claim),
                    style = UType.Button.copy(color = U.c.onAccent),
                    modifier = Modifier.clip(CircleShape).background(U.c.rose).clickable {
                        context.container.scope.launch { context.container.explorer.keepOath() }
                    }.padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        val progress = data.oathProgress.coerceIn(0.0, o.target.toDouble())
        Bar((progress / o.target).toFloat(), color = U.c.rose, track = U.c.roseSoft)
        Spacer(Modifier.height(4.dp))
        Text(
            (if (o.goal == OathGoal.METERS) "${distance(progress, imperial)} / ${distance(o.target.toDouble(), imperial)}"
            else "${progress.toInt()} / ${o.target}") +
                if (o.week == data.week) " · " + stringResource(R.string.days_left, Oaths.daysLeft(data.today)) else "",
            style = UType.Small,
        )
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.oath_reward, o.stake, c.relicOf(Oaths.rarity(o.stake, o.level)), Oaths.xp(o.stake, o.level)), style = UType.Muted)
    }
}

@Composable
private fun Offers(data: GameData) {
    val context = LocalContext.current
    val c = content()
    var pick by rememberSaveable { mutableIntStateOf(1) }
    var stake by rememberSaveable { mutableIntStateOf(Oaths.STAKES[1]) }
    var confirm by rememberSaveable { mutableStateOf(false) }
    val offers = data.oathOffers
    val chosen = offers[pick.coerceIn(0, offers.lastIndex)]
    UCard {
        Text(stringResource(R.string.oath_intro), style = UType.Muted)
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            offers.forEachIndexed { i, o ->
                val on = i == pick
                Row(
                    Modifier.fillMaxWidth().clip(U.RSm)
                        .background(if (on) U.c.roseSoft else U.c.surface2)
                        .border(1.5.dp, if (on) U.c.rose else U.c.surface2, U.RSm)
                        .clickable { pick = i }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(levelName(o.level).uppercase(), style = UType.Eyebrow.copy(color = if (on) U.c.rose else U.c.faint))
                        Text(c.oath(o), style = UType.Strong.copy(fontSize = 15.sp))
                    }
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Text(stringResource(R.string.oath_stake).uppercase(), style = UType.Eyebrow)
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Oaths.STAKES.forEach { s ->
                val can = data.coins >= s
                val on = s == stake
                Row(
                    Modifier.weight(1f).clip(CircleShape)
                        .background(if (on) U.c.goldSoft else U.c.surface2)
                        .border(1.5.dp, if (on) U.c.gold else U.c.surface2, CircleShape)
                        .clickable(enabled = can) { stake = s }.padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CoinIcon(Modifier.size(11.dp), if (can) U.c.gold else U.c.faint)
                    Spacer(Modifier.width(3.dp))
                    Text("$s", style = UType.Strong.copy(fontSize = 13.sp, color = if (can) U.c.ink else U.c.faint))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(stringResource(R.string.oath_reward, stake, c.relicOf(Oaths.rarity(stake, chosen.level)), Oaths.xp(stake, chosen.level)), style = UType.Muted)
        Spacer(Modifier.height(12.dp))
        PrimaryButton(stringResource(R.string.oath_swear), { confirm = true }, Modifier.fillMaxWidth(), enabled = data.coins >= stake, color = U.c.rose, icon = Icons.Rounded.Handshake)
    }
    if (confirm) {
        val short = stringResource(R.string.not_enough)
        ConfirmDialog(
            title = stringResource(R.string.oath_swear_title),
            text = stringResource(R.string.oath_swear_text, c.oath(chosen), stake),
            confirm = stringResource(R.string.oath_swear),
            onConfirm = {
                val container = context.container
                container.scope.launch {
                    if (container.explorer.swear(chosen, stake) == Bought.NO_GOLD) {
                        withContext(Dispatchers.Main) { Toast.makeText(context, short, Toast.LENGTH_SHORT).show() }
                    }
                }
            },
            onDismiss = { confirm = false },
        )
    }
}
