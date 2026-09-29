package com.saintnico.verdlyhabits.ui.components.habit

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.saintnico.verdlyhabits.ui.assets.VerdlyAssetCatalog
import com.saintnico.verdlyhabits.ui.components.VerdlyHeroLottie
import com.saintnico.verdlyhabits.ui.theme.ProfileAccent
import com.saintnico.verdlyhabits.ui.theme.ProfileAccents
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import kotlin.math.abs

@Composable
fun HabitPlantedCelebrationDialog(
    visible: Boolean,
    habitCount: Int,
    habitTitle: String,
    onDismiss: () -> Unit,
    onSeePro: () -> Unit,
) {
    if (!visible) return
    val theme = celebrationTheme(habitTitle)
    val isFirstHabit = habitCount == 1
    val headline = when (habitCount) {
        1 -> "Your first seed is in the ground"
        2 -> "Two seeds in the ground"
        3 -> "You're building a grove"
        4 -> "Four habits. Momentum is real"
        else -> "Another habit planted"
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(28.dp)),
            color = Color.Transparent,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(theme.heroTop, theme.heroMid, theme.heroBottom),
                        ),
                    ),
            ) {
                Canvas(Modifier.matchParentSize()) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(theme.glow.copy(alpha = 0.35f), Color.Transparent),
                            center = Offset(size.width * 0.85f, size.height * 0.1f),
                            radius = size.minDimension * 0.55f,
                        ),
                    )
                }
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (isFirstHabit) {
                        VerdlyHeroLottie(
                            rawRes = VerdlyAssetCatalog.FirstHabitCelebrationLottie,
                            size = 120.dp,
                            loop = false,
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(theme.glow.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            if (isFirstHabit) {
                                Image(
                                    painter = painterResource(VerdlyAssetCatalog.FirstHabitCelebrationSticker),
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    contentScale = ContentScale.Fit,
                                )
                            } else {
                                Icon(Icons.Rounded.Eco, null, tint = theme.highlight, modifier = Modifier.size(26.dp))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                headline,
                                fontFamily = frauncesFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = theme.ink,
                            )
                            Text(
                                "“$habitTitle” is live on home.",
                                fontFamily = dmSansFamily,
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.78f),
                            )
                        }
                    }

                    Text(
                        if (isFirstHabit) {
                            "You showed up. Premium keeps that momentum when life gets messy: streak shields, unlimited habits, and deeper insights."
                        } else {
                            "Premium members keep momentum when life gets messy. Streak shields, unlimited habits, and deep analytics when motivation dips."
                        },
                        fontFamily = dmSansFamily,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = Color.White.copy(alpha = 0.82f),
                        modifier = Modifier.fillMaxWidth(),
                    )

                    ProBenefitRow(Icons.Rounded.Shield, "Streak shields when you miss a day", theme)
                    ProBenefitRow(Icons.Rounded.WorkspacePremium, "Unlimited habits. No cap at five", theme)
                    ProBenefitRow(Icons.Rounded.Analytics, "90-day heatmaps & habit insights", theme)

                    Button(
                        onClick = onSeePro,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = theme.glow,
                            contentColor = Color(0xFF0D1510),
                        ),
                    ) {
                        Text("See Verdly Premium", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                    }
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    ) {
                        Text(
                            "Not now. Back to home",
                            color = Color.White.copy(alpha = 0.72f),
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProBenefitRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    theme: ProfileAccent,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = theme.highlight, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            label,
            fontFamily = dmSansFamily,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.88f),
        )
    }
}

private fun celebrationTheme(seed: String): ProfileAccent {
    val idx = abs(seed.hashCode()) % ProfileAccents.all.size
    return ProfileAccents.all[idx]
}
