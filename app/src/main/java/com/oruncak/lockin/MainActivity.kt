package com.oruncak.lockin

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.oruncak.lockin.data.Repository
import com.oruncak.lockin.ui.screens.AlarmsScreen
import com.oruncak.lockin.ui.screens.OverviewScreen
import com.oruncak.lockin.ui.screens.SettingsScreen
import com.oruncak.lockin.ui.screens.StreaksScreen
import com.oruncak.lockin.ui.theme.LockInTheme
import com.oruncak.lockin.util.isAccessibilityServiceEnabled
import com.oruncak.lockin.util.isIgnoringBatteryOptimizations

private data class Tab(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

private val tabs = listOf(
    Tab("overview", "Overview", Icons.Filled.Dashboard),
    Tab("alarms", "Alarms", Icons.Filled.Alarm),
    Tab("streaks", "Streaks", Icons.Filled.BarChart),
    Tab("settings", "Settings", Icons.Filled.Settings)
)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repo = Repository.get(this)

        val requestNotifPermission = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { /* granted or not — the pre-lock notification simply won't show if denied */ }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // (Re)schedule every enabled alarm on launch so edits take effect immediately.
        AlarmScheduler.scheduleAll(this, repo.alarms.value, repo.settings.value.warnBeforeMinutes)

        setContent {
            val settings by repo.settings.collectAsState()
            LockInTheme(appearance = settings.appearance) {
                RequireAccessibilityGate {
                    LockInApp(repo = repo, onAlarmsChanged = {
                        AlarmScheduler.scheduleAll(this, repo.alarms.value, repo.settings.value.warnBeforeMinutes)
                    })
                }
            }
        }
    }
}

/**
 * LockIn cannot actually block anything without its Accessibility service turned on, so the
 * rest of the app is unreachable until the user grants it — otherwise the alarms would
 * silently do nothing, which is worse than making the requirement explicit up front.
 */
@Composable
private fun RequireAccessibilityGate(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var granted by remember { mutableStateOf(isAccessibilityServiceEnabled(context)) }
    var batteryExempt by remember { mutableStateOf(isIgnoringBatteryOptimizations(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = isAccessibilityServiceEnabled(context)
                batteryExempt = isIgnoringBatteryOptimizations(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (granted) {
        content()
    } else {
        AccessibilityRequiredScreen(
            batteryExempt = batteryExempt,
            onOpenSettings = { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) },
            onOpenAppInfo = {
                context.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(android.net.Uri.parse("package:${context.packageName}"))
                )
            },
            onRequestBatteryExemption = {
                try {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                            .setData(android.net.Uri.parse("package:${context.packageName}"))
                    )
                } catch (e: Exception) {
                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                }
            }
        )
    }
}

@Composable
private fun AccessibilityRequiredScreen(
    batteryExempt: Boolean,
    onOpenSettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
    onRequestBatteryExemption: () -> Unit
) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Filled.Lock, contentDescription = null,
                modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(20.dp))
            Text(
                "LockIn needs one permission before it can lock anything",
                fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "Without Accessibility access, LockIn can schedule alarms but can never actually block a " +
                    "restricted app — so the app stays locked out itself until you turn this on.",
                fontSize = 13.sp, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(24.dp))
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("HOW TO ENABLE IT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "1. Tap \"Open app info\" below.\n" +
                            "2. Tap the ⋮ menu (top right) → \"Allow restricted settings\" " +
                            "— required once on Android 13+ for sideloaded apps.\n" +
                            "3. Tap \"Open Accessibility settings\", find LockIn under Downloaded apps, and turn it on.\n" +
                            "4. Come back here — this screen updates automatically.",
                        fontSize = 13.sp
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            OutlinedButton(onClick = onOpenAppInfo, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("Open app info")
            }
            Spacer(Modifier.height(10.dp))
            Button(onClick = onOpenSettings, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("Open Accessibility settings")
            }

            if (!batteryExempt) {
                Spacer(Modifier.height(24.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("RECOMMENDED: DISABLE BATTERY OPTIMIZATION", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Some phones (Samsung, Xiaomi, OnePlus, and others) kill background apps to " +
                                "save battery — this can silently stop LockIn from re-locking after the " +
                                "first time. Exempting LockIn avoids that.",
                            fontSize = 13.sp
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onRequestBatteryExemption, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Text("Disable battery optimization for LockIn")
                }
            }
        }
    }
}

@Composable
private fun LockInApp(repo: Repository, onAlarmsChanged: () -> Unit) {
    val navController = rememberNavController()
    Scaffold(
        bottomBar = {
            NavigationBar {
                val backStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = backStackEntry?.destination
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "overview",
            modifier = Modifier.padding(padding)
        ) {
            composable("overview") { OverviewScreen(repo) }
            composable("alarms") { AlarmsScreen(repo, onAlarmsChanged) }
            composable("streaks") { StreaksScreen(repo) }
            composable("settings") { SettingsScreen(repo, onAlarmsChanged) }
        }
    }
}
