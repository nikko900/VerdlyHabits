package com.saintnico.verdlyhabits.ui.screens.goals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily

@Composable
fun StillClearChip(
    confirmedToday: Boolean,
    accent: Color,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (confirmedToday) {
        Row(
            modifier = modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Clear today",
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = if (compact) 13.sp else 15.sp,
                color = accent,
            )
        }
        return
    }
    Button(
        onClick = onConfirm,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = accent),
        contentPadding = PaddingValues(vertical = if (compact) 10.dp else 14.dp),
    ) {
        Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            "Still clear today?",
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = if (compact) 14.sp else 16.sp,
        )
    }
}
