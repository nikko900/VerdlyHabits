package com.saintnico.verdlyhabits.ui.components.coins

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.model.CoinOathCopy
import com.saintnico.verdlyhabits.ui.theme.GoldColor
import com.saintnico.verdlyhabits.ui.viewmodel.StakeCeremony
import kotlinx.coroutines.delay

@Composable
fun StakeResolutionCeremony(
    ceremony: StakeCeremony,
    onBuyCoins: () -> Unit,
    onDismiss: () -> Unit,
) {
    var showText by remember { mutableStateOf(false) }
    var showCta by remember { mutableStateOf(false) }

    LaunchedEffect(ceremony) {
        showText = false
        showCta = false
        delay(120)
        showText = true
        delay(700)
        showCta = true
    }

    val headline = when (ceremony) {
        is StakeCeremony.Converted -> CoinOathCopy.convertHeadline(ceremony.xp)
        is StakeCeremony.Forfeited -> CoinOathCopy.FORFEIT_HEAD
    }
    val sub = when (ceremony) {
        is StakeCeremony.Converted -> CoinOathCopy.CONVERT_SUB
        is StakeCeremony.Forfeited -> CoinOathCopy.forfeitSub(ceremony.groupBonus)
    }
    val cta = when (ceremony) {
        is StakeCeremony.Converted -> CoinOathCopy.EMPTY_AFTER_CONVERT
        is StakeCeremony.Forfeited -> CoinOathCopy.SWEAR_AGAIN
    }
    val accent = when (ceremony) {
        is StakeCeremony.Converted -> GoldColor
        is StakeCeremony.Forfeited -> Color(0xFF95D5B2)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xE6000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp),
        ) {
            CoinIcon(size = 56.dp, premium = ceremony is StakeCeremony.Converted)
            Spacer(Modifier.height(28.dp))
            AnimatedVisibility(
                visible = showText,
                enter = fadeIn(tween(500)) + slideInVertically(initialOffsetY = { 16 }),
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        headline,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        when (ceremony) {
                            is StakeCeremony.Converted -> ceremony.habitName
                            is StakeCeremony.Forfeited -> ceremony.habitName
                        },
                        color = accent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        sub,
                        color = Color.White.copy(alpha = 0.7f),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                }
            }
            Spacer(Modifier.height(36.dp))
            AnimatedVisibility(visible = showCta, enter = fadeIn(tween(500))) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = onBuyCoins,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent.copy(alpha = 0.22f),
                            contentColor = accent,
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(cta, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "Tap anywhere to continue",
                        color = Color.White.copy(alpha = 0.3f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 20.dp),
                    )
                }
            }
        }
    }
}
