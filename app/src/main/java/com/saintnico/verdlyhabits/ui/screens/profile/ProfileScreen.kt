package com.saintnico.verdlyhabits.ui.screens.profile

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.saintnico.verdlyhabits.engine.GamificationEngine
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import com.saintnico.verdlyhabits.ui.viewmodel.UserStatsUiState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import com.saintnico.verdlyhabits.ui.components.AnimatedHabitIcon
import com.saintnico.verdlyhabits.ui.components.HabitHeatmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    statsState: UserStatsUiState,
    habits: List<HabitItem>,
    userName: String,
    userUsername: String,
    userPhotoUri: String?,
    userBio: String,
    userMotto: String,
    userFavoritePlant: String,
    totalFocusMinutes: Int,
    onNavigateToAchievements: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val accent = MaterialTheme.colorScheme.primary
    val bg = MaterialTheme.colorScheme.background
    val onBg = MaterialTheme.colorScheme.onBackground
    val surface = MaterialTheme.colorScheme.surface

    val hasUsername = userUsername.isNotBlank() && userUsername != "UnknownRival"
    val hasPhoto = !userPhotoUri.isNullOrBlank()
    val hasBio = userBio.isNotBlank() && userBio != "Planting habits, growing roots."
    val completionScore = listOf(hasUsername, hasPhoto, hasBio).count { it } / 3f

    val animProgress by animateFloatAsState(
        statsState.progressToNextLevel, tween(1200, easing = EaseOutCubic), label = "xp_bar"
    )

    val memberDate = remember(statsState.memberSince) {
        Instant.ofEpochMilli(statsState.memberSince)
            .atZone(ZoneId.systemDefault()).toLocalDate()
            .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    }

    // Shimmer on the level ring
    val infiniteTransition = rememberInfiniteTransition(label = "ring_shimmer")
    val shimmerAngle by infiniteTransition.animateFloat(
        0f, 360f, infiniteRepeatable(tween(4000, easing = LinearEasing)), label = "shimmer_angle"
    )

    val activeHabits = habits.filter { !it.isArchived }
    val totalActiveStreaks = activeHabits.sumOf { it.streak }
    val bestCurrentStreak = activeHabits.maxOfOrNull { it.streak } ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ─── Hero Section with gradient overlay ─────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .drawBehind {
                    val gradientBrush = Brush.verticalGradient(
                        0f to accent.copy(alpha = 0.18f),
                        0.6f to accent.copy(alpha = 0.04f),
                        1f to Color.Transparent
                    )
                    drawRect(gradientBrush)
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // Settings button top right
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp, end = 8.dp),
                    horizontalArrangement = Arrangement.End) {
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(Icons.Default.Settings, null, tint = onBg.copy(alpha = 0.5f))
                    }
                }

                // Level ring around avatar
                Box(modifier = Modifier.size(110.dp), contentAlignment = Alignment.Center) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Background ring
                        drawArc(accent.copy(alpha = 0.1f), 0f, 360f, false, style = Stroke(5.dp.toPx()))
                        // Progress ring
                        drawArc(accent, -90f, 360f * animProgress, false,
                            style = Stroke(5.dp.toPx(), cap = StrokeCap.Round))
                        // Shimmer dot
                        val angle = Math.toRadians((-90f + 360f * animProgress).toDouble())
                        val r = size.minDimension / 2f
                        val dotX = center.x + (r * Math.cos(angle)).toFloat()
                        val dotY = center.y + (r * Math.sin(angle)).toFloat()
                        if (animProgress > 0.05f) {
                            drawCircle(Color.White, radius = 4.dp.toPx(), center = Offset(dotX, dotY))
                        }
                    }

                    if (completionScore < 1f) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.size(104.dp)) {
                            drawArc(
                                color = accent.copy(alpha = 0.2f),
                                startAngle = -90f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                            drawArc(
                                color = accent,
                                startAngle = -90f,
                                sweepAngle = 360f * completionScore,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round)
                            )
                        }
                    }

                    if (!userPhotoUri.isNullOrBlank()) {
                        AsyncImage(
                            model = userPhotoUri,
                            contentDescription = "Avatar",
                            modifier = Modifier.size(90.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.size(90.dp).clip(CircleShape)
                                .background(accent.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                (userUsername.ifBlank { "L" }).firstOrNull()?.toString()?.uppercase() ?: "L",
                                fontSize = 36.sp, fontWeight = FontWeight.Bold, color = accent
                            )
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(userUsername.ifBlank { "UnknownRival" }, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = onBg)
                
                if (completionScore < 1f) {
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .background(accent.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            progress = { completionScore },
                            modifier = Modifier.size(14.dp),
                            color = accent,
                            strokeWidth = 2.dp,
                            trackColor = accent.copy(alpha = 0.2f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Profile ${(completionScore * 100).toInt()}% complete",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accent
                        )
                    }
                }
                if (userMotto.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "\"$userMotto\"",
                        fontSize = 14.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = onBg.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Eco, null, tint = accent, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Level ${statsState.level} · ${statsState.levelTitle}",
                        fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = accent
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text("Member since $memberDate", fontSize = 11.sp, color = onBg.copy(alpha = 0.4f))
            }
        }

        // ─── XP Card ────────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).offset(y = (-20).dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = surface),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                    Column {
                        Text("${statsState.totalXp} XP", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = onBg)
                        Text("Total experience earned", fontSize = 11.sp, color = onBg.copy(alpha = 0.4f))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${statsState.xpToNextLevel} XP to go", fontSize = 12.sp, color = onBg.copy(alpha = 0.5f))
                        Text("→ ${GamificationEngine.levelTitle(statsState.level + 1)}",
                            fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = accent)
                    }
                }
                Spacer(Modifier.height(12.dp))
                // Progress bar
                Box(
                    modifier = Modifier.fillMaxWidth().height(8.dp)
                        .clip(CircleShape).background(onBg.copy(alpha = 0.06f))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(animProgress).fillMaxHeight()
                            .clip(CircleShape)
                            .background(Brush.horizontalGradient(listOf(accent.copy(alpha = 0.6f), accent)))
                    )
                }
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                    Text("Lv ${statsState.level}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = accent)
                    Text("Lv ${statsState.level + 1}", fontSize = 10.sp, color = onBg.copy(alpha = 0.3f))
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // ─── Stats Grid ─────────────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PremiumStatCard(Modifier.weight(1f), Icons.Default.Spa, userFavoritePlant, "Favorite Plant", accent)
            PremiumStatCard(Modifier.weight(1f), Icons.Default.Check, "${statsState.totalCompletions}", "Completions", Color(0xFF4CAF50))
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PremiumStatCard(Modifier.weight(1f), Icons.Default.LocalFireDepartment, "${statsState.longestStreakEver}d", "Best Streak", Color(0xFFFF9800))
            PremiumStatCard(Modifier.weight(1f), Icons.Default.Timer, "${totalFocusMinutes / 60}h ${totalFocusMinutes % 60}m", "Focus Time", Color(0xFF7B61FF))
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PremiumStatCard(Modifier.weight(1f), Icons.Default.TrendingUp, "$totalActiveStreaks", "Active Streaks", Color(0xFF2196F3))
            PremiumStatCard(Modifier.weight(1f), Icons.Default.EmojiEvents, "${statsState.achievements.count { it.isUnlocked }}", "Badges", Color(0xFFFFD54F))
        }

        Spacer(Modifier.height(24.dp))

        // ─── Habit Heatmap ───────────────────────────────────────────────
        Text("Activity", fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = onBg,
            modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))

        // Build heatmap data from all habits
        val completionData = remember(habits) {
            val map = mutableMapOf<String, Int>()
            habits.forEach { habit ->
                habit.completedDates.forEach { dateStr ->
                    map[dateStr] = (map[dateStr] ?: 0) + 1
                }
            }
            map.toMap()
        }

        Card(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = surface),
            elevation = CardDefaults.cardElevation(2.dp)
        ) {
            HabitHeatmap(
                completionData = completionData,
                modifier = Modifier.padding(12.dp),
                accentColor = accent
            )
        }

        Spacer(Modifier.height(24.dp))

        // ─── Trophy Cabinet ─────────────────────────────────────────────
        Text("Trophy Cabinet", fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium,
            color = onBg,
            modifier = Modifier.padding(horizontal = 16.dp))
        Spacer(Modifier.height(8.dp))

        val trophyBadges = listOf(
            Triple("7 Day Warrior", Icons.Default.Shield, bestCurrentStreak >= 7),
            Triple("First Challenge Won", Icons.Default.EmojiEvents, statsState.achievements.any { it.id == "first_challenge" && it.isUnlocked }),
            Triple("Photo Proof Legend", Icons.Default.PhotoCamera, habits.any { it.completionProofs.size >= 10 }),
            Triple("30 Day Legend", Icons.Default.Diamond, statsState.longestStreakEver >= 30),
            Triple("100 Completions", Icons.Default.Verified, statsState.totalCompletions >= 100),
            Triple("Focus Master", Icons.Default.Spa, totalFocusMinutes >= 600),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            trophyBadges.forEach { (title, icon, unlocked) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier.size(52.dp).clip(CircleShape)
                            .background(
                                if (unlocked) Brush.radialGradient(listOf(accent.copy(alpha = 0.2f), accent.copy(alpha = 0.06f)))
                                else Brush.radialGradient(listOf(onBg.copy(alpha = 0.05f), onBg.copy(alpha = 0.02f)))
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (unlocked) {
                            AnimatedHabitIcon(icon, color = accent, size = 24.dp)
                        } else {
                            Icon(Icons.Default.Lock, null,
                                tint = onBg.copy(alpha = 0.15f),
                                modifier = Modifier.size(22.dp))
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(title, fontSize = 9.sp,
                        color = if (unlocked) onBg.copy(alpha = 0.7f) else onBg.copy(alpha = 0.25f),
                        fontWeight = if (unlocked) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(60.dp))
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ─── Achievements Preview ───────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            Arrangement.SpaceBetween, Alignment.CenterVertically
        ) {
            Text("Achievements", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = onBg)
            TextButton(onClick = onNavigateToAchievements) { Text("See all") }
        }
        Spacer(Modifier.height(8.dp))

        val unlockedAchievements = statsState.achievements.filter { it.isUnlocked }.take(6)
        if (unlockedAchievements.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = surface)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Lock, null, tint = onBg.copy(alpha = 0.2f), modifier = Modifier.size(28.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("No achievements yet", fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium, color = onBg.copy(alpha = 0.5f))
                        Text("Complete habits and build streaks to unlock badges.",
                            style = MaterialTheme.typography.bodySmall, color = onBg.copy(alpha = 0.3f))
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                unlockedAchievements.forEach { achievement ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier.size(52.dp).clip(CircleShape)
                                .background(
                                    Brush.radialGradient(
                                        listOf(accent.copy(alpha = 0.2f), accent.copy(alpha = 0.06f))
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            AnimatedHabitIcon(achievement.icon, color = accent, size = 26.dp)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(achievement.title, fontSize = 9.sp,
                            color = onBg.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium,
                            maxLines = 1)
                    }
                }
            }
        }

        Spacer(Modifier.height(40.dp))
    }
}

@Composable
private fun PremiumStatCard(
    modifier: Modifier,
    icon: ImageVector,
    value: String,
    label: String,
    iconTint: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                AnimatedHabitIcon(icon, color = iconTint, size = 18.dp)
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground)
                Text(label, fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f))
            }
        }
    }
}
