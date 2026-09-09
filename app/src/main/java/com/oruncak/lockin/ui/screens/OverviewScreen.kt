package com.oruncak.lockin.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oruncak.lockin.data.AlarmItem
import com.oruncak.lockin.data.Repository
import com.oruncak.lockin.ui.theme.Amber
import com.oruncak.lockin.ui.theme.AmberDim
import java.util.Calendar

@Composable
fun OverviewScreen(repo: Repository) {
    val alarms by repo.alarms.collectAsState()
    val lockEngaged by remember { derivedStateOf { repo.lockEngaged } }
    val enabled = alarms.filter { it.enabled }.sortedBy { it.hour * 60 + it.minute }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("System Enforcer", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                AssistChip(
                    onClick = {},
                    label = { Text(if (lockEngaged) "Locked" else "Unlocked") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (lockEngaged)
                            MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                    )
                )
            }
        }
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(
                    Modifier.padding(18.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("CURRENT STREAK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text("18 days", fontSize = 32.sp, fontWeight = FontWeight.Bold)
                        Text("Next lock sequence · ${nextLabel(enabled)}", fontSize = 12.sp)
                    }
                    Box(
                        Modifier.size(44.dp).background(AmberDim, CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Icon(Icons.Filled.Bolt, contentDescription = null, tint = Amber) }
                }
            }
        }
        item { Text("TODAY'S LOCK SEQUENCE", fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp)) }
        items(enabled) { alarm -> TaskRow(alarm) }
    }
}

@Composable
private fun TaskRow(alarm: AlarmItem) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Column(Modifier.weight(1f)) {
                Text(alarm.label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(
                    if (alarm.allApps) "Restricts all apps" else "Restricts ${alarm.restrictedPackages.size} app(s)",
                    fontSize = 12.sp
                )
            }
            Text("%02d:%02d".format(alarm.hour, alarm.minute), fontWeight = FontWeight.Bold)
        }
    }
}

private fun nextLabel(alarms: List<AlarmItem>): String {
    if (alarms.isEmpty()) return "no alarms set"
    val now = Calendar.getInstance()
    val nowMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
    val next = alarms.firstOrNull { it.hour * 60 + it.minute > nowMinutes } ?: alarms.first()
    return "%02d:%02d".format(next.hour, next.minute)
}
