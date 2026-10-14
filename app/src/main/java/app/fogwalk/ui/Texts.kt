package app.fogwalk.ui

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Backpack
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Castle
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.Cookie
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.Grain
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Hiking
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalDrink
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Museum
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import app.fogwalk.R
import app.fogwalk.container
import app.fogwalk.content.Content
import app.fogwalk.data.Units
import app.fogwalk.domain.Badge
import app.fogwalk.domain.Rarity
import app.fogwalk.ui.theme.U
import kotlinx.coroutines.runBlocking
import java.util.Locale

@Composable
fun locale(): Locale = LocalConfiguration.current.locales[0]

/** Texts in the current app language; switches along with it. */
@Composable
fun content(): Content {
    val l = locale()
    return remember(l.language) { Content.of(l) }
}

private fun imperial(context: Context): Boolean =
    runCatching { runBlocking { context.container.settings.now().units == Units.IMPERIAL } }.getOrDefault(false)

/** "850 m", "3.4 km", or feet and miles. */
fun formatDistance(context: Context, meters: Double, imperial: Boolean = imperial(context)): String {
    val l = context.resources.configuration.locales[0]
    return if (imperial) {
        val miles = meters / 1609.344
        if (miles < 0.1) context.getString(R.string.unit_ft, (meters * 3.28084).toInt())
        else context.getString(R.string.unit_mi, String.format(l, if (miles < 10) "%.1f" else "%.0f", miles))
    } else {
        if (meters < 1000) context.getString(R.string.unit_m, (meters / 10).toInt() * 10)
        else context.getString(R.string.unit_km, String.format(l, if (meters < 10_000) "%.1f" else "%.0f", meters / 1000))
    }
}

@Composable
fun distance(meters: Double, imperial: Boolean): String = formatDistance(LocalContext.current, meters, imperial)

/** Explored area: square metres while small, then km² or mi². */
@Composable
fun area(m2: Double, imperial: Boolean): String {
    val context = LocalContext.current
    val l = locale()
    return if (imperial) {
        val mi2 = m2 / 2_589_988.0
        context.getString(R.string.unit_mi2, String.format(l, if (mi2 < 1) "%.2f" else "%.1f", mi2))
    } else if (m2 < 100_000) context.getString(R.string.unit_m2, String.format(l, "%,d", m2.toInt()))
    else context.getString(R.string.unit_km2, String.format(l, if (m2 < 10_000_000) "%.2f" else "%.1f", m2 / 1_000_000))
}

@Composable
fun rarityColor(r: Rarity): Pair<Color, Color> = when (r) {
    Rarity.COMMON -> U.c.sage to U.c.sageSoft
    Rarity.RARE -> U.c.sky to U.c.skySoft
    Rarity.EPIC -> U.c.violet to U.c.violetSoft
    Rarity.LEGENDARY -> U.c.gold to U.c.goldSoft
}

fun relicIcon(id: String): ImageVector = when (id) {
    "compass" -> Icons.Rounded.Explore
    "boots" -> Icons.Rounded.Hiking
    "lantern" -> Icons.Rounded.Lightbulb
    "staff" -> Icons.AutoMirrored.Rounded.DirectionsWalk
    "flask" -> Icons.Rounded.LocalDrink
    "rope" -> Icons.Rounded.AllInclusive
    "torn_map", "city_plan" -> Icons.Rounded.Map
    "sea_chart" -> Icons.Rounded.Waves
    "star_chart" -> Icons.Rounded.Star
    "treasure_map" -> Icons.Rounded.Search
    "nowhere_map" -> Icons.Rounded.Route
    "watch" -> Icons.Rounded.Timer
    "spyglass" -> Icons.Rounded.TravelExplore
    "music_box" -> Icons.Rounded.MusicNote
    "hourglass" -> Icons.Rounded.HourglassBottom
    "quill" -> Icons.Rounded.Brush
    "snow_globe" -> Icons.Rounded.AcUnit
    "moonstone" -> Icons.Rounded.Nightlight
    "amber" -> Icons.Rounded.Cookie
    "pearl" -> Icons.Rounded.Workspaces
    "fossil" -> Icons.Rounded.Spa
    "geode" -> Icons.Rounded.Diamond
    "meteorite" -> Icons.Rounded.Landscape
    "dragon_scale" -> Icons.Rounded.Whatshot
    "phoenix_feather" -> Icons.Rounded.LocalFireDepartment
    "mermaid_comb" -> Icons.Rounded.Waves
    "griffin_claw" -> Icons.Rounded.NearMe
    "unicorn_hair" -> Icons.Rounded.AutoAwesome
    "kraken_ink" -> Icons.Rounded.Grain
    "comet_tail" -> Icons.Rounded.FlashOn
    "star_seed" -> Icons.Rounded.Flare
    "sun_coin" -> Icons.Rounded.LightMode
    "moon_key" -> Icons.Rounded.Key
    "aurora_ribbon" -> Icons.Rounded.WbTwilight
    else -> Icons.Rounded.Bedtime
}

fun setIcon(set: String): ImageVector = when (set) {
    "wayfarer" -> Icons.Rounded.Backpack
    "maps" -> Icons.Rounded.Map
    "curios" -> Icons.Rounded.Museum
    "stones" -> Icons.Rounded.Diamond
    "myths" -> Icons.Rounded.Whatshot
    else -> Icons.Rounded.Star
}

fun badgeIcon(b: Badge): ImageVector = groupIcon(b.group)

fun groupIcon(group: String): ImageVector = when (group) {
    "area" -> Icons.Rounded.Public
    "km" -> Icons.Rounded.Hiking
    "cells" -> Icons.Rounded.GridView
    "cache" -> Icons.Rounded.Inventory2
    "rare" -> Icons.Rounded.Diamond
    "epic" -> Icons.Rounded.AutoAwesome
    "legend" -> Icons.Rounded.Star
    "relic" -> Icons.Rounded.Museum
    "set" -> Icons.Rounded.Workspaces
    "sector" -> Icons.Rounded.Explore
    "charted" -> Icons.Rounded.Map
    "half" -> Icons.Rounded.Castle
    "streak" -> Icons.Rounded.LocalFireDepartment
    "beacon" -> Icons.Rounded.Flare
    "quest", "weekly" -> Icons.Rounded.TaskAlt
    "days" -> Icons.Rounded.WbSunny
    "bigday" -> Icons.Rounded.EmojiEvents
    "longday" -> Icons.Rounded.Route
    "night" -> Icons.Rounded.Nightlight
    "dawn" -> Icons.Rounded.WbTwilight
    "level" -> Icons.Rounded.MilitaryTech
    "walks" -> Icons.AutoMirrored.Rounded.DirectionsWalk
    "gold" -> Icons.Rounded.Savings
    else -> Icons.Rounded.Brush
}

@Composable
fun badgeColor(b: Badge): Pair<Color, Color> = groupColor(b.group)

@Composable
fun groupColor(group: String): Pair<Color, Color> = when (group) {
    "area", "sector", "charted", "half", "cells" -> U.c.accent to U.c.accentSoft
    "km", "longday", "walks", "days" -> U.c.sage to U.c.sageSoft
    "cache", "legend", "bigday", "gold" -> U.c.gold to U.c.goldSoft
    "relic", "set", "epic", "style" -> U.c.violet to U.c.violetSoft
    "streak", "level" -> U.c.flame to U.c.flameSoft
    "beacon", "dawn" -> U.c.rose to U.c.roseSoft
    else -> U.c.sky to U.c.skySoft
}
