# iOS App — Requirements

## Problem Statement

PollenInfo is a Compose Multiplatform app, and almost all of its code — every screen, every rule,
every translation — is already shared between Android and iOS. Yet there is no iPhone app: nothing
packages the shared code into something that can be built, installed and opened on an iPhone. The
iOS-specific pieces that do exist (location, language, version, storage) have only ever been
compiled, never run, so nobody knows whether they work.

The maintainer cannot use a paid Apple Developer account yet. Push notifications, TestFlight and the
App Store all depend on that account, so the first iPhone app has to deliver everything that works
without it, and must not claim the app's final identity with Apple before that account exists.

On top of that, an iOS build left unchecked would quietly break: shared code changes are only
compiled for iOS in CI, the app wrapper would not be built at all, and secrets stored on the phone
would by default travel into iCloud backups — something the Android app explicitly prevents.

## Solution

An iPhone app that hosts the shared PollenInfo app, buildable from the repository with Xcode and
installable on the iOS Simulator and on the maintainer's own iPhone using a free Apple ID
("Personal Team").

- Every feature that does not need push works on iPhone exactly as on Android: onboarding with
  nearest-station lookup, Home, All stations (list and map), Diary, Account, Settings with language
  choice and app version. The Alarms tab keeps the shared behaviour for a device without push: it
  asks for notification permission and then explains that push notifications aren't available on
  this device yet.
- Two builds, like on Android: a **debug** build named "PollenInfo (debug)" that talks to the
  developer's local backend (overridable per developer, e.g. to reach the Mac from a real iPhone),
  and a **release** build named "PollenInfo" that always talks to the production backend. Both can
  be installed side by side. The Android debug build gets the same "PollenInfo (debug)" name.
- The app's final identity (`ch.stenzel.tim.polleninfo`) is never claimed by a Personal Team
  install: until the paid Apple Developer account is ready, the repository holds the placeholder
  `ch.stenzel.tim.testproject` (to be switched to the final identifier then), and each developer's
  private, untracked settings may substitute a throwaway identifier and their own signing team.
- iOS gets its own app icon (default, dark and tinted), designed in Figma from the chosen gauge
  concept and reviewed by the maintainer, a launch screen in the app's surface colour, a location
  permission text in all four app languages, and a privacy manifest.
- The login and the device credential are kept out of iCloud backups and device transfer, as on
  Android.
- Continuous integration gains an iOS pipeline of its own that runs the whole shared test suite on
  an iOS Simulator and builds the iPhone app in both configurations; the Android pipeline is renamed
  to match. The rule deciding which pipeline runs for which change gets its own self-test.

## User Stories

### Using the app on iPhone

1. As an iPhone user, I want to install PollenInfo on my iPhone, so that I can check pollen levels without an Android phone.
2. As an iPhone user, I want the app to look and behave like the Android app, so that the experience is the same on both platforms.
3. As an iPhone user, I want to pick my station during onboarding, so that Home shows the readings that matter to me.
4. As an iPhone user, I want the app to suggest the nearest station from my location, so that I don't have to know which station is close.
5. As an iPhone user, I want to be asked for my location with a clear explanation in my language, so that I understand why it is needed and that it stays on my phone.
6. As an iPhone user, I want to be able to deny location access and still choose a station from the list, so that location is never required.
7. As an iPhone user, I want Home to show the current pollen levels of my station, so that I know how bad it is today.
8. As an iPhone user, I want to pull to refresh on Home, so that I get the newest reading.
9. As an iPhone user, I want to see when a reading is stale, so that I don't trust old data.
10. As an iPhone user, I want to browse all stations as a list and on the map, so that I can compare regions.
11. As an iPhone user, I want to tap a station on the map, so that I see its details.
12. As an iPhone user, I want to register, log in and log out, so that my diary is kept in my account.
13. As an iPhone user, I want the keyboard not to cover the field I'm typing into, so that I can fill in the account form.
14. As an iPhone user, I want to record how I feel today and see my diary next to the pollen history, so that I can relate symptoms to pollen.
15. As an iPhone user, I want to delete my account, so that my data is removed.
16. As an iPhone user, I want to change my station in Settings, so that I can follow a different region.
17. As an iPhone user, I want to choose the app language (English, German, French, Italian or system), so that I can read the app in my preferred language.
18. As an iPhone user, I want to be told that a new language applies the next time I open the app, so that I'm not confused when nothing changes immediately.
19. As an iPhone user, I want to see the app version in Settings, so that I can report which version I use.
20. As an iPhone user, I want to open the data source and other links from Settings, so that I can learn where the data comes from.
21. As an iPhone user, I want the Alarms tab to tell me that push notifications aren't available on this device yet, so that I don't expect alarms to arrive.
22. As an iPhone user, I want to swipe back from a screen, so that navigation feels natural on iPhone.
23. As an iPhone user, I want swiping back from Change station or Account to leave without changing anything, so that backing out is always safe.
24. As an iPhone user, I want the app to follow my light or dark appearance setting, so that it matches the rest of my phone.
25. As an iPhone user, I want the app to work in portrait and landscape, so that I can use it however I hold the phone.
26. As an iPhone user, I want content to stay clear of the notch, the Dynamic Island and the home indicator, so that nothing is hidden.
27. As an iPhone user, I want a recognisable PollenInfo icon that also fits dark and tinted home screens, so that I can find the app easily.
28. As an iPhone user, I want the app to open without a white or black flash, so that launching feels calm.
29. As an iPhone user, I want my login and device credential not to be copied into iCloud backups or onto a new phone, so that my account isn't exposed through backups.
30. As an iPhone user, I want my selected station and other preferences to survive closing and reopening the app, so that I don't have to set it up again.

### Developing and testing

31. As the developer, I want to build and run the iPhone app on the Simulator from the repository, so that I can work on iOS without extra setup.
32. As the developer, I want to install the app on my own iPhone with a free Apple ID, so that I can test on a real device before I have a paid account.
33. As the developer, I want my signing team and a throwaway app identifier in a private, untracked file, so that nothing personal is committed and the real identifier stays unclaimed.
34. As the developer, I want an example of that private file in the repository, so that I know which settings to fill in.
35. As the developer, I want the debug build to use my local backend by default, so that I can test against my own server.
36. As the developer, I want to override the debug backend address privately, so that my iPhone can reach the server on my Mac.
37. As the developer, I want the release build to always use the production backend, so that I can test the real experience and never ship a development address.
38. As the developer, I want insecure plain-HTTP traffic allowed only in the debug build, so that the release build stays secure.
39. As the developer, I want debug and release builds installable side by side with different names, so that I can tell them apart on the home screen.
40. As the developer, I want the Android debug build named "PollenInfo (debug)" as well, so that both platforms are consistent.
41. As the developer, I want the iPhone version name to match the Android one and the build number to follow the commit count, so that versions are comparable across platforms.
42. As the developer, I want the Xcode build to compile the shared code automatically and find a Java installation on its own, so that building from Xcode just works.
43. As the developer, I want the app icon to come from the Figma design file, so that there is one design source for it.
44. As the developer, I want to review the iOS icon in Figma before it goes into the app, so that I approve what ships.
45. As the developer, I want a manual test checklist for Simulator and iPhone, so that the platform parts that can't be unit-tested are checked consistently.
46. As the developer, I want the project documentation to describe the real iOS wrapper, so that future work starts from facts.

### Continuous integration

47. As the maintainer, I want CI to run the shared test suite on an iOS Simulator, so that iOS-specific breakage is caught before merging.
48. As the maintainer, I want CI to build the iPhone app in debug and release, so that broken project or configuration files are caught.
49. As the maintainer, I want the iOS checks in their own pipeline, so that Android and iOS results are separate and easy to read.
50. As the maintainer, I want the checks named `android-tests` and `ios-tests`, so that their purpose is obvious.
51. As the maintainer, I want the iOS pipeline to run when the iPhone wrapper or shared code changes, and to be skipped otherwise, so that CI time is not wasted.
52. As the maintainer, I want the rule that decides which pipeline runs to be tested, so that a mistake there cannot silently skip a check.
53. As the maintainer, I want the iOS pipeline to use a fixed Xcode version, so that a runner image update can't change the build unnoticed.
54. As the maintainer, I want a reminder in the pull request to update the required checks, so that merging isn't blocked by checks that no longer exist.

## User Acceptance Tests

### Building and installing

1. Given a fresh clone with no private settings file, when the developer builds and runs the debug app on the iOS Simulator from Xcode, then the app launches and shows onboarding.
2. Given the developer's private settings file with their free Apple ID team and a throwaway identifier, when they build and run the debug app on their iPhone, then it installs and launches, and Apple's device registration shows only the throwaway identifier.
3. Given the same private settings file, when the developer runs the release build on their iPhone, then it installs next to the debug build and both appear on the home screen as "PollenInfo" and "PollenInfo (debug)".
4. Given no private settings file, when the developer runs the release build on the Simulator, then the app shows real readings from the production backend.
5. Given the local backend is running on the Mac, when the debug app runs on the Simulator, then Home shows readings served by the local backend.
6. Given the developer's private settings override the debug backend address with the Mac's network address, when the debug app runs on their iPhone in the same network, then it shows readings from the local backend.
7. Given the release build, when its configuration is inspected, then it allows no plain-HTTP connections and points only at the production backend.
8. Given an Android debug build is installed, when the launcher is opened, then the app is named "PollenInfo (debug)", and the Android release build is still named "PollenInfo".
9. Given an iPhone build, when Settings is opened, then the version line shows the same version name as Android and a build number equal to the repository's commit count.

### Onboarding and location

10. Given a fresh install, when the user chooses to use their location during onboarding, then iOS asks for location permission with the PollenInfo explanation in the app's language.
11. Given the device language is German, French or Italian, when the location prompt appears, then its explanation is in that language; for any other language it is in English.
12. Given the user allows location, when the lookup finishes, then the nearest station is suggested.
13. Given the user denies location, when onboarding continues, then the user can still pick a station from the list and is told that location was not allowed.
14. Given a location lookup is still running, when the user picks a station from the list, then the late location result does not override the choice.
15. Given onboarding is completed, when the app is closed and reopened, then Home opens directly with the chosen station.

### Screens

16. Given the user is on Home, when they pull to refresh, then the reading reloads and the refresh time updates.
17. Given the All stations tab, when the user switches between list and map and taps a station on the map, then that station's details are shown.
18. Given the user has no account, when they register on the Account screen, then the keyboard never covers the active field and the diary becomes available.
19. Given the user is logged in, when they record a feeling and open the diary, then the feeling and the pollen history are shown together.
20. Given the user is logged in, when they log out or delete their account, then the app behaves as on Android.
21. Given Settings, when the user changes the language, then a note says it applies on next open; after closing and reopening, the app is in the new language.
22. Given Settings, when the user taps the data source link, then it opens in the browser.
23. Given the Alarms tab on iPhone, when the user opens it and allows notifications, then a message says that push notifications aren't available on this device yet, and no alarm list appears.
24. Given the user opened Change station from Settings and picked a different station without saving, when they swipe back, then Settings shows the previous station unchanged.
25. Given any non-tab screen, when the user swipes from the left edge, then the app goes back.
26. Given the phone is in dark mode, when the app is opened, then all screens use the dark theme; in light mode the light theme.
27. Given an iPhone with a Dynamic Island, when the app is rotated to landscape and back, then no content is hidden behind the island, the corners or the home indicator.

### Look and launch

28. Given the app is installed, when the home screen is shown in default, dark and tinted icon modes, then the PollenInfo gauge icon is shown in the matching variant, as approved in Figma.
29. Given the app is launched in light or dark mode, when it opens, then the launch screen has the app's surface colour and there is no flash between launch screen and first screen.

### Privacy and storage

30. Given the user is logged in on an iPhone, when the app's data is inspected, then the login and device credentials are stored in a location marked as excluded from backup, while station and preferences are stored normally.
31. Given the user is logged in and the iPhone is restored from an iCloud backup onto a device, when the app is opened, then the user is logged out and the station selection is kept.
32. Given the iPhone app bundle, when its privacy manifest is inspected, then it declares no tracking and the reason for using app preferences storage.

### Continuous integration

33. Given a pull request that changes shared app code, when CI runs, then both `android-tests` and `ios-tests` run and pass.
34. Given a pull request that changes only the iPhone wrapper, when CI runs, then `ios-tests` runs and `android-tests` is skipped.
35. Given a pull request that changes only the server, when CI runs, then `ios-tests` and `android-tests` are both skipped and count as passed.
36. Given a pull request that breaks the iPhone project configuration, when CI runs, then `ios-tests` fails.
37. Given a pull request with a shared test that fails only on iOS, when CI runs, then `ios-tests` fails.
38. Given a change to the rule that maps changed files to pipelines that breaks one of its cases, when CI runs, then the self-test fails.
39. Given the pull request for this feature, when it is opened, then its description reminds the maintainer to replace the required checks with `android-tests` and `ios-tests` before merging.

## Definition of Done

- All user acceptance tests pass, on the iOS Simulator and on the maintainer's iPhone where they concern the device.
- The manual iPhone checklist has been run once in full on the Simulator and once on a real iPhone, with results recorded.
- The iOS app icon variants were approved by the maintainer in Figma before being added to the app.
- The real app identifier was never registered with Apple during this work.
- The Android app behaves exactly as before, apart from the debug build's new name.
- The shared test suite passes on the JVM and on the iOS Simulator; server tests pass.
- Both CI pipelines pass on the pull request, and the pipeline-selection self-test passes.
- The iPhone app builds without new compiler or build warnings in debug and release.
- No developer-specific setting (signing team, personal identifier, local address) is committed.
- Project documentation (developer guide, README, operations runbook) describes the iPhone app, the private settings file, the new pipelines and the required check names.
- All remarks claiming that iOS code was never run are removed or replaced by what was actually checked.

## Out of Scope

- **Push notifications on iOS**: no Apple push setup, no Firebase SDK on iOS, no push-related entitlements, no server changes. The Alarms tab keeps its "not available yet" message, including the notification permission prompt that precedes it.
- **Crash reporting on iOS**: added later together with push, sharing the Firebase setup and the final app identifier.
- **Password autofill** (needs a paid account): no associated-domains setup, no website association file.
- **Distribution**: no archive, TestFlight, App Store listing, store screenshots or privacy labels.
- **iPad**: iPhone only.
- **iOS-native restyling**: the app keeps its Compose look; no Cupertino components or iOS-specific navigation bar.
- **Keychain storage** for credentials; files excluded from backup match the Android level of protection.
- **Updating GitHub branch protection**: done manually by the maintainer at merge time.
- **Changing alarms behaviour** for devices without push. Because the alarm list never loads on
  iPhone, creating an alarm, and with it the alarm editor, is unreachable there; it is verified on
  iOS once push exists.

## Further Notes

- The iOS-specific code paths (location, language, version, storage, notification permission) run for the first time in this feature. Fixing defects they reveal is in scope as long as Android behaviour is unchanged; anything that needs a design decision is noted in the checklist and raised with the maintainer instead of being redesigned.
- A Personal Team install expires after seven days and must then be reinstalled from Xcode; this is a limit of the free Apple ID, not of the app.
- When the paid account is available, removing two lines from the private settings file makes the builds use the real identifier; nothing in the repository needs to change for that.
- Whether the GitHub-hosted macOS runners offer the pinned Xcode version must be checked during implementation; if not, the newest available version that builds without warnings is pinned and the choice recorded.
- The location prompt text is the one piece of translated text outside the shared translation check; it is reviewed by hand against the project's language style rules.
- **Accepted limitation** (decided 2026-10-22): iOS words the location prompt in the device language. A language chosen only in PollenInfo's Settings does not change it; the app cannot influence this.

---

## Technical Annex
> Written against codebase as of: 2026-10-22 (branch `feature/ios-app`, HEAD `265ca0a`)

### Architectural Decisions

#### Existing state (verified)

- No `iosApp/` exists. `composeApp` already declares `iosArm64()` and `iosSimulatorArm64()` with a
  static framework `baseName = "ComposeApp"`; `iosMain` depends on `ktor-client-darwin`.
- `iosMain` contains `MainViewController.kt` (hard-coded `API_BASE_URL = "http://localhost:8080"`),
  `PlatformModule.ios.kt` (three DataStores in `NSDocumentDirectory`, `IosCoarseLocationProvider`,
  `UnavailablePushTokenProvider`, `IosAppInfo`, `IosLanguageRepository`, `NoCredentialStore`,
  `NoCrashReporter`), `LocationPermissionRequester.ios.kt`, `NotificationPermissionController.ios.kt`,
  `HttpClientEngine.ios.kt`. Several carry "compile-verified only / no wrapper yet" KDoc.
- `IosLanguageRepository.appliesImmediately = false` already drives `settings_language_restart_note`.
- Alarms on iOS: `AlarmRepositoryImpl` maps `PushTokenResult.Unavailable` → `PushUnavailableException`
  → `AppError.PushUnavailable`, shown by `AlarmsScreen`, after the permission gate. **Unchanged.**
- Xcode 27.0 is installed locally (CLAUDE.md still says "only CLT here").
- Theme surfaces: `surfaceLight = 0xFFFBFAF3`, `surfaceDark = 0xFF16211A` (`theme/.../Color.kt`).
- Android version: `appVersionName = "1.0.0"`, `appVersionCode` = `git rev-list --count HEAD`
  (fallback 1) in `androidApp/build.gradle.kts`; debug has `applicationIdSuffix = ".debug"`,
  `versionNameSuffix = "-debug"`. `app_name` = "PollenInfo" (`translatable="false"`) in
  `androidApp/src/main/res/values/strings.xml`.
- CI: `.github/workflows/android.yml` has jobs `changes`, `android-gate`, `ios-compile`
  (`compileTestKotlinIosSimulatorArm64` on `macos-latest`), `release` (`needs: [changes, android-gate]`).
  `.github/scripts/changed-components.sh` maps `.github/workflows/android.yml` to android **and** ios;
  `iosApp/` currently maps to nothing. No test exists for the script.
- Figma redesign file `naeELzkiFSdMuXsnmZVKnW`, page "App Icon" (`57:2`), concept A (gauge) chosen;
  Android geometry: 1024 canvas, centre 512,600, radii 214–300, grain r72.

#### 1. Xcode wrapper (`iosApp/`)

- `iosApp/iosApp.xcodeproj`, one target `iosApp`, file-system-synchronized group `iosApp/iosApp/`,
  no CocoaPods, no SPM packages.
- `PollenInfoApp.swift`: SwiftUI `@main`; reads `API_BASE_URL` from `Bundle.main.infoDictionary`
  and hosts `MainViewControllerKt.MainViewController(apiBaseUrl:)` through a
  `UIViewControllerRepresentable`, `.ignoresSafeArea()` (Compose handles safe area and IME insets).
- Build phase 1 "Compile Kotlin": `./gradlew :composeApp:embedAndSignAppleFrameworkForXcode`;
  script sandboxing off; JDK lookup: `$JAVA_HOME` → `/usr/libexec/java_home` → Android Studio JBR
  (`/Applications/Android Studio.app/Contents/jbr/Contents/Home`); fail with a clear message otherwise.
- Build phase "Stamp Build Number" (after Info.plist processing): `PlistBuddy -c "Set :CFBundleVersion <n>"`
  on `${TARGET_BUILD_DIR}/${INFOPLIST_PATH}`, `n = git rev-list --count HEAD` (fallback `1`), matching
  `androidApp/build.gradle.kts`. Declare input/output files or mark "based on dependency analysis"
  off so it runs every build.
- Deployment target iOS 18.x (lowest that links without Skiko/ICU warnings; determine during
  implementation), `TARGETED_DEVICE_FAMILY = 1`, portrait + both landscapes,
  `EXCLUDED_ARCHS[sdk=iphonesimulator*] = x86_64`.
- `Assets.xcassets`: `AppIcon` (single-size 1024 entries: any, dark, tinted), `LaunchBackground`
  colour set (any `#FBFAF3`, dark `#16211A`), `UILaunchScreen` → `UIColorName = LaunchBackground`.
- `PrivacyInfo.xcprivacy`: `NSPrivacyTracking = false`, no tracking domains, no collected data types,
  `NSPrivacyAccessedAPICategoryUserDefaults` reason `CA92.1`.

#### 2. Build configuration (`iosApp/Config/`)

```
// Base.xcconfig
BUNDLE_ID_BASE = ch.stenzel.tim.testproject   // placeholder; ch.stenzel.tim.polleninfo once the paid Apple account is ready
MARKETING_VERSION = 1.0.0            // keep in step with appVersionName in androidApp/build.gradle.kts
CURRENT_PROJECT_VERSION = 1          // overwritten in the built Info.plist by "Stamp Build Number"
DEVELOPMENT_TEAM =

// Debug.xcconfig
#include "Base.xcconfig"
PRODUCT_BUNDLE_IDENTIFIER = $(BUNDLE_ID_BASE).debug
PRODUCT_DISPLAY_NAME = PollenInfo (debug)
API_BASE_URL = http:/$()/localhost:8080
GCC_PREPROCESSOR_DEFINITIONS / INFOPLIST_PREPROCESSOR_DEFINITIONS = POLLENINFO_LOCAL_NETWORKING=1
#include? "Local.xcconfig"

// Release.xcconfig
#include "Base.xcconfig"
PRODUCT_BUNDLE_IDENTIFIER = $(BUNDLE_ID_BASE)
PRODUCT_DISPLAY_NAME = PollenInfo
API_BASE_URL = https:/$()/api.polleninfo.ch
#include? "Local.xcconfig"
```

- `Local.xcconfig` is git-ignored, optional (`#include?`), included by **both** configurations; it may
  set `BUNDLE_ID_BASE`, `DEVELOPMENT_TEAM` and (read by Debug only by convention) `API_BASE_URL`.
  Because Release includes it too, the example file must say an `API_BASE_URL` there applies to
  Release as well — guard it: Release re-assigns `API_BASE_URL` **after** the include
  (`API_BASE_URL = https:/$()/api.polleninfo.ch` placed below `#include?`) so Release always uses
  production. Debug's `API_BASE_URL` sits **above** the include so Local can override it.
- `Local.xcconfig.example` committed, documenting the three settings.
- In xcconfig, `//` starts a comment: URLs are written `http:/$()/…`.
- `Info.plist` hand-written, `INFOPLIST_PREPROCESS = YES` (`-traditional`): `API_BASE_URL`,
  `CFBundleDisplayName = $(PRODUCT_DISPLAY_NAME)`, `CFBundleShortVersionString = $(MARKETING_VERSION)`,
  `CFBundleVersion = $(CURRENT_PROJECT_VERSION)`, `CFBundleDevelopmentRegion = en`,
  `CFBundleLocalizations = [en, de, fr, it]`, `NSLocationWhenInUseUsageDescription`,
  `NSLocationDefaultAccuracyReduced = true`, `UILaunchScreen`, `UISupportedInterfaceOrientations`,
  `CADisableMinimumFrameDurationOnPhone = true`, and `NSAppTransportSecurity.NSAllowsLocalNetworking`
  inside `#if POLLENINFO_LOCAL_NETWORKING`. No `/*` may appear in the file (C preprocessor).
- `InfoPlist.xcstrings`: `NSLocationWhenInUseUsageDescription` in en/de/fr/it:
  - en: "Your location is used once to suggest the nearest pollen station. It never leaves your phone."
  - de: "Dein Standort wird einmalig verwendet, um die nächste Pollenmessstation vorzuschlagen. Er verlässt dein Telefon nie."
  - fr: "Votre position est utilisée une seule fois pour proposer la station pollinique la plus proche. Elle ne quitte jamais votre téléphone."
  - it: "La Sua posizione viene usata una sola volta per proporre la stazione pollinica più vicina. Non lascia mai il Suo telefono."
  Display name is not localized.

#### 3. Entry point

- `MainViewController(apiBaseUrl: String)` in `composeApp/src/iosMain/.../MainViewController.kt`;
  the `API_BASE_URL` constant is removed; `startKoin { modules(appModules(apiBaseUrl)) }` as today.
  Mirrors the Android rule "the base URL comes from the platform entry point".

#### 4. On-device storage (`PlatformModule.ios.kt`)

- All three DataStores move from `NSDocumentDirectory` to `NSApplicationSupportDirectory`
  (`create = true`; the directory does not exist on a fresh install).
- `DEVICE_DATA_STORE_NAME` and `SESSION_DATA_STORE_NAME` files live in
  `Application Support/secrets/`; the general preferences store stays in `Application Support/`.
- On every `platformModule` construction, create `secrets/` if missing and set
  `NSURLIsExcludedFromBackupKey = true` on the **directory** URL (`setResourceValue:forKey:error:`) —
  DataStore writes via temp file + rename, which would drop a flag set on the file. Failure to set
  the flag is not fatal (the app still works) but must not be silently swallowed: `require`/log as
  the existing `requireNotNull` pattern does; decide during implementation, keeping the "No
  hard-coded text" allowed-hits list in CLAUDE.md accurate.
- No migration from Documents (iOS never shipped).
- Stays "logic-free and untested" per the DataStore rule; verified by the checklist.

#### 5. Android debug name

- New `androidApp/src/debug/res/values/strings.xml` with
  `<string name="app_name" translatable="false">PollenInfo (debug)</string>`.
  `versionNameSuffix = "-debug"` stays.

#### 6. App icon (Figma → asset catalog)

- On Figma page "App Icon" (`57:2`), add an "iOS" section with three 1024×1024 frames from concept A,
  bound to the file's colour variables: **Default** (Android icon background, gauge re-centred and
  scaled to ~80 % of the canvas per Apple's grid), **Dark** (gauge on dark background), **Tinted**
  (grayscale gauge, transparent background). No rounded corners; no transparency except Tinted.
- Maintainer reviews in Figma before export. Export PNG at 1× (1024 px) into
  `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/` with a `Contents.json` using
  `"idiom": "universal", "platform": "ios", "size": "1024x1024"` and `appearances` for dark/tinted.
- CLAUDE.md records: the iOS icon's source is Figma page "App Icon"; change there and re-export.

#### 7. CI

- `.github/workflows/android.yml` ("Android"): `changes` (outputs `android` only), `android-tests`
  (renamed from `android-gate`, body unchanged), `release` (`needs: [changes, android-tests]`).
  `ios-compile` removed from this file; header comment updated.
- `.github/workflows/ios.yml` ("iOS", new): same triggers as the others (PRs into `develop`/`main`,
  pushes to `main`, no path filter), `permissions: contents: read`, concurrency group
  `ios-${{ github.ref }}`, `changes` job (output `ios`), and `ios-tests` on a pinned macOS runner:
  1. checkout (pinned SHA, `persist-credentials: false`), `./.github/actions/setup-jvm`
  2. `sudo xcode-select -s /Applications/Xcode_<pinned>.app`
  3. `./gradlew :composeApp:iosSimulatorArm64Test`
  4. `xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build`
  5. the same for `Release`
  Every action pinned by commit SHA (existing rule).
- `.github/scripts/changed-components.sh` mapping becomes:
  - `server/**`, `deploy/**`, `.github/workflows/server.yml` → server
  - `androidApp/**`, `.github/workflows/android.yml` → android
  - `iosApp/**`, `.github/workflows/ios.yml` → ios
  - `composeApp/**`, `theme/**` → android, ios
  - `gradle/**`, root Gradle files, `gradlew(.bat)`, `buildSrc/**`, `.github/actions/**`, `.github/scripts/**` → all
  Header comment updated (it currently lists `iosApp/` as "runs no gate").
- New `.github/scripts/changed-components-test.sh`: builds a throwaway git repo in a temp directory,
  for each case commits the listed paths on top of a base commit, runs `changed-components.sh <base> HEAD`
  with `GITHUB_OUTPUT` unset and compares stdout with the expected `server=/android=/ios=` lines.
  Cases at least: one per mapping row, a docs-only change (`README.md`, `.sdd/x.md`) → none, a
  mixed change, an all-zero base → all, an unresolvable base → all. Exits non-zero on the first
  mismatch with a diff of expected vs. actual. Run as the first step of every workflow's `changes`
  job (server, android, ios), so a broken mapping fails every pipeline. Since `.github/scripts/**`
  maps to all components, editing either script also runs every gate.
- Required checks change from `android-gate`/`ios-compile` to `android-tests`/`ios-tests` — manual,
  by the maintainer; the PR description says so.

#### 8. Documentation

- CLAUDE.md: replace "iOS wrapper configuration" ("There is no `iosApp` Xcode project yet") with the
  real wrapper (configurations table, `Local.xcconfig`, build phases, Info.plist preprocessing,
  icon source, storage location and backup exclusion); update the iOS row of the base-URL table;
  drop "needs full Xcode (only CLT here)"; add `iosSimulatorArm64Test` and `xcodebuild` commands;
  CI paragraph names `android-tests`, `ios-tests`, `ios.yml` and the self-test; keep the
  "No hard-coded text" allowed-hit list accurate.
- `README.md` and `deploy/README.md`: three workflows, new check names, required-checks list,
  iOS getting-started (Xcode, `Local.xcconfig.example`, Personal Team).
- `iosApp/CHECKLIST.md`: manual checks for Simulator and real iPhone (onboarding allow/deny,
  Home + refresh, All stations list/map tap, Diary with account, Account keyboard, Settings language
  relaunch / version / links, Alarms message, back swipe from non-tab screens, dark mode, landscape and
  safe areas, release build against production, backup-exclusion flag on `secrets/`), each with a
  result column.

#### 9. First-run fixes

- Defects found while running the iOS code are fixed in `iosMain`, or in shared code only when the
  defect is iOS-only and Android behaviour is unchanged. KDoc claiming "compile-verified only" /
  "no wrapper yet" is replaced by what was checked (on `IosCoarseLocationProvider`,
  `IosLanguageRepository`, `IosAppInfo`, `NotificationPermissionController.ios.kt`,
  `PlatformModule.ios.kt`, `MainViewController.kt`, `UnavailablePushTokenProvider` as applicable).

### Automated Testing Decisions

- Good tests check externally visible behaviour (states, results, outputs for given inputs), not
  implementation details; most of this feature is configuration and platform glue whose behaviour
  is only observable by building and running the app, so it is verified by builds and the manual
  checklist rather than unit tests.
- **Shared test suite on iOS:** `ios-tests` runs `:composeApp:iosSimulatorArm64Test`, i.e. all of
  `commonTest` on an iOS Simulator — the main automated safety net for this feature. It must pass
  locally and in CI.
- **Build verification:** `ios-tests` builds the wrapper unsigned for the Simulator in Debug and
  Release, catching xcconfig, Info.plist preprocessing, asset catalog and build-phase errors.
- **Shared-code fixes** discovered on first run get a `commonTest` where the behaviour is testable
  (prior art: `StationPickerTest`, `SettingsViewModelTest`, ViewModel tests with
  `StandardTestDispatcher` and hand-written fakes; backtick names without commas).
- **`changed-components-test.sh`** (new): table-driven shell self-test against a temporary git repo,
  one case per mapping row plus docs-only, mixed, all-zero-base and bad-base cases; runs in every
  workflow's `changes` job. No existing prior art for script tests in this repo; keep it plain bash
  with `set -euo pipefail`, no extra tooling.
- **Not unit-tested** (by decision): iOS DataStore location and backup exclusion (DataStore
  implementations are logic-free and untested by project rule; app tests may only live in
  `commonTest`), Swift entry point, build-number stamp, xcconfigs, icon and launch assets, Android
  debug name. These are covered by `iosApp/CHECKLIST.md` and the CI builds.
- Regression: `./gradlew :composeApp:testAndroidHostTest :server:test` and the Android pipeline stay
  green; no new compiler warnings (`allWarningsAsErrors`) on any target.
