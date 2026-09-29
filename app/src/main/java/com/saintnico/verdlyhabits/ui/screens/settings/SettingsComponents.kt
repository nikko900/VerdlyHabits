package com.saintnico.verdlyhabits.ui.screens.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.ui.theme.DangerRed

sealed class SettingsControl {
    data class Toggle(val checked: Boolean, val onCheckedChange: (Boolean) -> Unit) : SettingsControl()
    data object Chevron : SettingsControl()
    data class Value(val text: String) : SettingsControl()
    data object Destructive : SettingsControl()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPageScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(title, fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            content = content,
        )
    }
}

@Composable
fun SettingsSection(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!title.isNullOrBlank()) {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.5.sp,
            modifier = Modifier.padding(start = 4.dp, top = 8.dp, bottom = 8.dp),
        )
    } else {
        Spacer(Modifier.height(8.dp))
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(content = content)
    }
}

@Composable
fun SettingsRow(
    title: String,
    subtitle: String? = null,
    icon: ImageVector,
    iconTint: Color,
    control: SettingsControl,
    onClick: (() -> Unit)? = null,
    titleColor: Color = Color.Unspecified,
) {
    val resolvedTitle = if (titleColor != Color.Unspecified) titleColor
    else if (control is SettingsControl.Destructive) DangerRed
    else MaterialTheme.colorScheme.onBackground
    val clickable = when (control) {
        is SettingsControl.Toggle -> Modifier.clickable { control.onCheckedChange(!control.checked) }
        else -> if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(clickable)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconWell(icon = icon, tint = iconTint)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = resolvedTitle,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                )
            }
        }
        when (control) {
            is SettingsControl.Toggle -> {
                Switch(
                    checked = control.checked,
                    onCheckedChange = control.onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedThumbColor = Color.White,
                    ),
                )
            }
            is SettingsControl.Chevron -> {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                )
            }
            is SettingsControl.Value -> {
                Text(
                    text = control.text,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.45f),
                    modifier = Modifier.padding(end = 4.dp),
                )
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.3f),
                )
            }
            is SettingsControl.Destructive -> {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = DangerRed.copy(alpha = 0.45f),
                )
            }
        }
    }
}

@Composable
fun SettingsToggle(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    SettingsRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        iconTint = iconTint,
        control = SettingsControl.Toggle(checked, onCheckedChange),
    )
}

@Composable
fun SettingsSoonRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) {
                Toast.makeText(context, "Coming soon", Toast.LENGTH_SHORT).show()
            }
            .padding(horizontal = 20.dp, vertical = 16.dp)
            .alpha(0.45f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SettingsIconWell(icon = icon, tint = iconTint)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
        }
        SoonChip()
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 20.dp),
        thickness = 0.5.dp,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
    )
}

@Composable
private fun SettingsIconWell(icon: ImageVector, tint: Color) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(tint.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun SoonChip() {
    val sage = MaterialTheme.colorScheme.primary
    Text(
        text = "Soon",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = sage,
        modifier = Modifier
            .border(1.dp, sage.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
