package com.saintnico.verdlyhabits.ui.screens.login

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.saintnico.verdlyhabits.ui.components.AccountCreatedWelcome
import com.saintnico.verdlyhabits.ui.components.immersiveAuthBackground

@Composable
fun AccountCreatedScreen(
    onContinue: () -> Unit,
) {
    val displayName = remember {
        val user = FirebaseAuth.getInstance().currentUser
        user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .immersiveAuthBackground()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        AccountCreatedWelcome(
            displayName = displayName,
            onContinue = onContinue,
        )
    }
}
