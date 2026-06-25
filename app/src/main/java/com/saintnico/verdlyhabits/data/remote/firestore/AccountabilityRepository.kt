package com.saintnico.verdlyhabits.data.remote.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class DuoStreakState(
    val pairId: String = "",
    val buddyUid: String = "",
    val buddyUsername: String = "",
    val buddyPhotoUrl: String? = null,
    val status: String = "",
    val invitedBy: String = "",
    val streakDays: Int = 0,
    val myDoneToday: Boolean = false,
    val buddyDoneToday: Boolean = false,
    val myProgress: String = "0/0",
    val buddyProgress: String = "0/0",
    val isIncomingInvite: Boolean = false,
)

class AccountabilityRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val dateFmt = DateTimeFormatter.ISO_LOCAL_DATE

    private val currentUid: String?
        get() = auth.currentUser?.uid

    companion object {
        fun pairId(uidA: String, uidB: String): String =
            listOf(uidA, uidB).sorted().joinToString("_")

        fun todayKey(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    private fun pairsRef() = firestore.collection("duoStreaks")

    fun observeActivePair(myUid: String): Flow<DuoStreakState?> = callbackFlow {
        if (myUid.isBlank()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        var registration: ListenerRegistration? = null
        registration = pairsRef()
            .whereArrayContains("members", myUid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(null)
                    return@addSnapshotListener
                }
                try {
                    val doc = snap?.documents?.firstOrNull()
                    if (doc == null) {
                        trySend(null)
                        return@addSnapshotListener
                    }
                    val members = (doc.get("members") as? List<*>)?.filterIsInstance<String>().orEmpty()
                    val buddyUid = members.firstOrNull { it != myUid }.orEmpty()
                    val profiles = doc.get("memberProfiles") as? Map<*, *>
                    val buddyProfile = profiles?.get(buddyUid) as? Map<*, *>
                    val buddyUsername = buddyProfile?.get("username") as? String
                        ?: doc.getString("buddyUsername").orEmpty()
                    val buddyPhotoUrl = buddyProfile?.get("photoUrl") as? String
                        ?: doc.getString("buddyPhotoUrl")
                    val progress = doc.get("memberProgress") as? Map<*, *>
                    val today = todayKey()
                    val myMap = progress?.get(myUid) as? Map<*, *>
                    val buddyMap = progress?.get(buddyUid) as? Map<*, *>
                    val myDate = myMap?.get("date") as? String
                    val buddyDate = buddyMap?.get("date") as? String
                    val myDone = myDate == today && myMap?.get("allDone") == true
                    val buddyDone = buddyDate == today && buddyMap?.get("allDone") == true
                    val myCompleted = (myMap?.get("completed") as? Number)?.toInt() ?: 0
                    val myTotal = (myMap?.get("total") as? Number)?.toInt() ?: 0
                    val buddyCompleted = (buddyMap?.get("completed") as? Number)?.toInt() ?: 0
                    val buddyTotal = (buddyMap?.get("total") as? Number)?.toInt() ?: 0

                    trySend(
                        DuoStreakState(
                            pairId = doc.id,
                            buddyUid = buddyUid,
                            buddyUsername = buddyUsername,
                            buddyPhotoUrl = buddyPhotoUrl,
                            status = doc.getString("status").orEmpty(),
                            invitedBy = doc.getString("invitedBy").orEmpty(),
                            streakDays = (doc.getLong("streakDays") ?: 0L).toInt(),
                            myDoneToday = myDone,
                            buddyDoneToday = buddyDone,
                            myProgress = "$myCompleted/$myTotal",
                            buddyProgress = "$buddyCompleted/$buddyTotal",
                            isIncomingInvite = doc.getString("status") == "pending"
                                && doc.getString("invitedBy") != myUid,
                        ),
                    )
                } catch (_: Exception) {
                    trySend(null)
                }
            }
        awaitClose { registration?.remove() }
    }

    suspend fun inviteBuddy(
        buddyUid: String,
        buddyUsername: String,
        buddyPhotoUrl: String?,
        myUsername: String,
        myPhotoUrl: String?,
    ): Result<Unit> {
        val me = currentUid ?: return Result.failure(IllegalStateException("Sign in first"))
        if (buddyUid == me) return Result.failure(IllegalStateException("Pick someone else"))
        return try {
            val existing = pairsRef().whereArrayContains("members", me).limit(1).get().await()
            if (existing.documents.isNotEmpty()) {
                return Result.failure(IllegalStateException("You already have an accountability buddy"))
            }
            val id = pairId(me, buddyUid)
            val ref = pairsRef().document(id)
            val snap = ref.get().await()
            if (snap.exists()) {
                return Result.failure(IllegalStateException("Invite already sent or active"))
            }
            ref.set(
                mapOf(
                    "members" to listOf(me, buddyUid).sorted(),
                    "invitedBy" to me,
                    "status" to "pending",
                    "streakDays" to 0,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "memberProgress" to emptyMap<String, Any>(),
                    "memberProfiles" to mapOf(
                        me to mapOf(
                            "username" to myUsername,
                            "photoUrl" to myPhotoUrl,
                        ),
                        buddyUid to mapOf(
                            "username" to buddyUsername,
                            "photoUrl" to buddyPhotoUrl,
                        ),
                    ),
                ),
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun acceptInvite(pairId: String): Result<Unit> {
        val me = currentUid ?: return Result.failure(IllegalStateException("Sign in first"))
        return try {
            val ref = pairsRef().document(pairId)
            val snap = ref.get().await()
            if (!snap.exists()) return Result.failure(IllegalStateException("Invite not found"))
            val members = (snap.get("members") as? List<*>)?.filterIsInstance<String>().orEmpty()
            if (!members.contains(me)) return Result.failure(IllegalStateException("Not your invite"))
            ref.update("status", "active").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun declineInvite(pairId: String): Result<Unit> {
        val me = currentUid ?: return Result.failure(IllegalStateException("Sign in first"))
        return try {
            val ref = pairsRef().document(pairId)
            val snap = ref.get().await()
            if (!snap.exists()) return Result.success(Unit)
            val invitedBy = snap.getString("invitedBy")
            if (invitedBy != me && !(snap.get("members") as? List<*>)?.contains(me).orFalse()) {
                return Result.failure(IllegalStateException("Cannot decline"))
            }
            ref.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncMyProgress(completed: Int, total: Int, allDone: Boolean) {
        val me = currentUid ?: return
        try {
            val snap = pairsRef().whereArrayContains("members", me).limit(1).get().await()
            val doc = snap.documents.firstOrNull() ?: return
            if (doc.getString("status") != "active") return
            val today = todayKey()
            val members = (doc.get("members") as? List<*>)?.filterIsInstance<String>().orEmpty()
            val buddyUid = members.firstOrNull { it != me }.orEmpty()
            val progressField = "memberProgress.$me"
            val updates = mutableMapOf<String, Any>(
                progressField to mapOf(
                    "date" to today,
                    "completed" to completed,
                    "total" to total,
                    "allDone" to allDone,
                ),
            )
            val buddyMap = (doc.get("memberProgress") as? Map<*, *>)?.get(buddyUid) as? Map<*, *>
            val buddyDate = buddyMap?.get("date") as? String
            val buddyAllDone = buddyDate == today && buddyMap?.get("allDone") == true
            val lastBoth = doc.getString("lastBothCompleteDate")
            if (allDone && buddyAllDone && lastBoth != today) {
                val newStreak = (doc.getLong("streakDays") ?: 0L).toInt() + 1
                updates["streakDays"] = newStreak
                updates["lastBothCompleteDate"] = today
            }
            doc.reference.update(updates).await()
        } catch (_: Exception) {
            // Firestore rules/network may be unavailable — never crash the app for duo sync.
        }
    }

    private fun Boolean?.orFalse() = this == true
}
