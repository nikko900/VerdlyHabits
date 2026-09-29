@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.saintnico.verdlyhabits.ui.components.coins

import android.app.Activity
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.revenuecat.purchases.Package
import com.saintnico.verdlyhabits.billing.BillingManager
import com.saintnico.verdlyhabits.data.model.CoinEconomy
import com.saintnico.verdlyhabits.data.model.CoinOathCopy
import com.saintnico.verdlyhabits.data.model.CosmeticItem
import com.saintnico.verdlyhabits.data.model.coinsLabel
import com.saintnico.verdlyhabits.ui.viewmodel.BillingViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.CoinPackNeed
import com.saintnico.verdlyhabits.ui.viewmodel.CoinPackReason
import com.saintnico.verdlyhabits.ui.components.VerdlyLoadingIndicator

private fun Package.formattedPrice(): String = product.price.formatted

private fun Package.baseProductId(): String = product.id.substringBefore(":")

private fun List<Package>.findByProductId(productId: String): Package? =
    firstOrNull { it.baseProductId() == productId }

@Composable
fun CoinPackSheet(
    isOpen: Boolean,
    billingViewModel: BillingViewModel,
    isPremium: Boolean,
    onDismiss: () -> Unit,
    onPurchaseSuccess: () -> Unit,
    reason: CoinPackReason = CoinPackReason.BROWSE,
    need: CoinPackNeed? = null,
    pendingCosmeticId: String? = null,
    balance: Int = 0,
    ownedCosmeticIds: Set<String> = emptySet(),
    equippedGlowId: String? = null,
    equippedSkinId: String? = null,
    equippedFrameId: String? = null,
    equippedOathChipId: String? = null,
    equippedPlantId: String? = null,
    previewTitle: String = "Rooted",
    onBuyCosmetic: (String, (Boolean, String) -> Unit) -> Unit = { _, _ -> },
    onEquipCosmetic: (String) -> Unit = {},
    onUnequipCosmetic: (String) -> Unit = {},
    onOpenPremium: () -> Unit = {},
    onCosmeticNotice: (String) -> Unit = {},
) {
    if (!isOpen) return
    val context = LocalContext.current
    val activity = context as? Activity
    val coinOffering by billingViewModel.coinOffering.collectAsState()
    val offeringError by billingViewModel.coinOfferingError.collectAsState()
    val statusMessage by billingViewModel.coinOfferingStatusMessage.collectAsState()
    val isLoading by billingViewModel.coinOfferingLoading.collectAsState()
    val packages = coinOffering?.availablePackages.orEmpty()
    val matchedPacks = remember(packages) {
        CoinEconomy.PACK_OFFERS.mapNotNull { offer ->
            packages.findByProductId(offer.productId)?.let { offer to it }
        }
    }
    var purchasingId by remember { mutableStateOf<String?>(null) }
    var buyingCosmeticId by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var plantedItem by remember { mutableStateOf<CosmeticItem?>(null) }
    var localNeed by remember(need) { mutableStateOf(need) }
    val effectiveNeed = localNeed ?: need
    val gold = Color(0xFFF3B23E)

    LaunchedEffect(plantedItem?.id) {
        if (plantedItem == null) return@LaunchedEffect
        kotlinx.coroutines.delay(2200)
        plantedItem = null
    }

    LaunchedEffect(reason) {
        billingViewModel.loadCoinOffering()
        runCatching {
            com.google.firebase.analytics.FirebaseAnalytics
                .getInstance(context)
                .logEvent(
                    "coin_pack_open",
                    android.os.Bundle().apply {
                        putString("reason", reason.name.lowercase())
                    },
                )
        }
    }

    val subtitle = when {
        reason == CoinPackReason.NEED_ITEM && effectiveNeed != null ->
            if (effectiveNeed.shortfall <= 0) "You have enough for ${effectiveNeed.itemName}."
            else "You need ${coinsLabel(effectiveNeed.shortfall)} more for ${effectiveNeed.itemName}."
        reason == CoinPackReason.AFTER_LOCK -> CoinOathCopy.EMPTY_AFTER_LOCK
        reason == CoinPackReason.AFTER_CONVERT -> CoinOathCopy.EMPTY_AFTER_CONVERT
        reason == CoinPackReason.AFTER_FORFEIT -> "Swear again. Back yourself on the next one."
        reason == CoinPackReason.CANT_AFFORD -> "You need ${CoinEconomy.minBalanceToStake()} coins to swear an oath."
        reason == CoinPackReason.GROVE -> "Wear what you planted. Glows, frames, check-offs. Never a completed habit."
        else -> "Lock an oath. Finish and it becomes trophy progress."
    }
    val suggested = when {
        effectiveNeed != null && effectiveNeed.shortfall > 0 -> CoinEconomy.suggestPackFor(effectiveNeed.shortfall)
        reason == CoinPackReason.CANT_AFFORD -> CoinEconomy.suggestPackFor(CoinEconomy.minBalanceToStake() - balance)
        else -> null
    }
    val cosmeticsFirst = reason == CoinPackReason.GROVE ||
        (reason == CoinPackReason.NEED_ITEM && pendingCosmeticId != null && balance >= (effectiveNeed?.itemPrice ?: Int.MAX_VALUE))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinIcon(size = 32.dp, premium = isPremium)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        if (cosmeticsFirst) "The Grove" else CoinOathCopy.PACK_HEADER,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        coinsLabel(balance),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = if (isPremium) gold else MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close")
                }
            }

            if (!isPremium) {
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(gold.copy(alpha = 0.12f))
                        .border(1.dp, gold.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                        .clickable(onClick = onOpenPremium)
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Premium plants 2× XP",
                            fontWeight = FontWeight.Bold,
                            color = gold,
                        )
                        Text(
                            "Every finished oath. Plus 50 coins each month.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
                        )
                    }
                    Text(
                        "Unlock",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = gold,
                    )
                }
            }

            val groveBlock = @Composable {
                Spacer(Modifier.height(22.dp))
                GroveShopSection(
                    balance = balance,
                    ownedIds = ownedCosmeticIds,
                    equippedGlowId = equippedGlowId,
                    equippedSkinId = equippedSkinId,
                    equippedFrameId = equippedFrameId,
                    equippedOathChipId = equippedOathChipId,
                    equippedPlantId = equippedPlantId,
                    previewTitle = previewTitle,
                    buyingId = buyingCosmeticId,
                    plantedItem = plantedItem,
                    onBuy = { item ->
                        buyingCosmeticId = item.id
                        onBuyCosmetic(item.id) { ok, msg ->
                            buyingCosmeticId = null
                            if (ok) {
                                plantedItem = item
                                onCosmeticNotice(msg)
                            } else {
                                errorMessage = msg
                            }
                        }
                    },
                    onEquip = { item ->
                        onEquipCosmetic(item.id)
                        onCosmeticNotice("${item.name} is on you.")
                    },
                    onUnequip = { item ->
                        onUnequipCosmetic(item.id)
                        onCosmeticNotice("${item.name} taken off.")
                    },
                    onNeedCoins = { item ->
                        val shortfall = (item.price - balance).coerceAtLeast(1)
                        localNeed = CoinPackNeed(
                            itemName = item.name,
                            itemPrice = item.price,
                            shortfall = shortfall,
                            itemId = item.id,
                        )
                        errorMessage = null
                    },
                )
            }

            val packsBlock = @Composable {
                Spacer(Modifier.height(18.dp))
                when {
                    isLoading -> {
                        Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                            VerdlyLoadingIndicator(size = 32.dp)
                        }
                    }
                    offeringError || matchedPacks.isEmpty() -> {
                        CoinStoreEmptyState(statusMessage = statusMessage)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { billingViewModel.loadCoinOffering() }) {
                            Text("Try again")
                        }
                    }
                    else -> {
                        matchedPacks.forEach { (offer, pkg) ->
                            val isPurchasing = purchasingId == offer.productId
                            val covers = suggested?.productId == offer.productId
                            val featured = covers || offer.productId == "verdly_coins_500"
                            CoinPackRow(
                                offer = offer,
                                price = pkg.formattedPrice(),
                                isPremium = isPremium,
                                featured = featured,
                                coversItem = covers,
                                coverLabel = effectiveNeed?.itemName,
                                isPurchasing = isPurchasing,
                                enabled = !isPurchasing && activity != null,
                                onClick = {
                                    errorMessage = null
                                    purchasingId = offer.productId
                                    billingViewModel.purchase(
                                        activity = activity!!,
                                        pkg = pkg,
                                        onSuccess = {
                                            purchasingId = null
                                            onPurchaseSuccess()
                                            if (effectiveNeed?.itemId == null) {
                                                onDismiss()
                                            } else {
                                                localNeed = effectiveNeed.copy(shortfall = 0)
                                            }
                                        },
                                        onError = { msg, cancelled ->
                                            purchasingId = null
                                            if (!cancelled) errorMessage = msg
                                        },
                                    )
                                },
                            )
                        }
                    }
                }
            }

            if (cosmeticsFirst) {
                groveBlock()
                packsBlock()
            } else {
                packsBlock()
                groveBlock()
            }

            errorMessage?.let { msg ->
                Spacer(Modifier.height(8.dp))
                Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Coins are for in-app support and commitment only. Never cash or prizes.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
            )
        }
    }
}

@Composable
private fun CoinPackRow(
    offer: CoinEconomy.PackOffer,
    price: String,
    isPremium: Boolean,
    featured: Boolean,
    coversItem: Boolean = false,
    coverLabel: String? = null,
    isPurchasing: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val gold = Color(0xFFF3B23E)
    val highlighted = offer.badge != null
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(if (featured) 22.dp else 16.dp))
            .background(
                when {
                    featured -> gold.copy(alpha = 0.14f)
                    highlighted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                },
            )
            .border(
                if (featured) 1.5.dp else 1.dp,
                when {
                    featured -> gold.copy(alpha = 0.55f)
                    highlighted -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                },
                RoundedCornerShape(if (featured) 22.dp else 16.dp),
            )
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = if (featured) 18.dp else 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CoinIcon(size = if (featured) 28.dp else 22.dp, premium = isPremium || featured)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        offer.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = if (featured) 18.sp else 16.sp,
                    )
                    offer.badge?.let { badge ->
                        Spacer(Modifier.width(8.dp))
                        Text(
                            badge.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            color = if (featured) gold else MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (coversItem) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "COVERS IT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.6.sp,
                            color = gold,
                        )
                    }
                }
                if (coversItem && !coverLabel.isNullOrBlank()) {
                    Text(
                        "Smallest pack that covers $coverLabel",
                        style = MaterialTheme.typography.labelSmall,
                        color = gold.copy(alpha = 0.9f),
                    )
                }
                Text(
                    offer.framing,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                )
            }
            if (isPurchasing) {
                VerdlyLoadingIndicator(size = 22.dp)
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${offer.coins}",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = if (featured) gold else MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        price,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    )
                }
            }
        }
    }
}

@Composable
private fun CoinStoreEmptyState(statusMessage: String?) {
    Text(
        "Couldn't load coin packs",
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        statusMessage ?: "Couldn't load plans right now. Check your connection and try again.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
    )
}

@Composable
fun CoinBalanceChip(
    balance: Int,
    isPremium: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(
                if (isPremium) Color(0xFFF3B23E).copy(alpha = 0.14f)
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            )
            .border(
                1.dp,
                if (isPremium) Color(0xFFD4890A).copy(alpha = 0.35f)
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                RoundedCornerShape(50),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        CoinIcon(size = 16.dp, premium = isPremium)
        Text(
            "$balance coins",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}
