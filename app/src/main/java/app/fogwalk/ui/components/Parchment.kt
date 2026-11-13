package app.fogwalk.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.res.imageResource
import app.fogwalk.R
import app.fogwalk.ui.theme.U

/**
 * A panel of the atlas instead of a plain white card: old paper, a gilt edge with a fine inner rule a little inside
 * it, and a gold lozenge in each corner of the rule. [selected] draws the edge in the accent, thicker.
 */
@Composable
fun Modifier.frame(shape: androidx.compose.ui.graphics.Shape = U.RMd, selected: Boolean = false, elevation: androidx.compose.ui.unit.Dp = 0.dp): Modifier {
    val gilt = U.c.gilt
    val edge = if (selected) U.c.accent else gilt
    val gold = U.c.gold
    return this
        .then(if (elevation > 0.dp) Modifier.shadow(elevation, shape) else Modifier)
        .clip(shape)
        .background(U.c.surface)
        .parchment(0.32f)
        .drawBehind {
            val inset = 4.dp.toPx()
            val r = 6.dp.toPx()
            drawRoundRect(
                gilt.copy(alpha = 0.55f), topLeft = Offset(inset, inset), size = Size(size.width - 2 * inset, size.height - 2 * inset),
                cornerRadius = CornerRadius(r), style = Stroke(0.8.dp.toPx()),
            )
            // Lozenges where the rule turns its corners.
            val d = 3.dp.toPx()
            val k = inset + 2.dp.toPx()
            for ((x, y) in listOf(k to k, size.width - k to k, k to size.height - k, size.width - k to size.height - k)) {
                val p = Path().apply { moveTo(x, y - d); lineTo(x + d, y); lineTo(x, y + d); lineTo(x - d, y); close() }
                drawPath(p, gold)
            }
        }
        .border(if (selected) 2.dp else 1.5.dp, edge, shape)
}

/**
 * A round brass medallion, for the map's buttons and the chosen tab: a gilt rim catching the light from the top-left,
 * a dark groove, and old paper in the middle.
 */
@Composable
fun Modifier.medallion(elevation: androidx.compose.ui.unit.Dp = 6.dp, bright: Boolean = false): Modifier {
    val gold = U.c.gold
    val rimLight = androidx.compose.ui.graphics.Color(0xFFF6DC8C)
    val rimDark = androidx.compose.ui.graphics.Color(0xFF8A6420)
    val paper = if (bright) U.c.goldSoft else U.c.surface
    val shape = androidx.compose.foundation.shape.CircleShape
    return this
        .then(if (elevation > 0.dp) Modifier.shadow(elevation, shape) else Modifier)
        .clip(shape)
        .drawBehind {
            val r = size.minDimension / 2
            // The rim: light at the top-left, dark at the bottom-right.
            drawCircle(androidx.compose.ui.graphics.Brush.linearGradient(listOf(rimLight, gold, rimDark), Offset.Zero, Offset(size.width, size.height)), r)
            drawCircle(androidx.compose.ui.graphics.Color(0x663B2A1A), r * 0.86f)
            drawCircle(paper, r * 0.82f)
        }
}

/** Old paper under a panel: the painted parchment tiled faintly over its colour. Plain in the dark theme. */
@Composable
fun Modifier.parchment(alpha: Float = 0.45f): Modifier {
    if (U.c.dark) return this
    val tex: ImageBitmap = ImageBitmap.imageResource(R.drawable.parchment)
    val brush = remember(tex) { ShaderBrush(ImageShader(tex, TileMode.Repeated, TileMode.Repeated)) }
    return this.drawBehind { drawRect(brush, alpha = alpha) }
}
