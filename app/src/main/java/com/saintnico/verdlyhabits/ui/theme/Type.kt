package com.saintnico.verdlyhabits.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
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

private val dmSerifFamily = FontFamily(
    Font(googleFont = GoogleFont("DM Serif Display"), fontProvider = googleFontProvider, weight = FontWeight.Normal)
)

val frauncesFamily = FontFamily(
    Font(googleFont = GoogleFont("Fraunces"), fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("Fraunces"), fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("Fraunces"), fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("Fraunces"), fontProvider = googleFontProvider, weight = FontWeight.Bold),
    Font(googleFont = GoogleFont("Fraunces"), fontProvider = googleFontProvider, weight = FontWeight.Normal, style = FontStyle.Italic),
    Font(googleFont = GoogleFont("Fraunces"), fontProvider = googleFontProvider, weight = FontWeight.SemiBold, style = FontStyle.Italic)
)

val dmSansFamily = FontFamily(
    Font(googleFont = GoogleFont("DM Sans"), fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("DM Sans"), fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("DM Sans"), fontProvider = googleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = GoogleFont("DM Sans"), fontProvider = googleFontProvider, weight = FontWeight.Bold),
    Font(googleFont = GoogleFont("DM Sans"), fontProvider = googleFontProvider, weight = FontWeight.ExtraBold)
)

/** Same mono stack as saintnico.site — labels, codes, streetwise accents. */
val jetBrainsMonoFamily = FontFamily(
    Font(googleFont = GoogleFont("JetBrains Mono"), fontProvider = googleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = GoogleFont("JetBrains Mono"), fontProvider = googleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = GoogleFont("JetBrains Mono"), fontProvider = googleFontProvider, weight = FontWeight.Bold),
)

/**
 * App-wide typography: Fraunces / DM Serif (headings) + DM Sans (body) + JetBrains Mono (accents).
 * Matches the saintnico.site editorial stack.
 */
object VerdlyTypography {
    val material: Typography by lazy {
        Typography().copy(
            displayLarge = TextStyle(
                fontFamily = dmSerifFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 57.sp,
                lineHeight = 64.sp,
                letterSpacing = (-0.25).sp
            ),
            displayMedium = TextStyle(
                fontFamily = dmSerifFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 45.sp,
                lineHeight = 52.sp,
                letterSpacing = 0.sp
            ),
            headlineLarge = TextStyle(
                fontFamily = dmSerifFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 32.sp,
                lineHeight = 40.sp,
                letterSpacing = (-0.3).sp
            ),
            headlineMedium = TextStyle(
                fontFamily = dmSerifFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 28.sp,
                lineHeight = 34.sp,
                letterSpacing = (-0.25).sp
            ),
            headlineSmall = TextStyle(
                fontFamily = dmSerifFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 24.sp,
                lineHeight = 30.sp,
                letterSpacing = (-0.2).sp
            ),
            titleLarge = TextStyle(
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                letterSpacing = 0.sp
            ),
            titleMedium = TextStyle(
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.15.sp
            ),
            bodyLarge = TextStyle(
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                letterSpacing = 0.25.sp
            ),
            bodyMedium = TextStyle(
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.2.sp
            ),
            bodySmall = TextStyle(
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.35.sp,
                fontStyle = FontStyle.Italic
            ),
            labelLarge = TextStyle(
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                letterSpacing = 0.1.sp
            ),
            labelMedium = TextStyle(
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                letterSpacing = 0.5.sp
            ),
            labelSmall = TextStyle(
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                letterSpacing = 1.2.sp
            )
        )
    }
}
