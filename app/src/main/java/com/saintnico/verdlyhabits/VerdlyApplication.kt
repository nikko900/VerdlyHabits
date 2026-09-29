package com.saintnico.verdlyhabits

import android.app.Application
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.logInWith
import com.revenuecat.purchases.logOutWith
import com.saintnico.verdlyhabits.billing.RevenueCatBootstrap
import com.saintnico.verdlyhabits.i18n.LocaleHelper
import com.saintnico.verdlyhabits.preferences.ThemePreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class VerdlyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        runCatching {
            val stored = runBlocking { ThemePreference(this@VerdlyApplication).languageCode.first() }
            LocaleHelper.persist(this, stored)
        }
        RevenueCatBootstrap.configure(this)
        bindRevenueCatIdentityToFirebase()
    }

    /** Keeps the RevenueCat app user id in lockstep with Firebase auth. */
    private fun bindRevenueCatIdentityToFirebase() {
        if (!RevenueCatBootstrap.isConfigured()) return
        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val uid = auth.currentUser?.uid
            val purchases = Purchases.sharedInstance
            when {
                uid != null && purchases.appUserID != uid ->
                    purchases.logInWith(
                        uid,
                        onError = { Log.w("VerdlyRevenueCat", "logIn: ${it.message}") },
                        onSuccess = { _, _ -> },
                    )
                uid == null && !purchases.isAnonymous ->
                    purchases.logOutWith(
                        onError = { Log.w("VerdlyRevenueCat", "logOut: ${it.message}") },
                        onSuccess = { },
                    )
            }
        }
    }
}
