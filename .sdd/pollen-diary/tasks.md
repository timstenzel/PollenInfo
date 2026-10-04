# Pollen Diary — Tasks

Requirements: `requirements.md` (Technical Annex written 2026-10-04).

Annex deviation agreed during task generation: the Swiss time zone is a new `SWISS_ZONE` in the
server's `pollen/domain`; `ALARM_ZONE` becomes an alias of it, so `pollen/` never imports from
`alarm/`. `HistoryService` uses `SWISS_ZONE`.

Every task carries the same quality gates; "no new warnings" is judged against `main` for the same
Gradle tasks (no `w:` line that `main` does not also print). Criteria marked *(emulator)* are
checked on an Android emulator with `./gradlew :composeApp:assembleDebug` installed and
`./gradlew :server:run` running.

---

## Task [01-diary-tab-shows-last-month-of-pollen]

The tracer bullet. The third tab becomes "Diary" and shows, for the user's home station, a graph of
the daily pollen severity of every pollen type over the 30 days ending yesterday, with the
not-a-diagnosis note. End to end: MeteoSwiss `d_recent` → server parse, cache and classify → new
history endpoint → app history client → `DiaryViewModel` → canvas chart. Only the month range,
only the home station, no feeling line yet, no checkboxes yet (all species drawn).

### Implementation steps

- [x] Server: add `SWISS_ZONE` to `pollen/domain`; make `ALARM_ZONE` an alias of it.
- [x] Server: `PollenService.dailyRecent(station)`; implement in `MeteoSwissPollenService` (non-2xx throws). Extend `FakePollenService` with bytes, failures and a request record **per file kind**, keeping its existing hourly `bytes` / `failure` / `failures` / `requested` behaviour unchanged (`MeasurementServiceTest` and `AlarmSchedulerTest` rely on it). Update the anonymous `PollenService` in `MeasurementServiceTest`.
- [x] Server: `PollenCsvParser.parseDaily` (d0 columns → date → species → concentration). Add a verbatim `d_recent` fixture under `fixtures/ogd-pollen/pzh/`.
- [x] Server: `HistoryRange` (`WEEK`/`MONTH`/`YEAR`, parsed from `week|month|year`) and pure `historyWindow(range, today)`.
- [x] Server: `HistoryService` — `d_recent` `TtlCache` (3 h), window ending yesterday in `SWISS_ZONE`, gap filling, classification against `PollenThresholds`, stale-on-failure; wired in `Application.module()` and `configureRouting` (with default).
- [x] Server: `GET /pollen/stations/{abbr}/history?range=…` + DTOs; `400` bad/missing range (before station lookup), `404` unknown station, `502` failure with nothing retained.
- [x] App: `core/history` (API service, DTOs, mapper, `StationHistoryRepository` + `Impl`, `HistoryRange`), Koin wiring.
- [x] Theme: `SpeciesPalette.kt` (7 light/dark pairs, contrast recorded per value, hues distinct from `SeverityPalette`); app-side species id → colour using the surface-luminance rule.
- [x] App: pure `DiaryChartGeometry` (day → x, level → y with higher = worse, polylines split at gaps, y-ticks, month x-ticks) and a pure chart-description function; `DiaryChart` canvas as one accessibility node.
- [x] App: `DiaryViewModel` (`Loading` / `Content` / `Error` with Retry; home station, `MONTH`), `DiaryScreen` (top bar "Diary", explanatory line with asterisk, chart, disclaimer); register in `presentationModule`.
- [x] Navigation: `Screen.Feature3` → `Screen.Diary`, `FEATURE_3` → `DIARY` (`Icons.AutoMirrored.Filled.MenuBook`, "Diary"); update `TopLevelDestinationTest` (order list, name list, `currentTabOn`, and the test name mentioning "Feature 3").
- [x] CLAUDE.md: history endpoint in the REST table and its section, `SWISS_ZONE`, `core/history`, `SpeciesPalette`, the Diary tab.

### Acceptance criteria

- [x] Route tests: `GET /pollen/stations/pzh/history?range=month` returns 30 consecutive days ending yesterday (Swiss date), oldest first, each with all seven species in `PollenSpecies` order and `concentration`/`severity` `null` together on days without a value; the abbreviation is case-insensitive; a missing or unknown `range` is `400` even for an unknown station; an unknown station with a valid range is `404`; upstream failure with nothing retained is `502`.
- [x] `HistoryServiceTest` (`MutableClock`, `FakePollenService`): "yesterday" follows the Swiss date across UTC midnight; `d_recent` is not re-fetched within 3 hours and is after; a failure after a successful load serves the retained rows; classification with injected thresholds is pinned at a band edge and just below it.
- [x] Upstream tests: `PollenCsvParserTest` reads the `d_recent` fixture from d0 (not d1) columns, maps an empty cell and a missing column to `null`, skips a malformed date, keeps an all-empty row as all-`null`, and decodes ISO-8859-1; `MeteoSwissPollenServiceTest` requests the `d_recent` path and throws on non-2xx (`MockEngine`).
- [x] App data and geometry tests: `StationHistoryRepositoryImplTest` (real `HttpClient` + `MockEngine`) checks path and `range` query, mapping including `null` days, and `Failure` on 4xx, 5xx and transport errors; `DiaryChartGeometryTest` pins first/middle/last day x, the level → y inversion, and splitting at a missing day including an isolated single point.
- [x] `DiaryViewModelTest`: opens on the stored home station with `MONTH`, goes `Loading` → `Content`; a failed history request is `Error`, from which Retry reaches `Content`. `TopLevelDestinationTest` asserts "Diary" third, and `grep -r Feature3 composeApp/src` finds nothing.
- [x] `SpeciesPalette.kt` records a contrast ≥ 3:1 against `surfaceLight` / `surfaceDark` next to each of its 14 values, and none of its hues is a traffic-light green, amber, orange or red (reviewed against `SeverityPalette`).
- [x] *(emulator)* The third tab shows a book icon announced as "Diary"; the screen shows the line "Compare how you felt with the pollen levels at a station.\*", one coloured line per pollen type over 30 days, and the disclaimer "\* This is not a medical diagnosis. If you suspect a pollen allergy, please see a doctor."; TalkBack reads the graph as one element ending "last 30 days".

### Quality gates

- [x] `./gradlew :composeApp:testDebugUnitTest :server:test` passes.
- [x] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes.
- [x] `./gradlew :composeApp:assembleDebug` passes.
- [x] No new compiler warnings compared with `main`.
- [x] No JVM-only API in `commonMain` (`String.format`, `java.*`, …) and no comma in backtick test names.
- [x] CLAUDE.md updated as listed in the steps.

---

## Task [02-diary-week-month-and-year-ranges]

The user picks Week, Month or Year. The year view reaches back across 1 January, so the server
also reads MeteoSwiss's `d_historical` file, cached far longer since it changes once a year. The
chart's x-axis labels and its spoken description adapt to the range.

### Implementation steps

- [ ] Server: `PollenStation.dailyHistoricalPath`; `PollenService.dailyHistorical(station)` in `MeteoSwissPollenService`, `FakePollenService` (per-file bytes/failure/record from 01) and the anonymous `PollenService` in `MeasurementServiceTest`. Add a verbatim `d_historical` fixture.
- [ ] Server: second `TtlCache` (24 h) in `HistoryService`; fetch `d_historical` only when the window starts before 1 January of the current Swiss year; merge with recent winning on overlap; stale on failure; `Failed` with nothing retained → `502`.
- [ ] App: range `SingleChoiceSegmentedButtonRow` (Week / Month / Year) in `DiaryScreen` → `DiaryViewModel.onRangeSelected`; a change keeps the current graph with a loading indicator until the new history arrives.
- [ ] App: x-tick thinning per range in `DiaryChartGeometry` (week: every day; month: ~weekly; year: month starts); range-aware chart description.
- [ ] CLAUDE.md: ranges, the `d_historical` file, both cache TTLs.

### Acceptance criteria

- [ ] `range=week` and `range=year` return 7 and 365 days ending yesterday; `HistoryWindowTest` pins the window at 1 January and across a leap day; `PollenStationTest` pins `dailyHistoricalPath`, and the fixture sits at that path.
- [ ] `HistoryServiceTest`: a year window in October requests `d_historical` and returns values for dates before 1 January; a week or month window wholly inside the current year never requests it (per-file request record); on a date present in both files the `d_recent` value wins.
- [ ] `HistoryServiceTest`: `d_historical` is not re-fetched within 24 hours and is after; a failed `d_historical` fetch with a retained copy still yields a result; with nothing retained the year request fails even though `d_recent` succeeds (route test: `502`).
- [ ] `DiaryViewModelTest`: selecting `WEEK` while the load is gated keeps the previous history in `Content` with `isLoading = true`, then replaces it.
- [ ] `DiaryChartGeometryTest` pins x-tick count and positions for each range; the chart-description test pins the wording for week, month and year.

### Quality gates

- [ ] `./gradlew :composeApp:testDebugUnitTest :server:test` passes.
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes.
- [ ] No new compiler warnings compared with `main`.
- [ ] No JVM-only API in `commonMain` and no comma in backtick test names.
- [ ] CLAUDE.md updated as listed in the steps.

---

## Task [03-record-feeling-on-home]

Once per Swiss day, after Home's reading has loaded, a card floats over Home's list asking "How do
you feel today?". One tap stores the answer on the device and hides the card until the next day;
closing it ("Not today") hides it for the day without recording anything. Nothing leaves the
device. (The answer becomes visible in the Diary in task 04.)

### Implementation steps

- [ ] App: `core/diary` — `Feeling` (+ `level`), `DiaryEntry`, `DiaryRepository` (`entries`, `dismissedOn`, `record` that never overwrites a date, `dismiss`), pure `DiaryCodec`, logic-free `DataStoreDiaryRepository`, `swissToday(clock)`; Koin wiring; `FakeDiaryRepository` in `commonTest`.
- [ ] App: `HomeViewModel` takes `DiaryRepository` (update its `presentationModule` registration); `Content` gains `showFeelingPrompt` and `feelingSaveError`, re-derived on every `Content` emission and diary change; `onFeelingSelected` / `onFeelingPromptDismissed`.
- [ ] App: `FeelingPrompt` composable (heading, buttons Very bad → Very good left to right with full words, close announced as "Not today"), floated bottom-centre over Home's list; the list's bottom padding grows by the card's height while shown.
- [ ] CLAUDE.md: `core/diary`, device-only storage, the Home prompt.

### Acceptance criteria

- [ ] `DiaryCodecTest`: round trip, empty list, corrupt input → empty list, unknown feeling skipped; `FeelingTest` pins all four levels.
- [ ] `HomeViewModelTest`: the prompt is in `Content` only (never `Loading` or `Error`); it hides after an answer (via a diary change, without a reload) and after a dismissal for today; a dismissal records no entry; it shows again once the clock is on the next Swiss day; a failed save keeps it with `feelingSaveError` set.
- [ ] The existing `HomeViewModelTest` cases and all Alarms tests pass unchanged.
- [ ] No API service or request DTO in the app takes a `Feeling` or `DiaryEntry` (`grep` over `composeApp/src/commonMain/**/data/remote`).
- [ ] *(emulator)* The card appears over a loaded Home and not while loading or on error; the last species row can be scrolled fully above it; a tap hides it for the rest of the day across an app restart; with airplane mode on, an answer still saves; Home's pull-to-refresh and retry still work.
- [ ] *(emulator)* TalkBack reads the question as a heading, each button by its full word and the close button as "Not today".

### Quality gates

- [ ] `./gradlew :composeApp:testDebugUnitTest :server:test` passes.
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes.
- [ ] `./gradlew :composeApp:assembleDebug` passes.
- [ ] No new compiler warnings compared with `main`.
- [ ] No JVM-only API in `commonMain` and no comma in backtick test names.
- [ ] CLAUDE.md updated as listed in the steps.

---

## Task [04-feeling-line-in-diary]

The Diary graph shows the user's recorded answers as the feeling line — thick, `onSurface`, a dot
on every answered day, gaps on unanswered ones — on the shared scale (Very good = Low … Very bad =
Very high), with feeling words on the axis's second side. Today's answer is never plotted. A
period with no answers shows a hint pointing to Home.

### Implementation steps

- [ ] App: `DiaryViewModel` takes `DiaryRepository` (update its registration) and combines `entries`, filtered to the history window (today excluded).
- [ ] App: `DiaryChartGeometry` gains the feeling polylines and dots; `DiaryChart` draws them and labels the second axis side with feeling words.
- [ ] App: overlay the hint "Answer 'How do you feel today?' on Home to see your line here." when the window holds no entries.
- [ ] CLAUDE.md: the feeling line and the shared scale.

### Acceptance criteria

- [ ] `DiaryViewModelTest`: entries outside the window and today's entry are not in `Content`; a new entry for a day inside the window appears without a history reload.
- [ ] `DiaryViewModelTest`: `Content` reports "no entries" exactly when the window holds none.
- [ ] `DiaryChartGeometryTest`: a "Very bad" entry lands at the same y as `VERY_HIGH` and "Very good" at `LOW`; the feeling line splits at an unanswered day, with a dot on each answered day including an isolated one.
- [ ] *(emulator)* After answering on Home and moving the device date one day forward, the Diary shows the answer as a dot at the matching height; with no answers in the period the hint is shown above the pollen lines.

### Quality gates

- [ ] `./gradlew :composeApp:testDebugUnitTest :server:test` passes.
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes.
- [ ] `./gradlew :composeApp:assembleDebug` passes.
- [ ] No new compiler warnings compared with `main`.
- [ ] No JVM-only API in `commonMain` and no comma in backtick test names.
- [ ] CLAUDE.md updated as listed in the steps.

---

## Task [05-diary-station-choice-and-species-filters]

In the Diary the user picks any station (default home, never written back) and switches pollen
types on and off with checkboxes that double as the legend. Types the station does not measure in
the period are disabled with "Not measured here". Choices survive station and range changes and
tab switches, but not an app restart.

### Implementation steps

- [ ] App: `DiaryViewModel.onStationSelected` (keeps the old graph while loading, never touches `SelectedStationRepository`); station `ExposedDropdownMenuBox` in `DiaryScreen`, names from `StationRepository`.
- [ ] App: keep the chosen station, range and `checked` in ViewModel fields (not only in `Content`), so a failed reload's `Error` and its Retry resume the user's choices.
- [ ] App: species from `SpeciesRepository`; `checked` (all by default) and `onSpeciesToggled`, which never reloads; derive "not measured here" (every day in the window `null`).
- [ ] App: checkbox rows with a colour sample in species order; disabled rows labelled "Not measured here"; the geometry omits unchecked and unmeasured species.
- [ ] CLAUDE.md: station choice, filters, "Not measured here".

### Acceptance criteria

- [ ] `DiaryViewModelTest`: selecting another station reloads its history, keeps the previous graph while gated, and leaves `FakeSelectedStationRepository` unchanged.
- [ ] `DiaryViewModelTest`: all species are checked initially; a toggle changes `checked` without a history request; `checked` survives a station and a range change.
- [ ] `DiaryViewModelTest`: a failed reload after a station change is `Error`; Retry reloads the chosen station and range and `checked` is unchanged.
- [ ] `DiaryViewModelTest`: a species with no value on any day in the window is reported as not measured, and as measured for a station that reports it; `DiaryChartGeometryTest`: an unchecked species produces no line.
- [ ] *(emulator)* Choosing Basel leaves Home on the home station; Basel stays selected after switching tabs and back; after an app restart the Diary opens on the home station; an unmeasured type's checkbox is disabled and reads "Not measured here".

### Quality gates

- [ ] `./gradlew :composeApp:testDebugUnitTest :server:test` passes.
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes.
- [ ] `./gradlew :composeApp:assembleDebug` passes.
- [ ] No new compiler warnings compared with `main`.
- [ ] No JVM-only API in `commonMain` and no comma in backtick test names.
- [ ] CLAUDE.md updated as listed in the steps.
