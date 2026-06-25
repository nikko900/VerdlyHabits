package com.saintnico.verdlyhabits.ui.screens.challenge

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ElectricBolt
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.model.ReactionWeights

@Composable
fun ChallengeArenaOnboardingDialog(
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "Welcome to the Arena",
                fontWeight = FontWeight.ExtraBold,
            )
        },
        text = {
            Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                Text(
                    "Post a daily proof photo. Friends react with Fire, Heart, or Lightning — each reaction adds weighted points to the leaderboard.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                )
                ReactionExplainRow(
                    icon = Icons.Rounded.LocalFireDepartment,
                    tint = Color(0xFFFF9800),
                    label = "Fire",
                    points = ReactionWeights.pointsFor(ReactionWeights.FIRE),
                )
                ReactionExplainRow(
                    icon = Icons.Rounded.Favorite,
                    tint = Color(0xFFE91E63),
                    label = "Heart",
                    points = ReactionWeights.pointsFor(ReactionWeights.HEART),
                )
                ReactionExplainRow(
                    icon = Icons.Rounded.ElectricBolt,
                    tint = Color(0xFFFFEB3B),
                    label = "Lightning",
                    points = ReactionWeights.pointsFor(ReactionWeights.LIGHTNING),
                )
                Text(
                    "You can react to 10 proofs per day per challenge. The creator picks Consistency Cup, Crowd Favourite, or Hybrid scoring when the arena starts.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    lineHeight = 18.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Enter the Arena", fontWeight = FontWeight.Bold)
            }
        },
    )
}

@Composable
private fun ReactionExplainRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    tint: Color,
    label: String,
    points: Int,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            "$label — +$points pts per reaction",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
