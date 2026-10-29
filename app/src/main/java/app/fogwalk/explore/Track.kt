package app.fogwalk.explore

import app.fogwalk.geo.Grid
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Cleans raw positions before they touch the map. Phones mix satellite fixes with coarse network
 * ones and wander by tens of metres even when the walker stands still; taken as they come, a walk
 * zigzags, counts every wobble as distance and looks like a car on each jump. So:
 *
 * - network positions are only shown while satellites are silent, never walked on;
 * - a Kalman filter tuned to walking pace smooths the rest;
 * - a lone fix far from where the walker could be is dropped (three in a row mean a real jump);
 * - distance grows only once the smoothed position has left the last committed point by more than
 *   the noise, so standing still adds nothing;
 * - "too fast" needs the satellites' own speed, or several fast steps in a row, not one bad fix;
 * - while the pedometer listens and hears no steps, the walker is at rest whatever the satellites say:
 *   indoors their drift is slow, smooth and comes with a made-up speed, which nothing above catches.
 */
class Track {
    /** A cleaned position. [from] is where the walker came from when a step was made, else null. */
    data class Step(
        val lat: Double,
        val lon: Double,
        val accuracy: Float,
        val time: Long,
        val bearing: Float?,
        val from: Pair<Double, Double>?,
        val meters: Double,
        val fast: Boolean,
    )

    private var lat = 0.0
    private var lon = 0.0
    /** Variance of the estimate in m²; negative before the first fix. */
    private var variance = -1.0
    private var time = 0L
    private var lastGps = 0L
    private var rejects = 0
    private var fastHits = 0
    private var anchorLat = 0.0
    private var anchorLon = 0.0
    private var anchorTime = 0L
    private var hasAnchor = false
    /** When the pedometer last proved it hears steps, or 0 while it doesn't listen. Set from the main thread. */
    @Volatile
    private var pedometerSince = 0L
    @Volatile
    private var lastStepAt = 0L

    /** Null when [f] should only move the dot on the map (or nothing at all). */
    fun add(f: Fix): Step? {
        if (f.simulated) return simulated(f)
        // A cell tower's guess may move the dot (see [hasGps]) but never walks.
        if (!f.gps) return null
        if (f.accuracy > MAX_ACCURACY) return null
        lastGps = f.time
        val acc = max(f.accuracy.toDouble(), 3.0)

        if (variance < 0 || f.time - time > GAP_MS) {
            reset(f, acc)
            return Step(lat, lon, f.accuracy, f.time, f.bearing, null, 0.0, false)
        }

        val dt = max(0.0, (f.time - time) / 1000.0)
        val reported = f.speed
        val fastNow = reported != null && reported > Explorer.MAX_SPEED
        // A jump the walker could not have made in the time is noise, unless it repeats.
        val jump = Grid.distance(lat, lon, f.lat, f.lon)
        val possible = sqrt(variance) + acc + dt * MAX_JUMP_SPEED + 15
        if (jump > possible && !fastNow) {
            rejects++
            if (rejects < 3) return null
            // Three in a row: a real jump. Made at speed, it was a vehicle pulling away.
            val fast = dt > 0 && jump / dt > Explorer.MAX_SPEED
            reset(f, acc)
            if (fast) fastHits = FAST_HITS
            return Step(lat, lon, f.accuracy, f.time, f.bearing, null, 0.0, fast)
        }
        rejects = 0

        // Kalman update with walking-pace process noise.
        variance += dt * WALK_NOISE * WALK_NOISE
        val k = variance / (variance + acc * acc)
        lat += k * (f.lat - lat)
        lon += k * (f.lon - lon)
        variance *= (1 - k)
        time = f.time

        val step = Grid.distance(anchorLat, anchorLon, lat, lon)
        val stepDt = max(1.0, (f.time - anchorTime) / 1000.0)
        // Doppler speed is the best judge of a vehicle; without it, several fast steps in a row.
        // A short step says nothing either way.
        val fastEvidence = when {
            reported != null -> fastNow
            step >= minStep(acc, false) -> step / stepDt > Explorer.MAX_SPEED
            else -> null
        }
        if (fastEvidence != null) fastHits = if (fastEvidence) fastHits + 1 else 0
        val fast = fastHits >= FAST_HITS || (fastNow && reported!! > Explorer.MAX_SPEED * 1.5)

        if (fast) {
            moveAnchor(f.time)
            return Step(lat, lon, f.accuracy, f.time, f.bearing, null, 0.0, true)
        }
        // Standing still: the satellites say so, and the drift is small.
        val still = reported != null && reported < STILL_SPEED && step < STILL_DRIFT
        if (step < minStep(acc, reported != null) || still) return Step(lat, lon, f.accuracy, f.time, f.bearing, null, 0.0, false)
        // No steps heard: the drift is left behind, so it isn't counted once the walker sets off. A clean fix
        // at riding pace still counts, since a bike or a scooter makes no steps.
        val riding = reported != null && reported > RIDE_SPEED && f.accuracy <= RIDE_ACCURACY
        if (atRest(f.time) && !riding) {
            moveAnchor(f.time)
            return Step(lat, lon, f.accuracy, f.time, f.bearing, null, 0.0, false)
        }
        val from = anchorLat to anchorLon
        moveAnchor(f.time)
        return Step(lat, lon, f.accuracy, f.time, f.bearing, from, step, false)
    }

    /** True while satellites have spoken recently; network fixes may then not even move the dot. */
    fun hasGps(now: Long): Boolean = now - lastGps < GPS_FRESH_MS

    /** The pedometer started or stopped listening. Either way it is not trusted until it hears a first step. */
    fun pedometerRestarted() {
        pedometerSince = 0
        lastStepAt = 0
    }

    /** The pedometer heard steps at [at]. */
    fun stepped(at: Long) {
        if (pedometerSince == 0L) pedometerSince = at
        lastStepAt = at
    }

    private fun atRest(t: Long): Boolean = pedometerSince > 0 && t - lastStepAt > STEP_SILENCE_MS

    private fun simulated(f: Fix): Step {
        val had = hasAnchor
        val from = anchorLat to anchorLon
        lat = f.lat
        lon = f.lon
        variance = 25.0
        time = f.time
        moveAnchor(f.time)
        val meters = if (had) Grid.distance(from.first, from.second, lat, lon) else 0.0
        return Step(lat, lon, f.accuracy, f.time, f.bearing, if (had && meters > 0) from else null, meters, false)
    }

    private fun reset(f: Fix, acc: Double) {
        lat = f.lat
        lon = f.lon
        variance = acc * acc
        time = f.time
        rejects = 0
        fastHits = 0
        moveAnchor(f.time)
    }

    private fun moveAnchor(t: Long) {
        anchorLat = lat
        anchorLon = lon
        anchorTime = t
        hasAnchor = true
    }

    /** Without the satellites' speed, standing still can't be told from walking, so wobble needs more room. */
    private fun minStep(acc: Double, doppler: Boolean) =
        if (doppler) max(MIN_STEP, min(acc, 40.0) * 0.6) else max(MIN_STEP * 1.5, min(acc, 40.0) * 1.2)

    companion object {
        const val MAX_ACCURACY = 35f
        /** How fast the true position may drift between fixes, m/s: a brisk walk with slack. */
        const val WALK_NOISE = 2.5
        /** Faster than this between two fixes is a glitch, not a walker. */
        const val MAX_JUMP_SPEED = 12.0
        const val MIN_STEP = 10.0
        const val STILL_SPEED = 0.4f
        const val STILL_DRIFT = 25.0
        const val FAST_HITS = 3
        const val GAP_MS = 3 * 60_000L
        const val GPS_FRESH_MS = 30_000L
        /** No steps for this long means at rest; the sensor may hold its count back for several seconds. */
        const val STEP_SILENCE_MS = 30_000L
        /** Faster than a walk, m/s: the satellites' speed this high and this clean is a ride, not drift. */
        const val RIDE_SPEED = 2.5f
        const val RIDE_ACCURACY = 15f
    }
}
