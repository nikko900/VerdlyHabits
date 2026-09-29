package com.saintnico.verdlyhabits.navigation

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.saintnico.verdlyhabits.data.local.AppDataStore
import com.saintnico.verdlyhabits.preferences.ThemePreference
import com.saintnico.verdlyhabits.ui.navigation.Screen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Resolves the first screen after cold start.
 * Onboarding is shown at most once per install; signed-out users return to Google sign-in, not slides.
 *
 * Never hits Firestore here — [MainActivity.onCreate] runs this on the main thread.
 */
fun resolveAppStartDestination(context: Context): String = runBlocking {
    val themePref = ThemePreference(context)
    val appData = AppDataStore(context)
    var onboardingDone = themePref.hasCompletedOnboarding.first()

    if (!onboardingDone && appData.totalXp.first() > 0) {
        themePref.setOnboardingCompleted()
        onboardingDone = true
    }

    val firebaseUser = FirebaseAuth.getInstance().currentUser
    if (firebaseUser != null) {
        val localUsername = themePref.userUsername.first()
        val needsProfile = localUsername.isBlank() ||
            localUsername.equals("UnknownRival", ignoreCase = true)
        return@runBlocking if (needsProfile) Screen.ProfileSetup.route else Screen.Main.route
    }

    if (onboardingDone) Screen.Login.route else Screen.Onboarding.route
}
