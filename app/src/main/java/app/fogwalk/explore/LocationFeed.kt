package app.fogwalk.explore

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Looper
import androidx.core.content.ContextCompat

/**
 * One GPS subscription shared by the map screen and the walk service. Plain LocationManager, so it
 * works on phones without Google services. Positions go straight to the [Explorer].
 */
class LocationFeed(private val context: Context, private val explorer: Explorer, private val onActive: (Boolean) -> Unit = {}) {
    private val lm = context.getSystemService(LocationManager::class.java)
    private var users = 0
    private val listener = LocationListener { deliver(it) }

    fun permitted(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** Main thread only. */
    fun acquire() {
        users++
        if (users == 1) {
            start()
            onActive(true)
        }
    }

    fun release() {
        if (users == 0) return
        users--
        if (users == 0) {
            lm.removeUpdates(listener)
            onActive(false)
        }
    }

    /** Called after the permission is granted while someone already holds the feed. */
    fun restart() {
        if (users == 0) return
        lm.removeUpdates(listener)
        start()
    }

    private fun start() {
        // Screenshot builds stand still on the demo street instead of following the phone.
        if (app.fogwalk.BuildConfig.DEMO) {
            val (lat, lon) = app.fogwalk.data.DemoSeed.here
            if (lat != 0.0) explorer.submit(Fix(lat, lon, 6f, System.currentTimeMillis(), bearing = 40f, simulated = true))
            return
        }
        if (!permitted()) return
        try {
            val providers = lm.getProviders(true)
            if (LocationManager.GPS_PROVIDER in providers) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 0f, listener, Looper.getMainLooper())
            }
            // Network positions arrive at once indoors; they only place the dot until satellites answer.
            if (LocationManager.NETWORK_PROVIDER in providers) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 5000L, 0f, listener, Looper.getMainLooper())
            }
            providers.mapNotNull { lm.getLastKnownLocation(it) }
                .filter { System.currentTimeMillis() - it.time < 10 * 60_000L }
                .minByOrNull { it.accuracy }
                ?.let { deliver(it) }
        } catch (_: SecurityException) {
        } catch (_: IllegalArgumentException) {
        }
    }

    private fun deliver(l: Location) {
        explorer.submit(
            Fix(
                lat = l.latitude,
                lon = l.longitude,
                accuracy = if (l.hasAccuracy()) l.accuracy else 50f,
                time = l.time,
                bearing = if (l.hasBearing()) l.bearing else null,
                gps = l.provider == LocationManager.GPS_PROVIDER,
                speed = if (l.hasSpeed()) l.speed else null,
            )
        )
    }
}
