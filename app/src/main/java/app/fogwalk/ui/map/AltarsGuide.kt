package app.fogwalk.ui.map

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import app.fogwalk.R
import app.fogwalk.ui.theme.U
import app.fogwalk.ui.theme.UType
import kotlinx.coroutines.launch

private const val PAGES = 4
private val Azure = Color(0xFF7FB8FF)

/**
 * How altars work, a step to a page with a drawing over each (#55): one long text was too much to
 * take in at once. The drawings are painted here rather than screenshots, so they follow the theme,
 * the language and the numbers.
 */
@Composable
fun AltarsGuide(min: Int, side: String, accuracy: String, onDismiss: () -> Unit) {
    val pager = rememberPagerState { PAGES }
    val scope = rememberCoroutineScope()
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.padding(horizontal = 16.dp).fillMaxWidth().clip(U.RLg).background(U.c.bg)
                .border(1.5.dp, U.c.gilt, U.RLg).padding(18.dp),
        ) {
            Text(stringResource(R.string.altars_guide_t), style = UType.Strong.copy(fontSize = 19.sp))
            Spacer(Modifier.height(12.dp))
            HorizontalPager(pager, Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) { page ->
                val (title, text) = when (page) {
                    0 -> stringResource(R.string.altars_guide1_t) to stringResource(R.string.altars_guide1)
                    1 -> stringResource(R.string.altars_guide2_t) to stringResource(R.string.altars_guide2)
                    2 -> stringResource(R.string.altars_guide3_t) to stringResource(R.string.altars_guide3, min, side, accuracy)
                    else -> stringResource(R.string.altars_guide4_t) to stringResource(R.string.altars_guide4, side)
                }
                Column(Modifier.fillMaxWidth().height(430.dp).verticalScroll(rememberScrollState())) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(1.6f).clip(U.RMd).border(1.dp, U.c.gilt.copy(alpha = 0.6f), U.RMd),
                    ) { Picture(page, side) }
                    Spacer(Modifier.height(14.dp))
                    Text(stringResource(R.string.altars_step, page + 1, PAGES).uppercase(), style = UType.Eyebrow.copy(color = U.c.gold))
                    Spacer(Modifier.height(4.dp))
                    Text(title, style = UType.Strong.copy(fontSize = 18.sp))
                    Spacer(Modifier.height(6.dp))
                    Text(text, style = UType.Sub)
                }
            }
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center) {
                repeat(PAGES) { i ->
                    val on = i == pager.currentPage
                    Box(Modifier.padding(5.dp).size(if (on) 10.dp else 7.dp).rotate(45f).background(if (on) U.c.gold else U.c.line))
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                val first = pager.currentPage == 0
                val last = pager.currentPage == PAGES - 1
                TextButton(onClick = { if (first) onDismiss() else scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }) {
                    Text(stringResource(if (first) R.string.close else R.string.back), color = U.c.muted, style = UType.Button)
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = { if (last) onDismiss() else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) {
                    Text(stringResource(if (last) R.string.altars_got_it else R.string.next), color = U.c.accent, style = UType.Button)
                }
            }
        }
    }
}

/** The drawing for a step. */
@Composable
private fun Picture(page: Int, side: String) {
    if (page == 1) {
        // Where altars come from: the azure shrine, and the merchant.
        Box(Modifier.fillMaxWidth().aspectRatio(1.6f).background(U.c.surface), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.shrine_azure), null, Modifier.size(96.dp))
                Spacer(Modifier.width(10.dp))
                Text("→", style = UType.Strong.copy(fontSize = 30.sp, color = U.c.muted))
                Spacer(Modifier.width(10.dp))
                Box(contentAlignment = Alignment.TopEnd) {
                    Image(painterResource(R.drawable.item_altar), null, Modifier.size(84.dp))
                    Text(
                        "+1", style = UType.Strong.copy(color = Color.White, fontSize = 14.sp),
                        modifier = Modifier.clip(U.RMd).background(Azure).padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
            }
        }
        return
    }
    val paper = U.c.surface
    val ink = U.c.ink
    val muted = U.c.muted
    val mist = if (U.c.dark) Color(0xFF3A3642) else Color(0xFFB9B2A6)
    Canvas(Modifier.fillMaxWidth().aspectRatio(1.6f).background(paper)) {
        val w = size.width
        val h = size.height
        val u = h / 100f
        // The closed ground: a pond behind a fence, with a road walked around it.
        val corners = listOf(Offset(w * 0.30f, h * 0.24f), Offset(w * 0.72f, h * 0.20f), Offset(w * 0.76f, h * 0.76f), Offset(w * 0.27f, h * 0.80f))
        val ring = Path().apply { corners.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }; close() }
        val road = Path().apply {
            val m = 9 * u
            moveTo(corners[0].x - m, corners[0].y - m); lineTo(corners[1].x + m, corners[1].y - m)
            lineTo(corners[2].x + m, corners[2].y + m); lineTo(corners[3].x - m, corners[3].y + m); close()
        }
        pond(corners)
        drawPath(ring, Color(0xFF8C5A2E), style = Stroke(2 * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3 * u, 3 * u))))
        // Mist over all of it; a closed ring lifts it inside.
        val fog = Brush.radialGradient(listOf(mist.copy(alpha = 0.92f), mist.copy(alpha = 0.78f)), center = Offset(w / 2, h / 2), radius = w * 0.6f)
        if (page == 3) clipPath(ring, ClipOp.Difference) { drawRect(fog) } else drawRect(fog)
        // The road walked around the fence is clear of it.
        drawPath(road, paper, style = Stroke(9 * u, join = StrokeJoin.Round))
        drawPath(road, muted.copy(alpha = 0.3f), style = Stroke(5 * u, join = StrokeJoin.Round))
        when (page) {
            0 -> {
                // The walker stops at the fence: there is no way in.
                walker(Offset(w * 0.15f, h * 0.52f), u)
                drawLine(Color(0xFFD9534F), Offset(w * 0.20f, h * 0.44f), Offset(w * 0.25f, h * 0.60f), 2.5f * u, StrokeCap.Round)
                drawLine(Color(0xFFD9534F), Offset(w * 0.25f, h * 0.44f), Offset(w * 0.20f, h * 0.60f), 2.5f * u, StrokeCap.Round)
            }
            2 -> {
                // Three set and joined; the side that would close the ring still faint.
                val set = corners.take(3)
                for (i in 0 until set.size - 1) drawLine(Azure, set[i], set[i + 1], 2.5f * u, StrokeCap.Round)
                drawLine(Azure.copy(alpha = 0.6f), set.last(), corners[3], 1.5f * u, StrokeCap.Round, PathEffect.dashPathEffect(floatArrayOf(3 * u, 3 * u)))
                set.forEach { altar(it, u) }
                walker(Offset(corners[3].x + 4 * u, corners[3].y + 10 * u), u)
                label("≤ $side", Offset((corners[0].x + corners[1].x) / 2, corners[1].y - 2 * u), ink, u)
            }
            3 -> {
                drawPath(ring, Azure.copy(alpha = 0.16f))
                drawPath(ring, Azure, style = Stroke(2.5f * u))
                corners.forEach { altar(it, u) }
            }
        }
    }
}

private fun DrawScope.pond(c: List<Offset>) {
    val inset = size.height * 0.06f
    val p = Path().apply {
        moveTo(c[0].x + inset, c[0].y + inset * 1.4f)
        quadraticTo((c[0].x + c[1].x) / 2, c[0].y, c[1].x - inset, c[1].y + inset * 1.2f)
        quadraticTo(c[1].x + inset * 0.3f, (c[1].y + c[2].y) / 2, c[2].x - inset, c[2].y - inset)
        quadraticTo((c[2].x + c[3].x) / 2, c[2].y + inset * 0.4f, c[3].x + inset, c[3].y - inset)
        quadraticTo(c[3].x - inset * 0.3f, (c[3].y + c[0].y) / 2, c[0].x + inset, c[0].y + inset * 1.4f)
        close()
    }
    drawPath(p, Color(0xFF6FA8C8))
    drawPath(p, Color(0xFF4F86A6), style = Stroke(size.height * 0.012f))
}

/** A standing stone with a blue flame, as the altars stand on the map. */
private fun DrawScope.altar(at: Offset, u: Float) {
    drawOval(Color(0x55000000), Offset(at.x - 5 * u, at.y + 1 * u), Size(10 * u, 3.5f * u))
    drawRoundRect(Color(0xFF6B6259), Offset(at.x - 3.5f * u, at.y - 7 * u), Size(7 * u, 9 * u), androidx.compose.ui.geometry.CornerRadius(1.2f * u))
    drawCircle(Brush.radialGradient(listOf(Azure, Azure.copy(alpha = 0f)), center = Offset(at.x, at.y - 10 * u), radius = 7 * u), 7 * u, Offset(at.x, at.y - 10 * u))
    drawCircle(Color(0xFFDDEEFF), 1.8f * u, Offset(at.x, at.y - 9.5f * u))
}

/** The walker's token: gilt with an emerald heart. */
private fun DrawScope.walker(at: Offset, u: Float) {
    drawCircle(Color(0x50000000), 6 * u, at + Offset(0f, 1 * u))
    drawCircle(Color(0xFFE0B04A), 5.5f * u, at)
    drawCircle(Color(0xFFFFF3D0), 4.2f * u, at)
    drawCircle(Color(0xFF3E8E5E), 3 * u, at)
}

private fun DrawScope.label(text: String, at: Offset, ink: Color, u: Float) {
    drawContext.canvas.nativeCanvas.drawText(
        text, at.x, at.y,
        android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.argb((ink.alpha * 255).toInt(), (ink.red * 255).toInt(), (ink.green * 255).toInt(), (ink.blue * 255).toInt())
            textSize = 7 * u
            textAlign = android.graphics.Paint.Align.CENTER
        },
    )
}
