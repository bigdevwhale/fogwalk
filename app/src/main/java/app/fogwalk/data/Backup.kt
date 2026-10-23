package app.fogwalk.data

import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONObject

/** The whole explored world as one JSON file, to move to a new phone or keep safe. */
object Backup {
    suspend fun export(db: FogDatabase): String {
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
        return o.toString()
    }

    private fun round(v: Double) = Math.round(v * 1e6) / 1e6

    /** Merges a backup into what is already there; nothing is lost on either side. */
    suspend fun import(db: FogDatabase, text: String) {
        val o = JSONObject(text)
        require(o.optString("app") == "fogwalk")
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
