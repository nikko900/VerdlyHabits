package com.saintnico.verdlyhabits.data.remote.firestore

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.saintnico.verdlyhabits.data.model.PublicUserProfile
import com.saintnico.verdlyhabits.engine.GamificationEngine
import kotlinx.coroutines.tasks.await

private fun DocumentSnapshot.readNumberInt(field: String): Int {
    val raw = get(field) ?: return 0
    return when (raw) {
        is Number -> raw.toInt()
        else -> 0
    }
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
                "showRecentProof" to true
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
                android.util.Log.e("UserRepository", "Upload aborted — empty image bytes")
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

    /** Save all habits to Firestore */
    suspend fun saveHabits(habitsJson: String) {
        val user = auth.currentUser ?: return
        firestore.collection("users").document(user.uid)
            .set(mapOf("habitsJson" to habitsJson), SetOptions.merge())
            .await()
    }

    /** Save all stats and achievements to Firestore */
    suspend fun saveStats(
        xp: Int, 
        completions: Int, 
        longestStreak: Int, 
        focusMinutes: Int,
        achievementsJson: String
    ) {
        val user = auth.currentUser ?: return
        val level = GamificationEngine.calculateLevel(xp)
        val data = mapOf(
            "xp" to xp,
            "level" to level,
            "totalCompletions" to completions,
            "longestStreakEver" to longestStreak,
            "totalFocusMinutes" to focusMinutes,
            "achievementsJson" to achievementsJson
        )
        firestore.collection("users").document(user.uid)
            .set(data, SetOptions.merge())
            .await()
    }

    /** Fetch everything from Firestore for the current user */
    suspend fun fetchUserData(): Map<String, Any>? {
        val user = auth.currentUser ?: return null
        return firestore.collection("users").document(user.uid).get().await().data
    }

    suspend fun fetchPublicProfile(uid: String): PublicUserProfile? {
        if (uid.isBlank()) return null
        val snap = firestore.collection("users").document(uid).get().await()
        if (!snap.exists()) return null
        val xp = snap.readNumberInt("xp")
        // Level in Firestore is often stale (0); progression is defined from XP.
        val level = GamificationEngine.calculateLevel(xp)
        return PublicUserProfile(
            uid = uid,
            displayName = snap.getString("displayName").orEmpty(),
            username = snap.getString("username").orEmpty(),
            photoUrl = snap.getString("photoUrl"),
            bio = snap.getString("bio").orEmpty(),
            motto = snap.getString("motto").orEmpty(),
            favoritePlant = snap.getString("favoritePlant").orEmpty(),
            profileAccent = snap.getString("profileAccent") ?: "sage",
            level = level,
            xp = xp,
            showRecentProof = snap.getBoolean("showRecentProof") ?: true,
            equippedTitleId = snap.getString("equippedTitleId"),
            equippedTitleLabel = snap.getString("equippedTitleLabel"),
        )
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

    /** Toggle whether the public "Recent proof" highlight reel is visible to others. */
    suspend fun setShowRecentProof(value: Boolean) {
        val user = auth.currentUser ?: return
        firestore.collection("users").document(user.uid)
            .set(mapOf("showRecentProof" to value), SetOptions.merge())
            .await()
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
        if (query.length < 2) return emptyList()

        val me = auth.currentUser?.uid
        val col = firestore.collection("users")
        val end = query + "\uf8ff"
        val ranked = linkedMapOf<String, Pair<PublicUserProfile, Int>>()

        suspend fun absorb(snapshot: com.google.firebase.firestore.QuerySnapshot) {
            for (doc in snapshot.documents) {
                val uid = doc.id
                if (uid == me || ranked.containsKey(uid)) continue

                val username = doc.getString("username").orEmpty()
                if (username.isBlank() || username.equals("UnknownRival", ignoreCase = true)) continue

                val displayName = doc.getString("displayName").orEmpty()
                val profile = fetchPublicProfile(uid) ?: continue
                val score = UserSearchIndex.rank(query, username, displayName)
                ranked[uid] = profile to score
            }
        }

        val queryLimit = limit.toLong()

        absorb(col.whereEqualTo("usernameLower", query).limit(queryLimit).get().await())

        if (ranked.size < limit) {
            absorb(
                col.whereGreaterThanOrEqualTo("usernameLower", query)
                    .whereLessThanOrEqualTo("usernameLower", end)
                    .limit(queryLimit)
                    .get()
                    .await(),
            )
        }

        if (ranked.size < limit) {
            absorb(
                col.whereGreaterThanOrEqualTo("displayNameLower", query)
                    .whereLessThanOrEqualTo("displayNameLower", end)
                    .limit(queryLimit)
                    .get()
                    .await(),
            )
        }

        if (ranked.size < limit) {
            runCatching {
                absorb(col.whereArrayContains("searchKeywords", query).limit(queryLimit).get().await())
            }
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
