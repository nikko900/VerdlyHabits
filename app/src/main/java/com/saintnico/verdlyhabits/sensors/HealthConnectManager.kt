package com.saintnico.verdlyhabits.sensors

import android.content.Context

class HealthConnectManager(private val context: Context) {
    fun isAvailable(): Boolean = false
    fun hasAllPermissions(): Boolean = false
    
    suspend fun getChangesToken(): String = ""
    suspend fun getChanges(token: String): String = ""
}
