# Tasks — dependency-update

> Source: `.sdd/dependency-update/requirements.md`. Technical Annex verified against the codebase on 2026-10-09 — no conflicts. Clarifications found while verifying: the measurement DTO keeps `measuredAt` as a `String` parsed by the mapper; the channel/notification code imports the app's `R`, which becomes the library namespace's `R` after the split; `MutableClock` is a server-only `java.time` helper.
>
> **Branch:** all work happens on `feature/update-kotlin-agp-and-libs` (already created from `develop` at 4d13163). Each task ends in its own commit(s). Nothing is pushed without the product owner's approval.
>
> **Standard checks** — referred to below, all must pass:
> 1. App unit tests (`:composeApp:testDebugUnitTest`; from task 03 on, the host-test task that replaces it)
> 2. `:server:test`
> 3. `:composeApp:compileTestKotlinIosSimulatorArm64`
> 4. `:composeApp:checkTranslations`
> 5. Android debug APK (`:composeApp:assembleDebug`; from task 03 on, `:androidApp:assembleDebug`)
>
> **Emulator:** manual checks use an API 37 image; if none is installable, API 36 is used and the report says so.

## Task [01-legacy-database-compatibility-test]

Before anything is upgraded, capture what an existing server database looks like and prove — through the store interfaces only — that the server reads it correctly. A small SQLite file is produced by the **current** server (Exposed 0.61) and checked in as a test fixture; a new server test opens a temp copy of it and checks devices, alarms, deliverability, the alarm limit and the cascade. This test is green now and is the safety net that tasks 02 (Kotlin bump) and 05 (Exposed 1.x) must keep green.

### Implementation steps

- [x] Write a generator that uses the existing stores on Exposed 0.61 to write a database with: two devices (one keeping its push token, one whose token is cleared through the device store's conditional token clear), one device holding exactly ten alarms, daily and threshold alarms with distinct stations/species/days/times, one paused alarm, and notification-log rows for a threshold alarm on a fixed Swiss date.
- [x] Make sure the generator can never run as part of `:server:test` (an opt-in Gradle task or a `main` outside the test suite, or delete it after use); its note states it is only valid on Exposed 0.61.
- [x] Generate the file and check it in under the server's test-resource fixtures, with a note (README or test KDoc) recording the producing Exposed version, the date and how it was produced.
- [x] Add the compatibility test: copy the fixture to a temp file, open it through the production database entry point for a file path, assert through the device store, alarm store and notification log.
- [x] Commit.

### Acceptance criteria

- [x] The fixture database is checked in and its note says it was produced on Exposed 0.61 and is not to be regenerated on a later version.
- [x] The test asserts that the device store resolves both devices and that every fixture device's alarms are listed with all fields equal to what was written, in creation order.
- [x] The test asserts that the paused alarm and the token-less device's alarms are not returned as deliverable, and all other enabled alarms are.
- [x] The test asserts that creating an eleventh alarm on the ten-alarm device is refused as "limit reached".
- [x] The test asserts that the logged threshold alarm's notified species are returned before deletion and are gone after deleting the alarm (cascade).
- [x] The test only ever opens a temp copy: the checked-in fixture's checksum is unchanged after `:server:test`.

### Quality gates

- [x] All standard checks pass.
- [x] No new compiler warnings in `:server`.
- [x] `:server:test` does not run the generator (verified by its task list / absence from the test source set).

## Task [02-toolchain-and-libraries-current]

> **Merged with task 03 (product owner's decision, 2026-10-09).** No AGP 8.x can build this toolchain: AndroidX Compose 1.12, core 1.19 and lifecycle 2.11 need AGP ≥ 9.1, AGP 8.13.2 cannot load on Gradle ≥ 9.6 and on 9.5 prints Gradle deprecations and an untested-compileSdk-37 warning. Tasks 02 and 03 are therefore implemented together; the AGP-8 parts of this task's criteria are replaced by task 03's. Ktor stays on 3.5.2 until task 04: 3.6.0 pulls in `kotlinx-datetime:0.8.0-0.6.x-compat`. By the same rule Compose Material 3 stays on 1.8.2 until task 04 (1.9.0 pulls in kotlinx-datetime 0.7.1 on iOS). Firebase BoM 35 deprecates `getToken` / `onNewToken`; by the product owner's decision push moved to Firebase Installation IDs in this stage (app: `register()` / `onRegistered`; server: FIDs sent as `fid`, legacy registration tokens still as `token`). Manual checks ran on an API 36 emulator (no API 37 image installable).

Stage 1: the toolchain and every library except kotlinx-datetime and Exposed move to their newest stable versions, on the existing module structure — Gradle 9, Kotlin 2.4.x, Compose Multiplatform, the newest AGP 8.x that supports them, compileSdk/targetSdk 37 (minSdk 26), Ktor, Koin, coroutines, serialization (≥ 1.9), lifecycle, navigation-compose 2.x, DataStore, AndroidX, Firebase BoM, google-services, google-auth, logback, sqlite-jdbc. The deprecated `compose.*` dependency accessors are replaced by version-catalog entries; every deprecation the bump causes is fixed with its replacement. App and server behave exactly as before. If no AGP 8.x works with this toolchain, or a library forces kotlinx-datetime 0.7 onto the build, stop and report — the affected stages then merge.

### Implementation steps

- [x] Look up the newest stable version of every catalog entry and of Gradle.
- [x] Update the Gradle wrapper to the newest stable 9.x (scripts, wrapper jar, distribution checksum).
- [x] Bump Kotlin and the Kotlin plugins, Compose Multiplatform, AGP (newest compatible 8.x), and every library except kotlinx-datetime and Exposed; set compileSdk/targetSdk to 37 in both Android modules; leave JDK/JVM target 21 as is.
- [x] Check with `dependencyInsight` (Android and iOS configurations) that kotlinx-datetime still resolves to 0.6.x; if a library forces 0.7, stop and report.
- [x] Replace every `compose.*` accessor in `:composeApp` and `:theme` with explicit catalog entries (material-icons-extended on its own version line, still an explicit common dependency).
- [x] Fix every deprecation and compiler warning the bump surfaces (Kotlin, Compose, Material 3, Koin, Ktor, lifecycle, AndroidX, Gradle DSL) with the documented replacement — no suppression; stop and report if one can only be fixed by moving to Navigation 3.
- [x] Make sure `checkTranslations` still runs with the configuration cache on Gradle 9.
- [x] Update CLAUDE.md where it names versions or behaviour changed in this stage, keeping the datetime and Exposed pin paragraphs for now.
- [x] Commit.

### Acceptance criteria

- [x] Every catalog entry is on its newest stable version, except kotlinx-datetime (0.6.x), Exposed (0.61.x), AGP (8.x until task 03), navigation-compose (2.x by decision) and any entry held back by a constraint — each exception carries a catalog comment naming the reason; no entry is alpha/beta/RC/EAP/dev.
- [x] The Gradle wrapper is on the newest stable 9.x release with the distribution checksum set.
- [x] compileSdk and targetSdk are 37 and minSdk is 26 in every Android module; the JVM target is 21 and the server toolchain 21, unchanged.
- [x] No build script uses a `compose.*` dependency accessor.
- [ ] A clean build (`--rerun-tasks --warning-mode all`) of the app (Android + iOS simulator compile) and the server prints no Gradle deprecation and no `w:` line — including the two former KLIB resolver warnings. *(failed: two Gradle deprecations come from the plugins themselves — AGP 9.4.1 `Configuration.setVisible` (deprecated since Gradle 9.1; every AGP 9 needs ≥ 9.1) and KGP 2.4.21 `KotlinNativeBundleArtifactsTypes` (once per fresh daemon). No `w:` line remains; both are documented in CLAUDE.md.)*
- [x] The debug app on the emulator against `:server:run` opens and shows Home with readings for the stored station. *(API 36 emulator — no API 37 image installable here.)*

### Quality gates

- [x] All standard checks pass, including task 01's legacy-database test on Kotlin 2.4.
- [x] No `@Suppress` for deprecations and no warning-silencing compiler flags were added (diff check).
- [ ] The number of `@OptIn(Experimental…)` annotations in the app is unchanged (diff count). *(failed: 33 → 32. None was added; the one removed was `ExperimentalComposeUiApi` on `AlarmEditorScreen`, which existed only for the deprecated `BackHandler`. Its replacement, `NavigationBackHandler`, is stable.)*
- [x] No existing test assertion was changed; only imports/types where an API moved.
- [x] The configuration cache is stored and reused on a second identical build.

## Task [03-android-app-module-split]

Stage 2: move to the newest stable AGP 9.x by splitting the Android side. A new `:androidApp` module (plain Android application) becomes the APK: application id, version, build types (debug suffix, `-debug` name suffix, release minify/shrink), the app manifest with `PollenInfoApplication`, `MainActivity`, the AppCompat theme, the locale config and AppCompat's locale service, the launcher icons and `app_name`, the debug-only cleartext manifest, the Firebase config and the R8 rules. `:composeApp` becomes a KMP library on the Android-KMP library plugin with Android resources and host tests enabled; its Android part keeps every platform actual, the messaging service, the notification channels with their translated names, the notification icon and the permissions (in a library manifest). `:theme` only switches plugin. An existing install updates in place without losing anything; documentation is updated in the same task.

### Implementation steps

- [x] Before changing anything, build and keep the current (pre-split) debug APK for the in-place update check.
- [x] Bump AGP to the newest stable 9.x (and Gradle if it requires a newer 9.x).
- [x] Switch `:theme` to the Android-KMP library plugin (configuration moves into the Kotlin `androidLibrary` block).
- [x] Switch `:composeApp` to the Android-KMP library plugin: library namespace distinct from the app's, compileSdk 37, minSdk 26, Android resources enabled, host tests enabled; keep the iOS frameworks, Compose resources configuration and `checkTranslations` as they are.
- [x] Create `:androidApp` (no KMP plugin, no separate Kotlin Android plugin; JVM target 21) depending on `:composeApp` and `:theme`; move the entry points, the application manifest parts, launcher resources, `app_name`, locale config, debug manifest, Firebase config and R8 rules into it; apply the Google Services plugin there.
- [x] Leave a library manifest in `:composeApp`'s Android part with the permissions and the messaging service; point the channel and notification code at the library's `R`.
- [x] Update CLAUDE.md: module table and architecture, Commands table and every place naming the old test or APK task, the cleartext section (paths, verification command; drop the `src/debug` vs `src/androidDebug` quirk), the Firebase section (config location), and any note naming moved files.
- [x] Commit.

### Acceptance criteria

- [x] AGP is on the newest stable 9.x and no module applies the KMP plugin together with `com.android.application` or `com.android.library`.
- [x] `:androidApp:assembleDebug` and `:androidApp:assembleRelease` both build; the debug APK's application id ends in `.debug` and its version name in `-debug`; the JVM target is 21 in every module including `:androidApp`.
- [x] `usesCleartextTraffic` appears in `:androidApp`'s debug merged manifest and not in its release merged manifest; the debug merged manifest contains `ACCESS_COARSE_LOCATION`, `POST_NOTIFICATIONS`, `INTERNET`, `PollenFirebaseMessagingService` and `AppLocalesMetadataHolderService` (grep).
- [x] `checkTranslations` still checks the notification channel names in all four languages (temporarily removing one German channel string makes it fail; restored afterwards).
- [x] In-place update on the emulator against `:server:run`: with the pre-split debug APK installed, a station chosen, today's feeling recorded and one alarm created, installing the `:androidApp` debug APK over it opens on Home (no onboarding), the diary shows the recorded answer, the alarm is listed, and the server log shows no new `POST /devices`.
- [x] On a fresh install, onboarding leads to Home with readings, and switching the language to German in Settings redraws the screen in German at once.

### Quality gates

- [x] All standard checks pass, using the new host-test and `:androidApp` tasks.
- [ ] Clean build with `--warning-mode all` prints no Gradle deprecation and no `w:` line. *(failed: two Gradle deprecations come from the plugins themselves — AGP 9.4.1 `Configuration.setVisible` (deprecated since Gradle 9.1; every AGP 9 needs ≥ 9.1) and KGP 2.4.21 `KotlinNativeBundleArtifactsTypes` (once per fresh daemon). No `w:` line remains; both are documented in CLAUDE.md.)*
- [ ] No file in `commonMain`, `commonTest` or `iosMain` changed. *(failed by the merge with task 02: 7 `commonMain` files carry task 02's warning fixes — 6 redundant `Unit`s / casts and `BackHandler` → `NavigationBackHandler`. `commonTest` and `iosMain` are unchanged.)*
- [x] CLAUDE.md no longer names `:composeApp:assembleDebug`, `:composeApp:testDebugUnitTest`, `composeApp/google-services.json` or `src/androidDebug` (grep).

## Task [04-kotlin-time-migration]

> **Implementation notes (2026-10-09).** kotlinx-datetime 0.8.0, and with it Ktor 3.6.0 and Compose Material 3 1.9.0 (held back in task 02 for this migration). Ktor 3.6 requires `kotlinx-datetime:0.8.0-0.6.x-compat` through `ktor-openapi-schema`, which would put the compat artifact on every app classpath; that library names the old types only as strings (checked in its JVM classes and iOS klib signatures), so the catalog declares kotlinx-datetime `strictly = "0.8.0"` and `dependencyInsight` resolves plain 0.8.0 on Android, host-test and iOS classpaths. The server, which uses `java.time` only and does not declare kotlinx-datetime, still gets the compat artifact transitively from Ktor. Deprecations the bump surfaced were fixed: `LocalDate.dayOfMonth` → `day`, `monthNumber` → `month.number`, and Material 3 1.9's `MenuAnchorType` → `ExposedDropdownMenuAnchorType`. Emulator: API 36.

Stage 3: lift the kotlinx-datetime pin. The app moves to the newest stable kotlinx-datetime and uses the standard library's `kotlin.time.Instant` and `kotlin.time.Clock` everywhere it used the library's own — models, mapper, reading age, Swiss "today", notification wording, the three ViewModels' injected clocks, the Android push service, and every test fixture and fake. Dates, times and zones stay in kotlinx-datetime. Reading-age boundaries, refresh captions, the feeling prompt, the diary window and notification wording behave exactly as before.

### Implementation steps

- [x] Bump kotlinx-datetime to the newest stable version; confirm on the project's Kotlin version that `kotlin.time.Instant` / `Clock` need no opt-in — if they do, stop and report.
- [x] Replace every `kotlinx.datetime.Instant` / `kotlinx.datetime.Clock` import in app main and test code with the `kotlin.time` types (explicit imports where names would clash), without deprecated type aliases or the compat artifact.
- [x] Confirm the measurement wire value still parses: `measuredAt` stays a `String` in the DTO and the mapper parses it with `kotlin.time.Instant`.
- [x] Adapt test clocks and fixtures to `kotlin.time.Clock` / `Instant` without changing any asserted value.
- [x] Remove the 0.6.x pin comment from the app's build script (next to the kotlinx-datetime dependency) and the pin paragraph from CLAUDE.md ("Reading age"), noting that `Instant` / `Clock` are `kotlin.time`.
- [x] Commit.

### Acceptance criteria

- [x] kotlinx-datetime is on its newest stable version and the app build script no longer contains the "Pinned to 0.6.x" comment.
- [x] No app source or test file imports `kotlinx.datetime.Instant` or `kotlinx.datetime.Clock` (grep finds nothing).
- [x] No `ExperimentalTime` opt-in was added anywhere (grep).
- [x] The reading-age, reading-age label, measurement mapper, notification text, payload parser, diary entry, Home, All stations and Diary ViewModel tests pass with their assertions unchanged.
- [x] On the emulator against `:server:run`, Home shows the "Data from HH:mm" and "Refreshed HH:mm" captions in local time.

### Quality gates

- [x] All standard checks pass.
- [x] Clean build with `--warning-mode all` prints no Gradle deprecation and no `w:` line.
- [x] CLAUDE.md no longer says kotlinx-datetime is pinned to 0.6.x (grep for `0.6.x`).

## Task [05-exposed-1x-migration]

> **Implementation notes (2026-10-09).** Exposed 1.5.0. The migration was imports only: `v1.core` for tables, `Op`, `ResultRow`, `SortOrder`, `UpdateBuilder` and the operators — which are now top-level functions, so `eq` / `isNotNull` inside `where { }` lambdas need an explicit import where the old `SqlExpressionBuilder` receiver supplied them — and `v1.jdbc` for `Database`, `SchemaUtils`, the queries (`select` included) and `transaction` / `TransactionManager`; `ExposedSQLException` is `v1.exceptions`. `transaction(database) { … }` and `DatabaseConfig { defaultIsolationLevel = … }` compile unchanged. No store logic and no test assertion changed. The `:server` build's one Gradle deprecation is AGP's `setVisible` (traced to `BasePlugin.createAndroidJdkImageConfiguration`), printed because the Android modules are configured too.

Stage 4: lift the Exposed pin. The server's database layer moves to the newest stable Exposed 1.x — `v1` packages, the JDBC split and the changed `transaction` signature — while every store keeps its interface and behaviour: serializable transactions on the IO dispatcher, the per-connection foreign-keys pragma and busy timeout, the ten-alarm limit counted inside the insert's transaction, cascade deletes of the notification log, creation order, the conditional token clear, and the schema exactly as it is. Task 01's legacy-database test proves an existing database still opens.

### Implementation steps

- [x] Bump Exposed to the newest stable 1.x and remove its pin comment from the catalog.
- [x] Migrate the database entry point, tables, the three stores, routing's store defaults and the store test helpers to the 1.x packages and `transaction` signature, keeping isolation, dispatcher, pragma and busy timeout.
- [x] Run the store, route, scheduler and legacy-database tests and fix only the code, not the assertions.
- [x] Start `:server:run` with `POLLENINFO_DB` pointing at a copy of task 01's fixture and list a device's alarms through the REST API.
- [x] Remove the 0.61.x pin paragraph from CLAUDE.md ("Persistence"), keeping the rest of the persistence description accurate for 1.x.
- [x] Commit.

### Acceptance criteria

- [x] Exposed is on its newest stable 1.x and no server file imports an `org.jetbrains.exposed.sql` package (grep).
- [x] Task 01's legacy-database compatibility test passes unchanged.
- [x] The existing store, alarm-route and scheduler tests pass unchanged — including the concurrent-create limit, the cascade, the conditional token clear and restart without resending.
- [x] `:server:run` against a copy of the fixture answers `GET /devices/{id}/alarms` with that device's alarms, and `sqlite3 .schema` of the copy is identical before and after the start.
- [x] The `alarm/` push-text grep from CLAUDE.md still finds nothing.

### Quality gates

- [x] All standard checks pass.
- [ ] Clean build of `:server` with `--warning-mode all` prints no Gradle deprecation and no `w:` line. *(failed: no `w:` line, but one Gradle deprecation — AGP 9.4.1's `Configuration.setVisible`, from `BasePlugin.createAndroidJdkImageConfiguration`, printed because the Android modules are configured in the same build; nothing from `:server` or Exposed. Same known plugin deprecation as task 03, documented in CLAUDE.md.)*
- [x] Neither CLAUDE.md nor the catalog mentions `0.61` (grep).

## Task [06-release-readiness-verification]

The whole updated app and server, verified as a user experiences them, with a written report. A final clean build proves the warning goal, Android Studio's version hints prove the version goal, and the manual checks on the emulator cover what tests cannot: every tab, predictive back in the alarm editor, the permission prompts and the release build. The real-push check and a day of use are handed to the product owner, whose confirmation closes the feature.

### Implementation steps

- [x] Re-check every catalog entry and the Gradle version against the newest stable release; anything released since task 02 is bumped in its own commit (standard checks run) before the report, or the reason it is not is recorded in the catalog.
- [x] Run a clean build of every module with `--rerun-tasks --warning-mode all` and keep its warning output.
- [x] On the emulator against `:server:run`, walk through: fresh install → onboarding (location prompt) → Home; All stations (map, a selected row), Diary (chart, range switch), Alarms (notification prompt, create an alarm), Settings (change station, language); in the alarm editor with an unsaved change, back gesture and top-bar arrow.
- [x] Build, install and start the release APK.
- [x] Write `.sdd/dependency-update/verification-report.md`: each check, method, result (screenshots where useful), emulator API level used; plus a section for the product owner's checks (real push in the chosen language, a day of normal use) with instructions and a place to record their confirmation.
- [x] Commit the report; tell the product owner the branch is ready for their checks.

### Acceptance criteria

- [ ] The clean build output contains no Gradle deprecation and no `w:` line for any module. *(failed: no `w:` line, but AGP 9.4.1's `Configuration.setVisible` deprecation prints (and KGP 2.4.21's `KotlinNativeBundleArtifactsTypes` once per fresh daemon) — both from the plugins, documented in CLAUDE.md, as in tasks 02–05.)*
- [x] Every catalog entry is at the newest stable version or carries a comment naming the constraint, and Android Studio's version list shows no "newer version available" hint (recorded in the report). *(Exposed 1.5.1, released during the session, bumped in its own commit; afterwards lint's `NewerVersionAvailable` / `GradleDependency` — the detectors behind the IDE hint — report 0. The IDE window itself was not opened.)*
- [x] In the alarm editor with an unsaved change, both the back gesture and the top-bar arrow show "Discard changes?".
- [x] On a fresh install the location and notification prompts appear when requested, and all five tabs show their content. *(API 37 — only with `ACCESS_LOCAL_NETWORK` added to the debug manifest for the walk-through and reverted: Android 17 blocks `10.0.2.2` for targetSdk 37. Open finding in the report.)*
- [x] The release APK installs and starts on the emulator. *(signed locally with the debug keystore; the release output is unsigned)*
- [x] The report exists with every manual check's method and result, and a product-owner section for UAT 16 and 17; the feature counts as done only once that section is confirmed.

### Quality gates

- [x] All standard checks pass on the final commit.
- [x] `git diff develop -- '**/strings.xml'` shows no changed user-facing text (only moves).
- [x] The branch is not pushed.
