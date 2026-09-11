package com.oruncak.lockin.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oruncak.lockin.NotificationHelper
import com.oruncak.lockin.data.AlarmItem
import com.oruncak.lockin.data.Repository
import com.oruncak.lockin.util.isAccessibilityServiceEnabled
import com.oruncak.lockin.util.isIgnoringBatteryOptimizations

@Composable
fun SettingsScreen(repo: Repository, onSettingsChanged: () -> Unit) {
    val context = LocalContext.current
    val settings by repo.settings.collectAsState()
    val account by repo.account.collectAsState()
    var showAuth by remember { mutableStateOf(false) }
    var showExemptPicker by remember { mutableStateOf(false) }
    var showTestAppPicker by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().padding(20.dp).verticalScroll(rememberScrollState())
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            AssistChip(onClick = {}, label = { Text("Secure mode") })
        }
        Spacer(Modifier.height(16.dp))

        // Account
        Card(Modifier.fillMaxWidth().clickable { showAuth = true }) {
            Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(42.dp).clip(androidx.compose.foundation.shape.CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Text(if (account.signedIn) account.name.take(1).uppercase() else "?", fontWeight = FontWeight.Bold)
                }
                Column(Modifier.weight(1f)) {
                    Text(if (account.signedIn) account.name else "Not signed in", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        if (account.signedIn) "Signed in · via ${account.method}" else "Create an account to sync across devices",
                        fontSize = 11.sp
                    )
                }
                TextButton(onClick = { showAuth = true }) { Text(if (account.signedIn) "Manage" else "Sign in") }
            }
        }

        SectionLabel("Notifications")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 14.dp)) {
                SettingsRow("Pre-lock warning", "Sends a push notification before each scheduled lock") {
                    Switch(checked = true, onCheckedChange = {})
                }
                SettingsRow("Warn me before", "Minutes ahead of the lock time") {
                    EditableNumberField(
                        value = settings.warnBeforeMinutes, min = 1, max = 180,
                        display = { "$it min" }
                    ) { v ->
                        repo.updateSettings { it.copy(warnBeforeMinutes = v) }
                        onSettingsChanged()
                    }
                }
                SettingsRow("Second reminder", "Repeat the warning 1 minute before enforcement") {
                    Switch(checked = settings.secondReminder, onCheckedChange = { v ->
                        repo.updateSettings { it.copy(secondReminder = v) }
                    })
                }
            }
        }

        SectionLabel("Display preferences")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 14.dp)) {
                SettingsRow("Appearance", "Matte-black high-focus, or a bright interface") {
                    Row {
                        IconButton(onClick = { repo.updateSettings { it.copy(appearance = "dark") } }) {
                            Icon(Icons.Filled.DarkMode, contentDescription = "Dark mode",
                                tint = if (settings.appearance != "light") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { repo.updateSettings { it.copy(appearance = "light") } }) {
                            Icon(Icons.Filled.LightMode, contentDescription = "Light mode",
                                tint = if (settings.appearance == "light") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                SettingsRow("Dim on lock", "Aggressive brightness cut during countdown") {
                    Switch(checked = settings.dimOnLock, onCheckedChange = { v -> repo.updateSettings { it.copy(dimOnLock = v) } })
                }
            }
        }

        SectionLabel("System exemptions")
        Text("Exempted apps stay reachable even while a lock is active.", fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 14.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Phone", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("System", fontSize = 11.sp)
                }
                HorizontalDivider()
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 12.dp).clickable { showExemptPicker = true },
                    horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Select Apps…", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        if (settings.exemptPackages.isEmpty()) "None selected ›" else "${settings.exemptPackages.size} app(s) ›",
                        fontSize = 11.sp
                    )
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        var accessibilityOn by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
        var batteryExempt by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }
        val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                    accessibilityOn = isAccessibilityServiceEnabled(context)
                    batteryExempt = isIgnoringBatteryOptimizations(context)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        Card(
            Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (accessibilityOn) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.error.copy(alpha = 0.12f)
            )
        ) {
            Column(Modifier.padding(14.dp)) {
                Text(
                    if (accessibilityOn) "Locking is active" else "Locking is NOT active yet",
                    fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    color = if (accessibilityOn) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(4.dp))
                if (accessibilityOn) {
                    Text("LockIn's Accessibility service is enabled — restricted apps will be blocked during a lock.", fontSize = 12.sp)
                } else {
                    Text(
                        "Without this, apps never actually get blocked. On Android 13+, sideloaded apps hide this " +
                            "permission at first — tap below, then:\n" +
                            "1. Open LockIn's app info (⋮ menu → App info, or long-press the icon).\n" +
                            "2. Tap the ⋮ menu top-right → \"Allow restricted settings\".\n" +
                            "3. Then go to Accessibility → Downloaded apps → LockIn → turn it on.",
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Open Accessibility settings") }
            }
        }

        if (!batteryExempt) {
            Spacer(Modifier.height(6.dp))
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f))
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "Battery optimization may kill locking",
                        fontWeight = FontWeight.Bold, fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Some phones (Samsung, Xiaomi, OnePlus, and others) kill background apps to save " +
                            "battery, which can silently stop LockIn from re-locking after the first time. " +
                            "Exempt LockIn to keep it reliable.",
                        fontSize = 12.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            try {
                                context.startActivity(
                                    Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                        .setData(android.net.Uri.parse("package:${context.packageName}"))
                                )
                            } catch (e: Exception) {
                                context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Disable battery optimization for LockIn") }
                }
            }
        }

        SectionLabel("Debug")
        Text(
            "Triggers a lock right now scoped to just ONE app you pick, so you can test whether " +
                "the block reappears when you reopen that app after pressing Home — without " +
                "affecting every other app on your phone while we test. This adds a \"Test Lock\" " +
                "entry to your Alarms tab that never fires on its own — delete it there when done.",
            fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedButton(
            onClick = { showTestAppPicker = true },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Test Lock (choose one app)") }
        Spacer(Modifier.height(20.dp))

        SectionLabel("Habit grid colors")
        Text("Used on the Streaks tab: none/some/all of that day's tasks completed.", fontSize = 12.sp, modifier = Modifier.padding(bottom = 8.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ColorPickerRow("None done", settings.gridNoneColor) { c -> repo.updateSettings { it.copy(gridNoneColor = c) } }
                ColorPickerRow("Some done", settings.gridPartialColor) { c -> repo.updateSettings { it.copy(gridPartialColor = c) } }
                ColorPickerRow("All done", settings.gridAllColor) { c -> repo.updateSettings { it.copy(gridAllColor = c) } }
            }
        }
        Spacer(Modifier.height(40.dp))
    }

    if (showAuth) {
        AuthSheet(repo = repo, onDismiss = { showAuth = false })
    }
    if (showExemptPicker) {
        AppPickerSheet(
            title = "System Exemptions",
            subtitle = "Stay reachable even while a lock is active",
            apps = remember { repo.installedApps() },
            initiallySelected = settings.exemptPackages,
            onDismiss = { showExemptPicker = false },
            onSave = { picked ->
                repo.updateSettings { it.copy(exemptPackages = picked) }
                showExemptPicker = false
            }
        )
    }
    if (showTestAppPicker) {
        AppPickerSheet(
            title = "Test Lock",
            subtitle = "Pick the ONE app to test-block (e.g. Gallery)",
            apps = remember { repo.installedApps() },
            initiallySelected = emptySet(),
            onDismiss = { showTestAppPicker = false },
            onSave = { picked ->
                val pkg = picked.firstOrNull()
                if (pkg != null) {
                    // A throwaway alarm scoped to just this one package, so the accessibility
                    // service's "is this app restricted?" lookup (which reads from repo.alarms)
                    // treats ONLY this app as blocked — everything else, Home included, stays
                    // free, matching what we're specifically trying to verify right now.
                    val testAlarm = AlarmItem(
                        id = -999L, hour = 0, minute = 0, label = "Test Lock",
                        days = emptySet(), enabled = true, allApps = false,
                        restrictedPackages = setOf(pkg)
                    )
                    repo.saveAlarm(testAlarm)
                    repo.activeLockAlarmId = -999L
                    repo.activeLockLabel = "Test Lock"
                    repo.lockEngaged = true
                    NotificationHelper.showLocked(context, -999L, "Test Lock")
                }
                showTestAppPicker = false
            }
        )
    }
}

private val swatchPalette = listOf(
    0xFFE5584FL, 0xFFE8A33DL, 0xFFF2C94CL, 0xFF3FBF83L, 0xFF4E8DF0L,
    0xFF9B59B6L, 0xFFEC7FA9L, 0xFF5C6470L
)

@Composable
private fun ColorPickerRow(label: String, current: Long, onPick: (Long) -> Unit) {
    Column {
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            swatchPalette.forEach { hex ->
                val selected = hex == current
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(androidx.compose.ui.graphics.Color(hex))
                        .then(
                            if (selected) Modifier.border(
                                2.dp, MaterialTheme.colorScheme.onSurface, androidx.compose.foundation.shape.CircleShape
                            ) else Modifier
                        )
                        .clickable { onPick(hex) }
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(text.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
}

@Composable
private fun SettingsRow(title: String, subtitle: String, trailing: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.sp)
        }
        trailing()
    }
}
