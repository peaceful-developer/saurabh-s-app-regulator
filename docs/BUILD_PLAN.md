# SAR — Build Plan

Build SAR in 9 milestones. **One milestone per Claude Code session.** At the start of each milestone, enter plan mode, read the relevant FSD sections, and show Saurabh a short plan before writing code. At the end, run tests, install on the phone, and tell Saurabh exactly what to try.

Status: tick a milestone when Saurabh has confirmed it works on his phone.

- [ ] M1 Project setup, theme, database
- [ ] M2 Core logic (rules engine) with unit tests
- [ ] M3 App-launch detection and block screen
- [ ] M4 Home, Modes, Start sheet, Mode editor
- [ ] M5 Apps & rules picker and default modes
- [ ] M6 Schedules, locks, extend, collision warning, boot
- [ ] M7 Emergency time and Free a mode
- [ ] M8 History, Settings, Onboarding, notifications
- [ ] M9 Battery and performance audit, release build

---

## Architecture

```
app/src/main/java/com/saurabh/sar/
├── core/        Pure Kotlin. No android.* imports. Fully unit-tested.
│   ├── model/       Mode, Rule, ScheduleEntry, Run, AppUsage, Decision …
│   ├── engine/      RulesEngine, UsageLedger, RunStateMachine
│   ├── schedule/    ScheduleCalculator (next boundary, overlap check, overnight windows)
│   ├── challenge/   MathChallenge (question generator + checker, seedable Random)
│   └── Clock.kt     interface Clock { fun now(): Long }  ← inject everywhere
├── data/        Room database, DAOs, repositories, DataStore for settings
├── platform/    Android glue — kept thin
│   ├── SarAccessibilityService.kt   foreground-app events → engine
│   ├── BlockActivity.kt             block screen
│   ├── AlarmScheduler.kt            exact alarms for run start/end
│   ├── AlarmReceiver.kt / BootReceiver.kt
│   ├── Notifications.kt
│   └── InstalledApps.kt             launchable apps, icons, categories
├── ui/
│   ├── theme/       colours, type, shapes from docs/DESIGN.md
│   ├── components/  SarButton, AppRow, ModeCard, BottomNav, Chip, Segmented, Toggle, Ring …
│   └── screens/     home, modes, startsheet, editor, rules, block, free, history, settings, onboarding
└── SarApp.kt    Application; simple manual dependency container (no DI framework)
```

**Rules**

- `core/` decides everything (allow, block, when a limit will run out, what happens on collision). `platform/` only reports events and carries out decisions.
- All time comes from `Clock`; tests use a fake clock.
- Keep dependencies minimal: Compose (BOM), Room, DataStore, Kotlin coroutines, JUnit/Truth/Turbine for tests. Ask Saurabh before adding anything else. No image library (decode and downscale with `ImageDecoder` once), no analytics, no crash reporters, no networking.

## Data model (Room)

| Entity | Key fields |
| --- | --- |
| `Mode` | id, name, type (MANUAL/SCHEDULED), message, imagePath?, emergencyEnabled, emergencyMinutes, emergencyUses, emergencyEffort (WAIT/PHRASE), emergencyWaitSec, emergencyPhrase, sortOrder |
| `ScheduleEntry` | id, modeId, daysMask (Mon=bit0), startMinute, endMinute (end < start ⇒ overnight) |
| `AppRule` | modeId, packageName, type (BLOCK/LIMIT), allowanceMin?, capMin?, breakMin? |
| `Run` | id, modeId, startedAt, plannedEndAt?, locked, endedAt?, endReason (TIME_UP/STOPPED/FREED/SWITCHED/EXPIRED_WHILE_OFF), emergencyUsesUsed |
| `RunAppUsage` | runId, packageName, usedMs, stretchUsedMs, lastForegroundEndAt?, breakUntil?, emergencyUntil? |
| Settings (DataStore) | warningMinutes [5,1], historyRetentionDays 30, onboardingDone |

## Milestones

### M1 — Project setup, theme, database
FSD: 1, 11. DESIGN: Tokens.
- Gradle project, Kotlin, Compose, minSdk 29, target the latest stable SDK. Package `com.saurabh.sar`. R8 on for release.
- Bundle Orbitron and Exo 2 font files in `res/font`.
- `ui/theme`: colours, typography, shapes exactly from DESIGN.md. Dark only.
- Room database with the entities above; one migration-safe schema export.
- Placeholder Home screen with the bottom nav, showing the theme.
- **Done when:** app installs on the phone, opens to a themed placeholder, `./gradlew testDebugUnitTest` passes.

### M2 — Core logic with unit tests (TDD)
FSD: 4, 5, 6.2, 7.
- `UsageLedger`: foreground-start / foreground-end / screen-off events → used time per app per run; session stretch with the 2-minute gap rule.
- `RulesEngine.decide(app, run, usage, now)` → `Allow(untilAt?)` or `Block(reason: RULE_BLOCK | ALLOWANCE_USED | SESSION_BREAK(until))`. `untilAt` is the single moment to re-check (allowance or cap runs out, emergency window ends).
- `RunStateMachine`: start, startFor(duration), stop (unlocked only), extend (locked manual only, longer only), free, time-up, scheduled start, collision (pending scheduled mode, switch, start-when-current-ends, window-closed), boot recovery.
- `ScheduleCalculator`: next start/end for each mode, overnight windows, overlap detection for the editor.
- `MathChallenge`: 5 questions, + or −, operands 10–999, never negative, wrong answer ⇒ new set.
- **Tests to include:** every row of FSD 10 that is pure logic; allowance reset per run; extend never shortens and adds no allowance; emergency uses per run; editing allowance below used blocks immediately; overnight schedule across midnight and across Sunday→Monday.
- **Done when:** all core tests pass; no `android.*` import in `core/`.

### M3 — App-launch detection and block screen
FSD: 6.1, 9.1 (accessibility, overlay), 11.3.
- `SarAccessibilityService`: only `TYPE_WINDOW_STATE_CHANGED`, `canRetrieveWindowContent=false`, `notificationTimeout≈100ms`. Ignore events from SAR, the launcher, system UI and the keyboard. Early-return when no run is active.
- On a controlled app: ask the engine; if blocked, show the block screen; if allowed with `untilAt`, post **one** delayed callback (Handler) for that moment and cancel it on app switch or screen off.
- Screen on/off receiver registered inside the service.
- `BlockActivity` (or an accessibility overlay window — choose whichever reaches the screen faster and document the choice in DECISIONS.md): Figma 06. Go Home sends the user to the launcher. Back never returns to the blocked app.
- A debug-only "Start test mode" button to try it before the real UI exists.
- **Done when:** with a test mode blocking an app, opening it shows the block screen in under 1 s; a 1-minute allowance blocks the app while it is open.

### M4 — Home, Modes, Start sheet, Mode editor
FSD: 3, 5.1, 8.1. Figma 01, 02, 03, 04.
- Reusable components first (DESIGN: Components), then screens.
- Home: active-mode card with ring, stats, Extend; app list; no-mode-active state.
- Modes list, Start sheet (Start / Start for… with duration chips), Mode editor (name, type, schedule, message + image, links to rules and emergency settings), delete and duplicate.
- **Done when:** Saurabh can create a mode, start it both ways, see live time left, stop an unlocked run, and is prevented from stopping a locked one.

### M5 — Apps & rules picker and default modes
FSD: 3.4, 4. Figma 05.
- Installed launchable apps with icons, search, grouped by category; SAR, dialer and Settings excluded.
- Rule editing: Block/Limit, allowance stepper, session cap toggle with max/break.
- First-launch creation of the five default modes with category-based suggestions for Saurabh to confirm.
- Uninstalled apps removed from modes (package-removed receiver).
- **Done when:** default modes appear with sensible suggestions and rules take effect on the next app open.

### M6 — Schedules, locks, extend, collision, boot
FSD: 5.2–5.5, 10. 
- Exact alarms only at run boundaries (`setExactAndAllowWhileIdle`). Handle the exact-alarm permission on Android 12+ (sideloaded, so `USE_EXACT_ALARM` or the user grant are both acceptable — pick one and note it).
- Collision banner + notification with "Start <Mode>"; deferred start when the current run ends.
- Boot receiver: rebuild alarms, resume or close runs, start a scheduled mode whose window is still open.
- Time/zone change receiver.
- **Done when:** Office Work starts and ends on its own; the collision flow works both ways; a reboot mid-run resumes correctly.

### M7 — Emergency time and Free a mode
FSD: 6.2, 7. Figma 06, 07.
- Emergency button on the block screen → wait-timer or phrase screen (design in the same style; not in Figma) → app opens for N minutes → blocks again.
- Free a mode screen with keypad, progress segments, wrong-answer reset, confirmation, scheduled mode returns next time.
- **Done when:** both flows work on the phone and uses/limits match the mode settings.

### M8 — History, Settings, Onboarding, notifications
FSD: 8.2, 8.3, 9. Figma 08, 09, 10.
- History Today / 7 days, top apps, recent runs; retention clean-up when a run starts.
- Settings groups; Free a mode entry; export/import modes as JSON via the system file picker.
- Onboarding: one screen per permission with a live granted check; red banner when any permission is later revoked.
- Notifications: run status (ongoing, not a foreground service), 5-min and 1-min warnings scheduled as part of the single `untilAt` callback logic, collision.
- **Done when:** a fresh install walks through onboarding to a working Home.

### M9 — Battery and performance audit, release build
FSD: 11.1, 11.3.
- Run every check in FSD 11.3 "How it is verified" and write the results to `docs/BATTERY_REPORT.md`.
- Grep the code for forbidden patterns: `WakeLock`, `startForeground`, `PeriodicWorkRequest`, `while (true)`, `delay(` loops, `Timer(`, `INTERNET` permission.
- Baseline profile, R8, signed release APK for sideloading.
- **Done when:** all checks pass and Saurabh has the release APK installed.
