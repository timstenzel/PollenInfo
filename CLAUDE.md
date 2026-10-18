# PollenInfo

Compose Multiplatform (Android + iOS) app for Swiss pollen levels per station, plus a Ktor backend
that owns all contact with MeteoSwiss. Features, architecture overview, tech stack, getting started,
CI and deployment overview: **`README.md`**. Operations runbook: **`deploy/README.md`**. This file
holds the rules for implementing features and the technical facts the code alone does not explain.

## Core rules

- **The apps never call MeteoSwiss and never classify a concentration.** Fetching, CSV parsing,
  station metadata and severity thresholds live in `:server`; apps render the severity it computes
  (and may read `GET /pollen/thresholds` to label UI).
- **On-demand fetching, 30-minute cache** (`MeasurementService.CACHE_TTL`) per station; simultaneous
  first requests share one fetch. No cold-start poll. The only timer is the alarm scheduler, which
  reads through the **same** `MeasurementService`.
- **Stale on failure:** a failed fetch serves the last good reading with its **original**
  `measuredAt` (never re-stamped); with none, `502`. No max age on the server — the app's stale
  warning (`STALE_AFTER`, 3 h) is the counterpart. **Keep the two together**; the server's
  `READING_STALE_AFTER` must equal the app's `STALE_AFTER`.
- **History cache** (`HistoryService`): `d_recent` 3 h (`RECENT_TTL`), `d_historical` 24 h
  (`HISTORICAL_TTL`), same stale rule; the scheduler does not use it.
- **The diary lives in the account on the backend**; the phone keeps no copy and no offline queue.
  Readings, All stations and alarms need no account; alarms belong to the anonymous install.
- **Push sends what happened, not text**: data-only FCM messages, worded on the device. iOS has no
  push leg.

### Modules

`:androidApp` (APK: `MainActivity`, `PollenInfoApplication`, manifest, icons, `app_name`, locale
config, debug manifest, `BuildConfig.API_BASE_URL`, `google-services.json`, R8 rules) ·
`:composeApp` (KMP library: all app code and all app tests) · `:theme` (KMP library: Material 3
theme and palettes) · `:server` (Ktor/Netty, PostgreSQL). Package root:
`ch.stenzel.tim.polleninfo`. iOS targets: `iosArm64`, `iosSimulatorArm64` (no `iosX64` in CMP 1.12).

AGP 9 rejects the application plugin in a KMP module, hence the separate `:androidApp` (AGP's
built-in Kotlin). Both libraries use `com.android.kotlin.multiplatform.library` with Android
configured in `kotlin { android { … } }`. `:composeApp`'s namespace is
`ch.stenzel.tim.polleninfo.shared` (its `R` is `…shared.R`); code there cannot name `MainActivity` —
the push service opens the app via the package's launch intent.

## Commands

`JAVA_HOME`, `java`, `adb`, `emulator` come from `~/.zshrc` — no export prefix needed (if a Gradle
call reports no JDK, export `JAVA_HOME` for that one call).

| Task | Command |
| --- | --- |
| App unit tests (JVM) | `./gradlew :composeApp:testAndroidHostTest` |
| Server tests (needs Docker) | `./gradlew :server:test` |
| Both | `./gradlew :composeApp:testAndroidHostTest :server:test` |
| iOS sources compile | `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` |
| Translations | `./gradlew :composeApp:checkTranslations` (part of `check`) |
| Android Lint (all Android code) | `./gradlew :androidApp:lintRelease` (fails on any warning) |
| Dev database up / reset | `docker compose -f deploy/compose.dev.yaml up -d` / `… down -v` |
| Backend on :8080 | `./gradlew :server:run` (push logged unless `FCM_CREDENTIALS`) |
| Android debug / release | `./gradlew :androidApp:assembleDebug` / `:androidApp:assembleRelease` |
| Release without network | `./gradlew :androidApp:assembleRelease -Ppolleninfo.uploadCrashlyticsMapping=false` |
| Server image | `./gradlew :server:installDist` then `docker build -t polleninfo-server:local server` |

- `commonTest` is the single source of app tests; `testAndroidHostTest` runs it on the JVM.
  `iosSimulatorArm64Test` needs full Xcode (only CLT here) — compile the iOS test sources instead.
  Prefer targeted tasks over `./gradlew check`.
- Configuration cache is on; after odd build-script behaviour add `--no-configuration-cache`.
- Without Docker, non-database server tests pass when run alone (`--tests '*PollenRoutesTest'`);
  database tests fail with "The database tests need Docker". Inspect the dev DB:
  `docker compose -f deploy/compose.dev.yaml exec postgres psql -U postgres -d polleninfo`.
- **Kotlin compiler warnings fail the build** (`allWarningsAsErrors` in `kotlin { compilerOptions }`
  of `:composeApp` — all targets, iOS included — `:theme`, `:androidApp` and `:server`; locally and
  in CI). Fix the warning; never suppress one without asking the maintainer. K2 does not report
  unused variables (an IDE-only check), so those don't fail it. No new Gradle deprecation either:
  the only accepted ones come from plugins — AGP 9.4's `Configuration.setVisible` and KGP 2.4's
  `KotlinNativeBundleArtifactsTypes`.
- A release build uploads its R8 mapping to Crashlytics (`uploadCrashlyticsMappingFileRelease`,
  needs network). Gradle property `polleninfo.uploadCrashlyticsMapping` (default `true`) switches it
  off for offline/quick builds — their crashes stay obfuscated. Never set it in the project's
  `gradle.properties`.
- **Lint:** `:androidApp:lintRelease` checks `:androidApp` and, through `checkDependencies`, the
  Android code of `:composeApp` and `:theme` (both apply `com.android.lint`; without it the KMP
  library plugin has no `androidMain` lint and the module is silently skipped). Warnings are errors;
  only `NewerVersionAvailable` and `GradleDependency` are disabled. **No baseline** — fix a finding,
  or suppress it at the spot with a comment saying why (`@SuppressLint`, `@Suppress` in common code,
  `tools:ignore`).
- `gradle-wrapper.jar` is committed (CI validates it); regenerate only with `./gradlew wrapper`.
- CI: every pull request into `develop`/`main` and every push to `main` runs the gates of the
  components it changed, decided by `.github/scripts/changed-components.sh` (the one path →
  component mapping; docs-only changes run nothing, shared build files run everything). Server gate:
  `.github/workflows/server.yml` (`Test, build and scan`: server tests, image, Trivy; publishes on
  `main`). Android gate: `.github/workflows/android.yml` (`android-gate`: app tests,
  `checkTranslations`, `:androidApp:lintRelease`, debug and unsigned release build, the two
  debug-only merged-manifest `grep`s of this file). iOS compile gate: `ios-compile` in the same
  workflow (macOS, `:composeApp:compileTestKotlinIosSimulatorArm64`, when shared app code or shared
  build files change). Shared setup in `.github/actions/setup-jvm`; every action pinned by commit SHA.
  Rules in `deploy/README.md`.

## Git workflow

`develop` is the integration branch, `main` holds releases. Every feature is a pull request into
`develop` (squash merge, one commit `feature: <name>`); a release is a pull request `develop` →
`main` (merge commit). The maintainer opens, reviews and merges every pull request. Repository
setup (rulesets, merge methods): `deploy/README.md`, "Repository setup".

- **Branch:** at a feature's first task, `git fetch` and create `feature/<name>` from
  `origin/develop`, where `<name>` is the feature's `.sdd/<name>/` folder. **Stop and ask** if the
  working tree is not clean or `feature/<name>` already exists (locally or on `origin`).
- **First commit:** the specification, `.sdd/<name>/` (`requirements.md`, `tasks.md`).
- **One commit per finished task**, message `<name>: <task title>`, holding the task's code and its
  ticked steps in `tasks.md`; never commit half-done work. Push the feature branch right after each
  task commit.
- **Never** push to `develop` or `main`, **never** force-push (the only exception: the own feature
  branch after a rebase the maintainer asked for), **never** merge, and **never** open, edit or merge
  a pull request (no `gh pr …`).
- **Handover after the last task:** report the compare link
  `https://github.com/timstenzel/PollenInfo/compare/develop...feature/<name>?expand=1`, the title
  `feature: <name>` and a ready-to-paste Markdown description with the sections **Summary**,
  **Changes** (per task), **Quality gates** (what ran locally and the results) and **For the
  reviewer** (manual checks, settings to apply by hand). Only in the message — no file, and no
  attribution line in the description.
- **Releases** (`release: <versionName>`, `develop` → `main`) are opened and merged by the
  maintainer, never by the assistant.
- Dependabot pull requests target `develop` (`.github/dependabot.yml`).

## Data source: MeteoSwiss OGD pollen

The **only** upstream. Base `https://data.geo.admin.ch/ch.meteoschweiz.ogd-pollen/`, public, no key.
CSV, `;`-separated, **ISO-8859-1** (transcode on read). Timestamps `dd.MM.yyyy HH:mm` **UTC**.
Docs: <https://opendatadocs.meteoswiss.ch/a-data-groundbased/a7-pollen-stations>.

| File (`<abbr>` lowercase, e.g. `pzh/…`) | Use |
| --- | --- |
| `ogd-pollen_meta_stations.csv` / `_meta_parameters.csv` / `_meta_datainventory.csv` | Metadata |
| `<abbr>/ogd-pollen_<abbr>_h_now.csv` | Hourly, current day — measurements and alarms |
| `<abbr>/ogd-pollen_<abbr>_h_recent.csv` | Hourly, year to date (unused) |
| `<abbr>/ogd-pollen_<abbr>_d_recent.csv` | Daily means, year to date up to yesterday — history |
| `<abbr>/ogd-pollen_<abbr>_d_historical.csv` | Daily means, all earlier years (~560 KB) — history across 1 Jan |

15 stations: PBE PBS PBU PCF PDS PGE PLO PLS PLU PLZ PMU PNE PPY PSN PZH. Hourly codes (grains/m³):
`kaalnuh0` alder, `kabetuh0` birch, `kacoryh0` hazel, `kafaguh0` beech, `kafraxh0` ash,
`kaquerh0` oak, `khpoach0` grasses. Daily: `d0` suffix (06–06 UTC mean) — we use it, not `d1`.

**These 7 taxa are the whole set** (no mugwort/olive/ragweed). `PollenStation` / `PollenSpecies` in
`server/.../pollen/domain/` are authoritative, with codes and coordinates baked in — use them, never
re-derive. `PollenType` in `feature/example` is fake.

## Severity thresholds

Per species, in `server/.../pollen/domain/PollenThresholds.kt` (`DEFAULTS` is the single place to
change). `SpeciesThresholds` holds inclusive lower bounds: `0` NONE, `1..` LOW, `moderate..`,
`high..`, `veryHigh..`. Construction fails if a species is missing or bounds aren't strictly
increasing. Seeded (Gehrig et al. 2018): trees 15 / 90 / 1500, grasses 5 / 20 / 200.
`PollenSeverity` is ordered; use `severity.atLeast(minimum)`.

**Accepted limitation:** the bands are daily-mean bands applied to hourly readings in
`/measurements`, so severities skew high. Chosen deliberately (daily means only exist for yesterday;
own hourly bands would be guesswork). History uses daily means, so it doesn't skew.

### Colours and bars

- Palettes outside Material live in `:theme`: `SeverityPalette.kt`, `SpeciesPalette.kt` (Diary
  lines), `MapPalette.kt` (lakes). Each colour has an **explicit light and dark value** with its
  WCAG contrast against `surfaceLight`/`surfaceDark` recorded beside it (severity ≥ 3:1, species ≥
  3.5:1). Species hues stay in cyan→magenta so no line reads as a severity; **re-run a palette
  validator on any change**.
- `:theme` never sees `PollenSeverity`; the mapping is `core/ui/severity/SeverityColors.kt`, and it
  picks light/dark by **luminance of `colorScheme.surface`**, not `isSystemInDarkTheme()` (same rule
  for species and lake colours).
- `SeverityBar` fills to **fixed stops** (NONE 4 %, then 25/50/75/100 %; 0 % muted track = no
  reading), one solid colour, never interpolated. Colour never carries meaning alone — every bar sits
  beside its word. Map dots use the same `PollenSeverity.color()` — no separate dot colour function.

### Reading age

`readingAgeOf(measuredAt, now)` (`core/measurement/domain/model/ReadingAge.kt`, pure) at render time:
`< STALE_AFTER` (3 h) → `Fresh(localTime)` caption; otherwise `Stale.Today(localTime)` or
`Stale.Earlier(localDate)` in `errorContainer` colours. Device time zone. "Refreshed 10:42"
(`refreshedLabel`) is `Content.refreshedAt` — when the app last *received* a reading, from an
injected `kotlin.time.Clock` — a different fact from `measuredAt`, never a warning.

**Time types:** `Instant` / `Clock` are `kotlin.time`. kotlinx-datetime supplies `LocalDate`,
`TimeZone`, formatters. Never import `kotlinx.datetime.Instant`/`Clock` (deprecated, `-compat`
artifact); the catalog pins kotlinx-datetime **strictly** because Ktor's openapi module asks for
the compat one (see `libs.versions.toml`).

## App conventions (`:composeApp`)

### Feature package layout

```
feature/<name>/
├── data/          remote/ (<Name>ApiService + dto/, DTOs never leave data/), mapper/ (XDto.toDomain()), repository/
├── domain/        model/ (no serialization annotations), repository/ (interface), usecase/ (suspend operator fun invoke)
└── presentation/  <Name>Screen.kt, <Name>ViewModel.kt, <Name>UiState.kt
```

**`feature/example` is a reference implementation, not a product feature** — the full layering with
tests at each level, against a fictional `api.example.com` (its screen always lands in `Error`; its
tests pass through `MockEngine`). Mirror it, don't extend it. Its `Error` is the one place an
exception's own text is shown, and its strings are English-only `example_*`.

Real slices to model on: `onboarding` (form, one-shot completion event), `home` (observed selection,
full Loading/Content(isRefreshing)/Error cycle; `HomeViewModelTest` + `FakeStationMeasurementRepository.gate`
for intermediate states), `allstations` (fan-out use case emitting a growing list;
`PerStationMeasurementRepository` fake), `diary` (pure chart geometry + canvas), `alarms` (complete
slice with writes; pure `AlarmFormState`, repository recovering from a lost device), `settings`
(no Loading/Error, text all from resources), `account` (pure `AccountFormState`).

**A feature never imports another feature**; nothing in `core/` imports a feature except the DI
module. Code moves to `core/<area>` (same `data/`/`domain/` layering) once a second feature needs it.

| `core/` package | Contains |
| --- | --- |
| `station`, `species` | `GET /pollen/stations` (sorted by name), `GET /pollen/species` (server order) |
| `measurement` | Reading models, `ReadingAge`, repository, `GetStationMeasurementUseCase` (worst severity, `drivenBy`, display order) |
| `history` | `HistoryRange`, `StationHistory`/`HistoryDay`, `GET …/history` |
| `diary` | `Feeling` (with `level`), `DiaryRepository` (`record`, `today`, `entries`), `swissToday(clock)`, `FeelingPromptPreferences` |
| `session` | `Session`, `SessionRepository` (register/login/logout/deleteAccount/clear), `AccountApiService`, `SessionStorage`, `toSessionAppError()` |
| `push` | `PushTokenProvider`, `PushTokenUpdater`, `parseAlarmPayload`, `notificationText`; Android channels + messaging service in `androidMain` |
| `preferences` | `SelectedStationRepository`, `NotificationPermissionPreferences`, `CrashReportingPreferences`, `DeviceRegistrationRepository` |
| `crashreporting` | `CrashReporter` (platform-bound: `FirebaseCrashReporter` / iOS `NoCrashReporter`), `CrashReportingSync` |
| `stationpicker` | `StationPicker` state holder + `StationPickerContent`, `FindNearestStationUseCase` |
| `location`, `notifications`, `language`, `credentials`, `appinfo` | Platform-bound interfaces (see below) |
| `result`, `network`, `di`, `startup` | `Result`/`AppError`; `API_BASE_URL`, `createHttpClient`, `checkSuccess()`; Koin; startup gate |
| `ui/severity`, `ui/species`, `ui/feeling`, `ui/format`, `ui/error`, `ui/password` | Shared composables and wording: `SeverityBar`, `SpeciesRow`, `ReadingAgeView`, `speciesColor`/`speciesName`, `Feeling.label()`, `DateWording`, `ResourceText`, `AppError.message()`, `PasswordField` |

Fakes sit next to their subjects in `commonTest` (`FakeStationRepository`, `MeasurementFixtures.kt`,
`HistoryFixtures.kt`, `FakeDiaryRepository` (enforces first-answer-is-final, has gates),
`FakeSessionRepository`, `FakeCredentialStore`, `FakeAppInfo`, `FakeLanguageRepository`, …). Reuse
them before writing new ones.

### Talking to our own backend

The base URL (no trailing slash) comes from the **platform entry point** via
`appModules(apiBaseUrl)`, provided under Koin qualifier `API_BASE_URL`. No constant in `:composeApp`.
**API services take the base URL as a constructor parameter** (`single { StationApiService(get(),
get(API_BASE_URL)) }`) so tests point them at a `MockEngine` host.

| Build | Address |
| --- | --- |
| Android release | `https://api.polleninfo.ch` always |
| Android debug | `http://10.0.2.2:8080`, or Gradle property `polleninfo.apiBaseUrl` (set in `~/.gradle/gradle.properties`, never the project's) |
| iOS | `http://localhost:8080` in `MainViewController` |

- Cleartext (`usesCleartextTraffic`) and `ACCESS_LOCAL_NETWORK` are declared **only** in
  `androidApp/src/debug/AndroidManifest.xml`. Verify with `grep -l 'usesCleartextTraffic\|ACCESS_LOCAL_NETWORK'
  androidApp/build/intermediates/merged_manifest/*/process*MainManifest/AndroidManifest.xml` — only debug matches.
- API 37 emulators: after every fresh install run
  `adb shell pm grant ch.stenzel.tim.polleninfo.debug android.permission.ACCESS_LOCAL_NETWORK`,
  otherwise every request times out while `/health` answers from the host.

### iOS wrapper configuration

There is no `iosApp` Xcode project yet. Whoever creates it must add:

| `Info.plist` key | Value | Needed by |
| --- | --- | --- |
| `NSAppTransportSecurity.NSAllowsLocalNetworking` | `true` (debug only) | Cleartext dev backend |
| `NSLocationWhenInUseUsageDescription` | A sentence | **Mandatory** — without it `CLLocationManager` silently never prompts |
| `NSLocationDefaultAccuracyReduced` | `true` | Coarse location only |
| `CFBundleLocalizations` | `en`, `de`, `fr`, `it` | `IosLanguageRepository` |
| `CFBundleDevelopmentRegion` | `en` | English fallback |
| `CFBundleShortVersionString` / `CFBundleVersion` | e.g. `1.0.0` / `1` (whole number) | `IosAppInfo` (version line hidden otherwise) |

- Notifications need no key. A release build should pass `https://api.polleninfo.ch` to
  `appModules(…)` in `MainViewController` (e.g. from a build setting).
- **Password autofill:** Associated Domains entitlement `webcredentials:api.polleninfo.ch`
  (`?mode=developer` while testing) and `https://api.polleninfo.ch/.well-known/apple-app-site-association`
  serving `{"webcredentials":{"apps":["<TEAM_ID>.<bundle id>"]}}` as `application/json` — a Caddy
  route in `deploy/caddy/Caddyfile`. `CredentialStore` is a no-op on iOS (`NoCredentialStore`).

### Error handling

- **No exception escapes the data layer.** Repositories wrap calls in `safeCall { }` and return
  `core/result/Result`. ViewModels turn it into UI state, never rethrow.
- `Result` shadows `kotlin.Result`: **always import `ch.stenzel.tim.polleninfo.core.result.Result`
  explicitly**; use `safeCall`, never `runCatching`.
- **Every API service calls `HttpResponse.checkSuccess()` before `body()`** (throws
  `HttpStatusException`). Services map their own statuses to typed exceptions first
  (`AlarmApiService`: 400/401/409/404-on-single-alarm; `AccountApiService`: 409 register, 401 login →
  `InvalidCredentials`, 401 with a session → `SignedOut`, 429 on register/login/delete →
  `TooManyAttempts`).
- **UI states carry an `AppError`, never a string.** `Throwable.toAppError()`: any
  `kotlinx.io.IOException` → `Network`, 5xx → `ServerUnavailable`, 404 → `NotFound`, else `Unknown`.
  Feature-specific mappers (`toAlarmAppError()`, `toSessionAppError()`) handle their exceptions and
  delegate the rest; feature exceptions stay in the feature.
- **Screens word errors** with `core/ui/error/AppErrorText.kt`: `AppError.message(context)` /
  `loadMessage(context)`; the pure `AppError.text(context)` → `ErrorText(action, reason)` is tested in
  `AppErrorTextTest`. `ErrorContext` is `LOAD` or an action (`SAVE_ALARM`, `REGISTER`, `LOGIN`,
  `SAVE_FEELING`, `DELETE_ACCOUNT`, …) that prefixes "Couldn't …". No exception text, host or status
  ever reaches a screen.

### UI state and events

- One sealed interface per screen, `Loading` / `Content(…, isRefreshing)` / `Error(AppError)`;
  a single `StateFlow`, collected with `collectAsStateWithLifecycle()`. Pull-to-refresh keeps data
  on screen and holds the indicator at least `MIN_REFRESH_INDICATOR`.
- **One-shot actions (navigation, snackbars) are events**, not booleans on state: a
  `Channel<XEvent>(Channel.BUFFERED)` exposed via `receiveAsFlow()`, collected in a `LaunchedEffect`
  calling a lambda from `AppNavigation`. A snackbar that concerns only the screen itself (Alarms'
  `ToggleFailed`, Settings' `CrashReportsSaveFailed`) is shown by the screen's own `SnackbarHost`,
  without a lambda. Anything the screen keeps showing (e.g. `saveError`) is state.
- Form rules belong in a pure, unit-tested state class (`AlarmFormState`, `AccountFormState`);
  field changes are ignored while saving, and a save sends the form as it was when tapped.

### Persistence on the device

`core/preferences` and friends. All DataStore implementations are **deliberately logic-free and
untested** (a real test needs a platform path, and app tests may not live in `androidUnitTest`);
every consumer is tested against a fake. Keep them trivial; logic belongs in a caller.

- `SelectedStationRepository`: `Flow<SelectedStation?>` (`null` = nothing stored) and
  `select(...): Result<Unit>` — a write can fail and the caller must react.
- `NotificationPermissionPreferences.askedBefore` (Android needs it to tell "never asked" from
  "permanently denied").
- **Secrets in their own DataStore files, excluded from backup and device transfer:** the device
  token (`polleninfo_device`, `DEVICE_DATA_STORE`) and the login (`polleninfo_session`,
  `SESSION_DATA_STORE`), both provided by `platformModule`. Exclusion rules:
  `androidApp/src/main/res/xml/data_extraction_rules.xml` and `backup_rules.xml` — they name exactly
  these two files.
- `CrashReportingPreferences.enabled` (key `crash_reports_enabled`, `true` when unset) is an ordinary
  preference in the default DataStore — not a secret, kept in backups.
- `FeelingPromptPreferences` (Home prompt dismissal) stays on the device under the legacy key
  `diary_prompt_dismissed_on`; the orphaned `diary_entries` key is intentionally unread.
- **The app language is not stored here** — it belongs to the platform (see Localization).
- **A day is the Swiss calendar day:** `swissToday(clock)` (`Europe/Zurich`) is the app's only
  definition of "today"; the server uses `SWISS_ZONE`.

### Platform-bound pieces and DI

- Koin in `core/di/AppModule.kt`: `networkModule`, `dataModule`, `domainModule`,
  `presentationModule`, aggregated in `appModules`. ViewModels via `viewModel { }` /
  `koinViewModel()`; use cases `factory`, everything else `single`. Route args reach ViewModels via
  `toRoute` + `parametersOf`.
- Platform-built bindings go in `core/di/PlatformModule.kt` (`expect val platformModule`, first in
  `appModules`): the DataStores, `CoarseLocationProvider`, `PushTokenProvider`, `AppInfo`,
  `LanguageRepository`, `CredentialStore`, `CrashReporter`. These are plain platform classes, not `expect`/`actual`
  (only the implementation differs). iOS uses `Dispatchers.Default` (`Dispatchers.IO` is internal on
  Native).
- Things that need an activity-scoped launcher are **`@Composable expect fun`**:
  `rememberCoarseLocationPermissionRequester`, `rememberNotificationPermissionController()`. The
  screen prompts and hands the result to the ViewModel, which never touches a platform API.
- Platform callback APIs are bridged with `suspendCancellableCoroutine` so cancellation reaches the
  platform.
- Platform implementations (permission controllers, `FirebasePushTokenProvider`, messaging service,
  channels, `AndroidCredentialStore`) are checked by hand; iOS ones are compile-verified only.

### Location

- `CoarseLocationResult` has exactly `Success(lat, lon)`, `PermissionDenied`, `Unavailable` —
  every platform failure folds into `Unavailable`.
- Android: `LocationManagerCompat.getCurrentLocation` on `NETWORK_PROVIDER` only. **No Play
  Services, no GPS fallback, no `getLastKnownLocation`** (a stale fix picks the wrong station).
  Manifest has `ACCESS_COARSE_LOCATION` only. Coordinates never leave the device: nearest station
  is `FindNearestStationUseCase`, and no API service takes a coordinate.
- `LOCATION_TIMEOUT` (10 s) lives in `commonMain` and is applied by `StationPicker`, not the actuals.
- `StationPicker` (shared by onboarding and change-station) is a plain state holder owned by each
  ViewModel; it never persists. **Picking from the dropdown cancels an in-flight lookup** — a late fix
  must not re-pick for the user — and clears location errors. `StationPickerTest` pins it.

### Navigation

- Type-safe Compose Navigation: `@Serializable` destinations in `Screen`, registered in
  `AppNavigation`. `Screen.Example` stays registered but unreachable.
- **Startup gate:** `App()` composes nothing until `StartupViewModel` reads the **first** value of
  the selection (`NeedsOnboarding` | `Ready`); `Loading` is an empty `Surface`, not a destination.
  First value only — following the flow would rebuild the graph when onboarding saves. Onboarding
  completes with `popUpTo<Screen.Onboarding> { inclusive = true }`.
- **Tabs** are `navigation/TopLevelDestination` (enum in display order: HOME, ALL_STATIONS, DIARY,
  ALARMS, SETTINGS). Icon-only, so the `nav_*` `contentDescription` is the only name. The bar shows
  exactly when `TopLevelDestination.current(isOnRoute)` is non-null; non-tab screens (`Onboarding`,
  `AlarmEditor`, `ChangeStation`, `Account`, `Example`) hide it.
- **`navigateToTab`**: `popUpTo<Screen.Home> { saveState = true }`, `launchSingleTop`,
  `restoreState`. Pop to `Screen.Home`, **not** `findStartDestination()` — after a fresh install the
  start destination (Onboarding) is already popped and the `popUpTo` would be silently ignored.
  Reselecting the current tab does nothing. Each tab keeps its ViewModel.
- **Insets:** the outer `Scaffold` has `contentWindowInsets = WindowInsets(0)`; each screen's own
  `Scaffold` handles the top; the `NavHost` gets `.padding(innerPadding).consumeWindowInsets(innerPadding)`.
- **Adding a tab:** a `Screen` object, a `TopLevelDestination` entry (icon + `nav_*` string in all
  four languages), its `composable<Screen.X>`, and update `TopLevelDestinationTest`.

### Screen behaviour worth knowing

Details live in the ViewModels and their tests; these are the decisions that are easy to break.

- **Home** observes the stored station (station changes only via Settings; the pin icon is
  decorative). The feeling prompt shows once per Swiss day: logged out, a tap sends
  `HomeEvent.OpenAccount` and records nothing; logged in, it waits for `today()` (re-asked on every
  load, refresh and session change) and hides on success or a `SignedOutException`. Dismissal is
  per phone. List bottom padding grows by the card's measured height.
- **All stations** fans out one `/measurements` request per station in parallel (no batch
  endpoint); a failure is `Unavailable` for that row only. Full-screen `Error` only if the station
  list fails or **every** station is `Unavailable`. A refresh replaces all rows at once with the
  round's final result. Stale rows show a warning icon (slot reserved on every row). Selection is
  `Content.selectedAbbr`; the scroll to it is an event (`ScrollToStation`). Never touches the
  stored station.
- **Map** (`feature/allstations/map/`, pure Kotlin, tested): border `SWISS_BORDER` (Natural Earth
  1:10m, Douglas–Peucker 0.01°), lakes `SWISS_LAKES` (0.003°; Bodensee and Lago Maggiore omitted on
  request; landmarks only), equirectangular projection scaled by cos 46.8° — border, lakes, dots
  and the tab icon all go through the same `project`. Tap = **nearest** dot within `TAP_RADIUS`
  24 dp (`nearestStation`, ties → alphabetically first). One `clearAndSetSemantics` node, no focus
  stop per dot.
- **Diary** is gated on a session (`SignedOut` fetches nothing). Each load fetches the history and
  `entries(from, until)` **in parallel**, window computed on the phone with
  `historyWindow(range, swissToday(clock))`; either failing is `Error` (a chart without the feeling
  line would read as "no answers"). Station, range and checked types are ViewModel fields, never
  persisted; the station dropdown never writes the stored station. Feelings share the y-axis via
  `Feeling.level` (higher is worse). The chart never bridges a missing day; today is never plotted.
  `DiaryChartGeometry` and `diaryChartDescription` are pure.
- **Alarms**: permission gate first; the list loads only on the first `ENABLED` (no backend
  contact or registration before). `onResume()` reloads quietly. The switch is optimistic (full `PUT`,
  flip back + `ToggleFailed` event on failure). Limit `MAX_ALARMS` = 10 (backend's limit).
- **`AlarmRepositoryImpl` registers lazily** via one `withDevice { }`: no token → push address →
  `POST /devices`. A `401` clears the token, registers once and retries once, behind a `Mutex`
  (a newer stored token is reused). A `404` on a single alarm is `AlarmNotFoundException` — no
  re-registration. `updateToken` is not a `withDevice` call: without a stored token it does nothing.
- **Alarm editor**: leaving goes through `onBack()` (discard dialog when `isDirty`), using
  `NavigationBackHandler` (`navigationevent-compose`). An existing alarm's type is locked. Windows
  across midnight are out of scope.
- **Account**: after a successful login/registration, offer the credentials to `CredentialStore.save`
  (except a login made with the credential the manager just handed over); `Done` follows whatever
  happens. In `LOGIN` mode, `offer()` first. `logout()` clears locally **whatever the server
  answers**. A `401` on an authenticated call → `sessions.clear()` (only if the refused session is
  still stored). `AndroidCredentialStore` follows the current `ComponentActivity` via
  `ActivityLifecycleCallbacks` (weakly) and logs exceptions by type only, never with credentials.
- **Settings** observes the station, the session and the crash-report choice; the station name falls
  back to the stored one, never an error. The Privacy section (crash-report switch, whole row
  `toggleable` with `Role.Switch`) shows only when `CrashReporter.isAvailable`; the switch only writes
  `CrashReportingPreferences`, never the reporter; a failed write puts the switch back and sends
  `SettingsEvent.CrashReportsSaveFailed` (snackbar). The MeteoSwiss attribution (`settings_data_source_attribution`) is prescribed
  per language — **do not rephrase** (French keeps "Source:" without the space). Links go through
  `LocalUriHandler`.

### Firebase and push (Android)

- FCM is the main Play-services dependency (the others: Firebase Crashlytics and Credential
  Manager's `credentials-play-services-auth`). Location stays Play-free.
- `androidApp/google-services.json` is committed (not a secret) and holds clients for
  `ch.stenzel.tim.polleninfo` and `.debug`; re-download after adding an app id.
- **The push address is the Firebase Installation ID**, not a registration token
  (`firebase_messaging_installation_id_enabled` in the manifest; `getToken()` then throws). The
  "token" names (`fcmToken`, `fcm_token`, `PushTokenProvider`) are kept and carry the FID. A *failed*
  task throws (screen offers Retry); `Unavailable` means "platform cannot receive push" (iOS).
- Channels `daily_report` / `threshold_alert` (= server `PushChannel` ids) are created in
  `PollenInfoApplication` and again in `MainActivity.onCreate` (renames on language change).
- **Every message is data-only** and reaches `PollenFirebaseMessagingService.onMessageReceived`:
  `parseAlarmPayload` (pure) → `notificationText` (pure) → post. Anything not understood becomes
  `AlarmNotificationContent.Generic` — never a list that silently drops a type. Times in
  `Europe/Zurich`. The service aligns `Locale.getDefault()` with `appLanguageContext()` first.
- `onRegistered(fid)` → `PushTokenUpdater` with `runBlocking` + 20 s timeout (the service may stop
  as soon as it returns). Failures are only logged.
- Testing by hand: the Firebase console can't send data-only messages — run `:server:run` with
  `FCM_CREDENTIALS` and an alarm due in a minute, or POST
  `{"message":{"fid":…,"android":{"priority":"HIGH"},"data":{…}}}` to FCM HTTP v1.

### Crash reporting (Android)

- **Firebase Crashlytics, release builds only**, same Firebase project and `google-services.json`;
  no new secret. Library in `:composeApp`'s `androidMain` (BoM version), Gradle plugin in
  `:androidApp`. iOS has none yet (needs the native SDK, dSYM upload and a Kotlin bridge).
- **Fatal crashes and ANRs only.** No `recordException`, no custom keys, no logs, no user id —
  nothing that could carry a location, station, account, email, diary or token. Check:
  `grep -rnE 'setCustomKey|setUserId|recordException|Crashlytics.*\.log\(' composeApp/src androidApp/src`
  finds nothing. Crashlytics identifies the install by the FID (the same one push uses).
- **Debug builds never report:** `firebase_crashlytics_collection_enabled=false` in
  `androidApp/src/debug/AndroidManifest.xml`. Verify with `grep -l 'firebase_crashlytics_collection_enabled'
  androidApp/build/intermediates/merged_manifest/*/process*MainManifest/AndroidManifest.xml` — only debug matches.
- **Readable traces:** the plugin uploads the R8 mapping of every release (see Commands for the
  `polleninfo.uploadCrashlyticsMapping` switch); `proguard-rules.pro` keeps `SourceFile,LineNumberTable`
  and exception class names (the library's consumer rules do not).
- **Opt-out:** the user's switch (`CrashReportingPreferences`, on by default) is the **single source
  of truth**. `CrashReportingSync` (`core/crashreporting`, tested) applies every distinct stored value
  to `CrashReporter.setEnabled` — nothing else calls it — and swallows read failures (Firebase keeps
  its persisted value). `PollenInfoApplication.onCreate` starts it after Koin on a process-lifetime
  scope, so FCM-started processes honour it too. `FirebaseCrashReporter(collectionAllowed)` gets
  `!FLAG_DEBUGGABLE` from `platformModule` and then always disables: a runtime call overrides the
  debug manifest flag, so it must never pass `true` in a debug build.
- No NDK plugin (no native code of our own). Manual end-to-end check and the release disclosures
  (Data safety, privacy policy): `deploy/README.md`, "Crash reporting manual check".

### Localization

- Languages: English (default, `values/`), German, French, Italian. Everything is translated except
  station names, "PollenInfo" and `feature/example`.
- Compose resources in `composeApp/src/commonMain/composeResources/values{,-de,-fr,-it}/strings.xml`,
  accessor `ch.stenzel.tim.polleninfo.resources.Res`. **Positional placeholders only** (`%1$s`).
  Android-only text (channel names) in `composeApp/src/androidMain/res/values*/`. `app_name` is in
  `:androidApp`, untranslatable.
- Keys are `<area>_<thing>`: `nav_`, `settings_`, `error_`, `home_`, `onboarding_`,
  `change_station_`, `station_picker_`, `species_`, `severity_`, `feeling_`, `date_`, `alarm_`,
  `all_stations_`, `diary_`, `account_`, `reading_`, `notification_`, `common_` (shared words).
  `example_` is reserved, `values/` only.
- **Style:** German is Swiss Standard German ("ss", never "ß"), "du". French/Italian formal
  ("vous"/"Lei"); French uses U+202F before `? ! ;` and « ». Typographic apostrophes (’).
- **Vocabulary** has a composable and a `StringResource` form: `speciesName(id, fallback)` /
  `speciesNameResource` (server name as fallback for unknown ids), `PollenSeverity.label()` /
  `labelResource()`, `Feeling.label()`, `minimumLabel()`.
- **Dates:** `core/ui/format/DateWording` (pure; `fullDate`, `shortDate`, `monthOnly`,
  `weekdayShort/Full`, `weekdays` ranges), built from `string-array` resources via
  `rememberDateWording()` / `loadDateWording()`; times are `formatTime` (`HH:mm`). Tested against
  `DateWordingFixtures.kt` (keep in step with resources). **Never use kotlinx-datetime's
  `MonthNames.ENGLISH_*` / `DayOfWeekNames.ENGLISH_*`.**
- **Formatters stay pure:** they take `DateWording` and resolved words as parameters; a translated
  sentence is returned as `ResourceText(resource, args)` and resolved by the screen (`resolve()` /
  `load()`), so tests pin resource + args, not English text.
- **Choosing the language:** Android `AndroidLanguageRepository` over AppCompat per-app locales
  (`MainActivity` is an `AppCompatActivity`; `locales_config.xml` must list exactly `AppLanguage`'s
  languages). Text outside an activity uses `Context.appLanguageContext()`. iOS writes
  `AppleLanguages` in the app's own domain, applies on next launch.
- **The check** `checkTranslations` fails on missing/extra keys, placeholder mismatch, array length,
  blank values, or "ß" in `values-de`.
- **No hard-coded text.** Review with
  `grep -rnE '"[A-Z][a-z]+( [a-z]+)+' composeApp/src/commonMain composeApp/src/androidMain composeApp/src/iosMain --include='*.kt'`
  (ignoring comments and `feature/example`). Allowed hits only: exception messages
  (`HttpStatusException`, `AlarmFailures.kt`, `SessionFailures.kt`, `AlarmApiService` fallback, the
  activity `error(...)` in `NotificationPermissionController.android.kt`, the iOS Documents
  `requireNotNull`), log lines (`PollenFirebaseMessagingService`, `AndroidCredentialStore`) and the
  lake names in `SwissLakes.kt`.

### Multiplatform gotchas

- **`commonMain` has no `String.format`**, no `java.*`, `UUID`, `SimpleDateFormat` — it compiles
  for Android and breaks iOS. Use `kotlin.math` and kotlinx-datetime's `Format { }` builders.
- **Backtick test names may not contain a comma on Kotlin/Native** ("Name contains illegal
  characters").
- **`Icons.Default.*` is not transitive** for iOS; `material-icons-extended` is an explicit
  `commonMain` dependency — keep it.
- `expect`/`actual` files use `.android.kt` / `.ios.kt` suffixes. A composable `expect fun` works.
- **Always compile an iOS target after touching `commonMain`.**

## Server conventions

- Plugin config is split into `plugins/` extension functions (`configureSerialization`,
  `configureLogging`, `configureSecurity`, `configureRouting`, `configureAlarmRouting`,
  `configureAccountRouting`) composed in `Application.module(config)`, which builds long-lived
  collaborators once (`meteoSwissPollenService`, `MeasurementService`, `HistoryService`, database,
  stores) and passes them in, then starts the scheduler with the same `MeasurementService`.
- Routes are `fun Route.xRoutes(dependency)` per feature package, taking collaborators as
  parameters so tests supply their own. `configureSecurity` must be installed **before** routes.
- Domain types carry no transport concerns beyond `@Serializable`; wire shapes are separate DTOs in
  `model/`.
- `SWISS_ZONE` (`pollen/domain`, `Europe/Zurich`) is the one zone for "which day is it";
  `ALARM_ZONE` aliases it so `pollen/` never imports `alarm/`.
- Packages: `config/`, `plugins/`, `pollen/{domain,upstream,measurement,history,model}`,
  `alarm/{domain,store,push,scheduler,model}`, `account/{domain,store,model}`. Migrations in
  `server/src/main/resources/db/migration/`.

### REST API

| Method | Path | Returns |
| --- | --- | --- |
| GET | `/health` | `OK` |
| GET | `/pollen/stations` | All 15 stations with coordinates and altitude |
| GET | `/pollen/stations/{abbr}` | One station (case-insensitive), `404` if unknown |
| GET | `/pollen/stations/{abbr}/measurements` | Latest reading, classified (below) |
| GET | `/pollen/stations/{abbr}/history?range=week\|month\|year` | Daily levels, classified, days ending yesterday; `400` bad/missing range (checked before the station); `404` unknown station; `502` upstream failed, nothing retained |
| GET | `/pollen/species` | The 7 taxa with display and latin names |
| GET | `/pollen/thresholds` | Per-species bands + unit |
| POST | `/devices` | `{fcmToken}` → `201 {deviceToken}`; `400` missing/blank |
| DELETE | `/devices/me` | (auth) `204`; device, alarms and log deleted |
| PUT | `/devices/me/fcm-token` | (auth) `{fcmToken}` → `204`; `400` missing/blank |
| GET | `/devices/me/alarms` | (auth) `200 [Alarm]` in creation order |
| POST | `/devices/me/alarms` | (auth) Alarm without `id` → `201 Alarm`; `400` invalid; `409` at 10 alarms |
| PUT | `/devices/me/alarms/{alarmId}` | (auth) → `200 Alarm`; `400` invalid; `404` unknown, another device's, or not a UUID |
| DELETE | `/devices/me/alarms/{alarmId}` | (auth) `204`; `404` as for `PUT` |
| POST | `/users` | `{email, password, consent: true}` → `201 {sessionToken}`; `400` invalid/no consent; `409` email taken (any case) |
| POST | `/users/session` | `{email, password}` → `201 {sessionToken}`; `401` identical for wrong password and unknown email |
| DELETE | `/users/me/session` | (user auth) `204`; ends the calling session only |
| DELETE | `/users/me` | (user auth) `{password}` → `204`; account, sessions, diary deleted; `403` wrong password (not `401` — session stays valid) |
| POST | `/users/me/diary` | (user auth) `{feeling}` → `201 {date, feeling}` first answer of the Swiss day; `200` with the **stored** answer otherwise; `400` unknown feeling |
| GET | `/users/me/diary?from=…&until=…` | (user auth) `200 [{date, feeling}]`, both ends included, oldest first; `400` missing/unparseable date, `until < from`, or > 366 days |
| GET | `/users/me/diary/today` | (user auth) `200 {date, feeling}` or `204` |

(auth) = `Bearer <deviceToken>`, (user auth) = `Bearer <sessionToken>`. A missing, malformed,
unknown or expired token is `401` + `WWW-Authenticate: Bearer`, checked **before** the body. Any
route may also answer `429 {error}` + `Retry-After` (rate limit; never `/health`), `413` (body over
16 KB, declared or chunked) or `500 {"error":"internal error"}` (details logged, never sent).

**`/measurements`** — `{stationAbbr, measuredAt, unit, species: [{id, name, latinName,
concentration, severity}]}`. **All seven taxa always present**, in `PollenSpecies` order;
`concentration` and `severity` are `null` **together** = station doesn't report it (≠ `0`/`NONE`).
**No `overallSeverity` and no sorting** on the wire — those are presentation rules in the app.
`measuredAt` is mandatory. `200` fresh or retained; `404` unknown abbr or file with no usable row
and nothing retained; `502` fetch failed, nothing retained.

**`/history`** — `{stationAbbr, range, from, until, days: [{date, species: [{id, concentration,
severity}]}]}`. 7/30/365 days ending **yesterday** in `SWISS_ZONE` (`historyWindow`). **Every date
present**, oldest first, all seven taxa, `null` = no value (the publisher leaves days out of the
file). `d_historical` is read only when the window starts before 1 January (only rows from last
year's 1 January are kept); on overlap `d_recent` wins. If a needed file failed with nothing
retained, the whole result is `502`, even if the other answered.

### Pollen pipeline

`upstream/PollenService` (bytes; one production impl `MeteoSwissPollenService(client, baseUrl)`,
throws on non-2xx) → `upstream/PollenCsvParser` → `measurement/TtlCache` → `MeasurementService` /
`HistoryService` (classify per request, so threshold changes apply at once).

- `TtlCache<K, V>` is generic (knows nothing of pollen), returns `Fresh` / `Stale` / `Failed(cause)`,
  locks per key, shares a failed load's outcome with queued callers, never caches failures, takes a
  `java.time.Clock`. A file with no usable row is a failed load (`NoUsableRowException` → `404`).
- `meteoSwissPollenService` builds the `HttpClient(CIO)` (15 s timeouts, closed on
  `ApplicationStopped`); tests never construct it.
- Fixtures: verbatim downloads of all 15 files under `server/src/test/resources/fixtures/ogd-pollen/`
  (test resources — must not ship), read by `PollenCsvParserTest` via `PollenStation.hourlyNowPath`
  etc. `FakePollenService` has separate bytes/failure/requested per file; `hourlyCsv(...)` /
  `dailyCsv(...)` build files.

### Abuse protection

`plugins/Security.kt` `configureSecurity(rateLimits, trustedProxy)`:

- Token-bucket limiters: `REGISTER_LIMIT` (`POST /devices`, per address), `ACCOUNT_LIMIT` (`POST
  /users` and `/users/session` share **one** bucket, per address), `DEVICE_LIMIT` / `USER_LIMIT`
  (**inside** `authenticate(...)`, keyed on the principal, so unknown tokens never get a bucket),
  `POLLEN_LIMIT` (per address). Numbers in `RateLimits` (see Configuration).
- `XForwardedHeaders` only with `trustedProxy`, using the **last** `X-Forwarded-For` value.
- `RequestBodyLimit` 16 KB → `413`. `StatusPages` answers client errors with `{error}`; anything else
  is a generic `500`.
- `CallLogging` logs status, method, path, duration only — never headers, query or body. Tokens and
  emails travel only in headers/bodies, never in paths. No CORS plugin.

### Devices and alarms

- Device and session tokens: 256 bits `SecureRandom`, base64url unpadded (43 chars),
  `parseOrNull` before any lookup, `toString` hidden, only the **SHA-256** stored. `DeviceId` is
  internal, never sent.
- `plugins/Authentication.kt` has two bearer providers added with `Application.authentication { }`
  (installable once, extendable) — `device` by `configureAlarmRouting`, `user` by
  `configureAccountRouting`, in either order.
- `DeviceStore.authenticate` refreshes `last_seen_at` at most once per day
  (`LAST_SEEN_RESOLUTION`). `pruneInactive(now)` deletes devices with `fcm_token IS NULL` **and**
  last seen more than 90 days ago (exactly 90 kept); a device with a push address is never pruned.
- Alarm JSON: `{id, enabled, stationAbbr, species, minSeverity, days, schedule}` with `schedule`
  `{"type":"daily","at":"08:00"}` or `{"type":"threshold","from":"07:00","until":"21:00"}` — a
  sealed `ScheduleDto` using the default `type` discriminator. Times `HH:mm` Swiss local.
- Validation (`AlarmValidation`, pure, returns `Valid`/`Invalid(message)`, runs after auth and before
  the store): ≥ 1 species and day, exact enum names, known station (stored canonical), times parsed
  `STRICT` (rejects `24:00`), threshold needs `until > from` and severity ≠ `NONE`.
- **Max 10 alarms per device**: the device row is locked (`SELECT … FOR UPDATE`) before counting
  (`READ COMMITTED` alone wouldn't stop a race). `created_at` strictly increasing per device. Update
  and delete match alarm **and** device id in one statement; the same `404` for unknown, foreign and
  non-UUID ids. An update leaves the notification log alone.

### Accounts (server)

- `AccountValidation` (pure): email trimmed + lowercased, ≤ 254 chars, exactly one `@` with text on
  both sides; password 8–128 **code points**, never trimmed, no character rules; `consent` must be
  `true`. Messages never repeat a value; `AccountSpec`/`RegisterDto` don't print fields.
- `Argon2PasswordHasher`: Argon2id (Bouncy Castle), 16-byte salt, m=19456 t=2 p=1, PHC string (so
  parameters can be raised), constant-time `verify`, `verifyDummy` for unknown emails, runs on
  `Dispatchers.Default`. Tests use `cheapHasher()`.
- Email taken is decided by the unique index (`UserStore.register` → `EmailTaken`), not a lookup.
- Sessions: one row per phone; `SESSION_EXPIRY` 90 days since last use (exactly 90 valid),
  `last_used_at` refreshed at most daily; `pruneExpired(now)` daily via the scheduler.
- Diary: the **server picks the date** (Swiss day of the routes' clock). **The first answer of a day
  is final** — `insertIgnore` + read in one transaction (`Recorded` / `AlreadyAnswered`). Range read
  max `MAX_DIARY_RANGE_DAYS` = 366 inclusive, dates parsed strictly.
- **Accepted limitations** (no email delivery): no verification, no password reset (form says so);
  registration reveals whether an email exists (`409`), login doesn't; an answer tapped while logged
  out is not kept through login.

### Persistence

- PostgreSQL 18 (`postgres:18.6` — same tag in `deploy/compose*.yaml` and `TestPostgres.IMAGE`,
  checked by `DeployFilesTest`), Exposed 1.x DSL, HikariCP (`POOL_SIZE` 5). Imports: tables,
  operators and statement builders from `org.jetbrains.exposed.v1.core`; `Database`, queries and
  `transaction` from `…v1.jdbc`; `timestampWithTimeZone`/`time`/`date` from `…v1.javatime`;
  `javaUUID` from `…v1.core.java`.
- **Two roles** (`deploy/postgres/init/01-roles.sh`, shared by dev, tests, production):
  `polleninfo_owner` owns the schema and runs Flyway; `polleninfo_app` gets only
  `SELECT/INSERT/UPDATE/DELETE`.
- **The schema is Flyway's**: `PollenInfoDatabase.open(config)` migrates as owner, then pools as app.
  `Tables.kt` / `AccountTables.kt` only mirror the SQL. **A schema change is a new migration**; never
  edit an applied one (`V1__init.sql` is frozen since production; reset the dev DB instead).
- Tables: `devices`, `alarms` (→ devices, cascade), `notification_log` (→ alarms, cascade),
  `scheduler_state` (one row), `users`, `sessions` (→ users, cascade), `diary_entries`
  (→ users, cascade, PK `(user_id, date)`). Arrays hold enum names; instants written at UTC.
- Store interfaces are `suspend`; Exposed impls run on `Dispatchers.IO`. `configureRouting` has no
  database, so `PollenRoutesTest`/`RoutingTest` need no Docker.

### Configuration

`config/ServerConfig.load(env, readFile)` is pure, reads everything before a port opens, and throws
`ConfigException` listing **every** problem; `main()` exits 1.

| Variable | Development default | Production |
| --- | --- | --- |
| `POLLENINFO_ENV` | `development` (also unset) | `production`; anything else fails |
| `PORT` | `8080` | optional, 1–65535 |
| `DB_URL` | `jdbc:postgresql://localhost:5432/polleninfo` | required |
| `DB_OWNER_USER` / `DB_APP_USER` | `polleninfo_owner` / `polleninfo_app` | optional |
| `DB_OWNER_PASSWORD_FILE` / `DB_APP_PASSWORD_FILE` | `deploy/dev-secrets/` | required, readable, non-empty |
| `FCM_CREDENTIALS` | unset → `LoggingPushSender` | required, readable |
| `RATE_LIMIT_REGISTER_PER_HOUR` / `_DEVICE_PER_MINUTE` / `_POLLEN_PER_MINUTE` / `_ACCOUNT_PER_HOUR` / `_USER_PER_MINUTE` | 5 / 60 / 120 / 10 / 60 | optional, whole number > 0 |
| `TRUSTED_PROXY` | `false` | `true` by default |

Passwords only from files (trimmed), never variables; `DatabaseConfig.toString` omits them.
`deploy/dev-secrets/` is committed on purpose (`ServerConfigTest` checks it matches
`ServerConfig.DEV_*_PASSWORD`). A configured but unreadable `FCM_CREDENTIALS` fails in development too.

### Alarm delivery

- `AlarmScheduler.tick()` is the tested surface; `launchAlarmScheduler` is a logic-free minute loop
  that survives failing ticks. On `ApplicationStopping` the running tick may finish (max
  `TICK_STOP_TIMEOUT` 45 s); Netty grace 2 s (max 5 s); `deploy/compose.yaml`'s `stop_grace_period`
  (75 s) must exceed the sum (`DeployFilesTest`).
- Each tick: current minute in `ALARM_ZONE`, `AlarmStore.enabledWithDeliverableDevice()`, keep daily
  reports due this minute and open threshold windows, read each station **once**, stations in
  parallel under `supervisorScope` each with its own `try`.
- **Catch-up:** finished minutes are stored in `scheduler_state`; a tick processes missed minutes at
  most `MAX_CATCH_UP` (2 min) old, then the current one. Older minutes are **never replayed**. First
  start with no state runs only the current minute.
- The first tick of each Swiss day runs three independent prunes, each with its own day marker and
  `try` (never costing the minute's alarms): notification log, `pruneInactive`, `pruneExpired`.
- **`AlarmRules` (pure, `now` passed in) decide; the app words.** `PushMessage` has `kind`,
  `channel`, `station`, `alarmId`, `levels`, `measuredAt` — no text anywhere in `alarm/` (check:
  `grep -rnE '"[^"]*(Pollen in|No current reading|yesterday|No pollen of)' server/src/main/kotlin/ch/stenzel/tim/polleninfo/server/alarm`
  must find nothing).
- **Current** = younger than `READING_STALE_AFTER` (3 h) **and** from today's Swiss date.
- **Daily report** (due when weekday matches and truncated time == `at`): current → sent if minimum is
  `NONE` or a selected type reached it (`REPORT`, or `NO_POLLEN` / `NOT_REPORTED`); not current →
  nothing with a minimum, else `NO_CURRENT_READING` (with `measuredAt`) or `UNAVAILABLE`. `levels`:
  worst first, then `PollenSpecies` order. Not retried within its minute.
- **Threshold alert** (window `from` inclusive, `until` exclusive, evaluated every tick): only on a
  current reading — an outage must never produce a false alert. Qualifying types go into one
  `ALERT`. **At most once per type per alarm per Swiss day** via `NotificationLog`, recorded only on
  `Sent`; climbing further doesn't re-notify.
- `PushSender` → `Sent` | `Unregistered` | `Failed`, never throws. `FcmPushSender`: FCM HTTP v1,
  **data-only**, `android.priority HIGH`, all values strings; address in `fid` if it's 22 base64url
  chars, else legacy `token`. `404 UNREGISTERED` / `400 INVALID_ARGUMENT` → `Unregistered` →
  `DeviceStore.clearFcmToken(deviceId, token)` (conditional on the token still matching). Access
  token is an injected `suspend () -> String`.
- Payload contract with `parseAlarmPayload`: `kind` (`report`, `no_pollen`, `not_reported`,
  `no_current_reading`, `unavailable`, `alert`), `channel`, `stationAbbr`, `stationName`, `alarmId`,
  `levels` (`BIRCH:HIGH,GRASSES:MODERATE`, absent when empty), `measuredAt` (ISO, only
  `no_current_reading`). No version field — change both sides together.
- Logging via `logback-classic` (without it, every log line is dropped). The FCM key is never
  committed.

## Deployment

`deploy/README.md` is the runbook. Invariants to keep: only `caddy` publishes ports; secrets are
files under `/opt/polleninfo/secrets` (mode `0400`, owned by the reading uid: 999 Postgres, 10001
server), never variables, never committed; `deploy/.env` and `deploy/local-secrets/` are git-ignored;
deploy and rollback are `deploy/deploy.sh <sha>`.

## Testing

| Source set | Deps |
| --- | --- |
| `composeApp/src/commonTest` | `kotlin("test")`, `kotlinx-coroutines-test`, `ktor-client-mock` |
| `server/src/test` | `kotlin("test")`, `ktor-server-test-host`, `ktor-client-content-negotiation`, `ktor-client-mock`, `testcontainers-postgresql`, `logback-classic` |

- App tests in `commonTest`, **never `androidUnitTest`**.
- Test names are backtick sentences describing behaviour (no commas — Native).
- Repositories are tested through a **real `HttpClient` on `MockEngine`** (`createHttpClient(engine)`),
  not a mocked service.
- ViewModel tests: `Dispatchers.setMain(StandardTestDispatcher())`, `advanceUntilIdle()`,
  `resetMain()`; hold loads with fake gates to observe intermediate states.
- Hand-written fakes in plain `commonTest` files; **no mocking framework**.
- Server route tests: `testApplication { application { configureSerialization(); configureSecurity();
  configureRouting(own collaborators) } }`.
- **Pin boundaries from both sides** (`SpeciesThresholdsTest` is the model).
- **No test contacts MeteoSwiss**; time-dependent server tests use `MutableClock`, none sleeps.
- Database tests use `alarm/store/TestPostgres` (one container per JVM, production image and role
  script): `cleanDatabase()` (migrated once, truncated per test, as the app login) or
  `unmigratedDatabase()` for `MigrationTest`. Helpers in `AlarmFixtures.kt` (`insertAlarm`,
  `alarmId('a')`, `registerDevice`, `unknownDeviceId()`). `*FlowLoggingTest`s assert no token,
  email, password or push address reaches the log.
- When a test documents probably-wrong behaviour, say so in a comment.
- Keep this file in step with the code when a feature changes a rule written here.
