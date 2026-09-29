package com.saintnico.verdlyhabits.data.remote.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.SetOptions
import com.saintnico.verdlyhabits.data.model.MembershipTier
import com.saintnico.verdlyhabits.data.model.PublicUserProfile
import com.saintnico.verdlyhabits.engine.GamificationEngine
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

private fun DocumentSnapshot.readNumberInt(field: String): Int {
    val raw = get(field) ?: return 0
    return when (raw) {
        is Number -> raw.toInt()
        else -> 0
    }
}

private fun DocumentSnapshot.readMillis(field: String): Long {
    return when (val raw = get(field)) {
        is Number -> raw.toLong()
        is com.google.firebase.Timestamp -> raw.toDate().time
        else -> 0L
    }
}

private fun DocumentSnapshot.toPublicUserProfile(): PublicUserProfile? {
    if (!exists()) return null
    val xp = readNumberInt("xp")
    return PublicUserProfile(
        uid = id,
        displayName = getString("displayName").orEmpty(),
        username = getString("username").orEmpty(),
        photoUrl = getString("photoUrl"),
        bio = getString("bio").orEmpty(),
        motto = getString("motto").orEmpty(),
        favoritePlant = getString("favoritePlant").orEmpty(),
        profileAccent = getString("profileAccent") ?: "sage",
        level = GamificationEngine.calculateLevel(xp),
        xp = xp,
        showRecentProof = getBoolean("showRecentProof") ?: true,
        equippedTitleId = getString("equippedTitleId"),
        equippedTitleLabel = getString("equippedTitleLabel"),
        titleGlowId = getString("titleGlowId"),
        profileFrameId = getString("profileFrameId"),
        grovePlantId = getString("grovePlantId"),
        oathChipId = getString("oathChipId"),
        pinnedTrophyLabel = getString("pinnedTrophyLabel"),
        pinnedTrophyUntil = getLong("pinnedTrophyUntil") ?: 0L,
        membershipTier = MembershipTier.fromFirestore(getString("membershipTier")),
        lastPlantedAt = readMillis("lastPlantedAt"),
        lastActiveAt = readMillis("lastActiveAt"),
        shareLastActive = getBoolean("shareLastActive") ?: true,
        lastPlantedDay = getString("lastPlantedDay"),
        plantedCountToday = readNumberInt("plantedCountToday"),
        weeklyPlantDays = readNumberInt("weeklyPlantDays"),
        weeklyPlantWeekKey = getString("weeklyPlantWeekKey"),
        currentStreakPublic = readNumberInt("currentStreakPublic"),
        standInOpenGrove = getBoolean("standInOpenGrove") ?: false,
        openGroveWeekRank = readNumberInt("openGroveWeekRank"),
        openGrovePercentile = readNumberInt("openGrovePercentile"),
        openGroveWeekKey = getString("openGroveWeekKey"),
        groveLeadUntil = getLong("groveLeadUntil") ?: 0L,
        witnessCountToday = readNumberInt("witnessCountToday"),
        witnessDay = getString("witnessDay"),
        sharedPulseHabit = getString("sharedPulseHabit"),
        sharedPulseDay = getString("sharedPulseDay"),
    )
}

class UserRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        /** Name shown on profile cards, leaderboards, and challenge rosters — never Google legal name. */
        fun publicLabel(displayName: String?, username: String?): String {
            val dn = displayName?.trim().orEmpty()
            val un = username?.trim().orEmpty()
            return when {
                dn.isNotBlank() && !dn.equals("UnknownRival", ignoreCase = true) -> dn
                un.isNotBlank() && !un.equals("UnknownRival", ignoreCase = true) -> un
                else -> "Rival"
            }
        }
    }

    /** Create the Firestore users/{uid} doc when missing (safe to call on every sign-in). */
    suspend fun ensureUserDocument() {
        if (!userExists()) {
            seedDefaultUserData()
        } else {
            backfillSearchIndexFields()
        }
    }

    /** Initial seeding for a brand new user */
    suspend fun seedDefaultUserData() {
        val user = auth.currentUser ?: return
        val userDoc = firestore.collection("users").document(user.uid)

        val snapshot = userDoc.get().await()
        if (!snapshot.exists()) {
            val defaultUsername = "UnknownRival"
            val defaultData = hashMapOf(
                "displayName" to "Rival",
                "username" to defaultUsername,
                "usernameLower" to UserSearchIndex.usernameLower(defaultUsername),
                "displayNameLower" to UserSearchIndex.displayNameLower("Rival"),
                "searchKeywords" to UserSearchIndex.buildSearchKeywords(defaultUsername, "Rival"),
                "profileVersion" to 1,
                "lastUsernameEditTimestamp" to 0L,
                "email" to user.email,
                "photoUrl" to user.photoUrl?.toString(),
                "bio" to "Consistency is the craft.",
                "motto" to "Show up with intention.",
                "favoritePlant" to "Oak",
                "profileAccent" to "sage",
                "level" to 0,
                "xp" to 0,
                "createdAt" to com.google.firebase.Timestamp.now(),
                "streakCount" to 0,
                "totalCompletions" to 0,
                "longestStreakEver" to 0,
                "longestStreak" to 0,
                "totalChallengesCompleted" to 0,
                "totalChallengesLeft" to 0,
                "currentActiveChallenges" to 0,
                "totalFocusMinutes" to 0,
                "showRecentProof" to true,
                "shareLastActive" to true,
            )
            userDoc.set(defaultData).await()
        }
    }

    /** Update basic profile info */
    suspend fun updateProfile(
        name: String,
        username: String,
        bio: String,
        motto: String,
        favoritePlant: String,
        photoUrl: String?,
        profileAccent: String,
        usernameChanged: Boolean = false,
    ) {
        val user = auth.currentUser ?: return
        ensureUserDocument()
        val data = mutableMapOf<String, Any>(
            "displayName" to name,
            "username" to username,
            "usernameLower" to UserSearchIndex.usernameLower(username),
            "displayNameLower" to UserSearchIndex.displayNameLower(name),
            "searchKeywords" to UserSearchIndex.buildSearchKeywords(username, name),
            "bio" to bio,
            "motto" to motto,
            "favoritePlant" to favoritePlant,
            "profileAccent" to profileAccent,
            "profileVersion" to com.google.firebase.firestore.FieldValue.increment(1)
        )
        if (usernameChanged) {
            data["lastUsernameEditTimestamp"] = System.currentTimeMillis()
        }
        photoUrl?.let { data["photoUrl"] = it }
        
        android.util.Log.d("UserRepository", "Updating profile for ${user.uid}: $data")
        
        firestore.collection("users").document(user.uid)
            .set(data, SetOptions.merge())
            .await()
    }

    /** Upload profile picture to Firebase Storage and return download URL */
    suspend fun uploadProfilePicture(context: android.content.Context, uri: android.net.Uri): String? {
        val user = auth.currentUser ?: return null
        return try {
            user.getIdToken(true).await()
            val bytes = when (uri.scheme) {
                "file" -> {
                    val path = uri.path ?: return null
                    val file = java.io.File(path)
                    if (!file.exists() || file.length() == 0L) return null
                    file.readBytes()
                }
                else -> context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } ?: return null

            if (bytes.isEmpty()) {
                android.util.Log.e("UserRepository", "Upload aborted. empty image bytes")
                return null
            }

            val storageRef = com.google.firebase.storage.FirebaseStorage.getInstance().reference
                .child("profile_pictures/${user.uid}.jpg")
            val metadata = com.google.firebase.storage.StorageMetadata.Builder()
                .setContentType("image/jpeg")
                .setCacheControl("public,max-age=3600")
                .build()

            android.util.Log.d(
                "UserRepository",
                "Uploading photo to ${storageRef.path} (${bytes.size} bytes)",
            )
            storageRef.putBytes(bytes, metadata).await()
            val url = storageRef.downloadUrl.await().toString()
            android.util.Log.d("UserRepository", "Upload success: $url")
            url
        } catch (e: Exception) {
            android.util.Log.e("UserRepository", "Upload failed", e)
            null
        }
    }

    /** Save habits blob. Never clobber a non-empty cloud backup with an empty local list. */
    suspend fun saveHabits(habitsJson: String) {
        val user = auth.currentUser ?: return
        val trimmed = habitsJson.trim()
        val looksEmpty = trimmed.isEmpty() || trimmed == "[]" || trimmed == "null"
        val ref = firestore.collection("users").document(user.uid)
        if (looksEmpty) {
            val existing = ref.get().await().getString("habitsJson").orEmpty().trim()
            if (existing.isNotEmpty() && existing != "[]" && existing != "null") {
                android.util.Log.w("UserRepository", "Skipping empty habits upload; cloud already has a backup")
                return
            }
        }
        ref.set(mapOf("habitsJson" to habitsJson), SetOptions.merge()).await()
    }

    /**
     * Push stats to Firestore. Numeric fields only ever rise (max of local + cloud)
     * so a fresh install with empty DataStore cannot wipe a returning user's XP.
     */
    suspend fun saveStats(
        xp: Int,
        completions: Int,
        longestStreak: Int,
        focusMinutes: Int,
        achievementsJson: String,
        trophyFactsJson: String = "{}",
        memberSinceMillis: Long? = null,
        force: Boolean = false,
    ) {
        val user = auth.currentUser ?: return
        val ref = firestore.collection("users").document(user.uid)
        val snap = ref.get().await()
        fun cloudInt(field: String): Int {
            val raw = snap.get(field) ?: return 0
            return when (raw) {
                is Number -> raw.toInt()
                else -> 0
            }
        }
        val mergedXp = if (force) xp else maxOf(xp, cloudInt("xp"))
        val mergedCompletions = if (force) completions else maxOf(completions, cloudInt("totalCompletions"))
        val mergedStreak = if (force) longestStreak else maxOf(longestStreak, cloudInt("longestStreakEver"))
        val mergedFocus = if (force) focusMinutes else maxOf(focusMinutes, cloudInt("totalFocusMinutes"))
        val level = GamificationEngine.calculateLevel(mergedXp)
        val data = mutableMapOf<String, Any>(
            "xp" to mergedXp,
            "level" to level,
            "totalCompletions" to mergedCompletions,
            "longestStreakEver" to mergedStreak,
            "totalFocusMinutes" to mergedFocus,
            "achievementsJson" to achievementsJson,
            "trophyFactsJson" to trophyFactsJson,
            "statsSyncedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
        )
        if (memberSinceMillis != null && memberSinceMillis > 0L) {
            val existingCreated = when (val raw = snap.get("createdAt")) {
                is com.google.firebase.Timestamp -> raw.toDate().time
                is Number -> raw.toLong()
                else -> 0L
            }
            if (existingCreated <= 0L || memberSinceMillis < existingCreated) {
                data["createdAt"] = com.google.firebase.Timestamp(java.util.Date(memberSinceMillis))
            }
        }
        ref.set(data, SetOptions.merge()).await()
    }

    /** Fetch everything from Firestore for the current user */
    suspend fun fetchUserData(): Map<String, Any>? {
        val user = auth.currentUser ?: return null
        return firestore.collection("users").document(user.uid).get().await().data
    }

    suspend fun fetchPublicProfile(uid: String): PublicUserProfile? {
        if (uid.isBlank()) return null
        val snap = firestore.collection("users").document(uid).get().await()
        return snap.toPublicUserProfile()
    }

    /** One extra string on the existing user doc — not a story collection. */
    suspend fun shareGrovePulse(habitTitle: String) {
        val user = auth.currentUser ?: return
        val title = habitTitle.trim().take(48)
        if (title.isBlank()) return
        firestore.collection("users").document(user.uid)
            .set(
                mapOf(
                    "sharedPulseHabit" to title,
                    "sharedPulseDay" to com.saintnico.verdlyhabits.engine.grove.GroveCalendar.todayKey(),
                ),
                SetOptions.merge(),
            ).await()
    }

    suspend fun clearGrovePulseShare() {
        val user = auth.currentUser ?: return
        firestore.collection("users").document(user.uid)
            .set(
                mapOf(
                    "sharedPulseHabit" to com.google.firebase.firestore.FieldValue.delete(),
                    "sharedPulseDay" to com.google.firebase.firestore.FieldValue.delete(),
                ),
                SetOptions.merge(),
            ).await()
    }

    suspend fun syncGrovePresence(currentStreakPublic: Int) {
        val user = auth.currentUser ?: return
        val ref = firestore.collection("users").document(user.uid)
        val snap = ref.get().await()
        val next = com.saintnico.verdlyhabits.engine.grove.GroveCalendar.nextPresence(
            previousDay = snap.getString("lastPlantedDay"),
            previousCountToday = snap.readNumberInt("plantedCountToday"),
            previousWeeklyDays = snap.readNumberInt("weeklyPlantDays"),
            previousWeekKey = snap.getString("weeklyPlantWeekKey"),
            currentStreakPublic = currentStreakPublic,
        )
        ref.set(
            mapOf(
                "lastPlantedAt" to next.lastPlantedAt,
                "lastPlantedDay" to next.lastPlantedDay,
                "plantedCountToday" to next.plantedCountToday,
                "weeklyPlantDays" to next.weeklyPlantDays,
                "weeklyPlantWeekKey" to next.weeklyPlantWeekKey,
                "currentStreakPublic" to next.currentStreakPublic,
            ),
            SetOptions.merge(),
        ).await()
    }

    suspend fun setStandInOpenGrove(stand: Boolean) {
        val user = auth.currentUser ?: return
        val data = hashMapOf<String, Any>("standInOpenGrove" to stand)
        if (!stand) {
            data["openGroveWeekRank"] = com.google.firebase.firestore.FieldValue.delete()
            data["openGrovePercentile"] = com.google.firebase.firestore.FieldValue.delete()
        }
        firestore.collection("users").document(user.uid)
            .set(data, SetOptions.merge())
            .await()
    }

    suspend fun setGroveLeadUntil(untilMillis: Long) {
        val user = auth.currentUser ?: return
        firestore.collection("users").document(user.uid)
            .set(mapOf("groveLeadUntil" to untilMillis), SetOptions.merge())
            .await()
    }

    /** Publish trial/premium badge so friends and connections can see membership status. */
    suspend fun syncMembershipTier(tier: MembershipTier) {
        val user = auth.currentUser ?: return
        firestore.collection("users").document(user.uid)
            .set(mapOf("membershipTier" to tier.firestoreValue), SetOptions.merge())
            .await()
    }

    suspend fun setEquippedTitle(titleId: String?, titleLabel: String?) {
        val user = auth.currentUser ?: return
        val data = hashMapOf<String, Any>()
        if (titleId.isNullOrBlank()) {
            data["equippedTitleId"] = com.google.firebase.firestore.FieldValue.delete()
            data["equippedTitleLabel"] = com.google.firebase.firestore.FieldValue.delete()
        } else {
            data["equippedTitleId"] = titleId
            if (!titleLabel.isNullOrBlank()) data["equippedTitleLabel"] = titleLabel
        }
        firestore.collection("users").document(user.uid)
            .set(data, SetOptions.merge())
            .await()
    }

    suspend fun syncIdentityCosmetics(
        ownedCsv: String,
        titleGlowId: String?,
        checkSkinId: String?,
        profileFrameId: String?,
        oathChipId: String?,
        grovePlantId: String?,
        pinnedTrophyId: String?,
        pinnedTrophyLabel: String?,
        pinnedTrophyUntil: Long,
    ) {
        val user = auth.currentUser ?: return
        val data = hashMapOf<String, Any>("ownedCosmetics" to ownedCsv)
        fun putOrDelete(key: String, value: String?) {
            if (value.isNullOrBlank()) data[key] = com.google.firebase.firestore.FieldValue.delete()
            else data[key] = value
        }
        putOrDelete("titleGlowId", titleGlowId)
        putOrDelete("checkSkinId", checkSkinId)
        putOrDelete("profileFrameId", profileFrameId)
        putOrDelete("oathChipId", oathChipId)
        putOrDelete("grovePlantId", grovePlantId)
        if (pinnedTrophyId.isNullOrBlank() || pinnedTrophyUntil <= System.currentTimeMillis()) {
            data["pinnedTrophyId"] = com.google.firebase.firestore.FieldValue.delete()
            data["pinnedTrophyLabel"] = com.google.firebase.firestore.FieldValue.delete()
            data["pinnedTrophyUntil"] = com.google.firebase.firestore.FieldValue.delete()
        } else {
            data["pinnedTrophyId"] = pinnedTrophyId
            data["pinnedTrophyLabel"] = pinnedTrophyLabel.orEmpty()
            data["pinnedTrophyUntil"] = pinnedTrophyUntil
        }
        firestore.collection("users").document(user.uid)
            .set(data, SetOptions.merge())
            .await()
    }

    /** Toggle whether the public "Recent proof" highlight reel is visible to others. */
    suspend fun setShowRecentProof(value: Boolean) {
        val user = auth.currentUser ?: return
        firestore.collection("users").document(user.uid)
            .set(mapOf("showRecentProof" to value), SetOptions.merge())
            .await()
    }

    /** Ping last-active while the app is open. Owner-only write. */
    suspend fun touchLastActive() {
        val user = auth.currentUser ?: return
        val payload = mapOf(
            "lastActiveAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            "shareLastActive" to true,
        )
        firestore.collection("users").document(user.uid)
            .set(payload, SetOptions.merge())
            .await()
        firestore.collection("users").document(user.uid)
            .collection("presence").document("live")
            .set(payload, SetOptions.merge())
            .await()
    }

    /**
     * Activity sharing. Off both hides you and clears the timestamp so others
     * cannot keep showing a stale last-active.
     */
    suspend fun setShareLastActive(value: Boolean) {
        val user = auth.currentUser ?: return
        val ref = firestore.collection("users").document(user.uid)
        if (value) {
            val payload = mapOf(
                "shareLastActive" to true,
                "lastActiveAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
            )
            ref.set(payload, SetOptions.merge()).await()
            ref.collection("presence").document("live").set(payload, SetOptions.merge()).await()
        } else {
            val payload = mapOf(
                "shareLastActive" to false,
                "lastActiveAt" to com.google.firebase.firestore.FieldValue.delete(),
            )
            ref.set(payload, SetOptions.merge()).await()
            ref.collection("presence").document("live").delete().await()
        }
    }

    /** Permanently delete user document from Firestore */
    suspend fun resetUserAccount() {
        val user = auth.currentUser ?: return
        firestore.collection("users").document(user.uid).delete().await()
    }

    suspend fun userExists(): Boolean {
        val user = auth.currentUser ?: return false
        return firestore.collection("users").document(user.uid).get().await().exists()
    }

    /** True when the signed-in user still has the default handle and should see profile setup. */
    suspend fun needsUsernameSetup(): Boolean {
        val user = auth.currentUser ?: return false
        val snap = firestore.collection("users").document(user.uid).get().await()
        if (!snap.exists()) return true
        val username = snap.getString("username").orEmpty()
        return username.isBlank() || username.equals("UnknownRival", ignoreCase = true)
    }

    /** Find a rival by @username (case-insensitive, exact match). */
    suspend fun findByUsername(raw: String): PublicUserProfile? =
        searchUsers(raw, limit = 1).firstOrNull()

    /**
     * Instagram-style user discovery: prefix + keyword matching on username and display name.
     * Does not require the full exact handle.
     */
    suspend fun searchUsers(raw: String, limit: Int = 15): List<PublicUserProfile> {
        val query = raw.trim().removePrefix("@").lowercase()
        if (query.isEmpty()) return emptyList()

        val me = auth.currentUser?.uid
        val col = firestore.collection("users")
        val end = query + "\uf8ff"
        val ranked = linkedMapOf<String, Pair<PublicUserProfile, Int>>()

        fun absorb(snapshot: QuerySnapshot) {
            for (doc in snapshot.documents) {
                val uid = doc.id
                if (uid == me || ranked.containsKey(uid)) continue
                val profile = doc.toPublicUserProfile() ?: continue
                val username = profile.username
                if (username.isBlank() || username.equals("UnknownRival", ignoreCase = true)) continue
                val score = UserSearchIndex.rank(query, username, profile.displayName)
                ranked[uid] = profile to score
            }
        }

        val queryLimit = limit.toLong()
        coroutineScope {
            val exact = async {
                col.whereEqualTo("usernameLower", query).limit(queryLimit).get().await()
            }
            val userPrefix = async {
                col.whereGreaterThanOrEqualTo("usernameLower", query)
                    .whereLessThanOrEqualTo("usernameLower", end)
                    .limit(queryLimit)
                    .get()
                    .await()
            }
            val namePrefix = async {
                col.whereGreaterThanOrEqualTo("displayNameLower", query)
                    .whereLessThanOrEqualTo("displayNameLower", end)
                    .limit(queryLimit)
                    .get()
                    .await()
            }
            val keywords = async {
                runCatching {
                    col.whereArrayContains("searchKeywords", query).limit(queryLimit).get().await()
                }.getOrNull()
            }
            absorb(exact.await())
            absorb(userPrefix.await())
            absorb(namePrefix.await())
            keywords.await()?.let { absorb(it) }
        }

        return ranked.values
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    /**
     * People you may know / new on Verdly — for the empty Find Someone surface.
     * Mixes recent signups with people active in the last few days.
     */
    suspend fun fetchDiscoverSuggestions(
        excludeUids: Set<String> = emptySet(),
        limit: Int = 12,
    ): List<PublicUserProfile> {
        val me = auth.currentUser?.uid ?: return emptyList()
        val col = firestore.collection("users")
        val fetchN = (limit * 3).toLong().coerceAtLeast(24L)
        val ranked = linkedMapOf<String, Pair<PublicUserProfile, Int>>()

        fun absorb(docs: List<com.google.firebase.firestore.DocumentSnapshot>, baseScore: Int) {
            docs.forEachIndexed { index, doc ->
                val uid = doc.id
                if (uid == me || uid in excludeUids || ranked.containsKey(uid)) return@forEachIndexed
                val profile = doc.toPublicUserProfile() ?: return@forEachIndexed
                val username = profile.username
                if (username.isBlank() || username.equals("UnknownRival", ignoreCase = true)) return@forEachIndexed
                val bonus = (docs.size - index).coerceAtLeast(0)
                ranked[uid] = profile to (baseScore + bonus)
            }
        }

        coroutineScope {
            val newest = async {
                runCatching {
                    col.orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(fetchN)
                        .get()
                        .await()
                        .documents
                }.getOrElse { emptyList() }
            }
            val active = async {
                runCatching {
                    col.orderBy("lastActiveAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
                        .limit(fetchN)
                        .get()
                        .await()
                        .documents
                }.getOrElse { emptyList() }
            }
            absorb(newest.await(), baseScore = 100)
            absorb(active.await(), baseScore = 40)
        }

        return ranked.values
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    /** Ensures search fields exist for older accounts created before indexed discovery. */
    private suspend fun backfillSearchIndexFields() {
        val user = auth.currentUser ?: return
        val doc = firestore.collection("users").document(user.uid).get().await()
        if (!doc.exists()) return

        val username = doc.getString("username").orEmpty()
        val displayName = doc.getString("displayName").orEmpty()
        val updates = mutableMapOf<String, Any>()

        if (!doc.contains("usernameLower") && username.isNotBlank()) {
            updates["usernameLower"] = UserSearchIndex.usernameLower(username)
        }
        if (!doc.contains("displayNameLower") && displayName.isNotBlank()) {
            updates["displayNameLower"] = UserSearchIndex.displayNameLower(displayName)
        }
        val keywords = doc.get("searchKeywords") as? List<*>
        if (keywords.isNullOrEmpty() && (username.isNotBlank() || displayName.isNotBlank())) {
            updates["searchKeywords"] = UserSearchIndex.buildSearchKeywords(username, displayName)
        }

        if (updates.isNotEmpty()) {
            firestore.collection("users").document(user.uid)
                .set(updates, SetOptions.merge())
                .await()
        }
    }
}
