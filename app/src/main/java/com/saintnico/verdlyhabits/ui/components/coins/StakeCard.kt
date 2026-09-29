@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.saintnico.verdlyhabits.ui.components.coins

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.local.coins.ChallengeStakeEntity
import com.saintnico.verdlyhabits.data.model.CoinEconomy
import com.saintnico.verdlyhabits.data.model.CoinOathCopy
import kotlin.math.roundToInt

enum class OathDayStatus { SAFE, AT_RISK, MISSED }

@Composable
fun StakeConsistencyCard(
    balance: Int,
    activeStake: ChallengeStakeEntity?,
    streak: Int,
    daysRemaining: Long,
    dayStatus: OathDayStatus,
    isPremium: Boolean,
    challengeActive: Boolean,
    onCommitClick: (Int) -> Unit,
    onBuyCoins: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    if (!challengeActive) return

    val maxStake = CoinEconomy.maxStakeFor(balance).coerceAtLeast(CoinEconomy.MIN_STAKE)
    var sliderValue by remember(balance, maxStake) {
        mutableFloatStateOf(CoinEconomy.MIN_STAKE.toFloat().coerceAtMost(maxStake.toFloat()))
    }
    val selected = sliderValue.roundToInt().coerceIn(CoinEconomy.MIN_STAKE, maxStake.coerceAtLeast(CoinEconomy.MIN_STAKE))

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        ),
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CoinIcon(size = 20.dp, premium = isPremium)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (activeStake != null) "Your oath" else "Swear your consistency",
                    fontFamily = com.saintnico.verdlyhabits.ui.theme.frauncesFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 18.sp,
                )
            }

            if (activeStake != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "${activeStake.stakedAmount}",
                    fontFamily = com.saintnico.verdlyhabits.ui.theme.frauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 36.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    "coins on the line",
                    fontFamily = com.saintnico.verdlyhabits.ui.theme.dmSansFamily,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
                Spacer(Modifier.height(10.dp))
                LiveOathPanel(
                    amount = activeStake.stakedAmount,
                    streak = streak,
                    daysRemaining = daysRemaining,
                    status = dayStatus,
                    isPremium = isPremium,
                )
            } else if (CoinEconomy.canStake(balance)) {
                Spacer(Modifier.height(8.dp))
                Text(
                    CoinOathCopy.lockLine(selected, isPremium),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "$selected coins",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 22.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = CoinEconomy.MIN_STAKE.toFloat()..maxStake.toFloat().coerceAtLeast(CoinEconomy.MIN_STAKE.toFloat()),
                    steps = ((maxStake - CoinEconomy.MIN_STAKE) / 5).coerceAtLeast(0),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                    ),
                )
                Text(
                    "Up to $maxStake coins (${(CoinEconomy.MAX_STAKE_FRACTION * 100).roundToInt()}% of balance)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { onCommitClick(selected) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(CoinOathCopy.SWEAR, fontWeight = FontWeight.Bold)
                }
            } else {
                Spacer(Modifier.height(10.dp))
                Text(
                    "You need ${CoinEconomy.minBalanceToStake()} coins to swear (min ${CoinEconomy.MIN_STAKE}, at most half your wallet).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onBuyCoins,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                ) {
                    Text(CoinOathCopy.PACK_HEADER, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun LiveOathPanel(
    amount: Int,
    streak: Int,
    daysRemaining: Long,
    status: OathDayStatus,
    isPremium: Boolean,
    modifier: Modifier = Modifier,
) {
    val (label, color) = when (status) {
        OathDayStatus.SAFE -> CoinOathCopy.liveSafe(amount, daysRemaining) to MaterialTheme.colorScheme.primary
        OathDayStatus.AT_RISK -> CoinOathCopy.liveAtRisk(amount) to androidx.compose.ui.graphics.Color(0xFFE08A2E)
        OathDayStatus.MISSED -> "$amount gone. The group was boosted" to MaterialTheme.colorScheme.error
    }
    Column(modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(color.copy(alpha = 0.10f))
                .border(1.dp, color.copy(alpha = 0.28f), RoundedCornerShape(50))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoinIcon(size = 14.dp, premium = isPremium)
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp,
                color = color,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "$streak-day streak on this challenge",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        )
    }
}

@Composable
fun AtStakeChip(
    amount: Int,
    streak: Int,
    isPremium: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(50))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CoinIcon(size = 14.dp, premium = isPremium)
        Spacer(Modifier.width(6.dp))
        Text(
            "$amount planted",
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "$streak-day streak",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        )
    }
}

@Composable
fun HomeOathChip(
    amount: Int,
    daysRemaining: Long,
    atRisk: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    chipSkinId: String? = null,
) {
    val color = oathChipColors(chipSkinId, atRisk, MaterialTheme.colorScheme.primary)
    val text = if (atRisk) CoinOathCopy.liveAtRisk(amount) else CoinOathCopy.liveSafe(amount, daysRemaining)
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(
            if (chipSkinId == "oath_goldleaf") 2.dp else 1.dp,
            color.copy(alpha = if (chipSkinId == "oath_goldleaf") 0.55f else 0.28f),
        ),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CoinIcon(size = 16.dp, premium = false)
            Spacer(Modifier.width(8.dp))
            Text(
                text,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = color,
                modifier = Modifier.weight(1f),
            )
            Text(
                "Open",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = color,
            )
        }
    }
}

@Composable
fun StakeConfirmSheet(
    amount: Int,
    isPremium: Boolean,
    convertsDone: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    var locking by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                "Swear this oath",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(14.dp))
            ForecastLine(CoinOathCopy.lockLine(amount, isPremium))
            Spacer(Modifier.height(8.dp))
            ForecastLine(CoinOathCopy.missLine(amount))
            Spacer(Modifier.height(8.dp))
            ForecastLine(CoinOathCopy.NO_RETURN)
            CoinOathCopy.nextTitleLine(convertsDone)?.let { next ->
                Spacer(Modifier.height(8.dp))
                ForecastLine(next)
            }
            Spacer(Modifier.height(20.dp))
            if (locking) {
                com.saintnico.verdlyhabits.ui.components.VerdlyLoadingIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    size = 40.dp,
                )
                Spacer(Modifier.height(12.dp))
            }
            Button(
                onClick = {
                    locking = true
                    onConfirm()
                },
                enabled = !locking,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
            ) {
                Text("${CoinOathCopy.SWEAR}. $amount coins", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Not yet")
            }
        }
    }
}

@Composable
private fun ForecastLine(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.82f),
        lineHeight = 22.sp,
    )
}

@Composable
fun AfterLockBuySheet(
    onBuy: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Text(
                CoinOathCopy.EMPTY_AFTER_LOCK,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                "That was the taste. The next oath. And looking like the person who meant it. Takes a pack.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.75f),
                lineHeight = 22.sp,
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onBuy,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
            ) {
                Text(CoinOathCopy.PACK_HEADER, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Later")
            }
        }
    }
}
