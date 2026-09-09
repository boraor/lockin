package com.oruncak.lockin

import android.app.KeyguardManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.oruncak.lockin.data.Repository
import com.oruncak.lockin.ui.theme.Amber
import com.oruncak.lockin.ui.theme.AmberDim
import com.oruncak.lockin.ui.theme.LockInTheme
import com.oruncak.lockin.ui.theme.Red
import kotlinx.coroutines.delay

/**
 * Full-screen activity shown either as the pre-lock warning (dismissable, counts down)
 * or as the enforced lock (blocks back, only leaves once the habit is marked complete).
 * Shown over the lock screen via setShowWhenLocked/setTurnScreenOn, the same mechanism
 * alarm-clock and incoming-call apps use.
 */
class LockActivity : ComponentActivity() {

    companion object {
        const val EXTRA_MODE = "extra_mode"
        const val EXTRA_LABEL = "extra_label"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val MODE_WARNING = "warning"
        const val MODE_LOCKED = "locked"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(KeyguardManager::class.java)
            km?.requestDismissKeyguard(this, null)
        }

        val mode = intent.getStringExtra(EXTRA_MODE) ?: MODE_WARNING
        val label = intent.getStringExtra(EXTRA_LABEL) ?: "Habit"
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
        val repo = Repository.get(this)

        setContent {
            val settings by repo.settings.collectAsState()
            LockInTheme(appearance = settings.appearance) {
                var currentMode by remember { mutableStateOf(mode) }
                Surface(modifier = Modifier.fillMaxSize()) {
                    if (currentMode == MODE_WARNING) {
                        WarningScreen(
                            label = label,
                            warnMinutes = settings.warnBeforeMinutes,
                            onComplete = { finishLock(alarmId) },
                            onSnooze = { finish() }
                        )
                    } else {
                        RestrictedScreen(
                            label = label,
                            strict = settings.strictLockMode,
                            onComplete = { finishLock(alarmId) }
                        )
                    }
                }
            }
        }
    }

    private fun finishLock(alarmId: Long) {
        val repo = Repository.get(this)
        repo.lockEngaged = false
        repo.activeLockLabel = ""
        NotificationHelper.clear(this, alarmId)
        finish()
    }

    // Enforced lock ignores the system back gesture/button — only "mark complete" exits.
    override fun onBackPressed() {
        val mode = intent.getStringExtra(EXTRA_MODE)
        if (mode == MODE_LOCKED) return
        super.onBackPressed()
    }
}

@Composable
private fun WarningScreen(label: String, warnMinutes: Int, onComplete: () -> Unit, onSnooze: () -> Unit) {
    var secondsLeft by remember { mutableIntStateOf(warnMinutes * 60) }
    LaunchedEffect(Unit) {
        while (secondsLeft > 0) {
            delay(1000)
            secondsLeft--
        }
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(96.dp).background(AmberDim, shape = androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Timer, contentDescription = null, tint = Amber, modifier = Modifier.size(40.dp)) }
        Spacer(Modifier.height(20.dp))
        Text("SYSTEM LOCK WARNING", color = Amber, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "%d:%02d".format(secondsLeft / 60, secondsLeft % 60),
            fontSize = 44.sp, fontWeight = FontWeight.Bold
        )
        Text("Seconds remaining before enforcement locks this device", fontSize = 13.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(Modifier.height(28.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("PENDING TASK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onComplete, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Complete now & unlock")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onSnooze, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Snooze (2 min)")
        }
    }
}

@Composable
private fun RestrictedScreen(label: String, strict: Boolean, onComplete: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(96.dp).background(Red.copy(alpha = 0.15f), shape = androidx.compose.foundation.shape.CircleShape),
            contentAlignment = Alignment.Center
        ) { Icon(Icons.Filled.Lock, contentDescription = null, tint = Red, modifier = Modifier.size(40.dp)) }
        Spacer(Modifier.height(20.dp))
        Text("SYSTEM ENFORCED", color = Red, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        Text("Device access restricted", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(24.dp))
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("REQUIRED TO RELEASE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onComplete, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Mark complete & release")
        }
        if (!strict) {
            Spacer(Modifier.height(12.dp))
            Text("Emergency bypass available · 5 min lock-out", fontSize = 11.sp)
        }
    }
}
