package com.saintnico.verdlyhabits.ui.screens.achievements

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.engine.Achievement
import com.saintnico.verdlyhabits.engine.TrophyCatalog
import com.saintnico.verdlyhabits.engine.TrophyEvaluator
import com.saintnico.verdlyhabits.engine.TrophyTierLevel
import com.saintnico.verdlyhabits.ui.components.AchievementTrophy
import com.saintnico.verdlyhabits.ui.components.TrophyHallFramedCard
import com.saintnico.verdlyhabits.ui.components.TrophyHallListRow
import com.saintnico.verdlyhabits.ui.components.TrophyHallSectionDivider
import com.saintnico.verdlyhabits.ui.components.TrophyRankChip
import com.saintnico.verdlyhabits.ui.components.tierLevelToVisualTier
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.platform.LocalContext
import com.saintnico.verdlyhabits.ui.components.share.shareAchievementCard
import com.saintnico.verdlyhabits.ui.theme.GoldColor
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(
    achievements: List<Achievement>,
    onBack: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val onBg = MaterialTheme.colorScheme.onBackground

    val hallTrophies = remember(achievements) {
        TrophyEvaluator.hallTrophies(achievements)
    }

    val totalCount = TrophyCatalog.all.size
    val unlockedCount = hallTrophies.count { it.state == TrophyEvaluator.VisibleState.EARNED }
    val groupedTrophies = hallTrophies.groupBy { it.tier }

    var selectedAchievement by remember { mutableStateOf<TrophyEvaluator.VisibleTrophy?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Hall of trophies",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = onBg
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            item {
                TrophyHallFramedCard(horizontalPadding = 20.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 12.dp, top = 18.dp, bottom = 12.dp),
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
                                    "RANK COLLECTION",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    letterSpacing = 2.2.sp,
                                    color = primary
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Bronze → Mythic",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = onBg
                            )
                            Text(
                                "$unlockedCount of $totalCount unlocked · climb the ranks",
                                style = MaterialTheme.typography.bodySmall,
                                color = onBg.copy(alpha = 0.55f)
                            )
                        }
                    }
                    val overall = if (totalCount == 0) 0f else unlockedCount.toFloat() / totalCount
                    LinearProgressIndicator(
                        progress = { overall },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp)
                            .padding(bottom = 16.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = GoldColor,
                        trackColor = onBg.copy(alpha = 0.08f)
                    )
                }
            }

            TrophyTierLevel.entries.forEach { tier ->
                val tierTrophies = groupedTrophies[tier].orEmpty()
                val tierEarned = tierTrophies.count { it.state == TrophyEvaluator.VisibleState.EARNED }
                val tierTotal = tierTrophies.size

                item {
                    TrophyHallSectionDivider(
                        title = "${tier.displayName} · $tierEarned/$tierTotal",
                        primary = primary,
                        onBg = onBg
                    )
                }
                items(tierTrophies, key = { it.achievement.id }) { vt ->
                    val visualTier = tierLevelToVisualTier(vt.tier)
                    TrophyHallListRow(
                        achievement = vt.achievement,
                        visualTier = visualTier,
                        onClick = { selectedAchievement = vt },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }

    selectedAchievement?.let { vt ->
        val a = vt.achievement
        val visualTier = tierLevelToVisualTier(vt.tier)
        val context = LocalContext.current
        ModalBottomSheet(
            onDismissRequest = { selectedAchievement = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AchievementTrophy(
                    icon = a.icon,
                    tier = visualTier,
                    unlocked = a.isUnlocked,
                    size = 96.dp
                )
                Spacer(Modifier.height(10.dp))
                TrophyRankChip(tier = visualTier, unlocked = a.isUnlocked)
                Spacer(Modifier.height(12.dp))
                Text(
                    a.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = onBg,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    a.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = onBg.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Rounded.EmojiEvents,
                        contentDescription = null,
                        tint = GoldColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "+${a.xpReward} XP",
                        fontWeight = FontWeight.Bold,
                        color = GoldColor
                    )
                }

                if (!a.isUnlocked && a.progressTarget > 1) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Progress: ${a.progressCurrent} / ${a.progressTarget}",
                        style = MaterialTheme.typography.bodySmall,
                        color = onBg.copy(alpha = 0.55f)
                    )
                    Spacer(Modifier.height(6.dp))
                    val p = (a.progressCurrent.toFloat() / a.progressTarget).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { p },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(999.dp)),
                        color = primary,
                        trackColor = onBg.copy(alpha = 0.08f)
                    )
                }
                val earnedAt = a.unlockedAt
                if (a.isUnlocked && earnedAt != null) {
                    Spacer(Modifier.height(12.dp))
                    val date = Instant.ofEpochMilli(earnedAt).atZone(ZoneId.systemDefault())
                        .format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
                    Text(
                        "Earned $date",
                        style = MaterialTheme.typography.bodySmall,
                        color = onBg.copy(alpha = 0.45f)
                    )
                }

                if (a.isUnlocked) {
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = { shareAchievementCard(context, a) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GoldColor.copy(alpha = 0.15f),
                            contentColor = GoldColor,
                        ),
                    ) {
                        Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share trophy", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
