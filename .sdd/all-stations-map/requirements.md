# All Stations Map

## Problem Statement

The app tells a user about one station only — the one they picked during onboarding. Someone who
travels, commutes between cities, or plans a day trip has no way to see how pollen levels compare
across Switzerland, or to look up the readings of a station other than their own without changing
their setup. The second tab of the bottom navigation bar currently shows a "Coming soon"
placeholder.

## Solution

The second tab becomes **All stations**: a screen with a static map of Switzerland at the top and a
list of all fifteen MeteoSwiss pollen stations below it.

- The map stays fixed at the top while the list scrolls underneath. Every station appears on the
  map as a dot coloured by its current overall severity — the worst severity among the taxa it
  measures.
- Each list entry shows the station's name, and below it a severity bar filled to that overall
  severity together with the severity word.
- Tapping a dot on the map or a station in the list selects that station: its dot is circled, its
  list entry expands to show the concentration and severity of every taxon along with how current
  the reading is, and the list scrolls so the expanded entry is visible.
- The tab is identified by a small outline of Switzerland in the bottom bar.

Readings are fetched for every station independently, so a station that cannot currently be read
shows "No reading" without affecting the others. Readings that are no longer current are marked as
such, both in the collapsed entry and in the expanded detail.

## User Stories

### Navigation and entry

1. As a user, I want the second tab of the bottom bar to open the All stations screen, so that I can compare stations without leaving my home screen setup.
2. As a user, I want the second tab to show a small outline of Switzerland, so that I can recognise it as the country-wide view.
3. As a screen-reader user, I want the tab to be announced as "All stations", so that I know where it leads without seeing its icon.
4. As a user, I want the screen's title to read "All stations", so that the title and the tab's name agree.
5. As a user, I want switching away from the tab and back to show the screen as I left it, so that I don't lose my place or my selection.
6. As a user, I want the back gesture from All stations to return me to Home, so that the tab behaves like every other tab.
7. As a user, I want the remaining placeholder tabs to stay unchanged, so that only the feature that now exists looks different.

### Map

8. As a user, I want a map of Switzerland at the top of the screen, so that I can see where the stations are.
9. As a user, I want the map to stay in place while I scroll the list, so that I can always see the overview and tap a station on it.
10. As a user, I want the whole of Switzerland to be visible on the map without cropping, so that every station is reachable.
11. As a user, I want every one of the fifteen stations to appear as a dot at its true position, so that I can find a station by geography.
12. As a user, I want each dot coloured by its station's overall severity, so that I can see at a glance where pollen is worst.
13. As a user, I want a station without a reading to appear as a hollow, muted dot, so that I don't mistake missing data for clean air.
14. As a user, I want the dots to appear in a neutral colour while readings are still loading, so that I see the map immediately instead of waiting for all data.
15. As a user, I want the dot colours to follow the light or dark appearance of the app, so that they stay legible in both.
16. As a user, I want the map to remain a reasonable size on small screens and in landscape, so that the list below still has room.
17. As a user, I want a tap close to but not exactly on a dot to select the nearest station, so that I can hit small dots easily.
18. As a user, I want a tap between two nearby stations (for example Locarno and Lugano) to select whichever is closer, so that the result is never ambiguous.
19. As a user, I want a tap on empty map area to do nothing, so that I don't change my selection by accident.
20. As a user, I want the map to work without an internet map service, so that it loads instantly and needs no account or key.

### List

21. As a user, I want to see all fifteen stations in a list below the map, so that I can read every station's level.
22. As a user, I want the list sorted alphabetically by station name, so that I always find a station in the same place.
23. As a user, I want the list order not to change when readings change, so that the list doesn't reshuffle under me.
24. As a user, I want each entry to show the station name on its own line, so that it is easy to scan.
25. As a user, I want each entry to show a severity bar with the severity word next to it, so that I can read the level without relying on colour.
26. As a user, I want the bar filled to the station's worst current severity, so that a severe taxon is never hidden behind milder ones.
27. As a user, I want a station with no reading to show "No reading" with an empty bar, so that I can tell missing data from low pollen.
28. As a user, I want a station whose reading is not current to show a warning icon next to its severity, so that an old reading never passes for a current one.
29. As a user, I want station names shown in their local spelling (Genève, Zürich, Neuchâtel), so that they match what I know.
30. As a user, I want entries that are still loading to show a placeholder, so that I know data is on its way.

### Selection and detail

31. As a user, I want tapping a dot on the map to select that station, so that I can explore by geography.
32. As a user, I want tapping a station in the list to select it, so that I can explore by name.
33. As a user, I want the selected station's dot circled on the map, so that I can see where it is.
34. As a user, I want the circle not to hide the dot's severity colour, so that I still see its level.
35. As a user, I want the selected station's list entry to expand, so that I can see the detail.
36. As a user, I want the expanded entry to list all seven taxa with their concentration and severity, so that I can check the pollen I react to.
37. As a user, I want the taxa ordered worst first, with taxa the station does not measure at the end, so that what matters is at the top.
38. As a user, I want a taxon the station does not measure to say "No data", so that I don't mistake it for zero pollen.
39. As a user, I want the concentration unit stated once in the expanded entry, so that I know what the numbers mean.
40. As a user, I want the expanded entry to say when the reading was taken, so that I know how current it is.
41. As a user, I want a clearly highlighted warning in the expanded entry when the reading is not current, so that I don't rely on old data.
42. As a user, I want an expanded station without a reading to say that no reading is available right now, so that I understand why there is no detail.
43. As a user, I want the list to scroll so that the expanded entry is fully visible, so that I see the concentrations without scrolling myself.
44. As a user, I want the scroll to be animated, so that I can follow where the list moved.
45. As a user, I want only one station expanded at a time, so that the list stays compact.
46. As a user, I want selecting a different station to collapse the previous one, so that the circle and the expanded entry always match.
47. As a user, I want tapping the selected station again (on the map or in the list) to deselect it, so that I can return to the plain overview.
48. As a user, I want no station selected when I first open the screen, so that I start with the overview.
49. As a user, I want my selection to remain after a refresh, so that I can watch a station's values update.
50. As a user, I want selecting a station here not to change my home station, so that browsing never changes my setup.

### Freshness, refresh and errors

51. As a user, I want to see when the readings were last refreshed, so that I can tell whether a refresh happened.
52. As a user, I want to pull down on the list to refresh all readings, so that I can get the latest values.
53. As a user, I want the current readings to stay visible while a refresh runs, so that the screen doesn't go blank.
54. As a user, I want each station's entry to fill in as soon as its own reading arrives, so that one slow station doesn't hold up the rest.
55. As a user, I want a station that fails to load to show "No reading" while the others display normally, so that one failure doesn't block the whole screen.
56. As a user, I want a full-screen error with a retry button when the station list itself cannot be loaded, so that I know what's wrong and can try again.
57. As a user, I want a full-screen error with a retry button when no station's reading can be loaded at all, so that I'm told the service is unreachable instead of seeing fifteen empty entries.
58. As a user, I want a full-screen loading indicator only until the station list is available, so that the map appears as early as possible.
59. As a user, I want returning to the tab not to trigger an automatic reload, so that the screen behaves predictably and the way I left it.

### Accessibility

60. As a screen-reader user, I want the map announced as one element describing that it shows the fifteen stations and that stations are selected in the list, so that I'm not forced through fifteen dots that repeat the list.
61. As a screen-reader user, I want each list entry to be one focus stop that announces the name, severity and whether it is expanded, so that I can operate the screen fully from the list.
62. As a screen-reader user, I want the not-current warning icon to be announced, so that I get the same warning as sighted users.
63. As a colour-blind user, I want every severity spelled out in words next to its colour, so that I can read the screen without distinguishing colours.

### Consistency with Home

64. As a user, I want severities, colours, bars and wording to look exactly as on Home, so that the two screens never contradict each other.
65. As a user, I want the Home screen to keep working exactly as before, so that this feature introduces no regressions.

## User Acceptance Tests

1. Given the app has completed onboarding, when the user taps the second tab of the bottom bar, then the All stations screen opens with the title "All stations".
2. Given the bottom bar is visible, when the user looks at the second tab, then it shows an outline of Switzerland while the third to fifth tabs still show the pin.
3. Given a screen reader is active, when focus lands on the second tab, then it is announced as "All stations".
4. Given the All stations screen has loaded, when the user looks at the map, then the whole outline of Switzerland is visible and fifteen dots are shown at plausible positions (e.g. Genève at the south-west end, Münsterlingen near Lake Constance, Lugano and Locarno south of the Alps).
5. Given readings have loaded, when the user compares a dot with its station's list entry, then the dot colour matches the colour of that entry's severity bar.
6. Given a station whose reading could not be loaded, when the user looks at the map and list, then its dot is hollow and muted and its entry shows an empty bar with "No reading".
7. Given the screen is open, when the user scrolls the list, then the map stays fixed at the top and only the list moves.
8. Given the screen has loaded, when the user reads the list, then all fifteen stations appear in alphabetical order of their names, each with its name on one line and a bar with a severity word on the next.
9. Given a station measuring birch at High and grasses at Low, when the user looks at its entry, then the bar and word show High.
10. Given a station whose reading is three hours old or older, when the user looks at its collapsed entry, then a warning icon appears next to its severity word.
11. Given no station is selected, when the user taps the Zürich dot on the map, then the dot is circled, the Zürich entry expands, and the list scrolls so the whole expanded entry is visible.
12. Given no station is selected, when the user taps the Basel entry in the list, then the Basel dot is circled and the Basel entry expands showing all seven taxa with concentration and severity.
13. Given an expanded entry, when the user reads it, then the taxa are ordered worst first, taxa the station does not measure appear last with "No data" and a dash, and the concentration unit is stated once.
14. Given an expanded entry with a fresh reading, when the user reads it, then it shows a quiet caption such as "Data from 09:00".
15. Given an expanded entry whose reading is not current, when the user reads it, then it shows the highlighted warning "These readings are not current" with the reading's time or date.
16. Given Zürich is selected, when the user taps the Bern entry, then the Zürich entry collapses, the circle moves to Bern, and the Bern entry expands.
17. Given Zürich is selected, when the user taps the Zürich dot or the Zürich entry again, then the entry collapses and no dot is circled.
18. Given the screen is open, when the user taps the map between Locarno and Lugano but closer to Lugano, then Lugano is selected.
19. Given the screen is open, when the user taps an area of the map far from any dot, then nothing changes.
20. Given a station is selected, when the user pulls down on the list, then a refresh indicator appears, the current readings stay visible, and afterwards the same station is still selected and expanded.
21. Given readings have loaded, when the user pulls to refresh, then the "Refreshed" caption shows the new time.
22. Given the backend responds slowly for one station, when the screen loads, then the other stations' entries fill in without waiting for it.
23. Given the backend is unreachable, when the user opens the screen, then a full-screen error with a Retry button appears; when the backend becomes reachable and the user taps Retry, then the screen loads.
24. Given the backend returns readings for some stations and fails for others, when the screen loads, then only the failing stations show "No reading" and no full-screen error appears.
25. Given a station is selected on All stations, when the user switches to Home and back, then the screen shows the same readings and selection without reloading.
26. Given the user selects a station on All stations, when they go to Home, then Home still shows their onboarding station.
27. Given a screen reader is active, when focus moves through the screen, then the map is one element announcing that it shows the fifteen stations and that stations are selected in the list, and each list entry is one focus stop announcing name, severity and expanded or collapsed state.
28. Given the device is in dark mode, when the user views the map and list, then dots, bars and the circle remain clearly visible against the background.
29. Given the device is in landscape or has a small screen, when the user opens the screen, then the map is scaled down rather than cropped and the list still has room to show entries.
30. Given the feature is installed, when the user uses the Home screen, then it looks and behaves exactly as before.
31. Given the user is on All stations, when they use the back gesture, then they return to Home.

## Definition of Done

- All user acceptance tests pass on an Android emulator.
- The shared code compiles for iOS.
- All existing automated tests pass, and the new automated tests listed in the Technical Annex are in place and pass.
- The Home and Onboarding screens show no visible or behavioural change.
- Every severity shown on the new screen appears as a word next to its colour; no meaning is carried by colour alone.
- Every reading that is not current is marked as such in both the collapsed and expanded entry.
- The map is drawn without any third-party map service, API key or network request beyond our own backend.
- The project documentation (CLAUDE.md) describes the new screen, the new tab, the moved shared code and the map's projection and border data source.

## Out of Scope

- Any change to the backend, including a batch endpoint for all stations' readings.
- Panning, zooming or any interactive map gesture other than tapping a dot.
- A basemap with lakes, cantons, terrain, cities or text labels on the map.
- Making the selected station on this screen the user's home station, or any other way to change the stored station (that belongs to the planned settings feature).
- Persisting the selection across app restarts.
- Automatic periodic refresh, or reload when the tab is reopened.
- Sorting or filtering the list by severity, species or region.
- Per-dot screen-reader focus on the map.
- Push notifications.
- Icons for the remaining placeholder tabs.
- The iOS app wrapper.

## Further Notes

- Readings are fetched separately for every station. The backend caches each station's reading for
  30 minutes, so opening this screen costs at most one upstream request per station per period,
  regardless of how many users look at it. Within that period a refresh may legitimately return the
  same reading; the "Refreshed" caption still moves, which is how the user sees that the refresh
  happened.
- The backend can serve a station's last good reading after an upstream outage with no maximum age.
  The not-current marking on this screen is the counterpart of that behaviour and must not be
  removed.
- Severities on hourly readings skew high, because the bands are defined over daily means. This is
  an accepted limitation documented for the whole app and applies here too.
- Swiss border data comes from Natural Earth, which is public domain; no attribution is legally
  required, but the source is recorded alongside the data.

---

## Technical Annex
> Written against codebase as of: 2026-10-02 (branch `feature/all-stations`, after `9a31bd1`)

### Architectural Decisions

#### Data access: client-side fan-out, no server change

- The screen uses the existing `GET /pollen/stations` for positions and names and the existing
  `GET /pollen/stations/{abbr}/measurements` once per station, all 15 in parallel. `:server` is not
  touched.
- A failed per-station call (404 or 502 or transport failure) becomes "no reading" for that
  station only. The 404 / 502 distinction is not surfaced in this screen.
- A later batch endpoint could replace the fan-out behind the same use case without touching the
  presentation layer.

#### Step 0 — move shared code to `core/` (no behaviour change)

Mechanical move, done first and on its own. Packages change; signatures, behaviour and test
assertions do not. Tests move with their subjects.

| From | To | Contents |
| ---- | -- | -------- |
| `feature/onboarding/{data,domain}` (station parts) | `core/station/{data,domain}` | `Station`, `StationRepository`, `StationRepositoryImpl`, `StationApiService`, `StationDto`, `StationMapper` |
| `feature/home/{data,domain}` | `core/measurement/{data,domain}` | `StationMeasurement`, `SpeciesReading`, `PollenSeverity`, `StationPollenOverview`, `ReadingAge` + `readingAgeOf` + `STALE_AFTER`, `StationMeasurementRepository`(+`Impl`), `StationMeasurementApiService`, `StationMeasurementDto`, `StationMeasurementMapper`, `GetStationMeasurementUseCase` |
| `feature/home/presentation` | `core/ui/severity` | `SeverityBar` (+`SeverityBarSize`, `severityFillFraction`), `SeverityColors`, `SpeciesRow` (currently private in `HomeScreen.kt` → becomes `internal`/public), `PollenSeverity.label()` (currently private), `ReadingAgeLabel.kt` (`label()` for `ReadingAge`, `refreshedLabel`), the species list heading ("concentration in $unit") |

- `FindNearestStationUseCase` stays in `feature/onboarding` (one consumer).
- Test fixtures move with their subjects: `feature/onboarding/StationFixtures.kt`,
  `FakeStationRepository.kt` → `core/station/`; `feature/home/MeasurementFixtures.kt` →
  `core/measurement/`; plus the matching `*Test.kt` files.
- `HomeScreen`'s reading-age view (fresh caption / stale `errorContainer` warning) is extracted to
  `core/ui/severity` (or `core/ui/reading`) as a reusable composable, so the expanded row renders
  exactly Home's wording and styling.
- CLAUDE.md is updated: it currently names `feature/home/presentation/SeverityColors.kt` and
  `feature/home/domain/model/ReadingAge.kt` explicitly.

#### New feature slice `feature/allstations/`

```
feature/allstations/
├── domain/
│   ├── model/        StationReading (per-station outcome), MapPoint
│   └── usecase/      GetAllStationReadingsUseCase
├── map/              SwissBorder, SwissMapProjection, StationHitTest, SwissOutlineIcon
└── presentation/     AllStationsScreen, AllStationsViewModel, AllStationsUiState,
                      AllStationsEvent, SwissMap (composable)
```

No `data/` package: the slice consumes `core/station` and `core/measurement` only.
`map/` is a non-standard sub-package justified by being pure geometry that is neither domain nor
presentation; placing it under `domain/` is an acceptable alternative.

#### `SwissBorder`

- `internal val SWISS_BORDER: List<GeoPoint>` — one closed outer ring, lat/lon in WGS84, simplified
  from Natural Earth admin-0 (1:10m) to roughly 150–300 points. Source and simplification
  tolerance recorded in a KDoc comment.
- Enclaves/holes (Campione, Büsingen) are ignored; at this scale they are invisible.
- `GeoPoint(latitude: Double, longitude: Double)` is a small value class/data class in `map/`.

#### `SwissMapProjection`

Pure, no Compose types (sizes and points as plain `Float`/`Double` or a tiny local type so it is
testable in `commonTest`).

```kotlin
object SwissMapProjection {
    /** width / height of the projected bounding box of SWISS_BORDER (≈ 1.55). */
    val aspectRatio: Float

    /** Projects a WGS84 point into a canvas of [width] × [height], preserving aspect, centred. */
    fun project(point: GeoPoint, width: Float, height: Float): MapPoint
}
```

- Equirectangular: `x = lon · cos(46.8°)`, `y = −lat`, normalised to the border's bounding box
  (computed once from `SWISS_BORDER`, padded by a small margin so edge dots are not clipped).
- Fit-inside, never crop: scale = min of width- and height-fit, remainder split as centring offset.
- The same function places the border polygon and the station dots.

#### `StationHitTest`

```kotlin
fun nearestStation(
    tap: MapPoint,
    dots: Map<String /* abbr */, MapPoint>,
    radiusPx: Float,
): String?
```

- Returns the abbr of the closest dot whose distance is `<= radiusPx`, else `null`. Ties are
  practically impossible; if equal, deterministic by abbr.
- Radius: 24 dp, converted to px by the caller with `LocalDensity`.

#### `SwissOutlineIcon`

- `val SwissOutline: ImageVector` built once from `SWISS_BORDER` via `ImageVector.Builder` /
  `path { }`, projected into a 24×24 viewport with `SwissMapProjection` (centred, ~2 dp padding),
  stroke only (`stroke = SolidColor(Color.Black)`, `strokeLineWidth ≈ 1.5f`, no fill) so it tints
  like a Material icon via `LocalContentColor`.

#### `GetAllStationReadingsUseCase`

```kotlin
class GetAllStationReadingsUseCase(
    private val getStationMeasurement: GetStationMeasurementUseCase,
) {
    /**
     * Emits the full list after each station resolves, starting with every station Pending.
     * Alphabetical by name (input order from StationRepository is already alphabetical; sort
     * defensively by name so the rule is the use case's, not the repository's).
     */
    operator fun invoke(stations: List<Station>): Flow<List<StationReading>>
}

sealed interface StationReading {
    val station: Station
    data class Pending(override val station: Station) : StationReading
    data class Available(override val station: Station, val overview: StationPollenOverview) : StationReading
    data class Unavailable(override val station: Station) : StationReading
}
```

- Implementation: `channelFlow { stations.forEach { launch { … send(updated snapshot) } } }` with a
  mutable state map guarded by the flow's single producer, or `merge` + `scan`. All requests start
  concurrently; the flow completes when all have resolved.
- Overall severity, `drivenBy` and species order come from `GetStationMeasurementUseCase`
  unchanged — no second derivation.
- Stations and readings keyed by `Station.abbr`.

#### `AllStationsViewModel`, UI state and events

```kotlin
sealed interface AllStationsUiState {
    data object Loading : AllStationsUiState                     // station list not yet known
    data class Content(
        val stations: List<StationReading>,                      // alphabetical
        val selectedAbbr: String?,
        val refreshedAt: Instant?,                               // null until the first round completes
        val isRefreshing: Boolean = false,
    ) : AllStationsUiState
    data class Error(val message: String) : AllStationsUiState
}

sealed interface AllStationsEvent {
    data class ScrollToStation(val abbr: String) : AllStationsEvent
}
```

- Constructor: `AllStationsViewModel(stationRepository: StationRepository, getAllStationReadings:
  GetAllStationReadingsUseCase, clock: Clock = Clock.System)`; Koin
  `viewModel { AllStationsViewModel(get(), get()) }`. There is no `Clock` binding — this follows
  `HomeViewModel`, which takes `kotlinx.datetime.Clock` as a defaulted parameter that tests override.
- Load on init: `Loading` → fetch stations → failure ⇒ `Error`; success ⇒ `Content` with all
  `Pending`, then each emission of the use case replaces `stations`. When the round completes:
  if **every** station is `Unavailable` ⇒ `Error`; otherwise `refreshedAt = clock.now()`.
- `retry()` from `Error` restarts the full load (including the station list if it failed).
- `refresh()`: only from `Content`; sets `isRefreshing = true`, keeps the current `stations` on
  screen (do **not** reset to `Pending`), replaces them with the new round's final result when it
  completes, keeps `selectedAbbr`, updates `refreshedAt`. All-unavailable on refresh ⇒ `Error`
  (consistent with initial load). The station list is not re-fetched.
- `onStationClicked(abbr)` (used by both map and list): toggles — same abbr ⇒ `selectedAbbr = null`;
  other abbr ⇒ `selectedAbbr = abbr` and send `ScrollToStation(abbr)` on a
  `Channel<AllStationsEvent>(Channel.BUFFERED)` exposed as `events = receiveAsFlow()`. Deselect sends
  no event.
- Scroll is an **event**, not state (CLAUDE.md "One-shot events"): it must not re-fire on
  recomposition or after returning to the tab.
- No reload on tab re-entry: the ViewModel survives via `navigateToTab`'s `saveState`/`restoreState`.

#### `SwissMap` composable

- `Canvas` sized `fillMaxWidth().aspectRatio(SwissMapProjection.aspectRatio)` wrapped with
  `heightIn(max = 40 % of screen height)`; when capped, the canvas is narrower and centred. Use
  `BoxWithConstraints` or `LocalWindowInfo`/`LocalConfiguration`-free sizing so it works on iOS.
- Draw order: border path (stroke in `outline` / `onSurfaceVariant`, optional faint
  `surfaceVariant` fill), dots, selection ring.
- Dot fill: `severityColor(overview.overallSeverity)` from `core/ui/severity/SeverityColors` for
  `Available`; neutral `outlineVariant` for `Pending`; hollow (stroke-only, muted `outline`) for
  `Unavailable`. Dot radius ~5 dp.
- Ring: `colorScheme.primary` stroke ~2 dp, radius ≈ dot radius + 4 dp so a gap keeps the dot
  colour visible.
- Tap: `Modifier.pointerInput { detectTapGestures { offset -> nearestStation(…)?.let(onStationClick) } }`.
- Semantics: `Modifier.clearAndSetSemantics { contentDescription = "Map of 15 pollen stations. Select a station in the list below." }`
  (count derived from the list size, not hard-coded).

#### `AllStationsScreen`

- `Scaffold` with `TopAppBar(title = "All stations")`, consistent with `HomeScreen` and
  `ComingSoonScreen` (each screen's own Scaffold owns the top inset; see CLAUDE.md "Insets").
- Body: `Column { SwissMap; refreshed caption (refreshedLabel); PullToRefreshBox { LazyColumn } }` —
  only the `LazyColumn` scrolls and only it is inside pull-to-refresh.
- Row (one `item(key = abbr)` per station, `Modifier.clickable { onStationClicked(abbr) }`,
  `semantics(mergeDescendants = true) { stateDescription = "Expanded" | "Collapsed" }`):
  - Line 1: station name.
  - Line 2: `SeverityBar(overallSeverity | null)` with the severity word (or "No reading") on the
    same line; when the reading is stale per `readingAgeOf(measuredAt, now)` a warning icon
    (`Icons.Default.Warning`, `contentDescription = "Reading not current"`, `error` tint) sits
    next to the word. `Pending` renders a muted placeholder bar.
  - Expanded (`AnimatedVisibility`): reading-age view (shared from Home), species heading with unit,
    seven `SpeciesRow`s from `overview.species` (already ordered). `Unavailable` expanded:
    "No reading available for this station right now."
- `now` for staleness is sampled at render time, as on Home.
- Scroll: `LaunchedEffect(Unit) { viewModel.events.collect { ScrollToStation -> … } }` resolves the
  index of `abbr` and calls `listState.animateScrollToItem(index)`. Because the expand animation
  changes the item height, scroll after expansion settles (or scroll to the item and then ensure
  its bottom is visible with `animateScrollBy`); if the item is taller than the viewport, its top
  aligns with the list's top. Exact mechanics are an implementation detail; the requirement is
  "expanded row fully visible, header first if it does not fit".

#### Navigation

- `Screen.Feature2` → `@Serializable data object AllStations : Screen`.
- `TopLevelDestination.FEATURE_2` → `ALL_STATIONS(Screen.AllStations, SwissOutline, "All stations")`.
- `AppNavigation`: `composable<Screen.AllStations> { AllStationsScreen() }` replacing the
  `ComingSoonScreen` call for slot 2. `navigateToTab` unchanged.
- `TopLevelDestinationTest` updated for the new name/order assertions.

#### DI additions (`core/di/AppModule.kt`)

- `domainModule`: `factory { GetAllStationReadingsUseCase(get()) }`.
- `presentationModule`: `viewModel { AllStationsViewModel(get(), get()) }`.
- Existing bindings keep working after the Step 0 package move (imports updated only).

#### Multiplatform constraints

- All new code in `commonMain`. No `String.format`, no `java.*`; trig via `kotlin.math`.
- Backtick test names without commas (Kotlin/Native).
- `Icons.Default.Warning` resolves via the existing `compose.materialIconsExtended` dependency.
- Verify with `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64`.

### Automated Testing Decisions

What makes a good test here: assert observable behaviour through each module's public interface —
projected coordinates, which station a tap selects, which states a ViewModel emits — never private
helpers or call order. Pin every boundary from both sides (prior art: `SpeciesThresholdsTest`,
`ReadingAgeTest`). Hand-written fakes, no mocking framework. All app tests in `commonTest`.

The existing `StationFixtures` holds only three stations. The geometry tests need all 15, so a
fixture listing every station's abbreviation, name and WGS84 coordinates (copied from the server's
`PollenStation` enum, which `:composeApp` cannot depend on) is added next to it in
`core/station/`.

| Module | Type | What is asserted | Prior art |
| ------ | ---- | ---------------- | --------- |
| Step 0 moved code | existing unit tests, moved | Unchanged assertions all pass in their new packages | the tests themselves |
| `SwissMapProjection` | unit | Bounding-box corners map to canvas edges (within margin); aspect ratio is preserved when the canvas is wider or taller than the map (fit-inside, centred, never outside the canvas); every one of the 15 station coordinates projects inside the canvas; relative positions are geographically right (Genève west of Zürich, Lugano south of Luzern); `aspectRatio` lies in a plausible range (~1.4–1.7) | `FindNearestStationUseCaseTest` (pure geometry) |
| `SwissBorder` | unit | Ring is closed or treated as closed; has a plausible point count; every one of the 15 station coordinates lies inside the polygon (point-in-polygon in the test) — catches a wrong or mirrored dataset | — |
| `StationHitTest` | unit | Tap exactly on a dot selects it; tap at `radius` selects, at `radius + ε` returns `null`; between two dots the nearer wins (Locarno/Lugano-like geometry, both directions); empty `dots` returns `null` | `SpeciesThresholdsTest` (edge + just beyond) |
| `GetAllStationReadingsUseCase` | unit | First emission is all `Pending` in alphabetical order; a station whose fake fails becomes `Unavailable` while others become `Available`; with a gated fake, one held station stays `Pending` while the rest resolve (independent fill-in); final emission has every station resolved; order never changes between emissions; overview comes from `GetStationMeasurementUseCase` (worst severity) | `GetStationMeasurementUseCaseTest`, `FakeStationMeasurementRepository.gate` in `HomeViewModelTest` |
| `AllStationsViewModel` | unit (`StandardTestDispatcher`, `advanceUntilIdle`) | `Loading` → `Content`; station-list failure → `Error`, retry recovers; all readings failing → `Error`; partial failure → `Content` with `Unavailable` entries; `refreshedAt` set from the injected clock after a round; refresh holds `isRefreshing = true` with old readings in flight (gate) and keeps `selectedAbbr`; `onStationClicked` selects, re-click deselects, switching replaces; selecting emits exactly one `ScrollToStation`, deselecting emits none | `HomeViewModelTest`, `OnboardingViewModelTest` (events) |
| `TopLevelDestination` | unit | Updated order and names; `ALL_STATIONS` resolves for `Screen.AllStations` | `TopLevelDestinationTest` |

Not tested automatically:

- Composables (`SwissMap`, `AllStationsScreen`, `SwissOutlineIcon`) — consistent with the repo,
  which has no Compose UI tests; their logic is pushed into the pure modules above. Verified by the
  user acceptance tests.
- Scroll mechanics and animations — verified manually.
- iOS execution — `iosSimulatorArm64Test` needs full Xcode; iOS test sources are compiled with
  `compileTestKotlinIosSimulatorArm64` instead.

Commands: `./gradlew :composeApp:testDebugUnitTest` and
`./gradlew :composeApp:compileTestKotlinIosSimulatorArm64`.
