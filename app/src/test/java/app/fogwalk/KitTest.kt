package app.fogwalk

import app.fogwalk.data.Backup
import app.fogwalk.data.Settings
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

/** What was bought moves with a backup, and merging never takes anything away (#22). */
class KitTest {
    private val kit = JSONObject()
        .put("owned", JSONArray(listOf("mist", "starry")))
        .put("fog", "starry").put("fogDensity", 0.8)
        .put("gear", JSONObject().put("cloak", 3).put("boots", 1))
        .put("shields", 2).put("shielded", JSONArray(listOf(20000L))).put("altars", 1)
        .put("ring", JSONArray(listOf(55.1, 37.2, 55.2, 37.3)))

    @Test
    fun aFreshPhoneTakesTheWholeKit() {
        val s = Backup.mergeKit(Settings(), kit)
        assertEquals(setOf("mist", "starry"), s.owned)
        assertEquals("starry", s.fog)
        assertEquals(mapOf("cloak" to 3, "boots" to 1), s.gear)
        assertEquals(2, s.shields)
        assertEquals(1, s.altars)
        assertEquals(listOf(55.1 to 37.2, 55.2 to 37.3), s.ring)
    }

    @Test
    fun mergingKeepsTheBetterOfBoth() {
        val mine = Settings(owned = setOf("mist", "parchment"), fog = "parchment", gear = mapOf("cloak" to 5), shields = 3)
        val s = Backup.mergeKit(mine, kit)
        assertEquals(setOf("mist", "parchment", "starry"), s.owned)
        assertEquals("parchment", s.fog)
        assertEquals(mapOf("cloak" to 5, "boots" to 1), s.gear)
        assertEquals(3, s.shields)
    }
}
