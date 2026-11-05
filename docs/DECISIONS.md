# SAR — Decision Log

Decisions made by Saurabh while designing SAR (5 Nov 2026), with the reason behind each. The FSD already reflects all of them; this file explains *why*, so they are not "improved" away later. Add new decisions at the bottom with a date.

| # | Decision | Why |
| --- | --- | --- |
| 1 | **Personal app for Saurabh's own phone only.** Sideloaded, never on the Play Store. | Removes Play Store policy limits, so the most reliable blocking method (accessibility service) can be used freely. Earlier idea of publishing was dropped. |
| 2 | **Modes** group app rules. Only one mode runs at a time. | Simple mental model: "I'm in Study mode now." |
| 3 | Each mode has **its own block-screen message and image**. | The block screen should feel like a reminder from that context (study vs gym vs sleep), not a generic wall. |
| 4 | Each controlled app gets exactly **two options: Complete block or Allowed time**. Apps not picked are unaffected. | Keeps setup fast. Open-count limits were considered and left out. |
| 5 | **Session cap** (max continuous minutes, then a break) and **Emergency extra time** are both configurable. | Saurabh asked for both explicitly. Session cap is per app; emergency time is per mode. |
| 6 | Each mode is **Manual or Scheduled**, chosen when creating it, and can be switched later. | Some modes are habits on a timetable (Office Work), others are ad hoc (Gym). |
| 7 | A manual mode can be started **open-ended (stop any time)** or **for a set duration (locked)**. | Saurabh wanted a commitment option: "if I selected a time duration I can't stop it." |
| 8 | **Scheduled modes are locked** during their window. | Consistent with 7: a set time window means commitment. |
| 9 | **Allowance resets at the start of every run.** | The time window is the mode's run. Cheating by restarting is harmless for open-ended runs (they can be stopped anyway) and impossible for locked runs. |
| 10 | **Free a mode** in Settings ends a locked run after **5 addition/subtraction questions answered correctly in a row**; a wrong answer restarts. | A deliberate, slightly annoying way out — friction, not a prison. |
| 11 | **Collision:** when a scheduled mode is due during another run, show a warning with "Start <Mode>". Ignoring it keeps the current mode; the scheduled one starts when the current run ends (if its window is still open). Switching needs no math challenge. | Saurabh's rule: "If I want to continue I'll do nothing; if I want the next session I'll choose it." Switching to another restriction is not escaping. |
| 12 | **Five default modes:** Self Study, Gym, Meditation, Office Work, Weekend — all editable. | Saurabh asked for them; app suggestions come from Android app categories. |
| 13 | A locked manual run **can be extended, never shortened**; extending adds no allowance. | Extending only adds commitment, so it can't be used as a loophole. |
| 14 | **Emergency-time uses are counted per run.** | Consistent with allowance reset per run. |
| 15 | **SAR never blocks itself, the dialer or Settings, and does not protect against its own uninstall.** Uninstalling wipes all modes, locks and history. | Saurabh's call. Keeps the phone usable and avoids device-admin complexity. |
| 16 | Native **Kotlin + Jetpack Compose**, accessibility service for detection, Room for storage, exact alarms for schedules. | Instant blocking, low battery, full control. Flutter and polling approaches were rejected. |
| 17 | **Battery first:** event-driven, no polling, no foreground service, no wake locks, no network. Target < 1 % battery/day. | Saurabh: "it should not leak any power and battery." See FSD 11.3. |
| 18 | **Visual style: modern sci-fi, very easy to use.** Dark navy/black, neon cyan + violet, Orbitron + Exo 2. | Saurabh approved the Figma design ("looks great"). |
| 19 | Name: **Saurabh's App Regulator**, short name **SAR**. | Saurabh's choice. |

## Build decisions (5 Nov 2026, first build session)

| # | Decision | Why |
| --- | --- | --- |
| 20 | `core/` is its own pure-Kotlin Gradle module (`:core`), not a folder inside `app/`. Package names are unchanged (`com.saurabh.sar.core.*`). | The compiler now refuses any `android.*` import in core, and core tests run on any machine without the Android SDK. |
| 21 | Every APK (debug and release) is signed with one fixed key kept in the repo (`app/sar-signing.keystore`). | SAR is sideloaded on one phone. A fixed key lets each new APK install over the old one without wiping modes and history. Not a secret worth protecting for a personal app. |
| 22 | Exact alarms use `USE_EXACT_ALARM` (granted at install on Android 13+), with `SCHEDULE_EXACT_ALARM` only for Android 12. | Sideloaded, so no Play policy limits; no extra permission screen needed. |
| 23 | On-screen clocks (Home time left, break countdown, wait timer) update once a minute (once a second for countdowns) **only while that screen is visible**, and stop otherwise. | FSD 11.3 allows animations while visible. Blocking itself never uses a timer: it uses one callback at the moment a limit runs out. |
| 24 | Database (DAO) tests run on the phone (`./gradlew connectedDebugAndroidTest`) with the standard androidx.test runner; no Robolectric. | Avoids adding a library (CLAUDE.md). |
| 25 | The block screen is a full-screen Activity in its own hidden task (not an overlay window). | Starts in well under 1 s from the accessibility service, supports the emergency phrase keyboard, and Back/Recents never lead into the blocked app. |
| 26 | APKs are built by GitHub Actions (`.github/workflows/build-apk.yml`) and published as the `sar-latest` release. | Saurabh asked for an APK without needing a computer setup. Android Studio still works locally with the same commands. |
| 27 | No navigation or ViewModel library: a small back stack (`ui/Nav.kt`) and app-level flows. | Minimal dependencies (CLAUDE.md). |
| 28 | Versions pinned to a known-good set: AGP 8.11.1, Kotlin 2.2.21, KSP 2.2.21-2.0.5, Compose BOM 2025.07.00, Room 2.7.2, compileSdk/targetSdk 36. | Stable together; upgrade deliberately later. Runs fine on One UI 9. |
| 29 | All nine milestones were built in one session, because Saurabh asked for the whole app and an APK at once. Milestones in BUILD_PLAN stay unticked until he confirms each on the phone. | His instruction overrides "one milestone per session" for this build. |
| 30 | While no mode runs, the accessibility service only stores the package name of the app in front (one assignment) and returns. | If a scheduled mode starts while a blocked app is already open, it is blocked at once (FSD 6.1). |
| 31 | A scheduled mode starts in a window only if it has not already run in that window. | Makes "freed modes return next time" (FSD 7) and "ignored collision starts when the current run ends" (FSD 5.4) the same simple rule. |
| 32 | Emergency time can also open a Complete-block app, not only an app whose allowance is used up. | FSD 6.1 shows the Emergency button on every block screen where uses remain; only the session-cap break is excluded (FSD 6.2). |
| 33 | History counts a run's app time on the day the run started. | Simple and good enough within the 1-minute accuracy target; runs rarely cross midnight. |
