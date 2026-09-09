package com.oruncak.lockin

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationHelper {
    const val CHANNEL_ID = "lockin_warnings"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = context.getSystemService(NotificationManager::class.java)
            if (nm.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Lock warnings",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Pre-lock warnings and enforcement alerts"
                    enableVibration(true)
                }
                nm.createNotificationChannel(channel)
            }
        }
    }

    /** The pre-lock warning banner ("locks in 5 min"), tapping it opens the warning screen. */
    fun showWarning(context: Context, alarmId: Long, label: String, minutesLeft: Int) {
        ensureChannel(context)
        val fullScreenIntent = Intent(context, LockActivity::class.java).apply {
            putExtra(LockActivity.EXTRA_MODE, LockActivity.MODE_WARNING)
            putExtra(LockActivity.EXTRA_LABEL, label)
            putExtra(LockActivity.EXTRA_ALARM_ID, alarmId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, alarmId.toInt(), fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("$label locks in $minutesLeft min")
            .setContentText("Open LockIn now to get ahead of the countdown.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setAutoCancel(true)
            .build()
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.notify(1000 + alarmId.toInt(), notification)
    }

    /** Fired at the lock time itself — this is what actually engages enforcement. */
    fun showLocked(context: Context, alarmId: Long, label: String) {
        ensureChannel(context)
        val fullScreenIntent = Intent(context, LockActivity::class.java).apply {
            putExtra(LockActivity.EXTRA_MODE, LockActivity.MODE_LOCKED)
            putExtra(LockActivity.EXTRA_LABEL, label)
            putExtra(LockActivity.EXTRA_ALARM_ID, alarmId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context, 2000 + alarmId.toInt(), fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle("Device access restricted")
            .setContentText("Complete \"$label\" to release the lock.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setOngoing(true)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .build()
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.notify(2000 + alarmId.toInt(), notification)
        context.startActivity(fullScreenIntent)
    }

    fun clear(context: Context, alarmId: Long) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.cancel(1000 + alarmId.toInt())
        nm.cancel(2000 + alarmId.toInt())
    }
}
