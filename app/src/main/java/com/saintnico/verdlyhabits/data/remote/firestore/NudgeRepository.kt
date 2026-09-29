package com.saintnico.verdlyhabits.data.remote.firestore

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.saintnico.verdlyhabits.data.model.NudgeMessages
import com.saintnico.verdlyhabits.data.model.NudgeSurface
import com.saintnico.verdlyhabits.notifications.SocialNotificationDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

    private fun docFor(fromUid: String, toUid: String) =
        firestore.collection("nudges").document(pairDocId(fromUid, toUid))

    /**
     * Milliseconds remaining before [fromUid] may nudge [toUid] again.
     * Scoped to the pair doc `nudges/{fromUid}_{toUid}` — not per target alone.
     * 0 if free to send.
     */
    suspend fun cooldownRemaining(fromUid: String, toUid: String): Long {
        if (fromUid.isBlank() || toUid.isBlank() || fromUid == toUid) return 0L
        return try {
            val snap = docFor(fromUid, toUid).get().await()
            remainingFromSnap(snap.getTimestamp("lastSentAt")?.toDate()?.time)
        } catch (_: Exception) {
            0L
        }
    }

    /** Current user → [targetUid]. */
    suspend fun cooldownRemaining(targetUid: String): Long =
        cooldownRemaining(auth.currentUser?.uid.orEmpty(), targetUid)

    /**
     * One wait for the visible set. Firestore rules forbid `list` on `/nudges`, and the
     * Android client has no Admin-style `getAll`, so this fans out parallel `get`s on
     * known pair ids — not a query, and not one-read-per-row from Compose.
     */
    suspend fun cooldownsRemaining(targetUids: Collection<String>): Map<String, Long> {
        val me = auth.currentUser?.uid ?: return emptyMap()
        val unique = targetUids.filter { it.isNotBlank() && it != me }.distinct()
        if (me.isBlank() || unique.isEmpty()) return emptyMap()
        return try {
            coroutineScope {
                unique.map { uid ->
                    async {
                        val last = docFor(me, uid).get().await()
                            .getTimestamp("lastSentAt")?.toDate()?.time
                        uid to remainingFromSnap(last)
                    }
                }.awaitAll().toMap()
            }
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun remainingFromSnap(lastSentAtMillis: Long?): Long {
        if (lastSentAtMillis == null || lastSentAtMillis <= 0L) return 0L
        return (COOLDOWN_MILLIS - (System.currentTimeMillis() - lastSentAtMillis)).coerceAtLeast(0L)
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
        val clipped = message.trim().replace('\n', ' ').take(NudgeMessages.MESSAGE_MAX_CHARS)
        if (clipped.isBlank()) return NudgeOutcome.Failed("Write a short note first")

        val remaining = cooldownRemaining(me, targetUid)
        if (remaining > 0) return NudgeOutcome.OnCooldown(remaining)

        return try {
            docFor(me, targetUid).set(
                mapOf(
                    "fromUid" to me,
                    "toUid" to targetUid,
                    "message" to clipped,
                    "surface" to surface.wireValue,
                    "lastSentAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            ).await()

            SocialNotificationDispatcher.notifyNudge(
                targetUid = targetUid,
                fromUsername = fromUsername,
                fromPhotoUrl = fromPhotoUrl,
                message = clipped,
                surface = surface,
                contextId = contextId,
            )
            NudgeOutcome.Sent
        } catch (e: Exception) {
            Log.e(TAG, "sendNudge failed", e)
            NudgeOutcome.Failed(e.message ?: "Could not send nudge. try again.")
        }
    }
}
