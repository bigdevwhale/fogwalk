package app.fogwalk.ui.treasury

import app.fogwalk.ui.components.frame
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fogwalk.R
import app.fogwalk.container
import app.fogwalk.domain.Companions
import app.fogwalk.domain.Gear
import app.fogwalk.domain.Hook
import app.fogwalk.domain.Monuments
import app.fogwalk.domain.Rumors
import app.fogwalk.domain.Sectors
import app.fogwalk.domain.Shop
import app.fogwalk.domain.Treasures
import app.fogwalk.explore.Bought
import app.fogwalk.geo.Grid
import app.fogwalk.ui.Art
import app.fogwalk.ui.ArtImage
import app.fogwalk.ui.GameData
import app.fogwalk.ui.Go
import app.fogwalk.ui.MapFocus
import app.fogwalk.ui.components.Banner
import app.fogwalk.ui.components.PrimaryButton
import app.fogwalk.ui.components.UCard
import app.fogwalk.ui.content
import app.fogwalk.ui.map.CoinIcon
import app.fogwalk.ui.map.FogStyles
import app.fogwalk.ui.map.Sprites
import app.fogwalk.ui.theme.U
import app.fogwalk.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** The merchant's rooms: one shown at a time, each under its own painted banner. */
private enum class Ware(val chip: Int, val note: Int, @DrawableRes val banner: Int, @DrawableRes val sign: Int) {
    LOOKS(R.string.shop_to_looks, R.string.shop_looks_note, R.drawable.banner_looks, R.drawable.companion_hedgehog),
    CHARMS(R.string.shop_to_boosts, R.string.shop_boosts_note, R.drawable.banner_charms, R.drawable.item_seeing_stone),
    MAPS(R.string.shop_to_treasure, R.string.shop_treasure_note, R.drawable.banner_maps, R.drawable.item_map_seeker),
    GEAR(R.string.shop_to_gear, R.string.shop_gear_note, R.drawable.banner_gear, R.drawable.gear_boots),
    MONUMENTS(R.string.shop_to_monuments, R.string.shop_monuments_note, R.drawable.banner_monuments, R.drawable.monument_2),
    TAVERN(R.string.shop_to_tavern, R.string.shop_tavern_note, R.drawable.banner_tavern, R.drawable.item_rumor),
}

/**
 * A thing on the merchant's shelf: a tile with its picture, name, a word on where it stands and its price; tapped,
 * a sheet tells the rest and buys it. [price] is null when there is nothing to buy (owned, mastered, sold out);
 * then [act], if any, is what the sheet's button does instead (wear a style, choose a companion).
 */
private class Offer(
    val key: String,
    val title: String,
    val status: String?,
    val details: String,
    val price: Int?,
    val art: @Composable (Modifier) -> Unit,
    val level: Pair<Int, Int>? = null,
    val active: Boolean = false,
    val act: Pair<String, () -> Unit>? = null,
    val buy: (() -> Unit)? = null,
)

@Composable
fun TreasuryTab(data: GameData, go: Go, focus: MutableStateFlow<MapFocus?>) {
    var ware by rememberSaveable { mutableStateOf(Ware.LOOKS) }
    var open by remember { mutableStateOf<String?>(null) }
    val offers = offers(data, ware)
    Column(Modifier.fillMaxSize().statusBarsPadding()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Banner(ware.banner, Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Column(Modifier.align(Alignment.CenterStart).padding(start = 18.dp, end = 120.dp)) {
                    Text(stringResource(R.string.tab_treasury).uppercase(), style = UType.Eyebrow.copy(color = Color(0xEEFFF6E0)))
                    Text(stringResource(ware.chip), style = UType.Title.copy(color = Color(0xFFFFF6E0)))
                    Spacer(Modifier.height(2.dp))
                    Text(stringResource(ware.note), style = UType.Small.copy(color = Color(0xEEFFF6E0), fontSize = 13.sp), maxLines = 3)
                }
                Coins(data.coins, Modifier.align(Alignment.TopEnd).padding(10.dp))
            }
            // The rooms as painted signs, all in sight at once; the one open sits larger in a gold ring.
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Ware.entries.forEach { w ->
                    val on = w == ware
                    val name = stringResource(w.chip)
                    Box(
                        Modifier.size(if (on) 58.dp else 50.dp).clip(CircleShape)
                            .background(if (on) U.c.goldSoft else U.c.surface)
                            .border(if (on) 2.5.dp else 1.dp, if (on) U.c.gold else U.c.gilt.copy(alpha = 0.6f), CircleShape)
                            .clickable { ware = w },
                        contentAlignment = Alignment.Center,
                    ) {
                        androidx.compose.foundation.Image(
                            androidx.compose.ui.res.painterResource(w.sign), name,
                            Modifier.size(if (on) 44.dp else 36.dp).graphicsLayer(alpha = if (on) 1f else 0.85f),
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Column(Modifier.padding(horizontal = 12.dp)) {
                if (ware == Ware.MAPS) ActiveTreasure(data)
                if (ware == Ware.MONUMENTS && offers.isEmpty()) Text(stringResource(R.string.monuments_none), style = UType.Muted, modifier = Modifier.padding(6.dp))
                offers.groupBy { it.key.substringBefore(':') }.forEach { (_, group) ->
                    group.chunked(2).forEach { row ->
                        Row(Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { o -> Tile(o, data.coins, Modifier.weight(1f)) { open = o.key } }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
    offers.firstOrNull { it.key == open }?.let { o -> OfferSheet(o, data.coins) { open = null } }
}

/** The purse in the banner's corner. */
@Composable
private fun Coins(coins: Int, modifier: Modifier) {
    Row(modifier.clip(CircleShape).background(U.c.surface.copy(alpha = 0.92f)).padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        CoinIcon(Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("%,d".format(coins), style = UType.Strong.copy(color = U.c.gold))
    }
}

@Composable
private fun Tile(o: Offer, coins: Int, modifier: Modifier, onClick: () -> Unit) {
    Column(
        modifier.frame(selected = o.active)
            .clickable(onClick = onClick).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1.5f).clip(U.RSm).background(U.c.surface2), contentAlignment = Alignment.Center) {
            o.art(Modifier.fillMaxSize().padding(8.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(o.title, style = UType.Strong.copy(fontSize = 14.sp, textAlign = TextAlign.Center), maxLines = 2, overflow = TextOverflow.Ellipsis)
        o.level?.let { (l, max) -> Pips(l, max) }
        Spacer(Modifier.height(4.dp))
        when {
            o.price != null -> Price(o.price, coins >= o.price)
            o.status != null -> Text(o.status, style = UType.Small.copy(color = if (o.active) U.c.accent else U.c.muted), maxLines = 1)
        }
    }
}

/** A piece of gear's level as filled and empty beads. */
@Composable
private fun Pips(level: Int, max: Int) {
    Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(max) { i -> Box(Modifier.size(7.dp).clip(CircleShape).background(if (i < level) U.c.gold else U.c.line)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OfferSheet(o: Offer, coins: Int, onClose: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = U.c.bg) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) { o.art(Modifier.fillMaxSize()) }
            Spacer(Modifier.height(10.dp))
            Text(o.title, style = UType.Title.copy(fontSize = 22.sp, textAlign = TextAlign.Center))
            o.level?.let { (l, max) -> Pips(l, max) }
            Spacer(Modifier.height(10.dp))
            Text(o.details, style = UType.Sub.copy(textAlign = TextAlign.Center))
            Spacer(Modifier.height(20.dp))
            when {
                o.price != null && o.buy != null -> {
                    val enough = coins >= o.price
                    PrimaryButton(
                        stringResource(R.string.shop_buy_for, o.price), { o.buy.invoke(); onClose() }, Modifier.fillMaxWidth(),
                        enabled = enough, color = U.c.accent,
                    )
                    if (!enough) {
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.shop_short_by, o.price - coins), style = UType.Small)
                    }
                }
                o.act != null -> PrimaryButton(o.act.first, { o.act.second(); onClose() }, Modifier.fillMaxWidth(), color = U.c.accent)
                o.status != null -> Text(o.status, style = UType.Strong.copy(color = U.c.muted))
            }
        }
    }
}

/** The shelf of [ware], as offers. */
@Composable
private fun offers(data: GameData, ware: Ware): List<Offer> {
    val context = LocalContext.current
    val container = context.container
    val c = content()
    val s = data.settings
    val short = stringResource(R.string.not_enough)
    val selected = stringResource(R.string.style_active)
    val owned = stringResource(R.string.shop_owned)
    val choose = stringResource(R.string.shop_choose)

    fun buy(item: String, price: Int, apply: suspend () -> Unit) {
        container.scope.launch {
            if (container.explorer.buy(item, price)) apply()
            else withContext(Dispatchers.Main) { Toast.makeText(context, short, Toast.LENGTH_SHORT).show() }
        }
    }

    fun art(@DrawableRes id: Int): @Composable (Modifier) -> Unit = { m -> ArtImage(id, m) }
    val out = ArrayList<Offer>()
    when (ware) {
        Ware.LOOKS -> {
            Companions.all.forEach { pal ->
                val has = pal.price == 0 || Companions.key(pal.id) in s.owned
                val on = s.companion == pal.id
                val select = { container.scope.launch { container.settings.update { it.copy(companion = pal.id) } }; Unit }
                out += Offer(
                    "pal:${pal.id}", companionName(pal.id), if (on) selected else if (has) owned else null,
                    stringResource(R.string.shop_companion_details), if (has) null else pal.price,
                    art = { m -> Sprites.ids["companion_${pal.id}"]?.let { ArtImage(it, m) } ?: Box(m, contentAlignment = Alignment.Center) { CompanionToken() } },
                    active = on, act = if (has && !on) choose to select else null,
                    buy = { buy("pal:${pal.id}", pal.price) { container.settings.update { it.copy(owned = it.owned + Companions.key(pal.id), companion = pal.id) } } },
                )
            }
            Shop.styles.forEach { st ->
                val has = st.id in s.owned
                val on = s.fog == st.id
                val select = { container.scope.launch { container.settings.update { it.copy(fog = st.id) } }; Unit }
                out += Offer(
                    "style:${st.id}", c.styleName(st.id), if (on) selected else if (has) owned else null,
                    c.styleText(st.id) + "\n\n" + stringResource(R.string.shop_styles_note), if (has) null else st.price,
                    art = { m -> Box(m, contentAlignment = Alignment.Center) { StylePreview(st.id) } },
                    active = on, act = if (has && !on) choose to select else null,
                    buy = { buy("style:${st.id}", st.price) { container.settings.update { it.copy(owned = it.owned + st.id, fog = st.id) } } },
                )
            }
        }
        Ware.CHARMS -> {
            val now = System.currentTimeMillis()
            val boost = Gear.boostMs(s.gear)
            val boostMin = (boost / 60_000).toInt()
            out += Offer(
                "charm:torch", stringResource(R.string.lantern), null,
                stringResource(R.string.lantern_text, boostMin, (Gear.revealM(s.gear) * Shop.LANTERN_FACTOR).toInt(), Gear.revealM(s.gear).toInt()),
                Shop.LANTERN, art(Art.TORCH),
                buy = { buy("lantern", Shop.LANTERN) { container.settings.update { it.copy(lanternUntil = maxOf(it.lanternUntil, now) + boost) } } },
            )
            out += Offer(
                "charm:stone", stringResource(R.string.radar), null, stringResource(R.string.radar_text, boostMin), Shop.RADAR, art(Art.SEEING_STONE),
                buy = { buy("radar", Shop.RADAR) { container.settings.update { it.copy(radarUntil = maxOf(it.radarUntil, now) + boost) } } },
            )
            val full = s.shields >= Shop.MAX_SHIELDS
            out += Offer(
                "charm:ward", stringResource(R.string.shield), stringResource(R.string.shop_have, s.shields, Shop.MAX_SHIELDS),
                stringResource(R.string.shield_text, s.shields, Shop.MAX_SHIELDS), if (full) null else Shop.SHIELD, art(Art.WARD),
                buy = { buy("shield", Shop.SHIELD) { container.settings.update { it.copy(shields = (it.shields + 1).coerceAtMost(Shop.MAX_SHIELDS)) } } },
            )
            val canReroll = s.shrines.day == data.today && !data.todayRow.beacon
            val rerollPrice = Shop.rerollPrice(Shop.rerollsOn(data.today, data.ledger, data.zone))
            out += Offer(
                "charm:scroll", stringResource(R.string.reroll), if (canReroll) null else stringResource(R.string.shop_reroll_not_now),
                stringResource(R.string.reroll_text), if (canReroll) rerollPrice else null, art(Art.SCROLL),
                buy = { container.scope.launch { report(context, container.explorer.rerollShrines()) } },
            )
            // Altars for gold (#51): dear, and dearer with each one in a week.
            val bought = Shop.altarsBought(data.week, data.ledger)
            val altarPrice = Shop.altarPrice(bought)
            out += Offer(
                "charm:altar", stringResource(R.string.altar_buy), if (altarPrice == null) stringResource(R.string.shop_sold) else null,
                stringResource(R.string.altar_buy_text, s.altars) + "\n" +
                    if (altarPrice != null) stringResource(R.string.altar_buy_week, bought, Shop.ALTARS_A_WEEK) else stringResource(R.string.altar_buy_sold),
                altarPrice, art(Art.ALTAR),
                buy = { container.scope.launch { report(context, container.explorer.buyAltar()) } },
            )
        }
        Ware.MAPS -> {
            val active = s.treasure
            val names = listOf(R.string.treasure_0, R.string.treasure_1, R.string.treasure_2)
            val imperial = s.units == app.fogwalk.data.Units.IMPERIAL
            Treasures.tiers.forEachIndexed { i, t ->
                val span = app.fogwalk.ui.distance(t.minM, imperial) + "–" + app.fogwalk.ui.distance(t.maxM, imperial)
                out += Offer(
                    "map:$i", stringResource(names[i]), if (active != null) stringResource(R.string.shop_map_busy) else null,
                    stringResource(R.string.treasure_text, span, c.relicOf(t.rarity)) + "\n\n" + stringResource(R.string.shop_treasure_note),
                    if (active == null) t.price else null, art(Art.maps[i]),
                    buy = { container.scope.launch { report(context, container.explorer.buyTreasure(i)) } },
                )
            }
        }
        Ware.GEAR -> {
            val imperial = s.units == app.fogwalk.data.Units.IMPERIAL
            Gear.pieces.forEach { p ->
                val level = s.gear[p.id] ?: 0
                val price = Gear.price(level)
                val name = when (p.id) {
                    "cloak" -> R.string.gear_cloak
                    "senses" -> R.string.gear_senses
                    "satchel" -> R.string.gear_satchel
                    "hook" -> R.string.gear_hook
                    else -> R.string.gear_boots
                }
                out += Offer(
                    "gear:${p.id}", stringResource(name), if (price == null) stringResource(R.string.gear_max) else null,
                    gearEffect(p.id, level) + "\n" + if (price != null) stringResource(R.string.gear_next, gearValue(p.id, level + 1, imperial)) else stringResource(R.string.gear_max),
                    price, art(Art.gear(p.id)), level = level to Gear.MAX,
                    buy = { container.scope.launch { report(context, container.explorer.buyGear(p.id)) } },
                )
            }
        }
        Ware.MONUMENTS -> {
            // Gold with no end to spend it on (#50): the stone grows with each level.
            data.sectors.first.indices
                .filter { data.sectors.second[it].toDouble() / Grid.SECTOR_CELLS >= Sectors.CHARTED }
                .map { data.sectors.first[it] }
                .sortedWith(compareByDescending<Long> { data.monuments[it] ?: 0 }.thenByDescending { data.share(it) })
                .forEach { sector ->
                    val level = data.monuments[sector] ?: 0
                    val tier = Sprites.ids["monument_${Monuments.tier(maxOf(level, 1))}"]!!
                    out += Offer(
                        "land:$sector", c.sectorName(sector),
                        if (level > 0) stringResource(R.string.monument_level, level) else null,
                        (if (level > 0) stringResource(R.string.monument_level, level) else stringResource(R.string.monument_none_here, (data.share(sector) * 100).toInt())) +
                            "\n\n" + stringResource(R.string.monument_text_short, level + 1),
                        Monuments.price(level), { m -> ArtImage(tier, m, dim = level == 0) },
                        buy = { container.scope.launch { report(context, container.explorer.raiseMonument(sector)) } },
                    )
                }
        }
        Ware.TAVERN -> {
            val heard = s.rumor?.week == data.week
            out += Offer(
                "tavern:rumor", stringResource(R.string.rumor), if (heard) stringResource(R.string.shop_heard) else null,
                stringResource(if (heard) R.string.rumor_heard else R.string.rumor_text), if (heard) null else Rumors.PRICE, art(Art.RUMOR),
                buy = { container.scope.launch { report(context, container.explorer.hearRumor()) } },
            )
        }
    }
    return out
}

/** The treasure map being followed, with a way to throw it away. */
@Composable
private fun ActiveTreasure(data: GameData) {
    val active = data.settings.treasure ?: return
    val container = LocalContext.current.container
    val names = listOf(R.string.treasure_0, R.string.treasure_1, R.string.treasure_2)
    UCard(Modifier.padding(bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ArtImage(Art.maps[active.tier], Modifier.size(52.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.treasure_active, stringResource(names[active.tier])), style = UType.Strong)
                Text(stringResource(R.string.treasure_hint), style = UType.Muted)
                Text(
                    stringResource(R.string.treasure_drop), style = UType.Button.copy(color = U.c.danger),
                    modifier = Modifier.clip(U.RSm).clickable { container.explorer.dropTreasure() }.padding(vertical = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun gearEffect(id: String, lvl: Int): String {
    val g = mapOf(id to lvl)
    return when (id) {
        "cloak" -> stringResource(R.string.gear_cloak_text, Gear.revealM(g).toInt())
        "senses" -> stringResource(R.string.gear_senses_text, Gear.senseM(g).toInt())
        "satchel" -> stringResource(R.string.gear_satchel_text, (Gear.boostMs(g) / 60_000).toInt())
        "hook" -> stringResource(R.string.gear_hook_text, Hook.range(g).toInt(), Hook.throws(g))
        else -> stringResource(R.string.gear_boots_text, Gear.openM(g).toInt(), Gear.shrineM(g).toInt())
    }
}

/** What the next level brings, in exact metres: the usual distance rounds to tens, which would hide a 4 m step. */
@Composable
private fun gearValue(id: String, lvl: Int, imperial: Boolean): String {
    @Composable
    fun meters(m: Double) = if (imperial) stringResource(R.string.unit_ft, (m * 3.28084).toInt()) else stringResource(R.string.unit_m, m.toInt())
    val g = mapOf(id to lvl)
    return when (id) {
        "cloak" -> meters(Gear.revealM(g))
        "senses" -> meters(Gear.senseM(g))
        "satchel" -> stringResource(R.string.minutes_short, (Gear.boostMs(g) / 60_000).toInt())
        "hook" -> meters(Hook.range(g)) + if (Hook.throws(g) > Hook.throws(mapOf(id to lvl - 1))) ", " + stringResource(R.string.gear_hook_more) else ""
        // Both of what the boots do (#48): the chest's reach and the shrine's.
        else -> meters(Gear.openM(g)) + " / " + meters(Gear.shrineM(g))
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
private fun Price(price: Int, affordable: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        CoinIcon(Modifier.size(13.dp), if (affordable) U.c.gold else U.c.faint)
        Spacer(Modifier.width(4.dp))
        Text("%,d".format(price), style = UType.Strong.copy(fontSize = 14.sp, color = if (affordable) U.c.gold else U.c.faint))
    }
}

/** A companion's name in the player's language. */
@Composable
private fun companionName(id: String): String = stringResource(
    when (id) {
        "hedgehog" -> R.string.pal_hedgehog
        "cat" -> R.string.pal_cat
        "fox" -> R.string.pal_fox
        "owl" -> R.string.pal_owl
        "elder" -> R.string.pal_elder
        "granny" -> R.string.pal_granny
        else -> R.string.pal_token
    }
)

/** The gilt token with its emerald heart, as the map draws it. */
@Composable
private fun CompanionToken() {
    val gold = U.c.gold
    val accent = U.c.accent
    Canvas(Modifier.size(40.dp)) {
        val r = size.minDimension / 2
        drawCircle(Color(0x50000000), r * 0.95f, center.copy(y = center.y + r * 0.08f))
        drawCircle(gold, r * 0.92f)
        drawCircle(Color(0xFFFFF3D0), r * 0.72f)
        drawCircle(accent, r * 0.52f)
    }
}

/** The style's fog with a walked street cut through it. */
@Composable
private fun StylePreview(id: String) {
    val tex = remember(id) { FogStyles.texture(id).asImageBitmap() }
    Box(Modifier.fillMaxWidth().aspectRatio(1.25f).clip(U.RSm).background(Color(0xFFE8E4DA))) {
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
