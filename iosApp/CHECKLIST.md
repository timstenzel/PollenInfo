# iPhone manual checklist

The platform parts of the iPhone app that no automated test reaches. Run it on the iOS Simulator
and on a real iPhone (installed with the local settings file), and record each result with the date:
`pass`, `fail` (with the issue raised) or `n/a` (with the reason).

| # | Item | Simulator | iPhone |
| --- | --- | --- | --- |
| 1 | **Debug launch:** a fresh install of the Debug build opens onboarding; the station list loads. | pass 2026-10-22 (iPhone 17, iOS 27.0) | pending (device installs start with the local settings file) |
| 2 | **Debug backend:** with `./gradlew :server:run` on the Mac, Home shows a reading and the server log shows `GET /pollen/stations/<abbr>/measurements`. | pass 2026-10-22 (station seeded in the preferences file — no tapping tool on this Mac; picking in onboarding still to check by hand) | pending (device installs start with the local settings file) |
| 3 | **Release launch and backend:** the Release build shows a current reading while the local server receives no request (production backend). | pass 2026-10-22 (station seeded as in 2) | pending (device installs start with the local settings file) |
| 4 | **Side by side:** both builds installed at once, named "PollenInfo (debug)" and "PollenInfo" on the home screen. | pass 2026-10-22 | pending (device installs start with the local settings file) |
| 5 | **Local settings, identifiers:** with `Local.xcconfig` setting a throwaway `BUNDLE_ID_BASE` and a team, both builds install and launch on the iPhone, and Xcode's device registration (Settings → Accounts → Manage Certificates / the team's provisioning profiles) lists only the throwaway identifiers. | n/a (device only) | pending (no iPhone connected and no signing identity on this Mac, 2026-10-22) |
| 6 | **LAN backend:** with `API_BASE_URL` in `Local.xcconfig` set to the Mac's LAN address and `./gradlew :server:run`, the Debug app on the iPhone shows a reading and the server log shows the request. Note whether iOS shows a Local Network prompt. | n/a (the Simulator shares the Mac's network) | pending (no iPhone connected, 2026-10-22) |
| 7 | **Release stays on production:** with an `API_BASE_URL` in `Local.xcconfig`, the built Release `Info.plist` has `API_BASE_URL = https://api.polleninfo.ch`. | pass 2026-10-22 (Simulator build with a test `Local.xcconfig`) | pending (no iPhone connected, 2026-10-22) |
