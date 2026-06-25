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
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeeklyCheckInSheet(
    goal: GoalEntity,
    onDismiss: () -> Unit,
    onSubmit: (CheckInStatus, String) -> Unit
) {
    var selectedStatus by remember { mutableStateOf<CheckInStatus?>(null) }
    var note by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                goal.title,
                fontFamily = frauncesFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 24.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                "How did this week go?",
                fontFamily = dmSansFamily,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                StatusCard(
                    emoji = "🔥",
                    label = "Crushed It",
                    color = Color(0xFF4CAF50),
                    isSelected = selectedStatus == CheckInStatus.CRUSHED_IT,
                    modifier = Modifier.weight(1f)
                ) { selectedStatus = CheckInStatus.CRUSHED_IT }
                
                StatusCard(
                    emoji = "✓",
                    label = "On Track",
                    color = Color(0xFF2196F3),
                    isSelected = selectedStatus == CheckInStatus.ON_TRACK,
                    modifier = Modifier.weight(1f)
                ) { selectedStatus = CheckInStatus.ON_TRACK }
                
                StatusCard(
                    emoji = "→",
                    label = "Fell Behind",
                    color = Color(0xFFF44336),
                    isSelected = selectedStatus == CheckInStatus.FELL_BEHIND,
                    modifier = Modifier.weight(1f)
                ) { selectedStatus = CheckInStatus.FELL_BEHIND }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("Optional note...") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Button(
                onClick = { selectedStatus?.let { onSubmit(it, note) } },
                enabled = selectedStatus != null,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                contentPadding = PaddingValues(vertical = 16.dp)
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
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { onClick() }
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(emoji, fontSize = 32.sp)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            label,
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
        )
    }
}
