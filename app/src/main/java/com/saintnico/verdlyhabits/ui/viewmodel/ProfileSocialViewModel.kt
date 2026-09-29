package com.saintnico.verdlyhabits.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.saintnico.verdlyhabits.data.model.MembershipTier
import com.saintnico.verdlyhabits.data.model.PublicUserProfile
import com.saintnico.verdlyhabits.data.remote.firestore.BlockRepository
import com.saintnico.verdlyhabits.data.remote.firestore.ProfileViewRepository
import com.saintnico.verdlyhabits.data.remote.firestore.ProfileViewer
import com.saintnico.verdlyhabits.data.remote.firestore.UserRepository
import com.saintnico.verdlyhabits.data.remote.firestore.asPlayerLabel
import com.saintnico.verdlyhabits.notifications.SocialNotificationDispatcher
import com.saintnico.verdlyhabits.preferences.ThemePreference
import com.saintnico.verdlyhabits.session.firebaseAuthUidFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

data class ProfileViewsUiState(
    val viewers: List<ProfileViewer> = emptyList(),
    val weeklyCount: Int = 0,
    val unseenCount: Int = 0,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ProfileSocialViewModel(application: Application) : AndroidViewModel(application) {

    private val viewRepository = ProfileViewRepository()
    private val blockRepository = BlockRepository()
    private val userRepository = UserRepository()
    private val prefs = ThemePreference(application)
    private val publicProfiles = MutableStateFlow<Map<String, PublicUserProfile>>(emptyMap())
    private val hydrateRequested = ConcurrentHashMap.newKeySet<String>()

    val viewsState: StateFlow<ProfileViewsUiState> = firebaseAuthUidFlow()
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) {
                flowOf(ProfileViewsUiState())
            } else {
                combine(
                    viewRepository.observeRecentViewers(uid).onEach { hydratePublicCards(it) },
                    blockRepository.observeBlockedUids(uid),
                    prefs.lastProfileViewsSeenAt,
                    publicProfiles,
                ) { viewers, blocked, seenAt, profiles ->
                    val visible = viewers
                        .filter { it.uid !in blocked }
                        .map { it.mergedWith(profiles[it.uid]) }
                    val week = ProfileViewRepository.currentWeekKey()
                    ProfileViewsUiState(
                        viewers = visible,
                        weeklyCount = visible.count { it.weekKey == week },
                        unseenCount = if (seenAt <= 0L) {
                            0
                        } else {
                            visible.count { it.viewedAtMillis > seenAt }
                        },
                    )
                }.catch { error ->
                    Log.w(TAG, "profile views listen failed", error)
                    emit(ProfileViewsUiState())
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ProfileViewsUiState())

    init {
        viewModelScope.launch {
            viewsState.collect { state ->
                if (state.viewers.isNotEmpty()) {
                    com.saintnico.verdlyhabits.engine.TrophyBus.emit(
                        com.saintnico.verdlyhabits.engine.TrophyEvent.ProfileViewsReceived(state.viewers.size)
                    )
                }
            }
        }
    }

    val weeklyFriendViews: StateFlow<Int> = viewsState
        .map { it.weeklyCount }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun refreshWeeklyViews(friendUids: Set<String>) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            runCatching {
                viewRepository.friendViewsThisWeek(uid, friendUids)
            }.onFailure { error ->
                if (error is FirebaseFirestoreException) {
                    Log.w(TAG, "profileViews read denied or unavailable", error)
                }
            }
        }
    }

    fun recordProfileView(viewedUid: String) {
        val viewerUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch {
            runCatching {
                val live = withContext(Dispatchers.IO) {
                    userRepository.fetchPublicProfile(viewerUid)
                }
                val username = live?.username.asPlayerLabel()
                    ?: prefs.userUsername.first().asPlayerLabel()
                    ?: ""
                val displayName = live?.displayName.asPlayerLabel()
                    ?: prefs.userName.first().asPlayerLabel()
                    ?: username
                val photo = live?.photoUrl ?: prefs.userPhotoUri.first()
                val accent = live?.profileAccent?.ifBlank { null }
                    ?: prefs.profileAccent.first()
                val result = viewRepository.recordView(
                    viewedUid = viewedUid,
                    viewerUid = viewerUid,
                    viewerUsername = username,
                    viewerDisplayName = displayName,
                    viewerPhotoUrl = photo,
                    viewerProfileAccent = accent,
                    viewerMembershipTier = live?.membershipTier ?: MembershipTier.FREE,
                )
                if (result.shouldNotify) {
                    val notified = SocialNotificationDispatcher.notifyProfileView(
                        targetUid = viewedUid,
                        fromUsername = username.ifBlank { displayName },
                        fromPhotoUrl = photo,
                    )
                    if (notified) {
                        viewRepository.markNotified(viewedUid, viewerUid)
                    }
                }
                if (result.recorded) {
                    com.saintnico.verdlyhabits.engine.TrophyBus.emit(
                        com.saintnico.verdlyhabits.engine.TrophyEvent.ProfileViewedOther
                    )
                }
            }.onFailure { error ->
                if (error is FirebaseFirestoreException) {
                    Log.w(TAG, "profileViews write denied or unavailable. Skipped", error)
                } else {
                    Log.w(TAG, "recordProfileView failed", error)
                }
            }
        }
    }

    fun markViewsSeen() {
        viewModelScope.launch {
            prefs.markProfileViewsSeen(System.currentTimeMillis())
        }
    }

    private fun hydratePublicCards(viewers: List<ProfileViewer>) {
        val missing = viewers.map { it.uid }.filter { it.isNotBlank() && it !in hydrateRequested }
        if (missing.isEmpty()) return
        hydrateRequested.addAll(missing)
        viewModelScope.launch(Dispatchers.IO) {
            missing.chunked(8).forEach { chunk ->
                val fetched = coroutineScope {
                    chunk.map { uid ->
                        async {
                            runCatching { userRepository.fetchPublicProfile(uid) }.getOrNull()
                        }
                    }.awaitAll().filterNotNull()
                }
                if (fetched.isNotEmpty()) {
                    publicProfiles.update { current ->
                        current + fetched.associateBy { it.uid }
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "ProfileSocialVM"
    }
}

private fun ProfileViewer.mergedWith(profile: PublicUserProfile?): ProfileViewer {
    if (profile == null) return this
    return copy(
        username = profile.username.asPlayerLabel() ?: username,
        displayName = profile.displayName.asPlayerLabel()
            ?: profile.username.asPlayerLabel()
            ?: displayName,
        photoUrl = profile.photoUrl ?: photoUrl,
        profileAccent = profile.profileAccent.ifBlank { profileAccent },
        membershipTier = profile.membershipTier,
        equippedTitle = profile.equippedTitleLabel ?: equippedTitle,
        level = profile.level.takeIf { it > 0 } ?: level,
    )
}
