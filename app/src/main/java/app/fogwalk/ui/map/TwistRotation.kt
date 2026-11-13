package app.fogwalk.ui.map

import android.view.MotionEvent
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.abs
import kotlin.math.atan2

/**
 * Two fingers turn the map, but only once they have plainly twisted (#45). A pinch always tilts the line
 * between the fingers a little; osmdroid's own overlay turned the map with every degree of it, so zooming
 * left the map askew. Below [THRESHOLD] degrees nothing turns; past it the map follows from there on, without a jump.
 */
class TwistRotation(private val map: MapView) : Overlay() {
    private var start = 0f
    private var base = 0f
    private var orientation = 0f
    private var twisting = false
    private var tracking = false

    override fun onTouchEvent(e: MotionEvent, mapView: MapView): Boolean {
        if (e.pointerCount < 2) {
            tracking = false
            twisting = false
            return false
        }
        val angle = angle(e)
        if (!tracking || e.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
            tracking = true
            twisting = false
            start = angle
            return false
        }
        if (e.actionMasked != MotionEvent.ACTION_MOVE) return false
        if (!twisting) {
            if (abs(wrap(angle - start)) < THRESHOLD) return false
            twisting = true
            base = angle
            orientation = map.mapOrientation
        }
        map.mapOrientation = orientation + wrap(angle - base)
        return false
    }

    /** The line between the first two fingers, degrees, as osmdroid measures it. */
    private fun angle(e: MotionEvent): Float =
        Math.toDegrees(atan2((e.getY(0) - e.getY(1)).toDouble(), (e.getX(0) - e.getX(1)).toDouble())).toFloat()

    private fun wrap(d: Float): Float {
        var a = d % 360f
        if (a > 180f) a -= 360f
        if (a < -180f) a += 360f
        return a
    }

    companion object {
        const val THRESHOLD = 15f
    }
}
