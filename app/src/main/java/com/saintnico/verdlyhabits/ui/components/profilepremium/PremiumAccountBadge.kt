package com.saintnico.verdlyhabits.ui.components.profilepremium

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.ui.theme.GoldColor

private val PremiumGoldLight = Color(0xFFFFF3C4)
private val PremiumGoldMid = GoldColor
private val PremiumGoldDeep = Color(0xFFB8860B)
private val PremiumGoldAmber = Color(0xFFFFB300)

val PremiumMemberGradient: Brush
    get() = Brush.linearGradient(
        listOf(PremiumGoldLight, PremiumGoldMid, PremiumGoldAmber, PremiumGoldDeep),
    )

val PremiumMemberRingBrush: Brush
    get() = Brush.sweepGradient(
        listOf(
            PremiumGoldLight,
            PremiumGoldMid,
            PremiumGoldAmber,
            PremiumGoldDeep,
            PremiumGoldLight,
        ),
    )

/**
 * Golden ring bezel for premium subscribers — wraps profile avatars.
 */
@Composable
fun PremiumAvatarBezel(
    modifier: Modifier = Modifier,
    outerSize: Dp,
    innerPadding: Dp = 1.5.dp,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .size(outerSize)
            .clip(CircleShape)
            .background(PremiumMemberRingBrush)
            .padding(innerPadding)
            .clip(CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * Compact "Premium" pill shown on profile identity and account rows.
 */
@Composable
fun PremiumAccountBadge(
    modifier: Modifier = Modifier,
    label: String = "PREMIUM",
    compact: Boolean = false,
) {
    val hPad = if (compact) 6.dp else 10.dp
    val vPad = if (compact) 2.dp else 4.dp
    val iconSize = if (compact) 9.dp else 13.dp
    val textSize = if (compact) 8.sp else 10.sp
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        PremiumGoldDeep.copy(alpha = 0.92f),
                        PremiumGoldMid.copy(alpha = 0.88f),
                        PremiumGoldAmber.copy(alpha = 0.85f),
                    ),
                ),
            )
            .border(1.dp, PremiumGoldLight.copy(alpha = 0.65f), RoundedCornerShape(999.dp))
            .padding(horizontal = hPad, vertical = vPad),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.WorkspacePremium,
            contentDescription = null,
            tint = Color(0xFF1A1208),
            modifier = Modifier.size(iconSize),
        )
        Spacer(Modifier.size(if (compact) 3.dp else 5.dp))
        Text(
            label,
            color = Color(0xFF1A1208),
            fontSize = textSize,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = if (compact) 0.6.sp else 1.2.sp,
        )
    }
}
