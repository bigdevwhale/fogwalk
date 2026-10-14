package app.fogwalk.geo

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt

/**
 * The world is cut into Web Mercator cells, the same squares map tiles use. A base cell is a tile at
 * zoom [Z] (about 38 m at the equator, 22 m in Moscow). A cell is one Long: x in the high half, y in the low.
 * Coarser levels are the same grid with fewer bits, so a cell's parent is a shift away.
 */
object Grid {
    const val Z = 20
    const val N = 1 shl Z
    private const val EARTH_M = 6_371_008.8
    private const val EQUATOR_M = 40_075_016.686

    /** Sectors, the named districts of the map: zoom-14 tiles, 64 × 64 base cells. */
    const val SECTOR_Z = 14
    const val SECTOR_CELLS = 1 shl ((Z - SECTOR_Z) * 2)

    fun key(x: Int, y: Int): Long = (x.toLong() shl 32) or (y.toLong() and 0xffffffffL)
    fun x(key: Long): Int = (key ushr 32).toInt()
    fun y(key: Long): Int = key.toInt()

    /** Mercator x in 0..1 from west to east. */
    fun mx(lon: Double): Double = (lon + 180.0) / 360.0

    /** Mercator y in 0..1 from north to south. */
    fun my(lat: Double): Double {
        val s = sin(lat.coerceIn(-85.05, 85.05) * PI / 180)
        return 0.5 - ln((1 + s) / (1 - s)) / (4 * PI)
    }

    fun lon(mx: Double): Double = mx * 360.0 - 180.0
    fun lat(my: Double): Double = atan(sinh(PI * (1 - 2 * my))) * 180 / PI

    fun cell(lat: Double, lon: Double): Long =
        key(floor(mx(lon) * N).toInt().coerceIn(0, N - 1), floor(my(lat) * N).toInt().coerceIn(0, N - 1))

    /** The cell at [level] that contains base cell [key]. */
    fun parent(key: Long, level: Int): Long {
        val d = Z - level
        return key(x(key) shr d, y(key) shr d)
    }

    fun sector(key: Long): Long = parent(key, SECTOR_Z)

    fun centerLat(key: Long, level: Int = Z): Double = lat((y(key) + 0.5) / (1 shl level))
    fun centerLon(key: Long, level: Int = Z): Double = lon((x(key) + 0.5) / (1 shl level))

    /** Edge of a base cell in metres at [lat]. */
    fun cellMeters(lat: Double): Double = EQUATOR_M * cos(lat * PI / 180) / N

    /** Area of base cell [key] in square metres. */
    fun area(key: Long): Double {
        val m = cellMeters(centerLat(key))
        return m * m
    }

    /** Base cells whose centre lies within [radius] metres of the point. */
    inline fun around(lat: Double, lon: Double, radius: Double, out: (Long) -> Unit) {
        val m = cellMeters(lat)
        val r = (radius / m)
        val cx = mx(lon) * N
        val cy = my(lat) * N
        val x0 = floor(cx - r).toInt()
        val x1 = floor(cx + r).toInt()
        val y0 = floor(cy - r).toInt()
        val y1 = floor(cy + r).toInt()
        val r2 = r * r
        for (x in x0..x1) for (y in y0..y1) {
            val dx = x + 0.5 - cx
            val dy = y + 0.5 - cy
            if (dx * dx + dy * dy <= r2) out(key(x.coerceIn(0, N - 1), y.coerceIn(0, N - 1)))
        }
    }

    /** Great-circle distance in metres. */
    fun distance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = lat1 * PI / 180
        val p2 = lat2 * PI / 180
        val dp = p2 - p1
        val dl = (lon2 - lon1) * PI / 180
        val a = sin(dp / 2) * sin(dp / 2) + cos(p1) * cos(p2) * sin(dl / 2) * sin(dl / 2)
        return 2 * EARTH_M * asin(sqrt(a.coerceIn(0.0, 1.0)))
    }

    /** Compass bearing from the first point to the second, 0..360 degrees. */
    fun bearing(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val p1 = lat1 * PI / 180
        val p2 = lat2 * PI / 180
        val dl = (lon2 - lon1) * PI / 180
        val y = sin(dl) * cos(p2)
        val x = cos(p1) * sin(p2) - sin(p1) * cos(p2) * cos(dl)
        return (atan2(y, x) * 180 / PI + 360) % 360
    }

    /** The point [meters] away along [bearing] degrees. */
    fun offset(lat: Double, lon: Double, meters: Double, bearing: Double): Pair<Double, Double> {
        val d = meters / EARTH_M
        val b = bearing * PI / 180
        val p1 = lat * PI / 180
        val l1 = lon * PI / 180
        val p2 = asin(sin(p1) * cos(d) + cos(p1) * sin(d) * cos(b))
        val l2 = l1 + atan2(sin(b) * sin(d) * cos(p1), cos(d) - sin(p1) * sin(p2))
        return p2 * 180 / PI to ((l2 * 180 / PI + 540) % 360 - 180)
    }

    /** Land the whole Earth's surface covers, for the "share of the world" figure. */
    const val EARTH_KM2 = 510_072_000.0
}
