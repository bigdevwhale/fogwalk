package app.fogwalk

import app.fogwalk.content.ContentEn
import app.fogwalk.content.ContentRu
import app.fogwalk.data.Day
import app.fogwalk.domain.Badges
import app.fogwalk.domain.Caches
import app.fogwalk.domain.FogStore
import app.fogwalk.domain.Levels
import app.fogwalk.domain.Quests
import app.fogwalk.domain.Relics
import app.fogwalk.domain.Streaks
import app.fogwalk.explore.Fix
import app.fogwalk.explore.Track
import app.fogwalk.geo.Grid
import app.fogwalk.geo.LongIntMap
import app.fogwalk.geo.LongSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class FogwalkTest {
    @Test
    fun gridRoundTrips() {
        val k = Grid.cell(55.7558, 37.6173)
        assertEquals(55.7558, Grid.centerLat(k), 0.001)
        assertEquals(37.6173, Grid.centerLon(k), 0.001)
        // Base cells are about 22 m across in Moscow.
        assertEquals(21.6, Grid.cellMeters(55.75), 0.5)
        assertEquals(Grid.key(Grid.x(k) shr 6, Grid.y(k) shr 6), Grid.sector(k))
    }

    @Test
    fun revealCircleCoversTheRadius() {
        var n = 0
        Grid.around(55.75, 37.6, 40.0) { n++ }
        // π·40² / 21.6² ≈ 10.8 cells.
        assertTrue(n in 8..14)
    }

    @Test
    fun distanceAndOffsetAgree() {
        val (lat, lon) = Grid.offset(55.75, 37.6, 1000.0, 90.0)
        assertEquals(1000.0, Grid.distance(55.75, 37.6, lat, lon), 1.0)
        assertEquals(90.0, Grid.bearing(55.75, 37.6, lat, lon), 0.5)
    }

    @Test
    fun longSetBehavesLikeASet() {
        val s = LongSet(4)
        val ref = HashSet<Long>()
        val rnd = Random(1)
        repeat(20_000) {
            val v = rnd.nextLong(5000) shl 32 or rnd.nextLong(5000)
            assertEquals(ref.add(v), s.add(v))
        }
        assertEquals(ref.size, s.size)
        ref.forEach { assertTrue(it in s) }
        val m = LongIntMap()
        m.increment(7)
        m.increment(7)
        assertEquals(2, m[7])
        assertEquals(0, m[8])
    }

    @Test
    fun fogCountsCoarserLevels() {
        val fog = FogStore()
        Grid.around(55.75, 37.6, 40.0) { fog.add(it) }
        val k = Grid.cell(55.75, 37.6)
        assertEquals(fog.size, fog.count(Grid.sector(k), Grid.SECTOR_Z))
        assertFalse(fog.add(k))
        assertTrue(fog.areaM2 > 4000)
    }

    @Test
    fun cachesAreStableWithinAWeekAndChangeAcrossWeeks() {
        val a = Caches.inTile(39_000, 20_000, 2900)
        assertEquals(a, Caches.inTile(39_000, 20_000, 2900))
        val total = (0 until 50).sumOf { Caches.inTile(39_000 + it, 20_000, 2900).size }
        assertTrue(total in 40..90)
        val weeks = (0 until 10).map { Caches.inTile(39_000, 20_000, 2900L + it) }
        assertTrue(weeks.toSet().size > 3)
        assertEquals(Caches.week(LocalDays.MONDAY), Caches.week(LocalDays.MONDAY + 6))
        assertNotEquals(Caches.week(LocalDays.MONDAY), Caches.week(LocalDays.MONDAY + 7))
    }

    @Test
    fun levelsAreMonotonic() {
        assertEquals(1, Levels.level(0))
        assertEquals(2, Levels.level(120))
        var prev = 0
        for (l in 1..60) {
            assertTrue(Levels.start(l + 1) - Levels.start(l) == Levels.need(l))
            assertTrue(Levels.start(l) >= prev)
            prev = Levels.start(l)
        }
    }

    @Test
    fun streakCountsBackFromToday() {
        val today = 20_000L
        val days = listOf(Day(today - 3, cells = 40), Day(today - 2, meters = 1500.0), Day(today - 1, beacon = true))
        val s = Streaks.of(days, emptySet(), today)
        assertEquals(3, s.current)
        assertFalse(s.todayDone)
        // One missed day, one shield: covered.
        val gap = listOf(Day(today - 3, cells = 40), Day(today - 2, cells = 40))
        assertEquals(listOf(today - 1), Streaks.toShield(gap, emptySet(), 1, today))
        assertEquals(emptyList<Long>(), Streaks.toShield(gap, emptySet(), 0, today))
    }

    @Test
    fun questsAreFixedForTheDay() {
        assertEquals(Quests.daily(20_000, 5), Quests.daily(20_000, 5))
        assertEquals(3, Quests.daily(20_000, 5).size)
        assertEquals(36, Relics.all.size)
    }

    @Test
    fun everyDeedHasWordsInBothLanguages() {
        assertTrue(Badges.all.size >= 150)
        assertEquals(Badges.all.size, Badges.all.map { it.id }.toSet().size)
        for (c in listOf(ContentEn, ContentRu)) {
            Badges.all.forEach { b ->
                assertNotEquals(b.id, c.badgeTitle(b.id))
                assertTrue(b.id, c.badgeText(b).isNotBlank())
            }
            Badges.groups.forEach { assertTrue(c.groupName(it.id).isNotBlank()) }
            Relics.all.forEach { assertNotEquals(it.id, c.relicName(it.id)) }
        }
        // Russian land names agree in gender: no masculine ending before a feminine noun.
        assertFalse((0L until 500L).map { ContentRu.sectorName(it * 7919) }.any { it.endsWith("ый Роща") || it.endsWith("ий Лощина") })
    }

    private object LocalDays {
        /** 2026-10-12, a Monday. */
        const val MONDAY = 20_738L
    }

    /** A walk east from Moscow's centre: [seconds] fixes at [speed] m/s with GPS-like noise. */
    private fun noisyWalk(seconds: Int, speed: Double, sigma: Double, spikes: Boolean, network: Boolean, doppler: Boolean): List<Fix> {
        val rnd = java.util.Random(7)
        val lat0 = 55.75
        val lon0 = 37.6
        val mLon = 111_320.0 * Math.cos(Math.toRadians(lat0))
        // GPS error drifts rather than jumping: a slowly wandering offset plus a little white noise.
        var ex = 0.0
        var ey = 0.0
        val out = ArrayList<Fix>()
        for (t in 0 until seconds) {
            ex = ex * 0.9 + rnd.nextGaussian() * sigma * 0.45
            ey = ey * 0.9 + rnd.nextGaussian() * sigma * 0.45
            var x = speed * t + ex + rnd.nextGaussian() * 2
            var y = ey + rnd.nextGaussian() * 2
            if (spikes && t % 37 == 0 && t > 0) { x += 70; y -= 50 }
            val time = 1_000_000L + t * 1000L
            out += Fix(lat0 + y / 111_320.0, lon0 + x / mLon, (sigma * 1.5).toFloat(), time, speed = if (doppler) speed.toFloat() else null)
            if (network && t % 5 == 2) {
                out += Fix(lat0 + (y + 35) / 111_320.0, lon0 + (x - 30) / mLon, 30f, time + 1, gps = false)
            }
        }
        return out
    }

    private fun run(fixes: List<Fix>): Triple<Double, Int, Int> {
        val track = Track()
        var meters = 0.0
        var fast = 0
        var steps = 0
        for (f in fixes) {
            val s = track.add(f) ?: continue
            meters += s.meters
            if (s.fast) fast++
            if (s.from != null) steps++
        }
        return Triple(meters, fast, steps)
    }

    @Test
    fun noisyWalkCountsTheRealDistance() {
        // 1.4 m/s for 12 minutes: about 1 km.
        for (doppler in listOf(true, false)) {
            val (meters, fast, _) = run(noisyWalk(720, 1.4, 8.0, spikes = true, network = true, doppler = doppler))
            assertTrue("walked $meters m (doppler=$doppler)", meters in 850.0..1150.0)
            assertEquals("never too fast (doppler=$doppler)", 0, fast)
        }
    }

    @Test
    fun standingStillAddsAlmostNothing() {
        // Satellite fixes carry their own speed; without it a slow drift can't be told from a stroll.
        val (meters, fast, _) = run(noisyWalk(900, 0.0, 10.0, spikes = true, network = true, doppler = true))
        assertTrue("drifted $meters m", meters < 60)
        assertEquals(0, fast)
    }

    @Test
    fun aCarIsTooFast() {
        for (doppler in listOf(true, false)) {
            val (meters, fast, _) = run(noisyWalk(120, 14.0, 5.0, spikes = false, network = false, doppler = doppler))
            assertTrue("fast fixes $fast (doppler=$doppler)", fast > 60)
            assertTrue("counted $meters m in a car", meters < 150)
        }
    }
}
