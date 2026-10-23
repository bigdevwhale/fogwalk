package app.fogwalk.explore

import app.fogwalk.data.Cell
import app.fogwalk.data.Day
import app.fogwalk.data.Entry
import app.fogwalk.data.Find
import app.fogwalk.data.FogDatabase
import app.fogwalk.data.SettingsStore
import app.fogwalk.data.Walk
import app.fogwalk.data.Waypoint
import app.fogwalk.domain.Beacons
import app.fogwalk.domain.Cache
import app.fogwalk.domain.Caches
import app.fogwalk.domain.FogStore
import app.fogwalk.domain.Gear
import app.fogwalk.domain.Hook
import app.fogwalk.domain.Oath
import app.fogwalk.domain.Oaths
import app.fogwalk.domain.Race
import app.fogwalk.domain.Races
import app.fogwalk.domain.Rumors
import app.fogwalk.domain.TreasureMap
import app.fogwalk.domain.Treasures
import app.fogwalk.domain.pickRelic
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
    data object RaceStarted : GameEvent
    /** [margin] ms the walker beat the shadow by; negative when the shadow won. */
    data class RaceDone(val won: Boolean, val margin: Long) : GameEvent
}

/** How a purchase went. */
enum class Bought { OK, NO_GOLD, NO_FIX, NOTHING, BUSY, FAR }

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
    /** The walk whose route already has its starting point. */
    private var routed = -1L
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
        // The shrine needs only a rough idea of where the day begins; a cell tower's guess indoors will do.
        if (f.accuracy <= BEACON_ACCURACY) ensureBeacon(f)
        val step = track.add(f)
        if (step == null) {
            // Not walkable; still worth a dot on the map while satellites are silent.
            if (!track.hasGps(f.time)) _live.update { it.copy(fix = f) }
            return
        }
        _live.update { it.copy(fix = f.copy(lat = step.lat, lon = step.lon), tooFast = step.fast) }
        if (step.fast) return

        val local = Instant.ofEpochMilli(f.time).atZone(zone())
        val day = local.toLocalDate().toEpochDay()
        upkeep(day)
        val s = settings.now()
        val radius = Gear.revealM(s.gear) * if (s.lanternUntil > f.time) Shop.LANTERN_FACTOR else 1.0

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
        val reach = Gear.openM(s.gear) + min(f.accuracy.toDouble(), 20.0)
        for (c in Caches.near(step.lat, step.lon, NEAR_M + moved, week)) {
            if (c.key in opened) continue
            // Anywhere along the step counts, so a chest isn't missed between two fixes.
            val m = points.minOf { (la, lo) -> Grid.distance(la, lo, c.lat, c.lon) }
            if (m <= reach) {
                if (open(c, f.time)) {
                    foundHere++
                    if (c.rarity >= Rarity.RARE) rareHere++
                }
            } else if (c.key !in nearNotified) {
                nearNotified += c.key
                _events.tryEmit(GameEvent.Near(c, m.toInt()))
            }
        }

        s.treasure?.let { t -> digFor(t, points, f) }
        s.race?.let { r -> runRace(r, points, f.time) }

        // The beacon: placed at the day's first fix (see [ensureBeacon]), reached by coming within REACH_M, farther in better boots.
        var beacon = false
        val b = settings.now().beacon
        val today = dao.day(day) ?: Day(day)
        val toBeacon = points.minOf { (la, lo) -> Grid.distance(la, lo, b.lat, b.lon) }
        if (b.day == day && !today.beacon && toBeacon <= Gear.shrineM(s.gear) + min(f.accuracy.toDouble(), 20.0)) {
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
            if (from != null) {
                val route = ArrayList<Waypoint>(2)
                if (routed != w.id) route += Waypoint(walk = w.id, at = f.time, lat = from.first, lon = from.second)
                route += Waypoint(walk = w.id, at = f.time, lat = step.lat, lon = step.lon)
                dao.insertWaypoints(route)
                routed = w.id
            }
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

    /** Steps from the pedometer: they go to the day, and to the walk while one is under way. */
    fun addSteps(n: Int, at: Long) {
        scope.launch(worker) {
            ready.await()
            val dao = db.dao()
            val day = Instant.ofEpochMilli(at).atZone(zone()).toLocalDate().toEpochDay()
            val row = dao.day(day) ?: Day(day)
            dao.upsertDay(row.copy(steps = row.steps + n))
            val w = walk?.takeIf { at - it.end <= WALK_GAP_MS } ?: return@launch
            val next = w.copy(steps = w.steps + n)
            dao.upsertWalk(next)
            walk = next
            _live.update { it.copy(walk = next) }
        }
    }

    /** Opens [c] for its loot; false when it was already open. */
    private suspend fun open(c: Cache, at: Long): Boolean {
        val loot = Caches.open(c, ownedRelics)
        val find = Find(
            key = c.key, at = at, rarity = c.rarity.ordinal,
            relic = loot.relic?.id.orEmpty(), duplicate = loot.duplicate,
            coins = loot.coins, xp = loot.xp, lat = c.lat, lon = c.lon,
        )
        if (db.dao().insertFind(find) == -1L) return false
        if (c.key.startsWith("c:$openedWeek:")) opened += c.key
        loot.relic?.let { ownedRelics += it.id }
        _events.tryEmit(GameEvent.Opened(find))
        grantSets(at)
        return true
    }

    /** Pulls in [c] with the day's grappling hook, if it is within the throw. */
    suspend fun hook(c: Cache): Bought = withContext(worker) {
        val fix = _live.value.fix?.takeIf { it.time > 0 } ?: return@withContext Bought.NO_FIX
        val dao = db.dao()
        val now = System.currentTimeMillis()
        val day = today()
        if (dao.ledger().any { it.ref.startsWith("buy:${Hook.ref(day)}:") }) return@withContext Bought.BUSY
        if (dao.findKeys(c.key).contains(c.key)) return@withContext Bought.BUSY
        val beyond = Grid.distance(fix.lat, fix.lon, c.lat, c.lon) - Gear.openM(settings.now().gear)
        if (beyond > Hook.RANGE_M) return@withContext Bought.FAR
        if (!buy(Hook.ref(day), Hook.price(beyond))) return@withContext Bought.NO_GOLD
        if (open(c, now)) {
            val row = dao.day(day) ?: Day(day)
            dao.upsertDay(row.copy(caches = row.caches + 1, rare = row.rare + if (c.rarity >= Rarity.RARE) 1 else 0))
        }
        Bought.OK
    }

    /** Places the day's shrine around [f] unless today's is already there. */
    private suspend fun ensureBeacon(f: Fix) {
        val day = Instant.ofEpochMilli(f.time).atZone(zone()).toLocalDate().toEpochDay()
        if (settings.now().beacon.day == day) return
        val dao = db.dao()
        val level = Levels.level(Wallet.xp(fog.size, dao.finds(), dao.ledger()))
        val (lat, lon) = Beacons.place(day, 0, f.lat, f.lon, level) { la, lo -> fog.exploredAround(la, lo, 120.0) }
        settings.update { it.copy(beacon = app.fogwalk.data.BeaconSpot(day, lat, lon)) }
    }

    /** Digs up the map's treasure once the walker passes over it. */
    private suspend fun digFor(t: TreasureMap, points: List<Pair<Double, Double>>, f: Fix) {
        val m = points.minOf { (la, lo) -> Grid.distance(la, lo, t.lat, t.lon) }
        if (m > Treasures.FIND_M + min(f.accuracy.toDouble(), 15.0)) return
        val tier = Treasures.tiers[t.tier]
        val (relic, dup) = pickRelic(tier.rarity, ownedRelics, t.bought)
        val find = Find(
            key = t.key, at = f.time, rarity = tier.rarity.ordinal, relic = relic.id, duplicate = dup,
            coins = tier.coins + if (dup) Relics.dupeCoins(relic) else 0, xp = tier.xp, lat = t.lat, lon = t.lon,
        )
        if (db.dao().insertFind(find) != -1L) {
            ownedRelics += relic.id
            _events.tryEmit(GameEvent.Opened(find))
            grantSets(f.time)
        }
        settings.update { it.copy(treasure = null) }
    }

    private var raceRoute: Pair<Long, List<Waypoint>>? = null

    private suspend fun route(walk: Long): List<Waypoint> {
        raceRoute?.takeIf { it.first == walk }?.let { return it.second }
        return db.dao().waypoints(walk).also { raceRoute = walk to it }
    }

    /** Starts the race at the route's first point, follows the walker along it, and settles it at the last. */
    private suspend fun runRace(r: Race, points: List<Pair<Double, Double>>, now: Long) {
        val route = route(r.walk)
        if (route.size < 2) {
            settings.update { it.copy(race = null) }
            return
        }
        val pts = route.map { it.lat to it.lon }
        if (r.started == 0L) {
            if (points.any { (la, lo) -> Grid.distance(la, lo, pts[0].first, pts[0].second) <= Races.NEAR_M }) {
                settings.update { it.copy(race = r.copy(started = now, reached = 0)) }
                _events.tryEmit(GameEvent.RaceStarted)
            }
            return
        }
        val duration = route.last().at - route.first().at
        val reached = Races.advance(pts, r.reached, points)
        val elapsed = now - r.started
        when {
            reached == pts.lastIndex -> {
                val won = elapsed < duration
                db.dao().grant(Entry(Races.ref(r.walk, r.started, won), now, 0, if (won) Races.WIN_XP else Races.LOSE_XP))
                settings.update { it.copy(race = null) }
                _events.tryEmit(GameEvent.RaceDone(won, duration - elapsed))
            }
            elapsed > Races.timeout(duration) -> {
                db.dao().grant(Entry(Races.ref(r.walk, r.started, false), now, 0, Races.LOSE_XP))
                settings.update { it.copy(race = null) }
                _events.tryEmit(GameEvent.RaceDone(false, duration - elapsed))
            }
            reached != r.reached -> settings.update { it.copy(race = r.copy(reached = reached)) }
        }
    }

    /** Challenges the shadow of [walk]: the race begins at its first point. */
    fun startRace(walk: Long) {
        scope.launch(worker) { settings.update { it.copy(race = Race(walk, 0, 0)) } }
    }

    fun cancelRace() {
        scope.launch(worker) { settings.update { it.copy(race = null) } }
    }

    private suspend fun coins(): Int {
        val dao = db.dao()
        return Wallet.coins(dao.finds(), dao.ledger())
    }

    /** Swears [o] with [stake] gold on it; the day's progress so far doesn't count. */
    suspend fun swear(o: Oath, stake: Int): Bought = withContext(worker) {
        val s = settings.now()
        if (s.oath?.week == o.week) return@withContext Bought.BUSY
        if (coins() < stake) return@withContext Bought.NO_GOLD
        val dao = db.dao()
        val base = dao.day(o.day)?.let { Oaths.metric(o.goal, it) } ?: 0.0
        dao.grant(Entry(Oaths.stakeRef(o.week), System.currentTimeMillis(), -stake, 0))
        settings.update { it.copy(oath = o.copy(stake = stake, base = base)) }
        Bought.OK
    }

    /** A kept oath returns its stake with a relic and experience, shown as a find. */
    suspend fun keepOath(): Boolean = withContext(worker) {
        val o = settings.now().oath ?: return@withContext false
        val dao = db.dao()
        if (!Oaths.kept(o, dao.days())) return@withContext false
        val (relic, dup) = pickRelic(Oaths.rarity(o.stake, o.level), ownedRelics, o.week * 31 + o.stake)
        val at = _live.value.fix
        val find = Find(
            key = "o:${o.week}", at = System.currentTimeMillis(), rarity = relic.rarity.ordinal, relic = relic.id, duplicate = dup,
            coins = o.stake + if (dup) Relics.dupeCoins(relic) else 0, xp = Oaths.xp(o.stake, o.level),
            lat = at?.lat ?: 0.0, lon = at?.lon ?: 0.0,
        )
        if (dao.insertFind(find) == -1L) return@withContext false
        ownedRelics += relic.id
        grantSets(find.at)
        true
    }

    /** Buys a treasure map of [tier], its treasure hidden where the mist is thickest around the walker. */
    suspend fun buyTreasure(tier: Int): Bought = withContext(worker) {
        if (settings.now().treasure != null) return@withContext Bought.BUSY
        val fix = _live.value.fix?.takeIf { it.time > 0 } ?: return@withContext Bought.NO_FIX
        val t = Treasures.tiers[tier]
        val now = System.currentTimeMillis()
        val map = Treasures.place(now, tier, fix.lat, fix.lon) { a, b -> fog.exploredAround(a, b, 120.0) }
        if (!buy("treasure:$tier", t.price)) return@withContext Bought.NO_GOLD
        settings.update { it.copy(treasure = map) }
        Bought.OK
    }

    fun dropTreasure() {
        scope.launch(worker) { settings.update { it.copy(treasure = null) } }
    }

    /** The tavern's rumour: free when there is nothing worth telling. */
    suspend fun hearRumor(): Bought = withContext(worker) {
        val fix = _live.value.fix?.takeIf { it.time > 0 } ?: return@withContext Bought.NO_FIX
        val week = Caches.week(today())
        if (settings.now().rumor?.week == week) return@withContext Bought.BUSY
        val opened = db.dao().findKeys("c:$week:").toSet()
        val rumor = Rumors.hear(fix.lat, fix.lon, week, opened) ?: return@withContext Bought.NOTHING
        if (!buy("rumor:$week", Rumors.PRICE)) return@withContext Bought.NO_GOLD
        settings.update { it.copy(rumor = rumor) }
        Bought.OK
    }

    /** The next step of a piece of gear. */
    suspend fun buyGear(id: String): Bought = withContext(worker) {
        val level = settings.now().gear[id] ?: 0
        val price = Gear.price(level) ?: return@withContext Bought.BUSY
        if (!buy("gear:$id:${level + 1}", price)) return@withContext Bought.NO_GOLD
        settings.update { it.copy(gear = it.gear + (id to level + 1)) }
        Bought.OK
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
        if (coins() < price) return@withContext false
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
        /** Worst accuracy, m, that may still place the day's shrine. */
        const val BEACON_ACCURACY = 1000f
        const val NEAR_M = 150.0
    }
}
