# LockIn — Android project

Native Kotlin + Jetpack Compose app matching the LockIn prototype: Overview, Alarms, Streaks and
Settings tabs, a real pre-lock push notification fired by an OS-level exact alarm, and a
full-screen lock/warning activity shown even over the lock screen.

## Option A — no installs at all: let GitHub build the APK for you

You only need a free GitHub account and a browser; the actual Android build runs on GitHub's
servers via the included workflow (`.github/workflows/build-apk.yml`).

1. Go to github.com, sign in (or create a free account), and click **New repository**
   (name it `lockin`, keep it private or public, no need to add a README there).
2. On the new repo's page, click **uploading an existing file**, then drag in every file and
   folder from this `LockIn/` folder (keep the folder structure — GitHub preserves it when you
   drop a whole folder in). Commit the upload.
3. Click the **Actions** tab. GitHub will have already started (or offer to run) the
   "Build LockIn APK" workflow — if it just shows the workflow with a "Run workflow" button,
   click that.
4. Wait for the run to finish (a few minutes — the green check). Open it, scroll to
   **Artifacts**, and download **LockIn-debug-apk** — it's a zip containing `app-debug.apk`.
5. Get that `.apk` onto your phone (email it to yourself, AirDrop/Nearby Share, Google Drive —
   anything works), then tap it on the phone to install. Android will ask you to allow
   "install unknown apps" for whichever app you opened it from — allow it, then install.
6. Open LockIn and allow the notification permission prompt.

Every time you want to change the code, edit the files in the GitHub web UI (or re-upload),
and the workflow rebuilds a fresh APK automatically.

## Option B — Android Studio (if you'd rather build locally)

1. Open this folder (`LockIn/`) directly in **Android Studio** (File → Open).
2. Let Gradle sync — it will download the Gradle 8.7 distribution and dependencies the first time.
3. Connect your Android phone over USB with **USB debugging** enabled (Settings → About phone →
   tap "Build number" 7 times → Developer options → USB debugging), then select it as the run
   target and press **Run ▶**.
4. On first launch, allow the notification permission prompt (Android 13+).

## Turning the prototype into a real lock

Three things need a one-time manual step on the phone, because Android deliberately never lets
an app grant these to itself:

- **Notifications**: granted via the system prompt on first launch.
- **Exact alarms** (Android 12+): if the OS ever revokes it, re-enable it from
  Settings → Apps → LockIn → Alarms & reminders.
- **Accessibility service**: Settings screen has an "Enable LockIn in Accessibility settings"
  button — turning this on is what lets LockIn bring the lock screen back to front if a
  restricted app is opened while a lock is active. Without it, the lock still fires and shows,
  but switching away from it isn't caught.

## What's mocked vs. real

- **Real**: alarm scheduling (`AlarmManager.setAlarmClock`, survives reboot via `BootReceiver`),
  the pre-lock push notification at exactly `warnBeforeMinutes` before lock time, the full-screen
  lock/warning UI shown over the lock screen, and the accessibility-service re-lock.
- **Mocked for now**: "Continue with Google" just signs in locally as "Oruncak" — wiring it up for
  real needs a Firebase project (or Google Identity Services directly), a `google-services.json`,
  and your app's SHA-1 fingerprint registered with Google. Email/password sign-in is local-only
  too (no backend) — good enough to demo the flow, not to actually authenticate anyone.

## Structure

```
app/src/main/java/com/oruncak/lockin/
  MainActivity.kt          bottom-nav Compose scaffold (Overview/Alarms/Streaks/Settings)
  LockActivity.kt          full-screen warning + restricted screens
  AlarmScheduler.kt        computes + sets the next warning/lock alarm per AlarmItem
  AlarmReceiver.kt         fires the notification, re-arms the next occurrence
  BootReceiver.kt          re-schedules everything after a reboot
  NotificationHelper.kt    notification channel + builders
  service/LockAccessibilityService.kt   brings the lock back to front if bypassed
  data/Models.kt           AlarmItem, AppEntry, LockInSettings, Account
  data/Repository.kt       SharedPreferences-backed store (StateFlow-based)
  ui/theme/Theme.kt        colors matching the design prototype, light/dark aware
  ui/screens/*.kt          Overview, Alarms (+ editor + app picker), Streaks, Settings, Auth
```
