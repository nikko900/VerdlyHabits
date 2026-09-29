@file:OptIn(com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI::class)

package com.saintnico.verdlyhabits.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.saintnico.verdlyhabits.billing.BillingManager
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.viewmodel.BillingViewModel
import kotlinx.coroutines.delay
import com.saintnico.verdlyhabits.ui.components.VerdlyLoadingIndicator

private val MintGlow = Color(0xFF52B788)
private val ForestDeep = Color(0xFF0D1510)
private val ForestMid = Color(0xFF1B4332)
private val GoldAccent = Color(0xFFD4890A)

/**
 * Full-screen RevenueCat paywall (linked to the `default` offering).
 * Rendered as an overlay — not a partial dialog — so multipage layouts use the whole display.
 */
@Composable
fun RevenueCatProPaywallDialog(
    isOpen: Boolean,
    billingViewModel: BillingViewModel,
    onDismiss: () -> Unit,
    onPurchaseSuccess: () -> Unit = {},
) {
    if (!isOpen) return

    val proOffering by billingViewModel.proOffering.collectAsState()
    val proLoading by billingViewModel.proOfferingLoading.collectAsState()
    val proError by billingViewModel.proOfferingError.collectAsState()
    val hasPaidSubscription by billingViewModel.hasPaidSubscription.collectAsState()
    var showSuccess by remember { mutableStateOf(false) }
    val onDismissState = rememberUpdatedState(onDismiss)
    val onPurchaseSuccessState = rememberUpdatedState(onPurchaseSuccess)

    LaunchedEffect(isOpen) {
        if (isOpen) billingViewModel.loadProOffering()
    }

    LaunchedEffect(hasPaidSubscription) {
        if (hasPaidSubscription && !showSuccess) onDismissState.value()
    }

    val finishSuccess: () -> Unit = {
        showSuccess = false
        onPurchaseSuccessState.value()
        onDismissState.value()
    }

    val listener = remember {
        object : PaywallListener {
            override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
                showSuccess = true
            }

            override fun onRestoreCompleted(customerInfo: CustomerInfo) {
                val active = customerInfo.entitlements[BillingManager.ENTITLEMENT_PRO]?.isActive == true
                if (active) showSuccess = true
            }
        }
    }

    val paywallOptions = remember(proOffering?.identifier, onDismiss) {
        PaywallOptions.Builder(onDismiss)
            .setOffering(proOffering)
            .setListener(listener)
            .build()
    }

    BackHandler {
        if (showSuccess) finishSuccess() else onDismiss()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
            .background(ForestDeep),
    ) {
        if (showSuccess) {
            PremiumPurchaseSuccessOverlay(onContinue = finishSuccess)
        } else if (proOffering == null || proOffering?.availablePackages.isNullOrEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (proLoading) {
                    VerdlyLoadingIndicator(size = 48.dp)
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = "Loading plans",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Text(
                        text = "Couldn't load plans",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = proError
                            ?: "Couldn't load plans right now. Check your connection and try again in a moment.",
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = Color.White.copy(alpha = 0.78f),
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { billingViewModel.loadProOffering() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MintGlow,
                            contentColor = ForestDeep,
                        ),
                    ) {
                        Text("Try again", fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onDismiss) {
                    Text("Not now", color = GoldAccent, fontWeight = FontWeight.SemiBold)
                }
            }
        } else {
            key(proOffering?.identifier ?: "offering") {
                Box(modifier = Modifier.fillMaxSize()) {
                    Paywall(options = paywallOptions)
                }
            }
        }
    }
}

@Composable
private fun PremiumPurchaseSuccessOverlay(onContinue: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.82f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow,
        ),
        label = "premium_success_scale",
    )

    LaunchedEffect(Unit) {
        delay(80)
        startAnimation = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(ForestDeep, ForestMid, ForestDeep),
                ),
            ),
    ) {
        CelebrationKonfetti(
            trigger = startAnimation,
            modifier = Modifier.fillMaxSize(),
            intensity = BurstIntensity.HUGE,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Column(
                modifier = Modifier.scale(scale),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "Premium unlocked",
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Unlimited habits, streak shields, focus sounds, and your monthly coin drip are ready.",
                    fontSize = 16.sp,
                    lineHeight = 24.sp,
                    color = Color.White.copy(alpha = 0.82f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Spacer(modifier = Modifier.height(28.dp))
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MintGlow,
                        contentColor = ForestDeep,
                    ),
                ) {
                    Text(
                        text = "Start building",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Manage anytime in Settings → Subscription",
                    fontSize = 13.sp,
                    color = GoldAccent.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
