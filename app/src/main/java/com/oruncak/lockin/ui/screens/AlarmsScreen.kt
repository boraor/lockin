package com.oruncak.lockin.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oruncak.lockin.data.AlarmItem
import com.oruncak.lockin.data.Repository

private val dayLabels = listOf("M", "T", "W", "T", "F", "S", "S") // index 0..6 -> iso day 1..7

@Composable
fun AlarmsScreen(repo: Repository, onAlarmsChanged: () -> Unit) {
    val context = LocalContext.current
    val alarms by repo.alarms.collectAsState()
    var showEditor by remember { mutableStateOf<AlarmItem?>(null) }
    var showNew by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize().padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item { Text("Lock Alarms", fontSize = 24.sp, fontWeight = FontWeight.Bold) }
            items(alarms.sortedBy { it.hour * 60 + it.minute }) { alarm ->
                AlarmRow(
                    alarm = alarm,
                    onToggle = { repo.setAlarmEnabled(alarm.id, it); onAlarmsChanged() },
                    onClick = { showEditor = alarm }
                )
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
        FloatingActionButton(
            onClick = { showNew = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp)
        ) { Icon(Icons.Filled.Add, contentDescription = "New alarm") }
    }

    if (showNew) {
        AlarmEditorSheet(
            repo = repo,
            initial = null,
            onDismiss = { showNew = false },
            onSave = { alarm -> repo.saveAlarm(alarm); onAlarmsChanged(); showNew = false }
        )
    }
    showEditor?.let { alarm ->
        AlarmEditorSheet(
            repo = repo,
            initial = alarm,
            onDismiss = { showEditor = null },
            onSave = { updated -> repo.saveAlarm(updated); onAlarmsChanged(); showEditor = null },
            onDelete = { repo.deleteAlarm(alarm.id); onAlarmsChanged(); showEditor = null }
        )
    }
}

@Composable
private fun AlarmRow(alarm: AlarmItem, onToggle: (Boolean) -> Unit, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(14.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(Modifier.clickable(onClick = onClick)) {
                Text("%02d:%02d".format(alarm.hour, alarm.minute), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Column(Modifier.weight(1f).clickable(onClick = onClick)) {
                Text(alarm.label, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(daysSummary(alarm.days), fontSize = 11.sp)
            }
            Switch(checked = alarm.enabled, onCheckedChange = onToggle)
        }
    }
}

private fun daysSummary(days: Set<Int>): String {
    if (days.size == 7) return "Daily"
    return (1..7).filter { days.contains(it) }.joinToString(", ") { dayLabels[it - 1] }
}

@Composable
private fun AlarmEditorSheet(
    repo: Repository,
    initial: AlarmItem?,
    onDismiss: () -> Unit,
    onSave: (AlarmItem) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    var hour by remember { mutableIntStateOf(initial?.hour ?: 14) }
    var minute by remember { mutableIntStateOf(initial?.minute ?: 0) }
    var label by remember { mutableStateOf(initial?.label ?: "New Habit") }
    var days by remember { mutableStateOf(initial?.days ?: setOf(1, 2, 3, 4, 5)) }
    var allApps by remember { mutableStateOf(initial?.allApps ?: true) }
    var restricted by remember { mutableStateOf(initial?.restrictedPackages ?: emptySet()) }
    var showAppPicker by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onDismiss) { Text("Cancel") }
                Text(if (initial == null) "New Lock Alarm" else "Edit Alarm", fontWeight = FontWeight.Bold)
                TextButton(onClick = {
                    onSave(
                        AlarmItem(
                            id = initial?.id ?: System.currentTimeMillis(),
                            hour = hour, minute = minute, label = label, days = days,
                            enabled = initial?.enabled ?: true,
                            allApps = allApps, restrictedPackages = restricted
                        )
                    )
                }) { Text("Save") }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                EditableNumberField(hour, 0, 23, wrap = true, display = { "%02d".format(it) }) { hour = it }
                Text(":", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                EditableNumberField(minute, 0, 59, wrap = true, display = { "%02d".format(it) }) { minute = it }
            }
            Spacer(Modifier.height(16.dp))
            Text("TASK DESCRIPTION", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = label, onValueChange = { label = it }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(16.dp))
            Text("REPEAT DAYS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                (1..7).forEach { d ->
                    FilterChip(
                        selected = days.contains(d),
                        onClick = { days = if (days.contains(d)) days - d else days + d },
                        label = { Text(dayLabels[d - 1]) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("RESTRICT ACCESS SCOPE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("All apps")
                Switch(checked = allApps, onCheckedChange = { allApps = it })
            }
            Row(
                Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable(onClick = { showAppPicker = true }),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Select Apps…")
                Text(if (restricted.isEmpty()) "None selected" else "${restricted.size} app(s) ›")
            }
            if (onDelete != null) {
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDelete) { Text("Delete alarm", color = MaterialTheme.colorScheme.error) }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showAppPicker) {
        val context = LocalContext.current
        AppPickerSheet(
            title = "Select Apps",
            subtitle = "Locked while this alarm is active",
            apps = remember { repo.installedApps() },
            initiallySelected = restricted,
            onDismiss = { showAppPicker = false },
            onSave = { picked -> restricted = picked; allApps = false; showAppPicker = false }
        )
    }
}
