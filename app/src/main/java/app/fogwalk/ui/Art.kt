package app.fogwalk.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Anchor
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Brightness7
import androidx.compose.material.icons.rounded.Brush
import androidx.compose.material.icons.rounded.Castle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Cottage
import androidx.compose.material.icons.rounded.Diamond
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.EmojiNature
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Forest
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Handshake
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Landscape
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MilitaryTech
import androidx.compose.material.icons.rounded.Museum
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Park
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Sailing
import androidx.compose.material.icons.rounded.Savings
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Stars
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WbTwilight
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.Workspaces
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.ChangeHistory
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Backpack
import androidx.compose.material.icons.rounded.Hiking
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import app.fogwalk.ui.theme.U
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.res.painterResource
import app.fogwalk.R
import app.fogwalk.domain.Rarity
import app.fogwalk.domain.ShrineKind

/**
 * The painted pictures of the game, drawn as one set: relics, chests, shrines and the merchant's wares.
 * Named in code, so resource shrinking keeps them.
 */
object Art {
    val relics: Map<String, Int> = mapOf(
        "compass" to R.drawable.relic_compass, "boots" to R.drawable.relic_boots, "lantern" to R.drawable.relic_lantern,
        "staff" to R.drawable.relic_staff, "flask" to R.drawable.relic_flask, "rope" to R.drawable.relic_rope,
        "torn_map" to R.drawable.relic_torn_map, "sea_chart" to R.drawable.relic_sea_chart, "star_chart" to R.drawable.relic_star_chart,
        "city_plan" to R.drawable.relic_city_plan, "treasure_map" to R.drawable.relic_treasure_map, "nowhere_map" to R.drawable.relic_nowhere_map,
        "watch" to R.drawable.relic_watch, "spyglass" to R.drawable.relic_spyglass, "music_box" to R.drawable.relic_music_box,
        "hourglass" to R.drawable.relic_hourglass, "quill" to R.drawable.relic_quill, "snow_globe" to R.drawable.relic_snow_globe,
        "moonstone" to R.drawable.relic_moonstone, "amber" to R.drawable.relic_amber, "pearl" to R.drawable.relic_pearl,
        "fossil" to R.drawable.relic_fossil, "geode" to R.drawable.relic_geode, "meteorite" to R.drawable.relic_meteorite,
        "dragon_scale" to R.drawable.relic_dragon_scale, "phoenix_feather" to R.drawable.relic_phoenix_feather,
        "mermaid_comb" to R.drawable.relic_mermaid_comb, "griffin_claw" to R.drawable.relic_griffin_claw,
        "unicorn_hair" to R.drawable.relic_unicorn_hair, "kraken_ink" to R.drawable.relic_kraken_ink,
        "comet_tail" to R.drawable.relic_comet_tail, "star_seed" to R.drawable.relic_star_seed, "sun_coin" to R.drawable.relic_sun_coin,
        "moon_key" to R.drawable.relic_moon_key, "aurora_ribbon" to R.drawable.relic_aurora_ribbon, "void_pearl" to R.drawable.relic_void_pearl,
        "frost_bell" to R.drawable.relic_frost_bell, "icicle_wand" to R.drawable.relic_icicle_wand, "yule_candle" to R.drawable.relic_yule_candle,
        "sleigh_bell" to R.drawable.relic_sleigh_bell, "frost_rose" to R.drawable.relic_frost_rose, "winter_crown" to R.drawable.relic_winter_crown,
        "first_bud" to R.drawable.relic_first_bud, "nest_egg" to R.drawable.relic_nest_egg, "rain_flute" to R.drawable.relic_rain_flute,
        "thaw_stone" to R.drawable.relic_thaw_stone, "cuckoo_clock" to R.drawable.relic_cuckoo_clock, "dryad_seed" to R.drawable.relic_dryad_seed,
        "sunhat" to R.drawable.relic_sunhat, "firefly_jar" to R.drawable.relic_firefly_jar, "shell_horn" to R.drawable.relic_shell_horn,
        "honey_jar" to R.drawable.relic_honey_jar, "midsummer_fern" to R.drawable.relic_midsummer_fern,
        "solstice_mirror" to R.drawable.relic_solstice_mirror, "maple_leaf" to R.drawable.relic_maple_leaf, "acorn" to R.drawable.relic_acorn,
        "harvest_sickle" to R.drawable.relic_harvest_sickle, "lantern_gourd" to R.drawable.relic_lantern_gourd,
        "crane_feather" to R.drawable.relic_crane_feather, "hearth_ember" to R.drawable.relic_hearth_ember,
    )

    @DrawableRes
    fun chest(rarity: Rarity?, open: Boolean = false): Int = when (rarity) {
        null -> R.drawable.chest_unknown
        Rarity.COMMON -> if (open) R.drawable.chest_common_open else R.drawable.chest_common
        Rarity.RARE -> if (open) R.drawable.chest_rare_open else R.drawable.chest_rare
        Rarity.EPIC -> if (open) R.drawable.chest_epic_open else R.drawable.chest_epic
        Rarity.LEGENDARY -> if (open) R.drawable.chest_legendary_open else R.drawable.chest_legendary
    }

    @DrawableRes
    fun shrine(kind: ShrineKind): Int = when (kind) {
        ShrineKind.GOLD -> R.drawable.shrine_gold
        ShrineKind.GROVE -> R.drawable.shrine_grove
        ShrineKind.AZURE -> R.drawable.shrine_azure
    }

    val TORCH = R.drawable.item_torch
    val SEEING_STONE = R.drawable.item_seeing_stone
    val WARD = R.drawable.item_ward
    val SCROLL = R.drawable.item_scroll
    val ALTAR = R.drawable.item_altar
    val RUMOR = R.drawable.item_rumor
    val HOOK = R.drawable.item_hook
    val maps = listOf(R.drawable.item_map_scrap, R.drawable.item_map_seeker, R.drawable.item_map_legend)

    fun gear(id: String): Int = when (id) {
        "cloak" -> R.drawable.gear_cloak
        "senses" -> R.drawable.gear_senses
        "satchel" -> R.drawable.gear_satchel
        "boots" -> R.drawable.gear_boots
        else -> R.drawable.item_hook
    }
}

/**
 * The game's own icons painted (tabs, deeds, arms, sets, the figures of the map's panel), by the line icon each
 * stood for. Controls (back, close, share, zoom and the like) are not here and stay line icons.
 */
private val painted: Map<ImageVector, Int> by lazy {
    with(Icons.Rounded) {
        mapOf(
            AcUnit to R.drawable.ui_snowflake, Anchor to R.drawable.ui_anchor, AutoAwesome to R.drawable.ui_sparkles,
            Bedtime to R.drawable.ui_crescent, Bolt to R.drawable.ui_bolt, Brightness7 to R.drawable.ui_radiant,
            Brush to R.drawable.ui_brush, Castle to R.drawable.ui_castle, Cloud to R.drawable.ui_cloud, Cottage to R.drawable.ui_hearth,
            Diamond to R.drawable.ui_gem, Icons.AutoMirrored.Rounded.DirectionsWalk to R.drawable.ui_walker, Eco to R.drawable.ui_leaf,
            EmojiEvents to R.drawable.ui_trophy, EmojiNature to R.drawable.ui_bee, Explore to R.drawable.ui_compass,
            Forest to R.drawable.ui_forest, GridView to R.drawable.ui_tiles, Handshake to R.drawable.ui_oath, Key to R.drawable.ui_key,
            Landscape to R.drawable.ui_mountain, LightMode to R.drawable.ui_sun, LocalFireDepartment to R.drawable.ui_flame,
            LocalFlorist to R.drawable.ui_rose, Map to R.drawable.ui_atlas, Icons.AutoMirrored.Rounded.MenuBook to R.drawable.ui_chronicle,
            MilitaryTech to R.drawable.ui_medal, Museum to R.drawable.ui_cabinet, Nightlight to R.drawable.ui_moon_stars,
            Notifications to R.drawable.ui_bell, Park to R.drawable.ui_oak, Pets to R.drawable.ui_wolf, Public to R.drawable.ui_globe,
            Route to R.drawable.ui_road, Sailing to R.drawable.ui_ship, Savings to R.drawable.ui_pouch, Spa to R.drawable.ui_sprig,
            Speed to R.drawable.ui_winged_boot, Star to R.drawable.ui_star, Stars to R.drawable.ui_star_medal, Storefront to R.drawable.ui_stall,
            TaskAlt to R.drawable.ui_quest, Timer to R.drawable.ui_stopwatch, TravelExplore to R.drawable.ui_spyglass,
            WaterDrop to R.drawable.ui_drop, Waves to R.drawable.ui_waves, WbSunny to R.drawable.ui_sun, WbTwilight to R.drawable.ui_sunrise,
            Whatshot to R.drawable.ui_ember, Workspaces to R.drawable.ui_set,
            // Drawn already for the chests, the shrines and the shop.
            Inventory2 to R.drawable.chest_common, Flare to R.drawable.shrine_gold, ChangeHistory to R.drawable.item_altar,
            Lightbulb to R.drawable.item_torch, Radar to R.drawable.item_seeing_stone, Shield to R.drawable.item_ward,
            RecordVoiceOver to R.drawable.item_rumor, Backpack to R.drawable.gear_satchel, Hiking to R.drawable.gear_boots,
            Visibility to R.drawable.gear_senses,
        )
    }
}

/** The painting of a line icon, or null for a control that stays a line icon. */
fun paintedOf(icon: ImageVector): Int? = painted[icon]

/**
 * An icon of the game: its painting where it has one, else the line icon in [tint]. A painting greyed out when the
 * tint says the thing is faint (not reached, not yet done).
 */
@Composable
fun GameIcon(icon: ImageVector, contentDescription: String?, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    val art = painted[icon]
    if (art == null) {
        Icon(icon, contentDescription, modifier, tint)
        return
    }
    // Paintings sit a touch larger than line icons, which leave a margin inside their box.
    Box(modifier.defaultMinSize(24.dp, 24.dp), contentAlignment = Alignment.Center) {
        ArtImage(art, Modifier.fillMaxSize().scale(1.15f), dim = faded(tint))
    }
}

/** A painted picture, greyed out when [dim]. */
@Composable
fun ArtImage(@DrawableRes id: Int, modifier: Modifier = Modifier, dim: Boolean = false) {
    Image(
        painterResource(id), null, modifier,
        alpha = if (dim) 0.45f else 1f,
        colorFilter = if (dim) ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }) else null,
    )
}

/** The tints the screens give a thing not reached or not yet done: the faint and muted greys, or any washed-out colour. */
@Composable
private fun faded(tint: Color): Boolean {
    fun same(a: Color, b: Color) = a.copy(alpha = 1f) == b.copy(alpha = 1f)
    return tint.alpha < 0.6f || same(tint, U.c.faint) || same(tint, U.c.muted) || same(tint, Color(0xFF85745A))
}
