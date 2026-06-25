package com.saintnico.verdlyhabits.ui.screens.login

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.Intent
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.saintnico.verdlyhabits.util.UserFacingErrors
import com.saintnico.verdlyhabits.MainActivity
import com.saintnico.verdlyhabits.data.remote.auth.GoogleAuthClient
import com.saintnico.verdlyhabits.ui.components.immersiveAuthBackground
import com.saintnico.verdlyhabits.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNewUser: () -> Unit,
) {
    var isLoading by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    // Disable back button entirely
    BackHandler { /* Do nothing */ }

    val context = LocalContext.current
    val googleAuthClient = remember { GoogleAuthClient(context) }
    val coroutineScope = rememberCoroutineScope()

    val settingsViewModel: SettingsViewModel = viewModel()
    var showLanguageDialog by remember { mutableStateOf(false) }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            val idToken = account?.idToken
            if (idToken != null) {
                coroutineScope.launch {
                    val resultPair = googleAuthClient.firebaseAuthWithGoogle(idToken)
                    isLoading = false
                    if (resultPair != null) {
                        val (_, isTrulyNewUser) = resultPair
                        if (isTrulyNewUser) {
                            onNewUser()
                        } else {
                            onLoginSuccess()
                        }
                    } else {
                        Toast.makeText(
                            context,
                            UserFacingErrors.genericFailure("finish signing in"),
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            } else {
                isLoading = false
                Toast.makeText(context, "No ID token received", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            isLoading = false
            Toast.makeText(
                context,
                UserFacingErrors.googleSignInMessage(e.statusCode),
                Toast.LENGTH_LONG,
            ).show()
        }
    }

    LaunchedEffect(Unit) { isVisible = true }

    val infiniteTransition = rememberInfiniteTransition(label = "plantBounce")
    val plantOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "plantOffset"
    )
    val orbPulse by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(2600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbPulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .immersiveAuthBackground()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { showLanguageDialog = true }) {
                    Icon(imageVector = Icons.Default.Language, contentDescription = "Language", tint = Color.White.copy(alpha = 0.85f))
                }
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(1000)) + slideInVertically(tween(1000), initialOffsetY = { -100 })
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val primary = MaterialTheme.colorScheme.primary
                    val tertiary = MaterialTheme.colorScheme.tertiary
                    Box(
                        modifier = Modifier
                            .size(108.dp)
                            .scale(orbPulse)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(primary.copy(alpha = 0.45f), Color.Transparent)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(88.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(listOf(primary.copy(alpha = 0.35f), tertiary.copy(alpha = 0.2f)))
                                )
                                .padding(18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Eco,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier.fillMaxSize().offset(y = plantOffset.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Verdly",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 48.sp,
                            letterSpacing = (-2).sp,
                            color = Color.White,
                            fontWeight = FontWeight.Normal
                        )
                    )
                    Text(
                        text = "Habits — with a little green in every day.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 15.sp
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            Surface(
                onClick = {
                    if (!isLoading) {
                        isLoading = true
                        signInLauncher.launch(googleAuthClient.getSignInIntent())
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(50),
                color = Color.White,
                shadowElevation = 4.dp,
                enabled = !isLoading
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(26.dp),
                            color = Color(0xFF4285F4),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "G",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color(0xFF4285F4)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                "Continue with Google",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }

            Text(
                text = "One tap — your Google account is your Verdly identity.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.45f),
                modifier = Modifier.padding(top = 20.dp, start = 16.dp, end = 16.dp),
            )
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Language") },
            text = {
                Column {
                    val languages = listOf("en" to "English", "es" to "Español", "de" to "Deutsch", "sw" to "Kiswahili")
                    languages.forEach { (code, name) ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                settingsViewModel.setLanguage(code)
                                showLanguageDialog = false
                                val intent = Intent(context, MainActivity::class.java).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)
                                if (context is android.app.Activity) context.finish()
                            }.padding(vertical = 12.dp)
                        ) { Text(name) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLanguageDialog = false }) { Text("Cancel") } }
        )
    }
}
