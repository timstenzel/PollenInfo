# Tasks: iOS App with Feature Parity

Requirements: `.sdd/ios-app/requirements.md`. Tasks are numbered in implementation order; each names
the earlier tasks it needs.

Notes from verifying the Technical Annex against the codebase (`abc43f8`), no conflicts:

- `.gitignore` ignores `*.xcworkspace`, which would hide the SPM `Package.resolved` the annex wants
  committed — task 03 adds a negation.
- `checkTranslations` lives inside `composeApp/build.gradle.kts`; the `.strings` generator's pure
  transformation needs a unit-testable home (`buildSrc` with tests) — task 03.
- `parseAlarmPayload` depends on `speciesNameResource` (Compose) and `PollenSeverity`; the annex
  leaves the exact cut to implementation — task 04.
- The account fields already set `ContentType.EmailAddress + Username` / `Password` / `NewPassword`
  (`AccountScreen.kt`, `PasswordField.kt`) — task 05 only verifies they reach iOS autofill.

Real-iPhone criteria (push, autofill) can only be run once the manual Apple/Firebase prerequisites
exist (Further Notes in the requirements). Until then they stay unchecked; every other criterion of
the task must pass without them.

Accepted plugin warnings (everything else counts as new): AGP 9.4 `Configuration.setVisible`, KGP 2.4
`KotlinNativeBundleArtifactsTypes`.

## Task [01-iphone-app-runs-on-simulator]

A developer opens one checked-in Xcode project, builds the development or release configuration, and
runs the full shared app on an iPhone simulator: onboarding with approximate location, Home, All
stations with the map, Diary with an account, Settings. Debug talks to the Mac's server (overridable
by an untracked file), Release to production; both install side by side. The release build archives.
The shared app tests also run on the iOS simulator, and the iOS manual test checklist exists. Alarms
still report push as unavailable on iPhone (task 03); password manager and immediate language switch
come later.

### Implementation steps

- [x] `MainViewController` takes `apiBaseUrl` from its caller; remove the hard-coded `localhost`
      constant and its KDoc.
- [x] Create `iosApp/iosApp.xcodeproj` with the `iosApp` SwiftUI target hosting `MainViewController`
      via `UIViewControllerRepresentable`; iPhone only (`TARGETED_DEVICE_FAMILY = 1`), orientation as
      on Android, Compose content respecting safe areas.
- [x] Run Script phase `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode` (no CocoaPods), with
      `ENABLE_USER_SCRIPT_SANDBOXING = NO` and a `JAVA_HOME` fallback to `/usr/libexec/java_home`
      (Xcode does not read `~/.zshrc`).
- [x] `Config/Base.xcconfig` (`DEVELOPMENT_TEAM` empty, "Sign to Run Locally" for the simulator),
      `Debug.xcconfig`, `Release.xcconfig`, optional untracked `Config/Local.xcconfig` (`#include?`)
      overriding `API_BASE_URL`, git-ignored. Bundle id, display name, `API_BASE_URL` and
      `NSAllowsLocalNetworking` (Debug only) per the annex table.
- [x] `Info.plist`: `API_BASE_URL` exposed to Swift, `NSLocationWhenInUseUsageDescription` (localized
      via `InfoPlist.strings` for en/de/fr/it in the project's language style),
      `NSLocationDefaultAccuracyReduced`, `CFBundleLocalizations` (en, de, fr, it),
      `CFBundleDevelopmentRegion` en, `CFBundleShortVersionString` / `CFBundleVersion`, `UILaunchScreen`
      with a colour asset equal to `:theme` `surfaceLight` / `surfaceDark`.
- [x] Assets: `AppIcon` 1024 px from the Android adaptive icon (dark/tinted variants if clean);
      `PrivacyInfo.xcprivacy` declaring UserDefaults required-reason use and no tracking.
- [x] Make `./gradlew :composeApp:iosSimulatorArm64Test` run on Xcode 27 (configure the simulator
      device if KGP's default is not installed) and pass, fixing any shared code or test that behaves
      differently on Kotlin/Native.
- [x] Create the iOS manual test checklist (`deploy/README.md` § iOS, or a file it links) listing every
      manual UAT 1–33 with a result column and noting which need a real iPhone.
- [x] Docs: CLAUDE.md iOS wrapper section replaces "There is no `iosApp` Xcode project yet"; commands
      table gains the iOS simulator test, Xcode build and archive commands; "only CLT here" removed;
      README build instructions.

### Acceptance criteria

- [x] `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' build`
      and the same with `-configuration Release` succeed from a clean checkout; launched in a simulator,
      the Debug app shows onboarding on first launch (UAT 1).
- [ ] ~~The built bundles have ids `ch.stenzel.tim.polleninfo.debug` / `ch.stenzel.tim.polleninfo` and
      display name "PollenInfo"; both installed in one simulator keep separate station selections
      (UAT 2, station part). The Debug `Info.plist` has `API_BASE_URL` `http://localhost:8080` (or the
      `Local.xcconfig` value) and `NSAllowsLocalNetworking`; the Release one has
      `https://api.polleninfo.ch` and no `NSAllowsLocalNetworking` (UAT 3).~~ *(skipped: bundle ids, display name and both Info.plist URL/ATS values verified in the built bundles, and both apps install side by side with separate data containers; choosing a station in each needs taps in the simulator, which this environment cannot drive — UAT 2 station part is open in iosApp/CHECKLIST.md)*
- [x] The built bundle contains `PrivacyInfo.xcprivacy` with `NSPrivacyAccessedAPICategoryUserDefaults`
      (reason `CA92.1`) and `NSPrivacyTracking` false, and `AppIcon` in its asset catalog (story 2, 50).
- [x] `./gradlew :composeApp:iosSimulatorArm64Test :composeApp:testAndroidHostTest` passes.
- [x] `xcodebuild archive -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Release -destination 'generic/platform=iOS' -archivePath build/PollenInfo.xcarchive`
      succeeds (with `CODE_SIGNING_ALLOWED=NO` until a Team ID exists).
- [ ] ~~Manual checklist run on the simulator and recorded: dark-mode launch without light flash
      (UAT 4); localized location explanation and approximate-only, nearest station preselected
      (UAT 5, 6); nothing under notch / Dynamic Island / home indicator (UAT 7); edge swipe in the alarm
      editor with an unsaved change shows the discard question (UAT 8); Home levels, All stations list +
      map tap, registration, diary feeling on the chart (UAT 9–12).~~ *(skipped: UAT 1, 3 and 4 run and recorded in iosApp/CHECKLIST.md; UAT 5–12 need taps and swipes in the simulator, which this environment cannot drive (no Docker for the local backend either, and registration against production was not attempted) — to be run by hand)*

### Quality gates

- [x] Debug and Release simulator builds and `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64`
      produce no new warning.
- [x] After a Debug build, `git status --porcelain` is empty apart from intended source files and
      `git check-ignore iosApp/Config/Local.xcconfig` succeeds.
- [x] `./gradlew :androidApp:assembleDebug :composeApp:checkTranslations` succeeds.
- [x] `grep -nE 'no .iosApp. Xcode project|only CLT' CLAUDE.md README.md` finds nothing.

## Task [02-devices-state-their-platform]

Every device registration says which platform it runs on, end to end: the Android app sends
`ANDROID`, the iPhone app sends `IOS`, the backend stores it, refuses a registration without a valid
platform, and hands the platform to the push sender at alarm time. Android alarm messages stay
byte-for-byte as before.

Requires: none (01 for the iOS binding to be exercised).

### Implementation steps

- [ ] Migration `V3__device_platform.sql` exactly as in the annex; mirror the column in `Tables.kt`;
      `DevicePlatform { ANDROID, IOS }` in `alarm/domain`.
- [ ] `POST /devices` reads `{fcmToken, platform}`; missing/blank token or missing/unknown platform →
      `400 {error}` (never repeating the token). `PUT /devices/me/fcm-token` unchanged.
- [ ] `DeviceStore.register(fcmToken, platform)`; `AlarmWithToken(alarm, fcmToken, platform)` from
      `enabledWithDeliverableDevice()`; `AlarmScheduler` passes the platform on.
- [ ] `PushSender.send(token, platform, message)` in `FcmPushSender`, `LoggingPushSender` and
      `FakePushSender`; `FcmPushSender` keeps the Android JSON unchanged (iOS block in task 03).
- [ ] App: `RegisterDeviceRequestDto(fcmToken, platform)`; the platform comes from a `platformModule`
      binding (Android `ANDROID`, iOS `IOS`) through `AlarmRepositoryImpl` to
      `AlarmApiService.registerDevice`.
- [ ] CLAUDE.md REST table (`POST /devices` body and `400` cases) and device notes.

### Acceptance criteria

- [ ] `MigrationTest`: V3 applies on a database with existing devices, which become `ANDROID`; an
      insert without platform fails; an unknown value violates the check constraint.
- [ ] `AlarmRoutesTest`: `POST /devices` answers `201` for `ANDROID` and for `IOS` and stores that
      platform; `400 {error}` for a missing platform, an unknown platform and a blank token (UAT 34, 35).
- [ ] `ExposedStoresTest`: `enabledWithDeliverableDevice()` returns each alarm with its device's
      platform; `AlarmSchedulerTest`: the due alarm's platform reaches the `PushSender` fake.
- [ ] `FcmPushSenderTest`: the body for an `ANDROID` device equals the pre-change body byte for byte
      (UAT 27, automated part).
- [ ] App repository test on `MockEngine`: the `POST /devices` body contains `"platform"` with the
      bound value.

### Quality gates

- [ ] `./gradlew :composeApp:testAndroidHostTest :server:test` passes.
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes.
- [ ] `git diff main -- server/src/main/resources/db/migration/V1__init.sql server/src/main/resources/db/migration/V2__accounts.sql`
      is empty.
- [ ] `DeviceFlowLoggingTest` still finds no push address or token in the log.
- [ ] No new compiler or deprecation warning.

## Task [03-iphone-receives-alarm-notifications]

An iPhone user opens Alarms, grants notification permission (no badge), creates an alarm, and
receives it as a real push even with the app closed — Firebase + APNs, addressed by the Firebase
Installation ID. The backend sends iPhones an alert push with the unchanged `data` map plus fixed
localization keys, so iOS shows a localized generic notification from the app's bundle (wording each
message on the device is task 04). Foreground banners, tap-to-open, per-channel grouping, no badge, a
retryable error when push cannot be set up, and FID changes reaching the backend all work. Without
the Firebase/Apple files, the simulator build still runs as in task 01.

Requires: 01, 02.

### Implementation steps

- [ ] `FcmPushSender` for `IOS`: the annex JSON — `fid|token`, identical `data`, `apns.headers`
      (`apns-push-type: alert`, `apns-priority: 10`), `aps.alert` with `title-loc-key`
      `notification_title_no_station` and `loc-key` `notification_generic` (no `loc-args`),
      `mutable-content: 1`, `sound: default`, `thread-id` = channel id; no `android` block, no `badge`.
      Unregistered handling for both platforms.
- [ ] Gradle task (wired into `check` like `checkTranslations`) generating
      `en/de/fr/it.lproj/Localizable.strings` into the build directory from the Compose `strings.xml`
      for the keys notifications need (`notification_*`, species and severity words, date arrays as
      `<name>_<index>`): escape `"` and `\`, `%n$s` → `%n$@`, unescape `\'` / `\n`. Pure transformation
      in `buildSrc` with unit tests. An Xcode build phase copies the output into the app bundle (not
      committed).
- [ ] `IosPushTokenProvider` over a Swift-implemented `IosPushTokenSource`
      (`suspendCancellableCoroutine`; failure → existing failed path with Retry) replaces and deletes
      `UnavailablePushTokenProvider`; update the `PushTokenResult.Unavailable` KDoc.
- [ ] `IosPushRegistration.onRegistered(fid)` → `PushTokenUpdater` in an application scope (no
      `runBlocking`, failures logged only).
- [ ] `MainViewController(apiBaseUrl, appGroup, pushTokenSource)`; `Info.plist` exposes `APP_GROUP`.
- [ ] AppDelegate (`UIApplicationDelegateAdaptor`): `FirebaseApp.configure()` **only when
      `GoogleService-Info.plist` is in the bundle**, `registerForRemoteNotifications()` every launch,
      APNs token → `Messaging.messaging().apnsToken`, `MessagingDelegate` → `onRegistered`,
      `willPresent` → `[.banner, .list, .sound]`, `didReceive` → completion only.
- [ ] Firebase iOS SDK (`FirebaseMessaging`) via SPM, exact version; commit `Package.resolved` via
      `.gitignore` negations for `iosApp/iosApp.xcodeproj/project.xcworkspace/` and its
      `xcshareddata/swiftpm/Package.resolved`. `FirebaseMessagingInstallationIdEnabled = YES`; per-
      configuration `GoogleService-Info.plist` copy phase that skips a missing file.
- [ ] Entitlements: Push Notifications (`aps-environment`), App Groups
      (`group.ch.stenzel.tim.polleninfo[.debug]`).
- [ ] `NotificationPermissionController.ios.kt`: drop `UNAuthorizationOptionBadge`; denied → open the
      app's page in the Settings app.
- [ ] Docs: CLAUDE.md push section (remove "iOS has no push leg"; APNs block, loc-key fallback from
      the app bundle — in the language of the app's `AppleLanguages`, so a per-app language change
      reaches it only after relaunch); `deploy/README.md` Apple + Firebase prerequisites (Team ID, App
      IDs incl. extensions, App Groups, capabilities, APNs key in Firebase, iOS apps in Firebase);
      checklist entries.

### Acceptance criteria

- [ ] `FcmPushSenderTest` pins the full iOS JSON (`apns` headers, `mutable-content: 1`, both loc-keys,
      `sound`, `thread-id` = channel, no `android`, no `badge`, `data` identical to the Android
      message); an iOS `404 UNREGISTERED` yields `Unregistered`.
- [ ] Generator unit tests cover `"`/`\` escaping, `%1$s` → `%1$@`, `string-array` → `<name>_<index>`,
      `\'`/`\n` unescaping and key selection; after an Xcode build the app bundle contains
      `{en,de,fr,it}.lproj/Localizable.strings` with `notification_generic`.
- [ ] Without `GoogleService-Info.plist`, the Debug simulator build still launches to onboarding
      (UAT 1 re-checked); `git ls-files` lists `Package.resolved`.
- [ ] Simulator: `xcrun simctl push booted ch.stenzel.tim.polleninfo.debug <fixture>.apns` with the
      server's iOS payload (fixtures committed) shows the localized generic notification as a banner in
      the foreground (UAT 20), grouped by `thread-id` (UAT 22), opens the app on tap (UAT 21), with no
      badge (UAT 24); Alarms asks for permission without badge (UAT 13) and, after denial, opens
      PollenInfo's Settings page (UAT 14); with no network the Retry error appears and Retry succeeds
      after reconnecting (UAT 25).
- [ ] Real iPhone (after prerequisites; checklist): an alarm saved (UAT 15) arrives with the app
      closed; alarms keep arriving after the FID changes (UAT 26); Debug and Release builds keep
      separate device tokens, alarms and sessions (UAT 2).

### Quality gates

- [ ] `./gradlew :composeApp:testAndroidHostTest :composeApp:iosSimulatorArm64Test :server:test` and
      the `buildSrc` tests pass; `./gradlew :composeApp:checkTranslations` passes.
- [ ] Debug and Release simulator builds succeed with no new warning, with and without the Firebase
      plists present.
- [ ] Task 02's Android byte-for-byte test still passes; the CLAUDE.md `alarm/` text grep finds nothing.
- [ ] A flow logging test asserts no push address reaches the log for an iOS send.
- [ ] `grep -n 'iOS has no push leg' CLAUDE.md` finds nothing.

## Task [04-iphone-words-notifications-on-device]

Alarm notifications on an iPhone read exactly like on Android — "Pollen in Zürich" over "Grasses:
High · Birch: Moderate" and the no-pollen / not-reported / no-current-reading / unavailable texts — in
the language chosen in the app (the phone's language for "system default"). The wording lives in one
place, a new Compose-free `:pushText` module used by the Android messaging service and by a
Notification Service Extension on iPhone. If the extension cannot word a message in time, the
localized generic text is shown.

Requires: 03.

### Implementation steps

- [ ] Create `:pushText` (KMP library: Android with `withHostTest { }`, `iosArm64`,
      `iosSimulatorArm64`; only kotlinx-datetime, no Compose; package
      `ch.stenzel.tim.polleninfo.pushtext`; static framework `PushText`); `:composeApp` depends on it
      with `api`.
- [ ] Move into it `AlarmNotificationContent`, `AlarmChannel`, `parseAlarmPayload`, `notificationText`,
      the pure `DateWording`, `formatTime`, `SWISS_ZONE` / `swissToday`, and the species/severity
      identifiers the parser recognizes; `rememberDateWording()` / `loadDateWording()` stay in
      `:composeApp`.
- [ ] Replace `NotificationStrings` (`StringResource`-based) with `PushString` (each carrying its
      resource key name) and `PushStrings`; add the Swift-friendly entry point
      (`notificationTextFor(data, strings, dates, now)` in whatever blocking/callback form interop needs).
- [ ] Android adapter in `:composeApp` mapping each `PushString` to `Res.string.*`;
      `PollenFirebaseMessagingService` otherwise unchanged.
- [ ] Make the `.strings` generator's key selection agree with `PushString` and the date arrays (derive
      it, or fail the build on a difference).
- [ ] `NotificationService` extension target (no Firebase), embedding `PushText` through the
      `:pushText` equivalent of `embedAndSignAppleFrameworkForXcode`, App Group entitlement, generated
      `Localizable.strings` copied in. Reads `appLanguage` from the App Group (fallback
      `Bundle.preferredLocalizations`), loads that `.lproj`, implements `PushStrings`, sets title/body;
      `serviceExtensionTimeWillExpire` and any failure → generic localized text.
- [ ] `IosLanguageRepository.set()` writes/removes `appLanguage` in `NSUserDefaults(suiteName = appGroup)`;
      `MainViewController`'s configure syncs the current language into it.
- [ ] Move the existing push and `DateWording` tests and fixtures into `:pushText`, asserting
      `PushString` + args through a recording fake `PushStrings`.
- [ ] Docs: CLAUDE.md Modules (`:pushText`), `core/` table (`push`, `ui/format` moves), push section
      (extension, App Group language), commands; checklist entries.

### Acceptance criteria

- [ ] `./gradlew :pushText:testAndroidHostTest :pushText:iosSimulatorArm64Test` passes, with the moved
      tests pinning `PushString` + args for every content kind (no-current-reading today / yesterday /
      earlier, unknown kind and unknown species → generic).
- [ ] A test or the build fails when a `PushString` key or a used date array is missing from any of
      the four `strings.xml` (story 43); a `:composeApp` test asserts every `PushString` maps to an
      existing `Res.string`, and that `notificationText` through the Android `PushStrings` gives the same
      resource and args as before for every content kind (UAT 27, automated part).
- [ ] `./gradlew :pushText:dependencies` lists no `org.jetbrains.compose` artifact, and no wording
      function remains in `:composeApp` (story 42).
- [ ] Simulator: `xcrun simctl push` of committed `.apns` fixtures shows the worded title and body,
      German with app language German and phone English (UAT 16, 17), French with "system default" and
      phone French (UAT 18); a fixture making the extension fail shows the generic localized text
      (UAT 23). (Confirm early that simulated pushes run the service extension on Xcode 27; if not, these
      move to the real-iPhone criterion.)
- [ ] Real iPhone (after prerequisites; checklist): a daily report arrives with the app closed, worded
      in the app's language (UAT 16); exactly one threshold notification per type per day (UAT 19).
      Android (checklist): an alarm notification looks exactly as before (UAT 27).

### Quality gates

- [ ] `./gradlew :composeApp:testAndroidHostTest :composeApp:iosSimulatorArm64Test :composeApp:checkTranslations`
      passes.
- [ ] Both Xcode targets build for Debug and Release with no new warning; the extension's linked
      frameworks contain no Firebase.
- [ ] No new compiler or deprecation warning in `:pushText` or `:composeApp`.
- [ ] The CLAUDE.md hard-coded-text grep, extended to `pushText/src`, has no new hit.

## Task [05-iphone-password-autofill]

On the iPhone login screen, iOS offers the saved PollenInfo password and choosing it logs the user in;
the keyboard suggests saved credentials for email and password; iOS's own save prompt is relied on
after registration/login. The website publishes the Apple association file, with the Team ID from the
git-ignored deployment environment.

Requires: 01.

### Implementation steps

- [ ] `IosCredentialStore` replaces and deletes `NoCredentialStore`: `offer()` via
      `ASAuthorizationController` + `ASAuthorizationPasswordProvider().createRequest()`,
      `suspendCancellableCoroutine`, cancel / no entry → `null`; `save()` no-op; logs by exception type
      only.
- [ ] Verify the existing account field content types reach iOS autofill.
- [ ] Associated Domains entitlement per configuration (`webcredentials:api.polleninfo.ch`, Debug with
      `?mode=developer`).
- [ ] Caddy route `/.well-known/apple-app-site-association` answering the annex JSON with both app ids
      from `{$APPLE_TEAM_ID}` and `Content-Type: application/json`; `compose.yaml` passes
      `APPLE_TEAM_ID: ${APPLE_TEAM_ID:?}` to `caddy`; `deploy/.env.example` gets an `APPLE_TEAM_ID=`
      placeholder; `compose.local.yaml` a test value if needed.
- [ ] Docs: `deploy/README.md` (variable, route, setting it in production), CLAUDE.md
      password-autofill / `CredentialStore` notes; checklist entries; if iOS does not offer to save after
      a Compose form, record it as an accepted limitation.

### Acceptance criteria

- [ ] `DeployFilesTest`: the Caddyfile serves `/.well-known/apple-app-site-association` with
      `application/json` and both `{$APPLE_TEAM_ID}.ch.stenzel.tim.polleninfo[.debug]` ids, contains no
      literal `[A-Z0-9]{10}\.ch\.stenzel`; `compose.yaml` passes a required `APPLE_TEAM_ID` to `caddy`;
      `deploy/.env.example` has the placeholder.
- [ ] Local stack (`SERVER_TAG=local SECRETS_DIR=./local-secrets APPLE_TEAM_ID=ABCDE12345 docker compose -f deploy/compose.yaml -f deploy/compose.local.yaml up -d`):
      `curl -ik https://localhost/.well-known/apple-app-site-association` returns `200`,
      `application/json` and both ids with `ABCDE12345` (UAT 36 locally).
- [ ] Production (after the Team ID is set): the same request against `https://api.polleninfo.ch`
      returns the JSON with the real Team ID (UAT 36).
- [ ] Real iPhone (after prerequisites; checklist): saved credential offered on the login screen and
      logs in (UAT 28); none saved → no sheet, form usable at once (UAT 29); save offered after
      registration or recorded as limitation (UAT 30); keyboard suggests credentials in both fields
      (UAT 31).

### Quality gates

- [ ] `./gradlew :server:test --tests '*DeployFilesTest' :composeApp:testAndroidHostTest` passes.
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes; Debug and Release simulator
      builds succeed with no new warning.
- [ ] `grep -rn 'NoCredentialStore' composeApp CLAUDE.md` finds nothing.

## Task [06-iphone-language-applies-immediately]

Choosing a language in the app's settings on iPhone switches all visible text at once, with no
restart and no "applies after restart" hint, and the next notification uses it too. A language set
for PollenInfo in the iOS Settings app applies to the app and its notifications.

Requires: 01, 04 (App Group language sync).

### Implementation steps

- [ ] Recompose the app root on a language change (`key(language) { … }` around `App()` content, per
      JetBrains' Compose Resources pattern).
- [ ] `IosLanguageRepository.appliesImmediately = true`; update its KDoc (no longer
      "compile-verified only").
- [ ] Verify by hand; if any text stays in the old language, fall back to `appliesImmediately = false`
      and record why.
- [ ] Docs: CLAUDE.md Localization (iOS applies immediately or the recorded fallback); checklist entries.

### Acceptance criteria

- [ ] `IosLanguageRepository.appliesImmediately` is `true`, and the existing Settings test for
      `appliesImmediately = true` (no restart hint) passes — or the documented fallback is in place.
- [ ] Simulator (checklist): English → Italian in the app's settings switches every visible text on
      every tab immediately and no restart hint is shown (UAT 32).
- [ ] Simulator (checklist): after changing PollenInfo's language in the iOS Settings app and
      returning, the app shows that language and a `simctl push` notification uses it (UAT 33).
- [ ] Android language switching behaves as before (checklist).

### Quality gates

- [ ] `./gradlew :composeApp:testAndroidHostTest :composeApp:iosSimulatorArm64Test` passes.
- [ ] `./gradlew :androidApp:assembleDebug` succeeds; Xcode Debug build has no new warning.
- [ ] `grep -n 'compile-verified only\|applies the next time' composeApp/src/iosMain -r` finds nothing
      in `IosLanguageRepository` (unless the fallback was taken).
