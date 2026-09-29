package com.saintnico.verdlyhabits.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.saintnico.verdlyhabits.data.coins.CoinWalletRepository
import com.saintnico.verdlyhabits.data.local.coins.ChallengeStakeEntity
import com.saintnico.verdlyhabits.data.local.coins.GiftEventEntity
import com.saintnico.verdlyhabits.data.model.Challenge
import com.saintnico.verdlyhabits.data.model.CoinCosmetics
import com.saintnico.verdlyhabits.data.model.CoinEconomy
import com.saintnico.verdlyhabits.data.model.CoinOathCopy
import com.saintnico.verdlyhabits.data.model.CosmeticKind
import com.saintnico.verdlyhabits.data.remote.firestore.UserRepository
import com.saintnico.verdlyhabits.engine.TrophyBus
import com.saintnico.verdlyhabits.engine.TrophyEvent
import com.saintnico.verdlyhabits.preferences.ThemePreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.TimeUnit

/** One-shot notices for the premium notification bar. */
data class CoinNotice(val message: String, val isPositive: Boolean = true)

enum class CoinPackReason {
    BROWSE,
    GROVE,
    AFTER_LOCK,
    AFTER_CONVERT,
    AFTER_FORFEIT,
    CANT_AFFORD,
    NEED_ITEM,
}

data class CoinPackNeed(
    val itemName: String,
    val itemPrice: Int,
    val shortfall: Int,
    val itemId: String? = null,
)

sealed class StakeCeremony {
    data class Converted(val amount: Int, val xp: Int, val habitName: String) : StakeCeremony()
    data class Forfeited(val amount: Int, val groupBonus: Int, val habitName: String) : StakeCeremony()
}

class CoinViewModel(application: Application) : AndroidViewModel(application) {

    private val appContext = application.applicationContext
    private val repo: CoinWalletRepository
        get() = CoinWalletRepository.get(appContext)
    private val themePref = ThemePreference(appContext)
    private val userRepository = UserRepository()

    val balance: StateFlow<Int> = repo.observeBalance().stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = 0,
    )

    val ownedCosmeticIds: StateFlow<Set<String>> = themePref.ownedCosmeticsCsv.map { csv ->
        csv.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val equippedTitleGlow: StateFlow<String?> = themePref.equippedTitleGlow.stateIn(
        viewModelScope, SharingStarted.Eagerly, null,
    )
    val equippedCheckSkin: StateFlow<String?> = themePref.equippedCheckSkin.stateIn(
        viewModelScope, SharingStarted.Eagerly, null,
    )
    val equippedProfileFrame: StateFlow<String?> = themePref.equippedProfileFrame.stateIn(
        viewModelScope, SharingStarted.Eagerly, null,
    )
    val equippedOathChip: StateFlow<String?> = themePref.equippedOathChip.stateIn(
        viewModelScope, SharingStarted.Eagerly, null,
    )
    val equippedGrovePlant: StateFlow<String?> = themePref.equippedGrovePlant.stateIn(
        viewModelScope, SharingStarted.Eagerly, null,
    )
    val pinnedTrophyLabel: StateFlow<String?> = themePref.pinnedTrophyLabel.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), null,
    )
    val pinnedTrophyUntil: StateFlow<Long> = themePref.pinnedTrophyUntil.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L,
    )
    val pinnedTrophyId: StateFlow<String?> = themePref.pinnedTrophyId.stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000), null,
    )

    val activeStakes: StateFlow<List<ChallengeStakeEntity>> = repo.observeActiveStakes().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList(),
    )

    private val _notice = MutableStateFlow<CoinNotice?>(null)
    val notice: StateFlow<CoinNotice?> = _notice.asStateFlow()

    /** Set true briefly after a successful coin pack purchase to run the burst. */
    private val _purchaseCelebration = MutableStateFlow(false)
    val purchaseCelebration: StateFlow<Boolean> = _purchaseCelebration.asStateFlow()

    private val _packReason = MutableStateFlow<CoinPackReason?>(null)
    val packReason: StateFlow<CoinPackReason?> = _packReason.asStateFlow()

    private val _packNeed = MutableStateFlow<CoinPackNeed?>(null)
    val packNeed: StateFlow<CoinPackNeed?> = _packNeed.asStateFlow()

    private val _pendingCosmeticId = MutableStateFlow<String?>(null)
    val pendingCosmeticId: StateFlow<String?> = _pendingCosmeticId.asStateFlow()

    private val _ceremony = MutableStateFlow<StakeCeremony?>(null)
    val ceremony: StateFlow<StakeCeremony?> = _ceremony.asStateFlow()

    /** Shown right after a lock that leaves the wallet too small to swear again. */
    private val _afterLockBuy = MutableStateFlow(false)
    val afterLockBuy: StateFlow<Boolean> = _afterLockBuy.asStateFlow()

    private val _convertedCount = MutableStateFlow(0)
    val convertedCount: StateFlow<Int> = _convertedCount.asStateFlow()

    init {
        reconcileFromCloud()
        viewModelScope.launch(Dispatchers.IO) {
            refreshConvertedCount()
            publishEquippedLooks()
        }
        repo.startIncomingEventListener(
            onGiftReceived = { senderName, amount, message ->
                val suffix = message?.let { ". “$it”" } ?: ""
                _notice.value = CoinNotice("$senderName sent you $amount coins to keep going.$suffix")
            },
            onBonusReceived = { amount, challengeName ->
                _notice.value = CoinNotice("A rival's stake boosted the group. +$amount coins from $challengeName.")
            },
            scope = viewModelScope,
        )
    }

    fun consumeNotice() {
        _notice.value = null
    }

    fun openCoinPacks(reason: CoinPackReason = CoinPackReason.BROWSE, need: CoinPackNeed? = null) {
        _afterLockBuy.value = false
        _packNeed.value = need
        _pendingCosmeticId.value = need?.itemId
        _packReason.value = reason
    }

    fun closeCoinPacks() {
        _packReason.value = null
        _packNeed.value = null
    }

    fun consumePendingCosmetic() {
        _pendingCosmeticId.value = null
        _packNeed.value = null
    }

    fun openNeedCoins(itemName: String, itemPrice: Int, itemId: String? = null) {
        val shortfall = (itemPrice - balance.value).coerceAtLeast(1)
        openCoinPacks(
            CoinPackReason.NEED_ITEM,
            CoinPackNeed(itemName = itemName, itemPrice = itemPrice, shortfall = shortfall, itemId = itemId),
        )
    }

    fun consumeCeremony() {
        _ceremony.value = null
    }

    fun consumeAfterLockBuy() {
        _afterLockBuy.value = false
    }

    /** Pull wallet from Firestore — call on sign-in and account switch. */
    fun reconcileFromCloud() {
        viewModelScope.launch(Dispatchers.IO) {
            repo.reconcileFromFirestore()
            refreshConvertedCount()
        }
    }

    fun celebratePurchase() {
        _purchaseCelebration.value = true
    }

    fun endCelebration() {
        _purchaseCelebration.value = false
    }

    fun observeStakeFor(challengeId: String): Flow<ChallengeStakeEntity?> =
        repo.observeActiveStakeFor(challengeId)

    fun observeGiftsFor(challengeId: String): Flow<List<GiftEventEntity>> =
        repo.observeGiftsForChallenge(challengeId)

    fun refreshGiftsFor(challengeId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repo.recordPeerGiftsFromEvents(challengeId)
        }
    }

    fun stake(challengeId: String, amount: Int, onResult: (ok: Boolean, message: String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            when (val result = repo.stakeCoins(challengeId, amount)) {
                is CoinWalletRepository.WalletResult.Success -> {
                    TrophyBus.emit(TrophyEvent.OathLocked(amount))
                    onResult(true, "Sworn. $amount coins are planted on this challenge.")
                    val remaining = repo.currentBalance()
                    if (!CoinEconomy.canStake(remaining)) {
                        _afterLockBuy.value = true
                    }
                }
                is CoinWalletRepository.WalletResult.AlreadyApplied ->
                    onResult(true, "Already sworn on this challenge.")
                is CoinWalletRepository.WalletResult.Failure ->
                    onResult(false, result.reason)
            }
        }
    }

    fun sendGift(
        challenge: Challenge,
        receiverId: String,
        amount: Int,
        message: String?,
        senderName: String,
        onResult: (ok: Boolean, message: String) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val receiverName = challenge.memberNames[receiverId] ?: "Rival"
            when (
                val result = repo.sendGift(
                    challengeId = challenge.id,
                    receiverId = receiverId,
                    receiverName = receiverName,
                    amount = amount,
                    message = message,
                    challengeName = challenge.habitName,
                    senderName = senderName,
                )
            ) {
                is CoinWalletRepository.WalletResult.Success -> {
                    TrophyBus.emit(TrophyEvent.CoinsGifted(amount))
                    onResult(true, "Sent $amount coins to $receiverName.")
                }
                is CoinWalletRepository.WalletResult.AlreadyApplied ->
                    onResult(true, "Gift already sent.")
                is CoinWalletRepository.WalletResult.Failure ->
                    onResult(false, result.reason)
            }
        }
    }

    /**
     * Resolves my active stakes against current challenge state:
     * a miss forfeits immediately; a clean finished challenge converts to trophy XP.
     * Idempotent — resolved stakes disappear from the active query.
     */
    fun resolveStakes(challenges: List<Challenge>, isPremium: Boolean) {
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch(Dispatchers.IO) {
            challenges.forEach { challenge ->
                val stake = repo.getActiveStake(challenge.id) ?: return@forEach
                val missed = (challenge.missedDays[myUid] ?: 0) > 0
                val finished = !challenge.isActive || challenge.isArchived
                when {
                    missed -> {
                        repo.forfeitStake(
                            challengeId = challenge.id,
                            otherActiveMembers = challenge.members.filter { it != myUid },
                            challengeName = challenge.habitName,
                        )
                        TrophyBus.emit(TrophyEvent.OathForfeited(stake.stakedAmount))
                        _ceremony.value = StakeCeremony.Forfeited(
                            amount = stake.stakedAmount,
                            groupBonus = CoinEconomy.groupBonus(stake.stakedAmount),
                            habitName = challenge.habitName,
                        )
                    }
                    finished -> {
                        val xp = repo.convertStake(challenge.id, isPremium)
                        if (xp > 0) {
                            TrophyBus.emit(TrophyEvent.OathConverted(stake.stakedAmount))
                            refreshConvertedCount()
                            _ceremony.value = StakeCeremony.Converted(
                                amount = stake.stakedAmount,
                                xp = xp,
                                habitName = challenge.habitName,
                            )
                        }
                    }
                }
            }
        }
    }

    fun grantMonthlyDripIfDue(isPremium: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            if (repo.grantMonthlyDripIfDue(isPremium)) {
                _notice.value = CoinNotice("Premium perk: +50 coins for the month.")
            }
        }
    }

    /** Once per account, only on a truly empty wallet that has never earned. */
    fun grantFirstOathIfDue() {
        viewModelScope.launch(Dispatchers.IO) {
            if (repo.grantFirstOathIfDue()) {
                _notice.value = CoinNotice(CoinOathCopy.FIRST_OATH_NOTICE)
            }
        }
    }

    private suspend fun refreshConvertedCount() {
        _convertedCount.value = repo.convertedStakeCount()
    }

    fun buyCosmetic(itemId: String, onResult: (ok: Boolean, message: String) -> Unit) {
        val item = CoinCosmetics.find(itemId) ?: return onResult(false, "Unknown item")
        viewModelScope.launch(Dispatchers.IO) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid.isNullOrBlank()) {
                withContext(Dispatchers.Main) { onResult(false, "Not signed in") }
                return@launch
            }
            if (itemId in ownedCosmeticIds.value) {
                equipCosmetic(itemId)
                withContext(Dispatchers.Main) { onResult(true, "${item.name} is on you.") }
                return@launch
            }
            when (val result = repo.spendCosmetic(item.price, txId = "cosmetic_${itemId}_$uid")) {
                is CoinWalletRepository.WalletResult.Success,
                is CoinWalletRepository.WalletResult.AlreadyApplied -> {
                    val next = ownedCosmeticIds.value + itemId
                    themePref.setOwnedCosmeticsCsv(next.joinToString(","))
                    persistEquip(item.kind, itemId)
                    syncCosmetics(
                        ownedCsv = next.joinToString(","),
                        glow = if (item.kind == CosmeticKind.TITLE_GLOW) itemId else equippedTitleGlow.value,
                        skin = if (item.kind == CosmeticKind.CHECK_SKIN) itemId else equippedCheckSkin.value,
                        frame = if (item.kind == CosmeticKind.PROFILE_FRAME) itemId else equippedProfileFrame.value,
                        oath = if (item.kind == CosmeticKind.OATH_CHIP) itemId else equippedOathChip.value,
                        plant = if (item.kind == CosmeticKind.GROVE_PLANT) itemId else equippedGrovePlant.value,
                        pinId = pinnedTrophyId.value,
                        pinLabel = pinnedTrophyLabel.value,
                        pinUntil = pinnedTrophyUntil.value,
                    )
                    TrophyBus.emit(TrophyEvent.CosmeticOwned)
                    withContext(Dispatchers.Main) { onResult(true, "${item.name} is yours. It's on you.") }
                }
                is CoinWalletRepository.WalletResult.Failure ->
                    withContext(Dispatchers.Main) { onResult(false, result.reason) }
            }
        }
    }

    fun equipCosmetic(itemId: String) {
        val item = CoinCosmetics.find(itemId) ?: return
        if (itemId !in ownedCosmeticIds.value) return
        viewModelScope.launch(Dispatchers.IO) {
            persistEquip(item.kind, itemId)
            syncCosmetics(
                ownedCsv = ownedCosmeticIds.value.joinToString(","),
                glow = if (item.kind == CosmeticKind.TITLE_GLOW) itemId else equippedTitleGlow.value,
                skin = if (item.kind == CosmeticKind.CHECK_SKIN) itemId else equippedCheckSkin.value,
                frame = if (item.kind == CosmeticKind.PROFILE_FRAME) itemId else equippedProfileFrame.value,
                oath = if (item.kind == CosmeticKind.OATH_CHIP) itemId else equippedOathChip.value,
                plant = if (item.kind == CosmeticKind.GROVE_PLANT) itemId else equippedGrovePlant.value,
                pinId = pinnedTrophyId.value,
                pinLabel = pinnedTrophyLabel.value,
                pinUntil = pinnedTrophyUntil.value,
            )
        }
    }

    fun unequipCosmetic(itemId: String) {
        val item = CoinCosmetics.find(itemId) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            persistEquip(item.kind, null)
            syncCosmetics(
                ownedCsv = ownedCosmeticIds.value.joinToString(","),
                glow = if (item.kind == CosmeticKind.TITLE_GLOW) null else equippedTitleGlow.value,
                skin = if (item.kind == CosmeticKind.CHECK_SKIN) null else equippedCheckSkin.value,
                frame = if (item.kind == CosmeticKind.PROFILE_FRAME) null else equippedProfileFrame.value,
                oath = if (item.kind == CosmeticKind.OATH_CHIP) null else equippedOathChip.value,
                plant = if (item.kind == CosmeticKind.GROVE_PLANT) null else equippedGrovePlant.value,
                pinId = pinnedTrophyId.value,
                pinLabel = pinnedTrophyLabel.value,
                pinUntil = pinnedTrophyUntil.value,
            )
        }
    }

    private suspend fun persistEquip(kind: CosmeticKind, itemId: String?) {
        when (kind) {
            CosmeticKind.TITLE_GLOW -> themePref.setEquippedTitleGlow(itemId)
            CosmeticKind.CHECK_SKIN -> themePref.setEquippedCheckSkin(itemId)
            CosmeticKind.PROFILE_FRAME -> themePref.setEquippedProfileFrame(itemId)
            CosmeticKind.OATH_CHIP -> themePref.setEquippedOathChip(itemId)
            CosmeticKind.GROVE_PLANT -> themePref.setEquippedGrovePlant(itemId)
        }
    }

    fun pinTrophy(trophyId: String, label: String, onResult: (ok: Boolean, message: String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val until = System.currentTimeMillis() + TimeUnit.DAYS.toMillis(CoinCosmetics.PIN_DAYS)
            when (
                val result = repo.spendCosmetic(
                    CoinCosmetics.PIN_COST,
                    txId = "pin_${UUID.randomUUID()}",
                )
            ) {
                is CoinWalletRepository.WalletResult.Success -> {
                    themePref.setPinnedTrophy(trophyId, label, until)
                    syncCosmetics(
                        ownedCsv = ownedCosmeticIds.value.joinToString(","),
                        glow = equippedTitleGlow.value,
                        skin = equippedCheckSkin.value,
                        frame = equippedProfileFrame.value,
                        oath = equippedOathChip.value,
                        plant = equippedGrovePlant.value,
                        pinId = trophyId,
                        pinLabel = label,
                        pinUntil = until,
                    )
                    withContext(Dispatchers.Main) {
                        onResult(true, "Proclaiming “$label” for ${CoinCosmetics.PIN_DAYS} days.")
                    }
                }
                is CoinWalletRepository.WalletResult.AlreadyApplied ->
                    withContext(Dispatchers.Main) { onResult(true, "Already proclaimed.") }
                is CoinWalletRepository.WalletResult.Failure ->
                    withContext(Dispatchers.Main) { onResult(false, result.reason) }
            }
        }
    }

    fun livePinnedLabel(): String? {
        val until = pinnedTrophyUntil.value
        val label = pinnedTrophyLabel.value
        return if (until > System.currentTimeMillis() && !label.isNullOrBlank()) label else null
    }

    fun grantEarnedCosmetic(itemId: String) {
        val item = CoinCosmetics.find(itemId) ?: return
        viewModelScope.launch(Dispatchers.IO) {
            if (itemId in ownedCosmeticIds.value) return@launch
            val next = ownedCosmeticIds.value + itemId
            themePref.setOwnedCosmeticsCsv(next.joinToString(","))
            persistEquip(item.kind, itemId)
            syncCosmetics(
                ownedCsv = next.joinToString(","),
                glow = if (item.kind == CosmeticKind.TITLE_GLOW) itemId else equippedTitleGlow.value,
                skin = if (item.kind == CosmeticKind.CHECK_SKIN) itemId else equippedCheckSkin.value,
                frame = if (item.kind == CosmeticKind.PROFILE_FRAME) itemId else equippedProfileFrame.value,
                oath = if (item.kind == CosmeticKind.OATH_CHIP) itemId else equippedOathChip.value,
                plant = if (item.kind == CosmeticKind.GROVE_PLANT) itemId else equippedGrovePlant.value,
                pinId = pinnedTrophyId.value,
                pinLabel = pinnedTrophyLabel.value,
                pinUntil = pinnedTrophyUntil.value,
            )
        }
    }

    fun giftToFriend(
        toUid: String,
        fromUsername: String,
        itemId: String?,
        coinAmount: Int,
        onResult: (ok: Boolean, message: String) -> Unit,
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val item = itemId?.let { CoinCosmetics.find(it) }
            val spend = when {
                item != null -> item.price
                coinAmount > 0 -> coinAmount
                else -> {
                    withContext(Dispatchers.Main) { onResult(false, "Pick something to plant.") }
                    return@launch
                }
            }
            val giftId = UUID.randomUUID().toString()
            when (val result = repo.spendCosmetic(spend, txId = "grove_gift_sent_$giftId")) {
                is CoinWalletRepository.WalletResult.Failure -> {
                    withContext(Dispatchers.Main) {
                        onResult(false, result.reason)
                    }
                    return@launch
                }
                else -> Unit
            }
            val pushed = com.saintnico.verdlyhabits.data.remote.firestore.GroveSocialRepository()
                .sendGroveGift(toUid, fromUsername, item?.id, spend, giftId)
            if (pushed.isFailure) {
                repo.creditGroveGift(spend, "refund_$giftId", toUid)
                withContext(Dispatchers.Main) {
                    onResult(false, "Couldn't send. Coins returned.")
                }
                return@launch
            }
            TrophyBus.emit(TrophyEvent.CoinsGifted(spend))
            withContext(Dispatchers.Main) {
                val label = item?.name ?: "$spend coins"
                onResult(true, "Planted $label with them.")
            }
        }
    }

    fun claimIncomingGifts() {
        viewModelScope.launch(Dispatchers.IO) {
            val social = com.saintnico.verdlyhabits.data.remote.firestore.GroveSocialRepository()
            val gifts = social.pendingGiftsForMe()
            gifts.forEach { gift ->
                if (gift.amount > 0 && gift.itemId.isNullOrBlank()) {
                    repo.creditGroveGift(gift.amount, gift.id, gift.fromUid)
                }
                gift.itemId?.let { id ->
                    if (id !in ownedCosmeticIds.value) {
                        val next = ownedCosmeticIds.value + id
                        themePref.setOwnedCosmeticsCsv(next.joinToString(","))
                        CoinCosmetics.find(id)?.let { persistEquip(it.kind, id) }
                        syncCosmetics(
                            ownedCsv = next.joinToString(","),
                            glow = equippedTitleGlow.value,
                            skin = equippedCheckSkin.value,
                            frame = equippedProfileFrame.value,
                            oath = equippedOathChip.value,
                            plant = equippedGrovePlant.value,
                            pinId = pinnedTrophyId.value,
                            pinLabel = pinnedTrophyLabel.value,
                            pinUntil = pinnedTrophyUntil.value,
                        )
                    }
                }
                social.markGiftClaimed(gift.id)
            }
        }
    }

    /** Push local equipped looks so friends see glow / frame / plant, not the generic card. */
    private suspend fun publishEquippedLooks() {
        val ownedCsv = themePref.ownedCosmeticsCsv.first()
        val glow = themePref.equippedTitleGlow.first()
        val skin = themePref.equippedCheckSkin.first()
        val frame = themePref.equippedProfileFrame.first()
        val oath = themePref.equippedOathChip.first()
        val plant = themePref.equippedGrovePlant.first()
        val pinId = themePref.pinnedTrophyId.first()
        val pinLabel = themePref.pinnedTrophyLabel.first()
        val pinUntil = themePref.pinnedTrophyUntil.first()
        if (ownedCsv.isBlank() && glow == null && skin == null && frame == null &&
            oath == null && plant == null && pinId == null
        ) {
            return
        }
        syncCosmetics(
            ownedCsv = ownedCsv,
            glow = glow,
            skin = skin,
            frame = frame,
            oath = oath,
            plant = plant,
            pinId = pinId,
            pinLabel = pinLabel,
            pinUntil = pinUntil,
        )
    }

    private suspend fun syncCosmetics(
        ownedCsv: String,
        glow: String?,
        skin: String?,
        frame: String?,
        oath: String?,
        plant: String?,
        pinId: String?,
        pinLabel: String?,
        pinUntil: Long,
    ) {
        runCatching {
            userRepository.syncIdentityCosmetics(
                ownedCsv = ownedCsv,
                titleGlowId = glow,
                checkSkinId = skin,
                profileFrameId = frame,
                oathChipId = oath,
                grovePlantId = plant,
                pinnedTrophyId = pinId,
                pinnedTrophyLabel = pinLabel,
                pinnedTrophyUntil = pinUntil,
            )
        }
    }

    override fun onCleared() {
        repo.stopIncomingEventListener()
        super.onCleared()
    }
}
