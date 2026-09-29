package com.saintnico.verdlyhabits.ui.screens.challenge

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.model.ReactionWeights
import com.saintnico.verdlyhabits.ui.components.reactions.ReactionStickers

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
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Post a daily proof photo. Friends react with Verdly stickers. Each reaction adds weighted points to the leaderboard.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                )
                ReactionExplainRow(
                    drawableRes = ReactionStickers.primaryPickerFor(ReactionWeights.AWESOME),
                    label = "Hype stickers",
                    points = ReactionWeights.pointsFor(ReactionWeights.AWESOME),
                )
                ReactionExplainRow(
                    drawableRes = ReactionStickers.primaryPickerFor(ReactionWeights.HEART),
                    label = "Love stickers",
                    points = ReactionWeights.pointsFor(ReactionWeights.HEART),
                )
                ReactionExplainRow(
                    drawableRes = ReactionStickers.primaryPickerFor(ReactionWeights.WOW),
                    label = "Wow stickers",
                    points = ReactionWeights.pointsFor(ReactionWeights.WOW),
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
    drawableRes: Int,
    label: String,
    points: Int,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Image(
            painter = painterResource(drawableRes),
            contentDescription = null,
            modifier = Modifier.size(28.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            "$label. +$points pts per reaction",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
