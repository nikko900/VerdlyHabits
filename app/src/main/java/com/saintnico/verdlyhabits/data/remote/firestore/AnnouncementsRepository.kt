package com.saintnico.verdlyhabits.data.remote.firestore

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.saintnico.verdlyhabits.data.model.DevAnnouncement
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class AnnouncementsRepository {
    private val firestore = FirebaseFirestore.getInstance()

    fun observeActive(isPro: Boolean): Flow<List<DevAnnouncement>> = callbackFlow {
        var registration: ListenerRegistration? = null
        registration = firestore.collection("announcements")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(30)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val now = System.currentTimeMillis()
                val items = snap?.documents?.mapNotNull { doc ->
                    parse(doc.id, doc.data ?: return@mapNotNull null)
                }.orEmpty()
                    .filter { ann ->
                        ann.active &&
                            ann.expiresAtMillis?.let { now > it } != true &&
                            audienceMatches(ann.audience, isPro)
                    }
                trySend(items)
            }
        awaitClose { registration?.remove() }
    }

    suspend fun markDismissed(uid: String, announcementId: String) {
        if (uid.isBlank() || announcementId.isBlank()) return
        runCatching {
            firestore.collection("users").document(uid)
                .collection("announcementReads")
                .document(announcementId)
                .set(mapOf("readAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()))
                .await()
        }
    }

    fun observeDismissedIds(uid: String): Flow<Set<String>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptySet())
            close()
            return@callbackFlow
        }
        var registration: ListenerRegistration? = null
        registration = firestore.collection("users").document(uid)
            .collection("announcementReads")
            .addSnapshotListener { snap, _ ->
                val ids = snap?.documents?.map { it.id }?.toSet().orEmpty()
                trySend(ids)
            }
        awaitClose { registration?.remove() }
    }

    private fun audienceMatches(audience: String, isPro: Boolean): Boolean = when (audience.lowercase()) {
        "pro" -> isPro
        "free" -> !isPro
        else -> true
    }

    private fun parse(id: String, data: Map<String, Any?>): DevAnnouncement? {
        val title = data["title"] as? String ?: return null
        val body = data["body"] as? String ?: return null
        val created = when (val ts = data["createdAt"]) {
            is com.google.firebase.Timestamp -> ts.toDate().time
            is Number -> ts.toLong()
            else -> 0L
        }
        val expires = when (val ts = data["expiresAt"]) {
            is com.google.firebase.Timestamp -> ts.toDate().time
            is Number -> ts.toLong()
            else -> null
        }
        return DevAnnouncement(
            id = id,
            title = title,
            body = body,
            route = data["route"] as? String ?: "",
            ctaLabel = data["ctaLabel"] as? String ?: "",
            active = data["active"] as? Boolean ?: true,
            audience = data["audience"] as? String ?: "all",
            expiresAtMillis = expires,
            createdAtMillis = created,
        )
    }
}
