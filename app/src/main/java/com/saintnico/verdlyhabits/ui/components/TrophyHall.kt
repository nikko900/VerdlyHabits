package com.saintnico.verdlyhabits.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.engine.Achievement
import com.saintnico.verdlyhabits.ui.theme.GoldColor

/**
 * Gradient-framed “hall of trophies” surface used on Profile and Achievements.
 */
@Composable
fun TrophyHallFramedCard(
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .background(
                brush = Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                        GoldColor.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    )
                ),
                shape = RoundedCornerShape(28.dp)
            )
            .padding(2.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    ),
                content = content
            )
        }
    }
}

@Composable
fun TrophyHallHeaderRow(
    primary: Color,
    onBg: Color,
    unlocked: Int,
    total: Int,
    modifier: Modifier = Modifier,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 12.dp, top = 18.dp, bottom = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.EmojiEvents,
                    contentDescription = null,
                    tint = GoldColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "BADGES",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 2.2.sp,
                    color = primary
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Hall of trophies",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = onBg
            )
            Text(
                "$unlocked of $total unlocked · tap a trophy for details",
                style = MaterialTheme.typography.bodySmall,
                color = onBg.copy(alpha = 0.55f)
            )
        }
        trailing()
    }
}

@Composable
fun TrophyHallProgressBar(
    unlocked: Int,
    total: Int,
    onBg: Color,
    modifier: Modifier = Modifier
) {
    val progress = if (total == 0) 0f else unlocked.toFloat() / total.toFloat()
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .height(5.dp)
            .clip(RoundedCornerShape(999.dp)),
        color = GoldColor,
        trackColor = onBg.copy(alpha = 0.08f),
    )
}

@Composable
fun AchievementMedallion(
    achievement: Achievement,
    onClick: () -> Unit,
    interactionEnabled: Boolean = true
) {
    val earned = achievement.isUnlocked
    val tier = visualTierForAchievement(achievement.id, achievement.xpReward)
    val interaction = remember { MutableInteractionSource() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(76.dp)
            .clickable(
                enabled = interactionEnabled,
                interactionSource = interaction,
                indication = ripple(bounded = true),
                onClick = onClick
            )
            .padding(vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier.size(72.dp),
            contentAlignment = Alignment.Center
        ) {
            if (earned) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                                    Color.Transparent
                                ),
                                radius = 80f
                            ),
                            shape = CircleShape
                        )
                )
            }
            AchievementTrophy(
                icon = achievement.icon,
                tier = tier,
                unlocked = earned,
                size = if (earned) 64.dp else 56.dp
            )
            if (!earned) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            achievement.title,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (earned) FontWeight.SemiBold else FontWeight.Medium,
            color = if (earned) MaterialTheme.colorScheme.onBackground
            else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.38f),
            lineHeight = 14.sp,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
    }
}

@Composable
fun TrophyHallListRow(
    achievement: Achievement,
    visualTier: TrophyTier,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val earned = achievement.isUnlocked
    val tier = visualTier
    val primary = MaterialTheme.colorScheme.primary
    val onBg = MaterialTheme.colorScheme.onBackground

    val shape = RoundedCornerShape(20.dp)
    val borderModifier = if (earned) {
        Modifier.border(
            width = 1.dp,
            brush = Brush.linearGradient(
                listOf(
                    GoldColor.copy(alpha = 0.65f),
                    primary.copy(alpha = 0.45f),
                    GoldColor.copy(alpha = 0.45f)
                )
            ),
            shape = shape
        )
    } else {
        Modifier.border(1.dp, onBg.copy(alpha = 0.1f), shape)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(borderModifier)
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (earned) 4.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (earned) {
                        Brush.horizontalGradient(
                            listOf(
                                primary.copy(alpha = 0.06f),
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surface,
                                MaterialTheme.colorScheme.surface
                            )
                        )
                    }
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                if (earned) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        primary.copy(alpha = 0.25f),
                                        Color.Transparent
                                    ),
                                    radius = 64f
                                ),
                                shape = CircleShape
                            )
                    )
                }
                AchievementTrophy(
                    icon = achievement.icon,
                    tier = tier,
                    unlocked = earned,
                    size = 52.dp
                )
                if (!earned) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .offset(x = 2.dp, y = 2.dp)
                            .size(20.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = onBg.copy(alpha = 0.4f)
                        )
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TrophyRankChip(tier = tier, unlocked = earned)
                    Text(
                        achievement.title,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (earned) onBg else onBg.copy(alpha = 0.55f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = onBg.copy(alpha = if (earned) 0.58f else 0.38f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
                Row(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = GoldColor.copy(alpha = if (earned) 1f else 0.35f),
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "+${achievement.xpReward} XP",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = GoldColor.copy(alpha = if (earned) 0.95f else 0.45f)
                    )
                }
                if (!earned && achievement.progressTarget > 1) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "${achievement.progressCurrent} / ${achievement.progressTarget}",
                        fontSize = 11.sp,
                        color = primary.copy(alpha = 0.75f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(4.dp))
                    val p = (achievement.progressCurrent.toFloat() / achievement.progressTarget).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { p },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = primary,
                        trackColor = onBg.copy(alpha = 0.08f)
                    )
                }
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = onBg.copy(alpha = 0.25f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
fun AchievementsHallSummary(
    unlocked: Int,
    total: Int,
    primary: Color,
    onBg: Color
) {
    TrophyHallFramedCard(horizontalPadding = 20.dp) {
        TrophyHallHeaderRow(
            primary = primary,
            onBg = onBg,
            unlocked = unlocked,
            total = total,
            trailing = { }
        )
        Text(
            "Your full collection — locked badges show progress toward the next unlock.",
            style = MaterialTheme.typography.bodySmall,
            color = onBg.copy(alpha = 0.55f),
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .padding(bottom = 8.dp),
            lineHeight = 18.sp
        )
        TrophyHallProgressBar(unlocked = unlocked, total = total, onBg = onBg)
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun TrophyHallSectionDivider(title: String, primary: Color, onBg: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = onBg.copy(alpha = 0.08f)
        )
        Text(
            text = title.uppercase(),
            fontWeight = FontWeight.ExtraBold,
            fontSize = 10.sp,
            letterSpacing = 1.8.sp,
            color = primary,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = onBg.copy(alpha = 0.08f)
        )
    }
}
