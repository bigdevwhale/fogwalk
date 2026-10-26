package app.fogwalk.domain

import app.fogwalk.geo.Grid
import kotlin.math.floor
import kotlin.random.Random

enum class Rarity(val coins: Int, val xp: Int, val weight: Int, val relicChance: Double) {
    COMMON(12, 15, 62, 0.45),
    RARE(35, 40, 26, 0.75),
    EPIC(90, 100, 9, 1.0),
    LEGENDARY(220, 250, 3, 1.0),
}

/** A cache hidden in the world this week. */
data class Cache(val key: String, val lat: Double, val lon: Double, val rarity: Rarity)

/**
 * Caches are not stored anywhere: every zoom-16 tile (a few hundred metres across) hides 0–3 of them,
 * placed by a seed made of the tile and the week. Each Monday the whole map refills, including
 * streets already explored, so there is always a reason to go back out.
 */
object Caches {
    const val Z = 16
    private const val SIDE = 1 shl Z

    /** How close the walker must come to open one, before GPS slack. */
    const val OPEN_M = 30.0

    /** Caches this close show through the fog as a faint "?". */
    const val SENSE_M = 300.0

    /** Weeks start on Monday; epoch day 0 was a Thursday. */
    fun week(epochDay: Long): Long = Math.floorDiv(epochDay + 3, 7)

    fun inTile(tx: Int, ty: Int, week: Long): List<Cache> {
        val rnd = Random(seed(tx, ty, week))
        val roll = rnd.nextInt(100)
        val n = when {
            roll < 20 -> 0
            roll < 65 -> 1
            roll < 93 -> 2
            else -> 3
        }
        return List(n) { i ->
            val fx = 0.08 + rnd.nextDouble() * 0.84
            val fy = 0.08 + rnd.nextDouble() * 0.84
            val lat = Grid.lat((ty + fy) / SIDE)
            val lon = Grid.lon((tx + fx) / SIDE)
            Cache("c:$week:$tx:$ty:$i", lat, lon, rarity(rnd))
        }
    }

    /** Caches within [radius] metres of the point. */
    fun near(lat: Double, lon: Double, radius: Double, week: Long): List<Cache> {
        val tileM = Grid.cellMeters(lat) * (1 shl (Grid.Z - Z))
        val span = (radius / tileM).toInt() + 1
        val cx = floor(Grid.mx(lon) * SIDE).toInt()
        val cy = floor(Grid.my(lat) * SIDE).toInt()
        val out = ArrayList<Cache>()
        for (tx in cx - span..cx + span) for (ty in cy - span..cy + span) {
            if (tx !in 0 until SIDE || ty !in 0 until SIDE) continue
            inTile(tx, ty, week).filterTo(out) { Grid.distance(lat, lon, it.lat, it.lon) <= radius }
        }
        return out
    }

    /** Caches in the tile box, for drawing the visible map. */
    fun inBox(tx0: Int, ty0: Int, tx1: Int, ty1: Int, week: Long): List<Cache> {
        if ((tx1 - tx0 + 1).toLong() * (ty1 - ty0 + 1) > 900) return emptyList()
        val out = ArrayList<Cache>()
        for (tx in tx0..tx1) for (ty in ty0..ty1) {
            if (tx !in 0 until SIDE || ty !in 0 until SIDE) continue
            out += inTile(tx, ty, week)
        }
        return out
    }

    private fun rarity(rnd: Random): Rarity {
        var r = rnd.nextInt(Rarity.entries.sumOf { it.weight })
        for (q in Rarity.entries) {
            if (r < q.weight) return q
            r -= q.weight
        }
        return Rarity.COMMON
    }

    private fun seed(tx: Int, ty: Int, week: Long): Long =
        (tx.toLong() * 73_856_093L) xor (ty.toLong() * 19_349_663L) xor (week * 83_492_791L) xor 0x5DEECE66DL

    /** What opening [cache] in [season] gives, with [owned] relic ids already collected. */
    fun open(cache: Cache, owned: Set<String>, season: String): Loot {
        val rnd = Random(cache.key.hashCode().toLong() * 31 + 7)
        val coins = cache.rarity.coins + rnd.nextInt(cache.rarity.coins / 2 + 1)
        if (rnd.nextDouble() >= cache.rarity.relicChance) return Loot(coins, cache.rarity.xp, null, false)
        // Mostly a relic of the cache's own quality, sometimes one step lower.
        val tier = if (cache.rarity.ordinal > 0 && rnd.nextDouble() < 0.3) cache.rarity.ordinal - 1 else cache.rarity.ordinal
        val pool = Relics.pool(Rarity.entries[tier], season)
        var relic = pool[rnd.nextInt(pool.size)]
        // One second chance at something new keeps the collection moving.
        if (relic.id in owned) relic = pool[rnd.nextInt(pool.size)]
        val duplicate = relic.id in owned
        return Loot(coins + if (duplicate) Relics.dupeCoins(relic) else 0, cache.rarity.xp, relic, duplicate)
    }
}

data class Loot(val coins: Int, val xp: Int, val relic: Relic?, val duplicate: Boolean)
