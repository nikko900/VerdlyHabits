package com.saintnico.verdlyhabits.data.remote.firestore

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.saintnico.verdlyhabits.BuildConfig
import com.saintnico.verdlyhabits.data.model.UserReportDoc
import com.saintnico.verdlyhabits.data.model.UserReportReasons
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

data class MyReportsSnapshot(
    val reports: List<UserReportDoc> = emptyList(),
    val loaded: Boolean = false,
    val error: String? = null,
    val indexUrl: String? = null,
)

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
        // Missing docs are often unreadable under rules (resource == null). Treat that as "new report".
        val existing = try {
            docRef.get().await()
        } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
            if (e.code == com.google.firebase.firestore.FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                null
            } else {
                throw e
            }
        }
        val wasUpdate = existing?.exists() == true
        val existingStatus = existing?.getString("status").orEmpty().ifBlank { "pending" }

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
            "platform" to "android",
            "appVersion" to BuildConfig.VERSION_NAME,
            "updatedAt" to FieldValue.serverTimestamp(),
        )

        if (wasUpdate) {
            payload["submitCount"] = FieldValue.increment(1)
            // Never yank a report back to pending if you're already reviewing / closed it.
            if (existingStatus == "pending") {
                payload["status"] = "pending"
            }
        } else {
            payload["submitCount"] = 1
            payload["status"] = "pending"
            payload["outcome"] = ""
            payload["resolutionNote"] = ""
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

    fun observeMyReports(): Flow<MyReportsSnapshot> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            trySend(MyReportsSnapshot(loaded = true))
            close()
            return@callbackFlow
        }
        trySend(MyReportsSnapshot(loaded = false))

        // Equality-only so the query matches firestore.rules without a composite index.
        // Sort client-side by updatedAt. Keep reporterUid filter — required by allow list.
        val registration = firestore.collection(COLLECTION)
            .whereEqualTo("reporterUid", uid)
            .limit(50)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "observeMyReports failed: ${error.message}")
                    trySend(
                        MyReportsSnapshot(
                            loaded = true,
                            error = firestoreListError(error),
                            indexUrl = extractFirebaseIndexUrl(error.message),
                        ),
                    )
                    return@addSnapshotListener
                }
                val reports = snapshot?.documents?.mapNotNull(::parseReport).orEmpty()
                    .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                trySend(MyReportsSnapshot(reports = reports, loaded = true))
            }

        awaitClose { registration.remove() }
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
            outcome = doc.getString("outcome").orEmpty(),
            resolutionNote = doc.getString("resolutionNote").orEmpty(),
            submitCount = doc.getLong("submitCount")?.toInt() ?: 1,
            createdAt = doc.getTimestamp("createdAt"),
            updatedAt = doc.getTimestamp("updatedAt"),
            resolvedAt = doc.getTimestamp("resolvedAt"),
        )
    }
}
