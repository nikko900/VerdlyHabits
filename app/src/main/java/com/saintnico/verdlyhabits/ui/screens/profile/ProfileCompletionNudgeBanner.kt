package com.saintnico.verdlyhabits.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.engine.ProfileCompletionEngine
import com.saintnico.verdlyhabits.ui.components.profilepremium.ProfileStreakArc
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily

@Composable
fun ProfileCompletionNudgeBanner(
    displayName: String,
    username: String,
    photoUrl: String?,
    motto: String,
    bio: String,
    favoritePlant: String,
    onCompleteProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!ProfileCompletionEngine.shouldShowNudge(
            displayName, username, photoUrl, motto, bio, favoritePlant,
        )
    ) {
        return
    }

    val fraction = ProfileCompletionEngine.completionFraction(
        displayName, username, photoUrl, motto, bio, favoritePlant,
    )
    val remaining = ProfileCompletionEngine.remainingLabels(
        displayName, username, photoUrl, motto, bio, favoritePlant,
    )
    if (remaining.isEmpty()) return

    val pct = (fraction * 100).toInt()
    val remainingLine = when (remaining.size) {
        1 -> "Still need: ${remaining.first()}"
        2 -> "Still need: ${remaining[0]} and ${remaining[1]}"
        else -> "Still need: ${remaining.dropLast(1).joinToString(", ")} and ${remaining.last()}"
    }

    Card(
        onClick = onCompleteProfile,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.14f),
        ),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProfileStreakArc(
                progress = fraction,
                accent = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "Profile $pct% complete",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                Text(
                    remainingLine,
                    fontFamily = dmSansFamily,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "Tap to finish your profile",
                    fontFamily = dmSansFamily,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.secondary,
                )
            }
            Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.secondary)
            Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
