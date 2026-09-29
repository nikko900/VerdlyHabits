package com.saintnico.verdlyhabits.ui.assets

import com.saintnico.verdlyhabits.R

/**
 * Central reference for Verdly marketing / empty-state / onboarding art.
 * Prefer these constants over scattering [R.drawable] / [R.raw] ids.
 */
object VerdlyAssetCatalog {

    // ── Hero illustrations (drawable-nodpi) ────────────────────────────────
    val IllPlantsCitti = R.drawable.ill_plants_citti
    val IllAbstractCitti = R.drawable.ill_abstract_citti
    val IllPeopleParty = R.drawable.ill_people_party
    val IllPeopleOffice = R.drawable.ill_people_office
    val IllPeopleReview = R.drawable.ill_people_review
    val IllPeopleHandshake = R.drawable.ill_people_handshake
    val IllPeopleWave = R.drawable.ill_people_wave
    val IllPeopleFloat = R.drawable.ill_people_float
    val IllPeopleMobile = R.drawable.ill_people_mobile

    // ── Reaction stickers (drawable-nodpi) ───────────────────────────────────
    val RxnAngry = R.drawable.rxn_angry
    val RxnAppreciate = R.drawable.rxn_appreciate
    val RxnAppreciate2 = R.drawable.rxn_appreciate2
    val RxnAwesome = R.drawable.rxn_awesome
    val RxnDone = R.drawable.rxn_done
    val RxnHeart = R.drawable.rxn_heart
    val RxnHeartEyes = R.drawable.rxn_heart_eyes
    val RxnHeartEyes2 = R.drawable.rxn_heart_eyes2
    val RxnHeartPink = R.drawable.rxn_heart_pink
    val RxnOk = R.drawable.rxn_ok
    val RxnOmg = R.drawable.rxn_omg
    val RxnPlease = R.drawable.rxn_please
    val RxnSad = R.drawable.rxn_sad
    val RxnSadEyes = R.drawable.rxn_sad_eyes
    val RxnSuspicious = R.drawable.rxn_suspicious
    val RxnThanks = R.drawable.rxn_thanks
    val RxnWow = R.drawable.rxn_wow
    val RxnYass = R.drawable.rxn_yass

    // ── Lottie (res/raw) ───────────────────────────────────────────────────
    val LottieStreak = R.raw.streak
    val LottieSending = R.raw.sending
    val LottieMedalFirst = R.raw.medal_first
    val LottieBadgeRibbon = R.raw.badge_ribbon
    val LottiePlantPotReveal = R.raw.plant_pot_reveal
    val LottieGrowingPlant = R.raw.growing_plant
    val LottieAccountCreated = R.raw.account_created
    val LottieHandshakeLoop = R.raw.handshake_loop
    val LottieHandsApplause = R.raw.hands_applause
    val LottieLoading = R.raw.loading
    val LottieGiftFree = R.raw.gift_free
    val LottieGiftMorph = R.raw.gift_morph_open
    val LottieCouponReveal = R.raw.coupon_reveal
    val LottieMagnifierSpin = R.raw.magnifier_spin
    val LottieGhostHappy = R.raw.ghost_happy
    val LottieHandVoting = R.raw.hand_voting
    val LottiePersonSkateboard = R.raw.person_skateboard
    val LottieHandCallMe = R.raw.hand_call_me
    val LottieWarningTriangle = R.raw.warning_triangle
    val LottieThumbUp = R.raw.thumb_up
    val LottieThumbDown = R.raw.thumb_down
    val LottieAvatarsMessagePlus = R.raw.avatars_message_plus
    val LottieAvatarsMessageRoll = R.raw.avatars_message_roll
    val LottieAvatarsMessageFlat = R.raw.avatars_message_flat
    val LottieLockDollar = R.raw.lock_dollar
    val LottieCoinsJump = R.raw.coins_jump
    val LottieEmojiShock = R.raw.emoji_shock
    val LottieEmojiShockColor = R.raw.emoji_shock_color
    val LottieEmojiSmile = R.raw.emoji_smile
    val LottieStarDynamic = R.raw.star_dynamic
    val LottieCameraFlash = R.raw.camera_flash
    val LottieHeartMorph = R.raw.heart_morph
    val LottieAttribution = R.raw.attribution
    val LottieBadgeRibbonSolid = R.raw.badge_ribbon_solid
    val LottieInbox = R.raw.inbox_solid
    val LottiePencil = R.raw.pencil_solid
    val LottieAvatarSolid = R.raw.avatar_solid
    val LottieSpinnerDots = R.raw.spinner_dots
    val LottieHomeOutline = R.raw.home_outline
    val LottieMenuOpenClose = R.raw.menu_open_close
    val LottieScaleRetro = R.raw.scale_retro
    val LottiePersonWalking = R.raw.person_walking
    val LottieAlarmPinch = R.raw.alarm_pinch
    val LottieAvatarDeveloper = R.raw.avatar_developer
    val LottieTrashBin = R.raw.trash_bin
    val LottiePersonPushups = R.raw.person_pushups
    val LottiePersonExit = R.raw.person_exit
    val LottieAvatarsChatting = R.raw.avatars_chatting
    val LottieAvatarsMagnet = R.raw.avatars_magnet
    val LottiePersonCycling = R.raw.person_cycling
    val LottiePiggyBank = R.raw.piggy_bank
    val LottieImageMountain = R.raw.image_mountain
    val LottieLinkReveal = R.raw.link_reveal
    val LottieEyeMorph = R.raw.eye_morph
    val LottieHoldToDelete = R.raw.hold_to_delete

    /** Onboarding slide 0 — habits / growth */
    val OnboardingSlide0Lottie = LottieGrowingPlant
    val OnboardingSlide0People = IllPlantsCitti

    /** Onboarding slide 1 — challenge friends */
    val OnboardingSlide1People = IllPeopleParty
    val OnboardingSlide1Lottie = LottieAvatarsChatting

    /** Onboarding slide 2 — accountability */
    val OnboardingSlide2People = IllPeopleHandshake
    val OnboardingSlide2Lottie = LottieHandshakeLoop

    /** Onboarding slide 3 — photo proof */
    val OnboardingSlide3People = IllPeopleMobile
    val OnboardingSlide3Lottie = LottieCameraFlash

    /** Onboarding slide 4 — join the grove */
    val OnboardingSlide4People = IllPeopleWave
    val OnboardingSlide4Lottie = LottieAvatarsMagnet

    @Deprecated("Use OnboardingSlide0Lottie", ReplaceWith("OnboardingSlide0Lottie"))
    val OnboardingSlide0 = LottieGrowingPlant
    @Deprecated("Use OnboardingSlide1People", ReplaceWith("OnboardingSlide1People"))
    val OnboardingSlide1 = IllPeopleParty
    @Deprecated("Use OnboardingSlide2People", ReplaceWith("OnboardingSlide2People"))
    val OnboardingSlide2 = IllPeopleHandshake

    /** Home empty state (no habits yet) */
    val HomeEmptyHero = LottiePlantPotReveal

    /** Friends grove empty state */
    val GroveEmptyHero = IllPeopleParty
    val GroveEmptyLottie = LottieAvatarsChatting

    /** Add habit final step motif */
    val AddHabitReadyMotif = IllPlantsCitti
    val AddHabitReadyLottie = LottiePlantPotReveal

    /** First habit planted celebration accent */
    val FirstHabitCelebrationLottie = LottieHandsApplause
    val FirstHabitCelebrationSticker = RxnDone

    val LoginHeroPeople = IllPeopleWave
    val SignUpHeroLottie = LottieGrowingPlant
    val SearchLottie = LottieMagnifierSpin
    val NudgeLottie = LottieHandCallMe
    val InboxEmptyLottie = LottieInbox
    val StreakLostLottie = LottieWarningTriangle
    val WelcomeBackPeople = IllPeopleFloat
    val PaywallLottie = LottieLockDollar
    val GiftHeroLottie = LottieGiftMorph
    val CoinsLottie = LottieCoinsJump
    val InviteLottie = LottieLinkReveal
    val ArenaEmptyPeople = IllPeopleOffice
}
