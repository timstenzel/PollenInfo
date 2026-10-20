# Tasks: Redesign ("Meadow")

Requirements: [requirements.md](requirements.md) · Design: Figma *PollenInfo Redesign*
(<https://www.figma.com/design/naeELzkiFSdMuXsnmZVKnW>, page "Screens" in usage order, light row on
top, dark below; page "Direction & Foundations" for tokens and type).

Notes that apply to every task:

- **Visual only.** No ViewModel, UI-state, use-case, repository, API or navigation-graph change, and
  no existing test changes except `SeverityColorsTest` for the one deliberately changed colour. The
  only new strings are the five tab overlines (task 02, decided with the maintainer on 2026-10-19),
  the gauge overline `home_gauge_overline` and the tile's `reading_not_reported` (task 04, decided
  with the maintainer on 2026-10-20), the list heading `all_stations_list_heading` (task 05, decided
  with the maintainer on 2026-10-20), the chart legend `diary_feeling_legend` (task 06, decided with
  the maintainer on 2026-10-20, who also kept the feeling words on the chart's right axis) and the
  alarm count in task 07 plus the card's `alarm_paused_label` and the minimum tags
  `alarm_tag_minimum` ("from %1$s") / `alarm_tag_any` (task 07, decided with the maintainer on
  2026-10-20). If a screen seems to need a
  state change or another string, stop and ask.
- **Decisions taken while verifying the annex (2026-10-19):**
  - Home's feeling card moves **inline** (between the gauge card and the pollen tiles), as in Figma.
    Its rules (when it shows, answers, "Not today", save error) are unchanged; the "list end grows by
    the floating card's measured height" mechanism is removed (task 04).
  - Home keeps showing the **real station name in every state**, loading included; only the readings
    become skeletons (the Figma skeleton's grey title block is not followed).
  - Map lakes become a **flat fill only**; the separate lake edge colour (`mapWaterEdge*`) is removed
    (task 01).
  - Haze 2.0.1 is built against Compose 1.12.1 / Kotlin 2.4.20 / lifecycle 2.11.0, compatible with the
    project's 1.12.2 / 2.4.21 / 2.11.0.
  - The glass **action bar** is used by exactly three screens: Onboarding (Continue), Change station
    (Save) and the alarm editor (Save). The account screen keeps its primary button in the form.
- **Branch:** `feature/redesign` already exists locally (no upstream, no own commits, based on the
  current `develop`). CLAUDE.md says to stop and ask when the branch exists — confirm with the
  maintainer before the first commit. The specification (`.sdd/redesign/`) is its own first commit,
  before task 01's commit.
- **Every colour change is re-checked by the palette contrast test** (task 01) — add each new
  foreground/background pairing a task introduces to its table.
- **Compiler warnings fail the build** (`allWarningsAsErrors`), so every Gradle gate below also proves
  there is no new warning; additionally no new Gradle deprecation may appear in the output.
- **Runtime checks** run on an Android emulator against the local backend (`docker compose -f
  deploy/compose.dev.yaml up -d`, `./gradlew :server:run`); on API 37 grant
  `ACCESS_LOCAL_NETWORK` after each fresh install. Check light and dark and compare with the Figma
  frame of the same name. iOS is compile-verified only (no `iosApp`).
- **Languages:** every screen task opens its screens in German too (the longest texts) and confirms
  nothing is clipped; the full four-language pass is in task 11.
- **CLAUDE.md is kept in step** by the task that changes a rule it states.

## Task [01-meadow-theme-and-fonts]

The whole app switches to the Meadow foundation: Meadow light and dark colour schemes on both
platforms (Android no longer adopts wallpaper colours), the extended Meadow tokens (wash, glass,
pollen gold, warning, map land), Fraunces and Plus Jakarta Sans bundled and wired into the type
scale, the new shape scale, the darker light-theme Moderate and flat map lakes. Every existing screen
immediately renders in the new colours and body font, still with its current layout. A pure contrast
check guards all palette pairings from now on.

### Implementation steps

- [x] Confirm the existing `feature/redesign` branch with the maintainer; commit the specification (`.sdd/redesign/`) as the first commit and push
- [x] Replace the light and dark `ColorScheme` values with the Meadow mapping from the annex, each value with its contrast noted beside it
- [x] Add the extended Meadow tokens (data class + composition local provided by `PollenInfoTheme`) for wash, glass, accent/onAccent, warningContainer/onWarningContainer and map land
- [x] Remove dynamic colour from the Android theme so both platforms use the Meadow schemes
- [x] Change `severityModerateLight` to `#A07400`, update its recorded ratio, and update `SeverityColorsTest` for this value only
- [x] Replace the map water fill with the Meadow lake colour and remove the lake edge colour and its drawing
- [x] Add the font files and their OFL licence texts as Compose resources in `:theme`; build the typography from them with the annex's type scale; add the shape scale
- [x] Add the pure WCAG contrast function and the palette contrast test with every pairing listed in the annex's testing decisions
- [x] Update CLAUDE.md "Colours and bars" (palettes, extended tokens, no dynamic colour, flat lakes, contrast test replaces the manual validator) and the README tech stack (fonts)

### Acceptance criteria

- [x] Automated test: every severity colour reaches ≥ 3:1 against surface, raised, sunken, background, wash and map land, in both schemes
- [x] Automated test: every species colour reaches ≥ 3.5:1 against the chart card surface, and primary/secondary text, on-primary, on-accent and on-warning pairs reach ≥ 4.5:1, in both schemes
- [x] Automated test: the contrast function returns 21 for black on white and 1 for identical colours
- [x] Runtime (Android 12+ emulator with a non-green wallpaper): Home shows the Meadow forest green and cream background in light mode and the Meadow dark colours in dark mode, never wallpaper colours
- [x] Runtime: body text on Home, Settings and the alarm editor renders in Plus Jakarta Sans; the All stations map shows lakes as a flat fill without an edge line
- [x] Repository: an OFL licence text sits beside each of the two font families' files

### Quality gates

- [x] `./gradlew :composeApp:testAndroidHostTest` passes (all existing tests unchanged except `SeverityColorsTest`'s Moderate light value)
- [x] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [x] `./gradlew :androidApp:lintRelease` passes
- [x] New test names are backtick sentences without commas

## Task [02-glass-tab-bar-and-tab-screen-frame]

The five tab screens lose their title bars and gain the Meadow frame: soft background washes, a small
overline and a large serif title at the top of the scrolling content, and the glass bottom bar with
rounded upper corners whose content scrolls visibly (blurred) behind it. The selected tab is a green
pill; tabs, their order, names and navigation rules are unchanged. Everything that sits at the bottom
of a tab (lists' ends, Home's floating feeling card, Alarms' create button and snackbar, Settings'
snackbar) is lifted by the bar height so nothing hides behind the bar. Screen contents are otherwise
untouched.

### Implementation steps

- [x] Add Haze (pinned in the version catalog) and mark the tab host content as the blur source
- [x] Replace the Material navigation bar with the glass bar driven by `TopLevelDestination` (same descriptions, selection semantics and `navigateToTab`), with Haze blur and its fallback tint
- [x] Stop padding the tab host for the bar; expose the bar height to screens and add it to each tab screen's scrolling end padding and to Home's floating feeling card, Alarms' create button and snackbar host, and Settings' snackbar host
- [x] Add the shared tab-screen frame: background with decorative washes, header (overline + serif title, announced as heading), content slot
- [x] Move Home, All stations, Diary, Alarms and Settings onto the frame and remove their `TopAppBar`s; Home's header shows the station name in every state
- [x] Update CLAUDE.md "Navigation" (glass bar, insets now drawn behind the bar, bottom padding rule)

### Acceptance criteria

- [x] Automated test: `TopLevelDestinationTest` still passes unchanged
- [x] Runtime: on every tab there is no title bar; overline and serif (Fraunces) title sit at the top of the content, and Home shows the station name while loading, with content and on error
- [x] Runtime: the bar has rounded upper corners, shows the same five icons in the same order, and the current tab sits on a green pill
- [ ] ~~Runtime: scrolling Home and All stations shows the content blurred behind the bar, and the last item scrolls fully above the bar; Home's feeling card, Alarms' "Create alarm" and the Alarms and Settings snackbars (triggered with the backend stopped) appear fully above the bar~~ *(skipped in part: the Settings snackbar only appears when the crash-report preference write fails, which the backend cannot trigger; it uses the same `LocalBottomBarPadding` lift as the Alarms one. Everything else passed on API 37: blur behind the bar, last row of Home and All stations above it, feeling card, "Create alarm" and the Alarms snackbar)*
- [x] Runtime (Android 11 emulator): the bar is see-through without blur; a screenshot with Home's gauge card behind the bar shows every icon and the selected pill distinct from the content *(the gauge card arrives in task 04; checked with Home's species rows behind the bar at font scale 1.3)*
- [ ] ~~Runtime: switching tabs, reselecting the current tab and the back gesture behave as before; TalkBack announces each tab by its existing name and the title as a heading~~ *(skipped in part: TalkBack's speech could not be captured on the emulator, so the title's heading announcement is unverified (the semantics are set in `ScreenHeader`). Passed: tab switching, reselect without reload, back to Home and out of the app; the accessibility tree names the tabs Home, All stations, Diary, Alarms, Settings with the current one selected)*

### Quality gates

- [x] `./gradlew :composeApp:testAndroidHostTest` passes
- [x] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [x] `./gradlew :androidApp:lintRelease` passes
- [x] After `./gradlew :androidApp:assembleDebug :androidApp:assembleRelease -Ppolleninfo.uploadCrashlyticsMapping=false`, the two merged-manifest `grep`s from CLAUDE.md match only the debug manifest

## Task [03-non-tab-frame-and-dialogs]

Screens that are not tabs (onboarding, change station, alarm editor, account) lose their title bars
and gain the Meadow non-tab frame: a floating round glass back button (where they had a back arrow),
the overline + serif title in the content, a top scrim when content scrolls under the status bar, and
— for onboarding, change station and the alarm editor — a glass action bar at the bottom hosting their
main button in the new pill style. All confirmation dialogs take the Meadow dialog style: centred icon
badge, serif title, centred text, stacked full-width buttons with the destructive action in red.

### Implementation steps

- [x] Add the non-tab frame: back button (existing back description, 48 dp target), header, top scrim, optional glass action bar with rounded upper corners
- [x] Add the pill button variants (Primary, Tonal, Outline, Text, Danger, Accent) as a shared component
- [x] Move Onboarding, Change station, Alarm editor and Account onto the frame and remove their `TopAppBar`s; Onboarding's Continue and Change station's / the editor's Save move into the action bar
- [x] Restyle the four confirmation dialogs (discard changes, delete alarm, log out, delete account with its password field) as one shared dialog style
- [x] Update CLAUDE.md with the non-tab frame (back button, action bar, which screens use it) and the button variants

### Acceptance criteria

- [ ] ~~Runtime: Change station, alarm editor and account show a round back button top-left that behaves as the old back arrow (including the editor's discard dialog when dirty); onboarding has none; TalkBack announces it as "Back"~~ *(skipped in part: TalkBack's speech could not be captured on the emulator. Passed on API 37: the round glass button sits top-left on all three screens, leaves Change station without saving and the account screen without logging in, opens the editor's discard dialog when dirty (button and system back alike); onboarding has none; the accessibility tree has it as one focusable clickable node labelled "Back")*
- [x] Runtime: onboarding's "Continue" and change station's / the editor's "Save" sit in a glass bar with rounded upper corners at the bottom; long editor content scrolls behind it and fully above it; the account screen has no action bar
- [x] Runtime: the discard dialog shows a centred icon, "Discard changes?", "Discard" in red and "Keep editing" stacked full width; the delete-alarm, log-out and delete-account dialogs follow the same layout, the last with its password field
- [x] Runtime: titles of the four screens render in the serif font (Fraunces)

### Quality gates

- [x] `./gradlew :composeApp:testAndroidHostTest` passes
- [x] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [x] `./gradlew :androidApp:lintRelease` and `./gradlew :composeApp:checkTranslations` pass
- [x] The hard-coded-text `grep` from CLAUDE.md finds no new hits

## Task [04-home-redesign]

Home becomes the drawn hero screen in all its states: the severity gauge (four arc segments, worst
severity as a large word, "Driven by …", reading-time pill and "Refreshed …"), the inline green
feeling card with four labelled face buttons and "Not today", and the two-column grid of pollen-type
tiles (icon, concentration and unit, name, level word, bar; "No data" for unreported types), worst
first. An outdated reading shows the warning banner and a warning-coloured time pill; refreshing shows
the pull indicator. Loading shows the skeleton with shimmer; an error shows the illustrated status
card with the existing wording and "Retry". The shared skeleton/shimmer, status card, banner and
species tile are introduced here.

### Implementation steps

- [x] Add the pure gauge filling rule and its test; add the gauge composable (decorative, glow behind)
- [x] Add the species tile (replacing the row on Home) with the same semantics as the current row; restyle the severity bar track
- [x] Restyle the feeling prompt as the inline green card with face buttons; remove the floating overlay and its padding mechanism; keep its save-error display
- [x] Restyle reading age: time pill and stale banner in warning colours; keep "Refreshed …"
- [x] Add the skeleton + shimmer, status card and banner components; use them for Home's loading and error states
- [x] Update CLAUDE.md "Screen behaviour → Home" (inline card, padding sentence) and "Colours and bars" (gauge; fill stops unchanged)

### Acceptance criteria

- [x] Automated test: the gauge fills 0 segments for no reading and None, 1 for Low, 2 for Moderate, 3 for High, 4 for Very high, never decreasing with severity
- [x] Runtime: a High reading driven by Birch shows "High" with three filled segments and "Driven by Birch"; a None reading shows "None" with no filled segment; all seven types appear as tiles worst first, an unreported type shows "No data" with an empty bar
- [x] Runtime: with the feeling question due and logged in, the green card appears inline with face buttons labelled Very good, Good, Bad, Very bad and "Not today"; answering and "Not today" behave as before; logged out, a tap opens the account screen as before
- [x] Runtime: a reading older than three hours shows "These readings are not current." and the time pill in warning colours; loading shows the skeleton with a moving shimmer and the station name; a backend outage shows the status card with "Retry", which reloads
- [ ] ~~Runtime: TalkBack announces station, severity, driving type, reading time and each tile as before~~ *(skipped in part: TalkBack's speech could not be captured on the emulator. Passed on API 37 through the accessibility tree: the station name (heading), the severity word, "Driven by Birch", "Data from 19:00" and "Refreshed 20:25" are text nodes in that order; each face and "Not today" is one clickable node with its word; each tile is one node announced name, severity word, concentration ("Birch, High, 412"; "Beech, No data"), the order the rows had)*

### Quality gates

- [x] `./gradlew :composeApp:testAndroidHostTest` passes (including `SeverityBarTest` and `HomeViewModelTest` unchanged)
- [x] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [x] `./gradlew :androidApp:lintRelease` and `./gradlew :composeApp:checkTranslations` pass
- [x] The hard-coded-text `grep` from CLAUDE.md finds no new hits; new test names are backtick sentences without commas

## Task [05-all-stations-redesign]

All stations shows the map card (Meadow land fill, flat lakes, one dot per station in its severity
colour with a surface ring, the selected station enlarged with a halo and a "Name · Level" label) and
the legend naming all five levels, followed by the station rows (severity dot, name, "level · driving
type", reserved stale-icon slot, expand affordance). Loading and unavailable rows show a grey dot with
"Loading…" / "No reading"; an expanded row shows that station's readings as species tiles. Loading
shows the skeleton; total failure shows the status card.

### Implementation steps

- [x] Restyle the map: land fill, dots with ring, selected halo and "Name · Level" label; keep the single semantics node and tap behaviour
- [x] Add the legend row (dot + level word for each severity)
- [x] Restyle station rows and their loading / no-reading / stale variants; use species tiles in the expanded detail
- [x] Use the skeleton for loading and the status card for the full-screen error

### Acceptance criteria

- [x] Runtime: the map shows 15 coloured dots and the legend with None, Low, Moderate, High, Very high; selecting Zürich enlarges its dot with a halo and the label "Zürich · <its level>" and scrolls the list to it *(API 37, light and dark, English and German: 15 dots, legend None/Low/Moderate/High/Very high (German on one line too), tapping the Zürich dot shows "Zürich · None" over an 18 dp dot in a halo and scrolls its expanded row into view; a station near the western edge (Lausanne) keeps its tag inside the map)*
- [x] Runtime: during a refresh or with a station unavailable, affected rows show a grey dot and "Loading…" or "No reading"; stale rows keep the warning icon *(rows are pending only on the first load — a refresh keeps the old readings until its round completes, unchanged behaviour. Checked with a temporary, reverted delay and a forced Buchs failure in `GetAllStationReadingsUseCase`: every row "Loading…" with a grey ring and grey map dots, then Buchs "No reading" with a grey ring and a hollow map dot; Buchs, Luzern and Neuchâtel (outdated on the real backend) show the warning icon and "None · Reading not current")*
- [x] Runtime: expanding a row shows its readings as tiles; with every station failing, the status card with "No station's reading could be loaded…" and "Retry" is shown *(expanded Zürich: reading pill, "All species" heading and the two-column tile grid in the sage selected card; pull-to-refresh in airplane mode gave the card "The stations could not be loaded." / "No station's reading could be loaded. Please try again later." / Retry, which reloaded once the network was back)*
- [ ] ~~Runtime: TalkBack reads the map's single description as before~~ *(skipped in part: TalkBack's speech could not be captured on the emulator. Passed through the accessibility tree: the map (tag included) is one node described "Karte mit 15 Pollenmessstationen. Wähle eine Station in der Liste unten.", no node per dot; the legend is silent; each row header is one node)*

### Quality gates

- [x] `./gradlew :composeApp:testAndroidHostTest` passes (including `SwissMapProjectionTest`, `StationHitTestTest`, `SwissBorderTest`, `SwissLakesTest` and `AllStationsViewModelTest` unchanged)
- [x] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [x] `./gradlew :androidApp:lintRelease` and `./gradlew :composeApp:checkTranslations` pass
- [x] The hard-coded-text `grep` from CLAUDE.md finds no new hits

## Task [06-diary-redesign]

The Diary shows the signed-out gate as an illustrated card (account explanation, four points with
check badges, "Create account" and "Log in"), and when signed in the station dropdown with the
Week / Month / Year segmented toggle, the chart in a rounded card (feeling as a dashed primary line
with dots, species lines in their colours, severity words on the axis, the date range as card title),
the pollen types as tick chips in their line colour ("Not measured here" dimmed) and the disclaimer.
Loading shows the skeleton; errors show the status card. The segmented toggle and the Meadow dropdown
field are introduced here.

### Implementation steps

- [x] Restyle the gate using the status card pattern with the four points
- [x] Add the segmented toggle component and use it for the range; add the Meadow dropdown field style and use it for the station
- [x] Restyle the chart card and the feeling line (dashed, with points); keep the pure chart geometry and description unchanged
- [x] Restyle species chips (checked fill in the line colour, unmeasured dimmed with its caption)
- [x] Use skeleton and status card for loading and error

### Acceptance criteria

- [x] Runtime: logged out, the gate card shows the four points and both buttons, which open the account screen in the right mode *(API 37, light: emblem with gold grains, title, intro, four points with check badges; "Create account" opened the account screen titled "Create account" with the repeat-password field and consent, "Log in" the one titled "Log in")*
- [x] Runtime: logged in with a week of entries, the chart card shows severity words on the vertical axis, one coloured line per checked type, and the feeling as a dashed line with dots that never bridges a missing day *(test account with answers 13–19 Oct except 17 Oct: Week card "13 Oct – 19 Oct" with "Your feeling" legend, None…Very high on the left, feeling words on the right, weekdays Tue…Mon below; dashed primary line Tue–Fri, nothing across Saturday, then Sun–Mon; light and dark, English and German ("13. Okt. – 19. Okt.", "Dein Befinden"), nothing clipped. Found and fixed while checking: the first date label could collide with the second or be dropped)*
- [x] Runtime: switching Week / Month / Year and the station reloads as before; unchecking a species chip hides its line; an unmeasured type's chip is dimmed with "Not measured here" *(Week, Month ("20 Sep – 19 Oct") and Year ("20 Oct 2025 – 19 Oct 2026") each reloaded; unchecking Grasses hid its line; choosing Neuchâtel (no values this week) from the dropdown reloaded with all seven chips dimmed, disabled and captioned "Not measured here", the home station untouched)*
- [x] Runtime: a server outage shows the status card with "The pollen history could not be loaded." and "Retry" *(airplane mode then a range change: the warning card with that title, "Couldn’t reach the server…" and Retry, which reloaded once the network was back. Loading checked with a temporary, reverted 4 s delay: the skeleton with the moving shimmer under the real header)*

### Quality gates

- [x] `./gradlew :composeApp:testAndroidHostTest` passes (including `DiaryChartGeometryTest` and `DiaryViewModelTest` unchanged)
- [x] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [x] `./gradlew :androidApp:lintRelease` and `./gradlew :composeApp:checkTranslations` pass
- [x] The hard-coded-text `grep` from CLAUDE.md finds no new hits

## Task [07-alarms-list-redesign]

The Alarms tab shows each alarm as a card (type icon tile, summary, station, weekday circles, the
types and minimum as tags, switch; paused cards dimmed with "Paused"), the "n of 10" count, and the
gold extended "Create alarm" button floating above the bar. The notifications-off and no-alarms states
become illustrated status cards with their buttons. A failed switch flips back with the Meadow
snackbar (inverse colours), which pushes the create button up. Loading shows the skeleton; load
errors show the status card.

### Implementation steps

- [x] Add the one new string for the count ("%1$d of %2$d", `alarm_` prefix, positional placeholders) in all four languages *(`alarm_count`; with the maintainer's agreement also `alarm_paused_label`, `alarm_tag_minimum`, `alarm_tag_any`)*
- [x] Restyle the alarm card from the existing summary data (no new formatting rules) *(`AlarmCard`; `AlarmSummary` exposes `typeSummary` and `speciesSummary`, which `summaryOf` now composes, unchanged in output)*
- [x] Add the count chip and the gold extended create button above the glass bar
- [x] Restyle the permission gate (both variants) and the empty state as status cards *(`StatusCard` gains `grains`, so the warning-toned gate has Figma's grains)*
- [x] Restyle the toggle-failed snackbar (inverse surface) and its placement above the bar and below the create button *(one floating column instead of `Scaffold`)*
- [x] Use skeleton and status card for loading and error *(`AlarmsSkeleton`)*

### Acceptance criteria

- [x] Runtime: three alarms (one paused) show as cards with summaries, stations, weekday circles, tags and switches; the paused one is dimmed and marked "Paused"; "3 of 10" is shown (and its German, French and Italian translations) *(API 37: Bern daily 08:00 every day, Basel threshold 07:00–21:00 Mon–Fri "Birch"/"from High", Lugano daily 18:00 paused Sat–Sun "Grasses"/"from Moderate" with "PAUSED", grey day circles and dimmed fill; "3 of 10", "3 von 10", "3 sur 10", "3 di 10"; light and dark, nothing clipped in any language. Found while checking: Figma's 82 % opacity on the whole paused card takes its text to 4.24:1 — only the fill and the type tile are dimmed, pinned by `PaletteContrastTest`)*
- [x] Runtime: with notifications denied, the card shows "Notifications are disabled" and the request or open-settings button as before; with no alarms, "No alarms yet" with the gold "Create alarm" *(permission revoked: "Allow notifications"; after two denials "Open settings" with the settings wording; alarms deleted on the dev backend: neutral card with grains and the gold button, no count chip — alarms restored afterwards. Load error (offline): warning card "Your alarms could not be loaded." with Retry, which reloaded. Loading checked with a temporary, reverted 4 s delay: three shimmering skeleton cards)*
- [x] Runtime: with the backend stopped, flipping a switch flips it back and the snackbar appears above the bar with the create button above it *(server unreachable via the emulator's airplane mode instead of stopping the backend: Basel's switch flipped back, "Couldn’t pause the alarm. Couldn’t reach the server. …" in the dark inverse snackbar 20 dp from the edges above the bar, "Create alarm" above it. Found while checking: Material's snackbar adds a 12 dp margin of its own, now subtracted)*
- [x] Runtime: TalkBack announces each switch as "Alarm for …" as before *(checked in the accessibility node tree (`uiautomator dump`), not by listening to TalkBack: switches "Alarm for Bern" / "Alarm for Basel" / "Alarm for Lugano"; the weekday rows read as "Every day", "Mon–Fri", "Sat, Sun")*

### Quality gates

- [x] `./gradlew :composeApp:testAndroidHostTest` passes (including `AlarmsViewModelTest` and `AlarmSummaryTest` unchanged) *(803 tests, 0 failures; new `PaletteContrastTest` case for the alarm cards and snackbar)*
- [x] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [x] `./gradlew :androidApp:lintRelease` and `./gradlew :composeApp:checkTranslations` pass
- [x] The hard-coded-text `grep` from CLAUDE.md finds no new hits

## Task [08-alarm-editor-redesign]

The alarm editor shows the type choice as two large cards with the chosen type's description below
(the other type dimmed with a lock for an existing alarm), then station, pollen-type chips, minimum
chips with their note, weekday circles, the report time or the active window as time fields with
"Times are Swiss time.", the red outlined "Delete alarm" for existing alarms, and "Save" in the glass
action bar with a save error shown in red directly above it. The time picker opens in the Meadow
style. Loading shows the skeleton; a load failure shows the status card. The Meadow filter chips,
time field and time picker style are introduced here.

### Implementation steps

- [x] Replace the segmented type control with the two type cards, including the locked state and the description below *(`TypeCards` in `AlarmEditorParts.kt`, one radio group)*
- [x] Restyle sections: station dropdown, filter chips, weekday circles, time fields and window error *(shared `core/ui/selection/MeadowFilterChip` and `core/ui/field/SelectField`; `DayToggles`; the invalid window turns the end field red as in Figma)*
- [x] Restyle the time picker dialog (large hour and minute boxes, 24-hour dial, Cancel / OK) *(Figma's keyboard toggle is not drawn: it would need a new spoken string)*
- [x] Show the save error inside the action bar above "Save"; keep "Saving…" / "Deleting…" labels *(with Figma's error icon)*
- [x] Use the Danger button for delete; use skeleton and status card for loading and error *(`AlarmEditorSkeleton`; the delete button loses its icon, as in Figma)*

### Acceptance criteria

- [x] Runtime: a new alarm shows both type cards selectable and the chosen type's description below them; an existing alarm shows the other type dimmed with a lock and not selectable *(API 37: new alarm switched from Daily report to Threshold alert, description changed with it; Basel's threshold alert shows Daily report dimmed with a lock, a tap on it changed nothing, the node tree has both cards as disabled radio buttons with Threshold checked; light and dark)*
- [x] Runtime: tapping the report time opens the time picker with large hour and minute boxes and the 24-hour dial; the chosen time appears in the field *("REPORT TIME" overline, 08 / 00 boxes, 0–23 dial, Cancel / OK; 18:30 picked and shown in the field)*
- [x] Runtime: an end time before the start shows the window error under the fields; with the backend stopped, saving shows "Couldn't save the alarm. …" in red above "Save" *(07:00–06:00: red end field, "The end must be after the start.", Save disabled; server unreachable via airplane mode: "Couldn’t save the alarm. Couldn’t reach the server. …" with the error icon above Save. Also checked offline: the load error card with Retry; loading with a temporary, reverted 4 s delay: the skeleton)*
- [x] Runtime: in German, all section titles, chips and the type cards fit without clipping *(dark scheme: "Tagesbericht" / "Schwellenalarm", "Benachrichtigen ab", "Aktives Zeitfenster", chips and Mo–So fit; the window fields' labels wrap to two lines and the fields grow, nothing clipped)*

### Quality gates

- [x] `./gradlew :composeApp:testAndroidHostTest` passes (including `AlarmFormStateTest` and `AlarmEditorViewModelTest` unchanged) *(804 tests, 0 failures; new `PaletteContrastTest` case for the editor and time picker)*
- [x] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [x] `./gradlew :androidApp:lintRelease` and `./gradlew :composeApp:checkTranslations` pass
- [x] The hard-coded-text `grep` from CLAUDE.md finds no new hits *(only a KDoc comment)*

## Task [09-onboarding-and-change-station-redesign]

Onboarding opens with the Swiss map illustration (outline, all stations, pollen grains, the washes),
"Welcome to PollenInfo", the intro, the restyled station picker (dropdown with "Station", tonal "Use
my location") and "Continue" in the action bar; a failed station list shows an error card with "Retry"
in place of the dropdown and disables "Continue". Change station shows its explanation, the picker in
a card, the location notes as warning banners and "Save" in the action bar.

### Implementation steps

- [ ] Add the onboarding illustration (decorative, using the existing border and station projection)
- [ ] Restyle the shared station picker content, its location notes as banners and its load error as an inline card
- [ ] Restyle Change station around the picker card

### Acceptance criteria

- [ ] Runtime (fresh install): onboarding shows the illustration, picker and "Continue"; completing it opens Home as before
- [ ] Runtime: with the backend stopped at first launch, the error card with "Retry" replaces the dropdown and "Continue" is disabled; "Retry" after starting the backend shows the dropdown
- [ ] Runtime: with location permission denied, "Use my location" shows the permission note as a warning banner; picking from the dropdown still cancels a running lookup
- [ ] Runtime: TalkBack skips the illustration

### Quality gates

- [ ] `./gradlew :composeApp:testAndroidHostTest` passes (including `StationPickerTest`, `OnboardingViewModelTest` and `ChangeStationViewModelTest` unchanged)
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [ ] `./gradlew :androidApp:lintRelease` and `./gradlew :composeApp:checkTranslations` pass
- [ ] The hard-coded-text `grep` from CLAUDE.md finds no new hits

## Task [10-account-and-settings-redesign]

Log in and Create account show the key badge, the serif title, the rounded fields (password eye,
hints, mismatch error), the no-recovery warning banner, the consent checkbox card, the primary button
and the mode-switch text button; a failed request shows its error as a banner under the fields.
Settings shows the account card (email, "Log out") or the logged-out log-in action, then grouped rows
with icon tiles for default station, language, privacy (crash-report switch row), data source,
impressum and account deletion, and the version line; a failed crash-report save shows the Meadow
snackbar. The Meadow text field (including `PasswordField`) and checkbox are completed here.

### Implementation steps

- [ ] Restyle the account form (text and password fields, checkbox card), its error banner and the warning banner
- [ ] Restyle Settings: account card (logged in) and log-in row (logged out), grouped rows, privacy switch row (keeps `toggleable` with `Role.Switch`), data source with the unchanged attribution, impressum, deletion note and version
- [ ] Restyle the crash-report save-failed snackbar

### Acceptance criteria

- [ ] Runtime: a wrong password shows "Couldn't log in. The email address or password is incorrect." as a banner under the fields; on Create account, mismatching passwords outline the repeat field in red with "The passwords don't match." below, followed by the no-recovery warning and the consent checkbox
- [ ] Runtime: logged in, Settings shows the account card with the email and "Log out", then grouped rows for default station, language, privacy, data source, impressum and account deletion, and the version line
- [ ] Runtime: log out and delete account open their dialogs (the latter with a password field) and work as before
- [ ] Runtime: with a failed save forced by a temporary local change (the preference write is on-device, so a stopped backend does not trigger it; the change is not committed), the switch flips back and the snackbar above the bar says "Could not save your choice. Please try again."
- [ ] Runtime: the MeteoSwiss attribution text is unchanged in all four languages ("Source:" without space in French); TalkBack announces the crash-report row as a switch with its state

### Quality gates

- [ ] `./gradlew :composeApp:testAndroidHostTest` passes (including `AccountFormStateTest`, `AccountViewModelTest` and `SettingsViewModelTest` unchanged)
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [ ] `./gradlew :androidApp:lintRelease` and `./gradlew :composeApp:checkTranslations` pass
- [ ] The hard-coded-text `grep` from CLAUDE.md finds no new hits

## Task [11-cross-screen-verification-and-docs]

Final pass over the whole app as a user would meet it: every screen, dialog and state in all four
languages, at the largest font size, with TalkBack, in light and dark, on Android 12+ and Android 11,
compared with the Figma frames. Fix what the pass finds, and bring CLAUDE.md and the README fully in
line with the redesign.

### Implementation steps

- [ ] Walk the Figma "Screens" order (36 states) in German, French, Italian and English, light and dark; record each frame as matching or with a named deviation; fix clipping and layout defects
- [ ] Repeat Home, Alarms and the alarm editor at the largest font size; fix overlaps
- [ ] TalkBack and touch-target pass over every screen; fix missing or changed descriptions and targets under 48 dp
- [ ] Review CLAUDE.md and README end to end for stale statements (title bars, `NavigationBar`, dynamic colour, floating feeling card, palette validator, map edge colour)

### Acceptance criteria

- [ ] Runtime: no text is clipped or truncated on any screen or dialog in any of the four languages, and every screen title renders in the serif font
- [ ] Runtime: at the largest font size, Home, Alarms and the alarm editor show all text without overlap
- [ ] Runtime: every element that had a spoken description before the redesign still has one (TalkBack pass), and tabs, back button, face buttons, chips, weekday circles and switches have touch targets of at least 48 dp (Layout Inspector)
- [ ] Handover: a checklist of all 36 Figma states, each marked "matches" or listing its named deviation for the maintainer
- [ ] Repository: `git diff origin/develop -- composeApp/src/commonMain/composeResources` shows only the alarm-count string added (in all four languages); CLAUDE.md and README contain no statement contradicted by the redesign

### Quality gates

- [ ] `./gradlew :composeApp:testAndroidHostTest :composeApp:checkTranslations` passes
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [ ] `./gradlew :androidApp:lintRelease` passes
- [ ] `./gradlew :androidApp:assembleDebug` and `./gradlew :androidApp:assembleRelease -Ppolleninfo.uploadCrashlyticsMapping=false` pass, and the merged-manifest `grep`s match only debug
