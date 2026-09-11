package com.oruncak.lockin.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.view.Gravity as ViewGravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.oruncak.lockin.NotificationHelper
import com.oruncak.lockin.data.Repository

/**
 * While a lock is engaged, watches for the foreground window changing to something outside the
 * current alarm's scope (or outside the exemption list) and blocks it.
 *
 * IMPORTANT: earlier versions tried performGlobalAction(GLOBAL_ACTION_HOME) to bounce the user
 * back to the launcher. That call succeeds, but "go home" doesn't stop the user from immediately
 * relaunching the same app — GLOBAL_ACTION_HOME is a navigation action, not a block. What actually
 * prevents access is drawing a full-screen overlay window directly on top of the restricted app,
 * using the special TYPE_ACCESSIBILITY_OVERLAY window type: accessibility services are allowed to
 * create these without SYSTEM_ALERT_WINDOW permission, and because it sits on top of everything
 * and is opaque, every touch the user makes lands on OUR window, not the app underneath. That is
 * how real screen-time / focus apps actually block access — this replaces the Home-bounce attempt.
 */
class LockAccessibilityService : AccessibilityService() {

    private var lastTriggerAt = 0L
    private var lastPackage: String? = null
    private var overlayView: View? = null

    /**
     * The phone's own home-screen app. This is always allowed regardless of lock settings — the
     * block is meant to stop you opening a SPECIFIC restricted app, not trap you off the launcher
     * entirely. Resolved lazily/once since it never changes at runtime for a given device.
     */
    private val launcherPackage: String? by lazy {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        packageManager.resolveActivity(intent, 0)?.activityInfo?.packageName
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val pkg = (event?.packageName?.toString() ?: rootInActiveWindow?.packageName?.toString()) ?: return

        val repo = Repository.get(this)

        if (!repo.lockEngaged) {
            removeOverlay()
            return
        }

        if (pkg == packageName) {
            // Our own app (including the overlay's own window) — never block ourselves.
            return
        }

        val settings = repo.settings.value
        val activeAlarm = repo.alarms.value.firstOrNull { it.id == repo.activeLockAlarmId }
        val restricted = when {
            // Always allow the home screen and system UI (notification shade, recents, quick
            // settings) — otherwise even navigating around a blocked app looks like the phone is
            // fully frozen instead of just that one app being off-limits.
            pkg == launcherPackage -> false
            pkg == "com.android.systemui" -> false
            settings.exemptPackages.contains(pkg) -> false
            activeAlarm == null -> true
            activeAlarm.allApps -> true
            else -> activeAlarm.restrictedPackages.contains(pkg)
        }
        if (!restricted) {
            removeOverlay()
            return
        }

        Toast.makeText(this, "LockIn: blocking $pkg", Toast.LENGTH_SHORT).show()
        showOverlay(repo)

        val now = System.currentTimeMillis()
        if (pkg == lastPackage && now - lastTriggerAt < 800) return
        lastPackage = pkg
        lastTriggerAt = now
        NotificationHelper.showLocked(this, repo.activeLockAlarmId, repo.activeLockLabel)
    }

    private fun showOverlay(repo: Repository) {
        if (overlayView != null) return // already covering the screen

        val wm = getSystemService(WINDOW_SERVICE) as WindowManager

        val root = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#F2121212"))
            isClickable = true
            isFocusable = true
        }

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = ViewGravity.CENTER
            setPadding(64, 64, 64, 64)
        }

        val title = TextView(this).apply {
            text = "This app is currently blocked by LockIn. Finish your tasks!"
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = ViewGravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        val subtitle = TextView(this).apply {
            text = "Required to release: ${repo.activeLockLabel}"
            setTextColor(Color.LTGRAY)
            textSize = 14f
            gravity = ViewGravity.CENTER
            setPadding(0, 0, 0, 48)
        }

        val button = Button(this).apply {
            text = "Mark complete & release"
            setOnClickListener {
                val alarmId = repo.activeLockAlarmId
                if (alarmId != -1L) repo.markAlarmCompleted(alarmId)
                repo.lockEngaged = false
                repo.activeLockLabel = ""
                repo.activeLockAlarmId = -1
                NotificationHelper.clear(this@LockAccessibilityService, alarmId)
                removeOverlay()
            }
        }

        content.addView(title)
        content.addView(subtitle)
        content.addView(button)
        root.addView(
            content,
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGravity.CENTER)
        )

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        try {
            wm.addView(root, params)
            overlayView = root
            Toast.makeText(this, "LockIn: overlay added", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            // Surface the failure instead of silently swallowing it — a swallowed exception here
            // looks IDENTICAL to "the code isn't running at all" from the user's side, which is
            // exactly the ambiguity that's made this bug hard to pin down over several rounds.
            Toast.makeText(this, "LockIn: overlay FAILED - ${e.javaClass.simpleName}: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun removeOverlay() {
        val view = overlayView ?: return
        try {
            val wm = getSystemService(WINDOW_SERVICE) as WindowManager
            wm.removeView(view)
        } catch (e: Exception) {
            // Already removed / view not attached — nothing to do.
        }
        overlayView = null
    }

    override fun onInterrupt() {
        removeOverlay()
    }
}
