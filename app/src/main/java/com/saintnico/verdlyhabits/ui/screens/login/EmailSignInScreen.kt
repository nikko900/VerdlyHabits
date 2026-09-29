package com.saintnico.verdlyhabits.ui.screens.login

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.remote.auth.EmailAuthClient
import com.saintnico.verdlyhabits.ui.components.immersiveAuthBackground
import com.saintnico.verdlyhabits.ui.components.VerdlyLoadingIndicator
import com.saintnico.verdlyhabits.ui.theme.AccentGold
import com.saintnico.verdlyhabits.util.UserFacingErrors
import kotlinx.coroutines.launch

@Composable
fun EmailSignInScreen(
    onSignInSuccess: () -> Unit,
    onNeedsProfileSetup: () -> Unit,
    onBackToLogin: () -> Unit,
    onNavigateToSignUp: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val emailAuthClient = remember { EmailAuthClient(context) }
    val coroutineScope = rememberCoroutineScope()
    val keyboard = LocalSoftwareKeyboardController.current

    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    LaunchedEffect(Unit) { isVisible = true }

    fun attemptSignIn() {
        if (isLoading) return
        if (email.isBlank() || password.isBlank()) {
            Toast.makeText(context, "Enter your email and password", Toast.LENGTH_SHORT).show()
            return
        }
        isLoading = true
        keyboard?.hide()
        coroutineScope.launch {
            try {
                val (_, needsSetup) = emailAuthClient.signIn(email, password)
                if (needsSetup) onNeedsProfileSetup() else onSignInSuccess()
            } catch (e: Exception) {
                Toast.makeText(context, UserFacingErrors.message(e), Toast.LENGTH_LONG).show()
            } finally {
                isLoading = false
            }
        }
    }

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
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToLogin) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                }
            }

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(tween(700)) + slideInVertically(tween(700), initialOffsetY = { -40 })
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 28.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        text = "SIGN IN",
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = AccentGold.copy(alpha = 0.85f),
                            letterSpacing = 2.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Welcome back",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            color = Color.White,
                            fontWeight = FontWeight.Normal,
                            fontSize = 34.sp
                        )
                    )
                    Text(
                        text = "Sign in with the email you used to plant your account.",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color.White.copy(alpha = 0.55f),
                            fontSize = 14.sp
                        ),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.06f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.14f))
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    val fieldColors = authFieldColors(primary)
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        )
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        visualTransformation = if (passwordVisible) {
                            VisualTransformation.None
                        } else {
                            PasswordVisualTransformation()
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { attemptSignIn() }),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors,
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.Visibility
                                    else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Brush.horizontalGradient(listOf(primary, tertiary))),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            onClick = { attemptSignIn() },
                            modifier = Modifier.fillMaxSize(),
                            color = Color.Transparent,
                            enabled = !isLoading
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoading) {
                                    VerdlyLoadingIndicator(size = 24.dp)
                                } else {
                                    Text(
                                        text = "Sign in",
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            TextButton(
                onClick = onNavigateToSignUp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "New to Verdly? Create an account",
                    color = Color(0xFF8FD4AE),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
internal fun authFieldColors(primary: Color) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = primary,
    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
    focusedLabelColor = primary,
    unfocusedLabelColor = Color.White.copy(alpha = 0.4f),
    cursorColor = primary,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White.copy(alpha = 0.8f),
    focusedLeadingIconColor = Color.White.copy(alpha = 0.9f),
    unfocusedLeadingIconColor = Color.White.copy(alpha = 0.55f)
)
