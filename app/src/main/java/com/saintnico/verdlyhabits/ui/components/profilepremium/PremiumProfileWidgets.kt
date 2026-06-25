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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.saintnico.verdlyhabits.ui.theme.ProfileAccent
import com.saintnico.verdlyhabits.ui.theme.ProfileAccents
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily

@Composable
fun PremiumProfileHero(
    displayName: String,
    username: String,
    tagline: String,
    photoUrl: String?,
    level: Int,
    xp: Int,
    levelTitle: String? = null,
    accent: ProfileAccent = ProfileAccents.Sage,
    equippedTitle: String? = null,
    equippedTitleColor: Color = Color(0xFFFFC857),
    modifier: Modifier = Modifier,
) {
    val ringBrush = Brush.linearGradient(
        listOf(accent.highlight, accent.base, accent.base.copy(alpha = 0.85f)),
    )
    val gradient = Brush.verticalGradient(
        listOf(accent.heroTop, accent.heroMid, accent.heroBottom),
    )
    Box(
        modifier
            .fillMaxWidth()
            .background(gradient),
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.glow.copy(alpha = 0.38f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.12f),
                    radius = size.minDimension * 0.55f,
                ),
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(accent.highlight.copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.12f, size.height * 0.88f),
                    radius = size.minDimension * 0.45f,
                ),
            )
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(78.dp)
                        .border(2.dp, ringBrush, CircleShape)
                        .clip(CircleShape)
                        .background(accent.heroBottom.copy(alpha = 0.9f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (!photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape),
                        )
                    } else {
                        Text(
                            (displayName.ifBlank { username }).firstOrNull()?.uppercase() ?: "?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = accent.highlight,
                        )
                    }
                }
                Spacer(Modifier.size(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        displayName.ifBlank { username.ifBlank { "Rival" } },
                        fontFamily = frauncesFamily,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        color = Color.White,
                        maxLines = 2,
                    )
                    Text(
                        "@${username.ifBlank { "rival" }}",
                        color = accent.highlight.copy(alpha = 0.95f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    if (!equippedTitle.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(999.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            equippedTitleColor.copy(alpha = 0.32f),
                                            equippedTitleColor.copy(alpha = 0.12f),
                                        ),
                                    ),
                                )
                                .border(
                                    1.dp,
                                    equippedTitleColor.copy(alpha = 0.6f),
                                    RoundedCornerShape(999.dp),
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.WorkspacePremium,
                                contentDescription = null,
                                tint = equippedTitleColor,
                                modifier = Modifier.size(13.dp),
                            )
                            Spacer(Modifier.size(5.dp))
                            Text(
                                equippedTitle,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.4.sp,
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                tagline.ifBlank { "Rhythm over noise." },
                color = Color.White.copy(alpha = 0.72f),
                fontSize = 14.sp,
                lineHeight = 20.sp,
                modifier = Modifier.fillMaxWidth(0.92f),
            )
            Spacer(Modifier.height(18.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    "LV $level",
                    color = Color.White.copy(0.85f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "$xp XP",
                    color = Color.White.copy(0.55f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                )
            }
            if (!levelTitle.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    levelTitle,
                    color = accent.ink.copy(alpha = 0.55f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.6.sp,
                )
            }
        }
    }
}

@Composable
fun ProfileCardThemePicker(
    selectedKey: String,
    onSelect: (ProfileAccent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(
            "Card theme",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            "15 premium looks for your public profile and live preview.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(12.dp))
        ProfileAccents.all.chunked(3).forEach { rowThemes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowThemes.forEach { theme ->
                    ProfileThemeSwatch(
                        accent = theme,
                        selected = theme.key == selectedKey,
                        onClick = { onSelect(theme) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - rowThemes.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun ProfileThemeSwatch(
    accent: ProfileAccent,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .height(64.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    listOf(accent.heroMid, accent.heroBottom),
                ),
                shape,
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                brush = if (selected) {
                    Brush.linearGradient(listOf(accent.highlight, accent.glow))
                } else {
                    Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = 0.12f),
                            Color.White.copy(alpha = 0.06f),
                        ),
                    )
                },
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
    ) {
        Canvas(Modifier.matchParentSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    listOf(accent.glow.copy(alpha = 0.45f), Color.Transparent),
                ),
                radius = size.minDimension * 0.55f,
                center = Offset(size.width * 0.75f, size.height * 0.25f),
            )
        }
        Column(Modifier.align(Alignment.BottomStart)) {
            Text(
                accent.displayName,
                color = accent.ink,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(accent.highlight),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = accent.heroBottom,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

@Composable
fun ProfileStreakArc(
    progress: Float,
    modifier: Modifier = Modifier,
    accent: Color = Color(0xFF95D5B2),
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "arc",
    )
    Canvas(
        modifier
            .size(56.dp)
            .padding(4.dp),
    ) {
        val stroke = 5.dp.toPx()
        drawArc(
            color = Color.White.copy(0.08f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawArc(
            brush = Brush.sweepGradient(listOf(accent, accent.copy(alpha = 0.5f))),
            startAngle = -90f,
            sweepAngle = 360f * animated,
            useCenter = false,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

@Composable
fun LivePreviewPhoneFrame(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val outerShape = RoundedCornerShape(32.dp)
    val midShape = RoundedCornerShape(26.dp)
    val innerShape = RoundedCornerShape(22.dp)
    Box(
        modifier
            .fillMaxWidth()
            .clip(outerShape)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
            .padding(10.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(midShape)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(3.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(innerShape)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(12.dp),
            ) {
                Box(Modifier.clip(RoundedCornerShape(18.dp))) {
                    content()
                }
            }
        }
    }
}
