package com.saintnico.verdlyhabits.notifications

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.SelfImprovement
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.ui.graphics.vector.ImageVector
import com.saintnico.verdlyhabits.data.local.goals.GoalType
import com.saintnico.verdlyhabits.ui.models.HabitIconRegistry

/** Maps habits, goals, and motivation pings to the circular shade avatar. */
object NotificationGlyphs {

    fun forHabit(iconName: String?, color: Long): Bitmap =
        NotificationAvatars.glyphCircleForColor(HabitIconRegistry.iconFor(iconName), color)

    fun forHabit(icon: ImageVector, color: Long): Bitmap =
        NotificationAvatars.glyphCircleForColor(icon, color)

    fun forGoalType(type: GoalType, colorHex: String?): Bitmap {
        val vector = vectorForGoal(type)
        val bg = runCatching { Color.parseColor(colorHex) }.getOrDefault(0xFF1B4332.toInt())
        return NotificationAvatars.glyphCircleForColor(vector, bg)
    }

    fun forMotivation(colorHex: String? = null): Bitmap {
        val bg = runCatching { Color.parseColor(colorHex) }.getOrDefault(0xFF2D6A4F.toInt())
        return NotificationAvatars.glyphCircleForColor(Icons.Filled.AutoAwesome, bg)
    }

    fun forWeeklyCheckIn(): Bitmap =
        NotificationAvatars.glyphCircle(Icons.Filled.Flag)

    fun forProfileNudge(): Bitmap =
        NotificationAvatars.glyphCircle(Icons.Filled.Person)

    fun forStreak(icon: ImageVector? = null, color: Long = 0xFFD4890AL): Bitmap =
        NotificationAvatars.glyphCircleForColor(
            icon ?: Icons.Rounded.LocalFireDepartment,
            color,
        )

    fun vectorForGoal(type: GoalType): ImageVector = when (type) {
        GoalType.BUILD -> Icons.Rounded.Eco
        GoalType.REACH -> Icons.Rounded.TrackChanges
        GoalType.QUIT -> Icons.Rounded.SelfImprovement
        GoalType.MAINTAIN -> Icons.Rounded.LocalFireDepartment
    }
}
