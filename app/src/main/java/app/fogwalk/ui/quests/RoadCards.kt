package app.fogwalk.ui.quests

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Star
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fogwalk.R
import app.fogwalk.container
import app.fogwalk.data.Entry
import app.fogwalk.data.Units
import app.fogwalk.domain.Bonds
import app.fogwalk.domain.Road
import app.fogwalk.ui.ArtImage
import app.fogwalk.ui.GameData
import app.fogwalk.ui.GameIcon
import app.fogwalk.ui.Go
import app.fogwalk.ui.Tab
import app.fogwalk.ui.components.Bar
import app.fogwalk.ui.components.IconDisc
import app.fogwalk.ui.components.SoftButton
import app.fogwalk.ui.components.UCard
import app.fogwalk.ui.content
import app.fogwalk.ui.distance
import app.fogwalk.ui.map.Sprites
import app.fogwalk.ui.theme.U
import app.fogwalk.ui.theme.UType
import app.fogwalk.ui.treasury.companionName
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** What the companion's friendship gives at [level], in words; null for the token. */
@Composable
fun palPerk(id: String, level: Int): String? = when (id) {
    "hedgehog" -> stringResource(R.string.pal_perk_hedgehog, (level * Bonds.HEDGEHOG_M).toInt())
    "cat" -> stringResource(R.string.pal_perk_cat, (level * Bonds.CAT_SHARE * 100).roundToInt())
    "fox" -> stringResource(R.string.pal_perk_fox, (level * Bonds.FOX_M).toInt())
    "owl" -> stringResource(R.string.pal_perk_owl, (level * Bonds.OWL_M).toInt())
    "elder" -> stringResource(R.string.pal_perk_elder, level * Bonds.ELDER_MIN)
    "granny" -> stringResource(R.string.pal_perk_granny, (level * Bonds.GRANNY_SHARE * 100).roundToInt())
    else -> null
}

/** The companion on the map as a friend: how close, what it gives, how it misses the road, and today's present. */
@Composable
fun PalCard(data: GameData, go: Go) {
    val context = LocalContext.current
    val imperial = data.settings.units == Units.IMPERIAL
    val pal = data.pal
    if (!Bonds.befriends(pal)) {
        UCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconDisc(Icons.Rounded.Pets, 52.dp, U.c.sage, U.c.sageSoft)
                Spacer(Modifier.width(12.dp))
                Text(stringResource(R.string.pal_token_hint), style = UType.Muted, modifier = Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            SoftButton(stringResource(R.string.pal_to_shop), { go.tab(Tab.TREASURY) }, Modifier.fillMaxWidth(), color = U.c.sage, soft = U.c.sageSoft)
        }
        return
    }
    val level = data.palLevel
    val name = companionName(pal)
    UCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(U.c.sageSoft), contentAlignment = Alignment.Center) {
                Sprites.ids["companion_$pal"]?.let { ArtImage(it, Modifier.size(56.dp)) }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(name, style = UType.Title.copy(fontSize = 20.sp))
                Text(stringResource(R.string.pal_level, level, Bonds.MAX), style = UType.Small.copy(color = U.c.sage))
                val mood = when {
                    data.lastWalkDay == null && data.palMeters < 1 -> stringResource(R.string.pal_mood_new)
                    data.giftByOther -> stringResource(R.string.pal_mood_other)
                    data.todayRoadM >= Bonds.GIFT_M -> stringResource(R.string.pal_mood_today)
                    data.lastWalkDay == null || data.today - data.lastWalkDay <= 1 -> stringResource(R.string.pal_mood_wait)
                    else -> (data.today - data.lastWalkDay - 1).toInt().let {
                        stringResource(R.string.pal_mood_miss, pluralStringResource(R.plurals.days_without, it, it))
                    }
                }
                Text(mood, style = UType.Muted)
            }
        }
        Spacer(Modifier.height(12.dp))
        Bar(Bonds.toNext(data.palMeters), color = U.c.sage, track = U.c.sageSoft)
        Spacer(Modifier.height(4.dp))
        Text(
            if (level >= Bonds.MAX) stringResource(R.string.pal_max)
            else stringResource(R.string.pal_to_next, distance(Bonds.THRESHOLDS_KM[level] * 1000.0 - data.palMeters, imperial)),
            style = UType.Small,
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            GameIcon(Icons.Rounded.Star, null, Modifier.size(16.dp), tint = if (level > 0) U.c.gold else U.c.faint)
            Spacer(Modifier.width(6.dp))
            Text(
                if (level > 0) palPerk(pal, level).orEmpty() else stringResource(R.string.pal_perk_none),
                style = UType.Small.copy(color = if (level > 0) U.c.ink else U.c.muted),
            )
        }
        Spacer(Modifier.height(12.dp))
        val (coins, xp) = Bonds.gift(data.settings)
        when {
            data.giftReady -> Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.pal_gift_ready, name), style = UType.Strong.copy(fontSize = 15.sp))
                    Text(stringResource(R.string.pal_gift_got, coins, xp), style = UType.Small.copy(color = U.c.gold))
                }
                Text(
                    stringResource(R.string.claim),
                    style = UType.Button.copy(color = U.c.onAccent),
                    modifier = Modifier.clip(CircleShape).background(U.c.accent).clickable {
                        val c = context.container
                        c.scope.launch { c.db.dao().grant(Entry(Bonds.giftRef(data.today, data.pal), System.currentTimeMillis(), coins, xp)) }
                    }.padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
            data.giftByOther -> Text(stringResource(R.string.pal_gift_other), style = UType.Muted)
            Bonds.giftFrom(data.today, data.claimed) != null -> Text(stringResource(R.string.pal_gift_taken), style = UType.Muted)
            else -> {
                Text(stringResource(R.string.pal_gift, distance(Bonds.GIFT_M, imperial), name), style = UType.Sub)
                Spacer(Modifier.height(8.dp))
                Bar((data.todayRoadM / Bonds.GIFT_M).toFloat(), color = U.c.gold, track = U.c.goldSoft)
                Spacer(Modifier.height(4.dp))
                Text("${distance(data.todayRoadM.coerceAtMost(Bonds.GIFT_M), imperial)} / ${distance(Bonds.GIFT_M, imperial)}", style = UType.Small)
            }
        }
    }
}
