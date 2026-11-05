# SAR — Saurabh's App Regulator

Personal Android app (sideloaded, one user) that limits how long chosen apps can be used while a **mode** is active (Self Study, Gym, Office Work…) and blocks them when the limit is reached. Built with Kotlin + Jetpack Compose.

## Read before working

1. `docs/FSD.md` — what the app must do. **Source of truth.** If code and FSD disagree, the FSD wins; if the FSD seems wrong or unclear, ask Saurabh instead of guessing.
2. `docs/BUILD_PLAN.md` — milestones, architecture, data model. Work on the next unticked milestone.
3. `docs/DESIGN.md` — tokens, components, screen notes, Figma node IDs. Figma file: https://www.figma.com/design/iKNASnQXNzgJpyzP41Y2se
4. `docs/DECISIONS.md` — why things are the way they are. Do not undo a decision without asking.

## How to work with Saurabh

- **One milestone per session.** Start in plan mode: summarise what you'll build, which FSD sections it covers, and anything unclear. Wait for a yes.
- Write tests first for anything in `core/`.
- When a milestone is done: run unit tests, build, install on the phone, and give Saurabh a short checklist of what to try on the device. Tick the milestone in `BUILD_PLAN.md` only after he confirms.
- New decisions or deviations go at the bottom of `docs/DECISIONS.md` with the date.
- Explain in plain language; Saurabh wants to understand what was built, not every line.

## Architecture rules

- `core/` is pure Kotlin: **no `android.*` imports**. All logic (allow/block, allowance, session cap, run lifecycle, schedules, collisions, math challenge) lives there and is unit-tested.
- `platform/` (accessibility service, alarms, receivers, notifications, block screen) only reports events to `core/` and executes its decisions.
- Inject a `Clock` everywhere; never call `System.currentTimeMillis()` inside `core/`.
- Minimal dependencies: Compose, Room, DataStore, coroutines, test libs. **Ask before adding any other library.**

## Battery rules — hard requirements (FSD 11.3)

SAR must use < 1 % battery per day and do nothing between events.

- **Never poll.** No loops, no repeating timers, no `delay()` loops, no periodic WorkManager jobs.
- **No foreground service. No wake locks.** The accessibility service keeps the process alive.
- **No `INTERNET` permission**, no analytics, no crash reporting SDKs, no location, no sensors.
- Accessibility service: `TYPE_WINDOW_STATE_CHANGED` only, `canRetrieveWindowContent=false`, return immediately when no mode is running.
- Count usage from foreground start/end timestamps. For a limited app in the foreground, schedule **one** callback at the moment its limit runs out; cancel it on app switch or screen off.
- Exact alarms only at run start/end boundaries.
- DB writes once per app switch / run end, in one transaction.
- UI: static glows (never animated), animations only while visible, images downscaled once and cached.

## Product rules that are easy to get wrong

- Only one mode runs at a time. Allowance and emergency uses reset at the **start of every run**.
- Locked runs (timed manual or scheduled) can't be stopped — only extended (never shortened, no extra allowance) or ended via **Free a mode** (5 correct sums in a row).
- SAR never blocks itself, the dialer or Settings.
- Collision: a due scheduled mode only takes over if the user taps the warning; otherwise it starts when the current run ends, if its window is still open.

## Commands

```bash
./gradlew assembleDebug          # build
./gradlew installDebug           # install on the connected phone (USB debugging on)
./gradlew testDebugUnitTest      # unit tests
adb logcat -s SAR                # app logs (use tag "SAR")
adb shell dumpsys batterystats --reset   # before a battery test
adb shell dumpsys batterystats com.saurabh.sar   # after
```
