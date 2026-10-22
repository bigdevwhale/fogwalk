package app.fogwalk.ui.map

import android.view.MotionEvent
import android.view.ViewConfiguration
import org.osmdroid.views.MapView
import kotlin.math.abs

/**
 * Zoom with one thumb, as in most map apps: tap, then tap again and hold, and slide down to come
 * closer or up to step back. A plain double tap still zooms in a step where it lands.
 */
class OneFingerZoom(private val map: MapView) {
    private val config = ViewConfiguration.get(map.context)
    private val doubleTapMs = ViewConfiguration.getDoubleTapTimeout().toLong()
    private val doubleTapSlop = config.scaledDoubleTapSlop.toFloat()
    private val touchSlop = config.scaledTouchSlop.toFloat()
    /** Finger travel for one level of zoom. */
    private val pxPerLevel = 110 * map.resources.displayMetrics.density

    private var downX = 0f
    private var downY = 0f
    private var tapAt = 0L
    private var tapX = 0f
    private var tapY = 0f
    private var active = false
    private var sliding = false
    private var cancelling = false
    private var startZoom = 0.0

    /** True while the gesture owns the touch; the map sees none of it then. */
    fun onTouch(e: MotionEvent): Boolean {
        if (cancelling) return false
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val dx = e.x - tapX
                val dy = e.y - tapY
                if (e.eventTime - tapAt <= doubleTapMs && dx * dx + dy * dy <= doubleTapSlop * doubleTapSlop) {
                    active = true
                    sliding = false
                    startZoom = map.zoomLevelDouble
                    // The map forgets the first tap, so it neither zooms on its own nor opens a marker.
                    val cancel = MotionEvent.obtain(e)
                    cancel.action = MotionEvent.ACTION_CANCEL
                    cancelling = true
                    map.dispatchTouchEvent(cancel)
                    cancelling = false
                    cancel.recycle()
                }
                downX = e.x
                downY = e.y
            }
            MotionEvent.ACTION_MOVE -> if (active) {
                val dy = e.y - downY
                if (!sliding && abs(dy) > touchSlop) sliding = true
                if (sliding) map.controller.setZoom((startZoom + dy / pxPerLevel).coerceIn(map.minZoomLevel, map.maxZoomLevel))
            }
            MotionEvent.ACTION_UP -> {
                if (active) {
                    active = false
                    tapAt = 0
                    if (!sliding) map.controller.zoomInFixing(downX.toInt(), downY.toInt())
                    return true
                }
                // Only a tap that stayed put can be the first half of the gesture.
                val dx = e.x - downX
                val dy = e.y - downY
                if (dx * dx + dy * dy <= touchSlop * touchSlop) {
                    tapAt = e.eventTime
                    tapX = e.x
                    tapY = e.y
                } else tapAt = 0
            }
            MotionEvent.ACTION_CANCEL -> if (active) {
                active = false
                return true
            }
        }
        return active
    }
}
