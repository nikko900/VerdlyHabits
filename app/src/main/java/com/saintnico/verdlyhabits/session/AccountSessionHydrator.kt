package com.saintnico.verdlyhabits.session

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.saintnico.verdlyhabits.ui.viewmodel.HabitViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.SettingsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.UserStatsViewModel
import com.saintnico.verdlyhabits.widget.WidgetSnapshotWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun prepareSignedInSession(
    context: Context,
    habitViewModel: HabitViewModel,
    userStatsViewModel: UserStatsViewModel,
    settingsViewModel: SettingsViewModel,
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val switched = AccountSessionCoordinator.onUserSignedIn(context, uid)
    if (switched) {
        habitViewModel.onAccountSessionChanged()
    }
    habitViewModel.restoreFromFirestore()
    userStatsViewModel.restoreFromFirestore()
    runCatching { settingsViewModel.pullProfileFromCloud() }
    withContext(Dispatchers.IO) {
        WidgetSnapshotWriter.refreshHabitsFromLocalStore(context)
    }
}

suspend fun clearSignedOutSession(
    context: Context,
    habitViewModel: HabitViewModel,
) {
    AccountSessionCoordinator.onUserSignedOut(context)
    habitViewModel.onAccountSessionChanged()
}
