# iOS manual test checklist

What automated tests cannot reach on iPhone: the Swift wrapper, the platform implementations, layout
against the notch and home indicator, gestures, push and autofill. Numbers are the user acceptance
tests of `.sdd/ios-app/requirements.md`. Run the list before a release and after any change to the
wrapper or an iOS platform implementation; record the date, build and result in the last column.

**Where:** *Simulator* runs without an Apple Developer Program membership. *iPhone* needs a real
device and the Apple/Firebase setup (`deploy/README.md`); push and password autofill only work there.
**From** is the task of the iOS feature after which the test can pass.

Preparation: backend on the Mac (`docker compose -f deploy/compose.dev.yaml up -d`,
`./gradlew :server:run`), then run the `iosApp` scheme (Debug) on an iPhone simulator with a notch or
Dynamic Island (e.g. iPhone 17). For a fresh first launch: `xcrun simctl uninstall booted
ch.stenzel.tim.polleninfo.debug`. A location for the simulator: Features → Location → Custom Location
(e.g. 47.3769, 8.5417 for Zürich).

## Installation and basics

| # | Test | Where | From | Result |
| --- | --- | --- | --- | --- |
| 1 | Debug build on a simulator starts and shows onboarding on first launch | Simulator | 01 | 2026-10-17, iPhone 17 / iOS 27.0: passed |
| 2 | Debug and Release installed side by side: two apps named "PollenInfo", each keeps its own station (simulator), alarms and login (iPhone) | Both | 01 / 03 | 2026-10-17: two "PollenInfo" icons, separate data containers; choosing a station in each not yet run |
| 3 | Release loads readings from `https://api.polleninfo.ch`; Debug without `Local.xcconfig` from the Mac's server (stop the server: Debug shows an error, Release still loads) | Simulator | 01 | 2026-10-17, no local server: Debug shows the station-list error, Release loads the stations: passed |
| 4 | Dark mode: launch screen and first screen share the dark surface colour, no light flash | Simulator | 01 | 2026-10-17, Release: 12 screenshots ~0.6 s apart from launch, all #13140D behind the content: passed |
| 5 | Onboarding → "Use my location" → allow: the nearest station is preselected | Simulator | 01 | |
| 6 | First location request shows the app's explanation (in the app's language) and offers approximate location only (no "Precise" toggle on) | Simulator | 01 | |
| 7 | No content under the notch / Dynamic Island or the home indicator, on every tab and in landscape | Simulator | 01 | |
| 8 | Alarm editor with an unsaved change: edge swipe back shows the discard question | Simulator | 01 | |

## Readings, stations, diary, account

| # | Test | Where | From | Result |
| --- | --- | --- | --- | --- |
| 9 | Home shows the current levels per type with the same words and colours as Android | Simulator | 01 | |
| 10 | All stations: list and map show all 15 stations; tapping a map dot selects that station | Simulator | 01 | |
| 11 | Logged out → register with email, password and consent → logged in, diary available | Simulator | 01 | |
| 12 | Logged in → record today's feeling → it appears on the diary chart next to the pollen history | Simulator | 01 | |

## Alarms and push

| # | Test | Where | From | Result |
| --- | --- | --- | --- | --- |
| 13 | Notifications never requested → open Alarms: permission request without badge | Simulator | 03 | |
| 14 | Notifications denied → open Alarms: the app offers Settings and opens PollenInfo's settings page | Simulator | 03 | |
| 15 | Notifications allowed → create a daily report alarm: saved and listed | iPhone | 03 | |
| 16 | Daily report due in a minute, app force-quit: notification arrives, title names the station, body lists types and levels in the app's language | iPhone | 04 | |
| 17 | App language German, iPhone English: notification text is German | Both | 04 | |
| 18 | App language "system default", iPhone French: notification text is French | Both | 04 | |
| 19 | Threshold alarm whose type reached the level within the window: exactly one notification for that type that day | iPhone | 04 | |
| 20 | App in the foreground: an alarm notification shows as a banner with sound | Both | 03 | |
| 21 | Tapping an alarm notification opens the app | Both | 03 | |
| 22 | Reports and alerts are grouped separately in the notification centre | Both | 03 | |
| 23 | The extension cannot word a notification (simulated): a generic localized notification is shown | Both | 04 | |
| 24 | No badge on the app icon after a notification | Both | 03 | |
| 25 | No network on the first alarm → Alarms shows an error with Retry; Retry after reconnecting succeeds | Simulator | 03 | |
| 26 | After the push address (FID) changes, the next alarm still arrives | iPhone | 03 | |
| 27 | Android: alarm notifications look and behave exactly as before | Android | 02 / 04 | |

## Password manager

| # | Test | Where | From | Result |
| --- | --- | --- | --- | --- |
| 28 | A saved PollenInfo password is offered on the login screen; choosing it logs in | iPhone | 05 | |
| 29 | No saved password: no sheet, the form is usable at once | iPhone | 05 | |
| 30 | After registration iOS offers to save the password (or recorded as a known limitation) | iPhone | 05 | |
| 31 | Focusing email or password: the keyboard offers matching saved credentials | iPhone | 05 | |

## Language

| # | Test | Where | From | Result |
| --- | --- | --- | --- | --- |
| 32 | English → Italian in the app's settings: every visible text on every tab switches at once, no restart hint | Simulator | 06 | |
| 33 | Language changed for PollenInfo in the iOS Settings app: the app and the next notification use it | Simulator | 06 | |
