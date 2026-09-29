package com.saintnico.verdlyhabits.ui.screens.goals.nudge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.local.goals.GoalEntity
import com.saintnico.verdlyhabits.engine.GoalCelebrationCopy
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaintainNudgeSheet(
    goal: GoalEntity,
    onDismiss: () -> Unit,
    onOpenGoal: () -> Unit,
) {
    val copy = GoalCelebrationCopy.maintainNudge(goal)
    val palette = GoalCelebrationCopy.palette(goal.goalType)
    val primary = Color(palette.primary)
    val secondary = Color(palette.secondary)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                primary.copy(alpha = 0.18f),
                                secondary.copy(alpha = 0.12f),
                            ),
                        ),
                    )
                    .padding(20.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(copy.emoji, fontSize = 40.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        copy.badge.uppercase(),
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 2.sp,
                        color = primary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        copy.headline,
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 24.sp,
                        lineHeight = 30.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                copy.body,
                fontFamily = dmSansFamily,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
            )

            Spacer(Modifier.height(8.dp))

            Text(
                copy.cheer,
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = primary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onOpenGoal,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primary),
                contentPadding = PaddingValues(vertical = 16.dp),
            ) {
                Text(
                    "Do one small win now",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(8.dp))

            TextButton(onClick = onDismiss) {
                Text(
                    "I'll get to it later",
                    fontFamily = dmSansFamily,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                )
            }
        }
    }
}
