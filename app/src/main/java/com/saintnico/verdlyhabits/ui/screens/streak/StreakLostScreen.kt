package com.saintnico.verdlyhabits.ui.screens.streak

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.domain.streak.StreakLossRecord
import com.saintnico.verdlyhabits.ui.assets.VerdlyAssetCatalog
import com.saintnico.verdlyhabits.ui.components.StreakFlame
import com.saintnico.verdlyhabits.ui.components.VerdlyHeroLottie

/**
 * Dedicated loss moment. Surfaced once per lost run (persisted + acknowledged), never as a
 * silent status flip. When [StreakLossRecord.wasPreventableWithShield] is true this is also
 * the highest-intent paywall entry for shields.
 */
@Composable
fun StreakLostScreen(
    loss: StreakLossRecord,
    habitTitle: String,
    onGetShield: () -> Unit,
    onDismiss: () -> Unit,
) {
    val bg = MaterialTheme.colorScheme.background
    val onBg = MaterialTheme.colorScheme.onBackground

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
            .padding(24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            VerdlyHeroLottie(
                rawRes = VerdlyAssetCatalog.StreakLostLottie,
                size = 120.dp,
            )

            Text(
                "Your ${loss.finalStreak}-day streak ended",
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = onBg,
                textAlign = TextAlign.Center,
            )

            Text(
                habitTitle,
                fontSize = 15.sp,
                color = onBg.copy(alpha = 0.65f),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (loss.wasPreventableWithShield) {
                    Icon(
                        Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(28.dp),
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        "A shield would have saved this streak",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = onBg,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "One miss is recoverable. Hold a shield so the next gap doesn't end the run.",
                        fontSize = 13.sp,
                        color = onBg.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                    )
                } else {
                    Text(
                        "You're still growing. Plant tomorrow. This miss doesn't define the grove.",
                        fontSize = 14.sp,
                        color = onBg.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (loss.wasPreventableWithShield) {
                Button(
                    onClick = onGetShield,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFB300),
                        contentColor = Color(0xFF1A1A1A),
                    ),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Get a streak shield", fontWeight = FontWeight.Bold)
                }
            }
            if (loss.wasPreventableWithShield) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Continue", color = onBg.copy(alpha = 0.6f))
                }
            } else {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Text("Continue", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
