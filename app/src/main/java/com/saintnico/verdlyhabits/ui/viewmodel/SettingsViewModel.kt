package com.saintnico.verdlyhabits.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.saintnico.verdlyhabits.preferences.ThemePreference
import com.saintnico.verdlyhabits.util.UserFacingErrors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val themePreference = ThemePreference(application)
    private val appDataStore = com.saintnico.verdlyhabits.data.local.AppDataStore(application)
    private val userRepository = com.saintnico.verdlyhabits.data.remote.firestore.UserRepository()

    val isDarkMode = themePreference.isDarkMode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val isVibrationEnabled = themePreference.isVibrationEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val isNotificationsEnabled = themePreference.isNotificationsEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false,
    )

    /** Sound chime preference for habit completions / milestones / level-ups. */
    val isSoundEnabled = appDataStore.soundEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    /**
     * Local-only premium flag — debug toggle for now. Once Billing is wired this
     * will be replaced by a verified entitlement. See [com.saintnico.verdlyhabits.domain.PremiumGate].
     */
    val isPremium = appDataStore.isPremium.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    val isProDebugEnabled = appDataStore.proDebugEnabled.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    fun toggleSound() {
        viewModelScope.launch {
            appDataStore.setSoundEnabled(!isSoundEnabled.value)
        }
    }

    fun togglePremiumDebug() {
        viewModelScope.launch {
            appDataStore.setPremium(!isPremium.value)
        }
    }

    fun toggleProDebug() {
        viewModelScope.launch {
            appDataStore.setProDebugEnabled(!isProDebugEnabled.value)
        }
    }

    val languageCode = themePreference.languageCode.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "en"
    )
    
    val userName = themePreference.userName.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "UnknownRival"
    )
    val userUsername = themePreference.userUsername.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "UnknownRival"
    )
    val userEmail = themePreference.userEmail.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "hello@verdlyhabits.app"
    )
    val userBio = themePreference.userBio.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Consistency is the craft."
    )
    val userMotto = themePreference.userMotto.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Show up with intention."
    )
    val userFavoritePlant = themePreference.userFavoritePlant.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "Oak"
    )
    val userPhotoUri = themePreference.userPhotoUri.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )
    val userProfileAccent = themePreference.profileAccent.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "sage",
    )

    val showRecentProof = themePreference.showRecentProof.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true,
    )

    private val _isSaving = MutableStateFlow(false)
    val isSaving = _isSaving.asStateFlow()
    
    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent = _errorEvent.asSharedFlow()

    /** Emitted once after a profile save completes without throwing (including username cooldown bypass — not emitted). */
    private val _profileSaveSuccess = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val profileSaveSuccess = _profileSaveSuccess.asSharedFlow()

    fun toggleTheme() {
        viewModelScope.launch {
            themePreference.toggleTheme()
        }
    }

    fun toggleVibration() {
        viewModelScope.launch {
            themePreference.toggleVibration()
        }
    }

    fun setShowRecentProof(value: Boolean) {
        viewModelScope.launch {
            themePreference.setShowRecentProof(value)
            try {
                userRepository.setShowRecentProof(value)
            } catch (_: Exception) {
                // Local mirror already updated; Firestore will re-sync on next profile pull.
            }
        }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            try {
                themePreference.setNotificationsEnabled(enabled)
                val uid = FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val userRef = FirebaseFirestore.getInstance().collection("users").document(uid)
                    if (enabled) {
                        val token = FirebaseMessaging.getInstance().token.await()
                        userRef.update(
                            mapOf(
                                "notificationsEnabled" to true,
                                "fcmToken" to token,
                            ),
                        ).await()
                    } else {
                        userRef.update(
                            mapOf(
                                "notificationsEnabled" to false,
                                "fcmToken" to FieldValue.delete(),
                            ),
                        ).await()
                        runCatching { FirebaseMessaging.getInstance().deleteToken().await() }
                    }
                }
            } catch (e: Exception) {
                _errorEvent.emit("Notification settings failed — ${UserFacingErrors.message(e)}")
            }
        }
    }

    fun setLanguage(code: String) {
        viewModelScope.launch {
            themePreference.setLanguage(code)
        }
    }
    
    fun saveProfile(
        name: String,
        username: String,
        email: String,
        bio: String,
        motto: String,
        favoritePlant: String,
        photoUri: String?,
        profileAccentKey: String,
    ) {
        viewModelScope.launch {
            val lastEdit = themePreference.lastUsernameEditTimestamp.first()
            val currentUsername = themePreference.userUsername.first()
            val usernameChanged = username != currentUsername

            if (usernameChanged && (System.currentTimeMillis() - lastEdit < 7 * 24 * 60 * 60 * 1000L)) {
                _errorEvent.emit("Username can only be changed once every 7 days.")
                return@launch
            }

            try {
                _isSaving.value = true
                var finalPhotoUrl = photoUri

                // Only attempt upload if it's a new local URI
                if (photoUri != null && !photoUri.startsWith("http")) {
                    val uri = android.net.Uri.parse(photoUri)
                    val uploadedUrl = userRepository.uploadProfilePicture(uri)
                    if (uploadedUrl != null) {
                        finalPhotoUrl = uploadedUrl
                    } else {
                        // If upload failed, we keep the original photo if it was a remote one,
                        // or notify that the image sync failed.
                        _errorEvent.emit("Cloud storage upload failed. Image may not sync.")
                        // We continue saving other fields though.
                    }
                }

                // Save to Firestore FIRST (Remote First)
                userRepository.updateProfile(
                    name, username, bio, motto, favoritePlant, finalPhotoUrl, profileAccentKey, usernameChanged,
                )

                // Then Save to DataStore for local persistence
                themePreference.saveProfile(
                    name, username, email, bio, motto, favoritePlant, finalPhotoUrl, profileAccentKey,
                )
                if (usernameChanged) {
                    themePreference.updateLastUsernameEdit(System.currentTimeMillis())
                }

                // Sync identity to all active challenges for immediate update on leaderboards
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                if (uid != null) {
                    val challengeLabel = com.saintnico.verdlyhabits.data.remote.firestore.UserRepository
                        .publicLabel(name, username)
                    com.saintnico.verdlyhabits.data.remote.firestore.ChallengeRepository()
                        .updateMemberIdentity(uid, challengeLabel, finalPhotoUrl)
                }

                syncProfileFromFirebase()
                _profileSaveSuccess.emit(Unit)
            } catch (e: Exception) {
                _errorEvent.emit("Failed to save profile — ${UserFacingErrors.message(e)}")
            } finally {
                _isSaving.value = false
            }
        }
    }

    fun syncProfileFromFirebase() {
        viewModelScope.launch {
            pullProfileFromCloud()
        }
    }

    suspend fun pullProfileFromCloud() {
        try {
            val oldSyncedVersion = themePreference.syncedVersion.first()
            themePreference.syncFromFirebase()

            val newProfileVersion = themePreference.profileVersion.first()
            if (newProfileVersion > oldSyncedVersion) {
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                val currentName = themePreference.userName.first()
                val currentUsername = themePreference.userUsername.first()
                val currentPhoto = themePreference.userPhotoUri.first()

                if (uid != null) {
                    val challengeLabel = com.saintnico.verdlyhabits.data.remote.firestore.UserRepository
                        .publicLabel(currentName, currentUsername)
                    com.saintnico.verdlyhabits.data.remote.firestore.ChallengeRepository()
                        .updateMemberIdentity(uid, challengeLabel, currentPhoto)
                }
                themePreference.updateVersions(newProfileVersion, newProfileVersion)
            }
        } catch (_: Exception) {
            // Profile pull can fail offline — never crash on account switch.
        }
    }

    fun markOnboardingCompleted() {
        viewModelScope.launch {
            themePreference.setOnboardingCompleted()
        }
    }

    suspend fun completeOnboarding() {
        themePreference.setOnboardingCompleted()
    }

    suspend fun hasCompletedOnboarding(): Boolean =
        themePreference.hasCompletedOnboarding.first()

    fun clearProfile() {
        viewModelScope.launch {
            themePreference.clearProfile()
        }
    }

    fun hardReset() {
        viewModelScope.launch {
            themePreference.clearProfile()
            appDataStore.clearAllData()
            userRepository.resetUserAccount()
        }
    }
}
