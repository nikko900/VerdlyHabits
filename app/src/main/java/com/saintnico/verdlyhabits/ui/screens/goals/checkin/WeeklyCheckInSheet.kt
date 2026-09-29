package com.saintnico.verdlyhabits.ui.screens.goals.checkin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.local.goals.CheckInStatus
import com.saintnico.verdlyhabits.data.local.goals.GoalEntity
import com.saintnico.verdlyhabits.data.local.goals.GoalType
import com.saintnico.verdlyhabits.engine.GoalCelebrationCopy
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyCheckInSheet(
    goal: GoalEntity,
    onDismiss: () -> Unit,
    onSubmit: (CheckInStatus, String) -> Unit,
) {
    val intro = remember(goal.id) { GoalCelebrationCopy.weeklyCheckInIntro(goal) }
    val palette = GoalCelebrationCopy.palette(goal.goalType)
    val accent = Color(palette.primary)
    var selectedStatus by remember { mutableStateOf<CheckInStatus?>(null) }
    var note by remember { mutableStateOf("") }

    val options = when (goal.goalType) {
        GoalType.BUILD -> listOf(
            Triple("🌱", "Still hard", CheckInStatus.FELL_BEHIND),
            Triple("🌿", "Needs a nudge", CheckInStatus.ON_TRACK),
            Triple("✨", "Effortless", CheckInStatus.CRUSHED_IT),
        )
        GoalType.REACH -> listOf(
            Triple("🚀", "Ahead", CheckInStatus.CRUSHED_IT),
            Triple("🎯", "On pace", CheckInStatus.ON_TRACK),
            Triple("🌊", "Behind", CheckInStatus.FELL_BEHIND),
        )
        GoalType.QUIT -> listOf(
            Triple("🌤️", "Steady", CheckInStatus.CRUSHED_IT),
            Triple("🌊", "Mixed", CheckInStatus.ON_TRACK),
            Triple("⚡", "Urges hit", CheckInStatus.FELL_BEHIND),
        )
        GoalType.MAINTAIN -> listOf(
            Triple("🔥", "Floor held", CheckInStatus.CRUSHED_IT),
            Triple("🌤️", "Close", CheckInStatus.ON_TRACK),
            Triple("💨", "Slipped under", CheckInStatus.FELL_BEHIND),
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                goal.goalType.name,
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                letterSpacing = 1.4.sp,
                color = accent,
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                goal.title,
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                intro,
                fontFamily = dmSansFamily,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                modifier = Modifier.padding(horizontal = 4.dp),
            )
            Spacer(modifier = Modifier.height(32.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                options.forEach { (emoji, label, status) ->
                    val color = when (status) {
                        CheckInStatus.CRUSHED_IT -> Color(0xFF4CAF50)
                        CheckInStatus.ON_TRACK -> Color(0xFF2196F3)
                        CheckInStatus.FELL_BEHIND -> Color(0xFF8D6E63)
                    }
                    StatusCard(
                        emoji = emoji,
                        label = label,
                        color = color,
                        isSelected = selectedStatus == status,
                        modifier = Modifier.weight(1f),
                    ) { selectedStatus = status }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = {
                    Text(
                        when (goal.goalType) {
                            GoalType.QUIT -> "Where did the urge show up?"
                            GoalType.BUILD -> "Optional note…"
                            GoalType.REACH -> "What moved the number?"
                            GoalType.MAINTAIN -> "Anything drifting?"
                        },
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
            )
            Spacer(modifier = Modifier.height(32.dp))
            Button(
                onClick = { selectedStatus?.let { onSubmit(it, note) } },
                enabled = selectedStatus != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
            ) {
                Text("Save Check-In", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun StatusCard(
    emoji: String,
    label: String,
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isSelected) color.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            )
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(emoji, fontSize = 28.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            label,
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
        )
    }
}
