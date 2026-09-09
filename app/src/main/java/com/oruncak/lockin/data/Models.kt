package com.oruncak.lockin.data

/** A single scheduled lock alarm, e.g. "14:00 Mid-Day Core Stretch, Mon-Fri, restricts Instagram+TikTok". */
data class AlarmItem(
    val id: Long = System.currentTimeMillis(),
    val hour: Int,
    val minute: Int,
    val label: String,
    val days: Set<Int>,          // 1=Mon .. 7=Sun (Calendar.MONDAY..SUNDAY style, stored as 1..7)
    val enabled: Boolean = true,
    val allApps: Boolean = true,
    val restrictedPackages: Set<String> = emptySet()
)

/** A known app the user can pick from, for lock scope or exemptions. */
data class AppEntry(
    val packageName: String,
    val label: String
)

data class LockInSettings(
    val warnBeforeMinutes: Int = 5,
    val secondReminder: Boolean = false,
    val dimOnLock: Boolean = false,
    val strictLockMode: Boolean = true,
    val appearance: String = "system", // "system" | "light" | "dark"
    val exemptPackages: Set<String> = emptySet()
)

data class Account(
    val signedIn: Boolean = false,
    val name: String = "",
    val method: String = ""       // "google" | "email"
)
