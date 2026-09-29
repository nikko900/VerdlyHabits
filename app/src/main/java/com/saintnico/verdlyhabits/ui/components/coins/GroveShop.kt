package com.saintnico.verdlyhabits.ui.components.coins

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.model.CoinCosmetics
import com.saintnico.verdlyhabits.data.model.CosmeticBadge
import com.saintnico.verdlyhabits.data.model.CosmeticItem
import com.saintnico.verdlyhabits.data.model.CosmeticKind
import com.saintnico.verdlyhabits.data.model.coinsLabel
import kotlinx.coroutines.delay
import com.saintnico.verdlyhabits.ui.components.VerdlyLoadingIndicator

@Composable
fun GroveShopSection(
    balance: Int,
    ownedIds: Set<String>,
    equippedGlowId: String?,
    equippedSkinId: String?,
    equippedFrameId: String?,
    equippedOathChipId: String?,
    equippedPlantId: String?,
    previewTitle: String,
    buyingId: String?,
    onBuy: (CosmeticItem) -> Unit,
    onEquip: (CosmeticItem) -> Unit,
    onUnequip: (CosmeticItem) -> Unit,
    onNeedCoins: (CosmeticItem) -> Unit,
    plantedItem: CosmeticItem?,
) {
    val gold = Color(0xFFF3B23E)
    Text(
        "WEAR IT",
        fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp,
        letterSpacing = 1.6.sp,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(4.dp))
    Text(
        "Wear it. Never completes a habit for you.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
    )
    com.saintnico.verdlyhabits.engine.grove.GroveNightDrop.shopHint()?.let { night ->
        Spacer(Modifier.height(8.dp))
        Text(
            night,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = gold,
        )
    }

    plantedItem?.let { item ->
        Spacer(Modifier.height(12.dp))
        GrovePlantedCeremony(item = item, previewTitle = previewTitle)
    }

    GroveKindBlock(
        title = "Title glow",
        hint = CoinCosmetics.nextHint(CosmeticKind.TITLE_GLOW, ownedIds),
        items = CoinCosmetics.shopItems(CosmeticKind.TITLE_GLOW, com.saintnico.verdlyhabits.engine.grove.GroveNightDrop.isLive()),
        balance = balance,
        ownedIds = ownedIds,
        equippedId = equippedGlowId,
        buyingId = buyingId,
        gold = gold,
        previewTitle = previewTitle,
        onBuy = onBuy,
        onEquip = onEquip,
        onUnequip = onUnequip,
        onNeedCoins = onNeedCoins,
    )
    GroveKindBlock(
        title = "Check-off",
        hint = CoinCosmetics.nextHint(CosmeticKind.CHECK_SKIN, ownedIds),
        items = CoinCosmetics.shopItems(CosmeticKind.CHECK_SKIN, false),
        balance = balance,
        ownedIds = ownedIds,
        equippedId = equippedSkinId,
        buyingId = buyingId,
        gold = gold,
        previewTitle = previewTitle,
        onBuy = onBuy,
        onEquip = onEquip,
        onUnequip = onUnequip,
        onNeedCoins = onNeedCoins,
    )
    GroveKindBlock(
        title = "Profile frame",
        hint = CoinCosmetics.nextHint(CosmeticKind.PROFILE_FRAME, ownedIds),
        items = CoinCosmetics.shopItems(CosmeticKind.PROFILE_FRAME, false),
        balance = balance,
        ownedIds = ownedIds,
        equippedId = equippedFrameId,
        buyingId = buyingId,
        gold = gold,
        previewTitle = previewTitle,
        onBuy = onBuy,
        onEquip = onEquip,
        onUnequip = onUnequip,
        onNeedCoins = onNeedCoins,
    )
    GroveKindBlock(
        title = "Oath chip",
        hint = CoinCosmetics.nextHint(CosmeticKind.OATH_CHIP, ownedIds),
        items = CoinCosmetics.shopItems(CosmeticKind.OATH_CHIP, false),
        balance = balance,
        ownedIds = ownedIds,
        equippedId = equippedOathChipId,
        buyingId = buyingId,
        gold = gold,
        previewTitle = previewTitle,
        onBuy = onBuy,
        onEquip = onEquip,
        onUnequip = onUnequip,
        onNeedCoins = onNeedCoins,
    )
    GroveKindBlock(
        title = "Grove on your card",
        hint = CoinCosmetics.nextHint(CosmeticKind.GROVE_PLANT, ownedIds),
        items = CoinCosmetics.shopItems(CosmeticKind.GROVE_PLANT, false),
        balance = balance,
        ownedIds = ownedIds,
        equippedId = equippedPlantId,
        buyingId = buyingId,
        gold = gold,
        previewTitle = previewTitle,
        onBuy = onBuy,
        onEquip = onEquip,
        onUnequip = onUnequip,
        onNeedCoins = onNeedCoins,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        "Pin a trophy from your Hall for ${coinsLabel(CoinCosmetics.PIN_COST)} · ${CoinCosmetics.PIN_DAYS} days.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
    )
}

@Composable
private fun GroveKindBlock(
    title: String,
    hint: String?,
    items: List<CosmeticItem>,
    balance: Int,
    ownedIds: Set<String>,
    equippedId: String?,
    buyingId: String?,
    gold: Color,
    previewTitle: String,
    onBuy: (CosmeticItem) -> Unit,
    onEquip: (CosmeticItem) -> Unit,
    onUnequip: (CosmeticItem) -> Unit,
    onNeedCoins: (CosmeticItem) -> Unit,
) {
    Spacer(Modifier.height(16.dp))
    Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    if (!hint.isNullOrBlank()) {
        Spacer(Modifier.height(2.dp))
        Text(
            hint,
            style = MaterialTheme.typography.labelSmall,
            color = gold.copy(alpha = 0.9f),
        )
    }
    Spacer(Modifier.height(8.dp))
    val featured = items.filter { it.hero || it.badge == CosmeticBadge.FEATURED }
    val rest = items.filterNot { it in featured }
    featured.forEach { item ->
        GroveShopCard(
            item = item,
            owned = item.id in ownedIds,
            equipped = item.id == equippedId,
            busy = buyingId == item.id,
            balance = balance,
            featured = true,
            previewTitle = previewTitle,
            onClick = {
                when {
                    item.id == equippedId -> onUnequip(item)
                    item.id in ownedIds -> onEquip(item)
                    balance < item.price -> onNeedCoins(item)
                    else -> onBuy(item)
                }
            },
        )
        Spacer(Modifier.height(8.dp))
    }
    rest.forEach { item ->
        GroveShopCard(
            item = item,
            owned = item.id in ownedIds,
            equipped = item.id == equippedId,
            busy = buyingId == item.id,
            balance = balance,
            featured = false,
            previewTitle = previewTitle,
            onClick = {
                when {
                    item.id == equippedId -> onUnequip(item)
                    item.id in ownedIds -> onEquip(item)
                    balance < item.price -> onNeedCoins(item)
                    else -> onBuy(item)
                }
            },
        )
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun GroveShopCard(
    item: CosmeticItem,
    owned: Boolean,
    equipped: Boolean,
    busy: Boolean,
    balance: Int,
    featured: Boolean,
    previewTitle: String,
    onClick: () -> Unit,
) {
    val accent = Color(item.previewArgb)
    val badge = CoinCosmetics.badgeLabel(item.badge)
    val cta = when {
        equipped -> "On you"
        owned -> "Equip"
        else -> "Plant"
    }
    val short = balance < item.price && !owned
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(if (featured) 20.dp else 16.dp))
            .background(
                if (equipped) accent.copy(alpha = 0.18f)
                else if (featured) accent.copy(alpha = 0.10f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            )
            .border(
                if (featured || equipped) 1.5.dp else 1.dp,
                accent.copy(alpha = if (equipped) 0.7f else if (featured) 0.45f else 0.22f),
                RoundedCornerShape(if (featured) 20.dp else 16.dp),
            )
            .clickable(enabled = !busy, onClick = onClick)
            .padding(if (featured) 16.dp else 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GrovePreviewSwatch(itemId = item.id, size = if (featured) 58.dp else 44.dp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name, fontWeight = FontWeight.Bold, fontSize = if (featured) 16.sp else 14.sp)
                    if (badge != null) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            badge,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(accent.copy(alpha = 0.16f))
                                .padding(horizontal = 7.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(
                    item.desire,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.62f),
                )
                if (item.kind == CosmeticKind.TITLE_GLOW && (equipped || featured)) {
                    Spacer(Modifier.height(6.dp))
                    TitleNameplate(label = previewTitle, titleColor = accent, glowId = item.id)
                }
            }
            if (busy) {
                VerdlyLoadingIndicator(size = 20.dp)
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        if (owned) cta else coinsLabel(item.price),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        color = if (short) MaterialTheme.colorScheme.error else accent,
                    )
                    if (!owned) {
                        Text(
                            cta,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent.copy(alpha = 0.8f),
                        )
                    } else if (equipped) {
                        Text(
                            "Unequip",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                        )
                    }
                }
            }
        }
        if (short) {
            Spacer(Modifier.height(6.dp))
            Text(
                "Need ${coinsLabel(item.price - balance)} more. Open packs below.",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
fun GrovePlantedCeremony(item: CosmeticItem, previewTitle: String) {
    val accent = Color(item.previewArgb)
    var shown by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(item.id) {
        shown = true
        delay(1400)
    }
    val alpha by animateFloatAsState(
        if (shown) 1f else 0f,
        tween(280),
        label = "planted",
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(accent.copy(alpha = 0.22f * alpha))
            .border(1.5.dp, accent.copy(alpha = 0.55f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Planted · On you",
            fontWeight = FontWeight.ExtraBold,
            color = accent,
            fontSize = 13.sp,
        )
        Spacer(Modifier.height(8.dp))
        GrovePreviewSwatch(itemId = item.id, size = 64.dp)
        if (item.kind == CosmeticKind.TITLE_GLOW) {
            Spacer(Modifier.height(8.dp))
            TitleNameplate(label = previewTitle, titleColor = accent, glowId = item.id)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            item.name,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
