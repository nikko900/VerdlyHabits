package com.saintnico.verdlyhabits.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.saintnico.verdlyhabits.data.remote.firestore.AccountabilityRepository
import com.saintnico.verdlyhabits.data.remote.firestore.DuoStreakState
import com.saintnico.verdlyhabits.notifications.SocialNotificationDispatcher
import com.saintnico.verdlyhabits.session.firebaseAuthUidFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AccountabilityViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AccountabilityRepository()

    val duoState: StateFlow<DuoStreakState?> = firebaseAuthUidFlow()
        .flatMapLatest { uid ->
            if (uid.isNullOrBlank()) flowOf(null)
            else repository.observeActivePair(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _nudgeMessage = MutableStateFlow<String?>(null)
    val nudgeMessage: StateFlow<String?> = _nudgeMessage.asStateFlow()

    private var lastBuddyDone: Boolean? = null

    init {
        viewModelScope.launch {
            duoState.collect { state ->
                if (state == null) {
                    lastBuddyDone = null
                    return@collect
                }
                val buddyDone = state.buddyDoneToday
                val wasDone = lastBuddyDone
                lastBuddyDone = buddyDone
                if (wasDone == false && buddyDone && !state.myDoneToday && state.status == "active") {
                    val name = state.buddyUsername.ifBlank { "Your buddy" }
                    _nudgeMessage.value = "$name finished today — your turn."
                }
            }
        }
    }

    fun clearNudge() {
        _nudgeMessage.value = null
    }

    fun inviteBuddy(
        uid: String,
        username: String,
        photoUrl: String?,
        myUsername: String,
        myPhotoUrl: String?,
    ) {
        viewModelScope.launch {
            val result = runCatching {
                repository.inviteBuddy(uid, username, photoUrl, myUsername, myPhotoUrl)
            }.getOrElse { Result.failure(it) }
            if (result.isSuccess) {
                val me = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                val pairId = com.saintnico.verdlyhabits.data.remote.firestore.AccountabilityRepository.pairId(me, uid)
                SocialNotificationDispatcher.notifyDuoInvite(
                    targetUid = uid,
                    fromUsername = myUsername,
                    fromPhotoUrl = myPhotoUrl,
                    pairId = pairId,
                )
            }
        }
    }

    fun acceptInvite(pairId: String) {
        viewModelScope.launch {
            runCatching { repository.acceptInvite(pairId) }
        }
    }

    fun declineInvite(pairId: String) {
        viewModelScope.launch {
            runCatching { repository.declineInvite(pairId) }
        }
    }

    fun syncFromHabits(completedToday: Int, activeHabits: Int) {
        if (activeHabits <= 0) return
        val allDone = completedToday >= activeHabits
        viewModelScope.launch {
            runCatching {
                repository.syncMyProgress(completedToday, activeHabits, allDone)
            }
        }
    }
}
