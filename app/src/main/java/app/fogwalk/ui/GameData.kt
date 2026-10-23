package app.fogwalk.ui

import androidx.compose.runtime.Immutable
import app.fogwalk.data.Day
import app.fogwalk.data.Entry
import app.fogwalk.data.Find
import app.fogwalk.data.Settings
import app.fogwalk.data.Walk
import app.fogwalk.domain.Badge
import app.fogwalk.domain.Badges
import app.fogwalk.domain.Caches
import app.fogwalk.domain.Facts
import app.fogwalk.domain.Levels
import app.fogwalk.domain.Oath
import app.fogwalk.domain.Oaths
import app.fogwalk.domain.Rumor
import app.fogwalk.domain.Quest
import app.fogwalk.domain.Quests
import app.fogwalk.domain.Rarity
import app.fogwalk.domain.Relics
import app.fogwalk.domain.Sectors
import app.fogwalk.domain.Streaks
import app.fogwalk.domain.Wallet
import app.fogwalk.geo.Grid
import java.time.LocalDate
import java.time.ZoneId

/** Everything stored plus what follows from it; rebuilt when any of it changes. */
@Immutable
class GameData(
    val settings: Settings,
    val days: List<Day>,
    val finds: List<Find>,
    val ledger: List<Entry>,
    val walks: List<Walk>,
    val cells: Int,
    val areaM2: Double,
    /** Explored sectors and their explored base-cell counts. */
    val sectors: Pair<LongArray, IntArray>,
    val now: Long,
    val zone: ZoneId = ZoneId.systemDefault(),
) {
    val today: Long = LocalDate.now(zone).toEpochDay()
    val week: Long = Caches.week(today)
    val todayRow: Day = days.lastOrNull { it.day == today } ?: Day(today)
    val coins: Int = Wallet.coins(finds, ledger)
    val xp: Int = Wallet.xp(cells, finds, ledger)
    val level: Int = Levels.level(xp)
    val streak: Streaks.Streak = Streaks.of(days, settings.shielded, today)
    val relics: Set<String> = finds.mapNotNullTo(HashSet()) { it.relic.ifEmpty { null } }
    val claimed: Set<String> = ledger.mapTo(HashSet()) { it.ref }
    val opened: Set<String> = finds.filter { it.key.startsWith("c:$week:") }.mapTo(HashSet()) { it.key }
    /** Chests proper: finds from treasure maps and oaths aside. */
    private val chests = finds.filter { it.key.startsWith("c:") }
    private val findKeys: Set<String> = finds.mapTo(HashSet()) { it.key }

    /** This week's oath, or last week's while a kept one waits to be claimed. */
    val oath: Oath? = settings.oath?.takeIf { o ->
        "o:${o.week}" !in findKeys && (o.week == week || (o.week == week - 1 && Oaths.kept(o, days)))
    }
    val oathProgress: Double = oath?.let { Oaths.progress(it, days) } ?: 0.0
    val oathKept: Boolean = oath != null && oathProgress >= oath.target
    /** Whether this week's oath is already settled (claimed), so no new one may be sworn. */
    val oathDone: Boolean = "o:$week" in findKeys || settings.oath?.week == week
    val oathOffers: List<Oath> = if (oathDone) emptyList() else Oaths.offers(today, days)
    /** This week's oath once kept and claimed, with the find it brought. */
    val keptOath: Pair<Oath, Find>? = settings.oath?.takeIf { it.week == week }?.let { o -> finds.firstOrNull { it.key == "o:${o.week}" }?.let { o to it } }
    val rumor: Rumor? = settings.rumor?.takeIf { it.week == week && it.key !in opened }

    /** Experience before [day] began, so quest targets don't shift as the day goes on. */
    private fun levelBefore(day: Long): Int {
        val start = LocalDate.ofEpochDay(day).atStartOfDay(zone).toInstant().toEpochMilli()
        val later = days.filter { it.day >= day }.sumOf { it.cells } +
            finds.filter { it.at >= start }.sumOf { it.xp } + ledger.filter { it.at >= start }.sumOf { it.xp }
        return Levels.level(xp - later)
    }

    val quests: List<Quest> = Quests.daily(today, levelBefore(today))
    val weekly: Quest = Quests.weekly(week, levelBefore(today - Math.floorMod(today + 3, 7L)))

    fun progress(q: Quest): Int = if (q.ref.startsWith("w:")) {
        Quests.progress(q, days.filter { Caches.week(it.day) == week })
    } else Quests.progress(q, listOf(todayRow))

    fun done(q: Quest) = progress(q) >= q.target
    fun claimable(q: Quest) = done(q) && q.ref !in claimed
    val toClaim: Int = (quests + weekly).count { claimable(it) }

    fun share(sector: Long): Double {
        val i = sectors.first.indexOf(sector)
        return if (i < 0) 0.0 else sectors.second[i].toDouble() / Grid.SECTOR_CELLS
    }

    val facts = Facts(
        areaKm2 = areaM2 / 1_000_000,
        km = days.sumOf { it.meters } / 1000,
        cells = cells,
        caches = chests.size,
        rare = chests.count { it.rarity >= Rarity.RARE.ordinal },
        epic = chests.count { it.rarity >= Rarity.EPIC.ordinal },
        legendary = chests.count { it.rarity == Rarity.LEGENDARY.ordinal },
        relics = relics.size,
        sets = Relics.completeSets(relics).size,
        sectors = sectors.first.size,
        charted = sectors.second.count { it.toDouble() / Grid.SECTOR_CELLS >= Sectors.CHARTED },
        half = sectors.second.count { it.toDouble() / Grid.SECTOR_CELLS >= 0.5 },
        bestStreak = streak.best,
        beacons = days.count { it.beacon },
        quests = ledger.count { it.ref.startsWith("q:") || it.ref.startsWith("w:") },
        weeklies = ledger.count { it.ref.startsWith("w:") },
        activeDays = days.count { Streaks.active(it) },
        bestDayCells = days.maxOfOrNull { it.cells } ?: 0,
        bestDayKm = (days.maxOfOrNull { it.meters } ?: 0.0) / 1000,
        night = days.sumOf { it.night },
        dawn = days.sumOf { it.dawn },
        level = level,
        walks = walks.count { it.meters >= 300 },
        gold = finds.sumOf { it.coins } + ledger.sumOf { it.coins.coerceAtLeast(0) },
        styles = settings.owned.size,
        oaths = finds.count { it.key.startsWith("o:") },
        treasures = finds.count { it.key.startsWith("t:") },
        races = ledger.count { it.ref.startsWith("race:") && it.ref.endsWith(":w") },
        gear = settings.gear.values.sum(),
    )
    val badges: List<Badge> = Badges.unlocked(facts)
}
