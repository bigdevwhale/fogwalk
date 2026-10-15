package app.fogwalk.data

import android.content.Context
import app.fogwalk.domain.Caches
import app.fogwalk.domain.Levels
import app.fogwalk.domain.Quests
import app.fogwalk.domain.Wallet
import app.fogwalk.explore.Explorer
import app.fogwalk.geo.Grid
import org.json.JSONArray
import java.time.LocalDate
import java.time.ZoneId
import kotlin.random.Random

/**
 * Screenshot data for `-Pdemo` builds: three weeks of walks along real streets (assets/demo_walks.json,
 * made from OpenStreetMap by tools/demo_walks.py), with caches, relics and quests to match.
 */
object DemoSeed {
    /** Where the demo walker stands. */
    var here: Pair<Double, Double> = 0.0 to 0.0
        private set

    suspend fun seedIfEmpty(context: Context, db: FogDatabase, settings: SettingsStore) {
        val json = runCatching { context.assets.open("demo_walks.json").use { it.readBytes().decodeToString() } }.getOrNull() ?: return
        val o = org.json.JSONObject(json)
        here = o.getDouble("lat") to o.getDouble("lon")
        val dao = db.dao()
        if (dao.cellCount() > 0) return
        val walks = o.getJSONArray("walks")
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val rnd = Random(42)
        val seen = HashSet<Long>()
        val days = HashMap<Long, Day>()
        // Spread the walks over the last three weeks, a couple of rest days in between, and today last.
        val dayOf = IntArray(walks.length()) { i -> walks.length() - 1 - i }.map { it.toLong() * 22 / walks.length() }
        for (i in 0 until walks.length()) {
            val line = walks.getJSONArray(i)
            val day = today.minusDays(dayOf[i]).toEpochDay()
            var start = LocalDate.ofEpochDay(day).atTime(8 + rnd.nextInt(11), rnd.nextInt(60)).atZone(zone).toInstant().toEpochMilli()
            // Today's walk is over by now, whatever the hour.
            if (day == today.toEpochDay()) start = minOf(start, System.currentTimeMillis() - 2 * 3_600_000L)
            val fresh = ArrayList<Cell>()
            var meters = 0.0
            var prev: Pair<Double, Double>? = null
            for (j in 0 until line.length()) {
                val p = line.getJSONArray(j)
                val pt = p.getDouble(0) to p.getDouble(1)
                val pts = ArrayList<Pair<Double, Double>>()
                prev?.let { a ->
                    val d = Grid.distance(a.first, a.second, pt.first, pt.second)
                    meters += d
                    val steps = (d / Explorer.FILL_STEP).toInt().coerceAtLeast(1)
                    for (k in 1..steps) pts += (a.first + (pt.first - a.first) * k / steps) to (a.second + (pt.second - a.second) * k / steps)
                } ?: pts.add(pt)
                pts.forEach { (lat, lon) -> Grid.around(lat, lon, Explorer.REVEAL_M) { if (seen.add(it)) fresh += Cell(it, start) } }
                prev = pt
            }
            dao.insertCells(fresh)
            val minutes = (meters / 75).toLong()
            val id = dao.insertWalk(Walk(start = start, end = start + minutes * 60_000, meters = meters, cells = fresh.size, lat = line.getJSONArray(0).getDouble(0), lon = line.getJSONArray(0).getDouble(1)))
            dao.insertWaypoints(List(line.length()) { j ->
                val p = line.getJSONArray(j)
                Waypoint(walk = id, at = start + minutes * 60_000 * j / line.length(), lat = p.getDouble(0), lon = p.getDouble(1))
            })
            val d = days[day] ?: Day(day)
            days[day] = d.copy(cells = d.cells + fresh.size, meters = d.meters + meters, beacon = d.beacon || rnd.nextInt(3) == 0)
        }

        // Caches opened along the way, with a good spread of relics.
        val relicPlan = listOf(
            "compass", "boots", "lantern", "staff", "flask", "rope", "torn_map", "sea_chart", "city_plan", "treasure_map",
            "watch", "spyglass", "hourglass", "moonstone", "amber", "pearl", "geode", "dragon_scale", "phoenix_feather", "comet_tail",
        )
        val dayKeys = days.keys.sorted()
        relicPlan.forEachIndexed { i, relic ->
            val r = app.fogwalk.domain.Relics.byId(relic)!!
            val day = dayKeys[i % dayKeys.size]
            val at = LocalDate.ofEpochDay(day).atTime(12, i).atZone(zone).toInstant().toEpochMilli()
            dao.insertFind(
                Find("c:${Caches.week(day)}:demo:$i", at, r.rarity.ordinal, relic, false, r.rarity.coins + 10, r.rarity.xp, here.first, here.second, seen = true)
            )
            days[day] = days.getValue(day).let { it.copy(caches = it.caches + 1, rare = it.rare + if (r.rarity.ordinal > 0) 1 else 0) }
        }
        // Today's walk mostly retraced old streets; give the screenshot a livelier day.
        days[today.toEpochDay()]?.let { days[it.day] = it.copy(cells = maxOf(it.cells, 164)) }
        days.values.forEach { dao.upsertDay(it) }
        // Claimed quests on the days out, and the beacons reached.
        for (day in dayKeys) {
            val at = LocalDate.ofEpochDay(day).atTime(20, 0).atZone(zone).toInstant().toEpochMilli()
            Quests.daily(day, 5).take(if (day == today.toEpochDay()) 1 else 2).forEach { dao.grant(Entry(it.ref, at, it.coins, it.xp)) }
            if (days.getValue(day).beacon) dao.grant(Entry("q:$day:2", at, 60, 120))
        }
        val xp = Wallet.xp(seen.size, dao.finds(), dao.ledger())
        val level = Levels.level(xp)
        for (l in 2..level) dao.grant(Entry("lvl:$l", System.currentTimeMillis() - 86_400_000, Levels.reward(l), 0))
        dao.grant(Entry("buy:style:parchment:0", System.currentTimeMillis() - 86_400_000, -200, 0))
        settings.update {
            it.copy(
                onboarded = true, levelSeen = level, lastLat = here.first, lastLon = here.second,
                owned = it.owned + "parchment", shields = 1,
                seenBadges = app.fogwalk.domain.Badges.all.map { b -> b.id }.toSet() + app.fogwalk.domain.Relics.sets.map { s -> "set:$s" },
            )
        }
    }
}
