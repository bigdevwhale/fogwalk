package app.fogwalk.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

/** An explored base cell (see [app.fogwalk.geo.Grid]) and when it was first seen. */
@Entity(tableName = "cells")
data class Cell(@PrimaryKey val id: Long, val at: Long)

/** What one calendar day brought: new cells, metres walked, caches, the beacon. */
@Entity(tableName = "days")
data class Day(
    /** Epoch day in the local zone. */
    @PrimaryKey val day: Long,
    val cells: Int = 0,
    val meters: Double = 0.0,
    val caches: Int = 0,
    /** Caches of rare quality or better. */
    val rare: Int = 0,
    val beacon: Boolean = false,
    /** Cells revealed between 22:00 and 04:00. */
    val night: Int = 0,
    /** Cells revealed between 04:00 and 07:00. */
    val dawn: Int = 0,
    /** Steps from the pedometer, when it is on. */
    @ColumnInfo(defaultValue = "0") val steps: Int = 0,
)

/** A stretch of walking without a long pause. */
@Entity(tableName = "walks")
data class Walk(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val start: Long,
    val end: Long,
    val meters: Double = 0.0,
    val cells: Int = 0,
    val caches: Int = 0,
    /** Steps from the pedometer, when it is on. */
    @ColumnInfo(defaultValue = "0") val steps: Int = 0,
    /** Where it began, for the journal line. */
    val lat: Double = 0.0,
    val lon: Double = 0.0,
)

/** A point of a walk's route, one per step the walker made (about every 10 m). */
@Entity(tableName = "waypoints", indices = [Index("walk")])
data class Waypoint(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val walk: Long,
    val at: Long,
    val lat: Double,
    val lon: Double,
)

/** An opened cache and what it held. */
@Entity(tableName = "finds")
data class Find(
    /** The cache key from [app.fogwalk.domain.Caches]; each cache opens once. */
    @PrimaryKey val key: String,
    val at: Long,
    val rarity: Int,
    /** Relic id, or empty when the cache held only sparks. */
    val relic: String = "",
    /** True when the relic was already in the collection and turned into sparks. */
    val duplicate: Boolean = false,
    val coins: Int,
    val xp: Int,
    val lat: Double,
    val lon: Double,
    /** Shown to the user with the chest animation. */
    val seen: Boolean = false,
)

/**
 * Every spark and point of experience not tied to a cell: quests, beacons, sector milestones,
 * level rewards and purchases. [ref] is unique, so each reward is granted once.
 */
@Entity(tableName = "ledger")
data class Entry(
    @PrimaryKey val ref: String,
    val at: Long,
    val coins: Int = 0,
    val xp: Int = 0,
)

@Dao
interface FogDao {
    @Query("SELECT id FROM cells")
    suspend fun cellIds(): LongArray

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCells(cells: List<Cell>)

    @Query("SELECT COUNT(*) FROM cells")
    suspend fun cellCount(): Int

    @Query("SELECT * FROM days ORDER BY day")
    fun observeDays(): Flow<List<Day>>

    @Query("SELECT * FROM days ORDER BY day")
    suspend fun days(): List<Day>

    @Query("SELECT * FROM days WHERE day = :day")
    suspend fun day(day: Long): Day?

    @Upsert
    suspend fun upsertDay(d: Day)

    @Query("SELECT * FROM walks ORDER BY start DESC")
    fun observeWalks(): Flow<List<Walk>>

    @Query("SELECT * FROM walks ORDER BY start DESC LIMIT 1")
    suspend fun lastWalk(): Walk?

    @Insert
    suspend fun insertWalk(w: Walk): Long

    @Upsert
    suspend fun upsertWalk(w: Walk)

    @Query("SELECT * FROM walks WHERE id = :id")
    fun observeWalk(id: Long): Flow<Walk?>

    @Query("SELECT * FROM walks ORDER BY start")
    suspend fun walks(): List<Walk>

    @Query("SELECT id FROM walks WHERE start = :start LIMIT 1")
    suspend fun walkAt(start: Long): Long?

    @Insert
    suspend fun insertWaypoints(points: List<Waypoint>)

    @Query("SELECT * FROM waypoints WHERE walk = :walk ORDER BY at, id")
    suspend fun waypoints(walk: Long): List<Waypoint>

    @Query("SELECT * FROM finds ORDER BY at")
    fun observeFinds(): Flow<List<Find>>

    @Query("SELECT * FROM finds ORDER BY at")
    suspend fun finds(): List<Find>

    @Query("SELECT key FROM finds WHERE key LIKE :prefix || '%'")
    suspend fun findKeys(prefix: String): List<String>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFind(f: Find): Long

    @Query("UPDATE finds SET seen = 1 WHERE `key` = :key")
    suspend fun markSeen(key: String)

    @Query("SELECT * FROM ledger ORDER BY at")
    fun observeLedger(): Flow<List<Entry>>

    @Query("SELECT * FROM ledger ORDER BY at")
    suspend fun ledger(): List<Entry>

    /** -1 when [e] was already granted. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun grant(e: Entry): Long

    @Query("SELECT COALESCE(SUM(coins), 0) FROM ledger")
    suspend fun ledgerCoins(): Int

    @Query("SELECT COALESCE(SUM(coins), 0) FROM finds")
    suspend fun findCoins(): Int
}

@Database(
    entities = [Cell::class, Day::class, Walk::class, Find::class, Entry::class, Waypoint::class],
    version = 3,
    exportSchema = true,
    // 2: walk routes. 3: steps.
    autoMigrations = [AutoMigration(from = 1, to = 2), AutoMigration(from = 2, to = 3)],
)
abstract class FogDatabase : RoomDatabase() {
    abstract fun dao(): FogDao

    companion object {
        fun create(context: Context): FogDatabase =
            Room.databaseBuilder(context.applicationContext, FogDatabase::class.java, "fogwalk.db").build()
    }
}
