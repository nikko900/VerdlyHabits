@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.saintnico.verdlyhabits.ui.screens.profile

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.monetization.PaywallTrigger
import com.saintnico.verdlyhabits.referral.ReferralManager
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.viewmodel.BillingViewModel

/**
 * Profile hub for Verdly Pro: status, how Google Play billing works, trial nudges, restore, and Play Store management.
 * Money for subscriptions is collected by Google and paid out per your Play Console payout profile — not Stripe.
 */
@Composable
fun SubscriptionManageScreen(
    billingViewModel: BillingViewModel,
    onBack: () -> Unit,
    onRequestPaywall: (PaywallTrigger) -> Unit,
    onShowBanner: (String) -> Unit,
) {
    val context = LocalContext.current
    val isPro by billingViewModel.isPro.collectAsState()
    val hasFullAccess by billingViewModel.hasFullAccess.collectAsState()
    val inTrial = billingViewModel.isInFreeTrial() && !isPro
    val trialDays = billingViewModel.freeTrialDaysRemaining()
    val inBonus = ReferralManager.isInBonusPeriod(context)
    val referralCode = remember { ReferralManager.getReferralCode(context) }

    val heroTitle = when {
        isPro -> "You're on Verdly Pro"
        inTrial -> "Pro trial active"
        inBonus && !isPro -> "Pro perks unlocked"
        else -> "Grow with Verdly Pro"
    }
    val heroSubtitle = when {
        isPro -> "Unlimited habits, focus sounds, deep stats, and streak shields — all yours."
        inTrial -> "$trialDays day${if (trialDays == 1) "" else "s"} left to explore everything. Cancel anytime in Google Play before it renews."
        inBonus && !isPro -> "A referral gave you bonus Pro time on this device."
        else -> "Start a free trial or choose a plan. Billing is handled securely by Google Play."
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Subscription",
                        fontWeight = FontWeight.Bold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                Color(0xFF1B4332),
                                Color(0xFF0D1F14),
                                Color(0xFF2D6A4F),
                            ),
                        ),
                    )
                    .border(1.dp, Color(0xFF52B788).copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                    .padding(22.dp),
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = Color(0xFFFFE082),
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.size(12.dp))
                        Column {
                            Text(
                                heroTitle,
                                fontFamily = frauncesFamily,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = Color.White,
                            )
                            if (hasFullAccess) {
                                Spacer(Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.Verified,
                                        contentDescription = null,
                                        tint = Color(0xFF95D5B2),
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (isPro) "Active subscription or purchase" else "Full access (trial or bonus)",
                                        color = Color(0xFFB7E4C7),
                                        fontSize = 12.sp,
                                    )
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        heroSubtitle,
                        color = Color.White.copy(0.88f),
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                    )
                }
            }

            if (inTrial) {
                Spacer(Modifier.height(18.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp),
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(
                            "Make the most of your trial",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Spacer(Modifier.height(10.dp))
                        TrialTip("Try an ambient sound in Focus — rain, lo-fi, or forest.")
                        TrialTip("Open Stats after a few days to see your heatmap come alive.")
                        TrialTip("Add the habits that matter; Pro removes the 5-habit cap.")
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "How payments work",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Subscriptions and lifetime unlock are sold through Google Play. " +
                            "Google charges the customer and pays you according to the bank account and business profile " +
                            "linked in Play Console → Payments profile. You do not need Stripe for standard in-app purchases on Android.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                        lineHeight = 22.sp,
                    )
                }
            }

            Spacer(Modifier.height(18.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp),
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text(
                        "Your referral code",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        "Invite ${com.saintnico.verdlyhabits.referral.ReferralManager.FRIENDS_REQUIRED} friends — earn ${com.saintnico.verdlyhabits.referral.ReferralManager.REFERRER_REWARD_DAYS} days of Pro.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        referralCode,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    TextButton(
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("Verdly referral", referralCode))
                            onShowBanner("Referral code copied")
                        },
                    ) {
                        Text("Copy code")
                    }
                }
            }

            Spacer(Modifier.height(22.dp))

            if (!isPro) {
                Button(
                    onClick = { onRequestPaywall(PaywallTrigger.GoPro) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF52B788),
                        contentColor = Color(0xFF0D1510),
                    ),
                ) {
                    Text(
                        if (inTrial) "Choose a plan after trial" else "Start free trial or upgrade",
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.height(10.dp))
            }

            OutlinedButton(
                onClick = {
                    billingViewModel.restorePurchases()
                    onShowBanner("Checking Google Play for purchases…")
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Restore purchases", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(10.dp))

            OutlinedButton(
                onClick = {
                    val url = "https://play.google.com/store/account/subscriptions?package=${context.packageName}"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(8.dp))
                Text("Manage in Google Play", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
            Spacer(Modifier.height(12.dp))
            Text(
                "Products must be created in Play Console (same IDs as in the app). Test with an internal testing track and license testers before launch.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                lineHeight = 18.sp,
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TrialTip(line: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
    ) {
        Text("•  ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Text(
            line,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
        )
    }
}
