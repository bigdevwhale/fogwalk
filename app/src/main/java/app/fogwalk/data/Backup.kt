package app.fogwalk.data

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject

/** The whole explored world as one JSON file, to move to a new phone or keep safe. */
object Backup {
    suspend fun export(db: FogDatabase, s: Settings? = null): String {
        val dao = db.dao()
        val o = JSONObject()
        o.put("app", "fogwalk")
        o.put("version", 1)
        // Cells are the bulk; first-seen times are dropped to keep the file small.
        o.put("cells", JSONArray().apply { dao.cellIds().forEach { put(it) } })
        o.put("days", JSONArray().apply {
            dao.days().forEach { d ->
                put(JSONObject().put("day", d.day).put("cells", d.cells).put("meters", d.meters).put("caches", d.caches)
                    .put("rare", d.rare).put("beacon", d.beacon).put("night", d.night).put("dawn", d.dawn).put("steps", d.steps))
            }
        })
        o.put("finds", JSONArray().apply {
            dao.finds().forEach { f ->
                put(JSONObject().put("key", f.key).put("at", f.at).put("rarity", f.rarity).put("relic", f.relic)
                    .put("duplicate", f.duplicate).put("coins", f.coins).put("xp", f.xp).put("lat", f.lat).put("lon", f.lon))
            }
        })
        o.put("ledger", JSONArray().apply {
            dao.ledger().forEach { e -> put(JSONObject().put("ref", e.ref).put("at", e.at).put("coins", e.coins).put("xp", e.xp)) }
        })
        // Journeys with their routes as flat lat, lon pairs.
        o.put("walks", JSONArray().apply {
            dao.walks().forEach { w ->
                val route = JSONArray()
                dao.waypoints(w.id).forEach { p -> route.put(round(p.lat)).put(round(p.lon)) }
                put(JSONObject().put("start", w.start).put("end", w.end).put("meters", w.meters).put("cells", w.cells)
                    .put("caches", w.caches).put("steps", w.steps).put("lat", w.lat).put("lon", w.lon).put("route", route))
            }
        })
        // What was bought and earned to keep (#22): fog styles, gear, wards and altars.
        if (s != null) o.put("kit", JSONObject()
            .put("owned", JSONArray(s.owned.toList())).put("fog", s.fog).put("fogDensity", s.fogDensity.toDouble())
            .put("gear", JSONObject().apply { s.gear.forEach { (k, v) -> put(k, v) } })
            .put("shields", s.shields).put("shielded", JSONArray(s.shielded.toList())).put("altars", s.altars)
            .put("ring", JSONArray().apply { s.ring.forEach { (la, lo) -> put(la).put(lo) } }))
        return o.toString()
    }

    /** The kit of a backup laid over [mine]: more is kept, never less, on either side. */
    fun mergeKit(mine: Settings, kit: JSONObject): Settings {
        val owned = kit.optJSONArray("owned")?.let { a -> (0 until a.length()).map { a.getString(it) } }.orEmpty()
        val gear = kit.optJSONObject("gear")?.let { g -> g.keys().asSequence().associateWith { g.getInt(it) } }.orEmpty()
        val shielded = kit.optJSONArray("shielded")?.let { a -> (0 until a.length()).map { a.getLong(it) } }.orEmpty()
        val ring = kit.optJSONArray("ring")?.let { a -> (0 until a.length() / 2).map { a.getDouble(it * 2) to a.getDouble(it * 2 + 1) } }.orEmpty()
        val fog = kit.optString("fog")
        val untouched = mine.fog == Settings().fog
        return mine.copy(
            owned = mine.owned + owned,
            fog = if (untouched && fog.isNotEmpty() && fog in mine.owned + owned) fog else mine.fog,
            fogDensity = if (untouched && kit.has("fogDensity")) kit.getDouble("fogDensity").toFloat() else mine.fogDensity,
            gear = (mine.gear.keys + gear.keys).associateWith { maxOf(mine.gear[it] ?: 0, gear[it] ?: 0) },
            shields = maxOf(mine.shields, kit.optInt("shields")),
            shielded = mine.shielded + shielded,
            altars = maxOf(mine.altars, kit.optInt("altars")),
            ring = mine.ring.ifEmpty { ring },
        )
    }

    private fun round(v: Double) = Math.round(v * 1e6) / 1e6

    /** Merges a backup into what is already there; nothing is lost on either side. */
    suspend fun import(db: FogDatabase, text: String, settings: SettingsStore? = null) {
        val o = JSONObject(text)
        require(o.optString("app") == "fogwalk")
        // Backups before 1.9 carry no kit.
        o.optJSONObject("kit")?.let { kit -> settings?.update { mergeKit(it, kit) } }
        val dao = db.dao()
        db.withTransaction {
            val cells = o.getJSONArray("cells")
            val now = System.currentTimeMillis()
            (0 until cells.length()).chunked(5000).forEach { chunk -> dao.insertCells(chunk.map { Cell(cells.getLong(it), now) }) }
            val days = o.getJSONArray("days")
            for (i in 0 until days.length()) {
                val d = days.getJSONObject(i)
                val mine = dao.day(d.getLong("day"))
                val theirs = Day(
                    d.getLong("day"), d.getInt("cells"), d.getDouble("meters"), d.getInt("caches"),
                    d.getInt("rare"), d.getBoolean("beacon"), d.getInt("night"), d.getInt("dawn"), d.optInt("steps"),
                )
                dao.upsertDay(
                    if (mine == null) theirs else Day(
                        theirs.day, maxOf(mine.cells, theirs.cells), maxOf(mine.meters, theirs.meters), maxOf(mine.caches, theirs.caches),
                        maxOf(mine.rare, theirs.rare), mine.beacon || theirs.beacon, maxOf(mine.night, theirs.night), maxOf(mine.dawn, theirs.dawn),
                        maxOf(mine.steps, theirs.steps),
                    )
                )
            }
            val finds = o.getJSONArray("finds")
            for (i in 0 until finds.length()) {
                val f = finds.getJSONObject(i)
                dao.insertFind(
                    Find(
                        f.getString("key"), f.getLong("at"), f.getInt("rarity"), f.getString("relic"), f.getBoolean("duplicate"),
                        f.getInt("coins"), f.getInt("xp"), f.getDouble("lat"), f.getDouble("lon"), seen = true,
                    )
                )
            }
            val ledger = o.getJSONArray("ledger")
            for (i in 0 until ledger.length()) {
                val e = ledger.getJSONObject(i)
                dao.grant(Entry(e.getString("ref"), e.getLong("at"), e.getInt("coins"), e.getInt("xp")))
            }
            // Backups from 1.0 have no journeys.
            val walks = o.optJSONArray("walks") ?: JSONArray()
            for (i in 0 until walks.length()) {
                val w = walks.getJSONObject(i)
                val start = w.getLong("start")
                if (dao.walkAt(start) != null) continue
                val end = w.getLong("end")
                val id = dao.insertWalk(
                    Walk(
                        start = start, end = end, meters = w.getDouble("meters"), cells = w.getInt("cells"),
                        caches = w.getInt("caches"), steps = w.optInt("steps"), lat = w.getDouble("lat"), lon = w.getDouble("lon"),
                    )
                )
                val route = w.optJSONArray("route") ?: continue
                val n = route.length() / 2
                dao.insertWaypoints(List(n) { j ->
                    Waypoint(walk = id, at = start + (end - start) * j / maxOf(1, n - 1), lat = route.getDouble(j * 2), lon = route.getDouble(j * 2 + 1))
                })
            }
        }
    }
}
