package com.saintnico.verdlyhabits.session

import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/** Emits the current Firebase uid whenever auth state changes (sign-in, sign-out, account switch). */
fun firebaseAuthUidFlow(): Flow<String?> = callbackFlow {
    val auth = FirebaseAuth.getInstance()
    val listener = FirebaseAuth.AuthStateListener { a ->
        trySend(a.currentUser?.uid)
    }
    auth.addAuthStateListener(listener)
    trySend(auth.currentUser?.uid)
    awaitClose { auth.removeAuthStateListener(listener) }
}
