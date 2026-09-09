package com.oruncak.lockin

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.oruncak.lockin.data.Repository

/** Re-arms every enabled alarm after a reboot, since exact alarms don't survive a restart. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val repo = Repository.get(context)
        AlarmScheduler.scheduleAll(context, repo.alarms.value, repo.settings.value.warnBeforeMinutes)
    }
}
