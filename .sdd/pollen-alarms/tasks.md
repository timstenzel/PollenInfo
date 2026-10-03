# Tasks: pollen-alarms

> Source: `.sdd/pollen-alarms/requirements.md`. Technical Annex verified against `5ab23db` on
> 2026-10-03. Resolved points:
>
> - **Deviation from the annex:** the app loads the pollen-type vocabulary from the existing
>   `GET /pollen/species` (a new `core/species` slice mirroring `core/station`) instead of adding an
>   app-side species enum.
> - **Annex followed over the codebase:** `composeApp/google-services.json` is committed; its entry
>   in the root `.gitignore` is removed (task 02).
> - **iOS rendering** cannot be checked by hand, because there is no `iosApp` wrapper. For iOS the
>   gate is that the iOS sources compile.
>
> **Prerequisites the product owner must supply:**
> - A Firebase project with `composeApp/google-services.json`, needed from task 02 on. The build
>   fails without it.
> - A service-account key for the server, supplied via `FCM_CREDENTIALS`, needed for real delivery
>   from task 04 on. Without it the server only logs.

## Task [01-alarms-tab-with-permission-gate]

The fourth bottom-bar tab becomes "Alarms" and checks notification permission first.

- **Without permission:** the user sees an explanation and one button. It is either "Allow
  notifications" (the system prompt) or "Open settings" (permanently denied, or notifications
  switched off below API 33). Returning from the settings re-checks on resume.
- **With permission:** the tab shows the "No alarms yet" empty state. This `Content` is a
  placeholder that task 02 replaces with the backend-loaded list. There is no backend contact yet,
  and the create button is visible but disabled until task 03.
- **Revoking permission later** brings the explanation and button back.

Works on Android and iOS.

### Implementation steps

- [x] Rename `Screen.Feature4` → `Screen.Alarms` and `FEATURE_4` → `ALARMS` (bell icon `Icons.Default.Notifications`, "Alarms"); point its `composable` at a new `AlarmsScreen`; update `TopLevelDestinationTest`
- [x] Add `POST_NOTIFICATIONS` to the Android manifest
- [x] Add a logic-free DataStore preference for "notification permission asked before", with a `commonTest` fake
- [x] Add `NotificationPermissionState` (`ENABLED`, `CAN_REQUEST`, `MUST_OPEN_SETTINGS`) and the `@Composable expect fun rememberNotificationPermissionController()`, with two actuals:
  - Android: `areNotificationsEnabled`, rationale plus the asked-before flag, and `ACTION_APP_NOTIFICATION_SETTINGS`
  - iOS: `UNUserNotificationCenter` and the settings URL
- [x] Add `AlarmsViewModel` / `AlarmsUiState`: `PermissionRequired(state)` and an empty `Content`, fed by `onPermissionState(...)`. The screen re-reads the state on `ON_RESUME`
- [x] Build `AlarmsScreen`: the permission explanation with one button per state, and the empty state
- [x] Update CLAUDE.md: the bottom-bar tab list, the permission gate, and the iOS `Info.plist` table if anything new is needed

### Acceptance criteria

- [x] `TopLevelDestinationTest` asserts the fourth tab is `ALARMS` → `Screen.Alarms` with content description "Alarms", and that the bar is shown on it
- [x] `AlarmsViewModelTest`: `CAN_REQUEST` and `MUST_OPEN_SETTINGS` each map to `PermissionRequired` with that state; `ENABLED` maps to `Content`; a later non-enabled state returns to `PermissionRequired`
- [x] Manual, Android 13+ emulator, fresh install: the tab shows "Allow notifications"; granting shows the empty state; after two denials the button reads "Open settings" and opens the app's notification settings; enabling notifications there and returning shows the empty state without leaving the tab
- [x] Manual: switching to Home and back keeps the Alarms tab's state; back from the Alarms tab returns to Home
- [ ] ~~Manual (TalkBack): the tab is announced as "Alarms", and the permission button announces its label~~ *(skipped: TalkBack speech cannot be captured from this environment — enabling it on the emulator only raised its own permission dialog and its utterances are not logged. Supporting evidence from the accessibility tree: the tab node has content-desc "Alarms", and the permission button exposes its text "Allow notifications" / "Open settings")*

### Quality gates

- [x] `./gradlew :composeApp:testDebugUnitTest :server:test` passes
- [x] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [x] `./gradlew :composeApp:assembleDebug` passes, and the Kotlin compiler output reports no warnings for files added or changed in this task
- [x] The merged release manifest contains `POST_NOTIFICATIONS`
- [x] No backtick test name contains a comma

## Task [02-device-registration-and-alarm-list]

The first time the Alarms tab reaches the enabled state, the app:
1. obtains its FCM token;
2. registers with the backend and stores the device id the server issues;
3. loads the device's alarm list from the backend.

The backend persists devices and alarms in SQLite, so they survive a server restart.

The list has the full `Loading` / `Content(isRefreshing)` / `Error` + Retry cycle with
pull-to-refresh. If the backend has forgotten the device, every repository call re-registers once
transparently. On iOS, where there is no token, the tab shows "Push notifications aren't available
on this device yet" without any network call.

Real users still see an empty list, because creating alarms comes in task 03. Alarms inserted on
the server side are listed.

### Implementation steps

- [ ] Remove `google-services.json` from the root `.gitignore` and commit `composeApp/google-services.json`; add `data/` to `.gitignore`
- [ ] Server database:
  - add Exposed and sqlite-jdbc to the version catalog
  - database setup reads `POLLENINFO_DB` (default `./data/polleninfo.db`), creates the directory, sets `PRAGMA foreign_keys=ON` on every connection, and creates the schema on start
  - add the `devices` and `alarms` tables
- [ ] Server: `DeviceStore` (`register`, `exists`) and `AlarmStore.list(deviceId)` with Exposed implementations; the server-side `Alarm` / `AlarmSchedule` domain and wire DTOs, including the polymorphic `schedule` (the default `type` discriminator)
- [ ] Server wiring:
  - move `MeasurementService` construction out of `configureRouting` into `Application.module()`, which also builds the database and stores
  - pass them into `configureRouting(...)` with test-friendly defaults
  - `PollenRoutesTest` and `RoutingTest` stay unchanged
- [ ] Server endpoints:
  - `POST /devices` → `201 {deviceId}`: 128 bits from `SecureRandom`, base64url without padding
  - `GET /devices/{id}/alarms`: `404` for an unknown device
- [ ] App: Firebase setup (google-services plugin, BoM, `firebase-messaging` in `androidMain`); a `PushTokenProvider` with an Android Firebase implementation and an iOS "unavailable" implementation, bound in `platformModule`
- [ ] App data layer:
  - a logic-free stored `deviceId` preference, with a fake
  - `AlarmApiService`, DTOs and mapper
  - `AlarmRepository(Impl)` with a lazy `ensureRegistered()`, a typed `PushUnavailable` failure, and a shared "re-register once and retry on unknown-device `404`" wrapper used by every call
- [ ] App: `AlarmsViewModel` loads on the first `ENABLED`, with Loading / Content / Error / Retry and pull-to-refresh with `isRefreshing`; list rows show the station name and a placeholder summary
- [ ] Update CLAUDE.md: server persistence and the database file, the device endpoints, the Firebase setup, and the Google Play services exception (push only; location stays Play-free)

### Acceptance criteria

- [ ] `ExposedStoresTest`: a registered device exists; an unknown device's alarm list is distinguishable from an empty list; alarms are listed in creation order; data survives closing and reopening a file-backed database
- [ ] `AlarmRoutesTest`: `POST /devices` returns `201` with a 22-character id; `GET /devices/{id}/alarms` returns `200 []` for a new device and `404` for an unknown one; an alarm of each `schedule` variant serialises with `"type": "daily"` / `"threshold"`
- [ ] `AlarmRepositoryImplTest` (real client via MockEngine):
  - it registers once and reuses the stored id
  - an unavailable token fails with `PushUnavailable` and makes no request
  - a `404` clears the id, re-registers once and retries
  - both `schedule` variants round-trip through DTO and mapper
- [ ] `AlarmsViewModelTest`:
  - `PermissionRequired` makes no repository call
  - `ENABLED` → `Loading` → `Content`
  - a failing load → `Error`, and retry → `Content`
  - a refresh keeps the previous alarms with `isRefreshing = true` (observed with a gated fake)
  - `PushUnavailable` → `Error` with the push-unavailable flag
- [ ] Manual, emulator against `./gradlew :server:run`:
  - granting permission creates one row in `devices`
  - after a server restart the tab loads without re-registering
  - revoking and then re-enabling notifications shows the same list

### Quality gates

- [ ] `./gradlew :composeApp:testDebugUnitTest :server:test` passes
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [ ] `./gradlew :composeApp:assembleDebug` passes, and the Kotlin compiler output reports no warnings for files added or changed in this task
- [ ] `git ls-files` lists `composeApp/google-services.json` and lists no service-account key, `*.db` file or `data/` content
- [ ] No backtick test name contains a comma

## Task [03-create-daily-report]

From the alarm list, the user opens a new editor screen with the bottom bar hidden and configures
a daily report:
- the station (preselected to the home station);
- pollen types, as chips loaded from `GET /pollen/species`;
- a minimum severity, from Any to Very high;
- the days;
- one time, set with a time picker;
- a "Times are Swiss time" hint.

When the user saves, the backend validates and stores the alarm. The user returns to the list,
which reloads and shows the new alarm with its one-line summary. An invalid form cannot be saved,
and a failed save keeps the input and shows an error.

### Implementation steps

- [ ] Server:
  - `AlarmValidation` (pure): non-empty species and days, a known station (case-insensitive), `HH:mm`, the daily schedule
  - `POST /devices/{id}/alarms` → `201` / `400 {error}` / `404`
  - `AlarmStore.create`
- [ ] App: a `core/species` slice (`Species`, `SpeciesRepository(Impl)`, `SpeciesApiService`, DTO, mapper) for `GET /pollen/species`, mirroring `core/station`, wired in Koin
- [ ] App: `AlarmFormState` (pure)
  - daily defaults: home station, all species, all days, Any, 08:00
  - `isValid`, `isDirty`, `toDraft()`
- [ ] App: `AlarmSummary` (pure): the daily summary text, with day ranges collapsed ("Every day", "Mon–Fri", "Sat, Sun"), formatted with `kotlinx-datetime`
- [ ] App editor:
  - the `Screen.AlarmEditor(alarmId: String?)` route
  - `AlarmEditorViewModel` / UiState, loading stations and species in parallel (`Error` with retry if either fails)
  - `AlarmEditorEvent.Done` on a buffered channel
  - editor UI: station dropdown, species `FilterChip`s, severity chips, day chips, a `TimePicker` dialog, the Swiss-time hint, and Save with a progress indicator and an inline `saveError`
- [ ] App: enable the list's create button; the list reloads when the tab resumes after the editor (`AlarmsViewModel.onResume()`); rows show the `AlarmSummary`
- [ ] Update CLAUDE.md: the editor route, the alarm endpoints so far, and `core/species`

### Acceptance criteria

- [ ] `AlarmValidationTest`: rejects empty species, empty days, an unknown station and a malformed time; accepts a valid daily report; matches the station abbreviation case-insensitively
- [ ] `AlarmRoutesTest`: a valid `POST` → `201`, and the alarm appears in the next `GET`; an invalid one → `400` with an `error` message; an unknown device → `404`
- [ ] App domain tests:
  - `SpeciesRepositoryImplTest` (MockEngine): maps the seven species; a server error gives `Failure`
  - `AlarmFormStateTest`: the defaults are as specified; `isValid` is false with no species and with no days; `isDirty` is false initially and false again after a change is undone; `toDraft()` carries every field
- [ ] `AlarmSummaryTest` pins the daily summary and each day-range form
- [ ] `AlarmEditorViewModelTest`: success emits `Done` exactly once; a failure keeps the form and sets `saveError`; `isSaving` is observable while a gated save is in flight; a failing species load → `Error`
- [ ] `AlarmsViewModelTest`: `onResume()` reloads the list. `TopLevelDestinationTest`: `Screen.AlarmEditor` is not a tab, so the bar is hidden
- [ ] Manual:
  - with Zürich as the home station, creating a daily report for Bern shows it in the list, and Home still shows Zürich
  - the alarm is still listed after a server restart
  - TalkBack announces every chip's name and selected state, the time field, and Save's enabled state

### Quality gates

- [ ] `./gradlew :composeApp:testDebugUnitTest :server:test` passes
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes, including the Material 3 experimental opt-ins
- [ ] `./gradlew :composeApp:assembleDebug` passes, and the Kotlin compiler output reports no warnings for files added or changed in this task
- [ ] No backtick test name contains a comma

## Task [04-deliver-daily-reports]

The backend checks alarms every minute and delivers due daily reports as push notifications
through FCM, on the "Daily reports" channel, even when the app is not running. Tapping a
notification opens the app.

Reports follow the agreed rules:
- the exact minute and day, in Europe/Zurich;
- the severity filter;
- a worst-first body;
- "no current reading" text for Any reports on stale data;
- filtered reports skipped on stale data;
- no catch-up after downtime.

Only stations with enabled alarms are fetched, through the existing 30-minute cache. Without
`FCM_CREDENTIALS` the server logs the notification instead.

### Implementation steps

- [ ] Server: `ALARM_ZONE`; the 3-hour freshness constant, whose KDoc references the app's `STALE_AFTER`; `PushMessage` with a channel and data (`stationAbbr`, `alarmId`)
- [ ] Server: `AlarmRules.evaluateDaily` (pure) and the message text, including the stale-time forms `HH:mm`, `HH:mm yesterday` and `d MMMM`
- [ ] Server push senders:
  - `PushSender` / `PushResult`
  - `FcmPushSender`: Ktor CIO client with a timeout, HTTP v1, an injected access-token lambda
  - `LoggingPushSender`
  - credentials come from `FCM_CREDENTIALS` via `google-auth-library`, and the project id from the key file
- [ ] Server scheduler:
  - `AlarmStore.enabledWithDeliverableDevice()`
  - `AlarmScheduler.tick()`, sharing the routes' `MeasurementService`, with each station isolated in `supervisorScope`
  - the minute loop launched in `Application.module()`: exception-safe, cancelled on `ApplicationStopped`
- [ ] App: create the `daily_report` ("Daily reports") and `threshold_alert` ("Threshold alerts") channels in `PollenInfoApplication`; register `PollenFirebaseMessagingService` in the manifest, posting foreground messages on their channel
- [ ] Update CLAUDE.md: the architecture diagram and the "Fetching is on demand" section (the scheduler for alarm stations), push delivery, and `FCM_CREDENTIALS`

### Acceptance criteria

- [ ] `AlarmRulesTest` (daily timing): due at the exact minute and not one minute either side; an excluded day → null; a disabled alarm → null; an 08:00 alarm fires at 08:00 local time on both DST changeover days
- [ ] `AlarmRulesTest` (daily content and staleness):
  - filter met → sent; filter not met → null; Any → always sent
  - a stale reading → the "no current reading" text for Any (pinned in each of the three latest-time forms), and null when filtered
  - a failed reading → "unavailable" for Any
  - the body is worst-first; all-NONE gives the "No pollen of your selected types." body; the title is "Pollen in <station>"
- [ ] `FcmPushSenderTest` (MockEngine):
  - checks the request URL, the bearer header and the body (token, notification, `channel_id`, data)
  - `200` → `Sent`; `404 UNREGISTERED` and `400 INVALID_ARGUMENT` → `Unregistered`; `500` and a timeout → `Failed`
- [ ] `AlarmSchedulerTest` (`MutableClock`, in-memory stores, `FakePollenService`, `FakePushSender`):
  - a daily report is sent once at its minute and not at the next tick
  - a station without alarms is never requested
  - many ticks within 30 minutes cause one upstream fetch per station
  - nothing is sent after the clock jumps past the time
  - one station's upstream failure doesn't stop another station's report
- [ ] Manual: with credentials configured, a daily report set for the next minute arrives on the emulator with the app killed, on the "Daily reports" channel; tapping it opens the app
- [ ] Manual: without `FCM_CREDENTIALS`, the server starts with a warning and logs the report at its minute

### Quality gates

- [ ] `./gradlew :composeApp:testDebugUnitTest :server:test` passes
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [ ] `./gradlew :composeApp:assembleDebug` passes, and the Kotlin compiler output reports no warnings for files added or changed in this task
- [ ] No test sleeps or contacts Google/FCM or MeteoSwiss
- [ ] No backtick test name contains a comma

## Task [05-threshold-alerts]

Users can create threshold alerts. In the editor, the type toggle switches to Threshold: severity
runs from Low to Very high (default High), and there is an active window (default 07:00–21:00, end
after start). The backend validates and stores the alert, and the list summarises it.

The backend notifies on the "Threshold alerts" channel when a current reading shows selected
species at or above the threshold inside the window:
- species qualifying on the same reading are batched into one notification;
- at most once per alarm per species per Zurich day;
- never on readings older than 3 hours or from a previous day.

The once-per-day record is persisted, so a restart doesn't resend.

### Implementation steps

- [ ] Server: extend `AlarmValidation` (`until > from`, no `NONE` for threshold) and storage for threshold schedules
- [ ] Server: the `notification_log` table (`ON DELETE CASCADE` from `alarms`) and the `NotificationLog` store (`notifiedSpecies`, `record`, `pruneBefore`)
- [ ] Server: `AlarmRules.evaluateThreshold` (pure), returning the message and the species to record
- [ ] Server: the scheduler's threshold path evaluates active alarms every tick, records on `Sent`, doesn't record on `Failed`, and prunes the log when the Zurich date changes
- [ ] App editor:
  - a `SingleChoiceSegmentedButtonRow` type toggle on new alarms
  - `AlarmFormState` type switching resets the severity and time defaults
  - severity options per type
  - start and end time pickers
- [ ] App: the threshold `AlarmSummary` ("Threshold alert · Birch, Grasses ≥ High · 07:00–21:00")
- [ ] Update CLAUDE.md: a summary of the alarm rules for both types

### Acceptance criteria

- [ ] `AlarmValidationTest`: `until == from` is rejected and `until = from + 1 min` accepted; `NONE` is rejected for threshold and accepted for daily
- [ ] `AlarmRulesTest` (threshold window and severity): `from` is inclusive and `until` exclusive, checked one minute either side; severity at the threshold fires, one band below doesn't; species without a reading are ignored; a disabled alarm doesn't fire
- [ ] `AlarmRulesTest` (threshold dedup and freshness): `notifiedToday` excludes species; two qualifying species give one message naming both; a reading 2:59 old fires and one 3:00 old doesn't; a reading from yesterday's date doesn't fire
- [ ] `AlarmSchedulerTest`:
  - fires once per species per day, and again the next day
  - a second species qualifying later gets its own message
  - a `Failed` send is retried on the next tick
  - a species already over the threshold before the window opens fires at the first in-window tick
  - an upstream failure with only a stale reading sends nothing
  - a scheduler recreated on the same database doesn't resend that day
  - the log is pruned on date change
- [ ] `ExposedStoresTest`: `record` / `notifiedSpecies` / `pruneBefore`; deleting an alarm row removes its log rows, which proves foreign keys are enforced
- [ ] `AlarmFormStateTest`: switching to threshold sets High and 07:00–21:00, and back to daily sets Any and 08:00; `isValid` is false when end ≤ start; the threshold severity options exclude Any. `AlarmSummaryTest` pins the threshold summary
- [ ] Manual:
  - a threshold alert at Low for a station that is currently measuring delivers one notification on the "Threshold alerts" channel within a minute, and no second one on later ticks
  - the app's notification settings show "Daily reports" and "Threshold alerts" as separate, independently switchable categories
  - TalkBack announces the type toggle's selected segment and both window time fields

### Quality gates

- [ ] `./gradlew :composeApp:testDebugUnitTest :server:test` passes
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [ ] `./gradlew :composeApp:assembleDebug` passes, and the Kotlin compiler output reports no warnings for files added or changed in this task
- [ ] No test sleeps or contacts Google/FCM or MeteoSwiss
- [ ] No backtick test name contains a comma

## Task [06-edit-pause-and-delete-alarms]

Users manage their existing alarms:
- **Edit:** tapping a row opens the editor prefilled, with the type locked. Saving updates the
  alarm on the backend.
- **Pause:** the list's on/off switch pauses or resumes an alarm immediately. The change shows at
  once and reverts with a message if it fails. Paused alarms send nothing.
- **Delete:** the editor offers Delete with a confirmation.
- **Discard:** leaving the editor with unsaved changes asks "Discard changes?".

A device can never read, change or delete another device's alarm. Editing keeps the same-day
notification record.

### Implementation steps

- [ ] Server: `PUT` and `DELETE /devices/{id}/alarms/{alarmId}` with ownership checks (`404`) and the validation reused; `AlarmStore.update` / `delete`; an update doesn't reset the notification log
- [ ] App: repository `alarm(id)`, `update` and `delete`, through the shared re-register wrapper; `AlarmFormState` built from an existing alarm, with `typeLocked`
- [ ] App: `AlarmEditorViewModel` edit mode (Loading → Editing / Error); delete confirmation; the discard dialog only when dirty; `Done` after delete
- [ ] App: tapping a row opens `Screen.AlarmEditor(id)`; each row has a `Switch` that toggles optimistically via a full `PUT`, with a one-shot error event that reverts it
- [ ] Update CLAUDE.md: the full alarm API table

### Acceptance criteria

- [ ] `AlarmRoutesTest`: `PUT` returns the updated alarm; `DELETE` → `204` and the alarm is gone from `GET`; another device's alarm id → `404` on `PUT` and on `DELETE`; an invalid `PUT` → `400`
- [ ] `ExposedStoresTest`: updating or deleting another device's alarm returns not-found
- [ ] `AlarmSchedulerTest`: a disabled alarm sends nothing; updating an alarm after it notified about Birch today doesn't resend Birch, while a newly added qualifying species is sent
- [ ] App repository and editor tests:
  - `AlarmRepositoryImplTest`: update and delete re-register once and retry on an unknown-device `404`
  - `AlarmEditorViewModelTest`: edit mode loads the alarm with the type locked; back without changes emits `Done` with no dialog, and with changes shows the discard dialog; delete requires confirmation and then emits `Done`; a failed edit load → `Error`
- [ ] `AlarmsViewModelTest`: toggling updates the row immediately; a failing toggle reverts it and emits exactly one error event
- [ ] Manual: TalkBack announces the row switch's state, and the delete action and its confirmation

### Quality gates

- [ ] `./gradlew :composeApp:testDebugUnitTest :server:test` passes
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [ ] `./gradlew :composeApp:assembleDebug` passes, and the Kotlin compiler output reports no warnings for files added or changed in this task
- [ ] No backtick test name contains a comma

## Task [07-ten-alarm-limit]

A device can hold at most ten alarms. The backend answers `409` to the eleventh, and the app
disables the create button with a hint explaining the limit. If the limit is hit anyway (another
install sharing the id, or a race), the editor shows it as its save error.

### Implementation steps

- [ ] Server: a limit check inside `AlarmStore.create`'s transaction (`LimitReached`) → `409`
- [ ] App: a typed `AlarmLimitReached` failure from the repository
- [ ] App: `Content.limitReached` disables the create button and shows the hint; the editor maps `AlarmLimitReached` to its `saveError`

### Acceptance criteria

- [ ] `ExposedStoresTest`: the 10th alarm is created and the 11th returns `LimitReached`
- [ ] `AlarmRoutesTest`: a `POST` at 10 alarms → `409`
- [ ] `AlarmRepositoryImplTest`: `409` maps to `AlarmLimitReached`
- [ ] `AlarmsViewModelTest`: ten alarms → `limitReached = true`, nine → `false`. `AlarmEditorViewModelTest`: `AlarmLimitReached` on save sets a limit `saveError` and keeps the form
- [ ] Manual: with ten alarms the create button is disabled, the hint is shown, and TalkBack announces it as disabled

### Quality gates

- [ ] `./gradlew :composeApp:testDebugUnitTest :server:test` passes
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [ ] `./gradlew :composeApp:assembleDebug` passes, and the Kotlin compiler output reports no warnings for files added or changed in this task
- [ ] No backtick test name contains a comma

## Task [08-push-token-lifecycle]

Push delivery keeps working as tokens change:
- When FCM rotates the device's token, the app sends the new one to the backend.
- When FCM reports a token as unregistered (for example, the app was uninstalled), the backend
  clears it and stops processing that device's alarms.

This task closes with a final documentation pass over the whole feature.

### Implementation steps

- [ ] Server: `PUT /devices/{id}/token` → `204` / `404`; `DeviceStore.updateToken` / `clearToken`
- [ ] Server: the scheduler clears the token on `PushResult.Unregistered`
- [ ] App: `PollenFirebaseMessagingService.onNewToken` sends the token when a device id is stored and otherwise does nothing; add a repository `updateToken(token)`
- [ ] Final CLAUDE.md pass: everything in the requirements' documentation list is present and consistent (architecture, Firebase, persistence, endpoints, the Alarms tab, the editor route, the `feature/alarms` slice entry)

### Acceptance criteria

- [ ] `AlarmRoutesTest`: `PUT /devices/{id}/token` → `204` for a known device and `404` for an unknown one
- [ ] `ExposedStoresTest`: an updated token is returned by `enabledWithDeliverableDevice()`; a cleared token removes that device's alarms from it
- [ ] `AlarmSchedulerTest`: an `Unregistered` result clears the token, and the device's alarms are skipped on the next tick
- [ ] `AlarmRepositoryImplTest`: `updateToken` with a stored id sends `PUT /devices/{id}/token`, and without one makes no request
- [ ] Manual:
  1. clear the app's data or force a token refresh (`FirebaseMessaging.deleteToken()` via a debug path) and reopen the app
  2. `devices.fcm_token` shows the new token
  3. a daily report still arrives

### Quality gates

- [ ] `./gradlew :composeApp:testDebugUnitTest :server:test` passes
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [ ] `./gradlew :composeApp:assembleDebug` passes, and the Kotlin compiler output reports no warnings for files added or changed in this task
- [ ] No backtick test name contains a comma
