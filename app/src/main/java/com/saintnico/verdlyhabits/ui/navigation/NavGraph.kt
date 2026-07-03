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
import com.saintnico.verdlyhabits.monetization.PaywallTrigger
import com.saintnico.verdlyhabits.navigation.NotificationLaunch
import com.saintnico.verdlyhabits.session.clearSignedOutSession
import com.saintnico.verdlyhabits.session.prepareSignedInSession
import com.saintnico.verdlyhabits.ui.components.PremiumNotificationBar
import com.saintnico.verdlyhabits.ui.components.ProPaywallSheet
import com.saintnico.verdlyhabits.ui.components.referral.ReferralAnnualOfferSheet
import com.saintnico.verdlyhabits.ui.components.referral.ReferralShareSheet
import com.saintnico.verdlyhabits.ui.components.social.blocksNewDuoInvite
import com.saintnico.verdlyhabits.ui.components.social.needsDuoAttention
import com.saintnico.verdlyhabits.ui.screens.achievements.AchievementsScreen
import com.saintnico.verdlyhabits.ui.screens.challenge.ChallengeScreen
import com.saintnico.verdlyhabits.ui.screens.duo.DuoScreen
import com.saintnico.verdlyhabits.ui.screens.focus.FocusModeScreen
import com.saintnico.verdlyhabits.ui.screens.focus.PlantGardenScreen
import com.saintnico.verdlyhabits.ui.screens.goals.GoalsHomeScreen
import com.saintnico.verdlyhabits.ui.screens.goals.celebration.GoalCompletionScreen
import com.saintnico.verdlyhabits.ui.screens.goals.celebration.MilestoneCelebrationScreen
import com.saintnico.verdlyhabits.ui.screens.goals.checkin.WeeklyCheckInSheet
import com.saintnico.verdlyhabits.ui.screens.goals.creation.GoalCreationScreen
import com.saintnico.verdlyhabits.ui.screens.goals.detail.GoalDetailScreen
import com.saintnico.verdlyhabits.ui.screens.habit.AddHabitScreen
import com.saintnico.verdlyhabits.ui.screens.home.HomeScreen
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
import com.saintnico.verdlyhabits.ui.viewmodel.FocusViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.FriendsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.GoalCelebration
import com.saintnico.verdlyhabits.ui.viewmodel.GoalsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.HabitViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.MoodViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.NotificationsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.ReferralViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.SettingsViewModel
import com.saintnico.verdlyhabits.ui.viewmodel.UserStatsViewModel
import java.time.LocalDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

// ─── Route constants ─────────────────────────────────────────────────────────
sealed class Screen(val route: String) {
    object Onboarding : Screen("onboarding")
    object Login : Screen("login")
    object SignUp : Screen("signup")
    object Main : Screen("main")           // Shell with bottom nav
    object Home : Screen("home")
    object Stats : Screen("stats")
    object Focus : Screen("focus")
    object Goals : Screen("goals")
    object Profile : Screen("profile")
    object Challenges : Screen("challenges")
    object Duo : Screen("duo")
    object Achievements : Screen("achievements")
    object AddHabit : Screen("add_habit")
    object EditHabit : Screen("edit_habit/{habitId}") {
        fun createRoute(habitId: String) = "edit_habit/$habitId"
    }
    object Settings : Screen("settings")
    object EditProfile : Screen("edit_profile")
    object ProfileSetup : Screen("profile_setup")
    object MoodCheckIn : Screen("mood_checkin")
}

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

    var notificationMessage by remember { mutableStateOf("") }
    var showNotification by remember { mutableStateOf(false) }
    var notificationIsError by remember { mutableStateOf(false) }
    var notificationIcon by remember { mutableStateOf<androidx.compose.ui.graphics.vector.ImageVector?>(null) }
    val coroutineScope = rememberCoroutineScope()

    var paywallTrigger by remember { mutableStateOf<PaywallTrigger?>(null) }
    val hasFullAccess by billingViewModel.hasFullAccess.collectAsState()
    val hasPaidSubscription by billingViewModel.hasPaidSubscription.collectAsState()

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
    val userMotto by settingsViewModel.userMotto.collectAsState()
    val userFavoritePlant by settingsViewModel.userFavoritePlant.collectAsState()
    val totalFocusMinutes by focusViewModel.totalFocusMinutes.collectAsState()

    // Auto-restore data on startup if already logged in
    LaunchedEffect(Unit) {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null) {
            prepareSignedInSession(
                context = navController.context,
                habitViewModel = habitViewModel,
                userStatsViewModel = userStatsViewModel,
                settingsViewModel = settingsViewModel,
            )
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
                    onLoginSuccess = {
                        coroutineScope.launch {
                            prepareSignedInSession(
                                context = navController.context,
                                habitViewModel = habitViewModel,
                                userStatsViewModel = userStatsViewModel,
                                settingsViewModel = settingsViewModel,
                            )
                            val needsSetup = runCatching {
                                com.saintnico.verdlyhabits.data.remote.firestore.UserRepository()
                                    .needsUsernameSetup()
                            }.getOrDefault(true)
                            val dest = if (needsSetup) Screen.ProfileSetup.route else Screen.Main.route
                            navController.navigate(dest) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    },
                    onNewUser = {
                        coroutineScope.launch {
                            prepareSignedInSession(
                                context = navController.context,
                                habitViewModel = habitViewModel,
                                userStatsViewModel = userStatsViewModel,
                                settingsViewModel = settingsViewModel,
                            )
                            navController.navigate(Screen.ProfileSetup.route) {
                                popUpTo(Screen.Login.route) { inclusive = true }
                            }
                        }
                    },
                )
            }

            composable(Screen.SignUp.route) {
                SignUpScreen(
                    onSignUpSuccess = {
                        settingsViewModel.markOnboardingCompleted()
                        settingsViewModel.syncProfileFromFirebase()
                        navController.navigate(Screen.ProfileSetup.route) {
                            popUpTo(Screen.SignUp.route) { inclusive = true }
                        }
                    },
                    onBackToLogin = {
                        navController.popBackStack()
                    }
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
                val friendsViewModel: FriendsViewModel = viewModel()
                val goalsViewModel: GoalsViewModel = viewModel()
                val mainContext = androidx.compose.ui.platform.LocalContext.current
                val onLogoutMain: () -> Unit = {
                    coroutineScope.launch {
                        clearSignedOutSession(mainContext, habitViewModel)
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
                    billingViewModel = billingViewModel,
                    hasFullAccess = hasFullAccess,
                    onRequestPaywall = { paywallTrigger = it },
                    statsState = statsState,
                    userName = userName,
                    userUsername = userUsername,
                    userPhotoUri = userPhotoUri,
                    userBio = userBio,
                    userMotto = userMotto,
                    userFavoritePlant = userFavoritePlant,
                    totalFocusMinutes = totalFocusMinutes,
                    onNavigateToAddHabit = { navController.navigate(Screen.AddHabit.route) },
                    onNavigateToEditHabit = { id -> navController.navigate(Screen.EditHabit.createRoute(id)) },
                    onNavigateToSettings = { navController.navigate(Screen.Settings.route) },
                    onNavigateToEditProfile = { navController.navigate(Screen.EditProfile.route) },
                    onNavigateToAchievements = { navController.navigate(Screen.Achievements.route) },
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
                route = Screen.Settings.route,
                enterTransition = { slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(400)) },
                exitTransition = { slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(400)) }
            ) {
                val context = androidx.compose.ui.platform.LocalContext.current
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        coroutineScope.launch {
                            clearSignedOutSession(context, habitViewModel)
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
                    billingViewModel = billingViewModel,
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

        ProPaywallSheet(
            isOpen = paywallTrigger != null,
            trigger = paywallTrigger,
            billingViewModel = billingViewModel,
            onDismiss = { paywallTrigger = null },
            onRestorePurchases = { billingViewModel.restorePurchases() },
        )
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
    billingViewModel: BillingViewModel,
    hasFullAccess: Boolean,
    onRequestPaywall: (PaywallTrigger) -> Unit,
    statsState: com.saintnico.verdlyhabits.ui.viewmodel.UserStatsUiState,
    userName: String,
    userUsername: String,
    userPhotoUri: String?,
    userBio: String,
    userMotto: String,
    userFavoritePlant: String,
    totalFocusMinutes: Int,
    onNavigateToAddHabit: () -> Unit,
    onNavigateToEditHabit: (String) -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToEditProfile: () -> Unit,
    onNavigateToAchievements: () -> Unit,
    onLogout: () -> Unit,
    onShowNotification: (String, Boolean, androidx.compose.ui.graphics.vector.ImageVector?) -> Unit,
    widgetLaunch: com.saintnico.verdlyhabits.widget.WidgetLaunchRequest? = null,
    onWidgetLaunchConsumed: () -> Unit = {},
    notificationLaunch: NotificationLaunch? = null,
    onNotificationLaunchConsumed: () -> Unit = {},
) {
    val innerNavController = rememberNavController()
    val navigateToMemberProfile: (String) -> Unit = { memberId ->
        innerNavController.navigate("member_profile/${Uri.encode(memberId)}")
    }
    val navBackStackEntry by innerNavController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "home"

    val navigateToFocus: (String?) -> Unit = { habitId ->
        val route = if (habitId != null) "focus/$habitId" else "focus"
        innerNavController.navigate(route) { launchSingleTop = true }
    }

    val bottomNavTabRoute = when {
        currentRoute.startsWith("goals/") -> "goals"
        else -> currentRoute
    }

    var celebrationEvent by remember { mutableStateOf<GoalCelebration?>(null) }
    val weeklyGoalId by goalsViewModel.weeklyCheckInGoalId.collectAsState()

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
                innerNavController.navigate("duo") {
                    popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
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
                innerNavController.navigate("challenges") {
                    popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            com.saintnico.verdlyhabits.widget.WidgetNavigation.ROUTE_DUO -> {
                innerNavController.navigate("duo") {
                    popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
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
                innerNavController.navigate("challenges") {
                    popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
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
        currentRoute == "goals/create" -> false
        currentRoute.startsWith("goals/detail/") -> false
        currentRoute == "profile_subscription" -> false
        currentRoute == "notifications" -> false
        currentRoute.startsWith("member_profile") -> false
        else -> bottomNavItems.any { it.route == currentRoute }
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
    val bellBadgeCount = maxOf(
        notifState.pendingActionCount,
        notifState.unreadCount,
        incomingFriends.size + arenaInvites.size + if (duoState?.isIncomingInvite == true) 1 else 0,
    )
    val navigateToNotifications: () -> Unit = {
        innerNavController.navigate("notifications") { launchSingleTop = true }
    }
    val hasPaidSubscription by billingViewModel.hasPaidSubscription.collectAsState()
    var showReferralInvite by remember { mutableStateOf(false) }
    var showAnnualOffer by remember { mutableStateOf(false) }

    LaunchedEffect(hasPaidSubscription) {
        referralViewModel.refresh(hasPaidSubscription)
    }

    LaunchedEffect(referralState.rewardJustGranted, referralState.retentionBonusesGranted) {
        if (referralState.rewardJustGranted) {
            onShowNotification(
                "You did it — ${referralState.rewardDays} days of Pro are yours.",
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
                "+${referralState.retentionBonusesGranted * com.saintnico.verdlyhabits.referral.ReferralManager.REFERRER_RETENTION_BONUS_DAYS} bonus days — friends stayed 7 days!",
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

    LaunchedEffect(accountabilityViewModel) {
        accountabilityViewModel.milestoneEvent.collect { duoMilestone = it }
    }
    LaunchedEffect(accountabilityViewModel) {
        accountabilityViewModel.streakBrokenEvent.collect { duoBroken = it }
    }

    val duoNeedsAttention = duoState.needsDuoAttention()
    val navigateToDuo: () -> Unit = {
        innerNavController.navigate("duo") {
            popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

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
                        innerNavController.navigate(route) {
                            popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
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
                        innerNavController.navigate("challenges") {
                            popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
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
                        referralViewModel.ensureReady()
                        showReferralInvite = true
                    },
                    onDismissReferralBanner = { referralViewModel.dismissBanner() },
                    onNavigateToDuo = navigateToDuo,
                    duoNeedsAttention = duoNeedsAttention,
                    duoAtRisk = duoState?.streakAtRisk == true,
                    notificationBadgeCount = bellBadgeCount,
                    onOpenNotifications = navigateToNotifications,
                )
            }
            composable("duo") {
                com.saintnico.verdlyhabits.ui.screens.duo.DuoScreen(
                    duoState = duoState,
                    friendsViewModel = friendsViewModel,
                    myUsername = userUsername,
                    myPhotoUrl = userPhotoUri,
                    hasDuoBuddy = duoState?.status == "active" || duoState?.status == "pending",
                    onAcceptInvite = { accountabilityViewModel.acceptInvite(it) },
                    onDeclineInvite = { accountabilityViewModel.declineInvite(it) },
                    onApplyGrace = { pairId ->
                        accountabilityViewModel.applyGrace(pairId) { ok, err ->
                            onShowNotification(
                                if (ok) "Pro shield applied — yesterday forgiven" else (err ?: "Couldn't apply shield"),
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
                    onNavigateToMemberProfile = navigateToMemberProfile,
                    onShowNotification = { msg, isError -> onShowNotification(msg, isError, null) },
                    referralState = referralState,
                    onOpenReferral = {
                        referralViewModel.ensureReady()
                        showReferralInvite = true
                    },
                )
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
                        innerNavController.navigate("challenges") {
                            popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onOpenRoute = { route ->
                        when (route.lowercase()) {
                            "paywall", "pro", "subscription" -> onRequestPaywall(PaywallTrigger.GoPro)
                            "referral" -> {
                                referralViewModel.ensureReady()
                                showReferralInvite = true
                            }
                            "duo" -> navigateToDuo()
                            "challenges", "arena" -> {
                                innerNavController.navigate("challenges") {
                                    popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    },
                    onShowToast = { msg, isError -> onShowNotification(msg, isError, null) },
                )
            }
            composable("stats") {
                StatsScreen(
                    habits = habitViewModel.habits.toList(),
                    statsState = statsState,
                    hasFullAccess = hasFullAccess,
                    installDateMillis = billingViewModel.getInstallDateMillis(),
                    onRequestPaywall = onRequestPaywall,
                    onBack = { innerNavController.popBackStack() }
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
                PlantGardenScreen(onBack = { innerNavController.popBackStack() })
            }
            composable("challenges") {
                ChallengeScreen(
                    viewModel = challengeViewModel,
                    onShowNotification = { msg, isError -> onShowNotification(msg, isError, null) },
                    onNavigateToEditProfile = onNavigateToEditProfile,
                    onNavigateToMemberProfile = navigateToMemberProfile,
                    friendsViewModel = friendsViewModel,
                )
            }
            composable("goals") {
                GoalsHomeScreen(
                    viewModel = goalsViewModel,
                    habits = habitViewModel.habits.toList(),
                    onNavigateToAddGoal = {
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
                GoalCreationScreen(
                    habits = habitViewModel.habits.toList(),
                    onBack = { innerNavController.popBackStack() },
                    onNavigateToAddHabit = onNavigateToAddHabit,
                    onSaveGoal = { state ->
                        val type = state.goalType ?: GoalType.BUILD
                        val targetVal =
                            if (type == GoalType.REACH) state.targetValue.toFloatOrNull() else null
                        val unit = if (type == GoalType.REACH) state.unit.trim() else ""
                        goalsViewModel.saveGoal(
                            title = state.title.trim().ifEmpty { "My goal" },
                            whyStatement = state.whyStatement.trim(),
                            goalType = type,
                            targetValue = targetVal,
                            unit = unit,
                            startDate = state.startDate,
                            targetDate = state.targetDate,
                            linkedHabitIds = state.linkedHabitIds.toList(),
                            colorHex = state.colorHex
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
                    onBack = { innerNavController.popBackStack() }
                )
            }
            composable("profile") {
                val challengeState by challengeViewModel.state.collectAsState()
                val friendSummaries by friendsViewModel.friendSummaries.collectAsState()
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
                    equippedTitleId = equippedTitleId,
                    onEquipTitle = { id, label -> settingsViewModel.setEquippedTitleId(id, label) },
                    billingViewModel = billingViewModel,
                    friendsViewModel = friendsViewModel,
                    friendSummaries = friendSummaries,
                    duoState = duoState,
                    referralState = referralState,
                    weeklyProfileViews = 0,
                    hasPerfectWeek = hasPerfectWeek,
                    onNavigateToEditProfile = onNavigateToEditProfile,
                    onNavigateToSettings = onNavigateToSettings,
                    onNavigateToAchievements = onNavigateToAchievements,
                    onNavigateToMemberProfile = navigateToMemberProfile,
                    onNavigateToChallenges = {
                        innerNavController.navigate("challenges") {
                            popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onNavigateToSubscription = {
                        innerNavController.navigate("profile_subscription")
                    },
                    onOpenChallengeDetail = { challengeId ->
                        challengeViewModel.requestOpenChallengeDetail(challengeId)
                        innerNavController.navigate("challenges") {
                            popUpTo(innerNavController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
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
                    onBack = { innerNavController.popBackStack() },
                    onShowNotification = { msg, isError, icon -> onShowNotification(msg, isError, icon) },
                    canInviteToDuo = !duoState.blocksNewDuoInvite(),
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

    when (val event = celebrationEvent) {
        is GoalCelebration.Milestone -> {
            val goal by goalsViewModel.goalById(event.goalId).collectAsState(initial = null)
            val milestones by goalsViewModel.milestonesForGoal(event.goalId).collectAsState(initial = emptyList())
            val milestone = milestones.find { it.id == event.milestoneId }
            if (goal != null && milestone != null) {
                MilestoneCelebrationScreen(
                    goal = goal!!,
                    milestone = milestone,
                    onDismiss = { celebrationEvent = null },
                    onShare = { celebrationEvent = null },
                )
            }
        }
        is GoalCelebration.Completed -> {
            val goal by goalsViewModel.goalById(event.goalId).collectAsState(initial = null)
            goal?.let { g ->
                GoalCompletionScreen(
                    goal = g,
                    onArchive = {
                        goalsViewModel.archiveGoal(g.id)
                        celebrationEvent = null
                    },
                    onSetNewGoal = {
                        celebrationEvent = null
                        innerNavController.navigate("goals/create") { launchSingleTop = true }
                    },
                    onShare = {
                        onShowNotification("Share your win — invite friends with your link", false, null)
                    },
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

    ReferralShareSheet(
        visible = showReferralInvite,
        state = referralState,
        onDismiss = { showReferralInvite = false },
        onCopiedAck = {
            onShowNotification("Invite link copied — paste anywhere", false, null)
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
    }
}
