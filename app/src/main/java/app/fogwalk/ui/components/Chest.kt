package app.fogwalk.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import app.fogwalk.R
import app.fogwalk.data.Find
import app.fogwalk.domain.Rarity
import app.fogwalk.domain.Relics
import app.fogwalk.ui.content
import app.fogwalk.ui.map.CoinIcon
import app.fogwalk.ui.rarityColor
import app.fogwalk.ui.relicIcon
import app.fogwalk.ui.theme.U
import app.fogwalk.ui.theme.UType
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** A cache being opened: the chest shakes, bursts open with light, and shows what it held. */
@Composable
fun ChestDialog(find: Find, owned: Set<String>, onDismiss: () -> Unit) {
    val rarity = Rarity.entries[find.rarity]
    val color = rarityColor(rarity).first
    val c = content()
    val relic = find.relic.takeIf { it.isNotEmpty() }?.let { Relics.byId(it) }
    var open by remember(find.key) { mutableStateOf(false) }
    val shake = remember(find.key) { Animatable(0f) }
    val lid = remember(find.key) { Animatable(0f) }
    LaunchedEffect(find.key) {
        repeat(3) {
            shake.animateTo(1f, tween(70))
            shake.animateTo(-1f, tween(110))
            shake.animateTo(0f, tween(70))
            delay(120)
        }
        open = true
        lid.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 260f))
    }
    val spin = rememberInfiniteTransition(label = "rays")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Restart), label = "angle")

    Dialog(onDismissRequest = { if (open) onDismiss() }) {
        Box(Modifier.fillMaxWidth()) {
            Column(
                Modifier.fillMaxWidth().clip(U.RLg).background(U.c.bg).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(stringResource(R.string.cache_opened).uppercase(), style = UType.Eyebrow.copy(color = color))
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.cache_of, c.rarity(rarity)), style = UType.Title.copy(fontSize = 22.sp))
                Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
                    if (open) Canvas(Modifier.size(190.dp).rotate(angle)) { rays(color) }
                    Canvas(Modifier.size(110.dp).rotate(shake.value * 6f)) { chest(color, lid.value) }
                }
                AnimatedVisibility(open, enter = fadeIn(tween(400)) + scaleIn(tween(400), initialScale = 0.8f)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                            Reward(stringResource(R.string.plus_sparks, find.coins), U.c.gold, U.c.goldSoft, spark = true)
                            Spacer(Modifier.width(8.dp))
                            Reward(stringResource(R.string.plus_xp, find.xp), U.c.accent, U.c.accentSoft)
                        }
                        if (relic != null) {
                            val (rc, rs) = rarityColor(relic.rarity)
                            Spacer(Modifier.height(18.dp))
                            Row(
                                Modifier.fillMaxWidth().clip(U.RMd).background(U.c.surface).padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.size(52.dp).clip(CircleShape).background(rs), contentAlignment = Alignment.Center) {
                                    Icon(relicIcon(relic.id), null, Modifier.size(28.dp).scale(0.9f + 0.1f * lid.value), tint = rc)
                                }
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        (if (find.duplicate) stringResource(R.string.relic_duplicate) else stringResource(R.string.relic_new)).uppercase(),
                                        style = UType.Eyebrow.copy(color = if (find.duplicate) U.c.faint else rc),
                                    )
                                    Text(c.relicName(relic.id), style = UType.Strong)
                                    Text(c.relicLore(relic.id), style = UType.Muted)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                stringResource(R.string.relic_set_progress, c.setName(relic.set), Relics.inSet(relic.set).count { it.id in owned }, 6),
                                style = UType.Small, textAlign = TextAlign.Center,
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        PrimaryButton(stringResource(R.string.take), onDismiss, Modifier.fillMaxWidth(), color = color)
                    }
                }
                if (!open) Spacer(Modifier.height(80.dp))
            }
            if (open) Confetti(find.key, Modifier.matchParentSize())
        }
    }
}

@Composable
private fun Reward(text: String, color: Color, soft: Color, spark: Boolean = false) {
    Row(Modifier.clip(CircleShape).background(soft).padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (spark) {
            CoinIcon(Modifier.size(16.dp), color)
            Spacer(Modifier.width(6.dp))
        }
        Text(text, style = UType.Strong.copy(color = color))
    }
}

private fun DrawScope.rays(color: Color) {
    val cx = size.width / 2
    val cy = size.height / 2
    val r = size.minDimension / 2
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.45f), color.copy(alpha = 0f)), Offset(cx, cy), r), r)
    for (i in 0 until 12) {
        rotate(i * 30f, Offset(cx, cy)) {
            val p = androidx.compose.ui.graphics.Path().apply {
                moveTo(cx, cy)
                lineTo(cx + r * cos(-0.08 * PI).toFloat(), cy + r * sin(-0.08 * PI).toFloat())
                lineTo(cx + r * cos(0.08 * PI).toFloat(), cy + r * sin(0.08 * PI).toFloat())
                close()
            }
            drawPath(p, Brush.radialGradient(listOf(color.copy(alpha = 0.35f), color.copy(alpha = 0f)), Offset(cx, cy), r))
        }
    }
}

/** A wooden chest bound in bands of the cache's rarity; [open] lifts and tilts the lid. */
private fun DrawScope.chest(color: Color, open: Float) {
    val w = size.width
    val h = size.height
    val body = Size(w * 0.84f, h * 0.46f)
    val bodyTop = Offset(w * 0.08f, h * 0.44f)
    val wood = Color(0xFF6E4320)
    val woodLight = Color(0xFF8C5A2E)
    val edge = Color(0xFF3A2410)
    drawRoundRect(Color.Black.copy(alpha = 0.2f), Offset(bodyTop.x, bodyTop.y + h * 0.05f), body, CornerRadius(w * 0.05f))
    if (open > 0.05f) {
        drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF3C4), Color(0x00FFF3C4)), Offset(w / 2, bodyTop.y), w * 0.5f * open), w * 0.5f * open, Offset(w / 2, bodyTop.y))
    }
    drawRoundRect(wood, bodyTop, body, CornerRadius(w * 0.05f))
    // Planks.
    for (i in 1..2) drawRect(edge.copy(alpha = 0.35f), Offset(bodyTop.x, bodyTop.y + body.height * i / 3f), Size(body.width, h * 0.008f))
    drawRect(color, Offset(bodyTop.x, bodyTop.y), Size(body.width, body.height * 0.13f))
    drawRect(color, Offset(w * 0.2f, bodyTop.y), Size(w * 0.08f, body.height))
    drawRect(color, Offset(w * 0.72f, bodyTop.y), Size(w * 0.08f, body.height))
    drawRoundRect(edge, bodyTop, body, CornerRadius(w * 0.05f), style = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.02f))
    rotate(-open * 24f, Offset(bodyTop.x, bodyTop.y)) {
        val lidSize = Size(w * 0.84f, h * 0.26f)
        val lidTop = Offset(w * 0.08f, bodyTop.y - lidSize.height)
        drawRoundRect(woodLight, lidTop, lidSize, CornerRadius(w * 0.18f, w * 0.12f))
        drawRect(color, Offset(w * 0.2f, lidTop.y + h * 0.01f), Size(w * 0.08f, lidSize.height - h * 0.01f))
        drawRect(color, Offset(w * 0.72f, lidTop.y + h * 0.01f), Size(w * 0.08f, lidSize.height - h * 0.01f))
        drawRoundRect(edge, lidTop, lidSize, CornerRadius(w * 0.18f, w * 0.12f), style = androidx.compose.ui.graphics.drawscope.Stroke(w * 0.02f))
    }
    drawRoundRect(Color(0xFFF2C14E), Offset(w * 0.43f, bodyTop.y - h * 0.03f), Size(w * 0.14f, h * 0.16f), CornerRadius(w * 0.03f))
    drawCircle(edge, w * 0.022f, Offset(w * 0.5f, bodyTop.y + h * 0.04f))
}
