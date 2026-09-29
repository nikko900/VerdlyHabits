package com.saintnico.verdlyhabits.data.remote.firestore

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class BlockedUserDoc(
    val uid: String,
    val username: String,
    val displayName: String,
    val photoUrl: String?,
    val reason: String,
    /** True when we were friends (or pending) before the block — restore on unblock. */
    val wasFriend: Boolean = false,
)

data class UnblockResult(
    val wasFriend: Boolean,
)

data class BlockedListSnapshot(
    val users: List<BlockedUserDoc> = emptyList(),
    val uids: Set<String> = emptySet(),
    val loaded: Boolean = false,
    val error: String? = null,
)

/**
 * Personal blocks — you hide someone from your social surfaces.
 * Path: users/{myUid}/blocked/{blockedUid}
 *
 * This is NOT a platform ban. Platform bans are set by you in Firebase Console
 * on users/{uid} (e.g. banned: true) and enforced in rules later.
 */
class BlockRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "BlockRepository"
    }

    private fun blockedCol(myUid: String) =
        firestore.collection("users").document(myUid).collection("blocked")

    fun observeBlockedUids(myUid: String): Flow<Set<String>> = callbackFlow {
        if (myUid.isBlank()) {
            trySend(emptySet())
            close()
            return@callbackFlow
        }
        val reg: ListenerRegistration = blockedCol(myUid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.w(TAG, "observeBlockedUids: ${err.message}")
                    // Keep last emission; do not pretend the list is empty.
                    return@addSnapshotListener
                }
                trySend(snap?.documents?.map { it.id }?.toSet().orEmpty())
            }
        awaitClose { reg.remove() }
    }

    fun observeBlockedUsers(myUid: String): Flow<BlockedListSnapshot> = callbackFlow {
        if (myUid.isBlank()) {
            trySend(BlockedListSnapshot(loaded = true))
            close()
            return@callbackFlow
        }
        trySend(BlockedListSnapshot(loaded = false))
        var lastUsers = emptyList<BlockedUserDoc>()
        val reg = blockedCol(myUid).addSnapshotListener { snap, err ->
            if (err != null) {
                Log.w(TAG, "observeBlockedUsers: ${err.message}")
                trySend(
                    BlockedListSnapshot(
                        users = lastUsers,
                        uids = lastUsers.map { it.uid }.toSet(),
                        loaded = true,
                        error = firestoreListError(err),
                    ),
                )
                return@addSnapshotListener
            }
            val list = snap?.documents?.map { doc ->
                BlockedUserDoc(
                    uid = doc.id,
                    username = doc.getString("username").orEmpty(),
                    displayName = doc.getString("displayName").orEmpty(),
                    photoUrl = doc.getString("photoUrl")?.ifBlank { null },
                    reason = doc.getString("reason").orEmpty(),
                    // Missing flag = legacy block (friendship was torn down) — restore on unblock.
                    wasFriend = doc.getBoolean("wasFriend") ?: true,
                )
            }.orEmpty().sortedBy { it.username.ifBlank { it.displayName }.lowercase() }
            lastUsers = list
            trySend(
                BlockedListSnapshot(
                    users = list,
                    uids = list.map { it.uid }.toSet(),
                    loaded = true,
                    error = null,
                ),
            )
        }
        awaitClose { reg.remove() }
    }

    suspend fun isBlocked(otherUid: String): Boolean {
        val me = auth.currentUser?.uid ?: return false
        if (otherUid.isBlank() || otherUid == me) return false
        return try {
            blockedCol(me).document(otherUid).get().await().exists()
        } catch (e: Exception) {
            Log.w(TAG, "isBlocked failed: ${e.message}")
            false
        }
    }

    suspend fun blockUser(
        blockedUid: String,
        username: String,
        displayName: String,
        photoUrl: String? = null,
        reason: String = "user_block",
        wasFriend: Boolean = false,
    ): Result<Unit> {
        val me = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Sign in to block someone."))
        if (blockedUid.isBlank() || blockedUid == me) {
            return Result.failure(IllegalArgumentException("Invalid block target."))
        }
        return try {
            blockedCol(me).document(blockedUid).set(
                mapOf(
                    "blockedUid" to blockedUid,
                    "username" to username.trim().removePrefix("@"),
                    "displayName" to displayName.trim(),
                    "photoUrl" to photoUrl.orEmpty(),
                    "reason" to reason.ifBlank { "user_block" },
                    "wasFriend" to wasFriend,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            Log.d(TAG, "blocked $blockedUid wasFriend=$wasFriend")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "blockUser failed", e)
            Result.failure(e)
        }
    }

    suspend fun unblockUser(blockedUid: String): Result<UnblockResult> {
        val me = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Sign in to unblock."))
        if (blockedUid.isBlank()) {
            return Result.failure(IllegalArgumentException("Invalid unblock target."))
        }
        return try {
            val ref = blockedCol(me).document(blockedUid)
            val snap = try {
                ref.get().await()
            } catch (_: Exception) {
                null
            }
            // Legacy blocks (no wasFriend field) tore down the friendship — restore them.
            val wasFriend = snap?.getBoolean("wasFriend") ?: true
            ref.delete().await()
            Log.d(TAG, "unblocked $blockedUid wasFriend=$wasFriend")
            Result.success(UnblockResult(wasFriend = wasFriend))
        } catch (e: Exception) {
            Log.e(TAG, "unblockUser failed", e)
            Result.failure(e)
        }
    }
}

internal fun firestoreListError(err: Exception): String {
    val m = err.message.orEmpty()
    return when {
        m.contains("PERMISSION_DENIED", ignoreCase = true) ||
            m.contains("Missing or insufficient permissions", ignoreCase = true) ->
            "Couldn't load this list. Sign out and back in, then try again."
        m.contains("UNAVAILABLE", ignoreCase = true) ||
            m.contains("Unable to resolve host", ignoreCase = true) ||
            m.contains("offline", ignoreCase = true) ->
            "You're offline. Check your connection and retry."
        else -> m.ifBlank { "Couldn't load. Try again." }
    }
}

internal fun extractFirebaseIndexUrl(message: String?): String? {
    val m = message ?: return null
    return Regex("https://console\\.firebase\\.google\\.com\\S+")
        .find(m)
        ?.value
        ?.trimEnd('.', ',', ')', ']', '"')
}
