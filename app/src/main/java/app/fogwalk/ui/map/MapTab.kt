package app.fogwalk.ui.map

import app.fogwalk.ui.components.medallion
import app.fogwalk.ui.components.frame
import app.fogwalk.ui.GameIcon
import androidx.compose.animation.core.animateFloat
import app.fogwalk.ui.components.parchment
import android.Manifest
import android.annotation.SuppressLint
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.MotionEvent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.ChangeHistory
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocationSearching
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import app.fogwalk.BuildConfig
import app.fogwalk.R
import app.fogwalk.container
import app.fogwalk.data.Units
import app.fogwalk.domain.Altars
import app.fogwalk.domain.Caches
import app.fogwalk.domain.Shrine
import app.fogwalk.domain.Levels
import app.fogwalk.domain.Rarity
import app.fogwalk.domain.Sectors
import app.fogwalk.domain.Gear
import app.fogwalk.domain.Hook
import app.fogwalk.explore.Bought
import app.fogwalk.ui.Tab
import app.fogwalk.ui.components.ConfirmDialog
import kotlinx.coroutines.launch
import app.fogwalk.domain.Races
import app.fogwalk.domain.Treasures
import app.fogwalk.data.Waypoint
import app.fogwalk.ui.locale
import androidx.compose.runtime.produceState
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Timer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import app.fogwalk.explore.ExploreService
import app.fogwalk.explore.GameEvent
import app.fogwalk.geo.Grid
import app.fogwalk.ui.GameData
import app.fogwalk.ui.Go
import app.fogwalk.ui.MapFocus
import app.fogwalk.ui.components.Bar
import app.fogwalk.ui.components.PrimaryButton
import app.fogwalk.ui.components.ProgressRing
import app.fogwalk.ui.components.rememberNow
import app.fogwalk.ui.content
import app.fogwalk.ui.distance
import app.fogwalk.ui.rarityColor
import app.fogwalk.ui.shrineColor
import app.fogwalk.ui.shrineGives
import app.fogwalk.ui.shrineName
import app.fogwalk.ui.shrineReached
import app.fogwalk.ui.theme.U
import app.fogwalk.ui.theme.UType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import kotlin.math.abs
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.ui.draw.rotate

private class Toast(val id: Long, val icon: ImageVector, val text: String, val color: Color)

@SuppressLint("ClickableViewAccessibility")
@Composable
fun MapTab(data: GameData, go: Go, focus: MutableStateFlow<MapFocus?>) {
    val context = LocalContext.current
    val container = context.container
    val explorer = container.explorer
    val live by explorer.live.collectAsStateWithLifecycle()
    val s = data.settings
    val imperial = s.units == Units.IMPERIAL
    val c = content()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current.density

    var permitted by remember { mutableStateOf(BuildConfig.DEMO || container.feed.permitted()) }
    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        permitted = container.feed.permitted()
        container.feed.restart()
    }
    LifecycleResumeEffect(permitted) {
        permitted = BuildConfig.DEMO || container.feed.permitted()
        container.feed.acquire()
        onPauseOrDispose { container.feed.release() }
    }

    var follow by remember { mutableStateOf(true) }
    /** The map's turn, degrees; 0 is north up (#37). */
    var turned by remember { mutableStateOf(0f) }
    var pick by remember { mutableStateOf<Pick?>(null) }
    val toasts = remember { mutableStateListOf<Toast>() }
    // A chest opened, by walking up or by the hook, lets go of its card.
    LaunchedEffect(data.opened) {
        if ((pick as? Pick.OfCache)?.cache?.key in data.opened) pick = null
    }

    val overlay = remember {
        WorldOverlay(
            container.fog, density,
            onPick = { pick = it },
            onLongPress = if (BuildConfig.DEBUG) { lat, lon -> explorer.simulateTo(lat, lon) } else null,
            sprite = Sprites(context.applicationContext)::get,
        )
    }
    val dark = U.c.dark
    val mapView = remember {
        MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
            isTilesScaledToDpi = true
            minZoomLevel = 3.0
            maxZoomLevel = 19.5
            isHorizontalMapRepetitionEnabled = false
            isVerticalMapRepetitionEnabled = false
            setScrollableAreaLimitLatitude(MapView.getTileSystem().maxLatitude, MapView.getTileSystem().minLatitude, 0)
            // Two fingers turn the map once they plainly twist, not with a pinch (#37, #45); the compass button sets north back up.
            overlays.add(TwistRotation(this))
            overlays.add(overlay)
            val start = live.fix
            if (start != null) {
                controller.setZoom(17.0)
                controller.setCenter(GeoPoint(start.lat, start.lon))
            } else controller.setZoom(3.0)
            val zoom = OneFingerZoom(this)
            setOnTouchListener { _, e ->
                if (zoom.onTouch(e)) return@setOnTouchListener true
                if (e.action == MotionEvent.ACTION_MOVE && e.pointerCount >= 1) follow = false
                if (e.pointerCount >= 2 || e.actionMasked == MotionEvent.ACTION_UP) post { turned = mapOrientation }
                false
            }
        }
    }
    LaunchedEffect(dark) {
        mapView.overlayManager.tilesOverlay.setColorFilter(if (dark) nightTiles() else softTiles())
        mapView.overlayManager.tilesOverlay.loadingBackgroundColor = if (dark) 0xFF1E1810.toInt() else 0xFFE8DCBE.toInt()
        mapView.invalidate()
    }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        lifecycle.addObserver(obs)
        onDispose {
            lifecycle.removeObserver(obs)
            mapView.onPause()
            mapView.onDetach()
        }
    }

    // A race's route, loaded once per raced walk.
    val dao = container.db.dao()
    val raceRoute by produceState<List<Waypoint>?>(null, s.race?.walk) {
        value = s.race?.walk?.let { w -> withContext(Dispatchers.IO) { Races.clean(dao.waypoints(w), { it.lat }, { it.lon }, { it.at }) } }
    }
    val raceView = remember(raceRoute, s.race) {
        val r = s.race
        val rt = raceRoute
        if (r == null || rt == null || rt.size < 2) null else {
            val pts = rt.map { it.lat to it.lon }
            RaceView(pts, LongArray(rt.size) { rt[it].at }, Races.along(pts), r.started, r.reached)
        }
    }

    // The scene the overlay draws; markers pulse at about 25 frames a second while the map is open.
    val rarityColors = Rarity.entries.map { rarityColor(it).first.toArgb() }.toIntArray()
    val accent = U.c.accent.toArgb()
    val gold = U.c.gold.toArgb()
    val shadowInk = U.c.violet.toArgb()
    val shrines = s.shrines.takeIf { it.day == data.today }
    overlay.scene = Scene(
        fix = live.fix, trail = live.trail,
        shrines = shrines?.shrines.orEmpty(), shrineChosen = shrines?.chosen, shrineReached = data.todayRow.beacon, ring = s.ring,
        week = data.week, opened = data.opened + s.dismissed, radar = s.radarUntil > data.now,
        style = s.fog, density = s.fogDensity, rarityColors = rarityColors, accent = accent, gold = gold, shadow = shadowInk,
        picked = (pick as? Pick.OfLand)?.sector,
        senseM = Gear.senseM(s.gear), rumor = data.rumor?.key, treasure = s.treasure, race = raceView,
        reach = when (val p = pick) {
            is Pick.OfCache -> Triple(p.cache.lat, p.cache.lon, Gear.openM(s.gear))
            is Pick.OfShrine -> shrines?.of(p.kind)?.takeIf { !data.todayRow.beacon }?.let { Triple(it.lat, it.lon, Gear.shrineM(s.gear)) }
            else -> null
        },
        companion = s.companion, monuments = data.monuments,
    )
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                mapView.invalidate()
                delay(40)
            }
        }
    }

    // Follow the walker; the first real fix also zooms in from the world view.
    val fix = live.fix
    var centered by remember { mutableStateOf(false) }
    LaunchedEffect(fix?.lat, fix?.lon, follow) {
        if (fix == null) return@LaunchedEffect
        // Wait for the first layout: before it osmdroid ignores where it is asked to look.
        while (mapView.width == 0) delay(50)
        if (!centered && fix.time > 0) {
            centered = true
            follow = true
            // The demo opens wider, so screenshots show charted streets against the mist.
            mapView.controller.setZoom(if (BuildConfig.DEMO) 15.6 else 17.0)
            mapView.controller.setCenter(GeoPoint(fix.lat, fix.lon))
        } else if (follow) {
            if (mapView.zoomLevelDouble < 12) {
                mapView.controller.setZoom(17.0)
                mapView.controller.setCenter(GeoPoint(fix.lat, fix.lon))
            } else mapView.controller.animateTo(GeoPoint(fix.lat, fix.lon))
        }
    }
    LaunchedEffect(Unit) {
        focus.collect { f ->
            if (f == null) return@collect
            follow = false
            mapView.controller.setZoom(f.zoom)
            mapView.controller.animateTo(GeoPoint(f.lat, f.lon))
            focus.value = null
        }
    }

    // Game events become toasts and a tap of haptics.
    val sectorText = stringResource(R.string.toast_sector)
    val milestoneText = stringResource(R.string.toast_milestone)
    val ringText = stringResource(R.string.ring_closed)
    val raceText = stringResource(R.string.race_started)
    val sageC = U.c.sage
    val flame = U.c.flame
    val accentC = U.c.accent
    val goldC = U.c.gold
    LaunchedEffect(Unit) {
        explorer.events.collect { e ->
            val t = when (e) {
                is GameEvent.NewSector -> Toast(System.nanoTime(), Icons.Rounded.TravelExplore, sectorText.format(c.sectorName(e.sector)), accentC)
                is GameEvent.Milestone -> Toast(System.nanoTime(), Icons.Rounded.TravelExplore, milestoneText.format(c.sectorName(e.sector), (e.share * 100).toInt(), e.coins), goldC)
                // Shown big instead, like a chest (#52).
                is GameEvent.Beacon -> null
                is GameEvent.RingClosed -> Toast(System.nanoTime(), Icons.Rounded.ChangeHistory, ringText.format(e.cells), accentC)
                is GameEvent.Opened -> null
                is GameEvent.Near -> null
                is GameEvent.RaceStarted -> Toast(System.nanoTime(), Icons.Rounded.Timer, raceText, sageC)
                is GameEvent.RaceDone -> null
            }
            if (s.haptics && (e is GameEvent.Opened || e is GameEvent.Beacon)) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            if (t != null) {
                toasts += t
                if (toasts.size > 3) toasts.removeAt(0)
            }
        }
    }
    LaunchedEffect(toasts.size) {
        if (toasts.isEmpty()) return@LaunchedEffect
        delay(3500)
        if (toasts.isNotEmpty()) toasts.removeAt(0)
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView({ mapView }, Modifier.fillMaxSize())
        // The map as an old atlas: its edges browned and drifting with mist. Drawn only, so every touch reaches the map.
        AtlasEdges(U.c.dark)

        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp)) {
            TopHud(data) { go.tab(Tab.TREASURY) }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val stepsToday = data.todayRow.steps
                Pill(
                    Icons.AutoMirrored.Rounded.DirectionsWalk,
                    stringResource(R.string.today_line, pluralStringResource(R.plurals.cells, data.todayRow.cells, data.todayRow.cells), distance(data.todayRow.meters, imperial)) +
                        if ((s.steps || s.healthFrom >= 0) && stepsToday > 0) " · " + pluralStringResource(R.plurals.steps, stepsToday, stepsToday) else "",
                    U.c.accent,
                )
                if (s.lanternUntil > data.now) Pill(Icons.Rounded.Lightbulb, stringResource(R.string.min_left, ((s.lanternUntil - data.now) / 60_000 + 1).toInt()), U.c.gold)
                if (s.radarUntil > data.now) Pill(Icons.Rounded.Radar, stringResource(R.string.min_left, ((s.radarUntil - data.now) / 60_000 + 1).toInt()), U.c.violet)
            }
            Spacer(Modifier.height(8.dp))
            toasts.forEach { t ->
                AnimatedVisibility(true, enter = fadeIn() + slideInVertically(), exit = fadeOut() + slideOutVertically()) {
                    Row(
                        Modifier.padding(bottom = 6.dp).frame(elevation = 6.dp).padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GameIcon(t.icon, null, Modifier.size(20.dp), tint = t.color)
                        Spacer(Modifier.width(10.dp))
                        Text(t.text, style = UType.Body.copy(fontSize = 14.sp))
                    }
                }
            }
        }

        Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(12.dp)) {
            Column(Modifier.align(Alignment.End), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // The atlas's compass rose, always there and turning with the map; a tap sets north back up.
                Box(
                    Modifier.size(56.dp).clip(CircleShape).clickable { mapView.mapOrientation = 0f; turned = 0f },
                    contentAlignment = Alignment.Center,
                ) {
                    app.fogwalk.ui.ArtImage(R.drawable.ui_compass, Modifier.size(54.dp).rotate(turned))
                }
                // Zoom with one hand (#38).
                RoundButton(Icons.Rounded.Add, U.c.ink, stringResource(R.string.zoom_in)) { mapView.controller.zoomIn() }
                RoundButton(Icons.Rounded.Remove, U.c.ink, stringResource(R.string.zoom_out)) { mapView.controller.zoomOut() }
                RoundButton(if (follow) Icons.Rounded.MyLocation else Icons.Rounded.LocationSearching, U.c.accent, stringResource(R.string.my_location)) {
                    follow = true
                    live.fix?.let {
                        if (mapView.zoomLevelDouble < 15) mapView.controller.setZoom(17.0)
                        mapView.controller.animateTo(GeoPoint(it.lat, it.lon))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            pick?.let { p -> PickCard(p, data, imperial, raceView) { pick = null } }
            Text(
                stringResource(R.string.osm_credit), style = UType.Small.copy(fontSize = 10.sp, color = U.c.muted),
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp).clip(CircleShape).background(U.c.surface.copy(alpha = 0.7f)).padding(horizontal = 6.dp, vertical = 1.dp),
            )
            BottomPanel(data, permitted, imperial, raceView, onFocus = { lat, lon ->
                follow = false
                if (mapView.zoomLevelDouble < 15) mapView.controller.setZoom(15.5)
                mapView.controller.animateTo(GeoPoint(lat, lon))
            }, onShrine = { sh ->
                follow = false
                pick = Pick.OfShrine(sh.kind)
                if (mapView.zoomLevelDouble < 15) mapView.controller.setZoom(15.5)
                mapView.controller.animateTo(GeoPoint(sh.lat, sh.lon))
            }, onAllow = {
                askLocation.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            })
        }
    }
}

/** Browned corners and two banks of mist drifting slowly along the edges of the screen, like the margins of an old map. */
@Composable
private fun AtlasEdges(dark: Boolean) {
    val drift = androidx.compose.animation.core.rememberInfiniteTransition(label = "mist")
    val t by drift.animateFloat(
        0f, 1f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(24_000, easing = androidx.compose.animation.core.LinearEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "t",
    )
    val sepia = if (dark) Color(0xFF0E0A06) else Color(0xFF5A3E1E)
    val mist = if (dark) Color(0xFF8A8C92) else Color(0xFFF4EEDF)
    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        // The vignette: clear in the middle, browning toward the corners.
        drawRect(
            Brush.radialGradient(
                0.55f to Color.Transparent, 1f to sepia.copy(alpha = if (dark) 0.55f else 0.32f),
                center = center, radius = kotlin.math.hypot(w, h) / 2,
            ),
        )
        // Mist along the left and right margins, slowly changing places.
        for ((x, y, k) in listOf(Triple(-0.05f + 0.06f * t, 0.30f + 0.10f * t, 1f), Triple(1.05f - 0.06f * t, 0.70f - 0.12f * t, 0.85f))) {
            val c = Offset(w * x, h * y)
            val r = w * 0.55f * k
            drawCircle(Brush.radialGradient(listOf(mist.copy(alpha = 0.38f), Color.Transparent), c, r), r, c)
        }
    }
}

@Composable
private fun TopHud(data: GameData, onCoins: () -> Unit) {
    val c = content()
    val into = data.xp - Levels.start(data.level)
    val need = Levels.need(data.level)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier.weight(1f).frame(elevation = 6.dp).padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProgressRing(into.toFloat() / need, Modifier.size(40.dp), stroke = 4.dp) {
                Text(data.level.toString(), style = UType.Strong.copy(fontSize = 15.sp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(c.rank(Levels.rank(data.level)), style = UType.Strong.copy(fontSize = 14.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(4.dp))
                Bar(into.toFloat() / need, Modifier.height(5.dp))
                Spacer(Modifier.height(3.dp))
                Text(stringResource(R.string.xp_to_next, need - into), style = UType.Small.copy(fontSize = 10.5.sp))
            }
        }
        Spacer(Modifier.width(8.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill(Icons.Rounded.LocalFireDepartment, data.streak.current.toString(), if (data.streak.todayDone) U.c.flame else U.c.faint)
            Pill(Icons.Rounded.Inventory2, "%,d".format(data.coins), U.c.gold, spark = true, onClick = onCoins)
        }
    }
}

@Composable
fun Pill(icon: ImageVector, text: String, color: Color, spark: Boolean = false, onClick: (() -> Unit)? = null) {
    Row(
        Modifier.shadow(4.dp, CircleShape).clip(CircleShape).background(U.c.surface).parchment(0.5f).border(1.5.dp, U.c.gilt, CircleShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (spark) CoinIcon(Modifier.size(15.dp)) else GameIcon(icon, null, Modifier.size(16.dp), tint = color)
        Spacer(Modifier.width(5.dp))
        Text(text, style = UType.Strong.copy(fontSize = 13.sp), maxLines = 1, softWrap = false)
    }
}

/** The in-game currency: a gold coin with a struck star. */
@Composable
fun CoinIcon(modifier: Modifier = Modifier, color: Color = U.c.gold) {
    androidx.compose.foundation.Canvas(modifier) {
        val r = size.minDimension / 2
        val c = center
        val light = Color(0xFFFFE08A)
        drawCircle(androidx.compose.ui.graphics.Brush.radialGradient(listOf(light, color), c.copy(x = c.x - r * 0.3f, y = c.y - r * 0.3f), r * 1.6f), r, c)
        drawCircle(Color(0x66000000), r * 0.74f, c, style = androidx.compose.ui.graphics.drawscope.Stroke(r * 0.1f))
        val p = androidx.compose.ui.graphics.Path().apply {
            for (i in 0 until 10) {
                val a = Math.PI * 2 * i / 10 - Math.PI / 2
                val rr = if (i % 2 == 0) r * 0.5f else r * 0.22f
                val x = c.x + (rr * kotlin.math.cos(a)).toFloat()
                val y = c.y + (rr * kotlin.math.sin(a)).toFloat()
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
            close()
        }
        drawPath(p, Color(0x80FFF6D0))
    }
}

@Composable
private fun RoundButton(icon: ImageVector, tint: Color, label: String, onClick: () -> Unit) {
    Box(
        Modifier.size(50.dp).medallion().clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { GameIcon(icon, label, Modifier.size(22.dp), tint = if (tint == U.c.ink) Color(0xFF4A3420) else tint) }
}

@Composable
private fun PickCard(p: Pick, data: GameData, imperial: Boolean, race: RaceView?, onClose: () -> Unit) {
    val c = content()
    val context = LocalContext.current
    val fix = context.container.explorer.live.collectAsStateWithLifecycle().value.fix?.takeIf { it.time > 0 }
    val shrines = data.settings.shrines.takeIf { it.day == data.today }
    val target = when (p) {
        is Pick.OfCache -> p.cache.lat to p.cache.lon
        is Pick.OfShrine -> shrines?.of(p.kind)?.let { it.lat to it.lon }
        is Pick.OfLand -> null
        Pick.OfTreasure -> data.settings.treasure?.let { it.cLat to it.cLon }
        Pick.OfRace -> race?.route?.first()
    }
    val meters = if (fix != null && target != null) Grid.distance(fix.lat, fix.lon, target.first, target.second) else null
    val (color, soft) = when (p) {
        is Pick.OfCache -> if (p.sensed) U.c.muted to U.c.surface2 else rarityColor(p.cache.rarity)
        is Pick.OfShrine -> shrineColor(p.kind) to shrineColor(p.kind).copy(alpha = 0.18f)
        // A land wears its arms, in colour once its banner is raised.
        is Pick.OfLand -> if (data.share(p.sector) >= Sectors.CHARTED) U.c.accent to U.c.accentSoft else U.c.muted to U.c.surface2
        Pick.OfTreasure -> U.c.gold to U.c.goldSoft
        Pick.OfRace -> U.c.sage to U.c.sageSoft
    }
    Row(
        Modifier.fillMaxWidth().padding(bottom = 8.dp).frame(elevation = 8.dp).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(soft), contentAlignment = Alignment.Center) {
            val icon = when (p) {
                is Pick.OfCache -> Icons.Rounded.Inventory2
                is Pick.OfShrine -> Icons.Rounded.Flare
                is Pick.OfLand -> app.fogwalk.ui.armIcon(app.fogwalk.domain.Arms.of(p.sector))
                Pick.OfTreasure -> Icons.Rounded.Map
                Pick.OfRace -> Icons.Rounded.Timer
            }
            val art = when (p) {
                is Pick.OfCache -> app.fogwalk.ui.Art.chest(if (p.sensed) null else p.cache.rarity)
                is Pick.OfShrine -> app.fogwalk.ui.Art.shrine(p.kind)
                Pick.OfTreasure -> data.settings.treasure?.let { app.fogwalk.ui.Art.maps[it.tier] }
                is Pick.OfLand, Pick.OfRace -> null
            }
            if (art != null) app.fogwalk.ui.ArtImage(art, Modifier.size(38.dp))
            else GameIcon(icon, (p as? Pick.OfLand)?.let { c.armName(app.fogwalk.domain.Arms.of(it.sector)) }, Modifier.size(22.dp), tint = color)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                when (p) {
                    is Pick.OfCache -> if (p.sensed) stringResource(R.string.cache_unknown) else stringResource(R.string.cache_of, c.rarity(p.cache.rarity))
                    is Pick.OfShrine -> shrineName(context, p.kind)
                    is Pick.OfLand -> c.sectorName(p.sector)
                    Pick.OfTreasure -> stringResource(R.string.treasure_one)
                    Pick.OfRace -> stringResource(R.string.race_title)
                },
                style = UType.Strong,
            )
            Spacer(Modifier.height(2.dp))
            val hint = when (p) {
                is Pick.OfCache -> stringResource(R.string.cache_hint, Gear.openM(data.settings.gear).toInt())
                is Pick.OfShrine -> when {
                    !data.todayRow.beacon -> stringResource(R.string.beacon_hint, Gear.shrineM(data.settings.gear).toInt(), shrineGives(context, p.kind))
                    shrines?.chosen == p.kind -> stringResource(R.string.beacon_done)
                    else -> stringResource(R.string.shrine_others)
                }
                Pick.OfTreasure -> stringResource(R.string.treasure_hint)
                Pick.OfRace -> stringResource(if (race?.started == 0L) R.string.race_challenge_text else R.string.race_on)
                is Pick.OfLand -> {
                    val share = data.share(p.sector)
                    val next = Sectors.milestones.firstOrNull { share < it.first }
                    listOfNotNull(
                        stringResource(R.string.land_here).takeIf { fix != null && Grid.sector(Grid.cell(fix.lat, fix.lon)) == p.sector },
                        stringResource(R.string.land_charted, (share * 100).toInt()),
                        if (next != null) stringResource(R.string.land_next, (next.first * 100).toInt()) else stringResource(R.string.land_done),
                    ).joinToString(" · ")
                }
            }
            Text((meters?.let { distance(it, imperial) + " · " } ?: "") + hint, style = UType.Muted)
            if (p is Pick.OfLand) MonumentLine(p.sector, data)
            if (p is Pick.OfRace) {
                Spacer(Modifier.height(8.dp))
                Chip(stringResource(R.string.race_cancel), U.c.surface2) {
                    context.container.explorer.cancelRace()
                    onClose()
                }
            }
            if (p is Pick.OfCache && meters != null) HookLine(p.cache, meters, data)
            // A chest behind a fence or on a building site can go off the map till Monday (#46).
            if (p is Pick.OfCache) {
                val gone = stringResource(R.string.cache_dismissed)
                Spacer(Modifier.height(8.dp))
                Chip(stringResource(R.string.cache_dismiss), U.c.surface2) {
                    context.container.explorer.dismiss(p.cache)
                    android.widget.Toast.makeText(context, gone, android.widget.Toast.LENGTH_SHORT).show()
                    onClose()
                }
            }
            if (p is Pick.OfShrine && shrines != null && !data.todayRow.beacon) {
                val explorer = context.container.explorer
                Spacer(Modifier.height(8.dp))
                if (shrines.chosen == p.kind) {
                    Text(stringResource(R.string.shrine_chosen), style = UType.Small)
                    Spacer(Modifier.height(6.dp))
                    Chip(stringResource(R.string.shrine_unchoose), U.c.surface2) { explorer.choose(null) }
                } else Chip(stringResource(R.string.shrine_choose), shrineColor(p.kind).copy(alpha = 0.18f)) { explorer.choose(p.kind) }
            }
        }
        GameIcon(Icons.Rounded.Close, stringResource(R.string.close), Modifier.size(22.dp).clip(CircleShape).clickable(onClick = onClose), tint = U.c.faint)
    }
}

/** A small rounded action inside a card. */
@Composable
private fun Chip(text: String, background: Color, onClick: () -> Unit) {
    Text(
        text, style = UType.Strong.copy(fontSize = 14.sp), maxLines = 1,
        modifier = Modifier.clip(CircleShape).background(background).clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

/** A charted land's monument (#50): raised, or raised a level higher, for gold. */
@Composable
fun MonumentLine(sector: Long, data: GameData) {
    if (data.share(sector) < Sectors.CHARTED) return
    val context = LocalContext.current
    val container = context.container
    val level = data.monuments[sector] ?: 0
    val price = app.fogwalk.domain.Monuments.price(level)
    var asking by remember { mutableStateOf(false) }
    Spacer(Modifier.height(8.dp))
    if (level > 0) {
        Text(stringResource(R.string.monument_level, level), style = UType.Small)
        Spacer(Modifier.height(6.dp))
    }
    Row(
        Modifier.clip(CircleShape).background(U.c.goldSoft).clickable { asking = true }.padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(if (level == 0) R.string.monument_raise else R.string.monument_up), style = UType.Strong.copy(fontSize = 14.sp))
        Spacer(Modifier.width(8.dp))
        CoinIcon(Modifier.size(13.dp), if (data.coins >= price) U.c.gold else U.c.faint)
        Spacer(Modifier.width(4.dp))
        Text("%,d".format(price), style = UType.Strong.copy(fontSize = 14.sp, color = if (data.coins >= price) U.c.gold else U.c.faint))
    }
    if (asking) {
        val short = stringResource(R.string.not_enough)
        ConfirmDialog(
            title = stringResource(if (level == 0) R.string.monument_raise_q else R.string.monument_up_q, content().sectorName(sector)),
            text = stringResource(R.string.monument_text, level + 1, price, data.coins),
            confirm = stringResource(R.string.buy),
            onConfirm = {
                asking = false
                container.scope.launch {
                    if (container.explorer.raiseMonument(sector) == Bought.NO_GOLD) withContext(Dispatchers.Main) {
                        android.widget.Toast.makeText(context, short, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onDismiss = { asking = false },
        )
    }
}

/** The grappling hook, offered for a chest out of reach but within the throw. */
@Composable
private fun HookLine(cache: app.fogwalk.domain.Cache, meters: Double, data: GameData) {
    val context = LocalContext.current
    val container = context.container
    val beyond = meters - Gear.openM(data.settings.gear)
    if (beyond <= 0 || beyond > Hook.range(data.settings.gear)) return
    val throws = Hook.throws(data.settings.gear)
    val used = data.ledger.count { it.ref.startsWith("buy:${Hook.ref(data.today)}:") } >= throws
    val price = Hook.price(beyond)
    var asking by remember { mutableStateOf(false) }
    Spacer(Modifier.height(8.dp))
    if (used) {
        Text(stringResource(R.string.hook_used), style = UType.Small)
        return
    }
    Row(
        Modifier.clip(CircleShape).background(U.c.goldSoft).clickable { asking = true }.padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(stringResource(R.string.hook_throw), style = UType.Strong.copy(fontSize = 14.sp))
        Spacer(Modifier.width(8.dp))
        CoinIcon(Modifier.size(13.dp), if (data.coins >= price) U.c.gold else U.c.faint)
        Spacer(Modifier.width(4.dp))
        Text("$price", style = UType.Strong.copy(fontSize = 14.sp, color = if (data.coins >= price) U.c.gold else U.c.faint))
    }
    if (asking) {
        val short = stringResource(R.string.not_enough)
        val far = stringResource(R.string.hook_far)
        ConfirmDialog(
            title = stringResource(R.string.hook_title),
            text = pluralStringResource(R.plurals.hook_text, throws, price, data.coins, throws),
            confirm = stringResource(R.string.hook_throw),
            onConfirm = {
                asking = false
                container.scope.launch {
                    val text = when (container.explorer.hook(cache)) {
                        Bought.NO_GOLD -> short
                        Bought.FAR -> far
                        else -> null
                    }
                    if (text != null) withContext(Dispatchers.Main) { android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_SHORT).show() }
                }
            },
            onDismiss = { asking = false },
        )
    }
}

@Composable
private fun BottomPanel(
    data: GameData, permitted: Boolean, imperial: Boolean, race: RaceView?,
    onFocus: (Double, Double) -> Unit, onShrine: (Shrine) -> Unit, onAllow: () -> Unit,
) {
    val context = LocalContext.current
    val container = context.container
    val live by container.explorer.live.collectAsStateWithLifecycle()
    val s = data.settings
    Column(
        Modifier.fillMaxWidth().frame(U.RLg, elevation = 10.dp)
            .animateContentSize().padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        if (!permitted) {
            Text(stringResource(R.string.need_location_title), style = UType.Strong)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.need_location_text), style = UType.Muted)
            Spacer(Modifier.height(12.dp))
            PrimaryButton(stringResource(R.string.allow_location), onAllow, Modifier.fillMaxWidth(), icon = Icons.Rounded.MyLocation)
            return@Column
        }
        val fix = live.fix?.takeIf { it.time > 0 }
        // Each pursuit is a compass chip; tapping one opens its line and shows it on the map, so the map keeps the screen.
        val list = ventures(data, fix, imperial, race, onFocus, onShrine)
        var open by remember { mutableStateOf<String?>(null) }
        val shown = list.firstOrNull { it.key == open }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterHorizontally)) {
            list.forEach { v ->
                VentureChip(v, v.key == open, Modifier.weight(1f, fill = false).widthIn(min = 76.dp)) {
                    open = if (open == v.key) null else v.key.also { v.focus?.invoke() }
                }
            }
        }
        if (shown != null) {
            Spacer(Modifier.height(6.dp))
            VentureRow(
                shown.icon, shown.color, shown.soft, shown.title, shown.text,
                bearing = shown.bearing, onClose = shown.onClose, closeLabel = shown.closeLabel ?: stringResource(R.string.race_cancel),
                onClick = shown.onClick, lines = 3,
            )
            shown.extra?.invoke()
        }
        if (live.tooFast) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth().clip(U.RSm).background(U.c.flameSoft).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                GameIcon(Icons.Rounded.Speed, null, Modifier.size(18.dp), tint = U.c.flame)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.too_fast), style = UType.Muted.copy(color = U.c.ink))
            }
        }
        Spacer(Modifier.height(8.dp))
        if (s.walking) {
            val now = rememberNow(1000)
            val w = live.walk?.takeIf { now - it.end < 20 * 60_000L }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.walking_now), style = UType.Eyebrow.copy(color = U.c.accent))
                    Spacer(Modifier.height(3.dp))
                    Text(
                        if (w == null) stringResource(R.string.walking_wait)
                        else listOfNotNull(
                            clock(now - w.start), distance(w.meters, imperial), "+" + w.cells,
                            if ((s.steps || s.healthFrom >= 0) && w.steps > 0) pluralStringResource(R.plurals.steps, w.steps, w.steps) else null,
                        ).joinToString("  ·  "),
                        style = UType.Strong,
                    )
                }
                PrimaryButton(stringResource(R.string.stop_walk), { ExploreService.stop(context) }, color = U.c.ink, icon = Icons.Rounded.Stop)
            }
        } else {
            // The screen-off promise rides on a toast at the start rather than a line under the button.
            val hint = stringResource(R.string.start_walk_hint)
            PrimaryButton(
                stringResource(R.string.start_walk),
                {
                    ExploreService.start(context)
                    android.widget.Toast.makeText(context, hint, android.widget.Toast.LENGTH_SHORT).show()
                },
                Modifier.fillMaxWidth(),
                icon = Icons.AutoMirrored.Rounded.DirectionsWalk,
            )
        }
    }
}

/** One pursuit on the map: its chip in the strip, and the line it opens. */
private class Venture(
    val key: String, val icon: ImageVector, val color: Color, val soft: Color,
    val title: String, val text: String, val short: String,
    val bearing: Double? = null, val done: Boolean = false,
    /** Shows it on the map when its chip is opened. */
    val focus: (() -> Unit)? = null,
    /** A tap on the opened line. */
    val onClick: (() -> Unit)? = null,
    val onClose: (() -> Unit)? = null, val closeLabel: String? = null,
    val extra: (@Composable () -> Unit)? = null,
)

/** What the walker is after: a treasure, a rumoured chest, a race, the altars and the day's shrines. */
@Composable
private fun ventures(
    data: GameData, fix: app.fogwalk.explore.Fix?, imperial: Boolean, race: RaceView?,
    onFocus: (Double, Double) -> Unit, onShrine: (Shrine) -> Unit,
): List<Venture> {
    val context = LocalContext.current
    val c = content()
    val now = rememberNow(1000)
    val s = data.settings
    val list = mutableListOf<Venture>()
    s.treasure?.let { t ->
        val toCentre = fix?.let { Grid.distance(it.lat, it.lon, t.cLat, t.cLon) }
        val warmth = if (fix != null && toCentre != null && toCentre <= t.radius) Treasures.warmth(Grid.distance(fix.lat, fix.lon, t.lat, t.lon)) else null
        val text = when {
            toCentre == null -> stringResource(R.string.treasure_hint)
            warmth == null -> stringResource(R.string.treasure_to_circle, distance(toCentre - t.radius, imperial))
            else -> stringResource(listOf(R.string.treasure_hot, R.string.treasure_warm, R.string.treasure_cool).getOrElse(warmth) { R.string.treasure_cold })
        }
        val short = when {
            toCentre == null -> "…"
            warmth == null -> distance(toCentre - t.radius, imperial)
            else -> stringResource(listOf(R.string.treasure_hot_short, R.string.treasure_warm_short, R.string.treasure_cool_short).getOrElse(warmth) { R.string.treasure_cold_short })
        }
        val go = { onFocus(t.cLat, t.cLon) }
        list += Venture(
            "treasure", Icons.Rounded.Map, U.c.gold, U.c.goldSoft, stringResource(R.string.treasure_one), text, short,
            bearing = if (fix != null && warmth == null) Grid.bearing(fix.lat, fix.lon, t.cLat, t.cLon) else null,
            focus = go, onClick = go,
        )
    }
    data.rumor?.let { r ->
        val m = fix?.let { distance(Grid.distance(it.lat, it.lon, r.lat, r.lon), imperial) }
        val go = { onFocus(r.lat, r.lon) }
        list += Venture(
            "rumor", Icons.Rounded.RecordVoiceOver, U.c.flame, U.c.flameSoft, stringResource(R.string.rumor),
            stringResource(R.string.rumor_line, c.rarity(r.rarity), m ?: "…"), m ?: "…",
            bearing = fix?.let { Grid.bearing(it.lat, it.lon, r.lat, r.lon) }, focus = go, onClick = go,
        )
    }
    if (race != null) {
        val start = race.route.first()
        val toStart = fix?.let { distance(Grid.distance(it.lat, it.lon, start.first, start.second), imperial) }
        val (text, short) = if (race.started == 0L) {
            stringResource(R.string.race_to_start, toStart ?: "…") to (toStart ?: "…")
        } else {
            val gap = race.shadowM(now) - race.along[race.reached]
            when {
                gap > 10 -> stringResource(R.string.race_behind, distance(gap, imperial)) to "−" + distance(gap, imperial)
                gap < -10 -> stringResource(R.string.race_ahead, distance(-gap, imperial)) to "+" + distance(-gap, imperial)
                else -> stringResource(R.string.race_even) to stringResource(R.string.race_even_short)
            }
        }
        val go = { onFocus(start.first, start.second) }
        list += Venture(
            "race", Icons.Rounded.Timer, U.c.sage, U.c.sageSoft, stringResource(R.string.race_title), text, short,
            bearing = if (race.started == 0L && fix != null) Grid.bearing(fix.lat, fix.lon, start.first, start.second) else null,
            focus = go, onClick = go, onClose = { context.container.explorer.cancelRace() },
        )
    }
    if (s.altars > 0 || s.ring.isNotEmpty()) list += altars(data, imperial, onFocus)
    // The shrines: the one set out for, or else the nearest; its opened line brings up the shrine's own card.
    val sd = s.shrines.takeIf { it.day == data.today }
    val reached = data.todayRow.beacon
    val b = sd?.target(fix?.lat, fix?.lon)
    val color = if (reached || b == null) U.c.accent else shrineColor(b.kind)
    list += Venture(
        "shrine", Icons.Rounded.Flare, color, if (reached || b == null) U.c.accentSoft else color.copy(alpha = 0.18f), stringResource(R.string.shrines_of_day),
        when {
            reached && sd?.chosen != null -> stringResource(R.string.shrine_done, shrineName(context, sd.chosen))
            reached -> stringResource(R.string.beacon_done)
            b == null || fix == null -> stringResource(R.string.beacon_waiting)
            else -> stringResource(
                if (sd.chosen != null) R.string.shrine_heading else R.string.shrine_nearest,
                shrineName(context, b.kind), distance(Grid.distance(fix.lat, fix.lon, b.lat, b.lon), imperial),
            )
        },
        when {
            reached -> stringResource(R.string.shrine_done_short)
            b == null || fix == null -> "…"
            else -> distance(Grid.distance(fix.lat, fix.lon, b.lat, b.lon), imperial)
        },
        bearing = if (b != null && fix != null && !reached) Grid.bearing(fix.lat, fix.lon, b.lat, b.lon) else null,
        done = reached,
        focus = b?.let { { onFocus(it.lat, it.lon) } },
        onClick = b?.let { { onShrine(it) } },
    )
    return list
}

/** The altars from azure shrines: set them around a place that cannot be walked, then close the ring. */
@Composable
private fun altars(data: GameData, imperial: Boolean, onFocus: (Double, Double) -> Unit): Venture {
    val context = LocalContext.current
    val container = context.container
    val s = data.settings
    val azure = shrineColor(app.fogwalk.domain.ShrineKind.AZURE)
    val side = distance(Altars.MAX_SIDE_M, imperial)
    val first = s.ring.firstOrNull()
    return Venture(
        "altars", Icons.Rounded.ChangeHistory, azure, azure.copy(alpha = 0.18f), stringResource(R.string.altars_title),
        if (s.ring.isEmpty()) stringResource(R.string.altars_hint, Altars.MIN, side)
        else stringResource(R.string.altars_line, s.altars, s.ring.size),
        if (s.ring.isEmpty()) "×${s.altars}" else "${s.ring.size} / ${maxOf(Altars.MIN, s.ring.size)}",
        focus = first?.let { { onFocus(it.first, it.second) } },
        onClick = first?.let { { onFocus(it.first, it.second) } },
        onClose = if (s.ring.isNotEmpty()) { { container.explorer.liftAltars() } } else null,
        closeLabel = stringResource(R.string.altar_lift),
    ) {
        val say: (Bought) -> Unit = { b ->
            val text = when (b) {
                Bought.NO_FIX -> context.getString(R.string.altar_no_fix)
                Bought.FAR -> context.getString(R.string.altar_far, side)
                else -> null
            }
            if (text != null) android.widget.Toast.makeText(context, text, android.widget.Toast.LENGTH_SHORT).show()
        }
        // How it works, with an example and the limits: the line alone is too short to learn from (#28).
        var guide by remember { mutableStateOf(false) }
        if (guide) androidx.compose.material3.AlertDialog(
            onDismissRequest = { guide = false },
            containerColor = U.c.bg,
            title = { Text(stringResource(R.string.altars_guide_t), style = UType.Strong.copy(fontSize = 19.sp)) },
            text = {
                Text(
                    stringResource(R.string.altars_guide, Altars.MIN, side, distance(app.fogwalk.explore.Explorer.ALTAR_ACCURACY.toDouble(), imperial)),
                    style = UType.Sub, modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { guide = false }) { Text(stringResource(R.string.close), color = U.c.accent, style = UType.Button) } },
        )
        Row(Modifier.padding(start = 50.dp, top = 2.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(stringResource(R.string.altars_how), U.c.surface2) { guide = true }
            if (s.altars > 0) Chip(stringResource(R.string.altar_place), azure.copy(alpha = 0.18f)) {
                container.scope.launch {
                    val b = container.explorer.placeAltar()
                    withContext(Dispatchers.Main) { say(b) }
                }
            }
            if (s.ring.size >= Altars.MIN) Chip(stringResource(R.string.altar_close), if (Altars.canClose(s.ring)) U.c.accentSoft else U.c.surface2) {
                container.scope.launch {
                    val b = container.explorer.closeRing()
                    withContext(Dispatchers.Main) { if (b == Bought.FAR) android.widget.Toast.makeText(context, context.getString(R.string.ring_too_long, side), android.widget.Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }
}

/** A pursuit folded to its icon, a compass needle toward it and how far it is. */
@Composable
private fun VentureChip(v: Venture, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.clip(U.RSm).background(if (selected) U.c.surface2 else Color.Transparent)
            .clickable(onClickLabel = v.title, onClick = onClick).padding(vertical = 5.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(44.dp)) {
            Box(Modifier.size(38.dp).align(Alignment.Center).clip(CircleShape).background(v.soft), contentAlignment = Alignment.Center) {
                GameIcon(v.icon, v.title, Modifier.size(20.dp), tint = v.color)
            }
            if (v.bearing != null || v.done) {
                Box(
                    Modifier.size(19.dp).align(Alignment.BottomEnd).shadow(2.dp, CircleShape).clip(CircleShape).background(U.c.surface),
                    contentAlignment = Alignment.Center,
                ) {
                    if (v.bearing != null) GameIcon(Icons.Rounded.Navigation, null, Modifier.size(14.dp).rotate(v.bearing.toFloat()), tint = v.color)
                    else GameIcon(Icons.Rounded.Check, null, Modifier.size(14.dp), tint = v.color)
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(v.short, style = UType.Small.copy(fontSize = 12.sp, color = U.c.ink), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Lets a row reach past its parent's padding by [x] on each side, so its pressed shade has room around the icon. */
private fun Modifier.bleed(x: Dp) = layout { m, cs ->
    val px = x.roundToPx()
    if (!cs.hasBoundedWidth) return@layout m.measure(cs).let { p -> layout(p.width, p.height) { p.place(0, 0) } }
    val p = m.measure(cs.copy(minWidth = cs.minWidth + 2 * px, maxWidth = cs.maxWidth + 2 * px))
    layout(p.width - 2 * px, p.height) { p.place(-px, 0) }
}

@Composable
private fun VentureRow(
    icon: ImageVector, color: Color, soft: Color, title: String, text: String,
    bearing: Double? = null, onClose: (() -> Unit)? = null, closeLabel: String = stringResource(R.string.race_cancel), onClick: (() -> Unit)?,
    lines: Int = 2,
) {
    Row(
        Modifier.fillMaxWidth().bleed(8.dp).clip(U.RSm).then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(38.dp).clip(CircleShape).background(soft), contentAlignment = Alignment.Center) {
            GameIcon(icon, null, Modifier.size(20.dp), tint = color)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = UType.Strong.copy(fontSize = 15.sp))
            Text(text, style = UType.Muted, maxLines = lines, overflow = TextOverflow.Ellipsis)
        }
        if (bearing != null) GameIcon(Icons.Rounded.Navigation, null, Modifier.size(26.dp).rotate(bearing.toFloat()), tint = color)
        if (onClose != null) {
            Spacer(Modifier.width(6.dp))
            GameIcon(Icons.Rounded.Close, closeLabel, Modifier.size(22.dp).clip(CircleShape).clickable(onClick = onClose), tint = U.c.faint)
        }
    }
}

private fun clock(ms: Long): String {
    val t = (ms / 1000).coerceAtLeast(0)
    return if (t >= 3600) "%d:%02d:%02d".format(t / 3600, t / 60 % 60, t % 60) else "%d:%02d".format(t / 60, t % 60)
}

/** Night tiles: inverted, hue turned back so water stays blue, and dimmed. */
internal fun nightTiles(): ColorMatrixColorFilter {
    val invert = ColorMatrix(floatArrayOf(-1f, 0f, 0f, 0f, 255f, 0f, -1f, 0f, 0f, 255f, 0f, 0f, -1f, 0f, 255f, 0f, 0f, 0f, 1f, 0f))
    val hue = ColorMatrix(floatArrayOf(-0.574f, 1.43f, 0.144f, 0f, 0f, 0.426f, 0.43f, 0.144f, 0f, 0f, 0.426f, 1.43f, -0.856f, 0f, 0f, 0f, 0f, 0f, 1f, 0f))
    invert.postConcat(hue)
    val dim = ColorMatrix()
    dim.setScale(0.86f, 0.8f, 0.68f, 1f)
    invert.postConcat(dim)
    return ColorMatrixColorFilter(invert)
}

/** Day tiles as an old atlas: colour drained toward sepia ink on parchment. */
internal fun softTiles(): ColorMatrixColorFilter {
    val m = ColorMatrix()
    m.setSaturation(0.35f)
    val warm = ColorMatrix(floatArrayOf(1.0f, 0f, 0f, 0f, 6f, 0f, 0.93f, 0f, 0f, 2f, 0f, 0f, 0.78f, 0f, -4f, 0f, 0f, 0f, 1f, 0f))
    m.postConcat(warm)
    return ColorMatrixColorFilter(m)
}
