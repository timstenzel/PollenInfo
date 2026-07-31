# PollenInfo

Compose Multiplatform (Android + iOS) app for checking Swiss pollen levels per measuring station,
plus a Ktor backend that owns all contact with the MeteoSwiss public API.

## Architecture

```
┌─────────────────────┐        REST         ┌──────────────┐   1h scheduled poll   ┌───────────────────┐
│ composeApp          │ ──────────────────> │  server      │ ────────────────────> │ MeteoSwiss OGD    │
│ (Android + iOS)     │ <────────────────── │  (Ktor JVM)  │                       │ pollen (public)   │
└─────────────────────┘   JSON              └──────────────┘                       └───────────────────┘
         ▲                                        │
         └──────────── push (FCM / APNS) ─────────┘
```

**The apps never call the MeteoSwiss API directly.** All upstream polling, parsing, caching and
severity classification lives in `:server`; the apps only speak to our own REST API. This keeps CSV
parsing, station metadata and threshold logic in one place and off the devices.

Push notifications are a later feature and there is no push code in the tree — when they land, the
server gains an outbound leg to FCM/APNS and the apps a subscription call.

### Modules

| Module       | Type                        | Contains                                                    |
| ------------ | --------------------------- | ----------------------------------------------------------- |
| `:composeApp`| KMP (android, ios*)         | The app: UI, ViewModels, repositories, REST client           |
| `:server`    | Kotlin/JVM (Ktor + Netty)   | REST API for the apps, station/species/threshold domain, MeteoSwiss polling |
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
| `<abbr>/ogd-pollen_<abbr>_h_now.csv`                 | **Hourly, current day** — poll this on the schedule |
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

For a real feature that talks to our own backend, mirror `feature/onboarding` instead — it is the
same layering pointed at `:server` through `apiBaseUrl`.

Cross-feature code lives in `core/` (`core/network`, `core/result`, `core/di`).

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
`SelectedStationRepository` is the only one so far: a `Flow<SelectedStation?>` that emits `null`
while nothing is stored, plus `suspend fun select(...): Result<Unit>` — a write can fail on IO, and
the caller must react rather than move on to a screen with nothing to read.

It lives in `core/` rather than in `feature/onboarding` because it already has three consumers:
onboarding writes it, the startup gate reads it, and the home screen reads it to label itself.

`DataStoreSelectedStationRepository` is **deliberately logic-free** — it reads and writes two string
keys and nothing else. It has no unit test: a real one would need a platform file path, and app
tests may not live in `androidUnitTest`. Every consumer is tested against
`FakeSelectedStationRepository` in `commonTest` instead. Keep the implementation trivial enough that
this stays an honest trade; anything worth testing belongs in a caller.

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

Android uses `LocationManagerCompat.getCurrentLocation` on `NETWORK_PROVIDER` only. **No Play
Services**, and no GPS fallback: `ACCESS_COARSE_LOCATION` does not grant `GPS_PROVIDER`, and
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
`appModules`. It provides the `DataStore<Preferences>` and the `CoarseLocationProvider`. The
DataStore factory needs a file path and an IO dispatcher, neither of which exists in `commonMain`;
the location provider is a different platform class on each side. Each actual builds the store itself —
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
purpose is already fulfilled. `feature/home` is a **placeholder** standing in for the real
dashboard: it reads the stored selection and renders its name, with no ViewModel and no network.

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
  `kotlinx-datetime` instead. (`ExampleMapper.format` is a hand-rolled multiplatform
  replacement.)
- Platform-specific pieces use `expect`/`actual` with a `.android.kt` / `.ios.kt` filename suffix,
  as in `core/network/HttpClientEngine.kt`.
- Always compile an iOS target after touching `commonMain`; the Android build alone will not catch
  non-portable code.

### Server conventions

Ktor plugin configuration is split into `plugins/` extension functions on `Application`
(`configureSerialization`, `configureLogging`, `configureRouting`) and composed in
`Application.module()`. Routes are `fun Route.xRoutes(dependency)` extension functions grouped by
feature package, taking their collaborators as parameters so tests can supply their own instances.

```
server/src/main/kotlin/.../server/
├── plugins/            configureSerialization / configureLogging / configureRouting
└── pollen/
    ├── domain/         PollenStation, PollenSpecies, PollenSeverity, PollenThresholds
    ├── model/          Wire DTOs (@Serializable)
    └── PollenRoutes.kt
```

Domain enums and threshold logic have no Ktor or serialization-transport concerns beyond
`@Serializable`; wire shapes are separate DTOs in `model/` so the public API can evolve
independently of the domain.

### REST API

| Method | Path                      | Returns                                              |
| ------ | ------------------------- | ---------------------------------------------------- |
| GET    | `/health`                 | `OK`                                                 |
| GET    | `/pollen/stations`        | All 15 stations with coordinates and altitude        |
| GET    | `/pollen/stations/{abbr}` | One station (case-insensitive abbr), 404 if unknown  |
| GET    | `/pollen/species`         | The 7 taxa with display and latin names              |
| GET    | `/pollen/thresholds`      | Per-species severity bands + unit                    |

## Testing

| Source set                    | Deps                                                        |
| ----------------------------- | ----------------------------------------------------------- |
| `composeApp/src/commonTest`   | `kotlin("test")`, `kotlinx-coroutines-test`, `ktor-client-mock` |
| `server/src/test`             | `kotlin("test")`, `ktor-server-test-host`, `ktor-client-content-negotiation` |

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
- When a test documents behaviour that is probably wrong, say so in a comment on the test rather
  than silently encoding it.
