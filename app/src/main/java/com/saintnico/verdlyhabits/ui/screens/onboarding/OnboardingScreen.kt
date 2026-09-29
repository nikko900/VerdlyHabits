package com.saintnico.verdlyhabits.ui.screens.onboarding

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.annotation.DrawableRes
import androidx.annotation.RawRes
import com.saintnico.verdlyhabits.ui.assets.VerdlyAssetCatalog
import com.saintnico.verdlyhabits.ui.components.VerdlyHeroIllustration
import com.saintnico.verdlyhabits.ui.components.VerdlyHeroLottie
import com.saintnico.verdlyhabits.ui.theme.BackgroundDark
import com.saintnico.verdlyhabits.ui.theme.PrimaryGreen
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
data class OnboardingSlide(
    val title: String,
    val description: String,
)

val onboardingSlides = listOf(
    OnboardingSlide(
        title = "Build habits that stick",
        description = "Every great routine starts with a single seed. Let's grow yours.",
    ),
    OnboardingSlide(
        title = "Challenge your friends",
        description = "Level up together by competing in daily habit challenges.",
    ),
    OnboardingSlide(
        title = "People who keep you honest",
        description = "A handshake, a witness, a grove. Accountability that actually feels human.",
    ),
    OnboardingSlide(
        title = "No excuses. Prove it.",
        description = "Complete your habit with photo proof and watch your root grow deeper.",
    ),
    OnboardingSlide(
        title = "Join the grove",
        description = "Wave hello. Invite someone you trust. Verdly is better with people in it.",
    ),
)

private val slideGradientTops = listOf(
    Color(0xFF0D1F0F),
    Color(0xFF1A1A0D),
    Color(0xFF0F1A12),
    Color(0xFF0A1A1A),
    Color(0xFF12100A),
)
private val slideGradientBottoms = listOf(
    Color(0xFF050D06),
    Color(0xFF090908),
    Color(0xFF06100A),
    Color(0xFF040D0D),
    Color(0xFF0A0906),
)

private val bloomColors = listOf(
    PrimaryGreen,
    Color(0xFF6B5C2E),
    Color(0xFF3D6B4A),
    Color(0xFF1A6B60),
    Color(0xFF8A6A2A),
)

private val overlineLabels = listOf(
    "HABIT BUILDING",
    "SOCIAL",
    "ACCOUNTABILITY",
    "PROOF",
    "JOIN VERDLY",
)

private val EaseInOutSine = CubicBezierEasing(0.445f, 0.05f, 0.55f, 0.95f)

private val grainOffsets = List(100) {
    Random(42L + it).nextFloat() * 4f
}

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    onSkip: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingSlides.size })
    val coroutineScope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    val density = LocalDensity.current
    val ghostInteraction = remember { MutableInteractionSource() }
    val primaryCtaInteraction = remember { MutableInteractionSource() }

    DisposableEffect(view) {
        val window = (view.context as Activity).window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, view)
        val prevLight = controller.isAppearanceLightStatusBars
        val prevNavColor = window.navigationBarColor
        val prevStatusColor = window.statusBarColor
        window.statusBarColor = Color.Transparent.toArgb()
        window.navigationBarColor = Color.Transparent.toArgb()
        controller.isAppearanceLightStatusBars = false
        onDispose {
            controller.isAppearanceLightStatusBars = prevLight
            window.navigationBarColor = prevNavColor
            window.statusBarColor = prevStatusColor
        }
    }

    var prevPage by remember { mutableIntStateOf(pagerState.currentPage) }
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != prevPage) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            prevPage = pagerState.currentPage
        }
    }

    val verdlyTypography = rememberVerdlyTypography()
    MaterialTheme(
        typography = verdlyTypography,
        colorScheme = MaterialTheme.colorScheme,
        shapes = MaterialTheme.shapes
    ) {
        var screenVisible by remember { mutableStateOf(false) }
        val screenAlpha by animateFloatAsState(
            targetValue = if (screenVisible) 1f else 0f,
            animationSpec = tween(900),
            label = "entryFade"
        )
        LaunchedEffect(Unit) {
            screenVisible = true
        }

        val page = pagerState.currentPage
        val gradientTop by animateColorAsState(
            slideGradientTops[page],
            animationSpec = tween(600),
            label = "gradTop"
        )
        val gradientBottom by animateColorAsState(
            slideGradientBottoms[page],
            animationSpec = tween(600),
            label = "gradBottom"
        )
        val bloomTint by animateColorAsState(
            bloomColors[page],
            animationSpec = tween(600),
            label = "bloom"
        )
        val orbGlowTint by animateColorAsState(
            bloomColors[page],
            animationSpec = tween(600),
            label = "orbGlow"
        )

        val infinite = rememberInfiniteTransition(label = "onboardingMotion")
        val breathe by infinite.animateFloat(
            initialValue = 0.97f,
            targetValue = 1.03f,
            animationSpec = infiniteRepeatable(
                animation = tween(5200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathe"
        )
        val shimmerShift by infinite.animateFloat(
            initialValue = -0.02f,
            targetValue = 0.02f,
            animationSpec = infiniteRepeatable(
                animation = tween(8000, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shimmer"
        )
        val glowPulse by infinite.animateFloat(
            initialValue = 0.9f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2200, easing = EaseInOutSine),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glowPulse"
        )

        val shiftPx = with(density) { shimmerShift * 28.dp.toPx() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .drawBehind {
                    val brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to gradientTop,
                            (0.42f * breathe + shimmerShift * 0.1f).coerceIn(0.15f, 0.85f) to lerp(
                                gradientTop,
                                gradientBottom,
                                0.45f
                            ),
                            1f to gradientBottom
                        ),
                        startY = shiftPx,
                        endY = size.height + shiftPx
                    )
                    drawRect(brush)
                }
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                bloomTint.copy(alpha = 0.2f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.5f, size.height * 0.2f),
                            radius = size.width * 0.75f
                        )
                    )
                }
                .drawBehind {
                    val stepX = size.width / 10f
                    val stepY = size.height / 10f
                    repeat(100) { i ->
                        val ox = grainOffsets[i]
                        val oy = grainOffsets[(i + 17) % 100]
                        drawCircle(
                            color = Color.White.copy(alpha = 0.018f),
                            radius = 1f,
                            center = Offset(
                                x = (i % 10) * stepX + ox,
                                y = (i / 10) * stepY + oy
                            )
                        )
                    }
                }
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.systemBars)
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
            ) {
                androidx.compose.animation.AnimatedVisibility(
                    visible = pagerState.currentPage < onboardingSlides.lastIndex,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .alpha(screenAlpha),
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(200))
                ) {
                    TextButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSkip()
                        }
                    ) {
                        Text(
                            text = "I'll explore first",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.55f)
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .alpha(screenAlpha)
                ) {
                    Spacer(modifier = Modifier.weight(0.35f))

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) { p ->
                        OnboardingSlideHero(
                            pageIndex = p,
                            orbGlowTint = orbGlowTint,
                            glowPulse = glowPulse,
                        )
                    }

                    Spacer(modifier = Modifier.weight(0.2f))

                    AnimatedContent(
                        targetState = pagerState.currentPage,
                        transitionSpec = {
                            slideInHorizontally { it / 4 } + fadeIn(tween(400)) togetherWith
                                slideOutHorizontally { -it / 4 } + fadeOut(tween(200))
                        },
                        label = "onboardingCopy"
                    ) { p ->
                        val slide = onboardingSlides[p]
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                            ) {
                                Text(
                                    text = overlineLabels[p],
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    letterSpacing = 2.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            val titleStyle =
                                if (p == onboardingSlides.lastIndex) {
                                    MaterialTheme.typography.displayLarge
                                } else {
                                    MaterialTheme.typography.headlineMedium
                                }
                            Text(
                                text = slide.title,
                                style = titleStyle,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.5).sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = slide.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White.copy(alpha = 0.72f),
                                textAlign = TextAlign.Center,
                                lineHeight = 26.sp,
                                modifier = Modifier.padding(horizontal = 36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    Row(
                        modifier = Modifier
                            .width(120.dp)
                            .align(Alignment.CenterHorizontally),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(onboardingSlides.size) { index ->
                            val active = pagerState.currentPage == index
                            val fill by animateFloatAsState(
                                targetValue = if (active) 1f else 0f,
                                animationSpec = tween(500),
                                label = "segment$index"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(Color.White.copy(alpha = 0.25f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fill)
                                        .clip(RoundedCornerShape(50))
                                        .background(Color.White)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(28.dp))

                    val isLastPage = pagerState.currentPage == onboardingSlides.lastIndex
                    val primaryPressed by primaryCtaInteraction.collectIsPressedAsState()
                    val primaryScale by animateFloatAsState(
                        if (primaryPressed) 0.96f else 1f,
                        label = "primaryScale"
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!isLastPage) {
                            val ghostPressed by ghostInteraction.collectIsPressedAsState()
                            val ghostScale by animateFloatAsState(
                                if (ghostPressed) 0.96f else 1f,
                                label = "ghostScale"
                            )
                            val label = when (pagerState.currentPage) {
                                0 -> "Plant the seed  →"
                                1 -> "Meet the crew  →"
                                2 -> "Keep growing  →"
                                else -> "Show the proof  →"
                            }
                            OutlinedButton(
                                onClick = {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(58.dp)
                                    .scale(ghostScale),
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.5f)),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = Color.Transparent,
                                    contentColor = Color.White
                                ),
                                interactionSource = ghostInteraction
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                                Icon(
                                    imageVector = Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        } else {
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isLastPage,
                                enter = slideInVertically { it } + fadeIn(tween(500)),
                                exit = fadeOut(tween(200))
                            ) {
                                Button(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onFinish()
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(58.dp)
                                        .scale(primaryScale),
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color.White,
                                        contentColor = BackgroundDark
                                    ),
                                    interactionSource = primaryCtaInteraction
                                ) {
                                    Text(
                                        text = "Continue. Sign in with Google",
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF050D06)
                                    )
                                }
                            }
                            androidx.compose.animation.AnimatedVisibility(
                                visible = isLastPage,
                                enter = slideInVertically { it } + fadeIn(tween(500)),
                                exit = fadeOut(tween(200))
                            ) {
                                Text(
                                    text = "No commitment. Cancel anytime.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.4f),
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun OnboardingSlideHero(
    pageIndex: Int,
    orbGlowTint: Color,
    glowPulse: Float,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        val baseR = with(LocalDensity.current) { 110.dp.toPx() * glowPulse }
        Box(
            modifier = Modifier
                .size(260.dp)
                .drawBehind {
                    listOf(1.0f, 0.7f, 0.4f).zip(
                        listOf(0.03f, 0.07f, 0.14f),
                    ).forEach { (mul, alpha) ->
                        drawCircle(
                            color = orbGlowTint.copy(alpha = alpha),
                            radius = baseR * mul,
                            center = center,
                        )
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            when (pageIndex) {
                0 -> OnboardingPeopleAndLottie(
                    peopleRes = VerdlyAssetCatalog.OnboardingSlide0People,
                    lottieRes = VerdlyAssetCatalog.OnboardingSlide0Lottie,
                )
                1 -> OnboardingPeopleAndLottie(
                    peopleRes = VerdlyAssetCatalog.OnboardingSlide1People,
                    lottieRes = VerdlyAssetCatalog.OnboardingSlide1Lottie,
                )
                2 -> OnboardingPeopleAndLottie(
                    peopleRes = VerdlyAssetCatalog.OnboardingSlide2People,
                    lottieRes = VerdlyAssetCatalog.OnboardingSlide2Lottie,
                )
                3 -> OnboardingPeopleAndLottie(
                    peopleRes = VerdlyAssetCatalog.OnboardingSlide3People,
                    lottieRes = VerdlyAssetCatalog.OnboardingSlide3Lottie,
                )
                else -> OnboardingPeopleAndLottie(
                    peopleRes = VerdlyAssetCatalog.OnboardingSlide4People,
                    lottieRes = VerdlyAssetCatalog.OnboardingSlide4Lottie,
                )
            }
        }
    }
}

@Composable
private fun OnboardingPeopleAndLottie(
    @DrawableRes peopleRes: Int,
    @RawRes lottieRes: Int,
) {
    Box(contentAlignment = Alignment.Center) {
        VerdlyHeroIllustration(
            drawableRes = peopleRes,
            size = 236.dp,
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(4.dp),
        ) {
            VerdlyHeroLottie(
                rawRes = lottieRes,
                size = 72.dp,
            )
        }
    }
}
