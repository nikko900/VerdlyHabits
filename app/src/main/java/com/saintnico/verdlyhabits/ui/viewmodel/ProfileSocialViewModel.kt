package com.saintnico.verdlyhabits.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestoreException
import com.saintnico.verdlyhabits.data.remote.firestore.ProfileViewRepository
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileSocialViewModel(application: Application) : AndroidViewModel(application) {

    private val viewRepository = ProfileViewRepository()

    private val _weeklyFriendViews = MutableStateFlow(0)
    val weeklyFriendViews: StateFlow<Int> = _weeklyFriendViews.asStateFlow()

    private val firestoreErrors = CoroutineExceptionHandler { _, throwable ->
        Log.w(TAG, "Profile social Firestore call failed safely", throwable)
        _weeklyFriendViews.value = 0
    }

    fun refreshWeeklyViews(friendUids: Set<String>) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch(firestoreErrors) {
            _weeklyFriendViews.value = runCatching {
                viewRepository.friendViewsThisWeek(uid, friendUids)
            }.getOrElse { error ->
                if (error is FirebaseFirestoreException) {
                    Log.w(TAG, "profileViews read denied or unavailable — showing 0", error)
                }
                0
            }
        }
    }

    fun recordProfileView(viewedUid: String) {
        val viewerUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        viewModelScope.launch(firestoreErrors) {
            runCatching {
                viewRepository.recordView(viewedUid, viewerUid)
            }.onFailure { error ->
                if (error is FirebaseFirestoreException) {
                    Log.w(TAG, "profileViews write denied or unavailable — skipped", error)
                }
            }
        }
    }

    companion object {
        private const val TAG = "ProfileSocialVM"
    }
}
