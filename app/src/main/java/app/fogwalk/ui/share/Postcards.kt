package app.fogwalk.ui.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import app.fogwalk.R
import app.fogwalk.container
import app.fogwalk.data.Units
import app.fogwalk.domain.FogStore
import app.fogwalk.domain.Levels
import app.fogwalk.geo.Grid
import app.fogwalk.ui.GameData
import app.fogwalk.ui.area
import app.fogwalk.ui.components.ProgressRing
import app.fogwalk.ui.content
import app.fogwalk.ui.dateFormat
import app.fogwalk.ui.distance
import app.fogwalk.ui.journal.duration
import app.fogwalk.ui.journal.pace
import app.fogwalk.ui.journal.rememberRoute
import app.fogwalk.ui.journal.worldShare
import app.fogwalk.ui.locale
import app.fogwalk.ui.theme.DarkPalette
import app.fogwalk.ui.theme.LightPalette
import app.fogwalk.ui.theme.LocalPalette
import app.fogwalk.ui.theme.Palette
import app.fogwalk.ui.theme.U
import app.fogwalk.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.Instant
import java.time.format.FormatStyle
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

/** What a postcard shows: the whole chronicle, one journey, or a single honour from a celebration. */
sealed interface Postcard {
    data object Progress : Postcard
    data class Walk(val id: Long) : Postcard
    class Honor(val icon: ImageVector, val color: Color, val soft: Color, val eyebrow: String, val title: String, val text: String) : Postcard
}

/** The card is laid out on a 360 × 640 sheet and saved at 1080 × 1920, the size stories and feeds take. */
private const val SHEET_W = 360f
private const val PX_W = 1080
private const val PX_H = 1920

private const val STORE_URL = "https://play.google.com/store/apps/details?id=app.fogwalk"

/** A preview of the postcard with its day/night and map switches, and the way out to any app that takes a picture. */
@Composable
fun PostcardDialog(card: Postcard, data: GameData?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val app = U.c
    var dark by remember { mutableStateOf(app.dark) }
    var withMap by remember { mutableStateOf(true) }
    val mapped = card !is Postcard.Honor
    val layer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    val palette = if (dark) DarkPalette else LightPalette
    val chooser = stringResource(R.string.share)
    val message = stringResource(R.string.postcard_text, STORE_URL)

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Column(
            Modifier.fillMaxSize().background(Color(0xFF100C08)).statusBarsPadding().navigationBarsPadding().padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.postcard), style = UType.Title.copy(fontSize = 22.sp, color = Color(0xFFF1E4C6)), modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Rounded.Close, stringResource(R.string.close), tint = Color(0xFFC2AE8A)) }
            }
            Spacer(Modifier.height(6.dp))
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val w = min(maxWidth, maxHeight * (PX_W.toFloat() / PX_H))
                val h = w * (PX_H.toFloat() / PX_W)
                val density = LocalDensity.current
                val scale = with(density) { w.toPx() } / PX_W
                Box(Modifier.size(w, h).clip(RoundedCornerShape(10.dp))) {
                    // Laid out at full size and only drawn smaller, so the saved picture keeps every pixel.
                    Box(
                        Modifier.requiredSize(with(density) { PX_W.toDp() }, with(density) { PX_H.toDp() })
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                            .drawWithContent {
                                layer.record { this@drawWithContent.drawContent() }
                                drawLayer(layer)
                            },
                    ) {
                        CompositionLocalProvider(LocalDensity provides Density(PX_W / SHEET_W, 1f), LocalPalette provides palette) {
                            Face(card, data, withMap && mapped, app)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                Toggle(if (dark) Icons.Rounded.DarkMode else Icons.Rounded.LightMode, stringResource(if (dark) R.string.postcard_night else R.string.postcard_day), true) { dark = !dark }
                if (mapped) Toggle(Icons.Rounded.Map, stringResource(R.string.postcard_map), withMap) { withMap = !withMap }
            }
            if (mapped) {
                Spacer(Modifier.height(6.dp))
                Text(
                    stringResource(R.string.postcard_map_note), style = UType.Small.copy(color = Color(0xFF85745A), textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                Modifier.fillMaxWidth().height(54.dp).clip(U.RMd).background(if (busy) app.faint else app.accent)
                    .clickable(enabled = !busy) {
                        busy = true
                        scope.launch {
                            runCatching {
                                val bitmap = layer.toImageBitmap().asAndroidBitmap()
                                send(context, bitmap, chooser, message)
                            }
                            busy = false
                        }
                    },
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.Share, null, Modifier.size(20.dp), tint = app.onAccent)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.share), style = UType.Button.copy(color = app.onAccent))
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun Toggle(icon: ImageVector, label: String, on: Boolean, onClick: () -> Unit) {
    Row(
        Modifier.clip(CircleShape).background(if (on) Color(0xFF2A2116) else Color.Transparent)
            .border(1.dp, if (on) Color(0xFFB08A3C) else Color(0xFF433522), CircleShape)
            .clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, Modifier.size(18.dp), tint = if (on) Color(0xFFE9B949) else Color(0xFF85745A))
        Spacer(Modifier.width(6.dp))
        Text(label, style = UType.Strong.copy(fontSize = 14.sp, color = if (on) Color(0xFFF1E4C6) else Color(0xFF85745A)))
    }
}

/** Saves the picture where the share sheet can read it and hands it on with a line and the store link. */
private suspend fun send(context: Context, bitmap: Bitmap, chooser: String, message: String) {
    val uri = withContext(Dispatchers.IO) {
        val plain = if (bitmap.config == Bitmap.Config.HARDWARE) bitmap.copy(Bitmap.Config.ARGB_8888, false) else bitmap
        val dir = File(context.cacheDir, "postcards").apply { mkdirs(); listFiles()?.forEach { it.delete() } }
        val file = File(dir, "fogwalk-${System.currentTimeMillis()}.png")
        file.outputStream().use { plain.compress(Bitmap.CompressFormat.PNG, 100, it) }
        FileProvider.getUriForFile(context, "${context.packageName}.postcards", file)
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_TEXT, message)
        clipData = ClipData.newRawUri(null, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(intent, chooser).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION))
}

/** The card itself, in sheet units, framed like a page from an old atlas. */
@Composable
private fun Face(card: Postcard, data: GameData?, withMap: Boolean, from: Palette) {
    val p = U.c
    Box(Modifier.fillMaxSize().background(p.bg)) {
        Canvas(Modifier.fillMaxSize()) { frame(p) }
        Column(
            Modifier.fillMaxSize().padding(horizontal = 32.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Explore, null, Modifier.size(15.dp), tint = p.gold)
                Spacer(Modifier.width(7.dp))
                Text("FOGWALK", style = UType.Eyebrow.copy(fontSize = 13.sp, letterSpacing = 0.32.em, color = p.gold))
            }
            Spacer(Modifier.height(16.dp))
            Column(Modifier.weight(1f).fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                when (card) {
                    Postcard.Progress -> if (data != null) ProgressFace(data, withMap)
                    is Postcard.Walk -> if (data != null) WalkFace(card.id, data, withMap)
                    is Postcard.Honor -> HonorFace(card, data, from)
                }
            }
            Spacer(Modifier.height(14.dp))
            Flourish(p)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StoreQr(Modifier.size(64.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(stringResource(R.string.postcard_tagline), style = UType.Small.copy(fontSize = 12.sp, color = p.muted))
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(R.string.postcard_store), style = UType.Strong.copy(fontSize = 14.sp))
                }
            }
        }
    }
}

/** A gilt double rule around the page, with a diamond at each corner. */
private fun DrawScope.frame(p: Palette) {
    drawRect(Brush.radialGradient(listOf(Color.Transparent, p.line.copy(alpha = if (p.dark) 0.35f else 0.55f)), center, size.maxDimension * 0.75f))
    val o = 12.dp.toPx()
    val i = 17.dp.toPx()
    drawRoundRect(p.gilt, Offset(o, o), Size(size.width - 2 * o, size.height - 2 * o), CornerRadius(14.dp.toPx()), style = Stroke(1.6.dp.toPx()))
    drawRoundRect(p.gilt.copy(alpha = 0.5f), Offset(i, i), Size(size.width - 2 * i, size.height - 2 * i), CornerRadius(10.dp.toPx()), style = Stroke(0.7.dp.toPx()))
    val d = 4.dp.toPx()
    listOf(Offset(i, i), Offset(size.width - i, i), Offset(i, size.height - i), Offset(size.width - i, size.height - i)).forEach { c ->
        val path = Path().apply { moveTo(c.x, c.y - d); lineTo(c.x + d, c.y); lineTo(c.x, c.y + d); lineTo(c.x - d, c.y); close() }
        drawPath(path, p.gilt)
    }
}

/** The store's QR code on a parchment plate with a quiet margin, ink on light in either theme so cameras read it. */
@Composable
private fun StoreQr(modifier: Modifier) {
    val p = U.c
    val shape = RoundedCornerShape(6.dp)
    Canvas(modifier.clip(shape).background(Color(0xFFFBF4E2)).border(0.8.dp, p.gilt, shape)) {
        val quiet = 3
        val m = size.width / (STORE_QR.size + 2 * quiet)
        val ink = Color(0xFF2B1D10)
        STORE_QR.forEachIndexed { y, row ->
            row.forEachIndexed { x, bit ->
                // A hair wider than a module, so neighbours meet without seams.
                if (bit == '1') drawRect(ink, Offset((x + quiet) * m, (y + quiet) * m), Size(m + 0.6f, m + 0.6f))
            }
        }
    }
}

/** A thin rule with a diamond in the middle, above the footer. */
@Composable
private fun Flourish(p: Palette) {
    Canvas(Modifier.width(150.dp).height(8.dp)) {
        val y = size.height / 2
        val d = 3.5.dp.toPx()
        drawLine(Brush.horizontalGradient(listOf(Color.Transparent, p.gilt)), Offset(0f, y), Offset(size.width / 2 - d * 2, y), 0.8.dp.toPx())
        drawLine(Brush.horizontalGradient(listOf(p.gilt, Color.Transparent), size.width / 2 + d * 2, size.width), Offset(size.width / 2 + d * 2, y), Offset(size.width, y), 0.8.dp.toPx())
        val c = Offset(size.width / 2, y)
        drawPath(Path().apply { moveTo(c.x, c.y - d); lineTo(c.x + d, c.y); lineTo(c.x, c.y + d); lineTo(c.x - d, c.y); close() }, p.gilt)
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.ProgressFace(data: GameData, withMap: Boolean) {
    val p = U.c
    val c = content()
    val imperial = data.settings.units == Units.IMPERIAL
    val fog = LocalContext.current.container.fog
    val patch by produceState<Patch?>(null, data.cells) { value = withContext(Dispatchers.Default) { homePatch(fog) } }
    Text(stringResource(R.string.postcard_chronicle).uppercase(), style = UType.Eyebrow.copy(fontSize = 11.sp, color = p.faint))
    Spacer(Modifier.height(10.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        val into = data.xp - Levels.start(data.level)
        ProgressRing(into.toFloat() / Levels.need(data.level), Modifier.size(50.dp), stroke = 4.5.dp) {
            Text(data.level.toString(), style = UType.Number.copy(fontSize = 19.sp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            // Long ranks ("Легенда королевства") shrink to fit instead of being cut off.
            Text(
                c.rank(Levels.rank(data.level)), style = UType.Title.copy(fontSize = 24.sp, lineHeight = 26.sp), maxLines = 1,
                autoSize = TextAutoSize.StepBased(minFontSize = 14.sp, maxFontSize = 24.sp),
            )
            Text(stringResource(R.string.level_n, data.level), style = UType.Muted.copy(fontSize = 13.sp))
        }
    }
    Spacer(Modifier.height(14.dp))
    val shown = patch
    if (withMap && shown != null) {
        FogMap(shown, null, Modifier.weight(1f).fillMaxWidth())
        Spacer(Modifier.height(14.dp))
    } else Spacer(Modifier.weight(0.5f))
    Text(area(data.areaM2, imperial), style = UType.Display.copy(fontSize = if (withMap) 34.sp else 46.sp))
    Text(stringResource(R.string.explored).lowercase() + " · " + stringResource(R.string.world_share, worldShare(data.areaM2)), style = UType.Muted.copy(fontSize = 12.5.sp, textAlign = TextAlign.Center))
    Spacer(Modifier.height(14.dp))
    Row(Modifier.fillMaxWidth()) {
        Stat(Icons.AutoMirrored.Rounded.DirectionsWalk, distance(data.facts.km * 1000, imperial), stringResource(R.string.stat_walked), p.sage, Modifier.weight(1f))
        Stat(Icons.Rounded.Explore, data.facts.sectors.toString(), stringResource(R.string.stat_sectors), p.accent, Modifier.weight(1f))
        Stat(Icons.Rounded.Inventory2, data.facts.caches.toString(), stringResource(R.string.stat_caches), p.gold, Modifier.weight(1f))
    }
    Spacer(Modifier.height(10.dp))
    Row(Modifier.fillMaxWidth()) {
        Stat(Icons.Rounded.Flare, data.facts.beacons.toString(), stringResource(R.string.stat_beacons), p.rose, Modifier.weight(1f))
        Stat(Icons.Rounded.LocalFireDepartment, data.streak.best.toString(), stringResource(R.string.stat_best_streak), p.flame, Modifier.weight(1f))
        Stat(Icons.Rounded.Explore, "%,d".format(data.cells), stringResource(R.string.stat_cells), p.sky, Modifier.weight(1f))
    }
    if (!(withMap && shown != null)) Spacer(Modifier.weight(0.5f))
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.WalkFace(id: Long, data: GameData, withMap: Boolean) {
    val p = U.c
    val c = content()
    val w = data.walks.firstOrNull { it.id == id } ?: return
    val imperial = data.settings.units == Units.IMPERIAL
    val l = locale()
    val route = rememberRoute(id)?.map { it.lat to it.lon }
    val patch by produceState<Patch?>(null, route) { value = route?.takeIf { it.size >= 2 }?.let { r -> routeView(r) } }
    val day = dateFormat(FormatStyle.LONG).format(Instant.ofEpochMilli(w.start).atZone(data.zone))
    Text(day.uppercase(l), style = UType.Eyebrow.copy(fontSize = 11.sp, color = p.faint))
    Spacer(Modifier.height(8.dp))
    Text(
        c.sectorName(Grid.sector(Grid.cell(w.lat, w.lon))), style = UType.Title.copy(fontSize = 26.sp, lineHeight = 29.sp, textAlign = TextAlign.Center),
        maxLines = 2, overflow = TextOverflow.Ellipsis,
    )
    Spacer(Modifier.height(14.dp))
    val shown = patch
    if (withMap && shown != null) {
        FogMap(shown, route, Modifier.weight(1f).fillMaxWidth())
        Spacer(Modifier.height(16.dp))
    } else Spacer(Modifier.weight(0.5f))
    Text(distance(w.meters, imperial), style = UType.Display.copy(fontSize = if (withMap) 38.sp else 52.sp))
    Spacer(Modifier.height(12.dp))
    Row(Modifier.fillMaxWidth()) {
        Stat(null, duration(w.end - w.start), stringResource(R.string.walks_time), p.sage, Modifier.weight(1f))
        Stat(null, pace(w.end - w.start, w.meters, imperial), stringResource(R.string.walk_pace), p.sky, Modifier.weight(1f))
        Stat(null, "+" + "%,d".format(w.cells), stringResource(R.string.walk_cells), p.accent, Modifier.weight(1f))
    }
    if (w.caches > 0 || w.steps > 0) {
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth()) {
            Stat(null, w.caches.toString(), stringResource(R.string.walk_chests), p.gold, Modifier.weight(1f))
            if (w.steps > 0) Stat(null, "%,d".format(w.steps), stringResource(R.string.walk_steps), p.flame, Modifier.weight(1f)) else Spacer(Modifier.weight(1f))
            Spacer(Modifier.weight(1f))
        }
    }
    if (!(withMap && shown != null)) Spacer(Modifier.weight(0.5f))
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.HonorFace(card: Postcard.Honor, data: GameData?, from: Palette) {
    val p = U.c
    val c = content()
    val color = recolor(card.color, from, p)
    val soft = recolor(card.soft, from, p)
    Spacer(Modifier.weight(1f))
    Box(Modifier.size(230.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.28f), Color.Transparent)), size.minDimension / 2)
            val rays = 24
            for (i in 0 until rays) rotate(360f / rays * i) {
                drawLine(
                    p.gilt.copy(alpha = if (i % 2 == 0) 0.55f else 0.25f),
                    Offset(center.x, center.y - size.minDimension * 0.30f), Offset(center.x, center.y - size.minDimension * (if (i % 2 == 0) 0.48f else 0.42f)),
                    1.4.dp.toPx(), cap = StrokeCap.Round,
                )
            }
            drawCircle(p.gilt, size.minDimension * 0.27f, style = Stroke(1.6.dp.toPx()))
        }
        Box(Modifier.size(112.dp).clip(CircleShape).background(soft), contentAlignment = Alignment.Center) {
            Icon(card.icon, null, Modifier.size(56.dp), tint = color)
        }
    }
    Spacer(Modifier.height(18.dp))
    Text(card.eyebrow.uppercase(), style = UType.Eyebrow.copy(fontSize = 12.sp, color = color, textAlign = TextAlign.Center))
    Spacer(Modifier.height(8.dp))
    Text(card.title, style = UType.Title.copy(fontSize = 28.sp, lineHeight = 32.sp, textAlign = TextAlign.Center), maxLines = 3, overflow = TextOverflow.Ellipsis)
    Spacer(Modifier.height(12.dp))
    Text(card.text, style = UType.Sub.copy(textAlign = TextAlign.Center), maxLines = 7, overflow = TextOverflow.Ellipsis)
    Spacer(Modifier.weight(1f))
    if (data != null) {
        Text(
            stringResource(R.string.level_n, data.level) + " · " + c.rank(Levels.rank(data.level)),
            style = UType.Strong.copy(fontSize = 14.sp, color = p.muted),
        )
    }
}

@Composable
private fun Stat(icon: ImageVector?, value: String, label: String, color: Color, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(16.dp), tint = color)
            Spacer(Modifier.height(3.dp))
        }
        Text(value, style = UType.Number.copy(fontSize = 18.sp, color = if (icon == null) color else U.c.ink), maxLines = 1)
        Text(label, style = UType.Small.copy(fontSize = 11.sp, textAlign = TextAlign.Center), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** The same role's colour in another palette: an honour's emerald stays emerald by night. */
private fun recolor(c: Color, from: Palette, to: Palette): Color {
    val roles: List<(Palette) -> Color> = listOf(
        { it.accent }, { it.accentSoft }, { it.gold }, { it.goldSoft }, { it.sage }, { it.sageSoft }, { it.sky }, { it.skySoft },
        { it.violet }, { it.violetSoft }, { it.rose }, { it.roseSoft }, { it.flame }, { it.flameSoft }, { it.faint }, { it.surface2 },
    )
    return roles.firstOrNull { it(from) == c }?.invoke(to) ?: c
}

/** Explored ground inside a square of the Mercator plane, in cells of [level]; [x0], [y0] and [side] are in those cells. */
class Patch(val level: Int, val x0: Double, val y0: Double, val side: Double, val xs: IntArray, val ys: IntArray, val share: FloatArray)

/** The finest level that still fits the square in about 220 cells across, so streets show and the drawing stays light. */
private fun patch(fog: FogStore, cx: Double, cy: Double, half: Double): Patch {
    val level = intArrayOf(Grid.Z, 18, 16, 14, 12).firstOrNull { 2 * half * (1 shl it) <= 220 } ?: 12
    val n = (1 shl level).toDouble()
    val x0 = (cx - half) * n
    val y0 = (cy - half) * n
    val side = 2 * half * n
    val full = (1 shl ((Grid.Z - level) * 2)).toFloat()
    val xs = ArrayList<Int>()
    val ys = ArrayList<Int>()
    val share = ArrayList<Float>()
    fog.visit(level, floor(x0).toInt(), floor(y0).toInt(), ceil(x0 + side).toInt(), ceil(y0 + side).toInt()) { x, y, count ->
        xs += x
        ys += y
        share += (count / full).coerceIn(0f, 1f)
    }
    return Patch(level, x0, y0, side, xs.toIntArray(), ys.toIntArray(), share.toFloatArray())
}

/** Around the lands walked most: the busiest sector and its explored neighbours. */
private fun homePatch(fog: FogStore): Patch? {
    val (keys, counts) = fog.sectors()
    if (keys.isEmpty()) return null
    val top = counts.indices.maxBy { counts[it] }
    val tx = Grid.x(keys[top])
    val ty = Grid.y(keys[top])
    val near = keys.indices.filter { kotlin.math.abs(Grid.x(keys[it]) - tx) <= 3 && kotlin.math.abs(Grid.y(keys[it]) - ty) <= 3 && counts[it] >= 40 }
        .ifEmpty { listOf(top) }
    val weight = near.sumOf { counts[it].toDouble() }
    val cx = near.sumOf { (Grid.x(keys[it]) + 0.5) * counts[it] } / weight
    val cy = near.sumOf { (Grid.y(keys[it]) + 0.5) * counts[it] } / weight
    val reach = near.maxOf { max(kotlin.math.abs(Grid.x(keys[it]) + 0.5 - cx), kotlin.math.abs(Grid.y(keys[it]) + 0.5 - cy)) } + 0.5
    val sectors = (1 shl Grid.SECTOR_Z).toDouble()
    return patch(fog, cx / sectors, cy / sectors, reach.coerceIn(0.6, 3.5) / sectors)
}

/**
 * Around one route, with a margin, never tighter than a few hundred metres. Only the route's own swath is
 * drawn cleared, so the card tells this walk and keeps the rest of the neighbourhood in the mist.
 */
private fun routeView(route: List<Pair<Double, Double>>): Patch {
    val mx = route.map { Grid.mx(it.second) }
    val my = route.map { Grid.my(it.first) }
    val cx = (mx.min() + mx.max()) / 2
    val cy = (my.min() + my.max()) / 2
    val perUnit = Grid.cellMeters(route.first().first) * Grid.N
    val half = max(max(mx.max() - mx.min(), my.max() - my.min()) / 2 * 1.3, 250 / perUnit)
    val n = (1 shl Grid.Z).toDouble()
    return Patch(Grid.Z, (cx - half) * n, (cy - half) * n, 2 * half * n, IntArray(0), IntArray(0), FloatArray(0))
}

/** Explored ground as parchment showing through the mist, and the route inked over it. */
@Composable
private fun FogMap(patch: Patch, route: List<Pair<Double, Double>>?, modifier: Modifier) {
    val p = U.c
    val mist = if (p.dark) Color(0xFF34302A) else Color(0xFFB7B1A3)
    val land = if (p.dark) Color(0xFFD9C79E) else Color(0xFFFBF4E2)
    val shape = RoundedCornerShape(14.dp)
    // Cells grouped by how fully they are explored, so each group draws in one call.
    val groups = remember(patch) {
        val bands = 4
        Array(bands) { b -> (patch.share.indices).filter { i -> minOf(bands - 1, (sqrt(patch.share[i]) * bands).toInt()) == b } }
    }
    Box(modifier.clip(shape).border(1.2.dp, p.gilt, shape)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(mist)
            val rnd = Random(11)
            repeat(30) {
                val c = Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height)
                val r = (0.08f + rnd.nextFloat() * 0.22f) * size.maxDimension
                drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = if (p.dark) 0.05f else 0.16f), Color.Transparent), c, r), r, c)
            }
            // The square is cropped to the box, centred.
            val k = (max(size.width, size.height) / patch.side).toFloat()
            val ox = (size.width - patch.side.toFloat() * k) / 2
            val oy = (size.height - patch.side.toFloat() * k) / 2
            fun at(x: Double, y: Double) = Offset(ox + ((x - patch.x0) * k).toFloat(), oy + ((y - patch.y0) * k).toFloat())
            groups.forEachIndexed { b, idx ->
                if (idx.isEmpty()) return@forEachIndexed
                val pts = idx.map { at(patch.xs[it] + 0.5, patch.ys[it] + 0.5) }
                val a = 0.4f + 0.6f * (b + 1) / groups.size
                drawPoints(pts, PointMode.Points, land.copy(alpha = a * 0.22f), strokeWidth = k * 3.2f, cap = StrokeCap.Round)
                drawPoints(pts, PointMode.Points, land.copy(alpha = a), strokeWidth = k * 1.5f, cap = StrokeCap.Round)
            }
            if (route != null && route.size >= 2) {
                val n = (1 shl patch.level).toDouble()
                val path = Path()
                route.forEachIndexed { i, (lat, lon) ->
                    val o = at(Grid.mx(lon) * n, Grid.my(lat) * n)
                    if (i == 0) path.moveTo(o.x, o.y) else path.lineTo(o.x, o.y)
                }
                // The swath the walk cleared: the reveal radius either side of the line, with a soft edge.
                val cellM = Grid.cellMeters(route.first().first) * (1 shl (Grid.Z - patch.level))
                val swath = (2 * app.fogwalk.explore.Explorer.REVEAL_M / cellM * k).toFloat()
                drawPath(path, land.copy(alpha = 0.3f), style = Stroke(swath * 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(path, land, style = Stroke(swath, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawPath(path, p.accent, style = Stroke(3.6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                val (sLat, sLon) = route.first()
                val (eLat, eLon) = route.last()
                listOf(at(Grid.mx(sLon) * n, Grid.my(sLat) * n) to p.sage, at(Grid.mx(eLon) * n, Grid.my(eLat) * n) to p.gold).forEach { (o, col) ->
                    drawCircle(land, 7.dp.toPx(), o)
                    drawCircle(col, 4.8.dp.toPx(), o)
                }
            }
            drawRect(Brush.radialGradient(listOf(Color.Transparent, mist.copy(alpha = 0.75f)), center, size.maxDimension * 0.72f))
            compass(Offset(size.width - 26.dp.toPx(), 26.dp.toPx()), 15.dp.toPx(), p)
        }
    }
}

/** A small four-pointed compass rose, north inked dark. */
private fun DrawScope.compass(c: Offset, r: Float, p: Palette) {
    drawCircle(p.surface.copy(alpha = 0.85f), r * 1.15f, c)
    drawCircle(p.gilt, r * 1.15f, c, style = Stroke(1.dp.toPx()))
    for (i in 0 until 4) rotate(90f * i, c) {
        val tip = Path().apply { moveTo(c.x, c.y - r); lineTo(c.x + r * 0.26f, c.y); lineTo(c.x - r * 0.26f, c.y); close() }
        drawPath(tip, if (i == 0) p.flame else p.gilt)
    }
    drawCircle(p.surface, r * 0.14f, c)
}

/** The share button the screens put in their top bars. */
@Composable
fun ShareButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) { Icon(Icons.Rounded.Share, stringResource(R.string.share), tint = U.c.muted) }
}

