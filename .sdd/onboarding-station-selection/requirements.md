# Onboarding: Station Selection

## Problem Statement

Pollen levels in Switzerland are measured at 15 fixed stations, and a reading is only meaningful in
relation to the station it came from. A user who opens PollenInfo for the first time has no way to
tell the app which station's readings they care about, so the app cannot show them anything useful.

Making the user find their station by hand is friction at the worst possible moment — the very first
interaction with the app. Most users do not know which of the 15 stations is closest to them, and the
ones who do would still rather not scroll a list to say so. Conversely, a user who does not want to
share their location, or who wants readings for a place they are travelling to rather than where they
are standing, must not be forced into a location prompt.

## Solution

A single onboarding screen, shown once on first launch, that asks the user for exactly one thing: the
measuring station they want pollen levels for.

The screen offers two ways to answer, and the user may use either:

- **Use my location** — the app determines the user's approximate position and proposes the nearest
  of the 15 stations.
- **Pick a station** — a dropdown listing all 15 stations by name in alphabetical order.

The two paths converge. A successful location lookup does not silently commit a choice; it fills in
the dropdown with the nearest station, so the user always sees which station was proposed and can
change it before confirming. A `Continue` action confirms the selection, and the app remembers it.
From then on the app opens directly to its main content and never shows onboarding again.

Because the app only needs to distinguish between 15 stations spread across the country, it asks for
**approximate location only** — never precise location. Location is a convenience, never a
requirement: if the user declines the permission, or if their position cannot be determined, the
screen explains the situation in one short line and remains fully usable through the dropdown.

## User Stories

1. As a first-time user, I want to be asked which measuring station I care about, so that the pollen
   levels the app shows me are relevant to where I live.
2. As a first-time user, I want the app to propose the station nearest to me, so that I do not have
   to know which of the 15 stations is closest.
3. As a first-time user, I want to pick my station from a list, so that I can set up the app without
   sharing my location at all.
4. As a user browsing the station list, I want the stations sorted alphabetically by name, so that I
   can find the one I am looking for by scanning rather than by reading all 15 entries.
5. As a user browsing the station list, I want to see each station's name and nothing else, so that
   the list stays quick to scan.
6. As a user who prefers the location shortcut, I want the location option offered first, so that the
   fastest path is the most prominent one.
7. As a user who taps "Use my location", I want to see which station the app picked before it is
   saved, so that I am not surprised later by readings from a station I did not expect.
8. As a user whose nearest station is not the one I want, I want to change the proposal before
   confirming, so that I can choose a station near my workplace or a place I am travelling to.
9. As a user, I want my selection to take effect only when I confirm it, so that a mis-tap in the
   dropdown does not lock me into the wrong station.
10. As a user, I want the confirm action to stay unavailable until a station is actually selected, so
    that I cannot continue with nothing chosen.
11. As a privacy-conscious user, I want the app to request only approximate location, so that I am
    not asked to share my precise position for a feature that does not need it.
12. As a privacy-conscious user, I want my location to be used only on my device, so that my position
    is never transmitted to the app's servers.
13. As a user who declines the location permission, I want a short explanation telling me where to
    enable it, so that I know how to use the shortcut later if I change my mind.
14. As a user who declined the permission and changed my mind immediately, I want the location button
    to still respond, so that I can grant the permission without leaving the app when my device still
    allows it.
15. As a user whose position cannot be determined, I want to be told so and pointed at the manual
    option, so that I am not stuck waiting on something that will not happen.
16. As a user, I want the app to stop waiting for my location after a short while, so that a failed
    lookup does not leave me staring at a spinner indefinitely.
17. As a user waiting for a location lookup, I want visible feedback that the app is working, so that
    I do not tap the button repeatedly.
18. As a user waiting for a location lookup, I want to be able to pick a station manually instead, so
    that a slow lookup never blocks me.
19. As a user who has seen a location error, I want the message to disappear once I retry or pick a
    station manually, so that a stale error does not sit underneath a valid selection.
20. As a user with a working connection, I want the station list to load quickly and without
    ceremony, so that setup is a matter of seconds.
21. As a user on a poor connection, I want to be told that the station list could not be loaded and
    be offered a retry, so that a temporary network failure does not force me to restart the app.
22. As a user whose selection could not be saved, I want to be told and kept on the screen, so that I
      do not end up in an app that has forgotten what I just chose.
23. As a returning user, I want the app to remember my station across restarts, so that I am never
    asked to set it up twice.
24. As a returning user, I want the app to open directly on its main content, so that I do not see a
    flash of the setup screen on every launch.
25. As a user who has completed onboarding, I want the device's back gesture to leave the app rather
    than return me to setup, so that a screen whose purpose is already fulfilled does not reappear.
26. As a user, I want the station names shown in their local spelling (Genève, Zürich, Neuchâtel), so
    that the names match what I know them as.
27. As a user, I want the setup screen to look like the rest of the app, so that it feels like part of
    the product rather than a technical prompt.
28. As a user located outside Switzerland, I want the app to still propose the nearest Swiss station,
    so that the location shortcut works even when I am abroad.
29. As an iPhone user, I want the same setup experience as on Android, so that the app behaves
    consistently across my devices.
30. As a maintainer, I want the list of stations to come from our own backend rather than being built
    into the app, so that station metadata never has to be corrected by shipping an app update.
31. As a maintainer, I want the conventions this feature introduces written into the project
    documentation, so that the next feature follows them instead of inventing its own.

## User Acceptance Tests

1. Given a freshly installed app, when it is opened, then the station setup screen is shown.
2. Given the setup screen, when it has finished loading, then the location option appears above the
   station dropdown, and a confirm action appears below it.
3. Given the setup screen, when the station dropdown is opened, then all 15 measuring stations are
   listed by name in alphabetical order, starting with Basel and ending with Zürich.
4. Given the station dropdown is open, when the list is read from top to bottom, then Lausanne appears
   before Locarno / Monti.
5. Given the station dropdown is open, when a station row is read, then it shows the station name only
   — no canton, coordinates or altitude.
6. Given the setup screen has just loaded, when no station has been selected, then the confirm action
   is not available.
7. Given the setup screen, when a station is picked from the dropdown, then the dropdown shows that
   station and the confirm action becomes available.
8. Given a station is selected, when the confirm action is used, then the setup screen is left and the
   main screen is shown displaying the selected station's name.
9. Given the location permission has never been requested, when the location option is used, then the
   system permission prompt appears asking for approximate location.
10. Given the system permission prompt is shown, when approximate location is granted and a position
    is determined, then the dropdown is filled with the station nearest to that position and the
    confirm action becomes available.
11. Given the location lookup filled the dropdown with the nearest station, when the confirm action is
    used, then that station is saved and the main screen shows its name.
12. Given the location lookup filled the dropdown with the nearest station, when a different station
    is picked from the dropdown and confirmed, then the manually picked station is the one saved.
13. Given the location permission has been declined, when the location option is used, then the text
    "Location permission is off. Enable it in Settings to find your nearest station." appears
    directly below the location option.
14. Given the permission-declined message is shown, when the station dropdown is used to pick a
    station, then the message disappears.
15. Given the permission-declined message is shown, when the location option is used again, then the
    location option still responds — either by prompting again if the device permits it, or by
    leaving the message in place.
16. Given location services are switched off on the device and the permission is granted, when the
    location option is used, then the text "Your location could not be determined. Please pick a
    station manually." appears below the location option, and the dropdown remains usable.
17. Given the permission is granted but no position can be obtained, when more than roughly ten
    seconds have passed, then the location lookup stops and the "could not be determined" message is
    shown.
18. Given the location option has been used and the lookup is in progress, when the screen is
    observed, then the location option shows a progress indicator and cannot be used again, while the
    dropdown and the confirm action remain usable.
19. Given a location lookup is in progress, when a station is picked from the dropdown and confirmed,
    then the selection is saved without waiting for the lookup to finish.
20. Given the device is positioned near Winterthur, when the location option is used and succeeds,
    then Zürich is proposed.
21. Given the device is positioned in Valais near Sion, when the location option is used and
    succeeds, then Sion is proposed.
22. Given the device is positioned outside Switzerland, when the location option is used and succeeds,
    then a Swiss station is still proposed and no error is shown.
23. Given the backend is unreachable, when the setup screen is opened, then an error message and a
    retry action are shown instead of the dropdown.
24. Given the error message and retry action are shown, when the backend becomes reachable and retry
    is used, then the station list loads and the dropdown becomes usable.
25. Given a station has been confirmed, when the app is closed and reopened, then the setup screen is
    not shown and the main screen appears directly with the previously selected station.
26. Given a station has been confirmed, when the app is reopened, then no flash or partial display of
    the setup screen is visible before the main screen appears.
27. Given the main screen is shown after onboarding, when the device's back gesture is used, then the
    app is left and the setup screen is not shown.
28. Given the app is opened offline on a device that has never completed setup, then the setup screen
    reports that the station list could not be loaded and offers retry.
29. Given a device where saving the selection fails, when the confirm action is used, then the message
    "Could not save your selection. Please try again." appears and the user stays on the setup screen.
30. Given the setup screen, when it is compared against the rest of the app, then it uses the app's
    standard colours and typography and shows no title bar.
31. Given a device set to any language, when the setup screen is shown, then its wording is in
    English, while station names appear in their local spelling.

## Definition of Done

- All user acceptance tests that can be executed on Android pass on a physical or virtual Android
  device, against a locally running backend.
- The station list shown to the user is retrieved from the project's own backend at run time; the app
  contains no hardcoded copy of station names or coordinates.
- The user's position is used only on the device: no request sent to any server contains the user's
  coordinates.
- The app requests approximate location only. It never requests precise location on either platform.
- Exactly two location failure messages exist, matching the agreed wording, and both appear directly
  below the location option while leaving the rest of the screen usable.
- A confirmed selection survives an app restart, and onboarding is not shown again.
- The automated test suites for the app and the server pass.
- The iOS sources compile for the simulator target.
- The iOS implementation is written and compiles; the platform configuration keys it depends on are
  documented for whoever creates the iOS application wrapper.
- No regression in existing behaviour: the existing reference feature and the server's endpoints are
  unchanged in function.
- The project documentation is updated to describe the conventions this feature introduces.
- A release build of the Android app does not permit unencrypted network traffic.

## Out of Scope

- **Changing the station after onboarding.** There is deliberately no way to revisit the selection;
  this arrives with the planned app settings feature. Until then, clearing the app's data is the only
  way to return to onboarding.
- **A link into system settings** from the permission message. The message is static text; the
  settings shortcut arrives with the settings feature.
- **Localization.** All wording introduced by this feature is English. Translating the app into
  German, French and Italian is a separate, project-wide task.
- **The real main screen.** What follows onboarding is a deliberate placeholder that displays the
  selected station's name and nothing else. Pollen readings, refresh and any dashboard content are
  future features.
- **The iOS application wrapper.** The project has no iOS app project, so the iOS build cannot be run
  or manually tested. The iOS implementation is written and compile-verified only.
- **Push notifications.** Unaffected by this feature, though it establishes the stored station that a
  future subscription will reference.
- **Precise location, background location, and geofencing.**
- **Multiple stations per user, or favourites.** Exactly one station is selected.
- **A distance limit on the location shortcut.** The nearest Swiss station is always proposed,
  however far away the user is.
- **Automated user-interface tests.** The project has no UI test infrastructure, and this feature does
  not introduce it.

## Further Notes

- **This is the first feature that talks to our own backend.** The reference feature points at a
  fictional documentation host, so onboarding is where a real backend address is introduced. During
  development that address is a local server, which means the Android build must be allowed to make
  unencrypted requests — restricted to debug builds so a release build can never do so.
- **Onboarding needs connectivity on first launch.** This is accepted: a device that has never
  completed setup has no pollen data cached either, so the app has nothing to show offline regardless.
- **Only the Android path can be manually verified in the current environment.** The iOS location and
  permission code is written against the platform APIs and compiled, but never executed. Any defect
  in it will surface when the iOS application wrapper is created.
- **The two location failure messages are deliberately coarse.** In particular, "location services
  switched off at device level" is reported as "could not be determined" rather than getting its own
  message, and a declined permission is reported the same way whether the user can still be prompted
  or not. This keeps the screen to the two states specified.
- **The station name is stored alongside the station's abbreviation.** This lets any screen label the
  user's selection without a network request. The trade-off is that the stored name would go stale if
  MeteoSwiss ever renamed a station; refreshing it whenever the station list is fetched is an obvious
  later improvement and is not handled now.

---

## Technical Annex
> Written against codebase as of: 2026-07-31

This section contains the architectural and automated testing decisions derived from the planning
session. It is intended for architect and developer review. Specific class names, method signatures,
and implementation details are welcome here.

When this document is later used to generate tasks, the task-generation step will verify each
decision in this annex against the current state of the codebase and flag any conflicts before
proceeding.

### Architectural Decisions

#### Codebase state this builds on

- `:server` already serves `GET /pollen/stations` (all 15 stations with `abbr`, `name`, `canton`,
  `latitude`, `longitude`, `altitudeMasl`), covered by `PollenRoutesTest`. **No server changes are
  required by this feature.**
- `:composeApp` contains only `feature/example`, whose `ExampleApiService` targets the fictional
  `https://api.example.com/v1/pollen`. `Screen.Example` is the current `startDestination`.
- The app has no persistence library, no location code, and no base URL for our own backend.
- `AndroidManifest.xml` declares only `INTERNET`.
- There is no `iosApp` Xcode project. `MainViewController.kt` **does** call `startKoin { modules(appModules) }`
  inside `ComposeUIViewController(configure = …)`, so Koin initialization needs no change — only
  `appModules` gains an entry.

#### Deep modules

**1. `core/location/CoarseLocationProvider`** — hides permission state inspection, provider selection,
callback-to-coroutine bridging, cancellation and the timeout behind a three-outcome result.

```kotlin
// commonMain
sealed interface CoarseLocationResult {
    data class Success(val latitude: Double, val longitude: Double) : CoarseLocationResult
    data object PermissionDenied : CoarseLocationResult
    data object Unavailable : CoarseLocationResult
}

interface CoarseLocationProvider {
    suspend fun currentLocation(): CoarseLocationResult
}
```

- Actuals implemented with `suspendCancellableCoroutine` so coroutine cancellation propagates to the
  platform (Android `CancellationSignal`, iOS `stopUpdatingLocation()`).
- The `withTimeoutOrNull(LOCATION_TIMEOUT)` wrapper lives in **common** code (`LOCATION_TIMEOUT = 10.seconds`),
  so the timeout is one constant, testable in `commonTest`, rather than duplicated per platform. A
  timeout maps to `Unavailable`.
- **Android**: `LocationManager.getCurrentLocation(LocationManager.NETWORK_PROVIDER, …)`. **No Play
  Services / `FusedLocationProviderClient`.** No GPS fallback: `ACCESS_COARSE_LOCATION` alone does not
  grant `GPS_PROVIDER` on older API levels, and `FUSED_PROVIDER` requires API 31 while `minSdk` is 26.
  Missing or disabled provider → `Unavailable`. Permission not granted → `PermissionDenied`.
- **iOS**: `CLLocationManager` with `desiredAccuracy = kCLLocationAccuracyReduced` and
  `requestLocation()`, results delivered through a `CLLocationManagerDelegateProtocol` implementation.
  Status mapping: `.denied` and `.restricted` → `PermissionDenied`; `.notDetermined` → prompt;
  delegate failure or timeout → `Unavailable`. Mapping `.restricted` to "enable it in Settings" is
  knowingly imprecise (the user cannot grant it) and accepted rather than adding a third message.
- No explicit `getLastKnownLocation` fallback: `getCurrentLocation` already returns a sufficiently
  fresh cached fix, whereas an explicit last-known lookup risks a days-old position in another country
  silently resolving to the wrong station.

**2. `core/preferences/SelectedStationRepository`** — hides DataStore keys, the platform file path, and
write failures.

```kotlin
// commonMain
data class SelectedStation(val abbr: String, val name: String)

interface SelectedStationRepository {
    val selectedStation: Flow<SelectedStation?>
    suspend fun select(station: SelectedStation): Result<Unit>   // core.result.Result
}
```

- Backed by `androidx.datastore:datastore-preferences` (KMP-capable 1.1.x), with
  `expect fun dataStorePath(): String` + `.android.kt` / `.ios.kt` actuals, mirroring the existing
  `core/network/HttpClientEngine.kt` pattern.
- Two string keys: station abbreviation and display name. `selectedStation` emits `null` when the abbr
  key is absent. Abbr is the identity used by every future API call; the name is denormalized so any
  screen can label the selection with no network round-trip.
- `select` wraps the write in `safeCall` and returns `core.result.Result` — DataStore writes can throw
  on IO errors and the UI needs to react rather than navigate to a screen with nothing to read.
- Lives in `core/` (not the onboarding slice) because it has three consumers inside this feature
  alone: onboarding, the startup gate and the placeholder home screen.

**3. `feature/onboarding/domain/usecase/FindNearestStationUseCase`** — pure function, no dependencies.

```kotlin
class FindNearestStationUseCase {
    operator fun invoke(latitude: Double, longitude: Double, stations: List<Station>): Station?
}
```

- Haversine over WGS84 coordinates using `kotlin.math` only (no `java.*`, no `String.format`).
- Ties resolved by taking the first match in the incoming (alphabetically sorted) list, so behaviour
  is deterministic and assertable. `minByOrNull` semantics are sufficient.
- **No distance cap.** A position abroad still yields the nearest Swiss station; the confirm step makes
  the proposal visible, so the user can override it.
- Returns `null` only for an empty station list, which cannot occur in the real flow but keeps the
  signature honest.

**4. `feature/onboarding` station retrieval** — hides HTTP, DTO shape, mapping, sorting and error
wrapping behind one call.

```kotlin
// domain/repository
interface StationRepository {
    suspend fun getStations(): Result<List<Station>>          // core.result.Result
}

// domain/model
data class Station(val abbr: String, val name: String, val latitude: Double, val longitude: Double)
```

- `data/remote/StationApiService` + `data/remote/dto/StationDto` (`@Serializable`, matching the
  server's `StationDto`: `abbr`, `name`, `canton`, `latitude`, `longitude`, `altitudeMasl`) +
  `data/mapper/StationMapper` (`fun StationDto.toDomain()`), consumed by
  `data/repository/StationRepositoryImpl`. DTOs never leave `data/`.
- The domain model deliberately drops `canton` and `altitudeMasl` — no consumer in this feature.
- **The mapper sorts**: `sortedBy { it.name }`. Server order is `PollenStation` enum order (by
  abbreviation), which is *not* display-name alphabetical — e.g. `PLO` Locarno precedes `PLS` Lausanne.
  A plain `sortedBy` is correct here because every station name begins with an ASCII letter, so the
  accented characters in Genève / Münsterlingen / Neuchâtel / Zürich never affect ordering. **Add a
  comment saying so**, to stop a future change replacing it with a JVM-only `Collator`.
- Repository wraps in `safeCall` and returns `ch.stenzel.tim.polleninfo.core.result.Result` —
  explicitly imported, never `runCatching`.
- **Owned by `feature/onboarding`, not `core/`**: exactly one consumer today. Promote when a second
  appears; guessing the dashboard's needs now would produce a shared model that gets reshaped anyway.
- **Nearest-station computation stays on the client**, not behind a new server endpoint: the station
  list with coordinates is already in hand, so it costs no extra request and no extra mid-flow failure
  state, and the user's coordinates never leave the device. This is geometry, not the domain policy
  (CSV parsing, severity thresholds) that `:server` deliberately owns.

#### Shallow modules — platform glue and orchestration

**`core/location/LocationPermissionRequester`** — irreducibly thin, and the reason permission handling
is split from location retrieval: Android's request needs an `Activity`-scoped
`ActivityResultLauncher`, which a Koin-injected class holding only `applicationContext` cannot provide.

```kotlin
// commonMain
@Composable
expect fun rememberCoarseLocationPermissionRequester(onResult: (granted: Boolean) -> Unit): () -> Unit
```

- Android actual: `rememberLauncherForActivityResult(RequestPermission())` for
  `android.permission.ACCESS_COARSE_LOCATION`; if already granted, invoke `onResult(true)` without
  launching. iOS actual: `CLLocationManager.requestWhenInUseAuthorization()` with the delegate's
  authorization callback mapped to a boolean; already-determined statuses answer immediately.
- Rejected alternatives: a single `suspend fun resolve()` that also prompts, requiring a global
  `ActivityHolder` (leak-prone, breaks on configuration change); and `moko-permissions` (a dependency
  heavier than one coarse-location prompt justifies today — reconsider when push notifications add a
  second permission).
- Consequence: the ViewModel never touches platform APIs. The screen calls the requester, then hands
  the boolean to `viewModel.onPermissionResult(granted)`.

**`core/network/ApiConfig`**

```kotlin
expect val apiBaseUrl: String        // android: "http://10.0.2.2:8080"   ios: "http://localhost:8080"
```

Injected into `StationApiService` via Koin rather than referenced as a top-level constant, so tests can
point `MockEngine` at any host. No BuildConfig or Gradle flavour machinery — one hardcoded development
URL per platform, replaced when something is deployed.

**`core/di/platformModule`**

```kotlin
expect val platformModule: Module    // androidMain uses androidContext(); iosMain needs none
```

Appended to `appModules`. Provides `CoarseLocationProvider` and the DataStore instance. Keeps
`AppModule.kt` free of `expect`/`actual` noise. Both existing entry points already start Koin with
`appModules`, so no `initKoin()` extraction is needed.

**`feature/onboarding/presentation`**

```kotlin
sealed interface OnboardingUiState {
    data object Loading : OnboardingUiState
    data class Content(
        val stations: List<Station>,
        val selected: Station? = null,
        val isLocating: Boolean = false,
        val locationError: LocationError? = null,
        val saveError: Boolean = false,
    ) : OnboardingUiState
    data class Error(val message: String) : OnboardingUiState
}

enum class LocationError { PERMISSION_DENIED, UNAVAILABLE }

sealed interface OnboardingEvent {
    data object Completed : OnboardingEvent
}
```

- Location failures live **inside `Content`**, not in `Error`. `Error` means the screen cannot function
  (station list failed → full-screen message + retry). A location failure leaves the screen usable —
  the whole point of the second message is "pick a station manually" — so surfacing it as `Error`
  would blank out the dropdown the message just recommended.
- `isLocating` mirrors the established `isRefreshing` idea: the location button shows a spinner and is
  disabled; dropdown and Continue stay enabled.
- `locationError` and `saveError` clear on retry and on a manual dropdown pick.
- Completion is a **one-shot event** via `Channel<OnboardingEvent>(BUFFERED).receiveAsFlow()`,
  collected in a `LaunchedEffect`, calling an `onOnboardingComplete: () -> Unit` lambda from
  `AppNavigation`. Not a `isComplete` state flag: a state flag representing a one-time action re-fires
  on any state re-emission or recomposition. This introduces an events pattern the project does not yet
  have — **note why in a code comment**, since the documented convention is currently UiState-only.
- ViewModel collaborators: `StationRepository`, `FindNearestStationUseCase`, `CoarseLocationProvider`,
  `SelectedStationRepository`. All four are interfaces, so the ViewModel is fully testable with
  hand-written fakes.
- `OnboardingScreen`: no `TopAppBar`. Single centred `Column`, generous padding, order top to bottom —
  headline `Welcome to PollenInfo`; body `Choose the measuring station you want pollen levels for.`;
  full-width `Use my location` button; error text (`bodySmall`, `colorScheme.error`); read-only
  `ExposedDropdownMenuBox` labelled `Station` showing name only; `Continue` pinned at the bottom,
  enabled only when `selected != null`. Hardcoded English strings, consistent with `ExampleScreen`;
  no `composeResources` (a project-wide convention change belongs in its own task).
- Agreed error copy: `Location permission is off. Enable it in Settings to find your nearest station.`
  / `Your location could not be determined. Please pick a station manually.` /
  `Could not save your selection. Please try again.`

**`core/startup/StartupViewModel`**

```kotlin
sealed interface StartupState {
    data object Loading : StartupState
    data object NeedsOnboarding : StartupState
    data object Ready : StartupState
}
```

- Reads `selectedStationRepository.selectedStation.first()` and maps presence to `Ready`.
- `App()` collects it and builds the `NavHost` **only once resolved**, with `startDestination` set to
  `Screen.Onboarding` or `Screen.Home`. No flash for returning users, no `popUpTo` bookkeeping, and no
  `Splash` destination users could navigate back to. While `Loading`, render an empty themed `Surface`
  rather than a spinner — the read takes milliseconds and a one-frame spinner reads as a glitch.
- Lives in `core/` because it is app-level composition rather than part of the onboarding slice.

**`feature/home/presentation/HomeScreen`** — placeholder. Injects `SelectedStationRepository`, collects
`selectedStation`, renders `Selected station: <name>`. No ViewModel, no network, no way to change the
station. Explicitly commented as a stand-in for the real dashboard. `feature/example` is left
untouched — wiring a fictional-data screen that always lands in its `Error` state into the real user
flow would read as a bug.

#### Navigation

```kotlin
sealed interface Screen {
    @Serializable data object Example : Screen      // unchanged
    @Serializable data object Onboarding : Screen
    @Serializable data object Home : Screen
}
```

On completion: `navController.navigate(Screen.Home) { popUpTo<Screen.Onboarding> { inclusive = true } }`
so the back gesture from Home leaves the app.

#### Build, manifest and platform configuration

- `gradle/libs.versions.toml`: add `datastore-preferences` (`androidx.datastore:datastore-preferences`,
  1.1.x) to `commonMain`.
- `composeApp/src/androidMain/AndroidManifest.xml`: add
  `<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />`. Do **not** add
  `ACCESS_FINE_LOCATION`.
- New `composeApp/src/debug/AndroidManifest.xml` with `android:usesCleartextTraffic="true"` on
  `<application>` — debug-only, so a release build can never ship cleartext.
- **Documented for the future iOS wrapper** (no `Info.plist` exists to edit today):
  `NSLocationWhenInUseUsageDescription` (mandatory, or `CLLocationManager` silently never prompts),
  `NSLocationDefaultAccuracyReduced = true` (so iOS never asks for precise access — the direct
  expression of "coarse is enough"), and an ATS `NSAllowsLocalNetworking` exception for the cleartext
  development backend.
- `CLAUDE.md` update covering the four new conventions: the one-shot event channel, `platformModule`
  for platform DI, `core/preferences` as the home for persisted user selections, and the real backend
  base URL plus debug-only cleartext manifest. Also correct the `ExampleApiService` note that says
  "point a real feature at our own backend instead" — it now has a concrete answer to point at.

#### Primary data flow

1. `StartupViewModel` reads the stored station → `NeedsOnboarding` → `NavHost` starts at
   `Screen.Onboarding`.
2. `OnboardingViewModel.init` → `StationRepository.getStations()` → `Content(stations)` sorted
   alphabetically, or `Error` + retry.
3. `Use my location` → composable requester → system prompt → `onPermissionResult(granted)`.
   - `granted == false` → `Content(locationError = PERMISSION_DENIED)`.
   - `granted == true` → `isLocating = true` → `CoarseLocationProvider.currentLocation()`.
     - `Success` → `FindNearestStationUseCase` → `Content(selected = nearest, isLocating = false)`.
     - `PermissionDenied` → `PERMISSION_DENIED`; `Unavailable` (incl. timeout) → `UNAVAILABLE`.
4. Dropdown pick → `Content(selected = station, locationError = null, saveError = false)`.
5. `Continue` → `SelectedStationRepository.select(SelectedStation(abbr, name))`.
   - `Success` → emit `OnboardingEvent.Completed` → navigate to `Screen.Home` with `popUpTo`.
   - `Failure` → `Content(saveError = true)`, stay on screen.

### Automated Testing Decisions

**What makes a good test here.** Tests drive a module through its public interface and assert on the
values it returns or the states it emits — never on how it got there. No mocking framework: the project
uses hand-written fakes (`FakeExampleRepository` is the prior art). The repository is tested through a
real `HttpClient` backed by `MockEngine`, not a stubbed service, so the serialization contract with our
own backend is under test — that is where an upstream change actually bites. Boundaries are pinned from
both sides, following `SpeciesThresholdsTest`. Test names are backtick sentences describing behaviour.
All tests go in `composeApp/src/commonTest`, never `androidUnitTest`, so they run on every target.

**Modules with automated tests:**

| Module | Type | Cases |
| --- | --- | --- |
| `StationRepositoryImpl` (+ `StationApiService`, DTOs) | Integration via `createHttpClient(MockEngine)` | 15 stations deserialize from a realistic `/pollen/stations` payload; HTTP 500 → `Failure`; malformed JSON → `Failure`; unknown JSON fields tolerated |
| `StationMapper` | Unit | Alphabetical order, explicitly asserting `Lausanne` before `Locarno / Monti` (the case enum order gets wrong); Basel first and Zürich last; accented names sort where expected; DTO→domain field mapping |
| `FindNearestStationUseCase` | Unit | Coordinate near Winterthur → `PZH`; near Sion → `PSN`; a point in Germany still resolves to a Swiss station; exact tie → first in list; empty list → `null` |
| `OnboardingViewModel` | Unit, with fakes | `Loading` → `Content`; station fetch failure → `Error`; retry recovers; dropdown pick enables confirm; permission denied → `PERMISSION_DENIED`; provider `Unavailable` → `UNAVAILABLE`; timeout → `UNAVAILABLE`; location success pre-fills `selected` without completing; manual pick overrides a location proposal; errors clear on retry and on manual pick; confirm persists then emits `Completed` exactly once; save failure → `saveError` and no event; `isLocating` observable during the fix |
| `StartupViewModel` | Unit, with fake | Stored station → `Ready`; nothing stored → `NeedsOnboarding`; `Loading` observable first |

**Fakes to write** (plain files in `commonTest`, following `FakeExampleRepository`):
`FakeStationRepository` (programmable `Result`), `FakeCoarseLocationProvider` (programmable
`CoarseLocationResult`, optional delay to exercise `isLocating`), `FakeSelectedStationRepository`
(`MutableStateFlow`-backed, programmable write failure).

**ViewModel test mechanics**: `Dispatchers.setMain(StandardTestDispatcher())` + `advanceUntilIdle()` so
intermediate `Loading` / `isLocating` states are observable; `Dispatchers.resetMain()` in teardown.
Prior art: `ExampleViewModelTest`, `ExampleRepositoryImplTest`, `ExampleMapperTest`.

**Accepted gaps, stated plainly:**

- **The DataStore-backed `SelectedStationRepository` implementation has no unit test.** It needs a real
  platform file path, and app tests may not live in `androidUnitTest`. Mitigation: keep the
  implementation a thin key-read / key-write with zero logic, and test every consumer against the
  in-memory fake.
- **The platform actuals are untested by construction** — `CoarseLocationProvider.android/ios` and
  `rememberCoarseLocationPermissionRequester` are the `expect`/`actual` boundary. Android is verified
  by running the app against the acceptance tests; iOS is compile-verified only via
  `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64`.
- **No composable tests** for `OnboardingScreen` / `HomeScreen`: the project has no UI test
  infrastructure and this feature does not introduce it.
- **No new server tests**: `/pollen/stations` already exists and is covered by `PollenRoutesTest`.

**Commands** (no JDK on `PATH`; export
`JAVA_HOME="/Applications/Android Studio Panda 4.app/Contents/jbr/Contents/Home"` first):

```bash
./gradlew :composeApp:testDebugUnitTest :server:test
./gradlew :composeApp:compileTestKotlinIosSimulatorArm64
```
