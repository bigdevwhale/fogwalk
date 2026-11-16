package app.fogwalk

import app.fogwalk.data.Backup
import app.fogwalk.data.Day
import app.fogwalk.data.Settings
import app.fogwalk.domain.Bonds
import app.fogwalk.domain.Gear
import app.fogwalk.domain.Road
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The Great Road counts every kilometre, and a companion grows into a friend on walks. */
class RoadTest {
    @Test
    fun stepsCountWhenNoJourneyRan() {
        assertEquals(700.0, Road.meters(Day(1, steps = 1000)), 1e-9)
        // A journey and its steps are the same walk: the larger tells it, never the sum.
        assertEquals(1500.0, Road.meters(Day(1, meters = 1500.0, steps = 1000)), 1e-9)
    }

    @Test
    fun stationsAreReachedInOrderAndGoOnPastTheLast() {
        assertEquals(0, Road.reached(2.9))
        assertEquals(1, Road.reached(3.0))
        assertEquals(4, Road.reached(42.0))
        assertEquals(Road.stations.size, Road.reached(5000.0))
        assertEquals(6000, Road.at(Road.stations.size))
        assertEquals(Road.stations.size + 1, Road.reached(6000.0))
        assertEquals(0.5f, Road.leg(6.5), 1e-6f)
    }

    @Test
    fun friendshipGrowsWithKilometres() {
        assertEquals(0, Bonds.level(2_999.0))
        assertEquals(1, Bonds.level(3_000.0))
        assertEquals(Bonds.MAX, Bonds.level(1e9))
        assertEquals(1f, Bonds.toNext(1e9), 1e-6f)
        assertEquals(0.5f, Bonds.toNext(6_500.0), 1e-6f)
    }

    @Test
    fun onlyTheCompanionOnTheMapGivesItsGift() {
        val fox = Settings(companion = "fox", bond = mapOf("fox" to 25_000.0, "hedgehog" to 1e9))
        assertEquals(Gear.openM(fox.gear) + 3 * Bonds.FOX_M, Bonds.openM(fox), 1e-9)
        assertEquals(Gear.senseM(fox.gear), Bonds.senseM(fox), 1e-9)
        val token = Settings(bond = mapOf("token" to 1e9))
        assertEquals(0, Bonds.of(token).second)
        assertEquals(Gear.openM(token.gear), Bonds.openM(token), 1e-9)
    }

    @Test
    fun theOwlSeesFartherOnlyByNight() {
        val owl = Settings(companion = "owl", bond = mapOf("owl" to 1e9))
        assertEquals(Gear.revealM(owl.gear), Bonds.revealM(owl, 12), 1e-9)
        assertEquals(Gear.revealM(owl.gear) + Bonds.MAX * Bonds.OWL_M, Bonds.revealM(owl, 23), 1e-9)
    }

    @Test
    fun aCloserFriendBringsABiggerPresent() {
        val (c0, x0) = Bonds.gift(Settings(companion = "cat"))
        val (c5, x5) = Bonds.gift(Settings(companion = "cat", bond = mapOf("cat" to 1e9)))
        assertTrue(c5 > c0 && x5 > x0)
        val (g5, _) = Bonds.gift(Settings(companion = "granny", bond = mapOf("granny" to 1e9)))
        assertTrue(g5 > c5)
    }

    @Test
    fun friendshipRidesInABackupAndMergesToTheMore() {
        val kit = JSONObject().put("bond", JSONObject().put("fox", 9000.0).put("owl", 100.0))
        val merged = Backup.mergeKit(Settings(bond = mapOf("fox" to 1000.0, "owl" to 500.0)), kit)
        assertEquals(mapOf("fox" to 9000.0, "owl" to 500.0), merged.bond)
    }

    @Test
    fun theSatchelHoldsTheLastTenKilometresNotYetTaken() {
        assertEquals(emptyList<Int>(), app.fogwalk.domain.Wayside.satchel(0.9, emptySet()).map { it.km })
        assertEquals((3..12).toList(), app.fogwalk.domain.Wayside.satchel(12.4, emptySet()).map { it.km })
        val taken = setOf(app.fogwalk.domain.Wayside.ref(12), app.fogwalk.domain.Wayside.ref(5))
        assertEquals((3..11).filter { it != 5 }, app.fogwalk.domain.Wayside.satchel(12.4, taken).map { it.km })
        assertEquals(600.0, app.fogwalk.domain.Wayside.toNext(12.4), 1e-6)
    }

    @Test
    fun aKilometresFindIsAlwaysTheSameAndAllKindsTurnUp() {
        assertEquals(app.fogwalk.domain.Wayside.at(77), app.fogwalk.domain.Wayside.at(77))
        val kinds = (1..2000).map { app.fogwalk.domain.Wayside.at(it).kind }.toSet()
        assertEquals(app.fogwalk.domain.Wayside.Kind.entries.toSet(), kinds)
    }

    @Test
    fun farStationsGiveGrandGifts() {
        assertEquals(Road.Gift.Ward, Road.gift(0))
        assertEquals(Road.Gift.Relic(app.fogwalk.domain.Rarity.LEGENDARY), Road.gift(Road.stations.lastIndex))
        assertEquals(Road.Gift.Relic(app.fogwalk.domain.Rarity.EPIC), Road.gift(Road.stations.size + 3))
    }
}
