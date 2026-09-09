package com.oruncak.lockin.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
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
import com.oruncak.lockin.data.Repository

@Composable
fun SettingsScreen(repo: Repository, onSettingsChanged: () -> Unit) {
    val context = LocalContext.current
    val settings by repo.settings.collectAsState()
    val account by repo.account.collectAsState()
    var showAuth by remember { mutableStateOf(false) }
    var showExemptPicker by remember { mutableStateOf(false) }
    var editingWarn by remember { mutableStateOf(false) }
    var warnDraft by remember(settings.warnBeforeMinutes) { mutableStateOf(settings.warnBeforeMinutes.toString()) }

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
                    if (editingWarn) {
                        OutlinedTextField(
                            value = warnDraft,
                            onValueChange = { warnDraft = it.filter { c -> c.isDigit() }.take(3) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(72.dp),
                            suffix = { Text("min") },
                            trailingIcon = null
                        )
                        LaunchedEffect(Unit) { /* commit happens on Done below */ }
                        IconButton(onClick = {
                            val v = warnDraft.toIntOrNull()?.coerceIn(1, 180) ?: settings.warnBeforeMinutes
                            repo.updateSettings { it.copy(warnBeforeMinutes = v) }
                            onSettingsChanged()
                            editingWarn = false
                        }) { Text("✓") }
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.clickable { editingWarn = true }
                        ) {
                            IconButton(onClick = {
                                val v = (settings.warnBeforeMinutes - 1).coerceAtLeast(1)
                                repo.updateSettings { it.copy(warnBeforeMinutes = v) }; onSettingsChanged()
                            }) { Text("–") }
                            Text("${settings.warnBeforeMinutes} min", fontWeight = FontWeight.Bold)
                            IconButton(onClick = {
                                val v = (settings.warnBeforeMinutes + 1).coerceAtMost(180)
                                repo.updateSettings { it.copy(warnBeforeMinutes = v) }; onSettingsChanged()
                            }) { Text("+") }
                        }
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
        OutlinedButton(
            onClick = {
                context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Enable LockIn in Accessibility settings") }
        Text(
            "Required once so LockIn can bring the lock screen back if a restricted app is opened.",
            fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
        )

        SectionLabel("Enforcement")
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 14.dp)) {
                SettingsRow("Strict lock mode", "Disables emergency bypass during enforcer hours") {
                    Switch(checked = settings.strictLockMode, onCheckedChange = { v -> repo.updateSettings { it.copy(strictLockMode = v) } })
                }
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
