# Home Pollen Overview

## Problem Statement

A person with a pollen allergy wants to know one thing before they leave the house: how bad is it
outside right now, and which pollen is responsible. Today the app cannot answer that. Onboarding
asks them to choose a measuring station, and then hands them a screen that displays only the name of
the station they just picked. Every piece of information the app was installed for is missing.

The underlying measurements are published by MeteoSwiss, but they arrive as semicolon-separated
text files in a legacy character encoding, with cryptic parameter codes as column headers and raw
concentration counts that mean nothing without a classification table. Nobody is going to read that
on a phone, and no phone should be asked to parse it.

## Solution

After onboarding, the app opens on a home screen that answers the question immediately.

A bar across the top of the content shows the **overall severity** at the chosen station — the worst
severity across all measured pollen types — as a coloured progress bar with the severity spelled out
above it and the responsible pollen named below it. The colour runs from grey through green, amber
and orange to red as severity rises.

Below that, every pollen type the network measures is listed, worst first, each with the same style
of bar, its own severity word, and its measured concentration. A pollen type the station does not
report is shown explicitly as having no data, rather than being hidden or silently displayed as
zero.

The screen states how old the reading is, and says so prominently when the reading is no longer
current. Pulling down refreshes it.

All contact with MeteoSwiss stays on the backend. The backend fetches the station's current-day
measurements, decodes and parses them, classifies each concentration into a severity band using the
Swiss exposure classes it already holds, and serves the result to the app as a single simple
response. It caches what it fetches so that repeated visits do not repeatedly hit the public data
service, and it keeps serving the last reading it obtained if the upstream service becomes
unreachable.

## User Stories

### Seeing the current situation

1. As an allergy sufferer, I want the home screen to open on the pollen situation at my chosen
   station, so that I get my answer without navigating anywhere.
2. As an allergy sufferer, I want to see a single overall severity for my station, so that I can
   decide whether to go outside without reading seven separate numbers.
3. As an allergy sufferer, I want the overall severity to reflect the worst-affected pollen type, so
   that a severe reading is never hidden behind six mild ones.
4. As an allergy sufferer, I want to be told which pollen type is responsible for the overall
   severity, so that I know whether it is one I actually react to.
5. As an allergy sufferer, I want the overall severity spelled out as a word, so that I do not have
   to interpret a bar's length or colour.
6. As an allergy sufferer, I want the severity shown as a filled bar, so that I can judge the
   situation at a glance without reading.
7. As an allergy sufferer, I want the bar's colour to change with severity, so that a bad day is
   recognisable before I have read anything.
8. As an allergy sufferer, I want a calm day to look visibly calm, so that the screen does not read
   as alarming when there is nothing to worry about.

### Seeing individual pollen types

9. As an allergy sufferer, I want a list of every pollen type the network measures, so that I can
   find the specific one I react to.
10. As an allergy sufferer, I want the list ordered worst first, so that what matters is at the top
    and I rarely have to scroll.
11. As an allergy sufferer, I want pollen types of equal severity ordered alphabetically, so that
    the list does not reshuffle unpredictably between visits.
12. As an allergy sufferer, I want each pollen type to show its own severity word and bar, so that I
    can read one type's situation without reference to the others.
13. As an allergy sufferer, I want each pollen type to show its measured concentration, so that I
    can distinguish a reading at the bottom of a band from one at the top.
14. As an allergy sufferer, I want the unit of measurement stated once for the list, so that the
    numbers are meaningful without the unit being repeated on every row.
15. As an allergy sufferer whose pollen type is not measured at my station, I want that stated
    explicitly, so that I do not mistake an absent measurement for an absence of pollen.
16. As a colour-blind user, I want every severity to be given as a word as well as a colour, so that
    the screen is fully readable to me.

### Knowing the station

17. As an allergy sufferer, I want the station my readings come from named at the top of the screen,
    so that I never misread another region's data as my own.
18. As an allergy sufferer, I want the station name accompanied by a location marker, so that I
    recognise it as a place rather than a title.
19. As an allergy sufferer, I want the station name to appear immediately, so that the screen is
    identifiable while the readings are still loading.

### Freshness and refreshing

20. As an allergy sufferer, I want to see the time the reading was taken, so that I know how current
    my information is.
21. As an allergy sufferer, I want the reading's age shown in my own local time, so that I do not
    have to convert it.
22. As an allergy sufferer, I want a clearly different, more prominent message when the reading is
    no longer current, so that I am never misled into treating old data as today's air.
23. As an allergy sufferer, I want to pull down to refresh the screen, so that I can get the latest
    reading without restarting the app.
24. As an allergy sufferer, I want the previous reading to stay on screen while a refresh is in
    progress, so that the screen does not blank out and lose my place.
25. As an allergy sufferer, I want the screen to reload automatically if my selected station ever
    changes, so that I am never shown one station's name beside another station's readings.

### When things go wrong

26. As an allergy sufferer with no network connection, I want a clear error message rather than an
    endless spinner, so that I understand the app is not broken.
27. As an allergy sufferer who has hit an error, I want a retry button, so that I can recover
    without restarting the app.
28. As an allergy sufferer, I want the app to show me the last reading the service obtained when the
    public data service is temporarily unavailable, so that a brief outage does not leave me with
    nothing.
29. As an allergy sufferer, I want any such fallback reading to be labelled with its true age, so
    that resilience never becomes deception.
30. As an allergy sufferer at a station with no usable measurements at all, I want an error with a
    retry rather than a screen of empty rows, so that I can tell the difference between a broken
    screen and a calm day.

### Backend behaviour

31. As the operator of the service, I want all contact with the public data service confined to the
    backend, so that parsing, encoding and classification rules live in one place and can be changed
    without shipping an app release.
32. As the operator of the service, I want fetched measurements cached for a period, so that many
    users checking the same station do not multiply into many requests to the public data service.
33. As the operator of the service, I want simultaneous first-time requests for the same station to
    result in a single upstream fetch, so that a burst of traffic cannot fan out into duplicated
    load.
34. As the operator of the service, I want the last successfully fetched reading retained and served
    when an upstream fetch fails, so that an upstream outage degrades the service rather than
    breaking it.
35. As the operator of the service, I want station names and file contents decoded in the character
    encoding the publisher actually uses, so that names containing accents and umlauts are never
    corrupted.
36. As the operator of the service, I want severity classification to remain the backend's sole
    responsibility, so that all clients agree on what "High" means.
37. As a developer, I want the severity bands to stay configurable in one place, so that they can be
    retuned when a better source table becomes available without touching anything else.

## User Acceptance Tests

1. Given a user who has completed onboarding by selecting Zürich, when they open the app, then the
   home screen appears with "Zürich" and a location marker at the top.
2. Given the home screen is loading, when the readings have not yet arrived, then the station name
   is already visible and a loading indicator is shown in the content area.
3. Given a station whose worst pollen type is classified High, when the home screen finishes
   loading, then the overall bar reads "High", is filled to four fifths, and is coloured orange.
4. Given a station whose worst pollen type is Grasses, when the home screen finishes loading, then
   the text below the overall bar names Grasses as the driver.
5. Given a station where every pollen type is classified None, when the home screen finishes
   loading, then the overall bar reads "None", is coloured grey, and is partially rather than
   completely empty.
6. Given readings for a station, when the species list is displayed, then all seven pollen types
   measured by the network are listed.
7. Given readings where Grasses is High, Birch is Moderate and Alder is None, when the species list
   is displayed, then they appear in that order.
8. Given readings where Oak and Ash are both Moderate, when the species list is displayed, then Ash
   appears before Oak.
9. Given readings where Birch has no measurement, when the species list is displayed, then Birch
   appears last with an empty bar and the words "No data" in place of a severity, and a dash in
   place of a number.
10. Given readings where Birch has no measurement and every measured type is None, when the overall
    bar is displayed, then it reads "None" — the absent measurement does not affect it.
11. Given readings for a station, when the species list is displayed, then the unit of measurement
    is stated once as a heading above the list and not repeated on each row.
12. Given a reading taken at 09:00 local time, when the screen is viewed at 09:30 local time, then
    the screen states that it was updated at 09:00.
13. Given a reading taken more than three hours ago, when the screen is viewed, then the age is
    presented as a prominent warning showing the reading's date rather than as an ordinary update
    line.
14. Given a displayed reading, when the user pulls down on the screen, then a refresh indicator
    appears, the existing readings remain visible throughout, and the screen updates when the
    refresh completes.
15. Given the device has no network connection, when the user opens the home screen, then an error
    message with a retry button is shown.
16. Given an error message is shown, when the user taps retry and connectivity has returned, then
    the readings load and replace the error.
17. Given the backend has previously served a reading for a station and the public data service has
    since become unreachable, when the user opens the home screen, then the previous reading is
    shown, labelled with its true age.
18. Given the backend has never successfully fetched a reading for a station and the public data
    service is unreachable, when the user opens the home screen, then an error message with a retry
    button is shown.
19. Given a station whose published file contains no usable measurements, when the user opens the
    home screen, then an error message with a retry button is shown rather than a list of empty
    rows.
20. Given the backend has fetched a station's measurements within the cache period, when a second
    user requests the same station, then no additional request is made to the public data service
    and both users see the same reading.
21. Given the cache period for a station has elapsed, when a user requests that station, then the
    public data service is contacted again and a newer reading is served if one has been published.
22. Given a station whose name contains an umlaut or accent, when its measurements are processed by
    the backend, then the name is rendered correctly with no corrupted characters.
23. Given a Grasses concentration of 20 and a Birch concentration of 20 at the same station, when
    the readings are displayed, then Grasses is classified High and Birch is classified Moderate,
    reflecting the different bands for grasses and trees.
24. Given the home screen is displayed, when a user with colour vision deficiency views it, then
    every severity can be determined from its accompanying word without reference to colour.
25. Given the home screen is displayed in the device's dark theme, when the severity colours are
    rendered, then every bar remains clearly distinguishable from the background.

## Definition of Done

- All user acceptance tests pass.
- The home screen replaces the existing placeholder screen entirely; no remnant of the
  station-name-only placeholder remains reachable.
- The app makes no direct request to the public MeteoSwiss data service; every measurement reaches
  the app through our own backend.
- All seven measured pollen types are represented on screen whenever readings are available,
  including those the selected station does not report.
- Severity is never conveyed by colour alone.
- The reading's age is visible whenever readings are shown, and visually escalates when the reading
  is no longer current.
- The backend serves a previously obtained reading when the upstream service fails, and never
  presents such a reading as more recent than it is.
- Every severity band boundary behaves correctly at the boundary value and immediately below it.
- The feature works on both supported mobile platforms, and the shared code compiles for both.
- No regression in onboarding, the startup routing decision, or the existing backend endpoints.
- Project documentation is updated to describe the new behaviour, and no longer describes the home
  screen as a placeholder or the backend as polling on a schedule.

## Out of Scope

- **Changing the station from the home screen.** The location marker is decorative. Station changes
  belong to the planned settings feature; until then, clearing the app's data is the only route back
  to onboarding.
- **Push notifications.** No subscription, no alerting, no outbound leg from the backend.
- **Historical data and trends.** Only the most recent reading is shown. No charts, no yesterday, no
  forecast.
- **Daily average measurements.** The feature uses hourly readings only.
- **A per-species detail screen.** Tapping a row does nothing.
- **Scheduled background polling on the backend.** Superseded by on-demand caching; revisit when
  push notifications require severities for stations nobody is viewing.
- **Automated UI tests.** The project has no Compose UI test harness, and establishing one is
  separate work.
- **The iOS application wrapper.** No Xcode project exists; the transport-security and location keys
  it will need are already recorded in the project documentation.
- **Retuning the severity bands.** See the accepted limitation below.

## Further Notes

### Accepted limitation: hourly readings against daily-mean bands

The severity bands the backend already holds follow the Swiss exposure classes (Gehrig et al. 2018),
which are defined over **daily mean** concentrations. This feature classifies **hourly** readings
against them.

Hourly readings peak well above a day's mean, so severities will skew high — an hourly birch reading
of 100 grains/m³ is reported as High even on a day whose mean would fall in Moderate.

This was chosen deliberately over the alternatives. Daily averages are only complete for the
previous day, so a screen built on them would show yesterday's air while claiming to describe today.
Inventing our own hourly bands would replace a documented approximation with undocumented guesswork.
The bands remain configurable in a single place, so an authoritative hourly table can be adopted
later without touching anything else.

### Accepted limitation: refreshing inside the cache period

Pulling to refresh does not force the backend to re-contact the public data service. Within the
cache period the user receives the same reading, and the age line does not move. This is deliberate:
the upstream file gains a new row roughly hourly, so forcing a fetch would usually return identical
data at the cost of an extra upstream request, and a client-controllable cache bypass would let any
app instance generate unlimited load on a public service. The pull is not inert — it re-queries the
backend and picks up a new reading as soon as the cache period has rolled.

### Accepted limitation: unbounded staleness

The backend applies no maximum age to a fallback reading. After a prolonged upstream outage it will
serve a days-old reading rather than an error. This keeps presentation policy in the app, where it
belongs, and the app's escalating age warning is what makes the staleness impossible to miss. The
risk being accepted is that the two must stay in step: if the age warning were ever removed, the
backend would silently serve stale data as current.

---

## Technical Annex

> Written against codebase as of: 2026-08-01

This section contains the architectural and automated testing decisions derived from the planning
session. It is intended for architect and developer review.

### Starting state

`:server` currently serves only static metadata. `PollenStation`, `PollenSpecies`, `PollenSeverity`
and `PollenThresholds` exist as tested domain types, and `PollenRoutes` projects them to DTOs. There
is **no HTTP client dependency**, no CSV parsing, no cache, no scheduler and no measurement
endpoint. The `hourlyNowPath` / `hourlyRecentPath` / `dailyRecentPath` properties on `PollenStation`
exist and are tested but have no callers.

`feature/home` in `:composeApp` is a single `HomeScreen.kt` composable with no ViewModel, injecting
`SelectedStationRepository` directly via `koinInject()` and rendering the station name.

### Architectural Decisions

#### AD-1 — On-demand fetch with a TTL cache, not a scheduled poll

The architecture diagram in `CLAUDE.md` commits to a 1-hour scheduled poll. This is superseded. A
request for a station fetches its CSV only if the cached entry has expired.

Rationale: no scheduler lifecycle, no cold-start "first poll failed" state, no fetching of stations
nobody has selected, and stale-on-failure gives strictly better availability than a failed poll. The
scheduled poll becomes correct when push notifications require severities for unviewed stations; the
cache abstraction makes that a drop-in.

TTL: **30 minutes**. The upstream file gains a row roughly hourly; 30 minutes guarantees a new row is
picked up within ~30 minutes of publication without a schedule that can sit out of phase with it.

#### AD-2 — `TtlCache<K, V>` as an extracted generic deep module

Location: `server/.../pollen/measurement/TtlCache.kt`

Holds all three cache behaviours behind one method:

```kotlin
sealed interface CacheResult<out V> {
    data class Fresh<V>(val value: V) : CacheResult<V>
    data class Stale<V>(val value: V) : CacheResult<V>   // loader failed, previous value retained
    data object Failed : CacheResult<Nothing>            // loader failed, nothing retained
}

class TtlCache<K, V>(
    private val ttl: Duration,
    private val clock: Clock = Clock.systemUTC(),
) {
    suspend fun get(key: K, loader: suspend (K) -> V): CacheResult<V>
}
```

- **TTL expiry** — entries older than `ttl` are reloaded.
- **Per-key mutex** — concurrent misses on the same key produce exactly one `loader` invocation; the
  others await it. Keyed, not global, so a slow fetch for one station does not block another.
- **Stale retention** — when `loader` throws and a previous value exists, that value is returned as
  `Stale` and retained (its original timestamp is preserved — a failed reload must not refresh the
  entry's age). No maximum age is applied.
- **Injected `Clock`** — every behaviour above is clock-dependent; without this, each test is a
  `Thread.sleep`.

Extracted rather than inlined because concurrency and expiry are the trickiest logic in the feature
and have nothing to do with pollen; testing them through pollen-shaped fixtures would be incidental
complexity.

#### AD-3 — Upstream access

`server/.../pollen/upstream/MeteoSwissClient.kt`

```kotlin
class MeteoSwissClient(
    private val client: HttpClient,
    private val baseUrl: String = "https://data.geo.admin.ch/ch.meteoschweiz.ogd-pollen",
) {
    suspend fun fetchHourlyNow(station: PollenStation): ByteArray
}
```

Client and base URL are constructor parameters, following the `StationApiService(client, baseUrl)`
precedent, so tests drive it with `MockEngine`. Returns raw bytes — **decoding is the parser's job**,
because the encoding is a property of the file format, not of the transport.

#### AD-4 — CSV parsing

`server/.../pollen/upstream/PollenCsvParser.kt`

```kotlin
data class ParsedReading(
    val measuredAt: Instant,
    val concentrations: Map<PollenSpecies, Int?>,   // null = column absent or cell empty
)

object PollenCsvParser {
    fun parseHourly(bytes: ByteArray): ParsedReading?   // null = no usable row
}
```

- **ISO-8859-1 decoding.** Reading as UTF-8 silently corrupts `Münsterlingen` / `Genève` rather than
  throwing. Pinned by a fixture containing an umlaut.
- `;`-separated; header row maps column names to species via `PollenSpecies.fromHourlyCode`, which
  returns `null` for non-pollen columns — those are ignored.
- Timestamps are `dd.MM.yyyy HH:mm` in **UTC**.
- **Row selection: the single most recent row in which at least one species has a value.** All seven
  species are then read from that one row. The rejected alternative — per-species latest non-empty
  value — yields seven different timestamps, one of which could be 14 hours stale sitting beside a
  fresh one with nothing on screen distinguishing them.
- A species whose column is absent, or whose cell in the chosen row is empty, maps to `null`.
- Returns `null` when no row has any value.

#### AD-5 — `MeasurementService` composes, it does not contain

`server/.../pollen/measurement/MeasurementService.kt`

```kotlin
class MeasurementService(
    private val client: MeteoSwissClient,
    private val thresholds: PollenThresholds,
    private val cache: TtlCache<PollenStation, ParsedReading>,
) {
    suspend fun measurementFor(station: PollenStation): CacheResult<StationMeasurement>
}
```

Deliberately thin: the loader is `{ s -> PollenCsvParser.parseHourly(client.fetchHourlyNow(s)) ?: throw NoUsableRowException() }`,
and classification is `thresholds.severityOf(species, concentration)` per non-null concentration.
Classification stays the backend's sole responsibility (existing convention).

#### AD-6 — API contract

```
GET /pollen/stations/{abbr}/measurements
```

Nested under the existing `/pollen/stations/{abbr}` (a property of a station; `fromAbbr` already
resolves the segment case-insensitively).

```json
{
  "stationAbbr": "PZH",
  "measuredAt": "2026-08-01T09:00:00Z",
  "unit": "grains/m3",
  "species": [
    { "id": "BIRCH", "name": "Birch", "latinName": "Betula",
      "concentration": 42, "severity": "MODERATE" },
    { "id": "ASH", "name": "Ash", "latinName": "Fraxinus",
      "concentration": null, "severity": null }
  ]
}
```

- **All seven species always present.** `concentration` and `severity` are **nullable**; `null` means
  "no reading", which is a different fact from `0` / `NONE` — a station that does not measure Ash is
  not a station reporting no ash.
- **`species` is unsorted** (enum declaration order) and there is **no `overallSeverity` field**.
  Both the maximum and the ordering are pure functions of this list and are presentation rules; a
  derived field on the wire invites the two sides to disagree.
- **`concentration` is included and displayed** (a late change in the planning session — it is shown
  on each species row, not merely carried for debugging).
- **`measuredAt` is mandatory.** A response that can be stale must say when it is from.

| Case | Status |
| --- | --- |
| Success (fresh or stale) | `200` |
| Unknown abbr | `404` (matches existing `/pollen/stations/{abbr}`) |
| No usable row | `404` |
| Upstream failed, nothing cached | `502` |

#### AD-7 — Server dependency injection points

`configureRouting` already takes `thresholds: PollenThresholds` as a defaulted parameter; it gains
`measurementService: MeasurementService` the same way, so `PollenRoutesTest` can install a service
backed by fixtures. `java.time` is used server-side (JVM-only; kotlinx-datetime there would be for
symmetry alone).

#### AD-8 — App slice

```
feature/home/
├── data/
│   ├── remote/     StationMeasurementApiService, dto/StationMeasurementDto, dto/SpeciesReadingDto
│   ├── mapper/     StationMeasurementMapper.kt  (toDomain())
│   └── repository/ StationMeasurementRepositoryImpl
├── domain/
│   ├── model/      StationMeasurement, SpeciesReading, PollenSeverity, ReadingAge
│   ├── repository/ StationMeasurementRepository
│   └── usecase/    GetStationMeasurementUseCase
└── presentation/   HomeScreen, HomeViewModel, HomeUiState, SeverityBar, SeverityColors
```

`StationMeasurementApiService(client, baseUrl)` per the established convention — the service never
reads `apiBaseUrl` itself; Koin supplies it. Repository wraps in `safeCall { }` and returns
`core.result.Result` (explicit import required — it shadows `kotlin.Result`).

#### AD-9 — `PollenSeverity` is duplicated, not shared

`:composeApp` cannot depend on `:server`, so the enum is declared on both sides and the wire
contract is the string name (`"VERY_HIGH"`).

Chosen over extracting a shared KMP domain module: this is already the established pattern
(`StationDto` exists in both `server/pollen/model` and `feature/onboarding/data/remote/dto`, with
`Station` as a separate app model that deliberately drops fields), and a shared module would make
`:server` depend on a multiplatform module for the first time over five enum constants.

**Accepted risk:** a rename becomes a runtime deserialization failure rather than a compile error.
Mitigated by a mapper test pinning every enum name against its exact wire string.

It lives in `feature/home/domain/model`, not `core/`, until a second feature needs it —
`SelectedStationRepository` earned `core/` by having three consumers on day one; this has one.

#### AD-10 — Aggregation and ordering live in a use case

`GetStationMeasurementUseCase` owns both derived rules, so they are unit-tested without Compose:

- **Overall severity** = maximum across species with a non-null severity; species with no reading are
  ignored. If no species has a reading, the endpoint has already returned 404, so this case does not
  arise in `Content`.
- **Ordering** = severity descending, **nulls last**, alphabetical by display name as tie-break
  (including among the nulls).

#### AD-11 — `ReadingAge` as an extracted pure module

`feature/home/domain/model/ReadingAge.kt`

```kotlin
sealed interface ReadingAge {
    data class Fresh(val localTime: LocalTime) : ReadingAge      // "Updated 09:00"
    data class Stale(val localDate: LocalDate) : ReadingAge      // "Data from 29 July"
}

fun readingAgeOf(measuredAt: Instant, now: Instant): ReadingAge  // boundary: 3 hours
```

Extracted so the boundary and the UTC→local conversion are tested without a ViewModel or a
composable. The screen renders whichever case it receives, with `Stale` styled as a visible warning
rather than a caption. This is the counterweight to AD-1's unbounded stale serving — the two are a
pair and must not be separated.

#### AD-12 — `HomeViewModel` observes the selection

```kotlin
sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Content(
        val stationName: String,
        val measuredAt: Instant,
        val overallSeverity: PollenSeverity,
        val drivenBy: String,
        val species: List<SpeciesReading>,   // pre-sorted by the use case
        val isRefreshing: Boolean = false,
    ) : HomeUiState
    data class Error(val message: String) : HomeUiState
}
```

- **Observes `selectedStation` and reloads via `flatMapLatest`** rather than taking the first value.
  `StartupViewModel` takes only the first value for a reason that does not transfer — rebuilding the
  navigation graph mid-session would yank the user out of the navigation they just triggered. Here,
  reloading on change is the correct response. The two behave identically today (nothing can change
  the station); the observing version stays correct when settings lands.
- **Null selection → `Error`** with retry. Unreachable by design (the startup gate only routes to
  `Screen.Home` when a station is stored), but representable, and a silent spinner would make it
  undiagnosable if a future change made it reachable.
- Load on init; pull-to-refresh sets `Content.isRefreshing` so prior data stays on screen; `Error`
  offers retry. **No auto-refresh timer, no refresh-on-resume.**
- This removes `HomeScreen`'s current direct `koinInject<SelectedStationRepository>()` +
  `collectAsStateWithLifecycle`; the repository moves behind the ViewModel.

#### AD-13 — Severity bar rendering

**Continuous fill, whole bar one colour = the current severity.** Five fixed stops at
`(ordinal + 1) / 5`:

| Severity | Fill |
| --- | --- |
| `NONE` | 20 % |
| `LOW` | 40 % |
| `MODERATE` | 60 % |
| `HIGH` | 80 % |
| `VERY_HIGH` | 100 % |
| no reading | 0 %, muted track |

- **`NONE` at 20 %, not 0 %.** An empty bar already means "no reading"; at 0 % those two states would
  be visually identical.
- **No interpolation within a band**, despite the concentration being available. Band widths are
  wildly non-linear — for trees `MODERATE` spans 15–89 while `HIGH` spans 90–1499 — so a birch
  reading of 200 (comfortably High, but 7 % into a 1410-wide band) would render barely past the
  Moderate mark, with the bar contradicting its own label. Honest interpolation would require log
  scaling, which is substantial machinery for a value the user reads as a category.
- **Uniform colour, not a per-segment gradient.** In a thermometer bar, the leftmost third stays
  green during `VERY_HIGH`, which reads as reassurance inside a warning.
- One composable at two sizes: large for the overall bar, compact for species rows.

#### AD-14 — Layout

```
┌────────────────────────────────────────────┐
│ 📍  Zürich                                 │  TopAppBar; pin decorative,
├────────────────────────────────────────────┤  name from SelectedStation.name
│ Updated 09:00                              │  → warning style when Stale
│                                            │
│              HIGH                          │
│    ████████████████░░░░                    │
│         Driven by Grasses                  │
│  ─────────────────────────────────────     │
│  All species     concentration in grains/m³│
│                                            │
│  Grasses                                   │
│  ████████████████░░░░   High         42    │
│                                            │
│  Ash                                       │
│  ░░░░░░░░░░░░░░░░░░░░   No data       –    │
└────────────────────────────────────────────┘
```

- **Species row is two lines**: name on its own line, then bar + severity word + concentration.
- **Station name comes from `SelectedStationRepository`**, not the measurement response, so the bar
  labels itself correctly while data is loading or has failed.
- **Unit stated once** as a list caption; numbers bare and right-aligned so they form a scannable
  column.
- **"Driven by X"** is retained — the overall bar is an aggregate with no concentration of its own,
  so without it the user must scan the list to learn what is high.
- The severity word is present on every row; **colour never carries meaning alone.**

#### AD-15 — Severity palette

Values live in `:theme` as named colours with **explicit light and dark variants** — these are
custom colours outside the Material 3 scheme, so nothing generates dark equivalents, and a red tuned
for `surfaceLight` (`0xFFFCFAED`) is a glaring blob on `surfaceDark` (`0xFF13140D`).

The **enum → colour mapping stays in `feature/home/presentation`** (`SeverityColors.kt`), because
`:theme` must not depend on the app module and therefore cannot see `PollenSeverity`.

Derived starting values. Contrast ratios are computed against `surfaceLight` / `surfaceDark`
respectively and all exceed the WCAG 2.1 non-text contrast minimum of 3:1 (SC 1.4.11):

| Severity | Light | ratio | Dark | ratio |
| --- | --- | --- | --- | --- |
| `NONE` | `0xFF6B6B60` | 5.1 | `0xFF9C9C90` | 6.7 |
| `LOW` | `0xFF2E7D32` | 4.9 | `0xFF7DD87F` | 10.6 |
| `MODERATE` | `0xFFB08000` | 3.4 | `0xFFF0B429` | 9.9 |
| `HIGH` | `0xFFB4500F` | 4.9 | `0xFFF08135` | 7.0 |
| `VERY_HIGH` | `0xFFB3261E` | 6.2 | `0xFFF2564B` | 5.5 |

Unfilled track: the existing `outlineVariant` from the active scheme. These values are a computed
starting point and must still be checked visually on device — `MODERATE` light is the tightest at
3.4 and may need darkening if it reads weakly.

#### AD-16 — New dependencies

| Module | Dependency | Why |
| --- | --- | --- |
| `:server` main | `ktor-client-core`, `ktor-client-cio` | No HTTP client exists there. Both already in the version catalog |
| `:server` test | `ktor-client-mock` | Drive `MeteoSwissClient` against CSV fixtures |
| `:composeApp` common | `kotlinx-datetime` | `measuredAt` → local time. **Not currently a dependency**; `java.time` is forbidden in `commonMain` |

`kotlinx-datetime` must be added to the version catalog. Because it is new to `commonMain`,
`:composeApp:compileTestKotlinIosSimulatorArm64` is mandatory before this is considered done.

#### AD-17 — Documentation updates

`CLAUDE.md` is falsified in several places and is updated as the final task, so it describes what
was built:

- Architecture diagram: `1h scheduled poll` → on-demand fetch with a 30-minute TTL cache, noting the
  poll is the right shape once push lands.
- REST API table: add `GET /pollen/stations/{abbr}/measurements`.
- Severity thresholds section: add the hourly-vs-daily-mean caveat.
- Navigation section and `HomeScreen`'s own KDoc: remove "placeholder … no ViewModel and no network".
- Feature layout section: `feature/home` becomes a second reference slice alongside
  `feature/onboarding`.
- New dependencies and the `:theme` severity palette.

### Automated Testing Decisions

#### What makes a good test here

Tests assert **external behaviour through the module's public interface**, not internal structure. A
cache test asserts "the loader ran once", not that a particular map field was written. A parser test
asserts the `ParsedReading` produced from given bytes, not which intermediate strings were built. A
ViewModel test asserts the sequence of emitted states, not which private method ran.

Existing conventions apply: backtick sentence test names
(``fun `returns Failure when the upstream responds with a server error`()``), hand-written fakes in
`commonTest` with no mocking framework, and boundary values pinned **from both sides** — at the edge
and immediately below it — as `SpeciesThresholdsTest` already does.

App tests go in `commonTest`, never `androidUnitTest`.

#### Modules under test

**Server (`server/src/test`) — unit tests**

| Module | Cases |
| --- | --- |
| `PollenCsvParser` | ISO-8859-1 round-trip via an umlaut fixture; `;` separation; timestamp parsing as UTC; selects the latest row with any value; empty cell → `null`; absent column → `null`; unknown columns ignored; no usable rows → `null` |
| `TtlCache` | Hit inside TTL invokes the loader once; reload after TTL; concurrent misses on one key invoke the loader exactly once; loader failure with a prior value → `Stale` retaining the **original** timestamp; loader failure with no prior value → `Failed`; distinct keys do not block each other. All driven by the injected `Clock` — no sleeps |
| `MeasurementService` | Classification per species against `PollenThresholds`, asserted at each band edge and immediately below; `null` concentration → `null` severity; all seven species present in the result |

**Server — integration tests (`testApplication`)**

| Module | Cases |
| --- | --- |
| `PollenRoutes` | `200` with the full body shape including a null-severity species; `404` unknown abbr; `404` no usable row; `502` upstream failure with nothing cached; `200` with the stale reading and its original `measuredAt` on upstream failure with a prior value |

Prior art: `PollenRoutesTest` already uses
`testApplication { application { configureSerialization(); configureRouting(ownThresholds) } }`,
passing in its own collaborators. The new tests extend that pattern with a fixture-backed
`MeasurementService`.

**App (`composeApp/src/commonTest`)**

| Module | Type | Cases |
| --- | --- | --- |
| `StationMeasurementMapper` | unit | Every `PollenSeverity` name pinned against its exact wire string (the AD-9 drift guard); null concentration and null severity map through; all seven species preserved |
| `StationMeasurementRepositoryImpl` | integration | Real `HttpClient` + `MockEngine`: `200` → `Success`; `404` → `Failure`; `502` → `Failure`; malformed JSON → `Failure` |
| `GetStationMeasurementUseCase` | unit | Severity descending; alphabetical tie-break; nulls last and alphabetical among themselves; maximum ignores nulls; all-null input |
| `ReadingAge` | unit | `Fresh` just inside the 3-hour boundary; `Stale` just outside it; UTC→local conversion |
| `HomeViewModel` | unit | `Loading` → `Content`; repository failure → `Error`; retry from `Error` → `Content`; `isRefreshing` true during refresh with prior data retained; reload when the selected station changes; null selection → `Error` |

Prior art: `StationRepositoryImplTest` for the `MockEngine` pattern (a real `HttpClient` via
`createHttpClient(engine)`, so the production JSON configuration is under test too, which is where
upstream changes bite); `OnboardingViewModelTest` for `Dispatchers.setMain(StandardTestDispatcher())`
+ `advanceUntilIdle()` so intermediate `Loading` / `isRefreshing` states are observable;
`FakeSelectedStationRepository` and `FakeStationRepository` for the fake style. A new
`FakeStationMeasurementRepository` follows the same shape.

#### Explicitly not tested automatically

- `HomeScreen`, `SeverityBar` and the `:theme` palette — the project has no Compose UI test harness
  and establishing one is separate work. Bar fills, colours and contrast are verified by running the
  app.
- No test contacts the real MeteoSwiss service. Parser fixtures are checked-in CSV samples.
- `:composeApp:iosSimulatorArm64Test` cannot run here (it needs full Xcode);
  `:composeApp:compileTestKotlinIosSimulatorArm64` is the substitute and is mandatory given the new
  `kotlinx-datetime` dependency.
