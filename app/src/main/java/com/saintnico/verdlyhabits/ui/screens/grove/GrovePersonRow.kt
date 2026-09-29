package com.saintnico.verdlyhabits.ui.screens.grove

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.saintnico.verdlyhabits.data.model.MembershipTier
import com.saintnico.verdlyhabits.engine.LastActive
import com.saintnico.verdlyhabits.ui.components.coins.GroveAvatarHalo
import com.saintnico.verdlyhabits.ui.components.social.LastActiveAvatarWrap
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.viewmodel.GroveMember

private val Mint = Color(0xFF52B788)
private val Gold = Color(0xFFFFC857)

@Composable
fun GrovePersonRow(
    member: GroveMember,
    metric: String,
    planted: Boolean,
    onClick: () -> Unit,
    onNudge: (() -> Unit)? = null,
    onWitness: (() -> Unit)? = null,
    witnessed: Boolean = false,
    showWitness: Boolean = false,
    viewerSharesLastActive: Boolean = true,
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LastActiveAvatarWrap(
            snapshot = LastActive.resolve(
                viewerShares = viewerSharesLastActive,
                targetShares = member.shareLastActive,
                lastActiveAt = member.lastActiveAt,
                isSelf = member.isSelf,
            ),
            compact = true,
            dotSize = 8.dp,
        ) {
            GroveAvatarHalo(
                glowId = member.titleGlowId,
                frameId = member.profileFrameId,
                size = 44.dp,
            ) {
                if (!member.photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = member.photoUrl,
                        contentDescription = member.username,
                        modifier = Modifier.size(44.dp).clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        Modifier
                            .size(44.dp)
                            .background(Mint.copy(0.18f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            member.username.take(1).uppercase(),
                            fontWeight = FontWeight.Bold,
                            color = Mint,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (member.isSelf) "You" else "@${member.username}",
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                if (member.membershipTier.showsPremiumBadge) {
                    Spacer(Modifier.width(6.dp))
                    Text("PRO", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Gold)
                }
                if (member.groveLead) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Rounded.Spa, contentDescription = "Grove Lead", tint = Mint, modifier = Modifier.size(14.dp))
                }
            }
            Text(
                if (planted) "planted today" else "still growing",
                fontFamily = dmSansFamily,
                fontSize = 12.sp,
                color = if (planted) Mint else onBg.copy(0.5f),
                maxLines = 1,
            )
            val detail = when {
                !member.sharedHabitTitle.isNullOrBlank() -> member.sharedHabitTitle
                member.plantedCountToday > 1 -> "${member.plantedCountToday} planted today"
                member.currentStreakPublic > 0 -> "${member.currentStreakPublic} day streak"
                member.weeklyPlantDays > 0 -> "${member.weeklyPlantDays} of 7 this week"
                else -> member.equippedTitleLabel
            }
            if (!detail.isNullOrBlank()) {
                Text(detail, fontSize = 11.sp, color = onBg.copy(0.45f), maxLines = 1)
            }
            member.pinnedTrophyLabel?.let { pin ->
                Text(pin, fontSize = 11.sp, color = Gold.copy(0.9f), fontWeight = FontWeight.Medium, maxLines = 1)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(metric, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = onBg)
            if (showWitness && member.witnessCount > 0) {
                Text(
                    "${member.witnessCount} saw you",
                    fontSize = 10.sp,
                    color = onBg.copy(0.45f),
                )
            }
            Row {
                if (onWitness != null && !member.isSelf && planted) {
                    TextButton(onClick = onWitness, enabled = !witnessed) {
                        Icon(Icons.Rounded.Visibility, null, modifier = Modifier.size(14.dp), tint = if (witnessed) Mint else onBg.copy(0.55f))
                        Spacer(Modifier.width(4.dp))
                        Text(if (witnessed) "Witnessed" else "Saw you", fontSize = 11.sp)
                    }
                }
                if (onNudge != null && !member.isSelf && !planted) {
                    TextButton(onClick = onNudge) {
                        Icon(Icons.Rounded.Bolt, null, modifier = Modifier.size(14.dp), tint = Mint)
                        Spacer(Modifier.width(4.dp))
                        Text("Nudge", fontSize = 11.sp)
                    }
                }
            }
        }
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
fun GroveAvatarStack(
    members: List<GroveMember>,
    modifier: Modifier = Modifier,
    viewerSharesLastActive: Boolean = true,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
        members.take(5).forEach { m ->
            LastActiveAvatarWrap(
                snapshot = LastActive.resolve(
                    viewerShares = viewerSharesLastActive,
                    targetShares = m.shareLastActive,
                    lastActiveAt = m.lastActiveAt,
                    isSelf = m.isSelf,
                ),
                compact = false,
                dotSize = 7.dp,
            ) {
                GroveAvatarHalo(glowId = m.titleGlowId, frameId = m.profileFrameId, size = 28.dp) {
                    if (!m.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = m.photoUrl,
                            contentDescription = m.username,
                            modifier = Modifier.size(28.dp).clip(CircleShape).border(1.dp, MaterialTheme.colorScheme.background, CircleShape),
                            contentScale = ContentScale.Crop,
                        )
                    } else {
                        Box(
                            Modifier.size(28.dp).background(Mint.copy(0.2f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(m.username.take(1).uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Mint)
                        }
                    }
                }
            }
        }
    }
}
