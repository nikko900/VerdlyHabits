package com.saintnico.verdlyhabits.ui.components.social

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.model.UserReportDoc
import com.saintnico.verdlyhabits.data.model.UserReportOutcome
import com.saintnico.verdlyhabits.data.model.UserReportStatus
import com.saintnico.verdlyhabits.data.remote.firestore.MyReportsSnapshot
import com.saintnico.verdlyhabits.data.remote.firestore.UserReportRepository
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import com.saintnico.verdlyhabits.ui.theme.frauncesFamily
import java.text.DateFormat
import java.util.Date
import com.saintnico.verdlyhabits.ui.components.VerdlyLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyReportsSheet(onDismiss: () -> Unit) {
    val repository = remember { UserReportRepository() }
    val snapshot by repository.observeMyReports().collectAsState(initial = MyReportsSnapshot(loaded = false))
    val context = LocalContext.current

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
                "We’ll update each report as we look into it. Received → In review → Closed, with what we found.",
                fontFamily = dmSansFamily,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f),
            )
            Spacer(Modifier.height(16.dp))

            when {
                !snapshot.loaded && snapshot.error == null -> Row(
                    Modifier.fillMaxWidth().padding(vertical = 24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    VerdlyLoadingIndicator(size = 28.dp)
                }
                snapshot.error != null -> Column(Modifier.padding(vertical = 12.dp)) {
                    Text(
                        snapshot.error.orEmpty(),
                        fontFamily = dmSansFamily,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error,
                    )
                    snapshot.indexUrl?.let { url ->
                        Spacer(Modifier.height(8.dp))
                        TextButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                }
                            },
                        ) {
                            Text("Open index setup", fontFamily = dmSansFamily, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                snapshot.reports.isEmpty() -> Text(
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
                    snapshot.reports.forEach { report ->
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
    val status = UserReportStatus.from(report.status)
    val outcome = UserReportOutcome.from(report.outcome)
    val whenLabel = formatReportTime(report.updatedAt ?: report.createdAt)
    val statusColor = when (status) {
        UserReportStatus.Pending -> Color(0xFF5C6BC0)
        UserReportStatus.Reviewing -> Color(0xFFE08A2E)
        UserReportStatus.Resolved -> Color(0xFF52B788)
    }
    val outcomeColor = when (outcome) {
        UserReportOutcome.Warned -> Color(0xFFE08A2E)
        UserReportOutcome.Banned -> Color(0xFFE57373)
        UserReportOutcome.RemovedContent -> Color(0xFF5C6BC0)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    }
    val findingText = when {
        report.resolutionNote.isNotBlank() -> report.resolutionNote
        status == UserReportStatus.Resolved && outcome != UserReportOutcome.None -> outcome.reporterMessage
        status == UserReportStatus.Reviewing ->
            "Our team is looking into this. We’ll update you here when we close it."
        else ->
            "We received your report and will review it soon."
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "@$handle",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                StatusChip(label = status.label, color = statusColor)
            }
            Spacer(Modifier.height(4.dp))
            Text(
                report.reasonLabel,
                fontFamily = dmSansFamily,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
            )
            if (status == UserReportStatus.Resolved && outcome != UserReportOutcome.None) {
                Spacer(Modifier.height(8.dp))
                StatusChip(label = outcome.label, color = outcomeColor)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                findingText,
                fontFamily = dmSansFamily,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            if (report.details.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    report.details,
                    fontFamily = dmSansFamily,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    maxLines = 2,
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

@Composable
private fun StatusChip(label: String, color: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            label,
            fontFamily = dmSansFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = color,
        )
    }
}

private fun formatReportTime(timestamp: com.google.firebase.Timestamp?): String {
    if (timestamp == null) return ""
    return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
        .format(Date(timestamp.seconds * 1000))
}
