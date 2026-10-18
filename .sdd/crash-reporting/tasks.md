## Task [01-release-crashes-reported]

Release builds of the Android app report fatal crashes and ANRs to Firebase Crashlytics with
readable (deobfuscated) stack traces; debug builds never report. This is the tracer bullet: the
Crashlytics library and Gradle plugin are wired into the existing Firebase setup, the R8 mapping is
uploaded on every release build (switchable off for offline builds), and debug collection is
disabled by a debug-only manifest flag. No user-facing switch yet — collection is simply on in
release.

### Implementation steps

- [x] Add the Crashlytics library (version from the existing Firebase BoM) and the Crashlytics Gradle plugin to the version catalog, choosing a plugin version that works with AGP 9.4 and the configuration cache
- [x] Declare the plugin `apply false` in the root build script and apply it in `:androidApp`; add the library to `:composeApp`'s `androidMain` dependencies next to Firebase Messaging, with a comment in the existing style
- [x] Add the Gradle property `polleninfo.uploadCrashlyticsMapping` (default `true`) that controls the release mapping upload
- [x] Add `firebase_crashlytics_collection_enabled = false` as `<meta-data>` to the debug-only manifest, with the overlay's comment extended to explain it
- [x] Add the Crashlytics-recommended R8 rules (keep source file and line number attributes; keep exception classes unless the library's consumer rules already do) to the release R8 rules
- [x] Document in `CLAUDE.md` (the "Firebase and push (Android)" section, the Play-services dependency sentence, and the `polleninfo.uploadCrashlyticsMapping` property next to the "Commands" table) and `README.md` (tech stack, `androidApp` row): Android only, fatal + ANR only, no custom keys/logs/user id, debug off with its merged-manifest check, the mapping property
- [x] Add a "Crash reporting manual check" section to `deploy/README.md`: local release build with a temporary `throw` (and a > 5 s main-thread block for the ANR) in a scratch commit, provoke, check the console, revert; and list the manual release steps (Play Console Data safety: Crash logs + Diagnostics, optional; privacy-policy mention of Firebase Crashlytics)

### Acceptance criteria

- [x] With dependencies cached and the network disabled, `./gradlew --offline :androidApp:assembleRelease -Ppolleninfo.uploadCrashlyticsMapping=false` succeeds
- [x] `./gradlew :androidApp:assembleRelease --dry-run` lists the Crashlytics mapping-upload task for release (e.g. `uploadCrashlyticsMappingFileRelease`, name checked against the chosen plugin version); with `-Ppolleninfo.uploadCrashlyticsMapping=false` the task is not executed
- [x] After a release build, `grep -q 'com.google.firebase.crashlytics' androidApp/build/outputs/mapping/release/mapping.txt` succeeds
- [x] After `./gradlew :androidApp:assembleDebug :androidApp:assembleRelease -Ppolleninfo.uploadCrashlyticsMapping=false`, `grep -l 'firebase_crashlytics_collection_enabled' androidApp/build/intermediates/merged_manifest/*/process*MainManifest/AndroidManifest.xml` matches the debug manifest only
- [ ] ~~Manual: a provoked crash and a provoked ANR in a local release build appear in the Crashlytics console with a deobfuscated stack trace, app version, device model and Android version; the same crash in a debug build does not appear~~ *(skipped: needs a device, a signed release install and the Firebase console; procedure written in deploy/README.md "Crash reporting manual check")*
- [x] `grep -rnE 'setCustomKey|setUserId|recordException|Crashlytics.*\.log\(' composeApp/src androidApp/src` finds nothing
- [x] `deploy/README.md` contains the "Crash reporting manual check" section and the manual release steps

### Quality gates

- [x] No new deprecation or compiler warning in the build output (only the accepted AGP `Configuration.setVisible` and KGP `KotlinNativeBundleArtifactsTypes` ones)
- [x] A second identical `assembleRelease -Ppolleninfo.uploadCrashlyticsMapping=false` run prints "Reusing configuration cache."
- [x] Every library and plugin is added through `libs.versions.toml`; no version literals in `build.gradle.kts` files
- [x] `./gradlew :composeApp:testAndroidHostTest` passes
- [x] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes

## Task [02-crash-reports-opt-out]

A user can switch crash reporting off (and on again) in a new Privacy section of the Settings tab,
logged in or not. The choice is stored on the phone, defaults to on, applies immediately in the
running app and at every process start (including processes started by FCM without an activity),
and survives restarts. Debug builds stay silent whatever the switch says. On iOS the Privacy
section does not appear. The stored preference is the single source of truth: Settings only writes
it, and one synchronising component passes every stored value to the platform crash reporter.

Shapes decided in the requirements annex:

```kotlin
interface CrashReporter { val isAvailable: Boolean; fun setEnabled(enabled: Boolean) }
interface CrashReportingPreferences {
    val enabled: Flow<Boolean>                     // true when nothing is stored
    suspend fun setEnabled(enabled: Boolean): Result<Unit>
}
class CrashReportingSync(preferences, reporter) { suspend fun run() } // applies now and on every change
```

### Implementation steps

- [ ] Add `CrashReporter` in a new `core/crashreporting` package; Android `FirebaseCrashReporter(collectionAllowed)` (always disables when `collectionAllowed` is false, derived from the app's debuggable flag in `platformModule`); iOS `NoCrashReporter` (`isAvailable = false`, no-op); bind both in the platform modules
- [ ] Add `CrashReportingPreferences` and a logic-free `DataStoreCrashReportingPreferences` on the default preferences DataStore (key `crash_reports_enabled`, write in `safeCall`); bind it in DI
- [ ] Add `CrashReportingSync`: returns at once if the reporter is unavailable, otherwise collects the distinct preference values into `setEnabled`, catching flow failures; bind it as a single
- [ ] Start the sync once per process from `PollenInfoApplication.onCreate` after Koin, on an application-lifetime scope
- [ ] Extend `SettingsUiState` with `crashReportsAvailable` and `crashReportsEnabled`; `SettingsViewModel` observes the preference and gets `onCrashReportsToggled(enabled)` which updates the state and writes the preference (never calling the reporter)
- [ ] Add the Privacy section to `SettingsScreen`: title, a toggleable row (`Role.Switch`) with label, one-sentence explanation as supporting text and a `Switch`; hidden when `crashReportsAvailable` is false
- [ ] Add `settings_privacy_title`, `settings_crash_reports_label`, `settings_crash_reports_description` in English, German (Swiss, "du", no "ß"), French ("vous", U+202F) and Italian ("Lei"); the description names Google Firebase and states that no location, account or diary data is included
- [ ] Add `FakeCrashReporter` and `FakeCrashReportingPreferences` to `commonTest`; write `CrashReportingSyncTest` and extend `SettingsViewModelTest`
- [ ] Update `CLAUDE.md`: `core/` table gains `crashreporting`; the opt-out rule (preference is the source of truth applied by `CrashReportingSync`; Crashlytics identifies the install by the same FID as push; the preference is not a secret and stays in backup); Settings behaviour. Extend the manual check in `deploy/README.md` with the switch-off case

### Acceptance criteria

- [ ] `CrashReportingSyncTest`: nothing stored → reporter enabled; stored `false` → disabled at start; every change reaches the reporter, equal consecutive values once; unavailable reporter never called; a failing preference flow neither crashes nor calls the reporter
- [ ] `SettingsViewModelTest`: `crashReportsEnabled` follows the stored value; toggling writes the preference and shows the new value — also while signed out; the ViewModel never calls the reporter's `setEnabled`
- [ ] `SettingsViewModelTest`: an available reporter gives `crashReportsAvailable == true`, an unavailable one `false`
- [ ] `compileTestKotlinIosSimulatorArm64` passes and the iOS `platformModule` binds `NoCrashReporter` (runtime check on iOS deferred — there is no `iosApp` yet)
- [ ] Manual (Android release): fresh install shows the switch on; switching it off and provoking a crash sends nothing; the switch is still off after force-stopping and reopening the app; switching it on again reports the next crash; with the switch off, Home, All stations, Diary, Alarms and Settings work as before
- [ ] Manual (Android debug, switch on): a provoked crash does not appear in the console, and the merged-manifest grep still matches the debug manifest only

### Quality gates

- [ ] `./gradlew :composeApp:testAndroidHostTest` passes
- [ ] `./gradlew :composeApp:checkTranslations` passes
- [ ] The hard-coded-text grep from `CLAUDE.md` finds no new hit
- [ ] No new deprecation or compiler warning
- [ ] Test names are backtick sentences without commas; no mocking framework; `Result` imported explicitly from `core.result`

## Task [03-opt-out-save-failure-and-polish]

If storing the user's crash-report choice fails, the Settings switch returns to its previous
position and a snackbar says the choice could not be saved, so the switch never shows something
untrue. Settings has had no one-shot events so far; this task introduces its event channel and an
in-screen snackbar host. It also completes the hands-on checks of the Privacy section: the four
languages and screen-reader access.

### Implementation steps

- [ ] Add `sealed interface SettingsEvent` with `CrashReportsSaveFailed`, exposed from `SettingsViewModel` as a buffered `Channel` via `receiveAsFlow()`
- [ ] In `onCrashReportsToggled`, on `Result.Failure` restore the previous `crashReportsEnabled` value and send `CrashReportsSaveFailed`
- [ ] Give the `SettingsScreen` `Scaffold` a `SnackbarHost`, collect the events in a `LaunchedEffect` and show the translated message
- [ ] Add `settings_crash_reports_save_failed` in all four languages
- [ ] Make the next write of `FakeCrashReportingPreferences` failable and extend `SettingsViewModelTest`
- [ ] Update the "UI state and events" rule in `CLAUDE.md`: a snackbar that concerns only the screen itself may be shown in-screen, without a lambda from `AppNavigation`

### Acceptance criteria

- [ ] `SettingsViewModelTest`: a failed write restores the previous switch value and the preference still holds the old value
- [ ] `SettingsViewModelTest`: a failed write sends `CrashReportsSaveFailed` exactly once; a successful write sends no event
- [ ] Manual (Android): with a scratch change making `DataStoreCrashReportingPreferences.setEnabled` return `Result.Failure`, toggling shows the snackbar in the app language and the switch springs back; then revert the scratch change
- [ ] Manual (Android): with TalkBack, the crash-report row is announced with its label and on/off state and can be toggled
- [ ] Manual (Android): with the app language set to German, French and Italian in turn, the Privacy title, switch label, explanation and save-failure message appear in that language

### Quality gates

- [ ] `./gradlew :composeApp:testAndroidHostTest` passes
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [ ] `./gradlew :composeApp:checkTranslations` passes
- [ ] No new deprecation or compiler warning; the hard-coded-text grep finds no new hit
- [ ] Test names are backtick sentences without commas; no mocking framework; `Result` imported explicitly from `core.result`
