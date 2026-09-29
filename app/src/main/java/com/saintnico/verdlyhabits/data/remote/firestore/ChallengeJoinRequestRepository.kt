package com.saintnico.verdlyhabits.data.remote.firestore

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class ChallengeJoinRequestDoc(
    val id: String,
    val challengeId: String,
    val challengeTitle: String,
    val fromUid: String,
    val fromUsername: String,
    val hostUid: String,
    val targetMemberUid: String,
    val status: String,
)

class ChallengeJoinRequestRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "ChallengeJoinRequestRepo"
    }

    private suspend fun com.google.firebase.firestore.DocumentReference.getIfReadable():
        com.google.firebase.firestore.DocumentSnapshot? =
        try {
            get().await()
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Log.w(TAG, "getIfReadable: PERMISSION_DENIED on $id")
                null
            } else {
                throw e
            }
        }

    private val currentUid: String?
        get() = auth.currentUser?.uid

    private fun docId(challengeId: String, fromUid: String) = "${challengeId}_${fromUid}"

    private fun parse(d: com.google.firebase.firestore.DocumentSnapshot): ChallengeJoinRequestDoc =
        ChallengeJoinRequestDoc(
            id = d.id,
            challengeId = d.getString("challengeId").orEmpty(),
            challengeTitle = d.getString("challengeTitle").orEmpty(),
            fromUid = d.getString("fromUid").orEmpty(),
            fromUsername = d.getString("fromUsername").orEmpty(),
            hostUid = d.getString("hostUid").orEmpty(),
            targetMemberUid = d.getString("targetMemberUid").orEmpty(),
            status = d.getString("status").orEmpty(),
        )

    suspend fun sendJoinRequest(
        challengeId: String,
        challengeTitle: String,
        hostUid: String,
        targetMemberUid: String,
        fromUsername: String,
    ): Result<Unit> {
        val from = currentUid ?: return Result.failure(IllegalStateException("Sign in with Google first"))
        if (challengeId.isBlank() || hostUid.isBlank()) {
            return Result.failure(IllegalArgumentException("Invalid challenge"))
        }
        return try {
            val ref = firestore.collection("challengeJoinRequests").document(docId(challengeId, from))
            val existing = ref.getIfReadable()
            if (existing != null && existing.exists()) {
                val status = existing.getString("status").orEmpty()
                if (status == "pending" || status == "approved") return Result.success(Unit)
            }
            ref.set(
                mapOf(
                    "challengeId" to challengeId,
                    "challengeTitle" to challengeTitle,
                    "fromUid" to from,
                    "fromUsername" to fromUsername,
                    "hostUid" to hostUid,
                    "targetMemberUid" to targetMemberUid,
                    "status" to "pending",
                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                ),
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun findPendingRequest(challengeId: String, fromUid: String): ChallengeJoinRequestDoc? {
        val me = currentUid ?: return null
        if (fromUid != me) return null
        val snap = firestore.collection("challengeJoinRequests")
            .document(docId(challengeId, fromUid))
            .get()
            .await()
        if (!snap.exists()) return null
        val doc = parse(snap)
        return if (doc.status == "pending") doc else null
    }

    suspend fun approveRequest(requestId: String): Result<Unit> {
        val me = currentUid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val ref = firestore.collection("challengeJoinRequests").document(requestId)
            val snap = ref.get().await()
            if (snap.getString("hostUid") != me) {
                return Result.failure(SecurityException("Only the challenge host can approve"))
            }
            ref.update("status", "approved").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun declineRequest(requestId: String): Result<Unit> {
        val me = currentUid ?: return Result.failure(IllegalStateException("Not signed in"))
        return try {
            val ref = firestore.collection("challengeJoinRequests").document(requestId)
            val snap = ref.get().await()
            if (snap.getString("hostUid") != me) {
                return Result.failure(SecurityException("Only the challenge host can decline"))
            }
            ref.update("status", "declined").await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observePendingForHost(hostUid: String): Flow<List<ChallengeJoinRequestDoc>> = callbackFlow {
        if (hostUid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val reg: ListenerRegistration = firestore.collection("challengeJoinRequests")
            .whereEqualTo("hostUid", hostUid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snap?.documents?.map(::parse).orEmpty())
            }
        awaitClose { reg.remove() }
    }
}
