## Task [01-share-station-and-reading-code]

Make the station list, the per-station reading pipeline and the severity presentation shared app
code instead of property of the Onboarding and Home features, so a third screen can use them
without depending on another feature. This is a behaviour-preserving move: Onboarding and Home
look and behave exactly as before, and every existing test keeps its assertions and passes from its
new location. It is the enabling slice for every later task.

### Implementation steps

- [x] Before changing anything, take emulator screenshots of Home (light and dark, one fresh and one stale reading) as the comparison baseline, and record the warning lines of `./gradlew :composeApp:assembleDebug` on the current state.
- [x] Move the station model, repository (interface + implementation), API service, DTO and mapper from the onboarding feature into a shared `core/station` area; keep the nearest-station use case in onboarding.
- [x] Move the reading model (measurement, species reading, severity, overview, reading age and its stale threshold), repository (interface + implementation), API service, DTO, mapper and the get-station-measurement use case from the home feature into a shared `core/measurement` area.
- [x] Move the severity bar, severity colours, reading-age and "refreshed" labels into a shared `core/ui/severity` area; extract Home's private species row, species list heading, severity wording and reading-age view into it as reusable composables, and have Home call the shared versions.
- [x] Move the matching tests and fixtures (station fixtures, fake station repository, measurement fixtures with the gated fake measurement repository) alongside their subjects in `commonTest`.
- [x] Update DI and navigation imports (no binding changes) and KDoc links that point at the old packages (e.g. in the example API service).
- [x] Update CLAUDE.md wherever it names the old locations (severity colours, reading age, the feature slice descriptions, "cross-feature code lives in core").

### Acceptance criteria

- [x] No file under `feature/home` or `feature/onboarding` defines the station, measurement, severity, reading-age, severity-bar, severity-colour or species-row code any more; each exists exactly once under `core/`.
- [x] A grep over `composeApp/src` shows `feature.home` / `feature.onboarding` imported only by files inside that feature plus the DI module and the navigation host; no file under `core/` imports any `feature.*` package; no KDoc link points at a moved class's old package.
- [x] Every test that existed before the move still exists with unchanged assertions, and `./gradlew :composeApp:testDebugUnitTest` passes.
- [x] Emulator screenshots of Home after the move (light and dark, one fresh and one stale reading) are identical to the baseline apart from clock times.
- [x] Onboarding on an Android emulator still lists stations alphabetically and completes to Home.

### Quality gates

- [x] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` succeeds.
- [x] `./gradlew :composeApp:assembleDebug` succeeds and its warning lines match the recorded baseline.
- [ ] `git diff -M --stat` reports each moved file as a rename; non-rename hunks touch only `package`/`import` lines, visibility modifiers, KDoc links and the extracted composables. *(failed in the letter: at the default 50 % threshold two 16–17-line files — `StationRepositoryImpl`, `StationMeasurementRepositoryImpl` — show as delete + add, at 46 % / 49 % similarity, because most of their lines are the rewritten package/imports; `git diff -M30%` reports both as renames and their whole diff is `package`/`import` lines. All other moved files are renames at the default threshold, and every non-rename hunk is a `package`/`import` line, one of two KDoc links, or the extracted composables.)*

## Task [02-all-stations-tab-lists-every-station]

The second bottom-bar tab becomes "All stations": opening it shows a full-screen spinner until the
station list arrives, then all fifteen stations alphabetically, each with its name on one line and
the severity bar plus severity word on the next, filled to the station's worst current severity.
Every station's reading is requested in parallel and each row fills in as its own reading arrives,
showing a muted placeholder until then. A station whose reading fails shows an empty muted bar with
"No reading"; a stale reading shows a warning icon, announced as "Reading not current", next to its
severity word. No map, refresh or error handling yet beyond what the initial load needs — the list
fills the screen below the top bar. The back gesture returns to Home.

### Implementation steps

- [x] Rename the second tab's route and destination entry to All stations (content description and screen title "All stations"), keeping the pin icon for now; wire its destination to the new screen and update the tab tests.
- [x] Add the per-station outcome type (pending / available with overview / unavailable) and a use case that, given the station list, starts every station's reading at once and emits the full alphabetical list after each station resolves, starting all pending; per-station failures become unavailable.
- [x] Add the All stations ViewModel with loading / content (stations, selected station, refreshed-at, refreshing flag) / error states and the initial load: loading → content with every station pending → rows replaced as the use case emits.
- [x] Build the screen: top bar, list of rows using the shared severity bar and wording, muted placeholder for pending rows, "No reading" for unavailable rows, warning icon for stale readings judged at render time.
- [x] Register the use case and ViewModel in DI.
- [x] Document the feature in CLAUDE.md: the feature slice, the client-side fan-out rationale, and the bottom-navigation section (FEATURE_2 becomes ALL_STATIONS).

### Acceptance criteria

- [x] Use-case tests show: the first emission lists every station as pending in alphabetical order; a station whose fake fails ends unavailable while others end available; with one station held by a gate, the others resolve while it stays pending; the order never changes between emissions; the final emission has every station resolved; an available station's overall severity is its worst measured taxon.
- [x] ViewModel tests show: loading then content; with all readings held by a gate the state is content with every station pending; partial failure gives content with unavailable entries.
- [x] Tab tests assert the second tab is named "All stations" and resolves for the All stations route, with the other tabs unchanged.
- [ ] ~~On an Android emulator against the local backend, the second tab shows the title "All stations" and fifteen alphabetical rows each with name on one line and bar plus severity word on the next; TalkBack announces the tab as "All stations".~~ *(skipped in part: title, the fifteen alphabetical rows and their two-line layout verified by screenshot and uiautomator dump; the tab's accessibility node carries content-desc "All stations", but TalkBack's spoken output could not be captured when driven over adb — needs a manual listen)*
- [x] On the emulator, a station whose reading the backend cannot serve (e.g. a backend run with one station's upstream failing) shows an empty muted bar with "No reading" while the other rows display normally. *(verified through a local proxy that returned 502 for PLU, on a one-off build pointed at it; the source change was reverted)*
- [ ] ~~On the emulator, a station whose reading is three or more hours old shows a warning icon next to its severity word that TalkBack announces as "Reading not current".~~ *(skipped in part: the icon is verified by screenshot next to the word on Genève (proxy-aged), Neuchâtel and Payerne (genuinely 3+ hours old), and its accessibility node carries content-desc "Reading not current"; TalkBack's spoken output could not be captured over adb — needs a manual listen)*
- [x] On the emulator, the back gesture from All stations returns to Home.

### Quality gates

- [x] `./gradlew :composeApp:testDebugUnitTest` passes.
- [x] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` succeeds (no JVM-only APIs, no commas in test names).
- [x] `./gradlew :composeApp:assembleDebug` warning lines match those before this task.
- [x] Our `Result` type is explicitly imported wherever it is used; no `runCatching`.

## Task [03-refresh-and-full-screen-errors]

The All stations screen can be refreshed and fails gracefully. A "Refreshed HH:mm" caption sits
above the list; pulling down on the list reloads every station's reading while the current values
stay on screen, and the caption moves to the new time when the round completes. When the station
list cannot be loaded, or every station's reading fails, the screen shows a full-screen error with
a Retry button that reloads. Returning to the tab shows the saved state without reloading.

### Implementation steps

- [x] Add refresh to the ViewModel: only from content, sets the refreshing flag, keeps the current rows (no reset to pending), replaces them with the new round's final result, does not re-fetch the station list, stamps refreshed-at from an injected clock (defaulted parameter, as on Home).
- [x] Add the error rules: station-list failure → error; every reading unavailable (initial load or refresh) → error; retry restarts the full load.
- [x] Add the refreshed caption, pull-to-refresh around the list, and the full-screen error view with Retry to the screen.
- [x] Update CLAUDE.md with the refresh and error rules.

### Acceptance criteria

- [x] ViewModel tests show: station-list failure gives error and retry recovers; all readings failing gives error and retry after the readings recover gives content; refreshed-at is absent until the first round completes and then equals the injected clock.
- [x] ViewModel tests show: during a gated refresh the refreshing flag is true and the previous readings are still in the state; the station list is fetched only once across a refresh; all readings failing on refresh gives error.
- [x] On an Android emulator, pulling down on the list shows a refresh indicator while the rows stay visible, and afterwards the "Refreshed" caption shows the new time.
- [x] On the emulator, with the backend stopped, opening the tab shows the full-screen error with Retry; after restarting the backend, tapping Retry shows the fifteen rows.
- [x] On the emulator, switching to Home and back to All stations shows the same rows with no loading indicator and an unchanged "Refreshed" caption.

### Quality gates

- [x] `./gradlew :composeApp:testDebugUnitTest` passes.
- [x] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` succeeds.
- [x] `./gradlew :composeApp:assembleDebug` warning lines match those before this task.

## Task [04-swiss-map-shows-station-dots]

A static map of Switzerland sits fixed at the top of the All stations screen, above the refreshed
caption and the scrolling list. It draws an embedded simplified national border and places all
fifteen stations as dots at their true positions through one shared projection, so dots and outline
agree by construction. Dots are filled with the station's overall severity colour using the same
colour function as the bars (following the applied light/dark scheme), neutral while pending and
hollow and muted when unavailable. The map fits the width, keeps Switzerland's aspect ratio, is
capped at about 40 % of the screen height (scaled and centred, never cropped), and is a single
accessibility element describing the stations. The bottom-bar tab icon becomes a small Switzerland
outline built from the same border data.

### Implementation steps

- [ ] Add a fixture of all fifteen stations with abbreviation, name and WGS84 coordinates (copied from the server's station enum) next to the existing station fixtures in `commonTest`.
- [ ] Embed a simplified Natural Earth (public domain) outer border ring of Switzerland as WGS84 points (roughly 150–300 points), with source and simplification noted.
- [ ] Add a pure projection (equirectangular with longitude scaled by cos 46.8°, fitted inside a given canvas size and centred, with a small margin) exposing the map's aspect ratio.
- [ ] Draw the map composable: border, dots filled with the existing composable severity colour (`PollenSeverity.color()`, not a new function — a deliberate deviation from the annex), neutral pending dots, hollow unavailable dots, size and height cap; one accessibility node whose description is derived from the station count.
- [ ] Place the map above the refreshed caption and list so only the list scrolls and only the list is inside pull-to-refresh.
- [ ] Build the Swiss outline tab icon from the border data through the same projection, stroke only so it tints like a Material icon; assign it to the All stations tab.
- [ ] Update CLAUDE.md: the map's projection and border data source, the dot colour rule (shared with the bars), and the bottom-navigation icon sentence.

### Acceptance criteria

- [ ] Projection tests show: the border's bounding box maps to the canvas edges within the margin; for canvases wider and taller than the map the result stays inside the canvas and is centred; all fifteen stations project inside the canvas; Genève lies west of Zürich and Lugano south of Luzern; the aspect ratio lies between 1.4 and 1.7.
- [ ] Border tests show the ring is closed (or treated as closed), has between 150 and 300 points, and contains every one of the fifteen stations.
- [ ] On an Android emulator the map shows the full outline with fifteen dots; Genève is the left-most dot, Münsterlingen the top-right-most, Lugano and Locarno the two bottom-most; each dot's colour matches its row's bar in light and in dark mode; scrolling the list leaves the map in place.
- [ ] On the emulator, while readings are held (slow backend) the dots are neutral, and an unavailable station's dot is hollow and muted.
- [ ] In landscape and on a small-screen emulator profile the full outline is visible, the map height is at most about 40 % of the screen, and at least one full row is visible below it.
- [ ] TalkBack announces the map as one element, "Map of 15 pollen stations. Select a station in the list below.", with no focus stop per dot.
- [ ] The second tab shows a Switzerland outline while tabs 3–5 still show the pin.

### Quality gates

- [ ] `./gradlew :composeApp:testDebugUnitTest` passes.
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` succeeds.
- [ ] The border and projection source files contain no `androidx.compose` import (grep).
- [ ] Dot fill and the severity bar both call `PollenSeverity.color()` (code review).
- [ ] `git diff -- '*.gradle.kts' gradle/libs.versions.toml` adds no dependency.

## Task [05-select-station-from-list-expands-detail]

Tapping a station's row selects it: the row expands to show the reading-age line (quiet caption or
stale warning, as on Home), the unit heading and all seven taxa with bar, severity word and
concentration in Home's order; its dot on the map is circled in the primary colour with a gap that
keeps the severity colour visible; and the list animates so the whole expanded row is visible
(header at the top if it does not fit). Only one station is expanded at a time; tapping the selected
row again deselects it. An unavailable station expands to "No reading available for this station
right now." Selection survives refresh and tab switches, is not persisted, and never changes the
home station. Rows announce expanded/collapsed to screen readers.

### Implementation steps

- [ ] Add station-click handling to the ViewModel: select, re-click deselects, another station replaces; selecting sends a one-shot scroll-to-station event on a buffered channel, deselecting sends none; refresh keeps the selection.
- [ ] Render the expanded row with the shared reading-age view, species heading and species rows; render the unavailable variant.
- [ ] Draw the selection ring around the selected dot on the map.
- [ ] Collect the scroll event in the screen and animate the list so the expanded row is fully visible, header first when taller than the viewport.
- [ ] Make each row one focus stop with an expanded/collapsed state description.
- [ ] Update CLAUDE.md with the selection model and the scroll-as-event decision.

### Acceptance criteria

- [ ] ViewModel tests show: clicking a station selects it and emits exactly one scroll event for it; clicking it again deselects and emits no event; clicking another station replaces the selection; the selection is unchanged after a refresh completes.
- [ ] On an Android emulator, tapping the Basel row circles the Basel dot, expands the row with the unit stated once and all seven taxa ordered worst first, unmeasured taxa last as "No data" with a dash, and scrolls so the whole row is visible.
- [ ] On the emulator, an expanded fresh reading shows a caption like "Data from 09:00", an expanded stale reading shows the highlighted "These readings are not current" warning with its time or date, and an expanded unavailable station shows "No reading available for this station right now."
- [ ] On the emulator, tapping a different row collapses the previous one and moves the ring; tapping the selected row again collapses it and removes the ring; the ring is clearly visible in light and dark mode.
- [ ] On the emulator, with a station selected, pulling to refresh leaves it selected and expanded; switching to Home and back shows the same selection without a reload, and Home still shows the onboarding station.
- [ ] With TalkBack, each row is one focus stop announcing the name, severity and expanded or collapsed.

### Quality gates

- [ ] `./gradlew :composeApp:testDebugUnitTest` passes.
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` succeeds.
- [ ] The scroll request is not represented as a field of the UI state (code review).

## Task [06-tap-map-dot-selects-station]

Tapping the map selects the station whose dot is nearest to the tap, within about 24 dp; a tap
farther from every dot does nothing. It then behaves exactly like tapping the row: ring, expand,
scroll, and tapping the selected dot again deselects. Choosing the nearest dot keeps closely spaced
stations such as Locarno and Lugano unambiguous.

### Implementation steps

- [ ] Add a pure nearest-station hit test over projected dot positions and a radius, with a deterministic tie-break by abbreviation.
- [ ] Wire taps on the map through the hit test (radius converted from dp) into the same station-click handling the list uses.
- [ ] Update CLAUDE.md with the hit-test rule.

### Acceptance criteria

- [ ] Hit-test tests show: a tap exactly on a dot selects it; a tap at exactly the radius selects it; a tap just beyond the radius returns nothing; between two close dots the nearer one wins, checked from both sides; an exact tie resolves by abbreviation; no dots returns nothing.
- [ ] On an Android emulator, tapping the Zürich dot circles it, expands the Zürich row and scrolls to it; tapping it again collapses the row and removes the ring.
- [ ] On the emulator, tapping empty map area far from any dot changes nothing.
- [ ] On the emulator, tapping just on Lugano's side of the Locarno–Lugano midpoint selects Lugano, and just on Locarno's side selects Locarno.

### Quality gates

- [ ] `./gradlew :composeApp:testDebugUnitTest` passes.
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` succeeds.
- [ ] The hit-test source file contains no `androidx.compose` import (grep).
- [ ] The map composable exposes a single station-click callback, wired to the same ViewModel entry point as the rows (code review).
