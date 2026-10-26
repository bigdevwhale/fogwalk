package app.fogwalk.domain

import java.time.LocalDate

/** A collectible found in caches. Names and lore live in [app.fogwalk.content.Content]. */
data class Relic(val id: String, val set: String, val rarity: Rarity)

/**
 * Six sets of six that chests give all year, and four seasonal sets that turn up only in their own
 * season. A full set earns a reward and a title; copies found again raise a relic's stars, and a set
 * whose every relic reaches a star earns more.
 */
object Relics {
    val core = listOf("wayfarer", "maps", "curios", "stones", "myths", "stars")
    val seasonal = listOf("winter", "spring", "summer", "autumn")
    val sets = core + seasonal

    private val setRarity = mapOf(
        "wayfarer" to Rarity.COMMON,
        "maps" to Rarity.COMMON,
        "curios" to Rarity.RARE,
        "stones" to Rarity.RARE,
        "myths" to Rarity.EPIC,
        "stars" to Rarity.LEGENDARY,
    )

    private val ids = mapOf(
        "wayfarer" to listOf("compass", "boots", "lantern", "staff", "flask", "rope"),
        "maps" to listOf("torn_map", "sea_chart", "star_chart", "city_plan", "treasure_map", "nowhere_map"),
        "curios" to listOf("watch", "spyglass", "music_box", "hourglass", "quill", "snow_globe"),
        "stones" to listOf("moonstone", "amber", "pearl", "fossil", "geode", "meteorite"),
        "myths" to listOf("dragon_scale", "phoenix_feather", "mermaid_comb", "griffin_claw", "unicorn_hair", "kraken_ink"),
        "stars" to listOf("comet_tail", "star_seed", "sun_coin", "moon_key", "aurora_ribbon", "void_pearl"),
        "winter" to listOf("frost_bell", "icicle_wand", "yule_candle", "sleigh_bell", "frost_rose", "winter_crown"),
        "spring" to listOf("first_bud", "nest_egg", "rain_flute", "thaw_stone", "cuckoo_clock", "dryad_seed"),
        "summer" to listOf("sunhat", "firefly_jar", "shell_horn", "honey_jar", "midsummer_fern", "solstice_mirror"),
        "autumn" to listOf("maple_leaf", "acorn", "harvest_sickle", "lantern_gourd", "crane_feather", "hearth_ember"),
    )

    /** A seasonal set holds two common relics, two rare, one epic and one legendary, in that order. */
    private val seasonalRarity = listOf(Rarity.COMMON, Rarity.COMMON, Rarity.RARE, Rarity.RARE, Rarity.EPIC, Rarity.LEGENDARY)

    val all: List<Relic> = sets.flatMap { s ->
        ids.getValue(s).mapIndexed { i, id -> Relic(id, s, setRarity[s] ?: seasonalRarity[i]) }
    }

    fun byId(id: String): Relic? = all.firstOrNull { it.id == id }

    fun inSet(set: String): List<Relic> = all.filter { it.set == set }

    fun isSeasonal(set: String) = set in seasonal

    /** The colour a set is shown in; seasonal sets mix qualities and count as epic. */
    fun setRarity(set: String): Rarity = setRarity[set] ?: Rarity.EPIC

    /** Relics of [rarity] that chests and rewards can give in [season]. */
    fun pool(rarity: Rarity, season: String): List<Relic> =
        all.filter { it.rarity == rarity && (!isSeasonal(it.set) || it.set == season) }

    /** Sparks paid for a relic already in the collection. */
    fun dupeCoins(r: Relic): Int = 10 + r.rarity.ordinal * 25

    /** Reward for completing [set], granted once. */
    fun setReward(set: String): Pair<Int, Int> = if (isSeasonal(set)) 800 to 1000 else when (setRarity(set)) {
        Rarity.COMMON -> 150 to 200
        Rarity.RARE -> 300 to 400
        Rarity.EPIC -> 600 to 800
        Rarity.LEGENDARY -> 1500 to 2000
    }

    fun completeSets(owned: Set<String>): List<String> = sets.filter { s -> inSet(s).all { it.id in owned } }

    /** Copies needed for each star: the second copy gives the first star, the thirty-second the fifth. */
    val STARS = intArrayOf(2, 4, 8, 16, 32)
    const val MAX_STARS = 5

    fun stars(copies: Int): Int = STARS.count { copies >= it }

    /** Copies still needed for the next star, or 0 at the top. */
    fun toNextStar(copies: Int): Int = STARS.firstOrNull { it > copies }?.minus(copies) ?: 0

    /** How many times each relic has been found. */
    fun copies(found: Iterable<String>): Map<String, Int> = found.filter { it.isNotEmpty() }.groupingBy { it }.eachCount()

    /** The stars every relic of [set] has reached: the set's own star level. */
    fun setStars(set: String, copies: Map<String, Int>): Int = inSet(set).minOf { stars(copies[it.id] ?: 0) }

    /** Reward for raising every relic of [set] to [star] stars, granted once per star. */
    fun starReward(set: String, star: Int): Pair<Int, Int> = setReward(set).let { (c, x) -> c * star / 2 to x * star / 2 }

    fun starRef(set: String, star: Int) = "star:$set:$star"
}

/** The four seasons by month, turned around south of the equator. */
object Seasons {
    fun of(epochDay: Long, lat: Double = 0.0): String {
        val month = LocalDate.ofEpochDay(epochDay).monthValue
        val north = (month % 12) / 3
        val i = if (lat < 0) (north + 2) % 4 else north
        return listOf("winter", "spring", "summer", "autumn")[i]
    }

    /** The first day of the season after the one [epochDay] falls in. */
    fun nextStart(epochDay: Long): Long {
        val d = LocalDate.ofEpochDay(epochDay)
        val start = LocalDate.of(d.year, (d.monthValue % 12) / 3 * 3 + 3, 1)
        return (if (start.isAfter(d)) start else start.plusYears(1)).toEpochDay()
    }

    /** When [set] next begins, counting from [epochDay]; [epochDay] itself while it is on. */
    fun startOf(set: String, epochDay: Long, lat: Double = 0.0): Long {
        var d = epochDay
        repeat(5) {
            if (of(d, lat) == set) return d
            d = nextStart(d)
        }
        return d
    }
}
