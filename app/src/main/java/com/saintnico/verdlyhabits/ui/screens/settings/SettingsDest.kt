package com.saintnico.verdlyhabits.ui.screens.settings

/** Nested Settings destinations. Depth 0 = hub, depth 1 = one of these. */
object SettingsDest {
    const val Root = "settings_root"
    const val Notifications = "settings_notifications"
    const val Appearance = "settings_appearance"
    const val HabitsReminders = "settings_habits"
    const val FocusWidgets = "settings_focus"
    const val SocialPrivacy = "settings_social"
    const val Activity = "settings_activity"
    const val DuoArena = "settings_duo"
    const val SubscriptionCoins = "settings_subscription"
    const val DataStorage = "settings_data"
    const val HelpLegal = "settings_help"
    const val AccountActions = "settings_account"

    fun fromSectionArg(section: String?): String = when (section?.lowercase()) {
        "notifications" -> Notifications
        "appearance" -> Appearance
        "habits" -> HabitsReminders
        "focus" -> FocusWidgets
        "social" -> SocialPrivacy
        "activity" -> Activity
        "duo" -> DuoArena
        "subscription" -> SubscriptionCoins
        "data" -> DataStorage
        "help" -> HelpLegal
        "account" -> AccountActions
        else -> Root
    }
}
