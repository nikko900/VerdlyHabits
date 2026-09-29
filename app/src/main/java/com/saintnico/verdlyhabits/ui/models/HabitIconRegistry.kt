package com.saintnico.verdlyhabits.ui.models

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.saintnico.verdlyhabits.ui.theme.AccentGold
import com.saintnico.verdlyhabits.ui.theme.PrimaryGreen

data class PremiumIcon(
    val id: String,
    val vector: ImageVector,
    val label: String,
    val group: String,
)

/** Stable id ↔ icon mapping so habits persist and restore correctly on home. */
object HabitIconRegistry {

    val allIcons: List<PremiumIcon> = listOf(
        // Wellness
        PremiumIcon("water", Icons.Default.WaterDrop, "Hydrate", "Wellness"),
        PremiumIcon("meds", Icons.Default.Medication, "Medication", "Wellness"),
        PremiumIcon("sleep", Icons.Default.Bedtime, "Sleep", "Wellness"),
        PremiumIcon("heart", Icons.Default.MonitorHeart, "Health", "Wellness"),
        PremiumIcon("spa", Icons.Default.Spa, "Self care", "Wellness"),
        PremiumIcon("meditate", Icons.Default.SelfImprovement, "Meditate", "Wellness"),
        PremiumIcon("shower", Icons.Default.Shower, "Shower", "Wellness"),
        PremiumIcon("clean", Icons.Default.CleanHands, "Hygiene", "Wellness"),
        // Fitness
        PremiumIcon("run", Icons.Default.DirectionsRun, "Run", "Fitness"),
        PremiumIcon("walk", Icons.Default.DirectionsWalk, "Walk", "Fitness"),
        PremiumIcon("gym", Icons.Default.FitnessCenter, "Gym", "Fitness"),
        PremiumIcon("hike", Icons.Default.Hiking, "Hike", "Fitness"),
        PremiumIcon("pool", Icons.Default.Pool, "Swim", "Fitness"),
        // Mind
        PremiumIcon("book", Icons.Default.Book, "Read", "Mind"),
        PremiumIcon("journal", Icons.Default.EditNote, "Journal", "Mind"),
        PremiumIcon("psychology", Icons.Default.Psychology, "Mindset", "Mind"),
        PremiumIcon("school", Icons.Default.School, "Study", "Mind"),
        PremiumIcon("translate", Icons.Default.Translate, "Language", "Mind"),
        PremiumIcon("menu_book", Icons.Default.MenuBook, "Learn", "Mind"),
        // Lifestyle
        PremiumIcon("food", Icons.Default.Restaurant, "Eat well", "Lifestyle"),
        PremiumIcon("coffee", Icons.Default.Coffee, "Coffee", "Lifestyle"),
        PremiumIcon("kitchen", Icons.Default.Kitchen, "Cook", "Lifestyle"),
        PremiumIcon("home", Icons.Default.Home, "Home", "Lifestyle"),
        // Productivity
        PremiumIcon("code", Icons.Default.Code, "Code", "Productivity"),
        PremiumIcon("work", Icons.Default.Work, "Work", "Productivity"),
        PremiumIcon("calendar", Icons.Default.CalendarToday, "Plan", "Productivity"),
        PremiumIcon("check", Icons.Default.CheckCircle, "Tasks", "Productivity"),
        // Creative
        PremiumIcon("art", Icons.Default.Brush, "Create", "Creative"),
        PremiumIcon("music", Icons.Default.MusicNote, "Music", "Creative"),
        PremiumIcon("camera", Icons.Default.CameraAlt, "Photo", "Creative"),
        PremiumIcon("magic", Icons.Default.AutoAwesome, "Ritual", "Creative"),
        // Social & finance
        PremiumIcon("groups", Icons.Default.Groups, "Social", "Social"),
        PremiumIcon("volunteer", Icons.Default.VolunteerActivism, "Give back", "Social"),
        PremiumIcon("pets", Icons.Default.Pets, "Pet care", "Social"),
        PremiumIcon("savings", Icons.Default.Savings, "Save", "Finance"),
        PremiumIcon("money", Icons.Default.AttachMoney, "Budget", "Finance"),
        PremiumIcon("trend", Icons.Default.TrendingUp, "Growth", "Finance"),
        // Digital & nature
        PremiumIcon("phone_lock", Icons.Default.PhoneLocked, "Screen off", "Digital"),
        PremiumIcon("phone", Icons.Default.PhoneAndroid, "Phone", "Digital"),
        PremiumIcon("eco", Icons.Default.Eco, "Nature", "Nature"),
        PremiumIcon("flower", Icons.Default.LocalFlorist, "Garden", "Nature"),
        PremiumIcon("sun", Icons.Default.WbSunny, "Morning", "Nature"),
        PremiumIcon("night", Icons.Default.Nightlight, "Evening", "Nature"),
        PremiumIcon("game", Icons.Default.SportsEsports, "Play", "Digital"),
        PremiumIcon("star", Icons.Default.Star, "Star", "Other"),
    )

    val groups: List<String> = allIcons.map { it.group }.distinct()

    val themeColors: List<Pair<String, Color>> = listOf(
        "Mint" to PrimaryGreen,
        "Gold" to AccentGold,
        "Coral" to Color(0xFFFF6B6B),
        "Rose" to Color(0xFFE57373),
        "Sage" to Color(0xFF81C784),
        "Sky" to Color(0xFF64B5F6),
        "Ocean" to Color(0xFF4DB6AC),
        "Lavender" to Color(0xFF9575CD),
        "Peach" to Color(0xFFFFB74D),
        "Teal" to Color(0xFF2DD4BF),
        "Indigo" to Color(0xFF7B6CF6),
        "Amber" to Color(0xFFFFB300),
        "Plum" to Color(0xFFAB47BC),
        "Slate" to Color(0xFF78909C),
        "Forest" to Color(0xFF388E3C),
        "Sunset" to Color(0xFFFF7043),
    )

    fun iconFor(stored: String?): ImageVector {
        if (stored.isNullOrBlank()) return allIcons.first().vector
        allIcons.firstOrNull { it.id.equals(stored, ignoreCase = true) }?.let { return it.vector }
        allIcons.firstOrNull { it.vector.name == stored }?.let { return it.vector }
        allIcons.firstOrNull { it.label.equals(stored, ignoreCase = true) }?.let { return it.vector }
        val key = stored.substringAfterLast('.')
        allIcons.firstOrNull { it.vector.name?.contains(key, ignoreCase = true) == true }?.let { return it.vector }
        return allIcons.first().vector
    }

    fun stableId(icon: ImageVector): String {
        allIcons.firstOrNull { it.vector === icon || it.vector.name == icon.name }?.let { return it.id }
        return icon.name?.substringAfterLast('.')?.lowercase()?.replace(" ", "_") ?: allIcons.first().id
    }

    fun premiumIconFor(icon: ImageVector): PremiumIcon? =
        allIcons.firstOrNull { it.vector === icon || it.vector.name == icon.name }

    fun premiumIconForId(id: String): PremiumIcon? =
        allIcons.firstOrNull { it.id == id }
}

/** Backward-compatible alias used across the app. */
val premiumHabitIcons: List<PremiumIcon> = HabitIconRegistry.allIcons

fun Long.toHabitColor(): Color {
    val argb = when {
        this == 0L -> 0xFF52B788L
        this and 0xFF000000L == 0L -> this or 0xFF000000L
        else -> this
    }
    return Color(argb.toInt())
}
