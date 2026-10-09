# Verification report — dependency-update

Task 06 (release readiness), performed 2026-10-09 on branch `feature/update-kotlin-agp-and-libs`.
Screenshots are in `verification/`.

**Environment**

- Emulator: AVD "Medium_Phone", **API 37** (Android 17, `google_apis_playstore_ps16k` arm64,
  build `CP41.260831.007`), gesture navigation (`navigation_mode` 2). Tasks 02–05 used API 36;
  an API 37 image is installed now, so this pass uses it as the plan intended.
- Backend: `./gradlew :server:run` with `POLLENINFO_DB` pointing at a fresh scratch file, no
  `FCM_CREDENTIALS` (pushes are logged).
- UI driven with `adb shell input` and `uiautomator dump`; each result below was read from the
  dumped view tree and/or a screenshot.

## Finding — Android 17 blocks the development backend (resolved: option 1)

**On API 37 the debug app cannot reach `:server:run` at `http://10.0.2.2:8080`.** Android 17
restricts access to local-network addresses (the `10.0.0.0/8` range includes the emulator's
host alias) for apps targeting SDK 37, unless the app holds the new runtime permission
`android.permission.ACCESS_LOCAL_NETWORK` (protection level `dangerous`). With `targetSdk 37`,
which task 02 set, the station list fails with "Couldn't reach the server". The port is reachable
from `adb shell` (`nc 10.0.2.2 8080`), and the request never reaches the server log.

Proof: a debug build with `<uses-permission android:name="android.permission.ACCESS_LOCAL_NETWORK" />`
added to `androidApp/src/debug/AndroidManifest.xml` and the permission granted with
`adb shell pm grant ch.stenzel.tim.polleninfo.debug android.permission.ACCESS_LOCAL_NETWORK`
loads the list at once. **That change was made only to finish this walk-through and was reverted.
It is not committed.** Every emulator check below ran on that build. Apart from that one permission
line, the build is identical to the committed code.

Scope: development only. A deployed backend at a public `https` address is not a local-network
address, and the release build already refuses cleartext. API 36 and below are unaffected. Options
(not implemented — for the product owner to choose):

1. Declare `ACCESS_LOCAL_NETWORK` in the **debug** manifest only, and document the `pm grant`
   command (or request it in debug builds) next to the cleartext exception in CLAUDE.md.
2. Point the Android `apiBaseUrl` at `http://127.0.0.1:8080` and use `adb reverse tcp:8080
   tcp:8080`. Loopback is not local-network access, but every emulator session needs the
   `adb reverse` step.

**Resolved by the product owner's choice of option 1:** the debug manifest declares
`ACCESS_LOCAL_NETWORK` (merged into the debug manifest only; the release merged manifest does not
contain it), and CLAUDE.md ("Talking to our own backend") documents the `pm grant` command needed
after each fresh install. Verified on the API 37 emulator: after install + grant, the debug app's
`GET /pollen/stations` answers `200 OK` and onboarding shows the list.

## Checks performed

| # | Check | Method | Result |
| --- | --- | --- | --- |
| 1 | Versions current | Script over each catalog entry's `maven-metadata.xml` (Maven Central, Google Maven), stable versions only; Gradle via `services.gradle.org/versions/current`. Then `:androidApp:lintDebug`, which runs the same `NewerVersionAvailable` / `GradleDependency` detectors that draw Android Studio's "newer version available" highlight across `libs.versions.toml`. | Gradle 9.8.1 is current. Lint flagged **Exposed 1.5.0 → 1.5.1**, released during the session; bumped in its own commit `55c3160` (standard checks and 354 server tests incl. `LegacyDatabaseCompatibilityTest` green). Lint re-run afterwards: **0** `NewerVersionAvailable` / `GradleDependency` issues. Its only remaining warnings already existed and are not about versions: `ObsoleteSdkInt` (`mipmap-anydpi-v26`) and `UnusedAttribute` (`localeConfig` below API 33). Held entries keep their catalog comments: navigation-compose 2.x (decision), kotlinx-datetime `strictly` (compat artifact). The Android Studio window itself was not opened. |
| 2 | Clean build, all warnings | `./gradlew clean`, then `--rerun-tasks --warning-mode all` over host tests, server tests, iOS test compile, `checkTranslations`, `:androidApp:assembleDebug`, `:androidApp:assembleRelease` and the iOS main compiles of `:composeApp` / `:theme` (176 tasks, all executed). | **No `w:` line.** One Gradle deprecation: AGP 9.4.1 `Configuration.setVisible` (`> Configure project :androidApp`). KGP 2.4.21's `KotlinNativeBundleArtifactsTypes` printed in the fresh daemon's `clean` call instead. Both come from the plugins and are documented in CLAUDE.md; nothing in our scripts deprecates. After the Exposed bump, `:server:test --rerun-tasks --warning-mode all` was again clean. |
| 3 | Fresh install → onboarding | Debug APK installed on a new emulator (no previous install), launched. | Onboarding "Welcome to PollenInfo" (`01-onboarding.png`). |
| 4 | Location prompt | "Use my location". | System prompt "Allow PollenInfo to access this device's approximate location?" (`02-location-prompt.png`). After "Only this time" the picker says "Your location could not be determined…". This is expected on the emulator: it has no `NETWORK_PROVIDER` fix, the only provider the app uses by design. Station picked manually (Bern). |
| 5 | Home | Continue after picking Bern. | Bern's readings, "Data from 20:00", "Refreshed 22:21", seven species, feeling prompt (`03-home.png`). "Good" recorded and the prompt hid. |
| 6 | All stations | Tab, then tapped the Genève row. | Map with 15 dots and lakes, Genève circled, row expanded into Home's detail (`04-all-stations.png`). |
| 7 | Diary | Tab, then Week / Month / Year. | Chart "last 30 days", then "last 12 months" with species lines and both axes (`05-diary-month.png`, `06-diary-year.png`). `GET …/PBE/history` 200 both times. The no-answers hint shows, which is correct because today's answer is never plotted. |
| 8 | Notification prompt | Alarms tab, "Allow notifications". | System prompt "Allow PollenInfo to send you notifications?" (`07-notification-prompt.png`). After Allow: `POST /devices` 201, `PUT …/token` 204, empty list. |
| 9 | Create alarm | "Create alarm", defaults, Save. | `POST …/alarms` 201; row "Bern — Daily report at 08:00 · Every day · All pollen types" with its switch. |
| 10 | Discard dialog, back gesture | In the new-alarm editor, deselected Oak, then an edge swipe from the left (`input swipe 2 1200 600 1200`). | "Discard changes?" with Keep editing / Discard (`08-discard-gesture.png`). Keep editing closed it. |
| 11 | Discard dialog, top-bar arrow | Same unsaved change, tapped the "Back" arrow. | "Discard changes?" (`09-discard-arrow.png`). |
| 12 | Change station | Settings → Default station → Zürich → Save. | Settings row shows Zürich; Home then loads `PZH` (`GET /pollen/stations/PZH/measurements`). |
| 13 | Language | Settings → Language → Deutsch. | Screen redrawn at once: "Standardstation", "Sprache", "Entwickelt von Tim Stenzel" (`10-settings-de.png`). Home in German: "Daten von 20:00", "Aktualisiert um 22:24", "Bestimmt durch Erle" (`11-home-zurich-de.png`). |
| 14 | Release APK | `:androidApp:assembleRelease` (R8), `zipalign` + `apksigner` with the local debug keystore (the release output is unsigned), installed and launched. | Starts; `MainActivity` is the top resumed activity, no `FATAL`, onboarding shown (`12-release.png`). Its list fails with `CLEARTEXT communication to 10.0.2.2 not permitted`. That is the intended release behaviour (UAT 12, live). |
| 15 | Cleartext only in debug | `grep -l usesCleartextTraffic androidApp/build/intermediates/merged_manifest/*/process*MainManifest/AndroidManifest.xml` | Only `debug/processDebugMainManifest` matches. |
| 16 | User-facing text unchanged | `git diff develop -- '**/strings.xml'` | Only `app_name` moved from `composeApp/src/androidMain/res/values` to `androidApp/src/main/res/values`, same value (`PollenInfo`, `translatable="false"`). |
| 17 | Standard checks on the final code | `:composeApp:testAndroidHostTest :server:test :composeApp:compileTestKotlinIosSimulatorArm64 :composeApp:checkTranslations :androidApp:assembleDebug` | All green: 631 app tests, 354 server tests, 0 failures. |

Not re-checked by hand here, because earlier tasks covered them: the in-place update from the
pre-split APK (task 03, API 36) and the server starting on a copy of the Exposed 0.61 database
(task 05; automated in `LegacyDatabaseCompatibilityTest`, green on 1.5.1).

## Product owner's checks

The feature counts as done only once both are confirmed here.

### UAT 16 — a real push in the chosen language

1. Put a Firebase service-account key for the project somewhere outside the repository.
2. `FCM_CREDENTIALS=/path/to/key.json ./gradlew :server:run` (no "push notifications are logged"
   warning should appear at start).
3. Install the debug build on a device or emulator with Play services. **On API 37, see the finding
   above: after a fresh install run
   `adb shell pm grant ch.stenzel.tim.polleninfo.debug android.permission.ACCESS_LOCAL_NETWORK`.**
4. Set the app language in Settings (e.g. Deutsch), allow notifications, and create a daily report
   for any station, "Any", every day, at the next whole minute + 1.
5. When that minute passes, a notification on the "Daily reports" channel must arrive. Its title
   names the station and its text lists the pollen levels in the chosen language, with the same
   wording as before the update. Optionally repeat with a threshold alert.

| Result | Language / device | Date | Confirmed by |
| --- | --- | --- | --- |
| ☐ passed ☐ failed | | | |

Notes:

### UAT 17 — a day of normal use

Install the updated app over your current one on your own device (in-place update, same signing)
and use it normally for a day: Home and the feeling question, All stations, Diary, your alarms
arriving at their times, Settings. Anything that behaves differently from before counts as a
failure; please note it here.

| Result | Device / Android version | Date | Confirmed by |
| --- | --- | --- | --- |
| ☐ passed ☐ failed | | | |

Notes:
