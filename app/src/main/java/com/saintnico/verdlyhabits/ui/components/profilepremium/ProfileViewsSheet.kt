package com.saintnico.verdlyhabits.ui.components.profilepremium

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.remote.firestore.ProfileViewer
import com.saintnico.verdlyhabits.ui.theme.GoldColor
import com.saintnico.verdlyhabits.ui.theme.ProfileAccents
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import java.time.Instant
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileViewsSheet(
    viewers: List<ProfileViewer>,
    friendUids: Set<String>,
    onDismiss: () -> Unit,
    onOpenProfile: (String) -> Unit,
    onMarkedSeen: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(Unit) { onMarkedSeen() }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 18.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Profile views",
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                )
                TextButton(onClick = onDismiss) {
                    Text("Close", fontFamily = dmSansFamily)
                }
            }
            Text(
                "People who opened your profile card. Newest first.",
                fontFamily = dmSansFamily,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
            )
            Spacer(Modifier.height(16.dp))

            if (viewers.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f))
                        .padding(horizontal = 18.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "No names yet",
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                    )
                    Text(
                        "When someone opens your profile card, they land here with a name, time, and their title. Friends and rivals both show.",
                        fontFamily = dmSansFamily,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    )
                    Text(
                        "Share your card or stay active in an arena. Views follow attention.",
                        fontFamily = dmSansFamily,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.48f),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 520.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(viewers, key = { it.uid }) { viewer ->
                        ProfileViewerCard(
                            viewer = viewer,
                            isFriend = viewer.uid in friendUids,
                            onClick = {
                                onOpenProfile(viewer.uid)
                                onDismiss()
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileViewerCard(
    viewer: ProfileViewer,
    isFriend: Boolean,
    onClick: () -> Unit,
) {
    val accent = ProfileAccents.byKey(viewer.profileAccent)
    val premium = viewer.membershipTier.showsPremiumBadge
    val name = viewer.playerName
    val handle = viewer.playerHandle
    val shape = RoundedCornerShape(20.dp)
    val borderColor = if (premium) GoldColor.copy(alpha = 0.72f) else accent.highlight.copy(alpha = 0.38f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(accent.heroTop, accent.heroMid, accent.heroBottom),
                ),
            )
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PremiumProfileAvatar(
                photoUri = viewer.photoUrl,
                size = 56.dp,
                membershipTier = viewer.membershipTier,
                userId = viewer.uid,
                fallbackTint = accent.highlight,
                fallbackBackground = accent.heroBottom.copy(alpha = 0.9f),
            )
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    name,
                    fontFamily = frauncesFamily,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (handle != null) {
                    Text(
                        "@$handle",
                        fontFamily = dmSansFamily,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = accent.highlight.copy(alpha = 0.95f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                val title = viewer.equippedTitle?.takeIf { it.isNotBlank() }
                if (title != null) {
                    Text(
                        title,
                        fontFamily = dmSansFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (premium) GoldColor else accent.highlight.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (isFriend) {
                        ThemeChip(label = "Friend", fill = accent.glow.copy(alpha = 0.28f), ink = accent.ink)
                    }
                    ThemeChip(
                        label = formatViewedAt(viewer.viewedAtMillis),
                        fill = Color.White.copy(alpha = 0.10f),
                        ink = Color.White.copy(alpha = 0.82f),
                    )
                    if (viewer.viewCount > 1) {
                        ThemeChip(
                            label = "${viewer.viewCount} visits",
                            fill = Color.White.copy(alpha = 0.10f),
                            ink = Color.White.copy(alpha = 0.82f),
                        )
                    }
                    if (viewer.level > 0) {
                        ThemeChip(
                            label = "Lv ${viewer.level}",
                            fill = Color.White.copy(alpha = 0.10f),
                            ink = Color.White.copy(alpha = 0.82f),
                        )
                    }
                    if (premium) {
                        PremiumAccountBadge(label = viewer.membershipTier.compactBadgeLabel, compact = true)
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeChip(label: String, fill: Color, ink: Color) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(fill)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(
            label,
            fontFamily = dmSansFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = ink,
            maxLines = 1,
        )
    }
}

private fun formatViewedAt(millis: Long): String {
    if (millis <= 0L) return "Just now"
    val instant = Instant.ofEpochMilli(millis)
    val now = Instant.now()
    val minutes = ChronoUnit.MINUTES.between(instant, now)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        ChronoUnit.HOURS.between(instant, now) < 24 -> "${ChronoUnit.HOURS.between(instant, now)}h ago"
        ChronoUnit.DAYS.between(instant, now) < 7 -> "${ChronoUnit.DAYS.between(instant, now)}d ago"
        else -> "${ChronoUnit.DAYS.between(instant, now)}d ago"
    }
}
