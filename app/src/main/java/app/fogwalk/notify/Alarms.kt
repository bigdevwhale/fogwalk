package app.fogwalk.notify

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import app.fogwalk.MainActivity
import app.fogwalk.R
import app.fogwalk.container
import app.fogwalk.content.Content
import app.fogwalk.domain.Streaks
import app.fogwalk.explore.ExploreService
import app.fogwalk.explore.GameEvent
import app.fogwalk.explore.Live
import app.fogwalk.ui.formatDistance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/** The walk notification, cache pings and the evening nudge that guards the streak. */
object Alarms {
    private const val CHANNEL_WALK = "walk_v1"
    private const val CHANNEL_ALERTS = "alerts_v1"
    const val EXTRA_ROUTE = "route"

    private const val ACTION_REMIND = "app.fogwalk.notify.REMIND"

    const val ID_WALK = 1
    private const val ID_REMIND = 2
    private const val ID_NEAR = 3
    private const val ID_BEACON = 4

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_WALK) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_WALK, context.getString(R.string.channel_walk), NotificationManager.IMPORTANCE_LOW).apply {
                    description = context.getString(R.string.channel_walk_desc)
                    setShowBadge(false)
                }
            )
        }
        if (nm.getNotificationChannel(CHANNEL_ALERTS) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ALERTS, context.getString(R.string.channel_alerts), NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = context.getString(R.string.channel_alerts_desc)
                }
            )
        }
    }

    private fun pi(context: Context, action: String, code: Int) = PendingIntent.getBroadcast(
        context, code,
        Intent(context, AlarmReceiver::class.java).setAction(action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    /** Next time the clock shows [minutes] after midnight, strictly after now. */
    fun nextAt(minutes: Int, now: LocalDateTime): LocalDateTime {
        val m = Math.floorMod(minutes, 24 * 60)
        val today = now.toLocalDate().atTime(m / 60, m % 60)
        return if (today.isAfter(now)) today else today.plusDays(1)
    }

    suspend fun reschedule(context: Context) {
        val s = context.container.settings.now()
        val am = context.getSystemService(AlarmManager::class.java)
        val zone = ZoneId.systemDefault()
        val p = pi(context, ACTION_REMIND, 1)
        // A nudge doesn't need to-the-second precision; this avoids the exact-alarm permission.
        if (!s.remind) am.cancel(p)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAt(s.remindAt, LocalDateTime.now(zone)).atZone(zone).toInstant().toEpochMilli(), p)
    }

    suspend fun fire(context: Context, action: String) {
        if (action != ACTION_REMIND) return
        val c = context.container
        val s = c.settings.now()
        if (!s.remind) return
        val today = LocalDate.now().toEpochDay()
        val streak = Streaks.of(c.db.dao().days(), s.shielded, today)
        if (streak.todayDone) return
        val content = Content.of(context.resources.configuration.locales[0])
        val (title, text) = if (streak.current > 0) {
            context.getString(R.string.notif_streak_title, streak.current) to content.nudge(today, true)
        } else context.getString(R.string.notif_beacon_title) to content.nudge(today, false)
        notify(context, ID_REMIND, title, text, "map")
    }

    fun walkNotification(context: Context, live: Live): Notification {
        val w = live.walk?.takeIf { System.currentTimeMillis() - it.end < 20 * 60_000L }
        val text = if (w == null) context.getString(R.string.notif_walk_start)
        else context.getString(
            R.string.notif_walk_text,
            context.resources.getQuantityString(R.plurals.cells, w.cells, w.cells),
            formatDistance(context, w.meters),
        )
        return NotificationCompat.Builder(context, CHANNEL_WALK)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.accent))
            .setContentTitle(context.getString(R.string.notif_walk_title))
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(w != null)
            .setWhen(w?.start ?: System.currentTimeMillis())
            .setUsesChronometer(w != null)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(open(context, 200, "map"))
            .addAction(
                0, context.getString(R.string.stop_walk),
                PendingIntent.getService(context, 201, ExploreService.stopIntent(context), PendingIntent.FLAG_IMMUTABLE),
            )
            .build()
    }

    fun updateWalk(context: Context, live: Live) {
        if (!canNotify(context)) return
        try {
            NotificationManagerCompat.from(context).notify(ID_WALK, walkNotification(context, live))
        } catch (_: SecurityException) {
        }
    }

    /** Pings for a walk with the phone in a pocket; quiet while the app is on screen. */
    suspend fun onEvent(context: Context, e: GameEvent) {
        val c = context.container
        if (c.visible) return
        val s = c.settings.now()
        val content = Content.of(context.resources.configuration.locales[0])
        when (e) {
            is GameEvent.Near -> if (s.nearbyAlerts) notify(
                context, ID_NEAR,
                context.getString(R.string.notif_near_title, content.rarity(e.cache.rarity)),
                context.getString(R.string.notif_near_text, formatDistance(context, e.meters.toDouble())),
                "map", quiet = e.cache.rarity.ordinal == 0,
            )
            is GameEvent.Opened -> notify(
                context, ID_NEAR,
                context.getString(R.string.notif_opened_title, content.rarity(app.fogwalk.domain.Rarity.entries[e.find.rarity])),
                context.getString(R.string.notif_opened_text, e.find.coins),
                "map",
            )
            is GameEvent.Beacon -> notify(context, ID_BEACON, context.getString(R.string.beacon_reached), context.getString(R.string.beacon_reached_text), "map")
            else -> Unit
        }
    }

    private fun open(context: Context, code: Int, route: String) = PendingIntent.getActivity(
        context, code,
        Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(EXTRA_ROUTE, route),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun notify(context: Context, id: Int, title: String, text: String, route: String, quiet: Boolean = false) {
        if (!canNotify(context)) return
        val b = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(ContextCompat.getColor(context, R.color.accent))
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open(context, 100 + id, route))
            .setSilent(quiet)
            .setAutoCancel(true)
        try {
            NotificationManagerCompat.from(context).notify(id, b.build())
        } catch (_: SecurityException) {
        }
    }

    internal val actions = setOf(ACTION_REMIND)
}

private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action !in Alarms.actions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Alarms.fire(context, action)
                Alarms.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    private val actions = setOf(
        Intent.ACTION_BOOT_COMPLETED,
        Intent.ACTION_MY_PACKAGE_REPLACED,
        Intent.ACTION_TIME_CHANGED,
        Intent.ACTION_TIMEZONE_CHANGED,
    )

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in actions) return
        val pending = goAsync()
        receiverScope.launch {
            try {
                Alarms.reschedule(context)
            } finally {
                pending.finish()
            }
        }
    }
}
