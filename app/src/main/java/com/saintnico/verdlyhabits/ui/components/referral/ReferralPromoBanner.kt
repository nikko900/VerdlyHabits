@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.saintnico.verdlyhabits.ui.components.referral

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily

@Composable
fun ReferralPromoBanner(
    qualifiedCount: Int,
    friendsRequired: Int,
    rewardDays: Int,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shimmer = rememberInfiniteTransition(label = "referral_shimmer")
    val drift by shimmer.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "referral_drift",
    )
    val pulse by shimmer.animateFloat(
        initialValue = 0.85f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "referral_glow",
    )

    val gradient = Brush.linearGradient(
        colors = listOf(
            Color(0xFF1B4332),
            Color(0xFF2D6A4F),
            Color(0xFF40916C),
            Color(0xFF52B788).copy(alpha = pulse),
        ),
        start = androidx.compose.ui.geometry.Offset(0f, drift * 400f),
        end = androidx.compose.ui.geometry.Offset(800f, 200f + drift * 300f),
    )

    Card(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(32.dp),
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = "Dismiss",
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(18.dp),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 28.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.Redeem,
                        contentDescription = null,
                        tint = Color(0xFFFFE8A3),
                        modifier = Modifier.size(26.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Invite $friendsRequired friends",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = Color.White,
                        lineHeight = 22.sp,
                    )
                    Text(
                        "Unlock $rewardDays days of Pro — free",
                        fontFamily = dmSansFamily,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.88f),
                    )
                    Text(
                        "Tap for your code & QR",
                        fontFamily = dmSansFamily,
                        fontSize = 11.sp,
                        color = Color(0xFFFFE8A3),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                    Spacer(Modifier.height(8.dp))
                    ReferralProgressDots(
                        qualifiedCount = qualifiedCount,
                        friendsRequired = friendsRequired,
                    )
                }
            }
        }
    }
}

@Composable
fun ReferralProgressDots(
    qualifiedCount: Int,
    friendsRequired: Int,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xFFFFE8A3),
    inactiveColor: Color = Color.White.copy(alpha = 0.28f),
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(friendsRequired) { index ->
            val filled = index < qualifiedCount
            Box(
                modifier = Modifier
                    .size(if (filled) 10.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (filled) activeColor else inactiveColor),
            )
        }
        Spacer(Modifier.width(4.dp))
        Text(
            "$qualifiedCount / $friendsRequired joined",
            fontFamily = dmSansFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.82f),
        )
    }
}
