# SAR — Saurabh's App Regulator

Personal Android app that limits how long chosen apps can be used while a **mode** (Self Study, Gym, Office Work…) is running, and blocks them when time is up. Kotlin + Jetpack Compose, Android 10+.

Specs live in [`docs/`](docs): [FSD](docs/FSD.md) (what it does), [BUILD_PLAN](docs/BUILD_PLAN.md), [DESIGN](docs/DESIGN.md), [DECISIONS](docs/DECISIONS.md). Working rules for Claude Code are in [CLAUDE.md](CLAUDE.md).

## Get the APK

Every push builds an APK on GitHub Actions. Open the repo's **Releases → `sar-latest`** on the phone and install `SAR-<n>.apk` (allow "Install unknown apps" for your browser or Files app the first time). Later builds install over the old one and keep your modes.

## First run on the phone

1. Open SAR → **Get started** and grant the five permissions, one screen each.
2. **Accessibility greyed out?** Android blocks it for sideloaded apps until you allow it: *Settings → Apps → SAR → ⋮ → Allow restricted settings*, then turn on *Settings → Accessibility → Installed apps → SAR*.
3. On Samsung, also set *Settings → Apps → SAR → Battery → Unrestricted* so schedules fire on time.
4. Review the five default modes and their suggested apps, then **Create these modes**.

## Build locally (optional)

```bash
./gradlew :core:test testDebugUnitTest   # unit tests
./gradlew assembleRelease                # app/build/outputs/apk/release/app-release.apk
./gradlew installDebug                   # install over USB
./gradlew connectedDebugAndroidTest      # database tests on the phone
adb logcat -s SAR                        # logs
```

## Layout

```
core/   pure Kotlin rules engine (no Android), fully unit-tested
app/    data (Room, DataStore), platform (accessibility, alarms, block screen), ui (Compose)
```
