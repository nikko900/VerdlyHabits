package com.saintnico.verdlyhabits.util

import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.firestore.FirebaseFirestoreException

/**
 * Maps backend / SDK errors to short, LinkedIn-style messages users can act on.
 * Never surfaces SHA-1, HTTPS, Play Console, stack traces, or bare error codes.
 */
object UserFacingErrors {

    fun message(throwable: Throwable?): String {
        if (throwable == null) return genericFailure()
        return when (throwable) {
            is ApiException -> googleSignInMessage(throwable.statusCode)
            is FirebaseAuthException -> firebaseAuthMessage(throwable.errorCode)
            is FirebaseFirestoreException -> firestoreMessage(throwable.code)
            else -> forRawMessage(throwable.message)
        }
    }

    fun googleSignInMessage(statusCode: Int): String = when (statusCode) {
        CommonStatusCodes.NETWORK_ERROR ->
            "Couldn't sign in. Check your internet and try again."
        CommonStatusCodes.CANCELED, 12501 ->
            "Sign-in was cancelled."
        CommonStatusCodes.DEVELOPER_ERROR ->
            "Couldn't finish sign-in right now. Try again in a moment."
        CommonStatusCodes.INTERNAL_ERROR ->
            "Sign-in hit a snag. Try again in a moment."
        CommonStatusCodes.TIMEOUT ->
            "Sign-in timed out. Check your connection and try again."
        CommonStatusCodes.SIGN_IN_REQUIRED ->
            "Choose an account to continue."
        CommonStatusCodes.SERVICE_DISABLED,
        CommonStatusCodes.SERVICE_VERSION_UPDATE_REQUIRED,
        -> "Sign-in isn't available right now. Try again later."
        CommonStatusCodes.INVALID_ACCOUNT ->
            "That account can't be used here. Try a different one."
        12500 ->
            "Couldn't sign in. Try again or pick another account."
        12502 ->
            "Sign-in is already in progress. Wait a moment and try again."
        else -> genericFailure("sign in")
    }

    fun firebaseAuthMessage(errorCode: String): String = when (errorCode) {
        "ERROR_INVALID_EMAIL" -> "That email address doesn't look valid."
        "ERROR_WRONG_PASSWORD" -> "Incorrect password."
        "ERROR_EMAIL_ALREADY_IN_USE" -> "That email is already registered. Try signing in instead."
        "ERROR_WEAK_PASSWORD" -> "Password is too weak. Use at least 6 characters."
        "ERROR_USER_NOT_FOUND" -> "No account found with that email."
        "ERROR_USER_DISABLED" -> "This account has been disabled."
        "ERROR_TOO_MANY_REQUESTS" -> "Too many attempts. Wait a minute and try again."
        "ERROR_OPERATION_NOT_ALLOWED" ->
            "That sign-in method isn't available right now. Try again later."
        "ERROR_CREDENTIAL_ALREADY_IN_USE" ->
            "This sign-in is already linked to another account."
        "ERROR_INVALID_CREDENTIAL" -> "Sign-in details were wrong or expired. Try again."
        "ERROR_NETWORK_REQUEST_FAILED" ->
            "Couldn't reach the server. Check your connection and try again."
        "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL" ->
            "An account already exists with a different sign-in method."
        else -> genericFailure("sign in")
    }

    fun firestoreMessage(code: FirebaseFirestoreException.Code): String = when (code) {
        FirebaseFirestoreException.Code.PERMISSION_DENIED ->
            "You don't have permission to do that right now."
        FirebaseFirestoreException.Code.UNAVAILABLE ->
            "Couldn't reach the server. Check your internet and try again."
        FirebaseFirestoreException.Code.DEADLINE_EXCEEDED ->
            "That took too long. Try again."
        FirebaseFirestoreException.Code.NOT_FOUND ->
            "That item wasn't found. It may have been removed."
        FirebaseFirestoreException.Code.ALREADY_EXISTS ->
            "That already exists."
        FirebaseFirestoreException.Code.RESOURCE_EXHAUSTED ->
            "Too many requests. Wait a moment and try again."
        FirebaseFirestoreException.Code.UNAUTHENTICATED ->
            "You're not signed in. Sign in and try again."
        FirebaseFirestoreException.Code.CANCELLED ->
            "That was cancelled."
        else -> genericFailure()
    }

    fun forRawMessage(message: String?): String {
        val m = message?.trim().orEmpty()
        if (m.isBlank()) return genericFailure()
        if (looksLikeBareCode(m) || looksTechnical(m)) return genericFailure()

        return when {
            m.contains("PERMISSION_DENIED", ignoreCase = true) ||
                m.contains("Missing or insufficient permissions", ignoreCase = true) ->
                "You don't have permission to do that right now."

            m.contains("NETWORK_ERROR", ignoreCase = true) ||
                m.contains("Unable to resolve host", ignoreCase = true) ||
                m.contains("Failed to connect", ignoreCase = true) ||
                (m.contains("network", ignoreCase = true) && m.length < 100) ->
                "Couldn't reach the server. Check your connection and try again."

            m.contains("Sign in with Google", ignoreCase = true) ->
                "Sign in first, then try again."

            m.contains("DEVELOPER_ERROR", ignoreCase = true) ||
                m.contains("SHA-1", ignoreCase = true) ||
                m.contains("SHA1", ignoreCase = true) ->
                "Couldn't finish that right now. Try again in a moment."

            m.startsWith("ERROR_") -> firebaseAuthMessage(m)

            else -> m.takeIf { it.length <= 120 && !looksTechnical(it) } ?: genericFailure()
        }
    }

    fun genericFailure(action: String = "complete that"): String =
        "Something went wrong — we couldn't $action. Try again."

    private fun looksLikeBareCode(text: String): Boolean {
        if (text.matches(Regex("^\\d+$"))) return true
        if (text.matches(Regex("^[A-Z_]+:\\s*\\d+$"))) return true
        if (text.length <= 4 && text.all { it.isDigit() }) return true
        return false
    }

    private fun looksTechnical(text: String): Boolean {
        val t = text.lowercase()
        return t.contains("https://") ||
            t.contains("http://") ||
            t.contains("ssl") ||
            t.contains("certificate") ||
            t.contains("firebase") && t.contains("exception") ||
            t.contains("firestore") && t.contains("exception") ||
            t.startsWith("com.google.") ||
            t.contains("apiexception") ||
            t.contains("billingclient") ||
            t.contains("play console") ||
            t.contains("license test") ||
            t.contains("sha-1") ||
            t.contains("stack trace") ||
            Regex("""\b[A-Z][a-zA-Z]+(Exception|Error)\b""").containsMatchIn(text)
    }
}
