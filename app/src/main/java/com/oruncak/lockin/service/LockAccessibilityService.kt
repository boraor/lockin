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

        // Kick the user out of the restricted app IMMEDIATELY, synchronously, before doing
        // anything else. This is the piece that was missing: the notification/activity route
        // alone has latency (post → system shows it), which leaves a window where the user can
        // see and even tap around the restricted app, or bounce Home → tap another icon faster
        // than the notification reappears. performGlobalAction(GLOBAL_ACTION_HOME) is instant and
        // forces the foreground back to the launcher every single time a restricted app's window
        // comes up — this is the same trick real screen-time/parental-control apps rely on, since
        // third-party apps can't outright prevent a window from opening, only react to it fast
        // enough that it never gets a chance to render/be usable.
        performGlobalAction(GLOBAL_ACTION_HOME)

        // Still show/refresh the lock screen + notification so the user sees WHY they got bounced
        // and has a way to mark the task complete — but debounce this part only, so we're not
        // spamming fullScreenIntent launches while GLOBAL_ACTION_HOME is firing on every event.
        val now = System.currentTimeMillis()
        if (pkg == lastPackage && now - lastTriggerAt < 800) return
        lastPackage = pkg
        lastTriggerAt = now

        NotificationHelper.showLocked(this, repo.activeLockAlarmId, repo.activeLockLabel)
    }

    override fun onInterrupt() { /* no-op */ }
}
