package com.saintnico.verdlyhabits.data.remote.firestore

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import com.saintnico.verdlyhabits.data.model.MembershipTier
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class ProfileViewer(
    val uid: String,
    val username: String,
    val displayName: String,
    val photoUrl: String?,
    val viewedAtMillis: Long,
    val viewCount: Int,
    val weekKey: String,
    val profileAccent: String = "sage",
    val membershipTier: MembershipTier = MembershipTier.FREE,
    val equippedTitle: String? = null,
    val level: Int = 0,
) {
    val playerName: String
        get() = displayName.asPlayerLabel() ?: username.asPlayerLabel() ?: "Player"

    val playerHandle: String?
        get() = username.asPlayerLabel()
}

fun String?.asPlayerLabel(): String? {
    val t = orEmpty().trim()
    if (t.isEmpty()) return null
    return when (t.lowercase()) {
        "rival", "unknownrival", "unknown", "unknow" -> null
        else -> t
    }
}

data class RecordProfileViewResult(
    val recorded: Boolean,
    val shouldNotify: Boolean,
)

/**
 * Tracks who opened your profile card.
 * Path: `users/{viewedUid}/profileViews/{viewerUid}` — one doc per viewer, last visit wins.
 */
class ProfileViewRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val lastWriteAt = ConcurrentHashMap<String, Long>()

    companion object {
        private const val TAG = "ProfileViewRepo"
        private const val WRITE_DEBOUNCE_MS = 90_000L
        const val NOTIFY_COOLDOWN_MS = 24L * 60 * 60 * 1000L

        fun currentWeekKey(): String {
            val now = LocalDate.now()
            val week = now.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())
            return "${now.year}-W$week"
        }
    }

    private fun viewsRef(viewedUid: String) =
        firestore.collection("users").document(viewedUid).collection("profileViews")

    suspend fun recordView(
        viewedUid: String,
        viewerUid: String,
        viewerUsername: String,
        viewerDisplayName: String,
        viewerPhotoUrl: String?,
        viewerProfileAccent: String = "sage",
        viewerMembershipTier: MembershipTier = MembershipTier.FREE,
    ): RecordProfileViewResult {
        if (viewedUid.isBlank() || viewerUid.isBlank() || viewedUid == viewerUid) {
            return RecordProfileViewResult(recorded = false, shouldNotify = false)
        }
        val debounceKey = "$viewedUid:$viewerUid"
        val now = System.currentTimeMillis()
        val previousWrite = lastWriteAt[debounceKey] ?: 0L
        if (now - previousWrite < WRITE_DEBOUNCE_MS) {
            return RecordProfileViewResult(recorded = false, shouldNotify = false)
        }
        return try {
            val ref = viewsRef(viewedUid).document(viewerUid)
            val existing = runCatching { ref.get().await() }.getOrNull()
            val lastNotifiedAt = readMillis(existing?.get("lastNotifiedAt"))
            val shouldNotify = now - lastNotifiedAt >= NOTIFY_COOLDOWN_MS
            val data = hashMapOf<String, Any>(
                "viewerUid" to viewerUid,
                "viewedAt" to FieldValue.serverTimestamp(),
                "weekKey" to currentWeekKey(),
                "viewerUsername" to viewerUsername,
                "viewerDisplayName" to viewerDisplayName,
                "viewerProfileAccent" to viewerProfileAccent.ifBlank { "sage" },
                "viewerMembershipTier" to viewerMembershipTier.firestoreValue,
                "viewCount" to FieldValue.increment(1),
            )
            if (!viewerPhotoUrl.isNullOrBlank()) {
                data["viewerPhotoUrl"] = viewerPhotoUrl
            }
            ref.set(data, SetOptions.merge()).await()
            lastWriteAt[debounceKey] = now
            RecordProfileViewResult(recorded = true, shouldNotify = shouldNotify)
        } catch (e: FirebaseFirestoreException) {
            Log.w(TAG, "recordView permission denied. deploy firestore.rules profileViews", e)
            RecordProfileViewResult(recorded = false, shouldNotify = false)
        } catch (e: Exception) {
            Log.w(TAG, "recordView failed", e)
            RecordProfileViewResult(recorded = false, shouldNotify = false)
        }
    }

    suspend fun markNotified(viewedUid: String, viewerUid: String) {
        if (viewedUid.isBlank() || viewerUid.isBlank()) return
        runCatching {
            viewsRef(viewedUid).document(viewerUid)
                .set(mapOf("lastNotifiedAt" to FieldValue.serverTimestamp()), SetOptions.merge())
                .await()
        }
    }

    fun observeRecentViewers(myUid: String): Flow<List<ProfileViewer>> = callbackFlow {
        if (myUid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        val registration: ListenerRegistration = viewsRef(myUid)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.w(TAG, "observeRecentViewers: ${err.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val list = snap?.documents.orEmpty()
                    .mapNotNull { doc -> parseViewer(doc.id, doc.data) }
                    .sortedByDescending { it.viewedAtMillis }
                    .take(80)
                trySend(list)
            }
        awaitClose { registration.remove() }
    }

    /** Count unique friend profile views this week. Never throws. */
    suspend fun friendViewsThisWeek(viewedUid: String, friendUids: Set<String>): Int {
        if (viewedUid.isBlank() || friendUids.isEmpty()) return 0
        return try {
            val week = currentWeekKey()
            val snap = viewsRef(viewedUid).whereEqualTo("weekKey", week).get().await()
            snap.documents.count { doc ->
                val viewer = doc.getString("viewerUid") ?: doc.id
                viewer in friendUids
            }
        } catch (e: FirebaseFirestoreException) {
            Log.w(TAG, "friendViewsThisWeek permission denied. deploy firestore.rules profileViews", e)
            0
        } catch (e: Exception) {
            Log.w(TAG, "friendViewsThisWeek failed", e)
            0
        }
    }

    private fun parseViewer(docId: String, data: Map<String, Any>?): ProfileViewer? {
        if (data == null) return null
        val uid = (data["viewerUid"] as? String)?.takeIf { it.isNotBlank() } ?: docId
        if (uid.isBlank()) return null
        val count = when (val raw = data["viewCount"]) {
            is Number -> raw.toInt().coerceAtLeast(1)
            else -> 1
        }
        return ProfileViewer(
            uid = uid,
            username = (data["viewerUsername"] as? String).orEmpty(),
            displayName = (data["viewerDisplayName"] as? String).orEmpty(),
            photoUrl = data["viewerPhotoUrl"] as? String,
            viewedAtMillis = readMillis(data["viewedAt"]),
            viewCount = count,
            weekKey = (data["weekKey"] as? String).orEmpty(),
            profileAccent = (data["viewerProfileAccent"] as? String)?.ifBlank { null } ?: "sage",
            membershipTier = MembershipTier.fromFirestore(data["viewerMembershipTier"] as? String),
        )
    }

    private fun readMillis(value: Any?): Long = when (value) {
        is Timestamp -> value.toDate().time
        is Number -> value.toLong()
        else -> 0L
    }
}
