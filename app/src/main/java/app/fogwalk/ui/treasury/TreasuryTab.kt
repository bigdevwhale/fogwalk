package app.fogwalk.ui.treasury

import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Flare
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.fogwalk.R
import app.fogwalk.container
import app.fogwalk.data.BeaconSpot
import app.fogwalk.domain.Beacons
import app.fogwalk.domain.Relic
import app.fogwalk.domain.Relics
import app.fogwalk.domain.Shop
import app.fogwalk.ui.GameData
import app.fogwalk.ui.Go
import app.fogwalk.ui.MapFocus
import app.fogwalk.ui.components.ConfirmDialog
import app.fogwalk.ui.components.Segmented
import app.fogwalk.ui.components.SectionTitle
import app.fogwalk.ui.components.UCard
import app.fogwalk.ui.content
import app.fogwalk.ui.map.FogStyles
import app.fogwalk.ui.map.CoinIcon
import app.fogwalk.ui.rarityColor
import app.fogwalk.ui.relicIcon
import app.fogwalk.ui.setIcon
import app.fogwalk.ui.theme.U
import app.fogwalk.ui.theme.UType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class Part { RELICS, SHOP }

@Composable
fun TreasuryTab(data: GameData, go: Go, focus: MutableStateFlow<MapFocus?>) {
    var part by rememberSaveable { mutableStateOf(Part.RELICS) }
    Column(
        Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp).padding(bottom = 24.dp),
    ) {
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
        Text(stringResource(R.string.treasury_sub, data.relics.size, Relics.all.size), style = UType.Sub)
        Spacer(Modifier.height(16.dp))
        Segmented(
            Part.entries, part,
            label = { stringResource(if (it == Part.RELICS) R.string.relics else R.string.shop) },
            onSelect = { part = it },
        )
        when (part) {
            Part.RELICS -> RelicsPart(data)
            Part.SHOP -> ShopPart(data)
        }
    }
}

@Composable
private fun RelicsPart(data: GameData) {
    val c = content()
    var shown by remember { mutableStateOf<Relic?>(null) }
    Relics.sets.forEach { set ->
        val relics = Relics.inSet(set)
        val have = relics.count { it.id in data.relics }
        val (color, soft) = rarityColor(Relics.setRarity(set))
        Spacer(Modifier.height(14.dp))
        UCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(CircleShape).background(soft), contentAlignment = Alignment.Center) {
                    Icon(setIcon(set), null, Modifier.size(20.dp), tint = color)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(c.setName(set), style = UType.Strong)
                    Text(
                        if (have == relics.size) stringResource(R.string.set_title, c.setTitle(set))
                        else stringResource(R.string.set_reward, Relics.setReward(set).first),
                        style = UType.Muted,
                    )
                }
                Text("$have/${relics.size}", style = UType.Strong.copy(color = if (have == relics.size) color else U.c.muted))
            }
            Spacer(Modifier.height(12.dp))
            relics.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { r ->
                        val owned = r.id in data.relics
                        Column(
                            Modifier.weight(1f).clip(U.RSm).background(if (owned) soft else U.c.surface2)
                                .clickable(enabled = owned) { shown = r }.padding(vertical = 10.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (owned) Icon(relicIcon(r.id), null, Modifier.size(26.dp), tint = color)
                            else Text("?", style = UType.Strong.copy(fontSize = 20.sp, color = U.c.faint))
                            Spacer(Modifier.height(6.dp))
                            Text(
                                if (owned) c.relicName(r.id) else stringResource(R.string.relic_unknown),
                                style = UType.Small.copy(color = if (owned) U.c.ink else U.c.faint, textAlign = TextAlign.Center),
                                maxLines = 2, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
    shown?.let { r ->
        val (color, soft) = rarityColor(r.rarity)
        val count = data.finds.count { it.relic == r.id }
        app.fogwalk.ui.components.CelebrateDialog(
            icon = relicIcon(r.id), color = color, soft = soft,
            eyebrow = c.rarity(r.rarity) + " · " + c.setName(r.set),
            title = c.relicName(r.id),
            text = c.relicLore(r.id) + "\n\n" + stringResource(R.string.relic_found_times, count),
            confetti = false,
        ) { shown = null }
    }
}

@Composable
private fun ShopPart(data: GameData) {
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

    SectionTitle(stringResource(R.string.fog_styles))
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
                    Text(c.styleText(st.id), style = UType.Small, maxLines = 2, minLines = 2)
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

    SectionTitle(stringResource(R.string.boosts))
    val now = System.currentTimeMillis()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BoostRow(Icons.Rounded.Lightbulb, U.c.gold, U.c.goldSoft, stringResource(R.string.lantern), stringResource(R.string.lantern_text), Shop.LANTERN, data.coins) {
            confirm = Triple(context.getString(R.string.lantern), Shop.LANTERN) {
                buy("lantern", Shop.LANTERN) { container.settings.update { it.copy(lanternUntil = maxOf(it.lanternUntil, now) + Shop.BOOST_MS) } }
            }
        }
        BoostRow(Icons.Rounded.Radar, U.c.violet, U.c.violetSoft, stringResource(R.string.radar), stringResource(R.string.radar_text), Shop.RADAR, data.coins) {
            confirm = Triple(context.getString(R.string.radar), Shop.RADAR) {
                buy("radar", Shop.RADAR) { container.settings.update { it.copy(radarUntil = maxOf(it.radarUntil, now) + Shop.BOOST_MS) } }
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
        val canReroll = s.beacon.day == data.today && !data.todayRow.beacon && s.rerollDay != data.today
        BoostRow(Icons.Rounded.Flare, U.c.flame, U.c.flameSoft, stringResource(R.string.reroll), stringResource(R.string.reroll_text), Shop.REROLL, data.coins, enabled = canReroll) {
            confirm = Triple(context.getString(R.string.reroll), Shop.REROLL) {
                val fix = container.explorer.live.value.fix
                if (fix != null) buy("reroll", Shop.REROLL) {
                    val (lat, lon) = Beacons.place(data.today, 1, fix.lat, fix.lon, data.level) { a, b -> container.fog.exploredAround(a, b, 120.0) }
                    container.settings.update { it.copy(beacon = BeaconSpot(data.today, lat, lon), rerollDay = data.today) }
                }
            }
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
                Text(title, style = UType.Strong.copy(fontSize = 15.sp, color = if (enabled) U.c.ink else U.c.faint))
                Text(text, style = UType.Muted)
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
