package com.oruncak.lockin

import android.Manifest
import android.app.AlarmManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
                LockInApp(repo = repo, onAlarmsChanged = {
                    AlarmScheduler.scheduleAll(this, repo.alarms.value, repo.settings.value.warnBeforeMinutes)
                })
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
            modifier = androidx.compose.ui.Modifier.padding(padding)
        ) {
            composable("overview") { OverviewScreen(repo) }
            composable("alarms") { AlarmsScreen(repo, onAlarmsChanged) }
            composable("streaks") { StreaksScreen(repo) }
            composable("settings") { SettingsScreen(repo, onAlarmsChanged) }
        }
    }
}
