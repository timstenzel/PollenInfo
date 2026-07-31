# Tasks: Onboarding Station Selection

> Derived from `.sdd/onboarding-station-selection/requirements.md`, verified against the codebase on
> 2026-07-31. Implement in numeric order — the ordinal is the dependency order.
>
> Two decisions refine the Technical Annex, both agreed during task review:
> - `OnboardingUiState.Error` carries the exception with a derived `message` default, matching the
>   existing `ExampleUiState.Error` convention rather than the annex's plain `Error(message)`.
> - There is **no** `expect fun dataStorePath()`. Each platform actual in `platformModule` constructs
>   the `DataStore<Preferences>` itself, because the factory needs `Dispatchers.IO`, which does not
>   exist in `commonMain`. The common code depends only on `DataStore<Preferences>`.
>
> Environment: `JAVA_HOME`, `java`, `adb` and `emulator` come from `~/.zshrc`; no export prefix is
> needed. (This note previously claimed there was no JDK on `PATH` — that was stale.)

---

## Task [01-station-list-from-backend]

The first slice that talks to our own backend. A freshly installed app opens on the onboarding screen,
which retrieves the 15 measuring stations from the locally running Ktor server and offers them in a
read-only dropdown, alphabetically by name, showing names only. When the retrieval fails the screen
shows a message and a retry action instead of the dropdown. There is no confirm action yet — selecting
a station only updates the dropdown.

This slice establishes the real backend address, which the app has never had: the reference feature
points at a fictional documentation host.

### Implementation steps

- [x] Introduce the backend base address as a platform-provided value (`10.0.2.2` for the Android
      emulator, `localhost` for the iOS simulator) with actuals for both platforms, and inject it into
      the API service through Koin rather than referencing it as a top-level constant, so tests can
      point a mock engine at any host.
- [x] Allow unencrypted traffic in **debug builds only**, via a new debug source-set manifest, so a
      release build can never permit it.
- [x] Build the station retrieval path: wire DTO matching the server's station payload, a mapper to a
      domain model that sorts alphabetically by name, an API service, and a repository returning the
      project's own `Result` type via `safeCall`. Add a comment on the sort explaining why a plain
      comparison is correct and a JVM-only collator must not be introduced.
- [x] Add the onboarding UI state (loading / content / error, with the error carrying the exception as
      the existing reference screen's state does) and a view model that loads on init, exposes retry,
      and records the selected station.
- [x] Build the onboarding screen: no title bar, welcome headline and subtitle, and a read-only
      dropdown labelled `Station`. Full-screen message plus retry action in the error state.
- [x] Register the onboarding destination and make it the start destination. **Keep the existing
      reference destination registered** so that feature remains reachable and unchanged in function.
- [x] Write the tests, and update the project documentation with the backend base address convention,
      the debug-only cleartext manifest, and the iOS transport-security note the future iOS app wrapper
      will need for a cleartext development backend. Correct the reference API service's comment that
      says to point a real feature at our own backend, now that one does.

> Note: the debug manifest lives at `composeApp/src/debug/AndroidManifest.xml`, **not**
> `src/androidDebug/`. Despite `./gradlew :composeApp:sourceSets` reporting the latter as the debug
> manifest path in this KMP + AGP setup, only `src/debug/AndroidManifest.xml` is actually merged —
> verified against the merged manifest both ways.

### Acceptance criteria

- [x] With the backend running locally, launching the app on an Android emulator shows the onboarding
      screen, and the dropdown lists all 15 stations with Basel first and Zürich last.
- [x] An automated test proves the mapper's ordering, including that `Lausanne` precedes
      `Locarno / Monti` — the case the server's own ordering gets wrong — and that accented names sort
      where expected.
- [x] Automated tests drive the repository through a real HTTP client backed by a mock engine and show:
      a valid payload yields success with all 15 stations and every field mapped; HTTP 500 yields
      failure; malformed JSON yields failure; a payload containing an unknown extra field still
      succeeds.
- [x] With the backend stopped, the screen shows the error state with a retry action; starting the
      backend and using retry loads the list.
- [x] Each dropdown row shows the station name only — no canton, coordinates or altitude.
- [x] The merged manifest of a release build contains no cleartext-traffic permission, while the debug
      build's does.
- [x] The project documentation describes the backend base address convention, the debug-only cleartext
      manifest, and the iOS transport-security requirement.

### Quality gates

- [x] `./gradlew :composeApp:testDebugUnitTest :server:test` passes.
- [x] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes.
- [x] No `java.*`, `String.format`, `UUID` or `SimpleDateFormat` reference is introduced in
      `commonMain`.
- [x] Every file using the result type imports it explicitly from the project's own result package; no
      use of `runCatching`.
- [x] No station name, abbreviation list or station coordinate is hardcoded in `commonMain`,
      `androidMain` or `iosMain` — test sources are exempt, since fixtures legitimately contain them.
- [x] DTOs remain confined to the feature's data layer and are not referenced from domain or
      presentation code.

---

## Task [02-confirm-and-persist-station]

The selection becomes durable and the flow gets an exit. A confirm action, available only once a
station is selected, stores the station's abbreviation and display name on the device, then moves the
user to a placeholder main screen that displays the stored station's name. A failed write keeps the
user on the onboarding screen with an inline message and does not navigate.

Completion is modelled as a one-shot event rather than a state flag, because a state flag standing for
a one-time action re-fires on any state re-emission or recomposition. This is a pattern the project
does not yet have, so it needs a comment saying why.

### Implementation steps

- [x] Add the preferences-storage dependency to the version catalog.
- [x] Introduce the platform Koin module (with actuals for both platforms) and register it alongside
      the existing modules. The Android actual obtains the application context from Koin; each actual
      constructs the preference store itself, including its own file path and IO dispatcher.
- [x] Add a persisted-selection repository in the shared core layer, exposing the stored station as an
      observable stream that emits nothing-selected when absent, and a suspending write returning the
      project's `Result` type.
- [x] Add the confirm action to the onboarding screen, enabled only when a station is selected.
- [x] Emit a one-shot completion event on a successful write, collected by the screen and surfaced to
      navigation as a completion callback; on a failed write, set an inline error under the confirm
      action and emit nothing.
- [x] Add the placeholder main destination and screen, which reads the stored selection directly and
      displays its name, with a comment marking it as a stand-in for the real dashboard. Navigate to
      it such that the back gesture leaves the app rather than returning to onboarding.
- [x] Write hand-written in-memory fakes and the tests, and document the persisted-selection location,
      the platform module and the one-shot event pattern in the project documentation.

> Note: the iOS actual uses `Dispatchers.Default`, not `Dispatchers.IO` — the latter is `internal`
> in kotlinx-coroutines on Kotlin/Native and does not compile there. The Android actual uses
> `Dispatchers.IO` as intended. This is exactly the kind of per-platform difference the platform
> module exists to absorb; documented in `CLAUDE.md`.

### Acceptance criteria

- [x] The confirm action is unavailable when the screen loads and becomes available once a station is
      selected — verified by an automated view-model test and observable at runtime.
- [x] Confirming a selection leaves onboarding and shows the placeholder main screen displaying the
      selected station's name.
- [x] From the placeholder main screen, the device back gesture leaves the app and does not return to
      onboarding.
- [x] An automated test shows the view model emits its completion event exactly once for a successful
      write, and that the stored value contains both the station's abbreviation and its display name.
- [x] An automated test shows that when the write fails, no completion event is emitted and the state
      carries the save-failure flag; a static check shows the message
      `Could not save your selection. Please try again.` appears exactly once in the source.
- [x] The project documentation describes the persisted-selection location, the platform Koin module
      and the one-shot event pattern, including why it is not a state flag.

### Quality gates

- [x] Full test suite and iOS compile check pass.
- [x] Completion is delivered through a one-shot channel; the UI state contains no boolean standing
      for "already navigated".
- [x] Test doubles are hand-written; no mocking framework is added to the project.
- [x] No `java.*` or other JVM-only API is introduced in `commonMain`; the storage factory and its
      dispatcher live in the platform actuals.
- [x] The persisted-selection implementation contains no logic beyond reading and writing its two keys,
      since it is deliberately not unit-tested.

---

## Task [03-skip-onboarding-for-returning-users]

Returning users never see onboarding. On launch the app resolves whether a station is already stored
and builds its navigation graph only once that answer is known, so onboarding is never briefly visible
to someone who has already completed it. While resolving, the app shows an empty themed surface rather
than a spinner, because the read takes milliseconds and a one-frame spinner reads as a glitch.

### Implementation steps

- [x] Add a startup state holder in the shared core layer exposing resolving / needs-onboarding / ready,
      derived from the first value of the persisted selection, and register it for injection.
- [x] Restructure the app's root composable to collect that state and construct the navigation graph
      only after it resolves, choosing the start destination accordingly.
- [x] Render an empty themed surface while the state is unresolved.
- [x] Write the tests and note the startup gate in the project documentation.

### Acceptance criteria

- [x] An automated test shows the startup state resolves to ready when a station is stored, to
      needs-onboarding when none is, and that the resolving state is observable first.
- [x] After completing onboarding, closing and reopening the app shows the placeholder main screen
      directly; onboarding does not appear.
- [x] A static reading of the root composable confirms the navigation graph is not composed while the
      startup state is unresolved — the checkable form of "no onboarding flash".
- [x] With no station stored, launching the app shows onboarding.
- [x] The project documentation describes the startup gate.

### Quality gates

- [x] Full test suite and iOS compile check pass.
- [x] The startup state holder is registered in the common Koin module and injected, not constructed
      inline in a composable.
- [x] No navigation-graph destination exists for the resolving state — it is not modelled as a
      navigable screen.

---

## Task [04-nearest-station-from-location]

The location shortcut, covering every outcome it can produce. A `Use my location` action above the
dropdown requests approximate location, resolves a coarse fix and fills the dropdown with the nearest
of the 15 stations, which the user still confirms — the shortcut proposes, it never commits. A declined
permission, an unavailable position and a timeout are each mapped to one of exactly two inline error
states.

Both platform implementations are written; only the Android one can be executed, because the project
has no iOS app wrapper. The permission request lives in the composition, because Android's request
needs an activity-scoped launcher that an injected class holding only the application context cannot
provide.

### Implementation steps

- [x] Declare approximate-location permission in the main manifest. Do not declare precise location.
- [x] Add an explicit dependency on the Android activity-compose artifact rather than relying on it
      being present transitively.
- [x] Introduce a composable permission requester with actuals for both platforms, short-circuiting
      when permission is already granted. Verify it compiles for the iOS simulator target **before**
      building the rest of this task; if the composable `expect` proves incompatible with the Compose
      compiler plugin, fall back to a non-composable factory returning a small requester object.
- [x] Introduce the coarse-location abstraction: a suspending call returning success-with-coordinates,
      permission-denied or unavailable. Implement the Android actual on the platform location manager's
      network provider — no Play Services, no GPS fallback — and the iOS actual on the platform
      location manager with reduced accuracy, both bridging callbacks through a cancellable
      continuation so cancellation reaches the platform. Catch the platform exceptions for a missing
      provider and for permission revoked between check and call, mapping them to unavailable.
- [x] Apply the 10-second timeout in common code as a single constant, mapping expiry to unavailable.
- [x] Add the nearest-station calculation as a pure use case over the already-loaded station list:
      great-circle distance using multiplatform maths only, first-match tie-break, nothing-found for an
      empty list, and no distance cap.
- [x] Wire the view model: request permission, map a denial to the permission-denied error, run the
      lookup while exposing an in-progress flag, map success to a filled-in selection, and map
      unavailable and timeout to the could-not-determine error.
- [x] Wire the screen: the location action above the dropdown, showing a progress indicator and
      disabled while in progress, with the inline error text directly below it in the error colour.
- [x] Write the tests, and document the location abstraction plus the platform configuration keys the
      future iOS app wrapper needs (usage description and reduced-accuracy default).

> Notes from implementation:
> - The composable `expect`/`actual` **does** work with the Compose compiler plugin here; the
>   fallback was not needed (verified with `compileTestKotlinIosSimulatorArm64` before anything else
>   was built).
> - `LocationManagerCompat.getCurrentLocation`, not `LocationManager.getCurrentLocation`: the
>   framework method only exists from API 30 and `minSdk` is 26. That makes `androidx.core:core-ktx`
>   a second explicit `androidMain` dependency alongside activity-compose.
> - The timeout is applied by `OnboardingViewModel`, not by a decorator around the provider, so a
>   fake that never answers exercises it under virtual time in `commonTest`.

### Acceptance criteria

- [x] Automated tests of the nearest-station calculation show: coordinates near Winterthur resolve to
      the Zürich station; coordinates in Valais resolve to Sion; coordinates in Germany still resolve
      to a Swiss station with no error; an exact tie resolves to the first entry; an empty list resolves
      to nothing.
      *(`FindNearestStationUseCaseTest`, 7 tests, all passing.)*
- [x] Automated view-model tests show a successful lookup fills the selection without completing
      onboarding, that the in-progress flag is observable while the lookup runs, that a denial produces
      the permission-denied error, and that unavailable and timeout both produce the
      could-not-determine error — no outcome leaves the state unchanged.
      *(`OnboardingViewModelTest`, 28 tests, all passing — 10 of them new for the location path.)*
- [x] On an Android emulator with the backend running and a position injected via the emulator's
      location control, granting the permission fills the dropdown with a station and does not navigate
      automatically. If the emulator image exposes no network location provider, the run instead shows
      the could-not-determine message — record which occurred.
      **Recorded: the second outcome.** `Medium_Phone_API_36.1` reports a `network provider` whose
      state is `enabled=false, last location=null` (its GMS network location provider is inert), so
      `adb emu geo fix 8.7286 47.4989` never reaches it and the lookup returns no fix. Granting
      "approximate location" showed `Your location could not be determined. Please pick a station
      manually.` and the app stayed on onboarding. A shell test provider was not usable either:
      `cmd location providers add-test-provider` needs `MOCK_LOCATION`, which cannot be granted to
      `uid shell`. The success path is therefore covered by automated tests only.
- [x] The location action is disabled and shows progress while a lookup is in flight.
      *(View-model test `the in-progress flag is set while the lookup runs and cleared when it ends`
      drives exactly the flag the button binds to. The spinner itself was **not** visually observable
      on the emulator: with no working network provider the lookup resolves in well under 300 ms, and
      four tap-then-screenshot attempts caught only the settled state.)*
- [x] The manifest declares approximate location only; a static check confirms no precise-location
      permission and no Play Services dependency anywhere in the module graph.
      *(Merged manifests, debug and release: `INTERNET` + `ACCESS_COARSE_LOCATION`, no
      `ACCESS_FINE_LOCATION`. `:composeApp:dependencies --configuration debugRuntimeClasspath`:
      0 matches for `play-services` / `com.google.android.gms` across 1199 lines. The system prompt
      on the device read "access this device's **approximate** location".)*
- [ ] A static check confirms the app sends no coordinates to any server: the station API service is
      the only API service and takes no coordinate parameters.
      *(**Not true as written.** The substance holds: the only consumer of a `CoarseLocationResult` is
      `OnboardingViewModel`, which hands it to the pure `FindNearestStationUseCase` — no coordinate the
      user's device produces reaches any repository or API service, and `StationApiService.getStations()`
      takes no parameters. But `StationApiService` is not the only API service: the pre-existing
      `feature/example` reference slice has `ExampleApiService.getSnapshot(latitude, longitude)`, which
      sends coordinates as query parameters. They are hardcoded fictional constants in
      `ExampleViewModel` aimed at the fictional host `api.example.com`, never the user's position, and
      the screen is unreachable — but the criterion's literal claim is false and this feature may not
      change the reference slice.)*
- [x] The project documentation describes the location abstraction and lists the iOS configuration keys
      required by the future app wrapper.
      *(`CLAUDE.md`: new `### Location` section; `NSLocationWhenInUseUsageDescription` and
      `NSLocationDefaultAccuracyReduced` added to the iOS wrapper table.)*

### Quality gates

- [x] Full test suite and iOS compile check pass.
      *(`:composeApp:testDebugUnitTest :server:test --rerun-tasks` → BUILD SUCCESSFUL, 149 tests,
      0 failures. `:composeApp:compileTestKotlinIosSimulatorArm64 --rerun-tasks` → BUILD SUCCESSFUL.)*
- [x] The platform location implementations use cancellable continuations, so cancelling the calling
      coroutine stops the platform request.
      *(Both actuals use `suspendCancellableCoroutine` + `invokeOnCancellation`: Android cancels a
      `CancellationSignal`, iOS calls `stopUpdatingLocation()` and drops the delegate.)*
- [x] No platform API is referenced from `commonMain` or from the view model; the view model is
      exercised in tests through a hand-written fake location source.
      *(No `import java.` / `import android.` / `import platform.` anywhere in `commonMain`; the view
      model imports only `androidx.lifecycle`, project packages and `kotlinx.coroutines`. Tests drive
      it through `FakeCoarseLocationProvider`, hand-written like the other fakes.)*
- [x] The timeout exists as one constant in `commonMain`, not duplicated per platform.
      *(`LOCATION_TIMEOUT` is declared once in `core/location/CoarseLocationProvider.kt`; no duration
      or `withTimeout` appears in `androidMain` or `iosMain` at all.)*
- [x] The exhaustive handling of the location outcome type has no branch that silently does nothing.
      *(The `when (result)` has three branches, each returning a changed state — including the
      empty-list sub-case, which reports UNAVAILABLE rather than ending silently. No `else ->`,
      `-> {}` or `-> Unit` anywhere in the file.)*

---

## Task [05-location-error-recovery]

The failure paths become forgiving rather than merely correct. An error message never outlives its
cause: it clears the moment the user retries or picks a station manually. A denial does not disable the
shortcut, since the device may still allow a prompt. A lookup in progress never blocks the manual path,
and — the subtle case — a fix that lands *after* the user has already picked a station manually must not
overwrite that choice, which would silently select a station the user did not pick.

### Implementation steps

- [ ] Clear the location error when a lookup is retried and when a station is picked from the dropdown.
- [ ] Keep the location action enabled after a denial, so a device that still permits a prompt can
      grant the permission without leaving the app.
- [ ] Keep the dropdown and the confirm action usable while a lookup is in progress.
- [ ] Make a late-arriving location result yield to an existing manual selection instead of replacing
      it.
- [ ] Extend the tests to cover clearing, the post-denial retry, the timeout under virtual time, and
      the late-result race.
- [ ] Verify the two failure states on a device: deny the permission, then switch location services off.

### Acceptance criteria

- [ ] Denying the permission shows one line of text below the location action, and a static check
      confirms the exact wording
      `Location permission is off. Enable it in Settings to find your nearest station.` appears exactly
      once in the source.
- [ ] With the permission granted and location services switched off, the text below the action is
      exactly `Your location could not be determined. Please pick a station manually.`, and the
      dropdown still opens and can be used.
- [ ] An automated test using virtual time — not a real-time sleep — shows a source that never returns
      produces the could-not-determine error once the timeout elapses.
- [ ] Automated tests show the error clears both when a station is picked manually and when the
      location action is used again, and that the location action remains enabled after a denial.
- [ ] An automated test shows a successful lookup arriving after a manual pick leaves the manual
      selection in place, and that confirming during an in-flight lookup persists the manually picked
      station.
- [ ] The state model carries exactly two location error variants; a static check finds no third
      location error message in the app.

### Quality gates

- [ ] Full test suite and iOS compile check pass.
- [ ] No test in the suite sleeps in real time; the timeout test advances virtual time.
- [ ] Every user-visible message introduced by this feature exists exactly once in the source, so
      wording cannot drift between duplicated literals.
- [ ] All app tests added by tasks 01–05 live in the common test source set, none in the
      Android-only test source set.
