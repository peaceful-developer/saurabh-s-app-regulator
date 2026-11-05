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
