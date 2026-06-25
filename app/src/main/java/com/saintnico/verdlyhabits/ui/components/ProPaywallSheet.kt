@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.saintnico.verdlyhabits.ui.components

import android.app.Activity
import android.net.Uri
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.ProductDetails
import com.saintnico.verdlyhabits.R
import com.saintnico.verdlyhabits.billing.BillingManager
import com.saintnico.verdlyhabits.monetization.PaywallTrigger
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.viewmodel.BillingViewModel

private fun ProductDetails.subscriptionFormattedPrice(): String? =
    subscriptionOfferDetails?.firstOrNull()?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice

private val MintGlow = Color(0xFF52B788)
private val AnnualBg = Color(0xFF1B4332)
private const val PRIVACY_URL = "https://verdlyhabits.page.link/"

private enum class PlanTier { WEEKLY, MONTHLY, ANNUAL, LIFETIME }

@Composable
fun ProPaywallSheet(
    isOpen: Boolean,
    trigger: PaywallTrigger?,
    billingViewModel: BillingViewModel,
    onDismiss: () -> Unit,
    onRestorePurchases: () -> Unit,
) {
    if (!isOpen) return
    val context = LocalContext.current
    val activity = context as? Activity
    val products by billingViewModel.products.collectAsState()
    val isInTrial = billingViewModel.isInFreeTrial()
    val trialDaysLeft = billingViewModel.freeTrialDaysRemaining()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedTier by remember { mutableIntStateOf(PlanTier.ANNUAL.ordinal) }

    val weeklyPd = remember(products) { products.find { it.productId == BillingManager.PRODUCT_WEEKLY } }
    val monthlyPd = remember(products) { products.find { it.productId == BillingManager.PRODUCT_MONTHLY } }
    val annualPd = remember(products) { products.find { it.productId == BillingManager.PRODUCT_ANNUAL } }
    val lifetimePd = remember(products) { products.find { it.productId == BillingManager.PRODUCT_LIFETIME } }

    val headline = trigger?.headline ?: PaywallTrigger.GoPro.headline
    val subtext = trigger?.subtext ?: PaywallTrigger.GoPro.subtext
    val isReferralAnnual = trigger is PaywallTrigger.ReferralAnnual

    LaunchedEffect(isReferralAnnual) {
        if (isReferralAnnual) selectedTier = PlanTier.ANNUAL.ordinal
    }

    val trialBannerColor = when (trialDaysLeft) {
        1 -> Color(0xFFE53935)
        2 -> Color(0xFFFFB300)
        else -> Color(0xFF2D6A4F)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF0D1510),
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White.copy(0.7f))
                }
            }

            if (isInTrial) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(trialBannerColor.copy(alpha = 0.22f))
                        .border(1.dp, trialBannerColor.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                        .padding(vertical = 10.dp, horizontal = 14.dp),
                ) {
                    Text(
                        text = "⏱ $trialDaysLeft day${if (trialDaysLeft == 1) "" else "s"} left in your free trial",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(14.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    painter = painterResource(R.drawable.app_icon),
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp)),
                    tint = Color.Unspecified,
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Go Pro",
                        fontFamily = frauncesFamily,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = Color.White,
                    )
                    Text(
                        "Verdly",
                        style = MaterialTheme.typography.labelMedium,
                        color = MintGlow,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                headline,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                subtext,
                color = Color.White.copy(0.65f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
            if (!isReferralAnnual) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Priced for Kenya · Pay with M-Pesa via Google Play",
                    color = MintGlow.copy(0.85f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }

            Spacer(Modifier.height(22.dp))

            PlanCard(
                selected = selectedTier == PlanTier.WEEKLY.ordinal,
                onClick = { selectedTier = PlanTier.WEEKLY.ordinal },
                highlight = false,
                badge = null,
                title = "Weekly",
                subtitle = "Less than a snack per week",
                priceLine = weeklyPd?.subscriptionFormattedPrice() ?: "Ksh 99/wk",
                strikethrough = null,
                footnote = null,
            )
            Spacer(Modifier.height(10.dp))
            PlanCard(
                selected = selectedTier == PlanTier.MONTHLY.ordinal,
                onClick = { selectedTier = PlanTier.MONTHLY.ordinal },
                highlight = false,
                badge = null,
                title = "Monthly",
                subtitle = "Billed every month",
                priceLine = monthlyPd?.subscriptionFormattedPrice() ?: "Ksh 399/mo",
                strikethrough = "Ksh 428",
                footnote = null,
            )
            Spacer(Modifier.height(10.dp))
            PlanCard(
                selected = selectedTier == PlanTier.ANNUAL.ordinal,
                onClick = { selectedTier = PlanTier.ANNUAL.ordinal },
                highlight = true,
                badge = if (isReferralAnnual) "20% OFF — Referral reward" else "BEST VALUE — Save 37%",
                title = "Annual",
                subtitle = if (isReferralAnnual) "Your exclusive referral price" else "Billed once a year",
                priceLine = annualPd?.subscriptionFormattedPrice()
                    ?: if (isReferralAnnual) com.saintnico.verdlyhabits.referral.ReferralManager.discountedAnnualPriceKes() + "/yr"
                    else "Ksh 2,999/yr",
                strikethrough = if (isReferralAnnual) "Ksh 2,999" else "Ksh 4,788",
                footnote = if (isReferralAnnual) "Offer valid this week after your referral reward" else null,
            )
            Spacer(Modifier.height(10.dp))
            PlanCard(
                selected = selectedTier == PlanTier.LIFETIME.ordinal,
                onClick = { selectedTier = PlanTier.LIFETIME.ordinal },
                highlight = false,
                badge = null,
                title = "Lifetime",
                subtitle = "Pay once, own forever",
                priceLine = lifetimePd?.oneTimePurchaseOfferDetails?.formattedPrice ?: "Ksh 4,999",
                strikethrough = null,
                footnote = "Most popular with serious users",
            )

            Spacer(Modifier.height(22.dp))

            val ctaLabel = when {
                isReferralAnnual -> "Lock in annual — 20% off"
                isInTrial -> "Start 7-Day Free Trial"
                else -> "Upgrade Now"
            }
            Button(
                onClick = {
                    if (activity == null) return@Button
                    when (selectedTier) {
                        PlanTier.WEEKLY.ordinal -> weeklyPd?.let {
                            billingViewModel.launchPurchaseFlow(activity, it, isSubscription = true)
                        }
                        PlanTier.MONTHLY.ordinal -> monthlyPd?.let {
                            billingViewModel.launchPurchaseFlow(activity, it, isSubscription = true)
                        }
                        PlanTier.ANNUAL.ordinal -> annualPd?.let {
                            billingViewModel.launchPurchaseFlow(activity, it, isSubscription = true)
                        }
                        else -> lifetimePd?.let {
                            billingViewModel.launchPurchaseFlow(activity, it, isSubscription = false)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MintGlow, contentColor = Color(0xFF0D1510)),
            ) {
                Text(ctaLabel, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }

            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    "Cancel anytime",
                    color = Color.White.copy(0.45f),
                    fontSize = 11.sp,
                )
                Text(" · ", color = Color.White.copy(0.35f), fontSize = 11.sp)
                Text(
                    "Restore purchases",
                    color = MintGlow,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable {
                        onRestorePurchases()
                    },
                )
                Text(" · ", color = Color.White.copy(0.35f), fontSize = 11.sp)
                Text(
                    "Privacy Policy",
                    color = MintGlow,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, Uri.parse(PRIVACY_URL))
                        context.startActivity(intent)
                    },
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun PlanCard(
    selected: Boolean,
    onClick: () -> Unit,
    highlight: Boolean,
    badge: String?,
    title: String,
    subtitle: String,
    priceLine: String,
    strikethrough: String?,
    footnote: String?,
) {
    val borderColor = if (selected) MintGlow else Color.White.copy(0.12f)
    val bg = if (highlight) {
        Brush.verticalGradient(listOf(AnnualBg, AnnualBg.copy(alpha = 0.92f)))
    } else {
        Brush.verticalGradient(listOf(Color(0xFF121A16), Color(0xFF121A16)))
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        if (badge != null) {
            Text(
                badge,
                color = Color(0xFFFFB300),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(6.dp))
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
                Text(subtitle, color = Color.White.copy(0.55f), fontSize = 12.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    priceLine,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
                if (strikethrough != null) {
                    Text(
                        strikethrough,
                        color = Color.White.copy(0.45f),
                        fontSize = 12.sp,
                        textDecoration = TextDecoration.LineThrough,
                    )
                }
            }
        }
        if (footnote != null) {
            Spacer(Modifier.height(8.dp))
            Text(footnote, color = Color.White.copy(0.5f), fontSize = 11.sp)
        }
    }
}
