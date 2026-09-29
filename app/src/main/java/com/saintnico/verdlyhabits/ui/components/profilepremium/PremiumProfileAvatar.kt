package com.saintnico.verdlyhabits.ui.components.profilepremium

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import com.saintnico.verdlyhabits.data.model.MembershipTier
import com.saintnico.verdlyhabits.ui.components.ProfileAvatar
import com.saintnico.verdlyhabits.ui.theme.GoldColor

/**
 * Profile avatar with optional golden premium/trial bezel and mini badge.
 */
@Composable
fun PremiumProfileAvatar(
    photoUri: String?,
    size: Dp,
    membershipTier: MembershipTier = MembershipTier.FREE,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    userId: String? = null,
    fallbackTint: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
    fallbackBackground: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
    showMiniBadge: Boolean = true,
) {
    val showPremium = membershipTier.showsPremiumBadge
    val ringPadding = if (showPremium) 1.5.dp else 0.dp
    val innerSize = size - ringPadding * 2

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        if (showPremium) {
            PremiumAvatarBezel(
                outerSize = size,
                innerPadding = ringPadding,
            ) {
                ProfileAvatar(
                    photoUri = photoUri,
                    size = innerSize,
                    contentDescription = contentDescription,
                    userId = userId,
                    fallbackTint = fallbackTint,
                    fallbackBackground = fallbackBackground,
                )
            }
        } else {
            ProfileAvatar(
                photoUri = photoUri,
                size = size,
                contentDescription = contentDescription,
                userId = userId,
                fallbackTint = fallbackTint,
                fallbackBackground = fallbackBackground,
            )
        }

        if (showPremium && showMiniBadge) {
            PremiumAvatarMiniBadge(
                tier = membershipTier,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 1.dp, y = 1.dp),
            )
        }
    }
}

@Composable
fun PremiumAvatarMiniBadge(
    tier: MembershipTier,
    modifier: Modifier = Modifier,
) {
    if (!tier.showsPremiumBadge) return
    val label = tier.compactBadgeLabel
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFFB8860B),
                        GoldColor,
                        Color(0xFFFFB300),
                    ),
                ),
            )
            .border(1.dp, Color(0xFFFFF3C4).copy(alpha = 0.75f), RoundedCornerShape(999.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (label.length <= 7) {
            Text(
                label,
                color = Color(0xFF1A1208),
                fontSize = 7.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.2.sp,
                maxLines = 1,
            )
        } else {
            Icon(
                Icons.Filled.WorkspacePremium,
                contentDescription = label,
                tint = Color(0xFF1A1208),
                modifier = Modifier.size(8.dp),
            )
        }
    }
}
