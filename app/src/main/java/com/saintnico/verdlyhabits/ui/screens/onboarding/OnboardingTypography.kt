package com.saintnico.verdlyhabits.ui.screens.onboarding

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.R

private val googleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

private val dmSerifFamily = androidx.compose.ui.text.font.FontFamily(
    Font(
        googleFont = GoogleFont("DM Serif Display"),
        fontProvider = googleFontProvider,
        weight = FontWeight.Normal
    )
)

private val dmSansFamily = androidx.compose.ui.text.font.FontFamily(
    Font(
        googleFont = GoogleFont("DM Sans"),
        fontProvider = googleFontProvider,
        weight = FontWeight.Normal
    ),
    Font(
        googleFont = GoogleFont("DM Sans"),
        fontProvider = googleFontProvider,
        weight = FontWeight.Medium
    ),
    Font(
        googleFont = GoogleFont("DM Sans"),
        fontProvider = googleFontProvider,
        weight = FontWeight.SemiBold
    ),
    Font(
        googleFont = GoogleFont("DM Sans"),
        fontProvider = googleFontProvider,
        weight = FontWeight.Bold
    )
)

/**
 * Onboarding typography (DM Serif Display + DM Sans) per premium wellness spec.
 */
@Composable
fun rememberVerdlyTypography(): Typography = remember {
    Typography().copy(
        displayLarge = TextStyle(
            fontFamily = dmSerifFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 40.sp,
            lineHeight = 46.sp,
            letterSpacing = (-1).sp
        ),
        headlineMedium = TextStyle(
            fontFamily = dmSerifFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 28.sp,
            lineHeight = 34.sp,
            letterSpacing = (-0.5).sp
        ),
        bodyLarge = TextStyle(
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 16.sp,
            lineHeight = 26.sp,
            letterSpacing = 0.sp
        ),
        labelSmall = TextStyle(
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 16.sp,
            letterSpacing = 2.sp
        )
    )
}
