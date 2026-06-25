package com.saintnico.verdlyhabits.ui.screens.focus

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp as lerpFloat
import kotlin.math.cos
import kotlin.math.sin

/** Organic growing plant — shared by Focus session + garden grid. */
fun DrawScope.drawGrowingPlant(
    progress: Float,
    isWilting: Boolean,
    wiltAmount: Float,
    width: Float = size.width,
    height: Float = size.height,
    colorTint: Color? = null
) {
    val cx = width / 2f
    val cy = height
    val p = progress.coerceIn(0f, 1f)
    val wilt = if (isWilting) wiltAmount.coerceIn(0f, 1f) else 0f

    val baseGreen = Color(0xFF1B4332)
    val tipGreen = Color(0xFF52B788)
    val leafA = Color(0xFF40916C)
    val leafB = Color(0xFF52B788)
    val leafC = Color(0xFF74C69D)
    val soilDark = Color(0xFF3D2B1F)
    val soilLight = Color(0xFF5D4037)
    val wiltBrown = Color(0xFF795548)

    fun tint(c: Color): Color {
        var out = colorTint?.let { t -> lerp(c, t, 0.35f) } ?: c
        if (wilt > 0f) out = lerp(out, wiltBrown, wilt * 0.85f)
        return out
    }

    val stemH = height * 0.65f * p.coerceAtLeast(0.05f)

    val soilBrush = Brush.horizontalGradient(listOf(tint(soilDark), tint(soilLight)))
    drawRoundRect(
        brush = soilBrush,
        topLeft = Offset(cx - 42.dp.toPx(), cy - 10.dp.toPx()),
        size = Size(84.dp.toPx(), 18.dp.toPx()),
        cornerRadius = CornerRadius(10.dp.toPx(), 10.dp.toPx())
    )
    drawCircle(tint(soilDark.copy(alpha = 0.8f)), 3.dp.toPx(), center = Offset(cx - 28.dp.toPx(), cy - 4.dp.toPx()))
    drawCircle(tint(soilLight.copy(alpha = 0.7f)), 2.5f.dp.toPx(), center = Offset(cx + 30.dp.toPx(), cy - 3.dp.toPx()))
    drawCircle(tint(soilDark.copy(alpha = 0.6f)), 2f.dp.toPx(), center = Offset(cx + 18.dp.toPx(), cy - 2.dp.toPx()))

    val stemPath = Path().apply {
        val tipX = cx + sin(wilt * 0.35f) * 18f.dp.toPx()
        val tipY = cy - stemH
        moveTo(cx, cy - 8.dp.toPx())
        cubicTo(
            cx - 6.dp.toPx() + wilt * 10f,
            cy - stemH * 0.35f,
            cx + 8.dp.toPx() - wilt * 14f,
            cy - stemH * 0.72f,
            tipX,
            tipY
        )
    }
    drawPath(
        path = stemPath,
        brush = Brush.verticalGradient(
            colors = listOf(tint(baseGreen), tint(lerp(tipGreen, wiltBrown, wilt)))
        ),
        style = Stroke(
            width = lerpFloat(6.dp.toPx(), 3.dp.toPx(), p),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    )

    fun drawLeaf(center: Offset, dir: Float, scale: Float, phaseGate: Float) {
        if (p < phaseGate) return
        val ls = ((p - phaseGate) / (1f - phaseGate)).coerceIn(0f, 1f) * scale
        val droop = wilt * 0.75f
        val angle = dir * (1.1f - droop) + droop * 0.9f
        val w = 26.dp.toPx() * ls
        val h = 14.dp.toPx() * ls
        val leaf = Path().apply {
            val ox = cos(angle) * w * 0.4f
            val oy = sin(angle) * h * 0.35f
            moveTo(center.x, center.y)
            quadraticBezierTo(center.x - ox, center.y - oy - h * 0.5f, center.x - cos(angle) * w, center.y - sin(angle) * h)
            quadraticBezierTo(center.x + ox * 0.3f, center.y - oy * 0.2f, center.x, center.y)
            close()
        }
        drawPath(path = leaf, color = tint(lerp(leafA, leafB, ls)))
        drawLine(
            color = tint(leafC.copy(alpha = 0.45f)),
            start = center,
            end = Offset(center.x - cos(angle) * w * 0.55f, center.y - sin(angle) * h * 0.55f),
            strokeWidth = 1.dp.toPx()
        )
    }

    val stemBaseY = cy - 8.dp.toPx()
    drawLeaf(Offset(cx - 6.dp.toPx(), stemBaseY - stemH * 0.38f), -2.2f, 1f, 0.25f)
    drawLeaf(Offset(cx + 8.dp.toPx(), stemBaseY - stemH * 0.52f), 1.1f, 0.95f, 0.38f)
    drawLeaf(Offset(cx - 4.dp.toPx(), stemBaseY - stemH * 0.66f), -1.4f, 0.9f, 0.52f)

    if (p > 0.85f) {
        val cs = ((p - 0.85f) / 0.15f).coerceIn(0f, 1f)
        val top = Offset(cx + sin(wilt * 0.4f) * 12f.dp.toPx(), stemBaseY - stemH - 8.dp.toPx() * cs)
        val crownColors = listOf(leafA, leafB, leafC, leafA.copy(alpha = 0.85f), leafB.copy(alpha = 0.9f))
        repeat(6) { i ->
            val ang = (i / 6f) * 6.28f
            val r = 10.dp.toPx() * cs
            val c = Offset(top.x + cos(ang) * 14f.dp.toPx() * cs, top.y + sin(ang) * 10f.dp.toPx() * cs)
            val cp = Path().apply {
                moveTo(c.x, c.y)
                quadraticBezierTo(c.x + cos(ang + 0.4f) * r, c.y - r, c.x + cos(ang) * r * 1.2f, c.y + sin(ang) * r * 0.4f)
                quadraticBezierTo(c.x - cos(ang) * r * 0.4f, c.y + r * 0.2f, c.x, c.y)
                close()
            }
            drawPath(path = cp, color = tint(crownColors[i % crownColors.size]))
        }
    }
}
