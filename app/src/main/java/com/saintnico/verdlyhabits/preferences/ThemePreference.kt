package com.saintnico.verdlyhabits.preferences

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

val Context.dataStore by preferencesDataStore(name = "settings")

enum class ThemeChoice(val wireValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark"),
    ;

    fun resolvesDark(systemIsDark: Boolean): Boolean = when (this) {
        SYSTEM -> systemIsDark
        LIGHT -> false
        DARK -> true
    }
}

class ThemePreference(private val context: Context) {
    companion object {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val IS_VIBRATION_ENABLED = booleanPreferencesKey("is_vibration_enabled")
        val IS_NOTIFICATIONS_ENABLED = booleanPreferencesKey("is_notifications_enabled")
        val HABIT_REMINDERS_ENABLED = booleanPreferencesKey("habit_reminders_enabled")
        val SOCIAL_NOTIFICATIONS_ENABLED = booleanPreferencesKey("social_notifications_enabled")
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
        /** Share last-active with friends, and see theirs. Default on. */
        val SHARE_LAST_ACTIVE = booleanPreferencesKey("share_last_active")
        /** Equipped profile title id from [ProfileTitleEngine]. Empty = auto signature. */
        val EQUIPPED_TITLE_ID = androidx.datastore.preferences.core.stringPreferencesKey("equipped_title_id")
        val OWNED_COSMETICS = androidx.datastore.preferences.core.stringPreferencesKey("owned_cosmetics")
        val EQUIPPED_TITLE_GLOW = androidx.datastore.preferences.core.stringPreferencesKey("equipped_title_glow")
        val EQUIPPED_CHECK_SKIN = androidx.datastore.preferences.core.stringPreferencesKey("equipped_check_skin")
        val EQUIPPED_PROFILE_FRAME = androidx.datastore.preferences.core.stringPreferencesKey("equipped_profile_frame")
        val EQUIPPED_OATH_CHIP = androidx.datastore.preferences.core.stringPreferencesKey("equipped_oath_chip")
        val EQUIPPED_GROVE_PLANT = androidx.datastore.preferences.core.stringPreferencesKey("equipped_grove_plant")
        val PINNED_TROPHY_ID = androidx.datastore.preferences.core.stringPreferencesKey("pinned_trophy_id")
        val PINNED_TROPHY_LABEL = androidx.datastore.preferences.core.stringPreferencesKey("pinned_trophy_label")
        val PINNED_TROPHY_UNTIL = androidx.datastore.preferences.core.longPreferencesKey("pinned_trophy_until")
        /** Device-level: intro slides seen; survives logout. */
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        /** First challenge arena explainer (reactions / scoring). */
        val CHALLENGE_ARENA_ONBOARDING_SEEN = booleanPreferencesKey("challenge_arena_onboarding_seen")
        val GOAL_DAILY_REMINDER_ENABLED = booleanPreferencesKey("goal_daily_reminder_enabled")
        val GOAL_DAILY_REMINDER_TIME = androidx.datastore.preferences.core.stringPreferencesKey("goal_daily_reminder_time")
        val GOAL_WEEKLY_CHECKIN_ENABLED = booleanPreferencesKey("goal_weekly_checkin_enabled")
        /** Monday-start week key when home Weekly Wrapped was last viewed (e.g. 2026-08-25). */
        val LAST_HOME_WRAPPED_WEEK = androidx.datastore.preferences.core.stringPreferencesKey("last_home_wrapped_week")
        /** Epoch millis when the Profile views sheet was last opened. */
        val LAST_PROFILE_VIEWS_SEEN_AT = androidx.datastore.preferences.core.longPreferencesKey("last_profile_views_seen_at")
        val LOOK_AT_OPEN_GROVE = booleanPreferencesKey("look_at_open_grove")
        val GROVE_RIVAL_UID = androidx.datastore.preferences.core.stringPreferencesKey("grove_rival_uid")
        val WITNESSED_TODAY_KEY = androidx.datastore.preferences.core.stringPreferencesKey("grove_witnessed_day")
        val WITNESSED_TODAY_UIDS = androidx.datastore.preferences.core.stringPreferencesKey("grove_witnessed_uids")
    }

    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map {
        it[ONBOARDING_COMPLETED] == true
    }

    val hasSeenChallengeArenaOnboarding: Flow<Boolean> = context.dataStore.data.map {
        it[CHALLENGE_ARENA_ONBOARDING_SEEN] == true
    }

    val themeChoice: Flow<ThemeChoice> = context.dataStore.data.map { preferences ->
        when (preferences[THEME_MODE]?.lowercase()) {
            "light" -> ThemeChoice.LIGHT
            "dark" -> ThemeChoice.DARK
            "system" -> ThemeChoice.SYSTEM
            else -> when (preferences[IS_DARK_MODE]) {
                true -> ThemeChoice.DARK
                false -> ThemeChoice.LIGHT
                null -> ThemeChoice.SYSTEM
            }
        }
    }

    val isDarkMode: Flow<Boolean> = themeChoice.map { it == ThemeChoice.DARK }

    val isVibrationEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_VIBRATION_ENABLED] ?: true
        }

    val isNotificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[IS_NOTIFICATIONS_ENABLED] ?: false
        }

    val habitRemindersEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[HABIT_REMINDERS_ENABLED] ?: true }

    val socialNotificationsEnabled: Flow<Boolean> = context.dataStore.data
        .map { it[SOCIAL_NOTIFICATIONS_ENABLED] ?: true }

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
    val shareLastActive: Flow<Boolean> = context.dataStore.data.map { it[SHARE_LAST_ACTIVE] ?: true }
    val equippedTitleId: Flow<String?> = context.dataStore.data.map { it[EQUIPPED_TITLE_ID] }
    val ownedCosmeticsCsv: Flow<String> = context.dataStore.data.map { it[OWNED_COSMETICS] ?: "" }
    val equippedTitleGlow: Flow<String?> = context.dataStore.data.map { it[EQUIPPED_TITLE_GLOW] }
    val equippedCheckSkin: Flow<String?> = context.dataStore.data.map { it[EQUIPPED_CHECK_SKIN] }
    val equippedProfileFrame: Flow<String?> = context.dataStore.data.map { it[EQUIPPED_PROFILE_FRAME] }
    val equippedOathChip: Flow<String?> = context.dataStore.data.map { it[EQUIPPED_OATH_CHIP] }
    val equippedGrovePlant: Flow<String?> = context.dataStore.data.map { it[EQUIPPED_GROVE_PLANT] }
    val pinnedTrophyId: Flow<String?> = context.dataStore.data.map { it[PINNED_TROPHY_ID] }
    val pinnedTrophyLabel: Flow<String?> = context.dataStore.data.map { it[PINNED_TROPHY_LABEL] }
    val pinnedTrophyUntil: Flow<Long> = context.dataStore.data.map { it[PINNED_TROPHY_UNTIL] ?: 0L }

    suspend fun setEquippedTitleId(id: String?) {
        context.dataStore.edit { prefs ->
            if (id.isNullOrBlank()) prefs.remove(EQUIPPED_TITLE_ID)
            else prefs[EQUIPPED_TITLE_ID] = id
        }
    }

    suspend fun setOwnedCosmeticsCsv(csv: String) {
        context.dataStore.edit { it[OWNED_COSMETICS] = csv }
    }

    suspend fun setEquippedTitleGlow(id: String?) {
        context.dataStore.edit { prefs ->
            if (id.isNullOrBlank()) prefs.remove(EQUIPPED_TITLE_GLOW)
            else prefs[EQUIPPED_TITLE_GLOW] = id
        }
    }

    suspend fun setEquippedCheckSkin(id: String?) {
        context.dataStore.edit { prefs ->
            if (id.isNullOrBlank()) prefs.remove(EQUIPPED_CHECK_SKIN)
            else prefs[EQUIPPED_CHECK_SKIN] = id
        }
    }

    suspend fun setEquippedProfileFrame(id: String?) {
        context.dataStore.edit { prefs ->
            if (id.isNullOrBlank()) prefs.remove(EQUIPPED_PROFILE_FRAME)
            else prefs[EQUIPPED_PROFILE_FRAME] = id
        }
    }

    suspend fun setEquippedOathChip(id: String?) {
        context.dataStore.edit { prefs ->
            if (id.isNullOrBlank()) prefs.remove(EQUIPPED_OATH_CHIP)
            else prefs[EQUIPPED_OATH_CHIP] = id
        }
    }

    suspend fun setEquippedGrovePlant(id: String?) {
        context.dataStore.edit { prefs ->
            if (id.isNullOrBlank()) prefs.remove(EQUIPPED_GROVE_PLANT)
            else prefs[EQUIPPED_GROVE_PLANT] = id
        }
    }

    suspend fun setPinnedTrophy(id: String?, label: String?, untilMillis: Long) {
        context.dataStore.edit { prefs ->
            if (id.isNullOrBlank() || untilMillis <= 0L) {
                prefs.remove(PINNED_TROPHY_ID)
                prefs.remove(PINNED_TROPHY_LABEL)
                prefs.remove(PINNED_TROPHY_UNTIL)
            } else {
                prefs[PINNED_TROPHY_ID] = id
                prefs[PINNED_TROPHY_LABEL] = label.orEmpty()
                prefs[PINNED_TROPHY_UNTIL] = untilMillis
            }
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

    val lastHomeWrappedWeek: Flow<String?> = context.dataStore.data.map {
        it[LAST_HOME_WRAPPED_WEEK]
    }

    val lastProfileViewsSeenAt: Flow<Long> = context.dataStore.data.map {
        it[LAST_PROFILE_VIEWS_SEEN_AT] ?: 0L
    }

    val lookAtOpenGrove: Flow<Boolean> = context.dataStore.data.map {
        it[LOOK_AT_OPEN_GROVE] == true
    }

    val groveRivalUid: Flow<String?> = context.dataStore.data.map {
        it[GROVE_RIVAL_UID]?.takeIf { id -> id.isNotBlank() }
    }

    suspend fun setLookAtOpenGrove(value: Boolean) {
        context.dataStore.edit { it[LOOK_AT_OPEN_GROVE] = value }
    }

    suspend fun setGroveRivalUid(uid: String?) {
        context.dataStore.edit { prefs ->
            if (uid.isNullOrBlank()) prefs.remove(GROVE_RIVAL_UID)
            else prefs[GROVE_RIVAL_UID] = uid
        }
    }

    suspend fun witnessedUidsToday(today: String): Set<String> {
        val snap = context.dataStore.data.first()
        if (snap[WITNESSED_TODAY_KEY] != today) return emptySet()
        return snap[WITNESSED_TODAY_UIDS].orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    suspend fun addWitnessedUid(today: String, uid: String) {
        context.dataStore.edit { prefs ->
            val day = prefs[WITNESSED_TODAY_KEY]
            val existing = if (day == today) prefs[WITNESSED_TODAY_UIDS].orEmpty() else ""
            val next = (existing.split(',').map { it.trim() }.filter { it.isNotEmpty() } + uid).distinct()
            prefs[WITNESSED_TODAY_KEY] = today
            prefs[WITNESSED_TODAY_UIDS] = next.joinToString(",")
        }
    }

    suspend fun markProfileViewsSeen(atMillis: Long = System.currentTimeMillis()) {
        context.dataStore.edit { it[LAST_PROFILE_VIEWS_SEEN_AT] = atMillis }
    }

    suspend fun setShowRecentProof(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SHOW_RECENT_PROOF] = value
        }
    }

    suspend fun setShareLastActive(value: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[SHARE_LAST_ACTIVE] = value
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

    suspend fun markHomeWrappedSeenForWeek(weekKey: String) {
        context.dataStore.edit { it[LAST_HOME_WRAPPED_WEEK] = weekKey }
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
            preferences[THEME_MODE] = if (!current) "dark" else "light"
        }
    }

    suspend fun setThemeChoice(choice: ThemeChoice) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = choice.wireValue
            when (choice) {
                ThemeChoice.LIGHT -> preferences[IS_DARK_MODE] = false
                ThemeChoice.DARK -> preferences[IS_DARK_MODE] = true
                ThemeChoice.SYSTEM -> preferences.remove(IS_DARK_MODE)
            }
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

    suspend fun setHabitRemindersEnabled(enabled: Boolean) {
        context.dataStore.edit { it[HABIT_REMINDERS_ENABLED] = enabled }
    }

    suspend fun setSocialNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[SOCIAL_NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.dataStore.edit { it[IS_VIBRATION_ENABLED] = enabled }
    }

    suspend fun setLanguage(code: String) {
        com.saintnico.verdlyhabits.i18n.LocaleHelper.persist(context, code)
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
                prefs[SHARE_LAST_ACTIVE] = userDoc.getBoolean("shareLastActive") ?: true
                val equippedTitleId = userDoc.getString("equippedTitleId")
                if (equippedTitleId != null) prefs[EQUIPPED_TITLE_ID] = equippedTitleId
                else prefs.remove(EQUIPPED_TITLE_ID)
                userDoc.getString("ownedCosmetics")?.let { prefs[OWNED_COSMETICS] = it }
                // Cloud wins when present. If a field was never published, keep the
                // local equipped look so we can push it for friends to see.
                userDoc.getString("titleGlowId")?.let { prefs[EQUIPPED_TITLE_GLOW] = it }
                userDoc.getString("checkSkinId")?.let { prefs[EQUIPPED_CHECK_SKIN] = it }
                userDoc.getString("profileFrameId")?.let { prefs[EQUIPPED_PROFILE_FRAME] = it }
                userDoc.getString("oathChipId")?.let { prefs[EQUIPPED_OATH_CHIP] = it }
                userDoc.getString("grovePlantId")?.let { prefs[EQUIPPED_GROVE_PLANT] = it }
                val pinnedTrophyId = userDoc.getString("pinnedTrophyId")
                if (pinnedTrophyId != null) prefs[PINNED_TROPHY_ID] = pinnedTrophyId
                else prefs.remove(PINNED_TROPHY_ID)
                val pinnedTrophyLabel = userDoc.getString("pinnedTrophyLabel")
                if (pinnedTrophyLabel != null) prefs[PINNED_TROPHY_LABEL] = pinnedTrophyLabel
                else prefs.remove(PINNED_TROPHY_LABEL)
                val pinnedTrophyUntil = userDoc.getLong("pinnedTrophyUntil")
                if (pinnedTrophyUntil != null) prefs[PINNED_TROPHY_UNTIL] = pinnedTrophyUntil
                else prefs.remove(PINNED_TROPHY_UNTIL)
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
            prefs.remove(EQUIPPED_TITLE_ID)
            prefs.remove(OWNED_COSMETICS)
            prefs.remove(EQUIPPED_TITLE_GLOW)
            prefs.remove(EQUIPPED_CHECK_SKIN)
            prefs.remove(EQUIPPED_PROFILE_FRAME)
            prefs.remove(EQUIPPED_OATH_CHIP)
            prefs.remove(EQUIPPED_GROVE_PLANT)
            prefs.remove(PINNED_TROPHY_ID)
            prefs.remove(PINNED_TROPHY_LABEL)
            prefs.remove(PINNED_TROPHY_UNTIL)
        }
    }
}
