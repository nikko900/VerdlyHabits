package com.saintnico.verdlyhabits.data.remote.firestore

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.gson.Gson
import kotlinx.coroutines.tasks.await

class GoalsCloudRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val gson = Gson()

    suspend fun saveSnapshot(snapshot: GoalsCloudSnapshot) {
        val user = auth.currentUser ?: return
        val json = gson.toJson(snapshot)
        firestore.collection("users").document(user.uid)
            .set(mapOf(FIELD_GOALS_JSON to json), SetOptions.merge())
            .await()
    }

    suspend fun fetchSnapshot(): GoalsCloudSnapshot? {
        val user = auth.currentUser ?: return null
        val snap = firestore.collection("users").document(user.uid).get().await()
        val json = snap.getString(FIELD_GOALS_JSON) ?: return null
        return try {
            gson.fromJson(json, GoalsCloudSnapshot::class.java)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse goalsJson", e)
            null
        }
    }

    companion object {
        private const val TAG = "GoalsCloudRepository"
        const val FIELD_GOALS_JSON = "goalsJson"
    }
}
