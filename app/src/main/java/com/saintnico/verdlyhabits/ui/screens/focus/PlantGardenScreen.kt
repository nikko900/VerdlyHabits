package com.saintnico.verdlyhabits.ui.screens.focus

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saintnico.verdlyhabits.data.local.AppDatabase
import com.saintnico.verdlyhabits.data.local.focus.FocusSessionEntity
import com.saintnico.verdlyhabits.ui.theme.dmSansFamily
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantGardenScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).focusSessionDao() }
    var sessions by remember { mutableStateOf<List<FocusSessionEntity>>(emptyList()) }

    LaunchedEffect(Unit) {
        dao.observeAll().collectLatest { sessions = it }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A100D))
            .padding(16.dp)
    ) {
        Text(
            "Focus garden",
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = Color(0xFFD8F3DC)
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Past sessions",
            fontFamily = dmSansFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Color(0xFF74C69D).copy(alpha = 0.8f)
        )
        Spacer(Modifier.height(16.dp))

        if (sessions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Canvas(modifier = Modifier.height(120.dp).fillMaxWidth(0.6f)) {
                        drawGrowingPlant(
                            progress = 0.08f,
                            isWilting = false,
                            wiltAmount = 0f,
                            width = size.width,
                            height = size.height
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Your garden is waiting.",
                        fontFamily = dmSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = Color(0xFFB7E4C7)
                    )
                    Text(
                        "Complete your first focus session to grow your first plant.",
                        fontFamily = dmSansFamily,
                        fontSize = 14.sp,
                        color = Color(0xFF74C69D).copy(alpha = 0.75f),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(sessions, key = { it.id }) { session ->
                    GardenCell(session = session)
                }
            }
        }

        Spacer(Modifier.weight(1f))
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F))
        ) {
            Text("Back", fontFamily = dmSansFamily, color = Color.White)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GardenCell(session: FocusSessionEntity) {
    var showDetail by remember { mutableStateOf(false) }
    val tint = sessionDurationTint(session.durationMinutes)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDetail = true },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
        ) {
            drawGrowingPlant(
                progress = 1f,
                isWilting = false,
                wiltAmount = 0f,
                width = size.width,
                height = size.height,
                colorTint = tint
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            formatSessionDate(session.completedAt),
            fontFamily = dmSansFamily,
            fontSize = 11.sp,
            color = Color(0xFF74C69D).copy(alpha = 0.85f)
        )
    }
    if (showDetail) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showDetail = false },
            sheetState = sheetState,
            containerColor = Color(0xFF1A2E22)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    "Session",
                    fontFamily = dmSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFFD8F3DC)
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Date: ${formatSessionDate(session.completedAt)}",
                    fontFamily = dmSansFamily,
                    color = Color(0xFF74C69D)
                )
                Text(
                    "Duration: ${session.durationMinutes} min",
                    fontFamily = dmSansFamily,
                    color = Color(0xFF74C69D)
                )
                Text(
                    "XP: ${session.xpEarned}",
                    fontFamily = dmSansFamily,
                    color = Color(0xFF74C69D)
                )
                Text(
                    "Habit id: ${session.habitId ?: "None"}",
                    fontFamily = dmSansFamily,
                    color = Color(0xFF74C69D)
                )
                Spacer(Modifier.height(24.dp))
                Button(onClick = { showDetail = false }) { Text("Close") }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

private fun sessionDurationTint(minutes: Int): Color {
    val t = (minutes / 60f).coerceIn(0f, 1f)
    return lerp(Color(0xFF95D5B2), Color(0xFF1B4332), t)
}

private fun formatSessionDate(epoch: Long): String =
    SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(epoch))
