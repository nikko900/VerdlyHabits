package com.saintnico.verdlyhabits.data.remote.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.saintnico.verdlyhabits.R
import kotlinx.coroutines.tasks.await

/**
 * Google Sign-In client using the classic [GoogleSignInClient] intent-based flow.
 *
 * This approach launches the native "Choose an account" picker when the user
 * has Google accounts on the device, and falls through to the full Google
 * sign-in screen when they don't.
 */
class GoogleAuthClient(private val context: Context) {

    private val auth = FirebaseAuth.getInstance()
    private val userRepository = com.saintnico.verdlyhabits.data.remote.firestore.UserRepository()

    private val googleSignInClient: GoogleSignInClient by lazy {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        GoogleSignIn.getClient(context, gso)
    }

    /** Returns the [Intent] that launches the Google account picker. */
    fun getSignInIntent(): Intent = googleSignInClient.signInIntent

    /**
     * Exchanges the Google ID token for a Firebase credential and signs in.
     *
     * @param idToken The Google ID token obtained from the sign-in result.
     * @return Pair of (AuthResult, isTrulyNewUser) on success, or `null` on failure.
     */
    suspend fun firebaseAuthWithGoogle(idToken: String): Pair<AuthResult, Boolean>? {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()

            // If the Firestore profile doc is missing (new Google account, or a prior seed
            // failed), create it. Defaulting to "missing" on read errors avoids skipping seed
            // when rules/network briefly fail — that left second accounts without a users/{uid} doc.
            val firestoreProfileExists = runCatching { userRepository.userExists() }.getOrDefault(false)
            val needsProfileSetup = result.additionalUserInfo?.isNewUser == true || !firestoreProfileExists
            if (needsProfileSetup) {
                runCatching { userRepository.ensureUserDocument() }
                    .onFailure { it.printStackTrace() }
            }
            com.saintnico.verdlyhabits.referral.ReferralManager.processPendingReferral(context)

            result to needsProfileSetup
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /** Display name or email of the currently signed-in user, or null. */
    fun getSignedInUser(): String? {
        return auth.currentUser?.displayName ?: auth.currentUser?.email
    }

    /** Whether a user is currently authenticated with Firebase. */
    fun isSignedIn(): Boolean = auth.currentUser != null

    /** Signs out of both Firebase and the Google account. */
    fun signOut() {
        auth.signOut()
        googleSignInClient.signOut()
    }
}
