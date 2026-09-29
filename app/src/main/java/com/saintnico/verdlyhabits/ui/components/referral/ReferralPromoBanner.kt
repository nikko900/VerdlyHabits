@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.saintnico.verdlyhabits.ui.components.referral

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.R
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily

/** Slim one-row invite — sits above Today's Focus, not a hero card. */
@Composable
fun ReferralPromoBanner(
    qualifiedCount: Int,
    friendsRequired: Int,
    rewardDays: Int,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gradient = Brush.linearGradient(
        listOf(Color(0xFF1B4332), Color(0xFF2D6A4F), Color(0xFF40916C)),
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(gradient)
            .clickable(onClick = onOpen)
            .padding(start = 10.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Redeem,
                contentDescription = null,
                tint = Color(0xFFFFE8A3),
                modifier = Modifier.size(15.dp),
            )
        }
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.invite_friends_short, friendsRequired),
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                stringResource(R.string.invite_premium_days, rewardDays),
                fontFamily = dmSansFamily,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Text(
            "$qualifiedCount/$friendsRequired",
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = Color(0xFFFFE8A3),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.dismiss),
                tint = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/** Compact header chip — same invite, much smaller, dismissible. */
@Composable
fun ReferralInviteChip(
    qualifiedCount: Int,
    friendsRequired: Int,
    onOpen: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1B4332), Color(0xFF40916C), Color(0xFF52B788)),
                ),
            )
            .clickable(onClick = onOpen)
            .padding(start = 10.dp, end = 2.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.Redeem,
            contentDescription = null,
            tint = Color(0xFFFFE8A3),
            modifier = Modifier.size(14.dp),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            stringResource(R.string.invite_chip, qualifiedCount, friendsRequired),
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 11.sp,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(26.dp),
        ) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = stringResource(R.string.dismiss),
                tint = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.size(14.dp),
            )
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
                    .size(if (filled) 8.dp else 6.dp)
                    .clip(CircleShape)
                    .background(if (filled) activeColor else inactiveColor),
            )
        }
    }
}
