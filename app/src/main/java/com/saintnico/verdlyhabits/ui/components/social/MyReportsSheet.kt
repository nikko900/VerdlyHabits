package com.saintnico.verdlyhabits.ui.components.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.model.UserReportDoc
import com.saintnico.verdlyhabits.data.model.UserReportStatus
import com.saintnico.verdlyhabits.data.remote.firestore.UserReportRepository
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import java.text.DateFormat
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsSheet(onDismiss: () -> Unit) {
    val repository = remember { UserReportRepository() }
    var reports by remember { mutableStateOf<List<UserReportDoc>?>(null) }

    LaunchedEffect(Unit) {
        reports = repository.fetchMyReports()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Your reports",
                    fontFamily = frauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp,
                )
                TextButton(onClick = onDismiss) {
                    Text("Close", fontFamily = dmSansFamily)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Reports you submitted to help keep Verdly safe. Our team reviews each one.",
                fontFamily = dmSansFamily,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
            )
            Spacer(Modifier.height(16.dp))

            val loadedReports = reports
            when {
                loadedReports == null -> Row(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.dp)
                }
                loadedReports.isEmpty() -> Text(
                    "No reports yet.",
                    fontFamily = dmSansFamily,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    modifier = Modifier.padding(vertical = 20.dp),
                )
                else -> Column(
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    loadedReports.forEach { report ->
                        MyReportRow(report)
                    }
                }
            }
        }
    }
}

@Composable
private fun MyReportRow(report: UserReportDoc) {
    val handle = report.reportedUsername.ifBlank { report.reportedDisplayName }.ifBlank { "Rival" }
    val whenLabel = formatReportTime(report.updatedAt ?: report.createdAt)
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                "@$handle",
                fontFamily = dmSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                report.reasonLabel,
                fontFamily = dmSansFamily,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                UserReportStatus.labelFor(report.status),
                fontFamily = dmSansFamily,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
            )
            if (report.details.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    report.details,
                    fontFamily = dmSansFamily,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (whenLabel.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    whenLabel,
                    fontFamily = dmSansFamily,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f),
                )
            }
        }
    }
}

private fun formatReportTime(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return ""
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(timestamp.seconds * 1000))
}
