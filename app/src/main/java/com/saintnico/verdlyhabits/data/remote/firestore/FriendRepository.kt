package com.saintnico.verdlyhabits.data.remote.firestore

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class FriendRequestDoc(
    val id: String,
    val fromUid: String,
    val toUid: String,
    val status: String,
)

enum class FriendRelationship {
    Self,
    Friends,
    IncomingPending,
    OutgoingPending,
    None,
}

class FriendRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val currentUid: String?
        get() = auth.currentUser?.uid

    companion object {
        private const val TAG = "FriendRepository"
        fun requestDocId(fromUid: String, toUid: String): String = "${fromUid}_${toUid}"
    }

    private fun parseRequest(d: com.google.firebase.firestore.DocumentSnapshot): FriendRequestDoc =
        FriendRequestDoc(
            id = d.id,
            fromUid = d.getString("fromUid").orEmpty(),
            toUid = d.getString("toUid").orEmpty(),
            status = d.getString("status").orEmpty(),
        )

    /** Full-document write so security rules never depend on partial .update() fields. */
    private suspend fun DocumentReference.writeFriendRequest(
        fromUid: String,
        toUid: String,
        status: String,
        preserveCreatedAt: Any? = null,
    ) {
        val data = mutableMapOf<String, Any>(
            "fromUid" to fromUid,
            "toUid" to toUid,
            "status" to status,
        )
        data["createdAt"] = preserveCreatedAt
            ?: com.google.firebase.firestore.FieldValue.serverTimestamp()
        set(data).await()
    }

    /** GET is denied on missing docs under legacy rules; treat as absent and still allow create. */
    private suspend fun DocumentReference.getIfReadable(): com.google.firebase.firestore.DocumentSnapshot? =
        try {
            get().await()
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Log.w(TAG, "getIfReadable: PERMISSION_DENIED on $id — continuing")
                null
            } else {
                throw e
            }
        }

    private suspend fun DocumentReference.rewriteStatus(status: String) {
        val snap = getIfReadable() ?: return
        if (!snap.exists()) return
        writeFriendRequest(
            fromUid = snap.getString("fromUid").orEmpty(),
            toUid = snap.getString("toUid").orEmpty(),
            status = status,
            preserveCreatedAt = snap.get("createdAt"),
        )
    }

    suspend fun sendFriendRequest(toUid: String): Result<Unit> {
        val from = currentUid ?: return Result.failure(IllegalStateException("Sign in with Google first"))
        if (from == toUid) return Result.failure(IllegalStateException("Cannot add yourself"))
        return try {
            auth.currentUser?.getIdToken(true)?.await()
            val col = firestore.collection("friendRequests")
            val reverseRef = col.document(requestDocId(toUid, from))
            val reverse = reverseRef.getIfReadable()
            if (reverse != null && reverse.exists() && reverse.getString("status") == "pending") {
                reverseRef.rewriteStatus("accepted")
                Log.d(TAG, "sendFriendRequest: auto-accepted reverse ${reverseRef.id}")
                return Result.success(Unit)
            }
            val forwardRef = col.document(requestDocId(from, toUid))
            val forward = forwardRef.getIfReadable()
            if (forward != null && forward.exists()) {
                when (forward.getString("status").orEmpty()) {
                    "pending", "accepted" -> return Result.success(Unit)
                    "declined" -> forwardRef.delete().await()
                }
            }
            forwardRef.writeFriendRequest(from, toUid, "pending")
            Log.d(TAG, "sendFriendRequest: created ${forwardRef.id}")
            Result.success(Unit)
        } catch (e: FirebaseFirestoreException) {
            Log.e(TAG, "sendFriendRequest firestore ${e.code}", e)
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "sendFriendRequest failed", e)
            Result.failure(e)
        }
    }

    suspend fun acceptRequest(requestId: String): Result<Unit> {
        val me = currentUid ?: return Result.failure(IllegalStateException("Sign in with Google first"))
        return try {
            val ref = firestore.collection("friendRequests").document(requestId)
            val snap = ref.get().await()
            if (snap.getString("toUid") != me) return Result.failure(SecurityException("Not your request"))
            ref.rewriteStatus("accepted")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "acceptRequest failed", e)
            Result.failure(e)
        }
    }

    suspend fun declineRequest(requestId: String): Result<Unit> {
        val me = currentUid ?: return Result.failure(IllegalStateException("Sign in with Google first"))
        return try {
            val ref = firestore.collection("friendRequests").document(requestId)
            val snap = ref.get().await()
            if (snap.getString("toUid") != me) return Result.failure(SecurityException("Not your request"))
            ref.rewriteStatus("declined")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "declineRequest failed", e)
            Result.failure(e)
        }
    }

    suspend fun findPendingIncomingRequestId(fromUid: String): String? {
        val me = currentUid ?: return null
        val snap = firestore.collection("friendRequests")
            .document(requestDocId(fromUid, me))
            .get()
            .await()
        return if (snap.exists() && snap.getString("status") == "pending") snap.id else null
    }

    suspend fun relationshipWith(otherUid: String): FriendRelationship {
        val me = currentUid ?: return FriendRelationship.None
        if (me == otherUid) return FriendRelationship.Self
        val col = firestore.collection("friendRequests")
        val outgoing = col.document(requestDocId(me, otherUid)).getIfReadable()
        if (outgoing != null && outgoing.exists()) {
            when (outgoing.getString("status")) {
                "accepted" -> return FriendRelationship.Friends
                "pending" -> return FriendRelationship.OutgoingPending
            }
        }
        val incoming = col.document(requestDocId(otherUid, me)).getIfReadable()
        if (incoming != null && incoming.exists()) {
            when (incoming.getString("status")) {
                "accepted" -> return FriendRelationship.Friends
                "pending" -> return FriendRelationship.IncomingPending
            }
        }
        return FriendRelationship.None
    }

    fun observeIncomingRequests(myUid: String): Flow<List<FriendRequestDoc>> = callbackFlow {
        if (myUid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val reg: ListenerRegistration = firestore.collection("friendRequests")
            .whereEqualTo("toUid", myUid)
            .whereEqualTo("status", "pending")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "observeIncomingRequests", err)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snap?.documents?.map(::parseRequest).orEmpty())
            }
        awaitClose { reg.remove() }
    }

    fun observeAcceptedFromMe(myUid: String): Flow<List<FriendRequestDoc>> = callbackFlow {
        if (myUid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val reg = firestore.collection("friendRequests")
            .whereEqualTo("fromUid", myUid)
            .whereEqualTo("status", "accepted")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "observeAcceptedFromMe", err)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snap?.documents?.map(::parseRequest).orEmpty())
            }
        awaitClose { reg.remove() }
    }

    fun observeAcceptedToMe(myUid: String): Flow<List<FriendRequestDoc>> = callbackFlow {
        if (myUid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val reg = firestore.collection("friendRequests")
            .whereEqualTo("toUid", myUid)
            .whereEqualTo("status", "accepted")
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e(TAG, "observeAcceptedToMe", err)
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                trySend(snap?.documents?.map(::parseRequest).orEmpty())
            }
        awaitClose { reg.remove() }
    }
}
