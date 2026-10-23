package app.fogwalk.ui.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.fogwalk.R
import app.fogwalk.container
import app.fogwalk.ui.components.PrimaryButton
import app.fogwalk.ui.map.FogStyles
import app.fogwalk.ui.theme.U
import app.fogwalk.ui.theme.UType
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val container = context.container
    // The last page asks about the pedometer, on phones that have one.
    val pedometer = container.steps.available
    val pages = if (pedometer) 4 else 3
    val pager = rememberPagerState { pages }
    val scope = rememberCoroutineScope()
    fun finish(steps: Boolean = false) {
        container.scope.launch { container.settings.update { it.copy(onboarded = true, steps = steps) } }
        onDone()
    }
    fun afterLocation() {
        if (pedometer) scope.launch { pager.animateScrollToPage(3) } else finish()
    }
    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { afterLocation() }
    val askSteps = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { finish(steps = it) }

    Column(
        Modifier.fillMaxSize()
            .background(Brush.radialGradient(listOf(U.c.surface, U.c.bg), radius = 1800f))
            .statusBarsPadding().navigationBarsPadding().padding(22.dp),
    ) {
        HorizontalPager(pager, Modifier.weight(1f)) { page ->
            Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center) {
                when (page) {
                    0 -> {
                        Framed { Atlas(Modifier.fillMaxWidth().aspectRatio(1.05f)) }
                        Spacer(Modifier.height(26.dp))
                        Text(stringResource(R.string.ob1_title), style = UType.Display)
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.ob1_text), style = UType.Sub)
                    }
                    1 -> {
                        Text(stringResource(R.string.ob2_title), style = UType.Display)
                        Spacer(Modifier.height(18.dp))
                        Feature(Icons.Rounded.Inventory2, U.c.gold, U.c.goldSoft, stringResource(R.string.ob2_caches), stringResource(R.string.ob2_caches_text))
                        Feature(Icons.Rounded.Flare, U.c.sky, U.c.skySoft, stringResource(R.string.ob2_beacon), stringResource(R.string.ob2_beacon_text))
                        Feature(Icons.Rounded.AutoAwesome, U.c.violet, U.c.violetSoft, stringResource(R.string.ob2_relics), stringResource(R.string.ob2_relics_text))
                        Feature(Icons.Rounded.LocalFireDepartment, U.c.flame, U.c.flameSoft, stringResource(R.string.ob2_streak), stringResource(R.string.ob2_streak_text))
                    }
                    2 -> {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CompassRose(Modifier.size(220.dp)) }
                        Spacer(Modifier.height(26.dp))
                        Text(stringResource(R.string.ob3_title), style = UType.Display)
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.ob3_text), style = UType.Sub)
                    }
                    else -> {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Box(
                                Modifier.size(160.dp).clip(CircleShape).background(U.c.sageSoft).border(2.dp, U.c.gilt, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.AutoMirrored.Rounded.DirectionsWalk, null, Modifier.size(84.dp), tint = U.c.sage) }
                        }
                        Spacer(Modifier.height(26.dp))
                        Text(stringResource(R.string.ob4_title), style = UType.Display)
                        Spacer(Modifier.height(10.dp))
                        Text(stringResource(R.string.ob4_text), style = UType.Sub)
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            repeat(pages) { i ->
                val on = i == pager.currentPage
                Box(
                    Modifier.padding(5.dp).size(if (on) 12.dp else 8.dp).rotate(45f)
                        .background(if (on) U.c.gold else U.c.line),
                )
            }
        }
        if (pager.currentPage < 2) {
            PrimaryButton(stringResource(R.string.next), { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }, Modifier.fillMaxWidth(), icon = Icons.AutoMirrored.Rounded.ArrowForward)
        } else if (pager.currentPage == 2) {
            PrimaryButton(stringResource(R.string.allow_location), {
                askLocation.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }, Modifier.fillMaxWidth(), icon = Icons.Rounded.MyLocation)
            TextButton(onClick = { afterLocation() }, Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.later), color = U.c.muted, style = UType.Button)
            }
        } else {
            PrimaryButton(stringResource(R.string.ob4_yes), {
                if (container.steps.permitted()) finish(steps = true) else askSteps.launch(Manifest.permission.ACTIVITY_RECOGNITION)
            }, Modifier.fillMaxWidth(), color = U.c.sage, icon = Icons.AutoMirrored.Rounded.DirectionsWalk)
            TextButton(onClick = { finish() }, Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.ob4_no), color = U.c.muted, style = UType.Button)
            }
        }
    }
}

/** A gilt double frame, like a map hung in a hall. */
@Composable
private fun Framed(content: @Composable () -> Unit) {
    Box(
        Modifier.fillMaxWidth().clip(U.RLg).border(2.dp, U.c.gilt, U.RLg).padding(5.dp)
            .clip(U.RMd).border(1.dp, U.c.gilt.copy(alpha = 0.6f), U.RMd),
    ) { content() }
}

@Composable
private fun Feature(icon: ImageVector, color: Color, soft: Color, title: String, text: String) {
    Row(Modifier.padding(vertical = 9.dp), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(48.dp).clip(CircleShape).background(soft).border(1.5.dp, U.c.gilt, CircleShape),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, Modifier.size(24.dp), tint = color) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = UType.Strong.copy(fontFamily = app.fogwalk.ui.theme.Rune))
            Spacer(Modifier.height(2.dp))
            Text(text, style = UType.Muted)
        }
    }
}

/**
 * A hand-drawn atlas of a little realm under mist: a traveller walks a road, the mist parts behind them,
 * and a chest glints at the end of the way. Loops forever.
 */
@Composable
private fun Atlas(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "walk")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart), label = "p")
    val mist = remember { FogStyles.texture("mist").asImageBitmap() }
    val parchment = Color(0xFFEBDAB2)
    val ink = Color(0xFF6B4E2E)
    val water = Color(0xFF8FB3B8)
    val forest = Color(0xFF7E9A5A)
    val accent = U.c.accent
    val gold = U.c.gold
    val route = remember {
        Path().apply {
            moveTo(0.08f, 0.78f); cubicTo(0.2f, 0.7f, 0.28f, 0.74f, 0.36f, 0.6f)
            cubicTo(0.44f, 0.46f, 0.3f, 0.36f, 0.46f, 0.3f); cubicTo(0.6f, 0.24f, 0.66f, 0.42f, 0.78f, 0.4f)
            cubicTo(0.86f, 0.38f, 0.86f, 0.24f, 0.84f, 0.18f)
        }
    }
    Box(modifier.background(parchment)) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // River.
            val river = Path().apply { moveTo(w * 0.55f, h); cubicTo(w * 0.5f, h * 0.75f, w * 0.7f, h * 0.65f, w * 0.62f, h * 0.5f); cubicTo(w * 0.55f, h * 0.38f, w, h * 0.45f, w, h * 0.3f) }
            drawPath(river, water, style = Stroke(w * 0.035f, cap = StrokeCap.Round))
            // Forest.
            listOf(0.18f to 0.25f, 0.24f to 0.2f, 0.13f to 0.33f, 0.22f to 0.32f, 0.3f to 0.26f, 0.7f to 0.75f, 0.78f to 0.8f, 0.74f to 0.86f, 0.84f to 0.72f).forEach { (x, y) ->
                drawCircle(forest, w * 0.035f, Offset(w * x, h * y))
                drawCircle(ink.copy(alpha = 0.5f), w * 0.035f, Offset(w * x, h * y), style = Stroke(1.5f))
                drawLine(ink, Offset(w * x, h * y + w * 0.03f), Offset(w * x, h * y + w * 0.055f), 2f)
            }
            // Mountains.
            listOf(0.52f to 0.12f, 0.62f to 0.1f, 0.42f to 0.14f).forEach { (x, y) ->
                val m = Path().apply { moveTo(w * (x - 0.07f), h * (y + 0.1f)); lineTo(w * x, h * y); lineTo(w * (x + 0.07f), h * (y + 0.1f)) }
                drawPath(m, Color(0xFFD9C394))
                drawPath(m, ink, style = Stroke(2.2f, join = StrokeJoin.Round))
                drawLine(ink, Offset(w * x, h * y), Offset(w * (x + 0.02f), h * (y + 0.05f)), 1.5f)
            }
            // A castle on the hill.
            castle(Offset(w * 0.16f, h * 0.55f), w * 0.09f, ink)
            // The road.
            val scaled = Path().apply { addPath(route); transform(androidx.compose.ui.graphics.Matrix().apply { scale(w, h) }) }
            drawPath(scaled, ink.copy(alpha = 0.7f), style = Stroke(3f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
            rose(Offset(w * 0.86f, h * 0.86f), w * 0.09f, ink, 0f)
        }
        // The mist, with the walked road cut out of it.
        Box(Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)) {
            Image(mist, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.9f)
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val scaled = Path().apply { addPath(route); transform(androidx.compose.ui.graphics.Matrix().apply { scale(w, h) }) }
                val measure = PathMeasure().apply { setPath(scaled, false) }
                val walked = Path()
                measure.getSegment(0f, measure.length * p, walked, true)
                // Feathered edge: wide faint strokes under a narrow solid one.
                listOf(0.24f to 0.25f, 0.2f to 0.45f, 0.16f to 0.8f, 0.12f to 1f).forEach { (width, a) ->
                    drawPath(walked, Color.Black.copy(alpha = a), style = Stroke(w * width, cap = StrokeCap.Round, join = StrokeJoin.Round), blendMode = BlendMode.DstOut)
                }
                val head = measure.getPosition(measure.length * p)
                drawCircle(Brush.radialGradient(listOf(Color.Black, Color.Transparent), head, w * 0.15f), w * 0.15f, head, blendMode = BlendMode.DstOut)
            }
        }
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val scaled = Path().apply { addPath(route); transform(androidx.compose.ui.graphics.Matrix().apply { scale(w, h) }) }
            val measure = PathMeasure().apply { setPath(scaled, false) }
            // The chest at the road's end, once the mist is gone from it.
            if (p > 0.82f) {
                val end = measure.getPosition(measure.length)
                val a = ((p - 0.82f) / 0.18f).coerceIn(0f, 1f)
                drawCircle(Brush.radialGradient(listOf(gold.copy(alpha = 0.7f * a), Color.Transparent), end, w * 0.1f), w * 0.1f, end)
                drawRoundRect(Color(0xFF6E4320), Offset(end.x - w * 0.03f, end.y - w * 0.015f), Size(w * 0.06f, w * 0.035f), CornerRadius(3f), alpha = a)
                drawRoundRect(Color(0xFF8C5A2E), Offset(end.x - w * 0.03f, end.y - w * 0.035f), Size(w * 0.06f, w * 0.022f), CornerRadius(6f), alpha = a)
                drawRect(gold, Offset(end.x - w * 0.007f, end.y - w * 0.025f), Size(w * 0.014f, w * 0.02f), alpha = a)
            }
            val head = measure.getPosition(measure.length * p)
            drawCircle(Color.Black.copy(alpha = 0.3f), 12.dp.toPx(), head.copy(y = head.y + 2.dp.toPx()))
            drawCircle(gold, 11.dp.toPx(), head)
            drawCircle(Color(0xFFFFF3D0), 8.5.dp.toPx(), head)
            drawCircle(accent, 6.dp.toPx(), head)
        }
    }
}

private fun DrawScope.castle(at: Offset, s: Float, ink: Color) {
    val stone = Color(0xFFD8C7A0)
    val body = Offset(at.x - s * 0.5f, at.y - s * 0.5f)
    drawRect(stone, body, Size(s, s * 0.6f))
    drawRect(ink, body, Size(s, s * 0.6f), style = Stroke(2f))
    for (tx in listOf(-0.62f, 0.38f)) {
        drawRect(stone, Offset(at.x + s * tx, at.y - s * 0.85f), Size(s * 0.24f, s * 0.95f))
        drawRect(ink, Offset(at.x + s * tx, at.y - s * 0.85f), Size(s * 0.24f, s * 0.95f), style = Stroke(2f))
        val roof = Path().apply { moveTo(at.x + s * (tx - 0.04f), at.y - s * 0.85f); lineTo(at.x + s * (tx + 0.12f), at.y - s * 1.15f); lineTo(at.x + s * (tx + 0.28f), at.y - s * 0.85f); close() }
        drawPath(roof, Color(0xFFA8322A))
        drawPath(roof, ink, style = Stroke(2f))
    }
    drawRoundRect(ink, Offset(at.x - s * 0.1f, at.y - s * 0.22f), Size(s * 0.2f, s * 0.32f), CornerRadius(s * 0.1f))
    drawLine(ink, Offset(at.x + s * 0.5f, at.y - s * 1.15f), Offset(at.x + s * 0.5f, at.y - s * 1.35f), 2f)
    drawRect(Color(0xFFA8322A), Offset(at.x + s * 0.5f, at.y - s * 1.35f), Size(s * 0.18f, s * 0.1f))
}

/** An eight-pointed compass rose, gold on the cardinal points. */
private fun DrawScope.rose(c: Offset, r: Float, ink: Color, turn: Float) {
    rotate(turn, c) {
        for (i in 0 until 8) {
            val long = i % 2 == 0
            val a = i * PI / 4 - PI / 2
            val len = if (long) r else r * 0.6f
            val side = r * 0.16f
            val tip = Offset(c.x + (len * cos(a)).toFloat(), c.y + (len * sin(a)).toFloat())
            val l = Offset(c.x + (side * cos(a - PI / 2)).toFloat(), c.y + (side * sin(a - PI / 2)).toFloat())
            val rr = Offset(c.x + (side * cos(a + PI / 2)).toFloat(), c.y + (side * sin(a + PI / 2)).toFloat())
            drawPath(Path().apply { moveTo(c.x, c.y); lineTo(l.x, l.y); lineTo(tip.x, tip.y); close() }, if (long) Color(0xFFC9A24E) else Color(0xFFE8D7AE))
            drawPath(Path().apply { moveTo(c.x, c.y); lineTo(rr.x, rr.y); lineTo(tip.x, tip.y); close() }, if (long) Color(0xFF8C6A26) else Color(0xFFB9A57A))
            drawPath(Path().apply { moveTo(l.x, l.y); lineTo(tip.x, tip.y); lineTo(rr.x, rr.y) }, ink, style = Stroke(1.2f))
        }
        drawCircle(ink, r * 0.72f, c, style = Stroke(1.5f))
        drawCircle(ink, r * 0.08f, c)
    }
}

/** The last page's compass, slowly settling toward north. */
@Composable
private fun CompassRose(modifier: Modifier) {
    val t = rememberInfiniteTransition(label = "rose")
    val sway by t.animateFloat(-12f, 12f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "sway")
    val ink = Color(0xFF6B4E2E)
    val gold = U.c.gold
    Canvas(modifier) {
        val c = center
        val r = size.minDimension / 2
        drawCircle(Brush.radialGradient(listOf(Color(0xFFF6EBCF), Color(0xFFE2CC98)), c, r), r, c)
        drawCircle(gold, r, c, style = Stroke(r * 0.05f))
        drawCircle(ink.copy(alpha = 0.6f), r * 0.9f, c, style = Stroke(1.5f))
        for (i in 0 until 72) {
            val a = i * PI / 36
            val inner = if (i % 9 == 0) r * 0.8f else r * 0.85f
            drawLine(ink.copy(alpha = 0.6f), Offset(c.x + (inner * cos(a)).toFloat(), c.y + (inner * sin(a)).toFloat()), Offset(c.x + (r * 0.9f * cos(a)).toFloat(), c.y + (r * 0.9f * sin(a)).toFloat()), 1.2f)
        }
        rose(c, r * 0.74f, ink, sway)
    }
}
