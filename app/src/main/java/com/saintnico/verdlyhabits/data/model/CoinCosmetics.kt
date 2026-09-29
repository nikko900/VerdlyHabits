package com.saintnico.verdlyhabits.data.model

/**
 * Coin sinks that are identity, not capability.
 * Completing habits and winning challenges stay free. These only change how you look.
 */
enum class CosmeticKind { TITLE_GLOW, CHECK_SKIN, PROFILE_FRAME, OATH_CHIP, GROVE_PLANT }

enum class CosmeticBadge { NONE, MOST_CHOSEN, RARE, FEATURED, DECOY }

data class CosmeticItem(
    val id: String,
    val kind: CosmeticKind,
    val name: String,
    val hint: String,
    val desire: String,
    val price: Int,
    val previewArgb: Long,
    val badge: CosmeticBadge = CosmeticBadge.NONE,
    val hero: Boolean = false,
    val listedInShop: Boolean = true,
)

fun coinsLabel(amount: Int): String = if (amount == 1) "1 coin" else "$amount coins"

object CoinCosmetics {
    const val PIN_COST = 40
    const val PIN_DAYS = 14L

    val GLOWS: List<CosmeticItem> = listOf(
        CosmeticItem(
            "glow_ember", CosmeticKind.TITLE_GLOW, "Ember Rim",
            "Warm gold edge on your title",
            "The first ring people notice.",
            80, 0xFFE08A2E,
        ),
        CosmeticItem(
            "glow_moss", CosmeticKind.TITLE_GLOW, "Moss Halo",
            "Living green around your name",
            "Looks like you never missed a morning.",
            140, 0xFF52B788, CosmeticBadge.MOST_CHOSEN,
        ),
        CosmeticItem(
            "glow_dew", CosmeticKind.TITLE_GLOW, "Dewlight",
            "Soft silver-green bloom",
            "Quiet light. Hard to ignore.",
            180, 0xFFB7E4C7,
        ),
        CosmeticItem(
            "glow_mist", CosmeticKind.TITLE_GLOW, "Mist Veil",
            "Pale haze. thinner than Aurora",
            "Pretty. Aurora is louder.",
            230, 0xFF9FA8DA, CosmeticBadge.DECOY,
        ),
        CosmeticItem(
            "glow_aurora", CosmeticKind.TITLE_GLOW, "Aurora",
            "Violet light people screenshot",
            "The ring they ask about.",
            240, 0xFFB388FF, CosmeticBadge.FEATURED,
        ),
        CosmeticItem(
            "glow_night", CosmeticKind.TITLE_GLOW, "Nightbloom",
            "Deep indigo with slow sparkle",
            "After dark, you still glow.",
            320, 0xFF5B4B8A, CosmeticBadge.RARE,
        ),
        CosmeticItem(
            "glow_goldleaf", CosmeticKind.TITLE_GLOW, "Goldleaf",
            "The loudest title in the room",
            "Wear this when you want to be seen.",
            400, 0xFFFFC857, CosmeticBadge.FEATURED, hero = true,
        ),
        CosmeticItem(
            "glow_crownfire", CosmeticKind.TITLE_GLOW, "Crownfire",
            "Dual gold-ember with living sparks",
            "The grove's rarest ring.",
            600, 0xFFFF6B35, CosmeticBadge.RARE, hero = true,
        ),
        CosmeticItem(
            "glow_moonveil", CosmeticKind.TITLE_GLOW, "Moonveil",
            "Silver-night ring. only in the grove tonight",
            "Weekend light. Monday it's gone from the shop.",
            360, 0xFFC5CAE9, CosmeticBadge.RARE, hero = true,
        ),
    )

    val SKINS: List<CosmeticItem> = listOf(
        CosmeticItem(
            "check_bloom", CosmeticKind.CHECK_SKIN, "Bloom Burst",
            "Petals on every check-off",
            "Finish feels like a garden waking.",
            60, 0xFF95D5B2,
        ),
        CosmeticItem(
            "check_rain", CosmeticKind.CHECK_SKIN, "Coin Rain",
            "Coins fall when you complete (visual only)",
            "Spectacle, not a payout.",
            150, 0xFFF3B23E, CosmeticBadge.MOST_CHOSEN,
        ),
        CosmeticItem(
            "check_vine", CosmeticKind.CHECK_SKIN, "Vine Lash",
            "Vines snap across the card",
            "The habit itself looks rooted.",
            200, 0xFF2D6A4F,
        ),
        CosmeticItem(
            "check_mythic", CosmeticKind.CHECK_SKIN, "Mythic Bloom",
            "Full-screen garden burst",
            "The check-off they remember.",
            280, 0xFFFFC857, CosmeticBadge.FEATURED, hero = true,
        ),
        CosmeticItem(
            "check_goldleaf", CosmeticKind.CHECK_SKIN, "Goldleaf Fall",
            "Gold leaves and a heavier pulse",
            "Every finish feels crowned.",
            450, 0xFFE8C547, CosmeticBadge.RARE,
        ),
    )

    val FRAMES: List<CosmeticItem> = listOf(
        CosmeticItem(
            "frame_willow", CosmeticKind.PROFILE_FRAME, "Willow Hoop",
            "Soft sage ring around your face",
            "A living frame, not a stroke.",
            90, 0xFF95D5B2,
        ),
        CosmeticItem(
            "frame_oak", CosmeticKind.PROFILE_FRAME, "Oak Ring",
            "Warm timber band on your avatar",
            "Looks planted. Looks permanent.",
            160, 0xFFB08968, CosmeticBadge.MOST_CHOSEN,
        ),
        CosmeticItem(
            "frame_aurora", CosmeticKind.PROFILE_FRAME, "Aurora Frame",
            "Violet double-ring around you",
            "Your face wears the same light as your title.",
            280, 0xFFB388FF, CosmeticBadge.FEATURED,
        ),
        CosmeticItem(
            "frame_goldleaf", CosmeticKind.PROFILE_FRAME, "Goldleaf Frame",
            "Thick gold bezel with leaf ticks",
            "The loudest portrait in the grove.",
            420, 0xFFFFC857, CosmeticBadge.RARE, hero = true,
        ),
        CosmeticItem(
            "frame_grove_lead", CosmeticKind.PROFILE_FRAME, "Grove Lead",
            "Living crown for the friend who showed up most",
            "Earned. Never sold.",
            0, 0xFF52B788, CosmeticBadge.RARE, listedInShop = false,
        ),
    )

    val OATH_CHIPS: List<CosmeticItem> = listOf(
        CosmeticItem(
            "oath_moss", CosmeticKind.OATH_CHIP, "Moss Oath",
            "Green live-oath chip on Home",
            "Your stake looks rooted.",
            70, 0xFF52B788,
        ),
        CosmeticItem(
            "oath_ember", CosmeticKind.OATH_CHIP, "Ember Oath",
            "Warm gold chip while an oath is live",
            "The banner they notice first.",
            120, 0xFFE08A2E, CosmeticBadge.MOST_CHOSEN,
        ),
        CosmeticItem(
            "oath_goldleaf", CosmeticKind.OATH_CHIP, "Goldleaf Oath",
            "Gold-edged oath on Home",
            "Staking spectacle, not a casino.",
            220, 0xFFFFC857, CosmeticBadge.FEATURED, hero = true,
        ),
    )

    val PLANTS: List<CosmeticItem> = listOf(
        CosmeticItem(
            "plant_fern", CosmeticKind.GROVE_PLANT, "Fern Drape",
            "Ferns at the edges of your card",
            "Your profile feels like a grove.",
            100, 0xFF74C69D,
        ),
        CosmeticItem(
            "plant_ivy", CosmeticKind.GROVE_PLANT, "Ivy Cascade",
            "Ivy climbing the identity card",
            "Looks lived-in. Looks earned.",
            180, 0xFF40916C, CosmeticBadge.MOST_CHOSEN,
        ),
        CosmeticItem(
            "plant_blossom", CosmeticKind.GROVE_PLANT, "Night Blossom",
            "Violet blooms behind your name",
            "Soft light. Hard to scroll past.",
            260, 0xFFB388FF, CosmeticBadge.FEATURED,
        ),
        CosmeticItem(
            "plant_goldleaf", CosmeticKind.GROVE_PLANT, "Goldleaf Canopy",
            "Gold leaves over your hero",
            "The card they screenshot.",
            380, 0xFFFFC857, CosmeticBadge.RARE, hero = true,
        ),
    )

    val ALL: List<CosmeticItem> = GLOWS + SKINS + FRAMES + OATH_CHIPS + PLANTS

    fun find(id: String?): CosmeticItem? =
        id?.takeIf { it.isNotBlank() }?.let { key -> ALL.find { it.id == key } }

    fun forKind(kind: CosmeticKind): List<CosmeticItem> = ALL.filter { it.kind == kind }

    fun shopItems(kind: CosmeticKind, nightDropLive: Boolean): List<CosmeticItem> =
        forKind(kind).filter { item ->
            item.listedInShop && (item.id != "glow_moonveil" || nightDropLive)
        }

    fun nextHint(kind: CosmeticKind, owned: Set<String>): String? {
        val ladder = forKind(kind).filter { it.listedInShop && it.price > 0 }.sortedBy { it.price }
        val ownedHere = ladder.filter { it.id in owned }
        val next = ladder.firstOrNull { it.id !in owned } ?: return null
        if (ownedHere.isEmpty()) return null
        return when (kind) {
            CosmeticKind.TITLE_GLOW -> "${next.name} is the next ring"
            CosmeticKind.CHECK_SKIN -> "${next.name} is the next finish"
            CosmeticKind.PROFILE_FRAME -> "${next.name} is the next frame"
            CosmeticKind.OATH_CHIP -> "${next.name} is the next oath look"
            CosmeticKind.GROVE_PLANT -> "${next.name} is the next planting"
        }
    }

    fun badgeLabel(badge: CosmeticBadge): String? = when (badge) {
        CosmeticBadge.NONE -> null
        CosmeticBadge.MOST_CHOSEN -> "Most chosen"
        CosmeticBadge.RARE -> "Rare in the grove"
        CosmeticBadge.FEATURED -> "Wear this"
        CosmeticBadge.DECOY -> null
    }
}
