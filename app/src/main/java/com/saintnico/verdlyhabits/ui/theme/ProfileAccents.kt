package com.saintnico.verdlyhabits.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Premium profile card themes for hero preview, member cards, and edit profile.
 */
data class ProfileAccent(
    val key: String,
    val displayName: String,
    val base: Color,
    val highlight: Color,
    val ink: Color,
    val heroTop: Color,
    val heroMid: Color,
    val heroBottom: Color,
    val glow: Color,
)

object ProfileAccents {
    val Sage = accent(
        key = "sage",
        name = "Sage",
        base = 0xFF1D6B44,
        highlight = 0xFF95D5B2,
        ink = 0xFFE7F5EC,
        top = 0xFF0B1410,
        mid = 0xFF132A1F,
        bottom = 0xFF0A120E,
        glow = 0xFF40916C,
    )
    val Obsidian = accent(
        key = "obsidian",
        name = "Obsidian",
        base = 0xFF2A2A2A,
        highlight = 0xFFC7C7C7,
        ink = 0xFFF4F4F4,
        top = 0xFF0A0A0C,
        mid = 0xFF1A1A22,
        bottom = 0xFF050508,
        glow = 0xFF7B6CF6,
    )
    val Bronze = accent(
        key = "bronze",
        name = "Bronze",
        base = 0xFF8B5A2B,
        highlight = 0xFFE0B07E,
        ink = 0xFFF6E6D1,
        top = 0xFF1A1008,
        mid = 0xFF3D2814,
        bottom = 0xFF120A04,
        glow = 0xFFCD7F32,
    )
    val Gold = accent(
        key = "gold",
        name = "Gold",
        base = 0xFFB8860B,
        highlight = 0xFFFFE082,
        ink = 0xFFFFF8E1,
        top = 0xFF1A1406,
        mid = 0xFF3D2E08,
        bottom = 0xFF0F0B03,
        glow = 0xFFFFB300,
    )
    val Silver = accent(
        key = "silver",
        name = "Silver",
        base = 0xFF78909C,
        highlight = 0xFFECEFF1,
        ink = 0xFFF5F7FA,
        top = 0xFF101418,
        mid = 0xFF263238,
        bottom = 0xFF080A0C,
        glow = 0xFFB0BEC5,
    )
    val Rose = accent(
        key = "rose",
        name = "Rose",
        base = 0xFFC2185B,
        highlight = 0xFFF8BBD9,
        ink = 0xFFFFF0F5,
        top = 0xFF1A0810,
        mid = 0xFF3D1228,
        bottom = 0xFF0C0408,
        glow = 0xFFFF4081,
    )
    val Ivory = accent(
        key = "ivory",
        name = "Ivory",
        base = 0xFFB8A990,
        highlight = 0xFFFFF8F0,
        ink = 0xFFFAF4E8,
        top = 0xFF1C1A16,
        mid = 0xFF3A3530,
        bottom = 0xFF0E0D0B,
        glow = 0xFFE6DDC9,
    )
    val Sun = accent(
        key = "sun",
        name = "Sun",
        base = 0xFFF9A825,
        highlight = 0xFFFFF59D,
        ink = 0xFFFFFDE7,
        top = 0xFF1A1504,
        mid = 0xFF3D3208,
        bottom = 0xFF0F0C02,
        glow = 0xFFFFD54F,
    )
    val Indigo = accent(
        key = "indigo",
        name = "Indigo",
        base = 0xFF2A3779,
        highlight = 0xFF8C9CE6,
        ink = 0xFFE3E8FB,
        top = 0xFF080C1A,
        mid = 0xFF1A2248,
        bottom = 0xFF04060E,
        glow = 0xFF5C6BC0,
    )
    val Violet = accent(
        key = "violet",
        name = "Violet",
        base = 0xFF6A1B9A,
        highlight = 0xFFCE93D8,
        ink = 0xFFF3E5F5,
        top = 0xFF120818,
        mid = 0xFF2E1040,
        bottom = 0xFF08040C,
        glow = 0xFFAB47BC,
    )
    val Amethyst = accent(
        key = "amethyst",
        name = "Amethyst",
        base = 0xFF512DA8,
        highlight = 0xFFB39DDB,
        ink = 0xFFEDE7F6,
        top = 0xFF0E0818,
        mid = 0xFF281845,
        bottom = 0xFF06040A,
        glow = 0xFF7E57C2,
    )
    val Diamond = accent(
        key = "diamond",
        name = "Diamond",
        base = 0xFF4FC3F7,
        highlight = 0xFFE1F5FE,
        ink = 0xFFF0FAFF,
        top = 0xFF061018,
        mid = 0xFF0E2840,
        bottom = 0xFF030810,
        glow = 0xFF81D4FA,
    )
    val Ember = accent(
        key = "ember",
        name = "Ember",
        base = 0xFF8E2F2F,
        highlight = 0xFFE8A9A9,
        ink = 0xFFF8DCDC,
        top = 0xFF140606,
        mid = 0xFF3A1212,
        bottom = 0xFF080303,
        glow = 0xFFE57373,
    )
    val Midnight = accent(
        key = "midnight",
        name = "Midnight",
        base = 0xFF0D47A1,
        highlight = 0xFF90CAF9,
        ink = 0xFFE3F2FD,
        top = 0xFF040810,
        mid = 0xFF0A1E3A,
        bottom = 0xFF020408,
        glow = 0xFF42A5F5,
    )
    val Mythic = accent(
        key = "mythic",
        name = "Mythic",
        base = 0xFFE040FB,
        highlight = 0xFFFF80AB,
        ink = 0xFFFFF0F8,
        top = 0xFF120818,
        mid = 0xFF2A1038,
        bottom = 0xFF06040A,
        glow = 0xFFE040FB,
    )

    val all: List<ProfileAccent> = listOf(
        Sage,
        Obsidian,
        Bronze,
        Silver,
        Gold,
        Rose,
        Ivory,
        Sun,
        Indigo,
        Violet,
        Amethyst,
        Diamond,
        Ember,
        Midnight,
        Mythic,
    )

    fun byKey(key: String?): ProfileAccent =
        all.firstOrNull { it.key == key } ?: Sage

    private fun accent(
        key: String,
        name: String,
        base: Long,
        highlight: Long,
        ink: Long,
        top: Long,
        mid: Long,
        bottom: Long,
        glow: Long,
    ) = ProfileAccent(
        key = key,
        displayName = name,
        base = Color(base.toInt()),
        highlight = Color(highlight.toInt()),
        ink = Color(ink.toInt()),
        heroTop = Color(top.toInt()),
        heroMid = Color(mid.toInt()),
        heroBottom = Color(bottom.toInt()),
        glow = Color(glow.toInt()),
    )
}
