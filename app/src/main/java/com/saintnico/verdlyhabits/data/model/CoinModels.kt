package com.saintnico.verdlyhabits.data.model

/**
 * Coin economy domain types.
 *
 * HARD CONSTRAINT: coins can never be converted back to money. There is
 * deliberately no WITHDRAWAL / CASH_OUT / PAYOUT type here — do not add one.
 * Forfeited stakes are burned or redistributed as in-app recognition only.
 */
enum class CoinTransactionType {
    PURCHASE,
    STAKE,
    FORFEIT,
    GIFT_SENT,
    GIFT_RECEIVED,
    REWARD_REDEMPTION,
    /** Consistency bonus from a rival's forfeit, or the premium monthly drip. */
    BONUS,
    /** Grove cosmetics — title glow, check-off skin, proclaim pin. Never cash. */
    COSMETIC,
}

enum class StakeStatus { ACTIVE, FORFEITED, CONVERTED }

data class CoinWallet(
    val userId: String = "",
    val balance: Int = 0,
    val lifetimeEarned: Int = 0,
    val lifetimeSpent: Int = 0,
    val lastUpdated: Long = 0L,
)

data class CoinTransaction(
    val id: String = "",
    val userId: String = "",
    val type: CoinTransactionType = CoinTransactionType.PURCHASE,
    val amount: Int = 0,
    val challengeId: String? = null,
    val counterpartyUserId: String? = null,
    val timestamp: Long = 0L,
)

data class ChallengeStake(
    val id: String = "",
    val challengeId: String = "",
    val userId: String = "",
    val stakedAmount: Int = 0,
    val status: StakeStatus = StakeStatus.ACTIVE,
    val createdAt: Long = 0L,
    val resolvedAt: Long? = null,
)

data class GiftEvent(
    val id: String = "",
    val challengeId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val amount: Int = 0,
    val message: String? = null,
    val timestamp: Long = 0L,
)

object CoinEconomy {
    const val MIN_STAKE = 10

    /** Max fraction of the wallet a user may lock on a single stake. */
    const val MAX_STAKE_FRACTION = 0.5f

    /**
     * One-time taste so a brand-new wallet can actually lock [MIN_STAKE]
     * under the 50% cap. 10 coins cannot stake (max = 5). 20 can (max = 10).
     * After they swear the minimum they have 10 left — not enough to restake —
     * which is the first paid pack moment, not the convert weeks later.
     */
    const val FIRST_OATH_GRANT = 20

    /** Portion of a forfeited stake redistributed to other active participants. */
    const val FORFEIT_REDISTRIBUTION_FRACTION = 0.5f

    val GIFT_QUICK_PICKS = listOf(5, 10, 25, 50)
    const val GIFT_MESSAGE_MAX_CHARS = 40

    /** Premium retention perk: coins dripped once per calendar month. */
    const val PREMIUM_MONTHLY_DRIP = 50

    /** Trophy XP granted per staked coin on successful conversion. */
    const val XP_PER_COIN_FREE = 1
    const val XP_PER_COIN_PREMIUM = 2

    /** Free-tier cap on XP earned from a single converted stake. */
    const val FREE_CONVERSION_XP_CAP = 500

    fun maxStakeFor(balance: Int): Int =
        (balance * MAX_STAKE_FRACTION).toInt().coerceAtLeast(0)

    /** Smallest balance that can lock [MIN_STAKE] given [MAX_STAKE_FRACTION]. */
    fun minBalanceToStake(): Int =
        kotlin.math.ceil(MIN_STAKE / MAX_STAKE_FRACTION.toDouble()).toInt()

    fun canStake(balance: Int): Boolean = maxStakeFor(balance) >= MIN_STAKE

    fun plantXp(amount: Int, isPremium: Boolean): Int {
        val perCoin = if (isPremium) XP_PER_COIN_PREMIUM else XP_PER_COIN_FREE
        val raw = amount * perCoin
        return if (isPremium) raw else raw.coerceAtMost(FREE_CONVERSION_XP_CAP)
    }

    fun groupBonus(amount: Int): Int =
        (amount * FORFEIT_REDISTRIBUTION_FRACTION).toInt()

    data class PackOffer(
        val productId: String,
        val coins: Int,
        val name: String,
        val framing: String,
        val badge: String? = null,
    )

    val PACK_OFFERS: List<PackOffer> = listOf(
        PackOffer("verdly_coins_100", 100, "First Oath", "Two serious oaths, or Ember Rim"),
        PackOffer("verdly_coins_500", 500, "Serious Oath", "A real challenge. plus Goldleaf if you want it", "Best first buy"),
        PackOffer("verdly_coins_1200", 1200, "Grove Oath", "The one people watch in this room", "Most chosen"),
        PackOffer("verdly_coins_3000", 3000, "Deep Roots", "Many oaths, every glow, every skin", "Best value"),
    )

    /** Smallest pack whose coins cover [shortfall]. Null if nothing in the catalog is enough. */
    fun suggestPackFor(shortfall: Int): PackOffer? {
        if (shortfall <= 0) return PACK_OFFERS.firstOrNull()
        return PACK_OFFERS.sortedBy { it.coins }.firstOrNull { it.coins >= shortfall }
            ?: PACK_OFFERS.maxByOrNull { it.coins }
    }
}

/** Spoken language for the oath loop. Keep casino words out. */
object CoinOathCopy {
    const val SWEAR = "Swear it"
    const val PACK_HEADER = "Choose your oath"
    const val CONVERT_SUB = "It won't come back as coins. it's yours as proof, now."
    const val FORFEIT_HEAD = "The oath fed the group."
    const val SWEAR_AGAIN = "Swear again"
    const val EMPTY_AFTER_CONVERT = "Your last oath is proven. Ready for the next one?"
    const val EMPTY_AFTER_LOCK = "Your oath is live. The next one takes coins."
    const val FIRST_OATH_NOTICE = "First oath granted. 20 coins to swear with."
    const val WALLET_SUB = "Swear on a challenge. Finish and it becomes trophy progress."

    fun liveSafe(amount: Int, daysLeft: Long): String = "$amount planted · $daysLeft days left"
    fun liveAtRisk(amount: Int): String = "$amount at risk. check in today"
    fun convertHeadline(xp: Int): String = "Planted. $xp XP is yours."
    fun forfeitSub(bonus: Int): String =
        "$bonus went to keep others going. Yours are spent, honorably."

    fun lockLine(amount: Int, isPremium: Boolean): String {
        val planted = CoinEconomy.plantXp(amount, isPremium)
        return if (isPremium) {
            "Lock $amount → finish and plant $planted XP"
        } else {
            val premiumXp = CoinEconomy.plantXp(amount, isPremium = true)
            "Lock $amount → finish and plant $planted XP ($premiumXp with Premium)"
        }
    }

    fun missLine(amount: Int): String {
        val bonus = CoinEconomy.groupBonus(amount)
        return "Miss once → $bonus goes to the group as a consistency bonus. Yours are gone."
    }

    const val NO_RETURN =
        "This does not return to your wallet. It becomes trophy progress."

    fun nextTitleLine(convertsDone: Int): String? = when {
        convertsDone < 1 -> "1 convert unlocks Oathkeeper"
        convertsDone < 3 -> "${3 - convertsDone} more convert${if (3 - convertsDone == 1) "" else "s"} unlocks Rooted"
        convertsDone < 5 -> "${5 - convertsDone} more convert${if (3 - convertsDone == 1) "" else "s"} unlocks Proven"
        convertsDone < 10 -> "${10 - convertsDone} more convert${if (10 - convertsDone == 1) "" else "s"} unlocks Vowkeeper"
        else -> null
    }
}
