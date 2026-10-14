package app.fogwalk.explore

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import app.fogwalk.container
import app.fogwalk.notify.Alarms
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * A walk with the screen off: keeps GPS running in a foreground service with a quiet notification
 * that counts the new ground, and pings when a cache is close.
 */
class ExploreService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var holding = false
    private var ticker: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stop(this)
            return START_NOT_STICKY
        }
        val c = container
        if (!c.feed.permitted()) {
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            ServiceCompat.startForeground(
                this, Alarms.ID_WALK, Alarms.walkNotification(this, c.explorer.live.value),
                if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0,
            )
        } catch (_: Exception) {
            // Android refuses a location service started from the background; the next app launch restarts it.
            stopSelf()
            return START_NOT_STICKY
        }
        if (!holding) {
            holding = true
            c.feed.acquire()
            scope.launch { c.settings.update { it.copy(walking = true) } }
            ticker = scope.launch {
                while (true) {
                    delay(10_000)
                    Alarms.updateWalk(this@ExploreService, c.explorer.live.value)
                }
            }
            scope.launch {
                c.explorer.events.collect { e -> Alarms.onEvent(this@ExploreService, e) }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        if (holding) container.feed.release()
        holding = false
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val ACTION_STOP = "app.fogwalk.STOP_WALK"

        fun start(context: Context) {
            ContextCompat.startForegroundService(context, Intent(context, ExploreService::class.java))
        }

        fun stopIntent(context: Context): Intent = Intent(context, ExploreService::class.java).setAction(ACTION_STOP)

        fun stop(context: Context) {
            val c = context.container
            c.scope.launch { c.settings.update { it.copy(walking = false) } }
            context.stopService(Intent(context, ExploreService::class.java))
        }
    }
}
