package app.fogwalk.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("settings")

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class Units { METRIC, IMPERIAL }

data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val onboarded: Boolean = false,
    val name: String = "",
    val units: Units = Units.METRIC,
    /** Fog style id from [app.fogwalk.domain.Shop]. */
    val fog: String = "mist",
    /** How opaque the fog is, 0.6..1. */
    val fogDensity: Float = 0.92f,
    /** Shop items bought for good (fog styles). */
    val owned: Set<String> = setOf("mist"),
    val shields: Int = 0,
    /** Until when the lantern widens the reveal radius, epoch millis. */
    val lanternUntil: Long = 0,
    /** Until when the radar shows caches through the fog, epoch millis. */
    val radarUntil: Long = 0,
    /** Today's shrines, placed in the fog; day 0 means none yet. */
    val shrines: app.fogwalk.domain.ShrineDay = app.fogwalk.domain.ShrineDay(),
    /** Epoch day of the last beacon reroll, one per day. */
    val rerollDay: Long = 0,
    /** Epoch days a streak shield covered. */
    val shielded: Set<Long> = emptySet(),
    /** Evening nudge when the streak is at risk. */
    val remind: Boolean = true,
    /** Minutes after midnight. */
    val remindAt: Int = 18 * 60 + 30,
    /** Notifications during a walk when a cache is close. */
    val nearbyAlerts: Boolean = true,
    val haptics: Boolean = true,
    /** Count steps with the phone's pedometer. */
    val steps: Boolean = false,
    /** Where the map last saw the walker, to open there next time. */
    val lastLat: Double = 0.0,
    val lastLon: Double = 0.0,
    /** True while a background walk (the foreground service) should be running. */
    val walking: Boolean = false,
    val seenBadges: Set<String> = emptySet(),
    val levelSeen: Int = 1,
    /** This week's oath, if one was sworn. */
    val oath: app.fogwalk.domain.Oath? = null,
    /** The treasure map being followed. */
    val treasure: app.fogwalk.domain.TreasureMap? = null,
    /** A race against a past journey's shadow. */
    val race: app.fogwalk.domain.Race? = null,
    /** Gear levels by piece id. */
    val gear: Map<String, Int> = emptyMap(),
    /** The tavern's rumour of the week. */
    val rumor: app.fogwalk.domain.Rumor? = null,
    /** Altars in the satchel, from azure shrines. */
    val altars: Int = 0,
    /** Altars set on the ground, in the order they were set. */
    val ring: List<Pair<Double, Double>> = emptyList(),
    /** Journeys kept at hand for races. */
    val pinned: Set<Long> = emptySet(),
)

class SettingsStore(context: Context) {
    private val store = context.applicationContext.dataStore

    val settings: Flow<Settings> = store.data.map { it.read() }

    private fun Preferences.read() = Settings(
        theme = this[THEME]?.let { t -> runCatching { ThemeMode.valueOf(t) }.getOrNull() } ?: ThemeMode.SYSTEM,
        onboarded = this[ONBOARDED] ?: false,
        name = this[NAME] ?: "",
        units = this[UNITS]?.let { u -> runCatching { Units.valueOf(u) }.getOrNull() } ?: Units.METRIC,
        fog = this[FOG] ?: "mist",
        fogDensity = this[FOG_DENSITY] ?: 0.92f,
        owned = (this[OWNED] ?: emptySet()) + "mist",
        shields = this[SHIELDS] ?: 0,
        lanternUntil = this[LANTERN] ?: 0,
        radarUntil = this[RADAR] ?: 0,
        shrines = this[SHRINES]?.let { app.fogwalk.domain.ShrineDay.decode(it) } ?: app.fogwalk.domain.ShrineDay(),
        rerollDay = this[REROLL_DAY] ?: 0,
        shielded = (this[SHIELDED] ?: emptySet()).mapNotNull { it.toLongOrNull() }.toSet(),
        remind = this[REMIND] ?: true,
        remindAt = this[REMIND_AT] ?: (18 * 60 + 30),
        nearbyAlerts = this[NEARBY] ?: true,
        haptics = this[HAPTICS] ?: true,
        steps = this[STEPS] ?: false,
        lastLat = this[LAST_LAT] ?: 0.0,
        lastLon = this[LAST_LON] ?: 0.0,
        walking = this[WALKING] ?: false,
        seenBadges = this[SEEN_BADGES] ?: emptySet(),
        levelSeen = this[LEVEL_SEEN] ?: 1,
        oath = this[OATH]?.let { app.fogwalk.domain.Oath.decode(it) },
        treasure = this[TREASURE]?.let { app.fogwalk.domain.TreasureMap.decode(it) },
        race = this[RACE]?.let { app.fogwalk.domain.Race.decode(it) },
        gear = (this[GEAR] ?: emptySet()).mapNotNull { e -> e.split(":").takeIf { it.size == 2 }?.let { (k, v) -> v.toIntOrNull()?.let { k to it } } }.toMap(),
        rumor = this[RUMOR]?.let { app.fogwalk.domain.Rumor.decode(it) },
        altars = this[ALTARS] ?: 0,
        ring = this[RING]?.let { app.fogwalk.domain.Altars.decode(it) } ?: emptyList(),
        pinned = (this[PINNED] ?: emptySet()).mapNotNull { it.toLongOrNull() }.toSet(),
    )

    suspend fun now(): Settings = settings.first()

    suspend fun update(change: (Settings) -> Settings) {
        store.edit {
            val s = change(it.read())
            it[THEME] = s.theme.name
            it[ONBOARDED] = s.onboarded
            it[NAME] = s.name
            it[UNITS] = s.units.name
            it[FOG] = s.fog
            it[FOG_DENSITY] = s.fogDensity
            it[OWNED] = s.owned
            it[SHIELDS] = s.shields
            it[LANTERN] = s.lanternUntil
            it[RADAR] = s.radarUntil
            it[SHRINES] = s.shrines.encode()
            it[REROLL_DAY] = s.rerollDay
            it[SHIELDED] = s.shielded.map { d -> d.toString() }.toSet()
            it[REMIND] = s.remind
            it[REMIND_AT] = s.remindAt
            it[NEARBY] = s.nearbyAlerts
            it[HAPTICS] = s.haptics
            it[STEPS] = s.steps
            it[LAST_LAT] = s.lastLat
            it[LAST_LON] = s.lastLon
            it[WALKING] = s.walking
            it[SEEN_BADGES] = s.seenBadges
            it[LEVEL_SEEN] = s.levelSeen
            it[OATH] = s.oath?.encode().orEmpty()
            it[TREASURE] = s.treasure?.encode().orEmpty()
            it[RACE] = s.race?.encode().orEmpty()
            it[GEAR] = s.gear.map { (k, v) -> "$k:$v" }.toSet()
            it[RUMOR] = s.rumor?.encode().orEmpty()
            it[ALTARS] = s.altars
            it[RING] = app.fogwalk.domain.Altars.encode(s.ring)
            it[PINNED] = s.pinned.map { w -> w.toString() }.toSet()
        }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val NAME = stringPreferencesKey("name")
        val UNITS = stringPreferencesKey("units")
        val FOG = stringPreferencesKey("fog")
        val FOG_DENSITY = floatPreferencesKey("fog_density")
        val OWNED = stringSetPreferencesKey("owned")
        val SHIELDS = intPreferencesKey("shields")
        val LANTERN = longPreferencesKey("lantern_until")
        val RADAR = longPreferencesKey("radar_until")
        val SHRINES = stringPreferencesKey("shrines")
        val REROLL_DAY = longPreferencesKey("reroll_day")
        val SHIELDED = stringSetPreferencesKey("shielded")
        val REMIND = booleanPreferencesKey("remind")
        val REMIND_AT = intPreferencesKey("remind_at")
        val NEARBY = booleanPreferencesKey("nearby_alerts")
        val HAPTICS = booleanPreferencesKey("haptics")
        val STEPS = booleanPreferencesKey("steps")
        val LAST_LAT = doublePreferencesKey("last_lat")
        val LAST_LON = doublePreferencesKey("last_lon")
        val WALKING = booleanPreferencesKey("walking")
        val SEEN_BADGES = stringSetPreferencesKey("seen_badges")
        val LEVEL_SEEN = intPreferencesKey("level_seen")
        val OATH = stringPreferencesKey("oath")
        val TREASURE = stringPreferencesKey("treasure")
        val RACE = stringPreferencesKey("race")
        val GEAR = stringSetPreferencesKey("gear")
        val RUMOR = stringPreferencesKey("rumor")
        val ALTARS = intPreferencesKey("altars")
        val RING = stringPreferencesKey("ring")
        val PINNED = stringSetPreferencesKey("pinned_walks")
    }
}
