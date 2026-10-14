package app.fogwalk.domain

import app.fogwalk.data.Day
import app.fogwalk.geo.Grid
import kotlin.random.Random

/**
 * The beacon of the day: a point 0.6–1.7 km from where the day's walking began, put where the
 * fog is thickest. Reaching it is worth more than anything else in a day.
 */
object Beacons {
    const val REACH_M = 40.0
    const val COINS = 60
    const val XP = 120

    /** [explored] counts explored cells around a point; the least explored of ten candidates wins. */
    fun place(day: Long, salt: Int, lat: Double, lon: Double, level: Int, explored: (Double, Double) -> Int): Pair<Double, Double> {
        val rnd = Random(day * 7919 + salt * 104_729L)
        val reach = (900 + level * 25).coerceAtMost(1700).toDouble()
        val base = rnd.nextDouble() * 360
        return (0 until 10).map { i ->
            val d = 600 + rnd.nextDouble() * (reach - 600)
            Grid.offset(lat, lon, d, base + i * 36 + rnd.nextDouble() * 12)
        }.minBy { (a, b) -> explored(a, b) }
    }
}

enum class Goal { CELLS, METERS, CACHES, RARE, BEACON }

/** A quest; [ref] doubles as the ledger key, so claiming twice is impossible. */
data class Quest(val ref: String, val goal: Goal, val target: Int, val coins: Int, val xp: Int)

/**
 * Three quests a day (new ground, a second goal that changes, and the beacon) and one for the week.
 * Targets grow gently with level and are fixed for the day once generated.
 */
object Quests {
    fun daily(day: Long, level: Int): List<Quest> {
        val rnd = Random(day * 31 + 17)
        val l = level.coerceIn(1, 30)
        val cells = round10(60 + l * 8 + rnd.nextInt(40))
        val second = when (rnd.nextInt(3)) {
            0 -> Quest("q:$day:1", Goal.METERS, round100(1500 + l * 80 + rnd.nextInt(1500)), 40, 60)
            1 -> Quest("q:$day:1", Goal.CACHES, 1 + rnd.nextInt(if (l < 5) 2 else 3), 40, 60)
            else -> if (l >= 6) Quest("q:$day:1", Goal.RARE, 1, 60, 90)
            else Quest("q:$day:1", Goal.METERS, round100(2000 + rnd.nextInt(1000)), 40, 60)
        }
        return listOf(
            Quest("q:$day:0", Goal.CELLS, cells, 30, 50),
            second,
            Quest("q:$day:2", Goal.BEACON, 1, Beacons.COINS, Beacons.XP),
        )
    }

    fun weekly(week: Long, level: Int): Quest {
        val l = level.coerceIn(1, 30)
        return when (Math.floorMod(week, 3L).toInt()) {
            0 -> Quest("w:$week", Goal.CELLS, round10(600 + l * 60).coerceAtMost(2500), 250, 400)
            1 -> Quest("w:$week", Goal.METERS, round100(12_000 + l * 600).coerceAtMost(30_000), 250, 400)
            else -> Quest("w:$week", Goal.CACHES, (6 + l / 3).coerceAtMost(15), 250, 400)
        }
    }

    fun progress(q: Quest, days: List<Day>): Int {
        val cells = days.sumOf { it.cells }
        return when (q.goal) {
            Goal.CELLS -> cells
            Goal.METERS -> days.sumOf { it.meters }.toInt()
            Goal.CACHES -> days.sumOf { it.caches }
            Goal.RARE -> days.sumOf { it.rare }
            Goal.BEACON -> days.count { it.beacon }
        }
    }

    private fun round10(v: Int) = (v + 5) / 10 * 10
    private fun round100(v: Int) = (v + 50) / 100 * 100
}

/** Rewards for clearing a share of a sector: (share, sparks, xp). */
object Sectors {
    val milestones = listOf(
        Triple(0.05, 20, 50),
        Triple(0.15, 50, 120),
        Triple(0.25, 100, 250),
        Triple(0.40, 250, 500),
    )

    /** A sector counts as charted from this share on. */
    const val CHARTED = 0.25

    fun ref(sector: Long, share: Double) = "sec:$sector:${(share * 100).toInt()}"
}

/** What sparks buy. Fog styles are kept forever; the rest are used up. */
object Shop {
    data class Style(val id: String, val price: Int)

    val styles = listOf(
        Style("mist", 0),
        Style("parchment", 200),
        Style("snow", 250),
        Style("midnight", 300),
        Style("ember", 400),
        Style("aurora", 500),
    )

    const val LANTERN = 60
    const val RADAR = 40
    const val SHIELD = 120
    const val REROLL = 25
    const val MAX_SHIELDS = 3
    const val BOOST_MS = 60 * 60_000L

    /** The lantern widens the reveal circle by this much. */
    const val LANTERN_FACTOR = 1.6
}
