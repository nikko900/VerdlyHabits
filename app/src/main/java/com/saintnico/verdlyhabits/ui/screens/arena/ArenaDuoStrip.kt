package com.saintnico.verdlyhabits.ui.screens.arena

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.remote.firestore.DuoStreakState
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily

private val Mint = Color(0xFF52B788)
private val RiskAmber = Color(0xFFE08A2E)

@Composable
fun ArenaDuoStrip(
    duoState: DuoStreakState?,
    onNudge: () -> Unit,
    onInvite: () -> Unit,
    nudgeOnCooldown: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val atRisk = duoState?.streakAtRisk == true
    val accent = if (atRisk) RiskAmber else Mint
    val bg = if (atRisk) {
        Brush.horizontalGradient(listOf(RiskAmber.copy(0.22f), RiskAmber.copy(0.08f)))
    } else {
        Brush.horizontalGradient(listOf(Mint.copy(0.18f), Mint.copy(0.06f)))
    }

    val infinite = rememberInfiniteTransition(label = "arena_duo_pulse")
    val pulse by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Reverse),
        label = "arena_duo_pulse_scale",
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, accent.copy(alpha = if (atRisk) 0.45f else 0.28f), RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        when {
            duoState?.status == "active" || duoState?.status == "pending" -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        Icons.Rounded.LocalFireDepartment,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier
                            .size(22.dp)
                            .scale(if (atRisk) pulse else 1f),
                    )
                    Text(
                        stripCopy(duoState, atRisk),
                        modifier = Modifier.weight(1f),
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                        lineHeight = 18.sp,
                    )
                    if (!nudgeOnCooldown) {
                        if (atRisk) {
                            Button(
                                onClick = onNudge,
                                colors = ButtonDefaults.buttonColors(containerColor = RiskAmber),
                                contentPadding = ButtonDefaults.ContentPadding,
                            ) {
                                Text("Nudge", fontFamily = dmSansFamily, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(onClick = onNudge) {
                                Text("Nudge", fontFamily = dmSansFamily, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            duoState?.isIncomingInvite == true -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "Duo invite waiting",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                    Text(
                        "Open Duo tab",
                        fontFamily = dmSansFamily,
                        fontSize = 12.sp,
                        color = accent,
                    )
                }
            }
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        "No duo yet. Invite a friend",
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onBackground.copy(0.75f),
                    )
                    OutlinedButton(onClick = onInvite) {
                        Text("Invite", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private fun stripCopy(state: DuoStreakState, atRisk: Boolean): String {
    val buddy = state.buddyUsername.ifBlank { "your buddy" }
    return if (atRisk) {
        "Duo streak at risk. Check in before midnight with @$buddy"
    } else {
        "${state.streakDays} day streak with @$buddy"
    }
}
