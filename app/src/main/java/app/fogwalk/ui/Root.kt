package app.fogwalk.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import app.fogwalk.BuildConfig
import app.fogwalk.R
import app.fogwalk.container
import app.fogwalk.data.Entry
import app.fogwalk.data.Settings
import app.fogwalk.domain.Arms
import app.fogwalk.domain.Levels
import app.fogwalk.domain.Relics
import app.fogwalk.notify.Alarms
import app.fogwalk.ui.components.CelebrateDialog
import app.fogwalk.ui.components.ChestDialog
import app.fogwalk.ui.components.rememberNow
import app.fogwalk.ui.journal.JournalTab
import app.fogwalk.ui.map.MapTab
import app.fogwalk.ui.onboarding.OnboardingScreen
import app.fogwalk.ui.quests.QuestsTab
import app.fogwalk.ui.settings.SettingsScreen
import app.fogwalk.ui.theme.Sans
import app.fogwalk.ui.theme.U
import app.fogwalk.ui.treasury.TreasuryTab
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

enum class Tab { MAP, QUESTS, TREASURY, JOURNAL }

/** Everything a tab can open. */
class Go(private val nav: NavHostController, private val setTab: (Tab) -> Unit) {
    fun settings() = nav.navigate("settings")
    fun deeds() = nav.navigate("deeds")
    fun relics() = nav.navigate("relics")
    fun walks() = nav.navigate("walks")
    fun walk(id: Long) = nav.navigate("walk/$id")
    fun tab(t: Tab) = setTab(t)
    fun back() = nav.popBackStack()
}

/** Where the map should look when it opens next: the beacon, a cache, a sector. */
class MapFocus(val lat: Double, val lon: Double, val zoom: Double)

@Composable
fun FogwalkRoot(openRoute: MutableStateFlow<String?>, settings: Settings) {
    val context = LocalContext.current
    val container = context.container
    val nav = rememberNavController()
    var tab by rememberSaveable { mutableStateOf(Tab.MAP) }
    val go = remember(nav) { Go(nav) { tab = it } }
    val start = remember { if (settings.onboarded) "main" else "welcome" }
    val focus = remember { MutableStateFlow<MapFocus?>(null) }

    val dao = container.db.dao()
    val days by dao.observeDays().collectAsStateWithLifecycle(initialValue = null)
    val finds by dao.observeFinds().collectAsStateWithLifecycle(initialValue = emptyList())
    val ledger by dao.observeLedger().collectAsStateWithLifecycle(initialValue = emptyList())
    val walks by dao.observeWalks().collectAsStateWithLifecycle(initialValue = emptyList())
    val fogVersion by container.fog.version.collectAsStateWithLifecycle()
    val minute = rememberNow(60_000)
    val data = remember(days, finds, ledger, walks, fogVersion, settings, minute / 60_000) {
        val fog = container.fog
        GameData(settings, days.orEmpty(), finds, ledger, walks, fog.size, fog.areaM2, fog.sectors(), System.currentTimeMillis())
    }

    // Onboarding finished elsewhere (the demo seed, a restored backup) skips the welcome.
    LaunchedEffect(settings.onboarded) {
        if (settings.onboarded && nav.currentDestination?.route == "welcome") {
            nav.navigate("main") { popUpTo("welcome") { inclusive = true } }
        }
    }

    val requested by openRoute.collectAsStateWithLifecycle()
    LaunchedEffect(requested, settings.onboarded) {
        if (requested == null || !settings.onboarded) return@LaunchedEffect
        nav.popBackStack("main", inclusive = false)
        tab = when (requested) {
            "quests" -> Tab.QUESTS
            "chronicle", "deeds", "walks", "hoard" -> Tab.JOURNAL
            else -> Tab.MAP
        }
        if (requested == "deeds") go.deeds()
        if (requested == "hoard") go.relics()
        if (requested == "walks") go.walks()
        if (requested == "walk") data.walks.firstOrNull { it.meters > 500 }?.let { go.walk(it.id) }
        openRoute.value = null
    }

    val askPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(settings.onboarded) {
        if (settings.onboarded && Build.VERSION.SDK_INT >= 33 && !Alarms.canNotify(context) && !BuildConfig.DEMO) {
            askPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    NavHost(
        navController = nav,
        startDestination = start,
        modifier = Modifier.background(U.c.bg),
        enterTransition = { fadeIn(tween(240)) },
        exitTransition = { fadeOut(tween(180)) },
        popEnterTransition = { fadeIn(tween(240)) },
        popExitTransition = { fadeOut(tween(180)) },
    ) {
        composable("welcome") {
            OnboardingScreen { nav.navigate("main") { popUpTo("welcome") { inclusive = true } } }
        }
        composable("main") { MainTabs(tab, { tab = it }, go, data, focus) }
        composable("settings") { SettingsScreen(settings, onBack = { go.back() }) }
        composable("deeds") { app.fogwalk.ui.journal.DeedsScreen(data, onBack = { go.back() }) }
        composable("relics") { app.fogwalk.ui.journal.RelicsScreen(data, onBack = { go.back() }) }
        composable("walks") { app.fogwalk.ui.journal.WalksScreen(data, onBack = { go.back() }, onOpen = { go.walk(it) }) }
        composable("walk/{id}") { e ->
            val id = e.arguments?.getString("id")?.toLongOrNull() ?: -1L
            app.fogwalk.ui.journal.WalkScreen(id, data, onBack = { go.back() }, onRace = { walk ->
                container.explorer.startRace(walk)
                nav.popBackStack("main", inclusive = false)
                tab = Tab.MAP
            })
        }
    }

    if (settings.onboarded && days != null) Celebrations(data)
}

/** Chests, new badges, a new level and finished relic sets, each shown once. */
@Composable
private fun Celebrations(data: GameData) {
    val container = LocalContext.current.container
    val s = data.settings
    val c = content()
    val chest = data.finds.firstOrNull { !it.seen }
    val fresh = data.badges.filter { it.id !in s.seenBadges }
    val newSet = Relics.completeSets(data.relics).firstOrNull { "set:$it" !in s.seenBadges }
    val race = data.ledger.lastOrNull { it.ref.startsWith("race:") && it.ref !in s.seenBadges }
    val starUps = data.ledger.filter { it.ref.startsWith("star:") && it.ref !in s.seenBadges }
    val banners = data.ledger.filter { it.ref.startsWith("arms:") && it.ref !in s.seenBadges }
    when {
        race != null -> {
            val won = race.ref.endsWith(":w")
            // The margin is not stored; the walk's duration against the race's tells it.
            val parts = race.ref.split(":")
            val walk = data.walks.firstOrNull { it.id == parts.getOrNull(1)?.toLongOrNull() }
            val started = parts.getOrNull(2)?.toLongOrNull() ?: 0L
            val margin = if (walk != null) kotlin.math.abs((walk.end - walk.start) - (race.at - started)) else 0L
            val gap = "%d:%02d".format(margin / 60_000, margin / 1000 % 60)
            // The race was walked as a journey of its own, begun the moment it started; a won route can take the old one's place.
            val beaten = walk?.id
            val fresh = data.walks.filter { it.start in started..race.at }.minByOrNull { it.start }?.id?.takeIf { won && it !in s.pinned }
            val swap = beaten != null && beaten in s.pinned
            CelebrateDialog(
                icon = Icons.Rounded.Timer, color = if (won) U.c.sage else U.c.faint, soft = if (won) U.c.sageSoft else U.c.surface2,
                eyebrow = stringResource(R.string.race_title),
                title = stringResource(if (won) R.string.race_won else R.string.race_lost),
                text = stringResource(if (won) R.string.race_won_text else R.string.race_lost_text, gap, race.xp),
                confetti = won,
                action = fresh?.let { w ->
                    stringResource(if (swap) R.string.race_pin_swap else R.string.race_pin) to {
                        container.scope.launch { container.settings.update { it.copy(pinned = it.pinned - (if (swap) setOfNotNull(beaten) else emptySet()) + w) } }
                        Unit
                    }
                },
            ) { container.scope.launch { container.settings.update { it.copy(seenBadges = it.seenBadges + race.ref) } } }
        }
        chest != null -> ChestDialog(chest, data.relics, data.finds.count { it.relic.isNotEmpty() && it.relic == chest.relic && it.at <= chest.at }) {
            container.scope.launch { container.db.dao().markSeen(chest.key) }
        }
        newSet != null -> {
            val (coins, _) = Relics.setReward(newSet)
            CelebrateDialog(
                icon = setIcon(newSet), color = U.c.violet, soft = U.c.violetSoft,
                eyebrow = stringResource(R.string.set_complete),
                title = c.setName(newSet),
                text = stringResource(R.string.set_complete_text, c.setTitle(newSet), coins),
            ) { container.scope.launch { container.settings.update { it.copy(seenBadges = it.seenBadges + "set:$newSet") } } }
        }
        starUps.isNotEmpty() -> {
            // Stars found in a batch (or counted for the first time) show as one.
            val e = starUps.maxBy { it.ref.substringAfterLast(':').toIntOrNull() ?: 0 }
            val set = e.ref.split(":")[1]
            val star = e.ref.substringAfterLast(':').toIntOrNull() ?: 1
            CelebrateDialog(
                icon = setIcon(set), color = U.c.gold, soft = U.c.goldSoft,
                eyebrow = stringResource(R.string.set_star_up, star),
                title = c.setName(set),
                text = stringResource(R.string.set_star_up_text, c.setName(set), star, e.coins) +
                    if (starUps.size > 1) "\n" + stringResource(R.string.badge_more, starUps.size - 1) else "",
            ) { container.scope.launch { container.settings.update { it.copy(seenBadges = it.seenBadges + starUps.map { u -> u.ref }) } } }
        }
        banners.isNotEmpty() -> {
            val arm = banners.last().ref.substringAfter(':').toIntOrNull() ?: 0
            CelebrateDialog(
                icon = armIcon(arm), color = U.c.accent, soft = U.c.accentSoft,
                eyebrow = stringResource(R.string.arms_raised),
                title = c.armName(arm),
                text = stringResource(R.string.arms_raised_text, c.armName(arm), Arms.COINS) +
                    if (banners.size > 1) "\n" + stringResource(R.string.badge_more, banners.size - 1) else "",
            ) { container.scope.launch { container.settings.update { it.copy(seenBadges = it.seenBadges + banners.map { b -> b.ref }) } } }
        }
        fresh.isNotEmpty() -> {
            val b = fresh.last()
            val (color, soft) = badgeColor(b)
            CelebrateDialog(
                icon = badgeIcon(b), color = color, soft = soft,
                eyebrow = stringResource(R.string.badge_unlocked),
                title = c.badgeTitle(b.id),
                text = c.badgeText(b) + if (fresh.size > 1) "\n" + stringResource(R.string.badge_more, fresh.size - 1) else "",
            ) { container.scope.launch { container.settings.update { it.copy(seenBadges = it.seenBadges + fresh.map { f -> f.id }) } } }
        }
        data.level > s.levelSeen -> CelebrateDialog(
            icon = Icons.Rounded.Map, color = U.c.accent, soft = U.c.accentSoft,
            eyebrow = stringResource(R.string.level_up),
            title = stringResource(R.string.level_n, data.level) + " · " + c.rank(Levels.rank(data.level)),
            text = stringResource(R.string.level_up_text, (s.levelSeen + 1..data.level).sumOf { Levels.reward(it) }),
        ) {
            val level = data.level
            container.scope.launch {
                val now = System.currentTimeMillis()
                for (l in s.levelSeen + 1..level) container.db.dao().grant(Entry("lvl:$l", now, Levels.reward(l), 0))
                container.settings.update { it.copy(levelSeen = level) }
            }
        }
    }
}

@Composable
private fun MainTabs(tab: Tab, onTab: (Tab) -> Unit, go: Go, data: GameData, focus: MutableStateFlow<MapFocus?>) {
    // Back from any other tab returns to the map; from the map it leaves the app as before.
    BackHandler(enabled = tab != Tab.MAP) { onTab(Tab.MAP) }
    Column(Modifier.fillMaxSize().background(U.c.bg)) {
        Box(Modifier.weight(1f)) {
            when (tab) {
                Tab.MAP -> MapTab(data, go, focus)
                Tab.QUESTS -> QuestsTab(data, go, focus)
                Tab.TREASURY -> TreasuryTab(data, go, focus)
                Tab.JOURNAL -> JournalTab(data, go, focus)
            }
        }
        HorizontalDivider(color = U.c.line)
        Row(
            Modifier.fillMaxWidth().background(U.c.surface).navigationBarsPadding().height(66.dp).padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavItem(Icons.Rounded.Map, stringResource(R.string.tab_map), tab == Tab.MAP) { onTab(Tab.MAP) }
            NavItem(Icons.Rounded.TaskAlt, stringResource(R.string.tab_quests), tab == Tab.QUESTS, dot = data.toClaim > 0) { onTab(Tab.QUESTS) }
            NavItem(Icons.Rounded.Storefront, stringResource(R.string.tab_treasury), tab == Tab.TREASURY) { onTab(Tab.TREASURY) }
            NavItem(Icons.AutoMirrored.Rounded.MenuBook, stringResource(R.string.tab_journal), tab == Tab.JOURNAL) { onTab(Tab.JOURNAL) }
        }
    }
}

@Composable
private fun RowScope.NavItem(icon: ImageVector, label: String, selected: Boolean, dot: Boolean = false, onClick: () -> Unit) {
    val color = if (selected) U.c.accent else U.c.faint
    Column(
        Modifier
            .weight(1f)
            .clip(RoundedCornerShape(14.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box {
            Box(
                Modifier.clip(RoundedCornerShape(50)).background(if (selected) U.c.accentSoft else Color.Transparent).padding(horizontal = 18.dp, vertical = 4.dp),
            ) { Icon(icon, null, Modifier.size(22.dp), tint = color) }
            if (dot) Box(Modifier.align(Alignment.TopEnd).offset((-12).dp, 2.dp).size(9.dp).clip(CircleShape).background(U.c.flame))
        }
        Spacer(Modifier.height(3.dp))
        Text(label, fontFamily = Sans, fontSize = 11.5.sp, color = color, maxLines = 1)
    }
}
