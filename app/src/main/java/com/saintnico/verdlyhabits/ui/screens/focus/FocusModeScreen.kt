package com.saintnico.verdlyhabits.ui.screens.focus

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.EaseInOutSine
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.R
import com.saintnico.verdlyhabits.monetization.PaywallTrigger
import com.saintnico.verdlyhabits.ui.components.share.shareStreakCardWithImage
import com.saintnico.verdlyhabits.ui.screens.home.HabitItem
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import kotlinx.coroutines.delay
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import android.provider.Settings
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

private val DURATION_OPTIONS = listOf(15, 25, 30, 45, 60)

private val DURATION_LABELS = mapOf(
    15 to "Quick",
    25 to "Pomodoro",
    30 to "Deep",
    45 to "Flow",
    60 to "Master"
)

private enum class FocusPhase {
    Setup,
    Breathing,
    Active,
    Complete
}

private val MID_SESSION_QUOTES = listOf(
    "The plant doesn't rush. Neither should you.",
    "Every minute is a root going deeper.",
    "Distraction is the enemy of growth.",
    "You chose to be here. That already matters.",
    "Small sessions, compounded. That's the secret."
)

@Composable
private fun FocusDarkAnimatedGradient(
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val c1 = Color(0xFF0A100D)
    val c2 = Color(0xFF0D1F12)
    val c3 = Color(0xFF091510)
    if (reduceMotion) {
        Box(
            modifier = modifier.background(Brush.verticalGradient(listOf(c1, c2, c3))),
            content = content
        )
        return
    }
    val inf = rememberInfiniteTransition(label = "focus_bg")
    val stage by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "stage"
    )
    val top = when {
        stage < 1f / 3f -> lerp(c1, c2, (stage * 3f).coerceIn(0f, 1f))
        stage < 2f / 3f -> lerp(c2, c3, ((stage - 1f / 3f) * 3f).coerceIn(0f, 1f))
        else -> lerp(c3, c1, ((stage - 2f / 3f) * 3f).coerceIn(0f, 1f))
    }
    val mid = when {
        stage < 1f / 3f -> lerp(c2, c3, (stage * 3f).coerceIn(0f, 1f))
        stage < 2f / 3f -> lerp(c3, c1, ((stage - 1f / 3f) * 3f).coerceIn(0f, 1f))
        else -> lerp(c1, c2, ((stage - 2f / 3f) * 3f).coerceIn(0f, 1f))
    }
    Box(
        modifier = modifier.background(Brush.verticalGradient(listOf(top, mid, c2))),
        content = content
    )
}

@Composable
private fun PhosphorIcon(
    drawableId: Int,
    contentDescription: String?,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Icon(
        painter = painterResource(drawableId),
        contentDescription = contentDescription,
        modifier = modifier,
        tint = tint
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusModeScreen(
    habits: List<HabitItem>,
    preLinkedHabitId: String? = null,
    hasFullAccess: Boolean = true,
    onRequestPaywall: (PaywallTrigger) -> Unit = {},
    onSessionComplete: (minutes: Int, xpEarned: Int, habitId: String?) -> Unit,
    onBack: () -> Unit,
    onOpenGarden: () -> Unit = {}
) {
    val context = LocalContext.current
    val reduceMotion = remember(context) {
        try {
            val cr = context.contentResolver
            Settings.Global.getFloat(cr, Settings.Global.TRANSITION_ANIMATION_SCALE, 1f) == 0f ||
                Settings.Global.getFloat(cr, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        } catch (_: Exception) {
            false
        }
    }
    val soundManager = remember { AmbientSoundManager(context) }
    var selectedSound by remember { mutableStateOf(AmbientSoundManager.AmbientSound.NONE) }
    var ambientVolume by remember { mutableStateOf(0.7f) }
    var ambientDownloading by remember { mutableStateOf(false) }
    var ambientDownloadHint by remember { mutableStateOf<String?>(null) }

    val onAmbientSelected: (AmbientSoundManager.AmbientSound) -> Unit = { sound ->
        if (sound != AmbientSoundManager.AmbientSound.NONE && !hasFullAccess) {
            onRequestPaywall(PaywallTrigger.FocusSounds)
        } else {
            selectedSound = sound
        }
    }

    DisposableEffect(Unit) {
        onDispose { soundManager.release() }
    }

    LaunchedEffect(ambientVolume) {
        soundManager.setVolume(ambientVolume)
    }

    var phase by remember { mutableStateOf(FocusPhase.Setup) }
    var selectedMinutes by remember { mutableStateOf(25) }
    var selectedHabitId by remember { mutableStateOf(preLinkedHabitId) }
    var timeLeftSeconds by remember { mutableStateOf(25 * 60) }
    var showAbandonSheet by remember { mutableStateOf(false) }
    var isWilting by remember { mutableStateOf(false) }
    var sessionLogged by remember { mutableStateOf(false) }

    LaunchedEffect(phase, selectedSound) {
        when (phase) {
            FocusPhase.Setup -> {
                soundManager.stop()
                if (selectedSound != AmbientSoundManager.AmbientSound.NONE && hasFullAccess) {
                    ambientDownloading = !soundManager.isCached(selectedSound)
                    ambientDownloadHint = if (ambientDownloading) "Downloading ${selectedSound.label}…" else null
                    soundManager.playAsync(selectedSound)
                    ambientDownloading = false
                    ambientDownloadHint = null
                    delay(3000)
                    if (phase == FocusPhase.Setup) soundManager.stop()
                }
            }
            FocusPhase.Breathing, FocusPhase.Active -> {
                ambientDownloading = !soundManager.isCached(selectedSound) &&
                    selectedSound != AmbientSoundManager.AmbientSound.NONE
                ambientDownloadHint = if (ambientDownloading) "Downloading ${selectedSound.label}…" else null
                soundManager.playAsync(selectedSound)
                ambientDownloading = false
                ambientDownloadHint = null
            }
            FocusPhase.Complete -> Unit
        }
    }

    val progress by remember(timeLeftSeconds, selectedMinutes) {
        derivedStateOf { 1f - timeLeftSeconds.toFloat() / (selectedMinutes * 60) }
    }

    // Timer logic
    LaunchedEffect(phase) {
        if (phase == FocusPhase.Active) {
            timeLeftSeconds = selectedMinutes * 60
            while (timeLeftSeconds > 0 && phase == FocusPhase.Active) {
                delay(1000)
                timeLeftSeconds--
            }
            if (timeLeftSeconds == 0) {
                phase = FocusPhase.Complete
                val xp = 50 + selectedMinutes

                // Vibrate: 3 short bursts
                try {
                    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val vm = context.getSystemService(android.content.Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                        vm.defaultVibrator
                    } else {
                        @Suppress("DEPRECATION")
                        context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as Vibrator
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        vibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 100, 200, 100, 300), -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(longArrayOf(0, 200, 100, 200, 100, 300), -1)
                    }
                } catch (_: Exception) {}

                soundManager.playCompletionChime()
                soundManager.fadeOutAndStop(2000)

                onSessionComplete(selectedMinutes, xp, selectedHabitId)
            }
        }
    }

    val plantProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(800),
        label = "plant_progress"
    )

    LaunchedEffect(phase) {
        if (phase == FocusPhase.Setup) {
            sessionLogged = false
        }
        if (phase == FocusPhase.Complete && !sessionLogged) {
            sessionLogged = true
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val dao = com.saintnico.verdlyhabits.data.local.AppDatabase.getDatabase(context).focusSessionDao()
                    dao.insert(
                        com.saintnico.verdlyhabits.data.local.focus.FocusSessionEntity(
                            id = java.util.UUID.randomUUID().toString(),
                            habitId = selectedHabitId,
                            durationMinutes = selectedMinutes,
                            completedAt = System.currentTimeMillis(),
                            xpEarned = 50 + selectedMinutes
                        )
                    )
                } catch (_: Exception) {
                }
            }
        }
    }

    val activeHabits = habits.filter { !it.isArchived && !it.isPaused }
    val linkedHabit = activeHabits.find { it.id == selectedHabitId }

    var sessionQuote by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(phase) {
        sessionQuote = when (phase) {
            FocusPhase.Active -> MID_SESSION_QUOTES.random()
            else -> null
        }
    }

    val wiltProgress by animateFloatAsState(
        targetValue = if (isWilting) 1f else 0f,
        animationSpec = tween(1500),
        label = "wilt"
    )

    val linearProgress = progress

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        when (phase) {
            FocusPhase.Setup -> {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SetupView(
                        habits = activeHabits,
                        selectedMinutes = selectedMinutes,
                        selectedHabitId = selectedHabitId,
                        selectedSound = selectedSound,
                        ambientVolume = ambientVolume,
                        reduceMotion = reduceMotion,
                        soundManager = soundManager,
                        ambientDownloadHint = ambientDownloadHint,
                        onSelectMinutes = { selectedMinutes = it },
                        onSelectHabit = { selectedHabitId = it },
                        onSoundChange = onAmbientSelected,
                        onVolumeChange = { ambientVolume = it },
                        onBegin = { phase = FocusPhase.Breathing },
                        onBack = onBack
                    )
                }
            }

            FocusPhase.Breathing -> FocusDarkAnimatedGradient(reduceMotion, Modifier.fillMaxSize()) {
                BreathingView(
                    reduceMotion = reduceMotion,
                    onSkip = { phase = FocusPhase.Active },
                    onFinished = { phase = FocusPhase.Active }
                )
            }

            FocusPhase.Active -> FocusDarkAnimatedGradient(reduceMotion, Modifier.fillMaxSize()) {
                ActiveView(
                    progress = plantProgress,
                    linearProgress = linearProgress,
                    timeLeftSeconds = timeLeftSeconds,
                    linkedHabit = linkedHabit,
                    reduceMotion = reduceMotion,
                    isWilting = isWilting,
                    wiltProgress = wiltProgress,
                    selectedSound = selectedSound,
                    ambientVolume = ambientVolume,
                    sessionQuote = sessionQuote,
                    soundManager = soundManager,
                    ambientDownloadHint = ambientDownloadHint,
                    onSoundChange = onAmbientSelected,
                    onVolumeChange = { ambientVolume = it },
                    onAbandon = { showAbandonSheet = true }
                )
            }

            FocusPhase.Complete -> FocusDarkAnimatedGradient(reduceMotion, Modifier.fillMaxSize()) {
                CompleteView(
                    minutes = selectedMinutes,
                    xpEarned = 50 + selectedMinutes,
                    linkedHabit = linkedHabit,
                    reduceMotion = reduceMotion,
                    onDone = onBack,
                    onShare = {
                        shareStreakCardWithImage(
                            context,
                            headline = "Focus session complete",
                            habitName = linkedHabit?.title ?: "My habits",
                        )
                    },
                    onAnother = {
                        selectedSound = AmbientSoundManager.AmbientSound.NONE
                        phase = FocusPhase.Setup
                        timeLeftSeconds = selectedMinutes * 60
                    },
                    onOpenGarden = onOpenGarden
                )
            }
        }
    }

    if (showAbandonSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAbandonSheet = false },
            containerColor = Color(0xFF1A2E22)
        ) {
            AbandonSheetContent(
                reduceMotion = reduceMotion,
                onStay = { showAbandonSheet = false },
                onLeave = {
                    showAbandonSheet = false
                    isWilting = true
                }
            )
        }
    }

    LaunchedEffect(isWilting) {
        if (!isWilting) return@LaunchedEffect
        kotlinx.coroutines.delay(1500)
        soundManager.stop()
        phase = FocusPhase.Setup
        isWilting = false
        onSessionComplete((selectedMinutes - timeLeftSeconds / 60).coerceAtLeast(0), 0, selectedHabitId)
    }
}

@Composable
private fun BreathingView(
    reduceMotion: Boolean,
    onSkip: () -> Unit,
    onFinished: () -> Unit
) {
    var phaseText by remember { mutableStateOf("Breathe in...") }
    val scale by animateFloatAsState(
        targetValue = if (phaseText.startsWith("Breathe in")) 1f else 0.6f,
        animationSpec = tween(if (reduceMotion) 0 else 4000, easing = FastOutSlowInEasing),
        label = "breath_scale"
    )
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            onFinished()
            return@LaunchedEffect
        }
        repeat(3) {
            phaseText = "Breathe in..."
            kotlinx.coroutines.delay(4000)
            phaseText = "Breathe out..."
            kotlinx.coroutines.delay(4000)
        }
        phaseText = "Begin"
        kotlinx.coroutines.delay(600)
        onFinished()
    }
    Box(Modifier.fillMaxSize()) {
        TextButton(
            onClick = onSkip,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Text("Skip", color = Color(0xFF74C69D), fontFamily = dmSansFamily)
        }
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.size(220.dp)) {
                    drawCircle(Color(0xFF2D6A4F).copy(alpha = 0.35f), radius = size.minDimension / 2f * scale)
                }
            }
            Spacer(Modifier.height(24.dp))
            Crossfade(targetState = phaseText, label = "breath_text") { t ->
                Text(
                    text = t,
                    fontFamily = dmSansFamily,
                    color = Color(0xFFB7E4C7),
                    fontSize = 18.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AbandonSheetContent(
    reduceMotion: Boolean,
    onStay: () -> Unit,
    onLeave: () -> Unit
) {
    Column(Modifier.padding(24.dp)) {
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(120.dp)) {
                drawGrowingPlant(1f, isWilting = true, wiltAmount = 0.9f, width = size.width, height = size.height)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Leave your plant?",
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFD8F3DC)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "It won't survive without you.",
            fontFamily = dmSansFamily,
            color = Color(0xFF74C69D)
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onStay,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F))
        ) { Text("Stay and Focus", color = Color.White, fontFamily = dmSansFamily, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onLeave) {
            Text(
                "Leave anyway",
                color = MaterialTheme.colorScheme.error,
                fontFamily = dmSansFamily,
                fontSize = 14.sp
            )
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun AmbientSoundPicker(
    selected: AmbientSoundManager.AmbientSound,
    onSelect: (AmbientSoundManager.AmbientSound) -> Unit,
    soundManager: AmbientSoundManager,
    reduceMotion: Boolean,
    downloadHint: String? = null,
    textColor: Color = Color(0xFF74C69D),
) {
    val grouped = remember { AmbientSoundManager.soundsByCategory() }
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Ambient Sound",
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Medium,
            color = textColor.copy(alpha = 0.65f),
            fontSize = 11.sp,
        )
        if (downloadHint != null) {
            Spacer(Modifier.height(4.dp))
            Text(
                downloadHint,
                fontFamily = dmSansFamily,
                fontSize = 10.sp,
                color = textColor.copy(alpha = 0.8f),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap a track — downloads once, plays offline after",
            fontFamily = dmSansFamily,
            fontSize = 9.sp,
            color = textColor.copy(alpha = 0.45f),
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        AmbientSoundChip(
            sound = AmbientSoundManager.AmbientSound.NONE,
            isSelected = selected == AmbientSoundManager.AmbientSound.NONE,
            isCached = true,
            reduceMotion = reduceMotion,
            textColor = textColor,
            onSelect = onSelect,
        )
        grouped.forEach { (category, sounds) ->
            Spacer(Modifier.height(10.dp))
            Text(
                category.title,
                modifier = Modifier.fillMaxWidth(),
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.SemiBold,
                color = textColor.copy(alpha = 0.55f),
                fontSize = 10.sp,
            )
            Spacer(Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                items(sounds, key = { it.name }) { sound ->
                    AmbientSoundChip(
                        sound = sound,
                        isSelected = sound == selected,
                        isCached = soundManager.isCached(sound),
                        reduceMotion = reduceMotion,
                        textColor = textColor,
                        onSelect = onSelect,
                    )
                }
            }
        }
    }
}

@Composable
private fun AmbientSoundChip(
    sound: AmbientSoundManager.AmbientSound,
    isSelected: Boolean,
    isCached: Boolean,
    reduceMotion: Boolean,
    textColor: Color,
    onSelect: (AmbientSoundManager.AmbientSound) -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF2D6A4F) else Color.Transparent,
        animationSpec = tween(if (reduceMotion) 0 else 200),
        label = "sound_chip",
    )
    Surface(
        onClick = { onSelect(sound) },
        shape = RoundedCornerShape(20.dp),
        color = bgColor,
        border = BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF52B788) else Color(0xFF52B788).copy(alpha = 0.3f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(sound.emoji, fontSize = 15.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                if (isCached || sound == AmbientSoundManager.AmbientSound.NONE) sound.label else "${sound.label} ↓",
                fontFamily = dmSansFamily,
                fontSize = 10.sp,
                color = if (isSelected) Color.White else textColor.copy(alpha = 0.75f),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SetupView(
    habits: List<HabitItem>,
    selectedMinutes: Int,
    selectedHabitId: String?,
    onSelectMinutes: (Int) -> Unit,
    onSelectHabit: (String?) -> Unit,
    onBegin: () -> Unit,
    onBack: () -> Unit,
    selectedSound: AmbientSoundManager.AmbientSound = AmbientSoundManager.AmbientSound.NONE,
    ambientVolume: Float = 0.7f,
    reduceMotion: Boolean = false,
    soundManager: AmbientSoundManager,
    ambientDownloadHint: String? = null,
    onSoundChange: (AmbientSoundManager.AmbientSound) -> Unit = {},
    onVolumeChange: (Float) -> Unit = {}
) {
    var showHabitPicker by remember { mutableStateOf(false) }
    val scroll = rememberScrollState()
    val onBg = MaterialTheme.colorScheme.onBackground
    val accent = Color(0xFF2D6A4F)
    val pulseT = rememberInfiniteTransition(label = "begin_pulse")
    val pulseScale by pulseT.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    val beginScale = if (reduceMotion) 1f else pulseScale
    val beginBrush = Brush.horizontalGradient(listOf(Color(0xFF1B4332), Color(0xFF2D6A4F)))
    val linkedHabit = habits.find { it.id == selectedHabitId }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scroll)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth()) {
            IconButton(onClick = onBack) {
                PhosphorIcon(R.drawable.ic_phosphor_arrow_left, "Back", onBg, Modifier.size(24.dp))
            }
        }
        Spacer(Modifier.height(8.dp))
        PhosphorIcon(
            R.drawable.ic_phosphor_leaf,
            null,
            accent,
            Modifier.size(56.dp)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "Focus Mode",
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            color = onBg
        )
        Text(
            "Plant a seed. Guard your focus.",
            fontFamily = dmSansFamily,
            fontSize = 15.sp,
            color = Color(0xFF40916C).copy(alpha = 0.85f)
        )

        Spacer(Modifier.height(32.dp))

        Text(
            "Duration",
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.SemiBold,
            color = onBg,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(DURATION_OPTIONS, key = { it }) { minutes ->
                val selected = minutes == selectedMinutes
                val pillShape = RoundedCornerShape(50)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    val glow = if (selected && !reduceMotion) {
                        Modifier.drawBehind {
                            drawCircle(
                                color = Color(0xFF52B788).copy(alpha = 0.25f),
                                radius = size.minDimension / 2f + 8.dp.toPx()
                            )
                        }
                    } else Modifier
                    Surface(
                        onClick = { onSelectMinutes(minutes) },
                        modifier = Modifier
                            .then(glow)
                            .height(44.dp),
                        shape = pillShape,
                        color = if (selected) Color(0xFF2D6A4F) else Color.Transparent,
                        border = BorderStroke(
                            1.dp,
                            Color(0xFF2D6A4F).copy(alpha = if (selected) 1f else 0.3f)
                        )
                    ) {
                        Box(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${minutes}m",
                                fontFamily = dmSansFamily,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp,
                                color = if (selected) Color.White else onBg
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        DURATION_LABELS[minutes] ?: "",
                        fontFamily = dmSansFamily,
                        fontSize = 10.sp,
                        color = onBg.copy(alpha = 0.45f)
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))

        Text(
            "Habit",
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.SemiBold,
            color = onBg,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        if (linkedHabit == null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .drawBehind {
                        drawRoundRect(
                            color = Color(0xFF2D6A4F).copy(alpha = 0.25f),
                            style = Stroke(
                                width = 2.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f)
                            ),
                            cornerRadius = CornerRadius(16.dp.toPx())
                        )
                    }
                    .clickable { showHabitPicker = true }
                    .padding(vertical = 18.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                PhosphorIcon(R.drawable.ic_phosphor_link, null, accent, Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Link a habit (optional)",
                    fontFamily = dmSansFamily,
                    color = onBg.copy(alpha = 0.75f),
                    fontWeight = FontWeight.Medium
                )
            }
        } else {
            Surface(
                onClick = { showHabitPicker = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, Color(0xFF2D6A4F).copy(alpha = 0.35f))
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(Color((linkedHabit.color and 0xFFFFFFFFL).toInt()))
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        linkedHabit.title,
                        modifier = Modifier.weight(1f),
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = onBg
                    )
                    Surface(
                        onClick = { onSelectHabit(null) },
                        shape = CircleShape,
                        color = Color(0xFF2D6A4F).copy(alpha = 0.2f)
                    ) {
                        PhosphorIcon(
                            R.drawable.ic_phosphor_x,
                            "Remove",
                            onBg.copy(alpha = 0.8f),
                            Modifier
                                .padding(6.dp)
                                .size(16.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        Text(
            "Focus Soundtrack",
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.SemiBold,
            color = onBg,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Text(
            "Pick a sound to play while you focus (preview on setup)",
            fontFamily = dmSansFamily,
            fontSize = 12.sp,
            color = onBg.copy(alpha = 0.45f),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(12.dp))
        AmbientSoundPicker(
            selected = selectedSound,
            onSelect = onSoundChange,
            soundManager = soundManager,
            reduceMotion = reduceMotion,
            downloadHint = ambientDownloadHint,
            textColor = Color(0xFF40916C),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Volume · ${(ambientVolume * 100).toInt()}%",
            fontFamily = dmSansFamily,
            fontSize = 12.sp,
            color = onBg.copy(alpha = 0.45f)
        )
        Slider(
            value = ambientVolume,
            onValueChange = onVolumeChange,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(32.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .scale(beginScale)
                .clip(RoundedCornerShape(20.dp))
                .background(beginBrush)
                .clickable(onClick = onBegin),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PhosphorIcon(R.drawable.ic_phosphor_play, null, Color.White, Modifier.size(26.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    "Begin",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.White
                )
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showHabitPicker) {
        ModalBottomSheet(onDismissRequest = { showHabitPicker = false }) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Choose Habit", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(12.dp))
                HabitPickerRow("Free Focus", null, selectedHabitId) {
                    onSelectHabit(null); showHabitPicker = false
                }
                habits.forEach { habit ->
                    HabitPickerRow(habit.title, habit.id, selectedHabitId) {
                        onSelectHabit(habit.id); showHabitPicker = false
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun HabitPickerRow(title: String, id: String?, selectedId: String?, onSelect: () -> Unit) {
    val selected = id == selectedId
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground)
        if (selected) {
            PhosphorIcon(
                R.drawable.ic_phosphor_check,
                null,
                MaterialTheme.colorScheme.primary,
                Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun ActiveView(
    progress: Float,
    linearProgress: Float,
    timeLeftSeconds: Int,
    linkedHabit: HabitItem?,
    onAbandon: () -> Unit,
    reduceMotion: Boolean = false,
    isWilting: Boolean = false,
    wiltProgress: Float = 0f,
    selectedSound: AmbientSoundManager.AmbientSound = AmbientSoundManager.AmbientSound.NONE,
    ambientVolume: Float = 0.7f,
    sessionQuote: String? = null,
    soundManager: AmbientSoundManager,
    ambientDownloadHint: String? = null,
    onSoundChange: (AmbientSoundManager.AmbientSound) -> Unit = {},
    onVolumeChange: (Float) -> Unit = {}
) {
    val minutes = timeLeftSeconds / 60
    val seconds = timeLeftSeconds % 60
    val textColor = Color(0xFFE8F5E9)
    val plantBox = 220.dp
    val particleSeeds = remember {
        List(14) { Triple(Random.nextFloat(), Random.nextFloat(), 0.4f + Random.nextFloat() * 0.6f) }
    }

    val inf = rememberInfiniteTransition(label = "particles")
    val drift by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing)),
        label = "drift"
    )

    var quoteVisible by remember { mutableStateOf(false) }
    var quoteConsumed by remember { mutableStateOf(false) }
    LaunchedEffect(linearProgress, sessionQuote, reduceMotion) {
        if (reduceMotion || quoteConsumed || sessionQuote.isNullOrBlank()) return@LaunchedEffect
        if (linearProgress >= 0.5f) {
            quoteConsumed = true
            quoteVisible = true
            delay(6000)
            quoteVisible = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 22.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (linkedHabit != null) {
            Text(
                linkedHabit.title,
                fontFamily = dmSansFamily,
                fontSize = 13.sp,
                color = textColor.copy(alpha = 0.55f)
            )
            Spacer(Modifier.height(6.dp))
        }

        Box(
            modifier = Modifier.size(plantBox),
            contentAlignment = Alignment.Center
        ) {
            if (!reduceMotion) {
                Canvas(Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val driftPx = drift * h * 0.4f
                    particleSeeds.forEachIndexed { i, (nx, ny, spd) ->
                        val baseY = h * ny + driftPx * spd
                        val y = baseY % (h + 24f) - 12f
                        drawCircle(
                            color = Color(0xFF52B788).copy(alpha = 0.15f),
                            radius = (2f + (i % 3)).dp.toPx(),
                            center = Offset(nx * w, y.coerceIn(0f, h))
                        )
                    }
                }
            }
            Canvas(Modifier.fillMaxSize()) {
                val r = minOf(size.width, size.height) / 2f - 3.dp.toPx()
                val left = size.width / 2f - r
                val top = size.height / 2f - r
                drawArc(
                    color = Color(0xFF52B788).copy(alpha = 0.4f),
                    startAngle = -90f,
                    sweepAngle = 360f * progress.coerceIn(0f, 1f),
                    useCenter = false,
                    topLeft = Offset(left, top),
                    size = Size(r * 2f, r * 2f),
                    style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            Canvas(Modifier.size(200.dp)) {
                drawGrowingPlant(progress, isWilting, wiltProgress)
            }
        }

        Spacer(Modifier.height(16.dp))

        AnimatedVisibility(
            visible = quoteVisible && !reduceMotion,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = sessionQuote ?: "",
                fontFamily = dmSansFamily,
                fontStyle = FontStyle.Italic,
                fontSize = 13.sp,
                color = Color(0xFFB7E4C7).copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }

        Text(
            "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}",
            style = TextStyle(
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 56.sp,
                color = textColor,
                letterSpacing = 2.sp,
                fontFeatureSettings = "tnum"
            )
        )

        Spacer(Modifier.height(6.dp))
        Text(
            "${(progress * 100).toInt()}% grown",
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = Color(0xFF74C69D)
        )

        Spacer(Modifier.height(18.dp))
        AmbientSoundPicker(
            selected = selectedSound,
            onSelect = onSoundChange,
            soundManager = soundManager,
            reduceMotion = reduceMotion,
            downloadHint = ambientDownloadHint,
            textColor = textColor.copy(alpha = 0.85f),
        )

        AnimatedVisibility(
            visible = selectedSound != AmbientSoundManager.AmbientSound.NONE && !reduceMotion,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            val waveT = rememberInfiniteTransition(label = "wave")
            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 12.dp)
            ) {
                for (i in 0 until 5) {
                    key(i) {
                        val h by waveT.animateFloat(
                            initialValue = 4f,
                            targetValue = 18f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(400 + i * 80, easing = EaseInOutSine),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "bar_$i"
                        )
                        Box(
                            Modifier
                                .width(3.dp)
                                .height(h.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF52B788).copy(alpha = 0.5f))
                        )
                    }
                }
            }
        }

        Slider(
            value = ambientVolume,
            onValueChange = onVolumeChange,
            valueRange = 0f..1f,
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        Spacer(Modifier.height(20.dp))

        OutlinedButton(
            onClick = onAbandon,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF5350)),
            border = BorderStroke(1.dp, Color(0xFFEF5350).copy(alpha = 0.5f))
        ) {
            PhosphorIcon(R.drawable.ic_phosphor_x, null, Color(0xFFEF5350), Modifier.size(16.dp))
            Spacer(Modifier.width(6.dp))
            Text("Abandon", fontFamily = dmSansFamily)
        }
    }
}

@Composable
private fun StatBlock(
    drawableId: Int,
    value: String,
    label: String,
    tint: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PhosphorIcon(drawableId, null, Color(0xFF74C69D), Modifier.size(22.dp))
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = tint
        )
        Text(
            label,
            fontFamily = dmSansFamily,
            fontSize = 11.sp,
            color = tint.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun CompleteView(
    minutes: Int,
    xpEarned: Int,
    linkedHabit: HabitItem?,
    onDone: () -> Unit,
    reduceMotion: Boolean = false,
    onShare: () -> Unit = {},
    onAnother: () -> Unit = {},
    onOpenGarden: () -> Unit = {}
) {
    val textColor = Color(0xFFE8F5E9)
    val headlineColor = Color(0xFFD8F3DC)
    val cardBg = Color(0xFF1A2E22)
    val mint = Color(0xFF74C69D)
    val gradient = Brush.horizontalGradient(listOf(Color(0xFF1B4332), Color(0xFF2D6A4F)))
    var parties by remember { mutableStateOf<List<Party>>(emptyList()) }

    LaunchedEffect(Unit) {
        if (!reduceMotion) {
            val palette = listOf(
                Color(0xFF52B788).toArgb(),
                Color(0xFF40916C).toArgb(),
                Color(0xFFB7E4C7).toArgb(),
                Color(0xFFFFD166).toArgb()
            )
            parties = listOf(
                Party(
                    speed = 10f,
                    maxSpeed = 30f,
                    damping = 0.91f,
                    spread = 360,
                    colors = palette,
                    position = Position.Relative(0.5, 0.22),
                    emitter = Emitter(duration = 900L, TimeUnit.MILLISECONDS).perSecond(240)
                )
            )
            delay(3200)
            parties = emptyList()
        }
    }

    val swayT = rememberInfiniteTransition(label = "plant_sway")
    val sway by swayT.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sway"
    )
    val plantRotation = if (reduceMotion) 0f else sway

    Box(Modifier.fillMaxSize()) {
        if (parties.isNotEmpty()) {
            KonfettiView(
                modifier = Modifier.fillMaxSize(),
                parties = parties
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .rotate(plantRotation),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawGrowingPlant(1f, isWilting = false, wiltAmount = 0f)
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Your plant grew.",
                fontFamily = frauncesFamily,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.SemiBold,
                fontSize = 28.sp,
                color = headlineColor,
                textAlign = TextAlign.Center
            )
            if (linkedHabit != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "${linkedHabit.title} • session complete",
                    fontFamily = dmSansFamily,
                    fontSize = 13.sp,
                    color = mint.copy(alpha = 0.65f),
                    textAlign = TextAlign.Center
                )
            }
            linkedHabit?.let { h ->
                Spacer(Modifier.height(8.dp))
                Text(
                    "Streak extended to ${h.streak + 1} days",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = Color(0xFFFFB74D),
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(28.dp))
            val habitLabel = when {
                linkedHabit == null -> "—"
                linkedHabit.title.length <= 10 -> linkedHabit.title
                else -> linkedHabit.title.take(9) + "…"
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = cardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp, horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatBlock(
                        drawableId = R.drawable.ic_phosphor_play,
                        value = "${minutes}m",
                        label = "Duration",
                        tint = textColor
                    )
                    Box(
                        Modifier
                            .width(1.dp)
                            .height(52.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )
                    StatBlock(
                        drawableId = R.drawable.ic_phosphor_trophy,
                        value = "+$xpEarned",
                        label = "XP",
                        tint = textColor
                    )
                    Box(
                        Modifier
                            .width(1.dp)
                            .height(52.dp)
                            .background(Color.White.copy(alpha = 0.08f))
                    )
                    StatBlock(
                        drawableId = R.drawable.ic_phosphor_link,
                        value = habitLabel,
                        label = "Habit",
                        tint = textColor
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(gradient)
                    .clickable(onClick = onDone),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Back to Verdly",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(
                onClick = onAnother,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFF52B788).copy(alpha = 0.45f))
            ) {
                Text(
                    "Start Another Session",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = textColor
                )
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onShare) {
                Text(
                    "Share your session",
                    fontFamily = dmSansFamily,
                    color = mint.copy(alpha = 0.95f),
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = onOpenGarden) {
                Text(
                    "View focus garden",
                    fontFamily = dmSansFamily,
                    color = mint.copy(alpha = 0.85f)
                )
            }
        }
    }
}

