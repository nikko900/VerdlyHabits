package com.saintnico.verdlyhabits.data.remote.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.saintnico.verdlyhabits.engine.grove.GroveCalendar
import kotlinx.coroutines.tasks.await

data class OpenGroveEntry(
    val uid: String,
    val username: String,
    val photoUrl: String?,
    val weeklyPlantDays: Int,
    val currentStreakPublic: Int,
    val titleGlowId: String?,
    val profileFrameId: String?,
    val grovePlantId: String?,
    val pinnedTrophyLabel: String?,
    val pinnedTrophyUntil: Long,
    val membershipTier: String?,
    val rank: Int,
)

data class OpenGroveBoard(
    val weekKey: String,
    val standingCount: Int,
    val top: List<OpenGroveEntry>,
)

data class OpenGroveWeekRank(
    val weekKey: String,
    val rank: Int,
    val percentile: Int,
    val weeklyPlantDays: Int,
)

data class GroveGiftDoc(
    val id: String,
    val fromUid: String,
    val fromUsername: String,
    val toUid: String,
    val itemId: String?,
    val amount: Int,
    val claimed: Boolean,
)

class GroveSocialRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun uid(): String? = auth.currentUser?.uid

    suspend fun witness(targetUid: String): Result<Unit> {
        val me = uid() ?: return Result.failure(IllegalStateException("Sign in first"))
        if (targetUid.isBlank() || targetUid == me) {
            return Result.failure(IllegalStateException("Can't witness that"))
        }
        val day = GroveCalendar.todayKey()
        val id = "${day}_$me"
        return try {
            firestore.collection("users").document(targetUid)
                .collection("witnesses").document(id)
                .set(
                    mapOf(
                        "fromUid" to me,
                        "day" to day,
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun witnessCount(targetUid: String, day: String = GroveCalendar.todayKey()): Int {
        if (targetUid.isBlank()) return 0
        return try {
            firestore.collection("users").document(targetUid)
                .collection("witnesses")
                .whereEqualTo("day", day)
                .get()
                .await()
                .size()
        } catch (_: Exception) {
            0
        }
    }

    suspend fun sendGroveGift(
        toUid: String,
        fromUsername: String,
        itemId: String?,
        amount: Int,
        giftId: String,
    ): Result<Unit> {
        val me = uid() ?: return Result.failure(IllegalStateException("Sign in first"))
        if (toUid.isBlank() || toUid == me) {
            return Result.failure(IllegalStateException("Can't gift that person"))
        }
        return try {
            firestore.collection("users").document(toUid)
                .collection("groveGifts").document(giftId)
                .set(
                    mapOf(
                        "fromUid" to me,
                        "fromUsername" to fromUsername,
                        "toUid" to toUid,
                        "itemId" to (itemId ?: ""),
                        "amount" to amount,
                        "claimed" to false,
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun pendingGiftsForMe(): List<GroveGiftDoc> {
        val me = uid() ?: return emptyList()
        return try {
            firestore.collection("users").document(me)
                .collection("groveGifts")
                .whereEqualTo("claimed", false)
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    GroveGiftDoc(
                        id = doc.id,
                        fromUid = data["fromUid"] as? String ?: return@mapNotNull null,
                        fromUsername = data["fromUsername"] as? String ?: "",
                        toUid = me,
                        itemId = (data["itemId"] as? String)?.takeIf { it.isNotBlank() },
                        amount = (data["amount"] as? Number)?.toInt() ?: 0,
                        claimed = false,
                    )
                }
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun markGiftClaimed(giftId: String): Result<Unit> {
        val me = uid() ?: return Result.failure(IllegalStateException("Sign in first"))
        return try {
            firestore.collection("users").document(me)
                .collection("groveGifts").document(giftId)
                .update("claimed", true)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchOpenGroveBoard(weekKey: String = GroveCalendar.weekKey()): OpenGroveBoard {
        val snap = try {
            firestore.collection("openGrove").document(weekKey).get().await()
        } catch (_: Exception) {
            return OpenGroveBoard(weekKey, 0, emptyList())
        }
        if (!snap.exists()) {
            val current = try {
                firestore.collection("openGrove").document("current").get().await()
            } catch (_: Exception) {
                return OpenGroveBoard(weekKey, 0, emptyList())
            }
            return parseBoard(current.data, current.getString("weekKey") ?: weekKey)
        }
        return parseBoard(snap.data, weekKey)
    }

    @Suppress("UNCHECKED_CAST")
    private fun parseBoard(data: Map<String, Any>?, weekKey: String): OpenGroveBoard {
        if (data == null) return OpenGroveBoard(weekKey, 0, emptyList())
        val standing = (data["standingCount"] as? Number)?.toInt() ?: 0
        val raw = data["top"] as? List<*> ?: emptyList<Any>()
        val top = raw.mapIndexedNotNull { index, item ->
            val m = item as? Map<*, *> ?: return@mapIndexedNotNull null
            val uid = m["uid"] as? String ?: return@mapIndexedNotNull null
            OpenGroveEntry(
                uid = uid,
                username = (m["username"] as? String).orEmpty().ifBlank { "rival" },
                photoUrl = m["photoUrl"] as? String,
                weeklyPlantDays = (m["weeklyPlantDays"] as? Number)?.toInt() ?: 0,
                currentStreakPublic = (m["currentStreakPublic"] as? Number)?.toInt() ?: 0,
                titleGlowId = m["titleGlowId"] as? String,
                profileFrameId = m["profileFrameId"] as? String,
                grovePlantId = m["grovePlantId"] as? String,
                pinnedTrophyLabel = m["pinnedTrophyLabel"] as? String,
                pinnedTrophyUntil = (m["pinnedTrophyUntil"] as? Number)?.toLong() ?: 0L,
                membershipTier = m["membershipTier"] as? String,
                rank = (m["rank"] as? Number)?.toInt() ?: (index + 1),
            )
        }
        return OpenGroveBoard(
            weekKey = data["weekKey"] as? String ?: weekKey,
            standingCount = standing,
            top = top,
        )
    }

    suspend fun fetchMyOpenGroveHistory(limit: Int = 8): List<OpenGroveWeekRank> {
        val me = uid() ?: return emptyList()
        return try {
            firestore.collection("openGroveHistory").document(me)
                .collection("weeks")
                .get()
                .await()
                .documents
                .mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    OpenGroveWeekRank(
                        weekKey = data["weekKey"] as? String ?: doc.id,
                        rank = (data["rank"] as? Number)?.toInt() ?: 0,
                        percentile = (data["percentile"] as? Number)?.toInt() ?: 0,
                        weeklyPlantDays = (data["weeklyPlantDays"] as? Number)?.toInt() ?: 0,
                    )
                }
                .sortedByDescending { it.weekKey }
                .take(limit)
        } catch (_: Exception) {
            emptyList()
        }
    }
}
