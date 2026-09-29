package com.saintnico.verdlyhabits.session

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.saintnico.verdlyhabits.data.remote.firestore.UserRepository
import com.saintnico.verdlyhabits.ui.viewmodel.HabitViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.SettingsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.UserStatsViewModel
import com.saintnico.verdlyhabits.widget.WidgetSnapshotWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "VerdlySession"
/** Long enough for cold network + habitsJson download after reinstall. */
private const val SESSION_PREP_TIMEOUT_MS = 25_000L
private const val PROFILE_CHECK_TIMEOUT_MS = 4_000L

suspend fun prepareSignedInSession(
    context: Context,
    habitViewModel: HabitViewModel,
    userStatsViewModel: UserStatsViewModel,
    settingsViewModel: SettingsViewModel,
) {
    val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
    withTimeoutOrNull(SESSION_PREP_TIMEOUT_MS) {
        runCatching {
            val switched = AccountSessionCoordinator.onUserSignedIn(context, uid)
            if (switched) {
                habitViewModel.onAccountSessionChanged()
                userStatsViewModel.onAccountSessionChanged()
            }
            UserRepository().ensureUserDocument()
            // Await cloud restore so empty local DataStore cannot race-upload zeros.
            habitViewModel.restoreFromFirestoreNow()
            userStatsViewModel.restoreFromFirestoreNow()
            GoalsSyncCoordinator.restoreNow(context)
            GoalsSyncCoordinator.backupNow(context)
            settingsViewModel.pullProfileFromCloud()
            withContext(Dispatchers.IO) {
                WidgetSnapshotWriter.refreshHabitsFromLocalStore(context)
            }
        }.onFailure { Log.e(TAG, "prepareSignedInSession failed", it) }
    } ?: Log.w(TAG, "prepareSignedInSession timed out. continuing")
}

/**
 * Binds billing + local session after Firebase auth, then returns whether
 * profile setup is still required. Always returns — never blocks login navigation.
 */
suspend fun completeSignedInEntry(
    context: Context,
    habitViewModel: HabitViewModel,
    userStatsViewModel: UserStatsViewModel,
    settingsViewModel: SettingsViewModel,
    preferProfileSetup: Boolean,
    syncIdentity: (String?) -> Unit,
): Boolean {
    val uid = FirebaseAuth.getInstance().currentUser?.uid
    runCatching { syncIdentity(uid) }
    prepareSignedInSession(
        context = context,
        habitViewModel = habitViewModel,
        userStatsViewModel = userStatsViewModel,
        settingsViewModel = settingsViewModel,
    )
    if (preferProfileSetup) return true
    val needsSetup = withTimeoutOrNull(PROFILE_CHECK_TIMEOUT_MS) {
        runCatching { UserRepository().needsUsernameSetup() }.getOrNull()
    }
    // Timeout / network error: send returning users into the app, not profile setup.
    return needsSetup == true
}

suspend fun clearSignedOutSession(
    context: Context,
    habitViewModel: HabitViewModel,
    userStatsViewModel: UserStatsViewModel? = null,
) {
    AccountSessionCoordinator.onUserSignedOut(context)
    habitViewModel.onAccountSessionChanged()
    userStatsViewModel?.onAccountSessionChanged()
}
