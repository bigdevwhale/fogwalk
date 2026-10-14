package app.fogwalk.domain

import app.fogwalk.data.Day
import app.fogwalk.data.Entry
import app.fogwalk.data.Find

/** Experience from new ground, caches, quests and milestones; each level asks a bit more. */
object Levels {
    /** Experience to go from [level] to the next. */
    fun need(level: Int): Int = 120 + 60 * (level - 1)

    /** Total experience at which [level] begins. */
    fun start(level: Int): Int = 120 * (level - 1) + 30 * (level - 1) * (level - 2)

    fun level(xp: Int): Int {
        var l = 1
        while (xp >= start(l + 1)) l++
        return l
    }

    /** Explorer ranks; the index picks the title in [app.fogwalk.content.Content.rank]. */
    private val rankFrom = intArrayOf(1, 4, 8, 12, 17, 23, 30, 40)

    fun rank(level: Int): Int = rankFrom.indexOfLast { level >= it }

    /** Sparks for reaching [level]. */
    fun reward(level: Int): Int = 40 + level * 10
}

/** A day keeps the streak alive with any of these. */
object Streaks {
    const val CELLS = 25
    const val METERS = 1000.0

    fun active(d: Day?): Boolean = d != null && (d.cells >= CELLS || d.meters >= METERS || d.beacon)

    data class Streak(val current: Int, val best: Int, val todayDone: Boolean)

    fun of(days: List<Day>, shielded: Set<Long>, today: Long): Streak {
        val on = days.filter { active(it) }.map { it.day }.toSet() + shielded
        val todayDone = today in on
        var current = 0
        var d = if (todayDone) today else today - 1
        while (d in on) {
            current++
            d--
        }
        var best = 0
        var run = 0
        var prev = Long.MIN_VALUE
        for (x in on.sorted()) {
            run = if (x == prev + 1) run + 1 else 1
            best = maxOf(best, run)
            prev = x
        }
        return Streak(current, maxOf(best, current), todayDone)
    }

    /**
     * Missed days a shield should cover: the gap right before today, if it is no longer than the
     * shields at hand and a streak was alive before it.
     */
    fun toShield(days: List<Day>, shielded: Set<Long>, shields: Int, today: Long): List<Long> {
        if (shields <= 0) return emptyList()
        val on = days.filter { active(it) }.map { it.day }.toSet() + shielded
        val gap = ArrayList<Long>()
        var d = today - 1
        while (d !in on && gap.size <= shields) {
            gap += d
            d--
            if (today - d > 10) return emptyList()
        }
        return if (gap.isNotEmpty() && gap.size <= shields && d in on) gap else emptyList()
    }
}

/** Numbers the deeds are judged by. */
data class Facts(
    val areaKm2: Double,
    val km: Double,
    val cells: Int,
    val caches: Int,
    val rare: Int,
    val epic: Int,
    val legendary: Int,
    val relics: Int,
    val sets: Int,
    val sectors: Int,
    val charted: Int,
    val half: Int,
    val bestStreak: Int,
    val beacons: Int,
    val quests: Int,
    val weeklies: Int,
    val activeDays: Int,
    val bestDayCells: Int,
    val bestDayKm: Double,
    val night: Int,
    val dawn: Int,
    val level: Int,
    val walks: Int,
    val gold: Int,
    val styles: Int,
)

/** One deed: a [tier] of a [group], reached when the group's value hits [goal]. */
data class Badge(val id: String, val group: String, val tier: Int, val goal: Double, val value: (Facts) -> Double) {
    fun unlocked(f: Facts) = value(f) >= goal
    fun share(f: Facts) = (value(f) / goal).coerceIn(0.0, 1.0)
}

/** The book of deeds: a ladder of tiers per kind of feat, about a hundred and sixty in all. */
object Badges {
    class Group(val id: String, val goals: List<Double>, val value: (Facts) -> Double)

    private fun g(id: String, vararg goals: Number, value: (Facts) -> Number) =
        Group(id, goals.map { it.toDouble() }) { value(it).toDouble() }

    val groups: List<Group> = listOf(
        g("km", 1, 3, 5, 10, 25, 50, 100, 200, 300, 500, 750, 1000, 1500, 2000, 3000, 5000) { it.km },
        g("area", 0.01, 0.05, 0.1, 0.25, 0.5, 1, 2, 3, 5, 10, 20, 30, 50, 100) { it.areaKm2 },
        g("cells", 100, 500, 1000, 2500, 5000, 10_000, 25_000, 50_000, 100_000, 250_000) { it.cells },
        g("cache", 1, 5, 10, 25, 50, 100, 150, 250, 500, 1000) { it.caches },
        g("rare", 1, 10, 50, 100) { it.rare },
        g("epic", 1, 10, 25) { it.epic },
        g("legend", 1, 5, 10) { it.legendary },
        g("relic", 3, 6, 12, 18, 24, 30, 36) { it.relics },
        g("set", 1, 2, 3, 4, 5, 6) { it.sets },
        g("sector", 1, 3, 5, 10, 20, 30, 50, 75, 100, 200) { it.sectors },
        g("charted", 1, 3, 5, 10, 25) { it.charted },
        g("half", 1, 3, 10) { it.half },
        g("streak", 3, 7, 14, 30, 60, 100, 200, 365) { it.bestStreak },
        g("beacon", 1, 5, 10, 25, 50, 100, 200) { it.beacons },
        g("quest", 1, 10, 25, 50, 100, 200, 500) { it.quests },
        g("weekly", 1, 5, 10, 25, 52) { it.weeklies },
        g("days", 7, 30, 60, 100, 200, 365) { it.activeDays },
        g("bigday", 200, 500, 1000, 2000) { it.bestDayCells },
        g("longday", 5, 10, 15, 20, 30, 42) { it.bestDayKm },
        g("night", 50, 250, 1000) { it.night },
        g("dawn", 50, 250, 1000) { it.dawn },
        g("level", 5, 10, 15, 20, 25, 30, 40, 50) { it.level },
        g("walks", 1, 10, 50, 100, 250, 500) { it.walks },
        g("gold", 500, 2000, 10_000) { it.gold },
        g("style", 2, 4, 6) { it.styles },
    )

    val all: List<Badge> = groups.flatMap { gr -> gr.goals.mapIndexed { i, goal -> Badge("${gr.id}_${i + 1}", gr.id, i, goal, gr.value) } }

    fun unlocked(f: Facts): List<Badge> = all.filter { it.unlocked(f) }
}

/** Sparks and experience from everything stored. Cells give one experience point each. */
object Wallet {
    fun coins(finds: List<Find>, ledger: List<Entry>): Int = finds.sumOf { it.coins } + ledger.sumOf { it.coins }

    fun xp(cells: Int, finds: List<Find>, ledger: List<Entry>): Int = cells + finds.sumOf { it.xp } + ledger.sumOf { it.xp }
}
