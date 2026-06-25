package com.saintnico.verdlyhabits.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.saintnico.verdlyhabits.data.model.FriendshipStatus
import com.saintnico.verdlyhabits.data.model.MemberPublicProfile
import com.saintnico.verdlyhabits.data.remote.firestore.FriendsRepository
import com.saintnico.verdlyhabits.util.UserFacingErrors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

data class MemberProfileUiState(
    val uid: String = "",
    val isLoading: Boolean = true,
    val profile: MemberPublicProfile? = null,
    val friendshipStatus: FriendshipStatus = FriendshipStatus.NONE,
    val error: String? = null
)

class MemberProfileViewModel : ViewModel() {
    private val friendsRepo = FriendsRepository()

    private val _state = MutableStateFlow(MemberProfileUiState())
    val state: StateFlow<MemberProfileUiState> = _state.asStateFlow()

    fun load(uid: String) {
        if (uid.isBlank()) return
        if (_state.value.uid == uid && _state.value.profile != null) return
        _state.value = MemberProfileUiState(uid = uid, isLoading = true)
        viewModelScope.launch {
            val profile = friendsRepo.fetchPublicProfile(uid)
            _state.value = _state.value.copy(profile = profile, isLoading = false)
        }
        viewModelScope.launch {
            friendsRepo.observeStatus(uid).collectLatest { status ->
                _state.value = _state.value.copy(friendshipStatus = status)
            }
        }
    }

    fun sendRequest() {
        val uid = _state.value.uid
        if (uid.isBlank()) return
        viewModelScope.launch {
            try {
                friendsRepo.sendRequest(uid)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = UserFacingErrors.message(e))
            }
        }
    }

    fun acceptRequest() {
        val uid = _state.value.uid
        if (uid.isBlank()) return
        viewModelScope.launch {
            try {
                friendsRepo.acceptRequest(uid)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = UserFacingErrors.message(e))
            }
        }
    }

    fun cancelOrDecline() {
        val uid = _state.value.uid
        if (uid.isBlank()) return
        viewModelScope.launch {
            try {
                friendsRepo.declineOrCancel(uid)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = UserFacingErrors.message(e))
            }
        }
    }

    fun removeFriend() {
        val uid = _state.value.uid
        if (uid.isBlank()) return
        viewModelScope.launch {
            try {
                friendsRepo.removeFriend(uid)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = UserFacingErrors.message(e))
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
