package app.fogwalk.explore

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.net.toUri
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.Period

/**
 * Steps from Health Connect: the phone's shared health store, where Samsung Health, Mi Fitness,
 * Zepp Life, Google Fit, watches and bands put the steps they count. They count all day, not only
 * while Fogwalk listens, and steps a watch hands over late still arrive on the next read.
 */
class HealthSteps(private val context: Context) {
    private val status: Int get() = HealthConnectClient.getSdkStatus(context)

    /** Health Connect is on the phone and ready. */
    val available: Boolean get() = status == HealthConnectClient.SDK_AVAILABLE

    /** Health Connect could work here once it is installed or updated from the store. */
    val installable: Boolean get() = status == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED

    private var made: HealthConnectClient? = null

    /** Made once Health Connect is there, which it may become only after the app has started. */
    private val client: HealthConnectClient?
        get() = made ?: if (!available) null else runCatching { HealthConnectClient.getOrCreate(context) }
            .onFailure { Log.w(TAG, "client", it) }.getOrNull()?.also { made = it }

    suspend fun allowed(): Boolean {
        val c = client ?: return false
        return runCatching { READ in c.permissionController.getGrantedPermissions() }.getOrDefault(false)
    }

    /** Steps per day from [from] to [to], keyed by epoch day; null if Health Connect can't be read now. */
    suspend fun days(from: LocalDate, to: LocalDate): Map<Long, Long>? {
        val c = client ?: return null
        return runCatching {
            val req = AggregateGroupByPeriodRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(from.atStartOfDay(), to.plusDays(1).atStartOfDay()),
                timeRangeSlicer = Period.ofDays(1),
            )
            c.aggregateGroupByPeriod(req).associate { g ->
                g.startTime.toLocalDate().toEpochDay() to (g.result[StepsRecord.COUNT_TOTAL] ?: 0L)
            }
        }.onFailure { Log.w(TAG, "days", it) }.getOrNull()
    }

    /** Steps between two moments (epoch ms), for a walk; null if Health Connect can't be read now. */
    suspend fun between(start: Long, end: Long): Long? {
        val c = client ?: return null
        if (end <= start) return 0
        return runCatching {
            val req = AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(Instant.ofEpochMilli(start), Instant.ofEpochMilli(end)),
            )
            c.aggregate(req)[StepsRecord.COUNT_TOTAL] ?: 0L
        }.onFailure { Log.w(TAG, "walk", it) }.getOrNull()
    }

    /** The store page where Health Connect can be installed or updated. */
    fun storeIntent(): Intent = Intent(Intent.ACTION_VIEW, "market://details?id=$PROVIDER&url=healthconnect%3A%2F%2Fonboarding".toUri())
        .setPackage("com.android.vending")
        .putExtra("overlay", true)
        .putExtra("callerId", context.packageName)

    companion object {
        private const val TAG = "HealthSteps"
        const val PROVIDER = "com.google.android.apps.healthdata"
        val READ = HealthPermission.getReadPermission(StepsRecord::class)

        /** Health Connect's own screen for granting permissions. */
        fun requestContract() = PermissionController.createRequestPermissionResultContract()
    }
}
