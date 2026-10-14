package app.fogwalk.domain

import app.fogwalk.geo.Grid
import app.fogwalk.geo.LongIntMap
import app.fogwalk.geo.LongSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Every explored base cell, held in memory, with per-level counts so a zoomed-out map can draw a
 * district as partly cleared without visiting its thousands of cells. Writes come from one thread
 * (the explorer); the map reads under the same lock.
 */
class FogStore {
    private val cells = LongSet(4096)

    /** Explored base cells per coarser cell, for [LEVELS] after the first. */
    private val counts = Array(LEVELS.size) { LongIntMap(1024) }

    private val _version = MutableStateFlow(0)

    /** Bumped on every change; the map redraws and the screens recount on it. */
    val version: StateFlow<Int> = _version

    var areaM2 = 0.0
        private set

    val size: Int @Synchronized get() = cells.size

    @Synchronized
    operator fun contains(key: Long): Boolean = key in cells

    /** Adds without announcing; call [changed] after a batch. True when the cell is new. */
    @Synchronized
    fun add(key: Long): Boolean {
        if (!cells.add(key)) return false
        areaM2 += Grid.area(key)
        for (i in 1 until LEVELS.size) counts[i].increment(Grid.parent(key, LEVELS[i]))
        return true
    }

    fun changed() {
        _version.value++
    }

    /** How many explored base cells lie inside [key], a cell at [level]. */
    @Synchronized
    fun count(key: Long, level: Int): Int {
        val i = LEVELS.indexOf(level)
        return if (i <= 0) (if (key in cells) 1 else 0) else counts[i][key]
    }

    /** Share of sector [sector] that is explored, 0..1. */
    fun sectorShare(sector: Long): Double = count(sector, Grid.SECTOR_Z).toDouble() / Grid.SECTOR_CELLS

    /** Sectors with at least one explored cell and how many. */
    @Synchronized
    fun sectors(): Pair<LongArray, IntArray> = counts[LEVELS.indexOf(Grid.SECTOR_Z)].entries()

    /** Explored cells at [level] inside the given cell-index box, with their explored-base counts. */
    @Synchronized
    fun visit(level: Int, x0: Int, y0: Int, x1: Int, y1: Int, action: (x: Int, y: Int, count: Int) -> Unit) {
        val i = LEVELS.indexOf(level)
        val boxCells = (x1 - x0 + 1).toLong() * (y1 - y0 + 1)
        if (i == 0) {
            if (boxCells < cells.size) {
                for (x in x0..x1) for (y in y0..y1) if (Grid.key(x, y) in cells) action(x, y, 1)
            } else cells.forEach { k ->
                val x = Grid.x(k)
                val y = Grid.y(k)
                if (x in x0..x1 && y in y0..y1) action(x, y, 1)
            }
        } else {
            val map = counts[i]
            if (boxCells < map.size) {
                for (x in x0..x1) for (y in y0..y1) {
                    val c = map[Grid.key(x, y)]
                    if (c > 0) action(x, y, c)
                }
            } else map.forEach { k, c ->
                val x = Grid.x(k)
                val y = Grid.y(k)
                if (x in x0..x1 && y in y0..y1) action(x, y, c)
            }
        }
    }

    /** Explored base cells within [radius] metres of a point. */
    @Synchronized
    fun exploredAround(lat: Double, lon: Double, radius: Double): Int {
        var n = 0
        Grid.around(lat, lon, radius) { if (it in cells) n++ }
        return n
    }

    companion object {
        /** Zoom levels the map can draw from, finest first. */
        val LEVELS = intArrayOf(Grid.Z, 18, 16, 14, 12, 10, 8)
    }
}
