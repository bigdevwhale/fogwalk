package app.fogwalk.explore

import app.fogwalk.geo.Grid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos

/**
 * Ways a walker can take, from OpenStreetMap, so the day's shrines stand by a path and not in the
 * middle of a factory yard (#21). Footways, paths and streets near the candidate points, without
 * private ones and without the ones inside industrial, military, railway or building-site ground.
 */
object Paths {
    private val ENDPOINTS = listOf(
        "https://overpass-api.de/api/interpreter",
        "https://maps.mail.ru/osm/tools/overpass/api/interpreter",
    )
    private const val HIGHWAYS = "^(footway|path|pedestrian|living_street|residential|track|cycleway|steps|bridleway|unclassified|tertiary|secondary|primary)$"
    private const val CLOSED = "^(industrial|military|railway|construction|quarry|landfill)$"
    private const val SHUT = "^(private|no)$"

    /** The query: walkable ways within [radius] m of each point, minus those inside closed ground. */
    fun query(points: List<Pair<Double, Double>>, radius: Int): String {
        fun at(p: Pair<Double, Double>) = String.format(Locale.US, "%.6f,%.6f", p.first, p.second)
        val q = '"'
        val ways = points.joinToString("") { "way(around:$radius,${at(it)})[highway~$q$HIGHWAYS$q][access!~$q$SHUT$q][foot!~$q$SHUT$q];" }
        val near = points.joinToString("") { "way(around:$radius,${at(it)})[landuse~$q$CLOSED$q];" }
        val under = points.joinToString("") { "is_in(${at(it)});" }
        return "[out:json][timeout:25];($ways)->.w;($near)->.n;.n map_to_area->.na;($under)->.u;" +
            "area.u[landuse~$q$CLOSED$q]->.ua;(.na;.ua;)->.closed;way.w(area.closed)->.inside;(.w; - .inside;)->.ok;.ok out geom;"
    }

    /** The ways near [points] as lists of (lat, lon), or null when OpenStreetMap can't be reached. */
    suspend fun near(points: List<Pair<Double, Double>>, radius: Int = 200): List<List<Pair<Double, Double>>>? = withContext(Dispatchers.IO) {
        val body = "data=" + URLEncoder.encode(query(points, radius), "UTF-8")
        for (url in ENDPOINTS) {
            val text = runCatching {
                val c = URL(url).openConnection() as HttpURLConnection
                c.connectTimeout = 15_000
                c.readTimeout = 40_000
                c.requestMethod = "POST"
                c.doOutput = true
                c.setRequestProperty("User-Agent", "Fogwalk (app.fogwalk; github.com/bigdevwhale/fogwalk)")
                c.setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
                c.outputStream.use { it.write(body.toByteArray()) }
                if (c.responseCode != 200) error("HTTP ${c.responseCode}")
                c.inputStream.bufferedReader().use { it.readText() }
            }.getOrNull() ?: continue
            return@withContext runCatching { parse(text) }.getOrNull() ?: continue
        }
        null
    }

    fun parse(json: String): List<List<Pair<Double, Double>>> {
        val els = JSONObject(json).optJSONArray("elements") ?: return emptyList()
        return (0 until els.length()).mapNotNull { i ->
            val g = els.getJSONObject(i).optJSONArray("geometry") ?: return@mapNotNull null
            (0 until g.length()).map { k -> g.getJSONObject(k).let { it.getDouble("lat") to it.getDouble("lon") } }.takeIf { it.size >= 2 }
        }
    }

    /** The nearest point on any of [ways] to (lat, lon), if one is within [maxM] metres. */
    fun snap(lat: Double, lon: Double, ways: List<List<Pair<Double, Double>>>, maxM: Double = 250.0): Pair<Double, Double>? {
        // Flat local metres around the point are plenty at these distances.
        val my = 111_320.0
        val mx = my * cos(lat * PI / 180)
        var best: Pair<Double, Double>? = null
        var bestD = Double.MAX_VALUE
        for (w in ways) for (k in 0 until w.size - 1) {
            val ax = (w[k].second - lon) * mx
            val ay = (w[k].first - lat) * my
            val dx = (w[k + 1].second - lon) * mx - ax
            val dy = (w[k + 1].first - lat) * my - ay
            val len = dx * dx + dy * dy
            val t = if (len == 0.0) 0.0 else (-(ax * dx + ay * dy) / len).coerceIn(0.0, 1.0)
            val px = ax + t * dx
            val py = ay + t * dy
            val d = px * px + py * py
            if (d < bestD) {
                bestD = d
                best = (lat + py / my) to (lon + px / mx)
            }
        }
        return best?.takeIf { Grid.distance(lat, lon, it.first, it.second) <= maxM }
    }
}
