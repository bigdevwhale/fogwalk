package app.fogwalk.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.fogwalk.ui.theme.U
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** A burst of falling confetti for a reached goal; plays once for [key]. */
@Composable
fun Confetti(key: Any, modifier: Modifier = Modifier) {
    val colors = listOf(U.c.accent, U.c.gold, U.c.sage, U.c.sky, U.c.rose, U.c.violet)
    val pieces = remember(key) {
        val rnd = Random(key.hashCode())
        List(90) { Piece(rnd.nextFloat(), -rnd.nextFloat() * 0.4f, 0.25f + rnd.nextFloat() * 0.5f, rnd.nextFloat() * 360f, (rnd.nextFloat() - 0.5f) * 0.3f, colors[it % colors.size]) }
    }
    var elapsed by remember(key) { mutableLongStateOf(0L) }
    LaunchedEffect(key) {
        val start = withFrameMillis { it }
        while (elapsed < 3200) {
            elapsed = withFrameMillis { it } - start
        }
    }
    if (elapsed >= 3200) return
    Canvas(modifier) {
        val t = elapsed / 1000f
        val fade = (1f - (t - 2.2f).coerceAtLeast(0f)).coerceIn(0f, 1f)
        pieces.forEach { p ->
            val y = (p.y + p.speed * t * t * 0.6f + p.speed * t * 0.4f) * size.height
            val x = (p.x + p.drift * t + 0.02f * sin(t * 4 + p.rot)) * size.width
            rotate(p.rot + t * 220f * p.drift * 6, Offset(x, y)) {
                drawRect(p.color.copy(alpha = fade), Offset(x - 5f, y - 9f), Size(10f, 18f))
            }
        }
    }
}

private class Piece(val x: Float, val y: Float, val speed: Float, val rot: Float, val drift: Float, val color: Color)
