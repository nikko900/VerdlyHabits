package com.saintnico.verdlyhabits.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.local.goals.GoalEntity
import com.saintnico.verdlyhabits.engine.GoalCelebrationCopy
import com.saintnico.verdlyhabits.engine.GoalDailyRecapEngine
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDailyRecapSheet(
    goal: GoalEntity,
    recap: GoalDailyRecapEngine.DailyRecap,
    onDismiss: () -> Unit,
    onShare: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val palette = GoalCelebrationCopy.palette(goal.goalType)
    val accent = Color(palette.primary)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "Today's recap",
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 2.sp,
                color = accent,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                recap.dateLabel,
                fontFamily = dmSansFamily,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(accent.copy(alpha = 0.08f))
                    .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    goal.title,
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    recap.heroNumber,
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 44.sp,
                    color = accent,
                    textAlign = TextAlign.Center,
                    lineHeight = 48.sp,
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    recap.statLine,
                    fontFamily = dmSansFamily,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (goal.whyStatement.isNotBlank()) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "\"${goal.whyStatement}\"",
                        fontFamily = frauncesFamily,
                        fontStyle = FontStyle.Italic,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    recap.cheerLine,
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = accent,
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                "Shareable for today only. Post your number, then keep going.",
                fontFamily = dmSansFamily,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onShare,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
            ) {
                Icon(Icons.Rounded.Share, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Share today's recap",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            TextButton(onClick = onDismiss) {
                Text("Not now", fontFamily = dmSansFamily)
            }
        }
    }
}
