package com.saintnico.verdlyhabits.data.remote.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.saintnico.verdlyhabits.data.model.InboxNotification
import com.saintnico.verdlyhabits.data.model.InboxNotificationType
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class InboxRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private fun inboxRef(uid: String) =
        firestore.collection("users").document(uid).collection("inbox")

    fun observeInbox(uid: String, limit: Long = 60): Flow<List<InboxNotification>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }
        var registration: ListenerRegistration? = null
        registration = inboxRef(uid)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(limit)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val items = snap?.documents?.mapNotNull { doc ->
                    parse(doc.id, doc.data ?: return@mapNotNull null)
                }.orEmpty()
                trySend(items)
            }
        awaitClose { registration?.remove() }
    }

    suspend fun writeNotification(targetUid: String, notification: InboxNotification): Result<Unit> {
        if (targetUid.isBlank()) return Result.failure(IllegalArgumentException("No target"))
        val actor = auth.currentUser?.uid.orEmpty()
        if (actor.isBlank()) return Result.failure(IllegalStateException("Sign in first"))
        return try {
            val data = hashMapOf<String, Any>(
                "type" to notification.type.name,
                "title" to notification.title,
                "body" to notification.body,
                "actorUid" to actor,
                "actorUsername" to notification.actorUsername,
                "referenceId" to notification.referenceId,
                "read" to false,
                "actionState" to notification.actionState,
                "createdAt" to FieldValue.serverTimestamp(),
            )
            notification.actorPhotoUrl?.let { data["actorPhotoUrl"] = it }
            notification.challengeId?.let { data["challengeId"] = it }
            val id = notification.referenceId.ifBlank { inboxRef(targetUid).document().id }
            inboxRef(targetUid).document(id).set(data).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun markRead(uid: String, notificationId: String) {
        if (uid.isBlank() || notificationId.isBlank()) return
        runCatching {
            inboxRef(uid).document(notificationId).update("read", true).await()
        }
    }

    suspend fun markAllRead(uid: String) {
        if (uid.isBlank()) return
        runCatching {
            val snap = inboxRef(uid).whereEqualTo("read", false).limit(40).get().await()
            val batch = firestore.batch()
            snap.documents.forEach { batch.update(it.reference, "read", true) }
            batch.commit().await()
        }
    }

    suspend fun updateActionState(uid: String, notificationId: String, state: String) {
        if (uid.isBlank() || notificationId.isBlank()) return
        runCatching {
            inboxRef(uid).document(notificationId)
                .update(mapOf("actionState" to state, "read" to true))
                .await()
        }
    }

    private fun parse(id: String, data: Map<String, Any?>): InboxNotification? {
        val typeName = data["type"] as? String ?: return null
        val type = runCatching { InboxNotificationType.valueOf(typeName) }.getOrNull()
            ?: InboxNotificationType.SYSTEM
        val created = when (val ts = data["createdAt"]) {
            is com.google.firebase.Timestamp -> ts.toDate().time
            is Number -> ts.toLong()
            else -> 0L
        }
        return InboxNotification(
            id = id,
            type = type,
            title = data["title"] as? String ?: "",
            body = data["body"] as? String ?: "",
            actorUid = data["actorUid"] as? String ?: "",
            actorUsername = data["actorUsername"] as? String ?: "",
            actorPhotoUrl = data["actorPhotoUrl"] as? String,
            referenceId = data["referenceId"] as? String ?: id,
            challengeId = data["challengeId"] as? String,
            read = data["read"] as? Boolean ?: false,
            createdAtMillis = created,
            actionState = data["actionState"] as? String ?: "none",
        )
    }
}
