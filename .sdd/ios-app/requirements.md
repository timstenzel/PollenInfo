# iOS App with Feature Parity

## Problem Statement

PollenInfo is only usable on Android. The shared app code already compiles for iOS, but there is no
iOS app that a person can install, and the parts of the app that depend on the platform are
placeholders on iOS: pollen alarms can never notify an iPhone, the password manager is never
offered, the app always talks to a development server, and a language change only applies after a
restart. An iPhone user therefore cannot use PollenInfo at all, and once they could, they would get
a lesser app than an Android user.

## Solution

An installable iPhone app that offers everything the Android app offers: current readings for the
chosen station, all stations with the map, the diary with an account, pollen alarms with push
notifications, the password manager on login, and an in-app language choice that applies
immediately.

Alarm notifications on iPhone keep the product's rule that the server reports *what happened* and
the phone words it: the notification text is written on the iPhone, in the language chosen in the
app, using the same wording as Android. If the phone cannot word a notification in time, it still
shows a short localized generic notification instead of nothing.

The backend learns which platform each registered device runs on, so it can address Android and
iPhone correctly. A development build and a release build of the iPhone app can be installed side
by side, the development build talking to a local server and the release build to the production
server, exactly as on Android.

## User Stories

### Installing and starting the app

1. As an iPhone user, I want to install PollenInfo on my iPhone, so that I can check pollen levels without an Android device.
2. As an iPhone user, I want the app to have a recognizable PollenInfo icon on my home screen, so that I find it among my apps.
3. As an iPhone user, I want the app to open without a jarring flash of a different colour, so that starting the app feels calm in both light and dark mode.
4. As a first-time iPhone user, I want to go through the same onboarding as on Android, so that I choose my station before seeing readings.
5. As an iPhone user, I want the app's content to stay clear of the notch, the Dynamic Island and the home indicator, so that nothing is hidden or cut off.
6. As an iPhone user, I want to go back with the usual iOS edge swipe, so that navigation feels native.
7. As an iPhone user editing an alarm, I want the discard question to appear when I swipe back with unsaved changes, so that I do not lose an edit by accident.

### Readings, stations and diary

8. As an iPhone user, I want to see the current pollen levels of my station, so that I can plan my day.
9. As an iPhone user, I want to see all stations in a list and on the map, so that I can compare regions.
10. As an iPhone user, I want to find my nearest station from my approximate location, so that I do not have to know which station is close to me.
11. As an iPhone user, I want the app to ask only for my approximate location, so that my precise whereabouts stay private.
12. As an iPhone user, I want to see a clear explanation when the app asks for my location, so that I understand why it is needed.
13. As an iPhone user with an account, I want to keep my diary and see it next to the pollen history, so that I can relate how I feel to pollen levels.
14. As an iPhone user, I want to create an account, log in, log out and delete my account, so that I control my diary data.

### Alarms and notifications

15. As an iPhone user, I want to be asked for permission to send notifications only when I open Alarms, so that the app does not nag me at first start.
16. As an iPhone user, I want to be guided to the iOS Settings app when I have denied notifications before, so that I know how to enable alarms.
17. As an iPhone user, I want to create, edit, switch on and off, and delete pollen alarms, so that I am told about pollen that matters to me.
18. As an iPhone user, I want to receive my daily pollen report at the time I chose, so that I know the situation before I leave the house.
19. As an iPhone user, I want to receive a threshold alert when a selected pollen type reaches my chosen level within my time window, so that I can react in time.
20. As an iPhone user, I want notifications to be worded in the language I chose in the app, even if my iPhone is set to a different language, so that the app speaks to me consistently.
21. As an iPhone user, I want notifications to use the same words for pollen types and levels as the app screens, so that I recognize them immediately.
22. As an iPhone user, I want to receive notifications even when I have closed the app completely, so that alarms are reliable.
23. As an iPhone user, I want notifications to appear as a banner even while I am using the app, so that I do not miss them.
24. As an iPhone user, I want tapping a notification to open the app, so that I can look at the details.
25. As an iPhone user, I want daily reports and threshold alerts grouped separately in the notification centre, so that they do not clutter each other.
26. As an iPhone user, I want to see a short generic notification if my phone cannot word a notification in time, so that I never miss an alarm completely.
27. As an iPhone user, I want no badge number on the app icon, so that the app does not pretend there is something unread.
28. As an iPhone user, I want my alarms to keep working when the push address of my phone changes, so that I do not have to recreate them.
29. As an iPhone user, I want the app to tell me clearly when push cannot be set up and to let me retry, so that I am not left with alarms that never fire.

### Password manager

30. As an iPhone user, I want my saved PollenInfo password to be offered when I open the login screen, so that I can log in with one tap.
31. As an iPhone user, I want iOS to offer saving my password after I register or log in, so that I do not have to remember it.
32. As an iPhone user, I want my keyboard's password suggestions to fill email and password fields, so that typing is not necessary.

### Language

33. As an iPhone user, I want to choose the app language in the app's settings, so that I can use PollenInfo in English, German, French or Italian.
34. As an iPhone user, I want a language change to apply immediately, so that I see the result without restarting the app.
35. As an iPhone user, I want a language I set for PollenInfo in the iOS Settings app to apply to the app and its notifications, so that both places agree.
36. As an iPhone user who chose "system default", I want the app and its notifications to follow my iPhone's language, so that I do not have to configure anything.

### Development and operations

37. As the developer, I want a development build and a release build that can be installed side by side, so that I can test without losing my real alarms.
38. As the developer, I want the development build to talk to my local server and the release build to the production server, so that tests never touch production by accident.
39. As the developer, I want to point the development build at a different server address without changing tracked files, so that I can test on a real iPhone over my local network.
40. As the developer, I want the iPhone app to build from the same repository with one Xcode project, so that iOS and Android stay in step.
41. As the developer, I want the shared app tests to run on the iOS simulator as well as on the JVM, so that platform differences are caught.
42. As the developer, I want notification wording to exist in exactly one place, so that Android and iPhone never drift apart.
43. As the developer, I want translations for notifications to be checked by the existing translation check, so that a missing translation fails the build on both platforms.
44. As the developer, I want the backend to reject a device registration that does not say which platform it runs on, so that every device can be addressed correctly.
45. As the developer, I want the backend to build Android messages exactly as before, so that the iPhone work cannot break Android alarms.
46. As the developer, I want the backend's push messages for iPhone to contain no human-readable text, so that the "server sends what happened" rule still holds.
47. As the operator, I want the website to publish the association file needed for password autofill, with the Apple Team ID taken from the server configuration, so that no personal identifier is committed.
48. As the developer, I want a written list of the manual Apple and Firebase setup steps, so that I can complete them once I have the developer membership.
49. As the developer, I want a manual test checklist for the iPhone-only parts, so that I can verify what automated tests cannot reach.
50. As an App Store reviewer (later), I want the app to declare which privacy-relevant system features it uses, so that the app can be submitted without rework.

## User Acceptance Tests

### Installation and basics

1. Given a Mac with Xcode, when the developer opens the iOS project and runs the development build on an iPhone simulator, then PollenInfo starts and shows onboarding on first launch.
2. Given the development build and the release build, when both are installed on one iPhone, then both appear as separate apps named "PollenInfo" and each keeps its own station, alarms and login.
3. Given the release build, when it loads readings, then it contacts the production server; given the development build without a local override, when it loads readings, then it contacts the server on the developer's Mac.
4. Given the device is in dark mode, when the app is launched, then the launch screen and the first screen share the dark surface colour without a light flash.
5. Given onboarding is shown, when the user taps "use my location" and allows approximate location, then the nearest station is preselected.
6. Given onboarding is shown for the first time, when the app asks for location, then iOS shows the app's explanation text and offers only approximate location.
7. Given an iPhone with a notch or Dynamic Island, when any screen is shown, then no content is hidden behind it or behind the home indicator.
8. Given the alarm editor with an unsaved change, when the user swipes back from the screen edge, then the discard question is shown.

### Readings, stations, diary, account

9. Given a selected station, when Home is shown, then the current levels per pollen type appear with the same severity words and colours as on Android.
10. Given the All stations tab, when it is shown, then the list and the map show all 15 stations and tapping a map dot selects that station.
11. Given a user who is logged out, when they register with email, password and consent, then they are logged in and the diary is available.
12. Given a logged-in user, when they record today's feeling and open the diary, then the feeling appears on the chart next to the pollen history.

### Alarms and push

13. Given notifications were never requested, when the user opens Alarms, then the user is asked for permission without a badge permission being part of the request.
14. Given notifications were denied before, when the user opens Alarms, then the app offers to open the iOS Settings app and doing so opens PollenInfo's settings page.
15. Given notifications are allowed, when the user creates a daily report alarm, then the alarm is saved and appears in the list.
16. Given a daily report alarm due in one minute and the app is completely closed, when the time comes, then a notification arrives whose title names the station and whose body lists pollen types and levels in the app's language.
17. Given the app language is German and the iPhone language is English, when an alarm notification arrives, then its text is German.
18. Given the app language is "system default" and the iPhone language is French, when an alarm notification arrives, then its text is French.
19. Given a threshold alarm whose type has reached the chosen level within the window, when the alarm fires, then exactly one threshold notification arrives for that type that day.
20. Given the app is open in the foreground, when an alarm notification arrives, then it is shown as a banner with sound.
21. Given an alarm notification, when the user taps it, then the app opens.
22. Given several daily reports and threshold alerts were received, when the user opens the notification centre, then reports and alerts are grouped separately.
23. Given the phone cannot word a notification in time (simulated), when an alarm arrives, then a generic localized notification is shown instead.
24. Given an alarm notification arrives, when the user looks at the app icon, then no badge number is shown.
25. Given push cannot be set up (for example, no network on the first alarm), when the user opens Alarms, then an error with a retry option is shown and retrying after reconnecting succeeds.
26. Given an iPhone with registered alarms whose push address changes, when the next alarm is due, then the notification still arrives.
27. Given an Android phone with alarms, when its alarms fire after this feature is deployed, then its notifications look and behave exactly as before.

### Password manager

28. Given a password for PollenInfo is saved in the iPhone's keychain, when the user opens the login screen, then iOS offers the saved credential and choosing it logs the user in.
29. Given no password is saved, when the user opens the login screen, then no sheet is shown and the form is usable at once.
30. Given a user registers a new account, when registration succeeds, then iOS offers to save the password (or, if iOS does not offer it, this is recorded as a known limitation).
31. Given the login screen, when the user focuses the email or password field, then the keyboard offers matching saved credentials.

### Language

32. Given the app language is English, when the user chooses Italian in the app settings, then all visible text switches to Italian immediately without a restart and no "applies after restart" hint is shown.
33. Given the user changes PollenInfo's language in the iOS Settings app, when they return to the app and later receive an alarm, then both the app and the notification use that language.

### Backend

34. Given the backend, when a device registers without stating its platform, then the registration is refused as invalid.
35. Given the backend, when a device registers with an unknown platform, then the registration is refused as invalid.
36. Given the production website, when the password-autofill association file is requested, then it is served as JSON and lists the release and development app identifiers with the configured Team ID.

## Definition of Done

- All user acceptance tests pass on an iPhone simulator, and the push-related ones (16–26) also on a real iPhone, once the Apple developer membership and setup are in place.
- Android behaves exactly as before: its alarm notifications, wording and registration work unchanged apart from the device stating its platform.
- The shared app tests pass on the JVM and on the iOS simulator; the server tests pass.
- The translation check passes, and every text used by notifications exists in all four languages.
- The iPhone notification messages sent by the backend contain no human-readable text.
- No new compiler or deprecation warnings.
- The manual Apple and Firebase setup steps are documented as prerequisites, and the manual iPhone test checklist is written down and has been run.
- Project documentation (project rules, README, operations runbook) describes the iOS app, its build, its configuration and its push path, and no longer says the iOS wrapper is missing.
- The release build of the iPhone app can be archived in Xcode.

## Out of Scope

- Publishing the app: TestFlight, App Store Connect entry, store listing, screenshots, review submission.
- Continuous integration for iOS (and for the Android app, which has none today either).
- iPad layouts; the app targets iPhone only.
- Deep links from a notification to a specific screen (on both platforms a tap only opens the app).
- An app icon badge.
- Saving passwords from code on iOS; iOS's own save prompt is relied on.
- Automated tests for Swift code.
- Changes to what alarms do, when they fire, or what they report; only how they are delivered to iPhones changes.
- Supporting already-installed Android builds that do not state their platform (there are no production users yet).

## Further Notes

- **Prerequisites (manual, by the developer):** paid Apple Developer Program membership and Team ID;
  App IDs for release and development app and their notification extensions; App Groups; Push
  Notifications and Associated Domains capabilities; an APNs authentication key uploaded to the
  Firebase project; iOS apps (release and development) added to Firebase and their configuration
  files downloaded; the Team ID set in the server's deployment configuration. The code must compile
  and the simulator build must run before these exist; push can only be verified end to end after.
- **Known risk:** whether iOS offers to save a password after a Compose-based login form is
  uncertain; if it does not, it becomes an accepted limitation, not a workaround.
- **Known risk:** immediate language switching on iOS relies on the shared UI reading the language
  again after a change; if any text stays in the old language after verification, the fallback is
  "applies at next launch" as today.
- Full Xcode is now installed on the development machine, so iOS simulator tests can run locally.

---

## Technical Annex
> Written against codebase as of: 2026-10-17 (branch `develop`, commit `abc43f8`)

### Architectural Decisions

#### Push delivery to iOS

- iOS uses Firebase Cloud Messaging as Android does; the push address stays the **Firebase
  Installation ID** (`FirebaseMessagingInstallationIdEnabled = YES` in the app's `Info.plist`; FID via
  `MessagingDelegate.messaging(_:didReceiveRegistration:)`). The server's existing `fid` addressing
  is unchanged.
- iOS messages are **alert pushes with `mutable-content: 1`**, rewritten by a **Notification Service
  Extension** from the unchanged `data` map. Data-only (silent) pushes were rejected: iOS throttles
  them and does not deliver them after a force-quit. Direct APNs from the server was rejected (second
  sender, second token type).
- `loc-key` / `loc-args` alone was rejected: species and severity words in `loc-args` would not be
  localized, the level list has variable length, and "today/yesterday/date" depends on display time.

#### Server

- **Migration `V3__device_platform.sql`** (never edit V1/V2):
  ```sql
  ALTER TABLE devices ADD COLUMN platform text NOT NULL DEFAULT 'ANDROID';
  ALTER TABLE devices ALTER COLUMN platform DROP DEFAULT;
  ALTER TABLE devices ADD CONSTRAINT devices_platform_check CHECK (platform IN ('ANDROID', 'IOS'));
  ```
  Mirror in `Tables.kt`. New enum `DevicePlatform { ANDROID, IOS }` in `alarm/domain`.
- **`POST /devices`**: body `{fcmToken, platform}`, both **required**; missing/blank token or
  missing/unknown platform → `400 {error}`. `PUT /devices/me/fcm-token` unchanged and never changes
  the platform. Update the REST table in CLAUDE.md.
- `DeviceStore.register(fcmToken, platform)`; `AlarmStore.enabledWithDeliverableDevice()` returns
  `AlarmWithToken(alarm, fcmToken, platform)`; `AlarmScheduler` passes the platform through.
- **`PushSender.send(token: String, platform: DevicePlatform, message: PushMessage): PushResult`**
  (`FcmPushSender`, `LoggingPushSender` and test fakes updated).
- **`FcmPushSender` JSON** — exactly one platform block:
  - ANDROID: unchanged — `{fid|token, android: {priority: "HIGH"}, data}`.
  - IOS:
    ```json
    {
      "fid": "…",
      "data": { "kind": "…", "channel": "…", "stationAbbr": "…", "…": "…" },
      "apns": {
        "headers": { "apns-push-type": "alert", "apns-priority": "10" },
        "payload": { "aps": {
          "alert": { "title-loc-key": "notification_title_no_station",
                     "loc-key": "notification_generic" },
          "mutable-content": 1,
          "sound": "default",
          "thread-id": "<channel id: daily_report | threshold_alert>"
        } }
      }
    }
    ```
    The two loc-keys are fixed constants without `loc-args`; no `badge`. The `alarm/` text grep in
    CLAUDE.md must stay empty. `Unregistered` handling (404 UNREGISTERED / 400 INVALID_ARGUMENT →
    `clearFcmToken`) applies to both platforms.
- **Caddy**: route `/.well-known/apple-app-site-association` answering
  `{"webcredentials":{"apps":["{$APPLE_TEAM_ID}.ch.stenzel.tim.polleninfo","{$APPLE_TEAM_ID}.ch.stenzel.tim.polleninfo.debug"]}}`
  with `Content-Type: application/json`; `APPLE_TEAM_ID` from `deploy/.env` (git-ignored), passed to
  the `caddy` service; documented in `deploy/README.md`.

#### New module `:pushText`

- KMP library like `:theme` (`com.android.kotlin.multiplatform.library`, Android +
  `iosArm64` + `iosSimulatorArm64`), **no Compose dependency** (only kotlinx-datetime), package
  `ch.stenzel.tim.polleninfo.pushtext`, static iOS framework `PushText`. `:composeApp` depends on it
  with `api`.
- Moves in (exact cut decided at implementation; requirement is "PushText contains no Compose"):
  `AlarmNotificationContent`, `parseAlarmPayload`, `notificationText`, `DateWording` (pure class),
  `formatTime`, the Swiss zone / Swiss-today helper, and the species/severity identifiers the parser
  recognizes. `rememberDateWording()` / `loadDateWording()` stay in `:composeApp`.
- Strings by key instead of `StringResource`:
  ```kotlin
  enum class PushString { TITLE, TITLE_NO_STATION, LEVEL, NO_POLLEN, NOT_REPORTED,
      NO_CURRENT_READING_TODAY, NO_CURRENT_READING_YESTERDAY, NO_CURRENT_READING_EARLIER,
      UNAVAILABLE, GENERIC, /* species_*, severity_* … */ }
  fun interface PushStrings { suspend fun get(key: PushString, args: List<String>): String }
  ```
  Each `PushString` carries its resource key name (e.g. `notification_title`). A Swift-friendly entry
  point for the extension, e.g. `fun notificationTextFor(data: Map<String, String>, strings: PushStrings, dates: DateWording, now: Instant): NotificationText`
  (blocking or callback form as Swift interop requires).
- **Android adapter** in `:composeApp` maps `PushString` → `Res.string.*` and resolves with
  `getString`; `PollenFirebaseMessagingService` otherwise unchanged.

#### Localizable.strings generation

- A Gradle task (in `:pushText` or `:composeApp`, wired like `checkTranslations`) reads
  `composeApp/src/commonMain/composeResources/values{,-de,-fr,-it}/strings.xml` and writes
  `en/de/fr/it.lproj/Localizable.strings` with only the keys `PushString` and the date arrays need.
  Rules: escape `"` and `\`, convert positional `%n$s` → `%n$@`, `string-array` items → `<name>_<index>`
  keys, unescape Android resource escapes (`\'`, `\n`). Output is generated into the build directory
  and copied into both the app target and the extension by an Xcode build phase (not committed).
- The app target needs the file too: iOS resolves the server's fallback loc-keys from the **app's**
  bundle.

#### App (`:composeApp`)

- **Platform in registration:** `RegisterDeviceRequestDto(fcmToken, platform)`; the platform value
  comes from a `platformModule` binding (e.g. a `DevicePlatform` value or a field on `AppInfo`), used
  by `AlarmRepositoryImpl` / `AlarmApiService.registerDevice`.
- **`IosPushTokenProvider`** replaces `UnavailablePushTokenProvider` (deleted):
  ```kotlin
  interface IosPushTokenSource { fun fetch(callback: (fid: String?, error: String?) -> Unit) }
  ```
  implemented in Swift with `Messaging.messaging()`, bridged with `suspendCancellableCoroutine`;
  failure → the existing failed path (screen offers Retry). Update the `PushTokenResult.Unavailable`
  KDoc (no longer "iOS").
- **FID changes:** a Kotlin entry point (e.g. `IosPushRegistration.onRegistered(fid)`) called from
  the Swift `MessagingDelegate`, delegating to `PushTokenUpdater` in an application coroutine scope
  (no `runBlocking`; failures only logged).
- **`MainViewController(apiBaseUrl: String, appGroup: String, pushTokenSource: IosPushTokenSource)`**;
  the hard-coded `localhost` constant is removed. `configure` also syncs the current language into the
  App Group.
- **`IosLanguageRepository`**: on `set()` also writes/removes key `appLanguage` in
  `NSUserDefaults(suiteName = appGroup)`; `appliesImmediately = true`; the app root recomposes on change
  (`key(language) { … }` around `App()` content, per JetBrains' documented Compose Resources pattern).
  Fallback if verification fails: back to `false`.
- **`IosCredentialStore`** replaces `NoCredentialStore`: `offer()` via `ASAuthorizationController` +
  `ASAuthorizationPasswordProvider().createRequest()` (AuthenticationServices from Kotlin platform
  libs), bridged with `suspendCancellableCoroutine`, cancel/no entry → `null`; `save()` no-op.
- **`NotificationPermissionController.ios.kt`**: drop `UNAuthorizationOptionBadge`.
- Update the "compile-verified only / no wrapper" KDocs once checked by hand.

#### `iosApp` Xcode project

- `iosApp/iosApp.xcodeproj` checked in. Targets: `iosApp` (SwiftUI `@main`, hosts
  `MainViewController` via `UIViewControllerRepresentable`) and `NotificationService`
  (`UNNotificationServiceExtension`).
- Kotlin via Run Script `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode` (app) and the
  equivalent for `:pushText` (extension). No CocoaPods.
- Firebase iOS SDK via SPM (`FirebaseMessaging`), exact version, `Package.resolved` committed. The
  extension does **not** link Firebase.
- `Config/Base.xcconfig` (`DEVELOPMENT_TEAM` empty until available), `Debug.xcconfig`,
  `Release.xcconfig`, optional untracked `Config/Local.xcconfig` (`#include?`) for `API_BASE_URL`.
  `Info.plist` exposes `API_BASE_URL` and `APP_GROUP` to Swift.

  | | Debug | Release |
  | --- | --- | --- |
  | Bundle ID | `ch.stenzel.tim.polleninfo.debug` | `ch.stenzel.tim.polleninfo` |
  | Extension | `ch.stenzel.tim.polleninfo.debug.NotificationService` | `ch.stenzel.tim.polleninfo.NotificationService` |
  | App Group | `group.ch.stenzel.tim.polleninfo.debug` | `group.ch.stenzel.tim.polleninfo` |
  | `API_BASE_URL` | `http://localhost:8080` | `https://api.polleninfo.ch` |
  | `NSAllowsLocalNetworking` | `true` | absent |
  | Associated Domains | `webcredentials:api.polleninfo.ch?mode=developer` | `webcredentials:api.polleninfo.ch` |
  | Display name | PollenInfo | PollenInfo |
  | `GoogleService-Info.plist` | debug app's | release app's (copied per configuration) |

- `Info.plist` keys (app): `NSLocationWhenInUseUsageDescription`, `NSLocationDefaultAccuracyReduced`,
  `CFBundleLocalizations` (en, de, fr, it), `CFBundleDevelopmentRegion` en,
  `CFBundleShortVersionString`/`CFBundleVersion`, `FirebaseMessagingInstallationIdEnabled`,
  `UILaunchScreen` (background colour asset), `TARGETED_DEVICE_FAMILY = 1`, orientation as Android.
- Entitlements: Push Notifications (`aps-environment`), App Groups (both targets), Associated Domains.
- **AppDelegate** (`UIApplicationDelegateAdaptor`): `FirebaseApp.configure()`,
  `registerForRemoteNotifications()` every launch, APNs token → `Messaging.messaging().apnsToken`,
  `MessagingDelegate` → Kotlin `onRegistered`, `UNUserNotificationCenterDelegate`:
  `willPresent` → `[.banner, .list, .sound]`, `didReceive` → completion only.
- **NotificationService**: reads `appLanguage` from the App Group (fallback
  `Bundle.preferredLocalizations`), loads that `.lproj`'s `Localizable.strings`, implements
  `PushStrings`, calls `:pushText`, sets title/body; `serviceExtensionTimeWillExpire` and any failure
  → generic localized text.
- Assets: `AppIcon` 1024 px from the Android adaptive icon (dark/tinted variants if clean), launch
  background colour = `:theme` `surfaceLight`/`surfaceDark`. `PrivacyInfo.xcprivacy` declaring
  UserDefaults required-reason use and no tracking.

#### Documentation

- CLAUDE.md: iOS wrapper section (replace "There is no `iosApp` Xcode project yet"), push section
  ("iOS has no push leg" removed; extension, loc-key fallback, App Group language), `:pushText` in
  Modules, commands table (iOS simulator tests, Xcode build), remove "only CLT here", REST table
  (`POST /devices` platform), CredentialStore/language notes. README and `deploy/README.md`
  (Team ID variable, AASA route, Apple/Firebase setup prerequisites, manual iOS checklist).

### Automated Testing Decisions

- Good tests assert external behaviour through public interfaces: JSON on the wire, HTTP status
  codes, rows in the database, returned texts by resource key and args — not internal calls. Test
  names are backtick sentences without commas (Kotlin/Native).
- **`:pushText`** (unit, `commonTest`, run with `testAndroidHostTest` and `iosSimulatorArm64Test`):
  move the existing `parseAlarmPayload` / `notificationText` / `DateWording` tests and fixtures,
  asserting `PushString` keys + args via a recording fake `PushStrings` (as tests pin resource + args
  today). Prior art: current push tests in `composeApp/src/commonTest/.../core/push/`,
  `DateWordingFixtures.kt`.
- **Key coverage** (unit): every `PushString` key and every date array used exists in all four
  `strings.xml`. Prior art: `checkTranslations`.
- **Localizable.strings generator** (unit on the pure transformation): escaping, placeholder
  conversion `%1$s` → `%1$@`, array → indexed keys, key selection, Android escapes.
- **Android adapter** (unit): each `PushString` maps to an existing `Res.string` resource.
- **App registration** (repository test via real `HttpClient` on `MockEngine`): `POST /devices` body
  contains `platform`. Prior art: `AlarmRepositoryImpl` tests.
- **Server**:
  - `MigrationTest`: V3 applies; existing devices become `ANDROID`; insert without platform fails;
    unknown value violates the check.
  - Route tests: `POST /devices` → `201` with each platform; `400` missing / blank / unknown platform;
    stored platform. Prior art: device route tests.
  - Store tests: `enabledWithDeliverableDevice()` returns the platform. Prior art: `AlarmStore` tests
    with `TestPostgres`.
  - `FcmPushSender` tests pin the full JSON per platform: Android byte-for-byte unchanged; iOS has
    `apns` headers, `mutable-content`, loc-keys, `thread-id` = channel, no `android` block, no
    `badge`, `data` identical. Unregistered handling for an iOS device. Prior art: existing
    `FcmPushSender` tests on `MockEngine`.
  - `AlarmScheduler` test: the platform reaches the `PushSender` fake.
  - `DeployFilesTest`: Caddyfile has the AASA route with `application/json` and both app IDs using
    the Team ID variable.
  - `*FlowLoggingTest`: no push address leaks via the new code paths.
- **Not automated (manual checklist):** Swift code (AppDelegate, NotificationService),
  `IosPushTokenProvider`, `IosCredentialStore`, `IosLanguageRepository` (logic-free like the
  DataStore implementations), immediate language switch, safe areas, edge-swipe back, foreground
  presentation, real push on device.
- No iOS CI; iOS tests run locally (`./gradlew :composeApp:iosSimulatorArm64Test
  :pushText:iosSimulatorArm64Test`).
