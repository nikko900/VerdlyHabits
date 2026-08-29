package com.saintnico.verdlyhabits.ui.components.social

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Mail
import androidx.compose.material.icons.rounded.SportsScore
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.saintnico.verdlyhabits.data.model.MembershipTier
import com.saintnico.verdlyhabits.ui.components.profilepremium.PremiumProfileAvatar
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import com.saintnico.verdlyhabits.ui.viewmodel.FriendSummary

private val Mint = Color(0xFF52B788)
private val Teal = Color(0xFF2DD4BF)
private val Gold = Color(0xFFFFB300)

val ConnectionsPremiumGradient = Brush.linearGradient(
    listOf(
        Color(0xFF0F1A14),
        Color(0xFF152A20),
        Color(0xFF0A120E),
    ),
)

@Composable
fun ConnectionsPageHeader(
    friendCount: Int,
    pendingRequests: Int,
    arenaInvites: Int,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Connections",
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    "Friends, requests & arena invites",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(accent.copy(alpha = 0.14f))
                    .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Groups, null, tint = accent, modifier = Modifier.size(22.dp))
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ConnectionsStatTile(
                label = "Friends",
                value = friendCount.toString(),
                icon = Icons.Rounded.Groups,
                tint = Mint,
                modifier = Modifier.weight(1f),
            )
            ConnectionsStatTile(
                label = "Requests",
                value = pendingRequests.toString(),
                icon = Icons.Rounded.Mail,
                tint = Teal,
                highlight = pendingRequests > 0,
                modifier = Modifier.weight(1f),
            )
            ConnectionsStatTile(
                label = "Invites",
                value = arenaInvites.toString(),
                icon = Icons.Rounded.SportsScore,
                tint = Gold,
                highlight = arenaInvites > 0,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ConnectionsStatTile(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = if (highlight) 3.dp else 1.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (highlight) tint.copy(0.35f) else MaterialTheme.colorScheme.onBackground.copy(0.06f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.5f),
            )
        }
    }
}

@Composable
fun ConnectionsPremiumShell(
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        shadowElevation = 6.dp,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Brush.linearGradient(listOf(accent.copy(0.35f), Mint.copy(0.12f))),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ConnectionsPremiumGradient)
                .padding(18.dp),
        ) {
            content()
        }
    }
}

@Composable
fun ConnectionsSectionHeader(
    title: String,
    icon: ImageVector,
    tint: Color,
    badge: String? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 15.sp,
            color = Color.White.copy(alpha = 0.92f),
            modifier = Modifier.weight(1f),
        )
        badge?.let {
            Text(
                it,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = tint,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(tint.copy(alpha = 0.14f))
                    .padding(horizontal = 8.dp, vertical = 3.dp),
            )
        }
    }
}

@Composable
fun ConnectionsFriendCard(
    friend: FriendSummary,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDuoInvite: Boolean = false,
    onInviteDuo: (() -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.width(if (showDuoInvite) 118.dp else 108.dp),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.06f),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.10f)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ConnectionsAvatar(
                photoUrl = friend.photoUrl,
                label = friend.username,
                accent = accent,
                size = 52.dp,
                membershipTier = friend.membershipTier,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "@${friend.username}",
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.88f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (showDuoInvite && onInviteDuo != null) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(Gold.copy(alpha = 0.14f))
                        .clickable(onClick = onInviteDuo)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Rounded.LocalFireDepartment, null, tint = Gold, modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Duo", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Gold)
                }
            } else {
                Text(
                    "View profile",
                    fontSize = 9.sp,
                    color = accent.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
fun ConnectionsAvatar(
    photoUrl: String?,
    label: String,
    accent: Color,
    size: androidx.compose.ui.unit.Dp,
    membershipTier: MembershipTier = MembershipTier.FREE,
) {
    if (membershipTier.showsPremiumBadge) {
        PremiumProfileAvatar(
            photoUri = photoUrl,
            size = size,
            membershipTier = membershipTier,
            fallbackTint = accent.copy(alpha = 0.9f),
            fallbackBackground = accent.copy(alpha = 0.12f),
        )
        return
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(
                Brush.linearGradient(
                    listOf(accent.copy(0.22f), accent.copy(0.06f)),
                ),
            )
            .border(1.5.dp, accent.copy(alpha = 0.35f), CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (!photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        } else {
            Text(
                label.firstOrNull()?.uppercase() ?: "?",
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.Bold,
                color = accent,
                fontSize = (size.value * 0.34f).sp,
            )
        }
    }
}
