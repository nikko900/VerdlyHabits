package com.saintnico.verdlyhabits.data.remote.firestore

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.saintnico.verdlyhabits.BuildConfig
import com.saintnico.verdlyhabits.data.model.UserReportDoc
import com.saintnico.verdlyhabits.data.model.UserReportReasons
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class UserReportRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val userRepository = UserRepository()

    companion object {
        private const val TAG = "UserReportRepository"
        private const val COLLECTION = "userReports"
    }

    data class SubmitResult(
        val documentId: String,
        val wasUpdate: Boolean,
    )

    suspend fun submitUserReport(
        reportedUid: String,
        reportedUsername: String,
        reportedDisplayName: String,
        reasonCode: String,
        reasonLabel: String,
        details: String?,
        source: String,
    ): Result<SubmitResult> {
        val reporterUid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Sign in to submit a report."))
        if (reportedUid.isBlank() || reportedUid == reporterUid) {
            return Result.failure(IllegalArgumentException("Invalid report target."))
        }
        if (reasonCode.isBlank() || reasonLabel.isBlank()) {
            return Result.failure(IllegalArgumentException("Select a reason."))
        }
        if (UserReportReasons.byCode(reasonCode) == null) {
            return Result.failure(IllegalArgumentException("Invalid report reason."))
        }

        val reporterProfile = userRepository.fetchPublicProfile(reporterUid)
        val reporterUsername = reporterProfile?.username
            ?.ifBlank { reporterProfile.displayName }
            ?.ifBlank { auth.currentUser?.email?.substringBefore("@") }
            ?.ifBlank { "rival" }
            ?: "rival"

        val docId = "${reporterUid}_${reportedUid}"
        val docRef = firestore.collection(COLLECTION).document(docId)
        val existing = docRef.get().await()
        val wasUpdate = existing.exists()

        val payload = mutableMapOf<String, Any>(
            "reporterUid" to reporterUid,
            "reporterUsername" to reporterUsername,
            "reporterEmail" to auth.currentUser?.email.orEmpty(),
            "reportedUid" to reportedUid,
            "reportedUsername" to reportedUsername.trim().removePrefix("@"),
            "reportedDisplayName" to reportedDisplayName.trim(),
            "reasonCode" to reasonCode,
            "reasonLabel" to reasonLabel,
            "details" to details?.trim().orEmpty(),
            "source" to source.ifBlank { "unknown" },
            "status" to "pending",
            "platform" to "android",
            "appVersion" to BuildConfig.VERSION_NAME,
            "updatedAt" to FieldValue.serverTimestamp(),
        )

        if (wasUpdate) {
            payload["submitCount"] = FieldValue.increment(1)
        } else {
            payload["submitCount"] = 1
            payload["createdAt"] = FieldValue.serverTimestamp()
        }

        return try {
            docRef.set(payload, com.google.firebase.firestore.SetOptions.merge()).await()
            Log.d(TAG, "submitUserReport: saved $docId (update=$wasUpdate)")
            Result.success(SubmitResult(documentId = docId, wasUpdate = wasUpdate))
        } catch (e: Exception) {
            Log.e(TAG, "submitUserReport failed", e)
            Result.failure(e)
        }
    }

    fun observeMyReports(): Flow<List<UserReportDoc>> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val registration = firestore.collection(COLLECTION)
            .whereEqualTo("reporterUid", uid)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "observeMyReports failed: ${error.message}")
                    trySend(emptyList())
                    return@addSnapshotListener
                }
                val reports = snapshot?.documents?.mapNotNull(::parseReport).orEmpty()
                trySend(reports)
            }

        awaitClose { registration.remove() }
    }

    suspend fun fetchMyReports(): List<UserReportDoc> {
        val uid = auth.currentUser?.uid ?: return emptyList()
        return try {
            firestore.collection(COLLECTION)
                .whereEqualTo("reporterUid", uid)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .await()
                .documents
                .mapNotNull(::parseReport)
        } catch (e: Exception) {
            Log.e(TAG, "fetchMyReports failed", e)
            emptyList()
        }
    }

    private fun parseReport(doc: DocumentSnapshot): UserReportDoc? {
        val reporterUid = doc.getString("reporterUid") ?: return null
        val reportedUid = doc.getString("reportedUid") ?: return null
        return UserReportDoc(
            id = doc.id,
            reporterUid = reporterUid,
            reporterUsername = doc.getString("reporterUsername").orEmpty(),
            reportedUid = reportedUid,
            reportedUsername = doc.getString("reportedUsername").orEmpty(),
            reportedDisplayName = doc.getString("reportedDisplayName").orEmpty(),
            reasonCode = doc.getString("reasonCode").orEmpty(),
            reasonLabel = doc.getString("reasonLabel").orEmpty(),
            details = doc.getString("details").orEmpty(),
            source = doc.getString("source").orEmpty(),
            status = doc.getString("status").orEmpty().ifBlank { "pending" },
            submitCount = doc.getLong("submitCount")?.toInt() ?: 1,
            createdAt = doc.getTimestamp("createdAt"),
            updatedAt = doc.getTimestamp("updatedAt"),
        )
    }
}
