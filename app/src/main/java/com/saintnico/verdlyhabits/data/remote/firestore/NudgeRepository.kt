package com.saintnico.verdlyhabits.data.remote.firestore

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.saintnico.verdlyhabits.data.model.NudgeSurface
import com.saintnico.verdlyhabits.notifications.SocialNotificationDispatcher
import kotlinx.coroutines.tasks.await

sealed class NudgeOutcome {
    data object Sent : NudgeOutcome()
    data class OnCooldown(val remainingMillis: Long) : NudgeOutcome()
    data class Failed(val message: String) : NudgeOutcome()
}

/**
 * Sends a "nudge" — a lightweight poke to a friend, duo buddy, or arena rival — and
 * throttles repeats with a single small document per (sender, target) pair. This keeps
 * nudges from stacking up requests: one read + one write per send attempt, same order of
 * magnitude as an existing friend request, capped by [COOLDOWN_MILLIS] regardless of how
 * many times the button is tapped.
 */
class NudgeRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "NudgeRepository"

        /** Minimum time between nudges from the same person to the same target. */
        val COOLDOWN_MILLIS = 3 * 60 * 60 * 1000L

        fun pairDocId(fromUid: String, toUid: String) = "${fromUid}_${toUid}"
    }

    private fun docFor(targetUid: String) =
        firestore.collection("nudges").document(pairDocId(auth.currentUser?.uid.orEmpty(), targetUid))

    /** Milliseconds remaining before another nudge may be sent to [targetUid]; 0 if free to send. */
    suspend fun cooldownRemaining(targetUid: String): Long {
        val me = auth.currentUser?.uid ?: return 0L
        if (me.isBlank() || me == targetUid || targetUid.isBlank()) return 0L
        return try {
            val snap = docFor(targetUid).get().await()
            val last = snap.getTimestamp("lastSentAt")?.toDate()?.time ?: return 0L
            (COOLDOWN_MILLIS - (System.currentTimeMillis() - last)).coerceAtLeast(0L)
        } catch (_: Exception) {
            0L
        }
    }

    suspend fun sendNudge(
        targetUid: String,
        message: String,
        surface: NudgeSurface,
        fromUsername: String,
        fromPhotoUrl: String?,
        contextId: String? = null,
    ): NudgeOutcome {
        val me = auth.currentUser?.uid ?: return NudgeOutcome.Failed("Sign in first")
        if (targetUid.isBlank() || targetUid == me) return NudgeOutcome.Failed("Can't nudge that person")

        val remaining = cooldownRemaining(targetUid)
        if (remaining > 0) return NudgeOutcome.OnCooldown(remaining)

        return try {
            docFor(targetUid).set(
                mapOf(
                    "fromUid" to me,
                    "toUid" to targetUid,
                    "message" to message,
                    "surface" to surface.wireValue,
                    "lastSentAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            ).await()

            SocialNotificationDispatcher.notifyNudge(
                targetUid = targetUid,
                fromUsername = fromUsername,
                fromPhotoUrl = fromPhotoUrl,
                message = message,
                surface = surface,
                contextId = contextId,
            )
            NudgeOutcome.Sent
        } catch (e: Exception) {
            Log.e(TAG, "sendNudge failed", e)
            NudgeOutcome.Failed(e.message ?: "Could not send nudge — try again.")
        }
    }
}
