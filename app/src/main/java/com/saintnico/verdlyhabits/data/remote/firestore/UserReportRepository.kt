package com.saintnico.verdlyhabits.data.remote.firestore

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

class UserReportRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "UserReportRepository"
    }

    suspend fun submitUserReport(
        reportedUid: String,
        reportedUsername: String,
        reasonCode: String,
        reasonLabel: String,
        details: String?,
    ): Result<Unit> {
        val reporterUid = auth.currentUser?.uid
            ?: return Result.failure(IllegalStateException("Sign in to submit a report."))
        if (reportedUid.isBlank() || reportedUid == reporterUid) {
            return Result.failure(IllegalArgumentException("Invalid report target."))
        }
        if (reasonCode.isBlank()) {
            return Result.failure(IllegalArgumentException("Select a reason."))
        }

        val docId = "${reporterUid}_${reportedUid}_${System.currentTimeMillis()}"
        return try {
            firestore.collection("userReports").document(docId).set(
                mapOf(
                    "reporterUid" to reporterUid,
                    "reportedUid" to reportedUid,
                    "reportedUsername" to reportedUsername,
                    "reasonCode" to reasonCode,
                    "reasonLabel" to reasonLabel,
                    "details" to details?.trim().orEmpty(),
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                ),
            ).await()
            Log.d(TAG, "submitUserReport: saved $docId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "submitUserReport failed", e)
            Result.failure(e)
        }
    }
}
