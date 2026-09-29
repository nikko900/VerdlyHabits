package com.saintnico.verdlyhabits.data.local.coins

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "coin_wallet")
data class CoinWalletEntity(
    @PrimaryKey val userId: String,
    val balance: Int,
    val lifetimeEarned: Int,
    val lifetimeSpent: Int,
    val lastUpdated: Long,
)

@Entity(
    tableName = "coin_transactions",
    indices = [Index("userId"), Index("challengeId")],
)
data class CoinTransactionEntity(
    @PrimaryKey val id: String,
    val userId: String,
    /** [com.saintnico.verdlyhabits.data.model.CoinTransactionType] name. */
    val type: String,
    val amount: Int,
    val challengeId: String?,
    val counterpartyUserId: String?,
    val timestamp: Long,
    val synced: Boolean = false,
)

@Entity(
    tableName = "challenge_stakes",
    indices = [Index("challengeId"), Index("userId")],
)
data class ChallengeStakeEntity(
    @PrimaryKey val id: String,
    val challengeId: String,
    val userId: String,
    val stakedAmount: Int,
    /** [com.saintnico.verdlyhabits.data.model.StakeStatus] name. */
    val status: String,
    val createdAt: Long,
    val resolvedAt: Long?,
    val synced: Boolean = false,
)

@Entity(
    tableName = "gift_events",
    indices = [Index("challengeId"), Index("receiverId")],
)
data class GiftEventEntity(
    @PrimaryKey val id: String,
    val challengeId: String,
    val senderId: String,
    val receiverId: String,
    val amount: Int,
    val message: String?,
    val timestamp: Long,
    val synced: Boolean = false,
)
