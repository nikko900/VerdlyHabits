package com.saintnico.verdlyhabits.data.coins

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.saintnico.verdlyhabits.data.local.AppDataStore
import com.saintnico.verdlyhabits.data.local.AppDatabase
import com.saintnico.verdlyhabits.data.local.coins.ChallengeStakeEntity
import com.saintnico.verdlyhabits.data.local.coins.CoinTransactionEntity
import com.saintnico.verdlyhabits.data.local.coins.CoinWalletEntity
import com.saintnico.verdlyhabits.data.local.coins.GiftEventEntity
import com.saintnico.verdlyhabits.data.model.CoinEconomy
import com.saintnico.verdlyhabits.data.model.CoinTransactionType
import com.saintnico.verdlyhabits.data.model.StakeStatus
import com.saintnico.verdlyhabits.notifications.ChallengeNotificationDispatcher
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Offline-first coin wallet. Room is the instant read path; Firestore
 * (`users/{uid}/wallet/state`) is the durable source of truth reconciled on
 * connectivity. Every balance mutation runs inside a Room transaction and is
 * rejected if it would take the balance negative.
 *
 * There is intentionally no withdrawal or cash-out path anywhere in this class.
 */
class CoinWalletRepository private constructor(private val appContext: Context) {

    companion object {
        private const val TAG = "CoinWallet"

        @Volatile
        private var INSTANCE: CoinWalletRepository? = null

        fun get(context: Context): CoinWalletRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: CoinWalletRepository(context.applicationContext).also { INSTANCE = it }
            }

        fun resetInstance() {
            synchronized(this) {
                INSTANCE?.stopIncomingEventListener()
                INSTANCE = null
            }
        }
    }

    private val db = AppDatabase.getDatabase(appContext)
    private val dao = db.coinDao()
    private val firestore = FirebaseFirestore.getInstance()
    private val walletMutex = Mutex()
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var giftListener: ListenerRegistration? = null

    private val uid: String?
        get() = FirebaseAuth.getInstance().currentUser?.uid

    sealed class WalletResult {
        data object Success : WalletResult()
        /** Deterministic tx id was already applied — no balance change. */
        data object AlreadyApplied : WalletResult()
        data class Failure(val reason: String) : WalletResult()
    }

    // ── Reads ────────────────────────────────────────────────────────────────

    fun observeBalance(): Flow<Int> {
        val userId = uid ?: return flowOf(0)
        return dao.observeWallet(userId).map { it?.balance ?: 0 }
    }

    suspend fun currentBalance(): Int {
        val userId = uid ?: return 0
        return withContext(Dispatchers.IO) { dao.getWallet(userId)?.balance ?: 0 }
    }

    fun observeWallet(): Flow<CoinWalletEntity?> {
        val userId = uid ?: return flowOf(null)
        return dao.observeWallet(userId)
    }

    fun observeActiveStakeFor(challengeId: String): Flow<ChallengeStakeEntity?> {
        val userId = uid ?: return flowOf(null)
        return dao.observeActiveStakeFor(challengeId, userId)
    }

    fun observeActiveStakes(): Flow<List<ChallengeStakeEntity>> {
        val userId = uid ?: return flowOf(emptyList())
        return dao.observeActiveStakes(userId)
    }

    suspend fun convertedStakeCount(): Int {
        val userId = uid ?: return 0
        return withContext(Dispatchers.IO) { dao.convertedStakeCount(userId) }
    }

    /** One-shot read for stake resolution without collecting a Flow. */
    suspend fun getActiveStake(challengeId: String): ChallengeStakeEntity? {
        val userId = uid ?: return null
        return withContext(Dispatchers.IO) {
            dao.activeStakeFor(challengeId, userId)
        }
    }

    fun observeGiftsForChallenge(challengeId: String): Flow<List<GiftEventEntity>> =
        dao.observeGiftsForChallenge(challengeId)

    fun observeTransactions(): Flow<List<CoinTransactionEntity>> {
        val userId = uid ?: return flowOf(emptyList())
        return dao.observeTransactions(userId, limit = 50)
    }

    // ── Core mutation (single funnel, never negative) ────────────────────────

    /**
     * Applies a balance delta and records a transaction atomically.
     * [txId] defaults to a fresh UUID; pass a deterministic id (gift/event id)
     * when the operation may be replayed — the IGNORE insert makes it idempotent.
     */
    private suspend fun mutateWallet(
        delta: Int,
        type: CoinTransactionType,
        challengeId: String? = null,
        counterpartyUserId: String? = null,
        txId: String = UUID.randomUUID().toString(),
    ): WalletResult = walletMutex.withLock {
        val userId = uid ?: return WalletResult.Failure("Not signed in")
        withContext(Dispatchers.IO) {
            db.withTransaction {
            val wallet = dao.getWallet(userId) ?: CoinWalletEntity(
                userId = userId, balance = 0, lifetimeEarned = 0, lifetimeSpent = 0, lastUpdated = 0L,
            )
            val newBalance = wallet.balance + delta
            if (newBalance < 0) {
                return@withTransaction WalletResult.Failure("Not enough coins")
            }
            val inserted = dao.insertTransactionIfAbsent(
                CoinTransactionEntity(
                    id = txId,
                    userId = userId,
                    type = type.name,
                    amount = delta,
                    challengeId = challengeId,
                    counterpartyUserId = counterpartyUserId,
                    timestamp = System.currentTimeMillis(),
                )
            )
            if (inserted == -1L) {
                // Replayed event — already credited once; do nothing.
                return@withTransaction WalletResult.AlreadyApplied
            }
            dao.upsertWallet(
                wallet.copy(
                    balance = newBalance,
                    lifetimeEarned = wallet.lifetimeEarned + if (delta > 0) delta else 0,
                    lifetimeSpent = wallet.lifetimeSpent + if (delta < 0) -delta else 0,
                    lastUpdated = System.currentTimeMillis(),
                )
            )
            WalletResult.Success
        }
        }.also {
            if (it is WalletResult.Success) syncQuietly()
        }
    }

    // ── Purchases (called by BillingManager after RevenueCat confirms) ───────

    suspend fun creditPurchase(coins: Int, productId: String, transactionId: String) {
        val result = mutateWallet(
            delta = coins,
            type = CoinTransactionType.PURCHASE,
            txId = "purchase_$transactionId",
        )
        if (result is WalletResult.Failure) Log.e(TAG, "creditPurchase: ${result.reason}")
        else Log.i(TAG, "credited $coins coins ($productId)")
    }

    // ── Staking ──────────────────────────────────────────────────────────────

    /** Locks coins on a challenge. Deducts immediately; forfeits burn, success converts. */
    suspend fun stakeCoins(challengeId: String, amount: Int): WalletResult = withContext(Dispatchers.IO) {
        val userId = uid ?: return@withContext WalletResult.Failure("Not signed in")
        if (amount < CoinEconomy.MIN_STAKE) {
            return@withContext WalletResult.Failure("Minimum stake is ${CoinEconomy.MIN_STAKE} coins")
        }
        val wallet = dao.getWallet(userId)
        val maxStake = CoinEconomy.maxStakeFor(wallet?.balance ?: 0)
        if (amount > maxStake) {
            return@withContext WalletResult.Failure("Max stake right now is $maxStake coins")
        }
        if (dao.activeStakeFor(challengeId, userId) != null) {
            return@withContext WalletResult.Failure("You already have an active stake here")
        }
        val result = mutateWallet(
            delta = -amount,
            type = CoinTransactionType.STAKE,
            challengeId = challengeId,
        )
        if (result is WalletResult.Success) {
            dao.upsertStake(
                ChallengeStakeEntity(
                    id = UUID.randomUUID().toString(),
                    challengeId = challengeId,
                    userId = userId,
                    stakedAmount = amount,
                    status = StakeStatus.ACTIVE.name,
                    createdAt = System.currentTimeMillis(),
                    resolvedAt = null,
                )
            )
            syncQuietly()
        }
        result
    }

    /**
     * Streak broke before completion: stake is lost. A portion is redistributed
     * to other active participants as a consistency bonus (recognition, not cash).
     */
    suspend fun forfeitStake(
        challengeId: String,
        otherActiveMembers: List<String>,
        challengeName: String,
    ) {
        val userId = uid ?: return
        val stake = dao.activeStakeFor(challengeId, userId) ?: return
        dao.upsertStake(
            stake.copy(
                status = StakeStatus.FORFEITED.name,
                resolvedAt = System.currentTimeMillis(),
                synced = false,
            )
        )
        // Coins already left the balance at stake time — record the forfeit for history.
        dao.insertTransactionIfAbsent(
            CoinTransactionEntity(
                id = "forfeit_${stake.id}",
                userId = userId,
                type = CoinTransactionType.FORFEIT.name,
                amount = 0,
                challengeId = challengeId,
                counterpartyUserId = null,
                timestamp = System.currentTimeMillis(),
            )
        )
        redistributeForfeit(stake.stakedAmount, challengeId, otherActiveMembers, challengeName)
        syncQuietly()
    }

    private suspend fun redistributeForfeit(
        stakedAmount: Int,
        challengeId: String,
        recipients: List<String>,
        challengeName: String,
    ) {
        val others = recipients.filter { it != uid }
        if (others.isEmpty()) return
        val pool = (stakedAmount * CoinEconomy.FORFEIT_REDISTRIBUTION_FRACTION).toInt()
        val share = pool / others.size
        if (share <= 0) return
        runCatching {
            firestore.collection("challenges").document(challengeId)
                .collection("coinEvents")
                .add(
                    mapOf(
                        "type" to "CONSISTENCY_BONUS",
                        "senderId" to (uid ?: ""),
                        "recipients" to others,
                        "amountEach" to share,
                        "challengeName" to challengeName,
                        "timestamp" to System.currentTimeMillis(),
                    )
                ).await()
        }.onFailure { Log.w(TAG, "redistribute failed: ${it.message}") }
    }

    /**
     * Challenge finished with the stake intact: converts to trophy XP.
     * Coins are never returned as spendable balance — stake to prove, not to profit.
     */
    suspend fun convertStake(challengeId: String, isPremium: Boolean): Int {
        val userId = uid ?: return 0
        val stake = dao.activeStakeFor(challengeId, userId) ?: return 0
        dao.upsertStake(
            stake.copy(
                status = StakeStatus.CONVERTED.name,
                resolvedAt = System.currentTimeMillis(),
                synced = false,
            )
        )
        dao.insertTransactionIfAbsent(
            CoinTransactionEntity(
                id = "convert_${stake.id}",
                userId = userId,
                type = CoinTransactionType.REWARD_REDEMPTION.name,
                amount = 0,
                challengeId = challengeId,
                counterpartyUserId = null,
                timestamp = System.currentTimeMillis(),
            )
        )
        val perCoin = if (isPremium) CoinEconomy.XP_PER_COIN_PREMIUM else CoinEconomy.XP_PER_COIN_FREE
        val rawXp = stake.stakedAmount * perCoin
        val xp = if (isPremium) rawXp else rawXp.coerceAtMost(CoinEconomy.FREE_CONVERSION_XP_CAP)
        withContext(Dispatchers.IO) {
            runCatching { AppDataStore(appContext).addXp(xp) }
        }
        syncQuietly()
        return xp
    }

    // ── Gifting ──────────────────────────────────────────────────────────────

    suspend fun sendGift(
        challengeId: String,
        receiverId: String,
        receiverName: String,
        amount: Int,
        message: String?,
        challengeName: String,
        senderName: String,
    ): WalletResult {
        val userId = uid ?: return WalletResult.Failure("Not signed in")
        if (receiverId == userId) return WalletResult.Failure("You can't gift yourself")
        val trimmedMessage = message?.take(CoinEconomy.GIFT_MESSAGE_MAX_CHARS)?.ifBlank { null }
        val giftId = UUID.randomUUID().toString()

        val result = mutateWallet(
            delta = -amount,
            type = CoinTransactionType.GIFT_SENT,
            challengeId = challengeId,
            counterpartyUserId = receiverId,
            txId = "gift_sent_$giftId",
        )
        if (result is WalletResult.Failure) return result

        dao.insertGiftIfAbsent(
            GiftEventEntity(
                id = giftId,
                challengeId = challengeId,
                senderId = userId,
                receiverId = receiverId,
                amount = amount,
                message = trimmedMessage,
                timestamp = System.currentTimeMillis(),
            )
        )

        val pushed = runCatching {
            firestore.collection("challenges").document(challengeId)
                .collection("coinEvents").document(giftId)
                .set(
                    mapOf(
                        "type" to "GIFT",
                        "senderId" to userId,
                        "senderName" to senderName,
                        "receiverId" to receiverId,
                        "recipients" to listOf(receiverId),
                        "amountEach" to amount,
                        "message" to (trimmedMessage ?: ""),
                        "challengeName" to challengeName,
                        "timestamp" to System.currentTimeMillis(),
                    )
                ).await()
        }
        if (pushed.isFailure) {
            // Refund locally — the gift never reached the receiver.
            mutateWallet(
                delta = amount,
                type = CoinTransactionType.BONUS,
                challengeId = challengeId,
                txId = "gift_refund_$giftId",
            )
            return WalletResult.Failure("Couldn't send right now. coins returned")
        }

        runCatching {
            ChallengeNotificationDispatcher.notifyCoinGift(
                targetUid = receiverId,
                senderName = senderName,
                amount = amount,
                challengeId = challengeId,
                challengeTitle = challengeName,
                giftId = giftId,
            )
        }
        return WalletResult.Success
    }

    /**
     * Listens for gifts and consistency bonuses addressed to me across all
     * challenges. Crediting is idempotent — replays are ignored via event-id
     * transaction keys.
     */
    fun startIncomingEventListener(
        onGiftReceived: suspend (senderName: String, amount: Int, message: String?) -> Unit = { _, _, _ -> },
        onBonusReceived: suspend (amount: Int, challengeName: String) -> Unit = { _, _ -> },
        scope: kotlinx.coroutines.CoroutineScope,
    ) {
        val userId = uid ?: return
        giftListener?.remove()
        giftListener = firestore.collectionGroup("coinEvents")
            .whereArrayContains("recipients", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(60)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "coinEvents listener: ${error.message}")
                    return@addSnapshotListener
                }
                val docs = snapshot?.documents ?: return@addSnapshotListener
                scope.launch(Dispatchers.IO) {
                    docs.forEach { doc ->
                        val type = doc.getString("type") ?: return@forEach
                        val amount = (doc.getLong("amountEach") ?: 0L).toInt()
                        if (amount <= 0) return@forEach
                        val challengeId = doc.reference.parent.parent?.id ?: ""
                        when (type) {
                            "GIFT" -> {
                                val credited = mutateWallet(
                                    delta = amount,
                                    type = CoinTransactionType.GIFT_RECEIVED,
                                    challengeId = challengeId,
                                    counterpartyUserId = doc.getString("senderId"),
                                    txId = "gift_recv_${doc.id}",
                                )
                                if (credited is WalletResult.Success) {
                                    val newRow = dao.insertGiftIfAbsent(
                                        GiftEventEntity(
                                            id = doc.id,
                                            challengeId = challengeId,
                                            senderId = doc.getString("senderId") ?: "",
                                            receiverId = userId,
                                            amount = amount,
                                            message = doc.getString("message")?.ifBlank { null },
                                            timestamp = doc.getLong("timestamp") ?: 0L,
                                        )
                                    )
                                    if (newRow != -1L) {
                                        onGiftReceived(
                                            doc.getString("senderName") ?: "A rival",
                                            amount,
                                            doc.getString("message")?.ifBlank { null },
                                        )
                                    }
                                }
                            }
                            "CONSISTENCY_BONUS" -> {
                                val credited = mutateWallet(
                                    delta = amount,
                                    type = CoinTransactionType.BONUS,
                                    challengeId = challengeId,
                                    counterpartyUserId = doc.getString("senderId"),
                                    txId = "bonus_${doc.id}",
                                )
                                if (credited is WalletResult.Success) {
                                    onBonusReceived(amount, doc.getString("challengeName") ?: "a challenge")
                                }
                            }
                        }
                    }
                }
            }
    }

    fun stopIncomingEventListener() {
        giftListener?.remove()
        giftListener = null
    }

    /** Also mirrors sender-side gifts so "Most Supported" tallies locally. */
    suspend fun recordPeerGiftsFromEvents(challengeId: String) {
        runCatching {
            val docs = firestore.collection("challenges").document(challengeId)
                .collection("coinEvents")
                .whereEqualTo("type", "GIFT")
                .get().await()
            docs.documents.forEach { doc ->
                dao.insertGiftIfAbsent(
                    GiftEventEntity(
                        id = doc.id,
                        challengeId = challengeId,
                        senderId = doc.getString("senderId") ?: "",
                        receiverId = doc.getString("receiverId") ?: "",
                        amount = (doc.getLong("amountEach") ?: 0L).toInt(),
                        message = doc.getString("message")?.ifBlank { null },
                        timestamp = doc.getLong("timestamp") ?: 0L,
                    )
                )
            }
        }.onFailure { Log.w(TAG, "peer gift sync: ${it.message}") }
    }

    /**
     * One-time 20-coin taste so a brand-new wallet can lock the minimum stake.
     * Skipped if they've ever earned coins (purchase, drip, gift) so it cannot
     * be farmed and does not subsidize someone who already bought.
     */
    suspend fun grantFirstOathIfDue(): Boolean {
        val userId = uid ?: return false
        val wallet = withContext(Dispatchers.IO) { dao.getWallet(userId) }
        if ((wallet?.lifetimeEarned ?: 0) > 0 || (wallet?.balance ?: 0) > 0) return false
        val result = mutateWallet(
            delta = CoinEconomy.FIRST_OATH_GRANT,
            type = CoinTransactionType.BONUS,
            txId = "first_oath_$userId",
        )
        return result == WalletResult.Success
    }

    // ── Premium monthly drip ─────────────────────────────────────────────────

    suspend fun grantMonthlyDripIfDue(isPremium: Boolean): Boolean {
        if (!isPremium) return false
        val userId = uid ?: return false
        val monthKey = java.text.SimpleDateFormat("yyyy-MM", java.util.Locale.US)
            .format(java.util.Date())
        val result = mutateWallet(
            delta = CoinEconomy.PREMIUM_MONTHLY_DRIP,
            type = CoinTransactionType.BONUS,
            txId = "drip_${userId}_$monthKey",
        )
        return result == WalletResult.Success
    }

    /**
     * Spend on Grove identity. [txId] must be deterministic for one-time unlocks
     * (`cosmetic_{itemId}_{uid}`) so a replay cannot double-charge.
     */
    suspend fun spendCosmetic(amount: Int, txId: String): WalletResult {
        if (amount <= 0) return WalletResult.Failure("Invalid amount")
        return mutateWallet(
            delta = -amount,
            type = CoinTransactionType.COSMETIC,
            txId = txId,
        )
    }

    suspend fun creditGroveGift(amount: Int, giftId: String, fromUid: String): WalletResult {
        if (amount <= 0) return WalletResult.Failure("Invalid amount")
        return mutateWallet(
            delta = amount,
            type = CoinTransactionType.GIFT_RECEIVED,
            counterpartyUserId = fromUid,
            txId = "grove_gift_$giftId",
        )
    }

    // ── Firestore sync ───────────────────────────────────────────────────────

    private fun syncQuietly() {
        // Fire-and-forget push; reconciliation handles conflicts on next start.
        repoScope.launch {
            runCatching { pushToFirestore() }
                .onFailure { Log.w(TAG, "sync deferred: ${it.message}") }
        }
    }

    suspend fun pushToFirestore() {
        val userId = uid ?: return
        val wallet = dao.getWallet(userId) ?: return
        val stateRef = firestore.collection("users").document(userId)
            .collection("wallet").document("state")
        stateRef.set(
            mapOf(
                "balance" to wallet.balance,
                "lifetimeEarned" to wallet.lifetimeEarned,
                "lifetimeSpent" to wallet.lifetimeSpent,
                "lastUpdated" to wallet.lastUpdated,
            )
        ).await()

        val txs = dao.unsyncedTransactions(userId)
        if (txs.isNotEmpty()) {
            val batch = firestore.batch()
            txs.forEach { tx ->
                batch.set(
                    stateRef.collection("transactions").document(tx.id),
                    mapOf(
                        "type" to tx.type,
                        "amount" to tx.amount,
                        "challengeId" to (tx.challengeId ?: ""),
                        "counterpartyUserId" to (tx.counterpartyUserId ?: ""),
                        "timestamp" to tx.timestamp,
                    ),
                )
            }
            batch.commit().await()
            dao.markTransactionsSynced(txs.map { it.id })
        }

        val stakes = dao.unsyncedStakes(userId)
        if (stakes.isNotEmpty()) {
            val batch = firestore.batch()
            stakes.forEach { stake ->
                batch.set(
                    stateRef.collection("stakes").document(stake.id),
                    mapOf(
                        "challengeId" to stake.challengeId,
                        "stakedAmount" to stake.stakedAmount,
                        "status" to stake.status,
                        "createdAt" to stake.createdAt,
                        "resolvedAt" to (stake.resolvedAt ?: 0L),
                    ),
                )
            }
            batch.commit().await()
            dao.markStakesSynced(stakes.map { it.id })
        }
    }

    /** Pull remote wallet; newest lastUpdated wins. Call on sign-in / app start. */
    suspend fun reconcileFromFirestore() {
        val userId = uid ?: return
        runCatching {
            val remote = firestore.collection("users").document(userId)
                .collection("wallet").document("state").get().await()
            if (!remote.exists()) {
                pushToFirestore()
                return
            }
            val remoteUpdated = remote.getLong("lastUpdated") ?: 0L
            val local = dao.getWallet(userId)
            if (local == null || remoteUpdated > local.lastUpdated) {
                dao.upsertWallet(
                    CoinWalletEntity(
                        userId = userId,
                        balance = (remote.getLong("balance") ?: 0L).toInt(),
                        lifetimeEarned = (remote.getLong("lifetimeEarned") ?: 0L).toInt(),
                        lifetimeSpent = (remote.getLong("lifetimeSpent") ?: 0L).toInt(),
                        lastUpdated = remoteUpdated,
                    )
                )
            } else {
                pushToFirestore()
            }
        }.onFailure { Log.w(TAG, "reconcile: ${it.message}") }
    }
}
