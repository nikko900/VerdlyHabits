package com.saintnico.verdlyhabits.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * "Your streak is about to break — spend a shield?" confirmation dialog.
 * Shown when the user has missed a day on a >= 3-day streak and still has at
 * least one shield available.
 */
@Composable
fun StreakShieldDialog(
    isOpen: Boolean,
    habitTitle: String,
    streak: Int,
    shieldsAvailable: Int,
    onUseShield: () -> Unit,
    onBreakStreak: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!isOpen) return
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFB300).copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Shield, null,
                    tint = Color(0xFFFFB300),
                    modifier = Modifier.size(28.dp)
                )
            }
        },
        title = {
            Text(
                "Spend a streak shield?",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        },
        text = {
            Column {
                Text(
                    "$habitTitle is on a $streak-day streak. Missing today would break it.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f)
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Shield, null, tint = Color(0xFFFFB300), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "$shieldsAvailable shield${if (shieldsAvailable == 1) "" else "s"} available",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFB8860B)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onUseShield) {
                Text("Use shield", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onBreakStreak) {
                Text("Let it break", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f))
            }
        }
    )
}
