package com.oruncak.lockin.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/**
 * Everything is persisted as plain JSON in SharedPreferences — no database dependency needed
 * for a single-user local app. Swap for Room/DataStore later if you add sync.
 */
class Repository private constructor(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("lockin_store", Context.MODE_PRIVATE)
    private val pm: PackageManager = context.applicationContext.packageManager

    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val alarms: MutableStateFlow<List<AlarmItem>> = MutableStateFlow(loadAlarms())
    val settings: MutableStateFlow<LockInSettings> = MutableStateFlow(loadSettings())
    val account: MutableStateFlow<Account> = MutableStateFlow(loadAccount())
    /** date "yyyy-MM-dd" -> set of alarm ids completed that day. */
    val completionLog: MutableStateFlow<Map<String, Set<Long>>> = MutableStateFlow(loadLog())

    init {
        // Only days from first launch onward are eligible for the habit grid — otherwise a
        // fresh install would show every past day as red for "zero completed".
        if (!prefs.contains("first_run_date")) {
            prefs.edit().putString("first_run_date", todayKey()).apply()
        }
    }

    val firstRunDate: String get() = prefs.getString("first_run_date", todayKey()) ?: todayKey()

    /** Active lock state, read by LockActivity / the accessibility service. */
    var lockEngaged: Boolean
        get() = prefs.getBoolean("lock_engaged", false)
        set(value) = prefs.edit().putBoolean("lock_engaged", value).apply()

    var activeLockLabel: String
        get() = prefs.getString("active_lock_label", "") ?: ""
        set(value) = prefs.edit().putString("active_lock_label", value).apply()

    var activeLockAlarmId: Long
        get() = prefs.getLong("active_lock_alarm_id", -1)
        set(value) = prefs.edit().putLong("active_lock_alarm_id", value).apply()

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
                    restrictedPackages = (0 until (o.optJSONArray("restrictedPackages")?.length() ?: 0))
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

    // ---------- Completion log / streak / habit grid ----------
    fun todayKey(cal: Calendar = Calendar.getInstance()): String = dayFormat.format(cal.time)

    fun markAlarmCompleted(alarmId: Long, dateKey: String = todayKey()) {
        val current = completionLog.value.toMutableMap()
        current[dateKey] = (current[dateKey] ?: emptySet()) + alarmId
        completionLog.value = current
        persistLog(current)
    }

    private fun persistLog(map: Map<String, Set<Long>>) {
        val obj = JSONObject()
        map.forEach { (date, ids) -> obj.put(date, JSONArray(ids.toList())) }
        prefs.edit().putString("completion_log", obj.toString()).apply()
    }

    private fun loadLog(): Map<String, Set<Long>> {
        val raw = prefs.getString("completion_log", null) ?: return emptyMap()
        return try {
            val obj = JSONObject(raw)
            obj.keys().asSequence().associateWith { key ->
                val arr = obj.getJSONArray(key)
                (0 until arr.length()).map { arr.getLong(it) }.toSet()
            }
        } catch (e: Exception) {
            emptyMap()
        }
    }

    /** Alarm ids that were enabled and scheduled to fire on [dateKey] (by day-of-week). */
    fun scheduledAlarmIdsForDate(dateKey: String): Set<Long> {
        val cal = Calendar.getInstance().apply {
            val parts = dateKey.split("-").map { it.toInt() }
            set(parts[0], parts[1] - 1, parts[2], 0, 0, 0)
        }
        val iso = isoDayOfWeek(cal)
        return alarms.value.filter { it.enabled && it.days.contains(iso) }.map { it.id }.toSet()
    }

    fun dayStatus(dateKey: String): DayStatus {
        if (dateKey < firstRunDate) return DayStatus.EMPTY
        val scheduled = scheduledAlarmIdsForDate(dateKey)
        if (scheduled.isEmpty()) return DayStatus.EMPTY
        val completed = completionLog.value[dateKey] ?: emptySet()
        val doneCount = scheduled.count { completed.contains(it) }
        return when {
            doneCount == 0 -> DayStatus.NONE_DONE
            doneCount == scheduled.size -> DayStatus.ALL_DONE
            else -> DayStatus.PARTIAL
        }
    }

    /** Consecutive fully-completed days ending yesterday (today doesn't count until it's done). */
    fun currentStreak(): Int {
        var streak = 0
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        while (true) {
            val key = todayKey(cal)
            if (key < firstRunDate) break
            when (dayStatus(key)) {
                DayStatus.ALL_DONE -> { streak++; cal.add(Calendar.DAY_OF_YEAR, -1) }
                DayStatus.EMPTY -> cal.add(Calendar.DAY_OF_YEAR, -1) // no alarms that day, skip without breaking
                else -> return streak
            }
            if (streak > 3650) break // safety valve
        }
        // If today is already fully completed, count it too.
        if (dayStatus(todayKey()) == DayStatus.ALL_DONE) streak++
        return streak
    }

    fun completionRateLast30Days(): Int {
        var scheduled = 0
        var completed = 0
        val cal = Calendar.getInstance()
        repeat(30) {
            val key = todayKey(cal)
            if (key >= firstRunDate) {
                val s = scheduledAlarmIdsForDate(key)
                scheduled += s.size
                completed += (completionLog.value[key] ?: emptySet()).count { s.contains(it) }
            }
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        return if (scheduled == 0) 0 else ((completed * 100f) / scheduled).toInt()
    }

    fun tasksCompletedTotal(): Int = completionLog.value.values.sumOf { it.size }

    private fun isoDayOfWeek(cal: Calendar): Int {
        val c = cal.get(Calendar.DAY_OF_WEEK)
        return if (c == Calendar.SUNDAY) 7 else c - 1
    }

    // ---------- Settings ----------
    fun updateSettings(update: (LockInSettings) -> LockInSettings) {
        val next = update(settings.value)
        settings.value = next
        prefs.edit()
            .putInt("warnBeforeMinutes", next.warnBeforeMinutes)
            .putBoolean("secondReminder", next.secondReminder)
            .putBoolean("dimOnLock", next.dimOnLock)
            .putString("appearance", next.appearance)
            .putStringSet("exemptPackages", next.exemptPackages)
            .putLong("gridNoneColor", next.gridNoneColor)
            .putLong("gridPartialColor", next.gridPartialColor)
            .putLong("gridAllColor", next.gridAllColor)
            .apply()
    }

    private fun loadSettings() = LockInSettings(
        warnBeforeMinutes = prefs.getInt("warnBeforeMinutes", 5),
        secondReminder = prefs.getBoolean("secondReminder", false),
        dimOnLock = prefs.getBoolean("dimOnLock", false),
        appearance = prefs.getString("appearance", "system") ?: "system",
        exemptPackages = prefs.getStringSet("exemptPackages", emptySet())?.toSet() ?: emptySet(),
        gridNoneColor = prefs.getLong("gridNoneColor", 0xFFE5584F),
        gridPartialColor = prefs.getLong("gridPartialColor", 0xFFE8A33D),
        gridAllColor = prefs.getLong("gridAllColor", 0xFF3FBF83)
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

    fun appIcon(packageName: String): Drawable? = try {
        pm.getApplicationIcon(packageName)
    } catch (e: Exception) {
        null
    }

    companion object {
        @Volatile private var instance: Repository? = null
        fun get(context: Context): Repository =
            instance ?: synchronized(this) {
                instance ?: Repository(context).also { instance = it }
            }
    }
}
