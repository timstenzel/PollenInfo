# Crash Reporting

## Problem Statement

When PollenInfo crashes on a user's Android phone, the developer never learns about it. There is no
crash reporting of any kind: a crash in the field is invisible unless a user takes the trouble to
write in, and even then there is no stack trace, no device or Android version, and no way to tell
how often it happens or whether a release made things better or worse. Release builds are also
obfuscated, so even a stack trace obtained by other means would be unreadable.

At the same time, PollenInfo is deliberately careful with personal data — no analytics, the
location never leaves the phone, the diary needs explicit consent. Users must be able to trust that
crash reporting does not quietly undermine that, and must be able to switch it off.

## Solution

Release builds of the Android app report crashes and "app not responding" freezes to Google Firebase
Crashlytics, the crash service of the Firebase project the app already uses for push notifications.
Reports carry a readable stack trace together with technical facts about the phone (model, Android
version, app version) — never the user's location, station, account, email or diary.

Crash reporting is **on by default** and can be **switched off** at any time with a new switch,
"Send crash reports", in a new Privacy section of the Settings tab, with one sentence explaining
what is sent and to whom. The switch takes effect immediately and is remembered across launches.
Should the choice fail to save, the switch returns to its previous position and a short message
says so.

Debug builds never send crash reports, so the developer's own testing does not pollute the crash
statistics. iOS is not covered yet; on iOS the Privacy section does not appear at all.

## User Stories

1. As the developer, I want to be told about every crash of the released Android app, so that I can find and fix problems users actually hit.
2. As the developer, I want to be told when the released app freezes ("app not responding"), so that I can fix hangs as well as crashes.
3. As the developer, I want each crash report to contain a readable stack trace, so that I can see where in the code the crash happened despite release obfuscation.
4. As the developer, I want each report to show the app version, so that I can tell which release a crash belongs to and whether a fix worked.
5. As the developer, I want each report to show the device model and Android version, so that I can spot crashes tied to particular phones or Android releases.
6. As the developer, I want crashes grouped and counted, so that I can prioritise the most frequent ones.
7. As the developer, I want crashes from my debug builds to stay out of the crash statistics, so that the numbers reflect real users only.
8. As the developer, I want the information needed to make release stack traces readable to be delivered automatically with every release build, so that I never ship a release whose crashes I cannot read.
9. As the developer, I want to be able to build a release without network access, so that an offline build or a quick local release is still possible when I accept that its crashes will be unreadable.
10. As the developer, I want crash reporting to reuse the Firebase project the app already has, so that there is no new account, project or secret to manage.
11. As a user, I want crash reporting to work without any setup on my part, so that the app gets better without me doing anything.
12. As a user, I want a switch in Settings to stop sending crash reports, so that I stay in control of what leaves my phone.
13. As a user, I want to read in one sentence what a crash report contains and where it goes, so that I can decide knowingly.
14. As a user, I want to be assured that crash reports contain no location, station, account, email or diary data, so that I can leave the switch on without worrying.
15. As a user, I want my choice to take effect immediately without restarting the app, so that switching it off means it is off.
16. As a user, I want my choice remembered after I close and reopen the app or restart my phone, so that I do not have to switch it off again.
17. As a user, I want to be told if my choice could not be saved, and see the switch back where it was, so that the switch never shows something that is not true.
18. As a user who switched crash reports off, I want the app to keep working exactly as before, so that opting out costs me nothing.
19. As a user who switched crash reports off, I want to be able to switch them back on at any time, so that I can change my mind.
20. As a German-, French- or Italian-speaking user, I want the Privacy section, the switch and its explanation in my language, so that I understand what I am agreeing to.
21. As a user relying on a screen reader, I want the switch announced with its name and state, so that I can operate it without seeing it.
22. As an iOS user, I want not to see a crash-report switch that does nothing on my platform, so that Settings does not mislead me.
23. As a user without an account, I want crash reporting to behave the same as for users with an account, so that the choice has nothing to do with logging in.
24. As the developer, I want a written manual check for crash reporting, so that I can verify it end to end before a release without shipping a test crash in the app.
25. As the developer, I want the project documentation to state the crash-reporting rules, so that later changes keep debug builds silent and the opt-out honoured.
26. As the developer, I want a reminder of the store and privacy-policy disclosures that crash reporting requires, so that the next release is not rejected or non-compliant.

## User Acceptance Tests

1. Given a fresh install of the release app, when the user opens the Settings tab, then a Privacy section shows a "Send crash reports" switch that is on, with a sentence explaining that technical crash reports go to Google Firebase and contain no location, account or diary data.
2. Given a release build in which a crash has been deliberately provoked and the switch is on, when the app is reopened, then within a few minutes the crash appears in the Firebase Crashlytics console with a readable stack trace (real class and method names, not obfuscated ones), the app version, the device model and the Android version.
3. Given a release build with the switch off, when the same crash is provoked and the app is reopened, then no new crash appears in the Crashlytics console.
4. Given the user switches crash reports off, when they close the app completely and open it again, then the switch is still off.
5. Given the user switches crash reports off and then on again, when a crash is provoked, then it is reported again.
6. Given a debug build, when a crash is provoked, then nothing appears in the Crashlytics console, regardless of the switch.
7. Given the saving of the choice fails, when the user flips the switch, then the switch returns to its previous position and a short message says the choice could not be saved.
8. Given the app language is German, French or Italian, when the user opens Settings, then the Privacy section title, the switch label and the explanation are in that language.
9. Given a screen reader is active, when the user moves to the switch, then it is announced with its label and whether it is on or off, and can be toggled.
10. Given the app running on iOS, when the user opens Settings, then no Privacy section and no crash-report switch are shown.
11. Given a release build made with the mapping upload switched off, when the build runs without network access, then it completes successfully.
12. Given a normal release build with network access, when the build finishes, then the information needed to make stack traces readable has been delivered to Firebase without any manual step.
13. Given crash reports are switched off, when the user uses Home, All stations, Diary, Alarms and Settings, then every feature works as before.

## Definition of Done

- All user acceptance tests pass.
- Release builds report fatal crashes and freezes with readable stack traces; debug builds report nothing.
- The opt-out switch is shown on Android only, defaults to on, applies immediately and survives restarts.
- All new text is available in English, German, French and Italian, and the translation check passes.
- Crash reports contain no location, station, account, email, diary or token data.
- No regression in existing features; all existing automated tests pass.
- The app compiles for iOS.
- No new build or compiler warning.
- The manual end-to-end check for crash reporting is written down in the project documentation.
- Project documentation describes the crash-reporting rules (debug off, opt-out, fatal only, Android only, mapping upload).
- The manual release steps (Play Console Data safety declaration, privacy-policy mention) are listed in the project documentation.

## Out of Scope

- **iOS crash reporting.** Deferred until the iOS app project exists; it then needs the native Firebase SDK, symbol upload and a bridge for Kotlin crashes.
- **Non-fatal error reporting.** Errors the app catches and shows to the user (for example "Something went wrong") are not reported. A possible follow-up.
- **Breadcrumbs, custom keys, logs or user identifiers** in crash reports.
- **Analytics or performance monitoring** of any kind.
- **A test-crash button** or any hidden trigger in the shipped app.
- **Opt-in at onboarding** or any consent prompt; the choice lives in Settings only.
- **A privacy policy** document, page or link in the app. Writing or updating it, and filling in the Play Console Data safety form, are manual release steps outside this feature.
- **Building release apps in CI.** Release builds remain local.
- **Native (NDK) crash symbols** — the app has no native code of its own.
- **Deleting already-sent crash reports** when the user switches the switch off.

## Further Notes

- Crash reporting uses the existing Firebase project and its committed configuration file; no new secret is introduced.
- Crashlytics identifies an install by the Firebase Installation ID, the same identifier the app already uses as its push address. It is not linked to the user's account.
- An opt-out takes effect immediately in the running app. If the app crashes during the first moments of a launch, before the stored choice has been read, Firebase applies the choice it remembered from the previous launch, so the opt-out is still honoured.
- The choice is an ordinary preference (not a secret) and may be included in the device backup like the selected station.
- Manual release steps to schedule with the first release that contains this feature: declare "Crash logs" and "Diagnostics" (collected, not shared, optional) in the Play Console Data safety form, and mention Firebase Crashlytics in the privacy policy.

---

## Technical Annex
> Written against codebase as of: 2026-10-18

### Architectural Decisions

**Platform-bound reporter — `core/crashreporting`** (new package)

- `interface CrashReporter { val isAvailable: Boolean; fun setEnabled(enabled: Boolean) }` in `commonMain`.
- Android: `FirebaseCrashReporter` in `androidMain` — `isAvailable = true`, `setEnabled` calls
  `FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled)`. In a debug build the
  manifest flag keeps collection off, and `setEnabled(true)` must **not** override it (the runtime
  call takes precedence over the manifest). Decision: `FirebaseCrashReporter(collectionAllowed: Boolean)`; `platformModule`
  passes `collectionAllowed = !isDebuggable` read from `ApplicationInfo.FLAG_DEBUGGABLE` (`:composeApp`
  cannot see `:androidApp`'s `BuildConfig`). With `collectionAllowed = false` it calls
  `setCrashlyticsCollectionEnabled(false)` whatever it is given.
- iOS: `NoCrashReporter` in `iosMain` (or `commonMain`) — `isAvailable = false`, `setEnabled` no-op.
- Bound in `core/di/PlatformModule` (`single<CrashReporter> { … }`) on both platforms. Plain platform
  classes, not `expect`/`actual`. Checked by hand (Android) / compile-verified (iOS), like the other
  platform implementations.

**Preference — `core/preferences`**

- `interface CrashReportingPreferences { val enabled: Flow<Boolean>; suspend fun setEnabled(enabled: Boolean): Result<Unit> }`
  — `enabled` defaults to `true` when nothing is stored.
- `DataStoreCrashReportingPreferences(dataStore)` over the default `DataStore<Preferences>`
  (`polleninfo_preferences`, not a secret, not excluded from backup), key
  `booleanPreferencesKey("crash_reports_enabled")`, write wrapped in `safeCall`. Logic-free and
  untested, following `DataStoreNotificationPermissionPreferences`.
- `FakeCrashReportingPreferences` in `commonTest/.../core/preferences/` with a settable value and a
  way to make the next write fail.

**Sync — `core/crashreporting/CrashReportingSync`** (the deep module)

```kotlin
class CrashReportingSync(
    private val preferences: CrashReportingPreferences,
    private val reporter: CrashReporter,
) {
    /** Applies the stored choice now and after every change; never returns. */
    suspend fun run() {
        if (!reporter.isAvailable) return
        preferences.enabled.distinctUntilChanged().collect(reporter::setEnabled)
    }
}
```

- Single source of truth is the preference; nothing else calls `CrashReporter.setEnabled`.
- Bound `single { CrashReportingSync(get(), get()) }` in `dataModule` (or `domainModule`).
- Started once per process in `PollenInfoApplication.onCreate` after `startKoin`:
  an application-lifetime `CoroutineScope(SupervisorJob() + Dispatchers.Default)` launching
  `get<CrashReportingSync>().run()`. Runs in processes started by FCM too (no activity needed).
- A read failure of the DataStore flow must not crash the app: `enabled` errors are caught in the
  sync (`catch { }`), leaving Firebase on its last persisted value.

**Settings slice — `feature/settings/presentation`**

- `SettingsUiState` gains `crashReportsAvailable: Boolean` (from `CrashReporter.isAvailable`, fixed)
  and `crashReportsEnabled: Boolean` (observed from `CrashReportingPreferences.enabled`).
- `SettingsViewModel` gains constructor parameters `CrashReportingPreferences`, `CrashReporter`
  (only for `isAvailable`) and `fun onCrashReportsToggled(enabled: Boolean)`:
  optimistic `crashReportsEnabled = enabled`, then `setEnabled(enabled)`; on `Result.Failure` set it
  back to the previous value and send `SettingsEvent.CrashReportsSaveFailed`. The ViewModel never
  calls `CrashReporter.setEnabled` — the sync follows the preference.
- New `sealed interface SettingsEvent` with `CrashReportsSaveFailed`; `Channel<SettingsEvent>(Channel.BUFFERED)`
  exposed as `events = receiveAsFlow()` (Settings currently has no events). `SettingsScreen` adds a
  `SnackbarHost` to its `Scaffold` and collects the events in a `LaunchedEffect`, showing a
  translated message. (Purely in-screen, so no lambda from `AppNavigation` is needed.)
- `SettingsScreen`: a "Privacy" section with a `ListItem`/row holding the label, the explanation as
  supporting text and a `Switch`; the whole row `toggleable` with `Role.Switch` so the screen reader
  announces label + state. Hidden when `!crashReportsAvailable`.
- DI: `viewModel { SettingsViewModel(get(), get(), get(), get(), get(), get(), get()) }`.

**Strings** (`composeResources/values{,-de,-fr,-it}/strings.xml`)

- `settings_privacy_title`, `settings_crash_reports_label`, `settings_crash_reports_description`,
  `settings_crash_reports_save_failed`. Swiss Standard German "du", French "vous" with U+202F rules,
  Italian "Lei", typographic apostrophes.

**Build — `:androidApp` and catalog**

- `gradle/libs.versions.toml`: `firebase-crashlytics = { module = "com.google.firebase:firebase-crashlytics" }`
  (version from `firebase-bom`), plugin `firebaseCrashlytics = { id = "com.google.firebase.crashlytics", version.ref = … }`
  — pick a version compatible with AGP 9.4 and the configuration cache.
- Root `build.gradle.kts`: `alias(libs.plugins.firebaseCrashlytics) apply false`.
- Library dependency in `:composeApp`'s `androidMain` (next to `firebase-messaging`, needed by
  `FirebaseCrashReporter`); plugin applied in `:androidApp`.
- Mapping upload: `release { configure<CrashlyticsExtension> { mappingFileUploadEnabled = providers.gradleProperty("polleninfo.uploadCrashlyticsMapping").map(String::toBoolean).getOrElse(true) } }`.
- `androidApp/src/debug/AndroidManifest.xml`: `<meta-data android:name="firebase_crashlytics_collection_enabled" android:value="false" />`.
  Verify: `grep -l 'firebase_crashlytics_collection_enabled' androidApp/build/intermediates/merged_manifest/*/process*MainManifest/AndroidManifest.xml` — only debug matches.
- `androidApp/proguard-rules.pro`: `-keepattributes SourceFile,LineNumberTable` and
  `-keep public class * extends java.lang.Exception` (Crashlytics' recommended rules), unless the
  library's consumer rules already cover them.
- No NDK plugin.

**Docs**

- `README.md`: tech stack (Firebase Cloud Messaging + Crashlytics), `androidApp` row.
- `CLAUDE.md`: new section under Firebase — Android only, fatal + ANR only, no custom keys/logs/user
  id, debug off via debug manifest (with grep check), opt-out preference is the source of truth
  applied by `CrashReportingSync`, mapping property; add `core/crashreporting` to the `core/` table;
  Crashlytics in the Play-services dependency list.
- Manual check (in `README.md` or `deploy/README.md`): local release build with a temporary `throw`
  in a scratch commit → crash → console shows readable trace; switch off → crash → nothing; then
  revert the scratch commit.

### Automated Testing Decisions

- Good tests here pin **observable behaviour** — what reaches the reporter, what the screen state
  shows, which event is sent — never internal call order or Firebase internals.
- **`CrashReportingSyncTest`** (`commonTest/.../core/crashreporting/`, unit, `runTest` with the sync
  in `backgroundScope`): with `FakeCrashReporter` (records every `setEnabled` value, configurable
  `isAvailable`) and `FakeCrashReportingPreferences`:
  - nothing stored → reporter enabled;
  - stored `false` → reporter disabled at start;
  - every change of the preference reaches the reporter, repeated equal values only once;
  - an unavailable reporter is never called;
  - a failing preference flow does not crash and does not call the reporter.
- **`SettingsViewModelTest`** additions (unit, `StandardTestDispatcher`, `advanceUntilIdle`):
  - `crashReportsAvailable` mirrors the reporter; `crashReportsEnabled` follows the stored value;
  - toggling writes the preference and shows the new value;
  - a failed write restores the previous value and sends `CrashReportsSaveFailed` exactly once;
  - the ViewModel never calls the reporter's `setEnabled` (the sync owns it).
- Fakes `FakeCrashReporter` and `FakeCrashReportingPreferences` live next to their subjects in
  `commonTest`; no mocking framework. Prior art: `FakeNotificationPermissionPreferences`,
  `FakeLanguageRepository`, `FakeAppInfo`; ViewModel event tests in `AlarmsViewModelTest`
  (`ToggleFailed` flip-back) and `HomeViewModelTest`.
- **Not unit-tested** (by decision): `FirebaseCrashReporter`, `NoCrashReporter`,
  `DataStoreCrashReportingPreferences`, Gradle and manifest configuration. The debug manifest flag is
  checked by the merged-manifest grep; end-to-end behaviour by the manual check.
- Run `./gradlew :composeApp:testAndroidHostTest`, `:composeApp:compileTestKotlinIosSimulatorArm64`,
  `:composeApp:checkTranslations` and `:androidApp:assembleRelease -Ppolleninfo.uploadCrashlyticsMapping=false`.
