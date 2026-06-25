package com.saintnico.verdlyhabits.data.remote.firestore

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.saintnico.verdlyhabits.data.model.Challenge
import com.saintnico.verdlyhabits.data.model.ChallengeMode
import com.saintnico.verdlyhabits.data.model.ChallengeResultSnapshot
import com.saintnico.verdlyhabits.data.model.ChallengeResultStanding
import com.saintnico.verdlyhabits.data.model.ChallengeTopPost
import com.saintnico.verdlyhabits.data.model.ReactionWeights
import com.saintnico.verdlyhabits.util.ChallengeStreakCalculator
import com.saintnico.verdlyhabits.util.UserFacingErrors
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** Returned after creating a challenge — [documentId] is what Firestore uses; [inviteCode] is the short join code. */
data class CreatedChallenge(val documentId: String, val inviteCode: String)

/** Result of attempting to join a challenge (by Firestore document id, pasted link, or 6-letter [inviteCode]). */
sealed class JoinChallengeOutcome {
    data class Success(val challengeDocumentId: String) : JoinChallengeOutcome()
    /** No challenge / invite mapping matched the input. */
    data object NotFound : JoinChallengeOutcome()
    /** Resolved but the challenge is no longer active (ended / archived). */
    data object Ended : JoinChallengeOutcome()
    /** Firestore rules blocked read/update — rules must allow joiners to read the challenge doc and update [members]. */
    data object PermissionDenied : JoinChallengeOutcome()
    data object NotSignedIn : JoinChallengeOutcome()
    data class Error(val message: String?) : JoinChallengeOutcome()
}

class ChallengeRepository {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val challengesRef = firestore.collection("challenges")

    /**
     * Top-level invite mapping: `invites/{INVITECODE}` → `{ challengeId, creatorId, createdAt }`.
     *
     * Why a separate collection: Firestore evaluates security rules **against the query** for `list`
     * operations. If `challenges` is restricted to members (`request.auth.uid in resource.data.members`),
     * a `whereEqualTo("inviteCode", X)` query from a non-member is rejected — the join would fail
     * even though the doc with that code exists. A direct `get()` on `invites/{X}` only needs
     * `allow get` permission, which we grant to any signed-in user, so the resolve step always works.
     */
    private val invitesRef = firestore.collection("invites")

    companion object {
        /**
         * Accepts raw Firestore document IDs or pasted share URLs; returns a trimmed document id.
         */
        fun normalizeChallengeDocumentId(raw: String): String {
            var s = raw.trim()
            if (s.isEmpty()) return s
            if (s.contains("://", ignoreCase = true)) {
                try {
                    val uri = android.net.Uri.parse(s)
                    val segs = uri.pathSegments
                    val i = segs.indexOfFirst { it.equals("challenge", ignoreCase = true) }
                    if (i >= 0 && i + 1 < segs.size) {
                        val id = segs[i + 1].trim()
                        if (id.isNotEmpty()) return id
                    }
                    val last = segs.lastOrNull()?.trim()
                    if (!last.isNullOrEmpty() && last.length >= 10) return last
                } catch (_: Exception) { /* fall through */ }
            }
            s = s.removePrefix("#").trim()
            val alnum = Regex("[^a-zA-Z0-9]").replace(s, "")
            if (alnum.length >= 10) return alnum
            return s
        }
    }

    private val currentUid: String?
        get() = auth.currentUser?.uid

    @Suppress("UNCHECKED_CAST")
    private fun safeMap(value: Any?): MutableMap<String, Any> {
        return try {
            (value as? Map<*, *>)
                ?.entries
                ?.associate { it.key.toString() to (it.value ?: "") }
                ?.toMutableMap() ?: mutableMapOf()
        } catch (e: Exception) { mutableMapOf() }
    }

    private fun safeIntMap(value: Any?): MutableMap<String, Int> {
        return try {
            (value as? Map<*, *>)
                ?.entries
                ?.associate { it.key.toString() to ((it.value as? Long)?.toInt() ?: (it.value as? Int) ?: 0) }
                ?.toMutableMap() ?: mutableMapOf()
        } catch (e: Exception) { mutableMapOf() }
    }

    private fun safeBoolMap(value: Any?): MutableMap<String, Boolean> {
        return try {
            (value as? Map<*, *>)
                ?.entries
                ?.associate { it.key.toString() to (it.value as? Boolean ?: false) }
                ?.toMutableMap() ?: mutableMapOf()
        } catch (e: Exception) { mutableMapOf() }
    }

    private fun proofOwnerFromKey(proofKey: String): String =
        proofKey.substringBeforeLast("_", missingDelimiterValue = "").takeIf { it.isNotBlank() } ?: ""

    private fun proofDateFromKey(proofKey: String): String =
        proofKey.substringAfterLast("_", missingDelimiterValue = "").takeIf { it.isNotBlank() } ?: ""

    private fun scoreForMode(
        uid: String,
        mode: ChallengeMode,
        streaks: Map<String, Int>,
        reactionPoints: Map<String, Int>,
    ): Int = when (mode) {
        ChallengeMode.STREAK -> streaks[uid] ?: 0
        ChallengeMode.REACTIONS -> reactionPoints[uid] ?: 0
        ChallengeMode.HYBRID -> ((streaks[uid] ?: 0) * 10) + (reactionPoints[uid] ?: 0)
    }

    private fun leaderboardFromMaps(
        members: List<String>,
        mode: ChallengeMode,
        streaks: Map<String, Int>,
        reactionPoints: Map<String, Int>,
    ): List<Pair<String, Int>> =
        members.map { memberUid -> memberUid to scoreForMode(memberUid, mode, streaks, reactionPoints) }
            .sortedWith(
                compareByDescending<Pair<String, Int>> { it.second }
                    .thenByDescending { reactionPoints[it.first] ?: 0 }
                    .thenByDescending { streaks[it.first] ?: 0 }
                    .thenBy { it.first },
            )

    private fun reactionCountForDay(
        reactions: Map<*, *>,
        reactorUid: String,
        date: String,
        excludingProofKey: String,
    ): Int =
        reactions.count { (proofKeyAny, reactorMapAny) ->
            val proofKey = proofKeyAny?.toString().orEmpty()
            if (proofKey == excludingProofKey || proofDateFromKey(proofKey) != date) return@count false
            val reactorMap = reactorMapAny as? Map<*, *> ?: return@count false
            reactorMap.containsKey(reactorUid)
        }

    private fun resultSnapshotFromMaps(
        members: List<String>,
        mode: ChallengeMode,
        streaks: Map<String, Int>,
        reactionPoints: Map<String, Int>,
        reactions: Map<String, Map<String, List<String>>>,
        proofPhotos: Map<String, Map<String, String>>,
    ): Map<String, Any> {
        val finalBoard = leaderboardFromMaps(members, mode, streaks, reactionPoints)
        val finalLeaderboard = finalBoard.map { (memberUid, score) ->
            mapOf(
                "uid" to memberUid,
                "score" to score,
                "streak" to (streaks[memberUid] ?: 0),
                "reactionPoints" to (reactionPoints[memberUid] ?: 0),
            )
        }
        val topPosts = proofPhotos.flatMap { (ownerUid, photosByDate) ->
            photosByDate.map { (date, photoUrl) ->
                val proofKey = "${ownerUid}_$date"
                val postPoints = reactions[proofKey]
                    ?.values
                    ?.sumOf { reactionList -> reactionList.sumOf { ReactionWeights.pointsFor(it) } }
                    ?: 0
                mapOf(
                    "proofKey" to proofKey,
                    "ownerUid" to ownerUid,
                    "photoUrl" to photoUrl,
                    "reactionPoints" to postPoints,
                )
            }
        }.sortedByDescending { (it["reactionPoints"] as? Int) ?: 0 }.take(3)

        return mapOf(
            "winnerUid" to finalBoard.firstOrNull()?.first.orEmpty(),
            "winnerTitle" to when (mode) {
                ChallengeMode.STREAK -> "Most Consistent"
                ChallengeMode.REACTIONS -> "Crowd Favourite"
                ChallengeMode.HYBRID -> "Hybrid Champion"
            },
            "challengeMode" to mode.name,
            "finalLeaderboard" to finalLeaderboard,
            "topPosts" to topPosts,
            "finalizedAt" to System.currentTimeMillis(),
        )
    }

    /**
     * Create a new challenge and return ids for sharing (long document id + short invite code).
     *
     * Writes are batched atomically:
     *  1. `challenges/{docId}` — the challenge itself.
     *  2. `invites/{INVITECODE}` — public mapping a joiner can [get] without being a member yet.
     */
    suspend fun createChallenge(
        habitName: String,
        stake: String,
        durationDays: Int,
        dailyDeadline: String = "ANYTIME",
        challengeMode: ChallengeMode = ChallengeMode.HYBRID,
    ): CreatedChallenge? {
        val uid = currentUid ?: return null
        auth.currentUser ?: return null
        val now = System.currentTimeMillis()
        val userDoc = firestore.collection("users").document(uid).get().await()
        val memberLabel = UserRepository.publicLabel(
            userDoc.getString("displayName"),
            userDoc.getString("username"),
        )
        val photoUrl = userDoc.getString("photoUrl") ?: ""
        val inviteCode = generateUniqueInviteCode()

        val challengeDocRef = challengesRef.document()
        val inviteDocRef = invitesRef.document(inviteCode)

        val challenge = hashMapOf(
            "creatorId" to uid,
            "habitName" to habitName,
            "stake" to stake,
            "members" to listOf(uid),
            "memberNames" to mapOf(uid to memberLabel),
            "memberPhotos" to mapOf(uid to photoUrl),
            "startDate" to now,
            "endDate" to now + durationDays * 24 * 60 * 60 * 1000L,
            "completions" to emptyMap<String, Any>(),
            "proofPhotos" to emptyMap<String, Any>(),
            "reactions" to emptyMap<String, Any>(),
            "memberReactionPoints" to mapOf(uid to 0),
            "challengeMode" to challengeMode.name,
            "isActive" to true,
            "isArchived" to false,
            "endedAt" to null,
            "archivedReason" to null,
            "leftMembers" to emptyList<String>(),
            "memberStreaks" to emptyMap<String, Int>(),
            "memberXp" to emptyMap<String, Int>(),
            "missedDays" to emptyMap<String, Int>(),
            "inviteCode" to inviteCode,
            "streakFreezeAvailable" to mapOf(uid to true),
            "streakFreezeUsed" to emptyMap<String, Boolean>(),
            "lastMissCheckDate" to java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            "dailyDeadline" to dailyDeadline,
            "resultSnapshot" to null
        )

        val invite = hashMapOf(
            "challengeId" to challengeDocRef.id,
            "inviteCode" to inviteCode,
            "creatorId" to uid,
            "createdAt" to now,
            "isActive" to true
        )

        val batch = firestore.batch()
        batch.set(challengeDocRef, challenge)
        batch.set(inviteDocRef, invite)
        batch.commit().await()

        Log.d(
            "ChallengeRepo",
            "createChallenge: docId=${challengeDocRef.id} inviteCode=$inviteCode " +
                "(also wrote invites/$inviteCode)"
        )
        return CreatedChallenge(documentId = challengeDocRef.id, inviteCode = inviteCode)
    }

    /**
     * Join a challenge using the Firestore **document id**, a pasted app/deep link, or the **6-letter invite code**.
     *
     * The 6-letter code path resolves via `invites/{CODE}` (direct doc `get`), so it works even when
     * the `challenges` collection is restricted to members for `list` queries.
     */
    suspend fun joinChallenge(rawInput: String): JoinChallengeOutcome {
        val uid = currentUid ?: return JoinChallengeOutcome.NotSignedIn
        auth.currentUser ?: return JoinChallengeOutcome.NotSignedIn
        val trimmed = rawInput.trim()
        if (trimmed.isBlank()) {
            Log.d("ChallengeRepo", "joinChallenge: blank input")
            return JoinChallengeOutcome.NotFound
        }

        return try {
            val resolved = resolveChallengeDocumentId(trimmed)
            if (resolved == null) {
                Log.d("ChallengeRepo", "joinChallenge: could not resolve input='$trimmed' to a challenge")
                return JoinChallengeOutcome.NotFound
            }
            Log.d("ChallengeRepo", "joinChallenge: resolved input='$trimmed' -> docId=$resolved")

            val docRef = challengesRef.document(resolved)
            val preCheck = docRef.get().await()
            if (!preCheck.exists()) {
                Log.d("ChallengeRepo", "joinChallenge: docId=$resolved no longer exists")
                return JoinChallengeOutcome.NotFound
            }
            if (preCheck.getBoolean("isActive") != true) {
                Log.d("ChallengeRepo", "joinChallenge: docId=$resolved isActive!=true -> Ended")
                return JoinChallengeOutcome.Ended
            }

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) return@runTransaction
                val userDocRef = firestore.collection("users").document(uid)
                // Firestore requires every read before any write in a transaction.
                val userDoc = transaction.get(userDocRef)
                val members = parseMemberIdsFromFirestore(snapshot.get("members")).toMutableList()
                if (uid !in members) {
                    members.add(uid)
                    transaction.update(docRef, "members", members)

                    val memberLabel = UserRepository.publicLabel(
                        userDoc.getString("displayName"),
                        userDoc.getString("username"),
                    )
                    val photoUrl = userDoc.getString("photoUrl") ?: ""

                    val names = (snapshot.get("memberNames") as? Map<*, *>)?.toMutableMap() ?: mutableMapOf()
                    names[uid] = memberLabel
                    transaction.update(docRef, "memberNames", names)

                    val photos = (snapshot.get("memberPhotos") as? Map<*, *>)?.toMutableMap() ?: mutableMapOf()
                    photos[uid] = photoUrl
                    transaction.update(docRef, "memberPhotos", photos)

                    val streaks = safeIntMap(snapshot.get("memberStreaks"))
                    if (uid !in streaks) streaks[uid] = 0
                    transaction.update(docRef, "memberStreaks", streaks)

                    val xp = safeIntMap(snapshot.get("memberXp"))
                    if (uid !in xp) xp[uid] = 0
                    transaction.update(docRef, "memberXp", xp)

                    val reactionPoints = safeIntMap(snapshot.get("memberReactionPoints"))
                    if (uid !in reactionPoints) reactionPoints[uid] = 0
                    transaction.update(docRef, "memberReactionPoints", reactionPoints)
                }
            }.await()

            val after = docRef.get().await()
            val membersAfter = parseMemberIdsFromFirestore(after.get("members"))
            if (uid in membersAfter) {
                Log.d("ChallengeRepo", "joinChallenge: SUCCESS uid=$uid joined docId=$resolved")
                JoinChallengeOutcome.Success(resolved)
            } else {
                Log.w("ChallengeRepo", "joinChallenge: transaction completed but uid not in members")
                JoinChallengeOutcome.Error("Join did not complete. Try again.")
            }
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                Log.e("ChallengeRepo", "joinChallenge: PERMISSION_DENIED — check Firestore rules", e)
                JoinChallengeOutcome.PermissionDenied
            } else {
                Log.e("ChallengeRepo", "joinChallenge: firestore error ${e.code}", e)
                JoinChallengeOutcome.Error(UserFacingErrors.message(e))
            }
        } catch (e: Exception) {
            Log.e("ChallengeRepo", "joinChallenge: unexpected error", e)
            JoinChallengeOutcome.Error(UserFacingErrors.message(e))
        }
    }

    /** Host adds another user to [challengeId] (e.g. after approving a join request). */
    suspend fun addMemberByCreator(challengeId: String, newMemberUid: String): JoinChallengeOutcome {
        val hostUid = currentUid ?: return JoinChallengeOutcome.NotSignedIn
        if (challengeId.isBlank() || newMemberUid.isBlank()) return JoinChallengeOutcome.NotFound
        return try {
            val docRef = challengesRef.document(challengeId)
            val preCheck = docRef.get().await()
            if (!preCheck.exists()) return JoinChallengeOutcome.NotFound
            if (preCheck.getString("creatorId") != hostUid) {
                return JoinChallengeOutcome.PermissionDenied
            }
            if (preCheck.getBoolean("isActive") != true) return JoinChallengeOutcome.Ended

            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) return@runTransaction
                val userDocRef = firestore.collection("users").document(newMemberUid)
                val userDoc = transaction.get(userDocRef)
                val members = parseMemberIdsFromFirestore(snapshot.get("members")).toMutableList()
                if (newMemberUid !in members) {
                    members.add(newMemberUid)
                    transaction.update(docRef, "members", members)
                    val memberLabel = UserRepository.publicLabel(
                        userDoc.getString("displayName"),
                        userDoc.getString("username"),
                    )
                    val photoUrl = userDoc.getString("photoUrl") ?: ""
                    val names = (snapshot.get("memberNames") as? Map<*, *>)?.toMutableMap() ?: mutableMapOf()
                    names[newMemberUid] = memberLabel
                    transaction.update(docRef, "memberNames", names)
                    val photos = (snapshot.get("memberPhotos") as? Map<*, *>)?.toMutableMap() ?: mutableMapOf()
                    photos[newMemberUid] = photoUrl
                    transaction.update(docRef, "memberPhotos", photos)
                    val streaks = safeIntMap(snapshot.get("memberStreaks"))
                    if (newMemberUid !in streaks) streaks[newMemberUid] = 0
                    transaction.update(docRef, "memberStreaks", streaks)
                    val xp = safeIntMap(snapshot.get("memberXp"))
                    if (newMemberUid !in xp) xp[newMemberUid] = 0
                    transaction.update(docRef, "memberXp", xp)
                    val reactionPoints = safeIntMap(snapshot.get("memberReactionPoints"))
                    if (newMemberUid !in reactionPoints) reactionPoints[newMemberUid] = 0
                    transaction.update(docRef, "memberReactionPoints", reactionPoints)
                }
            }.await()

            val after = docRef.get().await()
            if (newMemberUid in parseMemberIdsFromFirestore(after.get("members"))) {
                JoinChallengeOutcome.Success(challengeId)
            } else {
                JoinChallengeOutcome.Error("Could not add member")
            }
        } catch (e: FirebaseFirestoreException) {
            if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                JoinChallengeOutcome.PermissionDenied
            } else {
                JoinChallengeOutcome.Error(UserFacingErrors.message(e))
            }
        } catch (e: Exception) {
            JoinChallengeOutcome.Error(UserFacingErrors.message(e))
        }
    }

    /**
     * Resolves [rawInput] to a challenge document id. Order:
     *  1. If it looks like a 6-letter invite code, hit `invites/{CODE}` directly (rule-safe).
     *  2. Legacy fallback: `whereEqualTo("inviteCode", code)` on `challenges` (only works if rules allow list).
     *  3. Otherwise normalize as a document id / share URL and try `challenges/{id}`.
     */
    private suspend fun resolveChallengeDocumentId(rawInput: String): String? {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) return null

        // 1. 6-letter invite code path
        val lettersOnly = Regex("[^A-Za-z]").replace(trimmed, "").uppercase()
        val looksLikeInviteCode = lettersOnly.length == 6 && trimmed.length <= 10
        if (looksLikeInviteCode) {
            try {
                val inviteDoc = invitesRef.document(lettersOnly).get().await()
                if (inviteDoc.exists()) {
                    val challengeId = inviteDoc.getString("challengeId")
                    if (!challengeId.isNullOrBlank()) {
                        Log.d("ChallengeRepo", "resolve: invites/$lettersOnly -> $challengeId")
                        return challengeId
                    }
                } else {
                    Log.d("ChallengeRepo", "resolve: invites/$lettersOnly does not exist; trying legacy query")
                }
            } catch (e: FirebaseFirestoreException) {
                Log.w("ChallengeRepo", "resolve: invites/$lettersOnly get failed (${e.code}); trying legacy query")
            } catch (e: Exception) {
                Log.w("ChallengeRepo", "resolve: invites/$lettersOnly get failed; trying legacy query", e)
            }

            // 2. Legacy fallback for challenges created before the invites collection existed
            try {
                val qs = challengesRef
                    .whereEqualTo("inviteCode", lettersOnly)
                    .limit(10)
                    .get()
                    .await()
                val active = qs.documents.filter { it.getBoolean("isActive") == true }
                val match = active.maxByOrNull { it.getLong("startDate") ?: 0L }
                    ?: qs.documents.maxByOrNull { it.getLong("startDate") ?: 0L }
                if (match != null) {
                    Log.d("ChallengeRepo", "resolve: legacy whereEqualTo matched ${match.id}")
                    return match.id
                }
            } catch (e: FirebaseFirestoreException) {
                Log.w("ChallengeRepo", "resolve: legacy query blocked (${e.code}); falling through")
            } catch (e: Exception) {
                Log.w("ChallengeRepo", "resolve: legacy query failed; falling through", e)
            }
            return null
        }

        // 3. Document id / share URL path
        val normalizedKey = normalizeChallengeDocumentId(trimmed).trim()
        if (normalizedKey.isNotEmpty()) {
            try {
                val byId = challengesRef.document(normalizedKey).get().await()
                if (byId.exists()) {
                    Log.d("ChallengeRepo", "resolve: challenges/$normalizedKey exists")
                    return byId.id
                }
            } catch (e: Exception) {
                Log.w("ChallengeRepo", "resolve: challenges/$normalizedKey get failed", e)
            }
        }

        return null
    }

    /**
     * Generates a 6-letter code unique against `invites/{CODE}` (cheap direct `get`, no list query).
     */
    private suspend fun generateUniqueInviteCode(): String {
        repeat(16) {
            val code = List(6) { ('A'..'Z').random() }.joinToString("")
            try {
                val dup = invitesRef.document(code).get().await()
                if (!dup.exists()) return code
            } catch (_: Exception) {
                return code
            }
        }
        return List(6) { ('A'..'Z').random() }.joinToString("")
    }

    /** Leave an existing challenge with an XP penalty */
    suspend fun leaveChallenge(challengeId: String): Boolean {
        val uid = currentUid ?: return false
        return try {
            val docRef = challengesRef.document(challengeId)
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                val userRef = firestore.collection("users").document(uid)
                val userSnapshot = transaction.get(userRef)
                val members = parseMemberIdsFromFirestore(snapshot.get("members")).toMutableList()
                if (uid in members) {
                    members.remove(uid)
                    transaction.update(docRef, "members", members)
                    
                    val leftMembers = (snapshot.get("leftMembers") as? List<*>)?.filterIsInstance<String>()?.toMutableList() ?: mutableListOf()
                    if (uid !in leftMembers) leftMembers.add(uid)
                    transaction.update(docRef, "leftMembers", leftMembers)

                    // Penalize: Deduct 50 XP for abandoning the tribe
                    if (userSnapshot.exists()) {
                        val currentXp = userSnapshot.getLong("xp") ?: 0L
                        val newXp = maxOf(0L, currentXp - 50L)
                        transaction.update(userRef, "xp", newXp)
                        val totalChallengesLeft = userSnapshot.getLong("totalChallengesLeft") ?: 0L
                        transaction.update(userRef, "totalChallengesLeft", totalChallengesLeft + 1)
                    }
                }
            }.await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /** End a challenge early (only creator can do this) */
    suspend fun endChallenge(challengeId: String): Boolean {
        val uid = currentUid ?: return false
        return try {
            finaliseChallenge(challengeId, endedByUid = uid, archivedReason = "creator_ended")
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun finaliseChallenge(
        challengeId: String,
        endedByUid: String? = currentUid,
        archivedReason: String = "completed",
    ): Boolean {
        val uid = endedByUid ?: return false
        val docRef = challengesRef.document(challengeId)
        return try {
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                if (!snapshot.exists()) return@runTransaction
                val creatorId = snapshot.getString("creatorId")
                val isCreator = creatorId == uid
                val isExpired = (snapshot.getLong("endDate") ?: Long.MAX_VALUE) <= System.currentTimeMillis()
                if (!isCreator && !isExpired) return@runTransaction

                val members = parseMemberIdsFromFirestore(snapshot.get("members"))
                val streaks = safeIntMap(snapshot.get("memberStreaks"))
                val reactionPoints = safeIntMap(snapshot.get("memberReactionPoints"))
                val mode = ChallengeMode.fromRaw(snapshot.getString("challengeMode"))
                val result = resultSnapshotFromMaps(
                    members = members,
                    mode = mode,
                    streaks = streaks,
                    reactionPoints = reactionPoints,
                    reactions = parseReactions(snapshot.get("reactions")),
                    proofPhotos = parseProofPhotos(snapshot.get("proofPhotos")),
                )
                transaction.update(docRef, "isActive", false)
                transaction.update(docRef, "isArchived", true)
                transaction.update(docRef, "archivedReason", archivedReason)
                transaction.update(docRef, "endedAt", System.currentTimeMillis())
                transaction.update(docRef, "resultSnapshot", result)
            }.await()
            true
        } catch (e: Exception) {
            Log.e("ChallengeRepo", "finaliseChallenge failed: ${e.message}", e)
            false
        }
    }

    suspend fun markCompletion(
        challengeId: String,
        date: String,
        photoUrl: String,
        lat: Double? = null,
        lng: Double? = null,
        isLate: Boolean = false
    ) {
        val uid = currentUid ?: return
        val docRef = challengesRef.document(challengeId)

        val existing = docRef.get().await()
        val memberIds = parseMemberIdsFromFirestore(existing.get("members"))
        if (uid !in memberIds) {
            Log.w("ChallengeRepo", "markCompletion ignored: user is not a member of this challenge")
            return
        }

        val hour = java.time.LocalTime.now().hour
        val fmt = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
        val yesterdayStr = java.time.LocalDate.now().minusDays(1).format(fmt)
        val todayStr = java.time.LocalDate.now().format(fmt)

        val existingCompletions = safeMap(safeMap(existing.get("completions"))[uid])
        val dailyDeadlineStr = existing.getString("dailyDeadline") ?: "ANYTIME"
        val deadlineH = when (dailyDeadlineStr) {
            "MORNING" -> 12
            "AFTERNOON" -> 18
            "NIGHT" -> 23
            "ANYTIME" -> 24
            else -> 24
        }

        if (isLate) {
            if (hour >= 3) {
                throw IllegalStateException("LATE_WINDOW_CLOSED")
            }
            if (date != yesterdayStr) {
                throw IllegalStateException("LATE_WINDOW_CLOSED")
            }
            val yDone = existingCompletions[date]
            if (yDone is Map<*, *> && yDone["completed"] == true) {
                throw IllegalStateException("ALREADY_POSTED_LATE")
            }
            if (yDone == true) {
                throw IllegalStateException("ALREADY_POSTED_LATE")
            }
        } else {
            if (hour < 3) {
                throw IllegalStateException("EARLY_MORNING_BLOCK")
            }
            if (date != todayStr) {
                throw IllegalStateException("LATE_WINDOW_CLOSED")
            }
            val alreadyDone = existingCompletions[date]
            if (alreadyDone is Map<*, *> && alreadyDone["completed"] == true) {
                throw IllegalStateException("ALREADY_CHECKED_IN")
            }
            if (alreadyDone == true) {
                throw IllegalStateException("ALREADY_CHECKED_IN")
            }
        }

        var newStreak = 0
        var oldRank = 0
        var newRank = 0
        var activityType = "checkin"
        var activityMessage = ""
        var activityDocSuffix = "checkin"

        try {
            firestore.runTransaction { transaction ->
                val snap = transaction.get(docRef)
                val userRef = firestore.collection("users").document(uid)
                val userSnap = transaction.get(userRef)

                val completions = safeMap(snap.get("completions"))
                val userCompletions = safeMap(completions[uid]).toMutableMap()
                val priorMissed = safeIntMap(snap.get("missedDays"))[uid] ?: 0
                val startMs = snap.getLong("startDate") ?: 0L
                val dd = snap.getString("dailyDeadline") ?: "ANYTIME"
                val dHour = when (dd) {
                    "MORNING" -> 12
                    "AFTERNOON" -> 18
                    "NIGHT" -> 23
                    "ANYTIME" -> 24
                    else -> 24
                }
                val nowH = java.time.LocalTime.now().hour

                val streaks = safeMap(snap.get("memberStreaks"))
                val members = parseMemberIdsFromFirestore(snap.get("members"))
                val oldStreaks = members.map { it to ((streaks[it] as? Number)?.toInt() ?: 0) }.sortedByDescending { it.second }
                oldRank = oldStreaks.indexOfFirst { it.first == uid } + 1
                val oldStreakVal = (streaks[uid] as? Number)?.toInt() ?: 0

                val trial = userCompletions.toMutableMap()
                val isAfterDeadline = !isLate && nowH >= dHour

                val xpGain: Int
                if (isLate) {
                    xpGain = 5
                    trial[date] = mapOf(
                        "completed" to true,
                        "timestamp" to System.currentTimeMillis(),
                        "photoUrl" to photoUrl,
                        "lat" to (lat ?: 0.0),
                        "lng" to (lng ?: 0.0),
                        "isLate" to true,
                        "isAfterDeadline" to false,
                        "xpEarned" to xpGain,
                        "priorMissed" to priorMissed
                    )
                    newStreak = oldStreakVal
                    streaks[uid] = oldStreakVal
                    activityType = "late_checkin"
                    activityDocSuffix = "late_checkin"
                } else {
                    trial[date] = mapOf("completed" to true)
                    newStreak = calculateCurrentStreak(trial, date, startMs)
                    val streakBonus = when {
                        newStreak >= 21 -> 20
                        newStreak >= 14 -> 15
                        newStreak >= 7 -> 10
                        newStreak >= 3 -> 5
                        else -> 0
                    }
                    xpGain = if (isAfterDeadline) 8 else (10 + streakBonus)
                    trial[date] = mapOf(
                        "completed" to true,
                        "timestamp" to System.currentTimeMillis(),
                        "photoUrl" to photoUrl,
                        "lat" to (lat ?: 0.0),
                        "lng" to (lng ?: 0.0),
                        "isLate" to false,
                        "isAfterDeadline" to isAfterDeadline,
                        "xpEarned" to xpGain,
                        "priorMissed" to priorMissed
                    )
                    streaks[uid] = newStreak
                    activityType = "checkin"
                    activityDocSuffix = "checkin"
                }

                completions[uid] = trial
                transaction.update(docRef, "completions", completions)

                val proofs = safeMap(snap.get("proofPhotos"))
                val userProofs = safeMap(proofs[uid])
                userProofs[date] = photoUrl
                proofs[uid] = userProofs
                transaction.update(docRef, "proofPhotos", proofs)

                transaction.update(docRef, "memberStreaks", streaks)

                val newStreaks = members.map { it to ((streaks[it] as? Number)?.toInt() ?: 0) }.sortedByDescending { it.second }
                newRank = newStreaks.indexOfFirst { it.first == uid } + 1

                val missed = safeMap(snap.get("missedDays"))
                missed[uid] = 0
                transaction.update(docRef, "missedDays", missed)

                val memberXp = safeMap(snap.get("memberXp"))
                val currentXp = (memberXp[uid] as? Number)?.toLong() ?: 0L
                memberXp[uid] = (currentXp + xpGain).toInt()
                transaction.update(docRef, "memberXp", memberXp)

                val globalXp = userSnap.getLong("xp") ?: 0L
                transaction.update(userRef, "xp", globalXp + xpGain)

                val rankChanges = safeIntMap(snap.get("rankChanges"))
                rankChanges[uid] = oldRank - newRank
                transaction.update(docRef, "rankChanges", rankChanges)
            }.await()
        } catch (e: FirebaseFirestoreException) {
            Log.e("ChallengeRepo", "Transaction failed: ${e.code} ${e.message}")
            throw e
        } catch (e: Exception) {
            Log.e("ChallengeRepo", "markCompletion crashed: ${e.message}", e)
            throw e
        }

        try {
            val userDoc = firestore.collection("users").document(uid).get().await()
            val username = userDoc.getString("username") ?: "UnknownRival"

            activityMessage = when {
                isLate -> "@$username posted late proof — still alive"
                hour >= deadlineH && !isLate -> "@$username checked in after the deadline — cutting it close"
                else -> "@$username checked in — Day $newStreak"
            }

            val docId = "${activityDocSuffix}_${uid}_$date"
            upsertActivityDocument(
                challengeId = challengeId,
                activityDocId = docId,
                type = activityType,
                message = activityMessage,
                proofPhotoUrl = photoUrl,
                metadata = mapOf("completionDate" to date)
            )

            if (newRank < oldRank && newRank == 1) {
                upsertActivityDocument(
                    challengeId = challengeId,
                    activityDocId = "tooklead_${uid}_$date",
                    type = "took_lead",
                    message = "@$username just took the lead!",
                    metadata = mapOf("completionDate" to date)
                )
            } else if (newRank < oldRank) {
                upsertActivityDocument(
                    challengeId = challengeId,
                    activityDocId = "rankup_${uid}_$date",
                    type = "rank_up",
                    message = "@$username jumped to #$newRank on the leaderboard",
                    metadata = mapOf("completionDate" to date)
                )
            }

            if (!isLate && newStreak in listOf(3, 7, 14, 21)) {
                val title = when (newStreak) {
                    3 -> "3 Day Starter"; 7 -> "7 Day Warrior"
                    14 -> "14 Day Machine"; 21 -> "21 Day Legend"; else -> ""
                }
                upsertActivityDocument(
                    challengeId = challengeId,
                    activityDocId = "milestone_${uid}_$date",
                    type = "milestone",
                    message = "@$username hit a $newStreak-day streak — $title!",
                    metadata = mapOf("completionDate" to date)
                )
            }
        } catch (e: Exception) {
            Log.e("ChallengeRepo", "Activity write failed: ${e.message}")
        }
    }

    /**
     * Removes today's check-in for the signed-in user so they can post a new proof the same day.
     * Streak / XP / missed counts are restored using [xpEarned] and [priorMissed] stored on the completion.
     * Nightly miss logic still applies if they never re-check in before the day rolls over.
     */
    suspend fun deleteTodayCheckIn(challengeId: String, date: String) {
        val uid = currentUid ?: return
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        if (date != today) {
            Log.d("ChallengeRepo", "deleteTodayCheckIn: only today's entry can be removed (got $date)")
            return
        }
        val hour = java.time.LocalTime.now().hour
        if (hour < 3) {
            Log.d("ChallengeRepo", "deleteTodayCheckIn: not available before 3 AM")
            return
        }

        val docRef = challengesRef.document(challengeId)
        val pre = docRef.get().await()
        val memberIds = parseMemberIdsFromFirestore(pre.get("members"))
        if (uid !in memberIds) return

        try {
            firestore.runTransaction { transaction ->
                val snap = transaction.get(docRef)
                val userRef = firestore.collection("users").document(uid)
                val userSnap = transaction.get(userRef)

                val members = parseMemberIdsFromFirestore(snap.get("members"))
                if (uid !in members) return@runTransaction

                val completions = safeMap(snap.get("completions"))
                val userCompletions = safeMap(completions[uid]).toMutableMap()
                val dayEntry = userCompletions[date] ?: return@runTransaction
                val completed = when (dayEntry) {
                    is Boolean -> dayEntry
                    is Map<*, *> -> dayEntry["completed"] == true
                    else -> false
                }
                if (!completed) return@runTransaction

                val dayMap = dayEntry as? Map<*, *>
                val xpEarned = (dayMap?.get("xpEarned") as? Number)?.toLong() ?: 0L
                val priorMissed = (dayMap?.get("priorMissed") as? Number)?.toInt() ?: 0

                userCompletions.remove(date)
                completions[uid] = userCompletions
                transaction.update(docRef, "completions", completions)

                val proofs = safeMap(snap.get("proofPhotos"))
                val userProofs = safeMap(proofs[uid]).toMutableMap()
                userProofs.remove(date)
                proofs[uid] = userProofs
                transaction.update(docRef, "proofPhotos", proofs)

                val reactions = (snap.get("reactions") as? Map<*, *>)?.toMutableMap() ?: mutableMapOf()
                reactions.remove("latest")
                reactions.remove("${uid}_$date")
                transaction.update(docRef, "reactions", reactions)

                val startMs = snap.getLong("startDate") ?: 0L
                val newStreak = calculateCurrentStreak(userCompletions, date, startMs)
                val streaks = safeMap(snap.get("memberStreaks"))
                val oldStreakSnap = safeIntMap(snap.get("memberStreaks"))
                val oldRankDel = members
                    .map { m -> m to (oldStreakSnap[m] ?: 0) }
                    .sortedByDescending { it.second }
                    .let { list -> list.indexOfFirst { it.first == uid } + 1 }

                streaks[uid] = newStreak
                transaction.update(docRef, "memberStreaks", streaks)

                val newRankDel = members
                    .map { m -> m to ((streaks[m] as? Number)?.toInt() ?: 0) }
                    .sortedByDescending { it.second }
                    .let { list -> list.indexOfFirst { it.first == uid } + 1 }

                val missed = safeMap(snap.get("missedDays"))
                missed[uid] = priorMissed
                transaction.update(docRef, "missedDays", missed)

                val memberXp = safeMap(snap.get("memberXp"))
                val curXp = (memberXp[uid] as? Number)?.toLong() ?: 0L
                memberXp[uid] = maxOf(0L, curXp - xpEarned).toInt()
                transaction.update(docRef, "memberXp", memberXp)

                val globalXp = userSnap.getLong("xp") ?: 0L
                transaction.update(userRef, "xp", maxOf(0L, globalXp - xpEarned))

                val rankChanges = safeIntMap(snap.get("rankChanges"))
                rankChanges[uid] = oldRankDel - newRankDel
                transaction.update(docRef, "rankChanges", rankChanges)
            }.await()
        } catch (e: FirebaseFirestoreException) {
            Log.e("ChallengeRepo", "deleteTodayCheckIn failed: ${e.code} ${e.message}", e)
            throw e
        } catch (e: Exception) {
            Log.e("ChallengeRepo", "deleteTodayCheckIn crashed: ${e.message}", e)
            throw e
        }

        deleteOptionalActivityDoc(challengeId, "checkin_${uid}_$date")
        deleteOptionalActivityDoc(challengeId, "tooklead_${uid}_$date")
        deleteOptionalActivityDoc(challengeId, "rankup_${uid}_$date")
        deleteOptionalActivityDoc(challengeId, "milestone_${uid}_$date")
    }

    /**
     * Current streak ending at [anchorDate]: skips incomplete trailing days (e.g. after deleting today)
     * and counts consecutive completed days back to challenge start.
     */
    fun calculateCurrentStreak(
        userCompletions: Map<String, Any>,
        anchorDate: String,
        challengeStartMillis: Long
    ): Int {
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val zone = ZoneId.systemDefault()
        val startDay = if (challengeStartMillis > 0) {
            Instant.ofEpochMilli(challengeStartMillis).atZone(zone).toLocalDate()
        } else {
            LocalDate.parse(anchorDate, formatter).minusYears(10)
        }

        fun isComplete(day: LocalDate): Boolean {
            val key = day.format(formatter)
            val dayData = userCompletions[key] ?: return false
            return when (dayData) {
                is Boolean -> dayData
                is Map<*, *> -> dayData["completed"] == true
                else -> false
            }
        }

        var anchor = LocalDate.parse(anchorDate, formatter)
        while (!isComplete(anchor) && anchor.isAfter(startDay)) {
            anchor = anchor.minusDays(1)
        }
        if (!isComplete(anchor)) return 0

        var streak = 0
        var d = anchor
        while (!d.isBefore(startDay) && isComplete(d)) {
            streak++
            d = d.minusDays(1)
        }
        return streak
    }

    private suspend fun upsertActivityDocument(
        challengeId: String,
        activityDocId: String,
        type: String,
        message: String,
        proofPhotoUrl: String? = null,
        metadata: Map<String, Any> = emptyMap()
    ) {
        val uid = currentUid ?: return
        val userDoc = firestore.collection("users").document(uid).get().await()
        val payload = mapOf(
            "type" to type,
            "actorId" to uid,
            "actorUsername" to (userDoc.getString("username") ?: "UnknownRival"),
            "actorPhotoURL" to (userDoc.getString("photoUrl") ?: ""),
            "message" to message,
            "proofPhotoUrl" to (proofPhotoUrl ?: ""),
            "timestamp" to System.currentTimeMillis(),
            "metadata" to metadata
        )
        firestore.collection("challenges")
            .document(challengeId)
            .collection("activity")
            .document(activityDocId)
            .set(payload)
            .await()
    }

    private suspend fun deleteOptionalActivityDoc(challengeId: String, activityDocId: String) {
        try {
            firestore.collection("challenges")
                .document(challengeId)
                .collection("activity")
                .document(activityDocId)
                .delete()
                .await()
        } catch (_: Exception) {
            // Missing doc is fine
        }
    }

    /** React to a proof photo */
    suspend fun addReaction(challengeId: String, proofKey: String, emoji: String): Int {
        val uid = currentUid ?: return 0
        if (!ReactionWeights.isAllowed(emoji)) return 0
        val proofOwnerUid = proofOwnerFromKey(proofKey)
        val proofDate = proofDateFromKey(proofKey)
        if (proofOwnerUid.isBlank() || proofDate.isBlank() || proofOwnerUid == uid) return 0
        val docRef = challengesRef.document(challengeId)
        val pre = docRef.get().await()
        val preMembers = parseMemberIdsFromFirestore(pre.get("members"))
        if (uid !in preMembers || proofOwnerUid !in preMembers) return 0
        var pointDelta = 0
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(docRef)
            if (snapshot.getBoolean("isActive") != true) return@runTransaction
            val activityRef = docRef.collection("activity")
                .document("reaction_${uid}_${proofKey}")
            val userRef = firestore.collection("users").document(uid)
            val userSnap = transaction.get(userRef)
            val reactions = (snapshot.get("reactions") as? Map<*, *>)?.toMutableMap() ?: mutableMapOf()
            val proofReactions = (reactions[proofKey] as? Map<*, *>)?.toMutableMap() ?: mutableMapOf()
            val existingReaction = (proofReactions[uid] as? List<*>)?.filterIsInstance<String>()?.firstOrNull()
            val alreadyReactedToday = reactionCountForDay(
                reactions = reactions,
                reactorUid = uid,
                date = proofDate,
                excludingProofKey = proofKey,
            )
            if (existingReaction == null && alreadyReactedToday >= 10) return@runTransaction

            val oldPoints = existingReaction?.let { ReactionWeights.pointsFor(it) } ?: 0
            val newPoints = ReactionWeights.pointsFor(emoji)
            pointDelta = newPoints - oldPoints
            proofReactions[uid] = listOf(emoji)
            reactions[proofKey] = proofReactions
            transaction.update(docRef, "reactions", reactions)

            if (pointDelta != 0) {
                val reactionPoints = safeIntMap(snapshot.get("memberReactionPoints"))
                reactionPoints[proofOwnerUid] = ((reactionPoints[proofOwnerUid] ?: 0) + pointDelta).coerceAtLeast(0)
                transaction.update(docRef, "memberReactionPoints", reactionPoints)

                val members = parseMemberIdsFromFirestore(snapshot.get("members"))
                val mode = ChallengeMode.fromRaw(snapshot.getString("challengeMode"))
                val streaks = safeIntMap(snapshot.get("memberStreaks"))
                val oldBoard = leaderboardFromMaps(
                    members = members,
                    mode = mode,
                    streaks = streaks,
                    reactionPoints = safeIntMap(snapshot.get("memberReactionPoints")),
                )
                val newBoard = leaderboardFromMaps(
                    members = members,
                    mode = mode,
                    streaks = streaks,
                    reactionPoints = reactionPoints,
                )
                val rankChanges = safeIntMap(snapshot.get("rankChanges"))
                members.forEach { memberUid ->
                    val oldRank = oldBoard.indexOfFirst { it.first == memberUid } + 1
                    val newRank = newBoard.indexOfFirst { it.first == memberUid } + 1
                    if (oldRank > 0 && newRank > 0) rankChanges[memberUid] = oldRank - newRank
                }
                transaction.update(docRef, "rankChanges", rankChanges)
            }

            val actorName = userSnap.getString("username") ?: "UnknownRival"
            transaction.set(
                activityRef,
                mapOf(
                    "type" to "reaction",
                    "actorId" to uid,
                    "actorUsername" to actorName,
                    "actorPhotoURL" to (userSnap.getString("photoUrl") ?: ""),
                    "message" to "@$actorName reacted to a proof",
                    "proofPhotoUrl" to "",
                    "timestamp" to System.currentTimeMillis(),
                    "metadata" to mapOf(
                        "proofKey" to proofKey,
                        "proofOwnerUid" to proofOwnerUid,
                        "reaction" to emoji,
                        "points" to ReactionWeights.pointsFor(emoji),
                        "pointDelta" to pointDelta,
                        "completionDate" to proofDate,
                    ),
                ),
            )
        }.await()
        return pointDelta
    }

    suspend fun getReactionLeaderboard(challengeId: String): List<Pair<String, Int>> {
        val doc = challengesRef.document(challengeId).get().await()
        val members = parseMemberIdsFromFirestore(doc.get("members"))
        val reactionPoints = safeIntMap(doc.get("memberReactionPoints"))
        return members.map { it to (reactionPoints[it] ?: 0) }
            .sortedWith(compareByDescending<Pair<String, Int>> { it.second }.thenBy { it.first })
    }

    /** Update user's name and photo across all challenges they are in */
    suspend fun updateMemberIdentity(uid: String, name: String, photoUrl: String?) {
        val challenges = challengesRef.whereArrayContains("members", uid).get().await()
        challenges.documents.forEach { doc ->
            val docRef = doc.reference
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(docRef)
                
                val names = (snapshot.get("memberNames") as? Map<*, *>)?.toMutableMap() ?: mutableMapOf()
                names[uid] = name
                transaction.update(docRef, "memberNames", names)
                
                if (photoUrl != null) {
                    val photos = (snapshot.get("memberPhotos") as? Map<*, *>)?.toMutableMap() ?: mutableMapOf()
                    photos[uid] = photoUrl
                    transaction.update(docRef, "memberPhotos", photos)
                }
            }.await()
        }
    }

    /** Real-time listener for challenges where [uid] is in `members` (must match signed-in user for security). */
    fun observeMyChallengesForUser(uid: String): Flow<List<Challenge>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val listener: ListenerRegistration = challengesRef
            .whereArrayContains("members", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("ChallengeRepo", "observeMyChallenges listener error: ${error.message}", error)
                    return@addSnapshotListener
                }
                val challenges = snapshot?.documents?.mapNotNull { doc ->
                    fetchChallengeFromDoc(doc)
                }?.filter { challenge ->
                    uid in challenge.members
                } ?: emptyList()
                trySend(challenges)
            }

        awaitClose { listener.remove() }
    }

    /** @see observeMyChallengesForUser — uses the currently signed-in Firebase user. */
    fun observeMyChallenges(): Flow<List<Challenge>> {
        val uid = currentUid ?: return flowOf(emptyList())
        return observeMyChallengesForUser(uid)
    }

    /** Fetch a single challenge by ID */
    suspend fun fetchChallenge(challengeId: String): Challenge? {
        return try {
            val doc = challengesRef.document(challengeId).get().await()
            if (!doc.exists()) return null
            fetchChallengeFromDoc(doc)
        } catch (e: Exception) { null }
    }

    suspend fun checkMissedDays(challengeId: String) {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val yesterdayDate = java.time.LocalDate.now().minusDays(1)
        val yesterday = yesterdayDate.format(formatter)

        val docRef = challengesRef.document(challengeId)
        val snap = docRef.get().await()

        val lastCheck = snap.getString("lastMissCheckDate") ?: ""
        if (lastCheck == yesterday) return

        val startDateLong = snap.getLong("startDate") ?: 0L
        val startDate = java.time.Instant.ofEpochMilli(startDateLong).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        if (yesterdayDate.isBefore(startDate)) return

        val members = parseMemberIdsFromFirestore(snap.get("members")).ifEmpty { return }
        val completions = parseCompletions(snap.get("completions"))
        val missed = safeIntMap(snap.get("missedDays")).toMutableMap()
        val freezeAvail = safeBoolMap(snap.get("streakFreezeAvailable")).toMutableMap()
        val freezeUsed = safeBoolMap(snap.get("streakFreezeUsed")).toMutableMap()
        val frozeMembers = mutableListOf<String>()

        members.forEach { memberUid ->
            val userComps = completions[memberUid] ?: emptyMap()
            val didComplete = when (val v = userComps[yesterday]) {
                is Boolean -> v
                is Map<*, *> -> v["completed"] == true
                else -> false
            }
            if (!didComplete) {
                // Streak Freeze check
                val hasFreeze = freezeAvail.getOrDefault(memberUid, true) && !(freezeUsed[memberUid] ?: false)
                if (hasFreeze) {
                    freezeUsed[memberUid] = true
                    freezeAvail[memberUid] = false
                    frozeMembers.add(memberUid)
                } else {
                    val count = (missed[memberUid] ?: 0) + 1
                    missed[memberUid] = count
                    // Do not zero streaks in Firestore — display uses completion history;
                    // wiping memberStreaks was unfair and hard to recover from.
                }
            }
        }

        try {
            firestore.runTransaction { transaction ->
                transaction.update(docRef, "missedDays", missed)
                transaction.update(docRef, "lastMissCheckDate", yesterday)
                transaction.update(docRef, "streakFreezeUsed", freezeUsed)
                transaction.update(docRef, "streakFreezeAvailable", freezeAvail)
            }.await()
        } catch (e: Exception) {
            Log.e("ChallengeRepo", "checkMissedDays failed: ${e.message}", e)
            return
        }

        // Post-transaction activity writes
        frozeMembers.forEach { memberUid ->
            try {
                val userDoc = firestore.collection("users").document(memberUid).get().await()
                val username = userDoc.getString("username") ?: "UnknownRival"
                writeActivity(challengeId, "streak_freeze", "@$username's Streak Shield activated — still alive!")
            } catch (_: Exception) {}
        }
    }

    /**
     * One-time Firestore repair: recompute [memberStreaks] from [completions], optionally restore
     * [leftMembers] into [members]. Use via [scripts/recover-challenges.mjs] or support tooling.
     */
    suspend fun repairChallengeIntegrity(
        challengeId: String,
        restoreEliminatedMembers: Boolean = true,
        reactivateIfEndDateOpen: Boolean = false,
    ): Boolean {
        val docRef = challengesRef.document(challengeId)
        val snap = docRef.get().await()
        if (!snap.exists()) return false

        val startDate = snap.getLong("startDate") ?: 0L
        val endDate = snap.getLong("endDate") ?: 0L
        val completions = parseCompletions(snap.get("completions"))
        val members = parseMemberIdsFromFirestore(snap.get("members")).toMutableList()
        val left = (snap.get("leftMembers") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
        val allUids = (members + left).distinct()

        val recomputed = allUids.associateWith { uid ->
            val userCompletions = completions[uid] ?: emptyMap()
            val anchor = ChallengeStreakCalculator.anchorDateFor(userCompletions)
            ChallengeStreakCalculator.currentStreak(userCompletions, anchor, startDate)
        }.toMutableMap()

        val updates = mutableMapOf<String, Any>(
            "memberStreaks" to recomputed,
        )

        if (restoreEliminatedMembers && left.isNotEmpty()) {
            members.addAll(left.filter { it !in members })
            updates["members"] = members
            updates["leftMembers"] = emptyList<String>()
        }

        if (reactivateIfEndDateOpen && endDate > System.currentTimeMillis()) {
            updates["isActive"] = true
            updates["isArchived"] = false
            updates.remove("endedAt")
            updates["archivedReason"] = "reopened"
        }

        docRef.update(updates).await()
        return true
    }

    suspend fun reportProof(challengeId: String, proofKey: String, reason: String = "inappropriate") {
        val uid = currentUid ?: return
        if (proofKey.isBlank()) return
        val reportId = "${uid}_${challengeId}_${proofKey.hashCode()}"
        try {
            firestore.collection("reports").document(reportId).set(
                mapOf(
                    "reporterUid" to uid,
                    "challengeId" to challengeId,
                    "proofKey" to proofKey,
                    "reason" to reason,
                    "createdAt" to System.currentTimeMillis(),
                ),
            ).await()
        } catch (e: Exception) {
            Log.w("ChallengeRepo", "reportProof failed: ${e.message}")
            throw e
        }
    }

    fun observeArchivedChallengesForUser(uid: String): Flow<List<Challenge>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(emptyList())
            close()
            return@callbackFlow
        }

        val mergeLock = Any()
        var endedChallenges: List<Challenge> = emptyList()
        var leftChallenges: List<Challenge> = emptyList()

        fun mergeAndEmit() {
            val merged = (endedChallenges + leftChallenges)
                .distinctBy { it.id }
                .sortedByDescending { it.endedAt ?: it.endDate }
            trySend(merged)
        }

        val endedListener = challengesRef
            .whereArrayContains("members", uid)
            .whereEqualTo("isArchived", true)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { fetchChallengeFromDoc(it) }
                    ?.filter { uid in it.members } ?: emptyList()
                synchronized(mergeLock) {
                    endedChallenges = list
                    mergeAndEmit()
                }
            }

        val leftListener = challengesRef
            .whereArrayContains("leftMembers", uid)
            .addSnapshotListener { snap, _ ->
                val list = snap?.documents?.mapNotNull { fetchChallengeFromDoc(it) }
                    ?.filter { uid in it.leftMembers } ?: emptyList()
                synchronized(mergeLock) {
                    leftChallenges = list
                    mergeAndEmit()
                }
            }

        awaitClose {
            endedListener.remove()
            leftListener.remove()
        }
    }

    fun observeArchivedChallenges(): Flow<List<Challenge>> {
        val uid = currentUid ?: return flowOf(emptyList())
        return observeArchivedChallengesForUser(uid)
    }

    suspend fun writeActivity(
        challengeId: String,
        type: String,
        message: String,
        proofPhotoUrl: String? = null,
        metadata: Map<String, Any> = emptyMap()
    ) {
        val uid = currentUid ?: return
        val userDoc = firestore.collection("users").document(uid).get().await()
        
        firestore.collection("challenges")
            .document(challengeId)
            .collection("activity")
            .add(mapOf(
                "type" to type,
                "actorId" to uid,
                "actorUsername" to (userDoc.getString("username") ?: "UnknownRival"),
                "actorPhotoURL" to (userDoc.getString("photoUrl") ?: ""),
                "message" to message,
                "proofPhotoUrl" to (proofPhotoUrl ?: ""),
                "timestamp" to System.currentTimeMillis(),
                "metadata" to metadata
            )).await()
    }

    fun observeActivity(challengeId: String): Flow<List<com.saintnico.verdlyhabits.data.model.ActivityItem>> = callbackFlow {
        val listener = firestore
            .collection("challenges").document(challengeId).collection("activity")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(50)
            .addSnapshotListener { snap, _ ->
                val items = snap?.documents?.mapNotNull { doc ->
                    com.saintnico.verdlyhabits.data.model.ActivityItem(
                        id = doc.id,
                        type = doc.getString("type") ?: "",
                        actorId = doc.getString("actorId") ?: "",
                        actorUsername = doc.getString("actorUsername") ?: "UnknownRival",
                        actorPhotoURL = doc.getString("actorPhotoURL") ?: "",
                        message = doc.getString("message") ?: "",
                        proofPhotoUrl = doc.getString("proofPhotoUrl"),
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        metadata = (doc.get("metadata") as? Map<String, Any>) ?: emptyMap()
                    )
                } ?: emptyList()
                trySend(items)
            }
        awaitClose { listener.remove() }
    }

    /** Supports array-of-strings or legacy map keys as member UIDs. */
    internal fun parseMemberIdsFromFirestore(value: Any?): List<String> {
        return when (value) {
            null -> emptyList()
            is List<*> -> value.mapNotNull { el ->
                when (el) {
                    is String -> el.trim().takeIf { it.isNotEmpty() }
                    else -> null
                }
            }
            is Map<*, *> -> value.keys.mapNotNull { k -> k?.toString()?.trim()?.takeIf { it.isNotEmpty() } }
            else -> emptyList()
        }
    }

    private fun fetchChallengeFromDoc(doc: com.google.firebase.firestore.DocumentSnapshot): Challenge? {
        return try {
            Challenge(
                id = doc.id,
                creatorId = doc.getString("creatorId") ?: "",
                habitName = doc.getString("habitName") ?: "",
                stake = doc.getString("stake") ?: "",
                members = parseMemberIdsFromFirestore(doc.get("members")),
                memberNames = (doc.get("memberNames") as? Map<*, *>)?.map { it.key.toString() to it.value.toString() }?.toMap() ?: emptyMap(),
                memberPhotos = (doc.get("memberPhotos") as? Map<*, *>)?.map { it.key.toString() to it.value.toString() }?.toMap() ?: emptyMap(),
                startDate = doc.getLong("startDate") ?: 0L,
                endDate = doc.getLong("endDate") ?: 0L,
                completions = parseCompletions(doc.get("completions")),
                proofPhotos = parseProofPhotos(doc.get("proofPhotos")),
                reactions = parseReactions(doc.get("reactions")),
                memberReactionPoints = safeIntMap(doc.get("memberReactionPoints")),
                challengeMode = ChallengeMode.fromRaw(doc.getString("challengeMode")),
                isActive = doc.getBoolean("isActive") ?: true,
                isArchived = doc.getBoolean("isArchived") ?: false,
                endedAt = doc.getLong("endedAt"),
                archivedReason = doc.getString("archivedReason"),
                resultSnapshot = parseResultSnapshot(doc.get("resultSnapshot")),
                leftMembers = (doc.get("leftMembers") as? List<*>)?.filterIsInstance<String>() ?: emptyList(),
                memberStreaks = safeIntMap(doc.get("memberStreaks")),
                memberXp = safeIntMap(doc.get("memberXp")),
                missedDays = safeIntMap(doc.get("missedDays")),
                inviteCode = doc.getString("inviteCode") ?: "",
                streakFreezeAvailable = safeBoolMap(doc.get("streakFreezeAvailable")),
                streakFreezeUsed = safeBoolMap(doc.get("streakFreezeUsed")),
                rankChanges = safeIntMap(doc.get("rankChanges")),
                dailyDeadline = doc.getString("dailyDeadline") ?: "ANYTIME"
            )
        } catch (e: Exception) { null }
    }


    private fun parseCompletions(data: Any?): Map<String, Map<String, Any>> {
        val raw = data as? Map<*, *> ?: return emptyMap()
        return raw.map { (uid, dates) ->
            uid.toString() to (dates as? Map<*, *>)?.map { (date, completionObj) ->
                date.toString() to (completionObj ?: false)
            }?.toMap().orEmpty()
        }.toMap()
    }

    private fun parseProofPhotos(data: Any?): Map<String, Map<String, String>> {
        val raw = data as? Map<*, *> ?: return emptyMap()
        return raw.map { (uid, photos) ->
            uid.toString() to (photos as? Map<*, *>)?.map { (date, url) ->
                date.toString() to url.toString()
            }?.toMap().orEmpty()
        }.toMap()
    }

    private fun parseReactions(data: Any?): Map<String, Map<String, List<String>>> {
        val raw = data as? Map<*, *> ?: return emptyMap()
        return raw.map { (key, reactorMap) ->
            key.toString() to (reactorMap as? Map<*, *>)?.map { (uid, emojis) ->
                uid.toString() to (emojis as? List<*>)?.filterIsInstance<String>().orEmpty()
            }?.toMap().orEmpty()
        }.toMap()
    }

    private fun parseResultSnapshot(data: Any?): ChallengeResultSnapshot? {
        val raw = data as? Map<*, *> ?: return null
        val standings = (raw["finalLeaderboard"] as? List<*>)?.mapNotNull { item ->
            val row = item as? Map<*, *> ?: return@mapNotNull null
            ChallengeResultStanding(
                uid = row["uid"]?.toString().orEmpty(),
                score = (row["score"] as? Number)?.toInt() ?: 0,
                streak = (row["streak"] as? Number)?.toInt() ?: 0,
                reactionPoints = (row["reactionPoints"] as? Number)?.toInt() ?: 0,
            )
        }.orEmpty()
        val topPosts = (raw["topPosts"] as? List<*>)?.mapNotNull { item ->
            val row = item as? Map<*, *> ?: return@mapNotNull null
            ChallengeTopPost(
                proofKey = row["proofKey"]?.toString().orEmpty(),
                ownerUid = row["ownerUid"]?.toString().orEmpty(),
                photoUrl = row["photoUrl"]?.toString().orEmpty(),
                reactionPoints = (row["reactionPoints"] as? Number)?.toInt() ?: 0,
            )
        }.orEmpty()
        return ChallengeResultSnapshot(
            winnerUid = raw["winnerUid"]?.toString().orEmpty(),
            winnerTitle = raw["winnerTitle"]?.toString().orEmpty(),
            challengeMode = ChallengeMode.fromRaw(raw["challengeMode"]?.toString()),
            finalLeaderboard = standings,
            topPosts = topPosts,
            finalizedAt = (raw["finalizedAt"] as? Number)?.toLong() ?: 0L,
        )
    }
}
