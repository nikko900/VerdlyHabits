package com.saintnico.verdlyhabits.ui.components.profilepremium

sealed class ProfileExplore {
    data object Xp : ProfileExplore()
    data object Streak : ProfileExplore()
    data object Challenges : ProfileExplore()
    data object Completions : ProfileExplore()
    data object Crew : ProfileExplore()
    data object Connections : ProfileExplore()
    data object Motivated : ProfileExplore()
    data object Wins : ProfileExplore()
    data object Duo : ProfileExplore()
    data object Invites : ProfileExplore()
}
