package com.saintnico.verdlyhabits.ui.screens.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.saintnico.verdlyhabits.engine.ProfileCompletionEngine
import com.saintnico.verdlyhabits.ui.components.profilepremium.LivePreviewPhoneFrame
import com.saintnico.verdlyhabits.ui.components.profilepremium.PremiumProfileHero
import com.saintnico.verdlyhabits.ui.components.profilepremium.ProfileCardThemePicker
import com.saintnico.verdlyhabits.ui.components.profilepremium.ProfileStreakArc
import com.saintnico.verdlyhabits.ui.screens.settings.ProfileTextField
import com.saintnico.verdlyhabits.ui.theme.ProfileAccents
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileSetupScreen(
    onComplete: () -> Unit,
    onRequireSignIn: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val initialEmail by settingsViewModel.userEmail.collectAsState()
    val isSaving by settingsViewModel.isSaving.collectAsState()

    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var motto by remember { mutableStateOf("Show up with intention.") }
    var bio by remember { mutableStateOf("Planting habits, growing roots.") }
    var favoritePlant by remember { mutableStateOf("Oak") }
    var photoUri by remember { mutableStateOf<String?>(null) }
    var profileAccentKey by remember { mutableStateOf("sage") }

    val context = LocalContext.current
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val cacheFile = File(context.cacheDir, "profile_setup_tmp.jpg")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    cacheFile.outputStream().use { output -> input.copyTo(output) }
                }
                photoUri = Uri.fromFile(cacheFile).toString()
            } catch (_: Exception) {
            }
        }
    }

    if (FirebaseAuth.getInstance().currentUser == null) {
        LaunchedEffect(Unit) { onRequireSignIn() }
        return
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val selectedAccent = remember(profileAccentKey) { ProfileAccents.byKey(profileAccentKey) }
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val stepLabels = listOf("Identity", "Story", "Preview")
    val magnetism = ProfileCompletionEngine.completionFraction(
        name, username, photoUri, motto, bio, favoritePlant,
    )

    var hasAttemptedSave by remember { mutableStateOf(false) }
    LaunchedEffect(isSaving) {
        if (hasAttemptedSave && !isSaving) onComplete()
    }

    fun save() {
        if (username.isBlank() || name.isBlank()) return
        hasAttemptedSave = true
        settingsViewModel.saveProfile(
            name, username, initialEmail, bio, motto, favoritePlant, photoUri, profileAccentKey,
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Set up your profile",
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.Bold,
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                stepLabels.forEachIndexed { index, label ->
                    FilterChip(
                        selected = pagerState.currentPage == index,
                        onClick = { scope.launch { pagerState.animateScrollToPage(index) } },
                        label = { Text(label, fontFamily = dmSansFamily) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { magnetism },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = primaryColor,
            )
            Text(
                "Profile pull ${(magnetism * 100).toInt()}% — rivals engage with complete cards.",
                fontFamily = dmSansFamily,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
            )

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                when (page) {
                    0 -> Column(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(
                            "Who are you in the arena?",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(20.dp))
                        Box(
                            Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(primaryColor.copy(alpha = 0.1f))
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (photoUri != null) {
                                AsyncImage(photoUri, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Default.CameraAlt, null, tint = primaryColor, modifier = Modifier.size(36.dp))
                            }
                        }
                        Spacer(Modifier.height(20.dp))
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            ),
                        ) {
                            Column(Modifier.padding(18.dp)) {
                                ProfileTextField(
                                    value = name,
                                    onValueChange = { name = it },
                                    label = "Display name",
                                    icon = Icons.Default.Person,
                                    placeholder = "Your public name",
                                )
                                Spacer(Modifier.height(14.dp))
                                ProfileTextField(
                                    value = username,
                                    onValueChange = { username = it.filter { c -> c.isLetterOrDigit() || c == '_' }.lowercase() },
                                    label = "Username",
                                    icon = Icons.Default.AlternateEmail,
                                    placeholder = "e.g. morning_rival",
                                )
                            }
                        }
                    }
                    1 -> Column(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Text(
                            "Your story",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(16.dp))
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            ),
                        ) {
                            Column(Modifier.padding(18.dp)) {
                                ProfileTextField(
                                    value = motto,
                                    onValueChange = { motto = it },
                                    label = "Headline",
                                    icon = Icons.Default.AutoAwesome,
                                    placeholder = "One line people remember",
                                )
                                Spacer(Modifier.height(14.dp))
                                ProfileTextField(
                                    value = favoritePlant,
                                    onValueChange = { favoritePlant = it },
                                    label = "Flair tag",
                                    icon = Icons.Default.Star,
                                    placeholder = "Your vibe or motif",
                                )
                                Spacer(Modifier.height(14.dp))
                                ProfileTextField(
                                    value = bio,
                                    onValueChange = { bio = it },
                                    label = "About you",
                                    icon = Icons.Default.Info,
                                    placeholder = "What you're building",
                                    singleLine = false,
                                    maxLines = 3,
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Card(
                            Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            ),
                        ) {
                            Column(Modifier.padding(18.dp)) {
                                ProfileCardThemePicker(
                                    selectedKey = profileAccentKey,
                                    onSelect = { profileAccentKey = it.key },
                                )
                            }
                        }
                    }
                    else -> Column(
                        Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProfileStreakArc(progress = magnetism, accent = selectedAccent.highlight)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Live preview", fontFamily = dmSansFamily, fontWeight = FontWeight.Bold)
                                Text(
                                    "This is how connections see you.",
                                    fontFamily = dmSansFamily,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        LivePreviewPhoneFrame {
                            PremiumProfileHero(
                                displayName = name.ifBlank { username.ifBlank { "Rival" } },
                                username = username.ifBlank { "rival" },
                                tagline = motto.ifBlank { bio },
                                photoUrl = photoUri,
                                level = 0,
                                xp = 0,
                                accent = selectedAccent,
                            )
                        }
                    }
                }
            }

            Row(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                if (pagerState.currentPage > 0) {
                    OutlinedButton(
                        onClick = { scope.launch { pagerState.animateScrollToPage(pagerState.currentPage - 1) } },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Back", fontFamily = dmSansFamily)
                    }
                    Spacer(Modifier.width(10.dp))
                }
                Button(
                    onClick = {
                        if (pagerState.currentPage < 2) {
                            scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                        } else {
                            save()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isSaving && (pagerState.currentPage < 2 || (username.isNotBlank() && name.isNotBlank())),
                    shape = RoundedCornerShape(16.dp),
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text(
                            if (pagerState.currentPage < 2) "Continue" else "Enter Verdly",
                            fontFamily = dmSansFamily,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
