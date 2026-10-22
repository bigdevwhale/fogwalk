package app.fogwalk.ui.map

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.view.MotionEvent
import app.fogwalk.domain.Cache
import app.fogwalk.domain.Caches
import app.fogwalk.domain.FogStore
import app.fogwalk.domain.Races
import app.fogwalk.domain.Rarity
import app.fogwalk.domain.TreasureMap
import app.fogwalk.explore.Fix
import app.fogwalk.geo.Grid
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** Something on the map the walker can tap. */
sealed interface Pick {
    data class OfCache(val cache: Cache, val sensed: Boolean) : Pick
    data object OfBeacon : Pick
    data class OfLand(val sector: Long) : Pick
    data object OfTreasure : Pick
}

/** A race in progress, ready to draw: the route, when the old walk passed each point, and how far along each is. */
class RaceView(
    val route: List<Pair<Double, Double>>,
    val times: LongArray,
    val along: DoubleArray,
    val started: Long,
    val reached: Int,
) {
    /** Metres along the route the shadow has come at [now]. */
    fun shadowM(now: Long): Double = if (started == 0L) 0.0 else Races.shadow(times, along, now - started)
}

/** What the overlay needs from the app; swapped as a whole when anything changes. */
class Scene(
    val fix: Fix?,
    val trail: List<Pair<Double, Double>>,
    val beacon: Pair<Double, Double>?,
    val beaconReached: Boolean,
    val week: Long,
    val opened: Set<String>,
    val radar: Boolean,
    val style: String,
    val density: Float,
    val rarityColors: IntArray,
    val accent: Int,
    val gold: Int,
    /** The land tapped on the map, highlighted. */
    val picked: Long?,
    /** How far chests glimmer through the mist. */
    val senseM: Double,
    /** A chest the tavern told of, shown through the mist. */
    val rumor: String?,
    val treasure: TreasureMap?,
    val race: RaceView?,
    /** How near the walker must come to the tapped chest or shrine: lat, lon and metres. */
    val reach: Triple<Double, Double, Double>? = null,
)

/**
 * The fog and everything that lives in it, drawn over the tiles. The fog is painted into a bitmap at
 * a fraction of the screen size, explored cells are cut out as circles, and the bitmap is scaled up
 * with filtering, which gives soft cloud-like edges for free. It is rebuilt only when the view or the
 * explored ground changes; the pulsing markers on top redraw every frame.
 */
class WorldOverlay(
    private val fog: FogStore,
    private val density: Float,
    private val onPick: (Pick?) -> Unit,
    private val onLongPress: ((Double, Double) -> Unit)?,
) : Overlay() {
    @Volatile
    var scene: Scene? = null

    private val scale = 3
    private var fogBitmap: Bitmap? = null
    private var fogCanvas: Canvas? = null
    private var fogKey: List<Any>? = null
    /** The view the fog bitmap was rendered for: scale and world corner, in Mercator units. */
    private var cacheAx = 1.0
    private var cacheAy = 1.0
    private var cacheOx = 0.0
    private var cacheOy = 0.0
    private var lastAx = 0.0
    private var zoomedAt = 0L
    private val dst = RectF()

    private val clearPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT) }
    private val texPaint = Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_ATOP) }
    private val blitPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val glow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val border = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val path = Path()

    /** Screen = a * mercator + b, refreshed each frame. */
    private var ax = 0.0
    private var bx = 0.0
    private var ay = 0.0
    private var by = 0.0

    /** Lands are drawn at this map scale, so a tap between markers may pick one. */
    private var landsShown = false

    /** Markers drawn this frame, for taps. */
    private val hits = ArrayList<Pair<Pick, FloatArray>>()

    private fun sx(lon: Double) = (ax * Grid.mx(lon) + bx).toFloat()
    private fun sy(lat: Double) = (ay * Grid.my(lat) + by).toFloat()

    override fun draw(c: Canvas, mv: MapView, shadow: Boolean) {
        if (shadow) return
        val s = scene ?: return
        val w = mv.width
        val h = mv.height
        if (w == 0 || h == 0) return
        val proj = mv.projection
        val bb = proj.boundingBox
        val nw = proj.toPixels(GeoPoint(bb.latNorth, bb.lonWest), null)
        val se = proj.toPixels(GeoPoint(bb.latSouth, bb.lonEast), null)
        val mx0 = Grid.mx(bb.lonWest)
        val mx1 = Grid.mx(bb.lonEast)
        val my0 = Grid.my(bb.latNorth)
        val my1 = Grid.my(bb.latSouth)
        if (mx1 - mx0 <= 0 || my1 - my0 <= 0) return
        ax = (se.x - nw.x) / (mx1 - mx0)
        bx = nw.x - ax * mx0
        ay = (se.y - nw.y) / (my1 - my0)
        by = nw.y - ay * my0

        drawFog(c, s, w, h)
        drawLands(c, s, w, h, mx0, mx1, my0, my1)
        hits.clear()
        val t = System.currentTimeMillis()
        drawTreasure(c, s)
        drawRace(c, s, t)
        drawTrail(c, s)
        drawReach(c, s)
        drawCaches(c, s, mx0, mx1, my0, my1, t)
        drawBeacon(c, s, t)
        drawWalker(c, s)
    }

    /**
     * The fog is rendered once, with a margin around the screen and pinned to a fixed lattice of the
     * world, then only moved and scaled while the map pans or zooms. Re-rendering it every frame made
     * the soft edges shimmer as they landed on the coarse bitmap's pixels a fraction differently.
     * It is redrawn when new ground is revealed, when the view leaves the margin, or once a zoom settles.
     */
    private fun drawFog(c: Canvas, s: Scene, w: Int, h: Int) {
        val key = listOf(fog.version.value, s.style, s.density, w, h)
        val now = android.os.SystemClock.uptimeMillis()
        if (ax != lastAx) zoomedAt = now
        lastAx = ax
        val bmp = fogBitmap
        val ratio = ax / cacheAx
        val rebuild = bmp == null || key != fogKey || run {
            val l = ax * cacheOx + bx
            val t = ay * cacheOy + by
            val r = l + bmp.width * scale * ratio
            val b = t + bmp.height * scale * (ay / cacheAy)
            val covers = l <= 0 && t <= 0 && r >= w && b >= h
            // Mid-zoom the old fog is stretched; it is redrawn sharp when the fingers rest.
            val settled = abs(ratio - 1) > 0.002 && now - zoomedAt > 220
            !covers || ratio !in 0.7..1.45 || settled
        }
        if (rebuild) {
            render(s, w, h)
            fogKey = key
        }
        val f = fogBitmap ?: return
        val l = (ax * cacheOx + bx).toFloat()
        val t = (ay * cacheOy + by).toFloat()
        dst.set(l, t, l + (f.width * scale * ax / cacheAx).toFloat(), t + (f.height * scale * ay / cacheAy).toFloat())
        c.drawBitmap(f, null, dst, blitPaint)
    }

    private fun render(s: Scene, w: Int, h: Int) {
        val margin = max(w, h) * 0.3
        // Mercator units per fog pixel, and the cache's corner snapped to that lattice.
        cacheAx = ax
        cacheAy = ay
        val ux = scale / ax
        val uy = scale / ay
        cacheOx = floor(((-margin - bx) / ax) / ux) * ux
        cacheOy = floor(((-margin - by) / ay) / uy) * uy
        val fw = ((w + 2 * margin) / scale).toInt() + 3
        val fh = ((h + 2 * margin) / scale).toInt() + 3
        var bmp = fogBitmap
        if (bmp == null || bmp.width != fw || bmp.height != fh) {
            bmp?.recycle()
            bmp = Bitmap.createBitmap(fw, fh, Bitmap.Config.ARGB_8888)
            fogBitmap = bmp
            fogCanvas = Canvas(bmp)
        }
        val fc = fogCanvas!!
        bmp.eraseColor(0)
        val look = FogStyles.look(s.style)
        fill.shader = null
        fill.color = look.base
        fill.alpha = (s.density * 255).toInt()
        fc.drawRect(0f, 0f, fw.toFloat(), fh.toFloat(), fill)
        // The cloud texture sticks to the world, scaled to stay a few hundred pixels wide.
        val tex = FogStyles.texture(s.style)
        val shader = BitmapShader(tex, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
        var tilePx = ax / scale / (1 shl 14)
        while (tilePx < 200) tilePx *= 2
        while (tilePx > 400) tilePx /= 2
        val m = Matrix()
        val k = (tilePx / tex.width).toFloat()
        m.setScale(k, k)
        m.postTranslate(((-cacheOx / ux) % tilePx).toFloat(), ((-cacheOy / uy) % tilePx).toFloat())
        shader.setLocalMatrix(m)
        texPaint.shader = shader
        texPaint.alpha = 255
        fc.drawRect(0f, 0f, fw.toFloat(), fh.toFloat(), texPaint)
        cutExplored(fc, cacheOx, cacheOx + fw * ux, cacheOy, cacheOy + fh * uy)
    }

    private fun cutExplored(fc: Canvas, mx0: Double, mx1: Double, my0: Double, my1: Double) {
        // The finest level whose cells are still a few pixels wide.
        var level = FogStore.LEVELS.last()
        for (l in FogStore.LEVELS) {
            if (ax / (1 shl l) >= 4 * density) {
                level = l
                break
            }
        }
        val n = 1 shl level
        val cellPx = ax / n / scale
        val margin = 2
        val x0 = max(0, floor(mx0 * n).toInt() - margin)
        val x1 = min(n - 1, floor(mx1 * n).toInt() + margin)
        val y0 = max(0, floor(my0 * n).toInt() - margin)
        val y1 = min(n - 1, floor(my1 * n).toInt() + margin)
        val capacity = 1 shl ((Grid.Z - level) * 2)
        val r = max(1.2, cellPx * 1.05).toFloat()
        clearPaint.color = 0xFF000000.toInt()
        // A soft edge on each cut so the clearing reads as thinning fog, not a row of coins.
        clearPaint.maskFilter = if (r >= 3) BlurMaskFilter(r * 0.7f, BlurMaskFilter.Blur.NORMAL) else null
        val kx = ax / scale
        val ky = ay / scale
        fog.visit(level, x0, y0, x1, y1) { x, y, count ->
            val strength = if (level == Grid.Z) 1.0 else min(1.0, 0.3 + sqrt(count.toDouble() / capacity) * 1.15)
            clearPaint.alpha = (strength * 255).toInt()
            val cx = (((x + 0.5) / n - mx0) * kx).toFloat()
            val cy = (((y + 0.5) / n - my0) * ky).toFloat()
            fc.drawCircle(cx, cy, r, clearPaint)
        }
    }

    /**
     * Borders of the lands as dashed gilt lines, the walker's own land drawn bolder. Names stay off
     * the map; tapping a land shows its card.
     */
    private fun drawLands(c: Canvas, s: Scene, w: Int, h: Int, mx0: Double, mx1: Double, my0: Double, my1: Double) {
        val n = 1 shl Grid.SECTOR_Z
        val sidePx = ax / n
        landsShown = sidePx >= 70 * density
        if (!landsShown) return
        val x0 = floor(mx0 * n).toInt()
        val x1 = floor(mx1 * n).toInt()
        val y0 = floor(my0 * n).toInt()
        val y1 = floor(my1 * n).toInt()
        if ((x1 - x0 + 1).toLong() * (y1 - y0 + 1) > 400) return
        val d = density
        border.color = s.gold
        border.alpha = 150
        border.strokeWidth = 1.6f * d
        border.pathEffect = android.graphics.DashPathEffect(floatArrayOf(9 * d, 6 * d), 0f)
        for (x in x0..x1 + 1) {
            val px = (ax * x / n + bx).toFloat()
            c.drawLine(px, 0f, px, h.toFloat(), border)
        }
        for (y in y0..y1 + 1) {
            val py = (ay * y / n + by).toFloat()
            c.drawLine(0f, py, w.toFloat(), py, border)
        }
        val here = s.fix?.takeIf { it.time > 0 }?.let { Grid.sector(Grid.cell(it.lat, it.lon)) }
        if (here != null) {
            val hx = Grid.x(here)
            val hy = Grid.y(here)
            border.alpha = 235
            border.strokeWidth = 3f * d
            border.pathEffect = android.graphics.DashPathEffect(floatArrayOf(14 * d, 6 * d), 0f)
            c.drawRect((ax * hx / n + bx).toFloat(), (ay * hy / n + by).toFloat(), (ax * (hx + 1) / n + bx).toFloat(), (ay * (hy + 1) / n + by).toFloat(), border)
        }
        border.pathEffect = null
        // The land picked on the map, washed in gold.
        s.picked?.let { k ->
            val px = Grid.x(k)
            val py = Grid.y(k)
            val r = RectF((ax * px / n + bx).toFloat(), (ay * py / n + by).toFloat(), (ax * (px + 1) / n + bx).toFloat(), (ay * (py + 1) / n + by).toFloat())
            fill.shader = null
            fill.color = withAlpha(s.gold, 40)
            c.drawRect(r, fill)
            border.color = s.gold
            border.alpha = 255
            border.strokeWidth = 3f * d
            c.drawRect(r, border)
        }
    }

    private fun metersPx(lat: Double) = (ax / Grid.N / Grid.cellMeters(lat)).toFloat()

    /** The treasure map's circle: a dashed gilt ring with a faint wash and a "?" at its heart. */
    private fun drawTreasure(c: Canvas, s: Scene) {
        val t = s.treasure ?: return
        val x = sx(t.cLon)
        val y = sy(t.cLat)
        val d = density
        val r = (t.radius.toFloat() * metersPx(t.cLat)).coerceAtLeast(10 * d)
        fill.shader = null
        fill.color = withAlpha(s.gold, 34)
        c.drawCircle(x, y, r, fill)
        stroke.pathEffect = android.graphics.DashPathEffect(floatArrayOf(10 * d, 7 * d), 0f)
        stroke.strokeWidth = 2.4f * d
        stroke.color = s.gold
        stroke.alpha = 230
        c.drawCircle(x, y, r, stroke)
        stroke.pathEffect = null
        text.color = s.gold
        text.textSize = 20 * d
        text.setShadowLayer(4 * d, 0f, 0f, 0x88FFFFFF.toInt())
        c.drawText("?", x, y + 7 * d, text)
        text.clearShadowLayer()
        hits += Pick.OfTreasure to floatArrayOf(x, y)
    }

    /** The raced route, the part already retraced in ink, the start and finish, and the shadow on its way. */
    private fun drawRace(c: Canvas, s: Scene, now: Long) {
        val r = s.race ?: return
        if (r.route.size < 2) return
        val d = density
        path.reset()
        r.route.forEachIndexed { i, (lat, lon) -> if (i == 0) path.moveTo(sx(lon), sy(lat)) else path.lineTo(sx(lon), sy(lat)) }
        stroke.strokeCap = Paint.Cap.ROUND
        stroke.strokeJoin = Paint.Join.ROUND
        stroke.pathEffect = android.graphics.DashPathEffect(floatArrayOf(2 * d, 7 * d), 0f)
        stroke.strokeWidth = 4.5f * d
        stroke.color = 0xFF5E5A66.toInt()
        stroke.alpha = 200
        c.drawPath(path, stroke)
        stroke.pathEffect = null
        if (r.started > 0 && r.reached > 0) {
            path.reset()
            for (i in 0..r.reached) {
                val (lat, lon) = r.route[i]
                if (i == 0) path.moveTo(sx(lon), sy(lat)) else path.lineTo(sx(lon), sy(lat))
            }
            stroke.color = s.accent
            stroke.alpha = 230
            stroke.strokeWidth = 5 * d
            c.drawPath(path, stroke)
        }
        fun dot(p: Pair<Double, Double>, color: Int) {
            fill.shader = null
            fill.color = 0xFFFFFFFF.toInt()
            c.drawCircle(sx(p.second), sy(p.first), 8 * d, fill)
            fill.color = color
            c.drawCircle(sx(p.second), sy(p.first), 5.5f * d, fill)
        }
        dot(r.route.first(), 0xFF5E8C4A.toInt())
        dot(r.route.last(), s.gold)
        if (r.started > 0) {
            val (lat, lon) = Races.pointAt(r.route, r.along, r.shadowM(now))
            val x = sx(lon)
            val y = sy(lat)
            // The shadow: the walker's token in smoke.
            glow.shader = RadialGradient(x, y, 26 * d, intArrayOf(0x995E5A66.toInt(), 0x005E5A66), null, Shader.TileMode.CLAMP)
            c.drawCircle(x, y, 26 * d, glow)
            glow.shader = null
            fill.color = 0xCC3C3946.toInt()
            c.drawCircle(x, y, 11 * d, fill)
            fill.color = 0x99D8D4E0.toInt()
            c.drawCircle(x, y, 6 * d, fill)
        }
    }

    /** A dashed ring around the tapped chest or shrine: step inside it and it is yours. */
    private fun drawReach(c: Canvas, s: Scene) {
        val (lat, lon, m) = s.reach ?: return
        val x = sx(lon)
        val y = sy(lat)
        val d = density
        val r = m.toFloat() * metersPx(lat)
        if (r < 6 * d) return
        fill.shader = null
        fill.color = withAlpha(s.accent, 26)
        c.drawCircle(x, y, r, fill)
        stroke.pathEffect = android.graphics.DashPathEffect(floatArrayOf(8 * d, 6 * d), 0f)
        stroke.strokeWidth = 2.2f * d
        stroke.color = s.accent
        stroke.alpha = 220
        c.drawCircle(x, y, r, stroke)
        stroke.pathEffect = null
    }

    private fun drawTrail(c: Canvas, s: Scene) {
        if (s.trail.size < 2) return
        path.reset()
        s.trail.forEachIndexed { i, (lat, lon) ->
            if (i == 0) path.moveTo(sx(lon), sy(lat)) else path.lineTo(sx(lon), sy(lat))
        }
        stroke.strokeWidth = 4 * density
        stroke.strokeCap = Paint.Cap.ROUND
        stroke.strokeJoin = Paint.Join.ROUND
        stroke.color = s.accent
        stroke.alpha = 170
        stroke.pathEffect = null
        c.drawPath(path, stroke)
    }

    private fun drawCaches(c: Canvas, s: Scene, mx0: Double, mx1: Double, my0: Double, my1: Double, t: Long) {
        val tiles = 1 shl Caches.Z
        // Caches show from street level only; farther out they would be a rash of dots.
        if (ax / tiles < 60 * density) return
        val list = Caches.inBox(floor(mx0 * tiles).toInt(), floor(my0 * tiles).toInt(), floor(mx1 * tiles).toInt(), floor(my1 * tiles).toInt(), s.week)
        val me = s.fix
        for (cache in list) {
            if (cache.key in s.opened) continue
            val revealed = Grid.cell(cache.lat, cache.lon) in fog
            val told = cache.key == s.rumor
            val near = me != null && me.accuracy < 200 && Grid.distance(me.lat, me.lon, cache.lat, cache.lon) <= s.senseM
            if (!revealed && !s.radar && !near && !told) continue
            val x = sx(cache.lon)
            val y = sy(cache.lat)
            val color = s.rarityColors[cache.rarity.ordinal]
            val phase = ((t + cache.key.hashCode()) % 1600) / 1600.0
            val pulse = (0.5 + 0.5 * sin(phase * 2 * PI)).toFloat()
            if (revealed || told) {
                drawGem(c, x, y, color, Math.floorMod(t + cache.key.hashCode(), 3000L) / 3000f, cache.rarity)
                hits += Pick.OfCache(cache, false) to floatArrayOf(x, y)
            } else if (s.radar) {
                // The stone shows where a chest lies in the mist, not what it holds.
                drawGem(c, x, y, UNKNOWN, Math.floorMod(t + cache.key.hashCode(), 3000L) / 3000f, Rarity.COMMON)
                hits += Pick.OfCache(cache, true) to floatArrayOf(x, y)
            } else {
                // Felt, not seen: a shimmer with a question mark.
                val r = (14 + 6 * pulse) * density
                glow.shader = RadialGradient(x, y, r * 1.6f, intArrayOf(withAlpha(color, 150), withAlpha(color, 0)), null, Shader.TileMode.CLAMP)
                c.drawCircle(x, y, r * 1.6f, glow)
                glow.shader = null
                text.color = 0xFFFFFFFF.toInt()
                text.textSize = 15 * density
                text.setShadowLayer(4 * density, 0f, 0f, 0x99000000.toInt())
                c.drawText("?", x, y + 5 * density, text)
                text.clearShadowLayer()
                hits += Pick.OfCache(cache, true) to floatArrayOf(x, y)
            }
        }
    }

    private fun drawGem(c: Canvas, x: Float, y: Float, color: Int, pulse: Float, rarity: Rarity) {
        val d = density
        val size = (10 + rarity.ordinal * 1.5f) * d
        // A halo in the chest's rarity, breathing.
        glow.shader = RadialGradient(x, y, size * 3f, intArrayOf(withAlpha(color, (110 + 90 * pulse).toInt()), withAlpha(color, 0)), null, Shader.TileMode.CLAMP)
        c.drawCircle(x, y, size * 3f, glow)
        glow.shader = null
        // A wooden chest bound in bands of its rarity, with a gold lock.
        val body = RectF(x - size, y - size * 0.3f, x + size, y + size * 0.8f)
        val lid = RectF(x - size, y - size * 0.95f, x + size, y - size * 0.15f)
        fill.shader = null
        fill.color = 0x66000000
        c.drawRoundRect(RectF(body.left + 1.5f * d, body.top + 2.5f * d, body.right + 1.5f * d, body.bottom + 3f * d), 3 * d, 3 * d, fill)
        fill.color = 0xFF6E4320.toInt()
        c.drawRoundRect(body, 2.5f * d, 2.5f * d, fill)
        fill.color = 0xFF8C5A2E.toInt()
        c.drawRoundRect(lid, size * 0.5f, size * 0.5f, fill)
        fill.color = color
        val band = size * 0.22f
        for (bx in floatArrayOf(x - size * 0.62f, x + size * 0.62f - band)) {
            c.drawRect(bx, lid.top + 1.5f * d, bx + band, body.bottom, fill)
        }
        c.drawRect(body.left, body.top, body.right, body.top + band * 0.8f, fill)
        stroke.pathEffect = null
        stroke.strokeWidth = 1.3f * d
        stroke.color = 0xFF3A2410.toInt()
        stroke.alpha = 255
        c.drawRoundRect(body, 2.5f * d, 2.5f * d, stroke)
        c.drawRoundRect(lid, size * 0.5f, size * 0.5f, stroke)
        fill.color = 0xFFF2C14E.toInt()
        c.drawRoundRect(RectF(x - size * 0.2f, y - size * 0.32f, x + size * 0.2f, y + size * 0.2f), 2 * d, 2 * d, fill)
        fill.color = 0xFF3A2410.toInt()
        c.drawCircle(x, y - size * 0.08f, size * 0.07f, fill)
        if (rarity.ordinal >= 2) {
            // Epic and legendary chests throw off motes of light.
            fill.color = 0xFFFFF3C4.toInt()
            for (i in 0 until 3) {
                val a = (pulse + i / 3f) * 2 * PI
                c.drawCircle(x + (size * 1.4f * kotlin.math.cos(a)).toFloat(), y - size * 0.4f + (size * 0.9f * sin(a * 1.3)).toFloat(), 1.6f * d, fill)
            }
        }
    }

    private fun drawBeacon(c: Canvas, s: Scene, t: Long) {
        val b = s.beacon ?: return
        val x = sx(b.second)
        val y = sy(b.first)
        val d = density
        val color = if (s.beaconReached) s.accent else 0xFF7FB8FF.toInt()
        // Runic rings ripple out from the shrine.
        for (i in 0 until 3) {
            val p = (((t / 2400.0) + i / 3.0) % 1.0).toFloat()
            stroke.pathEffect = android.graphics.DashPathEffect(floatArrayOf(6 * d, 4 * d), p * 40 * d)
            stroke.strokeWidth = 2.2f * d
            stroke.color = color
            stroke.alpha = ((1 - p) * 220).toInt()
            c.drawOval(RectF(x - (12 + p * 50) * d, y - (6 + p * 25) * d, x + (12 + p * 50) * d, y + (6 + p * 25) * d), stroke)
        }
        stroke.pathEffect = null
        // A pillar of light rising into the sky.
        glow.shader = android.graphics.LinearGradient(x, y - 90 * d, x, y, withAlpha(color, 0), withAlpha(color, 170), Shader.TileMode.CLAMP)
        c.drawRect(x - 5 * d, y - 90 * d, x + 5 * d, y, glow)
        glow.shader = RadialGradient(x, y - 14 * d, 34 * d, intArrayOf(withAlpha(color, 190), withAlpha(color, 0)), null, Shader.TileMode.CLAMP)
        c.drawCircle(x, y - 14 * d, 34 * d, glow)
        glow.shader = null
        // Stone plinth.
        fill.shader = null
        fill.color = 0xFF6B6259.toInt()
        c.drawRoundRect(RectF(x - 10 * d, y - 3 * d, x + 10 * d, y + 4 * d), 2 * d, 2 * d, fill)
        // The floating crystal.
        val bob = (sin(t / 500.0) * 2.5 * d).toFloat()
        path.reset()
        path.moveTo(x, y - 34 * d + bob)
        path.lineTo(x + 8 * d, y - 18 * d + bob)
        path.lineTo(x, y - 6 * d + bob)
        path.lineTo(x - 8 * d, y - 18 * d + bob)
        path.close()
        fill.color = color
        c.drawPath(path, fill)
        fill.color = 0xAAFFFFFF.toInt()
        path.reset()
        path.moveTo(x, y - 34 * d + bob)
        path.lineTo(x + 3 * d, y - 18 * d + bob)
        path.lineTo(x, y - 6 * d + bob)
        path.close()
        c.drawPath(path, fill)
        stroke.strokeWidth = 1.2f * d
        stroke.color = 0xFFFFFFFF.toInt()
        stroke.alpha = 200
        path.reset()
        path.moveTo(x, y - 34 * d + bob)
        path.lineTo(x + 8 * d, y - 18 * d + bob)
        path.lineTo(x, y - 6 * d + bob)
        path.lineTo(x - 8 * d, y - 18 * d + bob)
        path.close()
        c.drawPath(path, stroke)
        hits += Pick.OfBeacon to floatArrayOf(x, y - 16 * d)
    }

    private fun drawWalker(c: Canvas, s: Scene) {
        val f = s.fix ?: return
        if (f.time == 0L) return
        val x = sx(f.lon)
        val y = sy(f.lat)
        val d = density
        val metersPx = (ax / Grid.N / Grid.cellMeters(f.lat)).toFloat()
        if (f.accuracy in 15f..2000f) {
            fill.shader = null
            fill.color = withAlpha(s.accent, 34)
            c.drawCircle(x, y, f.accuracy * metersPx, fill)
        }
        f.bearing?.let { b ->
            glow.shader = RadialGradient(x, y, 34 * d, intArrayOf(withAlpha(s.accent, 140), withAlpha(s.accent, 0)), null, Shader.TileMode.CLAMP)
            c.save()
            c.rotate(b, x, y)
            path.reset()
            path.moveTo(x, y)
            path.lineTo(x - 18 * d, y - 32 * d)
            path.quadTo(x, y - 40 * d, x + 18 * d, y - 32 * d)
            path.close()
            c.drawPath(path, glow)
            c.restore()
            glow.shader = null
        }
        // The traveller: a gilt token with an emerald heart.
        fill.shader = null
        fill.color = 0x50000000
        c.drawCircle(x, y + 2f * d, 12 * d, fill)
        fill.color = s.gold
        c.drawCircle(x, y, 11.5f * d, fill)
        fill.color = 0xFFFFF3D0.toInt()
        c.drawCircle(x, y, 9 * d, fill)
        fill.color = s.accent
        c.drawCircle(x, y, 6.5f * d, fill)
    }

    override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
        val r = 30 * density
        val hit = hits.minByOrNull { (_, p) -> (p[0] - e.x) * (p[0] - e.x) + (p[1] - e.y) * (p[1] - e.y) }
            ?.takeIf { (_, p) -> (p[0] - e.x) * (p[0] - e.x) + (p[1] - e.y) * (p[1] - e.y) <= r * r }
        if (hit != null) {
            onPick(hit.first)
            return true
        }
        // Between markers a tap names the land beneath it; tapping the picked land again lets it go.
        if (!landsShown) {
            onPick(null)
            return false
        }
        val g = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt())
        val land = Grid.sector(Grid.cell(g.latitude, g.longitude))
        onPick(if (scene?.picked == land) null else Pick.OfLand(land))
        return true
    }

    override fun onLongPress(e: MotionEvent, mapView: MapView): Boolean {
        val cb = onLongPress ?: return false
        val p = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt())
        cb(p.latitude, p.longitude)
        return true
    }

    override fun onDetach(mapView: MapView?) {
        fogBitmap?.recycle()
        fogBitmap = null
        super.onDetach(mapView)
    }

    private companion object {
        /** A chest of unknown worth: bands of weathered iron. */
        const val UNKNOWN = 0xFF8E8A84.toInt()
    }

    private fun withAlpha(color: Int, a: Int) = (color and 0x00FFFFFF) or (a.coerceIn(0, 255) shl 24)

    private fun lighten(color: Int): Int {
        fun ch(s: Int) = (((color shr s) and 255) + (255 - ((color shr s) and 255)) * 0.3).toInt()
        return (0xFF shl 24) or (ch(16) shl 16) or (ch(8) shl 8) or ch(0)
    }
}
