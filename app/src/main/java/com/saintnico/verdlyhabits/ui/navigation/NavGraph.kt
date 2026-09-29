package com.saintnico.verdlyhabits.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Redeem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.*
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import android.net.Uri
import com.saintnico.verdlyhabits.data.local.goals.GoalType
import com.saintnico.verdlyhabits.ui.components.habit.HabitPlantedCelebrationDialog
import com.saintnico.verdlyhabits.data.local.AppDataStore
import com.saintnico.verdlyhabits.data.model.NudgeSurface
import com.saintnico.verdlyhabits.domain.PremiumGate
import com.saintnico.verdlyhabits.monetization.PaywallTrigger
import com.saintnico.verdlyhabits.ui.components.social.NudgeSheetHost
import com.saintnico.verdlyhabits.ui.components.social.NudgeTarget
import com.saintnico.verdlyhabits.navigation.NotificationLaunch
import com.saintnico.verdlyhabits.session.clearSignedOutSession
import com.saintnico.verdlyhabits.session.completeSignedInEntry
import com.saintnico.verdlyhabits.challenge.ChallengeInviteManager
import com.saintnico.verdlyhabits.audio.AppAudio
import com.saintnico.verdlyhabits.audio.rememberAppHaptics
import androidx.activity.compose.BackHandler
import com.saintnico.verdlyhabits.ui.components.PremiumNotificationBar
import com.saintnico.verdlyhabits.ui.components.RevenueCatProPaywallDialog
import com.saintnico.verdlyhabits.ui.components.referral.ReferralAnnualOfferSheet
import com.saintnico.verdlyhabits.ui.components.referral.ReferralShareSheet
import com.saintnico.verdlyhabits.ui.components.social.blocksNewDuoInvite
import com.saintnico.verdlyhabits.ui.components.social.needsDuoAttention
import com.saintnico.verdlyhabits.engine.GoalCelebrationCopy
import com.saintnico.verdlyhabits.engine.GoalProgressEngine
import com.saintnico.verdlyhabits.engine.ProfileSocialEngine
import com.saintnico.verdlyhabits.engine.TrophyBus
import com.saintnico.verdlyhabits.engine.TrophyEvent
import com.saintnico.verdlyhabits.ui.components.TrophyUnlockCeremony
import com.saintnico.verdlyhabits.ui.components.coins.AfterLockBuySheet
import com.saintnico.verdlyhabits.ui.components.coins.CoinBurstAnimation
import com.saintnico.verdlyhabits.ui.components.coins.CoinPackSheet
import com.saintnico.verdlyhabits.ui.components.coins.StakeResolutionCeremony
import com.saintnico.verdlyhabits.ui.viewmodel.CoinPackReason
import com.saintnico.verdlyhabits.ui.components.share.OwnProfileShot
import com.saintnico.verdlyhabits.ui.components.share.ShareShotController
import com.saintnico.verdlyhabits.ui.components.share.ShareShotHost
import com.saintnico.verdlyhabits.ui.components.share.shareAchievementCard
import com.saintnico.verdlyhabits.ui.components.share.shareGoalMilestoneCard
import com.saintnico.verdlyhabits.ui.components.share.shareProfileCard
import com.saintnico.verdlyhabits.ui.components.share.shareStreakCardWithImage
import com.saintnico.verdlyhabits.ui.screens.achievements.AchievementsScreen
import com.saintnico.verdlyhabits.ui.screens.arena.ArenaScreen
import com.saintnico.verdlyhabits.ui.screens.arena.ArenaSegment
import com.saintnico.verdlyhabits.ui.screens.focus.FocusModeScreen
import com.saintnico.verdlyhabits.ui.screens.focus.PlantGardenScreen
import com.saintnico.verdlyhabits.ui.screens.goals.GoalsHomeScreen
import com.saintnico.verdlyhabits.ui.screens.goals.celebration.GoalCompletionScreen
import com.saintnico.verdlyhabits.ui.screens.goals.celebration.GoalMomentRitualScreen
import com.saintnico.verdlyhabits.ui.screens.goals.celebration.MilestoneCelebrationScreen
import com.saintnico.verdlyhabits.ui.screens.goals.nudge.MaintainNudgeSheet
import com.saintnico.verdlyhabits.ui.screens.goals.catchup.QuitCatchUpSheet
import com.saintnico.verdlyhabits.ui.screens.goals.checkin.WeeklyCheckInSheet
import com.saintnico.verdlyhabits.ui.screens.goals.creation.GoalCreationScreen
import com.saintnico.verdlyhabits.ui.screens.goals.detail.GoalDetailScreen
import com.saintnico.verdlyhabits.ui.screens.habit.AddHabitScreen
import com.saintnico.verdlyhabits.ui.screens.home.HomeScreen
import com.saintnico.verdlyhabits.ui.screens.grove.FriendsGroveScreen
import com.saintnico.verdlyhabits.ui.screens.grove.OpenGroveScreen
import com.saintnico.verdlyhabits.ui.screens.login.EmailSignInScreen
import com.saintnico.verdlyhabits.ui.screens.login.LoginScreen
import com.saintnico.verdlyhabits.ui.screens.login.SignUpScreen
import com.saintnico.verdlyhabits.ui.screens.mood.MoodCheckInScreen
import com.saintnico.verdlyhabits.ui.screens.notifications.NotificationsHubScreen
import com.saintnico.verdlyhabits.ui.screens.profile.AccountScreen
import com.saintnico.verdlyhabits.ui.screens.profile.MemberProfileScreen
import com.saintnico.verdlyhabits.ui.screens.profile.SubscriptionManageScreen
import com.saintnico.verdlyhabits.ui.screens.settings.EditProfileScreen
import com.saintnico.verdlyhabits.ui.screens.settings.SettingsScreen
import com.saintnico.verdlyhabits.ui.screens.stats.StatsScreen
import com.saintnico.verdlyhabits.ui.viewmodel.AccountabilityViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.BillingViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.ChallengeViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.CoinViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.GroveViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.ProfileSocialViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.FocusViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.FriendsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.GoalCelebration
import com.saintnico.verdlyhabits.ui.viewmodel.GoalsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.HabitViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.MoodViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.NotificationsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.ReEngagementViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.ReferralViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.SettingsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.UserStatsViewModel
import com.saintnico.verdlyhabits.data.engagement.ComebackDestination
import com.saintnico.verdlyhabits.ui.screens.reengagement.WelcomeBackScreen
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.saintnico.verdlyhabits.notifications.InboxLocalNotifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

// ─── Route constants ─────────────────────────────────────────────────────────
sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object AccountCreated : Screen("account_created")
    object EmailSignIn : Screen("email_signin")
    object Main : Screen("main")           // Shell with bottom nav
    object Home : Screen("home")
    object Stats : Screen("stats")
    object Focus : Screen("focus")
    object Goals : Screen("goals")
    object Profile : Screen("profile")
    object Arena : Screen("arena")
    object Challenges : Screen("challenges")
    object Duo : Screen("duo")
    object Achievements : Screen("achievements")
    object AddHabit : Screen("add_habit")
    object EditHabit : Screen("edit_habit/{habitId}") {
        fun createRoute(habitId: String) = "edit_habit/$habitId"
    }
    object Settings : Screen("settings") {
        fun section(section: String) = "settings?section=$section"
    }
    object SubscriptionManage : Screen("subscription_manage")
    object EditProfile : Screen("edit_profile")
    object ProfileSetup : Screen("profile_setup")
    object MoodCheckIn : Screen("mood_checkin")
    object WeeklyWrapped : Screen("weekly_wrapped")
}

/** Maps NavHost destination routes (including query templates) to bottom-nav tab ids. */
internal fun normalizeMainTabRoute(route: String): String = when {
    route.startsWith("goals") -> "goals"
    route.startsWith("arena") -> "arena"
    route == "duo" || route == "challenges" -> "arena"
    else -> route.substringBefore("?").substringBefore("/")
}

internal fun isMainTabRoute(route: String): Boolean =
    bottomNavRoutes.contains(normalizeMainTabRoute(route))

@Composable
fun VerdlyNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Login.route,
    billingViewModel: BillingViewModel = viewModel(),
    widgetLaunch: com.saintnico.verdlyhabits.widget.WidgetLaunchRequest? = null,
    onWidgetLaunchConsumed: () -> Unit = {},
    notificationLaunch: NotificationLaunch? = null,
    onNotificationLaunchConsumed: () -> Unit = {},
) {
    val habitViewModel: HabitViewModel = viewModel()
    val userStatsViewModel: UserStatsViewModel = viewModel()
    val moodViewModel: MoodViewModel = viewModel()
    val focusViewModel: FocusViewModel = viewModel()
    val settingsViewModel: SettingsViewModel = viewModel()
    val reEngagementViewModel: ReEngagementViewModel = viewModel()
    val friendsViewModel: FriendsViewModel = viewModel()
    val coinViewModel: CoinViewModel = viewModel()

    var pendingMainAction by remember { mutableStateOf<String?>(null) }

    var notificationMessage by remember { mutableStateOf("") }
    var showNotification by remember { mutableStateOf(false) }
    var notificationIsError by remember { mutableStateOf(false) }
    var notificationIcon by remember { mutableStateOf<androidx.compose.ui.graphics.vector.ImageVector?>(null) }
    val coroutineScope = rememberCoroutineScope()

    var paywallTrigger by remember { mutableStateOf<PaywallTrigger?>(null) }
    var habitPlantedCelebration by remember {
        mutableStateOf<Pair<Int, String>?>(null) // count, title
    }
    val hasFullAccess by billingViewModel.hasFullAccess.collectAsState()
    val hasPaidSubscription by billingViewModel.hasPaidSubscription.collectAsState()
    val membershipTier by billingViewModel.membershipTier.collectAsState()

    LaunchedEffect(membershipTier) {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                com.saintnico.verdlyhabits.data.remote.firestore.UserRepository()
                    .syncMembershipTier(membershipTier)
            }
        }
    }

    val appContext = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val streakRepo = remember { com.saintnico.verdlyhabits.data.streak.StreakRepository.get(appContext) }
    val streakSnap by streakRepo.snapshot.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val onMainSurface = navBackStackEntry?.destination?.route == Screen.Main.route
    val habitsReady = habitViewModel.habits.isNotEmpty()
    val welcomeBackState by reEngagementViewModel.state.collectAsState()
    val showWelcomeBack = welcomeBackState.visible && onMainSurface && habitsReady
    val lossToSurface = streakSnap.lossToSurface?.takeIf {
        onMainSurface && habitsReady && !showWelcomeBack
    }
    var lossAcknowledging by remember { mutableStateOf(false) }

    LaunchedEffect(paywallTrigger) {
        val trigger = paywallTrigger ?: return@LaunchedEffect
        runCatching {
            com.google.firebase.analytics.FirebaseAnalytics
                .getInstance(appContext)
                .logEvent(
                    "paywall_open",
                    android.os.Bundle().apply {
                        putString("trigger_id", trigger.analyticsId)
                        putString("trigger_class", trigger::class.simpleName)
                    },
                )
        }
    }

    fun showEpicNotification(message: String, isError: Boolean = false, icon: androidx.compose.ui.graphics.vector.ImageVector? = null) {
        notificationMessage = message
        notificationIsError = isError
        notificationIcon = icon
        showNotification = true
        coroutineScope.launch {
            delay(3000)
            showNotification = false
        }
    }

    val statsState by userStatsViewModel.state.collectAsState()
    val userName by settingsViewModel.userName.collectAsState()
    val userUsername by settingsViewModel.userUsername.collectAsState()
    val userPhotoUri by settingsViewModel.userPhotoUri.collectAsState()
    val userBio by settingsViewModel.userBio.collectAsState()
    val userProfileAccent by settingsViewModel.userProfileAccent.collectAsState()
    val userMotto by settingsViewModel.userMotto.collectAsState()
    val userFavoritePlant by settingsViewModel.userFavoritePlant.collectAsState()
    val totalFocusMinutes by focusViewModel.totalFocusMinutes.collectAsState()

    fun navigateAfterAuth(preferProfileSetup: Boolean) {
        val dest = if (preferProfileSetup) Screen.ProfileSetup.route else Screen.Main.route
        navController.navigate(dest) {
            popUpTo(Screen.Login.route) { inclusive = true }
            launchSingleTop = true
        }
        coroutineScope.launch {
            runCatching {
                completeSignedInEntry(
                    context = navController.context,
                    habitViewModel = habitViewModel,
                    userStatsViewModel = userStatsViewModel,
                    settingsViewModel = settingsViewModel,
                    preferProfileSetup = preferProfileSetup,
                    syncIdentity = billingViewModel::syncIdentity,
                )
            }
        }
    }

    // Auto-restore data on startup if already logged in — never block first paint.
    LaunchedEffect(Unit) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null) {
            runCatching {
                completeSignedInEntry(
                    context = navController.context,
                    habitViewModel = habitViewModel,
                    userStatsViewModel = userStatsViewModel,
                    settingsViewModel = settingsViewModel,
                    preferProfileSetup = false,
                    syncIdentity = billingViewModel::syncIdentity,
                )
            }
        }
    }

    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.fillMaxSize()
        ) {

            // ── Auth / Onboarding ──────────────────────────────────────────
            composable(
                route = Screen.Login.route,
                enterTransition = { fadeIn(tween(700)) },
                exitTransition = { fadeOut(tween(700)) }
            ) {
                LoginScreen(
                    onLoginSuccess = { navigateAfterAuth(false) },
                    onNewUser = { navigateAfterAuth(true) },
                    onNavigateToSignUp = {
                        navController.navigate(Screen.SignUp.route)
                    },
                    onNavigateToEmailSignIn = {
                        navController.navigate(Screen.EmailSignIn.route)
                    },
                )
            }

            composable(Screen.EmailSignIn.route) {
                EmailSignInScreen(
                    onSignInSuccess = { navigateAfterAuth(false) },
                    onNeedsProfileSetup = { navigateAfterAuth(true) },
                    onBackToLogin = { navController.popBackStack() },
                    onNavigateToSignUp = {
                        navController.navigate(Screen.SignUp.route) {
                            popUpTo(Screen.EmailSignIn.route) { inclusive = true }
                        }
                    },
                )
            }

            composable(Screen.SignUp.route) {
                SignUpScreen(
                    onSignUpSuccess = {
                        coroutineScope.launch {
                            settingsViewModel.markOnboardingCompleted()
                            navController.navigate(Screen.AccountCreated.route) {
                                popUpTo(Screen.SignUp.route) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    onBackToLogin = {
                        navController.popBackStack()
                    }
                )
            }

            composable(Screen.AccountCreated.route) {
                com.saintnico.verdlyhabits.ui.screens.login.AccountCreatedScreen(
                    onContinue = {
                        navController.navigate(Screen.ProfileSetup.route) {
                            popUpTo(Screen.AccountCreated.route) { inclusive = true }
                            launchSingleTop = true
                        }
                        coroutineScope.launch {
                            runCatching {
                                completeSignedInEntry(
                                    context = navController.context,
                                    habitViewModel = habitViewModel,
                                    userStatsViewModel = userStatsViewModel,
                                    settingsViewModel = settingsViewModel,
                                    preferProfileSetup = true,
                                    syncIdentity = billingViewModel::syncIdentity,
                                )
                            }
                        }
                    },
                )
            }

            composable(Screen.Onboarding.route) {
                com.saintnico.verdlyhabits.ui.screens.onboarding.OnboardingScreen(
                    onFinish = {
                        coroutineScope.launch {
                            settingsViewModel.completeOnboarding()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    },
                    onSkip = {
                        coroutineScope.launch {
                            settingsViewModel.completeOnboarding()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(Screen.Onboarding.route) { inclusive = true }
                            }
                        }
                    },
                )
            }

            composable(Screen.ProfileSetup.route) {
                com.saintnico.verdlyhabits.ui.screens.profile.ProfileSetupScreen(
                    onComplete = {
                        navController.navigate(Screen.Main.route) {
                            popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                        }
                    },
                    onRequireSignIn = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.ProfileSetup.route) { inclusive = true }
                        }
                    },
                    settingsViewModel = settingsViewModel,
                )
            }

            // ── Main shell with bottom nav ─────────────────────────────────
            composable(
                route = Screen.Main.route,
                enterTransition = { fadeIn(tween(500)) },
                exitTransition = { fadeOut(tween(500)) }
            ) {
                // Scoped to Main only: destroyed on logout so challenge lists never leak across Google accounts.
                val challengeViewModel: ChallengeViewModel = viewModel()
                val goalsViewModel: GoalsViewModel = viewModel()
                val mainContext = androidx.compose.ui.platform.LocalContext.current
                val onLogoutMain: () -> Unit = {
                    coroutineScope.launch {
                        clearSignedOutSession(mainContext, habitViewModel, userStatsViewModel)
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                        val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                            com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                        ).build()
                        com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(mainContext, gso).signOut()
                        settingsViewModel.clearProfile()
                        com.saintnico.verdlyhabits.widget.WidgetRefresh.updateAll(mainContext)
                        navController.navigate(Screen.Login.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                }
                MainShell(
                    habitViewModel = habitViewModel,
                    userStatsViewModel = userStatsViewModel,
                    moodViewModel = moodViewModel,
                    focusViewModel = focusViewModel,
                    settingsViewModel = settingsViewModel,
                    challengeViewModel = challengeViewModel,
                    friendsViewModel = friendsViewModel,
                    goalsViewModel = goalsViewModel,
                    reEngagementViewModel = reEngagementViewModel,
                    billingViewModel = billingViewModel,
                    hasFullAccess = hasFullAccess,
                    onRequestPaywall = { paywallTrigger = it },
                    statsState = statsState,
                    userName = userName,
                    userUsername = userUsername,
                    userPhotoUri = userPhotoUri,
                    userBio = userBio,
                    userProfileAccent = userProfileAccent,
                    userMotto = userMotto,
                    userFavoritePlant = userFavoritePlant,
                    totalFocusMinutes = totalFocusMinutes,
                    onNavigateToAddHabit = { navController.navigate(Screen.AddHabit.route) },
                    onNavigateToEditHabit = { id -> navController.navigate(Screen.EditHabit.createRoute(id)) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToSettingsSection = { section ->
                        navController.navigate(Screen.Settings.section(section))
                    },
                    onNavigateToEditProfile = { navController.navigate(Screen.EditProfile.route) },
                    onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) },
                    coinViewModel = coinViewModel,
                    pendingMainAction = pendingMainAction,
                    onPendingMainActionConsumed = { pendingMainAction = null },
                    onLogout = onLogoutMain,
                    onShowNotification = { msg, isError, icon -> showEpicNotification(msg, isError, icon) },
                    widgetLaunch = widgetLaunch,
                    onWidgetLaunchConsumed = onWidgetLaunchConsumed,
                    notificationLaunch = notificationLaunch,
                    onNotificationLaunchConsumed = onNotificationLaunchConsumed,
                )
            }

            // ── Add / Edit Habit ───────────────────────────────────────────
            composable(
                route = Screen.AddHabit.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(500)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(500)) }
            ) {
                AddHabitScreen(
                    habitToEdit = null,
                    onBack = { navController.popBackStack() },
                    onSave = { habit ->
                        val count = habitViewModel.habits.count { !it.isArchived && !it.isPaused }
                        if (!hasFullAccess && count >= 5) {
                            paywallTrigger = PaywallTrigger.HabitLimit
                            return@AddHabitScreen
                        }
                        habitViewModel.addHabit(habit)
                        navController.popBackStack()
                        showEpicNotification("Seed planted successfully!")
                        val newCount = count + 1
                        if (!hasFullAccess && newCount in 1..4) {
                            habitPlantedCelebration = newCount to habit.title
                        }
                    },
                    onShowNotification = { msg, isError -> showEpicNotification(msg, isError) }
                )
            }

            composable(
                route = Screen.EditHabit.route,
                arguments = listOf(navArgument("habitId") { type = NavType.StringType }),
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(500)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(500)) }
            ) { backStackEntry ->
                val habitId = backStackEntry.arguments?.getString("habitId")
                val habitToEdit = habitViewModel.habits.find { it.id == habitId }
                if (habitToEdit != null) {
                    AddHabitScreen(
                        habitToEdit = habitToEdit,
                        onBack = { navController.popBackStack() },
                        onSave = { updated ->
                            habitViewModel.updateHabit(updated)
                            navController.popBackStack()
                            showEpicNotification("Habit updated!")
                        },
                        onShowNotification = { msg, isError -> showEpicNotification(msg, isError) }
                    )
                } else {
                    LaunchedEffect(Unit) { navController.popBackStack() }
                }
            }

            // ── Settings ───────────────────────────────────────────────────
            composable(
                route = "settings?section={section}",
                arguments = listOf(
                    navArgument("section") {
                        type = NavType.StringType
                        defaultValue = "root"
                    },
                ),
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(400)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(400)) }
            ) { entry ->
                val context = androidx.compose.ui.platform.LocalContext.current
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        coroutineScope.launch {
                            clearSignedOutSession(context, habitViewModel, userStatsViewModel)
                            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                            val gso = com.google.android.gms.auth.api.signin.GoogleSignInOptions.Builder(
                                com.google.android.gms.auth.api.signin.GoogleSignInOptions.DEFAULT_SIGN_IN
                            ).build()
                            com.google.android.gms.auth.api.signin.GoogleSignIn.getClient(context, gso).signOut()
                            settingsViewModel.clearProfile()
                            navController.navigate(Screen.Login.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    },
                    onShowNotification = { msg, isError -> showEpicNotification(msg, isError) },
                    onNavigateToEditProfile = { navController.navigate(Screen.EditProfile.route) },
                    onNavigateToProfile = {
                        pendingMainAction = "profile"
                        navController.popBackStack(Screen.Main.route, inclusive = false)
                    },
                    onNavigateToNotificationsInbox = {
                        pendingMainAction = "notifications"
                        navController.popBackStack(Screen.Main.route, inclusive = false)
                    },
                    onNavigateToManageSubscription = {
                        navController.navigate(Screen.SubscriptionManage.route)
                    },
                    onOpenGroveShop = {
                        pendingMainAction = "grove_shop"
                        navController.popBackStack(Screen.Main.route, inclusive = false)
                    },
                    startSection = entry.arguments?.getString("section"),
                    settingsViewModel = settingsViewModel,
                    habitViewModel = habitViewModel,
                    billingViewModel = billingViewModel,
                    coinViewModel = coinViewModel,
                    friendsViewModel = friendsViewModel,
                )
            }

            composable(
                route = Screen.SubscriptionManage.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(400)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(400)) }
            ) {
                SubscriptionManageScreen(
                    billingViewModel = billingViewModel,
                    onBack = { navController.popBackStack() },
                    onRequestPaywall = { paywallTrigger = it },
                    onShowBanner = { msg -> showEpicNotification(msg, false) },
                )
            }

            composable(
                route = Screen.EditProfile.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(400)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(400)) }
            ) {
                EditProfileScreen(
                    onBack = { navController.popBackStack() },
                    onShowNotification = { msg, isError -> showEpicNotification(msg, isError) },
                    settingsViewModel = settingsViewModel,
                    statsState = statsState,
                )
            }

            // ── Achievements ───────────────────────────────────────────────
            composable(
                route = Screen.Achievements.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(400)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(400)) }
            ) {
                LaunchedEffect(Unit) {
                    TrophyBus.emit(TrophyEvent.HallOpened)
                }
                AchievementsScreen(
                    achievements = statsState.achievements,
                    onBack = { navController.popBackStack() }
                )
            }

            // ── Mood check-in ──────────────────────────────────────────────
            composable(
                route = Screen.MoodCheckIn.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Up, tween(400)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Down, tween(400)) }
            ) {
                MoodCheckInScreen(
                    onLog = { mood, tags, note ->
                        moodViewModel.logMood(mood, tags, note)
                        navController.popBackStack()
                        showEpicNotification("Mood logged!")
                    },
                    onDismiss = { navController.popBackStack() }
                )
            }
        }

        PremiumNotificationBar(
            message = notificationMessage,
            isVisible = showNotification,
            isError = notificationIsError,
            icon = notificationIcon
        )

        habitPlantedCelebration?.let { (count, title) ->
            HabitPlantedCelebrationDialog(
                visible = true,
                habitCount = count,
                habitTitle = title,
                onDismiss = { habitPlantedCelebration = null },
                onSeePro = {
                    habitPlantedCelebration = null
                    paywallTrigger = PaywallTrigger.HabitPlanted
                },
            )
        }

        RevenueCatProPaywallDialog(
            isOpen = paywallTrigger != null,
            billingViewModel = billingViewModel,
            onDismiss = { paywallTrigger = null },
            onPurchaseSuccess = {
                showEpicNotification(
                    message = "Welcome to Premium!",
                    icon = Icons.Rounded.Redeem,
                )
            },
        )

        if (lossToSurface != null && !lossAcknowledging) {
            val loss = lossToSurface
            val title = habitViewModel.habits.firstOrNull { it.id == loss.habitId }?.title ?: "Your habit"
            com.saintnico.verdlyhabits.ui.screens.streak.StreakLostScreen(
                loss = loss,
                habitTitle = title,
                onGetShield = {
                    lossAcknowledging = true
                    coroutineScope.launch {
                        runCatching { streakRepo.acknowledgeLoss(loss.identity) }
                        lossAcknowledging = false
                        if (loss.wasPreventableWithShield) {
                            paywallTrigger = PaywallTrigger.StreakLost(loss.finalStreak, title)
                        }
                    }
                },
                onDismiss = {
                    lossAcknowledging = true
                    coroutineScope.launch {
                        runCatching { streakRepo.acknowledgeLoss(loss.identity) }
                        lossAcknowledging = false
                    }
                },
            )
        }

        if (showWelcomeBack) {
            WelcomeBackScreen(
                headline = welcomeBackState.headline,
                body = welcomeBackState.body,
                contextLine = welcomeBackState.contextLine,
                primaryLabel = welcomeBackState.primaryLabel,
                suggestedHabit = welcomeBackState.suggestedHabit,
                onPrimaryAction = {
                    if (welcomeBackState.destination == ComebackDestination.HOME_COMPLETE_HABIT) {
                        welcomeBackState.suggestedHabit?.let { habitViewModel.toggleHabitCompletion(it.id) }
                    }
                    reEngagementViewModel.onPrimaryAction()
                },
                onDismiss = { reEngagementViewModel.onDismiss() },
                onShown = { reEngagementViewModel.onWelcomeBackShown() },
            )
        }
    }
}

@Composable
private fun MainShell(
    habitViewModel: HabitViewModel,
    userStatsViewModel: UserStatsViewModel,
    moodViewModel: MoodViewModel,
    focusViewModel: FocusViewModel,
    settingsViewModel: SettingsViewModel,
    challengeViewModel: ChallengeViewModel,
    friendsViewModel: FriendsViewModel,
    goalsViewModel: GoalsViewModel,
    reEngagementViewModel: ReEngagementViewModel,
    billingViewModel: BillingViewModel,
    hasFullAccess: Boolean,
    onRequestPaywall: (PaywallTrigger) -> Unit,
    statsState: com.saintnico.verdlyhabits.ui.viewmodel.UserStatsUiState,
    userName: String,
    userUsername: String,
    userPhotoUri: String?,
    userBio: String,
    userProfileAccent: String,
    userMotto: String,
    userFavoritePlant: String,
    totalFocusMinutes: Int,
    onNavigateToAddHabit: () -> Unit,
    onNavigateToEditHabit: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToSettingsSection: (String) -> Unit = { onNavigateToSettings() },
    onNavigateToEditProfile: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    coinViewModel: CoinViewModel,
    pendingMainAction: String? = null,
    onPendingMainActionConsumed: () -> Unit = {},
    onLogout: () -> Unit,
    onShowNotification: (String, Boolean, androidx.compose.ui.graphics.vector.ImageVector?) -> Unit,
    widgetLaunch: com.saintnico.verdlyhabits.widget.WidgetLaunchRequest? = null,
    onWidgetLaunchConsumed: () -> Unit = {},
    notificationLaunch: NotificationLaunch? = null,
    onNotificationLaunchConsumed: () -> Unit = {},
) {
    val innerNavController = rememberNavController()
    val profileSocialViewModel: ProfileSocialViewModel = viewModel()
    val navigateToMemberProfile: (String) -> Unit = { memberId ->
        innerNavController.navigate("member_profile/${Uri.encode(memberId)}")
    }
    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    val navigateToFocus: (String?) -> Unit = { habitId ->
        val route = if (habitId != null) "focus/$habitId" else "focus"
        innerNavController.navigate(route) { launchSingleTop = true }
    }

    val navigateToArena: (tab: String, action: String?) -> Unit = { tab, action ->
        val route = buildString {
            append("arena?tab=${Uri.encode(tab)}")
            append("&action=${Uri.encode(action.orEmpty())}")
        }
        innerNavController.navigate(route) {
            popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val bottomNavTabRoute = normalizeMainTabRoute(currentRoute)

    val activeGoals by goalsViewModel.activeGoals.collectAsState()
    val creationDraft by goalsViewModel.creationDraft.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val appContext = LocalContext.current
    val inboxUid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()

    DisposableEffect(inboxUid) {
        if (inboxUid.isNotBlank()) {
            InboxLocalNotifier.start(appContext, inboxUid)
        }
        onDispose { InboxLocalNotifier.stop() }
    }

    LaunchedEffect(creationDraft?.returnAfterAddHabit) {
        if (creationDraft?.returnAfterAddHabit == true) {
            goalsViewModel.clearReturnAfterAddHabit()
            innerNavController.navigate("goals") {
                popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
            innerNavController.navigate("goals/create") { launchSingleTop = true }
        }
    }

    DisposableEffect(lifecycleOwner, habitViewModel.habits.size, activeGoals.size) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                reEngagementViewModel.evaluateOnForeground(
                    habits = habitViewModel.habits.toList(),
                    hasActiveGoals = activeGoals.isNotEmpty(),
                )
                TrophyBus.emit(TrophyEvent.AppOpened)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(reEngagementViewModel) {
        reEngagementViewModel.pendingDestination.collect { destination ->
            if (destination == null) return@collect
            when (destination) {
                ComebackDestination.GOALS -> {
                    innerNavController.navigate("goals") {
                        popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
                ComebackDestination.CHALLENGES -> navigateToArena("challenges", null)
                ComebackDestination.FOCUS -> {
                    navigateToFocus(null)
                }
                ComebackDestination.HOME_COMPLETE_HABIT,
                ComebackDestination.HOME_BROWSE -> {
                    innerNavController.navigate("home") {
                        popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
            reEngagementViewModel.consumePendingDestination()
        }
    }

    var celebrationEvent by remember { mutableStateOf<GoalCelebration?>(null) }
    val weeklyGoalId by goalsViewModel.weeklyCheckInGoalId.collectAsState()
    val maintainNudgeGoalId by goalsViewModel.maintainNudgeGoalId.collectAsState()
    val catchUpGoalId by goalsViewModel.catchUpGoalId.collectAsState()

    LaunchedEffect(habitViewModel) {
        snapshotFlow { habitViewModel.habits.toList() }
            .distinctUntilChanged()
            .collect { habits ->
                goalsViewModel.syncWithHabits(habits)
            }
    }

    LaunchedEffect(Unit) {
        goalsViewModel.celebration.collect { celebrationEvent = it }
    }

    LaunchedEffect(notificationLaunch) {
        when (val launch = notificationLaunch) {
            NotificationLaunch.WeeklyCheckIn -> {
                goalsViewModel.requestWeeklyCheckIn()
                innerNavController.navigate("goals") {
                    popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
                onNotificationLaunchConsumed()
            }
            is NotificationLaunch.GoalDetail -> {
                innerNavController.navigate("goals/detail/${Uri.encode(launch.goalId)}") {
                    launchSingleTop = true
                }
                onNotificationLaunchConsumed()
            }
            NotificationLaunch.EditProfile -> {
                onNavigateToEditProfile()
                onNotificationLaunchConsumed()
            }
            NotificationLaunch.Inbox -> {
                innerNavController.navigate("notifications") { launchSingleTop = true }
                onNotificationLaunchConsumed()
            }
            NotificationLaunch.Duo -> {
                navigateToArena("duo", null)
                onNotificationLaunchConsumed()
            }
            null -> Unit
        }
    }

    // Show bottom nav on main tabs; hide on nested focus/{id}, stats, and goals sub-flows
    LaunchedEffect(widgetLaunch) {
        val launch = widgetLaunch ?: return@LaunchedEffect
        when (launch.route) {
            com.saintnico.verdlyhabits.widget.WidgetNavigation.ROUTE_CHALLENGES -> {
                launch.challengeId?.let { challengeViewModel.requestOpenChallengeDetail(it) }
                navigateToArena("challenges", null)
            }
            com.saintnico.verdlyhabits.widget.WidgetNavigation.ROUTE_DUO -> {
                navigateToArena("duo", null)
            }
            else -> {
                innerNavController.navigate("home") {
                    popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
        onWidgetLaunchConsumed()
    }

    LaunchedEffect(widgetLaunch) {
        val launch = widgetLaunch ?: return@LaunchedEffect
        when (launch.route) {
            com.saintnico.verdlyhabits.widget.WidgetNavigation.ROUTE_CHALLENGES -> {
                launch.challengeId?.let { challengeViewModel.requestOpenChallengeDetail(it) }
                navigateToArena("challenges", null)
            }
            else -> {
                innerNavController.navigate("home") {
                    popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
        }
        onWidgetLaunchConsumed()
    }

    val showBottomNav = when {
        currentRoute == "stats" -> false
        currentRoute.startsWith("focus/") && currentRoute != "focus" -> false
        currentRoute == "focus" || currentRoute == "focus_garden" -> false
        currentRoute == "goals/create" -> false
        currentRoute.startsWith("goals/detail/") -> false
        currentRoute == "profile_subscription" -> false
        currentRoute == "notifications" -> false
        currentRoute.startsWith("member_profile") -> false
        currentRoute == "friends_grove" || currentRoute == "open_grove" -> false
        else -> isMainTabRoute(currentRoute)
    }

    val referralViewModel: ReferralViewModel = viewModel()
    val accountabilityViewModel: AccountabilityViewModel = viewModel()
    val notificationsViewModel: NotificationsViewModel = viewModel()
    val referralState by referralViewModel.state.collectAsState()
    val duoState by accountabilityViewModel.duoState.collectAsState()
    val accountabilityNudge by accountabilityViewModel.nudgeMessage.collectAsState()
    val notifState by notificationsViewModel.state.collectAsState()
    val incomingFriends by friendsViewModel.incomingRequestsUi.collectAsState()
    val arenaInvites by friendsViewModel.pendingChallengeJoinRequests.collectAsState()
    val friendSummaries by friendsViewModel.friendSummaries.collectAsState()
    val challengeStateForTrophies by challengeViewModel.state.collectAsState()
    val bellBadgeCount = maxOf(
        notifState.pendingActionCount,
        notifState.unreadCount,
        incomingFriends.size + arenaInvites.size + if (duoState?.isIncomingInvite == true) 1 else 0,
    )
    val navigateToNotifications: () -> Unit = {
        innerNavController.navigate("notifications") { launchSingleTop = true }
    }
    LaunchedEffect(pendingMainAction) {
        when (pendingMainAction) {
            "notifications" -> {
                navigateToNotifications()
                onPendingMainActionConsumed()
            }
            "profile" -> {
                innerNavController.navigate("profile") { launchSingleTop = true }
                onPendingMainActionConsumed()
            }
            "subscription" -> {
                innerNavController.navigate("profile_subscription") { launchSingleTop = true }
                onPendingMainActionConsumed()
            }
            "grove_shop" -> onPendingMainActionConsumed()
            else -> Unit
        }
    }
    val navigateToFriendsGrove: () -> Unit = {
        innerNavController.navigate("friends_grove") { launchSingleTop = true }
    }
    val navigateToOpenGrove: () -> Unit = {
        innerNavController.navigate("open_grove") { launchSingleTop = true }
    }
    val hasPaidSubscription by billingViewModel.hasPaidSubscription.collectAsState()
    val isPro by billingViewModel.isPro.collectAsState()
    val groveViewModel: GroveViewModel = viewModel()
    val coinNotice by coinViewModel.notice.collectAsState()
    val coinPackReason by coinViewModel.packReason.collectAsState()
    val coinPackNeed by coinViewModel.packNeed.collectAsState()
    val pendingCosmeticId by coinViewModel.pendingCosmeticId.collectAsState()
    val stakeCeremony by coinViewModel.ceremony.collectAsState()
    val afterLockBuy by coinViewModel.afterLockBuy.collectAsState()
    val purchaseCelebration by coinViewModel.purchaseCelebration.collectAsState()
    val coinBalance by coinViewModel.balance.collectAsState()
    val ownedCosmeticIds by coinViewModel.ownedCosmeticIds.collectAsState()
    val equippedGlowId by coinViewModel.equippedTitleGlow.collectAsState()
    val equippedSkinId by coinViewModel.equippedCheckSkin.collectAsState()
    val equippedFrameId by coinViewModel.equippedProfileFrame.collectAsState()
    val equippedOathChipId by coinViewModel.equippedOathChip.collectAsState()
    val equippedPlantId by coinViewModel.equippedGrovePlant.collectAsState()
    val equippedTitleId by settingsViewModel.equippedTitleId.collectAsState()
    val membershipTier by billingViewModel.membershipTier.collectAsState()
    val pinnedTrophyLabel by coinViewModel.pinnedTrophyLabel.collectAsState()
    val pinnedTrophyUntil by coinViewModel.pinnedTrophyUntil.collectAsState()
    val shareShotController = remember { ShareShotController() }

    LaunchedEffect(inboxUid) {
        if (inboxUid.isNotBlank()) {
            coinViewModel.reconcileFromCloud()
            coinViewModel.claimIncomingGifts()
            groveViewModel.refreshMe()
        }
    }

    LaunchedEffect(habitViewModel.habits.size) {
        userStatsViewModel.bindHabits(habitViewModel.habits.toList())
    }
    LaunchedEffect(friendSummaries.size) {
        if (friendSummaries.isNotEmpty()) {
            TrophyBus.emit(TrophyEvent.FriendsCount(friendSummaries.size))
        }
    }
    LaunchedEffect(challengeStateForTrophies.challenges) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        val wins = ProfileSocialEngine.challengeWinsFor(challengeStateForTrophies.challenges, uid)
        if (wins > 0) TrophyBus.emit(TrophyEvent.ChallengeWins(wins))
    }

    LaunchedEffect(challengeStateForTrophies.challenges, challengeStateForTrophies.archivedChallenges, isPro) {
        coinViewModel.resolveStakes(
            challengeStateForTrophies.challenges + challengeStateForTrophies.archivedChallenges,
            isPro,
        )
    }

    LaunchedEffect(isPro, hasPaidSubscription) {
        if (isPro || hasPaidSubscription) {
            coinViewModel.grantMonthlyDripIfDue(true)
        }
    }

    LaunchedEffect(coinNotice) {
        coinNotice?.let { notice ->
            onShowNotification(notice.message, !notice.isPositive, null)
            coinViewModel.consumeNotice()
        }
    }

    var showReferralInvite by remember { mutableStateOf(false) }
    var showAnnualOffer by remember { mutableStateOf(false) }

    LaunchedEffect(hasPaidSubscription) {
        referralViewModel.refresh(hasPaidSubscription)
    }

    LaunchedEffect(referralState.rewardJustGranted, referralState.retentionBonusesGranted) {
        if (referralState.rewardJustGranted) {
            onShowNotification(
                "You did it. ${referralState.rewardDays} days of Premium are yours.",
                false,
                Icons.Rounded.Redeem,
            )
            billingViewModel.refreshBonusAccess()
            if (!hasPaidSubscription && referralState.showAnnualOffer) {
                kotlinx.coroutines.delay(900)
                showAnnualOffer = true
            }
            referralViewModel.consumeRewardCelebration()
        }
        if (referralState.retentionBonusesGranted > 0) {
            onShowNotification(
                "+${referralState.retentionBonusesGranted * com.saintnico.verdlyhabits.referral.ReferralManager.REFERRER_RETENTION_BONUS_DAYS} bonus days. Friends stayed 7 days!",
                false,
                Icons.Rounded.Redeem,
            )
            billingViewModel.refreshBonusAccess()
            referralViewModel.consumeRetentionBonuses()
        }
    }

    LaunchedEffect(accountabilityNudge) {
        accountabilityNudge?.let { msg ->
            onShowNotification(msg, false, Icons.Rounded.Redeem)
            accountabilityViewModel.clearNudge()
        }
    }

    LaunchedEffect(hasFullAccess) {
        accountabilityViewModel.setPremiumAccess(hasFullAccess)
        notificationsViewModel.setPremiumAccess(hasFullAccess)
    }

    LaunchedEffect(userUsername) {
        accountabilityViewModel.setMyUsername(userUsername)
    }

    var duoMilestone by remember { mutableStateOf<com.saintnico.verdlyhabits.data.remote.firestore.DuoMilestoneReached?>(null) }
    var duoBroken by remember { mutableStateOf<com.saintnico.verdlyhabits.data.remote.firestore.DuoStreakBroken?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(Unit) {
        if (ChallengeInviteManager.peekPendingJoin(context) != null) {
            navigateToArena("challenges", null)
        }
    }

    LaunchedEffect(accountabilityViewModel) {
        accountabilityViewModel.milestoneEvent.collect { duoMilestone = it }
    }
    LaunchedEffect(accountabilityViewModel) {
        accountabilityViewModel.streakBrokenEvent.collect { duoBroken = it }
    }

    val duoNeedsAttention = duoState.needsDuoAttention()
    var showCreateSheet by remember { mutableStateOf(false) }
    var showPulseComposer by remember { mutableStateOf(false) }
    var proofLaunchToken by remember { mutableStateOf(0) }
    var habitsFocusToken by remember { mutableStateOf(0) }
    var nudgeTarget by remember { mutableStateOf<NudgeTarget?>(null) }
    var createPickerMode by remember { mutableStateOf<CreatePickerMode?>(null) }
    var createFabOrigin by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Unspecified) }
    val createHaptics = rememberAppHaptics()
    val createScope = rememberCoroutineScope()
    val createStore = remember { AppDataStore(context) }
    val sharedMilestoneKey by createStore.createSharedMilestoneKey.collectAsState(initial = null)
    val groveMe by groveViewModel.myPresence.collectAsState()
    val createToday = LocalDate.now()
    val createActiveHabits = habitViewModel.habits.filter { !it.isArchived }
    val createCompleted = createActiveHabits.filter { it.isCompleted }
    val createCompletedTitles = createCompleted.map { it.title }.distinct()
    val createProofPair = createCompleted.firstNotNullOfOrNull { habit ->
        challengeViewModel.challengeProofReminderForHabit(habit.title, habit.isCompleted)
            ?.let { habit to it }
    }
    val createMilestone = CreateMilestones.resolve(
        achievements = statsState.achievements,
        habits = createCompleted.map {
            CreateHabitSnap(it.id, it.title, it.streak, it.isCompleted)
        },
        today = createToday,
        alreadySharedKey = sharedMilestoneKey,
    )
    val createFriends = friendSummaries.map {
        CreateNudgeFriend(it.uid, it.username, it.photoUrl)
    }
    val createState = CreateViewState(
        plantedToday = createCompletedTitles.isNotEmpty(),
        completedToday = createCompletedTitles,
        alreadySharedTitle = groveMe?.sharedHabitTitle,
        proofWindowOpen = createProofPair != null,
        proofHabitTitle = createProofPair?.first?.title,
        milestone = createMilestone,
        friendCount = friendSummaries.size,
        nudgeFriends = createFriends,
        hasDuo = duoState.blocksNewDuoInvite(),
        duoSwapAvailable = false,
        isSignedIn = inboxUid.isNotBlank(),
        canAddHabit = PremiumGate.canAddHabit(
            createActiveHabits.count { !it.isPaused },
            hasFullAccess,
        ),
        hasHabits = createActiveHabits.isNotEmpty(),
        photoUrl = groveMe?.photoUrl ?: userPhotoUri,
        username = groveMe?.username?.ifBlank { null } ?: userUsername.ifBlank { userName },
        titleGlowId = groveMe?.titleGlowId,
        profileFrameId = groveMe?.profileFrameId,
    )

    val closeCreateOverlays = {
        showCreateSheet = false
        showPulseComposer = false
    }
    val goHomeTab: () -> Unit = {
        innerNavController.navigate("home") {
            popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    BackHandler(enabled = showCreateSheet || showPulseComposer) { closeCreateOverlays() }
    val navigateToDuo: () -> Unit = { navigateToArena("duo", null) }

    LaunchedEffect(habitViewModel, accountabilityViewModel) {
        snapshotFlow { habitViewModel.habits.toList() }
            .distinctUntilChanged()
            .collect { habits ->
                val active = habits.filter { !it.isArchived }
                if (active.isEmpty()) return@collect
                val today = java.time.LocalDate.now().toString()
                val completed = active.count { it.completedDates.contains(today) }
                accountabilityViewModel.syncFromHabits(completed, active.size, userStatsViewModel)
            }
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        bottomBar = {
            if (showBottomNav) {
                VerdlyBottomNavBar(
                    currentRoute = bottomNavTabRoute,
                    onNavigate = { route ->
                        closeCreateOverlays()
                        when (route) {
                            "arena" -> navigateToArena("auto", null)
                            else -> innerNavController.navigate(route) {
                                popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    },
                    onCreateClick = {
                        if (showPulseComposer) {
                            showPulseComposer = false
                        } else {
                            showCreateSheet = !showCreateSheet
                        }
                    },
                    onCreateLongClick = {
                        if (createState.canLongPressComposer) {
                            showCreateSheet = false
                            showPulseComposer = true
                        } else {
                            showCreateSheet = !showCreateSheet
                        }
                    },
                    haptics = createHaptics,
                    createOpen = showCreateSheet || showPulseComposer,
                    proofDue = createState.proofWindowOpen,
                    onCreateFabPositioned = { createFabOrigin = it },
                    duoGlowing = duoNeedsAttention,
                    duoAtRisk = duoState?.streakAtRisk == true,
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = innerNavController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                HomeScreen(
                    viewModel = habitViewModel,
                    userStatsViewModel = userStatsViewModel,
                    challengeViewModel = challengeViewModel,
                    userName = userName,
                    userUsername = userUsername,
                    billingViewModel = billingViewModel,
                    hasFullAccess = hasFullAccess,
                    onRequestPaywall = onRequestPaywall,
                    onNavigateToAddHabit = onNavigateToAddHabit,
                    onNavigateToEditHabit = onNavigateToEditHabit,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToFocus = navigateToFocus,
                    onNavigateToStats = {
                        innerNavController.navigate("stats") { launchSingleTop = true }
                    },
                    onNavigateToChallenge = { challengeId ->
                        challengeViewModel.requestOpenChallengeDetail(challengeId)
                        navigateToArena("challenges", null)
                    },
                    onShowNotification = onShowNotification,
                    goalsViewModel = goalsViewModel,
                    onNavigateToGoals = {
                        innerNavController.navigate("goals") {
                            popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToEditProfile = onNavigateToEditProfile,
                    userPhotoUri = userPhotoUri,
                    userMotto = userMotto,
                    userBio = userBio,
                    userFavoritePlant = userFavoritePlant,
                    referralState = referralState,
                    onOpenReferral = {
                        referralViewModel.ensureReady(hasPaidSubscription)
                        showReferralInvite = true
                    },
                    onDismissReferralBanner = { referralViewModel.dismissBanner() },
                    onDismissReferralFeedBanner = { referralViewModel.dismissFeedBanner() },
                    onNavigateToDuo = navigateToDuo,
                    duoNeedsAttention = duoNeedsAttention,
                    duoAtRisk = duoState?.streakAtRisk == true,
                    notificationBadgeCount = bellBadgeCount,
                    onOpenNotifications = navigateToNotifications,
                    coinViewModel = coinViewModel,
                    groveViewModel = groveViewModel,
                    friendsViewModel = friendsViewModel,
                    onNavigateToGrove = navigateToFriendsGrove,
                    onNavigateToConnect = navigateToFriendsGrove,
                    onNavigateToMemberProfile = navigateToMemberProfile,
                    duoState = duoState,
                    proofLaunchToken = proofLaunchToken,
                    habitsFocusToken = habitsFocusToken,
                )
            }
            composable("duo") {
                LaunchedEffect(Unit) {
                    innerNavController.navigate("arena?tab=duo&action=") {
                        popUpTo("duo") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            composable("notifications") {
                NotificationsHubScreen(
                    notificationsViewModel = notificationsViewModel,
                    friendsViewModel = friendsViewModel,
                    accountabilityViewModel = accountabilityViewModel,
                    onBack = { innerNavController.popBackStack() },
                    onNavigateToMemberProfile = navigateToMemberProfile,
                    onOpenChallenge = { challengeId ->
                        challengeViewModel.requestOpenChallengeDetail(challengeId)
                        navigateToArena("challenges", null)
                    },
                    onOpenRoute = { route ->
                        when (route.lowercase()) {
                            "paywall", "pro", "subscription" -> onRequestPaywall(PaywallTrigger.GoPro)
                            "referral" -> {
                                referralViewModel.ensureReady(hasPaidSubscription)
                                showReferralInvite = true
                            }
                            "duo" -> navigateToArena("duo", null)
                            "challenges", "arena" -> navigateToArena("challenges", null)
                            "profile", "profile_views" -> {
                                innerNavController.navigate("profile") { launchSingleTop = true }
                            }
                            "notifications" -> navigateToNotifications()
                            "friends_grove", "grove" -> navigateToFriendsGrove()
                            "open_grove" -> navigateToOpenGrove()
                            else -> navigateToNotifications()
                        }
                    },
                    onShowToast = { msg, isError -> onShowNotification(msg, isError, null) },
                )
            }
            composable("friends_grove") {
                FriendsGroveScreen(
                    groveViewModel = groveViewModel,
                    friendsViewModel = friendsViewModel,
                    onBack = { innerNavController.popBackStack() },
                    onOpenMember = navigateToMemberProfile,
                    onOpenOpenGrove = navigateToOpenGrove,
                    onOpenConnect = {
                        innerNavController.navigate("profile") { launchSingleTop = true }
                    },
                    onShowNotification = { msg, err -> onShowNotification(msg, err, null) },
                )
            }
            composable("open_grove") {
                OpenGroveScreen(
                    groveViewModel = groveViewModel,
                    isPremium = isPro,
                    onBack = { innerNavController.popBackStack() },
                    onOpenMember = navigateToMemberProfile,
                    onRequestPremium = { onRequestPaywall(PaywallTrigger.GoPro) },
                )
            }
            composable("stats") {
                LaunchedEffect(Unit) { TrophyBus.emit(TrophyEvent.StatsOpened) }
                StatsScreen(
                    habits = habitViewModel.habits.toList(),
                    statsState = statsState,
                    hasFullAccess = hasFullAccess,
                    installDateMillis = billingViewModel.getInstallDateMillis(),
                    onRequestPaywall = onRequestPaywall,
                    onBack = { innerNavController.popBackStack() }
                )
            }
            composable(Screen.WeeklyWrapped.route) {
                com.saintnico.verdlyhabits.ui.screens.stats.WrappedStoryScreen(
                    onClose = { innerNavController.popBackStack() },
                )
            }
            composable(
                route = "focus/{habitId}",
                arguments = listOf(
                    navArgument("habitId") { type = NavType.StringType }
                )
            ) { entry ->
                val habitId = entry.arguments?.getString("habitId")
                FocusModeScreen(
                    habits = habitViewModel.habits.toList(),
                    preLinkedHabitId = habitId,
                    hasFullAccess = hasFullAccess,
                    onRequestPaywall = onRequestPaywall,
                    onSessionComplete = { minutes, xp, linkedId ->
                        focusViewModel.saveSession(linkedId, minutes, true, xp)
                        userStatsViewModel.onFocusSessionComplete(minutes, xp)
                        onShowNotification("Focus session saved! +$xp XP", false, null)
                    },
                    onBack = { innerNavController.popBackStack() },
                    onOpenGarden = {
                        innerNavController.navigate("focus_garden") { launchSingleTop = true }
                    }
                )
            }
            composable("focus") {
                FocusModeScreen(
                    habits = habitViewModel.habits.toList(),
                    preLinkedHabitId = null,
                    hasFullAccess = hasFullAccess,
                    onRequestPaywall = onRequestPaywall,
                    onSessionComplete = { minutes, xp, linkedId ->
                        focusViewModel.saveSession(linkedId, minutes, true, xp)
                        userStatsViewModel.onFocusSessionComplete(minutes, xp)
                        onShowNotification("Focus session saved! +$xp XP", false, null)
                    },
                    onBack = { innerNavController.popBackStack() },
                    onOpenGarden = {
                        innerNavController.navigate("focus_garden") { launchSingleTop = true }
                    }
                )
            }
            composable("focus_garden") {
                LaunchedEffect(Unit) { TrophyBus.emit(TrophyEvent.GardenOpened) }
                PlantGardenScreen(onBack = { innerNavController.popBackStack() })
            }
            composable("challenges") {
                LaunchedEffect(Unit) {
                    innerNavController.navigate("arena?tab=challenges&action=") {
                        popUpTo("challenges") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
            composable(
                route = "arena?tab={tab}&action={action}",
                arguments = listOf(
                    navArgument("tab") {
                        type = NavType.StringType
                        defaultValue = "auto"
                    },
                    navArgument("action") {
                        type = NavType.StringType
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                val tab = entry.arguments?.getString("tab").orEmpty()
                val action = entry.arguments?.getString("action").orEmpty()
                val challengeState by challengeViewModel.state.collectAsState()
                val hasActiveChallenges = challengeState.challenges.any { it.isEffectivelyActive() }
                val initialSegment = when (tab) {
                    "duo" -> ArenaSegment.Duo
                    "challenges" -> ArenaSegment.Challenges
                    else -> if (hasActiveChallenges) ArenaSegment.Challenges else ArenaSegment.Duo
                }
                ArenaScreen(
                    initialSegment = initialSegment,
                    duoState = duoState,
                    hasActiveChallenges = hasActiveChallenges,
                    challengeViewModel = challengeViewModel,
                    billingViewModel = billingViewModel,
                    friendsViewModel = friendsViewModel,
                    coinViewModel = coinViewModel,
                    myUsername = userUsername,
                    myPhotoUrl = userPhotoUri,
                    onShowNotification = { msg, isError -> onShowNotification(msg, isError, null) },
                    onNavigateToEditProfile = onNavigateToEditProfile,
                    onNavigateToMemberProfile = navigateToMemberProfile,
                    onAcceptInvite = { accountabilityViewModel.acceptInvite(it) },
                    onDeclineInvite = { accountabilityViewModel.declineInvite(it) },
                    onApplyGrace = { pairId ->
                        accountabilityViewModel.applyGrace(pairId) { ok, err ->
                            onShowNotification(
                                if (ok) "Premium shield applied. Yesterday forgiven" else (err ?: "Couldn't apply shield"),
                                !ok,
                                null,
                            )
                        }
                    },
                    onInviteBuddy = { uid, username, photo, onResult ->
                        accountabilityViewModel.inviteBuddy(
                            uid = uid,
                            username = username,
                            photoUrl = photo,
                            myUsername = userUsername.ifBlank { userName },
                            myPhotoUrl = userPhotoUri,
                            onResult = onResult,
                        )
                    },
                    referralState = referralState,
                    onOpenReferral = {
                        referralViewModel.ensureReady(hasPaidSubscription)
                        showReferralInvite = true
                    },
                    openDuoNudge = action == "nudge",
                    onGrantDuoSwap = { onResult ->
                        accountabilityViewModel.grantDuoSwap { ok, err ->
                            onShowNotification(
                                if (ok) "You covered your buddy today." else (err ?: "Couldn't cover them"),
                                !ok,
                                null,
                            )
                            onResult(ok, err)
                        }
                    },
                )
            }
            composable("goals") {
                GoalsHomeScreen(
                    viewModel = goalsViewModel,
                    habits = habitViewModel.habits.toList(),
                    onNavigateToAddGoal = {
                        innerNavController.navigate("goals/create") { launchSingleTop = true }
                    },
                    onNavigateToContinueGoal = {
                        innerNavController.navigate("goals/create") { launchSingleTop = true }
                    },
                    onNavigateToGoalDetail = { goalId ->
                        innerNavController.navigate("goals/detail/${Uri.encode(goalId)}") {
                            launchSingleTop = true
                        }
                    }
                )
            }
            composable("goals/create") {
                val savedDraft by goalsViewModel.creationDraft.collectAsState()
                GoalCreationScreen(
                    habits = habitViewModel.habits.toList(),
                    savedDraft = savedDraft,
                    onBack = { innerNavController.popBackStack() },
                    onNavigateToAddHabit = onNavigateToAddHabit,
                    onPersistDraft = { state, step, returnAfterAddHabit ->
                        goalsViewModel.persistCreationDraft(state, step, returnAfterAddHabit)
                    },
                    onClearDraft = { goalsViewModel.clearCreationDraft() },
                    onSaveGoal = { state ->
                        val type = state.goalType ?: GoalType.BUILD
                        val softReview = if (type == GoalType.BUILD && state.softReviewEnabled) {
                            state.startDate + 56L * 24 * 60 * 60 * 1000
                        } else null
                        goalsViewModel.saveGoal(
                            title = state.title.trim().ifEmpty { "My goal" },
                            whyStatement = state.whyStatement.trim(),
                            goalType = type,
                            targetValue = if (type == GoalType.REACH) state.targetValue.toFloatOrNull() else null,
                            unit = if (type == GoalType.REACH) state.unit.trim() else "",
                            startDate = state.startDate,
                            targetDate = state.targetDate,
                            linkedHabitIds = state.linkedHabitIds.toList(),
                            colorHex = state.colorHex,
                            tinyVersionText = state.tinyVersionText,
                            anchorCue = state.anchorCue,
                            softReviewDate = softReview,
                            paceProfile = if (type == GoalType.REACH) state.paceProfile else null,
                            triggerText = state.triggerText,
                            replacementText = state.replacementText,
                            costPerOccurrence = if (type == GoalType.QUIT && state.trackCost) {
                                state.costPerOccurrence.toFloatOrNull()
                            } else null,
                            costUnit = if (type == GoalType.QUIT && state.trackCost) state.costUnit else null,
                            floorCount = if (type == GoalType.MAINTAIN) state.floorCount.toIntOrNull() else null,
                            floorPeriodDays = if (type == GoalType.MAINTAIN) {
                                state.floorPeriodDays.toIntOrNull() ?: 7
                            } else null,
                        )
                        innerNavController.popBackStack()
                        onShowNotification("Goal created", false, null)
                    }
                )
            }
            composable(
                route = "goals/detail/{goalId}",
                arguments = listOf(navArgument("goalId") { type = NavType.StringType })
            ) { entry ->
                val rawId = entry.arguments?.getString("goalId").orEmpty()
                val decodedId = Uri.decode(rawId)
                GoalDetailScreen(
                    goalId = decodedId,
                    viewModel = goalsViewModel,
                    habits = habitViewModel.habits.toList(),
                    onBack = { innerNavController.popBackStack() }
                )
            }
            composable("profile") {
                val challengeState by challengeViewModel.state.collectAsState()
                val friendSummaries by friendsViewModel.friendSummaries.collectAsState()
                val weeklyProfileViews by profileSocialViewModel.weeklyFriendViews.collectAsState()
                val equippedTitleId by settingsViewModel.equippedTitleId.collectAsState()
                val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                val allUserChallenges = remember(challengeState.challenges, challengeState.archivedChallenges, uid) {
                    (challengeState.challenges + challengeState.archivedChallenges)
                        .distinctBy { it.id }
                        .filter { uid.isNotEmpty() && uid in it.members }
                }
                val habitsSnapshot = habitViewModel.habits
                    .filter { !it.isArchived && !it.isPaused }
                    .map { it.id to it.completedDates.size }
                val hasPerfectWeek = remember(habitsSnapshot) {
                    val active = habitViewModel.habits.filter { !it.isArchived && !it.isPaused }
                    if (active.isEmpty()) {
                        false
                    } else {
                        val today = LocalDate.now()
                        (0..6).all { offset ->
                            val day = today.minusDays(offset.toLong()).toString()
                            active.all { it.completedDates.contains(day) }
                        }
                    }
                }
                AccountScreen(
                    statsState = statsState,
                    activeChallenges = challengeState.challenges.filter { ch ->
                        ch.isActive && uid.isNotEmpty() && uid in ch.members
                    },
                    allUserChallenges = allUserChallenges,
                    userId = uid,
                    userName = userName,
                    userUsername = userUsername,
                    userPhotoUri = userPhotoUri,
                    userBio = userBio,
                    profileAccentKey = userProfileAccent,
                    equippedTitleId = equippedTitleId,
                    onEquipTitle = { id, label -> settingsViewModel.setEquippedTitleId(id, label) },
                    billingViewModel = billingViewModel,
                    coinViewModel = coinViewModel,
                    friendsViewModel = friendsViewModel,
                    friendSummaries = friendSummaries,
                    duoState = duoState,
                    referralState = referralState,
                    weeklyProfileViews = weeklyProfileViews,
                    hasPerfectWeek = hasPerfectWeek,
                    profileSocialViewModel = profileSocialViewModel,
                    onNavigateToEditProfile = onNavigateToEditProfile,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToAchievements = onNavigateToAchievements,
                    onNavigateToMemberProfile = navigateToMemberProfile,
                    onNavigateToChallenges = {
                        navigateToArena("challenges", null)
                    },
                    onNavigateToFocusGarden = {
                        innerNavController.navigate("focus_garden") { launchSingleTop = true }
                    },
                    onNavigateToSubscription = {
                        onNavigateToSettingsSection("subscription")
                    },
                    onOpenChallengeDetail = { challengeId ->
                        challengeViewModel.requestOpenChallengeDetail(challengeId)
                        navigateToArena("challenges", null)
                    },
                    onShowNotification = { msg, isError -> onShowNotification(msg, isError, null) },
                    onLogout = onLogout,
                    hasDuoBuddy = duoState.blocksNewDuoInvite(),
                    myUsername = userUsername,
                    myPhotoUrl = userPhotoUri,
                    onInviteAccountabilityBuddy = { uid, username, photo, onResult ->
                        accountabilityViewModel.inviteBuddy(
                            uid = uid,
                            username = username,
                            photoUrl = photo,
                            myUsername = userUsername.ifBlank { userName },
                            myPhotoUrl = userPhotoUri,
                            onResult = onResult,
                        )
                    },
                    notificationBadgeCount = bellBadgeCount,
                    onOpenNotifications = navigateToNotifications,
                    notificationsViewModel = notificationsViewModel,
                    onNavigateToFriendsGrove = navigateToFriendsGrove,
                    onNavigateToOpenGrove = navigateToOpenGrove,
                )
            }
            composable("profile_subscription") {
                SubscriptionManageScreen(
                    billingViewModel = billingViewModel,
                    onBack = { innerNavController.popBackStack() },
                    onRequestPaywall = onRequestPaywall,
                    onShowBanner = { msg -> onShowNotification(msg, false, null) },
                )
            }
            composable(
                route = "member_profile/{userId}",
                arguments = listOf(navArgument("userId") { type = NavType.StringType }),
            ) { entry ->
                val raw = entry.arguments?.getString("userId").orEmpty()
                val decodedUid = Uri.decode(raw)
                MemberProfileScreen(
                    memberUid = decodedUid,
                    challengeViewModel = challengeViewModel,
                    friendsViewModel = friendsViewModel,
                    profileSocialViewModel = profileSocialViewModel,
                    onBack = { innerNavController.popBackStack() },
                    onShowNotification = { msg, isError, icon -> onShowNotification(msg, isError, icon) },
                    canInviteToDuo = !duoState.blocksNewDuoInvite(),
                    coinViewModel = coinViewModel,
                    myUsername = userUsername.ifBlank { userName },
                    onInviteToDuo = { username, photo ->
                        accountabilityViewModel.inviteBuddy(
                            uid = decodedUid,
                            username = username,
                            photoUrl = photo,
                            myUsername = userUsername.ifBlank { userName },
                            myPhotoUrl = userPhotoUri,
                        ) { ok, err ->
                            onShowNotification(
                                if (ok) "Duo streak invite sent to @$username" else (err ?: "Couldn't send duo invite"),
                                !ok,
                                null,
                            )
                        }
                    },
                )
            }
        }
    }

    weeklyGoalId?.let { goalId ->
        val goal by goalsViewModel.goalById(goalId).collectAsState(initial = null)
        goal?.let { g ->
            WeeklyCheckInSheet(
                goal = g,
                onDismiss = { goalsViewModel.dismissWeeklyCheckIn() },
                onSubmit = { status, note ->
                    goalsViewModel.submitWeeklyCheckIn(goalId, status, note)
                    onShowNotification("Check-in saved", false, null)
                },
            )
        }
    }

    if (weeklyGoalId == null) {
        catchUpGoalId?.let { goalId ->
            val goal by goalsViewModel.goalById(goalId).collectAsState(initial = null)
            goal?.let { g ->
                QuitCatchUpSheet(
                    goal = g,
                    onDismiss = { goalsViewModel.dismissQuitCatchUp() },
                    onResolve = { resolution, slipDate ->
                        goalsViewModel.resolveQuitCatchUp(goalId, resolution, slipDate)
                    },
                )
            }
        }
    }

    maintainNudgeGoalId?.let { nudgeGoalId ->
        val goal by goalsViewModel.goalById(nudgeGoalId).collectAsState(initial = null)
        goal?.let { g ->
            MaintainNudgeSheet(
                goal = g,
                onDismiss = { goalsViewModel.dismissMaintainNudge() },
                onOpenGoal = {
                    goalsViewModel.dismissMaintainNudge()
                    innerNavController.navigate("goals/detail/${Uri.encode(g.id)}") {
                        launchSingleTop = true
                    }
                },
            )
        }
    }

    when (val event = celebrationEvent) {
        is GoalCelebration.Milestone -> {
            val goal by goalsViewModel.goalById(event.goalId).collectAsState(initial = null)
            val milestones by goalsViewModel.milestonesForGoal(event.goalId).collectAsState(initial = emptyList())
            val milestone = milestones.find { it.id == event.milestoneId }
            val habitsSnapshot = habitViewModel.habits.toList()
            val metrics = goal?.let { goalsViewModel.metricsFor(it, habitsSnapshot) }
            if (goal != null && milestone != null) {
                val shareMilestone = {
                    val g = goal!!
                    val m = milestone!!
                    val cheer = GoalCelebrationCopy.milestoneCheer(g, m)
                    val linked = GoalProgressEngine.linkedHabits(g, habitsSnapshot)
                    val habitReps = linked.sumOf { it.completedDates.size }
                    shareGoalMilestoneCard(
                        context = context,
                        goal = g,
                        milestone = m,
                        metrics = metrics,
                        cheerLine = cheer,
                        habitReps = habitReps,
                    )
                    celebrationEvent = null
                }
                if (event.isRitual) {
                    GoalMomentRitualScreen(
                        goal = goal!!,
                        milestone = milestone,
                        metrics = metrics,
                        onDismiss = { celebrationEvent = null },
                        onShare = shareMilestone,
                    )
                } else {
                    MilestoneCelebrationScreen(
                        goal = goal!!,
                        milestone = milestone,
                        metrics = metrics,
                        onDismiss = { celebrationEvent = null },
                        onShare = shareMilestone,
                    )
                }
            }
        }
        is GoalCelebration.Completed -> {
            val goal by goalsViewModel.goalById(event.goalId).collectAsState(initial = null)
            var stats by remember(event.goalId) {
                mutableStateOf<com.saintnico.verdlyhabits.ui.viewmodel.GoalCompletionStats?>(null)
            }
            LaunchedEffect(event.goalId, habitViewModel.habits.size) {
                stats = goalsViewModel.completionStatsFor(
                    event.goalId,
                    habitViewModel.habits.toList(),
                )
            }
            goal?.let { g ->
                GoalCompletionScreen(
                    goal = g,
                    stats = stats,
                    onArchive = {
                        goalsViewModel.archiveGoal(g.id)
                        celebrationEvent = null
                    },
                    onSetNewGoal = {
                        celebrationEvent = null
                        innerNavController.navigate("goals/create") { launchSingleTop = true }
                    },
                    onShare = { /* share handled inside GoalCompletionScreen */ },
                )
            }
        }
        null -> Unit
    }

    duoMilestone?.let { milestone ->
        androidx.compose.foundation.layout.Box(
            Modifier
                .fillMaxSize()
                .background(androidx.compose.ui.graphics.Color.Black.copy(0.92f)),
        ) {
            com.saintnico.verdlyhabits.ui.components.social.DuoMilestoneCelebration(
                milestone = milestone,
                onDismiss = { duoMilestone = null },
                onShare = {
                    com.saintnico.verdlyhabits.ui.components.share.shareDuoMilestoneCard(
                        context = context,
                        streakDays = milestone.streakDays,
                        buddyUsername = milestone.buddyUsername,
                    )
                    duoMilestone = null
                },
            )
        }
    }

    LaunchedEffect(duoBroken) {
        duoBroken?.let { broken ->
            onShowNotification(
                "Duo streak ended at ${broken.previousStreak} days with ${broken.buddyUsername}",
                true,
                Icons.Rounded.LocalFireDepartment,
            )
            duoBroken = null
        }
    }

    statsState.unlockQueue.firstOrNull()?.let { achievement ->
        key(achievement.id) {
            TrophyUnlockCeremony(
                achievement = achievement,
                haptics = AppAudio.haptics(context),
                sound = AppAudio.sound(context),
                onDismiss = { userStatsViewModel.consumeUnlock() },
                onShare = { shareAchievementCard(context, achievement) },
            )
        }
    }

    stakeCeremony?.let { ceremony ->
        StakeResolutionCeremony(
            ceremony = ceremony,
            onBuyCoins = {
                val reason = when (ceremony) {
                    is com.saintnico.verdlyhabits.ui.viewmodel.StakeCeremony.Converted -> CoinPackReason.AFTER_CONVERT
                    is com.saintnico.verdlyhabits.ui.viewmodel.StakeCeremony.Forfeited -> CoinPackReason.AFTER_FORFEIT
                }
                coinViewModel.consumeCeremony()
                coinViewModel.openCoinPacks(reason)
            },
            onDismiss = { coinViewModel.consumeCeremony() },
        )
    }

    if (afterLockBuy && stakeCeremony == null) {
        AfterLockBuySheet(
            onBuy = { coinViewModel.openCoinPacks(CoinPackReason.AFTER_LOCK) },
            onDismiss = { coinViewModel.consumeAfterLockBuy() },
        )
    }

    if (coinPackReason != null) {
        CoinPackSheet(
            isOpen = true,
            billingViewModel = billingViewModel,
            isPremium = isPro,
            reason = coinPackReason ?: CoinPackReason.BROWSE,
            need = coinPackNeed,
            pendingCosmeticId = pendingCosmeticId,
            balance = coinBalance,
            ownedCosmeticIds = ownedCosmeticIds,
            equippedGlowId = equippedGlowId,
            equippedSkinId = equippedSkinId,
            equippedFrameId = equippedFrameId,
            equippedOathChipId = equippedOathChipId,
            equippedPlantId = equippedPlantId,
            previewTitle = userUsername.ifBlank { userName }.ifBlank { "Rooted" },
            onBuyCosmetic = { id, cb ->
                coinViewModel.buyCosmetic(id) { ok, msg ->
                    if (ok) coinViewModel.consumePendingCosmetic()
                    cb(ok, msg)
                }
            },
            onEquipCosmetic = { coinViewModel.equipCosmetic(it) },
            onUnequipCosmetic = { coinViewModel.unequipCosmetic(it) },
            onOpenPremium = { onRequestPaywall(PaywallTrigger.OathDoubleXp) },
            onCosmeticNotice = { msg ->
                onShowNotification(msg, false, Icons.Rounded.Redeem)
            },
            onDismiss = { coinViewModel.closeCoinPacks() },
            onPurchaseSuccess = { coinViewModel.celebratePurchase() },
        )
    }

    if (purchaseCelebration) {
        CoinBurstAnimation(
            premium = isPro,
            onFinished = { coinViewModel.endCelebration() },
        )
    }

    CreateSheet(
        visible = showCreateSheet,
        currentTabRoute = bottomNavTabRoute,
        state = createState,
        haptics = createHaptics,
        fabOriginInWindow = createFabOrigin,
        onDismiss = { showCreateSheet = false },
        onAction = { action ->
            when (action) {
                CreateAction.AddHabit -> {
                    if (!createState.canAddHabit) {
                        onRequestPaywall(PaywallTrigger.HabitLimit)
                    } else {
                        onNavigateToAddHabit()
                    }
                }
                CreateAction.NewGoal -> {
                    innerNavController.navigate("goals") {
                        popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                    innerNavController.navigate("goals/create") { launchSingleTop = true }
                }
                CreateAction.CreateChallenge -> {
                    challengeViewModel.requestOpenCreate()
                    navigateToArena("challenges", null)
                }
                CreateAction.StartFocus -> navigateToFocus(null)
                CreateAction.SharePlant -> showPulseComposer = true
                CreateAction.PostProof -> {
                    goHomeTab()
                    proofLaunchToken += 1
                }
                CreateAction.PlantSomething -> {
                    goHomeTab()
                    habitsFocusToken += 1
                }
                CreateAction.ShareCard -> {
                    val uid = inboxUid
                    val allChallenges = (
                        challengeStateForTrophies.challenges +
                            challengeStateForTrophies.archivedChallenges
                        ).distinctBy { it.id }
                        .filter { uid.isNotEmpty() && uid in it.members }
                    val activeHabits = habitViewModel.habits.filter { !it.isArchived && !it.isPaused }
                    val hasPerfectWeek = if (activeHabits.isEmpty()) {
                        false
                    } else {
                        val today = java.time.LocalDate.now()
                        (0..6).all { offset ->
                            val day = today.minusDays(offset.toLong()).toString()
                            activeHabits.all { it.completedDates.contains(day) }
                        }
                    }
                    val livePin = if (pinnedTrophyUntil > System.currentTimeMillis() &&
                        !pinnedTrophyLabel.isNullOrBlank()
                    ) {
                        pinnedTrophyLabel
                    } else {
                        null
                    }
                    shareProfileCard(
                        context,
                        OwnProfileShot.fromShell(
                            userId = uid,
                            userName = userName,
                            userUsername = userUsername,
                            userPhotoUri = userPhotoUri,
                            userBio = userBio,
                            statsState = statsState,
                            equippedTitleId = equippedTitleId,
                            titleGlowId = equippedGlowId ?: groveMe?.titleGlowId,
                            profileFrameId = equippedFrameId ?: groveMe?.profileFrameId,
                            grovePlantId = equippedPlantId ?: groveMe?.grovePlantId,
                            proclaimedLabel = livePin,
                            membershipTier = membershipTier,
                            accentKey = userProfileAccent,
                            friendSummaries = friendSummaries,
                            duoState = duoState,
                            allUserChallenges = allChallenges,
                            referralsSent = referralState.qualifiedCount,
                            isPro = isPro,
                            hasPerfectWeek = hasPerfectWeek,
                            ownedCosmeticIds = ownedCosmeticIds,
                        ),
                    )
                }
                CreateAction.ShareMilestone -> {
                    val mile = createState.milestone
                    when {
                        mile?.achievement != null -> shareAchievementCard(context, mile.achievement)
                        mile != null -> shareStreakCardWithImage(
                            context = context,
                            headline = "${mile.streakDays}-day streak",
                            habitName = mile.habitTitle.orEmpty(),
                        )
                    }
                    mile?.shareKey?.let { key ->
                        createScope.launch { createStore.setCreateSharedMilestoneKey(key) }
                    }
                }
                CreateAction.Nudge -> {
                    val friends = createState.nudgeFriends
                    when {
                        friends.size == 1 -> nudgeTarget = NudgeTarget(
                            uid = friends[0].uid,
                            username = friends[0].username,
                            photoUrl = friends[0].photoUrl,
                            surface = NudgeSurface.GROVE,
                        )
                        friends.isNotEmpty() -> createPickerMode = CreatePickerMode.NUDGE
                    }
                }
                CreateAction.Invite -> {
                    referralViewModel.ensureReady(hasPaidSubscription)
                    showReferralInvite = true
                }
                CreateAction.Duo -> {
                    if (createState.hasDuo) {
                        navigateToDuo()
                    } else {
                        createPickerMode = CreatePickerMode.DUO
                    }
                }
            }
        },
    )

    CreatePulseComposer(
        visible = showPulseComposer,
        state = createState,
        haptics = createHaptics,
        fabOriginInWindow = createFabOrigin,
        onShare = { title ->
            groveViewModel.sharePulse(title)
            showPulseComposer = false
            onShowNotification("Shared with the grove", false, null)
        },
        onClearShare = {
            groveViewModel.clearPulseShare()
            showPulseComposer = false
            onShowNotification("Taken down", false, null)
        },
        onDismiss = { showPulseComposer = false },
    )

    NudgeSheetHost(
        target = nudgeTarget,
        onDismiss = { nudgeTarget = null },
        onSent = { onShowNotification("Nudge sent", false, null) },
        onError = { onShowNotification(it, true, null) },
        settingsViewModel = settingsViewModel,
    )

    createPickerMode?.let { mode ->
        CreateFriendPicker(
            mode = mode,
            friends = createState.nudgeFriends,
            onPick = { friend ->
                createPickerMode = null
                when (mode) {
                    CreatePickerMode.NUDGE -> nudgeTarget = NudgeTarget(
                        uid = friend.uid,
                        username = friend.username,
                        photoUrl = friend.photoUrl,
                        surface = NudgeSurface.GROVE,
                    )
                    CreatePickerMode.DUO -> accountabilityViewModel.inviteBuddy(
                        uid = friend.uid,
                        username = friend.username,
                        photoUrl = friend.photoUrl,
                        myUsername = userUsername.ifBlank { userName },
                        myPhotoUrl = userPhotoUri,
                    ) { ok, err ->
                        onShowNotification(
                            if (ok) "Duo invite sent to @${friend.username}" else (err ?: "Couldn't send duo invite"),
                            !ok,
                            null,
                        )
                    }
                }
            },
            onDismiss = { createPickerMode = null },
        )
    }

    ReferralShareSheet(
        visible = showReferralInvite,
        state = referralState,
        onDismiss = { showReferralInvite = false },
        onCopiedAck = {
            onShowNotification("Invite link copied. Paste anywhere", false, null)
        },
        onRegisterVanityCode = { vanity, onResult ->
            referralViewModel.registerVanityCode(vanity, onResult)
        },
    )

    ReferralAnnualOfferSheet(
        visible = showAnnualOffer,
        daysLeft = referralState.annualOfferDaysLeft,
        onClaimAnnual = {
            showAnnualOffer = false
            onRequestPaywall(PaywallTrigger.ReferralAnnual)
        },
        onDismiss = {
            showAnnualOffer = false
            referralViewModel.dismissAnnualOffer()
        },
    )

    com.saintnico.verdlyhabits.ui.components.StreakRepairPromptHost(
        visible = (currentRoute == "arena" || currentRoute.startsWith("arena") ||
            currentRoute == "challenges" || currentRoute == "duo") &&
            habitViewModel.habits.isNotEmpty(),
        habits = habitViewModel.habits.toList(),
        onUseShield = { habitId, missedDate, onResult ->
            habitViewModel.repairWithShield(habitId, missedDate) { ok ->
                if (ok) {
                    onShowNotification("Streak shield used. Run preserved", false, null)
                }
                onResult(ok)
            }
        },
        onRequestPaywall = { onRequestPaywall(PaywallTrigger.StreakRepairableNoShield) },
    )

    ShareShotHost(shareShotController)
    }
}
