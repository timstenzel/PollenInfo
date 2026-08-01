# Tasks — Home Pollen Overview

Derived from `.sdd/home-pollen-overview/requirements.md`, verified against the codebase on
2026-08-01. Tasks are in topological order; the ordinal is the implementation order.

Documentation updates are distributed into the task that falsifies each statement rather than
collected into a trailing documentation task — a statement should never be wrong on `main`.

**One deviation from the Technical Annex, decided during task review:** AD-12 sketches the screen
state with the station name only on the content variant, but the requirements demand the station
name be visible while readings are still loading (UAT 2), and AD-14 makes it available precisely so
that it can be. The state is therefore a sealed interface declaring the station name as a property
that all three variants carry.

---

## Task [01-measurements-from-fixture]

The walking skeleton: a real pollen reading travels from a published MeteoSwiss file to the phone
screen, end to end, with the file supplied from a checked-in fixture rather than the network.

The backend gains an endpoint that reads a station's current-day hourly file, decodes it in the
encoding the publisher actually uses, parses the most recent usable row, classifies every
concentration against the existing threshold table, and returns all seven pollen types — including
the ones that station does not report, distinguished from a reading of zero. The app gains a home
slice that calls it and replaces the placeholder screen with the station name and the single overall
severity.

The seam where the file comes from is an interface from the start; the next task supplies the
network implementation behind it. Everything else — the parser, the classification, the wire
contract, the whole app slice — is finished here, because splitting those would mean splitting by
layer.

Deliberately excluded: no caching, no species list, no bars or colour, no age line, no
pull-to-refresh. The wire contract, however, is settled here in full, since every consumer
downstream depends on its shape:

```json
{ "stationAbbr": "PZH", "measuredAt": "2026-08-01T09:00:00Z", "unit": "grains/m3",
  "species": [ { "id": "BIRCH", "name": "Birch", "latinName": "Betula",
                 "concentration": 42, "severity": "MODERATE" },
               { "id": "ASH", "name": "Ash", "latinName": "Fraxinus",
                 "concentration": null, "severity": null } ] }
```

Note on what the endpoint does when reading the file fails: until the caching task, that surfaces as
a generic server error rather than the gateway error the annex specifies. That is accepted for now;
the app reaches its error state either way.

### Implementation steps

- [x] Define the upstream-file seam as an interface returning the raw bytes of a station's
      current-day hourly file, and provide a fixture-backed implementation reading checked-in sample
      files. Check the fixtures in, including one containing Latin-1 encoded non-ASCII characters.
- [x] Add a parser that decodes those bytes as ISO-8859-1, splits on semicolons, maps column headers
      to pollen types via the existing species vocabulary, parses the `dd.MM.yyyy HH:mm` timestamps
      as UTC, selects the most recent row in which at least one type has a value, and reports an
      absent column or empty cell as "no reading".
- [x] Add a measurement service composing the file source, the parser and the existing threshold
      table into a classified reading.
- [x] Add the wire model and the endpoint, passing the service into routing configuration the same
      way the threshold table already is, so route tests can install their own.
- [x] Add the app's home data layer — API service taking its base URL by injection, wire models,
      mapper, and a repository returning the project's own result type — plus the domain models and
      the app-side severity enum.
- [x] Add a use case computing the overall severity as the maximum across pollen types that have a
      reading.
- [x] Add a home view model that observes the stored station selection and reloads when it changes,
      exposing loading / content / error state with the station name available in all three.
- [x] Replace the placeholder home screen with a top bar carrying a decorative location pin and the
      station name, and content showing the overall severity word. Confirm the pin glyph resolves
      from the icons already available transitively; add the extended icon set only if it does not.
      *(It does not: `Icons.Default.LocationOn` resolves for the Android target through material3
      and fails on `compileKotlinIosSimulatorArm64`, so `compose.materialIconsExtended` was added to
      `commonMain`.)*
- [x] Register the new collaborators in dependency injection.
- [x] Update the project documentation: add the measurements endpoint to the endpoint table, record
      that the severity bands are defined over daily means but applied here to hourly readings so
      severities skew high, and remove every description of the home screen as a placeholder with no
      view model and no network access.

### Acceptance criteria

- [x] Requesting measurements for a known station returns a successful response carrying the station
      abbreviation, the reading's timestamp, the unit and exactly seven pollen types; a type the
      station does not report has no concentration and no severity, distinguishable from a reported
      zero, which is present with a severity of "none".
      *(`PollenRoutesTest`: "measurements endpoint returns the station the timestamp the unit and
      seven taxa", "an unmeasured taxon is distinguishable from one measuring zero". Live: `GET
      /pollen/stations/PZH/measurements` → 200 with `measuredAt` `2026-08-01T07:00:00Z`, unit
      `grains/m3`, seven taxa.)*
- [x] Requesting an unknown station abbreviation returns "not found", as does requesting a station
      whose file contains no usable row.
      *(`PollenRoutesTest`: "measurements for an unknown station return 404", "a station whose file
      holds no usable row returns 404". Live: `/pollen/stations/XXX/measurements` → 404.)*
- [x] Decoding is exercised directly on a fixture containing Latin-1 encoded non-ASCII characters
      and yields the exact expected text — a test that would fail if the bytes were read as UTF-8.
      *(`PollenCsvParserTest`: "decodes the publisher's Latin-1 bytes rather than reading them as
      UTF-8" — asserts `Münsterlingen` from a fixture holding the single byte `0xFC`, and asserts no
      replacement character, which is what a UTF-8 read would substitute.)*
- [x] The parser selects the most recent row containing at least one value, reports "no reading" for
      cells empty in that row, and yields the row's timestamp interpreted as UTC.
      *(`PollenCsvParserTest`: "a row of nothing but empty cells is not the latest reading", "the
      most recent row is chosen by timestamp not by position in the file", "an empty cell in the
      chosen row reads as no measurement not as zero", "timestamps are read as UTC".)*
- [x] Every species' moderate, high and very-high bound is classified correctly at the bound and at
      one below it; grasses and a tree at the same concentration receive different severities.
      *(`MeasurementServiceTest`: "every band bound classifies correctly at the bound and
      immediately below it" — all 7 taxa × 3 bounds × 2 sides; "classifies each taxon against its
      own bands" — 20 grains/m³ is MODERATE for birch and HIGH for grasses.)*
- [x] All five severity wire strings map to their app-side enum constants, and an unrecognised
      string fails the mapping rather than silently defaulting.
      *(`StationMeasurementMapperTest`: "every severity constant maps to its exact wire string",
      "the wire strings are the ones the server sends", "an unrecognised severity fails the mapping
      rather than defaulting".)*
- [x] The app's repository returns a failure result — never a thrown exception — for not-found,
      server-error and malformed-response cases.
      *(`StationMeasurementRepositoryImplTest`: Failure for 404, 500, 502, malformed JSON, missing
      `measuredAt`, unknown severity string, and a thrown connection error.)*
- [x] The home screen shows the selected station's name beside a location pin — including while the
      readings are still loading — and below it the overall severity, which equals the highest
      severity among types that have a reading; changing the stored station selection reloads the
      readings.
      *(`HomeViewModelTest`: "names the station while the readings are still loading", "names the
      station on every state including the failure", "resolves to Content with the overall
      severity", "reloads when the stored station changes". Observed on the emulator against the
      running backend: pin + "Zürich" in the top bar with "Low" below — the backend reported
      grasses 3 (LOW) and every other taxon 0 (NONE) — and the same top bar over the error state
      with the backend stopped.)*

### Quality gates

- [x] `./gradlew :composeApp:testDebugUnitTest :server:test` passes.
      *(BUILD SUCCESSFUL — server 79 tests, composeApp 143 tests, 0 failures.)*
- [x] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` succeeds.
- [x] Compiling the touched modules from clean emits no Kotlin compiler warnings originating in
      files this task adds or changes.
      *(`--rerun-tasks` over `:server:compileKotlin`, `:server:compileTestKotlin`,
      `:composeApp:compileDebugKotlinAndroid`, `:composeApp:compileDebugUnitTestKotlinAndroid`,
      `:composeApp:compileTestKotlinIosSimulatorArm64`: the only `w:` line is the pre-existing
      opt-in warning in `OnboardingViewModelTest.kt`, which this task does not touch.)*
- [x] No MeteoSwiss address appears anywhere in the app module's sources.
      *(Every URL under `composeApp/src/` is an Android schema URL, the RFC 2606 host in
      `feature/example`, or a localhost/emulator backend address.)*
- [x] The new repository uses the project's `safeCall` helper with an explicit import of the
      project's own `Result`; no use of `runCatching` or the standard library's `Result`.
- [x] Wire models stay inside the feature's data layer and do not appear in domain or presentation
      code.
- [x] Backend classification uses the existing threshold table rather than re-deriving bounds.
      *(`MeasurementService` calls `thresholds.severityOf(species, concentration)`; no band bound
      appears anywhere under `pollen/measurement/` or `pollen/upstream/`.)*

---

## Task [02-live-upstream-fetch]

The reading on screen becomes today's actual air rather than a checked-in sample.

The fixture-backed file source is joined by one that fetches over HTTP from the public data service,
and that becomes the wiring the application runs with. This is the only slice that touches the real
upstream, and it is deliberately small, because it is where the genuinely unverifiable-by-unit-test
risks live: the base address, the per-station path convention, and the published column layout.

That risk is why this task carries a runtime check against the live service. The station path
properties on the existing station type have never had a caller, so nothing has ever confirmed they
are right.

### Implementation steps

- [ ] Add an HTTP client to the backend's main source set and a mock engine to its test source set;
      the backend currently has neither. Both are already in the version catalog.
- [ ] Add an HTTP implementation of the file source, taking its client and base address as
      constructor parameters so tests can drive it against a mock engine.
- [ ] Wire the HTTP implementation into the running application, leaving the fixture-backed one for
      tests.
- [ ] Update the architecture overview in the project documentation to describe on-demand fetching
      rather than scheduled hourly polling, and document the new backend dependencies.

### Acceptance criteria

- [ ] The file source requests the path derived from the station's own abbreviation, verified
      against a mock engine for more than one station.
- [ ] An upstream response that is not a success surfaces as a failure from the file source rather
      than as empty or partial content.
- [ ] The running application resolves the HTTP implementation, and the fixture-backed one is no
      longer reachable from production wiring.

### Quality gates

- [ ] `./gradlew :composeApp:testDebugUnitTest :server:test` passes.
- [ ] With the backend running, requesting measurements for a real station returns a successful
      response whose timestamp is from today and which contains at least one non-null concentration
      — confirming the base address, the station path convention and the published column layout
      against the live service.
- [ ] The app, run against the local backend, displays a severity for the selected station that
      matches the response observed above.
- [ ] Compiling the touched modules from clean emits no Kotlin compiler warnings originating in
      files this task adds or changes.

---

## Task [03-species-list]

The screen stops summarising and starts informing: every pollen type the network measures is listed,
worst first, with its own severity and its measured concentration.

Types with no reading sink to the bottom rather than being hidden — a user who reacts to ash needs
to see that ash is unmeasured at their station, not to find ash missing from the list.

Still no bars and no colour; the severity is a word. Those arrive next.

### Implementation steps

- [ ] Extend the use case to order pollen types by severity descending, placing types with no
      reading last, with alphabetical ordering by display name as the tie-break within each group.
- [ ] Carry the ordered list and the name of the type responsible for the overall severity in the
      screen's content state.
- [ ] Render each type as a two-line row: its name, then its severity word and concentration.
- [ ] Render a type with no reading as "No data" with a dash in place of the number.
- [ ] State the unit once as a heading above the list rather than on every row.
- [ ] Name the responsible pollen type beneath the overall severity.

### Acceptance criteria

- [ ] All seven pollen types measured by the network appear in the list.
- [ ] The list is ordered by severity descending; two types of equal severity appear alphabetically;
      types with no reading appear last and alphabetically among themselves.
- [ ] A type with no reading shows "No data" and a dash rather than a severity word and a number.
- [ ] The overall severity is unaffected by types that have no reading.
- [ ] The unit of measurement appears exactly once, as a heading above the list.
- [ ] The text beneath the overall severity names the type holding the highest severity.

### Quality gates

- [ ] `./gradlew :composeApp:testDebugUnitTest :server:test` passes.
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` and
      `./gradlew :composeApp:assembleDebug` succeed.
- [ ] Ordering and aggregation are covered by tests that exercise them without instantiating any UI
      component.
- [ ] The list is observed on a running device: seven rows present, the unit heading appearing once,
      and the unmeasured type rendering as "No data" with a dash.
- [ ] Compiling the touched modules from clean emits no Kotlin compiler warnings originating in
      files this task adds or changes.

---

## Task [04-severity-bars]

Severity becomes readable at a glance rather than only by reading.

The overall severity and every list row gain a horizontal bar that fills further and changes colour
as severity rises. The whole bar takes a single colour — the current severity's — so a bad reading
is one unambiguous signal rather than a gradient showing reassurance and warning simultaneously.

The fill has five fixed stops and does not track the concentration, because the bands are wildly
unequal in width and an interpolated bar would visibly contradict its own label. A type with no
reading is the only thing that renders as an empty bar, which is why the calmest real severity still
fills a fifth of the track.

| Severity | Fill | Colour |
| --- | --- | --- |
| None | 20 % | neutral grey |
| Low | 40 % | green |
| Moderate | 60 % | amber |
| High | 80 % | orange |
| Very high | 100 % | red |
| no reading | 0 %, muted track | — |

### Implementation steps

- [ ] Add severity colour values to the shared theme module with explicit light and dark variants —
      these sit outside the Material scheme, so no dark equivalents are generated for them — and
      record each value's computed contrast ratio against its scheme's surface alongside it.
- [ ] Add the severity-to-colour mapping in the home feature, not the theme module, since the theme
      module cannot depend on the app and therefore cannot see the severity type.
- [ ] Add a bar component taking a severity and rendering the fixed fill and single colour, with a
      distinct muted empty rendering for "no reading".
- [ ] Use the bar at a large size for the overall severity and compactly in each list row.
- [ ] Document the theme module's severity palette in the project documentation.

### Acceptance criteria

- [ ] Each severity fills the bar to its fixed proportion — one fifth for "none" rising to full for
      "very high" — and a type with no reading renders an empty, visibly muted track.
- [ ] The entire filled portion of a bar is a single colour corresponding to the current severity;
      no bar shows more than one colour.
- [ ] The colour mapping is the one tabulated above: grey, green, amber, orange, red.
- [ ] Distinct values exist for all five severities in both light and dark themes.
- [ ] Every bar is accompanied by its severity word, so severity is never conveyed by colour alone.

### Quality gates

- [ ] `./gradlew :composeApp:testDebugUnitTest :server:test` passes.
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` and
      `./gradlew :composeApp:assembleDebug` succeed.
- [ ] Every one of the ten colour values has a computed contrast ratio of at least 3:1 against its
      scheme's surface colour, recorded next to the value.
- [ ] No file in the theme module references the severity type.
- [ ] The screen is observed on a running device in both light and dark theme and every bar reads
      clearly against its background.
- [ ] Compiling the touched modules from clean emits no Kotlin compiler warnings originating in
      files this task adds or changes.

---

## Task [05-reading-age]

The screen stops implying its data is current and starts saying how current it is.

A reading taken within the last few hours is labelled with its local time as an unobtrusive caption.
An older one escalates to a prominent warning naming the reading's date. This is what allows the
backend, in the next task, to keep serving its last known reading through an upstream outage without
that resilience becoming deception — the two are a pair, and this half comes first.

### Implementation steps

- [ ] Add a multiplatform date-time library to the version catalog and to the app's shared source
      set; the app has none today, and the platform date library is unavailable in shared code.
- [ ] Add a pure conversion from the reading's timestamp and the current instant to either a fresh
      local time or a stale local date, with the boundary at three hours.
- [ ] Carry the reading's timestamp through to the screen's content state.
- [ ] Render the fresh case as a caption beneath the top bar and the stale case as a visually
      prominent warning.
- [ ] Document the new dependency in the project documentation.

### Acceptance criteria

- [ ] A reading less than three hours old is presented as an update time in the device's local time
      zone, not the source's zone.
- [ ] A reading more than three hours old is presented as a warning naming the reading's date, and
      is visually distinct from the fresh caption rather than differing only in wording.
- [ ] The three-hour boundary behaves correctly when tested immediately on either side of it.

### Quality gates

- [ ] `./gradlew :composeApp:testDebugUnitTest :server:test` passes.
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` succeeds — mandatory here, since a
      new dependency enters shared code and the Android build alone will not catch non-portable use.
- [ ] The age conversion is verified without instantiating a view model or any UI component.
- [ ] Shared code contains no platform-specific date or time API, and no use of the JVM-only string
      formatting, UUID or legacy date-format types.
- [ ] Compiling the touched modules from clean emits no Kotlin compiler warnings originating in
      files this task adds or changes.

---

## Task [06-caching-resilience]

The backend stops fetching on every request and starts degrading gracefully.

Fetched readings are cached for a period, so many people checking the same station produce one
upstream request rather than many, and simultaneous first-time requests for one station collapse
into a single fetch. When a fetch fails and a previous reading exists, that reading is served with
its original timestamp — never refreshed to look current — which the previous task's age warning
then surfaces honestly. When no previous reading exists, the request fails clearly.

The cache is built as a general-purpose component with an injected clock, because expiry,
deduplication and stale retention are the trickiest logic in the feature and have nothing to do with
pollen; testing them through pollen-shaped fixtures would be incidental complexity, and testing them
against a real clock would mean sleeping.

### Implementation steps

- [ ] Add a general-purpose time-to-live cache with an injected clock, distinguishing three
      outcomes: a fresh value, a retained previous value after a failed load, and outright failure.
- [ ] Give it per-key locking so concurrent misses on one key produce exactly one load without
      blocking loads for other keys.
- [ ] Preserve the original entry timestamp when a reload fails, so a failed refresh cannot make a
      stale reading appear current.
- [ ] Wire the cache into the measurement service with a thirty-minute period.
- [ ] Map the three outcomes onto responses: fresh and retained both succeed; outright failure
      returns a gateway error.
- [ ] Complete the architecture overview in the project documentation with the caching behaviour and
      its period, noting that scheduled polling becomes the right shape when push notifications
      require severities for stations nobody is viewing.

### Acceptance criteria

- [ ] A second request for the same station within the cache period causes no further contact with
      the upstream source and returns the same reading.
- [ ] A request after the cache period has elapsed causes a fresh fetch.
- [ ] Simultaneous first-time requests for one station result in exactly one upstream fetch, while a
      slow fetch for one station does not delay a request for a different station.
- [ ] When an upstream fetch fails and a previous reading exists, the request succeeds and returns
      that reading with its original timestamp, unchanged by the failed reload.
- [ ] When an upstream fetch fails and no previous reading exists, the request returns a gateway
      error.

### Quality gates

- [ ] `./gradlew :composeApp:testDebugUnitTest :server:test` passes.
- [ ] No test sleeps or otherwise depends on real elapsed time; all time-dependent behaviour is
      driven by the injected clock.
- [ ] The cache component is tested through its own interface, with no pollen-specific types in its
      tests.
- [ ] With the backend running and its upstream address pointed at an unreachable host, the app is
      observed to show the previously fetched reading together with the stale-age warning; with no
      reading ever fetched under the same conditions, it is observed to show its error state.
- [ ] Compiling the touched modules from clean emits no Kotlin compiler warnings originating in
      files this task adds or changes.

---

## Task [07-refresh-and-recovery]

The user gains control: they can ask for fresher data, and they can get out of an error state
without restarting the app.

Pulling down refreshes, keeping the existing readings on screen throughout so the display never
blanks. An error offers a retry. The unreachable-but-representable case of arriving at the screen
with no station stored resolves to an error with a message rather than an indefinite spinner.

One accepted behaviour, made explicit rather than treated as a defect: refreshing within the
backend's cache period legitimately returns the same reading and leaves the age unchanged. The pull
is not inert — it re-queries the backend and picks up a new reading as soon as the period has rolled
— but it does not force the backend to re-contact the public service.

### Implementation steps

- [ ] Add pull-to-refresh to the home screen, flagging the refresh in the content state so existing
      readings stay visible while it runs.
- [ ] Add a retry action to the error state.
- [ ] Resolve a missing station selection to the error state with a message.
- [ ] Complete the project documentation by presenting the home feature as a second reference
      example of the feature layering alongside onboarding.

### Acceptance criteria

- [ ] Pulling down triggers a reload; the previously loaded readings remain visible for its whole
      duration and are replaced only when the new ones arrive.
- [ ] Retrying from the error state loads the readings and replaces the error.
- [ ] Arriving at the screen with no station stored produces the error state with a message, not a
      spinner that never resolves.
- [ ] Refreshing within the backend's cache period succeeds and leaves the displayed reading and its
      stated age unchanged, without producing an error.

### Quality gates

- [ ] `./gradlew :composeApp:testDebugUnitTest :server:test` passes.
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` succeeds.
- [ ] The refresh gesture is exercised on a running device: a refresh indicator appears, and the
      display is observed not to blank during the reload.
- [ ] Every command in the project documentation's command table still runs as written; the backend
      run command starts and answers a health check.
- [ ] No description of the home screen as a placeholder remains in the project documentation or in
      the feature's source comments.
- [ ] Compiling the touched modules from clean emits no Kotlin compiler warnings originating in
      files this task adds or changes.

