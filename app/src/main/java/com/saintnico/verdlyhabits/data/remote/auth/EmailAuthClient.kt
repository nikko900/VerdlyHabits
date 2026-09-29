package com.saintnico.verdlyhabits.data.remote.auth

import android.content.Context
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.saintnico.verdlyhabits.session.AccountSessionCoordinator
import kotlinx.coroutines.tasks.await

/**
 * Email / password auth against Firebase, mirroring the Google path for
 * session prep and Firestore profile seeding.
 */
class EmailAuthClient(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()

    /**
     * @return Pair of (AuthResult, needsProfileSetup) on success.
     */
    suspend fun signIn(email: String, password: String): Pair<AuthResult, Boolean> {
        val result = auth.signInWithEmailAndPassword(email.trim(), password).await()
        return finalizeSession(result)
    }

    /**
     * @return Pair of (AuthResult, needsProfileSetup) on success — always true for new creates.
     */
    suspend fun signUp(email: String, password: String): Pair<AuthResult, Boolean> {
        val result = auth.createUserWithEmailAndPassword(email.trim(), password).await()
        return finalizeSession(result, forceProfileSetup = true)
    }

    private suspend fun finalizeSession(
        result: AuthResult,
        forceProfileSetup: Boolean = false,
    ): Pair<AuthResult, Boolean> {
        val uid = result.user?.uid
        if (uid != null) {
            runCatching { AccountSessionCoordinator.onUserSignedIn(context, uid) }
        }
        val needsProfileSetup = forceProfileSetup || result.additionalUserInfo?.isNewUser == true
        return result to needsProfileSetup
    }
}
