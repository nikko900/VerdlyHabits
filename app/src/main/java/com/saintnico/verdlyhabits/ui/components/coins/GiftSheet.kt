@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.saintnico.verdlyhabits.ui.components.coins

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material3.Icon
import com.saintnico.verdlyhabits.R
import com.saintnico.verdlyhabits.ui.components.VerdlyActionLottie
import com.saintnico.verdlyhabits.ui.components.VerdlySendingIndicator
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.saintnico.verdlyhabits.data.model.CoinEconomy

@Composable
fun GiftSupportSheet(
    receiverName: String,
    balance: Int,
    isPremium: Boolean,
    onSend: (amount: Int, message: String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var amount by remember { mutableIntStateOf(CoinEconomy.GIFT_QUICK_PICKS[1]) }
    var message by remember { mutableStateOf("") }
    var sending by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                VerdlyActionLottie(
                    rawRes = R.raw.gift_morph_open,
                    size = 36.dp,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Support $receiverName",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                "Send encouragement coins. They stay in-app and can't be cashed out.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            )
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CoinEconomy.GIFT_QUICK_PICKS.forEach { pick ->
                    val selected = amount == pick
                    val affordable = pick <= balance
                    Surface(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(enabled = affordable) { amount = pick },
                        shape = RoundedCornerShape(50),
                        color = if (selected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        ),
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            CoinIcon(size = 14.dp, premium = isPremium)
                            Spacer(Modifier.width(4.dp))
                            Text("$pick", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Note (optional)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            )
            Spacer(Modifier.height(6.dp))
            BasicTextField(
                value = message,
                onValueChange = { message = it.take(CoinEconomy.GIFT_MESSAGE_MAX_CHARS) },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(12.dp),
                decorationBox = { inner ->
                    if (message.isEmpty()) {
                        Text(
                            "Keep going…",
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.35f),
                        )
                    }
                    inner()
                },
            )
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    if (sending) return@Button
                    sending = true
                    onSend(amount, message.ifBlank { null })
                },
                enabled = !sending && amount <= balance && amount > 0,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(50),
            ) {
                if (sending) {
                    VerdlySendingIndicator(size = 22.dp)
                } else {
                    Text("Send $amount coins", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun SupportIconButton(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Icon(
        Icons.Rounded.VolunteerActivism,
        contentDescription = contentDescription,
        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
    )
}
