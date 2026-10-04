# Pollen Diary

## Problem Statement

People with hay fever often don't know which pollen they react to. The app shows today's pollen
levels and can alert on them, but it gives users no way to record how they actually felt, or to
compare those days with the pollen in the air. Without that record, a user can only guess whether
their bad days line up with birch, grasses or something else. And memories of how one felt a week
or a month ago are unreliable.

## Solution

A diary in two parts:

1. **A daily question on Home.** Once a day, when the home station's reading has loaded, a small
   card floats at the bottom of the Home screen and asks "How do you feel today?". The user answers
   with one tap: Very bad, Bad, Good or Very good. They can also close the card for the day without
   answering. The answer is stored on the phone only.

2. **A Diary tab** (the third tab in the bottom bar). It shows a graph of how the user felt over the
   last week, month or year, together with the daily pollen levels of every pollen type at a chosen
   station. Each pollen type can be switched on or off. Feelings and pollen levels share one scale:
   "Very bad" sits at the same height as "Very high" pollen, so a pollen type whose line rises with
   the user's bad days stands out. A note on the screen makes clear that this is not a medical
   diagnosis and that a doctor should be consulted.

## User Stories

### Recording how I feel

1. As a user, I want the app to ask me once a day how I feel, so that I build up a record without
   having to remember to do it.
2. As a user, I want to answer with a single tap, so that recording takes no effort.
3. As a user, I want four clear choices (Very bad, Bad, Good, Very good), so that I can answer
   quickly without overthinking.
4. As a user, I want the choices ordered from Very bad on the left to Very good on the right, so
   that the scale reads consistently.
5. As a user, I want the question to appear on the Home screen, which I open anyway, so that I
   don't have to go to another screen.
6. As a user, I want the question to float above the bottom bar without hiding Home's content
   permanently, so that I can still scroll to every pollen type behind it.
7. As a user, I want the question to appear only after Home has loaded its pollen reading, so that
   it never covers a loading spinner or an error message.
8. As a user, I want the question to disappear as soon as I have answered, so that it doesn't get in
   my way for the rest of the day.
9. As a user, I want to close the question for today without answering, so that I'm not pushed to
   record a day I don't want to record.
10. As a user, I want a closed question to stay closed until tomorrow, even if I restart the app, so
    that it doesn't keep coming back.
11. As a user, I want the question to come back the next day, so that I can record every day.
12. As a user, I want "a day" to mean the Swiss calendar day, so that my answers line up with the
    Swiss pollen data they are compared with.
13. As a user, I want to see a message if my answer could not be saved, so that I know to try again.
14. As a user, I want my answers to stay on my phone, so that my health information isn't sent
    anywhere.
15. As a user, I want my answers to be kept indefinitely, so that I can look back over long periods
    and move them to an account later.
16. As a screen reader user, I want the question announced as a heading and each choice announced by
    its full word, so that I can answer without seeing the screen.
17. As a screen reader user, I want the close button announced as "Not today", so that I know what
    it does.

### Viewing the diary

18. As a user, I want a Diary tab in the bottom bar, so that I can reach my diary from anywhere in
    the app.
19. As a user, I want the tab to show a book icon and be announced as "Diary", so that I can
    recognise it.
20. As a user, I want the diary to open on my home station, so that I see the place I usually care
    about without choosing it.
21. As a user, I want to choose any other station in the diary, so that I can compare my feelings
    with the pollen where I work or where I spent time.
22. As a user, I want choosing a station in the diary to leave my home station unchanged, so that
    browsing never changes what Home shows.
23. As a user, I want the station I chose in the diary to stay while I switch tabs, so that I don't
    lose my place.
24. As a user, I want my feeling line to stay the same whichever station I pick, so that I can see
    which station's pollen fits my days best.
25. As a user, I want to choose between the last week, the last month (30 days) and the last year
    (365 days), so that I can look for short-term and seasonal patterns.
26. As a user, I want the diary to show the last month by default, so that I see enough days to spot
    a pattern.
27. As a user, I want the graph to end with yesterday, so that every day shown has complete pollen
    data.
28. As a user, I want my feelings shown as one clearly distinguishable line, so that I can follow
    them among the pollen lines.
29. As a user, I want a dot on every day I answered, so that single answers between missing days
    are still visible.
30. As a user, I want one line per pollen type in its own colour, so that I can tell the types apart.
31. As a user, I want pollen lines to show severity levels (None to Very high) rather than raw
    numbers, so that types with very different typical amounts can be compared fairly.
32. As a user, I want feelings and pollen levels on one shared scale, with Very bad aligned to Very
    high, so that a matching pattern shows up as lines rising together.
33. As a user, I want the scale labelled with both feeling words and severity words, so that I
    understand what each height means for both kinds of line.
34. As a user, I want days without an answer or without pollen data shown as gaps rather than
    connected lines, so that the graph never pretends to know a missing day.
35. As a user, I want a checkbox for every pollen type, all checked at first, so that I start with
    the full picture and can narrow it down.
36. As a user, I want each checkbox to show its line's colour, so that the checkboxes also work as
    the graph's legend.
37. As a user, I want unchecking a pollen type to remove its line, so that I can focus on the types I
    suspect.
38. As a user, I want my checkbox choices kept when I change the station or the period, so that I
    don't have to redo them.
39. As a user, I want pollen types the selected station does not measure shown as disabled with
    "Not measured here", so that I don't mistake a missing line for no pollen.
40. As a user, I want a hint telling me how to add my line when the period holds none of my answers,
    so that a new diary isn't confusing.
41. As a user, I want the previous graph kept on screen while a new station or period loads, so that
    the screen doesn't flash empty.
42. As a user, I want a clear error with a Retry button when the pollen history can't be loaded, so
    that I can try again.
43. As a user, I want a visible note that the diary is not a medical diagnosis and that I should see
    a doctor if I suspect an allergy, so that I don't draw medical conclusions from the graph alone.
44. As a screen reader user, I want the graph announced with a short description of what it shows
    and for which period, so that I know what is on screen.

### Pollen history

45. As a user, I want daily pollen levels for every station and every pollen type over the last
    year, so that the diary can compare them with my feelings.
46. As a user, I want the history to be classified using the same severity bands as the rest of the
    app, so that "High" means the same everywhere.
47. As a user, I want the history to stay available when the data source is briefly unreachable, so
    that a short outage doesn't break my diary.

### Product owner

48. As the product owner, I want the diary to work on Android and iOS alike, so that it doesn't
    depend on push notifications or an account.
49. As the product owner, I want the diary to cause only a few requests to the data publisher however
    many users open it, so that the service stays cheap and well-behaved.
50. As the product owner, I want the stored answers to be movable to a future account feature, so
    that users don't lose their history when accounts arrive.

## User Acceptance Tests

### Home question

1. Given the user has not answered or dismissed the question today, when Home finishes loading the
   home station's reading, then a card asking "How do you feel today?" floats above the bottom bar
   with the buttons Very bad, Bad, Good and Very good from left to right, and a close button.
2. Given Home is still loading, when the user looks at Home, then the question is not shown.
3. Given Home shows an error, when the user looks at Home, then the question is not shown.
4. Given the question is shown, when the user scrolls Home's pollen list, then the list scrolls
   behind the card and the last pollen type can be scrolled fully into view above it.
5. Given the question is shown, when the user taps "Bad", then the card disappears and doesn't
   reappear for the rest of the day, including after switching tabs and after restarting the app.
6. Given the question is shown, when the user taps the close button, then the card disappears and
   doesn't reappear for the rest of the day, including after restarting the app, and no answer is
   recorded for that day.
7. Given the user answered or dismissed the question yesterday, when they open Home today, then the
   question is shown again.
8. Given the question is shown, when the user switches to another tab, then no question is shown
   there.
9. Given a screen reader is on, when focus reaches the card, then the question is read as a heading,
   each button by its full word, and the close button as "Not today".

### Diary screen

10. Given the app is set up, when the user looks at the bottom bar, then the third tab shows a book
    icon and is announced as "Diary".
11. Given the home station is Zürich, when the user opens the Diary tab for the first time, then
    Zürich is selected, the period "Month" is selected, and the graph covers the 30 days ending
    yesterday.
12. Given the diary is open, when the user selects "Week" or "Year", then the graph covers the 7 or
    365 days ending yesterday.
13. Given the user answered today, when they open the diary, then today's answer is not in the graph.
    When they open it the next day, the answer appears as the last point.
14. Given the user answered "Very bad" on a day when grasses were "Very high", when they view that
    period, then the feeling dot and the grasses line are at the same height on that day.
15. Given the user answered on Monday and Wednesday but not Tuesday, when they view that week, then
    the feeling line shows dots on Monday and Wednesday and no line connecting them across Tuesday.
16. Given all pollen types are checked, when the user unchecks Birch, then the birch line disappears
    and the other lines stay.
17. Given Birch is unchecked, when the user changes the station or the period, then Birch stays
    unchecked.
18. Given the selected station does not measure a pollen type during the period, when the diary
    shows that station, then that type's checkbox is disabled and labelled "Not measured here".
19. Given the user picks Basel in the diary, when they go to Home, then Home still shows Zürich.
20. Given the user picked Basel in the diary, when they switch to another tab and back, then Basel is
    still selected. After an app restart the diary opens on the home station again.
21. Given the user picks another station, when the switch happens, then the previous graph stays
    visible with a loading indicator until the new one is ready.
22. Given the user has no answers in the selected period, when they view the diary, then the pollen
    lines are shown with the hint "Answer 'How do you feel today?' on Home to see your line here."
23. Given the backend can't be reached, when the user opens the diary, then an error message with a
    Retry button is shown. When Retry is tapped once the backend is reachable again, the graph is
    shown.
24. Given any diary state with a graph, when the user scrolls to the bottom, then the note "\* This
    is not a medical diagnosis. If you suspect a pollen allergy, please see a doctor." is visible,
    and the line above the graph ends with a matching asterisk.
25. Given a screen reader is on, when focus reaches the graph, then it is read as one element
    describing the diary and pollen types and the selected period.

### History and robustness

26. Given the period "Year" in October, when the diary loads, then pollen levels are shown for
    every day back to October of the previous year, including the days before 1 January.
27. Given a day for which the publisher has no value at the selected station, when the diary shows
    that day, then each pollen line has a gap on it.
28. Given the diary loaded once, when the data publisher becomes unreachable and the user reloads,
    then the previously loaded history is still shown instead of an error.
29. Given a user switches the phone into airplane mode, when they answer the Home question, then the
    answer is still saved (it needs no network).

## Definition of Done

- All user acceptance tests pass on an Android emulator.
- The iOS sources compile, including tests.
- All automated tests listed in the Technical Annex exist and pass.
- The third bottom bar tab is the Diary. No "Feature 3" placeholder remains.
- Feeling answers are stored on the device only. No request made by the app contains them.
- The history endpoint is documented alongside the existing REST API, including its error cases.
- The new species line colours have light and dark values with their contrast against the surface
  recorded, and none of them can be mistaken for a severity colour.
- The project documentation (CLAUDE.md) describes the diary feature, its storage and the history
  endpoint.
- No regression in Home, All stations or Alarms.

## Out of Scope

- Editing or deleting a recorded answer, and recording answers for past days.
- More than one answer per day, symptoms, medication or free-text notes.
- Syncing, backing up or exporting answers, and accounts (planned as a later feature).
- Reminders or push notifications asking the user to answer.
- Tapping or scrubbing the graph to see a day's exact values (no tooltip).
- Showing today in the graph, or using hourly values in it.
- Any automatic analysis, correlation score or "you are probably allergic to …" statement.
- Custom date ranges beyond week, month and year.
- Changing the home station from the diary.
- Remembering the diary's station, period or checkbox choices across app restarts.
- Pull-to-refresh on the diary screen.
- Showing the question anywhere other than Home.

## Further Notes

- **Feeling to severity mapping:** Very good = Low, Good = Moderate, Bad = High, Very bad = Very
  high. A higher point always means worse. "None" has no feeling counterpart.
- **Daily data is more accurate here than on Home.** The severity bands are defined for daily mean
  concentrations. Home applies them to hourly readings and skews high (an accepted limitation). The
  diary uses daily means, which is what the bands were made for.
- The answer is about the user, not a place, so it is stored without a station.
- Answers survive app updates but not clearing the app's data or reinstalling. That's accepted
  until accounts exist.
- MeteoSwiss's daily automatic measurements start on 1 January 2023, so a year-long window is always
  covered.

---

## Technical Annex
> Written against codebase as of: 2026-10-04

### Architectural Decisions

#### Upstream data (verified on 2026-10-04)

- `<abbr>/ogd-pollen_<abbr>_d_recent.csv`: year to date, one row per day, already containing
  yesterday. `<abbr>/ogd-pollen_<abbr>_d_historical.csv`: all earlier years, ending 31 December of
  last year (~560 KB for PZH). Same format as the hourly files (`;`, ISO-8859-1, header row), with
  `reference_timestamp` as `dd.MM.yyyy 00:00` and columns for both `…d0` and `…d1`. **We read the
  `d0` columns only** (`PollenSpecies.dailyCode`). The date part of `reference_timestamp` is the
  day's date, used as-is as the Swiss date the day pairs with.
- `PollenStation` already has `dailyRecentPath`. Add `dailyHistoricalPath`
  (`pathFor(granularity = "d", frequency = "historical")`). Check the path against a fixture.

#### Server

- **`PollenService`** gains `suspend fun dailyRecent(station): ByteArray` and
  `suspend fun dailyHistorical(station): ByteArray`, implemented in `MeteoSwissPollenService` (same
  non-2xx → throw rule) and `FakePollenService` (programmable bytes and failures per file).
- **`PollenCsvParser.parseDaily(bytes): Map<LocalDate, Map<PollenSpecies, Int?>>`**: pure. Every
  row with a parseable date becomes an entry. Every `PollenSpecies` key is present, with `null` for
  an empty cell or a missing column. Rows with no value at all are kept as all-`null` (equivalent to
  a gap). Decoding goes through `decodePublished`.
- **`HistoryRange`** enum `WEEK(7)`, `MONTH(30)`, `YEAR(365)`, parsed from the lowercase query value
  (`week|month|year`, exact match; anything else or missing → `400 {error}`).
- **`historyWindow(range, today: LocalDate): ClosedRange<LocalDate>`**: pure, `until = today - 1`,
  `from = until - (days - 1)`.
- **`HistoryService(pollenService, thresholds, clock)`**:
  - `suspend fun historyFor(station, range): CacheResult<StationHistory>`. "Today" is
    `LocalDate.now(clock.withZone(ALARM_ZONE))`, Europe/Zurich.
  - Two `TtlCache<PollenStation, Map<LocalDate, …>>`: `recentCache` with a TTL of
    `RECENT_TTL = 3h` and `historicalCache` with `HISTORICAL_TTL = 24h`. Both cache the *parsed*
    rows. Classification happens per request, so threshold changes apply immediately.
  - `d_historical` is only requested when `window.start < 1 January of today's year`.
  - Rows are merged with recent winning on an overlapping date. Every date in the window gets an
    entry (all `null` if absent), oldest first.
  - Failure semantics follow `MeasurementService`: a stale cache entry is used. If either required
    file is `Failed` with nothing retained, the whole result is `Failed` (→ `502`). Dates the stale
    copy doesn't cover yet come back as `null`.
  - Wired once in `Application.module()` and passed to `configureRouting(...)` with a default, as
    `MeasurementService` is. The alarm scheduler doesn't use it.
- **Route** in `PollenRoutes.kt`: `GET /pollen/stations/{abbr}/history?range=week|month|year`.
  - Unknown abbr (case-insensitive lookup) → `404`, bad range → `400 {error}`, `Failed` → `502`.
  - Range validation comes before the station lookup, consistent with alarm validation.
  - Wire DTOs in `pollen/model/`:

  ```json
  { "stationAbbr": "PZH", "range": "month", "from": "2026-09-04", "until": "2026-10-03",
    "days": [ { "date": "2026-09-04",
                "species": [ { "id": "BIRCH", "concentration": 12, "severity": "LOW" },
                             { "id": "ASH", "concentration": null, "severity": null } ] } ] }
  ```

  All seven species on every day, in `PollenSpecies` order. `concentration` and `severity` are
  `null` together, as for measurements. Dates are ISO `yyyy-MM-dd`.

#### App: `core/diary`

- `enum class Feeling { VERY_BAD, BAD, GOOD, VERY_GOOD }` with `val level: PollenSeverity`
  (`VERY_GOOD → LOW`, `GOOD → MODERATE`, `BAD → HIGH`, `VERY_BAD → VERY_HIGH`).
- `data class DiaryEntry(val date: LocalDate, val feeling: Feeling)`, one per date.
- `interface DiaryRepository`:
  - `val entries: Flow<List<DiaryEntry>>`, sorted by date
  - `val dismissedOn: Flow<LocalDate?>`
  - `suspend fun record(date: LocalDate, feeling: Feeling): Result<Unit>`, which ignores a date that
    already has an entry (answers are immutable)
  - `suspend fun dismiss(date: LocalDate): Result<Unit>`
- `DataStoreDiaryRepository` uses the existing `DataStore<Preferences>` with two string keys: the
  entries as JSON produced by `DiaryCodec`, and the dismissed date as ISO text. It stays logic-free
  and untested, like `DataStoreSelectedStationRepository`.
- `DiaryCodec` is pure, with `encode(List<DiaryEntry>): String` and `decode(String): List<DiaryEntry>`
  (kotlinx.serialization with private `@Serializable` DTOs, dates ISO, feelings as enum names).
  Unknown feelings and malformed elements are skipped, and corrupt input decodes to an empty list
  rather than crashing.
- `swissToday(clock: Clock): LocalDate` uses `TimeZone.of("Europe/Zurich")` and is the single
  definition of "today" in the app.
- Retention is unlimited and nothing is ever pruned.
- Koin: `single<DiaryRepository> { DataStoreDiaryRepository(get()) }` in `dataModule`.

#### App: `core/history`

- `StationHistoryApiService(client, baseUrl)` → `GET /pollen/stations/{abbr}/history?range=…`. DTOs
  and a mapper live under `data/`.
- `StationHistoryRepository.history(abbr, range: HistoryRange): Result<StationHistory>` with
  `safeCall`. The domain model is
  `StationHistory(stationAbbr, from, until, days: List<HistoryDay(date, levels: Map<String, PollenSeverity?>)>)`,
  keyed by species id. Severity strings map through the existing `toPollenSeverity`.
- It sits in `core/` rather than `feature/diary` because it's a reading-pipeline counterpart of
  `core/measurement`. If keeping it in `feature/diary/data` turns out simpler while it has only one
  consumer, that's acceptable, and the task step may decide.

#### App: Home prompt (`feature/home`)

- `HomeViewModel` additionally takes `DiaryRepository` and uses its existing injected `Clock`.
- `HomeUiState.Content` gains `showFeelingPrompt: Boolean` and `feelingSaveError: String?`.
  `showFeelingPrompt = no entry for swissToday(clock) && dismissedOn != swissToday(clock)`, combined
  from the diary flows. Loading and Error never carry the prompt.
- `onFeelingSelected(feeling)` → `record(today, feeling)`. On failure the prompt stays and
  `feelingSaveError` is set. `onFeelingPromptDismissed()` → `dismiss(today)`.
- "Today" is evaluated whenever Home produces `Content` (load, refresh, selection change, diary
  change). A Swiss midnight passing while Home stays open shows the prompt at the next such event.
  That's accepted.
- `FeelingPrompt` composable: a card aligned bottom-centre in a `Box` over the list, with elevation
  and margins. The list's `contentPadding.bottom` grows by the card's measured height while it is
  shown. The question is a heading, the buttons are full words, Very bad → Very good from left to
  right, and the close `IconButton` is announced as "Not today".

#### App: `feature/diary`

- `DiaryViewModel(selectedStationRepository, stationRepository, speciesRepository,
  stationHistoryRepository, diaryRepository, clock = Clock.System)`.
- `DiaryUiState`: `Loading` | `Content(stations, selectedAbbr, range, species, checked: Set<String>,
  history, entries, isLoading)` | `Error(message)`.
  - The default station is the stored home station (the first listed station if it's no longer
    listed). The default range is `MONTH`, and all species are checked.
  - `onStationSelected` and `onRangeSelected` reload the history with `isLoading = true` and keep
    the previous `history` until the new one arrives. A failed reload from `Content` turns into
    `Error` with Retry (decided: Error with Retry on a failed history request).
  - `onSpeciesToggled(id)` changes `checked` only, with no reload.
  - The selection never writes `SelectedStationRepository`. It survives tab switches through the
    ViewModel's saved navigation state and isn't persisted.
  - The feeling entries come from `diaryRepository.entries`, filtered to the history window.
  - A species is "not measured here" when every day in the window is `null` for it.
- `DiaryChartGeometry` is pure, with no Compose import:
  - Input: window dates, entries, per-species levels, checked set, and the plot size in px.
  - Output: per line, a list of polylines split at missing days (single-point runs included), the
    feeling dots, y-ticks for the 5 levels, and x-ticks thinned per range (week: each day; month:
    roughly weekly; year: month starts).
  - Mapping: x by day index over the window; y with `NONE = 0 … VERY_HIGH = 4`, inverted to screen
    coordinates so higher means worse.
- `DiaryChart` is a canvas drawing the geometry. The feeling line is thicker, in
  `colorScheme.onSurface`, with dots. Species lines are thinner and use their palette colour. The
  y-axis is labelled with severity words on one side and feeling words on the other. The whole
  chart is one `clearAndSetSemantics` node: "Graph of your diary and N pollen types, last 30 days"
  (the wording follows the range).
- `DiaryScreen`, top to bottom: `TopAppBar("Diary")`, station `ExposedDropdownMenuBox`,
  `SingleChoiceSegmentedButtonRow` (Week / Month / Year), the line "Compare how you felt with the
  pollen levels at a station.\*", the chart (with the empty-entries hint overlaid when the window
  has no entries), checkboxes with colour samples in species order, and the disclaimer in
  `bodySmall` / `onSurfaceVariant`. The screen scrolls vertically. There's no pull-to-refresh.

#### Theme

- `:theme/SpeciesPalette.kt` holds 7 categorical colours `speciesAlderLight` …
  `speciesGrassesDark`, each with an explicit light and dark value and a recorded contrast against
  `surfaceLight` / `surfaceDark` (≥ 3:1). They must be visually distinct from the `SeverityPalette`
  hues (no traffic-light green, amber, orange or red).
- The mapping from species id to colour lives in the app (for example `core/ui/species/SpeciesColors.kt`),
  using the same surface-luminance rule as `PollenSeverity.color()`.

#### Navigation

- `Screen.Feature3` → `Screen.Diary`.
- `TopLevelDestination.FEATURE_3` → `DIARY(Screen.Diary, Icons.AutoMirrored.Filled.MenuBook, "Diary")`.
- `composable<Screen.Diary> { DiaryScreen(...) }` in `AppNavigation`.
- Register `DiaryViewModel` in `presentationModule`.

### Automated Testing Decisions

Good tests check observable behaviour through a module's public interface: returned values,
emitted states and HTTP responses. They don't check private helpers or call order. Use hand-written
fakes (no mocking framework), backtick sentence names without commas, and pin boundaries from both
sides.

**Server (`server/src/test`)**

- **`PollenCsvParserTest` (daily):** parses checked-in verbatim `d_recent` and `d_historical`
  fixtures (add them under `fixtures/ogd-pollen/<abbr>/`). Checks the d0 columns as opposed to d1,
  empty cells as `null`, a missing column as `null`, malformed dates skipped, and ISO-8859-1
  decoding. Prior art: the existing hourly parser tests.
- **`HistoryWindowTest`:** each range ends yesterday with the right length, plus the edges at
  1 January and in a leap year.
- **`HistoryServiceTest`** with `FakePollenService` and `MutableClock`:
  - The window ends yesterday in Swiss time around midnight (UTC vs Europe/Zurich).
  - `d_historical` is requested only when the window crosses 1 January.
  - Merging with recent winning on overlap, and missing dates filled with `null`.
  - Classification with injected thresholds at the band edges.
  - Both TTLs (no refetch just before expiry, a refetch after).
  - A stale value served on failure, and `Failed` with nothing retained.
  - Prior art: `MeasurementService` / `TtlCache` tests.
- **`PollenRoutesTest` (history):**
  - `200` shape with all seven species per day in order.
  - `400` for a missing or unknown range, before the station lookup.
  - `404` for an unknown station, and the case-insensitive abbreviation.
  - `502` on failure with nothing retained.
- **`MeteoSwissPollenServiceTest`:** the daily recent and historical paths through `MockEngine`, and
  non-2xx → throw.

**App (`composeApp/src/commonTest`)**

- **`DiaryCodecTest`:** round trip, an empty list, corrupt input → empty list, unknown feelings
  skipped.
- **`FeelingTest`:** the four-way mapping to severity levels.
- **`StationHistoryRepositoryImplTest`:** through a real `HttpClient` + `MockEngine` (via
  `createHttpClient(engine)`). Covers the request path and query, DTO → domain mapping including
  `null` days, and `Failure` on 4xx, 5xx and transport errors. Prior art:
  `StationMeasurementRepositoryImpl` tests.
- **`HomeViewModelTest` (prompt):**
  - Shown in `Content` only, never in `Loading` or `Error`.
  - Hidden after an answer or a dismissal for today, and shown again when the clock moves to the
    next Swiss day.
  - A failed save keeps the prompt and sets the error.
  - Uses `FakeDiaryRepository` and a controllable `Clock`.
- **`DiaryViewModelTest`:**
  - Defaults: home station, `MONTH`, all checked.
  - A station or range change keeps the old history while loading (gated fake), and never writes
    the home station.
  - Checks survive a reload.
  - "Not measured here" when every day in the window is `null` for a species.
  - Entries are filtered to the window, and today is excluded.
  - `Error` and Retry.
  - Prior art: `HomeViewModelTest` with `FakeStationMeasurementRepository.gate`.
- **`DiaryChartGeometryTest`:**
  - Positions for the first, last and middle day.
  - The level → y inversion.
  - Gaps split polylines, and isolated single points survive.
  - Unchecked species are absent.
  - Tick thinning per range.
  - Prior art: `SwissMapProjection` / `StationHitTest` tests.
- **`TopLevelDestinationTest`:** updated order and names ("Diary" third).
- **New fakes:** `FakeDiaryRepository`, `FakeStationHistoryRepository` (with a `gate`).

**Checked by hand:** the `DiaryChart` canvas and `DiaryScreen` layout, the `FeelingPrompt` overlay
and list padding, `DataStoreDiaryRepository`, and the `SpeciesPalette` contrast (recorded next to
each value).

Always run `:composeApp:testDebugUnitTest`, `:server:test` and
`:composeApp:compileTestKotlinIosSimulatorArm64`.
