package com.saintnico.verdlyhabits.data.remote.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.saintnico.verdlyhabits.data.model.FriendSummary
import com.saintnico.verdlyhabits.data.model.Friendship
import com.saintnico.verdlyhabits.data.model.FriendshipStatus
import com.saintnico.verdlyhabits.data.model.MemberPublicProfile
import com.saintnico.verdlyhabits.data.model.ProfileLink
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FriendsRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val friendshipsRef get() = firestore.collection("friendships")

    private val currentUid: String? get() = auth.currentUser?.uid

    private fun pairKey(a: String, b: String): String =
        if (a < b) "${a}_$b" else "${b}_$a"

    suspend fun sendRequest(targetUid: String) {
        val uid = currentUid ?: return
        if (uid == targetUid) return
        val docId = pairKey(uid, targetUid)
        val now = System.currentTimeMillis()
        val data = mapOf(
            "id" to docId,
            "from" to uid,
            "to" to targetUid,
            "status" to "PENDING",
            "createdAt" to now,
            "updatedAt" to now,
            "participants" to listOf(uid, targetUid)
        )
        friendshipsRef.document(docId).set(data, SetOptions.merge()).await()
    }

    suspend fun acceptRequest(otherUid: String) {
        val uid = currentUid ?: return
        val docId = pairKey(uid, otherUid)
        friendshipsRef.document(docId)
            .set(
                mapOf(
                    "status" to "ACCEPTED",
                    "updatedAt" to System.currentTimeMillis()
                ),
                SetOptions.merge()
            )
            .await()
    }

    suspend fun declineOrCancel(otherUid: String) {
        val uid = currentUid ?: return
        val docId = pairKey(uid, otherUid)
        friendshipsRef.document(docId).delete().await()
    }

    suspend fun removeFriend(otherUid: String) {
        declineOrCancel(otherUid)
    }

    /** One-shot status lookup between current user and [otherUid]. */
    suspend fun statusWith(otherUid: String): FriendshipStatus {
        val uid = currentUid ?: return FriendshipStatus.NONE
        val docId = pairKey(uid, otherUid)
        val snap = friendshipsRef.document(docId).get().await()
        if (!snap.exists()) return FriendshipStatus.NONE
        val status = snap.getString("status") ?: return FriendshipStatus.NONE
        val from = snap.getString("from") ?: return FriendshipStatus.NONE
        return when (status) {
            "ACCEPTED" -> FriendshipStatus.ACCEPTED
            "BLOCKED" -> FriendshipStatus.BLOCKED
            "PENDING" -> if (from == uid) FriendshipStatus.OUTGOING_PENDING else FriendshipStatus.INCOMING_PENDING
            else -> FriendshipStatus.NONE
        }
    }

    /** Live status flow (re-emits whenever the friendship doc changes). */
    fun observeStatus(otherUid: String): Flow<FriendshipStatus> = callbackFlow {
        val uid = currentUid
        if (uid == null || uid == otherUid) {
            trySend(FriendshipStatus.NONE)
            awaitClose { }
            return@callbackFlow
        }
        val docId = pairKey(uid, otherUid)
        val reg: ListenerRegistration =
            friendshipsRef.document(docId).addSnapshotListener { snap, _ ->
                if (snap == null || !snap.exists()) {
                    trySend(FriendshipStatus.NONE)
                    return@addSnapshotListener
                }
                val status = snap.getString("status").orEmpty()
                val from = snap.getString("from").orEmpty()
                val mapped = when (status) {
                    "ACCEPTED" -> FriendshipStatus.ACCEPTED
                    "BLOCKED" -> FriendshipStatus.BLOCKED
                    "PENDING" ->
                        if (from == uid) FriendshipStatus.OUTGOING_PENDING
                        else FriendshipStatus.INCOMING_PENDING
                    else -> FriendshipStatus.NONE
                }
                trySend(mapped)
            }
        awaitClose { reg.remove() }
    }

    fun observeAcceptedFriends(): Flow<List<FriendSummary>> = callbackFlow {
        val uid = currentUid
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val reg: ListenerRegistration = friendshipsRef
            .whereArrayContains("participants", uid)
            .whereEqualTo("status", "ACCEPTED")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    val from = doc.getString("from").orEmpty()
                    val to = doc.getString("to").orEmpty()
                    val other = if (from == uid) to else from
                    if (other.isBlank()) null else FriendSummary(
                        uid = other,
                        username = "",
                        displayName = "",
                        photoUrl = null
                    )
                }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeIncomingRequests(): Flow<List<Friendship>> = callbackFlow {
        val uid = currentUid
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val reg: ListenerRegistration = friendshipsRef
            .whereEqualTo("to", uid)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    Friendship(
                        id = doc.id,
                        from = doc.getString("from").orEmpty(),
                        to = doc.getString("to").orEmpty(),
                        status = doc.getString("status").orEmpty(),
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        updatedAt = doc.getLong("updatedAt") ?: 0L
                    ).takeIf { it.from.isNotBlank() && it.to.isNotBlank() }
                }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    fun observeOutgoingRequests(): Flow<List<Friendship>> = callbackFlow {
        val uid = currentUid
        if (uid == null) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }
        val reg: ListenerRegistration = friendshipsRef
            .whereEqualTo("from", uid)
            .whereEqualTo("status", "PENDING")
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    Friendship(
                        id = doc.id,
                        from = doc.getString("from").orEmpty(),
                        to = doc.getString("to").orEmpty(),
                        status = doc.getString("status").orEmpty(),
                        createdAt = doc.getLong("createdAt") ?: 0L,
                        updatedAt = doc.getLong("updatedAt") ?: 0L
                    ).takeIf { it.from.isNotBlank() && it.to.isNotBlank() }
                }
                trySend(list)
            }
        awaitClose { reg.remove() }
    }

    suspend fun acceptedFriendsCount(uid: String): Int {
        return try {
            val snap = friendshipsRef
                .whereArrayContains("participants", uid)
                .whereEqualTo("status", "ACCEPTED")
                .get()
                .await()
            snap.size()
        } catch (_: Exception) {
            0
        }
    }

    /** Fetch public profile for [uid]. Caller decides what to render based on visibility. */
    suspend fun fetchPublicProfile(uid: String): MemberPublicProfile? {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (!doc.exists()) return null
            val rawLinks = (doc.get("links") as? List<*>).orEmpty().mapNotNull { item ->
                (item as? Map<*, *>)?.let { map ->
                    val label = map["label"]?.toString().orEmpty()
                    val url = map["url"]?.toString().orEmpty()
                    if (label.isBlank() && url.isBlank()) null else ProfileLink(label, url)
                }
            }
            val createdAt = when (val ts = doc.get("createdAt")) {
                is com.google.firebase.Timestamp -> ts.toDate().time
                is Long -> ts
                else -> 0L
            }
            MemberPublicProfile(
                uid = doc.id,
                username = doc.getString("username").orEmpty(),
                displayName = doc.getString("displayName").orEmpty(),
                photoUrl = doc.getString("photoUrl"),
                bio = doc.getString("bio").orEmpty(),
                motto = doc.getString("motto").orEmpty(),
                accentKey = doc.getString("profileAccent").orEmpty(),
                links = rawLinks,
                level = (doc.getLong("level") ?: 0L).toInt(),
                totalXp = doc.getLong("xp") ?: 0L,
                longestStreak = (doc.getLong("longestStreakEver") ?: 0L).toInt(),
                totalCompletions = doc.getLong("totalCompletions") ?: 0L,
                totalFocusMinutes = doc.getLong("totalFocusMinutes") ?: 0L,
                friendsCount = acceptedFriendsCount(uid),
                visibility = doc.getString("profileVisibility") ?: "PUBLIC",
                createdAt = createdAt
            )
        } catch (_: Exception) {
            null
        }
    }
}
