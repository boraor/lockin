package com.oruncak.lockin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oruncak.lockin.data.DayStatus
import com.oruncak.lockin.data.Repository
import java.util.Calendar

@Composable
fun StreaksScreen(repo: Repository) {
    val alarms by repo.alarms.collectAsState()
    val log by repo.completionLog.collectAsState()
    val settings by repo.settings.collectAsState()

    // Recompute whenever alarms or the log change.
    val days = remember(alarms, log) {
        val cal = Calendar.getInstance()
        (0 until 28).map { i ->
            val key = repo.todayKey(cal)
            val status = repo.dayStatus(key)
            cal.add(Calendar.DAY_OF_YEAR, -1)
            status
        }.reversed()
    }
    val completionRate = remember(alarms, log) { repo.completionRateLast30Days() }
    val tasksTotal = remember(log) { repo.tasksCompletedTotal() }
    val streak = remember(alarms, log) { repo.currentStreak() }

    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("Analytics", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatTile("Completion (30d)", "$completionRate%", Modifier.weight(1f))
            StatTile("Tasks done", "$tasksTotal", Modifier.weight(1f))
        }
        Spacer(Modifier.height(20.dp))
        Text("HABIT GRID — LAST 4 WEEKS", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth()) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier.padding(12.dp).height(140.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                userScrollEnabled = false
            ) {
                items(days) { status ->
                    val color = when (status) {
                        DayStatus.EMPTY -> MaterialTheme.colorScheme.surfaceVariant
                        DayStatus.NONE_DONE -> Color(settings.gridNoneColor)
                        DayStatus.PARTIAL -> Color(settings.gridPartialColor)
                        DayStatus.ALL_DONE -> Color(settings.gridAllColor)
                    }
                    Box(Modifier.aspectRatio(1f).background(color, RoundedCornerShape(5.dp)))
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Legend(settings.gridNoneColor, settings.gridPartialColor, settings.gridAllColor)
        Spacer(Modifier.height(20.dp))
        Text("STREAK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("$streak day${if (streak == 1) "" else "s"} — every scheduled task completed", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text("Resets the moment a scheduled day is missed.", fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun Legend(none: Long, partial: Long, all: Long) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        LegendDot(Color(none), "None done")
        LegendDot(Color(partial), "Some done")
        LegendDot(Color(all), "All done")
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(3.dp)))
        Text(label, fontSize = 11.sp)
    }
}

@Composable
private fun StatTile(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}
