package com.saintnico.verdlyhabits.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * Animates an integer from 0 to [targetValue] using a Spring physics spec.
 *
 * The animation springs in with a low-stiffness, medium-bounce feel — giving
 * stat numbers that satisfying premium "land" effect.
 *
 * @param targetValue The number to animate towards
 * @param suffix      Optional suffix like "%" or " days"
 * @param prefix      Optional prefix like "+" or "$"
 * @param fontSize    Text size — meant to be large (52–72.sp for hero stats)
 * @param fontWeight  Defaults to Black for maximum impact
 * @param color       Text color — defaults to onBackground
 */
@Composable
fun CountUpText(
    targetValue: Int,
    modifier: Modifier = Modifier,
    suffix: String = "",
    prefix: String = "",
    fontSize: TextUnit = 56.sp,
    fontWeight: FontWeight = FontWeight.Black,
    color: Color = MaterialTheme.colorScheme.onBackground,
    textAlign: TextAlign = TextAlign.Start
) {
    val animatable = remember { Animatable(0f) }
    var displayValue by remember { mutableIntStateOf(0) }

    LaunchedEffect(targetValue) {
        animatable.snapTo(0f)
        animatable.animateTo(
            targetValue = targetValue.toFloat(),
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    // Update display each frame
    displayValue = animatable.value.toInt()

    Text(
        text = "$prefix$displayValue$suffix",
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        textAlign = textAlign,
        modifier = modifier,
        letterSpacing = (-1).sp,
        lineHeight = fontSize * 1.1f
    )
}

/**
 * Variant for Float values (e.g., completion percentages shown as "87.5%").
 */
@Composable
fun CountUpFloat(
    targetValue: Float,
    modifier: Modifier = Modifier,
    decimalPlaces: Int = 0,
    suffix: String = "",
    prefix: String = "",
    fontSize: TextUnit = 56.sp,
    fontWeight: FontWeight = FontWeight.Black,
    color: Color = MaterialTheme.colorScheme.onBackground,
    textAlign: TextAlign = TextAlign.Start
) {
    val animatable = remember { Animatable(0f) }

    LaunchedEffect(targetValue) {
        animatable.snapTo(0f)
        animatable.animateTo(
            targetValue = targetValue,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    val formatted = "%.${decimalPlaces}f".format(animatable.value)

    Text(
        text = "$prefix$formatted$suffix",
        fontSize = fontSize,
        fontWeight = fontWeight,
        color = color,
        textAlign = textAlign,
        modifier = modifier,
        letterSpacing = (-1).sp,
        lineHeight = fontSize * 1.1f
    )
}
