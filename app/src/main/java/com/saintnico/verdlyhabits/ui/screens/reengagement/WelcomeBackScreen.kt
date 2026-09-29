package com.saintnico.verdlyhabits.ui.screens.reengagement

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
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.engagement.ComebackCopyBank
import com.saintnico.verdlyhabits.ui.assets.VerdlyAssetCatalog
import com.saintnico.verdlyhabits.ui.components.VerdlyHeroIllustration
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import com.saintnico.verdlyhabits.ui.theme.AccentGold
import com.saintnico.verdlyhabits.ui.theme.PrimaryGreen

/**
 * Non-shame re-entry surface shown after 2+ days away from the app.
 * Frames return as effort-based recovery, never as punishment or loss.
 */
@Composable
fun WelcomeBackScreen(
    headline: String,
    body: String,
    contextLine: String?,
    primaryLabel: String,
    suggestedHabit: HabitItem?,
    onPrimaryAction: () -> Unit,
    onDismiss: () -> Unit,
    onShown: () -> Unit,
) {
    LaunchedEffect(Unit) { onShown() }

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
            VerdlyHeroIllustration(
                drawableRes = VerdlyAssetCatalog.WelcomeBackPeople,
                size = 160.dp,
            )

            Text(
                headline,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                color = onBg,
                textAlign = TextAlign.Center,
            )

            if (contextLine != null) {
                Text(
                    contextLine,
                    fontSize = 14.sp,
                    color = onBg.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    body,
                    fontSize = 15.sp,
                    color = onBg.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                )

                if (suggestedHabit != null) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        suggestedHabit.title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = onBg,
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
            Button(
                onClick = onPrimaryAction,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGold,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(primaryLabel, fontWeight = FontWeight.Bold)
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    ComebackCopyBank.dismissLabel(),
                    color = onBg.copy(alpha = 0.6f),
                )
            }
        }
    }
}
