package app.fogwalk.domain

import app.fogwalk.data.Day
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * The Great Road: the Wayfarers' road from the Guild's gate to the lighthouse at the world's edge. Every
 * kilometre walked anywhere carries the walker's caravan along it, on new streets or on the same old ones,
 * charted with a journey under way or only counted by the pedometer. Each station is a chapter of the
 * story and pays once; past the last the road goes on, a station every [BEYOND_KM].
 */
object Road {
    /** Kilometres from the gate to each named station. */
    val stations = intArrayOf(3, 10, 21, 42, 70, 100, 150, 200, 300, 420, 600, 800, 1000, 1500, 2000, 3000, 5000)
    const val BEYOND_KM = 1000

    /** A step of this many metres, for days only the pedometer heard. */
    const val STEP_M = 0.7

    /** How far a day took the walker: the journey's metres, or its steps if they tell of more. */
    fun meters(d: Day): Double = max(d.meters, d.steps * STEP_M)

    fun km(days: List<Day>): Double = days.sumOf { meters(it) } / 1000

    /** Kilometres to station [i], named or beyond the last. */
    fun at(i: Int): Int = if (i < stations.size) stations[i] else stations.last() + (i - stations.lastIndex) * BEYOND_KM

    /** Stations reached after [km]. */
    fun reached(km: Double): Int {
        var i = 0
        while (at(i) <= km) i++
        return i
    }

    fun coins(i: Int): Int = 30 + at(i) * 2
    fun xp(i: Int): Int = 40 + at(i) * 3

    fun ref(i: Int) = "road:$i"

    /** What a station gives on top of its gold: a ward of the streak, an altar, or a relic of a rarity. */
    sealed interface Gift {
        data object Ward : Gift
        data object Altar : Gift
        data class Relic(val rarity: Rarity) : Gift
    }

    private val gifts: List<Gift> = listOf(
        Gift.Ward, Gift.Relic(Rarity.COMMON), Gift.Altar, Gift.Relic(Rarity.RARE), Gift.Ward, Gift.Relic(Rarity.EPIC),
        Gift.Altar, Gift.Relic(Rarity.RARE), Gift.Relic(Rarity.EPIC), Gift.Altar, Gift.Relic(Rarity.LEGENDARY),
        Gift.Relic(Rarity.EPIC), Gift.Relic(Rarity.LEGENDARY), Gift.Relic(Rarity.LEGENDARY), Gift.Altar,
        Gift.Relic(Rarity.LEGENDARY), Gift.Relic(Rarity.LEGENDARY),
    )

    /** The gift of station [i]; beyond the lighthouse, an epic relic each. */
    fun gift(i: Int): Gift = gifts.getOrNull(i) ?: Gift.Relic(Rarity.EPIC)

    /** Gold instead of a ward when the walker already carries all a satchel holds. */
    const val WARD_COINS = 100

    /** The key of a station's gift, as a find (a relic) or in the ledger (the rest). */
    fun giftRef(i: Int) = "st:$i"

    /** Share of the way from the last station reached to the next. */
    fun leg(km: Double): Float {
        val i = reached(km)
        val from = if (i == 0) 0 else at(i - 1)
        return ((km - from) / (at(i) - from)).toFloat().coerceIn(0f, 1f)
    }
}

/**
 * Finds along the Great Road: every kilometre walked turns something up by the wayside, a purse, a tale, now and
 * then a ward or a relic. They go into the walker's satchel, which holds [SATCHEL]; what is left by the road while
 * the satchel is full is gone, so it pays to empty it now and then. Each kilometre's find is fixed by its number.
 */
object Wayside {
    const val SATCHEL = 10
    /** Variants of each kind's story, in [app.fogwalk.content.Content.waysideFind]. */
    const val VARIANTS = 4
    /** Gold instead of a ward when there is no room for one. */
    const val WARD_COINS = 60

    enum class Kind { PURSE, TALE, STRONGBOX, WARD, RELIC }

    data class Find(val km: Int, val kind: Kind, val variant: Int, val coins: Int, val xp: Int, val rarity: Rarity? = null)

    fun at(km: Int): Find {
        val rnd = kotlin.random.Random(km * 7919L + 104_729L)
        val roll = rnd.nextDouble()
        val variant = rnd.nextInt(VARIANTS)
        return when {
            roll < 0.45 -> Find(km, Kind.PURSE, variant, 8 + rnd.nextInt(13), 5)
            roll < 0.75 -> Find(km, Kind.TALE, variant, 0, 20 + rnd.nextInt(16))
            roll < 0.90 -> Find(km, Kind.STRONGBOX, variant, 35 + rnd.nextInt(26), 10)
            roll < 0.96 -> Find(km, Kind.WARD, variant, 0, 10)
            else -> Find(km, Kind.RELIC, variant, 0, 25, if (rnd.nextDouble() < 0.75) Rarity.COMMON else Rarity.RARE)
        }
    }

    /** What waits in the satchel after [km] along the road: the last kilometres' finds not yet taken. */
    fun satchel(km: Double, claimed: Set<String>): List<Find> {
        val n = kotlin.math.floor(km).toInt()
        if (n < 1) return emptyList()
        return (maxOf(1, n - SATCHEL + 1)..n).filter { ref(it) !in claimed }.map { at(it) }
    }

    /** Metres to the next find. */
    fun toNext(km: Double): Double = (kotlin.math.floor(km) + 1 - km) * 1000

    fun ref(km: Int) = "rf:$km"
}

/**
 * Friendship with a companion (#41 grown up): it deepens with every kilometre walked with it on the map,
 * and each of its [THRESHOLDS_KM] brings a little gift of its own kind. Every day it walks a kilometre with
 * the walker, it brings back a present; the gilt token is no friend and brings nothing.
 */
object Bonds {
    val THRESHOLDS_KM = intArrayOf(3, 10, 25, 60, 150)
    val MAX = THRESHOLDS_KM.size

    /** The day's walk that earns the companion's present. */
    const val GIFT_M = 1000.0

    fun level(meters: Double): Int = THRESHOLDS_KM.count { meters >= it * 1000.0 }

    /** Share of the way to the next level; 1 at the last. */
    fun toNext(meters: Double): Float {
        val l = level(meters)
        if (l >= MAX) return 1f
        val from = if (l == 0) 0.0 else THRESHOLDS_KM[l - 1] * 1000.0
        return ((meters - from) / (THRESHOLDS_KM[l] * 1000.0 - from)).toFloat().coerceIn(0f, 1f)
    }

    fun befriends(id: String) = id != Companions.TOKEN

    /** The companion on the map and how close a friend it is. */
    fun of(s: app.fogwalk.data.Settings): Pair<String, Int> = s.companion to if (befriends(s.companion)) level(s.bond[s.companion] ?: 0.0) else 0

    private fun perk(s: app.fogwalk.data.Settings, id: String): Int = of(s).let { (pal, l) -> if (pal == id) l else 0 }

    /** Hedgehog: chests felt this much farther per level. */
    const val HEDGEHOG_M = 15.0
    /** Cat: this share more gold in chests per level. */
    const val CAT_SHARE = 0.06
    /** Fox: chests and the shrine reached this much farther per level. */
    const val FOX_M = 3.0
    /** Owl: a wider reveal by night, per level. */
    const val OWL_M = 5.0
    /** Wanderer: the torch and the stone burn this many minutes longer per level. */
    const val ELDER_MIN = 6
    /** Granny: the day's present is this share bigger per level. */
    const val GRANNY_SHARE = 0.25

    fun senseM(s: app.fogwalk.data.Settings) = Gear.senseM(s.gear) + perk(s, "hedgehog") * HEDGEHOG_M
    fun openM(s: app.fogwalk.data.Settings) = Gear.openM(s.gear) + perk(s, "fox") * FOX_M
    fun shrineM(s: app.fogwalk.data.Settings) = Gear.shrineM(s.gear) + perk(s, "fox") * FOX_M
    fun boostMs(s: app.fogwalk.data.Settings) = Gear.boostMs(s.gear) + perk(s, "elder") * ELDER_MIN * 60_000L

    /** The reveal radius at [hour]: the owl sees farther from 21:00 to 06:00. */
    fun revealM(s: app.fogwalk.data.Settings, hour: Int) =
        Gear.revealM(s.gear) + if (hour >= 21 || hour < 6) perk(s, "owl") * OWL_M else 0.0

    fun chestCoins(s: app.fogwalk.data.Settings, coins: Int) = (coins * (1 + perk(s, "cat") * CAT_SHARE)).roundToInt()

    /** What the companion brings for the day's walk: gold and experience, more from a closer friend. */
    fun gift(s: app.fogwalk.data.Settings): Pair<Int, Int> {
        val (_, l) = of(s)
        val granny = 1 + perk(s, "granny") * GRANNY_SHARE
        return (((20 + 8 * l) * granny).roundToInt()) to ((30 + 10 * l) * granny).roundToInt()
    }

    /** Gold and experience for reaching [level]. */
    fun levelCoins(level: Int) = 40 * level
    fun levelXp(level: Int) = 60 * level

    fun levelRef(id: String, level: Int) = "bond:$id:$level"
    /** The day's present: one a day, whoever brings it; the ref keeps who did, so another pal can say so. */
    fun giftRef(day: Long, pal: String) = "gift:$day:$pal"

    /**
     * Who brought [day]'s present, from the ledger's refs: "" for one taken before the refs named the pal,
     * null if none was taken yet.
     */
    fun giftFrom(day: Long, claimed: Set<String>): String? {
        if ("gift:$day" in claimed) return ""
        val p = "gift:$day:"
        return claimed.firstOrNull { it.startsWith(p) }?.removePrefix(p)
    }
}
