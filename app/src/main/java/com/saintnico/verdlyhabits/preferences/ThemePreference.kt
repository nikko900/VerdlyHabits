package com.saintnico.verdlyhabits.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

val Context.dataStore by preferencesDataStore(name = "settings")

class ThemePreference(private val context: Context) {
    companion object {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val IS_VIBRATION_ENABLED = booleanPreferencesKey("is_vibration_enabled")
        val IS_NOTIFICATIONS_ENABLED = booleanPreferencesKey("is_notifications_enabled")
        val LANGUAGE_CODE = androidx.datastore.preferences.core.stringPreferencesKey("language_code")
        val USER_NAME = androidx.datastore.preferences.core.stringPreferencesKey("user_name")
        val USER_USERNAME = androidx.datastore.preferences.core.stringPreferencesKey("user_username")
        val USER_EMAIL = androidx.datastore.preferences.core.stringPreferencesKey("user_email")
        val USER_BIO = androidx.datastore.preferences.core.stringPreferencesKey("user_bio")
        val USER_MOTTO = androidx.datastore.preferences.core.stringPreferencesKey("user_motto")
        val USER_FAVORITE_PLANT = androidx.datastore.preferences.core.stringPreferencesKey("user_favorite_plant")
        val USER_PHOTO_URI = androidx.datastore.preferences.core.stringPreferencesKey("user_photo_uri")
        val PROFILE_ACCENT = androidx.datastore.preferences.core.stringPreferencesKey("profile_accent")
        val PROFILE_VERSION = androidx.datastore.preferences.core.intPreferencesKey("profile_version")
        val SYNCED_VERSION = androidx.datastore.preferences.core.intPreferencesKey("synced_version")
        val LAST_USERNAME_EDIT_TIMESTAMP = androidx.datastore.preferences.core.longPreferencesKey("last_username_edit_timestamp")
        /** Public privacy: show the "Recent proof" highlight reel on my profile. */
        val SHOW_RECENT_PROOF = booleanPreferencesKey("show_recent_proof")
        /** Equipped profile title id from [ProfileTitleEngine]. Empty = auto signature. */
        val EQUIPPED_TITLE_ID = androidx.datastore.preferences.core.stringPreferencesKey("equipped_title_id")
        /** Device-level: intro slides seen; survives logout. */
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        /** First challenge arena explainer (reactions / scoring). */
        val CHALLENGE_ARENA_ONBOARDING_SEEN = booleanPreferencesKey("challenge_arena_onboarding_seen")
        val GOAL_DAILY_REMINDER_ENABLED = booleanPreferencesKey("goal_daily_reminder_enabled")
        val GOAL_DAILY_REMINDER_TIME = androidx.datastore.preferences.core.stringPreferencesKey("goal_daily_reminder_time")
        val GOAL_WEEKLY_CHECKIN_ENABLED = booleanPreferencesKey("goal_weekly_checkin_enabled")
    }

    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map {
        it[ONBOARDING_COMPLETED] == true
    }

    val hasSeenChallengeArenaOnboarding: Flow<Boolean> = context.dataStore.data.map {
        it[CHALLENGE_ARENA_ONBOARDING_SEEN] == true
    }

    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_DARK_MODE] ?: true
        }

    val isVibrationEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_VIBRATION_ENABLED] ?: true
        }

    val isNotificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_NOTIFICATIONS_ENABLED] ?: false
        }

    val languageCode: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[LANGUAGE_CODE] ?: "en"
        }
        
    val userName: Flow<String> = context.dataStore.data.map { it[USER_NAME] ?: "UnknownRival" }
    val userUsername: Flow<String> = context.dataStore.data.map { it[USER_USERNAME] ?: "UnknownRival" }
    val userEmail: Flow<String> = context.dataStore.data.map { it[USER_EMAIL] ?: "hello@verdlyhabits.app" }
    val userBio: Flow<String> = context.dataStore.data.map { it[USER_BIO] ?: "Consistency is the craft." }
    val userMotto: Flow<String> = context.dataStore.data.map { it[USER_MOTTO] ?: "Show up with intention." }
    val userFavoritePlant: Flow<String> = context.dataStore.data.map { it[USER_FAVORITE_PLANT] ?: "Oak" }
    val userPhotoUri: Flow<String?> = context.dataStore.data.map { it[USER_PHOTO_URI] }
    val profileAccent: Flow<String> = context.dataStore.data.map { it[PROFILE_ACCENT] ?: "sage" }
    val profileVersion: Flow<Int> = context.dataStore.data.map { it[PROFILE_VERSION] ?: 0 }
    val syncedVersion: Flow<Int> = context.dataStore.data.map { it[SYNCED_VERSION] ?: 0 }
    val lastUsernameEditTimestamp: Flow<Long> = context.dataStore.data.map { it[LAST_USERNAME_EDIT_TIMESTAMP] ?: 0L }
    val showRecentProof: Flow<Boolean> = context.dataStore.data.map { it[SHOW_RECENT_PROOF] ?: true }
    val equippedTitleId: Flow<String?> = context.dataStore.data.map { it[EQUIPPED_TITLE_ID] }

    suspend fun setEquippedTitleId(id: String?) {
        context.dataStore.edit { prefs ->
            if (id.isNullOrBlank()) prefs.remove(EQUIPPED_TITLE_ID)
            else prefs[EQUIPPED_TITLE_ID] = id
        }
    }

    val goalDailyReminderEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[GOAL_DAILY_REMINDER_ENABLED] ?: true
    }
    val goalDailyReminderTime: Flow<String> = context.dataStore.data.map {
        it[GOAL_DAILY_REMINDER_TIME] ?: "20:30"
    }
    val goalWeeklyCheckInEnabled: Flow<Boolean> = context.dataStore.data.map {
        it[GOAL_WEEKLY_CHECKIN_ENABLED] ?: true
    }

    suspend fun setShowRecentProof(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SHOW_RECENT_PROOF] = value
        }
    }

    suspend fun setGoalDailyReminderEnabled(enabled: Boolean) {
        context.dataStore.edit { it[GOAL_DAILY_REMINDER_ENABLED] = enabled }
    }

    suspend fun setGoalDailyReminderTime(time: String) {
        context.dataStore.edit { it[GOAL_DAILY_REMINDER_TIME] = time }
    }

    suspend fun setGoalWeeklyCheckInEnabled(enabled: Boolean) {
        context.dataStore.edit { it[GOAL_WEEKLY_CHECKIN_ENABLED] = enabled }
    }

    suspend fun setOnboardingCompleted() {
        context.dataStore.edit { prefs ->
            prefs[ONBOARDING_COMPLETED] = true
        }
    }

    suspend fun setChallengeArenaOnboardingSeen() {
        context.dataStore.edit { prefs ->
            prefs[CHALLENGE_ARENA_ONBOARDING_SEEN] = true
        }
    }

    suspend fun toggleTheme() {
        context.dataStore.edit { preferences ->
            val current = preferences[IS_DARK_MODE] ?: true
            preferences[IS_DARK_MODE] = !current
        }
    }

    suspend fun toggleVibration() {
        context.dataStore.edit { preferences ->
            val current = preferences[IS_VIBRATION_ENABLED] ?: true
            preferences[IS_VIBRATION_ENABLED] = !current
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[IS_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setLanguage(code: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_CODE] = code
        }
    }

    suspend fun saveProfile(
        name: String,
        username: String,
        email: String,
        bio: String,
        motto: String,
        favoritePlant: String,
        photoUri: String?,
        profileAccentKey: String,
    ) {
        context.dataStore.edit { preferences ->
            preferences[USER_NAME] = name
            preferences[USER_USERNAME] = username
            preferences[USER_EMAIL] = email
            preferences[USER_BIO] = bio
            preferences[USER_MOTTO] = motto
            preferences[USER_FAVORITE_PLANT] = favoritePlant
            preferences[PROFILE_ACCENT] = profileAccentKey
            if (photoUri != null) {
                preferences[USER_PHOTO_URI] = photoUri
            } else {
                preferences.remove(USER_PHOTO_URI)
            }
        }
    }

    suspend fun updateVersions(profileVer: Int, syncedVer: Int) {
        context.dataStore.edit { prefs ->
            prefs[PROFILE_VERSION] = profileVer
            prefs[SYNCED_VERSION] = syncedVer
        }
    }

    suspend fun updateLastUsernameEdit(timestamp: Long) {
        context.dataStore.edit { prefs ->
            prefs[LAST_USERNAME_EDIT_TIMESTAMP] = timestamp
        }
    }

    /** Pull name, email, and photo from the currently signed-in Firebase user */
    suspend fun syncFromFirebase() {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser ?: return
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val userDoc = firestore.collection("users").document(user.uid).get().await()

            context.dataStore.edit { prefs ->
                prefs[USER_NAME] = userDoc.getString("displayName") ?: "Rival"
                prefs[USER_USERNAME] = userDoc.getString("username") ?: "UnknownRival"
                prefs[USER_EMAIL] = user.email ?: ""
                prefs[USER_BIO] = userDoc.getString("bio") ?: "Consistency is the craft."
                prefs[USER_MOTTO] = userDoc.getString("motto") ?: "Show up with intention."
                prefs[USER_FAVORITE_PLANT] = userDoc.getString("favoritePlant") ?: "Oak"
                prefs[PROFILE_ACCENT] = userDoc.getString("profileAccent") ?: "sage"

                val currentLocalPhoto = prefs[USER_PHOTO_URI]
                val photoUrl = userDoc.getString("photoUrl") ?: user.photoUrl?.toString()
                when {
                    !photoUrl.isNullOrBlank() -> {
                        // Prefer cloud URL; keep any local cache-bust query if same base path.
                        val localBase = currentLocalPhoto?.substringBefore("?")
                        val cloudBase = photoUrl.substringBefore("?")
                        if (localBase != null && localBase == cloudBase && currentLocalPhoto.contains("?")) {
                            prefs[USER_PHOTO_URI] = currentLocalPhoto
                        } else {
                            prefs[USER_PHOTO_URI] = photoUrl
                        }
                    }
                    currentLocalPhoto?.startsWith("file:") == true -> {
                        val path = android.net.Uri.parse(currentLocalPhoto).path
                        val stillExists = path != null && java.io.File(path).exists()
                        if (stillExists) {
                            prefs[USER_PHOTO_URI] = currentLocalPhoto
                        } else {
                            prefs.remove(USER_PHOTO_URI)
                        }
                    }
                    !currentLocalPhoto.isNullOrBlank() -> {
                        // Keep whatever we already have (e.g. pending local upload).
                        prefs[USER_PHOTO_URI] = currentLocalPhoto
                    }
                    else -> prefs.remove(USER_PHOTO_URI)
                }
                prefs[PROFILE_VERSION] = userDoc.getLong("profileVersion")?.toInt() ?: 0
                prefs[LAST_USERNAME_EDIT_TIMESTAMP] = userDoc.getLong("lastUsernameEditTimestamp") ?: 0L
                prefs[SHOW_RECENT_PROOF] = userDoc.getBoolean("showRecentProof") ?: true
                userDoc.getString("equippedTitleId")?.let { prefs[EQUIPPED_TITLE_ID] = it }
                    ?: prefs.remove(EQUIPPED_TITLE_ID)
            }
        } catch (_: Exception) {
            // Offline or rules error — keep cached profile.
        }
    }

    /** Wipe all user-specific profile data (called on logout) */
    suspend fun clearProfile() {
        context.dataStore.edit { prefs ->
            prefs.remove(USER_NAME)
            prefs.remove(USER_USERNAME)
            prefs.remove(USER_EMAIL)
            prefs.remove(USER_BIO)
            prefs.remove(USER_MOTTO)
            prefs.remove(USER_FAVORITE_PLANT)
            prefs.remove(PROFILE_ACCENT)
            prefs.remove(USER_PHOTO_URI)
            prefs.remove(PROFILE_VERSION)
            prefs.remove(SYNCED_VERSION)
            prefs.remove(LAST_USERNAME_EDIT_TIMESTAMP)
        }
    }
}
