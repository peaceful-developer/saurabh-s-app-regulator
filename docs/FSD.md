# Saurabh's App Regulator — Functional Specification

Nov 5, 2026 · Saurabh

> Source of truth for *what* SAR does. If code and this document disagree, this document wins — raise the conflict instead of guessing.

## 1. Overview

Saurabh's App Regulator (SAR) is a personal Android app that limits how long chosen apps can be used while a **mode** is active, and blocks them once the limit is reached.

**Purpose.** Help one person (the owner) stick to self-set limits on distracting apps during study, work, exercise, rest and leisure.

**Scope.**

- One user, one phone. The app is installed directly (sideloaded), not published on the Play Store.
- All data stays on the phone. No account, server, sync or analytics.
- Android 10 (API 29) and later.

**Goals.**

1. Group app limits into reusable modes, each with its own block screen.
2. Block an app the instant its limit is reached, for the rest of that mode's run.
3. Make leaving a locked mode deliberate (a math challenge), not impossible.
4. Show how much time each controlled app has used.

## 2. Glossary

| Term | Meaning |
| --- | --- |
| Mode | A named set of app rules, with its own block-screen message and image. Only one mode is active at a time. |
| Manual mode | A mode the user starts and stops by hand. |
| Scheduled mode | A mode that starts and ends automatically on set days and times. |
| Run | One continuous period during which a mode is active, from its start to its end. |
| Locked | A running mode that cannot be stopped or switched away from until its end time, except through Free a mode. |
| Controlled app | An app the user has added to a mode. Apps not added are unaffected by that mode. |
| Rule | The setting for one controlled app: either Complete block or Allowed time. |
| Allowance | The minutes a controlled app may be used during one run. |
| Session cap | The most minutes an app may be used in one continuous stretch, followed by a forced break. |
| Block screen | The full-screen page that covers a blocked app. |
| Emergency extra time | A limited, deliberately effortful way to unlock a few more minutes from the block screen. |
| Free a mode | The settings option that ends a locked mode after 5 arithmetic questions are answered correctly. |

## 3. Modes

The user creates, edits and deletes modes; the app ships with five editable defaults.

### 3.1 Mode fields

| Field | Required | Notes |
| --- | --- | --- |
| Name | Yes | Unique, up to 30 characters. |
| Type | Yes | Manual or Scheduled. Chosen at creation, changeable later (3.3). |
| Schedule | Scheduled only | One or more entries of days of the week + start time + end time. An end time earlier than the start time runs past midnight (e.g. 23:00–07:00). |
| Block-screen message | Yes | Up to 120 characters. |
| Block-screen image | No | Picked from the gallery or a built-in image. Copied into app storage so it survives the original being deleted. |
| Controlled apps and rules | No | See section 4. A mode with no apps is allowed but blocks nothing. |
| Emergency extra time | Yes | On/off plus its settings (section 6.2). |

### 3.2 Create, edit, delete

- **Create:** a form with the fields above, then the app picker (section 4).
- **Edit:** every field can be changed. Changes to a mode that is currently running take effect immediately, except that minutes already used in this run are kept.
- **Delete:** asks for confirmation. A running mode cannot be deleted; it must end first.
- **Duplicate:** copies a mode as a starting point for a new one.

### 3.3 Switching between manual and scheduled

- Manual → Scheduled: the user must add at least one schedule entry before saving.
- Scheduled → Manual: the schedule is removed after confirmation.
- The type cannot be changed while that mode is running and locked.

### 3.4 Default modes

On first launch the app creates these modes. Suggested apps are matched from Android's app categories (social, games, video) against what is installed, and shown for the user to adjust before saving.

| Mode | Type | Suggested rules | Emergency time |
| --- | --- | --- | --- |
| Self Study | Manual | Social and games blocked; video apps 20 min | On |
| Gym | Manual | Social blocked; music apps not controlled | On |
| Meditation | Manual | All suggested apps blocked | Off |
| Office Work | Scheduled, Mon–Fri 09:00–18:00 | Social 15 min; games blocked | On |
| Weekend | Scheduled, Sat–Sun 00:00–23:59 | Social 60 min; games 60 min | On |

Each default comes with its own message and built-in image, both editable.

## 4. App rules within a mode

Each controlled app in a mode has exactly one rule: **Complete block** or **Allowed time**.

### 4.1 App picker

- Lists all launchable apps installed on the phone, with icon and name, searchable and grouped by category.
- The user ticks the apps the mode should control.
- SAR itself, the phone dialer and the Settings app cannot be added, so the phone stays usable for calls and the user can always reach Free a mode.

### 4.2 Rule types

| Rule | Setting | Behaviour while the mode runs |
| --- | --- | --- |
| Complete block | none | Block screen appears every time the app is opened. |
| Allowed time | Minutes, 1–720 | App opens normally until the allowance for this run is used up, then the block screen appears. |

### 4.3 Session cap (Allowed time only, optional)

- Settings: **maximum continuous minutes** (e.g. 15) and **break minutes** (e.g. 10).
- Continuous use = time the app is in the foreground without a gap of 2 minutes or more. A gap of 2+ minutes resets the stretch.
- When the cap is hit, the block screen appears with a countdown to the end of the break. After the break the app opens again if allowance remains.
- The session cap never adds time: allowance still runs out at its total.

### 4.4 Counting usage

- Only time with the app in the foreground and the screen on counts.
- Usage is counted per run of the mode. Picture-in-picture video is counted as foreground use.

## 5. Mode lifecycle

One mode runs at a time; a run is either unlocked (stop any time) or locked (runs to its end time unless freed).

**States and transitions:**

```
                 Start
  No mode  ───────────────▶  Unlocked run (manual, stop any time)
  active   ◀───────────────
                 Stop

           Start for… / schedule begins
  No mode  ───────────────▶  Locked run (timed manual, or scheduled)
  active   ◀───────────────
           Time up / Free a mode
```

A due scheduled mode can also replace either kind of run when the user taps its warning (5.4).

### 5.1 Starting a manual mode

From the home screen the user taps a manual mode and chooses:

- **Start** — an unlocked run with no end time. The user can stop it at any time with one tap.
- **Start for…** — a locked run for a chosen duration (15 min to 12 h, in 15-minute steps, or a custom end time). It cannot be stopped or switched away from until it ends. While it runs, the user can **extend** it (by 15-minute steps or to a later end time) but never shorten it. Extending does not add allowance minutes.

If another mode is already running, the user is asked to stop it first. If that run is locked, it can only be ended through Free a mode (section 7).

### 5.2 Scheduled modes

- A scheduled mode starts automatically at each scheduled start time and ends at the matching end time.
- A scheduled run is always locked.
- A scheduled mode cannot be started by hand outside its schedule. To run it ad hoc, duplicate it as a manual mode.

### 5.3 Allowance reset

- Every app's allowance, session-cap counters and emergency-time uses reset at the **start of each run**.
- An unlocked run can be stopped and restarted for a fresh allowance. This is accepted: the user could stop it anyway.

### 5.4 Collisions with a scheduled mode

When a scheduled mode's start time arrives while another mode is running:

1. A notification and an in-app banner say: "<Mode> is scheduled now. <Current mode> is still active." with a **Start <Mode>** button.
2. If the user taps **Start <Mode>**, the current run ends and the scheduled mode starts, with no math challenge. This works even if the current run is locked.
3. If the user does nothing, the current run continues.
4. When the current run ends, the scheduled mode starts automatically for whatever remains of its window.
5. If the scheduled window has already closed by then, nothing starts.

When two scheduled modes overlap each other, the one that started first keeps running and the later one follows the same steps. The mode editor warns when a new schedule overlaps an existing one.

### 5.5 Ending a run

| Run type | Ends when |
| --- | --- |
| Manual, unlocked | User taps Stop |
| Manual, locked | Chosen duration elapses, or Free a mode, or the user switches to a due scheduled mode |
| Scheduled | Scheduled end time, or Free a mode, or the user switches to another due scheduled mode |

When a run ends, all its blocks lift immediately.

## 6. Block screen and emergency extra time

The block screen covers a blocked app within 1 second of it opening and shows the active mode's own message and image.

### 6.1 Block screen

**Shown when** a controlled app comes to the foreground and any of these is true:

- its rule is Complete block;
- its allowance for this run is used up;
- it is in a session-cap break.

If the app is already open when the limit is reached, the block screen appears at that moment.

**Contents:**

- The mode's image (full width) and message.
- The blocked app's icon and name.
- The reason: "Blocked in <Mode>", "30 of 30 min used", or "Break: 7 min left".
- When the block lifts: the run's end time, or "when you stop <Mode>" for an unlocked run.
- **Go home** button (returns to the launcher).
- **Emergency time** button, only if enabled for the mode and uses remain.

Back and Recent-apps gestures lead away from the blocked app, never into it.

### 6.2 Emergency extra time

Configured per mode:

| Setting | Range | Default |
| --- | --- | --- |
| Enabled | On / Off | On |
| Minutes per use | 1–15 | 5 |
| Uses per run | 1–5 | 2 |
| Unlock effort | Wait timer (10–120 s) or type a phrase | Wait 30 s |
| Phrase (if typing) | Free text, up to 100 characters | "I am choosing to break my own rule" |

**Flow:** the user taps Emergency time → completes the effort (waits, or types the phrase exactly) → the app opens for the set minutes → the block screen returns when they run out.

- Uses are counted per run, shared across all apps in that mode.
- Emergency time does not apply to the session-cap break.

## 7. Free a mode

A locked run ends early only after the user answers 5 addition or subtraction questions correctly in a row.

**Where:** Settings → Free a mode. The option is greyed out when no locked run is active.

**Questions:**

- 5 questions, each randomly addition or subtraction.
- Operands are random integers from 10 to 999. Subtraction never gives a negative result.
- One question per screen, numeric keypad only, no time limit.

**Rules:**

1. Any wrong answer discards progress and starts a new set of 5 questions.
2. Leaving the screen discards progress.
3. After the 5th correct answer the user confirms "End <Mode> now?" and the run ends; all blocks lift.
4. A scheduled mode that is freed starts again at its next scheduled time.
5. An unlocked run never needs Free a mode; it has a plain Stop button.

## 8. Home screen, usage tracking and history

The home screen answers "which mode is on, and how much have I got left?" at a glance.

### 8.1 Home screen

- **Active mode card:** mode name, Locked/Unlocked badge, started at, ends at (or "until you stop it"), and a Stop button for unlocked runs.
- **Controlled apps list** for the active mode, each row showing icon, name and either "Blocked" or a progress bar "18 / 30 min". Rows near or at their limit are highlighted. Session-cap breaks show their countdown.
- **Emergency uses left** for this run.
- **Up next:** the next scheduled mode and its start time.
- **Modes list:** all modes with their type; tapping a manual mode offers Start / Start for…; a long press opens Edit.
- When no mode is running: "No mode active" plus the modes list.

### 8.2 Notifications

- A persistent notification while a mode runs: "<Mode> active · ends 18:00". Status only; it does not keep extra work running.
- A warning when an app has **5 minutes** of allowance left, and again at **1 minute**.
- The collision warning from section 5.4.

### 8.3 History

- A **Today** view and a **7-day** view of minutes per controlled app, per mode.
- Each run is listed with mode, start, end, how it ended (time up, stopped, freed, switched) and emergency uses.
- History is kept for **30 days**, then deleted automatically.

## 9. Settings, permissions and onboarding

SAR needs five permissions to block reliably; onboarding walks through each one and SAR shows a red banner if any is later revoked.

### 9.1 Permissions

| Permission | Why SAR needs it |
| --- | --- |
| Accessibility service | Detects the instant an app comes to the foreground, so the block screen can cover it. |
| Usage access | Reads per-app foreground time for usage counting and history. |
| Display over other apps | Shows the block screen on top of the blocked app. |
| Notifications | Run status notification, limit warnings, collision warning. |
| Ignore battery optimisation | Lets schedule alarms fire on time (see 11.3). |

### 9.2 Onboarding (first launch)

1. Welcome screen: one sentence on what SAR does.
2. Permission steps, one screen each, with a button that opens the right system page and a check that it was granted.
3. Default modes created and their suggested apps shown for review (section 3.4).
4. Land on the home screen.

### 9.3 Settings

- **Free a mode** (section 7).
- **Permissions status** with a fix button for any that are missing.
- **Warning times:** minutes-left warnings (default 5 and 1).
- **History retention:** 7, 30 or 90 days (default 30).
- **Clear history.**
- **Export / import modes** as a JSON file, as a backup.

## 10. Edge cases

| Situation | Expected behaviour |
| --- | --- |
| Phone restarts during a run | SAR restarts on boot and resumes the run with minutes used so far. A run whose end time passed while off is closed. |
| Phone off when a schedule starts | On boot, if the window is still open, the scheduled mode starts for the remaining time. |
| Date, time or time zone changed | Run end times are fixed at start. A schedule is re-evaluated against the new clock. |
| Accessibility permission turned off during a run | Persistent notification and red banner: "Blocking is off". Usage keeps counting from usage access. |
| A controlled app is uninstalled | It is removed from all modes silently. |
| A controlled app is updated | Rules stay attached (matched by package name). |
| Allowance edited mid-run below minutes used | App is blocked immediately. |
| Allowance edited mid-run above minutes used | App unblocks if no other rule blocks it. |
| App opened from a notification, link or share sheet | Treated as opening the app; blocked the same way. |
| Split screen | The block screen takes over the whole screen; the other app must be reopened. |
| Free a mode during an emergency unlock | The run ends; the unlock no longer matters. |
| Schedule end time equals start time | Rejected by the editor. |

## 11. Non-functional requirements and technical approach

SAR is a native Kotlin app that detects app launches through an accessibility service and stores everything in a local database.

### 11.1 Non-functional requirements

| Area | Requirement |
| --- | --- |
| Block speed | Block screen visible within 1 second of a blocked app reaching the foreground. |
| Usage accuracy | Within 1 minute per app per run. |
| Battery | Under 1% of daily battery from SAR on a typical day; zero wake locks held by SAR; no CPU wake-ups while no mode is running (see 11.3). |
| Reliability | Survives reboot, app updates and Android killing background work. |
| Privacy | No network permission. No data leaves the phone except a user-initiated export. |
| Compatibility | Android 10 (API 29) and later; tested on the owner's phone. |

### 11.2 Technical approach

| Component | Built with | Responsibility |
| --- | --- | --- |
| UI | Kotlin, Jetpack Compose | Home, mode editor, app picker, history, settings, Free a mode. |
| App launch watcher | AccessibilityService | Reports each foreground app change to the rules engine. |
| Rules engine | Plain Kotlin | Decides allow / block / break for an app, given the active run and its usage. |
| Usage tracker | Foreground time from watcher events, checked against UsageStatsManager | Counts minutes per app per run. |
| Block screen | Full-screen activity launched over the blocked app | Shows mode message, image, reason and buttons. |
| Mode scheduler | AlarmManager exact alarms | Starts and ends scheduled runs and timed manual runs; re-registered on boot. |
| Run notification | Ongoing notification (no foreground service) | Shows the active run; the accessibility service already keeps SAR alive. |
| Storage | Room (SQLite) | Modes, rules, schedules, runs, usage, history. |

The rules engine is kept free of Android classes so it can be unit-tested on its own.

### 11.3 Battery and performance

SAR does work only when the foreground app changes or a mode starts or ends; between those moments it does nothing.

**Event-driven, never polling**

- The accessibility service listens only to window-state-changed events, does not read window content, and ignores every event while no mode is running.
- Usage is counted from timestamps (app came to front, app left front, screen off), not by a timer ticking every second.
- When a limited app comes to the front, SAR schedules **one** callback for the moment its allowance or session cap runs out, and cancels it when the app leaves the front or the screen turns off.
- `UsageStatsManager` is queried only when the user opens History or after a reboot, never on a loop.

**Scheduling**

- Exact alarms are set only for mode start and end times and timed-run ends. No periodic jobs.
- History clean-up runs when a mode run starts, not on its own schedule.
- Ignore battery optimisation is requested only so schedule alarms fire on time; it does not license any background work.

**Nothing left running**

- No foreground service, no wake locks, no network permission, no location, no sensors.
- When the screen turns off, counting stops and pending callbacks are cancelled.
- Database writes happen once per app switch and at run end, batched in one transaction.

**UI**

- Dark theme with true-black areas to save power on OLED screens.
- Glows and progress rings are static; animations run only while their screen is visible.
- Block-screen images are downscaled to screen size once, when chosen, and cached.
- Release builds use R8 shrinking and a baseline profile for fast start-up.

**How it is verified**

| Check | Tool | Pass |
| --- | --- | --- |
| Wake locks and wake-ups | `adb shell dumpsys batterystats` / Battery Historian over 24 h | 0 SAR wake locks; wake-ups only at mode boundaries |
| Battery share | Android battery settings over a normal day | Under 1% |
| Idle CPU | Android Studio profiler, no mode running | About 0% CPU, no SAR threads active |
| Block speed | Test log timestamps | Under 1 second |

## 12. Out of scope and decisions

**Out of scope for version 1:**

- Protection against uninstalling SAR or turning off its permissions. SAR never blocks its own screens or its uninstall. Uninstalling SAR deletes all modes, locks and history, and blocking stops at once; a reinstall starts fresh with the default modes.
- Website blocking inside browsers.
- Open-count limits ("open Instagram at most 5 times").
- Multiple users, cloud sync, Play Store release, parental control.
- Home-screen widgets and wearables.

**Decided (5 Nov 2026):**

- A locked manual run can be extended while it runs, never shortened.
- Emergency-time uses are counted per run.
- SAR does not block its own uninstall; uninstalling resets everything.
