package app.fogwalk.domain

import app.fogwalk.data.Day
import app.fogwalk.geo.Grid
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * A relic of [rarity] for a reward, a missing one when there is any; the same [seed] gives the same pick.
 * Duplicate is true when the whole tier is already collected.
 */
fun pickRelic(rarity: Rarity, owned: Set<String>, seed: Long): Pair<Relic, Boolean> {
    val pool = Relics.all.filter { it.rarity == rarity }
    val missing = pool.filter { it.id !in owned }
    val rnd = Random(seed)
    return if (missing.isNotEmpty()) missing[rnd.nextInt(missing.size)] to false else pool[rnd.nextInt(pool.size)] to true
}

enum class OathGoal { METERS, CELLS, CHESTS, SHRINES }

/** A vow for the rest of the week, sworn on [day] when the day's [goal] stood at [base]. */
data class Oath(val week: Long, val goal: OathGoal, val level: Int, val target: Int, val stake: Int, val day: Long, val base: Double) {
    fun encode() = listOf(week, goal.name, level, target, stake, day, base).joinToString("|")

    companion object {
        fun decode(s: String): Oath? = runCatching {
            val p = s.split("|")
            Oath(p[0].toLong(), OathGoal.valueOf(p[1]), p[2].toInt(), p[3].toInt(), p[4].toInt(), p[5].toLong(), p[6].toDouble())
        }.getOrNull()
    }
}

/**
 * The walker's oath: a goal for the rest of the week with gold staked on it. Kept, the stake comes
 * back with a relic and experience that grow with it; broken, the stake is gone. So gold buys
 * relics and a push to go out, and never breeds more gold.
 */
object Oaths {
    val STAKES = intArrayOf(100, 250, 500, 1000, 2000)
    const val MIN_STAKE = 100

    /** Easy, hard, heroic: how far above the walker's own pace. */
    private val stretch = doubleArrayOf(0.8, 1.2, 1.7)

    fun metric(goal: OathGoal, d: Day): Double = when (goal) {
        OathGoal.METERS -> d.meters
        OathGoal.CELLS -> d.cells.toDouble()
        OathGoal.CHESTS -> d.caches.toDouble()
        OathGoal.SHRINES -> if (d.beacon) 1.0 else 0.0
    }

    /** A weekly figure from the four weeks before [week], never below a beginner's floor. */
    fun pace(goal: OathGoal, days: List<Day>, week: Long): Double {
        val sum = days.filter { Caches.week(it.day) in week - 4 until week }.sumOf { metric(goal, it) }
        val floor = when (goal) {
            OathGoal.METERS -> 6000.0
            OathGoal.CELLS -> 300.0
            OathGoal.CHESTS -> 4.0
            OathGoal.SHRINES -> 2.0
        }
        return max(sum / 4, floor)
    }

    /** Days of the week still ahead, today included. */
    fun daysLeft(today: Long): Int = 7 - Math.floorMod(today + 3, 7L).toInt()

    /** Three vows on offer, easy to heroic, each on its own goal, sized to what is left of the week. */
    fun offers(today: Long, days: List<Day>): List<Oath> {
        val week = Caches.week(today)
        val left = daysLeft(today)
        val goals = OathGoal.entries.shuffled(Random(week * 977 + 13)).take(3)
        return goals.mapIndexed { level, goal ->
            val raw = pace(goal, days, week) * left / 7.0 * stretch[level]
            val target = when (goal) {
                OathGoal.METERS -> max(1000, (raw / 500).roundToInt() * 500)
                OathGoal.CELLS -> max(50, (raw / 10).roundToInt() * 10)
                OathGoal.CHESTS -> max(1, raw.roundToInt())
                OathGoal.SHRINES -> raw.roundToInt().coerceIn(1, left)
            }
            Oath(week, goal, level, target, 0, today, 0.0)
        }
    }

    /** Progress since the oath was sworn, up to the week's end. */
    fun progress(o: Oath, days: List<Day>): Double {
        val end = o.day + daysLeft(o.day) - 1
        return days.filter { it.day in o.day..end }.sumOf { metric(o.goal, it) } - o.base
    }

    fun kept(o: Oath, days: List<Day>) = progress(o, days) >= o.target

    /** The relic's quality grows with the stake; a heroic vow adds a step. */
    fun rarity(stake: Int, level: Int): Rarity {
        val byStake = when {
            stake >= 2000 -> 3
            stake >= 1000 -> 2
            stake >= 250 -> 1
            else -> 0
        }
        return Rarity.entries[min(3, byStake + if (level == 2) 1 else 0)]
    }

    fun xp(stake: Int, level: Int): Int = (stake * (0.5 + 0.25 * level)).roundToInt()

    fun ref(week: Long) = "oath:$week"
    fun stakeRef(week: Long) = "oathstake:$week"
}

/** A bought treasure map: the treasure lies at [lat], [lon], somewhere in the circle drawn on the map. */
data class TreasureMap(val bought: Long, val tier: Int, val lat: Double, val lon: Double, val cLat: Double, val cLon: Double, val radius: Double) {
    val key get() = "t:$bought"

    fun encode() = listOf(bought, tier, lat, lon, cLat, cLon, radius).joinToString("|")

    companion object {
        fun decode(s: String): TreasureMap? = runCatching {
            val p = s.split("|")
            TreasureMap(p[0].toLong(), p[1].toInt(), p[2].toDouble(), p[3].toDouble(), p[4].toDouble(), p[5].toDouble(), p[6].toDouble())
        }.getOrNull()
    }
}

/**
 * Treasure maps from the merchant: a circle in the mist a walk away, the treasure hidden inside, found
 * by "warmer, colder". The gold inside is a small part of the price; the point is the relic, of a quality
 * chests rarely give, and the walk to a part of the map one would not visit otherwise.
 */
object Treasures {
    class Tier(val price: Int, val minM: Double, val maxM: Double, val radius: Double, val rarity: Rarity, val xp: Int, val coins: Int)

    val tiers = listOf(
        Tier(150, 800.0, 1500.0, 200.0, Rarity.RARE, 120, 30),
        Tier(400, 1500.0, 2500.0, 180.0, Rarity.EPIC, 300, 60),
        Tier(1000, 2500.0, 4000.0, 150.0, Rarity.LEGENDARY, 700, 120),
    )

    const val FIND_M = 25.0

    /** Places a map's treasure where the mist is thickest; [explored] counts explored cells near a point. */
    fun place(bought: Long, tier: Int, lat: Double, lon: Double, explored: (Double, Double) -> Int): TreasureMap {
        val t = tiers[tier]
        val rnd = Random(bought)
        val base = rnd.nextDouble() * 360
        val (tLat, tLon) = (0 until 10).map { i ->
            Grid.offset(lat, lon, t.minM + rnd.nextDouble() * (t.maxM - t.minM), base + i * 36 + rnd.nextDouble() * 12)
        }.minBy { (a, b) -> explored(a, b) }
        // The circle is centred near, not on, the treasure.
        val off = sqrt(rnd.nextDouble()) * t.radius * 0.8
        val (cLat, cLon) = Grid.offset(tLat, tLon, off, rnd.nextDouble() * 360)
        return TreasureMap(bought, tier, tLat, tLon, cLat, cLon, t.radius)
    }

    /** 0 hot, 1 warm, 2 cool, 3 cold: how close the walker is, without saying where. */
    fun warmth(meters: Double): Int = when {
        meters < 45 -> 0
        meters < 90 -> 1
        meters < 160 -> 2
        else -> 3
    }
}

/** A race against walk [walk]; [started] is 0 until the walker reaches its start, [reached] the furthest point passed. */
data class Race(val walk: Long, val started: Long, val reached: Int) {
    fun encode() = listOf(walk, started, reached).joinToString("|")

    companion object {
        fun decode(s: String): Race? = runCatching {
            val p = s.split("|")
            Race(p[0].toLong(), p[1].toLong(), p[2].toInt())
        }.getOrNull()
    }
}

/**
 * Racing one's own shadow: walk a past route again while the shadow retraces it at the old pace.
 * The route must be followed point by point (a few may be skipped), and the finish counts once the last
 * point is reached.
 */
object Races {
    const val NEAR_M = 30.0
    /** How many route points ahead a single step may jump, so short cuts across loops don't count. */
    const val LOOKAHEAD = 30
    const val MIN_ROUTE_M = 300.0
    const val WIN_XP = 200
    const val LOSE_XP = 40

    /** Distance along the route at each point. */
    fun along(route: List<Pair<Double, Double>>): DoubleArray {
        val out = DoubleArray(route.size)
        for (i in 1 until route.size) {
            out[i] = out[i - 1] + Grid.distance(route[i - 1].first, route[i - 1].second, route[i].first, route[i].second)
        }
        return out
    }

    /** The furthest point reached after walking through [points], starting from [reached]. */
    fun advance(route: List<Pair<Double, Double>>, reached: Int, points: List<Pair<Double, Double>>): Int {
        var r = reached
        for ((lat, lon) in points) {
            for (j in r + 1..min(route.lastIndex, r + LOOKAHEAD)) {
                if (Grid.distance(lat, lon, route[j].first, route[j].second) <= NEAR_M) r = j
            }
        }
        return r
    }

    /** Where the shadow is [elapsed] ms after the start, as distance along the route. */
    fun shadow(times: LongArray, along: DoubleArray, elapsed: Long): Double {
        if (times.isEmpty()) return 0.0
        val t = times[0] + elapsed
        if (t >= times.last()) return along.last()
        var i = 1
        while (i < times.size && times[i] < t) i++
        if (i >= times.size) return along.last()
        val span = (times[i] - times[i - 1]).coerceAtLeast(1)
        val k = (t - times[i - 1]).toDouble() / span
        return along[i - 1] + (along[i] - along[i - 1]) * k
    }

    /** The point at [meters] along the route. */
    fun pointAt(route: List<Pair<Double, Double>>, along: DoubleArray, meters: Double): Pair<Double, Double> {
        if (route.isEmpty()) return 0.0 to 0.0
        var i = 1
        while (i < route.size && along[i] < meters) i++
        if (i >= route.size) return route.last()
        val k = ((meters - along[i - 1]) / (along[i] - along[i - 1]).coerceAtLeast(0.01)).coerceIn(0.0, 1.0)
        return (route[i - 1].first + (route[i].first - route[i - 1].first) * k) to (route[i - 1].second + (route[i].second - route[i - 1].second) * k)
    }

    /** Past this long the shadow has long won; the race ends. */
    fun timeout(duration: Long): Long = duration * 2 + 30 * 60_000L

    fun ref(walk: Long, started: Long, won: Boolean) = "race:$walk:$started:${if (won) "w" else "l"}"
}

/**
 * The walker's gear, bought for good in five steps that each cost about twice the last: a little more
 * of what makes a walk work. Gold keeps a use long after the mists are bought.
 */
object Gear {
    class Piece(val id: String, val step: Double)

    /** Cloak: wider reveal (m); senses: chests felt farther (m); satchel: longer torch and stone (min); boots: chests open from farther (m). */
    val pieces = listOf(Piece("cloak", 4.0), Piece("senses", 100.0), Piece("satchel", 20.0), Piece("boots", 4.0))
    val prices = intArrayOf(300, 600, 1200, 2500, 5000)
    const val MAX = 5

    fun price(level: Int): Int? = prices.getOrNull(level)

    fun revealM(gear: Map<String, Int>) = app.fogwalk.explore.Explorer.REVEAL_M + bonus(gear, "cloak")
    fun senseM(gear: Map<String, Int>) = Caches.SENSE_M + bonus(gear, "senses")
    fun boostMs(gear: Map<String, Int>) = Shop.BOOST_MS + (bonus(gear, "satchel") * 60_000).toLong()
    fun openM(gear: Map<String, Int>) = Caches.OPEN_M + bonus(gear, "boots")

    fun bonus(gear: Map<String, Int>, id: String): Double =
        (gear[id] ?: 0).coerceIn(0, MAX) * pieces.first { it.id == id }.step
}

/** What the tavern keeper told: the best chest of the week within reach. */
data class Rumor(val week: Long, val key: String, val lat: Double, val lon: Double, val rarity: Rarity) {
    fun encode() = listOf(week, key, lat, lon, rarity.name).joinToString("|")

    companion object {
        fun decode(s: String): Rumor? = runCatching {
            val p = s.split("|")
            Rumor(p[0].toLong(), p[1], p[2].toDouble(), p[3].toDouble(), Rarity.valueOf(p[4]))
        }.getOrNull()
    }
}

/** A rumour a week: the rarest unopened chest within [RANGE_M], nearest first among equals. */
object Rumors {
    const val PRICE = 80
    const val RANGE_M = 2000.0

    fun hear(lat: Double, lon: Double, week: Long, opened: Set<String>): Rumor? =
        Caches.near(lat, lon, RANGE_M, week).filter { it.key !in opened && it.rarity >= Rarity.RARE }
            .sortedWith(compareByDescending<Cache> { it.rarity }.thenBy { Grid.distance(lat, lon, it.lat, it.lon) })
            .firstOrNull()?.let { Rumor(week, it.key, it.lat, it.lon, it.rarity) }
}
