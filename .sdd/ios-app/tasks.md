# iOS App — Tasks

Feature: `ios-app` · Requirements: `.sdd/ios-app/requirements.md`

**Common quality gates** (every task, unless it says otherwise):
- `./gradlew :composeApp:testAndroidHostTest` passes; no Kotlin compiler warning on any target (`allWarningsAsErrors`).
- `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes; from task 02 on, `./gradlew :composeApp:iosSimulatorArm64Test` passes.
- `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration {Debug,Release} -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build` succeeds for both configurations with no new warning.
- No signing team, personal bundle identifier or private backend address is committed.
- Rows this task adds to the manual checklist have a recorded result.

---

## Task [01-app-runs-on-simulator]

The tracer bullet: an Xcode project that builds the shared Kotlin code, hosts the shared app and
launches it on the iOS Simulator, talking to the backend chosen by its build configuration. Debug
uses the local backend with plain HTTP allowed; Release always uses production with no exception.
Both have their final bundle identifiers and display names, install side by side, iPhone only,
portrait and landscape. **Simulator only** — device installs start in task 04, with the local
settings file, so the final identifier is never registered with Apple.

### Implementation steps

- [x] Create the Xcode project with one iPhone app target, a file-system-synchronized source group, deployment target iOS 18.x (lowest that links without Skiko warnings), simulator `x86_64` excluded, portrait and both landscape orientations. Do not set an app icon name yet (task 08), so no missing-icon warning appears.
- [x] Add the "Compile Kotlin" build phase running the shared framework embed task, with the JDK lookup order `JAVA_HOME` → `/usr/libexec/java_home` → Android Studio's bundled JBR, and a clear failure message when none is found.
- [x] Add the SwiftUI entry point that reads the backend address from the app's Info.plist and hosts the shared main view controller full-screen (ignoring safe areas; Compose pads itself).
- [x] Change the shared iOS entry point to take the backend address as a parameter and remove the hard-coded constant.
- [x] Add `Base`/`Debug`/`Release` build configuration files: base identifier with `.debug` suffix in Debug, display names "PollenInfo (debug)" / "PollenInfo", backend address per configuration, the Debug-only preprocessing flag for local networking, `MARKETING_VERSION = 1.0.0` and `CURRENT_PROJECT_VERSION = 1`.
- [x] Hand-write the preprocessed Info.plist (backend address, display name, version keys from build settings, orientations, 120 Hz key, local-networking exception guarded by the Debug-only flag) and a minimal `UILaunchScreen`.
- [x] Create the manual checklist skeleton (columns: item, Simulator result, iPhone result) with rows for launch and backend per configuration.
- [x] Update CLAUDE.md: iOS row of the base-URL table, iOS build commands, and drop "only CLT here".

### Acceptance criteria

- [ ] ~~With the local backend running, the Debug app on an iPhone Simulator shows onboarding, and after picking a station Home shows readings served by the local backend.~~ *(skipped: no Simulator UI or tapping tool on this Mac, so the station could not be picked by hand. Verified instead: onboarding shows with the station list from the local backend; with the station stored in the preferences file, Home shows Zürich's reading and the server logs `GET /pollen/stations/PZH/measurements`. The maintainer should pick a station in onboarding once.)*
- [x] The Release app on the Simulator shows readings from `https://api.polleninfo.ch`.
- [x] The processed Release Info.plist contains no App Transport Security exception; the Debug one contains `NSAllowsLocalNetworking`.
- [x] Built bundle identifiers are `ch.stenzel.tim.polleninfo.debug` (Debug) and `ch.stenzel.tim.polleninfo` (Release); both apps appear side by side on the Simulator home screen as "PollenInfo (debug)" and "PollenInfo".
- [x] `env -u JAVA_HOME xcodebuild … -configuration Debug … build` succeeds.
- [x] `grep -rn 'localhost:8080\|api.polleninfo.ch' composeApp/src` finds nothing.

### Quality gates

- [x] Common quality gates pass (except `iosSimulatorArm64Test`, task 02).
- [x] `grep -c '/\*' iosApp/iosApp/Info.plist` prints 0.

---

## Task [02-shared-suite-green-on-ios]

The whole shared test suite runs and passes on an iOS Simulator for the first time (until now it
was only compiled for iOS). Any test or code that fails only on iOS is fixed without changing
Android behaviour.

### Implementation steps

- [x] Run `./gradlew :composeApp:iosSimulatorArm64Test` and collect failures. *(807 tests on an iPhone 17 Simulator, iOS 27.0: no failure.)*
- [x] Fix each iOS-only failure in `iosMain` or, if the defect is in shared code, in a way that keeps Android behaviour identical; shared fixes keep or gain a common test. *(None to fix.)*
- [x] Note in a test comment any test that documents probably-wrong behaviour (project rule). *(No test changed; none found.)*

### Acceptance criteria

- [x] `./gradlew :composeApp:iosSimulatorArm64Test` passes locally.
- [x] `./gradlew :composeApp:testAndroidHostTest` still passes with the same number of tests or more. *(807, the same as on iOS.)*
- [x] Every shared-code change made here is covered by a common test that fails without it. *(No shared code changed.)*

### Quality gates

- [x] Common quality gates pass.
- [x] No test is deleted or disabled to make the suite pass.

---

## Task [03-ios-ci-pipeline]

CI verifies the iPhone app on every relevant pull request: a new iOS pipeline runs the shared test
suite on an iOS Simulator and builds the app in Debug and Release; the Android gate is renamed;
the rule mapping changed files to pipelines covers the iOS wrapper and has a self-test that runs
in every pipeline.

### Implementation steps

- [x] Rename the Android gate to `android-tests` (and the release job's dependency) and remove the iOS compile job from the Android workflow; update its header comment.
- [x] Add the iOS workflow: same triggers and least-privilege permissions as the others, own concurrency group, a change-detection job, and `ios-tests` on a pinned macOS runner (not `macos-latest`) with a pinned Xcode selected by `xcode-select`, running the iOS Simulator test task and unsigned Debug and Release Simulator builds; all actions pinned by commit SHA. Check which Xcode the runner offers; if not the local one, pin the newest available that builds without warnings and record the choice. *(Xcode 27 only on the preview image `xcode-27`; pinned `macos-26` + Xcode 26.6, the newest stable, recorded in `deploy/README.md`, "iOS". Whether 26.6 builds without warnings shows on the first CI run.)*
- [x] Update the change-detection mapping: iOS wrapper and iOS workflow → ios; Android workflow → android only; header comment updated.
- [x] Add the table-driven self-test (temporary git repo, commits made with an explicit throwaway identity and signing off; bash 3.2 compatible — no associative arrays, no `mapfile`): one case per mapping row, docs-only, mixed, all-zero base, unresolvable base, and Android workflow → android only. Run it as the first step of every workflow's change-detection job.
- [x] Update CLAUDE.md, README and the deploy runbook: three workflows, `android-tests` / `ios-tests`, required-checks list, the self-test.
- [x] Write the PR description reminder for the maintainer to replace the required checks. *(For the handover's "For the reviewer": "Before merging, replace the required checks `android-gate` and `ios-compile` with `android-tests` and `ios-tests` in the rulesets `develop` and `main` (Settings → Rules → Rulesets); otherwise the old checks never report and block the merge.")*

### Acceptance criteria

- [x] The self-test passes locally (with `/bin/bash` on macOS) and fails with an expected-vs-actual message when one mapping row is deliberately broken. *(bash 3.2.57: 28 cases pass; with `iosApp/*` removed from the ios row in a copy: `FAIL: iosApp/iosApp/iOSApp.swift`, `expected: ios=true` / `actual: ios=false`, exit 1.)*
- [ ] ~~On the pull request, `ios-tests` runs and passes, including both Xcode builds; `android-tests` and the Server pipeline pass; the release job depends on `android-tests`.~~ *(skipped: the workflows run only on pull requests, which the maintainer opens; no `gh` here. Locally: the same Gradle task and both `xcodebuild`s pass, `release` has `needs: [changes, android-tests]`. Check on the PR.)*
- [ ] ~~A shared test that fails only on iOS makes `ios-tests` fail (checked once with a temporary failing test on a scratch branch, then removed).~~ *(skipped: needs a pull request to trigger the workflow, which the assistant may not open. The step runs `iosSimulatorArm64Test`, whose failure fails the job; the maintainer can confirm once with a throwaway PR.)*
- [x] `grep -rn 'android-gate\|ios-compile' .github README.md deploy CLAUDE.md` finds nothing. *(exit 1, no output.)*
- [ ] ~~The PR description tells the maintainer to replace the required checks `android-gate`/`ios-compile` with `android-tests`/`ios-tests` before merging.~~ *(skipped: the description is written at the handover after task 09; the reminder text is recorded in the step above.)*
- [x] The iOS workflow names a fixed macOS runner image and a fixed Xcode version, and the chosen version is recorded in the runbook. *(`runs-on: macos-26`, `XCODE_APP: /Applications/Xcode_26.6.app` + `xcode-select`; `deploy/README.md` "iOS" records Xcode 26.6 and why.)*

### Quality gates

- [x] Common quality gates pass. *(Re-run with `--rerun`: 807/807 on the JVM and on the iOS Simulator; iOS compile up to date; `xcodebuild` Debug and Release `BUILD SUCCEEDED` with 0 warnings; only the accepted `Configuration.setVisible` deprecation; no team, identifier or address in the diff.)*
- [x] Every action in the new workflow is pinned by commit SHA; the workflow grants `contents: read` only.
- [x] Both scripts use `set -euo pipefail` and pass `bash -n`.

---

## Task [04-install-on-own-iphone]

A developer with a free Apple ID installs both builds on their own iPhone without committing
anything personal and without claiming the app's final identifier: a git-ignored local settings
file supplies the signing team, a throwaway base identifier and, for Debug only, a backend address
reachable from the phone.

### Implementation steps

- [x] Include an optional, git-ignored local settings file from both configurations: Debug's backend address placed before the include (overridable), Release's after it (always production).
- [x] Commit an example file documenting the signing team, base identifier and Debug backend address.
- [x] Ignore the local settings file in git.
- [ ] ~~Try the Debug build against the Mac's LAN address on the iPhone; if iOS's Local Network prompt appears or blocks requests, raise with the maintainer whether to add a Debug-only local-network usage text (not covered by the requirements yet).~~ *(skipped: no iPhone connected and no signing identity on this Mac. Verified on the Simulator that a Local `API_BASE_URL` reaches the Debug Info.plist with `NSAllowsLocalNetworking`. Raised with the maintainer, who decided on 2026-10-22 to add a Debug-only `NSLocalNetworkUsageDescription` (English, inside `#if POLLENINFO_LOCAL_NETWORKING`); the built Debug Info.plist has it, Release has not. Checklist row 6.)*
- [x] Document the Personal Team setup and the seven-day expiry in README and CLAUDE.md; add checklist rows for device install and LAN backend.

### Acceptance criteria

- [x] Without a local settings file, `xcodebuild -showBuildSettings` for Debug and Release shows the identifiers and addresses from task 01. *(Debug `ch.stenzel.tim.polleninfo.debug` / `http://localhost:8080`, Release `ch.stenzel.tim.polleninfo` / `https://api.polleninfo.ch`, no `DEVELOPMENT_TEAM`.)*
- [ ] ~~With a local file setting a throwaway base identifier and team, `xcodebuild -showBuildSettings` for Debug and Release shows only the throwaway `PRODUCT_BUNDLE_IDENTIFIER`s, and both builds install and launch on the maintainer's iPhone.~~ *(skipped in part: no iPhone connected and no signing identity here. Build settings verified with a test `Local.xcconfig`: `ch.example.throwaway.polleninfo.debug` / `ch.example.throwaway.polleninfo`, team `TEST123456` in both. Install and launch on the iPhone: checklist row 5, for the maintainer.)*
- [ ] ~~With a local Debug backend address set to the Mac's LAN IP, the Debug app on the iPhone shows readings from the local backend.~~ *(skipped: no iPhone connected. The built Debug Info.plist has `API_BASE_URL = http://192.168.1.20:8080` and `NSAllowsLocalNetworking = true`. Checklist row 6, for the maintainer.)*
- [x] With a backend address in the local file, the processed Release Info.plist still contains `https://api.polleninfo.ch`. *(Simulator Release build with a test `Local.xcconfig` setting `http://192.168.1.20:8080`: `plutil -extract API_BASE_URL` → `https://api.polleninfo.ch`, no `NSAppTransportSecurity`.)*
- [x] `git check-ignore iosApp/Config/Local.xcconfig` exits 0, and `git ls-files iosApp/Config` lists the example but not the local file. *(exit 0; `Base`, `Debug`, `Local.xcconfig.example`, `Release`.)*

### Quality gates

- [x] Common quality gates pass. *(JVM and iOS Simulator test tasks and iOS compile pass (no Kotlin changed, up to date); `xcodebuild` Debug and Release `BUILD SUCCEEDED`, 0 warnings; only placeholders (`ABCDE12345`, `ch.example.yourname`, `192.168.1.20`) in the example and README; checklist rows 5–7 recorded.)*

---

## Task [05-nearest-station-on-iphone]

Onboarding and Change station suggest the nearest station on iPhone: iOS asks for location with a
PollenInfo explanation in the app's language, the reduced-accuracy fix picks the nearest station,
and denying keeps the list usable. The iOS location code runs here for the first time.

### Implementation steps

- [x] Add the location usage description, reduced-accuracy key, `CFBundleLocalizations` (en, de, fr, it) and development region en to the Info.plist.
- [x] Add the Info.plist string catalog with the four approved location texts. *(`iosApp/iosApp/InfoPlist.xcstrings`; `de`, `fr`, `it` added to the project's known regions; the built app has `en/de/fr/it.lproj/InfoPlist.strings`.)*
- [x] Run onboarding on the Simulator (simulated location in Switzerland), allowing and denying; fix any defect in the iOS location provider or permission requester. *(Driven by a throwaway XCUITest runner in the scratchpad, not committed. No defect in the provider or requester. One finding outside the app's control: iOS words the prompt in the device language, not the app language chosen in PollenInfo's Settings — see the first criterion.)*
- [x] Replace the "compile-verified only" remarks on the iOS location code with what was checked; add checklist rows. *(Rows 8–14; CLAUDE.md's platform-implementations line and `Info.plist` table updated.)*

### Acceptance criteria

- [ ] ~~On a fresh install, choosing location in onboarding shows the iOS prompt with the English text; with the app language set to German, French or Italian (and relaunched), the prompt shows that language's text.~~ *(accepted by the maintainer on 2026-10-22 as an iOS limitation. Failed in part: English passes on a fresh install. With the **device** language set to German, French or Italian, the prompt shows that language's text (requirement 11 as worded). With the device in English and German chosen in PollenInfo's Settings, then a relaunch, the app is German and iOS Settings lists its language as German, but the prompt stays English: iOS words it in the device language. The app cannot change this. Checklist row 10.)*
- [x] Allowing location with a simulated position near Zurich suggests the Zurich station. *(`simctl location set 47.3769,8.5417`, "Allow While Using App" → `Station, Zürich`.)*
- [x] Denying location shows the shared "not allowed" message and the station list stays usable. *("Location permission is off. Enable it in Settings to find your nearest station."; then picking Bern → `Station, Bern`, message cleared.)*
- [x] With Simulator location set to None, picking a station while the lookup runs keeps that station after the 10 s timeout. *(`simctl location clear` plus a Simulator reboot (otherwise the last fix is reused). Alone, the lookup ends in "Your location could not be determined…"; with Bern picked while the button was disabled (lookup running), 13 s later still `Station, Bern` and no message.)*
- [x] After completing onboarding and relaunching, Home opens with the chosen station. *(Luzern picked, Continue, app terminated and relaunched → Home shows Luzern; server log `GET /pollen/stations/PLZ/measurements`.)*

### Quality gates

- [x] Common quality gates pass (including `StationPickerTest` within `iosSimulatorArm64Test`). *(807/807 on the JVM and on the iOS Simulator; iOS compile passes; `xcodebuild` Debug and Release `BUILD SUCCEEDED`, 0 warnings; no team, identifier or address in the diff; checklist rows 8–14 recorded.)*
- [x] The four location texts follow the language style rules (Swiss "ss", "du"; formal fr/it; typographic apostrophes; U+202F before French `? ! ;` if any). *(de: "Dein/dein", no "ß"; fr: "Votre", no `? ! ;`; it: "Sua/Suo"; no apostrophes in any text.)*

---

## Task [06-credentials-kept-out-of-backups]

On iPhone, the login and the device credential are stored where iCloud backup and device transfer
never copy them, while the station and other preferences stay backed up — matching Android.
(The device store is only written once push exists; its location is set up now.)

### Implementation steps

- [ ] Move all three on-device stores from Documents to Application Support (created if missing).
- [ ] Put the device and session stores in a `secrets` subdirectory and mark that directory excluded from backup on every launch; a failure to mark it is reported, not swallowed.
- [ ] Update the iOS storage remarks; add the iOS exclusion to CLAUDE.md beside the Android backup rules; add checklist rows.

### Acceptance criteria

- [ ] On the Simulator, after logging in, the session store file exists under `Library/Application Support/secrets/` and the preferences file under `Library/Application Support/`; `Documents/` is empty.
- [ ] The device store's path resolves into `secrets/` (code review).
- [ ] `xattr -p com.apple.metadata:com_apple_backup_excludeItem "$(xcrun simctl get_app_container booted ch.stenzel.tim.polleninfo.debug data)/Library/Application Support/secrets"` prints `com.apple.backupd`. This check stands in for the iCloud-restore acceptance test.
- [ ] Logging in, closing and relaunching keeps the session and the selected station.
- [ ] A fresh install with no Application Support directory starts and completes onboarding.

### Quality gates

- [ ] Common quality gates pass.
- [ ] The CLAUDE.md "No hard-coded text" grep finds no new hit outside the allowed list (or the list is updated for a new exception/log message).

---

## Task [07-version-and-debug-names]

Both platforms show comparable versions and mark their debug builds the same way: the iPhone
version line in Settings shows 1.0.0 with the commit count as build number, and the Android debug
build is named "PollenInfo (debug)". (Android debug keeps its `-debug` version suffix; iOS debug
shows plain `1.0.0`, since the display name already marks it — intended.)

### Implementation steps

- [ ] Add cross-reference comments between the iOS marketing version and the Android version name.
- [ ] Add the build phase that writes the git commit count (fallback 1) into the built Info.plist on every build.
- [ ] Add the debug-only Android app name "PollenInfo (debug)".
- [ ] Replace the "compile-verified only" remark on the iOS app-info code with what was checked; add checklist rows.

### Acceptance criteria

- [ ] Settings on the iPhone Simulator shows version 1.0.0 with build number equal to `git rev-list --count HEAD`.
- [ ] After a new commit and an incremental rebuild, the build number shown increases.
- [ ] Building a `git archive` export (no `.git`) succeeds with build number 1.
- [ ] The Android debug build's launcher name is "PollenInfo (debug)"; the release build's is "PollenInfo".

### Quality gates

- [ ] Common quality gates pass.
- [ ] `./gradlew :androidApp:assembleDebug :androidApp:assembleRelease -Ppolleninfo.uploadCrashlyticsMapping=false` passes.

---

## Task [08-icon-launch-screen-privacy-manifest]

The iPhone app looks finished on the home screen and at launch: an app icon in default, dark and
tinted variants designed in Figma and approved by the maintainer, a launch screen in the app's
surface colour for light and dark with no flash into the first screen, and a privacy manifest
declaring no tracking.

### Implementation steps

- [ ] On the Figma "App Icon" page, add an iOS section with Default, Dark and Tinted 1024×1024 frames from the gauge concept, re-fitted to Apple's grid and bound to the file's colour variables.
- [ ] Ask the maintainer to review in Figma; record the approval (date) before exporting.
- [ ] Export the three PNGs into the app icon set with dark and tinted appearances and set the app icon name.
- [ ] Add the launch background colour (`#FBFAF3` light, `#16211A` dark), point the launch screen at it, and give the SwiftUI host view the same background.
- [ ] Add the privacy manifest: no tracking, no tracking domains, no collected data, UserDefaults reason `CA92.1`.
- [ ] Note in CLAUDE.md that the iOS icon's source is the Figma page; add checklist rows.

### Acceptance criteria

- [ ] The maintainer's approval of the three frames is recorded before export.
- [ ] The Simulator home screen shows the gauge icon in default, dark and tinted modes with the matching variant.
- [ ] A screen recording of launch in light and in dark appearance shows no frame in a colour other than the surface colour before the first screen.
- [ ] The built app bundle contains the privacy manifest with the declared values.

### Quality gates

- [ ] Common quality gates pass (asset catalog compiles without warnings).
- [ ] `sips -g pixelWidth -g pixelHeight -g hasAlpha` reports 1024 / 1024 / no for the default and dark PNGs, 1024 / 1024 for tinted.

---

## Task [09-full-iphone-run-and-docs]

The complete manual checklist is run on the Simulator and the maintainer's iPhone, remaining iOS-
only defects are fixed, and the project documents the real iOS app instead of saying the wrapper
doesn't exist.

### Implementation steps

- [ ] Add the remaining checklist rows: Home + refresh + stale warning, All stations list/map tap, Account register/login/logout/delete with keyboard, Diary after registering, Settings language relaunch / version / links, Alarms "not available" message, back swipe from Change station and Account, dark mode, landscape and safe areas, Release build against production.
- [ ] Run the full checklist on the Simulator and the iPhone; record results.
- [ ] Fix iOS-only defects found (shared fixes keep Android unchanged and get a common test where testable); raise anything needing a design decision with the maintainer.
- [ ] Remove the remaining "compile-verified only / no wrapper yet" remarks (language, notification permission, push provider, entry point) with what was checked.
- [ ] Replace CLAUDE.md's "iOS wrapper configuration" section with the real wrapper (configurations, local settings file, build phases, preprocessed Info.plist, icon source, storage) and update README's iOS getting-started.

### Acceptance criteria

- [ ] Every checklist item passes on the Simulator and the iPhone, or has a raised issue the maintainer accepted.
- [ ] After registering, the Diary tab loads; changing the language shows the "applies next time" note, and after relaunch the app is in the new language.
- [ ] Opening Alarms and allowing notifications shows "Push notifications aren't available on this device yet" and no list.
- [ ] Swiping back from Change station after picking a different station without saving returns to Settings with the previous station; swiping back from Account returns to the previous screen.
- [ ] Registering never hides the focused field behind the keyboard, and in landscape on a Dynamic Island device no content is hidden.
- [ ] `grep -rn 'compile-verified\|no wrapper\|There is no .iosApp\|only CLT' composeApp CLAUDE.md README.md` finds nothing.

### Quality gates

- [ ] Common quality gates pass.
- [ ] `./gradlew :composeApp:checkTranslations :server:test :androidApp:lintRelease :androidApp:assembleDebug :androidApp:assembleRelease -Ppolleninfo.uploadCrashlyticsMapping=false` passes.
- [ ] The CLAUDE.md "No hard-coded text" grep finds only the allowed hits.
