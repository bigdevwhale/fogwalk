package app.fogwalk.ui.map

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.random.Random

/** How a fog style looks: a base colour, a lighter cloud colour and optional sparkles. */
class FogLook(
    val base: Int,
    val cloud: Int,
    /** Second cloud colour mixed in by a slower noise (aurora bands, ember glow). */
    val accent: Int? = null,
    val sparkles: Int? = null,
    val sparkleCount: Int = 0,
    /** Colour of the soft rim drawn where fog meets explored ground. */
    val rim: Int,
)

object FogStyles {
    fun look(id: String): FogLook = when (id) {
        "parchment" -> FogLook(0xFFCDB38A.toInt(), 0xFFE7D5B0.toInt(), accent = 0xFFB08F5E.toInt(), rim = 0xFF8C6B3E.toInt())
        "snow" -> FogLook(0xFFD5DEE6.toInt(), 0xFFFFFFFF.toInt(), sparkles = 0xFFFFFFFF.toInt(), sparkleCount = 90, rim = 0xFF9FB4C6.toInt())
        "midnight" -> FogLook(0xFF161D33.toInt(), 0xFF26304F.toInt(), sparkles = 0xFFE8ECFF.toInt(), sparkleCount = 70, rim = 0xFF5C6BC0.toInt())
        "ember" -> FogLook(0xFF2E211D.toInt(), 0xFF4A302A.toInt(), accent = 0xFFB4532A.toInt(), sparkles = 0xFFFFA04D.toInt(), sparkleCount = 40, rim = 0xFFE5672A.toInt())
        "aurora" -> FogLook(0xFF15283A.toInt(), 0xFF1F3F55.toInt(), accent = 0xFF3FBF9A.toInt(), sparkles = 0xFFB9A8FF.toInt(), sparkleCount = 40, rim = 0xFF4FD1BC.toInt())
        else -> FogLook(0xFF8F979E.toInt(), 0xFFB7BEC4.toInt(), rim = 0xFF5F6870.toInt())
    }

    private val textures = HashMap<String, Bitmap>()

    /** A seamless cloudy texture for the style, baked once. */
    @Synchronized
    fun texture(id: String): Bitmap = textures.getOrPut(id) { bake(look(id)) }

    private const val SIZE = 256

    private fun bake(look: FogLook): Bitmap {
        val px = IntArray(SIZE * SIZE)
        val n1 = ValueNoise(7, 8)
        val n2 = ValueNoise(13, 16)
        val n3 = ValueNoise(29, 4)
        for (y in 0 until SIZE) for (x in 0 until SIZE) {
            val u = x.toDouble() / SIZE
            val v = y.toDouble() / SIZE
            val clouds = (n1.at(u, v) * 0.6 + n2.at(u, v) * 0.4).pow(1.4)
            var c = mix(look.base, look.cloud, clouds)
            if (look.accent != null) {
                val band = ((n3.at(u, v) - 0.45) * 2.2).coerceIn(0.0, 1.0)
                c = mix(c, look.accent, band * 0.55)
            }
            px[y * SIZE + x] = c
        }
        val sparkle = look.sparkles
        if (sparkle != null) {
            val rnd = Random(look.base)
            repeat(look.sparkleCount) {
                val sx = rnd.nextInt(SIZE)
                val sy = rnd.nextInt(SIZE)
                val big = rnd.nextInt(5) == 0
                val strength = 0.45 + rnd.nextDouble() * 0.55
                for (dy in -2..2) for (dx in -2..2) {
                    val d = sqrt((dx * dx + dy * dy).toDouble())
                    val a = (if (big) 1.0 - d / 2.4 else 1.0 - d / 1.3).coerceAtLeast(0.0) * strength
                    if (a <= 0) continue
                    val i = Math.floorMod(sy + dy, SIZE) * SIZE + Math.floorMod(sx + dx, SIZE)
                    px[i] = mix(px[i], sparkle, a)
                }
            }
        }
        return Bitmap.createBitmap(px, SIZE, SIZE, Bitmap.Config.ARGB_8888)
    }

    private fun mix(a: Int, b: Int, t: Double): Int {
        val k = t.coerceIn(0.0, 1.0)
        fun ch(s: Int) = (((a shr s) and 255) * (1 - k) + ((b shr s) and 255) * k).toInt()
        return Color.argb(255, ch(16), ch(8), ch(0))
    }
}

/** Tileable value noise with smooth interpolation; [period] lattice cells across the texture. */
private class ValueNoise(seed: Int, private val period: Int) {
    private val grid = Random(seed).let { r -> DoubleArray(period * period) { r.nextDouble() } }

    private fun g(x: Int, y: Int) = grid[Math.floorMod(y, period) * period + Math.floorMod(x, period)]

    fun at(u: Double, v: Double): Double {
        val x = u * period
        val y = v * period
        val x0 = floor(x).toInt()
        val y0 = floor(y).toInt()
        val fx = smooth(x - x0)
        val fy = smooth(y - y0)
        val a = g(x0, y0) + (g(x0 + 1, y0) - g(x0, y0)) * fx
        val b = g(x0, y0 + 1) + (g(x0 + 1, y0 + 1) - g(x0, y0 + 1)) * fx
        return a + (b - a) * fy
    }

    private fun smooth(t: Double) = t * t * (3 - 2 * t)
}
