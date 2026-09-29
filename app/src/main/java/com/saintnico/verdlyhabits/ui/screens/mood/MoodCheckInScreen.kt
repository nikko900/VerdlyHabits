package com.saintnico.verdlyhabits.ui.screens.mood

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class MoodOption(val level: Int, val label: String, val icon: ImageVector, val color: Color)

val moodOptions = listOf(
    MoodOption(0, "Struggling", Icons.Filled.SentimentVeryDissatisfied, Color(0xFFEF5350)),
    MoodOption(1, "Low",        Icons.Filled.SentimentDissatisfied,     Color(0xFFFF8A65)),
    MoodOption(2, "Neutral",    Icons.Filled.SentimentNeutral,          Color(0xFFFFCA28)),
    MoodOption(3, "Good",       Icons.Filled.SentimentSatisfied,        Color(0xFF66BB6A)),
    MoodOption(4, "Thriving",   Icons.Filled.SentimentVerySatisfied,    Color(0xFF26C6DA)),
)

val moodTags = listOf(
    "Energised", "Tired", "Motivated", "Stressed",
    "Focused", "Distracted", "Grateful", "Anxious"
)

@Composable
fun MoodCheckInScreen(
    onLog: (mood: Int, tags: List<String>, note: String?) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedMood by remember { mutableStateOf<Int?>(null) }
    var selectedTags by remember { mutableStateOf(setOf<String>()) }
    var noteText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(32.dp))

        Text("How are you today?", fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground, textAlign = TextAlign.Center)
        Text("This helps us understand your patterns.", style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), textAlign = TextAlign.Center)

        Spacer(Modifier.height(36.dp))

        // Mood selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            moodOptions.forEach { option ->
                val isSelected = selectedMood == option.level
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.18f else 1f,
                    animationSpec = spring(Spring.DampingRatioMediumBouncy),
                    label = "mood_scale_${option.level}"
                )
                Column(
                    modifier = Modifier
                        .scale(scale)
                        .clickable { selectedMood = option.level },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) option.color.copy(alpha = 0.2f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .border(
                                width = if (isSelected) 2.dp else 0.dp,
                                color = if (isSelected) option.color else Color.Transparent,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(option.icon, null,
                            tint = if (isSelected) option.color else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                            modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(option.label, fontSize = 10.sp,
                        color = if (isSelected) option.color else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        // Tags
        AnimatedVisibility(visible = selectedMood != null) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("What's shaping today?", fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground, style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(12.dp))

                // Wrap layout for tags
                val rows = moodTags.chunked(4)
                rows.forEach { rowTags ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowTags.forEach { tag ->
                            val isSel = tag in selectedTags
                            FilterChip(
                                selected = isSel,
                                onClick = {
                                    selectedTags = if (isSel) selectedTags - tag else selectedTags + tag
                                },
                                label = { Text(tag, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    selectedLabelColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }

                Spacer(Modifier.height(20.dp))

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { if (it.length <= 140) noteText = it },
                    label = { Text("Add a note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    maxLines = 3,
                    supportingText = { Text("${noteText.length}/140", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)) }
                )

                Spacer(Modifier.height(28.dp))

                Button(
                    onClick = {
                        val mood = selectedMood ?: return@Button
                        onLog(mood, selectedTags.toList(), noteText.ifBlank { null })
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = selectedMood != null
                ) {
                    Text("Log Mood", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip for now", color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                }
            }
        }
    }
}
