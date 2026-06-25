package com.saintnico.verdlyhabits.ui.screens.challenge

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.saintnico.verdlyhabits.data.model.ReactionWeights

data class ProofViewerArgs(
    val photoUrl: String,
    val username: String,
    val avatarUrl: String?,
    val streakDay: Int,
    val relativeTime: String,
    val proofKey: String = "",
    val challengeId: String = "",
    val reactionCounts: Map<String, Int> = emptyMap(),
    val onReact: (String) -> Unit = {}
)

@Composable
fun ProofViewerScreen(
    args: ProofViewerArgs,
    onDismiss: () -> Unit,
    hapticsEnabled: Boolean = true,
    onReport: (() -> Unit)? = null,
) {
    val haptic = LocalHapticFeedback.current
    var dragAccum by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.92f))
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onVerticalDrag = { _, dragAmount -> dragAccum += dragAmount },
                    onDragEnd = {
                        if (dragAccum > 80f) onDismiss()
                        dragAccum = 0f
                    }
                )
            }
    ) {
        AsyncImage(
            model = args.photoUrl,
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!args.avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = args.avatarUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF333333), CircleShape)
                    )
                } else {
                    Box(
                        Modifier
                            .size(32.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape)
                    )
                }
                Spacer(Modifier.size(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(args.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(
                        "Day ${args.streakDay} · ${args.relativeTime}",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp
                    )
                }
                if (onReport != null && args.challengeId.isNotBlank() && args.proofKey.isNotBlank()) {
                    Icon(
                        Icons.Rounded.Flag,
                        contentDescription = "Report proof",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable { onReport() },
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(vertical = 16.dp, horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val reactions = listOf(
                Triple(ReactionWeights.FIRE, Icons.Rounded.LocalFireDepartment, Color(0xFFFF9800)),
                Triple(ReactionWeights.HEART, Icons.Rounded.Favorite, Color(0xFFE91E63)),
                Triple(ReactionWeights.LIGHTNING, Icons.Rounded.ElectricBolt, Color(0xFFFFEB3B))
            )
            reactions.forEach { (key, image, tint) ->
                val count = args.reactionCounts[key] ?: 0
                ReactionOrb(
                    image = image,
                    tint = tint,
                    count = count,
                    points = ReactionWeights.pointsFor(key),
                    onPress = {
                        if (hapticsEnabled) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                        args.onReact(key)
                    }
                )
            }
        }
    }
}

@Composable
private fun ReactionOrb(
    image: ImageVector,
    tint: Color,
    count: Int,
    points: Int,
    onPress: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "reactScale"
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .scale(scale)
                .background(tint.copy(alpha = 0.18f), CircleShape)
                .clickable(interactionSource = interaction, indication = null, onClick = onPress),
            contentAlignment = Alignment.Center
        ) {
            Icon(image, null, tint = tint, modifier = Modifier.size(26.dp))
        }
        Text("+$points · $count", color = Color.White.copy(alpha = 0.8f), fontSize = 11.sp)
    }
}
