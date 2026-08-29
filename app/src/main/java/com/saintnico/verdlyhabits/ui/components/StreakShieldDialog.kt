package com.saintnico.verdlyhabits.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.domain.streak.RepairOffer

/**
 * Offer-on-open repair. Shown when a streak is [com.saintnico.verdlyhabits.domain.streak.StreakStatus.REPAIRABLE]
 * — the run is held, not destroyed, until the user spends a shield or the window closes.
 */
@Composable
fun StreakShieldDialog(
    offer: RepairOffer?,
    habitTitle: String,
    onUseShield: () -> Unit,
    onGetShield: () -> Unit,
    onAskMeLater: () -> Unit,
) {
    if (offer == null) return
    AlertDialog(
        onDismissRequest = onAskMeLater,
        icon = {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFB300).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Shield,
                    contentDescription = null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(28.dp),
                )
            }
        },
        title = {
            Text(
                if (offer.canRepairNow) "Save your streak?" else "Your streak needs a shield",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        },
        text = {
            Column {
                Text(
                    "$habitTitle has a ${offer.streakAtStake}-day streak on the line. " +
                        "A shield covers ${offer.missedDate} without adding to the count.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        if (offer.canRepairNow) {
                            "${offer.shieldsAvailable} shield${if (offer.shieldsAvailable == 1) "" else "s"} available"
                        } else {
                            "No shields left — get one to keep this run"
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFB8860B),
                    )
                }
            }
        },
        confirmButton = {
            if (offer.canRepairNow) {
                TextButton(onClick = onUseShield) {
                    Text("Use shield", fontWeight = FontWeight.Bold)
                }
            } else {
                TextButton(onClick = onGetShield) {
                    Text("Get a shield", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onAskMeLater) {
                Text(
                    "Ask me later",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                )
            }
        },
    )
}
