# Dependency and toolchain update

## Problem Statement

PollenInfo's build tooling and libraries are two to three releases behind. The project is held on an
old Kotlin release, and two libraries — the date-and-time library and the server's database library —
are deliberately pinned to old lines because their current versions need a newer Kotlin. The
development environment keeps pointing at outdated versions, the iOS build prints resolver warnings
caused by mismatched library versions, and every month the gap grows: each later update gets larger,
riskier and harder to attribute when something breaks. The Android build tooling has meanwhile moved
to a new major version that no longer accepts the way the app module is structured, so staying put
also means drifting away from the platform's documented project layout.

## Solution

Bring the whole toolchain — Kotlin, the Android build tooling, the build system and every library —
to its newest stable release in one feature, delivered in four verified stages so that any regression
can be attributed to one cause. Restructure the Android side into a separate application module plus
a shared library, as the new Android build tooling requires. Lift the two version pins and migrate the
code to the libraries' current APIs instead of papering over deprecations. Raise the Android API level
the app is built and targeted against to the newest one, while keeping the oldest supported Android
version unchanged. Users notice nothing: every screen, alarm, notification, stored answer and stored
alarm behaves exactly as before.

## User Stories

### Developer — toolchain

1. As a developer, I want Kotlin on its newest stable release, so that I can use current language features and current library versions.
2. As a developer, I want the Android build tooling on its newest stable major version, so that the project follows the platform's supported setup and receives fixes.
3. As a developer, I want the build system on its newest stable major version, so that the Android build tooling it requires is supported and build performance improvements apply.
4. As a developer, I want the Java version used to build and run the project to stay the same, so that every installed Android Studio runtime keeps working without setup changes.
5. As a developer, I want every library on its newest stable release, so that I am no longer stuck on old versions.
6. As a developer, I want only stable releases chosen, never alpha, beta, release-candidate or preview versions, so that the app does not depend on unfinished software.
7. As a developer, I want any case where the newest version of one library is incompatible with another to be written down next to the version, so that the next person knows why it is not the newest.
8. As a developer, I want the date-and-time pin lifted and the code moved to the standard time types, so that the app uses the language's own instant and clock rather than a deprecated copy.
9. As a developer, I want the database library pin lifted and the server moved to its current major version, so that the server is no longer on a superseded line.

### Developer — warnings and deprecations

10. As a developer, I want a clean build of every module to print no build-system deprecation warnings, so that real problems are not buried in noise.
11. As a developer, I want a clean build of every module to print no compiler warnings, so that a new warning is immediately noticeable.
12. As a developer, I want the iOS library resolver warnings gone, so that the iOS build uses a consistent set of libraries.
13. As a developer, I want Android Studio to show no "newer version available" hints in the version list, so that I can see at a glance that the project is current.
14. As a developer, I want every deprecation introduced by the update fixed with its documented replacement, so that warnings are removed, not hidden.
15. As a developer, I want no warning silenced by suppression, so that the clean build reflects clean code.
16. As a developer, I want the existing deliberate opt-ins to experimental UI components left as they are, so that this feature does not turn into a UI redesign.

### Developer — project structure

17. As a developer, I want the Android application to live in its own module, so that the project builds with the new Android build tooling.
18. As a developer, I want all shared code, its Android and iOS parts and all app tests to stay in the shared module, so that the code layout I know does not change.
19. As a developer, I want the theme module to keep its contents and only adopt the new Android library setup, so that the change there is minimal.
20. As a developer, I want the debug-only allowance for unencrypted traffic to stay in debug builds only, so that a release build can never talk to an unencrypted server.
21. As a developer, I want the Firebase project configuration to move with the application, so that push keeps working for both the debug and the release app.
22. As a developer, I want the translation check to keep covering every translated file, including the Android-only notification channel names, so that a missing translation still fails the build.
23. As a developer, I want the project documentation's module list, commands and file locations updated in the same stage as the change, so that the documentation is never wrong about how to build.
24. As a developer, I want the documentation's explanations of the two lifted pins removed, so that nobody keeps honouring a constraint that no longer exists.

### Developer — delivery

25. As a developer, I want the update delivered in four stages — libraries and toolchain, module split, date-and-time migration, database migration — so that any failure has one cause.
26. As a developer, I want every stage to leave all app tests, all server tests, the iOS test compilation, the translation check and the Android debug build passing, so that each stage is a safe point to stop or bisect.
27. As a developer, I want to be told openly if the first two stages cannot be separated, so that the plan's deviation is a decision rather than a surprise.
28. As a developer, I want the work on its own branch from the development branch and nothing pushed without my approval, so that I stay in control of what is published.
29. As a developer, I want a written report of every manual check and how it was performed, so that I know what was verified by hand and what was not.

### App user — nothing changes

30. As an app user, I want the app to start on the screen it started on before (setup or home), so that the update is invisible to me.
31. As an app user, I want all five tabs to show the same information as before, so that I can rely on the app after the update.
32. As an app user, I want the reading-age caption and the stale-reading warning to behave exactly as before, so that I am never shown old data as current.
33. As an app user, I want my diary answers kept and shown in the diary chart as before, so that I lose no history.
34. As an app user, I want the daily feeling question to appear and disappear on the same days as before, so that it is not asked twice or skipped.
35. As an app user, I want going back from the alarm editor with unsaved changes to still ask whether to discard them, with the back gesture and with the back arrow, so that I do not lose edits by accident.
36. As an app user, I want the location and notification permission prompts to work as before, so that I can use the location shortcut and receive alarms.
37. As an app user, I want changing the app language to still switch the screen language immediately on Android, so that the language setting keeps working.
38. As an app user, I want alarm notifications to keep arriving in my chosen language with the same wording, so that alarms stay understandable.
39. As an app user, I want the app to keep running on the same oldest Android version as before, so that my device is not dropped.
40. As an app user on a recent Android version, I want the app to behave correctly under the newest Android rules, so that it keeps working as Android tightens its requirements.

### Alarm owner — server

41. As an alarm owner, I want all my existing alarms to still be there after the server is updated, so that I do not have to set them up again.
42. As an alarm owner, I want my existing alarms to keep firing at the same times with the same rules, so that the update does not change what I am told.
43. As an alarm owner, I want the ten-alarm limit to still hold, so that the server behaves as before.
44. As an alarm owner, I want deleting an alarm to still remove its delivery history, so that the server keeps no orphaned records.
45. As an alarm owner, I want a threshold alert to still notify at most once per pollen type per day after the update, including across a server restart, so that I am not sent duplicates.
46. As an alarm owner, I want my installation's registration to stay valid after the update, so that the app does not have to register again.

### Maintainer

47. As a maintainer, I want an automated test that opens a database written by the server before this update, so that this and every future database library update proves existing data still loads.
48. As a maintainer, I want the release build with code shrinking to still build and run, so that the update does not break a release.

## User Acceptance Tests

1. Given the updated project, when a clean build of the Android app, the iOS sources and the server is run with all build warnings shown, then no build-system deprecation warning and no compiler warning is printed.
2. Given the updated project opened in Android Studio, when the version list is opened, then no entry is highlighted as having a newer version available.
3. Given the updated version list, when each version is compared with its project's published releases, then each is the newest stable release, or carries a comment naming the incompatibility that holds it back.
4. Given the updated version list, when it is inspected, then no version is an alpha, beta, release candidate or preview.
5. Given the updated project, when all app tests, all server tests, the iOS test compilation and the translation check are run, then all pass.
6. Given an emulator on the newest Android version with the backend running locally, when the freshly installed debug app is opened, then the setup screen appears; after choosing a station, the home screen shows that station's readings.
7. Given a completed setup, when each of the five tabs is opened, then each shows its content as before the update (home readings, all stations with map, diary chart, alarm list, settings).
8. Given the alarm editor with an unsaved change, when the back gesture is used, then the "Discard changes?" dialog appears; the same happens with the back arrow.
9. Given setup on a device that has never granted location, when "Use my location" is tapped, then the system location prompt appears.
10. Given the alarm tab on a device that has never been asked about notifications, when "Allow notifications" is tapped, then the system notification prompt appears.
11. Given the settings screen in English, when German is chosen as language, then the screen is immediately shown in German.
12. Given a debug build and a release build, when their final merged app configuration is inspected, then only the debug build allows unencrypted traffic.
13. Given the release build with code shrinking, when it is built and installed, then it starts and shows its first screen.
14. Given a database file written by the server before the update, containing devices, both alarm types and a delivery history, when the updated server starts on it, then every device's alarms are listed unchanged.
15. Given that same pre-update database, when a device already holding ten alarms tries to create another, then it is refused as before; when an alarm with delivery history is deleted, then its history is gone too.
16. Given the server configured with a push key and an alarm due a minute later, when the minute passes, then a notification arrives on the device in the app's chosen language with the same wording as before. *(Performed by the product owner.)*
17. Given the updated app on the product owner's own device, when it is used normally for a day, then nothing behaves differently from before. *(Performed by the product owner.)*

## Definition of Done

- All user acceptance tests pass; tests 16 and 17 are confirmed by the product owner.
- A clean build prints no build-system deprecation warnings and no compiler warnings, and no warning is suppressed.
- Every dependency and tool is on its newest stable release or documents why not.
- Each of the four stages was committed separately and left all automated checks passing.
- All existing automated tests pass without any change to what they assert.
- The new database-compatibility test exists and passes.
- No user-visible behaviour of the app or the server has changed.
- The project documentation describes the new module layout, commands and file locations, and no longer mentions the lifted pins.
- A written report lists every manual check and how it was performed.
- The work is on its own branch; nothing has been pushed without the product owner's approval.

## Out of Scope

- Moving to the new navigation library generation (Navigation 3); the app stays on the current, non-deprecated navigation library.
- Changing the Java version (staying on 21 for build and runtime).
- Changing the oldest supported Android version.
- Removing the deliberate experimental-component opt-ins or redesigning any screen.
- Tooling or bots that report or propose future updates.
- Any functional change, new feature or UI change.
- An iOS application project; iOS stays compile-verified only.
- Migrating any data: the database layout and the on-device stored data stay as they are.

## Further Notes

- The app tests cannot run on iOS without full Xcode; iOS is verified by compiling its sources and tests, as before.
- The exact name of the Android host test task changes with the new module setup; it is confirmed during implementation and documented.
- If the newest Android API level for compilation and targeting is not supported by some required library or by the build tooling, the compatible maximum is used and the reason documented.
- Item "newest" is judged at implementation time; versions named in this document reflect the state on 2026-10-09.

---

## Technical Annex
> Written against codebase as of: 2026-10-09

### Architectural Decisions

**Starting point (verified)**

| Item | Current | Target |
| --- | --- | --- |
| Kotlin (KMP, JVM, compose compiler, serialization plugins) | 2.1.21 | newest stable 2.4.x (2.4.20 at time of writing) |
| AGP | 8.9.2 | newest stable 9.x (stage 1: newest 8.x that works) |
| Gradle wrapper | 8.11.1 | newest stable 9.x |
| Compose Multiplatform | 1.8.1 | newest stable (1.12.1 at time of writing) |
| compileSdk / targetSdk / minSdk | 35 / 35 / 26 | 37 / 37 / 26 |
| JDK / JVM target | 21 / `JVM_21`, `jvmToolchain(21)` | unchanged |
| kotlinx-datetime | 0.6.2 (pinned) | newest 0.7.x+ |
| Exposed | 0.61.0 (pinned) | newest 1.x (1.4.x at time of writing) |
| Ktor, Koin, coroutines, serialization, lifecycle, navigation-compose, DataStore, activity, core-ktx, appcompat, sqlite-jdbc, Firebase BoM, google-services plugin, google-auth, logback | various | newest stable each |

Current warnings: only two `w: KLIB resolver: Could not find "org.jetbrains.androidx.savedstate:savedstate"` / `"…lifecycle-viewmodel-savedstate"` on `compileKotlinIosSimulatorArm64` — lifecycle / navigation / CMP version skew; aligning them removes it.

**Version policy**

- Stable only. On conflict, the compatible version wins and `libs.versions.toml` gets a comment in the existing pin style naming the constraint.
- No `@Suppress("DEPRECATION")` or `-nowarn`; every deprecation is migrated.
- `navigation-compose` stays on 2.x (Navigation 3 out of scope). If 2.x itself becomes deprecated in a way that warns at our call sites, stop and report rather than suppress.
- `compose.runtime` / `compose.foundation` / `compose.material3` / `compose.ui` / `compose.components.resources` / `compose.components.uiToolingPreview` / `compose.materialIconsExtended` / `compose.preview` accessors (in `composeApp` and `theme`) are replaced by explicit catalog entries (`org.jetbrains.compose.*` coordinates). `material-icons-extended` is published on its own version line — catalog it separately. The `Icons.Default.*`-not-transitive rule in CLAUDE.md still applies: keep icons an explicit `commonMain` dependency.
- `compose-ui-backhandler` keeps following `composeMultiplatform` (or its own ref if it diverges).

**Stage 1 — toolchain and libraries on the current structure**

- Bump everything except kotlinx-datetime and Exposed; Gradle 9 wrapper (`./gradlew wrapper --gradle-version …` + `--gradle-distribution-sha256-sum`), regenerating `gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`.
- AGP: newest 8.x compatible with Gradle 9 / Kotlin 2.4 / compileSdk 37. If none exists, merge stages 1 and 2 and say so.
- `CheckTranslationsTask` must stay configuration-cache compatible under Gradle 9.
- kotlinx-serialization ≥ 1.9 comes with this stage (needed by stage 3).
- Kotlin 2.4 may surface new compiler warnings in existing code; fix them here.

**Stage 2 — AGP 9 and module split**

Target layout (agreed in clarification):

```
androidApp/                    NEW — com.android.application + compose compiler + google-services; no KMP plugin
├── build.gradle.kts           namespace/applicationId ch.stenzel.tim.polleninfo, versionCode 1, versionName 1.0.0,
│                                debug { applicationIdSuffix ".debug", versionNameSuffix "-debug" },
│                                release { minify + shrink, proguard-android-optimize + proguard-rules.pro },
│                                compileSdk 37, targetSdk 37, minSdk 26, Java 21;
│                                implementation(project(":composeApp")), implementation(project(":theme")),
│                                activity-compose, appcompat, koin-android, Firebase BoM as needed by its own code
├── google-services.json       ← composeApp/
├── proguard-rules.pro         ← composeApp/
└── src/
    ├── main/AndroidManifest.xml   <application>: PollenInfoApplication, MainActivity, Theme.AppCompat.DayNight.NoActionBar,
    │                               android:localeConfig, AppLocalesMetadataHolderService (autoStoreLocales)
    ├── main/kotlin/ch/stenzel/tim/polleninfo/{MainActivity.kt, PollenInfoApplication.kt}   ← composeApp/androidMain
    ├── main/res/{drawable/ic_launcher_*, mipmap-anydpi-v26/*, values/strings.xml (app_name, translatable=false),
    │             xml/locales_config.xml}   ← composeApp/androidMain/res
    └── debug/AndroidManifest.xml  usesCleartextTraffic="true"   ← composeApp/src/debug

composeApp/                    com.android.kotlin.multiplatform.library
├── build.gradle.kts           kotlin { androidLibrary { namespace = "ch.stenzel.tim.polleninfo.shared" (or similar —
│                                must differ from the app's), compileSdk 37, minSdk 26, androidResources { enable = true },
│                                withHostTest { } } ; iosX64/iosArm64/iosSimulatorArm64 frameworks unchanged },
│                                compose.resources { packageOfResClass } and checkTranslations unchanged
└── src/androidMain/
    ├── AndroidManifest.xml    ACCESS_COARSE_LOCATION, POST_NOTIFICATIONS, INTERNET, PollenFirebaseMessagingService
    ├── kotlin/…/core/**       unchanged (PlatformModule.android, location, network, language, push, notifications, appinfo)
    └── res/{drawable/ic_notification.xml, values{,-de,-fr,-it}/strings.xml (notification_channel_*)}

theme/                         com.android.kotlin.multiplatform.library — android {} → kotlin { androidLibrary {} }
```

- Namespaces: `R` references in `NotificationChannels.kt` / the messaging service / `ic_notification` resolve against the library namespace; `MainActivity` / `PollenInfoApplication` stay in package `ch.stenzel.tim.polleninfo` and reference app resources by the app's `R`. Verify every `R.` import after the move.
- `checkTranslations` input path for Android strings stays `composeApp/src/androidMain/res/values*/strings.xml`; `app_name` moves out of its scope (it was `translatable="false"` and exempt anyway).
- `AndroidAppInfo` reads `PackageManager` of the running package — unaffected by the split; the version still comes from `:androidApp`'s `defaultConfig`.
- `src/debug` now sits in a plain Android app module — the CLAUDE.md quirk ("`src/debug`, not `src/androidDebug`") is deleted.
- Do not apply `org.jetbrains.kotlin.android` in `:androidApp` (AGP 9 built-in Kotlin).
- CLAUDE.md in the same stage: architecture/module table (add `:androidApp`), Commands table (`:androidApp:assembleDebug`, the host-test task replacing `:composeApp:testDebugUnitTest` everywhere it is named, incl. "Testing"), cleartext section paths + verify command, Firebase section (`androidApp/google-services.json`), "Navigation"/startup mentions of `MainActivity` location if any.

**Stage 3 — kotlinx-datetime 0.7 → `kotlin.time`**

- Replace `kotlinx.datetime.Instant` → `kotlin.time.Instant`, `kotlinx.datetime.Clock` → `kotlin.time.Clock` (and `Clock.System`) in the 26 files that import them:
  - commonMain: `core/ui/severity/ReadingAgeLabel.kt`, `core/measurement/data/mapper/StationMeasurementMapper.kt`, `core/measurement/domain/model/{ReadingAge, StationMeasurement, StationPollenOverview}.kt`, `core/push/{AlarmNotificationText, AlarmNotificationContent}.kt`, `core/diary/domain/model/SwissToday.kt`, `feature/home/presentation/{HomeScreen, HomeViewModel, HomeUiState}.kt`, `feature/allstations/presentation/{AllStationsUiState, AllStationsScreen, AllStationsViewModel}.kt`, `feature/diary/presentation/DiaryViewModel.kt`
  - androidMain: `core/push/PollenFirebaseMessagingService.kt`
  - commonTest: `ReadingAgeLabelTest`, `StationMeasurementMapperTest`, `MeasurementFixtures`, `ReadingAgeTest`, `AlarmNotificationTextTest`, `AlarmPayloadParserTest`, `DiaryEntryTest`, `HomeViewModelTest`, `AllStationsViewModelTest`, `DiaryViewModelTest`
- No deprecated typealiases, no `-0.6.x-compat` artifact. Extension functions (`toLocalDateTime`, `todayIn`, `Instant.parse`, …) keep coming from `kotlinx.datetime`; watch for import ambiguity and import explicitly.
- Confirm on the chosen Kotlin version that `kotlin.time.Instant` / `Clock` need no `@OptIn(ExperimentalTime::class)`. If they still do, stop and report — an opt-in at every use is what the pin existed to avoid.
- The measurement DTO's `measuredAt` serialization: confirm whether the DTO holds a `String` parsed in the mapper or an `Instant` with a serializer; with an `Instant` field, kotlinx-serialization ≥ 1.9's built-in serializer must decode the same ISO-8601 wire value (pinned by `StationMeasurementMapperTest`).
- Remove the 0.6.x pin comment in `composeApp/build.gradle.kts` and the CLAUDE.md paragraph in "Reading age" ("It is pinned to **0.6.x** …"); mention that `Instant` / `Clock` are `kotlin.time`.
- The server uses `java.time` only — untouched.

**Stage 4 — Exposed 1.x**

- Files: `alarm/store/{PollenInfoDatabase, Tables, AlarmStore, DeviceStore, NotificationLog}.kt`, `plugins/Routing.kt`, tests `ExposedStoresTest`, `AlarmFixtures`.
- Imports: `org.jetbrains.exposed.sql.*` → `org.jetbrains.exposed.v1.core.*`; `Database`, `DatabaseConfig`, `SchemaUtils`, `transaction`, `TransactionManager`, `selectAll`/`insert`/`update`/`deleteWhere`/`insertIgnore` → `org.jetbrains.exposed.v1.jdbc.*` per the 1.0 migration guide; `SqlExpressionBuilder.eq/less` → the 1.x operator imports; `ExposedSQLException` → its 1.x package.
- `transaction(…)` signature changed in 1.0: keep `SERIALIZABLE` isolation and running on `Dispatchers.IO`; keep the count-then-insert of the 10-alarm limit inside one transaction.
- Keep: per-connection `PRAGMA foreign_keys = ON` + busy timeout, the shared-cache `inMemory()` kept alive by one held connection, `SchemaUtils.create` on start, `ON DELETE CASCADE`, `created_at` strictly increasing per device, `clearToken` conditional on the current token.
- **No schema change**: table and column names, types and the text encodings (comma-separated enum names, `HH:mm`, ISO dates) are identical, so an existing `polleninfo.db` opens as-is. `SchemaUtils.create` on an existing file must not alter it.
- Store interfaces `DeviceStore`, `AlarmStore`, `NotificationLog` do not change; only the `Exposed*` implementations.
- Remove the pin comment in `libs.versions.toml` and CLAUDE.md "Persistence" ("Exposed is pinned to **0.61.x** …").

**Manual verification (written report, per stage where relevant)**

- By the implementer: emulator API 37 (and/or 36) + `:server:run`: onboarding → Home, all five tabs; alarm editor discard dialog via gesture back and top-bar arrow (predictive back at targetSdk ≥ 36); location prompt; notification prompt; language switch recreates in German; `assembleRelease` builds and starts; `usesCleartextTraffic` present only in the debug merged manifest; pre-update database opens (also automated, below).
- By the product owner: real FCM push (needs `FCM_CREDENTIALS`), wording and language; a day of normal use on own device.

### Automated Testing Decisions

- Good tests here assert externally visible behaviour — what a store returns, what a ViewModel emits, which wording is chosen — not which library type or API produced it. That is exactly why the existing suites are the regression net for a version bump.
- **Existing suites, unchanged in what they assert:** all of `composeApp/src/commonTest` (run via the host-test task after stage 2; iOS compile via `compileTestKotlinIosSimulatorArm64`) and all of `server/src/test`. Only types/imports are adapted (e.g. `MutableClock`-style fakes and fixtures moving to `kotlin.time.Clock` / `Instant`; Exposed imports in `ExposedStoresTest` / `AlarmFixtures`). A test whose assertion would have to change is a behaviour change and must be reported, not edited.
- Particularly load-bearing for this feature: `ReadingAgeTest` / `ReadingAgeLabelTest` (the 3-hour boundary and UTC → local conversion after the `Instant` swap), `AlarmNotificationTextTest` (Swiss-zone "today/yesterday" wording), `HomeViewModelTest` / `DiaryViewModelTest` (`swissToday(clock)` and the feeling prompt), `StationMeasurementMapperTest` (wire `measuredAt`), `ExposedStoresTest` (limit race, cascade, conditional token clear, creation order), `AlarmSchedulerTest` (restart = second scheduler on the same database, per-day exclusion), `AlarmRoutesTest`.
- **New: database compatibility test (server, integration).**
  - Fixture: a small SQLite file generated by the **pre-upgrade** server (Exposed 0.61, i.e. in or before stage 1, before stage 4 starts) and checked in under `server/src/test/resources/fixtures/` (e.g. `db/polleninfo-exposed-0.61.db`), with a short README or KDoc recording how it was produced. Contents: two devices (one with a token, one with `fcm_token` NULL), one device at exactly ten alarms, at least one daily and one threshold alarm with distinct days/species/times, a paused alarm, and `notification_log` rows for a threshold alarm.
  - Test (e.g. `alarm/store/LegacyDatabaseCompatibilityTest`): copy the fixture to a temp file (never modify the checked-in one), open it with `PollenInfoDatabase.file(path)`, and assert through the store interfaces: `AlarmStore.list` returns every alarm with all fields and in creation order; `enabledWithDeliverableDevice()` excludes the paused alarm and the token-less device; `create` on the ten-alarm device is `LimitReached`; `delete` of the logged threshold alarm removes its `notifiedSpecies`; `DeviceStore` resolves both devices.
  - Prior art: `ExposedStoresTest` (temp-file databases), the checked-in CSV fixtures read by `PollenCsvParserTest`.
- No new app tests: the split and the version bump add no logic. The Android-only pieces (permission controllers, messaging service, channels, language context) remain checked by hand, as CLAUDE.md already states.
- No test contacts MeteoSwiss or FCM (unchanged rule).
