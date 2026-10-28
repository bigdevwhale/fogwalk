package app.fogwalk.domain

import app.fogwalk.data.Day
import app.fogwalk.geo.Grid
import kotlin.random.Random

/** Gold on top of the quest's; the grove gives a boost and experience; the azure shrine an altar. */
enum class ShrineKind { GOLD, GROVE, AZURE }

data class Shrine(val kind: ShrineKind, val lat: Double, val lon: Double)

/**
 * The day's shrines, one of each kind. [chosen] is the one the walker set out for, and after one
 * is reached, the one that was; until then any of them wakes.
 */
data class ShrineDay(val day: Long = 0, val shrines: List<Shrine> = emptyList(), val chosen: ShrineKind? = null) {
    fun of(kind: ShrineKind?): Shrine? = shrines.firstOrNull { it.kind == kind }

    /** The chosen shrine, or else the nearest to the walker. */
    fun target(lat: Double?, lon: Double?): Shrine? =
        of(chosen) ?: if (lat == null || lon == null) shrines.firstOrNull() else shrines.minByOrNull { Grid.distance(lat, lon, it.lat, it.lon) }

    fun encode() = "$day|${chosen?.name.orEmpty()}|" + shrines.joinToString(";") { "${it.kind.name},${it.lat},${it.lon}" }

    companion object {
        fun decode(s: String): ShrineDay? = runCatching {
            val p = s.split("|")
            ShrineDay(
                p[0].toLong(),
                p[2].split(";").filter { it.isNotEmpty() }.map { e -> e.split(",").let { Shrine(ShrineKind.valueOf(it[0]), it[1].toDouble(), it[2].toDouble()) } },
                p[1].takeIf { it.isNotEmpty() }?.let { ShrineKind.valueOf(it) },
            )
        }.getOrNull()
    }
}

/**
 * The shrines of the day: three points 0.6–1.7 km from where the day's walking began, each in its own
 * third of the compass and put where that third's fog is thickest, so one at least can be reached when
 * the others stand behind fences. Only one wakes a day. Reaching it is worth more than anything else in a day.
 */
object Beacons {
    const val REACH_M = 40.0
    const val COINS = 60
    const val XP = 120
    /** What each kind adds to the quest's reward. */
    const val GOLD_COINS = 60
    const val GROVE_XP = 80

    /** [explored] counts explored cells around a point; in each third the least explored of six candidates wins. */
    fun place(day: Long, salt: Int, lat: Double, lon: Double, level: Int, explored: (Double, Double) -> Int): List<Shrine> {
        val rnd = Random(day * 7919 + salt * 104_729L)
        val reach = (900 + level * 25).coerceAtMost(1700).toDouble()
        val base = rnd.nextDouble() * 360
        return ShrineKind.entries.shuffled(rnd).mapIndexed { third, kind ->
            val (a, b) = (0 until 6).map { i ->
                val d = 600 + rnd.nextDouble() * (reach - 600)
                Grid.offset(lat, lon, d, base + third * 120 + i * 20 + rnd.nextDouble() * 10)
            }.minBy { (a, b) -> explored(a, b) }
            Shrine(kind, a, b)
        }
    }

    /** The grove's gift on [day]: 0 a lantern, 1 a seeing stone, 2 a streak shield while there is room for one. */
    fun gift(day: Long, shields: Int): Int = Random(day * 131 + 7).nextInt(if (shields < Shop.MAX_SHIELDS) 3 else 2)

    fun ref(day: Long, kind: ShrineKind) = "shrine:$day:${kind.name.lowercase()}"
}

/**
 * Altars from the azure shrine, set one by one around ground that cannot be walked: a fenced yard, a
 * factory, a lake. Once three or more stand, the ring is closed and the mist inside it lifts; the altars
 * are spent. Each side may be [MAX_SIDE_M] at most, so a ring takes a walk around the place, not a map.
 */
object Altars {
    const val MIN = 3
    const val MAX_SIDE_M = 400.0

    fun encode(ring: List<Pair<Double, Double>>) = ring.joinToString(";") { "${it.first},${it.second}" }

    fun decode(s: String): List<Pair<Double, Double>> =
        s.split(";").filter { it.isNotEmpty() }.mapNotNull { e -> e.split(",").takeIf { it.size == 2 }?.let { (a, b) -> a.toDouble() to b.toDouble() } }

    /** The side that would close the ring, from the last altar back to the first, in metres. */
    fun closing(ring: List<Pair<Double, Double>>): Double =
        if (ring.size < 2) 0.0 else Grid.distance(ring.last().first, ring.last().second, ring.first().first, ring.first().second)

    fun canClose(ring: List<Pair<Double, Double>>) = ring.size >= MIN && closing(ring) <= MAX_SIDE_M

    /** Base cells whose centres lie inside the ring. */
    fun inside(ring: List<Pair<Double, Double>>, out: (Long) -> Unit) {
        if (ring.size < MIN) return
        val xs = DoubleArray(ring.size) { Grid.mx(ring[it].second) * Grid.N }
        val ys = DoubleArray(ring.size) { Grid.my(ring[it].first) * Grid.N }
        for (x in xs.min().toInt()..xs.max().toInt()) for (y in ys.min().toInt()..ys.max().toInt()) {
            val px = x + 0.5
            val py = y + 0.5
            var inside = false
            var j = ring.lastIndex
            for (i in ring.indices) {
                if ((ys[i] > py) != (ys[j] > py) && px < xs[i] + (py - ys[i]) / (ys[j] - ys[i]) * (xs[j] - xs[i])) inside = !inside
                j = i
            }
            if (inside) out(Grid.key(x, y))
        }
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

/**
 * The arms of the lands: every land bears one of [COUNT] charges, the same for everyone, and charting it
 * raises its banner. The first banner of each charge pays; a full roll of arms takes lands far and wide.
 */
object Arms {
    const val COUNT = 24
    const val COINS = 100
    const val XP = 200

    fun of(sector: Long): Int {
        var h = sector * 0x2545F4914F6CDD1DL
        h = h xor (h ushr 31)
        return ((h and Long.MAX_VALUE) % COUNT).toInt()
    }

    /** Charges whose banner flies over at least one charted land. */
    fun raised(sectors: LongArray, cells: IntArray): Set<Int> =
        sectors.indices.filter { cells[it].toDouble() / Grid.SECTOR_CELLS >= Sectors.CHARTED }.mapTo(HashSet()) { of(sectors[it]) }

    fun ref(arm: Int) = "arms:$arm"
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
