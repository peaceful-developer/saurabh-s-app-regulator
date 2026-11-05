# SAR — Design Spec

The visual design lives in Figma. This file gives everything needed to build it in Jetpack Compose even without Figma access.

**Figma file:** https://www.figma.com/design/iKNASnQXNzgJpyzP41Y2se
Page: `SAR · Screens`. If the Figma MCP server is connected, read a screen with its node ID below (for example `get_design_context` with node `6:76`). Treat Figma output as a reference to adapt to Compose, not code to paste.

## Look and feel

Dark "sci-fi console": near-black background, deep-navy cards, neon **cyan** for primary actions and progress, **violet** for locks and schedules, soft glows on the important element of each screen. Easy to use beats decorative: one main button per screen, large tap targets (≥ 48 dp), four bottom tabs.

## Screens

| # | Screen | Figma node | Link |
| --- | --- | --- | --- |
| 01 | Home | `6:76` | https://www.figma.com/design/iKNASnQXNzgJpyzP41Y2se?node-id=6-76 |
| 02 | Modes | `6:190` | https://www.figma.com/design/iKNASnQXNzgJpyzP41Y2se?node-id=6-190 |
| 03 | Start mode sheet | `6:303` | https://www.figma.com/design/iKNASnQXNzgJpyzP41Y2se?node-id=6-303 |
| 04 | Edit mode | `7:225` | https://www.figma.com/design/iKNASnQXNzgJpyzP41Y2se?node-id=7-225 |
| 05 | Apps & rules | `7:320` | https://www.figma.com/design/iKNASnQXNzgJpyzP41Y2se?node-id=7-320 |
| 06 | Block screen | `7:413` | https://www.figma.com/design/iKNASnQXNzgJpyzP41Y2se?node-id=7-413 |
| 07 | Free a mode | `8:259` | https://www.figma.com/design/iKNASnQXNzgJpyzP41Y2se?node-id=8-259 |
| 08 | History | `8:333` | https://www.figma.com/design/iKNASnQXNzgJpyzP41Y2se?node-id=8-333 |
| 09 | Settings | `8:443` | https://www.figma.com/design/iKNASnQXNzgJpyzP41Y2se?node-id=8-443 |
| 10 | Onboarding · permission | `8:552` | https://www.figma.com/design/iKNASnQXNzgJpyzP41Y2se?node-id=8-552 |
| — | Components board | `5:2` | https://www.figma.com/design/iKNASnQXNzgJpyzP41Y2se?node-id=5-2 |

Frames are 360 × 800 (Android baseline). Values below are in dp/sp.

## Tokens

### Colours (Figma variable collection `SAR Tokens`, mode `Dark`)

| Token | Hex | Use |
| --- | --- | --- |
| `bg/base` | `#05070F` | Screen background (near-black, OLED friendly) |
| `bg/surface` | `#0B1020` | Cards, rows, inputs, bottom nav |
| `bg/raised` | `#131B36` | Bottom sheets |
| `line/subtle` | `#1F2B52` | Borders, dividers, progress tracks |
| `accent/cyan` | `#22E5FF` | Primary actions, progress, active tab, selection |
| `accent/violet` | `#8B5CF6` | Locks, scheduled modes, app-letter tiles |
| `status/danger` | `#FF3D7F` | Blocked, destructive |
| `status/warn` | `#FFB020` | Near limit, emergency time |
| `status/ok` | `#2BFFA3` | Done, granted, correct |
| `text/primary` | `#E8EEFF` | Main text |
| `text/secondary` | `#8A97C2` | Labels, metadata |
| `text/on-accent` | `#04121A` | Text/icons on cyan |

Tinted fills use the accent at low alpha, e.g. cyan 6–14 % for selected cards and segments, violet 8–18 % for lock badges, danger 8–18 % for blocked tiles. Borders on tinted cards use the same colour at 35–70 %.

### Typography (bundle both fonts as app resources — no downloadable fonts, no network)

| Role | Font | Size | Notes |
| --- | --- | --- | --- |
| Logo "SAR" | Orbitron Bold | 24 | letter-spacing 12 %, cyan with glow |
| Screen title | Orbitron Bold | 24 | letter-spacing 8 %, ALL CAPS |
| Hero mode name | Orbitron Bold | 26 | ALL CAPS |
| Big numbers (timer, math question) | Orbitron Bold | 26–42 | |
| Section label | Orbitron Bold | 10 | letter-spacing 14 %, `text/secondary`, ALL CAPS |
| Button label | Orbitron Bold | 13 | letter-spacing 8 %, ALL CAPS |
| Chip / tag | Orbitron Bold | 9 | letter-spacing 10 % |
| Body | Exo 2 Regular/Medium | 13–15 | |
| Row title | Exo 2 SemiBold | 14–15 | |
| Block-screen message | Exo 2 SemiBold | 30 / line-height 36 | |

Both are Google Fonts under the SIL Open Font License.

### Shape, spacing, effects

- Screen side padding 24; vertical gap between blocks 14–16.
- Radius: buttons 14, cards 16–24, chips 8, inputs 14, keypad keys 16, bottom sheet top corners 28.
- Borders 1 dp.
- Glow = a coloured blur behind the element (cyan, radius 12–30, alpha 0.2–0.5). Use only on: primary button, active mode card, active tab icon, progress arcs/bars, selected chip. **Static only — never animate glows.**
- Background accent: one large blurred circle (cyan or violet, ~30 % alpha) in the top-right corner of each screen. Draw it as a pre-blurred drawable or a single static `Brush`; do not run a blur every frame.

### Icons

Line icons, 2 dp stroke, rounded caps (Lucide style, ISC licence). Convert the needed ones to vector drawables: home, layers (modes), bar-chart (history), sliders (settings), lock, unlock, play, clock, plus, minus, ban, zap, chevron-left/right, check, calendar, image, search, book-open, activity, wind, briefcase, sun, hourglass, eye, bell, battery, upload, download, x, delete (backspace), alert-triangle, trash.

App icons in lists use the real installed app icon. In Figma they are placeholder letter tiles.

## Components (Figma `Components` board)

| Component | Variants | Content |
| --- | --- | --- |
| Button | Primary (cyan fill, dark text, glow) · Ghost (cyan 6 % fill, cyan border) · Danger (danger 8 % fill, danger border) | Label; height 52; full width in most screens |
| App Row | Limit (cyan bar) · Warning (amber bar, ≥ 80 % used) · Blocked (danger border and bar) | App icon, name, "14 / 20 MIN" or "BLOCKED", 6 dp progress bar |
| Mode Card | Idle (tag START) · Running (cyan tint + border + glow, tag RUNNING) · Scheduled (violet tag AUTO) | Icon tile 48, name (Orbitron 14), meta line, tag chip |
| Bottom Nav | Active = Home · Modes · History · Settings | 76 high; active tab gets cyan 10 % pill and glowing icon |

## Screen notes

**01 Home.** Header with SAR logo and bell. Active-mode card: "ACTIVE MODE" label, LOCKED badge (violet), mode name, progress ring (116, 12 % stroke) with time left in the centre, stats column (ENDS AT, STARTED, EMERGENCY n LEFT in amber), Ghost button "EXTEND +15 MIN" (only for locked manual runs). "APPS IN THIS MODE" list of App Rows. Bottom nav. Also needed (not drawn): no-mode-active state, and an "Up next" strip when space allows.

**02 Modes.** Title + subtitle "Tap to start · hold to edit", cyan "+" button (48). Two groups: MANUAL and SCHEDULED, each a list of Mode Cards.

**03 Start sheet.** Bottom sheet over a dimmed screen. Header with mode icon and name. Two selectable option cards: START (runs until you stop it) and START FOR… (locked). The selected one has cyan border and a filled check. Duration chips (30 MIN, 45 MIN, 1 HR, 1.5 HR, 2 HR, CUSTOM) wrap; selected chip is solid cyan. Hint line "Ends at 18:45 · can be extended, never shortened". Primary button "LOCK IN · 1 HOUR".

**04 Edit mode.** Top bar: back, title + mode name, SAVE (cyan text). Fields: NAME input; TYPE segmented control (MANUAL / SCHEDULED); SCHEDULE card with 7 day circles (selected = solid cyan) and START → END time boxes, "+ ADD ANOTHER TIME"; BLOCK SCREEN row with image thumbnail and message; navigation rows "Apps & rules" and "Emergency time" with summaries and chevrons.

**05 Apps & rules.** Top bar, search field. Expanded rule card for the selected app: checkbox, icon, name, BLOCK / LIMIT segmented control, "Allowed time" stepper (− 15 MIN +), "Session cap" toggle with one-line summary. Collapsed rows for other apps with a checkbox and a chip (BLOCK in danger, LIMIT · 15M in violet); unticked apps at 60 % opacity. Primary button "DONE · 4 APPS" pinned at the bottom.

**06 Block screen.** Full-bleed mode image in the top ~45 % (Figma uses a planet illustration as a stand-in for the user's chosen image), fading into the background. Mode chip "SELF STUDY · LOCKED" at top-left. App line: icon, name, BLOCKED chip. Message in large type, sub-line in secondary. Info card with USED and UNLOCKS AT. Primary "GO HOME", Ghost "EMERGENCY TIME · 2 LEFT". No status of the blocked app is ever visible behind it.

**07 Free a mode.** Top bar. Violet notice card "Ending Self Study early · Locked until 19:00 · solve 5 in a row". Progress: 5 segments (green = correct, cyan = current, grey = to do) with "QUESTION 3 OF 5" and "2 CORRECT". Question "347 − 158" (Orbitron 42) and "= ?". Answer field with cyan border and caret. 3 × 4 keypad (keys 56 high): digits, backspace, and a cyan ✓ key. Footnote "A wrong answer starts a new set of 5."

**08 History.** Title with TODAY / 7 DAYS segmented control. Weekly chart card: total, change vs last week (green), 7 bars (violet 55 %, today in cyan with glow). TOP APPS (App Rows with durations). RECENT RUNS rows with a TIME UP / STOPPED / FREED / SWITCHED chip.

**09 Settings.** "FREE A MODE" card at the top (violet tint, shows which mode is locked and until when; disabled look when nothing is locked). Grouped lists: PERMISSIONS (status in green or red), ALERTS, HISTORY (retention, Clear history in danger), BACKUP (Export, Import).

**10 Onboarding · permission.** 5-segment progress, "STEP 2 OF 5", concentric-ring illustration with the permission icon, title, one-paragraph plain explanation, checklist of all five permissions (done / current / to do), Primary "OPEN SETTINGS", text link "WHY IS THIS NEEDED?". One such screen per permission.

## Not yet designed (build in the same style)

- Emergency-time unlock: wait-timer countdown ring, or type-the-phrase field.
- Collision warning: in-app banner "Office Work is scheduled now" with START button, plus the notification.
- Home with no mode active; empty states; delete-mode confirmation; "End mode now?" confirmation after Free a mode.
- Session-cap break variant of the block screen (countdown instead of "unlocks at").
