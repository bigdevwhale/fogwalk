package app.fogwalk.ui.treasury

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backpack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Hiking
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fogwalk.R
import app.fogwalk.container
import app.fogwalk.domain.ShrineDay
import app.fogwalk.domain.Beacons
import app.fogwalk.domain.Gear
import app.fogwalk.domain.Rumors
import app.fogwalk.domain.Treasures
import app.fogwalk.explore.Bought
import app.fogwalk.domain.Shop
import app.fogwalk.ui.GameData
import app.fogwalk.ui.Go
import app.fogwalk.ui.MapFocus
import app.fogwalk.ui.components.ConfirmDialog
import app.fogwalk.ui.components.SectionTitle
import app.fogwalk.ui.components.UCard
import app.fogwalk.ui.content
import app.fogwalk.ui.map.FogStyles
import app.fogwalk.ui.map.CoinIcon
import app.fogwalk.ui.theme.U
import app.fogwalk.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The merchant's wares by kind, each with a word on how long a purchase lasts. */
private enum class Ware(val title: Int, val chip: Int, val note: Int) {
    STYLES(R.string.fog_styles, R.string.shop_to_styles, R.string.shop_styles_note),
    BOOSTS(R.string.boosts, R.string.shop_to_boosts, R.string.shop_boosts_note),
    TREASURE(R.string.treasure_title, R.string.shop_to_treasure, R.string.shop_treasure_note),
    GEAR(R.string.gear_title, R.string.shop_to_gear, R.string.shop_gear_note),
    TAVERN(R.string.tavern_title, R.string.shop_to_tavern, R.string.shop_tavern_note),
}

@Composable
fun TreasuryTab(data: GameData, go: Go, focus: MutableStateFlow<MapFocus?>) {
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    val tops = remember { mutableStateMapOf<Ware, Int>() }
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.padding(horizontal = 18.dp)) {
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.tab_treasury), style = UType.Title, modifier = Modifier.weight(1f))
                Row(Modifier.clip(CircleShape).background(U.c.goldSoft).padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    CoinIcon(Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("%,d".format(data.coins), style = UType.Strong.copy(color = U.c.gold))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.treasury_sub), style = UType.Sub)
        }
        Spacer(Modifier.height(12.dp))
        // Quick ways to each kind of ware, so the gear is not a long scroll away.
        Row(Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Ware.entries.forEach { w ->
                Text(
                    stringResource(w.chip), style = UType.Strong.copy(fontSize = 15.sp, color = U.c.ink),
                    modifier = Modifier.clip(CircleShape).background(U.c.surface).border(1.dp, U.c.gilt.copy(alpha = 0.7f), CircleShape)
                        .clickable { scope.launch { scroll.animateScrollTo(tops[w] ?: 0) } }.padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(horizontal = 18.dp).padding(bottom = 24.dp)) {
            ShopPart(data) { w -> Modifier.onGloballyPositioned { tops[w] = it.positionInParent().y.toInt() } }
        }
    }
}

/** A kind of ware: its name and what buying it means. */
@Composable
private fun WareTitle(w: Ware, modifier: Modifier) {
    Column(modifier) {
        SectionTitle(stringResource(w.title))
        Text(stringResource(w.note), style = UType.Muted, modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp).offset(y = (-4).dp))
    }
}

@Composable
private fun ShopPart(data: GameData, mark: (Ware) -> Modifier) {
    val context = LocalContext.current
    val container = context.container
    val c = content()
    val s = data.settings
    var confirm by remember { mutableStateOf<Triple<String, Int, () -> Unit>?>(null) }
    val short = stringResource(R.string.not_enough)

    fun buy(item: String, price: Int, apply: suspend () -> Unit) {
        container.scope.launch {
            if (container.explorer.buy(item, price)) apply()
            else withContext(Dispatchers.Main) { Toast.makeText(context, short, Toast.LENGTH_SHORT).show() }
        }
    }

    WareTitle(Ware.STYLES, mark(Ware.STYLES))
    Shop.styles.chunked(2).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            row.forEach { st ->
                val owned = st.id in s.owned
                val active = s.fog == st.id
                Column(
                    Modifier.weight(1f).clip(U.RMd).background(U.c.surface)
                        .border(if (active) 2.dp else 1.dp, if (active) U.c.accent else U.c.line, U.RMd)
                        .clickable {
                            if (owned) container.scope.launch { container.settings.update { it.copy(fog = st.id) } }
                            else confirm = Triple(c.styleName(st.id), st.price) {
                                buy("style:${st.id}", st.price) { container.settings.update { it.copy(owned = it.owned + st.id, fog = st.id) } }
                            }
                        }
                        .padding(10.dp),
                ) {
                    StylePreview(st.id)
                    Spacer(Modifier.height(8.dp))
                    Text(c.styleName(st.id), style = UType.Strong.copy(fontSize = 14.sp))
                    Text(c.styleText(st.id), style = UType.Small.copy(fontSize = 13.sp), maxLines = 2, minLines = 2)
                    Spacer(Modifier.height(6.dp))
                    when {
                        active -> Text(stringResource(R.string.style_active), style = UType.Small.copy(color = U.c.accent))
                        owned -> Text(stringResource(R.string.style_use), style = UType.Small.copy(color = U.c.muted))
                        else -> Price(st.price, data.coins >= st.price)
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
    }

    WareTitle(Ware.BOOSTS, mark(Ware.BOOSTS))
    val now = System.currentTimeMillis()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        val boost = Gear.boostMs(s.gear)
        val boostMin = (boost / 60_000).toInt()
        BoostRow(Icons.Rounded.Lightbulb, U.c.gold, U.c.goldSoft, stringResource(R.string.lantern), stringResource(R.string.lantern_text, boostMin), Shop.LANTERN, data.coins) {
            confirm = Triple(context.getString(R.string.lantern), Shop.LANTERN) {
                buy("lantern", Shop.LANTERN) { container.settings.update { it.copy(lanternUntil = maxOf(it.lanternUntil, now) + boost) } }
            }
        }
        BoostRow(Icons.Rounded.Radar, U.c.violet, U.c.violetSoft, stringResource(R.string.radar), stringResource(R.string.radar_text, boostMin), Shop.RADAR, data.coins) {
            confirm = Triple(context.getString(R.string.radar), Shop.RADAR) {
                buy("radar", Shop.RADAR) { container.settings.update { it.copy(radarUntil = maxOf(it.radarUntil, now) + boost) } }
            }
        }
        BoostRow(
            Icons.Rounded.Shield, U.c.sky, U.c.skySoft, stringResource(R.string.shield),
            stringResource(R.string.shield_text, s.shields, Shop.MAX_SHIELDS), Shop.SHIELD, data.coins,
            enabled = s.shields < Shop.MAX_SHIELDS,
        ) {
            confirm = Triple(context.getString(R.string.shield), Shop.SHIELD) {
                buy("shield", Shop.SHIELD) { container.settings.update { it.copy(shields = (it.shields + 1).coerceAtMost(Shop.MAX_SHIELDS)) } }
            }
        }
        val canReroll = s.shrines.day == data.today && !data.todayRow.beacon && s.rerollDay != data.today
        BoostRow(Icons.Rounded.Flare, U.c.flame, U.c.flameSoft, stringResource(R.string.reroll), stringResource(R.string.reroll_text), Shop.REROLL, data.coins, enabled = canReroll) {
            confirm = Triple(context.getString(R.string.reroll), Shop.REROLL) {
                val fix = container.explorer.live.value.fix
                if (fix != null) buy("reroll", Shop.REROLL) {
                    val shrines = Beacons.place(data.today, 1, fix.lat, fix.lon, data.level) { a, b -> container.fog.exploredAround(a, b, 120.0) }
                    container.settings.update { it.copy(shrines = ShrineDay(data.today, shrines), rerollDay = data.today) }
                }
            }
        }
    }

    WareTitle(Ware.TREASURE, mark(Ware.TREASURE))
    TreasureMaps(data) { name, price, action -> confirm = Triple(name, price, action) }

    WareTitle(Ware.GEAR, mark(Ware.GEAR))
    GearRows(data) { name, price, action -> confirm = Triple(name, price, action) }

    WareTitle(Ware.TAVERN, mark(Ware.TAVERN))
    val heard = data.settings.rumor?.week == data.week
    BoostRow(
        Icons.Rounded.RecordVoiceOver, U.c.flame, U.c.flameSoft, stringResource(R.string.rumor),
        stringResource(if (heard) R.string.rumor_heard else R.string.rumor_text), Rumors.PRICE, data.coins, enabled = !heard,
    ) {
        confirm = Triple(context.getString(R.string.rumor), Rumors.PRICE) {
            container.scope.launch { report(context, container.explorer.hearRumor()) }
        }
    }

    confirm?.let { (name, price, action) ->
        ConfirmDialog(
            title = stringResource(R.string.buy_title, name),
            text = stringResource(R.string.buy_text, price, data.coins),
            confirm = stringResource(R.string.buy),
            onConfirm = action,
            onDismiss = { confirm = null },
        )
    }
}

/** What a purchase came to, said in a toast unless it simply went through. */
private suspend fun report(context: android.content.Context, b: Bought) {
    val text = when (b) {
        Bought.OK, Bought.BUSY -> return
        Bought.NO_GOLD -> R.string.not_enough
        Bought.NO_FIX -> R.string.need_fix
        Bought.NOTHING -> R.string.rumor_nothing
        Bought.FAR -> R.string.hook_far
    }
    withContext(Dispatchers.Main) { Toast.makeText(context, text, Toast.LENGTH_SHORT).show() }
}

@Composable
private fun TreasureMaps(data: GameData, ask: (String, Int, () -> Unit) -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val c = content()
    val active = data.settings.treasure
    val names = listOf(R.string.treasure_0, R.string.treasure_1, R.string.treasure_2)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (active != null) {
            UCard {
                Text(stringResource(R.string.treasure_active, stringResource(names[active.tier])), style = UType.Strong)
                Spacer(Modifier.height(2.dp))
                Text(stringResource(R.string.treasure_hint), style = UType.Muted)
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(R.string.treasure_drop), style = UType.Button.copy(color = U.c.danger),
                    modifier = Modifier.clip(U.RSm).clickable { container.explorer.dropTreasure() }.padding(vertical = 4.dp),
                )
            }
        }
        Treasures.tiers.forEachIndexed { i, t ->
            val name = stringResource(names[i])
            val imperial = data.settings.units == app.fogwalk.data.Units.IMPERIAL
            val span = app.fogwalk.ui.distance(t.minM, imperial) + "–" + app.fogwalk.ui.distance(t.maxM, imperial)
            BoostRow(
                Icons.Rounded.Map, U.c.gold, U.c.goldSoft, name, stringResource(R.string.treasure_text, span, c.relicOf(t.rarity)),
                t.price, data.coins, enabled = active == null,
            ) {
                ask(name, t.price) { container.scope.launch { report(context, container.explorer.buyTreasure(i)) } }
            }
        }
    }
}

@Composable
private fun GearRows(data: GameData, ask: (String, Int, () -> Unit) -> Unit) {
    val context = LocalContext.current
    val container = context.container
    val gear = data.settings.gear
    val imperial = data.settings.units == app.fogwalk.data.Units.IMPERIAL
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Gear.pieces.forEach { p ->
            val level = gear[p.id] ?: 0
            val price = Gear.price(level)
            val (icon, name) = when (p.id) {
                "cloak" -> Icons.Rounded.Cloud to R.string.gear_cloak
                "senses" -> Icons.Rounded.Visibility to R.string.gear_senses
                "satchel" -> Icons.Rounded.Backpack to R.string.gear_satchel
                else -> Icons.Rounded.Hiking to R.string.gear_boots
            }
            @Composable
            fun effect(lvl: Int): String {
                val g = mapOf(p.id to lvl)
                return when (p.id) {
                    "cloak" -> stringResource(R.string.gear_cloak_text, Gear.revealM(g).toInt())
                    "senses" -> stringResource(R.string.gear_senses_text, Gear.senseM(g).toInt())
                    "satchel" -> stringResource(R.string.gear_satchel_text, (Gear.boostMs(g) / 60_000).toInt())
                    else -> stringResource(R.string.gear_boots_text, Gear.openM(g).toInt(), Gear.shrineM(g).toInt())
                }
            }
            val title = stringResource(name) + " · " + stringResource(R.string.gear_level, level, Gear.MAX)
            // Exact metres: the usual distance rounds to tens, which would hide a 4 m step.
            @Composable
            fun meters(m: Double) = if (imperial) stringResource(R.string.unit_ft, (m * 3.28084).toInt()) else stringResource(R.string.unit_m, m.toInt())

            @Composable
            fun value(lvl: Int): String {
                val g = mapOf(p.id to lvl)
                return when (p.id) {
                    "cloak" -> meters(Gear.revealM(g))
                    "senses" -> meters(Gear.senseM(g))
                    "satchel" -> stringResource(R.string.minutes_short, (Gear.boostMs(g) / 60_000).toInt())
                    else -> meters(Gear.openM(g))
                }
            }
            val text = effect(level) + "\n" + if (price != null) stringResource(R.string.gear_next, value(level + 1)) else stringResource(R.string.gear_max)
            BoostRow(icon, U.c.sage, U.c.sageSoft, title, text, price ?: 0, data.coins, enabled = price != null) {
                if (price != null) ask(context.getString(name), price) { container.scope.launch { report(context, container.explorer.buyGear(p.id)) } }
            }
        }
    }
}

@Composable
private fun Price(price: Int, affordable: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CoinIcon(Modifier.size(13.dp), if (affordable) U.c.gold else U.c.faint)
        Spacer(Modifier.width(4.dp))
        Text("$price", style = UType.Strong.copy(fontSize = 14.sp, color = if (affordable) U.c.gold else U.c.faint))
    }
}

@Composable
private fun BoostRow(icon: ImageVector, color: Color, soft: Color, title: String, text: String, price: Int, coins: Int, enabled: Boolean = true, onClick: () -> Unit) {
    UCard(onClick = if (enabled) onClick else null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(soft), contentAlignment = Alignment.Center) {
                Icon(icon, null, Modifier.size(22.dp), tint = color)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = UType.Strong.copy(fontSize = 16.sp, color = if (enabled) U.c.ink else U.c.faint))
                Text(text, style = UType.Muted.copy(fontSize = 15.sp, lineHeight = 20.sp))
            }
            Spacer(Modifier.width(8.dp))
            if (enabled) Price(price, coins >= price) else Icon(Icons.Rounded.Check, null, Modifier.size(18.dp), tint = U.c.faint)
        }
    }
}

/** The style's fog with a walked street cut through it. */
@Composable
private fun StylePreview(id: String) {
    val tex = remember(id) { FogStyles.texture(id).asImageBitmap() }
    Box(Modifier.fillMaxWidth().aspectRatio(1.6f).clip(U.RSm).background(Color(0xFFE8E4DA))) {
        Canvas(Modifier.fillMaxSize()) {
            // A hint of streets under the fog.
            val road = Color(0xFFFFFFFF)
            drawLine(road, Offset(0f, size.height * 0.62f), Offset(size.width, size.height * 0.38f), 7f)
            drawLine(road, Offset(size.width * 0.35f, 0f), Offset(size.width * 0.55f, size.height), 5f)
            drawCircle(Color(0xFFB9D7A8), size.height * 0.16f, Offset(size.width * 0.78f, size.height * 0.75f))
        }
        Box(
            Modifier.fillMaxSize().graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
        ) {
            Image(tex, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            Canvas(Modifier.fillMaxSize()) {
                val center = Offset(size.width * 0.45f, size.height * 0.52f)
                drawCircle(
                    Brush.radialGradient(listOf(Color.Black, Color.Black, Color.Transparent), center, size.height * 0.42f),
                    size.height * 0.42f, center, blendMode = BlendMode.DstOut,
                )
            }
        }
    }
}
