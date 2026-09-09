package com.oruncak.lockin.util

import android.content.Context
import android.os.PowerManager
import android.provider.Settings

/** Whether LockIn's Accessibility service is currently enabled by the user in system settings. */
fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expected = "${context.packageName}/com.oruncak.lockin.service.LockAccessibilityService"
    val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        ?: return false
    return enabled.split(":").any { it.equals(expected, ignoreCase = true) }
}

/**
 * Whether the OS is exempting LockIn from battery optimization. Several OEMs (Samsung, Xiaomi,
 * OnePlus, etc.) kill background accessibility services under aggressive battery management —
 * without this exemption, locking can work right after granting Accessibility and then quietly
 * stop working later, which looks like a bug but is really the OS suspending the service.
 */
fun isIgnoringBatteryOptimizations(context: Context): Boolean {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.isIgnoringBatteryOptimizations(context.packageName)
}
