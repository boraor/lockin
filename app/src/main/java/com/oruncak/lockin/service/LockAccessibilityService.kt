package com.oruncak.lockin.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.oruncak.lockin.LockActivity
import com.oruncak.lockin.data.Repository

/**
 * While a lock is engaged, watches for the foreground app changing to something outside the
 * current alarm's restricted scope (or outside the exemption list) and brings LockActivity
 * back to the front. Requires the user to enable it once under
 * Settings > Accessibility > Downloaded apps > LockIn (Android won't let an app grant this
 * to itself — that's a deliberate OS restriction).
 */
class LockAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = event?.packageName?.toString() ?: return
        if (pkg == packageName) return

        val repo = Repository.get(this)
        if (!repo.lockEngaged) return

        val settings = repo.settings.value
        val activeAlarm = repo.alarms.value.firstOrNull { it.label == repo.activeLockLabel }
        val restricted = when {
            settings.exemptPackages.contains(pkg) -> false
            activeAlarm == null -> true
            activeAlarm.allApps -> true
            else -> activeAlarm.restrictedPackages.contains(pkg)
        }

        if (restricted) {
            val intent = Intent(this, LockActivity::class.java).apply {
                putExtra(LockActivity.EXTRA_MODE, LockActivity.MODE_LOCKED)
                putExtra(LockActivity.EXTRA_LABEL, repo.activeLockLabel)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() { /* no-op */ }
}
