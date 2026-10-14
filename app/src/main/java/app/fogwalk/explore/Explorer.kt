package app.fogwalk.explore

import app.fogwalk.data.Cell
import app.fogwalk.data.Day
import app.fogwalk.data.Entry
import app.fogwalk.data.Find
import app.fogwalk.data.FogDatabase
import app.fogwalk.data.SettingsStore
import app.fogwalk.data.Walk
import app.fogwalk.domain.Beacons
import app.fogwalk.domain.Cache
import app.fogwalk.domain.Caches
import app.fogwalk.domain.FogStore
import app.fogwalk.domain.Levels
import app.fogwalk.domain.Rarity
import app.fogwalk.domain.Relics
import app.fogwalk.domain.Sectors
import app.fogwalk.domain.Shop
import app.fogwalk.domain.Streaks
import app.fogwalk.domain.Wallet
import app.fogwalk.geo.Grid
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** A position from GPS (or the debug walker). */
data class Fix(
    val lat: Double,
    val lon: Double,
    val accuracy: Float,
    val time: Long,
    val bearing: Float? = null,
    /** From the debug walker: skips the vehicle check. */
    val simulated: Boolean = false,
    /** False for network (cell and Wi-Fi) positions, which are shown but never walked on. */
    val gps: Boolean = true,
    /** Ground speed from the satellites, m/s, when the phone reports it. */
    val speed: Float? = null,
)

/** What the map shows live: where the walker is and how the current walk goes. */
data class Live(
    val fix: Fix? = null,
    val walk: Walk? = null,
    /** Points of the current walk, for the trail on the map. */
    val trail: List<Pair<Double, Double>> = emptyList(),
    /** True while the last fixes looked like a car or a train: nothing is revealed then. */
    val tooFast: Boolean = false,
)

sealed interface GameEvent {
    data class Opened(val find: Find) : GameEvent
    data class NewSector(val sector: Long) : GameEvent
    data class Milestone(val sector: Long, val share: Double, val coins: Int) : GameEvent
    data object Beacon : GameEvent
    data class Near(val cache: Cache, val meters: Int) : GameEvent
}

/**
 * Turns positions into explored ground. Fixes are processed one at a time on a single worker:
 * the line from the previous fix is filled in so a walk leaves a continuous corridor, then caches,
 * the beacon and sector milestones are checked, and everything is written to the database.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class Explorer(
    private val db: FogDatabase,
    private val settings: SettingsStore,
    val fog: FogStore,
    private val scope: CoroutineScope,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {
    private val worker = Dispatchers.Default.limitedParallelism(1)
    private val ready = CompletableDeferred<Unit>()

    private val _live = MutableStateFlow(Live())
    val live: StateFlow<Live> = _live

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 32)
    val events: SharedFlow<GameEvent> = _events

    private val track = Track()
    private var walk: Walk? = null
    private var opened = HashSet<String>()
    private var openedWeek = Long.MIN_VALUE
    private val ownedRelics = HashSet<String>()
    private val grantedSectors = HashSet<String>()
    private val nearNotified = HashSet<String>()
    private var lastSaved = 0L
    private var upkeepDay = Long.MIN_VALUE
    @Volatile
    private var simulatingUntil = 0L

    fun load() {
        scope.launch(worker) {
            val dao = db.dao()
            dao.cellIds().forEach { fog.add(it) }
            fog.changed()
            walk = dao.lastWalk()
            dao.finds().forEach { if (it.relic.isNotEmpty()) ownedRelics += it.relic }
            dao.ledger().forEach { if (it.ref.startsWith("sec:")) grantedSectors += it.ref }
            val s = settings.now()
            if (s.lastLat != 0.0 || s.lastLon != 0.0) {
                _live.update { it.copy(fix = Fix(s.lastLat, s.lastLon, 9999f, 0)) }
            }
            ready.complete(Unit)
            upkeep(today())
        }
    }

    suspend fun awaitReady() = ready.await()

    fun submit(fix: Fix) {
        scope.launch(worker) {
            ready.await()
            process(fix)
        }
    }

    /** Daily chores: streak shields for missed days. */
    private suspend fun upkeep(today: Long) {
        if (upkeepDay == today) return
        upkeepDay = today
        val s = settings.now()
        val days = db.dao().days()
        val cover = Streaks.toShield(days, s.shielded, s.shields, today)
        if (cover.isNotEmpty()) settings.update { it.copy(shields = it.shields - cover.size, shielded = it.shielded + cover) }
    }

    private fun today(): Long = LocalDate.now(zone()).toEpochDay()

    private suspend fun process(f: Fix) {
        // While the debug walker runs, real GPS would drag the walker back home.
        if (!f.simulated && System.currentTimeMillis() < simulatingUntil) return
        val prev = _live.value.fix
        if (prev != null && prev.time > 0 && f.time <= prev.time && !f.simulated) return
        val step = track.add(f)
        if (step == null) {
            // Not walkable; still worth a dot on the map while satellites are silent.
            if (!track.hasGps(f.time) && f.accuracy < 200) _live.update { it.copy(fix = f) }
            return
        }
        _live.update { it.copy(fix = f.copy(lat = step.lat, lon = step.lon), tooFast = step.fast) }
        if (step.fast) return

        val local = Instant.ofEpochMilli(f.time).atZone(zone())
        val day = local.toLocalDate().toEpochDay()
        upkeep(day)
        val s = settings.now()
        val radius = REVEAL_M * if (s.lanternUntil > f.time) Shop.LANTERN_FACTOR else 1.0

        // The way from the last committed point, filled in so a walk leaves a continuous corridor.
        val points = ArrayList<Pair<Double, Double>>()
        val from = step.from
        val moved = step.meters
        if (from == null) {
            points += step.lat to step.lon
        } else {
            val steps = ceil(moved / FILL_STEP).toInt().coerceAtLeast(1)
            for (i in 1..steps) {
                val t = i.toDouble() / steps
                points += (from.first + (step.lat - from.first) * t) to (from.second + (step.lon - from.second) * t)
            }
        }

        val fresh = ArrayList<Long>()
        val newSectors = HashSet<Long>()
        val touched = HashSet<Long>()
        for ((lat, lon) in points) {
            Grid.around(lat, lon, radius) { k ->
                val sector = Grid.sector(k)
                val wasNew = fog.count(sector, Grid.SECTOR_Z) == 0
                if (fog.add(k)) {
                    fresh += k
                    touched += sector
                    if (wasNew) newSectors += sector
                }
            }
        }

        val dao = db.dao()
        if (fresh.isNotEmpty()) dao.insertCells(fresh.map { Cell(it, f.time) })

        // Caches.
        val week = Caches.week(day)
        if (week != openedWeek) {
            opened = dao.findKeys("c:$week:").toHashSet()
            openedWeek = week
        }
        var foundHere = 0
        var rareHere = 0
        val reach = Caches.OPEN_M + min(f.accuracy.toDouble(), 20.0)
        for (c in Caches.near(step.lat, step.lon, NEAR_M + moved, week)) {
            if (c.key in opened) continue
            // Anywhere along the step counts, so a chest isn't missed between two fixes.
            val m = points.minOf { (la, lo) -> Grid.distance(la, lo, c.lat, c.lon) }
            if (m <= reach) {
                val loot = Caches.open(c, ownedRelics)
                val find = Find(
                    key = c.key, at = f.time, rarity = c.rarity.ordinal,
                    relic = loot.relic?.id.orEmpty(), duplicate = loot.duplicate,
                    coins = loot.coins, xp = loot.xp, lat = c.lat, lon = c.lon,
                )
                if (dao.insertFind(find) != -1L) {
                    opened += c.key
                    loot.relic?.let { ownedRelics += it.id }
                    foundHere++
                    if (c.rarity >= Rarity.RARE) rareHere++
                    _events.tryEmit(GameEvent.Opened(find))
                    grantSets(f.time)
                }
            } else if (c.key !in nearNotified) {
                nearNotified += c.key
                _events.tryEmit(GameEvent.Near(c, m.toInt()))
            }
        }

        // The beacon: placed at the first good fix of the day, reached by coming within REACH_M.
        var beacon = false
        var b = s.beacon
        if (b.day != day) {
            val level = Levels.level(Wallet.xp(fog.size, dao.finds(), dao.ledger()))
            val (lat, lon) = Beacons.place(day, 0, step.lat, step.lon, level) { la, lo -> fog.exploredAround(la, lo, 120.0) }
            settings.update { it.copy(beacon = app.fogwalk.data.BeaconSpot(day, lat, lon)) }
            b = app.fogwalk.data.BeaconSpot(day, lat, lon)
        }
        val today = dao.day(day) ?: Day(day)
        val toBeacon = points.minOf { (la, lo) -> Grid.distance(la, lo, b.lat, b.lon) }
        if (!today.beacon && toBeacon <= Beacons.REACH_M + min(f.accuracy.toDouble(), 20.0)) {
            beacon = true
            dao.grant(Entry("q:$day:2", f.time, Beacons.COINS, Beacons.XP))
            _events.tryEmit(GameEvent.Beacon)
        }

        // Sector milestones.
        for (sector in touched) {
            val share = fog.sectorShare(sector)
            for ((at, coins, xp) in Sectors.milestones) {
                if (share < at) break
                val ref = Sectors.ref(sector, at)
                if (ref in grantedSectors) continue
                grantedSectors += ref
                if (dao.grant(Entry(ref, f.time, coins, xp)) != -1L) _events.tryEmit(GameEvent.Milestone(sector, at, coins))
            }
        }
        newSectors.forEach { _events.tryEmit(GameEvent.NewSector(it)) }

        val hour = local.hour
        if (fresh.isNotEmpty() || moved > 0 || foundHere > 0 || beacon) {
            dao.upsertDay(
                today.copy(
                    cells = today.cells + fresh.size,
                    meters = today.meters + moved,
                    caches = today.caches + foundHere,
                    rare = today.rare + rareHere,
                    beacon = today.beacon || beacon,
                    night = today.night + if (hour >= 22 || hour < 4) fresh.size else 0,
                    dawn = today.dawn + if (hour in 4..6) fresh.size else 0,
                )
            )
            var w = walk
            if (w == null || f.time - w.end > WALK_GAP_MS) {
                w = Walk(start = f.time, end = f.time, lat = step.lat, lon = step.lon)
                w = w.copy(id = dao.insertWalk(w))
                _live.update { it.copy(trail = emptyList()) }
            }
            w = w.copy(end = f.time, meters = w.meters + moved, cells = w.cells + fresh.size, caches = w.caches + foundHere)
            dao.upsertWalk(w)
            walk = w
            // The trail follows steps only; a point for every wobble in place would scribble.
            if (from != null) _live.update { l -> l.copy(walk = w, trail = (l.trail.ifEmpty { listOf(from) } + points).takeLast(4000)) }
            else _live.update { it.copy(walk = w) }
        }
        if (fresh.isNotEmpty()) fog.changed()

        if (f.time - lastSaved > 60_000) {
            lastSaved = f.time
            settings.update { it.copy(lastLat = step.lat, lastLon = step.lon) }
        }
    }

    /** A completed relic set pays out once. */
    private suspend fun grantSets(at: Long) {
        for (set in Relics.completeSets(ownedRelics)) {
            val (coins, xp) = Relics.setReward(set)
            db.dao().grant(Entry("set:$set", at, coins, xp))
        }
    }

    /** Debug builds: walk to a point at a stroll, without leaving the chair. */
    fun simulateTo(lat: Double, lon: Double) {
        scope.launch {
            val from = _live.value.fix ?: return@launch
            val d = Grid.distance(from.lat, from.lon, lat, lon)
            val steps = max(1, (d / 8).toInt())
            simulatingUntil = System.currentTimeMillis() + steps * 60L + 60_000
            for (i in 1..steps) {
                val t = i.toDouble() / steps
                submit(Fix(from.lat + (lat - from.lat) * t, from.lon + (lon - from.lon) * t, 5f, System.currentTimeMillis(), simulated = true))
                kotlinx.coroutines.delay(60)
            }
        }
    }

    /** Opens what can be bought: a fog style, a boost or a shield. False when sparks run short. */
    suspend fun buy(item: String, price: Int): Boolean = withContext(worker) {
        val dao = db.dao()
        val coins = Wallet.coins(dao.finds(), dao.ledger())
        if (coins < price) return@withContext false
        val now = System.currentTimeMillis()
        dao.grant(Entry("buy:$item:$now", now, -price, 0))
        true
    }

    companion object {
        /** Radius of ground revealed around each point of a walk. */
        const val REVEAL_M = 40.0
        /** Faster than this (m/s, about 30 km/h) is a vehicle. */
        const val MAX_SPEED = 8.5
        const val FILL_STEP = 8.0
        const val WALK_GAP_MS = 20 * 60_000L
        const val NEAR_M = 150.0
    }
}
