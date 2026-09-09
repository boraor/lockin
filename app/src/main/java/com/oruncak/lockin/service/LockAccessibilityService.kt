package com.oruncak.lockin.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import com.oruncak.lockin.NotificationHelper
import com.oruncak.lockin.data.Repository

/**
 * While a lock is engaged, watches for the foreground window changing to something outside the
 * current alarm's scope (or outside the exemption list) and re-shows the lock screen — including
 * when the user presses Home, since the launcher itself counts as "an app" for an all-apps lock.
 *
 * Re-showing goes through NotificationHelper's high-priority full-screen-intent notification
 * rather than calling startActivity directly from here: a bound service calling startActivity
 * from the background can get silently blocked by Android's background-activity-start
 * restrictions on some OS versions, whereas a fullScreenIntent notification posted by the app's
 * own process is reliably honored. That mismatch — the first lock working (it's notification-
 * driven) but re-locks after Home/app-switch doing nothing (they were direct startActivity calls)
 * — is exactly the bug this fixes.
 *
 * Requires the user to enable this service once under Settings > Accessibility > Downloaded
 * apps > LockIn, and to exempt LockIn from battery optimization — several OEMs (Samsung, Xiaomi,
 * OnePlus, etc.) kill background accessibility services under aggressive battery management,
 * which looks identical to "the lock stopped working."
 */
class LockAccessibilityService : AccessibilityService() {

    private var lastTriggerAt = 0L
    private var lastPackage: String? = null

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = (event?.packageName?.toString() ?: rootInActiveWindow?.packageName?.toString()) ?: return
        if (pkg == packageName) return

        val repo = Repository.get(this)
        if (!repo.lockEngaged) return

        val settings = repo.settings.value
        val activeAlarm = repo.alarms.value.firstOrNull { it.id == repo.activeLockAlarmId }
        val restricted = when {
            settings.exemptPackages.contains(pkg) -> false
            activeAlarm == null -> true
            activeAlarm.allApps -> true
            else -> activeAlarm.restrictedPackages.contains(pkg)
        }
        if (!restricted) return

        // Light debounce: the same foreground package can fire several window events in a row
        // (e.g. while it's still animating in) — no need to re-post the notification each time.
        val now = System.currentTimeMillis()
        if (pkg == lastPackage && now - lastTriggerAt < 800) return
        lastPackage = pkg
        lastTriggerAt = now

        NotificationHelper.showLocked(this, repo.activeLockAlarmId, repo.activeLockLabel)
    }

    override fun onInterrupt() { /* no-op */ }
}
