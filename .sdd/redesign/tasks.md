# Tasks: Redesign ("Meadow")

Requirements: [requirements.md](requirements.md) · Design: Figma *PollenInfo Redesign*
(<https://www.figma.com/design/naeELzkiFSdMuXsnmZVKnW>, page "Screens" in usage order, light row on
top, dark below; page "Direction & Foundations" for tokens and type).

Notes that apply to every task:

- **Visual only.** No ViewModel, UI-state, use-case, repository, API or navigation-graph change, and
  no existing test changes except `SeverityColorsTest` for the one deliberately changed colour. The
  only new string is the alarm count in task 07. If a screen seems to need a state change or another
  string, stop and ask.
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

- [ ] Confirm the existing `feature/redesign` branch with the maintainer; commit the specification (`.sdd/redesign/`) as the first commit and push
- [ ] Replace the light and dark `ColorScheme` values with the Meadow mapping from the annex, each value with its contrast noted beside it
- [ ] Add the extended Meadow tokens (data class + composition local provided by `PollenInfoTheme`) for wash, glass, accent/onAccent, warningContainer/onWarningContainer and map land
- [ ] Remove dynamic colour from the Android theme so both platforms use the Meadow schemes
- [ ] Change `severityModerateLight` to `#A07400`, update its recorded ratio, and update `SeverityColorsTest` for this value only
- [ ] Replace the map water fill with the Meadow lake colour and remove the lake edge colour and its drawing
- [ ] Add the font files and their OFL licence texts as Compose resources in `:theme`; build the typography from them with the annex's type scale; add the shape scale
- [ ] Add the pure WCAG contrast function and the palette contrast test with every pairing listed in the annex's testing decisions
- [ ] Update CLAUDE.md "Colours and bars" (palettes, extended tokens, no dynamic colour, flat lakes, contrast test replaces the manual validator) and the README tech stack (fonts)

### Acceptance criteria

- [ ] Automated test: every severity colour reaches ≥ 3:1 against surface, raised, sunken, background, wash and map land, in both schemes
- [ ] Automated test: every species colour reaches ≥ 3.5:1 against the chart card surface, and primary/secondary text, on-primary, on-accent and on-warning pairs reach ≥ 4.5:1, in both schemes
- [ ] Automated test: the contrast function returns 21 for black on white and 1 for identical colours
- [ ] Runtime (Android 12+ emulator with a non-green wallpaper): Home shows the Meadow forest green and cream background in light mode and the Meadow dark colours in dark mode, never wallpaper colours
- [ ] Runtime: body text on Home, Settings and the alarm editor renders in Plus Jakarta Sans; the All stations map shows lakes as a flat fill without an edge line
- [ ] Repository: an OFL licence text sits beside each of the two font families' files

### Quality gates

- [ ] `./gradlew :composeApp:testAndroidHostTest` passes (all existing tests unchanged except `SeverityColorsTest`'s Moderate light value)
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [ ] `./gradlew :androidApp:lintRelease` passes
- [ ] New test names are backtick sentences without commas

## Task [02-glass-tab-bar-and-tab-screen-frame]

The five tab screens lose their title bars and gain the Meadow frame: soft background washes, a small
overline and a large serif title at the top of the scrolling content, and the glass bottom bar with
rounded upper corners whose content scrolls visibly (blurred) behind it. The selected tab is a green
pill; tabs, their order, names and navigation rules are unchanged. Everything that sits at the bottom
of a tab (lists' ends, Home's floating feeling card, Alarms' create button and snackbar, Settings'
snackbar) is lifted by the bar height so nothing hides behind the bar. Screen contents are otherwise
untouched.

### Implementation steps

- [ ] Add Haze (pinned in the version catalog) and mark the tab host content as the blur source
- [ ] Replace the Material navigation bar with the glass bar driven by `TopLevelDestination` (same descriptions, selection semantics and `navigateToTab`), with Haze blur and its fallback tint
- [ ] Stop padding the tab host for the bar; expose the bar height to screens and add it to each tab screen's scrolling end padding and to Home's floating feeling card, Alarms' create button and snackbar host, and Settings' snackbar host
- [ ] Add the shared tab-screen frame: background with decorative washes, header (overline + serif title, announced as heading), content slot
- [ ] Move Home, All stations, Diary, Alarms and Settings onto the frame and remove their `TopAppBar`s; Home's header shows the station name in every state
- [ ] Update CLAUDE.md "Navigation" (glass bar, insets now drawn behind the bar, bottom padding rule)

### Acceptance criteria

- [ ] Automated test: `TopLevelDestinationTest` still passes unchanged
- [ ] Runtime: on every tab there is no title bar; overline and serif (Fraunces) title sit at the top of the content, and Home shows the station name while loading, with content and on error
- [ ] Runtime: the bar has rounded upper corners, shows the same five icons in the same order, and the current tab sits on a green pill
- [ ] Runtime: scrolling Home and All stations shows the content blurred behind the bar, and the last item scrolls fully above the bar; Home's feeling card, Alarms' "Create alarm" and the Alarms and Settings snackbars (triggered with the backend stopped) appear fully above the bar
- [ ] Runtime (Android 11 emulator): the bar is see-through without blur; a screenshot with Home's gauge card behind the bar shows every icon and the selected pill distinct from the content
- [ ] Runtime: switching tabs, reselecting the current tab and the back gesture behave as before; TalkBack announces each tab by its existing name and the title as a heading

### Quality gates

- [ ] `./gradlew :composeApp:testAndroidHostTest` passes
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [ ] `./gradlew :androidApp:lintRelease` passes
- [ ] After `./gradlew :androidApp:assembleDebug :androidApp:assembleRelease -Ppolleninfo.uploadCrashlyticsMapping=false`, the two merged-manifest `grep`s from CLAUDE.md match only the debug manifest

## Task [03-non-tab-frame-and-dialogs]

Screens that are not tabs (onboarding, change station, alarm editor, account) lose their title bars
and gain the Meadow non-tab frame: a floating round glass back button (where they had a back arrow),
the overline + serif title in the content, a top scrim when content scrolls under the status bar, and
— for onboarding, change station and the alarm editor — a glass action bar at the bottom hosting their
main button in the new pill style. All confirmation dialogs take the Meadow dialog style: centred icon
badge, serif title, centred text, stacked full-width buttons with the destructive action in red.

### Implementation steps

- [ ] Add the non-tab frame: back button (existing back description, 48 dp target), header, top scrim, optional glass action bar with rounded upper corners
- [ ] Add the pill button variants (Primary, Tonal, Outline, Text, Danger, Accent) as a shared component
- [ ] Move Onboarding, Change station, Alarm editor and Account onto the frame and remove their `TopAppBar`s; Onboarding's Continue and Change station's / the editor's Save move into the action bar
- [ ] Restyle the four confirmation dialogs (discard changes, delete alarm, log out, delete account with its password field) as one shared dialog style
- [ ] Update CLAUDE.md with the non-tab frame (back button, action bar, which screens use it) and the button variants

### Acceptance criteria

- [ ] Runtime: Change station, alarm editor and account show a round back button top-left that behaves as the old back arrow (including the editor's discard dialog when dirty); onboarding has none; TalkBack announces it as "Back"
- [ ] Runtime: onboarding's "Continue" and change station's / the editor's "Save" sit in a glass bar with rounded upper corners at the bottom; long editor content scrolls behind it and fully above it; the account screen has no action bar
- [ ] Runtime: the discard dialog shows a centred icon, "Discard changes?", "Discard" in red and "Keep editing" stacked full width; the delete-alarm, log-out and delete-account dialogs follow the same layout, the last with its password field
- [ ] Runtime: titles of the four screens render in the serif font (Fraunces)

### Quality gates

- [ ] `./gradlew :composeApp:testAndroidHostTest` passes
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [ ] `./gradlew :androidApp:lintRelease` and `./gradlew :composeApp:checkTranslations` pass
- [ ] The hard-coded-text `grep` from CLAUDE.md finds no new hits

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

- [ ] Add the pure gauge filling rule and its test; add the gauge composable (decorative, glow behind)
- [ ] Add the species tile (replacing the row on Home) with the same semantics as the current row; restyle the severity bar track
- [ ] Restyle the feeling prompt as the inline green card with face buttons; remove the floating overlay and its padding mechanism; keep its save-error display
- [ ] Restyle reading age: time pill and stale banner in warning colours; keep "Refreshed …"
- [ ] Add the skeleton + shimmer, status card and banner components; use them for Home's loading and error states
- [ ] Update CLAUDE.md "Screen behaviour → Home" (inline card, padding sentence) and "Colours and bars" (gauge; fill stops unchanged)

### Acceptance criteria

- [ ] Automated test: the gauge fills 0 segments for no reading and None, 1 for Low, 2 for Moderate, 3 for High, 4 for Very high, never decreasing with severity
- [ ] Runtime: a High reading driven by Birch shows "High" with three filled segments and "Driven by Birch"; a None reading shows "None" with no filled segment; all seven types appear as tiles worst first, an unreported type shows "No data" with an empty bar
- [ ] Runtime: with the feeling question due and logged in, the green card appears inline with face buttons labelled Very good, Good, Bad, Very bad and "Not today"; answering and "Not today" behave as before; logged out, a tap opens the account screen as before
- [ ] Runtime: a reading older than three hours shows "These readings are not current." and the time pill in warning colours; loading shows the skeleton with a moving shimmer and the station name; a backend outage shows the status card with "Retry", which reloads
- [ ] Runtime: TalkBack announces station, severity, driving type, reading time and each tile as before

### Quality gates

- [ ] `./gradlew :composeApp:testAndroidHostTest` passes (including `SeverityBarTest` and `HomeViewModelTest` unchanged)
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [ ] `./gradlew :androidApp:lintRelease` and `./gradlew :composeApp:checkTranslations` pass
- [ ] The hard-coded-text `grep` from CLAUDE.md finds no new hits; new test names are backtick sentences without commas

## Task [05-all-stations-redesign]

All stations shows the map card (Meadow land fill, flat lakes, one dot per station in its severity
colour with a surface ring, the selected station enlarged with a halo and a "Name · Level" label) and
the legend naming all five levels, followed by the station rows (severity dot, name, "level · driving
type", reserved stale-icon slot, expand affordance). Loading and unavailable rows show a grey dot with
"Loading…" / "No reading"; an expanded row shows that station's readings as species tiles. Loading
shows the skeleton; total failure shows the status card.

### Implementation steps

- [ ] Restyle the map: land fill, dots with ring, selected halo and "Name · Level" label; keep the single semantics node and tap behaviour
- [ ] Add the legend row (dot + level word for each severity)
- [ ] Restyle station rows and their loading / no-reading / stale variants; use species tiles in the expanded detail
- [ ] Use the skeleton for loading and the status card for the full-screen error

### Acceptance criteria

- [ ] Runtime: the map shows 15 coloured dots and the legend with None, Low, Moderate, High, Very high; selecting Zürich enlarges its dot with a halo and the label "Zürich · <its level>" and scrolls the list to it
- [ ] Runtime: during a refresh or with a station unavailable, affected rows show a grey dot and "Loading…" or "No reading"; stale rows keep the warning icon
- [ ] Runtime: expanding a row shows its readings as tiles; with every station failing, the status card with "No station's reading could be loaded…" and "Retry" is shown
- [ ] Runtime: TalkBack reads the map's single description as before

### Quality gates

- [ ] `./gradlew :composeApp:testAndroidHostTest` passes (including `SwissMapProjectionTest`, `StationHitTestTest`, `SwissBorderTest`, `SwissLakesTest` and `AllStationsViewModelTest` unchanged)
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [ ] `./gradlew :androidApp:lintRelease` and `./gradlew :composeApp:checkTranslations` pass
- [ ] The hard-coded-text `grep` from CLAUDE.md finds no new hits

## Task [06-diary-redesign]

The Diary shows the signed-out gate as an illustrated card (account explanation, four points with
check badges, "Create account" and "Log in"), and when signed in the station dropdown with the
Week / Month / Year segmented toggle, the chart in a rounded card (feeling as a dashed primary line
with dots, species lines in their colours, severity words on the axis, the date range as card title),
the pollen types as tick chips in their line colour ("Not measured here" dimmed) and the disclaimer.
Loading shows the skeleton; errors show the status card. The segmented toggle and the Meadow dropdown
field are introduced here.

### Implementation steps

- [ ] Restyle the gate using the status card pattern with the four points
- [ ] Add the segmented toggle component and use it for the range; add the Meadow dropdown field style and use it for the station
- [ ] Restyle the chart card and the feeling line (dashed, with points); keep the pure chart geometry and description unchanged
- [ ] Restyle species chips (checked fill in the line colour, unmeasured dimmed with its caption)
- [ ] Use skeleton and status card for loading and error

### Acceptance criteria

- [ ] Runtime: logged out, the gate card shows the four points and both buttons, which open the account screen in the right mode
- [ ] Runtime: logged in with a week of entries, the chart card shows severity words on the vertical axis, one coloured line per checked type, and the feeling as a dashed line with dots that never bridges a missing day
- [ ] Runtime: switching Week / Month / Year and the station reloads as before; unchecking a species chip hides its line; an unmeasured type's chip is dimmed with "Not measured here"
- [ ] Runtime: a server outage shows the status card with "The pollen history could not be loaded." and "Retry"

### Quality gates

- [ ] `./gradlew :composeApp:testAndroidHostTest` passes (including `DiaryChartGeometryTest` and `DiaryViewModelTest` unchanged)
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [ ] `./gradlew :androidApp:lintRelease` and `./gradlew :composeApp:checkTranslations` pass
- [ ] The hard-coded-text `grep` from CLAUDE.md finds no new hits

## Task [07-alarms-list-redesign]

The Alarms tab shows each alarm as a card (type icon tile, summary, station, weekday circles, the
types and minimum as tags, switch; paused cards dimmed with "Paused"), the "n of 10" count, and the
gold extended "Create alarm" button floating above the bar. The notifications-off and no-alarms states
become illustrated status cards with their buttons. A failed switch flips back with the Meadow
snackbar (inverse colours), which pushes the create button up. Loading shows the skeleton; load
errors show the status card.

### Implementation steps

- [ ] Add the one new string for the count ("%1$d of %2$d", `alarm_` prefix, positional placeholders) in all four languages
- [ ] Restyle the alarm card from the existing summary data (no new formatting rules)
- [ ] Add the count chip and the gold extended create button above the glass bar
- [ ] Restyle the permission gate (both variants) and the empty state as status cards
- [ ] Restyle the toggle-failed snackbar (inverse surface) and its placement above the bar and below the create button
- [ ] Use skeleton and status card for loading and error

### Acceptance criteria

- [ ] Runtime: three alarms (one paused) show as cards with summaries, stations, weekday circles, tags and switches; the paused one is dimmed and marked "Paused"; "3 of 10" is shown (and its German, French and Italian translations)
- [ ] Runtime: with notifications denied, the card shows "Notifications are disabled" and the request or open-settings button as before; with no alarms, "No alarms yet" with the gold "Create alarm"
- [ ] Runtime: with the backend stopped, flipping a switch flips it back and the snackbar appears above the bar with the create button above it
- [ ] Runtime: TalkBack announces each switch as "Alarm for …" as before

### Quality gates

- [ ] `./gradlew :composeApp:testAndroidHostTest` passes (including `AlarmsViewModelTest` and `AlarmSummaryTest` unchanged)
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [ ] `./gradlew :androidApp:lintRelease` and `./gradlew :composeApp:checkTranslations` pass
- [ ] The hard-coded-text `grep` from CLAUDE.md finds no new hits

## Task [08-alarm-editor-redesign]

The alarm editor shows the type choice as two large cards with the chosen type's description below
(the other type dimmed with a lock for an existing alarm), then station, pollen-type chips, minimum
chips with their note, weekday circles, the report time or the active window as time fields with
"Times are Swiss time.", the red outlined "Delete alarm" for existing alarms, and "Save" in the glass
action bar with a save error shown in red directly above it. The time picker opens in the Meadow
style. Loading shows the skeleton; a load failure shows the status card. The Meadow filter chips,
time field and time picker style are introduced here.

### Implementation steps

- [ ] Replace the segmented type control with the two type cards, including the locked state and the description below
- [ ] Restyle sections: station dropdown, filter chips, weekday circles, time fields and window error
- [ ] Restyle the time picker dialog (large hour and minute boxes, 24-hour dial, Cancel / OK)
- [ ] Show the save error inside the action bar above "Save"; keep "Saving…" / "Deleting…" labels
- [ ] Use the Danger button for delete; use skeleton and status card for loading and error

### Acceptance criteria

- [ ] Runtime: a new alarm shows both type cards selectable and the chosen type's description below them; an existing alarm shows the other type dimmed with a lock and not selectable
- [ ] Runtime: tapping the report time opens the time picker with large hour and minute boxes and the 24-hour dial; the chosen time appears in the field
- [ ] Runtime: an end time before the start shows the window error under the fields; with the backend stopped, saving shows "Couldn't save the alarm. …" in red above "Save"
- [ ] Runtime: in German, all section titles, chips and the type cards fit without clipping

### Quality gates

- [ ] `./gradlew :composeApp:testAndroidHostTest` passes (including `AlarmFormStateTest` and `AlarmEditorViewModelTest` unchanged)
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [ ] `./gradlew :androidApp:lintRelease` and `./gradlew :composeApp:checkTranslations` pass
- [ ] The hard-coded-text `grep` from CLAUDE.md finds no new hits

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
