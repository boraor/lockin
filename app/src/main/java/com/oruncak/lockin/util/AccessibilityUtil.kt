package com.oruncak.lockin.util

import android.content.Context
import android.provider.Settings

/** Whether LockIn's Accessibility service is currently enabled by the user in system settings. */
fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val expected = "${context.packageName}/com.oruncak.lockin.service.LockAccessibilityService"
    val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        ?: return false
    return enabled.split(":").any { it.equals(expected, ignoreCase = true) }
}
