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
import androidx.compose.material.icons.rounded.Handshake
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
import androidx.compose.material.icons.rounded.Agriculture
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.Anchor
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.BeachAccess
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Brightness7
import androidx.compose.material.icons.rounded.Campaign
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Egg
import androidx.compose.material.icons.rounded.EmojiNature
import androidx.compose.material.icons.rounded.EmojiObjects
import androidx.compose.material.icons.rounded.FilterVintage
import androidx.compose.material.icons.rounded.Fireplace
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Forest
import androidx.compose.material.icons.rounded.Grass
import androidx.compose.material.icons.rounded.Highlight
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Nature
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Sailing
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WorkspacePremium
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
import app.fogwalk.domain.Beacons
import app.fogwalk.domain.ShrineKind
import app.fogwalk.domain.Rarity
import app.fogwalk.ui.theme.U
import kotlinx.coroutines.runBlocking
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun locale(): Locale = LocalConfiguration.current.locales[0]

/**
 * Hours and minutes the way the phone tells them, 24-hour or not. The pattern is spelled out: under some regional
 * preferences the localized short time comes out of ICU mangled, as "12 ├minute: 2┤".
 */
@Composable
fun timeFormat(): DateTimeFormatter {
    val l = locale()
    val h24 = android.text.format.DateFormat.is24HourFormat(LocalContext.current)
    return remember(l, h24) { DateTimeFormatter.ofPattern(if (h24) "H:mm" else "h:mm a", l) }
}

/** A date in the given style, read without the locale's extensions, which can upset ICU's patterns. */
@Composable
fun dateFormat(style: FormatStyle): DateTimeFormatter {
    val l = locale()
    return remember(l, style) { DateTimeFormatter.ofLocalizedDate(style).withLocale(Locale.Builder().setLocale(l).clearExtensions().build()) }
}

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
    "frost_bell" -> Icons.Rounded.NotificationsActive
    "icicle_wand" -> Icons.Rounded.AutoFixHigh
    "yule_candle" -> Icons.Rounded.Highlight
    "sleigh_bell" -> Icons.Rounded.Notifications
    "frost_rose" -> Icons.Rounded.FilterVintage
    "winter_crown" -> Icons.Rounded.WorkspacePremium
    "first_bud" -> Icons.Rounded.Grass
    "nest_egg" -> Icons.Rounded.Egg
    "rain_flute" -> Icons.Rounded.Umbrella
    "thaw_stone" -> Icons.Rounded.WaterDrop
    "cuckoo_clock" -> Icons.Rounded.Alarm
    "dryad_seed" -> Icons.Rounded.Park
    "sunhat" -> Icons.Rounded.BeachAccess
    "firefly_jar" -> Icons.Rounded.EmojiObjects
    "shell_horn" -> Icons.Rounded.Campaign
    "honey_jar" -> Icons.Rounded.EmojiNature
    "midsummer_fern" -> Icons.Rounded.Eco
    "solstice_mirror" -> Icons.Rounded.Brightness7
    "maple_leaf" -> Icons.Rounded.Spa
    "acorn" -> Icons.Rounded.Nature
    "harvest_sickle" -> Icons.Rounded.Agriculture
    "lantern_gourd" -> Icons.Rounded.Mood
    "crane_feather" -> Icons.Rounded.Air
    "hearth_ember" -> Icons.Rounded.Fireplace
    else -> Icons.Rounded.Bedtime
}

fun setIcon(set: String): ImageVector = when (set) {
    "wayfarer" -> Icons.Rounded.Backpack
    "maps" -> Icons.Rounded.Map
    "curios" -> Icons.Rounded.Museum
    "stones" -> Icons.Rounded.Diamond
    "myths" -> Icons.Rounded.Whatshot
    "winter" -> Icons.Rounded.AcUnit
    "spring" -> Icons.Rounded.LocalFlorist
    "summer" -> Icons.Rounded.WbSunny
    "autumn" -> Icons.Rounded.Eco
    else -> Icons.Rounded.Star
}

/** The charge on a land's arms, in the order of [app.fogwalk.content.Content.armName]. */
private val armIcons = listOf(
    Icons.Rounded.Castle, Icons.Rounded.Anchor, Icons.Rounded.Park, Icons.Rounded.Sailing, Icons.Rounded.Key, Icons.Rounded.Star,
    Icons.Rounded.Bedtime, Icons.Rounded.LightMode, Icons.Rounded.LocalFlorist, Icons.Rounded.Spa, Icons.Rounded.Whatshot, Icons.Rounded.Diamond,
    Icons.Rounded.Pets, Icons.Rounded.Notifications, Icons.Rounded.WaterDrop, Icons.Rounded.Bolt, Icons.Rounded.Landscape, Icons.Rounded.Cottage,
    Icons.Rounded.EmojiNature, Icons.Rounded.Forest, Icons.Rounded.Cloud, Icons.Rounded.AcUnit, Icons.Rounded.Waves, Icons.Rounded.Lightbulb,
)

fun armIcon(arm: Int): ImageVector = armIcons[arm.coerceIn(0, armIcons.size - 1)]

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
    "season" -> Icons.Rounded.Eco
    "stars" -> Icons.Rounded.Stars
    "radiant" -> Icons.Rounded.Brightness7
    "arms" -> Icons.Rounded.Shield
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
    "oath" -> Icons.Rounded.Handshake
    "treasure" -> Icons.Rounded.Map
    "race" -> Icons.Rounded.Timer
    "gear" -> Icons.Rounded.Backpack
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
    "beacon", "dawn", "oath" -> U.c.rose to U.c.roseSoft
    "treasure", "gear" -> U.c.gold to U.c.goldSoft
    "race", "season" -> U.c.sage to U.c.sageSoft
    "stars", "radiant" -> U.c.gold to U.c.goldSoft
    "arms" -> U.c.accent to U.c.accentSoft
    else -> U.c.sky to U.c.skySoft
}

fun shrineName(context: Context, kind: ShrineKind): String = context.getString(
    when (kind) {
        ShrineKind.GOLD -> R.string.shrine_gold
        ShrineKind.GROVE -> R.string.shrine_grove
        ShrineKind.AZURE -> R.string.shrine_azure
    }
)

/** What a shrine of [kind] gives when reached. */
fun shrineGives(context: Context, kind: ShrineKind): String = when (kind) {
    ShrineKind.GOLD -> context.getString(R.string.shrine_gives_gold, Beacons.COINS + Beacons.GOLD_COINS, Beacons.XP)
    ShrineKind.GROVE -> context.getString(R.string.shrine_gives_grove, Beacons.COINS, Beacons.XP + Beacons.GROVE_XP)
    ShrineKind.AZURE -> context.getString(R.string.shrine_gives_azure, Beacons.COINS, Beacons.XP)
}

/** What reaching a shrine brought, for the toast and the notification; [gift] is the grove's. */
fun shrineReached(context: Context, kind: ShrineKind, gift: Int): String = shrineName(context, kind) + ": " + when (kind) {
    ShrineKind.GOLD -> context.getString(R.string.shrine_got_gold, Beacons.COINS + Beacons.GOLD_COINS)
    ShrineKind.GROVE -> context.getString(
        when (gift) {
            0 -> R.string.shrine_got_lantern
            1 -> R.string.shrine_got_stone
            else -> R.string.shrine_got_shield
        }
    )
    ShrineKind.AZURE -> context.getString(R.string.shrine_got_altar)
}

/** A shrine's colour on the map and in the panels. */
fun shrineColor(kind: ShrineKind): Color = Color(app.fogwalk.ui.map.WorldOverlay.shrineColor(kind))
