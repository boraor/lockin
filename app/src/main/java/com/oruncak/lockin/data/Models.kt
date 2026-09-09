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

/** Habit-grid day status, derived from the completion log — never stored directly. */
enum class DayStatus { EMPTY, NONE_DONE, PARTIAL, ALL_DONE }

data class LockInSettings(
    val warnBeforeMinutes: Int = 5,
    val secondReminder: Boolean = false,
    val dimOnLock: Boolean = false,
    val appearance: String = "system", // "system" | "light" | "dark"
    val exemptPackages: Set<String> = emptySet(),
    // Habit-grid colors, stored as ARGB ints so they're user-changeable in Settings.
    val gridNoneColor: Long = 0xFFE5584F,   // red   — scheduled tasks, zero completed
    val gridPartialColor: Long = 0xFFE8A33D, // amber — some but not all completed
    val gridAllColor: Long = 0xFF3FBF83     // green — every scheduled task completed
)

data class Account(
    val signedIn: Boolean = false,
    val name: String = "",
    val method: String = ""       // "google" | "email"
)
