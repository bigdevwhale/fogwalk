package app.fogwalk.explore

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

/**
 * The pedometer: the phone's own step counter, listened to while the location feed runs and the walker
 * has it switched on. The sensor counts since boot, so only the difference between readings is passed on.
 */
class StepCounter(private val context: Context, private val explorer: Explorer) {
    private val sm = context.getSystemService(SensorManager::class.java)
    private val sensor: Sensor? = sm?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    private var active = false
    private var enabled = false
    private var listening = false
    private var last = -1L
    private var pending = 0
    private val main = Handler(Looper.getMainLooper())
    /** Writes the steps still waiting once the walker stops, without waiting for the next one. */
    private val late = Runnable { flush() }

    private val listener = object : SensorEventListener {
        override fun onSensorChanged(e: SensorEvent) {
            val total = e.values[0].toLong()
            // The first reading only sets the mark; a lower one means the phone restarted.
            if (last >= 0 && total > last) {
                explorer.stepped(System.currentTimeMillis())
                if (pending == 0) main.postDelayed(late, FLUSH_MS)
                pending += (total - last).toInt()
            }
            last = total
            if (pending >= FLUSH_STEPS) flush()
        }

        override fun onAccuracyChanged(s: Sensor?, accuracy: Int) {}
    }

    /** True when the phone has a step counter at all. */
    val available: Boolean get() = sensor != null

    fun permitted(): Boolean = Build.VERSION.SDK_INT < 29 ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

    /** Main thread only: whether the location feed is running. */
    fun setActive(on: Boolean) {
        active = on
        refresh()
    }

    /** Main thread only: whether the walker wants steps counted. Also re-checks the permission. */
    fun setEnabled(on: Boolean) {
        enabled = on
        refresh()
    }

    fun refresh() {
        val want = active && enabled && sensor != null && permitted()
        if (want == listening) return
        listening = want
        explorer.pedometerRestarted()
        if (want) {
            last = -1
            sm?.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        } else {
            sm?.unregisterListener(listener)
            flush()
        }
    }

    private fun flush() {
        main.removeCallbacks(late)
        if (pending == 0) return
        explorer.addSteps(pending, System.currentTimeMillis())
        pending = 0
    }

    private companion object {
        const val FLUSH_STEPS = 20
        const val FLUSH_MS = 10_000L
    }
}
