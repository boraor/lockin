package com.oruncak.lockin.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * Everything is persisted as plain JSON in SharedPreferences — no database dependency needed
 * for a single-user local app. Swap for Room/DataStore later if you add sync.
 */
class Repository private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("lockin_store", Context.MODE_PRIVATE)
    private val pm: PackageManager = context.applicationContext.packageManager

    val alarms: MutableStateFlow<List<AlarmItem>> = MutableStateFlow(loadAlarms())
    val settings: MutableStateFlow<LockInSettings> = MutableStateFlow(loadSettings())
    val account: MutableStateFlow<Account> = MutableStateFlow(loadAccount())

    /** Active lock state, read by LockActivity / the accessibility service. */
    var lockEngaged: Boolean
        get() = prefs.getBoolean("lock_engaged", false)
        set(value) = prefs.edit().putBoolean("lock_engaged", value).apply()

    var activeLockLabel: String
        get() = prefs.getString("active_lock_label", "") ?: ""
        set(value) = prefs.edit().putString("active_lock_label", value).apply()

    // ---------- Alarms ----------
    fun saveAlarm(alarm: AlarmItem) {
        val current = alarms.value.toMutableList()
        val idx = current.indexOfFirst { it.id == alarm.id }
        if (idx >= 0) current[idx] = alarm else current.add(alarm)
        alarms.value = current
        persistAlarms(current)
    }

    fun deleteAlarm(id: Long) {
        val current = alarms.value.filterNot { it.id == id }
        alarms.value = current
        persistAlarms(current)
    }

    fun setAlarmEnabled(id: Long, enabled: Boolean) {
        val current = alarms.value.map { if (it.id == id) it.copy(enabled = enabled) else it }
        alarms.value = current
        persistAlarms(current)
    }

    private fun persistAlarms(list: List<AlarmItem>) {
        val arr = JSONArray()
        list.forEach { a ->
            arr.put(JSONObject().apply {
                put("id", a.id)
                put("hour", a.hour)
                put("minute", a.minute)
                put("label", a.label)
                put("days", JSONArray(a.days.toList()))
                put("enabled", a.enabled)
                put("allApps", a.allApps)
                put("restrictedPackages", JSONArray(a.restrictedPackages.toList()))
            })
        }
        prefs.edit().putString("alarms", arr.toString()).apply()
    }

    private fun loadAlarms(): List<AlarmItem> {
        val raw = prefs.getString("alarms", null) ?: return defaultAlarms()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                AlarmItem(
                    id = o.getLong("id"),
                    hour = o.getInt("hour"),
                    minute = o.getInt("minute"),
                    label = o.getString("label"),
                    days = (0 until o.getJSONArray("days").length())
                        .map { o.getJSONArray("days").getInt(it) }.toSet(),
                    enabled = o.optBoolean("enabled", true),
                    allApps = o.optBoolean("allApps", true),
                    restrictedPackages = (0 until o.optJSONArray("restrictedPackages")!!.length())
                        .map { o.getJSONArray("restrictedPackages").getString(it) }.toSet()
                )
            }
        } catch (e: Exception) {
            defaultAlarms()
        }
    }

    private fun defaultAlarms(): List<AlarmItem> = listOf(
        AlarmItem(id = 1, hour = 7, minute = 30, label = "Morning Oral Routine", days = (1..7).toSet()),
        AlarmItem(id = 2, hour = 14, minute = 0, label = "Mid-Day Core Stretch", days = setOf(1, 2, 3, 4, 5)),
        AlarmItem(id = 3, hour = 21, minute = 30, label = "Mindfulness Journaling", days = (1..7).toSet())
    )

    // ---------- Settings ----------
    fun updateSettings(update: (LockInSettings) -> LockInSettings) {
        val next = update(settings.value)
        settings.value = next
        prefs.edit()
            .putInt("warnBeforeMinutes", next.warnBeforeMinutes)
            .putBoolean("secondReminder", next.secondReminder)
            .putBoolean("dimOnLock", next.dimOnLock)
            .putBoolean("strictLockMode", next.strictLockMode)
            .putString("appearance", next.appearance)
            .putStringSet("exemptPackages", next.exemptPackages)
            .apply()
    }

    private fun loadSettings() = LockInSettings(
        warnBeforeMinutes = prefs.getInt("warnBeforeMinutes", 5),
        secondReminder = prefs.getBoolean("secondReminder", false),
        dimOnLock = prefs.getBoolean("dimOnLock", false),
        strictLockMode = prefs.getBoolean("strictLockMode", true),
        appearance = prefs.getString("appearance", "system") ?: "system",
        exemptPackages = prefs.getStringSet("exemptPackages", setOf(pmSelfPackage))?.toSet() ?: emptySet()
    )

    // ---------- Account (local-only mock; wire to Firebase Auth / Credential Manager for real sign-in) ----------
    fun signIn(name: String, method: String) {
        val acc = Account(signedIn = true, name = name, method = method)
        account.value = acc
        prefs.edit().putBoolean("signedIn", true).putString("acctName", name).putString("acctMethod", method).apply()
    }

    fun signOut() {
        account.value = Account()
        prefs.edit().putBoolean("signedIn", false).apply()
    }

    private fun loadAccount() = Account(
        signedIn = prefs.getBoolean("signedIn", false),
        name = prefs.getString("acctName", "") ?: "",
        method = prefs.getString("acctMethod", "") ?: ""
    )

    // ---------- Installed user apps, for the app-picker sheets ----------
    fun installedApps(): List<AppEntry> {
        val flags = PackageManager.GET_META_DATA
        return pm.getInstalledApplications(flags)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 && it.packageName != "com.oruncak.lockin" }
            .map { AppEntry(it.packageName, pm.getApplicationLabel(it).toString()) }
            .sortedBy { it.label.lowercase() }
    }

    companion object {
        private const val pmSelfPackage = "com.android.dialer"
        @Volatile private var instance: Repository? = null
        fun get(context: Context): Repository =
            instance ?: synchronized(this) {
                instance ?: Repository(context).also { instance = it }
            }
    }
}
