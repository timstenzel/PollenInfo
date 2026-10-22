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
