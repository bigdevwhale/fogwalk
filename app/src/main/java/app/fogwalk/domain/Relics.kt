package app.fogwalk.domain

/** A collectible found in caches. Names and lore live in [app.fogwalk.content.Content]. */
data class Relic(val id: String, val set: String, val rarity: Rarity)

/** Six sets of six; a full set earns a reward and a title. */
object Relics {
    val sets = listOf("wayfarer", "maps", "curios", "stones", "myths", "stars")

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
    )

    val all: List<Relic> = sets.flatMap { s -> ids.getValue(s).map { Relic(it, s, setRarity.getValue(s)) } }

    fun byId(id: String): Relic? = all.firstOrNull { it.id == id }

    fun inSet(set: String): List<Relic> = all.filter { it.set == set }

    fun setRarity(set: String): Rarity = setRarity.getValue(set)

    /** Sparks paid for a relic already in the collection. */
    fun dupeCoins(r: Relic): Int = 10 + r.rarity.ordinal * 25

    /** Reward for completing [set], granted once. */
    fun setReward(set: String): Pair<Int, Int> = when (setRarity(set)) {
        Rarity.COMMON -> 150 to 200
        Rarity.RARE -> 300 to 400
        Rarity.EPIC -> 600 to 800
        Rarity.LEGENDARY -> 1500 to 2000
    }

    fun completeSets(owned: Set<String>): List<String> = sets.filter { s -> inSet(s).all { it.id in owned } }
}
