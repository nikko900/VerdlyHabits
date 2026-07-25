package com.saintnico.verdlyhabits.ui.screens.settings

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.saintnico.verdlyhabits.ui.components.profilepremium.LivePreviewPhoneFrame
import com.saintnico.verdlyhabits.ui.components.profilepremium.PremiumProfileHero
import com.saintnico.verdlyhabits.ui.components.profilepremium.ProfileCardThemePicker
import com.saintnico.verdlyhabits.ui.components.profilepremium.ProfileStreakArc
import com.saintnico.verdlyhabits.ui.theme.ProfileAccents
import com.saintnico.verdlyhabits.engine.GamificationEngine
import com.saintnico.verdlyhabits.ui.viewmodel.SettingsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.UserStatsUiState
import kotlinx.coroutines.launch
import kotlin.math.min

private fun profileMagnetism(
    displayName: String,
    username: String,
    photoUri: String?,
    motto: String,
    flair: String,
    bio: String,
): Float {
    var s = 0f
    if (displayName.isNotBlank()) s += 0.2f
    if (username.isNotBlank()) s += 0.26f
    if (!photoUri.isNullOrBlank()) s += 0.22f
    if (motto.isNotBlank()) s += 0.2f
    if (flair.isNotBlank()) s += 0.14f
    if (bio.isNotBlank()) s += min(0.18f, bio.length / 120f * 0.18f)
    return s.coerceIn(0f, 1f)
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EditProfileScreen(
    onBack: () -> Unit,
    onShowNotification: (String, Boolean) -> Unit,
    statsState: UserStatsUiState,
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val initialName by settingsViewModel.userName.collectAsState()
    val initialUsername by settingsViewModel.userUsername.collectAsState()
    val initialEmail by settingsViewModel.userEmail.collectAsState()
    val initialBio by settingsViewModel.userBio.collectAsState()
    val initialMotto by settingsViewModel.userMotto.collectAsState()
    val initialFavoritePlant by settingsViewModel.userFavoritePlant.collectAsState()
    val initialPhotoUri by settingsViewModel.userPhotoUri.collectAsState()
    val initialProfileAccent by settingsViewModel.userProfileAccent.collectAsState()
    val showRecentProof by settingsViewModel.showRecentProof.collectAsState()
    val isSaving by settingsViewModel.isSaving.collectAsState()

    LaunchedEffect(Unit) {
        settingsViewModel.errorEvent.collect { error: String ->
            onShowNotification(error, true)
        }
    }

    LaunchedEffect(Unit) {
        settingsViewModel.profileSaveSuccess.collect {
            onShowNotification("Profile saved. Your public card updates everywhere.", false)
            onBack()
        }
    }

    var displayName by remember { mutableStateOf(initialName) }
    var username by remember { mutableStateOf(initialUsername) }
    var bio by remember { mutableStateOf(initialBio) }
    var motto by remember { mutableStateOf(initialMotto) }
    var favoritePlant by remember { mutableStateOf(initialFavoritePlant) }
    var photoUri by remember { mutableStateOf(initialPhotoUri) }
    var profileAccentKey by remember { mutableStateOf(initialProfileAccent) }

    var isInitialized by remember { mutableStateOf(false) }
    LaunchedEffect(
        initialName,
        initialUsername,
        initialBio,
        initialMotto,
        initialFavoritePlant,
        initialPhotoUri,
        initialProfileAccent,
    ) {
        val hasAnySeed =
            initialName.isNotBlank() ||
                initialUsername.isNotBlank() ||
                initialBio.isNotBlank() ||
                initialMotto.isNotBlank() ||
                !initialPhotoUri.isNullOrBlank() ||
                initialProfileAccent.isNotBlank()
        if (!isInitialized && hasAnySeed) {
            displayName = initialName
            username = initialUsername
            bio = initialBio
            motto = initialMotto
            favoritePlant = initialFavoritePlant
            photoUri = initialPhotoUri
            profileAccentKey = initialProfileAccent
            isInitialized = true
        }
    }

    val selectedAccent = remember(profileAccentKey) { ProfileAccents.byKey(profileAccentKey) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val profileDir = java.io.File(context.filesDir, "profile").also { it.mkdirs() }
            val photoFile = java.io.File(profileDir, "profile_edit_tmp.jpg")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    photoFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
                photoUri = android.net.Uri.fromFile(photoFile).toString()
            } catch (_: Exception) {
                onShowNotification("Failed to process image", true)
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.background,
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        ),
    )

    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val stepLabels = listOf("Identity", "Story", "Preview")
    val magnetism = profileMagnetism(displayName, username, photoUri, motto, favoritePlant, bio)

    fun save() {
        settingsViewModel.saveProfile(
            displayName, username, initialEmail, bio, motto, favoritePlant, photoUri, profileAccentKey,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            "Edit profile",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = { save() },
                            enabled = !isSaving && username.isNotBlank(),
                        ) {
                            Text("Save", fontWeight = FontWeight.Bold, color = primaryColor)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                    ),
                )
            },
            containerColor = Color.Transparent,
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(backgroundGradient)
                    .padding(horizontal = 20.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    stepLabels.forEachIndexed { index, label ->
                        FilterChip(
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                            label = { Text(label, maxLines = 1) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) { page ->
                    when (page) {
                        0 -> Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            editorialHint(
                                "Your display name is public — pick anything you want, not your Google name.",
                                secondaryColor,
                            )
                            Spacer(Modifier.height(16.dp))
                            Box(
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor.copy(alpha = 0.1f))
                                    .clickable {
                                        photoPickerLauncher.launch(
                                            androidx.activity.result.PickVisualMediaRequest(
                                                ActivityResultContracts.PickVisualMedia.ImageOnly,
                                            ),
                                        )
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                com.saintnico.verdlyhabits.ui.components.ProfileAvatar(
                                    photoUri = photoUri,
                                    size = 120.dp,
                                    contentDescription = "Profile picture",
                                    fallbackTint = primaryColor,
                                    fallbackBackground = primaryColor.copy(alpha = 0.1f),
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Change picture",
                                        tint = Color.White.copy(alpha = 0.9f),
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Photo",
                                style = MaterialTheme.typography.labelLarge,
                                color = primaryColor,
                                fontWeight = FontWeight.Medium,
                            )
                            Spacer(Modifier.height(20.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                ),
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        "Public identity",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = primaryColor,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    ProfileTextField(
                                        value = displayName,
                                        onValueChange = { displayName = it },
                                        label = "Display name",
                                        icon = Icons.Default.Person,
                                        placeholder = "Name on your card and in challenges",
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    ProfileTextField(
                                        value = username,
                                        onValueChange = {
                                            username = it.filter { c -> c.isLetterOrDigit() || c == '_' }.lowercase()
                                        },
                                        label = "Username",
                                        icon = Icons.Default.AlternateEmail,
                                        placeholder = "e.g. morning_rival",
                                    )
                                }
                            }
                        }

                        1 -> Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 24.dp),
                        ) {
                            editorialHint(
                                "A sharp card gets taps, invites, and challenge joins.",
                                secondaryColor,
                            )
                            Spacer(Modifier.height(16.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                ),
                            ) {
                                Column(modifier = Modifier.padding(20.dp)) {
                                    Text(
                                        "Story",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = secondaryColor,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    ProfileTextField(
                                        value = motto,
                                        onValueChange = { motto = it },
                                        label = "Headline",
                                        icon = Icons.Default.AutoAwesome,
                                        placeholder = "One line people remember.",
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    ProfileTextField(
                                        value = favoritePlant,
                                        onValueChange = { favoritePlant = it },
                                        label = "Flair tag",
                                        icon = Icons.Default.Star,
                                        placeholder = "A vibe, discipline, or motif.",
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    ProfileTextField(
                                        value = bio,
                                        onValueChange = { bio = it },
                                        label = "About you",
                                        icon = Icons.Default.Info,
                                        singleLine = false,
                                        maxLines = 4,
                                        placeholder = "What you optimize for—short and confident.",
                                    )
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                ),
                            ) {
                                Column(Modifier.padding(20.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        Icon(
                                            Icons.Default.Visibility,
                                            contentDescription = null,
                                            tint = primaryColor,
                                            modifier = Modifier.size(22.dp),
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(Modifier.weight(1f)) {
                                            Text(
                                                "Recent proof on profile",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onBackground,
                                            )
                                            Text(
                                                if (showRecentProof) {
                                                    "Connections see your latest challenge proof photos in the story-style reel."
                                                } else {
                                                    "Hidden — only you see recent proof. Connections won't see the reel."
                                                },
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                                lineHeight = 18.sp,
                                            )
                                        }
                                        Switch(
                                            checked = showRecentProof,
                                            onCheckedChange = { settingsViewModel.setShowRecentProof(it) },
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(20.dp))
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                                ),
                            ) {
                                Column(Modifier.padding(20.dp)) {
                                    ProfileCardThemePicker(
                                        selectedKey = profileAccentKey,
                                        onSelect = { profileAccentKey = it.key },
                                    )
                                }
                            }
                        }

                        else -> Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(bottom = 24.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                ProfileStreakArc(progress = magnetism, accent = selectedAccent.highlight)
                                Spacer(Modifier.width(14.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        "Profile pull",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                    Text(
                                        "Fill the story tab to max this ring—great cards get shared.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "${(magnetism * 100).toInt()}%",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 20.sp,
                                        color = primaryColor,
                                    )
                                    Text(
                                        "LV ${statsState.level}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Live preview",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                            )
                            Spacer(Modifier.height(8.dp))
                            LivePreviewPhoneFrame {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            scaleX = 0.92f
                                            scaleY = 0.92f
                                        },
                                ) {
                                    PremiumProfileHero(
                                        displayName = displayName,
                                        username = username,
                                        tagline = motto.ifBlank { bio },
                                        photoUrl = photoUri,
                                        level = statsState.level,
                                        xp = statsState.totalXp,
                                        levelTitle = GamificationEngine.levelTitle(statsState.level),
                                        accent = selectedAccent,
                                    )
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Preview uses your headline first, then your about text. Level and XP reflect your real progress.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { save() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp),
                    enabled = !isSaving && username.isNotBlank(),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Syncing…", fontWeight = FontWeight.Bold)
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Save profile", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    }
                }
            }
        }

        if (isSaving) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center,
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(color = primaryColor)
                        Spacer(Modifier.height(16.dp))
                        Text("Saving…", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun editorialHint(text: String, accent: Color) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = accent.copy(alpha = 0.9f),
        fontWeight = FontWeight.Medium,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
fun ProfileTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    readOnly: Boolean = false,
    enabled: Boolean = true,
    singleLine: Boolean = true,
    maxLines: Int = 1,
    placeholder: String = "",
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = {
            if (placeholder.isNotEmpty()) {
                Text(placeholder, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        singleLine = singleLine,
        maxLines = maxLines,
        readOnly = readOnly,
        enabled = enabled,
        leadingIcon = {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
            )
        },
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
            disabledBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
            disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        ),
    )
}
