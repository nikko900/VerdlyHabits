package com.saintnico.verdlyhabits.ui.components.profilepremium

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WorkspacePremium
import com.saintnico.verdlyhabits.util.DebugSessionLog
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.saintnico.verdlyhabits.engine.ProfileSocialEngine
import com.saintnico.verdlyhabits.engine.ProfileTitleEngine
import com.saintnico.verdlyhabits.ui.components.notifications.NotificationBellButton
import com.saintnico.verdlyhabits.ui.theme.GoldColor

/**
 * Profile hero — wave header with level ring, title, flairs, and member story.
 * Kept free of Lottie / sweep gradients to avoid device crashes in scroll lists.
 */
@Composable
fun ProfileIdentityCard(
    primary: Color,
    background: Color,
    userName: String,
    userUsername: String,
    userPhotoUri: String?,
    bio: String,
    level: Int,
    levelTitle: String,
    levelProgress: Float,
    equippedTitle: ProfileTitleEngine.ProfileTitle,
    flairs: List<ProfileSocialEngine.ProfileFlair>,
    memberStoryLine: String,
    avatarScale: Float,
    avatarBorder: Color,
    onEditClick: () -> Unit,
    onOpenTitlePicker: () -> Unit,
    notificationBadgeCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
) {
    val titleColor = Color(ProfileTitleEngine.tierColorArgb(equippedTitle.tier))
    val safeProgress = levelProgress.coerceIn(0f, 1f).let { if (it.isNaN()) 0f else it }

    // #region agent log
    DebugSessionLog.log(
        location = "ProfileIdentityCard.kt:compose",
        message = "ProfileIdentityCard composing",
        hypothesisId = "H1",
        data = mapOf(
            "flairCount" to flairs.size,
            "levelProgress" to safeProgress,
            "level" to level,
        ),
    )
    // #endregion

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to primary.copy(alpha = 0.92f),
                        1f to background,
                    ),
                )
                val wave = Path().apply {
                    val w = size.width
                    val h = size.height
                    moveTo(0f, h - 48f)
                    quadraticTo(w * 0.5f, h - 4f, w, h - 48f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(wave, background)
            }
            .padding(bottom = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            NotificationBellButton(
                unreadCount = notificationBadgeCount,
                onClick = onOpenNotifications,
                tint = Color.White.copy(alpha = 0.92f),
                accent = Color.White,
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 36.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                LevelProgressRing(
                    progress = safeProgress,
                    accent = Color.White.copy(alpha = 0.85f),
                    track = Color.White.copy(alpha = 0.18f),
                    modifier = Modifier.size(108.dp),
                )
                Box(
                    modifier = Modifier
                        .scale(avatarScale)
                        .size(90.dp)
                        .clip(CircleShape)
                        .border(3.dp, avatarBorder, CircleShape)
                        .background(Color.White.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    com.saintnico.verdlyhabits.ui.components.ProfileAvatar(
                        photoUri = userPhotoUri,
                        size = 90.dp,
                        fallbackTint = Color.White.copy(alpha = 0.85f),
                        fallbackBackground = Color.White.copy(alpha = 0.12f),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = (-4).dp, y = 4.dp)
                        .clip(CircleShape)
                        .background(primary)
                        .clickable(onClick = onEditClick),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(14.dp))
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                userName.ifBlank { "Verdly" },
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                Text(
                    if (userUsername.isNotBlank() && userUsername != "UnknownRival") "@$userUsername" else "@rival",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.72f),
                )
            }

            if (flairs.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                ) {
                    items(flairs.take(3), key = { "${it.type}:${it.label}" }) { flair ->
                        ProfileFlairChip(flair = flair, primary = primary)
                    }
                }
                // #region agent log
                DebugSessionLog.log(
                    location = "ProfileIdentityCard.kt:flairs",
                    message = "Flair row composed without FlowRow",
                    hypothesisId = "H1",
                    data = mapOf("flairCount" to flairs.take(3).size),
                )
                // #endregion
            }

            Spacer(Modifier.height(6.dp))
            Text(
                "Level $level · $levelTitle",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp,
                color = Color.White.copy(alpha = 0.88f),
            )

            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(titleColor.copy(alpha = 0.22f))
                    .border(1.dp, titleColor.copy(alpha = 0.5f), RoundedCornerShape(999.dp))
                    .clickable(onClick = onOpenTitlePicker)
                    .padding(horizontal = 12.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.WorkspacePremium, null, tint = titleColor, modifier = Modifier.size(14.dp))
                Spacer(Modifier.size(6.dp))
                Text(
                    equippedTitle.label,
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                bio,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.62f),
                fontStyle = FontStyle.Italic,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(8.dp))
            Text(
                memberStoryLine,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                color = Color.White.copy(alpha = 0.48f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 28.dp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LevelProgressRing(
    progress: Float,
    accent: Color,
    track: Color,
    modifier: Modifier = Modifier,
) {
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "levelRing",
    )
    Canvas(modifier) {
        val stroke = 4.dp.toPx()
        drawArc(
            color = track,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        if (animated > 0f) {
            drawArc(
                color = accent,
                startAngle = -90f,
                sweepAngle = 360f * animated,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun ProfileFlairChip(flair: ProfileSocialEngine.ProfileFlair, primary: Color) {
    val (icon, tint) = when (flair.type) {
        ProfileSocialEngine.FlairType.PRO -> Icons.Filled.WorkspacePremium to GoldColor
        ProfileSocialEngine.FlairType.DUO_FIRE -> Icons.Default.LocalFireDepartment to Color(0xFFFF7043)
        ProfileSocialEngine.FlairType.CHALLENGE_LEADER -> Icons.Rounded.EmojiEvents to GoldColor
        ProfileSocialEngine.FlairType.PERFECT_WEEK -> Icons.Rounded.EmojiEvents to primary
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Black.copy(alpha = 0.2f))
            .border(1.dp, tint.copy(alpha = 0.45f), RoundedCornerShape(999.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(11.dp))
        Spacer(Modifier.size(3.dp))
        Text(flair.label, color = Color.White.copy(alpha = 0.92f), fontSize = 9.sp, fontWeight = FontWeight.Bold)
    }
}

/** Must be hosted outside [androidx.compose.foundation.lazy.LazyColumn] items. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTitlePickerSheet(
    unlockedTitles: List<ProfileTitleEngine.ProfileTitle>,
    equippedId: String,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSelect: (String, String) -> Unit,
    onAuto: () -> Unit,
) {
    val onBg = MaterialTheme.colorScheme.onBackground
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(bottom = 32.dp)) {
            Text(
                "Equip a title",
                modifier = Modifier.padding(horizontal = 24.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                "Pick a badge to show on your profile.",
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp),
                style = MaterialTheme.typography.bodySmall,
                color = onBg.copy(alpha = 0.6f),
            )
            TextButton(onClick = onAuto, modifier = Modifier.padding(horizontal = 16.dp)) {
                Text("Use auto (rarest earned)")
            }
            HorizontalDivider(color = onBg.copy(alpha = 0.08f))
            LazyColumn(modifier = Modifier.height(320.dp)) {
                items(unlockedTitles, key = { it.id }) { title ->
                    val color = Color(ProfileTitleEngine.tierColorArgb(title.tier))
                    val selected = title.id == equippedId
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(title.id, title.label) }
                            .background(if (selected) color.copy(alpha = 0.1f) else Color.Transparent)
                            .padding(horizontal = 24.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.WorkspacePremium, null, tint = color, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.size(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(title.label, fontWeight = FontWeight.SemiBold, color = onBg)
                            Text(title.hint, style = MaterialTheme.typography.bodySmall, color = onBg.copy(alpha = 0.55f))
                        }
                        if (selected) {
                            Text("Equipped", color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
