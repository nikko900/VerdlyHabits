package com.saintnico.verdlyhabits.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Premium upgrade bottom sheet. Pricing displayed in both KES and USD so the
 * Kenyan market and travellers see something familiar. Billing isn't wired yet —
 * [onSelectPlan] is invoked with the plan id and the caller decides what to do.
 *
 * Phase 1: scaffold only; PremiumGate.enforce is false so this sheet never opens
 * from a real gate. It can still be opened from Settings for design review.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaywallSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSelectPlan: (PaywallPlan) -> Unit,
    initialPlan: PaywallPlan = PaywallPlan.ANNUAL
) {
    if (!isOpen) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selected by remember { mutableStateOf(initialPlan) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 8.dp)
                .padding(bottom = 28.dp)
        ) {
            // Hero badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                                Color(0xFFFFB300).copy(alpha = 0.18f)
                            )
                        )
                    )
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Rounded.AutoAwesome, null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Rootine Pro",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Build the version of you that doesn't break.",
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            FeatureRow(Icons.Rounded.AllInclusive, "Unlimited habits", "Plant as many as you like")
            FeatureRow(Icons.Rounded.Shield, "Streak shields", "Protect a streak when life happens")
            FeatureRow(Icons.Rounded.MusicNote, "Focus ambient sounds", "Rain, lo-fi, forest, white noise")
            FeatureRow(Icons.Rounded.QueryStats, "Advanced analytics", "90-day score, exports, deep insights")
            FeatureRow(Icons.Rounded.Schedule, "Advanced scheduling", "Smart windows, habit stacking")
            FeatureRow(Icons.Rounded.Widgets, "Home screen widgets", "Quick check-in from anywhere")

            Spacer(Modifier.height(20.dp))
            Text(
                "Choose your plan",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(10.dp))

            PaywallPlan.entries.forEach { plan ->
                PlanRow(
                    plan = plan,
                    selected = plan == selected,
                    onSelect = { selected = plan }
                )
                Spacer(Modifier.height(8.dp))
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = { onSelectPlan(selected) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Rounded.Bolt, null, tint = Color(0xFFFFE082))
                Spacer(Modifier.width(8.dp))
                Text("Start with ${selected.displayName}", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Cancel anytime. Billing is dormant in this build.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun FeatureRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f))
        }
        Icon(Icons.Rounded.Check, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun PlanRow(plan: PaywallPlan, selected: Boolean, onSelect: () -> Unit) {
    val border = if (selected) MaterialTheme.colorScheme.primary
                 else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.10f)
    val bg = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
             else MaterialTheme.colorScheme.surface
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect),
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = androidx.compose.foundation.BorderStroke(if (selected) 2.dp else 1.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(plan.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
                    if (plan.badge != null) {
                        Spacer(Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFFFB300).copy(alpha = 0.20f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(plan.badge, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB8860B))
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(plan.subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(plan.priceKes, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
                Text(plan.priceUsd, fontSize = 10.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f))
            }
        }
    }
}

enum class PaywallPlan(
    val displayName: String,
    val subtitle: String,
    val priceKes: String,
    val priceUsd: String,
    val badge: String?
) {
    MONTHLY ("Monthly",  "Try Pro for a month",         "KES 599 / mo",   "~$4.99",   null),
    ANNUAL  ("Annual",   "Save 58% — billed yearly",   "KES 2,999 / yr", "~$24.99",  "BEST VALUE"),
    LIFETIME("Lifetime", "One-time payment, forever",  "KES 7,999",      "~$64.99",  null)
}
