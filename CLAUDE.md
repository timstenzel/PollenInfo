# PollenInfo

Compose Multiplatform (Android + iOS) app for checking Swiss pollen levels per measuring station,
plus a Ktor backend that owns all contact with the MeteoSwiss public API.

## Architecture

```
┌─────────────────────┐        REST         ┌──────────────┐  on-demand CSV fetch  ┌───────────────────┐
│ composeApp          │ ──────────────────> │  server      │ ────────────────────> │ MeteoSwiss OGD    │
│ (Android + iOS)     │ <────────────────── │  (Ktor JVM)  │   30-min TTL cache    │ pollen (public)   │
└─────────────────────┘   JSON              └──────────────┘                       └───────────────────┘
         ▲                                        │
         └──────────── push (FCM / APNS) ─────────┘
```

**Fetching is on demand, not scheduled.** A request for a station's measurements fetches that
station's current-day CSV only if the server has no reading for it younger than **30 minutes**
(`MeasurementService.CACHE_TTL`); nothing is polled on a timer. The upstream file gains a row
roughly hourly, so a new row is picked up within half an hour of publication, and any number of
people looking at one station cost one upstream request per period. Simultaneous first requests for
a station share a single fetch.

When a fetch fails, the station's last good reading is served with its **original** `measuredAt` —
never re-stamped — and the app's stale-age warning makes its age visible. With no earlier reading
the request fails (`502`). No maximum age is applied to a retained reading; that is presentation
policy and lives in the app's warning, so the two must stay together.

This means no scheduler lifecycle, no cold-start "the first poll failed" state, and no traffic for
the fourteen stations nobody is looking at. **A scheduled poll becomes the right shape once push
notifications need severities for stations nobody has open** — the cache is the seam it would fill.

**The apps never call the MeteoSwiss API directly.** All upstream fetching, parsing and severity
classification lives in `:server`; the apps only speak to our own REST API. This keeps CSV parsing,
station metadata and threshold logic in one place and off the devices.

Push notifications are being built (see "Alarms"). So far the Android app obtains an FCM token and
registers the device with the backend, which keeps devices and alarms in SQLite (see "Persistence");
nothing is sent yet — the outbound leg to FCM arrives with the alarm scheduler. iOS has no push leg.

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
| Run the backend on :8080        | `./gradlew :server:run`                     |
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
| `<abbr>/ogd-pollen_<abbr>_h_now.csv`                 | **Hourly, current day** — the only file we fetch |
| `<abbr>/ogd-pollen_<abbr>_h_recent.csv`              | Hourly, year to date                        |
| `<abbr>/ogd-pollen_<abbr>_d_recent.csv`              | Daily averages, year to date                |

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
"All stations".)

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
  `Stale.Earlier(localDate)`.

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
are the same layering pointed at `:server` through `apiBaseUrl`. None owns its data layer any
more: the station list and the reading pipeline they consume live in `core/` (below), so the slices
themselves are mostly `presentation/`.

- **`feature/onboarding`** — a form: one list fetched once, a user choice persisted through
  `core/preferences`, and completion delivered as a one-shot event. Its own domain code is
  `FindNearestStationUseCase`, which has no other consumer.
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

Cross-feature code lives in `core/` (`core/network`, `core/result`, `core/di`, …). A feature never
imports another feature, and nothing in `core/` imports a feature except the DI module that wires
them. Code moves to `core/` once a second feature needs it, keeping the same
`data/` / `domain/` layering inside:

| Package | Contains |
| --- | --- |
| `core/station` | `Station`, `StationRepository`(+`Impl`), `StationApiService`, `StationDto`, `StationMapper` — `GET /pollen/stations`, sorted alphabetically by name |
| `core/species` | `Species` (`id`, `name`), `SpeciesRepository`(+`Impl`), `SpeciesApiService`, `SpeciesDto`, `SpeciesMapper` — `GET /pollen/species`, in the server's order. The app's pollen-type vocabulary where there is no reading (the alarm editor), loaded rather than declared so `PollenSpecies` stays authoritative |
| `core/measurement` | `StationMeasurement`, `SpeciesReading`, `PollenSeverity`, `StationPollenOverview`, `ReadingAge` (+`readingAgeOf`, `STALE_AFTER`), `StationMeasurementRepository`(+`Impl`), `StationMeasurementApiService`, its DTO and mapper, and `GetStationMeasurementUseCase` (worst severity, `drivenBy`, display order) |
| `core/ui/severity` | The composables and wording every screen showing a reading uses, so they cannot drift apart: `SeverityBar` (+`SeverityBarSize`, `severityFillFraction`), `SeverityColors` (`PollenSeverity.color()`), `PollenSeverity.label()`, `ReadingAge.label()` + `refreshedLabel`, `ReadingAgeView` (fresh caption / stale warning), `SpeciesListHeading`, `SpeciesRow` |

Test fixtures sit next to their subjects in `commonTest`: `core/station/StationFixtures.kt` and
`FakeStationRepository.kt`, `core/measurement/MeasurementFixtures.kt` (including the gated
`FakeStationMeasurementRepository`).

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

It lives in `core/` rather than in `feature/onboarding` because it already has three consumers:
onboarding writes it, the startup gate reads it, and the home screen reads it to label itself.

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

**That cancellation is load-bearing, not just tidy.** Picking a station from the dropdown cancels any
lookup still in flight (`OnboardingViewModel.locationJob`). The shortcut proposes and the user
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
`OnboardingViewModel`, not by either actual.** One constant that cannot drift between platforms, and
one that `commonTest` can drive under virtual time with a fake that never answers.

`rememberCoarseLocationPermissionRequester` is a **`@Composable expect fun`** and is the reason
prompting is separate from looking up: Android's request needs an activity-scoped
`ActivityResultLauncher`, which a Koin-injected class holding only the application context cannot
provide. The screen prompts and hands the resulting boolean to `onPermissionResult(granted)`, so the
ViewModel touches no platform API and is fully testable against `FakeCoarseLocationProvider`. (A
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
`appModules`. It provides the `DataStore<Preferences>`, the `CoarseLocationProvider` and the
`PushTokenProvider`. The DataStore factory needs a file path and an IO dispatcher, neither of which
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

It **observes** the selection rather than taking its first value, unlike the startup gate below —
see that section for why the two differ. There is deliberately no way to change the station from
Home; the pin in its top bar is decorative, and station changes belong to the planned settings
feature.

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
`Screen.Home`, `ALL_STATIONS` → `Screen.AllStations` (see "All stations" above), `FEATURE_3` →
`Screen.Feature3`, `ALARMS` → `Screen.Alarms` (see "Alarms" below) and `FEATURE_5` →
`Screen.Feature5`. Tabs 3 and 5 are placeholders for features not yet defined. All
stations shows `SwissOutline` (`feature/allstations/map/SwissOutlineIcon.kt`), a stroke-only
`ImageVector` built from `SWISS_BORDER` through `SwissMapProjection`, so it tints like a Material
icon; Alarms shows `Icons.Default.Notifications`; the other three use `Icons.Default.LocationOn` for
now, at the product owner's request. The tabs are icon-only,
so `contentDescription` ("Home", "All stations", "Feature 3", "Alarms", "Feature 5") is the only name a screen reader has to tell them apart; it is set on the `Icon`, and the item has no
label.

Each placeholder destination renders `navigation/ComingSoonScreen(title)` — a stateless `Scaffold`
with a `TopAppBar` naming the tab (its `contentDescription`, so the title and the spoken name cannot
disagree), a pin and "Coming soon". The placeholder routes are separate `data object`s rather than
one parameterised route, so each tab keeps its own saved state.

**The bar is shown exactly when the current back-stack destination is a tab** — never on
`Onboarding` or `Example`. The rule is `TopLevelDestination.current(isOnRoute)`: it returns the
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
- **Back from any placeholder returns to Home; back from Home leaves the app**, on both launch paths.
  No `BackHandler` is involved.
- **Each tab keeps its state.** The tab being left is saved and the one entered is restored, so
  Home's `HomeViewModel` survives a round trip and its readings reappear without a reload.
- **Reselecting the current tab does nothing** — the click is skipped, not a refresh.

It pops up to `Screen.Home`, deliberately **not** `graph.findStartDestination()` as in the usual
Compose sample: after a fresh install the graph's start destination is `Onboarding`, which has
already been popped, and a `popUpTo` on a destination not in the back stack is silently ignored —
history would then pile up with every tab switch.

**Replacing a placeholder with a real feature:** rename the `Screen.FeatureN` object (and its
`TopLevelDestination` entry, with a real icon and `contentDescription`), and point its
`composable<Screen.X>` in `AppNavigation` at the feature's screen instead of `ComingSoonScreen`.
Nothing else knows about the tab. Update the order and name assertions in
`TopLevelDestinationTest`.

#### Alarms

The fourth tab, `feature/alarms`: the user's pollen alarms, delivered as push notifications. **So
far the permission gate, the backend-loaded list and creating daily reports exist** (see "Alarm
editor" below) — threshold alerts, editing, pausing and deleting are not built yet, and nothing is
sent.

**Notifications are checked first.** `AlarmsUiState` is `CheckingPermission` (renders nothing; the
permission has not been read yet) → `PermissionRequired(state)`, or, once `ENABLED`, the list:
`Loading` → `Content(alarms, isRefreshing)` | `Error(message, pushUnavailable)`.
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
`MIN_REFRESH_INDICATOR`, and `Error` offers Retry. `Error(pushUnavailable = true)` — iOS — says
"Push notifications aren't available on this device yet" with no Retry. Station names come from
`StationRepository` and pollen-type names from `SpeciesRepository`, both fetched only when there is
an alarm to describe (cached after that; the abbreviation or species id stands in if one fails), so
the iOS path makes no network call at all.

Each row is the station name over `summaryOf(alarm, speciesNames)` (`AlarmSummary.kt`), e.g. "Daily
report at 08:00 · Mon–Fri · Birch, Grasses ≥ Moderate": types in display order ("All pollen types"
when every one is selected), the minimum only when it is not "Any", and days collapsed by
`daysSummary` — "Every day", runs of three or more as a range ("Mon–Fri"), shorter runs listed
("Sat, Sun"). Severity words come from `PollenSeverity.label()`; `minimumLabel()` calls `NONE`
"Any". It sits in `presentation/`, not `domain/`, because it is wording. Threshold rows still show a
placeholder ("Threshold alert 07:00–21:00") until threshold alerts can be created.

**`onResume()` reloads quietly.** The screen calls it on every `RESUMED`, right after handing over
the permission — so back from the editor, from another tab or from another app. With a list on
screen and notifications enabled it reloads without `isRefreshing` and swaps the rows when the
answer arrives; a failed quiet reload keeps them (pull-to-refresh shows the error). Before the first
load, while any load runs, or without permission it does nothing. "Create alarm" is the empty state's
button and, once there are alarms, an extended FAB; the list has bottom padding so the FAB never
covers the last row.

**`AlarmRepositoryImpl` registers lazily.** Every call goes through one `withDevice { }` wrapper:
with no stored id it asks `PushTokenProvider` for a token (`Unavailable` → `PushUnavailableException`,
before any request), `POST /devices`, and stores the issued id. If a device call answers `404`
(`UnknownDeviceException` from `AlarmApiService` — the backend lost its database, say) it clears the
id, registers once and retries once; a second `404` is a `Failure`. Registration is behind a
`Mutex`, and a re-registration that finds a newer id already stored uses it instead of replacing it
again, so concurrent calls never register twice. `AlarmApiService` checks status codes itself rather
than letting `body()` read an error response as data.

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

`AlarmsViewModelTest` covers the mapping, the flag, the list cycle and `onResume`
(`FakeAlarmRepository.gate` holds a load in flight). `AlarmRepositoryImplTest` drives registration, the `404` retry and both
`schedule` variants through `MockEngine`, with `FakePushTokenProvider` and
`FakeDeviceRegistrationRepository`. The permission controllers and `FirebasePushTokenProvider` are
checked by hand.

##### Alarm editor

`Screen.AlarmEditor(alarmId: String? = null)` — not a tab, so the bottom bar is hidden on it. The
list's "Create alarm" navigates to it, and its `onDone` (`popBackStack`) runs on the top bar's back
arrow and on `AlarmEditorEvent.Done`, sent on a buffered `Channel` after a successful save. Every
editor is a new daily report so far; `alarmId` is reserved for editing and not read yet.

`AlarmEditorViewModel` loads the stations and the pollen types in parallel (`Error` with Retry if
either fails) and opens `Editing(form, stations, species, isSaving, saveError)` on
`AlarmFormState.newDailyReport(home, speciesIds)`: the stored home station (the first station if it
is no longer listed), every pollen type, every day, "Any", 08:00. Choosing another station never
touches the stored home station.

**`AlarmFormState` (`domain/model/`) holds every rule**, pure and unit-tested: field changes return a
new state, `isValid` needs at least one pollen type and one day, `isDirty` compares against the state
the editor opened with (so an undone change is not a change), `severityOptions` is Any … Very high,
and `toDraft()` is the `AlarmDraft` a create sends. `Editing.canSave` is `isValid && !isSaving`;
`save()` is ignored otherwise, and so are field changes while a save runs — the save sends the form
as it was when tapped. A failed save keeps the form and shows its message as `saveError` until the
next attempt. A backend `400` arrives as `InvalidAlarmException` with the server's `error` text.

The screen: a station `ExposedDropdownMenuBox` (as in onboarding), `FilterChip`s in `FlowRow`s for
pollen types, minimum severity and days (day chips announce the full day name), a time button that
opens a 24-hour `TimePicker` in an `AlertDialog` and is announced as "Report time 08:00", the hint
"Times are Swiss time.", and Save with a progress indicator. Section titles are headings.
`AlarmEditorViewModelTest` covers loading, both load errors, `isSaving` under a gated create
(`FakeAlarmRepository.createGate`), `Done` exactly once and a failed save; the composable is checked
by hand.

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
- The server's service-account key (for sending) is never committed; it arrives with sending.

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
`MeasurementService` (via `meteoSwissMeasurementService`), the database and the stores — and passes
them into `configureRouting(...)`. Routes are `fun Route.xRoutes(dependency)` extension functions grouped by
feature package, taking their collaborators as parameters so tests can supply their own instances.

```
server/src/main/kotlin/.../server/
├── plugins/            configureSerialization / configureLogging / configureRouting
├── pollen/
│   ├── domain/         PollenStation, PollenSpecies, PollenSeverity, PollenThresholds
│   ├── upstream/       PollenService, MeteoSwissPollenService, PollenCsvParser
│   ├── measurement/    MeasurementService, StationMeasurement
│   ├── model/          Wire DTOs (@Serializable)
│   └── PollenRoutes.kt
└── alarm/
    ├── domain/         Alarm, AlarmSchedule (Daily | Threshold), DeviceId, AlarmId, newDeviceId
    ├── store/          DeviceStore, AlarmStore (+ Exposed implementations), tables, PollenInfoDatabase
    ├── model/          Wire DTOs incl. the polymorphic ScheduleDto
    └── AlarmRoutes.kt
```

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
| GET    | `/pollen/species`                       | The 7 taxa with display and latin names              |
| GET    | `/pollen/thresholds`                    | Per-species severity bands + unit                    |
| POST   | `/devices`                              | `{ "fcmToken": "…" }` → `201 { "deviceId": "…" }`; `400 {error}` if missing or blank |
| GET    | `/devices/{deviceId}/alarms`            | `200 [Alarm]` in creation order; `404` unknown device |
| POST   | `/devices/{deviceId}/alarms`            | Alarm without `id` → `201 Alarm`; `400 {error}` invalid or malformed; `404` unknown device |

#### Devices and alarms

There are no accounts. An install registers once, anonymously, and gets a `deviceId` — 128 bits from
`SecureRandom`, base64url without padding, always 22 characters. **The id is the secret**: whoever
holds it can read that device's alarms; alarms hold no personal data, so this is accepted for now.
Every path under `/devices/{deviceId}` answers `404` for an unknown id, which is how the app learns
to register again. An unknown device's list is a `404`, never `200 []` — `AlarmStore.list` returns
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
form), and a time that is exactly `HH:mm` (parsed `STRICT`, since the default resolver reads
`24:00` as midnight). Threshold schedules are refused for now. Validation runs before the device is
looked up, so an invalid body for an unknown device is a `400`. `AlarmStore.create` returns
`CreateResult.Created(alarm)` | `UnknownDevice` (→ `404`); the id is a random UUID, and `created_at` is
kept strictly increasing per device so two alarms created in the same millisecond still list in
creation order.

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
- Tables: `devices(id PK, fcm_token NULL, created_at)` and `alarms(id PK, device_id → devices,
  enabled, station_abbr, species, min_severity, days, type, at_time, from_time, until_time,
  created_at)`. Sets are comma-separated enum names, times `HH:mm`, so the file reads well in
  `sqlite3`.
- Store interfaces are `suspend`; the Exposed implementations run each transaction on
  `Dispatchers.IO`. `configureRouting`'s store defaults share one private in-memory database, never
  the production file, so `PollenRoutesTest` and `RoutingTest` need no database setup.

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
fetches `BASE_URL/<abbr>/ogd-pollen_<abbr>_h_now.csv` over HTTP, takes its `HttpClient` and base
address as constructor parameters (the `StationApiService(client, baseUrl)` precedent, so tests
drive it with `MockEngine`), and throws on any non-2xx rather than letting an error body reach the
parser as if it were a file. The only other implementation is `FakePollenService` in
`server/src/test` — programmable bytes, a settable failure and a record of what was requested,
which is how band boundaries and the "no usable row" case get driven.

Verbatim downloads of all 15 published files live in `server/src/test/resources/fixtures/
ogd-pollen/`, laid out under the same relative paths the service serves them from. They are test
resources rather than main ones — they must not ship in the server jar — and no class wraps them:
`PollenCsvParserTest` reads them directly by `PollenStation.hourlyNowPath` and asserts every one
still parses to a reading covering all seven taxa, and that each file holds the abbreviation of the
directory it sits in. That is what keeps the parser honest against the real column layout, and what
catches a re-download filed into the wrong station's directory.

Production wiring lives in `meteoSwissMeasurementService`, which `Application.module()` calls (and
which is also `configureRouting`'s default): it builds the `HttpClient(CIO)`, installs a 15-second
request/connect timeout on it, and closes it on `ApplicationStopped`. Tests pass their
own `MeasurementService` and never construct that client. This outbound leg is why `:server` has
`ktor-client-core` + `ktor-client-cio` on `implementation` (CIO because the server needs no
platform HTTP stack) and `ktor-client-mock` on `testImplementation`.

## Testing

| Source set                    | Deps                                                        |
| ----------------------------- | ----------------------------------------------------------- |
| `composeApp/src/commonTest`   | `kotlin("test")`, `kotlinx-coroutines-test`, `ktor-client-mock` |
| `server/src/test`             | `kotlin("test")`, `ktor-server-test-host`, `ktor-client-content-negotiation`, `ktor-client-mock` |

Store tests (`ExposedStoresTest`) run against `PollenInfoDatabase.inMemory()` or a temp file, never
the real one; `alarm/store/AlarmFixtures.kt` builds alarms and inserts them directly
(`Database.insertAlarm`) when a test needs to choose the id or the creation time — or a threshold
alert, which the API does not accept yet. `AlarmRoutesTest` hands its own stores to
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
