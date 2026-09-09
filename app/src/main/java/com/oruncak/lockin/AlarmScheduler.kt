package com.oruncak.lockin

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.oruncak.lockin.data.AlarmItem
import java.util.Calendar

/**
 * Schedules two exact alarms per lock-alarm occurrence: the pre-lock warning
 * (lockTime - warnBeforeMinutes) and the lock itself. Both survive app kill because
 * they're OS-level alarms delivered to AlarmReceiver, which re-arms the next
 * occurrence when it fires.
 */
object AlarmScheduler {

    fun scheduleAll(context: Context, alarms: List<AlarmItem>, warnBeforeMinutes: Int) {
        val am = context.getSystemService(AlarmManager::class.java)
        alarms.forEach { alarm ->
            cancelForAlarm(context, alarm.id)
            if (alarm.enabled) scheduleNextOccurrence(context, am, alarm, warnBeforeMinutes)
        }
    }

    fun scheduleNextOccurrence(context: Context, am: AlarmManager, alarm: AlarmItem, warnBeforeMinutes: Int) {
        val lockTime = nextOccurrence(alarm.hour, alarm.minute, alarm.days)
        val warnTime = lockTime.clone() as Calendar
        warnTime.add(Calendar.MINUTE, -warnBeforeMinutes)

        setExact(context, am, warnTime.timeInMillis, alarm.id, alarm.label, AlarmReceiver.TYPE_WARNING)
        setExact(context, am, lockTime.timeInMillis, alarm.id, alarm.label, AlarmReceiver.TYPE_LOCK)
    }

    private fun setExact(
        context: Context, am: AlarmManager, atMillis: Long,
        alarmId: Long, label: String, type: String
    ) {
        // Safety net: never arm an alarm for a time that's already passed (or right now) — that
        // makes AlarmManager fire it immediately, which is how a rapid-repeat notification loop
        // happens if something ever reschedules mid-window. Just skip it; the next full
        // scheduleNextOccurrence() call will compute a correct future time instead.
        if (atMillis <= System.currentTimeMillis()) return

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = type
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_LABEL, label)
        }
        val requestCode = (if (type == AlarmReceiver.TYPE_WARNING) 1_000_000 else 2_000_000) + alarmId.toInt()
        val pi = PendingIntent.getBroadcast(
            context, requestCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        am.setAlarmClock(AlarmManager.AlarmClockInfo(atMillis, pi), pi)
    }

    fun cancelForAlarm(context: Context, alarmId: Long) {
        val am = context.getSystemService(AlarmManager::class.java)
        listOf(AlarmReceiver.TYPE_WARNING to 1_000_000, AlarmReceiver.TYPE_LOCK to 2_000_000).forEach { (type, base) ->
            val intent = Intent(context, AlarmReceiver::class.java).apply { action = type }
            val pi = PendingIntent.getBroadcast(
                context, base + alarmId.toInt(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            am.cancel(pi)
        }
    }

    /** Next Calendar instant matching one of [days] (1=Mon..7=Sun) at hour:minute, today if still ahead. */
    private fun nextOccurrence(hour: Int, minute: Int, days: Set<Int>): Calendar {
        val now = Calendar.getInstance()
        val candidate = Calendar.getInstance()
        candidate.set(Calendar.HOUR_OF_DAY, hour)
        candidate.set(Calendar.MINUTE, minute)
        candidate.set(Calendar.SECOND, 0)
        candidate.set(Calendar.MILLISECOND, 0)

        for (i in 0..7) {
            val test = candidate.clone() as Calendar
            test.add(Calendar.DAY_OF_YEAR, i)
            val isoDay = isoDayOfWeek(test)
            if ((days.isEmpty() || days.contains(isoDay)) && test.after(now)) {
                return test
            }
        }
        // fallback: tomorrow at the same time
        candidate.add(Calendar.DAY_OF_YEAR, 1)
        return candidate
    }

    private fun isoDayOfWeek(cal: Calendar): Int {
        // Calendar.SUNDAY=1..SATURDAY=7  ->  ISO Monday=1..Sunday=7
        val c = cal.get(Calendar.DAY_OF_WEEK)
        return if (c == Calendar.SUNDAY) 7 else c - 1
    }
}
