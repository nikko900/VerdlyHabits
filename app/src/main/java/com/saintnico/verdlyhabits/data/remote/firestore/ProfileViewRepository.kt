package com.saintnico.verdlyhabits.data.remote.firestore

import android.util.Log
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Privacy-safe profile view tracking: only friend viewers in the current ISO week are counted.
 * Requires Firestore rules for `users/{uid}/profileViews/{viewerUid}` — see firestore.rules.
 */
class ProfileViewRepository {
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val TAG = "ProfileViewRepo"

        fun currentWeekKey(): String {
            val now = LocalDate.now()
            val week = now.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear())
            return "${now.year}-W$week"
        }
    }

    private fun viewsRef(viewedUid: String) =
        firestore.collection("users").document(viewedUid).collection("profileViews")

    suspend fun recordView(viewedUid: String, viewerUid: String) {
        if (viewedUid.isBlank() || viewerUid.isBlank() || viewedUid == viewerUid) return
        try {
            viewsRef(viewedUid).document(viewerUid).set(
                mapOf(
                    "viewerUid" to viewerUid,
                    "viewedAt" to FieldValue.serverTimestamp(),
                    "weekKey" to currentWeekKey(),
                ),
            ).await()
        } catch (e: FirebaseFirestoreException) {
            Log.w(TAG, "recordView permission denied — deploy firestore.rules profileViews", e)
        } catch (e: Exception) {
            Log.w(TAG, "recordView failed", e)
        }
    }

    /** Count unique friend profile views this week (Pro insight). Never throws. */
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
            Log.w(TAG, "friendViewsThisWeek permission denied — deploy firestore.rules profileViews", e)
            0
        } catch (e: Exception) {
            Log.w(TAG, "friendViewsThisWeek failed", e)
            0
        }
    }
}
