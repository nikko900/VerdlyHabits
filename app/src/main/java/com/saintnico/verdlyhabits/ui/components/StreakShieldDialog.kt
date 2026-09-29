package com.saintnico.verdlyhabits.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.domain.streak.RepairOffer
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily

private val ShieldGold = Color(0xFFFFB300)
private val ShieldInk = Color(0xFFB8860B)

/**
 * Offer-on-open repair. Shown when a streak is [com.saintnico.verdlyhabits.domain.streak.StreakStatus.REPAIRABLE]
 * — the run is held, not destroyed, until the user spends a shield or the window closes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakShieldDialog(
    offer: RepairOffer?,
    habitTitle: String,
    onUseShield: () -> Unit,
    onGetShield: () -> Unit,
    onAskMeLater: () -> Unit,
) {
    if (offer == null) return
    ModalBottomSheet(
        onDismissRequest = onAskMeLater,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(top = 12.dp, bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                StreakFlame(
                    streak = offer.streakAtStake.coerceAtLeast(1),
                    size = 52.dp,
                )
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        if (offer.canRepairNow) "Save this streak" else "This streak needs a shield",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        habitTitle,
                        fontFamily = dmSansFamily,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "${offer.streakAtStake}-day run. A shield covers ${offer.missedDate} without adding to the count.",
                fontFamily = dmSansFamily,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
            )
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = ShieldGold,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    if (offer.canRepairNow) {
                        "${offer.shieldsAvailable} shield${if (offer.shieldsAvailable == 1) "" else "s"} left"
                    } else {
                        "None left. Get one to keep this run"
                    },
                    fontFamily = dmSansFamily,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ShieldInk,
                )
            }
            Spacer(Modifier.height(22.dp))
            if (offer.canRepairNow) {
                Button(
                    onClick = onUseShield,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ShieldGold, contentColor = Color(0xFF1A1208)),
                ) {
                    Text("Use shield", fontFamily = dmSansFamily, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = onGetShield,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ShieldGold, contentColor = Color(0xFF1A1208)),
                ) {
                    Text("Get a shield", fontFamily = dmSansFamily, fontWeight = FontWeight.Bold)
                }
            }
            TextButton(
                onClick = onAskMeLater,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
            ) {
                Text(
                    "Ask me later",
                    fontFamily = dmSansFamily,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                )
            }
        }
    }
}
