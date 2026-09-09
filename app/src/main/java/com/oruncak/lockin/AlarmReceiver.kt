package com.oruncak.lockin

import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.oruncak.lockin.data.Repository

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1)
        val label = intent.getStringExtra(EXTRA_LABEL) ?: "Habit"
        val repo = Repository.get(context)

        when (intent.action) {
            TYPE_WARNING -> {
                val minutes = repo.settings.value.warnBeforeMinutes
                NotificationHelper.showWarning(context, alarmId, label, minutes)
            }
            TYPE_LOCK -> {
                repo.lockEngaged = true
                repo.activeLockLabel = label
                repo.activeLockAlarmId = alarmId
                NotificationHelper.showLocked(context, alarmId, label)
            }
        }

        // Re-arm the same alarm for its next matching day so it keeps repeating.
        val alarm = repo.alarms.value.firstOrNull { it.id == alarmId }
        if (alarm != null && alarm.enabled) {
            val am = context.getSystemService(AlarmManager::class.java)
            AlarmScheduler.scheduleNextOccurrence(context, am, alarm, repo.settings.value.warnBeforeMinutes)
        }
    }

    companion object {
        const val TYPE_WARNING = "com.oruncak.lockin.ACTION_WARNING"
        const val TYPE_LOCK = "com.oruncak.lockin.ACTION_LOCK"
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        const val EXTRA_LABEL = "extra_label"
    }
}
