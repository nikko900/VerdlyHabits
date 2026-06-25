package com.saintnico.verdlyhabits.navigation

sealed class NotificationLaunch {
    data object WeeklyCheckIn : NotificationLaunch()
    data class GoalDetail(val goalId: String) : NotificationLaunch()
    data object EditProfile : NotificationLaunch()
    data object Inbox : NotificationLaunch()
}

fun android.content.Intent.readNotificationLaunch(): NotificationLaunch? = when {
    getBooleanExtra("open_notifications", false) -> NotificationLaunch.Inbox
    getBooleanExtra("open_weekly_check_in", false) -> NotificationLaunch.WeeklyCheckIn
    getBooleanExtra("open_edit_profile", false) -> NotificationLaunch.EditProfile
    !getStringExtra("open_goal_detail").isNullOrBlank() ->
        NotificationLaunch.GoalDetail(getStringExtra("open_goal_detail")!!)
    else -> null
}
