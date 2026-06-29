package com.saintnico.verdlyhabits.data.remote.firestore

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.saintnico.verdlyhabits.engine.DuoStreakEngine
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
    val streakAtRisk: Boolean = false,
    val bothDoneToday: Boolean = false,
    val countdownHours: Int = 0,
    val countdownMinutes: Int = 0,
    val graceAvailable: Boolean = false,
    val graceUsedThisWeek: Boolean = false,
)

/** Emitted when shared streak increments to a celebration milestone. */
data class DuoMilestoneReached(val streakDays: Int, val buddyUsername: String, val pairId: String)

/** Emitted when the cooperative streak resets after a missed day. */
data class DuoStreakBroken(val previousStreak: Int, val buddyUsername: String, val pairId: String)

class AccountabilityRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "AccountabilityRepo"

        fun pairId(uidA: String, uidB: String): String =
            listOf(uidA, uidB).sorted().joinToString("_")

        fun todayKey(): String = DuoStreakEngine.todayKey()
    }

    private val currentUid: String?
        get() = auth.currentUser?.uid

    private fun pairsRef() = firestore.collection("duoStreaks")

    private suspend fun com.google.firebase.firestore.DocumentReference.getPairIfReadable():
        com.google.firebase.firestore.DocumentSnapshot? =
        try {
            get().await()
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Log.w(TAG, "getPairIfReadable: PERMISSION_DENIED on $id")
                null
            } else {
                throw e
            }
        }

    fun observeActivePair(myUid: String, isPremium: Boolean = false): Flow<DuoStreakState?> = callbackFlow {
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
                    Log.e(TAG, "observeActivePair failed for $myUid", err)
                    trySend(null)
                    return@addSnapshotListener
                }
                try {
                    val doc = snap?.documents?.firstOrNull()
                    if (doc == null) {
                        trySend(null)
                        return@addSnapshotListener
                    }
                    trySend(parseDuoState(doc.id, doc.data ?: emptyMap(), myUid, isPremium))
                } catch (e: Exception) {
                    Log.e(TAG, "observeActivePair parse failed", e)
                    trySend(null)
                }
            }
        awaitClose { registration?.remove() }
    }

    private fun parseDuoState(
        pairId: String,
        data: Map<String, Any?>,
        myUid: String,
        isPremium: Boolean,
    ): DuoStreakState {
        val members = (data["members"] as? List<*>)?.filterIsInstance<String>().orEmpty()
        val buddyUid = members.firstOrNull { it != myUid }.orEmpty()
        val profiles = data["memberProfiles"] as? Map<*, *>
        val buddyProfile = profiles?.get(buddyUid) as? Map<*, *>
        val buddyUsername = buddyProfile?.get("username") as? String
            ?: (data["buddyUsername"] as? String).orEmpty()
        val buddyPhotoUrl = buddyProfile?.get("photoUrl") as? String
            ?: data["buddyPhotoUrl"] as? String
        val progress = data["memberProgress"] as? Map<*, *>
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
        val streakDays = (data["streakDays"] as? Number)?.toInt() ?: 0
        val lastBoth = data["lastBothCompleteDate"] as? String
        val graceWeekKey = data["graceWeekKey"] as? String
        val graceUsed = graceWeekKey == DuoStreakEngine.isoWeekKey()
        val atRisk = data["status"] == "active"
            && streakDays > 0
            && DuoStreakEngine.missedYesterday(lastBoth)
            && !(myDone && buddyDone)
        val (h, m) = DuoStreakEngine.timeUntilMidnight()
        return DuoStreakState(
            pairId = pairId,
            buddyUid = buddyUid,
            buddyUsername = buddyUsername,
            buddyPhotoUrl = buddyPhotoUrl,
            status = (data["status"] as? String).orEmpty(),
            invitedBy = (data["invitedBy"] as? String).orEmpty(),
            streakDays = streakDays,
            myDoneToday = myDone,
            buddyDoneToday = buddyDone,
            myProgress = "$myCompleted/$myTotal",
            buddyProgress = "$buddyCompleted/$buddyTotal",
            isIncomingInvite = data["status"] == "pending" && data["invitedBy"] != myUid,
            streakAtRisk = atRisk,
            bothDoneToday = myDone && buddyDone,
            countdownHours = h,
            countdownMinutes = m,
            graceAvailable = DuoStreakEngine.graceAvailableThisWeek(graceWeekKey, isPremium),
            graceUsedThisWeek = graceUsed,
        )
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
            val existingPair = ref.getPairIfReadable()
            if (existingPair != null && existingPair.exists()) {
                return Result.failure(IllegalStateException("Invite already sent or active"))
            }
            val payload = mapOf(
                "members" to listOf(me, buddyUid).sorted(),
                "invitedBy" to me,
                "status" to "pending",
                "streakDays" to 0,
                "createdAt" to FieldValue.serverTimestamp(),
                "memberProgress" to emptyMap<String, Any>(),
                "milestonesAwarded" to emptyList<Int>(),
                "memberProfiles" to mapOf(
                    me to mapOf("username" to myUsername, "photoUrl" to myPhotoUrl),
                    buddyUid to mapOf("username" to buddyUsername, "photoUrl" to buddyPhotoUrl),
                ),
            )
            ref.set(payload).await()
            Log.d(TAG, "inviteBuddy: created duoStreaks/$id")
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "inviteBuddy firestore ${e.code}", e)
            val message = when (e.code) {
                FirebaseFirestoreException.Code.ALREADY_EXISTS -> "Invite already sent or active"
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    "Couldn't send duo invite — deploy latest Firestore rules and try again"
                else -> e.message ?: "Couldn't send duo invite"
            }
            Result.failure(IllegalStateException(message))
        } catch (e: Exception) {
            Log.e(TAG, "inviteBuddy failed", e)
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

    /**
     * Pro perk: forgive yesterday's miss once per ISO week.
     */
    suspend fun applyDuoGrace(pairId: String, isPremium: Boolean): Result<Unit> {
        if (!isPremium) return Result.failure(IllegalStateException("Duo streak shield is a Pro perk"))
        val me = currentUid ?: return Result.failure(IllegalStateException("Sign in first"))
        return try {
            val ref = pairsRef().document(pairId)
            val snap = ref.get().await()
            if (!snap.exists()) return Result.failure(IllegalStateException("Pair not found"))
            val graceWeekKey = snap.getString("graceWeekKey")
            if (graceWeekKey == DuoStreakEngine.isoWeekKey()) {
                return Result.failure(IllegalStateException("Grace already used this week"))
            }
            ref.update(
                mapOf(
                    "graceForgivenDate" to DuoStreakEngine.yesterdayKey(),
                    "graceWeekKey" to DuoStreakEngine.isoWeekKey(),
                ),
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    data class SyncOutcome(
        val milestone: DuoMilestoneReached? = null,
        val broken: DuoStreakBroken? = null,
        val buddyJustFinished: Boolean = false,
    )

    suspend fun syncMyProgress(
        completed: Int,
        total: Int,
        allDone: Boolean,
        myUsername: String,
        isPremium: Boolean,
    ): SyncOutcome {
        val me = currentUid ?: return SyncOutcome()
        try {
            val snap = pairsRef().whereArrayContains("members", me).limit(1).get().await()
            val doc = snap.documents.firstOrNull() ?: return SyncOutcome()
            if (doc.getString("status") != "active") return SyncOutcome()

            val members = (doc.get("members") as? List<*>)?.filterIsInstance<String>().orEmpty()
            val buddyUid = members.firstOrNull { it != me }.orEmpty()
            val profiles = doc.get("memberProfiles") as? Map<*, *>
            val buddyProfile = profiles?.get(buddyUid) as? Map<*, *>
            val buddyUsername = buddyProfile?.get("username") as? String ?: "Your buddy"

            var broken: DuoStreakBroken? = null
            val lastBoth = doc.getString("lastBothCompleteDate")
            val graceForgiven = doc.getString("graceForgivenDate")
            val graceWeekKey = doc.getString("graceWeekKey")
            val currentStreak = (doc.getLong("streakDays") ?: 0L).toInt()
            val canDeferWithGrace = isPremium && DuoStreakEngine.graceAvailableThisWeek(graceWeekKey, isPremium)
            if (DuoStreakEngine.shouldResetStreak(lastBoth, graceForgiven, currentStreak) && !canDeferWithGrace) {
                doc.reference.update(
                    mapOf(
                        "streakDays" to 0,
                        "lastStreakBrokenAt" to FieldValue.serverTimestamp(),
                    ),
                ).await()
                broken = DuoStreakBroken(currentStreak, buddyUsername, doc.id)
            }

            val freshSnap = doc.reference.get().await()
            val today = todayKey()
            val progress = freshSnap.get("memberProgress") as? Map<*, *>
            val myMap = progress?.get(me) as? Map<*, *>
            val wasDone = myMap?.get("date") == today && myMap?.get("allDone") == true
            val buddyMap = progress?.get(buddyUid) as? Map<*, *>
            val buddyDate = buddyMap?.get("date") as? String
            val buddyWasDone = buddyDate == today && buddyMap?.get("allDone") == true

            val progressField = "memberProgress.$me"
            val updates = mutableMapOf<String, Any>(
                progressField to mapOf(
                    "date" to today,
                    "completed" to completed,
                    "total" to total,
                    "allDone" to allDone,
                ),
            )

            val buddyAllDone = buddyWasDone
            val lastBothFresh = freshSnap.getString("lastBothCompleteDate")
            var milestone: DuoMilestoneReached? = null
            if (allDone && buddyAllDone && lastBothFresh != today) {
                val newStreak = (freshSnap.getLong("streakDays") ?: 0L).toInt() + 1
                updates["streakDays"] = newStreak
                updates["lastBothCompleteDate"] = today
                val awarded = (freshSnap.get("milestonesAwarded") as? List<*>)?.filterIsInstance<Number>()
                    ?.map { it.toInt() }.orEmpty()
                if (newStreak in DuoStreakEngine.CELEBRATION_MILESTONES && newStreak !in awarded) {
                    updates["milestonesAwarded"] = awarded + newStreak
                    milestone = DuoMilestoneReached(newStreak, buddyUsername, freshSnap.id)
                }
            }

            freshSnap.reference.update(updates).await()

            val buddyJustFinished = allDone && !wasDone && buddyWasDone
            return SyncOutcome(
                milestone = milestone,
                broken = broken,
                buddyJustFinished = buddyJustFinished,
            )
        } catch (e: Exception) {
            Log.w(TAG, "syncMyProgress failed", e)
            return SyncOutcome()
        }
    }

    private fun Boolean?.orFalse() = this == true
}
