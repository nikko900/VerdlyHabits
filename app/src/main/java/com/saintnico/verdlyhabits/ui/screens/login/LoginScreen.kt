package com.saintnico.verdlyhabits.ui.screens.login

import android.app.Activity
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import com.saintnico.verdlyhabits.MainActivity
import com.saintnico.verdlyhabits.R
import com.saintnico.verdlyhabits.data.remote.auth.GoogleAuthClient
import com.saintnico.verdlyhabits.ui.components.immersiveAuthBackground
import com.saintnico.verdlyhabits.ui.theme.AccentGold
import com.saintnico.verdlyhabits.ui.viewmodel.SettingsViewModel
import com.saintnico.verdlyhabits.util.UserFacingErrors
import kotlinx.coroutines.launch

private val AuthPanelFill = Color.White.copy(alpha = 0.06f)
private val AuthPanelStroke = Color.White.copy(alpha = 0.14f)
private val AuthMuted = Color.White.copy(alpha = 0.55f)

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onNewUser: () -> Unit,
    onNavigateToSignUp: () -> Unit = {},
    onNavigateToEmailSignIn: () -> Unit = {},
) {
    var isLoading by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    BackHandler { /* Do nothing */ }

    val context = LocalContext.current
    val view = LocalView.current
    val googleAuthClient = remember { GoogleAuthClient(context) }
    val coroutineScope = rememberCoroutineScope()

    val settingsViewModel: SettingsViewModel = viewModel()
    var showLanguageDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val window = (view.context as? Activity)?.window
        val previousStatus = window?.statusBarColor
        val previousNav = window?.navigationBarColor
        if (window != null) {
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
        onDispose {
            if (window != null && previousStatus != null && previousNav != null) {
                window.statusBarColor = previousStatus
                window.navigationBarColor = previousNav
            }
        }
    }

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
                        if (isTrulyNewUser) onNewUser() else onLoginSuccess()
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
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
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

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .immersiveAuthBackground()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { showLanguageDialog = true }) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = "Language",
                        tint = Color.White.copy(alpha = 0.85f)
                    )
                }
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(900)) + slideInVertically(tween(900), initialOffsetY = { -80 })
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    Text(
                        text = "WELCOME BACK",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = AccentGold.copy(alpha = 0.85f),
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .scale(orbPulse)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(primary.copy(alpha = 0.5f), Color.Transparent)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
                                        listOf(primary.copy(alpha = 0.4f), tertiary.copy(alpha = 0.22f))
                                    )
                                )
                                .padding(20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Eco,
                                contentDescription = "Logo",
                                tint = Color.White,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset(y = plantOffset.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(22.dp))
                    Text(
                        text = "Verdly",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 52.sp,
                            letterSpacing = (-2).sp,
                            color = Color.White,
                            fontWeight = FontWeight.Normal
                        )
                    )
                    Text(
                        text = "Habits — with a little green in every day.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = Color.White.copy(alpha = 0.62f),
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center
                        ),
                        modifier = Modifier.padding(top = 10.dp, start = 12.dp, end = 12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(1000, delayMillis = 180)) +
                    slideInVertically(tween(1000, delayMillis = 180), initialOffsetY = { 90 })
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    color = AuthPanelFill,
                    border = BorderStroke(1.dp, AuthPanelStroke),
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Continue to your garden",
                            style = MaterialTheme.typography.titleMedium.copy(
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 17.sp
                            )
                        )
                        Text(
                            text = "Pick how you want to sign in",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = AuthMuted,
                                fontSize = 13.sp
                            ),
                            modifier = Modifier.padding(top = 6.dp, bottom = 20.dp)
                        )

                        // Google — primary CTA
                        Surface(
                            onClick = {
                                if (!isLoading) {
                                    isLoading = true
                                    signInLauncher.launch(googleAuthClient.getSignInIntent())
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            shadowElevation = 2.dp,
                            enabled = !isLoading
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        color = Color(0xFF4285F4),
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Image(
                                            painter = painterResource(R.drawable.ic_google_g),
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Continue with Google",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF1F1F1F)
                                        )
                                    }
                                }
                            }
                        }

                        AuthOrDivider(modifier = Modifier.padding(vertical = 16.dp))

                        // Email
                        Surface(
                            onClick = { if (!isLoading) onNavigateToEmailSignIn() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Transparent,
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.28f)),
                            enabled = !isLoading
                        ) {
                            Row(
                                modifier = Modifier.fillMaxSize(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.9f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Sign in with email",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { if (!isLoading) onNavigateToSignUp() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "New to Verdly? Create an account",
                                color = Color(0xFF8FD4AE),
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Text(
                text = "By continuing you agree to Verdly’s terms & privacy practices.",
                style = MaterialTheme.typography.bodySmall.copy(
                    color = Color.White.copy(alpha = 0.38f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp
                ),
                modifier = Modifier.padding(top = 16.dp, start = 12.dp, end = 12.dp)
            )
        }
    }

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Language") },
            text = {
                Column {
                    val languages = listOf(
                        "en" to "English",
                        "es" to "Español",
                        "de" to "Deutsch",
                        "sw" to "Kiswahili"
                    )
                    languages.forEach { (code, name) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    settingsViewModel.setLanguage(code)
                                    showLanguageDialog = false
                                    val intent = Intent(context, MainActivity::class.java).apply {
                                        addFlags(
                                            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                                Intent.FLAG_ACTIVITY_NEW_TASK
                                        )
                                    }
                                    context.startActivity(intent)
                                    if (context is Activity) context.finish()
                                }
                                .padding(vertical = 12.dp)
                        ) { Text(name) }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun AuthOrDivider(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color.White.copy(alpha = 0.14f),
            thickness = 1.dp
        )
        Text(
            text = "or",
            color = Color.White.copy(alpha = 0.4f),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            color = Color.White.copy(alpha = 0.14f),
            thickness = 1.dp
        )
    }
}
