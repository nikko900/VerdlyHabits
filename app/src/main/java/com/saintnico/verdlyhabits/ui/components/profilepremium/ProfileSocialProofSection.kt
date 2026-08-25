package com.saintnico.verdlyhabits.ui.components.profilepremium

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.engine.ProfileSocialEngine
import com.saintnico.verdlyhabits.ui.theme.GoldColor
import kotlin.random.Random

@Composable
fun ProfileSocialProofSection(
    snapshot: ProfileSocialEngine.ProfileSocialSnapshot,
    weeklyProfileViews: Int,
    isPro: Boolean,
    primary: Color,
    onBg: Color,
    modifier: Modifier = Modifier,
    onUnlockPro: () -> Unit = {},
    pulseFeed: List<ProfileSocialEngine.ProfileActivityItem>? = null,
    onSeeAllActivity: (() -> Unit)? = null,
) {
    val stats = snapshot.stats
    val livePulse = pulseFeed ?: snapshot.activityFeed
    val animatedFriends by animateIntAsState(stats.friendsCount, tween(900), label = "friends")
    val animatedDuo by animateIntAsState(stats.duoStreakDays, tween(900), label = "duo")
    val animatedWins by animateIntAsState(stats.challengeWins, tween(900), label = "wins")
    val animatedReferrals by animateIntAsState(stats.referralsSent, tween(900), label = "refs")

    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(primary.copy(alpha = 0.28f), GoldColor.copy(alpha = 0.16f)),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.Timeline, null, tint = primary, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.size(12.dp))
            Column {
                Text(
                    "YOUR CREW",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 11.sp,
                    letterSpacing = 2.sp,
                    color = primary,
                )
                Text(
                    "Community watching your rhythm",
                    style = MaterialTheme.typography.bodySmall,
                    color = onBg.copy(alpha = 0.55f),
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        ) {
            Column(Modifier.padding(vertical = 16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    SocialStatCell("$animatedFriends", "FRIENDS", Icons.Default.Groups, primary, onBg)
                    SocialDivider(onBg)
                    SocialStatCell(
                        if (animatedDuo > 0) "$animatedDuo" else "—",
                        "DUO",
                        Icons.Default.LocalFireDepartment,
                        Color(0xFFFF7043),
                        onBg,
                    )
                    SocialDivider(onBg)
                    SocialStatCell("$animatedWins", "WINS", Icons.Rounded.EmojiEvents, GoldColor, onBg)
                    SocialDivider(onBg)
                    SocialStatCell("$animatedReferrals", "INVITES", Icons.Default.PersonAdd, primary, onBg)
                }

                if (snapshot.peopleMotivatedToday > 0) {
                    Spacer(Modifier.height(14.dp))
                    Row(
                        modifier = Modifier
                            .padding(horizontal = 16.dp)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(primary.copy(alpha = 0.14f), GoldColor.copy(alpha = 0.08f)),
                                ),
                            )
                            .border(1.dp, primary.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Favorite, null, tint = Color(0xFFE57373), modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "${snapshot.peopleMotivatedToday} people you motivate today",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = onBg,
                            )
                            Text(
                                "Friends locked in through your duo & challenges",
                                fontSize = 11.sp,
                                color = onBg.copy(alpha = 0.55f),
                            )
                        }
                    }
                }

                if (livePulse.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(
                        modifier = Modifier.padding(horizontal = 16.dp),
                        color = onBg.copy(alpha = 0.06f),
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            "LIVE PULSE",
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            letterSpacing = 1.8.sp,
                            color = onBg.copy(alpha = 0.45f),
                        )
                        if (onSeeAllActivity != null) {
                            TextButton(onClick = onSeeAllActivity) {
                                Text(
                                    "See all",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primary,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    livePulse.take(5).forEach { item ->
                        ActivityFeedRow(item = item, onBg = onBg, primary = primary)
                    }
                }

                Spacer(Modifier.height(12.dp))
                ProfileVisitorsRow(
                    weeklyViews = weeklyProfileViews,
                    isPro = isPro,
                    onBg = onBg,
                    primary = primary,
                    onClick = onUnlockPro,
                )
            }
        }
    }
}

@Composable
private fun SocialStatCell(
    value: String,
    label: String,
    icon: ImageVector,
    tint: Color,
    onBg: Color,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(tint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = tint)
        Text(
            label,
            fontSize = 9.sp,
            letterSpacing = 1.2.sp,
            color = onBg.copy(alpha = 0.45f),
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun SocialDivider(onBg: Color) {
    Box(
        Modifier
            .size(width = 1.dp, height = 36.dp)
            .background(onBg.copy(alpha = 0.08f)),
    )
}

@Composable
private fun ActivityFeedRow(
    item: ProfileSocialEngine.ProfileActivityItem,
    onBg: Color,
    primary: Color,
) {
    val dotColor = when (item.accent) {
        ProfileSocialEngine.ActivityAccent.SUCCESS -> primary
        ProfileSocialEngine.ActivityAccent.RANK -> GoldColor
        ProfileSocialEngine.ActivityAccent.DUO -> Color(0xFFFF7043)
        ProfileSocialEngine.ActivityAccent.NEUTRAL -> onBg.copy(alpha = 0.35f)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(dotColor),
        )
        Spacer(Modifier.size(10.dp))
        Text(
            item.message,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = onBg.copy(alpha = 0.82f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun ProfileVisitorsRow(
    weeklyViews: Int,
    isPro: Boolean,
    onBg: Color,
    primary: Color,
    onClick: () -> Unit = {},
) {
    val fakeViews = remember { Random.nextInt(3, 14) }
    
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(onBg.copy(alpha = 0.04f))
            .clickable(enabled = !isPro, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (isPro) Icons.Default.Visibility else Icons.Rounded.Lock,
            contentDescription = null,
            tint = if (isPro) primary else onBg.copy(alpha = 0.35f),
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.size(10.dp))
        if (isPro) {
            Text(
                if (weeklyViews > 0) {
                    "$weeklyViews friend${if (weeklyViews == 1) "" else "s"} viewed your profile this week"
                } else {
                    "Friends can see your streak when they visit — views show here"
                },
                fontSize = 12.sp,
                color = onBg.copy(alpha = 0.65f),
                lineHeight = 16.sp,
            )
        } else {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Profile visitors — Pro perk",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = onBg.copy(alpha = 0.7f),
                )
                Text(
                    "See who checked your streak this week",
                    fontSize = 11.sp,
                    color = onBg.copy(alpha = 0.45f),
                )
            }
            
            // Fake blurred profiles to entice users
            Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color.Gray.copy(alpha = 0.5f),
                                        Color.DarkGray.copy(alpha = 0.8f)
                                    )
                                )
                            )
                            .blur(4.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .border(1.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        .background(primary.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "+$fakeViews",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = primary
                    )
                }
            }
        }
    }
}
