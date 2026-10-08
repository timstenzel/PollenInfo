# PollenInfo

Compose Multiplatform (Android + iOS) app for checking Swiss pollen levels per measuring station,
plus a Ktor backend that owns all contact with the MeteoSwiss public API.

## Architecture

```
┌─────────────────────┐        REST         ┌──────────────┐  CSV fetch, on demand ┌───────────────────┐
│ composeApp          │ ──────────────────> │  server      │  + alarm stations     │ MeteoSwiss OGD    │
│ (Android + iOS)     │ <────────────────── │  (Ktor JVM)  │ ────────────────────> │ pollen (public)   │
└─────────────────────┘   JSON              │  SQLite      │   30-min TTL cache    └───────────────────┘
         ▲                                  │  scheduler   │
         │                                  └──────────────┘
         │                                        │ FCM HTTP v1 (Android only)
         └──────────── push notification ─────────┘
```

**Fetching is on demand, plus a scheduler for alarm stations.** A request for a station's measurements fetches that
station's current-day CSV only if the server has no reading for it younger than **30 minutes**
(`MeasurementService.CACHE_TTL`). The upstream file gains a row
roughly hourly, so a new row is picked up within half an hour of publication, and any number of
people looking at one station cost one upstream request per period. Simultaneous first requests for
a station share a single fetch.

When a fetch fails, the station's last good reading is served with its **original** `measuredAt` —
never re-stamped — and the app's stale-age warning makes its age visible. With no earlier reading
the request fails (`502`). No maximum age is applied to a retained reading; that is presentation
policy and lives in the app's warning, so the two must stay together.

The one timer is the **alarm scheduler** (see "Alarm delivery"): once a minute it reads a station
only when one of that station's enabled alarms needs it — a daily report due that minute, or a
threshold alert whose window is open — and it reads through the **same**
`MeasurementService`, so alarms and app users together still cost at most one upstream request per
station per cache period. A station with no due alarm and nobody looking at it costs nothing. There
is still no cold-start poll and no "the first poll failed" state: a scheduler fetch is just another
caller of the cache.

The Diary's **pollen history** is a second, separate cache on the same terms: a station's
year-to-date daily file is fetched on demand and kept for **3 hours** (`HistoryService.RECENT_TTL`),
and its file of earlier years — needed only for a year that reaches back across 1 January — for
**24 hours** (`HistoryService.HISTORICAL_TTL`), each with the same stale-on-failure rule. The scheduler does not use it. See
"`GET /pollen/stations/{abbr}/history`".

**The apps never call the MeteoSwiss API directly.** All upstream fetching, parsing and severity
classification lives in `:server`; the apps only speak to our own REST API. This keeps CSV parsing,
station metadata and threshold logic in one place and off the devices.

Push notifications deliver the user's alarms (see "Alarms"). The Android app obtains an FCM token,
registers the device with the backend and sends it a new token whenever FCM rotates it; the backend
keeps devices and alarms in SQLite (see "Persistence"), delivers daily reports and threshold alerts
through FCM (see "Alarm delivery") and drops a token FCM reports as unregistered. iOS has no push
leg.

### Modules

| Module       | Type                        | Contains                                                    |
| ------------ | --------------------------- | ----------------------------------------------------------- |
| `:composeApp`| KMP (android, ios*)         | The app: UI, ViewModels, repositories, REST client           |
| `:server`    | Kotlin/JVM (Ktor + Netty)   | REST API for the apps, station/species/threshold domain, MeteoSwiss fetching, device and alarm store (SQLite) |
| `:theme`     | KMP (android, ios*)         | Shared Material 3 colors / typography / `PollenInfoTheme`    |

Package root everywhere: `ch.stenzel.tim.polleninfo`.

## Commands

`JAVA_HOME`, `java`, `adb` and `emulator` all come from `~/.zshrc` — **no export prefix is needed.**
`JAVA_HOME` points at an Android Studio JBR (JDK 21); any of the installed Studio JBRs work, they
ship the same build. If a Gradle call ever reports no JDK, that shell did not source the profile —
export `JAVA_HOME` for that one call rather than adding it back to the docs.

| Task                            | Command                                     |
| ------------------------------- | ------------------------------------------- |
| App unit tests (fast, JVM)      | `./gradlew :composeApp:testDebugUnitTest`   |
| Server unit tests               | `./gradlew :server:test`                    |
| All unit tests we can run here  | `./gradlew :composeApp:testDebugUnitTest :server:test` |
| Verify iOS sources compile      | `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` |
| Check translations (also part of `check`) | `./gradlew :composeApp:checkTranslations` |
| Run the backend on :8080        | `./gradlew :server:run` (push is logged unless `FCM_CREDENTIALS` is set) |
| Android debug APK               | `./gradlew :composeApp:assembleDebug`       |

Notes:

- `commonTest` is the single source of truth for app tests; `testDebugUnitTest` executes it on the
  JVM. **`:composeApp:iosSimulatorArm64Test` requires full Xcode** — with only Command Line Tools
  installed (`xcode-select -p` → `/Library/Developer/CommandLineTools`) linking the test binary
  fails at `xcrun`. Compile the iOS test sources instead to catch non-portable code.
- Prefer the targeted test tasks over `./gradlew check` for the same reason.
- The Gradle configuration cache is enabled; if a build behaves oddly after editing build scripts,
  add `--no-configuration-cache`.
- `./gradlew :server:run` writes its database to `server/data/polleninfo.db` (override with
  `POLLENINFO_DB`). Delete the file for a clean backend; the app then re-registers on its own. Inspect
  it with the SDK's `sqlite3` (`platform-tools/`). The directory is git-ignored.
- The Android build needs `composeApp/google-services.json` (committed — see "Firebase").

## Data source: MeteoSwiss OGD pollen

This is the **only** upstream source. Docs:
<https://opendatadocs.meteoswiss.ch/a-data-groundbased/a7-pollen-stations>.

Base: `https://data.geo.admin.ch/ch.meteoschweiz.ogd-pollen/`. Public, no auth, no API key.
All files are **CSV, `;`-separated, ISO-8859-1 encoded** (not UTF-8 — transcode on read).
Timestamps are `dd.MM.yyyy HH:mm` in **UTC**.

| File                                                 | Contents                                    |
| ---------------------------------------------------- | ------------------------------------------- |
| `ogd-pollen_meta_stations.csv`                       | All 15 stations: abbr, name, canton, WGS84 lat/lon, altitude |
| `ogd-pollen_meta_parameters.csv`                     | Parameter codes → taxon, unit, granularity  |
| `ogd-pollen_meta_datainventory.csv`                  | Which station reports which parameter since when |
| `<abbr>/ogd-pollen_<abbr>_h_now.csv`                 | **Hourly, current day** — fetched for measurements and alarms |
| `<abbr>/ogd-pollen_<abbr>_h_recent.csv`              | Hourly, year to date                        |
| `<abbr>/ogd-pollen_<abbr>_d_recent.csv`              | **Daily averages, year to date**, one row per day up to yesterday — fetched for the history |
| `<abbr>/ogd-pollen_<abbr>_d_historical.csv`          | **Daily averages, every earlier year**, ending 31 December of last year (~560 KB for PZH) — fetched for a year-long history |

`<abbr>` is the lowercase station abbreviation, e.g. `pzh/ogd-pollen_pzh_h_now.csv`.

### The 15 stations

`PBE` Bern · `PBS` Basel · `PBU` Buchs SG · `PCF` La Chaux-de-Fonds · `PDS` Davos/Wolfgang ·
`PGE` Genève · `PLO` Locarno/Monti · `PLS` Lausanne · `PLU` Lugano · `PLZ` Luzern ·
`PMU` Münsterlingen · `PNE` Neuchâtel · `PPY` Payerne · `PSN` Sion · `PZH` Zürich

### Hourly parameter codes → taxon

Values are integer concentrations in **grains/m³** (`No/m³`).

| Code       | Taxon                  |
| ---------- | ---------------------- |
| `kaalnuh0` | Alder (*Alnus*)        |
| `kabetuh0` | Birch (*Betula*)       |
| `kacoryh0` | Hazel (*Corylus*)      |
| `kafaguh0` | Beech (*Fagus*)        |
| `kafraxh0` | Ash (*Fraxinus*)       |
| `kaquerh0` | Oak (*Quercus*)        |
| `khpoach0` | Grasses (*Poaceae*)    |

Daily equivalents use `d0` (06–06 UTC average) and `d1` (00–00 UTC average) suffixes, e.g.
`kabetud0`. We use the `d0` variant.

> **These 7 taxa are the whole set** (automatic measurement method, since 2023-01-01). There is no
> mugwort, olive or ragweed in this dataset. `PollenSpecies` in `:server` is the authoritative
> vocabulary — the `PollenType` inside `feature/example` is invented placeholder data and is not it.

Stations and species are modelled as exhaustive enums in
`server/.../pollen/domain/` (`PollenStation`, `PollenSpecies`) with the parameter codes and station
coordinates baked in and covered by tests. Use those rather than re-deriving codes or paths.

## Severity thresholds

**Thresholds are per species and live in the server** — see
`server/.../pollen/domain/PollenThresholds.kt`. The apps never classify concentrations; they render
the severity the server computes, and can read the table from `GET /pollen/thresholds` to label and
colour their UI with the same numbers.

`SpeciesThresholds` holds the *inclusive lower bound* of each band, in grains/m³:

```
0        1..moderate-1   moderate..high-1   high..veryHigh-1   veryHigh..
NONE     LOW             MODERATE           HIGH               VERY_HIGH
```

Configuration is a `Map<PollenSpecies, SpeciesThresholds>` — one entry per species, so any single
taxon can be retuned without touching the others. Construction fails fast if a species is missing
or if the bounds are not strictly increasing.

Seeded values follow the Swiss exposure classes (Gehrig et al. 2018), which currently give the six
tree taxa one set of bounds and grasses a much lower one:

| Species                          | MODERATE from | HIGH from | VERY_HIGH from |
| -------------------------------- | ------------- | --------- | -------------- |
| Alder, Birch, Hazel, Beech, Ash, Oak | 15        | 90        | 1500           |
| Grasses                          | 5             | 20        | 200            |

So 20 grains/m³ is `MODERATE` for birch but already `HIGH` for grasses. Note MeteoSwiss does not
publish these numbers in machine-readable form; if you get an authoritative per-taxon table,
`PollenThresholds.DEFAULTS` is the single place to change.

`PollenSeverity` is ordered `NONE < LOW < MODERATE < HIGH < VERY_HIGH`, so a "at least this severe"
comparison is expressible as `severity.atLeast(minimum)`.

### Accepted limitation: hourly readings against daily-mean bands

**These bands are defined over daily mean concentrations; `/pollen/stations/{abbr}/measurements`
applies them to hourly readings, so the severities it reports skew high.** An hourly birch reading
of 100 grains/m³ comes back as `HIGH` even on a day whose mean would land in `MODERATE`.

This was chosen over the alternatives rather than overlooked. Daily averages are only complete for
the previous day, so a screen built on them would show yesterday's air while claiming to describe
today; inventing our own hourly bands would replace a documented approximation with undocumented
guesswork. `PollenThresholds.DEFAULTS` stays the one place to change, so an authoritative hourly
table can be adopted later without touching anything else.

### Severity palette and bars

(The only other colours outside the Material scheme are the map's lakes, `MapPalette.kt` — see
"All stations" — and the Diary's species lines, `SpeciesPalette.kt` — see "Diary".)

The app shows a severity as a word and a bar. The bar colours live in `:theme` as
`SeverityPalette.kt` — `severityNoneLight` … `severityVeryHighDark`, running grey → green → amber →
orange → red. They sit outside the Material 3 scheme, so nothing generates dark equivalents: each
has an **explicit light and dark value**, and the WCAG contrast ratio against `surfaceLight` /
`surfaceDark` is recorded next to each value (all ≥ 3:1, SC 1.4.11; Moderate-light is the tightest
at 3.38).

| Severity  | Light        | Dark         | Fill  |
| --------- | ------------ | ------------ | ----- |
| NONE      | `0xFF6B6B60` | `0xFF9C9C90` | 4 %   |
| LOW       | `0xFF2E7D32` | `0xFF7DD87F` | 25 %  |
| MODERATE  | `0xFFB08000` | `0xFFF0B429` | 50 %  |
| HIGH      | `0xFFB4500F` | `0xFFF08135` | 75 %  |
| VERY_HIGH | `0xFFB3261E` | `0xFFF2564B` | 100 % |
| no reading | —           | —            | 0 %, muted track |

`:theme` cannot depend on the app, so it never sees `PollenSeverity`. **The mapping from severity to
colour lives in `core/ui/severity/SeverityColors.kt`**, and it picks light or dark from the
luminance of the applied `colorScheme.surface` rather than `isSystemInDarkTheme()`, so it follows a
`darkTheme` override and Android's dynamic schemes. Note that on Android 12+ `PollenInfoTheme` uses
dynamic colour, so the surface on device is not exactly the `surfaceLight` / `surfaceDark` the
ratios were computed against.

`SeverityBar` fills to fixed stops (`severityFillFraction`) and does **not** track the
concentration: the bands are so unequal in width that an interpolated bar would contradict its own
label. NONE fills a 4 % sliver because an empty bar means "no reading"; Low to Very high fill quarters. The whole fill is one colour — a
gradient would leave the left end green during Very high. Colour never carries meaning alone; every
bar sits beside its severity word.

### Reading age

Above the overall bar, the home screen says how current its reading is. `measuredAt` travels from
the wire to `HomeUiState.Content` as an `Instant`, and the screen classifies it at render time with
`readingAgeOf(measuredAt, now)` in `core/measurement/domain/model/ReadingAge.kt` — a pure function, so
the boundary and the UTC → local conversion are tested without a ViewModel or a composable.

- **Less than 3 hours old** (`STALE_AFTER`) → `ReadingAge.Fresh(localTime)`, a quiet caption:
  "Data from 09:00".
- **3 hours or older** → `ReadingAge.Stale`, a warning in the `errorContainer` colours with an
  icon: "Data from 29 July — These readings are not current." If the reading is from the same
  local calendar day it is `Stale.Today(localTime)` and names the time instead ("Data from 06:00
  today"), since a warning naming today's date reads as a contradiction; otherwise
  `Stale.Earlier(localDate)`. The date is in the app's language (`DateWording.fullDate`, see
  "Localization"), the time always `HH:mm`.

Both are in the **device's** time zone, not the source's UTC. The stale warning is the counterpart
of the backend serving its last known reading through an upstream outage with no maximum age: if the
warning were ever removed, a stale reading would pass for current. Keep the two together.

Below it sits a second caption, "Refreshed 10:42" (`refreshedLabel` in `core/ui/severity/ReadingAgeLabel.kt`; it
adds the date once it is no longer today). That is `Content.refreshedAt`, the time the app last
*received* a reading, stamped by `HomeViewModel` from an injected `kotlinx.datetime.Clock`. It is
a different fact from `measuredAt`: within the backend's cache period a refresh moves it while the
data time stays put, which is how the user can tell the refresh happened. It is never a warning.

This is what `kotlinx-datetime` is in `commonMain` for. It is pinned to **0.6.x**: 0.7 moves
`Instant` and `Clock` into `kotlin.time`, which is still experimental on our Kotlin 2.1 and would
need an opt-in at every use. Revisit the pin when Kotlin is bumped.

## Conventions

### Feature package layout (`:composeApp`)

Each feature is a vertical slice under `feature/<name>/`:

```
feature/example/
├── data/
│   ├── remote/          <Name>ApiService + dto/  (@Serializable, DTOs never leave data/)
│   ├── mapper/          DTO -> domain extension functions (`fun XDto.toDomain()`)
│   └── repository/      <Name>RepositoryImpl
├── domain/
│   ├── model/           Plain data classes + enums, no serialization annotations
│   ├── repository/      Repository interface
│   └── usecase/         Single-purpose classes with `suspend operator fun invoke(...)`
└── presentation/        <Name>Screen.kt, <Name>ViewModel.kt, <Name>UiState.kt
```

**`feature/example` is a reference implementation, not a product feature.** It exists to show the
layering end to end — DTO → mapper → domain model → repository → use case → ViewModel → UI, with
tests at each level. Come back to it when starting a real feature and mirror its structure; don't
extend it.

Everything it names is deliberately fictional: it calls `https://api.example.com/v1/pollen`
(RFC 2606 documentation host, nothing served there), and its `PollenType` / `PollenLevel` are
invented placeholders, **not** the real vocabulary — that lives in `:server` as `PollenSpecies` /
`PollenSeverity`. Consequence: the example screen cannot load data at runtime and will land in its
`Error` state. Its tests all pass because they drive it through `MockEngine`.

For a real feature that talks to our own backend, mirror one of the real slices instead — all
are the same layering pointed at `:server` through `apiBaseUrl`. The first three no longer own a
data layer: the station list and the reading pipeline they consume live in `core/` (below), so
those slices are mostly `presentation/`. `feature/alarms` is the full layering again, since nothing
else reads alarms. `feature/diary` reads `core/history` and so has no `data/` either.

- **`feature/onboarding`** — a form: the shared `core/stationpicker` (list, pick, location
  shortcut), a user choice persisted through `core/preferences`, and completion delivered as a
  one-shot event. `presentation/` only — its picking moved to `core/stationpicker` when the
  change-station screen needed the same thing.
- **`feature/home`** — a dashboard over a reading: a ViewModel that *observes* a stored selection
  and reloads on change, derived rules (worst severity, ordering) kept in a use case so they are
  tested without Compose, the full `Loading` / `Content(isRefreshing)` / `Error` cycle with
  pull-to-refresh and retry, and a time-dependent label classified at render time
  (`ReadingAge`). `HomeViewModelTest` is the model for holding a load in flight
  (`FakeStationMeasurementRepository.gate`) to observe the intermediate state.
- **`feature/allstations`** — many readings at once: a use case that fans out one request per
  station and emits a growing list (`Flow<List<StationReading>>`), so rows fill in independently.
  It has no `data/` package at all — only `domain/` (the per-station outcome and the fan-out use
  case), `map/` (pure map geometry, see "All stations" below) and `presentation/`. `PerStationMeasurementRepository` in its `commonTest` is the fake for
  scripting a failure or a gate **per station**, which the single-result
  `FakeStationMeasurementRepository` cannot.
- **`feature/diary`** — a chart over a history: `chart/` (pure `DiaryChartGeometry` and the
  spoken `diaryChartDescription`, tested in `commonTest`, plus the `DiaryChart` canvas) and
  `presentation/`. See "Diary" below.
- **`feature/alarms`** — the complete slice, with writes: `data/` (`AlarmApiService`, DTOs incl. the
  polymorphic `ScheduleDto`, mapper, `AlarmRepositoryImpl` with lazy device registration),
  `domain/` (`Alarm`, `AlarmDraft`, the pure `AlarmFormState`, typed failures, the repository
  interface) and `presentation/` (the list with a permission gate, an optimistic switch and quiet
  reload on resume; the editor with dirty-checking and confirm dialogs; `AlarmSummary` wording). The
  model for a create/edit form whose rules are tested without a ViewModel (`AlarmFormStateTest`),
  and for a repository that recovers from the backend forgetting this install
  (`AlarmRepositoryImplTest`). See "Alarms" below.
- **`feature/settings`** — a screen of mostly fixed text: `presentation/` only, with a
  `SettingsUiState` that has no `Loading` or `Error` (its one network value, the station name, has
  a stored fallback). The first screen whose text comes entirely from string resources — the model
  for that (see "Localization"). Also the change-station screen, the second user of
  `core/stationpicker`. See "Settings" below.

Cross-feature code lives in `core/` (`core/network`, `core/result`, `core/di`, …). A feature never
imports another feature, and nothing in `core/` imports a feature except the DI module that wires
them. Code moves to `core/` once a second feature needs it, keeping the same
`data/` / `domain/` layering inside:

| Package | Contains |
| --- | --- |
| `core/station` | `Station`, `StationRepository`(+`Impl`), `StationApiService`, `StationDto`, `StationMapper` — `GET /pollen/stations`, sorted alphabetically by name |
| `core/species` | `Species` (`id`, `name`), `SpeciesRepository`(+`Impl`), `SpeciesApiService`, `SpeciesDto`, `SpeciesMapper` — `GET /pollen/species`, in the server's order. The app's pollen-type vocabulary where there is no reading (the alarm editor), loaded rather than declared so `PollenSpecies` stays authoritative |
| `core/measurement` | `StationMeasurement`, `SpeciesReading`, `PollenSeverity`, `StationPollenOverview`, `ReadingAge` (+`readingAgeOf`, `STALE_AFTER`), `StationMeasurementRepository`(+`Impl`), `StationMeasurementApiService`, its DTO and mapper, and `GetStationMeasurementUseCase` (worst severity, `drivenBy`, display order) |
| `core/history` | `HistoryRange` (`WEEK` / `MONTH` / `YEAR`), `StationHistory` + `HistoryDay` (`levels` by species id, `null` = no value), `StationHistoryRepository`(+`Impl`), `StationHistoryApiService`, its DTOs and mapper — `GET /pollen/stations/{abbr}/history`. In `core/` as the reading pipeline's daily counterpart of `core/measurement` |
| `core/diary` | `Feeling` (`VERY_BAD` … `VERY_GOOD`, each with its `level` on the pollen scale), `DiaryEntry` (+ the pure `recording`, which never replaces a date's answer), `swissToday(clock)`, `DiaryRepository`, the pure `DiaryCodec` and the logic-free `DataStoreDiaryRepository` — the user's answers, **on the device only** (see "Diary answers") |
| `core/ui/feeling` | `Feeling.label()` (+`labelResource()`) — "Very bad" … "Very good", the words on Home's prompt buttons and on the Diary chart's feeling axis |
| `core/ui/species` | `speciesColor(id)` — a species id to its `SpeciesPalette` colour, light or dark by the same surface-luminance rule as `PollenSeverity.color()`; `null` for an id the app has no colour for. `speciesName(id, fallback)` (`SpeciesNames.kt`) — the species' name in the app's language, the server's name for an id the app does not know (see "Localization") |
| `core/ui/severity` | The composables and wording every screen showing a reading uses, so they cannot drift apart: `SeverityBar` (+`SeverityBarSize`, `severityFillFraction`), `SeverityColors` (`PollenSeverity.color()`), `PollenSeverity.label()` (+`labelResource()`), `ReadingAge.label(dates)` + `refreshedLabel` (each a `ResourceText`), `ReadingAgeView` (fresh caption / stale warning), `SpeciesListHeading`, `SpeciesRow` |
| `core/ui/format` | `DateWording` (month and weekday names, day-month pattern; `fullDate`, `shortDate`, `monthOnly`, `weekdayShort` / `weekdayFull`, `weekdays` ranges), `formatTime` (`HH:mm`), `rememberDateWording()` / `loadDateWording()` — dates in the app's language (see "Localization"); `ResourceText` (+`resolve()` / `load()`) — a translated sentence chosen by a pure function |
| `core/push` | `PushTokenProvider` (+`PushTokenResult`), bound per platform, and `PushTokenUpdater`, which the alarm repository implements so the Android push service can report a rotated token without importing a feature; Android's channels and messaging service sit in `androidMain` (see "Firebase") |
| `core/result` | `Result` (+`safeCall`, `map`, `onSuccess`, `onFailure`), `AppError` and `Throwable.toAppError()` (see "Error handling") |
| `core/network` | `apiBaseUrl`, `createHttpClient`, the per-platform engine, and `HttpStatusException` + `HttpResponse.checkSuccess()`, which every API service calls before `body()` |
| `core/ui/error` | `ErrorContext`, the pure `AppError.text(context)` → `ErrorText`, and `AppError.message(context)` / `loadMessage(context)` — every error sentence a screen shows |
| `core/notifications` | `NotificationPermissionState` and `rememberNotificationPermissionController()` (see "Alarms") |
| `core/stationpicker` | Choosing a station, shared by onboarding and the change-station screen: `FindNearestStationUseCase` (`domain/usecase`), and in `presentation/` the `StationPicker` state holder, `StationPickerState` (`Loading` / `Content(stations, selected, isLocating, locationError)` / `Error(AppError)`), `LocationError` and the `StationPickerContent` composable (see "Location") |
| `core/language` | `AppLanguage` (`SYSTEM`, `EN`, `DE`, `FR`, `IT`, each with its BCP 47 `tag`; the pure `fromTag`, primary subtag only, anything unsupported → `SYSTEM`) and `LanguageRepository` (`current`, `set`, `appliesImmediately`) — bound in `platformModule`: `AndroidLanguageRepository`, `IosLanguageRepository`; Android's `appLanguageContext()` sits in `androidMain` (see "Localization"). Consumers use `FakeLanguageRepository` |
| `core/appinfo` | `AppVersion(name, code)` and `AppInfo` (`version: AppVersion?`, `null` when the platform reports no complete version) — bound in `platformModule`: `AndroidAppInfo` (`PackageManager`, `PackageInfoCompat.getLongVersionCode`), `IosAppInfo` (`CFBundleShortVersionString` / `CFBundleVersion`; a non-integer build number counts as missing). Checked by hand; consumers use `FakeAppInfo` |

Test fixtures sit next to their subjects in `commonTest`: `core/station/StationFixtures.kt` and
`FakeStationRepository.kt`, `core/measurement/MeasurementFixtures.kt` (including the gated
`FakeStationMeasurementRepository`), `core/history/HistoryFixtures.kt` (`stationHistory(...)` and
the gated `FakeStationHistoryRepository`), `core/diary/FakeDiaryRepository.kt`,
`core/appinfo/FakeAppInfo.kt`.

### Talking to our own backend

`core/network/ApiConfig.kt` declares `expect val apiBaseUrl: String` — the base address of `:server`,
without a trailing slash. There is one actual per platform because a development server on the host
machine is reachable under a different name from each emulator:

| Platform | `apiBaseUrl`            | Why                                                    |
| -------- | ----------------------- | ------------------------------------------------------ |
| Android  | `http://10.0.2.2:8080`  | The emulator's alias for the host's loopback interface |
| iOS      | `http://localhost:8080` | The simulator shares the host's network stack          |

**API services never read `apiBaseUrl` themselves.** They take the base URL as a constructor
parameter and Koin supplies it (`single { StationApiService(get(), apiBaseUrl) }`), so a test can
construct one against whatever host its `MockEngine` answers for. Replace the actuals with an
`https` address once the backend is deployed; there is deliberately no BuildConfig or Gradle
flavour machinery for this yet.

Because those addresses are plain HTTP, both platforms need a transport-security exception:

- **Android** — `composeApp/src/debug/AndroidManifest.xml` sets `android:usesCleartextTraffic="true"`
  on `<application>`. It lives in the **debug source set only**, so a release build can never ship
  it (verify with `grep usesCleartextTraffic composeApp/build/intermediates/merged_manifest/<variant>/…`).
  Note the path is `src/debug/`, not `src/androidDebug/`: despite what `./gradlew :composeApp:sourceSets`
  reports, only `src/debug/AndroidManifest.xml` is actually merged in this KMP + AGP setup.
- **iOS** — no `Info.plist` exists yet (there is no iOS app project). Whoever creates the iOS
  wrapper must add an ATS exception, `NSAllowsLocalNetworking = true`, or the simulator will refuse
  the cleartext development backend. See "iOS wrapper configuration" below.

### iOS wrapper configuration

The project has no `iosApp` Xcode project, so these keys cannot be set today. They are the
`Info.plist` entries the shared code already depends on, listed here for whoever creates the wrapper:

| Key                                       | Value  | Needed by                                            |
| ----------------------------------------- | ------ | ---------------------------------------------------- |
| `NSAppTransportSecurity.NSAllowsLocalNetworking` | `true` | The cleartext development backend on `localhost` |
| `NSLocationWhenInUseUsageDescription`      | A sentence explaining the station shortcut | `rememberCoarseLocationPermissionRequester` — **mandatory**: without it `CLLocationManager` silently never prompts, so the shortcut fails with no error to debug |
| `NSLocationDefaultAccuracyReduced`         | `true` | `IosCoarseLocationProvider` — the direct expression of "coarse is enough": iOS then never asks for precise access at all |
| `CFBundleLocalizations`                    | `en`, `de`, `fr`, `it` | `IosLanguageRepository` — iOS only switches an app to a language its bundle declares |
| `CFBundleDevelopmentRegion`                | `en`   | English as the fallback for every other device language, as `values/` is on Android |
| `CFBundleShortVersionString` / `CFBundleVersion` | e.g. `1.0.0` / `1` (a whole number) | `IosAppInfo` — Settings' version line; hidden if either is missing or the build number is not a whole number |

The notification permission (`IosNotificationPermissionController`) needs no `Info.plist` key.

### Error handling

Never let exceptions escape the data layer. Repositories wrap calls in `safeCall { }` and return
`Result<T>` (`core/result/Result.kt`), a `Success`/`Failure` sealed class with
`map`/`onSuccess`/`onFailure` helpers. ViewModels turn `Result` into a UI state — they do not
rethrow.

Our `Result` deliberately shadows `kotlin.Result`, which is a default import. **Always import
`ch.stenzel.tim.polleninfo.core.result.Result` explicitly** — the explicit import wins, but a file
that omits it binds to the stdlib type and fails to compile against `Success` / `Failure`. Use
`safeCall`, not `runCatching` (which returns the stdlib type).

**UI states carry an `AppError`, never a message string.** `core/result/AppError.kt` is a sealed
interface of what went wrong *for the user*: `Network`, `ServerUnavailable`, `NotFound` (mapped from
exceptions), `NoStationSelected`, `NoReadings`, `NoStations` (conditions a ViewModel finds itself),
`PushUnavailable`, `AlarmLimitReached`, `InvalidAlarm` (alarm failures) and `Unknown`. A ViewModel
turns a `Failure` into one with `exception.toAppError()`: any `kotlinx.io.IOException` — every
engine's transport failures and Ktor's `HttpRequestTimeoutException` — is `Network`, an
`HttpStatusException` 5xx `ServerUnavailable` and 404 `NotFound`, anything else (a payload that does
not parse, another status) `Unknown`. `feature/alarms` has its own `toAlarmAppError()`
(`domain/model/AlarmAppError.kt`) that maps its five exceptions — `UnknownDeviceException`, which
only escapes after the repository's one re-registration, is `Unknown` — and delegates the rest; the
alarm exceptions stay in the feature because `core/` may not import one.

**Every API service checks the status before `body()`** through `core/network`'s
`HttpResponse.checkSuccess()`, which throws `HttpStatusException(status)` on a non-2xx — so an error
body is never read as the expected payload and the mapping can tell a 502 from a 404.
(`AlarmApiService` first turns its own statuses — 400, 404, 409 — into the alarm exceptions.)

**Screens turn an `AppError` into text** with `core/ui/error/AppErrorText.kt`:
`AppError.message(context)` in composition, `loadMessage(context)` (suspending, `getString`) for a
snackbar shown from an event. `ErrorContext` is `LOAD` (the reason sentence alone, under the
screen's own heading) or one of `SAVE_ALARM` / `DELETE_ALARM` / `PAUSE_ALARM` / `RESUME_ALARM`, which
put "Couldn't save the alarm." etc. before the reason; `NotFound` reads "This alarm no longer
exists." outside `LOAD`. The choice is the pure `AppError.text(context)` → `ErrorText(action,
reason)` of `StringResource`s, tested in `AppErrorTextTest`; the sentences are `error_*` resources
in all four languages. No exception text — host names, status codes, library messages — reaches a
screen; `feature/example` is the one exception: its `Error` carries the exception's own text (the
screen words only the case where there is none, `example_error_unexpected`).

### UI state

One sealed interface per screen with `Loading` / `Content` / `Error` (see
`ExampleUiState`). `Content` carries an `isRefreshing` flag so pull-to-refresh keeps the
previous data on screen. ViewModels expose a single `StateFlow<XUiState>` and collect with
`collectAsStateWithLifecycle()`.

### One-shot events

State is what the screen should **show**; an event is what it should **do**, once. Navigation and
one-off snackbars are events. A ViewModel delivers them on a `Channel<XEvent>(Channel.BUFFERED)`
exposed as `receiveAsFlow()`, collected in a `LaunchedEffect` that calls a lambda supplied by
`AppNavigation` — see `OnboardingEvent` / `OnboardingViewModel.events`.

**Do not model these as a boolean on the UI state.** A flag like `isComplete` describes a state, but
"navigate away" must happen exactly once: a flag re-fires on every state re-emission and every
recomposition that reads it, so the screen navigates again on a configuration change or any later
update, and the flag then has to be cleared — bookkeeping that exists only to undo the wrong model.
A channel delivers each event to exactly one collector exactly once and needs no reset.

Anything the screen should keep displaying — including error flags such as
`OnboardingUiState.Content.saveError` — stays in the UI state.

### Persisted user selections

`core/preferences/` owns everything the user has chosen and the app must remember.
`SelectedStationRepository` is the main one: a `Flow<SelectedStation?>` that emits `null`
while nothing is stored, plus `suspend fun select(...): Result<Unit>` — a write can fail on IO, and
the caller must react rather than move on to a screen with nothing to read.

It lives in `core/` rather than in `feature/onboarding` because it has many consumers: onboarding
and the change-station screen write it; the startup gate, Home, Settings, the Diary (once, for its
initial station) and the alarm editor (for a new alarm's station) read it.

`DataStoreSelectedStationRepository` is **deliberately logic-free** — it reads and writes two string
keys and nothing else. It has no unit test: a real one would need a platform file path, and app
tests may not live in `androidUnitTest`. Every consumer is tested against
`FakeSelectedStationRepository` in `commonTest` instead. Keep the implementation trivial enough that
this stays an honest trade; anything worth testing belongs in a caller.

`NotificationPermissionPreferences` is the second one: a single `askedBefore` flag that records
whether the notification prompt has ever been shown, which Android needs (see "Alarms"). The same
rules apply: `DataStoreNotificationPermissionPreferences` is logic-free and untested, and consumers
use `FakeNotificationPermissionPreferences`.

`DeviceRegistrationRepository` is the third: the anonymous `deviceId` the backend issued
(`Flow<String?>`, `store`, `clear`). It is the only key to this install's alarms, so it lives on
the device alone; clearing app data loses access to them. Same rules again:
`DataStoreDeviceRegistrationRepository` is logic-free, consumers use
`FakeDeviceRegistrationRepository`.

**The app language is deliberately not here.** It belongs to the platform, so it can never disagree
with the platform's own setting: Android's per-app language (AppCompat stores it below API 33),
iOS's `AppleLanguages` default. `core/language/LanguageRepository` reads and writes it there — see
"Localization".

#### Diary answers

`core/diary/DiaryRepository` is the fourth, and the only one holding health information: the
user's daily answers (`entries`, sorted by date), the last date the Home prompt was closed
unanswered (`dismissedOn`), `record(date, feeling)` and `dismiss(date)`. **The answers never leave
the device** — no API service or request DTO takes a `Feeling` or `DiaryEntry`, so nothing has to be
promised about a server. They are kept indefinitely and never pruned; clearing app data or
reinstalling loses them (accepted until accounts exist). An answer is immutable: `record` on a date
that already has one changes nothing.

`DataStoreDiaryRepository` stores two string keys in the shared `DataStore<Preferences>` — the
entries as `DiaryCodec` JSON (`[{"date":"2026-10-04","feeling":"BAD"}]`, ISO dates and enum names,
so the file is portable to a later account) and the dismissed date as ISO text. It is logic-free and
untested like the others: the format is `DiaryCodec` (pure, `DiaryCodecTest` — an unknown feeling or
a malformed element is skipped, corrupt input decodes to an empty diary rather than crashing every
start) and the never-overwrite rule is `List<DiaryEntry>.recording` (`DiaryEntryTest`); `record`
applies it inside one `edit`, so two quick taps cannot both see an empty day. Consumers use
`FakeDiaryRepository`.

**A day is the Swiss calendar day**: `swissToday(clock)` (`Europe/Zurich`) is the app's single
definition of "today", so an answer is filed under the same date as the backend's daily means it is
compared with, whatever zone the device is in.

### Location

`core/location/` holds the whole location story, split into **two** pieces on purpose.

`CoarseLocationProvider` is `suspend fun currentLocation(): CoarseLocationResult`, and
`CoarseLocationResult` has exactly three cases — `Success(lat, lon)`, `PermissionDenied`,
`Unavailable`. Everything a platform can fail with (no provider, provider disabled, no fix, delegate
error, timeout) folds into `Unavailable`, because the screen has two messages and every one of those
cases ends in the same advice: pick a station manually. `AndroidCoarseLocationProvider` /
`IosCoarseLocationProvider` are plain platform classes bound in `platformModule` — no
`expect`/`actual`, since only the *implementation* differs, not the shape.

Both actuals bridge their platform's callback API through `suspendCancellableCoroutine`, so
cancelling the calling coroutine reaches the platform (`CancellationSignal.cancel()` /
`stopUpdatingLocation()`) instead of abandoning a request that keeps running.

**The station picker.** Onboarding and the change-station screen choose a station the same way,
so the logic lives once in `core/stationpicker`: `StationPicker(stationRepository,
coarseLocationProvider, findNearestStation, scope, initialAbbr)` is a plain state holder — not a
ViewModel — that each screen's ViewModel owns and drives on its `viewModelScope`. It loads the list
(`Error(AppError)` + `retry()` if that fails), starts on `initialAbbr` (asked once, on the first load:
`null` for onboarding, the stored station for change-station; a station no longer listed leaves
nothing selected), and handles `onStationSelected` and `onPermissionResult`. It never persists — the
screens' own Continue / Save do. Each ViewModel combines `picker.state` with its own fields into its
UI state (`OnboardingUiState(picker, saveError)`, `ChangeStationUiState`). `StationPickerContent` is
the shared screen body — spinner, the list error with Retry, or header slot, "Use my location", the
location error, the dropdown and footer slot (where each screen puts its button) — and requests the
location permission itself. `StationPickerTest` pins all of it under virtual time; the ViewModel
tests pin only what each screen adds.

**That cancellation is load-bearing, not just tidy.** Picking a station from the dropdown cancels any
lookup still in flight (`StationPicker.locationJob`). The shortcut proposes and the user
commits, so a fix that lands *after* they have already chosen must not re-pick for them — cancelling
makes that impossible rather than something a guard has to remember to check, and it stops the device
looking for a position nobody is waiting for. The two location messages clear on the same pick: an
error must not outlive its cause.

Android uses `LocationManagerCompat.getCurrentLocation` on `NETWORK_PROVIDER` only. **No Play
Services** (push is the one feature that needs them — see "Firebase"; location stays Play-free),
and no GPS fallback: `ACCESS_COARSE_LOCATION` does not grant `GPS_PROVIDER`, and
`FUSED_PROVIDER` needs API 31 against `minSdk` 26. The compat shim is what makes this work below API
30, which is why `androidx.core:core-ktx` is an explicit `androidMain` dependency. There is
deliberately no `getLastKnownLocation` fallback — a days-old fix from another country would silently
resolve to the wrong station.

**The 10-second timeout is `LOCATION_TIMEOUT` in `commonMain` and is applied by
`StationPicker`, not by either actual.** One constant that cannot drift between platforms, and
one that `commonTest` can drive under virtual time with a fake that never answers.

`rememberCoarseLocationPermissionRequester` is a **`@Composable expect fun`** and is the reason
prompting is separate from looking up: Android's request needs an activity-scoped
`ActivityResultLauncher`, which a Koin-injected class holding only the application context cannot
provide. The screen prompts (inside `StationPickerContent`) and hands the resulting boolean to
`onPermissionResult(granted)`, so the picker touches no platform API and is fully testable against `FakeCoarseLocationProvider`. (A
composable `expect`/`actual` pair does work with the Compose compiler plugin here — verified against
`compileTestKotlinIosSimulatorArm64`.)

The manifest declares `ACCESS_COARSE_LOCATION` and **not** `ACCESS_FINE_LOCATION`. The coordinates
never leave the device: the nearest-station calculation is `FindNearestStationUseCase`, pure
`kotlin.math` over the station list the app already holds, and no API service in the app takes a
coordinate parameter.

### DI

Koin, wired in `core/di/AppModule.kt` — one module per layer (`networkModule`, `dataModule`,
`domainModule`, `presentationModule`), aggregated into `appModules`. ViewModels are registered with
`viewModel { }` and injected with `koinViewModel()`. Use cases are `factory`, everything else
`single`.

Bindings that can only be built with platform APIs go in **`core/di/PlatformModule.kt`**
(`expect val platformModule: Module`, with `.android.kt` / `.ios.kt` actuals), which is first in
`appModules`. It provides the `DataStore<Preferences>`, the `CoarseLocationProvider`, the
`PushTokenProvider`, `AppInfo` and the `LanguageRepository`. The DataStore factory needs a file path and an IO dispatcher, neither of which
exists in `commonMain`; the two providers are different platform classes on each side. Each actual builds the store itself —
Android from the `androidContext()` Koin installs plus `preferencesDataStoreFile`, iOS from the
Documents directory plus an okio `Path`. Note the dispatcher differs by necessity: `Dispatchers.IO`
is `internal` on Kotlin/Native, so the iOS actual uses `Dispatchers.Default`. Keeping this module
separate is what keeps `AppModule.kt` free of `expect`/`actual` noise.

### Navigation

Type-safe Compose Navigation: destinations are `@Serializable` objects/classes nested in the
`Screen` sealed interface, registered via `composable<Screen.X>` in `AppNavigation`. The start
destination is `Screen.Onboarding`; `Screen.Example` stays registered so the reference feature
remains reachable, but nothing navigates to it.

Completing onboarding navigates to `Screen.Home` with `popUpTo<Screen.Onboarding> { inclusive =
true }`, so the back gesture from Home leaves the app instead of reopening a setup screen whose
purpose is already fulfilled. `feature/home` is the app's dashboard: `HomeViewModel` observes the
stored selection and loads that station's reading from `/pollen/stations/{abbr}/measurements`.
Pulling down refreshes it with the current readings kept on screen (`Content.isRefreshing`), and
`Error` offers a retry. Within the backend's 30-minute cache period a refresh legitimately returns
the same reading and its age does not move — the backend never re-contacts MeteoSwiss on demand,
and there is deliberately no client-controllable cache bypass. A missing selection (unreachable
past the startup gate) resolves to `Error` with a message, never an endless spinner.

**The feeling prompt.** Once per Swiss day Home asks "How do you feel today?" in a card
(`FeelingPrompt.kt`) floated bottom-centre over its list, inside the pull-to-refresh box. It has
four full-word buttons, Very bad → Very good left to right, a close `IconButton` announced as "Not
today", and the question as a heading. `HomeViewModel` takes `DiaryRepository` and collects its two
flows; `Content.showFeelingPrompt` is re-derived on every `Content` it builds (load, refresh, station
change) and on every diary change: shown while `swissToday(clock)` has neither an entry nor
`dismissedOn`. Until the diary has been read it is hidden, so the card never flashes up for a day
already answered. `Loading` and `Error` never carry it. `onFeelingSelected(feeling)` records today's
answer and `onFeelingPromptDismissed()` records only the dismissal; either hides the card through the
diary flow, with no reload. A failed save keeps the card with `Content.feelingSaveError` (a
`Boolean` — a local storage failure, shown as `home_feeling_save_error`), cleared by the next
attempt. Swiss midnight passing while Home stays open shows the prompt at the next such
event, not on the dot — accepted. While the card is up the list's bottom padding grows by its
measured height (plus its margin), so the last species row still scrolls fully above it.

It **observes** the selection rather than taking its first value, unlike the startup gate below —
see that section for why the two differ — so a station changed in Settings shows here at once.
There is deliberately no way to change the station from Home; the pin in its top bar is decorative,
and station changes belong to Settings (see "Settings").

#### All stations

The second tab, `feature/allstations`: every station's current reading, one row per station,
alphabetical by name and never reordered. Each row is the station name over a compact
`SeverityBar` filled to its overall (worst) severity, with the severity word beside it — the same
`GetStationMeasurementUseCase` derivation Home uses, so the two screens cannot disagree.

**Readings are fetched client-side, one `GET /pollen/stations/{abbr}/measurements` per station, all
fifteen in parallel** (`GetAllStationReadingsUseCase`). There is no batch endpoint and adding one
was out of scope. This costs the backend nothing extra upstream — its 30-minute cache means at most
one MeteoSwiss request per station per period however many users open the screen — and isolates
failures: a 404, 502 or transport error becomes `StationReading.Unavailable` for that station only
("No reading", empty muted bar), never an error for the screen. A batch endpoint could later
replace the fan-out behind the same use-case signature without touching the presentation layer.

The use case emits the whole list first with every station `Pending`, then again after each
station resolves, and completes when all have. `AllStationsViewModel` shows its full-screen spinner
only until the station list arrives; from the first emission on it is `Content`, and a pending row
shows a faint placeholder bar and "Loading…", deliberately fainter than the "no reading" track so
loading is not mistaken for failure.

**Errors.** The screen is `Error` — full screen, with Retry — in exactly two cases: the station list
fails, or a round completes with **every** station `Unavailable` (on the initial load or on a
refresh). Fifteen "No reading" rows would hide an unreachable backend behind fourteen
per-station-looking failures; a single answered station keeps the list. `retry()` restarts the full
load; the station list is re-fetched only if it never arrived.

**Refresh.** A "Refreshed 10:42" caption (`refreshedLabel`) sits above the list and is absent until
the first round completes — `Content.refreshedAt` is stamped from an injected
`kotlinx.datetime.Clock` (a defaulted constructor parameter, as on `HomeViewModel`) when a round
completes, not when rows fill in. Pull-to-refresh wraps only the list and works from `Content`
only: it sets `isRefreshing`, keeps the current rows exactly as they are (no reset to `Pending`),
does **not** re-fetch the station list, and replaces all rows at once with the new round's *final*
result — intermediate emissions are skipped, so a refresh never shows a mix of rounds. It holds the
flag for `MIN_REFRESH_INDICATOR` for the same reason Home does. There is no reload on returning to
the tab: the ViewModel survives through `navigateToTab`'s saved state, caption included.

A row whose reading is `Stale` per `readingAgeOf` (judged at render time, as on Home) shows a
warning icon announced as "Reading not current" beside its word — the collapsed-row counterpart of
Home's stale warning, and like it, the partner of the backend serving old readings through an
outage. Its slot is reserved on every row so the bars all end at the same x.

**Map.** Above the caption and the list sits `SwissMap`: Switzerland's outline and its big lakes,
with one dot per station. It is outside the `LazyColumn` and outside pull-to-refresh, so it stays put while the list
scrolls. It is drawn on a plain canvas — no map service, tiles, key or network request.

- **Border data** is `map/SwissBorder.kt` (`SWISS_BORDER`): one closed ring of WGS84 points from
  Natural Earth admin-0 1:10m (public domain), simplified with Douglas–Peucker at 0.01° to 203
  points. Enclaves are ignored. The KDoc records the source and tolerance so it can be re-derived.
- **Lakes** are `map/SwissLakes.kt` (`SWISS_LAKES`): 10 rings from Natural Earth lakes 1:10m and
  its Europe supplement (public domain), simplified at 0.003° — Léman, Neuchâtel, Biel, Thun,
  Brienz, Vierwaldstättersee, Zug, Zürich, Walensee, Lugano. The Bodensee and Lago Maggiore are
  left out at the product owner's request. They
  are **landmarks only**, there so the dots are easier to place; nothing is identified by them
  alone. Border lakes are kept whole rather than cut at the border. `SwissLakesTest` pins each
  lakeshore station (Genève, Lausanne, Neuchâtel, Luzern, Zürich, Lugano) to within 5 km of
  its lake. The tab icon does **not** draw them — at 24 dp they would be noise.
- **Projection** is `map/SwissMapProjection`: equirectangular with longitude scaled by cos 46.8°,
  framed on the border's bounding box plus a 4 % margin, fitted inside any canvas and centred —
  never cropped. `aspectRatio` (≈ 1.55) sizes the canvas. Border, lakes, dots and the tab icon all go
  through the same `project`, so they agree by construction. Both files are pure Kotlin (no Compose
  import) and tested in `commonTest` — `SwissBorderTest` checks every station lies inside the ring,
  which catches a mirrored or lat/lon-swapped dataset.
- **Size.** The map fills the width at its aspect ratio, capped at 40 % of the screen's content
  height (`MAP_MAX_HEIGHT_FRACTION`, measured with `BoxWithConstraints`, so it works on iOS); when
  capped it is narrower and centred.
- **Dots** are filled with `PollenSeverity.color()` — the **same function the row's `SeverityBar`
  uses**, so a dot and its bar cannot disagree, in either scheme. A `Pending` station's dot is
  neutral `outlineVariant`; an `Unavailable` one is hollow `outline`, the dot form of the empty
  "no reading" track. There is deliberately no separate dot colour function.
- **Lake colours** are `mapWater*` in `:theme`'s `MapPalette.kt` — a muted blue fill plus an edge,
  each with an explicit light and dark value (a lake is blue whatever the dynamic scheme is), picked
  by the same surface-luminance rule as `PollenSeverity.color()`. The edge carries the shape and
  clears 3:1 against the surface; keep the lakes quieter than the severity colours.
- **Tapping** the map selects the station whose dot is **nearest** to the tap, if it is within
  24 dp (`TAP_RADIUS` in `SwissMap.kt`, converted to px there); a tap farther from every dot does
  nothing. The rule is `map/StationHitTest.kt`'s `nearestStation(tap, dots, radiusPx)` — pure
  Kotlin over already-projected points, tested in `commonTest`; an exact tie goes to the
  alphabetically first abbreviation. Nearest rather than first-within-radius is what keeps Locarno
  and Lugano (closer together on screen than two radii) unambiguous: each owns its side of the
  midpoint. The dots are projected at tap time through the same `project` they are drawn with, and a
  hit goes to the map's single `onStationClick` — wired to `AllStationsViewModel.onStationClicked`,
  the rows' entry point — so a dot tap behaves exactly like a row tap, deselect included.
- **Accessibility.** The map is one node (`clearAndSetSemantics`) announced as "Map of 15 pollen
  stations. Select a station in the list below." — the count comes from the list. There is no focus
  stop per dot: the dots repeat the list, and the list is where a station is operated. Tapping a dot
  is a pointer shortcut only.

**Selection.** Tapping a row calls `AllStationsViewModel.onStationClicked(abbr)`: it selects that
station, a second tap on it deselects, and tapping another replaces it — at most one is selected
(`Content.selectedAbbr`, `null` on first open). The selected row expands beneath its header
(`AnimatedVisibility`) into exactly Home's detail: `ReadingAgeView`, `SpeciesListHeading` and
`SpeciesRow`s in `overview.species` order; an `Unavailable` station says "No reading available for
this station right now." The map circles the selected dot in `primary` with a gap, so the dot's
severity colour stays visible. The selection survives a refresh (rows are replaced, `selectedAbbr`
is kept) and a tab switch, is never persisted, and never touches the stored home station.

**The scroll is an event, not state.** Selecting sends `AllStationsEvent.ScrollToStation(abbr)` on a
buffered `Channel` (deselecting sends nothing); `AllStationsScreen` collects it and calls
`revealExpandedItem`, which waits for the expansion to settle (`EXPAND_DURATION_MILLIS`, shared with
the animation) and then scrolls as little as needed for the whole row to be visible — top-aligned
when the row is taller than the list, so its header is what shows. As state, the scroll would re-fire
on recomposition and on returning to the tab with the selection still in place.

**Accessibility of rows.** The header (name, bar, word, stale icon) is one clickable focus stop with
a `stateDescription` of "Expanded" / "Collapsed". The expanded detail sits outside that node, so a
screen reader does not read seven taxa as part of the row's name.

Browsing here never changes the station stored during onboarding; Home keeps showing that one.

#### Bottom navigation bar

`AppNavigation` wraps the `NavHost` in an outer `Scaffold` whose `bottomBar` is a Material 3
`NavigationBar`. **`navigation/TopLevelDestination` is the tab list** — an enum in display order,
each entry carrying its `Screen`, icon and `contentDescription`. It holds five tabs: `HOME` →
`Screen.Home`, `ALL_STATIONS` → `Screen.AllStations` (see "All stations" above), `DIARY` →
`Screen.Diary` (see "Diary" below), `ALARMS` → `Screen.Alarms` (see "Alarms" below) and
`SETTINGS` → `Screen.Settings` (see "Settings" below). All
stations shows `SwissOutline` (`feature/allstations/map/SwissOutlineIcon.kt`), a stroke-only
`ImageVector` built from `SWISS_BORDER` through `SwissMapProjection`, so it tints like a Material
icon; Diary shows the book `Icons.AutoMirrored.Filled.MenuBook`; Alarms shows
`Icons.Default.Notifications`; Settings shows `Icons.Default.Settings`; Home uses
`Icons.Default.LocationOn`, at the product owner's request. The tabs are icon-only, so
`contentDescription` is the only name a screen reader has to tell them apart. It is a
`StringResource` (`nav_home`, `nav_all_stations`, `nav_diary`, `nav_alarms`, `nav_settings` — "Home",
"All stations", "Diary", "Alarms", "Settings" in English), resolved with `stringResource` where it
is set on the `Icon`, so the enum stays a pure, testable table; the item has no label.

Every route is a separate `data object` (or class) rather than one parameterised route, so each tab
keeps its own saved state. There is no placeholder screen any more: the former `ComingSoonScreen` was
deleted when the last placeholder tab became Settings.

**The bar is shown exactly when the current back-stack destination is a tab** — never on
`Onboarding`, `AlarmEditor`, `ChangeStation` or `Example`. The rule is `TopLevelDestination.current(isOnRoute)`: it returns the
matching tab (which is also the selected one) or `null` (bar hidden). It takes a
`(KClass<out Screen>) -> Boolean` predicate rather than a `NavDestination`, so `AppNavigation`
calls it with `destination.hasRoute(it)` and `TopLevelDestinationTest` with a plain class
comparison — the tested rule is the shipped rule, not a copy.

Insets: the outer `Scaffold` has `contentWindowInsets = WindowInsets(0)` and applies none itself.
Each screen's own `Scaffold` handles the top (a system-bars inset here would add a gap above Home's
`TopAppBar` and pad Onboarding twice), and `NavigationBar` applies the navigation-bar inset on its
own. The `NavHost` gets `Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)`; the
consume is what stops Home's inner `Scaffold` re-applying the bottom inset as a double gap above the
bar. Off a tab the bar is absent, `innerPadding` is zero and the screen lays out as if there were no
outer `Scaffold`.

**Switching tabs** goes through `navigateToTab` in `AppNavigation.kt`:
`popUpTo<Screen.Home> { saveState = true }`, `launchSingleTop = true`, `restoreState = true`. So:

- **At most one tab sits above Home** on the back stack — switching never builds up history.
- **Back from any other tab returns to Home; back from Home leaves the app**, on both launch paths.
  No `BackHandler` is involved.
- **Each tab keeps its state.** The tab being left is saved and the one entered is restored, so
  Home's `HomeViewModel` survives a round trip and its readings reappear without a reload.
- **Reselecting the current tab does nothing** — the click is skipped, not a refresh.

It pops up to `Screen.Home`, deliberately **not** `graph.findStartDestination()` as in the usual
Compose sample: after a fresh install the graph's start destination is `Onboarding`, which has
already been popped, and a `popUpTo` on a destination not in the back stack is silently ignored —
history would then pile up with every tab switch.

**Adding or replacing a tab:** add or rename its `Screen` object and `TopLevelDestination` entry
(icon and a `nav_*` string resource in all four languages), and register its `composable<Screen.X>`
in `AppNavigation`. Nothing else knows about the tab. Update the order and name assertions in
`TopLevelDestinationTest`.

#### Diary

The third tab, `feature/diary`: the user's recorded feelings and the daily pollen levels at a
chosen station over the last week, month or year ending yesterday, as one line per checked pollen
type plus the feeling line. Top to bottom: station dropdown, range buttons, the line "Compare how you
felt with the pollen levels at a station.\*", the chart, one checkbox per pollen type, and the note
"\* This is not a medical diagnosis. If you suspect a pollen allergy, please see a doctor." The
screen scrolls as a whole and has no pull-to-refresh.

`DiaryViewModel(selectedStationRepository, stationRepository, speciesRepository,
stationHistoryRepository, diaryRepository, clock)` reads the stored home station **once** and never
writes it, fetches the station list and the pollen types (in parallel, once — kept across reloads,
and a Retry fetches only what has not arrived), then loads
`GET /pollen/stations/{abbr}/history?range=…` through `StationHistoryRepository`: `Loading` →
`Content(stations, stationAbbr, range, historyRange, history, species, checked, entries, isLoading)` |
`Error(AppError)` with Retry. No stored station is `Error(NoStationSelected)`, never a spinner; a
failed station or pollen-type list is an `Error` too, and an empty station list `Error(NoStations)`.

**The chosen station, range and checked types are ViewModel fields**, not only `Content` fields, so
a failed load's `Error` and its Retry resume all three. None is persisted: they survive tab switches
(the ViewModel lives in `navigateToTab`'s saved state) but an app restart opens on the home station,
`MONTH` and every type checked again.

**Station choice.** An `ExposedDropdownMenuBox` (as in onboarding and the alarm editor) lists every
station by name and calls `onStationSelected(abbr)`. It opens on the stored home station — the first
listed station if that is no longer listed — and **never writes `SelectedStationRepository`**, so
Home keeps its station. Choosing the station already shown does nothing. Like a range change it
keeps the current graph with `isLoading` until the new history arrives; `stationAbbr` is the
selection and moves at once, `history.stationAbbr` is what is drawn. The feeling line is the same
for every station — answers are about the user, not a place.

**Pollen-type filters.** One checkbox row per `SpeciesRepository` type, in the backend's order, each
with a short bar in its line's colour so the rows double as the chart's legend; the whole row is one
toggleable focus stop. `checked` starts with every type; `onSpeciesToggled(id)` changes it and
**never reloads**. `Content.notMeasured` (derived) is every type with no value on any day of the
history on screen; its row is disabled, shown unchecked and labelled "Not measured here", so a
missing line is not read as "no pollen". Its choice stays in `checked`, so the line comes back at a
station that measures it. `Content.shownSpeciesIds` — checked and measured, in order — is what the
chart draws and counts in its description.

**The feeling line.** `DiaryViewModel` collects `DiaryRepository.entries` for as long as it lives;
`Content.entries` are the answers whose date lies in `history.from..history.until` **and** before
`swissToday(clock)` — today's answer is never plotted, even if a history reached today. The filter
follows the history on screen, not the selected range, so while a range change loads the old
history keeps its own answers. An answer recorded while the Diary is open joins the graph through
the flow, with no history reload. `Content.hasNoEntries` (derived, no answer in the window) overlays
the hint "Answer 'How do you feel today?' on Home to see your line here." at the top of the chart —
outside the chart's semantics node, so a screen reader reads it on its own.

**The shared scale.** Feelings and pollen levels share the chart's y-axis through `Feeling.level`:
Very good = Low, Good = Moderate, Bad = High, Very bad = Very high, so higher is worse for both and
a type whose line rises with the user's bad days lines up with them. `None` has no feeling. The
feeling words (`core/ui/feeling/FeelingLabel.kt`'s `Feeling.label()`, shared with Home's prompt
buttons) label the axis's right side, the severity words its left.

**Ranges.** A `SingleChoiceSegmentedButtonRow` (Week / Month / Year) under the station dropdown calls
`onRangeSelected(range)`; it opens on `MONTH`, and the range already selected does nothing. A change
keeps the current graph on screen with `isLoading` (a linear indicator above the chart, its slot
always reserved) until the new history arrives. `range` is the selection and moves at once;
`historyRange` is what `history` covers, and the chart is laid out and announced for that, so the two
differ only while loading.

**The chart.** `DiaryChartGeometry` (`chart/`, pure, no Compose import) lays the history out for a
plot of a given size: x by day index (first day at the left edge, last at the right), y by severity
with `NONE` at the bottom and `VERY_HIGH` at the top, so higher is always worse; one line per id it is
given (the caller passes the checked ones; an id with no value on any day gets no line), each a
list of polylines split at every day without a value — a single known day between gaps survives as
a one-point run, drawn as a dot — so the chart never bridges a day it does not know; the feeling
line (`FeelingLine`) split the same way at every unanswered day, with `dots` on every answered day
(answers outside the days shown are ignored); one grid line per severity and one `FeelingTick` per
feeling at its level's height; date labels thinned per range — every day for a week, every 7th day counted back from
yesterday for a month, the first of each month for a year. `DiaryChart` draws it on a canvas —
severity words on the left, feeling words on the right, dates below in the app's language
(`DateWording.shortDate`, "4 Sep"; for a year `monthOnly`, the month alone, "Oct") — where the
language's words are too wide for every tick (German's twelve months on a phone), only every n-th
is labelled, counted back from the last so the latest is always named (`dateLabelStep`, measured
at draw time, tested in `DateLabelStepTest`) — 2 dp lines in each species' palette colour, and on top of them the feeling line,
3 dp in `colorScheme.onSurface` with a dot on every answered day — and is one `clearAndSetSemantics` node announced as
`diaryChartDescription(...)`: "Graph of your diary and 7 pollen types, last 30 days" ("last 7 days",
"last 12 months" for the other ranges). It stays pure by returning a `DiaryChartDescription(resource,
speciesCount)` — one plural per range (`diary_chart_description_week|month|year`), the count as both
quantity and argument — which the chart resolves with `resolve()`; `DiaryChartGeometryTest` pins the
resource and count, not the sentence.

**Species colours** are `SpeciesPalette.kt` in `:theme` — 7 categorical colours, each with an
explicit light and dark value and its WCAG contrast against `surfaceLight` / `surfaceDark` recorded
beside it (all ≥ 3.5:1). Their hues stay inside cyan → blue → violet → magenta so no line can be
read as a severity (grey, green, amber, orange and red belong to `SeverityPalette`); the species
order is one of the few that clear colour-vision checks for neighbouring legend entries in both
schemes, so **re-run a palette validator on any change** rather than nudging a value. The one
documented near-miss is Oak's light teal, ΔE 9.4 from `severityLowLight` — accepted because
severity colours are not drawn in the chart. The id → colour mapping is `core/ui/species/
SpeciesColors.kt`.

#### Alarms

The fourth tab, `feature/alarms`: the user's pollen alarms, delivered as push notifications — a
permission gate, the backend-loaded list, creating, editing, pausing and deleting daily reports and
threshold alerts (see "Alarm editor" below), delivery (see "Alarm delivery" under the server) and
keeping the push token current as FCM rotates or drops it (see "Firebase").

**Notifications are checked first.** `AlarmsUiState` is `CheckingPermission` (renders nothing; the
permission has not been read yet) → `PermissionRequired(state)`, or, once `ENABLED`, the list:
`Loading` → `Content(alarms, isRefreshing)` | `Error(AppError)`.
`AlarmsViewModel.onPermissionState(state)` maps the two non-enabled
`NotificationPermissionState`s to `PermissionRequired`, in either direction, so revoking later brings
the explanation back. The ViewModel never touches a platform API.

**The list loads on the first `ENABLED` and not before**, so a user who never allows notifications
causes no backend contact and no device registration. The ViewModel tracks permission and list
separately: a later `ENABLED` does not reload through `onPermissionState`, and a list kept through a
revocation is shown again unchanged when notifications come back. Reloading on return is
`onResume()` instead (below). (On Android revoking the permission kills the
process, so in practice that return is a fresh load of the same list — same stored device id.) It
has Home's cycle: pull-to-refresh keeps the rows with `isRefreshing` for at least
`MIN_REFRESH_INDICATOR`, and `Error` offers Retry. `Error(AppError.PushUnavailable)` — iOS — says
"Push notifications aren't available on this device yet" with no Retry. Station names come from
`StationRepository` and pollen-type names from `SpeciesRepository`, both fetched only when there is
an alarm to describe (cached after that; the abbreviation or species id stands in if one fails), so
the iOS path makes no network call at all.

Each row is the station name over `summaryOf(alarm, speciesNames, severityLabels, dates, wording)` (`AlarmSummary.kt`), e.g. "Daily
report at 08:00 · Mon–Fri · Birch, Grasses ≥ Moderate" or "Threshold alert 07:00–21:00 · Every day ·
Birch, Grasses ≥ High" — the same three parts for both types: types in display order ("All pollen types"
when every one is selected), the minimum only when it is not "Any", and days collapsed by
`daysSummary` — "Every day", otherwise `DateWording.weekdays`: runs of three or more as a range
("Mon–Fri", "Mo–Fr"), shorter runs listed ("Sat, Sun"). Pollen-type names, severity words
(`PollenSeverity.label()`; `minimumLabel()` calls `NONE` "Any") and weekdays are in the app's
language: the ViewModel puts only the backend's names by id (`AlarmListItem.speciesNames`) in the
state, and the screen resolves the words and calls the pure `summaryOf`. It sits in
`presentation/`, not `domain/`, because it is wording. A paused alarm's summary is wrapped in
"Paused · …". The sentences are `AlarmSummaryWording` — the `alarm_summary_*` and `alarm_paused`
patterns read unformatted by `rememberAlarmSummaryWording()`, their `%1$s` / `%2$s` filled by
`summaryOf` itself, as `DateWording` does with `date_day_month` — so `AlarmSummaryTest` pins the
chosen pattern and its arguments with marker words, not English sentences.

**Tapping a row** opens `Screen.AlarmEditor(id)` (`onEditAlarm`). **Its `Switch` pauses or resumes
the alarm optimistically**: `AlarmsViewModel.onEnabledToggled(id, enabled)` flips the row at once and
stores the whole alarm with the new flag through `AlarmRepository.update` (a full `PUT`). If that
fails the row flips back and `AlarmsEvent.ToggleFailed(error, enabling)` — a buffered `Channel`, as
for any one-shot event — shows a snackbar once ("Couldn't resume the alarm." / "Couldn't pause the
alarm." before the reason). The switch is its own focus stop, named "Alarm for
<station>", so TalkBack reads its on/off state with what it switches; the row's click label is "Edit
alarm".

**`onResume()` reloads quietly.** The screen calls it on every `RESUMED`, right after handing over
the permission — so back from the editor, from another tab or from another app. With a list on
screen and notifications enabled it reloads without `isRefreshing` and swaps the rows when the
answer arrives; a failed quiet reload keeps them (pull-to-refresh shows the error). Before the first
load, while any load runs, or without permission it does nothing. "Create alarm" is the empty state's
button and, once there are alarms, an extended FAB; the list has bottom padding so the FAB never
covers the last row.

**At ten alarms** (`MAX_ALARMS`, the backend's limit) `Content.limitReached` — derived from the
list, so it follows every reload — disables the FAB and shows a hint fixed *above* the list (as a
first list item a reload into the limit would leave it scrolled out of view). Material's extended
FAB has no disabled state, so it takes the spec's disabled colours (composited, so rows do not show
through), ignores clicks and is `disabled()` for TalkBack. If the limit is hit anyway (another
install sharing the id, or a race), the backend's `409` arrives as `AlarmLimitReachedException`, which
the editor shows as its `saveError` (`AppError.AlarmLimitReached`).

**`AlarmRepositoryImpl` registers lazily.** Every alarm call — `alarms`, `alarm(id)`, `create`,
`update`, `delete` — goes through one `withDevice { }` wrapper:
with no stored id it asks `PushTokenProvider` for a token (`Unavailable` → `PushUnavailableException`,
before any request), `POST /devices`, and stores the issued id. If a device call answers `404`
(`UnknownDeviceException` from `AlarmApiService` — the backend lost its database, say) it clears the
id, registers once and retries once; a second `404` is a `Failure`. Registration is behind a
`Mutex`, and a re-registration that finds a newer id already stored uses it instead of replacing it
again, so concurrent calls never register twice. `AlarmApiService` checks status codes itself rather
than letting `body()` read an error response as data; any status it does not map is
`HttpStatusException`. `UnknownDeviceException` lives in `domain/model/AlarmFailures.kt` with the
other alarm failures, since it can reach a caller.

On a single alarm's path (`PUT` / `DELETE …/alarms/{alarmId}`) the backend answers the **same**
`404` for an unknown device and for an alarm the device does not have. Treating the latter as an
unknown device would re-register and cut the install off from all its other alarms, so on that `404`
`AlarmApiService` asks the device's list: a `404` there is `UnknownDeviceException` (re-register as
above), otherwise it is `AlarmNotFoundException`, a plain `Failure`. `alarm(id)` has no endpoint of its
own — it is the device's list, filtered — and fails with `AlarmNotFoundException` too.

`AlarmRepositoryImpl` is also `core/push/PushTokenUpdater` (one Koin instance bound as both, so the
two share the registration `Mutex`). `updateToken(token)` is deliberately **not** a `withDevice`
call: with no stored id it makes no request and succeeds — the first registration will send
whichever token is current then — and otherwise sends `PUT /devices/{id}/token`. A `404` there
drops the stored id (if it is still the one used) so the next alarm call registers afresh, with the
current token; it never registers from the push service itself.

`core/notifications/` holds the platform side. `NotificationPermissionState` is `ENABLED`,
`CAN_REQUEST` (button "Allow notifications", the system prompt) or `MUST_OPEN_SETTINGS` (button
"Open settings", the app's notification settings). `rememberNotificationPermissionController()` is a
**`@Composable expect fun`** for the same reason as the location requester: Android's prompt needs
an activity-scoped launcher. Its `currentStatus(askedBefore)` **suspends**, because iOS only answers
through a completion handler.

- **Android** — `NotificationManagerCompat.areNotificationsEnabled()` first, so a granted permission
  with notifications switched off is not enabled. Not enabled below API 33 → `MUST_OPEN_SETTINGS`
  (there is no prompt). On API 33+, `shouldShowRequestPermissionRationale` is `false` both before
  the first request and after a permanent denial; the persisted `askedBefore` flag tells them apart
  (`askedBefore && !rationale` → `MUST_OPEN_SETTINGS`, else `CAN_REQUEST`). Settings is
  `ACTION_APP_NOTIFICATION_SETTINGS`. The manifest declares `POST_NOTIFICATIONS`.
- **iOS** — `UNUserNotificationCenter`: `notDetermined` → `CAN_REQUEST`, `authorized` /
  `provisional` / `ephemeral` → `ENABLED`, anything else → `MUST_OPEN_SETTINGS`; settings is
  `UIApplicationOpenSettingsURLString`. Compile-verified only.

**`AlarmsScreen` re-reads the state whenever its lifecycle reaches `RESUMED`** (collecting
`lifecycle.currentStateFlow`), which is what makes returning from system settings update the screen
on its own. It also re-reads when `askedBefore` changes and when a prompt is answered, since iOS
prompts with an alert that does not pause the screen. It waits until `AlarmsViewModel.askedBefore`
(`null` until read) is known, so the wrong button never flashes. Answering the prompt calls
`onPermissionRequested()`, which sets the flag through `NotificationPermissionPreferences`.

`AlarmsViewModelTest` covers the mapping, the flag, the list cycle, `onResume` and the optimistic
switch (`FakeAlarmRepository.gate` holds a load in flight, `updateGate` an update).
`AlarmRepositoryImplTest` drives registration, the `404` retry for every call, the unknown-alarm
`404` that must not re-register, `updateToken` with and without a stored id, and both `schedule`
variants through `MockEngine`, with `FakePushTokenProvider` and
`FakeDeviceRegistrationRepository`. The permission controllers and `FirebasePushTokenProvider` are
checked by hand.

##### Alarm editor

`Screen.AlarmEditor(alarmId: String? = null)` — not a tab, so the bottom bar is hidden on it. The
list's "Create alarm" navigates to it without an id, a tapped row with that alarm's id
(`toRoute` in `AppNavigation`, handed to the ViewModel through Koin's `parametersOf`). Its `onDone`
(`popBackStack`) runs on `AlarmEditorEvent.Done`, sent on a buffered `Channel` after a successful
save or delete, and when leaving is allowed.

**Leaving goes through `onBack()`** — the top bar's arrow and the system back alike (the
multiplatform `BackHandler` from `org.jetbrains.compose.ui:ui-backhandler`, an explicit
`commonMain` dependency). With unsaved changes (`form.isDirty`) it sets `showDiscardDialog` ("Discard
changes?" — Discard → `Done`, Keep editing closes the dialog); without, or from `Loading` / `Error`,
it sends `Done` at once.

`AlarmEditorViewModel` loads the stations and the pollen types — and when editing, the alarm
(`AlarmRepository.alarm(id)`) — in parallel (`Error` with Retry if any fails) and opens
`Editing(form, stations, species, canDelete, isSaving, isDeleting, saveError, deleteError,
showDiscardDialog, showDeleteDialog)`; an empty station list is `Error(AppError.NoStations)`. An existing alarm opens on `AlarmFormState.fromAlarm(alarm)`: every field as
stored, `typeLocked` (the type toggle is shown disabled and `withType` changes nothing) and its
paused state kept, since the list's switch owns that. Save then calls `update(id, draft)`, and "Delete
alarm" (existing alarms only, `canDelete`) asks first (`showDeleteDialog`); confirming deletes and
sends `Done`, a failed delete stays with its `AppError` in `deleteError` (shown in the
`DELETE_ALARM` context). A new alarm opens on
`AlarmFormState.newDailyReport(home, speciesIds)`: the stored home station (the first station if it
is no longer listed), every pollen type, every day, "Any", 08:00. Choosing another station never
touches the stored home station.

**`AlarmFormState` (`domain/model/`) holds every rule**, pure and unit-tested: field changes return a
new state, `isValid` needs at least one pollen type, one day and — for a threshold alert — a window
whose end is after its start (`isWindowValid`; windows across midnight are out of scope), `isDirty`
compares against the state the editor opened with (so an undone change is not a change), and
`toDraft()` is the `AlarmDraft` a create sends. `type` (`AlarmType.DAILY` | `THRESHOLD`) follows the
schedule; `withType` switches it and resets what means something different in each — daily is "Any"
at 08:00, threshold is High from 07:00 to 21:00 — keeping station, types and days. `severityOptions`
is Any … Very high for a daily report and Low … Very high for a threshold alert. `withTime` only
applies to a daily report, `withWindowStart` / `withWindowEnd` only to a threshold alert. `Editing.canSave` is `isValid && !isBusy`
(`isBusy` = saving or deleting); `save()` is ignored otherwise, and so are field changes while a save
or delete runs — the save sends the form
as it was when tapped. A failed save keeps the form and shows its `AppError` as `saveError` (in the
`SAVE_ALARM` context) until the next save or delete, which clears both errors. A backend `400`
arrives as `InvalidAlarmException` with the server's `error` text, which stays in the exception — the
screen shows `AppError.InvalidAlarm`'s sentence.

The screen: a `SingleChoiceSegmentedButtonRow` type toggle ("Daily report" / "Threshold alert"), a
station `ExposedDropdownMenuBox` (as in onboarding), `FilterChip`s in `FlowRow`s for pollen types,
severity and days (day chips announce the full day name), then either a time button that opens a
24-hour `TimePicker` in an `AlertDialog` and is announced as "Report time 08:00", or two of them for
the window ("Window start 07:00", "Window end 21:00") with a hint while the end is not after the
start, the hint "Times are Swiss time.", and Save with a progress indicator. Section titles are headings.
`AlarmEditorViewModelTest` covers loading (new and edit), every load error, `isSaving` under a gated
create (`FakeAlarmRepository.createGate`), `Done` exactly once, a failed save, the discard dialog
only when dirty, the locked type, and delete only after confirmation; the composable is checked by
hand.

#### Firebase

Push is FCM, and **FCM is the one place the app depends on Google Play services** — push on devices
without them is out of scope, and location deliberately stays on the platform provider.

- `composeApp/google-services.json` is **committed**: it identifies the Firebase project and is not a
  secret. It holds two Android clients, `ch.stenzel.tim.polleninfo` and
  `ch.stenzel.tim.polleninfo.debug` (the debug `applicationIdSuffix`); without a client for the
  variant's id, `process<Variant>GoogleServices` fails the build. Re-download it from the Firebase
  console after adding an app id.
- Gradle: the `com.google.gms.google-services` plugin on `:composeApp`, and the Firebase BoM plus
  `firebase-messaging` in `androidMain` only.
- `core/push/PushTokenProvider` (`suspend fun token(): PushTokenResult` — `Available(token)` |
  `Unavailable`) is bound in `platformModule`: `FirebasePushTokenProvider` on Android wraps
  `FirebaseMessaging.getInstance().token` with `suspendCancellableCoroutine`; iOS binds
  `UnavailablePushTokenProvider`. A *failed* token task (no Play services, no network) is thrown,
  not `Unavailable`, so the screen offers Retry — `Unavailable` is reserved for "this platform cannot
  receive push".
- **Receiving.** `PollenInfoApplication` creates the two channels on every start —
  `daily_report` ("Daily reports") and `threshold_alert` ("Threshold alerts"), default importance,
  `core/push/NotificationChannels.kt` — before any push can arrive, since FCM may start the process
  with no activity. Their names are Android string resources (`notification_channel_*`) read
  through `appLanguageContext()`, and `MainActivity.onCreate` creates them again — a language change
  re-creates the activity, and re-creating an existing channel updates only its name, keeping what
  the user set. Their ids are the server's `PushChannel` ids. In the background the system shows
  a notification message itself, on the channel the message names, with `ic_notification` (the
  manifest's `default_notification_icon`) and a tap that opens the launcher activity. In the
  foreground FCM shows nothing, so `PollenFirebaseMessagingService.onMessageReceived` posts it on the
  same channel with a tap that opens `MainActivity` — the two cases look the same. The service, the
  channels and the icon are checked by hand.
- **Token rotation.** `PollenFirebaseMessagingService.onNewToken` hands the new token to
  `PushTokenUpdater` (Koin `inject()`; it lives in `core/push` so the service does not import the
  alarms feature). It runs the update with `runBlocking` under a 20-second timeout, on the Firebase
  worker thread that calls it — the service may be stopped as soon as `onNewToken` returns, so a
  launched coroutine could be cancelled mid-request. A failed update is only logged: FCM does not
  call again for the same token, so the backend keeps the old one until the next rotation or until
  it reports the old one unregistered (accepted for now).
- **Sending** needs a Firebase service-account key for the server, passed as a file path in
  `FCM_CREDENTIALS`. It is **never committed** (see "Alarm delivery").

#### Settings

The fifth tab, `feature/settings`: one scrolling screen (`SettingsScreen`, its `rememberScrollState`
restored through `navigateToTab`'s saved state) under a `TopAppBar` titled "Settings"
(`settings_title`). Section titles are `semantics { heading() }`. Top to bottom:

- **Default station** (`settings_section_station`) — the home station's name in one full-width row
  ≥ 48 dp (`onClickLabel` "Change the default station"), which opens `Screen.ChangeStation`.
  `SettingsViewModel` **observes** `SelectedStationRepository`, so the row shows a new station as
  soon as it is saved, and takes the name from `StationRepository` (fetched once); until the list
  arrives, or if it fails, the name stored with the selection stands in — never an error for the
  screen. "Not set" only while nothing is stored (unreachable past the startup gate).
- **Language** (`settings_section_language`) — the current choice in the same kind of row
  (`onClickLabel` "Change the language"), which opens an `AlertDialog` of radio rows (a
  `selectableGroup`, Cancel to close): System default (`settings_language_system`, translated),
  English, Deutsch, Français, Italiano — each in its own name, a code constant, so a user can find
  their language whatever the app is in. Choosing one sets it through `LanguageRepository` and
  closes the dialog; choosing the current one only closes it. On Android the screen is re-created
  in the new language at once (its ViewModel survives, so does every tab's); on iOS a note
  (`settings_language_restart_note`) says the change applies the next time the app is opened.
  The screen re-reads the language on every `RESUMED`, so a change in Android's per-app language
  screen shows in the row on return.
- **Impressum** (`settings_section_impressum`; "Mentions légales" / "Note legali") — "Developed by
  Tim Stenzel" (`settings_developed_by`, the name a code constant) and the contact address
  `developer.mobile.t3s@gmail.com`, a full-width row ≥ 48 dp that opens
  `mailto:developer.mobile.t3s@gmail.com?subject=PollenInfo`.
- **Data source** — the attribution in the exact form MeteoSwiss's terms of use prescribe for the
  language (`settings_data_source_attribution`: "Source: MeteoSwiss", "Quelle: MeteoSchweiz",
  "Source: MétéoSuisse", "Fonte: MeteoSvizzera" — **do not rephrase**, and the French one keeps the
  prescribed "Source:" without the usual space before the colon), a link to their site in the same
  language (`settings_meteoswiss_url`, a per-language resource: `meteoswiss` / `meteoschweiz` /
  `meteosuisse` / `meteosvizzera.admin.ch`, shown without `https://`), and the statement that
  PollenInfo is independent and not affiliated with or endorsed by MeteoSwiss
  (`settings_data_source_disclaimer`), which those terms also require.
- **Version** — "Version {name} ({code})" (`settings_version`) from `AppInfo`, hidden when it is
  `null`. Gradle's values show through as they are, so a debug build reads "Version 1.0.0-debug (1)"
  (`versionNameSuffix`) and a release "Version 1.0.0 (1)".

Both links go through `LocalUriHandler`; a device with nothing to open them with swallows the tap
rather than crash. Each row's icon is decorative and its `onClickLabel` says what a double tap does.
`SettingsViewModel(selectedStationRepository, stationRepository, languageRepository, appInfo)` holds
no text — only the station name, the language (`language`, `showLanguageDialog`,
`languageAppliesOnRestart` once a change was made where it does not apply immediately) and the
version pass through it (`SettingsViewModelTest`); the composable is checked by hand.

**Changing the default station.** `Screen.ChangeStation` — not a tab, so the bottom bar is hidden —
is a `TopAppBar` "Default station" with a back arrow over `StationPickerContent` (see "Location"),
a line saying Home follows this station while alarms and the Diary keep their own, and Save with a
progress indicator. `ChangeStationViewModel(selectedStationRepository, stationRepository,
coarseLocationProvider, findNearestStation)` reads the stored station **once**, as the picker's
`initialAbbr` and as `ChangeStationUiState.storedAbbr`. `canSave` is a pick that differs from it and
no save running; picks are ignored while a save runs. Save writes `SelectedStationRepository` and,
on success, sends `ChangeStationEvent.Done` on a buffered `Channel` (`popBackStack` in
`AppNavigation`); `isSaving` stays set, so it cannot save twice on the way out. A failed write keeps
the screen with `saveError` (a `Boolean`, local storage, as on onboarding), cleared by the next pick.
Back — arrow or gesture — leaves without saving. Only the home station changes: existing alarms keep
their stations, a new alarm starts on the new one, and an open Diary keeps the station chosen in its
dropdown. `ChangeStationViewModelTest` covers the preselection, `canSave` (gated save through
`FakeSelectedStationRepository.writeGate`), `Done` once, a failed save and the list's retry.

### The startup gate

`App()` does not compose `AppNavigation` until `core/startup/StartupViewModel` has resolved
`StartupState` (`Loading` → `NeedsOnboarding` | `Ready`) from the **first** value of
`SelectedStationRepository.selectedStation`. `AppNavigation` therefore takes its `startDestination`
as a parameter and is built once, already correct.

This is what makes "no onboarding flash" structural rather than a timing accident: while the answer
is unknown there is no navigation graph at all, so onboarding cannot appear and then be navigated
away from. No launch-time `popUpTo` bookkeeping, and no splash destination reachable with back.

Two deliberate choices:

- **`Loading` is not a navigation destination.** It is a moment before the graph exists, not a place
  the user can be. `App()` renders an empty themed `Surface` for it — not a spinner, since the read
  is a local file access and a one-frame indicator reads as a glitch.
- **First value only, not an ongoing subscription.** "Where does the app start" is asked once.
  Following the flow would rebuild the graph the instant onboarding writes its selection, yanking
  the user out of the navigation that write just triggered.

### Localization

The app is being translated into German, French and Italian; English is the default. **Done so far:**
every screen — the bottom bar's tab names, Home (feeling prompt included), All stations (the map's spoken
description included), the reading composables in `core/ui/severity` they share, the Settings
screen, onboarding, the change-station screen and the station picker they share, the Diary (chart
description, axis words and the no-answers hint included), Alarms (permission gate, list, row
summaries, limit hint, snackbars, the editor with its dialogs and time pickers, and every spoken
label) — every error sentence (`error_*`, see "Error handling"), the Android notification channel
names, and the vocabulary every screen shares — pollen-type, severity and feeling words, month and
weekday names and date forms (below). **Not translated, by decision:** station names, "PollenInfo",
and `feature/example`, whose text is English-only `example_*` resources in `values/` (so the
reference shows the pattern too). Still open: the alarm push notifications themselves, which the
server currently words in English.

- **Vocabulary lookups.** Each has a composable form and a `StringResource` form for code outside
  Compose (`getString`, the push service later): `speciesName(id, fallback)` /
  `speciesNameResource(id)` in `core/ui/species` (`species_*`, keyed by the backend's ids; an id
  the app does not know shows the server's English name rather than nothing — pinned by
  `SpeciesNamesTest`), `PollenSeverity.label()` / `labelResource()` (`severity_*`),
  `minimumLabel()` / `minimumLabelResource()` in `feature/alarms` (`alarm_minimum_any`, the other
  bands share `severity_*`) and `Feeling.label()` / `labelResource()` (`feeling_*`). The server's
  `name` fields stay in the domain models and are only ever the fallback. Station names are never
  translated.
- **Dates.** `core/ui/format/DateWording` is a pure value — 12 full and 12 short month names, 7
  full and 7 short weekday names (Monday first), and `dayMonthPattern` (`%1$s %2$s`, German
  `%1$s. %2$s`) — with the forms as functions: `fullDate` "29 July" / "29. Juli" / "29 juillet" /
  "29 luglio" (stale reading, refresh caption), `shortDate` "4 Sep" / "4. Sep." / "4 sept." / "4 set"
  and `monthOnly` "Oct" / "Okt." / "oct." / "ott" (Diary axis), `weekdayShort` / `weekdayFull` (day
  chips, spoken full) and `weekdays(days)` ranges "Mo–Fr" and lists "Sa, So" (alarm summaries).
  Abbreviations carry their own full stop. The names are `string-array` resources
  (`date_months_full`, `date_months_short`, `date_weekdays_full`, `date_weekdays_short`) plus
  `date_day_month`; composables build it with `rememberDateWording()`, suspending code with
  `loadDateWording()`. Times are `formatTime` — `HH:mm` in every language. `DateWordingTest`
  pins every form against literal per-language fixtures (`DateWordingFixtures.kt` in
  `commonTest`, to be kept in step with the resources) — no resource loading in tests. Never use
  kotlinx-datetime's `MonthNames.ENGLISH_*` / `DayOfWeekNames.ENGLISH_*` in app code.
- **Formatters stay pure.** A function that words something (`ReadingAge.label(dates)`,
  `refreshedLabel`, `summaryOf`) takes the `DateWording` and the resolved words as parameters; the
  screen resolves them in composition. Where the sentence itself is a translation it returns a
  `ResourceText(resource, args)` (`core/ui/format`) — the `StringResource` and its formatted
  arguments — which the screen turns into text with `resolve()` (`load()` outside Compose), so
  `ReadingAgeLabelTest` pins the chosen sentence and its arguments without loading a resource. That is why `HomeUiState.Content.drivenBy` is the
  `SpeciesReading` itself — the screen names it — and why a chart resolves its labels before its
  draw lambda, which is not composable.

- **Resources.** Compose Multiplatform resources in
  `composeApp/src/commonMain/composeResources/values{,-de,-fr,-it}/strings.xml`; the generated
  accessor is `ch.stenzel.tim.polleninfo.resources.Res` (`compose.resources { packageOfResClass }`
  in `composeApp/build.gradle.kts`), internal to the module, so `commonTest` can compare
  `Res.string.x` values (`StringResource` has value equality). Composables use
  `stringResource(Res.string.x, args…)` (`stringArrayResource(Res.array.x)` for a
  `string-array`). Placeholders are positional only — `%1$s`, `%2$d` — since
  that is all Compose resources substitute. Android-only text goes in
  `androidMain/res/values{,-de,-fr,-it}/strings.xml` — the notification channel names, and
  `app_name` (`values/` only, `translatable="false"`).
- **Fallback.** A device language other than de/fr/it gets `values/` (English). That is all
  "System default" is: no language set, so resource resolution picks `values-de|fr|it` for a
  matching device language and English otherwise.
- **Choosing the language** (Settings, see there) goes through `core/language`:
  - **Android** — `AndroidLanguageRepository` over `AppCompatDelegate.getApplicationLocales()` /
    `setApplicationLocales(…)` (`androidx.appcompat`, explicit in `androidMain`). That is the
    system's per-app language on API 33+ — `res/xml/locales_config.xml` (en, de, fr, it) is its
    `android:localeConfig` and must list exactly `AppLanguage`'s languages — and below API 33
    AppCompat stores it itself through the manifest's `AppLocalesMetadataHolderService`
    (`autoStoreLocales`). It applies only to an `AppCompatActivity`, which is why `MainActivity` is
    one (the manifest theme is `Theme.AppCompat.DayNight.NoActionBar` for the same reason). A change
    re-creates the activity in place, like any configuration change.
  - **Text outside an activity** (notification channels, later the push service) uses
    `Context.appLanguageContext()` (`core/language/LocalizedContext.kt`, `androidMain`): the
    application context on API 33+, where the system localizes it, and below that a
    `createConfigurationContext` wrapper in the chosen locales. Below API 33 AppCompat only loads
    its stored choice once an activity has been created, so in a process FCM started without one
    it falls back to the device language.
  - **iOS** — `IosLanguageRepository` writes `AppleLanguages` = `[tag]` in
    `NSUserDefaults.standardUserDefaults` (removes it for System default), which iOS reads at launch:
    `appliesImmediately = false`. It reads back from the app's own persistent domain only, since the
    global domain's `AppleLanguages` is the device list and would make System default read as a
    language. Compile-verified only; needs `CFBundleLocalizations` (see "iOS wrapper configuration").
- **Keys** are `<area>_<thing>`: `nav_*`, `settings_*`, `error_*`, `home_*`, `onboarding_*`,
  `change_station_*`, `station_picker_*`, `species_*`, `severity_*`, `feeling_*`, `date_*`,
  `alarm_*`, `all_stations_*`, `diary_*`, `reading_*` (the reading composables of `core/ui/severity`:
  reading age, refresh caption, species list), and `common_*` for words several screens share ("Retry",
  "Back", "Cancel"). Keys starting with `example_` are reserved
  for the English-only reference feature and live in `values/` only.
- **Style.** German is Swiss Standard German — "ss", never "ß" — and says "du". French and Italian
  are formal ("vous" / "Lei"); French typography puts a narrow no-break space (U+202F) before `? ! ;`
  and uses « » quotes. Use typographic apostrophes (’), which also sidesteps XML escaping. Station
  names and "PollenInfo" are never translated.
- **The check.** `./gradlew :composeApp:checkTranslations` (a dependency of `check`) compares each
  language file with `values/` in the same resource set and fails, listing every problem, on a
  missing or extra key, a different placeholder multiset per key, a `string-array` with another
  number of items, a blank value (or array item), or "ß" in a
  `values-de` file. `example_` keys and `translatable="false"` keys are exempt from parity. It is
  `CheckTranslationsTask` in `composeApp/build.gradle.kts`, with plain file inputs, so it works with
  the configuration cache.
- **No hard-coded text.** Review new code with
  `grep -rnE '"[A-Z][a-z]+( [a-z]+)+' composeApp/src/commonMain composeApp/src/androidMain composeApp/src/iosMain --include='*.kt'`
  (comment and KDoc lines and `feature/example` left out). Every hit must be on this reviewed
  allowlist — text that is never shown: exception messages (`HttpStatusException`, the alarm
  failures in `AlarmFailures.kt`, `InvalidAlarmException`'s fallback in `AlarmApiService`, the
  activity `error(...)` in `NotificationPermissionController.android.kt`, the iOS Documents
  `requireNotNull`), log lines (`PollenFirebaseMessagingService`) and the lake names in
  `SwissLakes.kt` (data labels). Screens never show an exception's `message`; they show
  `AppError.message()`. Single quoted words outside the grep are ids (species, severity wire names,
  `AppleLanguages`, Info.plist keys) or the language names in their own language
  (`SettingsScreen`).

### Multiplatform gotchas

- **`commonMain` has no `String.format`** — it is JVM-only. It compiles for Android and then breaks
  the iOS build. Same for `java.*` anything, `UUID`, `SimpleDateFormat`. Use `kotlin.math` and
  `kotlinx-datetime` (a `commonMain` dependency) instead — its `LocalTime.Format { … }` builders
  cover date and time formatting, as in `ReadingAgeLabel.kt`. (`ExampleMapper.format` is a
  hand-rolled multiplatform replacement for numbers.)
- **Backtick test names may not contain a comma on Kotlin/Native.** The JVM accepts them, so
  `testDebugUnitTest` passes and then `compileTestKotlinIosSimulatorArm64` fails with "Name contains
  illegal characters". Another reason to compile the iOS test sources.
- **`Icons.Default.*` is not transitive.** `compose.material3` supplies it for the Android target
  and not for iOS, so a screen using an icon compiles for Android and then fails to resolve
  `androidx.compose.material.icons` on `compileKotlinIosSimulatorArm64`. `compose.materialIconsExtended`
  is an explicit `commonMain` dependency for exactly this reason — a second instance of the same
  trap as `String.format` above.
- Platform-specific pieces use `expect`/`actual` with a `.android.kt` / `.ios.kt` filename suffix,
  as in `core/network/HttpClientEngine.kt`.
- Always compile an iOS target after touching `commonMain`; the Android build alone will not catch
  non-portable code.

### Server conventions

Ktor plugin configuration is split into `plugins/` extension functions on `Application`
(`configureSerialization`, `configureLogging`, `configureRouting`) and composed in
`Application.module()`, which also builds the long-lived collaborators once — the
upstream `PollenService` (via `meteoSwissPollenService`), the `MeasurementService` and the
`HistoryService` over it, the database and the stores — and passes them into `configureRouting(...)`, then starts the alarm scheduler with the same
`MeasurementService` and alarm store. Routes are `fun Route.xRoutes(dependency)` extension functions grouped by
feature package, taking their collaborators as parameters so tests can supply their own instances.

```
server/src/main/kotlin/.../server/
├── plugins/            configureSerialization / configureLogging / configureRouting
├── pollen/
│   ├── domain/         PollenStation, PollenSpecies, PollenSeverity, PollenThresholds, SWISS_ZONE
│   ├── upstream/       PollenService, MeteoSwissPollenService, PollenCsvParser
│   ├── measurement/    MeasurementService, StationMeasurement, TtlCache
│   ├── history/        HistoryRange, historyWindow, HistoryService, StationHistory
│   ├── model/          Wire DTOs (@Serializable)
│   └── PollenRoutes.kt
└── alarm/
    ├── domain/         Alarm, AlarmSchedule (Daily | Threshold), DeviceId, AlarmId, newDeviceId,
    │                   ALARM_ZONE (= SWISS_ZONE), AlarmRules, PushMessage / PushChannel, AlarmValidation
    ├── store/          DeviceStore, AlarmStore, NotificationLog (+ Exposed implementations), tables,
    │                   PollenInfoDatabase
    ├── push/           PushSender, FcmPushSender, LoggingPushSender, pushSenderFromEnvironment
    ├── scheduler/      AlarmScheduler (tick) + launchAlarmScheduler (the minute loop)
    ├── model/          Wire DTOs incl. the polymorphic ScheduleDto
    └── AlarmRoutes.kt
```

**`SWISS_ZONE`** (`pollen/domain`, `Europe/Zurich`) is the one zone in which the server answers
"which day is it" — the history window and every alarm. `ALARM_ZONE` is an alias of it, so
`pollen/` never imports from `alarm/`.

Domain enums and threshold logic have no Ktor or serialization-transport concerns beyond
`@Serializable`; wire shapes are separate DTOs in `model/` so the public API can evolve
independently of the domain.

### REST API

| Method | Path                                    | Returns                                              |
| ------ | --------------------------------------- | ---------------------------------------------------- |
| GET    | `/health`                               | `OK`                                                 |
| GET    | `/pollen/stations`                      | All 15 stations with coordinates and altitude        |
| GET    | `/pollen/stations/{abbr}`               | One station (case-insensitive abbr), 404 if unknown  |
| GET    | `/pollen/stations/{abbr}/measurements`  | That station's latest reading, classified            |
| GET    | `/pollen/stations/{abbr}/history?range=week\|month\|year` | That station's daily levels, classified, over the days ending yesterday; `400 {error}` bad or missing range; `404` unknown station; `502` upstream failed with nothing retained |
| GET    | `/pollen/species`                       | The 7 taxa with display and latin names              |
| GET    | `/pollen/thresholds`                    | Per-species severity bands + unit                    |
| POST   | `/devices`                              | `{ "fcmToken": "…" }` → `201 { "deviceId": "…" }`; `400 {error}` if missing or blank |
| PUT    | `/devices/{deviceId}/token`             | `{ "fcmToken": "…" }` → `204`; `400 {error}` if missing or blank; `404` unknown device |
| GET    | `/devices/{deviceId}/alarms`            | `200 [Alarm]` in creation order; `404` unknown device |
| POST   | `/devices/{deviceId}/alarms`            | Alarm without `id` → `201 Alarm`; `400 {error}` invalid or malformed; `404` unknown device; `409 {error}` at 10 alarms |
| PUT    | `/devices/{deviceId}/alarms/{alarmId}`  | Alarm without `id` → `200 Alarm`; `400 {error}` invalid or malformed; `404` unknown device, unknown alarm or another device's alarm |
| DELETE | `/devices/{deviceId}/alarms/{alarmId}`  | `204`; `404` as for `PUT` |

#### Devices and alarms

There are no accounts. An install registers once, anonymously, and gets a `deviceId` — 128 bits from
`SecureRandom`, base64url without padding, always 22 characters. **The id is the secret**: whoever
holds it can read that device's alarms; alarms hold no personal data, so this is accepted for now.
Every path under `/devices/{deviceId}` answers `404` for an unknown id, which is how the app learns
to register again. **`PUT …/token`** replaces the device's push token when FCM rotates it
(`DeviceStore.updateToken`, `false` → `404`); it also brings back a device whose token was dropped. An unknown device's list is a `404`, never `200 []` — `AlarmStore.list` returns
`null` for it so the two cannot be confused.

```json
{ "id": "…", "enabled": true, "stationAbbr": "PZH",
  "species": ["BIRCH", "GRASSES"], "minSeverity": "NONE",
  "days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
  "schedule": { "type": "daily", "at": "08:00" } }
// or "schedule": { "type": "threshold", "from": "07:00", "until": "21:00" }
```

`schedule` is a `@Serializable sealed interface` (`ScheduleDto` on both sides) using the default
`type` class discriminator — neither `Json` configures one. Days are `DayOfWeek` names, times `HH:mm`
Swiss local time. The app keeps `minSeverity` a `String` on the wire, mapped through the same
`toPollenSeverity` / `toWireName` pair as measurement severities.

**Creating** (`POST /devices/{deviceId}/alarms`) takes the same shape without `id`. The body decodes
into `AlarmInputDto`, whose values are all plain strings, and `alarm/domain/AlarmValidation` — pure,
returning `ValidationResult.Valid(AlarmSpec)` | `Invalid(message)` rather than throwing — decides
what is wrong and names it in the `400 {error}`: at least one species and one day, known species,
severity and day names (exact enum names), a known station (case-insensitive, stored in its official
form), and times that are exactly `HH:mm` (parsed `STRICT`, since the default resolver reads
`24:00` as midnight). A threshold schedule also needs `until` after `from` (no window across
midnight) and a severity other than `NONE`, since "Any" would alert on nothing at all. Validation runs before the device is
looked up, so an invalid body for an unknown device is a `400`. `AlarmStore.create` returns
`CreateResult.Created(alarm)` | `UnknownDevice` (→ `404`) | `LimitReached` (→ `409`). **A device holds at
most ten alarms** (`MAX_ALARMS_PER_DEVICE`), counted in the same transaction as the insert, so two
concurrent creates at nine cannot both get through; the id is a random UUID, and `created_at` is
kept strictly increasing per device so two alarms created in the same millisecond still list in
creation order.

**Updating** (`PUT …/alarms/{alarmId}`) takes the same body through the same validation (shared
`receiveAlarmSpec`, so again a `400` before any lookup) and replaces every field but the id and
`created_at`, so an edited alarm keeps its place in the list. **Deleting** removes the alarm and,
through `ON DELETE CASCADE`, its notification log. **A device can never touch another device's
alarm**: `AlarmStore.update` returns `null` and `delete` `false` unless the alarm id *and* the device
id match in one statement, and the route answers the same `404` for an unknown device, an unknown
alarm and someone else's alarm, so an alarm id reveals nothing. **An update leaves the notification
log alone**: it is keyed per type, so editing a threshold alert neither repeats a type it notified
about today nor holds back a newly added one. Pausing is an update with `enabled = false`; the
scheduler only ever loads enabled alarms.

#### Persistence

Devices and alarms are the first state the backend must not lose, so the server is no longer
stateless: **its database file has to be kept across restarts and deployments.** SQLite through
JetBrains Exposed's DSL (`exposed-core`, `exposed-jdbc`, `org.xerial:sqlite-jdbc`). Exposed is
pinned to **0.61.x**: 1.x is built against Kotlin 2.2+ and would put that stdlib under our 2.1
compiler — revisit with the Kotlin bump.

- `alarm/store/PollenInfoDatabase` opens it: `fromEnvironment()` reads `POLLENINFO_DB` (default
  `./data/polleninfo.db`, relative to the working directory — `server/` under `:server:run`) and
  creates the directory; `file(path)` for a given path; `inMemory()` for tests and routing defaults
  (a shared-cache memory database kept alive by one held connection, since Exposed closes its
  connection after every transaction).
- **Every connection** runs `PRAGMA foreign_keys = ON` (SQLite defaults it off per connection) and a
  busy timeout. Isolation is `SERIALIZABLE`, one of the two SQLite supports.
- The schema is `SchemaUtils.create` on start. There is no migration tool: changing an existing
  table needs one first.
- Tables: `devices(id PK, fcm_token NULL, created_at)` — `fcm_token` is `NULL` once FCM reported it
  unregistered, which keeps the device and its alarms but stops their delivery until the app sends a
  new token — `alarms(id PK, device_id → devices,
  enabled, station_abbr, species, min_severity, days, type, at_time, from_time, until_time,
  created_at)` and `notification_log(alarm_id → alarms ON DELETE CASCADE, species, local_date,
  PK(all three))` — which pollen types each threshold alert has notified about on a Swiss date
  (ISO `yyyy-MM-dd`). Sets are comma-separated enum names, times `HH:mm`, so the file reads well in
  `sqlite3`.
- Store interfaces are `suspend`; the Exposed implementations run each transaction on
  `Dispatchers.IO`. `configureRouting`'s store defaults share one private in-memory database, never
  the production file, so `PollenRoutesTest` and `RoutingTest` need no database setup.

#### Alarm delivery

`alarm/scheduler/AlarmScheduler.tick()` runs at the start of every minute
(`launchAlarmScheduler` in `Application.module()`: a coroutine on the application scope that waits
for the next whole minute, logs and survives a failing tick, and is cancelled on
`ApplicationStopped`). The loop holds no logic; `tick()` is the tested surface.

Each tick takes the current minute in `ALARM_ZONE` (`SWISS_ZONE`, `Europe/Zurich` — every alarm day and time is
Swiss time, and `java.time` handles daylight saving), loads `AlarmStore.enabledWithDeliverableDevice()`
(enabled alarms whose device has a push token), keeps the daily reports due **this minute** and the
threshold alerts whose window is open, groups them by station and reads each such station **once** through the routes' shared `MeasurementService`.
Stations run in parallel children of a `supervisorScope`, each with its own `try`, so one station's
failure — upstream or delivery — never stops another's reports. A second tick within the same minute
does nothing. **There is no catch-up**: a minute the scheduler did not run in (backend down, clock
jump) is never replayed, since an "08:00 report" at 08:40 is worse than none.

**The rules are `alarm/domain/AlarmRules`** — pure, with `now` passed in, no clock, I/O or logging:

**Daily reports.**

- **Due** (`isDailyDue`): enabled, today's weekday selected, and the local time truncated to the
  minute equals `at`.
- **Current reading** (`isCurrent`): younger than `READING_STALE_AFTER` (3 hours — it must equal the
  app's `STALE_AFTER`) **and** from today's Swiss date. Judged on `measuredAt`, so a `Fresh` cache
  result for a file that stopped updating is not current either.
- **Current** → sent if the minimum is "Any" (`NONE`) or at least one selected type has reached it;
  otherwise nothing. Body: the selected types that have a reading, worst first, then in
  `PollenSpecies` order — "Grasses: High · Birch: Moderate", words as the app's
  `PollenSeverity.label()`. All of them at `NONE` → "No pollen of your selected types."; none of them
  reported by the station → "No reading for your selected types."
- **Not current** → with a minimum, nothing (never judged on old data). With "Any": "No current
  reading for Zürich. Latest from 06:00." (`HH:mm` today, `HH:mm yesterday`, else `d MMMM`), or
  "…Readings are currently unavailable." when there is no reading at all.
- Title "Pollen in <station>"; channel `PushChannel.DAILY_REPORT`; data `stationAbbr` and `alarmId`,
  so opening a station from a notification can be added without a backend change.

**Threshold alerts** (`evaluateThreshold(alarm, now, reading, notifiedToday)` → `ThresholdOutcome`
(message + the types to record) or `null`):

- **Active** (`isThresholdActive`): enabled, today's weekday selected, and `from <= local time <
  until` — start inclusive, end exclusive. Evaluated on **every** tick of the window; the
  30-minute cache bounds what that costs upstream, so a new reading is noticed within half an hour.
- **Only on a current reading** (the same `isCurrent`). A stale or missing one sends nothing, ever —
  an outage at the source must never produce a false "high today".
- **Qualifying types**: selected, reported by the station, `severity.atLeast(minSeverity)`, and not
  in `notifiedToday`. All of them go into **one** message, body as a daily report's ("Grasses: Very
  high · Birch: High"); none → `null`. Same title and data, channel `PushChannel.THRESHOLD_ALERT`.
- **At most once per type per alarm per Swiss day.** `alarm/store/NotificationLog`
  (`notifiedSpecies`, `record`, `pruneBefore`) is the persisted record, so a restart never resends.
  The scheduler records the types only on `Sent`; after `Failed` or `Unregistered` nothing is
  recorded and the next tick in the window tries again. It is keyed per type, so a type that
  qualifies later the same day still notifies on its own, and a type already over the threshold
  before the window opens notifies at the first tick inside it. Climbing further (High → Very
  high) does not notify again. The log is pruned of earlier days on the first tick of each Swiss
  day; a failed prune is retried next tick and never costs that minute's alarms.

**Sending** is `alarm/push/PushSender` → `PushResult` `Sent` | `Unregistered` | `Failed(cause)`; it
never throws for a delivery failure.

- `FcmPushSender(client, projectId, accessToken, baseUrl)` — FCM HTTP v1, one
  `POST /v1/projects/{projectId}/messages:send` with `token`, `notification{title, body}`,
  `android.notification.channel_id` and `data`, encoded by the sender itself so the shape does not
  depend on the client's plugins. `404` with `UNREGISTERED`, or `400` with `INVALID_ARGUMENT`, is
  `Unregistered`; any other non-2xx, a timeout or a transport error is `Failed`. The access token is
  an injected `suspend () -> String`, so `FcmPushSenderTest` never touches Google.
- `pushSenderFromEnvironment()` (`PushWiring.kt`) builds it when **`FCM_CREDENTIALS`** names a
  service-account key: `google-auth-library-oauth2-http` turns the key into a token with the
  `firebase.messaging` scope (refreshed when it expires), the project id comes from the key file, and
  the `HttpClient(CIO)` has a 15-second timeout and closes on `ApplicationStopped`. A configured key
  that cannot be read fails startup rather than silently logging.
- Without `FCM_CREDENTIALS` the server starts with a warning and uses `LoggingPushSender`, which logs
  the token's last six characters and the message and reports `Sent`. **The key is never
  committed.**
- On **`Unregistered`** the scheduler calls `DeviceStore.clearToken(deviceId, token)`: the device and
  its alarms stay, but `enabledWithDeliverableDevice()` no longer returns them, so none is evaluated
  or fetched for until `PUT /devices/{id}/token` sets a new token. The clear is conditional on the
  token still being the rejected one, so a rotation that landed in the meantime is never wiped.
  Nothing is recorded in the notification log. Other alarms of the same device in the same tick still
  try (and fail) once; from the next tick on they are skipped.
- **`Failed`** is only logged; a daily report is not retried within its minute, a threshold alert is
  retried on the next tick of its window.

The server logs through `logback-classic` (`server/src/main/resources/logback.xml`, INFO); without
an SLF4J backend every log line, including the logged pushes, would be dropped.

`AlarmSchedulerTest` runs `tick()` against an in-memory database, `MeasurementService` over
`FakePollenService` (whose `failures` map fails single stations), `FakePushSender` and
`MutableClock` — a scheduler built a second time on the same database stands in for a restart.
It also pins token handling: an `Unregistered` result clears the token and the device's alarms are
skipped from the next tick, and a new token brings them back.
`AlarmRulesTest` pins timing (both minute edges, both 2026 DST changeovers, both window edges),
content, batching, the per-day exclusion and every staleness form.

#### `GET /pollen/stations/{abbr}/measurements`

```json
{ "stationAbbr": "PZH", "measuredAt": "2026-08-01T09:00:00Z", "unit": "grains/m3",
  "species": [ { "id": "BIRCH", "name": "Birch", "latinName": "Betula",
                 "concentration": 42, "severity": "MODERATE" },
               { "id": "ASH", "name": "Ash", "latinName": "Fraxinus",
                 "concentration": null, "severity": null } ] }
```

- **All seven taxa are always present**, in `PollenSpecies` declaration order. `concentration` and
  `severity` are nullable **together** and mean *no reading* — the station does not report that
  taxon. That is a different fact from `0` / `NONE`: a station that does not measure ash is not a
  station reporting no ash, and a client must be able to say so.
- **No `overallSeverity` field, and `species` is unsorted.** Both the worst severity and the display
  order are pure functions of `species` and are presentation rules; a derived copy on the wire only
  gives the two sides two answers that can disagree.
- **`measuredAt` is mandatory** — a response that can be served from a cache after an upstream
  failure has to say when its reading is from.

| Case | Status |
| --- | --- |
| Success, fresh or a retained earlier reading | `200` |
| Unknown abbr | `404` |
| Published file holds no usable row, nothing retained | `404` |
| Upstream fetch failed, nothing retained | `502` |

The pipeline behind it lives in `server/.../pollen/`: `upstream/PollenService` (where the bytes come
from), `upstream/PollenCsvParser` (decode, parse, pick the row), `measurement/TtlCache` (expiry,
per-key deduplication, stale retention) and `measurement/MeasurementService` (compose them and
classify against `PollenThresholds`).

`TtlCache<K, V>` is generic on purpose and knows nothing about pollen; its `get` returns a
`CacheResult` — `Fresh`, `Stale` (load failed, previous value kept with its original age) or
`Failed(cause)`. The lock is per key, so a slow fetch for one station never delays another, and
callers queued behind a failed load share its outcome instead of retrying one after another.
Failures are not cached. It takes a `java.time.Clock`, and `MutableClock` in `server/src/test`
drives every time-dependent test — none of them sleeps. What is cached is the *parsed* reading, so
a threshold change applies without waiting for expiry. A file with no usable row counts as a failed
load, so an earlier reading still covers it; `Failed.cause` is then a `NoUsableRowException`, which
the route maps to `404` rather than `502`.

`PollenService` has exactly one production implementation, **`MeteoSwissPollenService`** — it
fetches `BASE_URL/<abbr>/ogd-pollen_<abbr>_h_now.csv` (`hourlyNow`),
`BASE_URL/<abbr>/ogd-pollen_<abbr>_d_recent.csv` (`dailyRecent`) and
`BASE_URL/<abbr>/ogd-pollen_<abbr>_d_historical.csv` (`dailyHistorical`) over HTTP, takes its `HttpClient` and base
address as constructor parameters (the `StationApiService(client, baseUrl)` precedent, so tests
drive it with `MockEngine`), and throws on any non-2xx rather than letting an error body reach the
parser as if it were a file. The only other implementation is `FakePollenService` in
`server/src/test` — programmable bytes, a settable failure and a record of what was requested,
which is how band boundaries and the "no usable row" case get driven. The hourly file has `bytes` /
`failure` / `failures` / `requested`; each daily file has its own set (`dailyRecentBytes`,
`dailyRecentFailure`, `dailyRecentRequested`, and the same three `dailyHistorical…`), so one file
can fail while another answers, and a test can see which file a caller asked for.
`hourlyCsv(...)` and `dailyCsv(...)` build files in the published shape.

Verbatim downloads of all 15 published files live in `server/src/test/resources/fixtures/
ogd-pollen/`, laid out under the same relative paths the service serves them from. They are test
resources rather than main ones — they must not ship in the server jar — and no class wraps them:
`PollenCsvParserTest` reads them directly by `PollenStation.hourlyNowPath` (and Zürich's two daily
files by `dailyRecentPath` and `dailyHistoricalPath`) and asserts every one
still parses to a reading covering all seven taxa, and that each file holds the abbreviation of the
directory it sits in. That is what keeps the parser honest against the real column layout, and what
catches a re-download filed into the wrong station's directory.

Production wiring lives in `meteoSwissPollenService`, which `Application.module()` calls once and
hands to both the `MeasurementService` and the `HistoryService` (`configureRouting`'s defaults call
it too): it builds the `HttpClient(CIO)`, installs a 15-second request/connect timeout on it, and
closes it on `ApplicationStopped`. Tests pass their own `MeasurementService` and `HistoryService`
and never construct that client. This outbound leg is why `:server` has
`ktor-client-core` + `ktor-client-cio` on `implementation` (CIO because the server needs no
platform HTTP stack) and `ktor-client-mock` on `testImplementation`.

#### `GET /pollen/stations/{abbr}/history`

```json
{ "stationAbbr": "PZH", "range": "month", "from": "2026-09-04", "until": "2026-10-03",
  "days": [ { "date": "2026-09-04",
              "species": [ { "id": "BIRCH", "concentration": 12, "severity": "LOW" },
                           { "id": "ASH", "concentration": null, "severity": null } ] } ] }
```

- `range` is exactly `week`, `month` or `year` (`HistoryRange`, 7 / 30 / 365 days). **The window
  ends yesterday** in `SWISS_ZONE` (`historyWindow(range, today)`, pure): a day's mean exists only
  once the day is over. Today never appears.
- **Every date of the window is present**, oldest first, each with all seven taxa in `PollenSpecies`
  order. `concentration` and `severity` are `null` together and mean no value — including whole days
  the publisher **left out of the file**, which it does (Zürich's 2026 file skips 19 days, most of
  April). The client never has to line dates up itself.
- These are the `d0` daily means (`PollenSpecies.dailyCode`), classified against the same
  `PollenThresholds` — the bands' own unit, so unlike the hourly measurements this does not skew high.

| Case | Status |
| --- | --- |
| Success, fresh or retained rows | `200` |
| Missing or unknown `range` — checked **before** the station, so also for an unknown one | `400 {error}` |
| Unknown abbr (case-insensitive) | `404` |
| Upstream fetch failed, nothing retained | `502` |

Behind it is `pollen/history/HistoryService(pollenService, thresholds, clock)`: two `TtlCache`s of
the *parsed* daily rows (`PollenCsvParser.parseDaily` — d0 columns only, `null` for an empty cell or
a missing column, all-`null` rows kept, unparseable dates skipped), per station, with
classification per request:

- **`d_recent`**, year to date, read for every request and kept for `RECENT_TTL` = 3 hours.
- **`d_historical`**, every earlier year, read **only when the window starts before 1 January** of
  the current Swiss year — a year always, a week or month only in early January — and kept for
  `HISTORICAL_TTL` = 24 hours, since it changes once a year. Only rows from 1 January of last year
  on are kept (no window reaches further), so the decades of earlier data never sit in memory.
- On a date both files hold, **`d_recent` wins**.

Each file follows the stale rule on its own: on a failed reload its retained rows are served (dates
they do not reach yet come back empty) and the result is `Stale`. If a file the window needs has
failed with nothing retained, the whole result is `Failed` (`502`) — even when the other answered,
since a year with a silent hole where last year should be would read as a clean season.
`pollen/` declares its own `ErrorDto` for the `400` body, the same shape as the alarm routes'.
`HistoryServiceTest` pins the Swiss-midnight edge, gap filling, both TTLs from both sides, when
`d_historical` is and is not read (a month starting on 1 January versus on 31 December), the
overlap rule, stale serving per file and classification at a band edge; `HistoryWindowTest` pins the
windows at 1 January and across a leap day.

## Testing

| Source set                    | Deps                                                        |
| ----------------------------- | ----------------------------------------------------------- |
| `composeApp/src/commonTest`   | `kotlin("test")`, `kotlinx-coroutines-test`, `ktor-client-mock` |
| `server/src/test`             | `kotlin("test")`, `ktor-server-test-host`, `ktor-client-content-negotiation`, `ktor-client-mock` |

Store tests (`ExposedStoresTest`) run against `PollenInfoDatabase.inMemory()` or a temp file, never
the real one; `alarm/store/AlarmFixtures.kt` builds alarms and inserts them directly
(`Database.insertAlarm`) when a test needs to choose the id or the creation time. `AlarmRoutesTest` hands its own stores to
`configureRouting(database = …, devices = …, alarms = …)`.

Rules of the road:

- Put app tests in `commonTest`, never in `androidUnitTest` — they must run on every target.
- Test names are backtick sentences describing behaviour, e.g.
  ``fun `returns Failure when the upstream responds with a server error`()``.
- Test the repository through a **real `HttpClient` backed by `MockEngine`**, not a mocked service.
  `createHttpClient(engine)` accepts an engine so the production JSON/plugin config is under test
  too. This covers the serialization contract, which is where upstream changes bite.
- ViewModel tests use `Dispatchers.setMain(StandardTestDispatcher())` and `advanceUntilIdle()`, so
  the intermediate `Loading` / `isRefreshing` states are observable. Reset with
  `Dispatchers.resetMain()`.
- Shared test fixtures go in a plain file in `commonTest` (see `FakeExampleRepository.kt`) —
  hand-written fakes, no mocking framework in this project.
- Server route tests use `testApplication { application { configureSerialization();
  configureRouting(ownThresholds) } }` and pass in their own collaborators, so a test can assert
  against the exact configuration it installed (see `PollenRoutesTest`).
- Pin boundaries from both sides. `SpeciesThresholdsTest` is the model: every band bound is
  asserted at the edge and just below it, since an off-by-one there silently mislabels severity.
- **No test contacts the real MeteoSwiss service.** `MeteoSwissPollenServiceTest` drives it
  through `MockEngine`, and parser tests read checked-in CSV samples. That the published address
  actually serves those paths is a manual check against the running backend, not a test.
- When a test documents behaviour that is probably wrong, say so in a comment on the test rather
  than silently encoding it.
