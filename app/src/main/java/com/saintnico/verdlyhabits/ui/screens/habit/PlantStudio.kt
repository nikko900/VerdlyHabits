package com.saintnico.verdlyhabits.ui.screens.habit

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.domain.HabitCategory
import com.saintnico.verdlyhabits.ui.components.AnimatedHabitIcon
import com.saintnico.verdlyhabits.ui.components.StreakFlame
import com.saintnico.verdlyhabits.ui.models.HabitIconRegistry
import com.saintnico.verdlyhabits.ui.models.PremiumIcon
import com.saintnico.verdlyhabits.ui.theme.ProfileAccent
import com.saintnico.verdlyhabits.ui.theme.ProfileAccents
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily

data class HabitTheme(
    val accent: ProfileAccent,
    val tintColor: Color,
)

object HabitThemes {
    val all: List<HabitTheme> = ProfileAccents.all.map { accent ->
        HabitTheme(accent = accent, tintColor = accent.glow)
    }

    fun nearest(color: Color): HabitTheme {
        var best = all.first()
        var bestDist = Float.MAX_VALUE
        for (theme in all) {
            val d = colorDistance(color, theme.tintColor)
            if (d < bestDist) {
                bestDist = d
                best = theme
            }
        }
        return best
    }

    private fun colorDistance(a: Color, b: Color): Float {
        val dr = a.red - b.red
        val dg = a.green - b.green
        val db = a.blue - b.blue
        return dr * dr + dg * dg + db * db
    }
}

object HabitCategoryResolver {
    fun fromIconGroup(group: String): HabitCategory = when (group) {
        "Wellness", "Fitness", "Nature", "Lifestyle" -> HabitCategory.HEALTH
        "Mind", "Productivity" -> HabitCategory.MIND
        "Creative" -> HabitCategory.CREATIVE
        "Social" -> HabitCategory.SOCIAL
        "Finance" -> HabitCategory.FINANCE
        else -> HabitCategory.OTHER
    }

    fun suggestFromTitle(title: String): List<PremiumIcon> {
        val lower = title.lowercase()
        val keywords = mapOf(
            listOf("run", "walk", "gym", "workout", "hike", "swim", "fitness") to "Fitness",
            listOf("read", "book", "study", "learn", "journal", "meditate", "mind") to "Mind",
            listOf("water", "drink", "hydrate", "sleep", "med", "shower", "spa") to "Wellness",
            listOf("code", "work", "plan", "task") to "Productivity",
            listOf("draw", "art", "music", "photo", "create") to "Creative",
            listOf("save", "money", "budget") to "Finance",
            listOf("friend", "social", "pet") to "Social",
            listOf("garden", "nature", "morning", "sun") to "Nature",
        )
        val icons = mutableListOf<PremiumIcon>()
        for ((keys, group) in keywords) {
            if (keys.any { lower.contains(it) }) {
                icons += HabitIconRegistry.allIcons.filter { it.group == group }.take(4)
            }
        }
        if (icons.isEmpty()) icons += HabitIconRegistry.allIcons.take(8)
        return icons.distinctBy { it.id }.take(8)
    }
}

@Composable
fun HabitStudioHeroPreview(
    title: String,
    titleHint: String,
    selectedIconId: String,
    theme: HabitTheme,
    modifier: Modifier = Modifier,
    plantedLabel: String? = null,
) {
    val icon = HabitIconRegistry.iconFor(selectedIconId)
    val habitColor = theme.tintColor
    val displayTitle = title.ifBlank { titleHint }
    val previewDesc = "Preview: $displayTitle, ${theme.accent.displayName} theme"

    Crossfade(targetState = selectedIconId + theme.accent.key, label = "hero_preview") {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(22.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            habitColor.copy(alpha = 0.14f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                        ),
                    ),
                )
                .border(1.dp, habitColor.copy(alpha = 0.24f), RoundedCornerShape(22.dp))
                .padding(18.dp)
                .semantics { contentDescription = previewDesc },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(habitColor.copy(0.22f), habitColor.copy(0.06f)),
                            ),
                        )
                        .border(1.dp, habitColor.copy(0.20f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    AnimatedHabitIcon(icon = icon, color = habitColor, size = 26.dp)
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        displayTitle,
                        fontFamily = frauncesFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StreakFlame(streak = 1, size = 14.dp)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "Day 1 · on your home screen",
                            fontFamily = dmSansFamily,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.58f),
                        )
                    }
                    plantedLabel?.let {
                        Text(
                            it,
                            fontFamily = dmSansFamily,
                            fontSize = 11.sp,
                            color = habitColor,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HabitThemePicker(
    themes: List<HabitTheme>,
    selected: HabitTheme,
    onSelect: (HabitTheme) -> Unit,
    hapticsEnabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Pick a vibe",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            for (theme in themes) {
                val isSelected = theme.accent.key == selected.accent.key
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1.08f else 1f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                    label = "theme_scale",
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(64.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .scale(scale)
                            .size(56.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(theme.accent.heroTop, theme.accent.heroMid, theme.accent.heroBottom),
                                ),
                            )
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) theme.tintColor else Color.White.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(18.dp),
                            )
                            .clickable {
                                if (hapticsEnabled) {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                                onSelect(theme)
                            }
                            .semantics { contentDescription = "${theme.accent.displayName} theme" },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isSelected) {
                            Icon(Icons.Rounded.Check, null, tint = theme.accent.highlight, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        theme.accent.displayName,
                        fontFamily = dmSansFamily,
                        fontSize = 11.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) theme.tintColor
                        else MaterialTheme.colorScheme.onBackground.copy(0.65f),
                    )
                }
            }
        }
    }
}

@Composable
fun HabitIconStudioPicker(
    title: String,
    selectedIconId: String,
    accent: Color,
    onSelect: (String) -> Unit,
    hapticsEnabled: Boolean = true,
) {
    val haptic = LocalHapticFeedback.current
    val suggested = remember(title) { HabitCategoryResolver.suggestFromTitle(title) }
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(
            "Choose an icon",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
        )
        if (suggested.isNotEmpty()) {
            Text(
                "Suggested",
                fontFamily = dmSansFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
            )
            IconRow(
                icons = suggested,
                selectedIconId = selectedIconId,
                accent = accent,
                onSelect = { id ->
                    if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelect(id)
                },
            )
        }
        for (group in HabitIconRegistry.groups) {
            Text(
                group,
                fontFamily = dmSansFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
            )
            IconRow(
                icons = HabitIconRegistry.allIcons.filter { it.group == group },
                selectedIconId = selectedIconId,
                accent = accent,
                onSelect = { id ->
                    if (hapticsEnabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSelect(id)
                },
            )
        }
    }
}

@Composable
private fun IconRow(
    icons: List<PremiumIcon>,
    selectedIconId: String,
    accent: Color,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        for (premiumIcon in icons) {
            val isSelected = selectedIconId == premiumIcon.id
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.06f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "icon_scale",
            )
            Box(
                modifier = Modifier
                    .scale(scale)
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        if (isSelected) accent.copy(0.18f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.45f),
                    )
                    .border(
                        if (isSelected) 2.dp else 1.dp,
                        if (isSelected) accent else Color.White.copy(alpha = 0.12f),
                        RoundedCornerShape(14.dp),
                    )
                    .clickable { onSelect(premiumIcon.id) }
                    .semantics { contentDescription = "${premiumIcon.label} icon" },
                contentAlignment = Alignment.Center,
            ) {
                AnimatedHabitIcon(
                    icon = premiumIcon.vector,
                    color = if (isSelected) accent else MaterialTheme.colorScheme.onBackground.copy(0.55f),
                    size = 22.dp,
                )
            }
        }
    }
}

@Composable
fun HabitCategoryAutoChip(
    selectedCategory: HabitCategory,
    iconGroup: String?,
    onSelectCategory: (HabitCategory) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column {
            Text(
                "Category",
                fontFamily = dmSansFamily,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(0.55f),
            )
            Text(
                selectedCategory.displayName,
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            iconGroup?.let {
                Text(
                    "From your $it icon",
                    fontFamily = dmSansFamily,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(0.45f),
                )
            }
        }
        Box {
            TextButton(onClick = { expanded = true }) {
                Text("Change", fontWeight = FontWeight.Bold)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                for (cat in HabitCategory.entries) {
                    DropdownMenuItem(
                        text = { Text(cat.displayName) },
                        onClick = {
                            onSelectCategory(cat)
                            expanded = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun PlantStepStudio(
    title: String,
    selectedIconId: String,
    selectedTheme: HabitTheme,
    selectedCategory: HabitCategory,
    plantedLabel: String?,
    onSelectIcon: (String) -> Unit,
    onSelectTheme: (HabitTheme) -> Unit,
    onSelectCategory: (HabitCategory) -> Unit,
    hapticsEnabled: Boolean = true,
) {
    val iconMeta = HabitIconRegistry.premiumIconForId(selectedIconId)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            "Make it feel like yours",
            fontFamily = frauncesFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 30.sp,
            lineHeight = 38.sp,
        )
        Text(
            "This is how it will greet you on home every day.",
            fontFamily = dmSansFamily,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
        )
        HabitStudioHeroPreview(
            title = title,
            titleHint = "Your habit",
            selectedIconId = selectedIconId,
            theme = selectedTheme,
            plantedLabel = plantedLabel,
        )
        HabitThemePicker(
            themes = HabitThemes.all,
            selected = selectedTheme,
            onSelect = onSelectTheme,
            hapticsEnabled = hapticsEnabled,
        )
        HabitIconStudioPicker(
            title = title,
            selectedIconId = selectedIconId,
            accent = selectedTheme.tintColor,
            onSelect = { id ->
                onSelectIcon(id)
                HabitIconRegistry.premiumIconForId(id)?.let { icon ->
                    onSelectCategory(HabitCategoryResolver.fromIconGroup(icon.group))
                }
            },
            hapticsEnabled = hapticsEnabled,
        )
        HabitCategoryAutoChip(
            selectedCategory = selectedCategory,
            iconGroup = iconMeta?.group,
            onSelectCategory = onSelectCategory,
        )
    }
}
