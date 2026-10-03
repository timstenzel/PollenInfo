# Pollen Alarms (Push Notifications)

## Problem Statement

People with pollen allergies currently have to open PollenInfo to learn how bad the air is. The app
shows the current situation for their home station and for every station in Switzerland, but only
when they look. Two common needs go unmet:

- **A routine briefing.** "Tell me every weekday morning at eight what the pollen situation is, so
  I can decide whether to take my medication before leaving the house."
- **A warning when it matters.** "Let me know as soon as birch reaches a high level where I work,
  so I am not caught out."

Both require the app to reach the user unprompted, at a chosen time or when a chosen condition
becomes true. Today there is no way to do that, and the backend only fetches pollen data when
somebody is looking at a station, so it cannot notice a condition on its own.

## Solution

A new **Alarms** tab, in the fourth position of the bottom navigation bar, where users create and
manage pollen alarms that arrive as push notifications.

- **Permission first.** The tab checks whether the app may post notifications. If not, it explains
  that notifications are disabled and offers one button: either "Allow notifications", which shows
  the system prompt, or "Open settings" when the system will no longer prompt (permission
  permanently denied, or notifications switched off in system settings). Returning from the
  settings updates the screen without any further action.
- **Alarm list.** Once notifications are allowed, the tab lists the user's alarms. Each row names
  the station, summarises the alarm in one line, and has an on/off switch that pauses it without
  deleting it. Tapping a row opens it for editing. A button leads to a new screen for creating an
  alarm. A device can hold at most ten alarms.
- **Alarm editor.** An alarm watches **one station** (preselected to the home station) and is one
  of two types:
  - **Daily report.** At one chosen time on the chosen days, it sends a summary of the selected
    pollen types at that station. If a minimum severity is chosen, the report is sent only when at
    least one selected type has reached it. With "Any", it is always sent.
  - **Threshold alert.** During a chosen time window on the chosen days, it notifies as soon as a
    selected pollen type is at or above the chosen severity, at most once per pollen type per day.

  The user picks pollen types, the severity, the days and the time(s) with chips and a time
  picker, then saves. The configuration is stored on the backend. Existing alarms can be edited
  or deleted.
- **Backend evaluation.** The backend keeps the alarms and checks them every minute. It fetches
  current readings only for stations that at least one active alarm watches, decides which alarms
  are due, and sends push notifications. It never alerts on outdated data, and it is honest in a
  daily report when no current reading exists.
- **All alarm times are Swiss time** (Europe/Zurich), and the editor says so.

In this release notifications are delivered on **Android** only. On iOS the tab, the permission
handling and the screens all work, but the alarm list reports that push notifications are not yet
available on the device.

## User Stories

### Finding the feature

1. As a user, I want an "Alarms" tab in the bottom navigation bar, so that I can find pollen
   notifications without searching through menus.
2. As a screen-reader user, I want the Alarms tab announced as "Alarms", so that I can tell it apart
   from the other icon-only tabs.
3. As a user, I want the Alarms tab to keep its state when I switch to another tab and back, so
   that I do not lose my place.
4. As a user, I want the back gesture from the Alarms tab to return to Home, so that it behaves like
   every other tab.

### Notification permission

5. As a user who has not yet allowed notifications, I want the Alarms tab to tell me that
   notifications are disabled, so that I understand why I cannot set alarms yet.
6. As a user who has not yet been asked, I want a button that shows the system permission prompt,
   so that I can allow notifications in one tap.
7. As a user who has denied the permission so often that the system no longer prompts, I want the
   button to open the app's notification settings instead, so that I still have a way to enable
   notifications.
8. As a user on an older Android version without a runtime prompt, who has switched notifications
   off in system settings, I want the same "Open settings" route, so that I can switch them back on.
9. As a user returning from system settings after enabling notifications, I want the Alarms tab to
   show my alarms straight away, so that I do not have to leave and re-enter the tab.
10. As a user who later revokes the notification permission, I want the Alarms tab to show the
    permission screen again, so that I understand why alarms are no longer arriving.
11. As a user who revokes the permission, I want my alarms to remain stored, so that they work
    again as soon as I re-enable notifications.
12. As a user who never opens the Alarms tab, I want the app not to register me with the backend
    for push, so that nothing about my device is stored unless I use the feature.

### Alarm list

13. As a user with notifications enabled, I want to see a list of all my alarms, so that I know what
    I will be notified about.
14. As a user, I want each alarm row to show its station, so that I can tell alarms for different
    places apart.
15. As a user, I want each alarm row to summarise its type, pollen types, severity, days and time in
    one line, so that I can review my alarms without opening each one.
16. As a user, I want an on/off switch on each alarm, so that I can pause an alarm, for example on
    holiday, without deleting and recreating it.
17. As a user, I want switching an alarm off or on to take effect on the backend immediately, so that
    a paused alarm really stops notifying me.
18. As a user, I want to be told if switching an alarm failed, with the switch back in its previous
    position, so that the screen never claims a state the backend does not hold.
19. As a user, I want to tap an alarm to open it for editing, so that I can change it.
20. As a user without alarms, I want an empty state explaining that I have none yet, with a
    prominent button to create one, so that I know how to start.
21. As a user with alarms, I want a button to create a new alarm, so that I can add another.
22. As a user with ten alarms, I want the create button disabled with an explanation of the limit,
    so that I am not surprised by a failed save.
23. As a user, I want to pull down to refresh the list, so that I can make sure it matches the
    backend.
24. As a user, I want a full-screen error with a Retry button if my alarms cannot be loaded, so that
    I can recover from a network problem.
25. As a user whose alarm list is refreshing, I want the current alarms to stay visible, so that the
    screen does not flash empty.
26. As a user returning to the list after saving or deleting an alarm, I want the list to reflect the
    change, so that I can trust what I see.
27. As an iOS user, I want a clear message that push notifications are not yet available on my
    device, so that I do not create alarms that would never arrive.

### Creating and editing an alarm

28. As a user, I want to choose between "Daily report" and "Threshold alert" when creating an alarm,
    so that I can express either a routine briefing or a warning.
29. As a user editing an existing alarm, I want its type to be fixed, so that the meaning of its
    time setting never changes underneath me. (To change type, I create a new alarm.)
30. As a user, I want to choose the station an alarm watches, so that I can be notified about a place
    other than my home station, for example where I work.
31. As a user creating an alarm, I want the station preselected to my home station, so that the
    common case needs no extra step.
32. As a user, I want to select one or more pollen types with chips, so that I am only notified
    about the plants I react to.
33. As a user, I want all pollen types selected by default, so that a new alarm covers everything
    until I narrow it down.
34. As a user, I want to be prevented from saving an alarm with no pollen types, so that I do not
    create an alarm that can never fire.
35. As a user creating a daily report, I want to choose a minimum severity from Any, Low, Moderate,
    High and Very high, so that I can get the report only on days when it is relevant.
36. As a user creating a daily report, I want "Any" to be the default, so that by default I get the
    report every chosen day.
37. As a user creating a threshold alert, I want to choose the severity that triggers it from Low,
    Moderate, High and Very high, with High as the default, so that I am warned at the level that
    matters to me.
38. As a user, I want to select the days of the week with chips, so that an alarm can run only on
    workdays, for example.
39. As a user, I want all seven days selected by default, and to be prevented from saving with none,
    so that a new alarm works every day and never silently does nothing.
40. As a user creating a daily report, I want to pick the time it is sent with a time picker,
    defaulting to 08:00, so that it arrives when I need it.
41. As a user creating a threshold alert, I want to pick the start and end of an active window,
    defaulting to 07:00–21:00, so that I am not woken at night.
42. As a user, I want to be prevented from saving a window whose end is not after its start, so that
    I do not create a window that never opens.
43. As a user, I want the editor to tell me that all times are Swiss time, so that I am not confused
    when abroad.
44. As a user, I want the Save button enabled only when the alarm is complete and valid, so that I
    cannot submit something the backend will reject.
45. As a user, I want to see that saving is in progress, so that I do not tap Save twice.
46. As a user, I want to return to the alarm list automatically after a successful save, so that I
    can see my new or changed alarm.
47. As a user whose save failed, I want to stay in the editor with my input intact and see an error
    message, so that I can retry without re-entering everything.
48. As a user leaving the editor with unsaved changes, I want to be asked whether to discard them,
    so that I do not lose work by accident.
49. As a user leaving the editor without changes, I want to go straight back, so that I am not asked
    pointless questions.
50. As a user editing an alarm, I want a delete action with a confirmation, so that I can remove an
    alarm I no longer need without deleting one by mistake.
51. As a user, I want to return to the list after deleting an alarm, so that I can see it is gone.
52. As a user editing an alarm, I want the editor prefilled with its current settings, so that I
    only change what I want to change.
53. As a user, I want the editor without the bottom navigation bar, so that it reads as a focused
    task with a clear way back.
54. As a screen-reader user, I want every chip, the switch, the time fields and the buttons to
    announce their name and state, so that I can configure alarms without seeing the screen.

### Receiving notifications

55. As a user with a daily report, I want a notification at the chosen time on the chosen days,
    titled with the station, so that I know where the report is for.
56. As a user, I want the daily report to list my selected pollen types that have a reading, worst
    first, with their severity, so that the most important information comes first.
57. As a user whose selected pollen types are all at "none", I want the daily report to say so
    plainly, so that a quiet day is clearly reported as quiet.
58. As a user with a filtered daily report, I want no notification on days when none of my selected
    types reaches the minimum, so that I am only disturbed when it matters.
59. As a user with an "Any" daily report, I want the report to say clearly when no current reading
    is available, and how old the latest one is, so that I never mistake old data for today's.
60. As a user with a filtered daily report, I want it skipped when no current reading is available,
    so that I am not alerted or reassured on the basis of old data.
61. As a user with a threshold alert, I want a notification soon after a new reading shows a
    selected pollen type at or above my severity, within my window, so that I can react in time.
62. As a user, I want several pollen types that qualify on the same reading combined into one
    notification, so that I am not flooded.
63. As a user, I want at most one threshold notification per pollen type per alarm per day, so that
    a level hovering around the threshold does not ping me repeatedly.
64. As a user, I want a pollen type that only qualifies later in the day to still notify me, so that
    I am warned about each plant I asked about.
65. As a user whose threshold was already exceeded before my window opened, I want to be notified at
    the first reading inside the window, so that I do not miss it.
66. As a user, I want threshold alerts never to fire on outdated data (older than three hours or from
    a previous day), so that an outage at the data source never produces a false "high today" alert.
67. As a user, I want no notifications from paused alarms, so that the on/off switch means what it
    says.
68. As a user, I want daily reports and threshold alerts in separate notification categories in the
    system settings, so that I can mute one kind without losing the other.
69. As a user, I want tapping a notification to open the app, so that I can see the full picture.
70. As a user, I want notifications to arrive even when the app is not running, so that alarms work
    without keeping the app open.
71. As a user, I want edits to an alarm on the same day not to repeat a threshold notification I
    already got today, so that adjusting an alarm does not spam me.
72. As a user, I want a pollen type I add to a threshold alert to notify me that day if it already
    qualifies, so that the change takes effect immediately.
73. As a user, I want no catch-up burst of old notifications after the backend has been down, so
    that I never get a "08:00 report" at 08:40.

### Reliability and privacy

74. As a user, I want my alarms to survive backend restarts, so that a saved alarm stays saved.
75. As a user, I want my device to keep receiving notifications when its push token changes, so that
    alarms do not silently stop working.
76. As a user, I want the app to recover by re-registering if the backend no longer knows my device,
    so that the Alarms tab never gets stuck in an error.
77. As a user, I want no account or personal data to be required, so that I can use alarms
    anonymously.
78. As a user, I want my location never to be sent to the backend for alarms, so that only the
    station I chose is shared.

### Operations

79. As an operator, I want the backend to fetch pollen data only for stations that active alarms
    watch, so that unused stations cost no upstream requests.
80. As an operator, I want alarm polling to share the existing 30-minute reading cache, so that
    alarms and app users together cost at most one upstream request per station per period.
81. As an operator, I want the backend to drop push tokens that the push service reports as no
    longer registered, so that uninstalled apps stop being processed.
82. As a developer without push credentials, I want the backend to start anyway and log the
    notifications it would send, so that I can run and develop it locally.
83. As an operator, I want the backend to reject invalid alarm configurations and enforce the
    ten-alarm limit, so that a misbehaving client cannot store nonsense or fill the database.
84. As an operator, I want a device to be unable to read or change another device's alarms by
    alarm id, so that alarms stay scoped to their owner.

## User Acceptance Tests

### Navigation

1. Given the app is past onboarding, when the user looks at the bottom navigation bar, then the
   fourth tab shows a bell icon and a screen reader announces it as "Alarms".
2. Given the user is on the Alarms tab, when they press back, then they return to Home.
3. Given the user is on the alarm list, when they switch to Home and back to Alarms, then the list
   is shown as before.

### Permission

4. Given a fresh install on Android 13 or later, when the user opens the Alarms tab, then a text
   explains that notifications are disabled and an "Allow notifications" button is shown.
5. Given the permission screen with "Allow notifications", when the user taps it and grants the
   permission, then the alarm list (or its empty state) appears.
6. Given the user has denied the notification permission twice, when they open the Alarms tab, then
   the button reads "Open settings", and tapping it opens the app's notification settings.
7. Given the app's notification settings are open from the Alarms tab, when the user enables
   notifications and returns to the app, then the alarm list appears without further action.
8. Given a device below Android 13 with the app's notifications switched off in system settings,
   when the user opens the Alarms tab, then the "Open settings" button is shown.
9. Given the user has alarms and then disables notifications for the app in system settings, when
   they return to the Alarms tab, then the permission screen is shown; when they re-enable
   notifications, then the same alarms are listed again.

### Alarm list

10. Given notifications are enabled and the user has no alarms, when the Alarms tab opens, then an
    empty state says there are no alarms yet and offers a button to create one.
11. Given the user has a daily report for Zürich at 08:00 Monday to Friday, when the list is shown,
    then its row names Zürich and summarises it as a daily report at 08:00 on Mon–Fri.
12. Given the user has a threshold alert for Birch and Grasses at High, 07:00–21:00, when the list is
    shown, then its row summarises the types, "≥ High" and the window.
13. Given an alarm is on, when the user turns its switch off, then it stays off after leaving and
    re-entering the tab, and the alarm sends no notifications while off.
14. Given the backend is unreachable, when the user toggles an alarm's switch, then an error is shown
    and the switch returns to its previous position.
15. Given the backend is unreachable, when the user opens the Alarms tab with notifications enabled,
    then a full-screen error with Retry is shown; when the backend is reachable again and the user
    taps Retry, then the list appears.
16. Given the user has ten alarms, when they view the list, then the create button is disabled and a
    hint explains the limit of ten.
17. Given the alarm list is shown, when the user pulls down, then a refresh indicator appears and the
    existing alarms stay visible until the refresh completes.
18. Given the app runs on iOS with notifications allowed, when the alarm list would load, then a
    message says push notifications are not yet available on this device.

### Editor

19. Given the user taps the create button, when the editor opens, then the type is "Daily report",
    the station is the home station, all pollen types and all days are selected, severity is "Any",
    the time is 08:00, a hint says times are Swiss time, and the bottom navigation bar is hidden.
20. Given a new alarm in the editor, when the user switches the type to "Threshold alert", then the
    severity choices are Low to Very high with High selected, and a 07:00–21:00 window replaces the
    single time.
21. Given the editor, when the user deselects every pollen type, then Save is disabled; when they
    select one again, then Save is enabled.
22. Given the editor, when the user deselects every day, then Save is disabled.
23. Given a threshold alert in the editor, when the user sets the window end to the same time as or
    before the start, then Save is disabled.
24. Given a valid new alarm, when the user taps Save, then a progress indicator is shown, the user is
    returned to the list, and the new alarm appears in it.
25. Given the backend is unreachable, when the user taps Save, then they stay in the editor, an error
    message is shown, and all their choices are kept.
26. Given the user taps an existing alarm, when the editor opens, then all fields show that alarm's
    settings and the type cannot be changed.
27. Given the user has changed a field in the editor, when they press back, then a "Discard
    changes?" dialog appears; choosing to discard returns to the list without saving, choosing to
    keep editing stays in the editor.
28. Given the user has not changed anything in the editor, when they press back, then they return to
    the list without a dialog.
29. Given an existing alarm in the editor, when the user taps Delete, then a confirmation dialog
    appears; confirming returns to the list without that alarm, cancelling keeps the editor open.
30. Given the user chooses Bern as station in a new alarm while their home station is Zürich, when
    they save, then the list shows the alarm for Bern and Home still shows Zürich.

### Notifications

31. Given an enabled "Any" daily report for Zürich at the next whole minute today, when that minute
    arrives in Swiss time, then a notification titled "Pollen in Zürich" arrives listing the selected
    types with a reading, worst severity first.
32. Given an enabled daily report whose days exclude today, when its time arrives, then no
    notification is sent.
33. Given a daily report with minimum severity High while all selected types are Low or below, when
    its time arrives, then no notification is sent.
34. Given an "Any" daily report and the station's latest reading is more than three hours old, when
    its time arrives, then the notification says there is no current reading and when the latest one
    is from.
35. Given a daily report with a minimum severity and the station's latest reading is more than three
    hours old, when its time arrives, then no notification is sent.
36. Given an enabled threshold alert for Birch at High within its active window, when a current
    reading shows Birch at High or above, then a notification for that station names Birch and its
    severity.
37. Given the threshold alert in test 36 has already notified about Birch today, when later readings
    show Birch at High again, then no further Birch notification is sent that day.
38. Given a threshold alert for Birch and Grasses, when one reading shows both at or above the
    threshold, then exactly one notification naming both is sent.
39. Given a threshold alert that already notified about Birch today, when Grasses later reaches the
    threshold, then a separate notification for Grasses is sent.
40. Given a threshold alert with window 07:00–21:00, when a qualifying reading arrives at 06:30, then
    no notification is sent; when the first reading inside the window still qualifies, then a
    notification is sent.
41. Given a threshold alert, when the data source is unavailable and only a reading older than three
    hours exists, then no threshold notification is sent.
42. Given a disabled alarm whose conditions are met, when its time or reading arrives, then no
    notification is sent.
43. Given notifications have arrived, when the user opens the app's notification settings, then
    "Daily reports" and "Threshold alerts" appear as separate categories that can be switched off
    independently.
44. Given the app is not running, when an alarm fires, then the notification is still shown, and
    tapping it opens the app.
45. Given the backend is restarted after alarms were created and after a threshold notification was
    sent today, when it is running again, then all alarms are still listed and the same threshold
    notification is not sent a second time today.
46. Given the backend was stopped over a daily report's time, when it starts again after that time,
    then that day's missed report is not sent.

### Operations

47. Given the backend is started without push credentials configured, when an alarm fires, then the
    backend keeps running and logs the notification it would have sent.
48. Given only alarms for Zürich exist, when the backend runs for an hour, then it fetches pollen data
    for Zürich only, and at most once per 30 minutes.

## Definition of Done

- All user acceptance tests pass on an Android device or emulator with Google Play services, against
  a backend configured with real push credentials.
- The iOS sources compile, and the Alarms tab, the permission screen and the editor render on iOS.
- All automated tests for the app and the backend pass, including the new ones listed in the
  Technical Annex.
- No regression in onboarding, Home, All stations or the bottom navigation bar.
- Alarms, device registrations and the per-day notification record survive a backend restart.
- The backend starts and runs without push credentials, falling back to logging.
- Push credentials for the backend are not committed to the repository.
- Every interactive element on the new screens has an accessible name and state.
- Project documentation describes the new tab, the alarm rules, the backend's scheduled polling
  alongside on-demand fetching, backend persistence, the new endpoints, and the Firebase setup
  (including that push is the one place Google Play services is required).

## Out of Scope

- Push delivery on iOS (Apple Push Notification service) and the iOS app wrapper project. iOS gets
  the screens and permission handling only.
- Push on Android devices without Google Play services.
- Opening a specific station or screen when a notification is tapped. The station is included in the
  notification data so this can be added later without backend changes.
- An in-app history or inbox of received notifications.
- User accounts, login, and restoring alarms after reinstalling the app or on another device.
- Per-alarm or device time zones. All alarm times are Swiss time.
- More than one time per daily report. A second time needs a second alarm.
- Threshold windows that cross midnight.
- A second threshold notification on the same day when a pollen type climbs further (e.g. from High
  to Very high). A second alarm at the higher level covers this.
- Catching up on notifications missed while the backend was down.
- Changing an alarm's type after creation.
- Swipe-to-delete in the list.
- Changing the home station (planned settings feature).
- Localisation of notification texts. They are English, like the app.
- Hourly-specific severity bands. Alarms use the same thresholds as the rest of the app, with the
  documented "skews high" limitation.

## Further Notes

- **Firebase setup is a prerequisite only the product owner can provide**: a Firebase project, its
  Android configuration file (committed, not secret), and a service-account key for the backend
  (supplied through configuration, never committed).
- **Device identity is anonymous and acts as a secret.** Whoever obtains a device's identifier can
  read and change that device's alarms. Alarms contain no personal data (a station, pollen types,
  severities and times), so this is accepted for now and should be revisited with any account work.
- **Severity in notifications skews high**, for the same reason as everywhere else in the app:
  hourly readings are classified against bands defined over daily means.
- **This changes a documented architectural principle.** The backend was deliberately on-demand
  with no scheduler. Its documentation already named push notifications as the point where a
  scheduled poll becomes right. The scheduled poll covers only stations with active alarms and
  reuses the existing cache, so the "no traffic for stations nobody watches" property holds.
- **The backend becomes stateful.** Until now a restart lost nothing of value. With alarms, the
  backend's database file must be kept across restarts and deployments.
- Pollen readings typically arrive about hourly, so a threshold alert follows a new reading by up to
  about 30 minutes (the cache period), not instantly.

---

## Technical Annex
> Written against codebase as of: 2026-10-03 (branch `feature/alarms` at `5ab23db`)

### Architectural Decisions

#### Server (`:server`)

New package layout under `server/src/main/kotlin/ch/stenzel/tim/polleninfo/server/`:

```
├── alarm/
│   ├── domain/        Alarm, AlarmSchedule, AlarmRules, AlarmValidation, PushMessage
│   ├── store/         DeviceStore, AlarmStore, NotificationLog (+ Exposed implementations, Database setup)
│   ├── push/          PushSender, FcmPushSender, LoggingPushSender, PushResult
│   ├── scheduler/     AlarmScheduler (tick) + the minute loop
│   ├── model/         Wire DTOs (@Serializable) incl. polymorphic schedule
│   └── AlarmRoutes.kt fun Route.alarmRoutes(...)
```

**Domain model** (pure Kotlin, `java.time`):

```kotlin
data class Alarm(
    val id: AlarmId,
    val deviceId: DeviceId,
    val enabled: Boolean,
    val station: PollenStation,
    val species: Set<PollenSpecies>,      // non-empty
    val minSeverity: PollenSeverity,      // NONE only allowed for Daily ("Any")
    val days: Set<DayOfWeek>,             // non-empty
    val schedule: AlarmSchedule,
)

sealed interface AlarmSchedule {
    data class Daily(val at: LocalTime) : AlarmSchedule
    data class Threshold(val from: LocalTime, val until: LocalTime) : AlarmSchedule // until > from
}
```

- `ALARM_ZONE = ZoneId.of("Europe/Zurich")` is a single constant. All day, time and "today"
  calculations go through it. `java.time` handles DST.
- `MAX_ALARMS_PER_DEVICE = 10`.
- Freshness reuses the app's rule: a reading is current when `now - measuredAt < 3h` **and**
  `measuredAt` falls on today's date in `ALARM_ZONE`. Mirror the app's `STALE_AFTER` (3 hours) as a
  server constant and reference the app constant in its KDoc. The two must stay equal.

**`AlarmRules`** is the deep module. It is pure, with no clock read, no I/O and no logging:

```kotlin
object AlarmRules {
    /** Daily report due at this minute? Returns the message to send or null. */
    fun evaluateDaily(alarm: Alarm, now: ZonedDateTime, reading: CacheResult<StationMeasurement>): PushMessage?

    /** Threshold alert: qualifying species not yet notified today, batched into one message. */
    fun evaluateThreshold(
        alarm: Alarm, now: ZonedDateTime, reading: StationMeasurement?, notifiedToday: Set<PollenSpecies>,
    ): ThresholdOutcome? // message + the species to record
}
```

- **Daily.** Due when `alarm.enabled`, `now.dayOfWeek in days`, and `now.toLocalTime()` truncated
  to minutes `== at`.
  - **Current reading.** If at least one selected species has `severity.atLeast(minSeverity)`, or
    `minSeverity == NONE`, send. Otherwise return null.
  - **Stale or failed reading.** For `minSeverity == NONE`, send a "no current reading" message,
    including the latest `measuredAt` if a `Stale` value exists. Otherwise return null.
- **Threshold.**
  - **Active when** `enabled`, `now.dayOfWeek in days` and `from <= now.toLocalTime() < until`
    (inclusive start, exclusive end), and the reading is current.
  - **Qualifying species:** selected, `severity != null`, `severity.atLeast(minSeverity)`, and not
    in `notifiedToday`. All of them go into one message.
- **Message text** (English, built here):
  - Title: `"Pollen in <station display name>"`.
  - Body: selected species with a reading, worst first, as `"Birch HIGH · Hazel MODERATE"`. Use the
    same severity and display order as the app's `GetStationMeasurementUseCase` (worst first, then
    `PollenSpecies` declaration order).
  - All selected species at `NONE`: `"No pollen of your selected types."`
  - Stale or unavailable: `"No current reading for <station>. Latest from <HH:mm | HH:mm yesterday | d MMMM>."`
    or `"…currently unavailable."`
  - The exact wording is finalised in the task. It is pinned by tests.
- `PushMessage(title, body, channel: DAILY_REPORT | THRESHOLD_ALERT, data = mapOf("stationAbbr", "alarmId"))`.

**`AlarmValidation`** is a pure function, `AlarmInput -> Either<ValidationError, ValidatedInput>`
(a sealed result, not exceptions). It enforces:
- non-empty species and days;
- a known station abbreviation (case-insensitive, via `PollenStation`);
- `Threshold.until > from`;
- `minSeverity != NONE` for `Threshold`;
- `HH:mm` parsing.

It is used by both POST and PUT.

**Persistence**: SQLite via JetBrains Exposed (DSL) plus `org.xerial:sqlite-jdbc`, with versions
pinned in `libs.versions.toml` at implementation time.
- **Database file:** from `POLLENINFO_DB` env, default `./data/polleninfo.db` (directory created on
  start). The schema is created with `SchemaUtils.create` on start. There's no migration tool yet.
- **Tables:**
  - `devices(id TEXT PK, fcm_token TEXT NULL, created_at)`. `fcm_token` becomes null when FCM
    reports `UNREGISTERED`. Devices without a token are skipped by the scheduler.
  - `alarms(id TEXT PK, device_id FK, enabled, station_abbr, species TEXT /*csv of enum names*/,
    min_severity, days TEXT /*csv*/, type, at_time NULL, from_time NULL, until_time NULL, created_at)`.
  - `notification_log(alarm_id FK ON DELETE CASCADE, species, local_date, PK(alarm_id, species, local_date))`.
    Rows older than today are pruned by the scheduler once per day.
- **Interfaces** (suspend, `Dispatchers.IO` inside the implementation):
  - `DeviceStore`: `register(token): DeviceId`, `updateToken(id, token): Boolean`, `clearToken(id)`,
    `exists(id)`.
  - `AlarmStore`: `list(deviceId)` in creation order, `create(deviceId, input): CreateResult`
    (returning `LimitReached` at 10, checked in the same transaction), `update(deviceId, alarmId,
    input): Alarm?` (null if not found or not owned), `delete(deviceId, alarmId): Boolean`, and
    `enabledWithDeliverableDevice(): List<AlarmWithToken>`.
  - `NotificationLog`: `notifiedSpecies(alarmId, date)`, `record(alarmId, species, date)`,
    `pruneBefore(date)`.
- **IDs:** `deviceId` is 128 bits from `SecureRandom`, base64url without padding (22 chars).
  `alarmId` is a random UUID string.

**`PushSender`**:

```kotlin
interface PushSender { suspend fun send(token: String, message: PushMessage): PushResult }
sealed interface PushResult { data object Sent; data object Unregistered; data class Failed(val cause: Throwable) }
```

- **`FcmPushSender(client: HttpClient, projectId: String, accessToken: suspend () -> String, baseUrl = "https://fcm.googleapis.com")`.**
  - It POSTs to `/v1/projects/{projectId}/messages:send` with `message.token`,
    `message.notification{title, body}`, `message.android.notification.channel_id`, and
    `message.data{stationAbbr, alarmId}`.
  - Responses: HTTP 404 with `UNREGISTERED`, or 400 `INVALID_ARGUMENT` on the token, maps to
    `Unregistered`. Any other non-2xx maps to `Failed`.
  - The access token comes from `google-auth-library-oauth2-http`
    (`GoogleCredentials.fromStream(...).createScoped("https://www.googleapis.com/auth/firebase.messaging")`,
    refreshed when needed). It's wrapped in the `accessToken` lambda so tests never touch Google.
- **`LoggingPushSender`** logs the token suffix and the message. It's used when `FCM_CREDENTIALS`
  (path to the service-account JSON) is unset, with a startup warning. The project ID is read from
  the credentials file.
- **Wiring** follows the `meteoSwissMeasurementService` precedent: an `HttpClient(CIO)` with a
  timeout, closed on `ApplicationStopped`.

**`AlarmScheduler`**:

```kotlin
class AlarmScheduler(
    private val alarms: AlarmStore, private val devices: DeviceStore, private val log: NotificationLog,
    private val measurements: MeasurementService, private val push: PushSender, private val clock: java.time.Clock,
) {
    suspend fun tick()
}
```

- **Each `tick()`:**
  1. Read `now = ZonedDateTime.now(clock).withZoneSameInstant(ALARM_ZONE)`.
  2. Load `enabledWithDeliverableDevice()` and group by station.
  3. For each station that has at least one alarm due or active this minute, call
     `measurements.measurementFor(station)` once. Stations in parallel, each in its own
     `supervisorScope` child so one failure can't stop the others.
  4. Evaluate with `AlarmRules`, then send.
  5. On `Sent` for a threshold alert, `record` each species. On `Unregistered`, call
     `devices.clearToken`. On `Failed`, log, and **don't** record, so the next tick within the
     window can retry.
  6. Prune `notification_log` when the date changes.
- **Coverage.** Threshold alarms are evaluated every tick while their window is open. The
  `MeasurementService` cache (30-minute TTL) bounds upstream traffic. Daily alarms match only their
  exact minute.
- **No catch-up.** Only the current minute is evaluated, and missed minutes are not replayed.
- **The loop** is a coroutine launched from `Application.module()` on the application scope. It
  computes the delay to the next whole minute from `clock` and calls `tick()` inside `try/catch`, so
  one failing tick never stops the loop. It's cancelled on `ApplicationStopped`. The loop has no
  logic worth testing. `tick()` is the tested surface.
- **Wiring changes in `configureRouting`.** Today it builds the `MeasurementService` privately.
  Restructure so `Application.module()` builds the `MeasurementService`, the stores, the
  `PushSender` and the scheduler once, and passes the `MeasurementService` and stores into
  `configureRouting(...)`. Keep the defaulted-collaborator style so `PollenRoutesTest` continues to
  work unchanged.

**REST API** (`fun Route.alarmRoutes(devices: DeviceStore, alarms: AlarmStore)`):

| Method | Path | Body → Response |
|---|---|---|
| POST | `/devices` | `{ "fcmToken": "…" }` → `201 { "deviceId": "…" }` |
| PUT | `/devices/{deviceId}/token` | `{ "fcmToken": "…" }` → `204`; `404` unknown device |
| GET | `/devices/{deviceId}/alarms` | → `200 [Alarm]` (creation order); `404` unknown device |
| POST | `/devices/{deviceId}/alarms` | `AlarmInput` → `201 Alarm`; `400` invalid; `404` unknown device; `409` at 10 |
| PUT | `/devices/{deviceId}/alarms/{alarmId}` | `AlarmInput` → `200 Alarm`; `400`; `404` unknown device, unknown alarm or not owned |
| DELETE | `/devices/{deviceId}/alarms/{alarmId}` | → `204`; `404` as above |

```json
{ "id": "…", "enabled": true, "stationAbbr": "PZH",
  "species": ["BIRCH", "GRASSES"], "minSeverity": "HIGH",
  "days": ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY"],
  "schedule": { "type": "threshold", "from": "07:00", "until": "21:00" } }
// or "schedule": { "type": "daily", "at": "08:00" }
```

- `AlarmInput` is the same shape without `id`.
- `schedule` is a `@Serializable sealed interface` with `@SerialName("daily")` /
  `@SerialName("threshold")`. Uses the `type` class discriminator: configure the server's `Json` in
  `configureSerialization` (and the app's in `createHttpClient`) with `classDiscriminator = "type"`,
  or annotate with `@JsonClassDiscriminator("type")`.
- Days are `java.time.DayOfWeek` names. Times are `HH:mm` strings in `ALARM_ZONE`.
- `400` bodies are `{ "error": "…" }`.
- **No notification-log reset on PUT.** Editing keeps "already notified today" entries, which are
  keyed per species, so newly added species can still fire.
- Wire DTOs live in `alarm/model/`, separate from `alarm/domain/`, following the `pollen/model/`
  convention.

**New server dependencies:** `exposed-core`, `exposed-jdbc`, `sqlite-jdbc`,
`google-auth-library-oauth2-http`.

#### App (`:composeApp`)

**Navigation**
- `Screen.Feature4` becomes `Screen.Alarms` (data object). `TopLevelDestination.FEATURE_4` becomes
  `ALARMS(Screen.Alarms, Icons.Default.Notifications, "Alarms")`.
- New `@Serializable data class Screen.AlarmEditor(val alarmId: String? = null)`. It's not a tab, so
  `TopLevelDestination.current` returns null and the bar hides.
- `AppNavigation` registers both. `AlarmsScreen` receives `onCreateAlarm` and `onEditAlarm(id)`
  lambdas, and the editor receives `onDone` (calls `popBackStack`).
- **List refresh after the editor.** `AlarmsViewModel` reloads when the tab becomes visible again.
  Use a `LaunchedEffect` keyed on the back-stack entry lifecycle reaching `RESUMED`, or a
  saved-state-handle result flag. Pick one in the task and test the ViewModel side as
  `onResume() → reload`.

**`core/push`**
- `PushTokenProvider` interface: `suspend fun token(): PushTokenResult` (`Available(token)` |
  `Unavailable`), bound in `platformModule`.
  - Android: `FirebasePushTokenProvider`, wrapping `FirebaseMessaging.getInstance().token` with
    `suspendCancellableCoroutine`.
  - iOS: `UnavailablePushTokenProvider`.
- `PollenFirebaseMessagingService : FirebaseMessagingService` (androidMain, registered in the
  manifest). `onNewToken`: if a `deviceId` is stored, call the token endpoint. It doesn't override
  `onMessageReceived` for display, since notification messages are shown by the system in the
  background. Foreground display: override `onMessageReceived` to post the notification on the
  given channel via `NotificationManagerCompat`, so foreground and background behave the same.
- Notification channels `daily_report` ("Daily reports") and `threshold_alert` ("Threshold
  alerts"), default importance. They're created on app start (in the `Application` or
  `MainActivity`), and the channel IDs match the server's `PushMessage.channel` serialisation.
- Manifest: `POST_NOTIFICATIONS` permission and the messaging service.
- Gradle: `com.google.gms.google-services` plugin, Firebase BoM plus `firebase-messaging` in
  `androidMain`. `composeApp/google-services.json` is committed.

**`core/preferences`**
- `DeviceRegistrationRepository` (or extend preferences with a second logic-free repository):
  `deviceId: Flow<String?>`, `suspend fun store(id): Result<Unit>`, `suspend fun clear(): Result<Unit>`.
- `NotificationPermissionPreferences`: `askedBefore: Flow<Boolean>` plus `suspend fun markAsked()`.
- DataStore-backed and logic-free, like `DataStoreSelectedStationRepository`, with fakes in
  `commonTest`.

**`core/notifications` (permission)**
- `enum class NotificationPermissionState { ENABLED, CAN_REQUEST, MUST_OPEN_SETTINGS }`.
- `@Composable expect fun rememberNotificationPermissionController(): NotificationPermissionController`,
  exposing `fun currentStatus(askedBefore: Boolean): NotificationPermissionState`,
  `fun request(onResult: (Boolean) -> Unit)` and `fun openSettings()`.
  - **Android:** `NotificationManagerCompat.areNotificationsEnabled()`. On API 33 and above, when
    not enabled and `askedBefore && !shouldShowRequestPermissionRationale`, the state is
    `MUST_OPEN_SETTINGS`; otherwise `CAN_REQUEST`. Below API 33, when not enabled, the state is
    always `MUST_OPEN_SETTINGS`. `openSettings` fires `Settings.ACTION_APP_NOTIFICATION_SETTINGS`.
  - **iOS:** `UNUserNotificationCenter.getNotificationSettings`. `notDetermined` is
    `CAN_REQUEST`, `denied` is `MUST_OPEN_SETTINGS`, `authorized` or `provisional` is `ENABLED`.
    `request` calls `requestAuthorization`, and `openSettings` opens
    `UIApplicationOpenSettingsURLString`.
- `AlarmsScreen` re-reads the status on `Lifecycle.Event.ON_RESUME` and hands it to
  `AlarmsViewModel.onPermissionState(state)`. The ViewModel never touches platform APIs, following
  the `rememberCoarseLocationPermissionRequester` precedent.

**`feature/alarms`** (vertical slice, mirroring `feature/home` layering):

```
feature/alarms/
├── data/remote/        AlarmApiService(client, baseUrl), dto/ (AlarmDto, AlarmInputDto, ScheduleDto sealed, DeviceDto)
├── data/mapper/        AlarmDto.toDomain(), Alarm.toInputDto()
├── data/repository/    AlarmRepositoryImpl
├── domain/model/       Alarm, AlarmSchedule (Daily(at: LocalTime) | Threshold(from, until)), AlarmDraft, AlarmFormState
├── domain/repository/  AlarmRepository
├── domain/             AlarmSummary formatting (pure)
└── presentation/       AlarmsScreen/ViewModel/UiState, AlarmEditorScreen/ViewModel/UiState/Event
```

- **Domain types:** `kotlinx.datetime.LocalTime` / `DayOfWeek`, `PollenSeverity` from
  `core/measurement`, `Station` from `core/station`. Species use the app's existing species
  representation in `core/measurement` (id plus display name). If no species enum exists
  app-side, add one in `core/measurement` mirroring the server's seven `PollenSpecies` IDs, since
  the editor needs the vocabulary without a reading.
- **`AlarmRepository`:** `alarms(): Result<List<Alarm>>`, `create(draft): Result<Alarm>`,
  `update(id, draft): Result<Alarm>`, `delete(id): Result<Unit>`, `alarm(id): Result<Alarm>`.
  - Calls `ensureRegistered()` first. If no `deviceId` is stored, it gets a token from
    `PushTokenProvider` (`Unavailable` fails with a typed `PushUnavailable` failure that the UI maps
    to the "not available on this device" message), POSTs `/devices`, and stores the ID.
  - On a `404` that signals an unknown device, it clears the stored ID, re-registers once, and
    retries once.
  - A `409` maps to a typed `AlarmLimitReached` failure, and a `400` to `InvalidAlarm(message)`.
  - Everything is wrapped in `safeCall`. Import our `Result` explicitly.
- **`AlarmFormState`** (pure, deep):
  - Built from defaults (new; home station from `SelectedStationRepository`) or from an existing
    `Alarm` (edit).
  - Exposes `isValid`, `isDirty` (compared to its initial value), `typeLocked`,
    `severityOptions` (with Any for Daily, without it for Threshold), and `toDraft()`.
  - Switching the type on a new alarm resets the severity to that type's default and swaps the
    time fields to the defaults: Daily is 08:00, Any; Threshold is 07:00–21:00, High.
- **`AlarmSummary`** (pure): `fun summaryOf(alarm, speciesNames): String`. Day sets collapse to
  ranges ("Mon–Fri", "Every day", "Sat, Sun"). The list row shows the station name separately,
  resolved from the station list. Formatting uses `kotlinx-datetime` format builders, not
  `String.format`.
- **`AlarmsUiState`:** `PermissionRequired(state: CAN_REQUEST | MUST_OPEN_SETTINGS)` | `Loading` |
  `Content(alarms, isRefreshing, limitReached, toggleError)` | `Error(message, pushUnavailable)`.
  - The toggle is optimistic. On failure the switch reverts and `toggleError` is shown as a
    snackbar via a one-shot event.
  - Registration and loading start only on the first `ENABLED` permission state.
- **`AlarmEditorUiState`:** `Loading` (edit fetch) | `Editing(form, stations, isSaving, saveError,
  showDiscardDialog, showDeleteDialog)` | `Error`.
  - `AlarmEditorEvent.Done` goes on a `Channel(BUFFERED)` after a successful save or delete.
  - Back with `isDirty` shows the dialog. Without it, `Done` is sent.
- **DI:** `AlarmApiService` and the repository go in `dataModule`. `PushTokenProvider` and the
  permission-free platform bits go in `platformModule`. The ViewModels use `viewModel { }`, with
  `AlarmEditorViewModel` taking `alarmId` as a parameter.
- **Material 3 components:** `SingleChoiceSegmentedButtonRow`, `FilterChip`, `TimePicker` in an
  `AlertDialog`, `ExposedDropdownMenuBox` (as in onboarding) and `Switch`. Confirm the experimental
  opt-ins compile for iOS.

**Documentation:** update CLAUDE.md with:
- the architecture diagram and the "Fetching is on demand" section (the scheduler now exists for
  alarm stations);
- the Firebase setup and the Google Play services exception;
- server persistence and the database file;
- the new REST endpoints;
- the Alarms tab and editor route;
- the bottom-bar tab list;
- a `feature/alarms` entry under the real slices.

### Automated Testing Decisions

**What makes a good test here:** assert external behaviour through each module's public surface,
with inputs and outputs and no peeking at internals. Time is always injected: `MutableClock` on the
server, explicit `ZonedDateTime` arguments to `AlarmRules`, and `TestScope` virtual time in the app.
No test sleeps, contacts MeteoSwiss, or contacts Google/FCM. Pin every boundary from both sides, as
`SpeciesThresholdsTest` does. Backtick test names, with no commas, because Kotlin/Native rejects
them.

**Server (`server/src/test`)**
- **`AlarmRulesTest`** (unit, the largest suite):
  - Daily: due at the exact minute, not one minute before or after. Day included or excluded.
    The filter met, not met, and `NONE` always sends. A stale reading with `NONE` gives the "no
    current reading" text (with the latest time) and a filtered one gives null. `Failed` gives
    "unavailable" or null.
  - Threshold: the window edges (`from` inclusive, `until` exclusive, a minute either side).
    Severity exactly at the threshold and one band below. Species without a reading are ignored.
    `notifiedToday` excludes species. Batching. Freshness at 2:59 and 3:00, and a reading from
    yesterday's date. A disabled alarm never sends.
  - DST: an 08:00 alarm on both changeover Sundays.
  - Message text: worst first, the all-NONE body, and the title.
- **`AlarmValidationTest`** (unit): every rejection and every acceptance at its boundary
  (`until == from`, `until = from + 1 min`).
- **`ExposedStoresTest`** (integration against `jdbc:sqlite::memory:` or a temp file): register and
  update tokens; clearing a token removes the device from `enabledWithDeliverableDevice`;
  ownership (another device's alarm gives null or false); creation order; the limit at 9→10 OK and
  10→11 `LimitReached`; delete cascades the notification log; record, query and prune of the log;
  data survives reopening a file-backed database.
- **`AlarmSchedulerTest`** (integration of `tick()` with real `AlarmRules`, in-memory stores,
  `MeasurementService` over `FakePollenService` fixtures, `FakePushSender` recording messages, and
  `MutableClock`):
  - A daily report fires once at its minute and not at the next tick.
  - A threshold alert fires once per species per day, again the next day, and a `Failed` send is
    retried on the next tick.
  - `Unregistered` clears the token.
  - A station without alarms is never requested (assert on `FakePollenService`'s request record).
  - Only one upstream fetch per station within the cache TTL across many ticks.
  - An upstream failure with a stale reading produces no threshold alert.
  - No catch-up after the clock jumps past a daily time.
  - One station failing doesn't block the others.
- **`FcmPushSenderTest`** (`MockEngine`): the request URL, auth header, and JSON body shape
  (token, notification, channel_id, data). `200` gives `Sent`. `404 UNREGISTERED` gives
  `Unregistered`. `500` and timeouts give `Failed`.
- **`AlarmRoutesTest`** (`testApplication`, own stores injected, following `PollenRoutesTest`):
  every status code in the API table; the polymorphic `schedule` round-trips in both variants; case
  handling of the station abbreviation; cross-device access gives `404`.
- **Prior art:** `PollenRoutesTest`, `TtlCacheTest` / `MutableClock`, `FakePollenService`,
  `MeteoSwissPollenServiceTest`, `SpeciesThresholdsTest`.

**App (`composeApp/src/commonTest`)**
- **`AlarmRepositoryImplTest`** (real `HttpClient` via `createHttpClient(MockEngine)`, fake
  preferences, fake `PushTokenProvider`): lazy registration happens once and is then reused; an
  unavailable token gives `PushUnavailable` with no network call; a `404` re-registers once and
  retries; `409` gives `AlarmLimitReached`; `400` gives `InvalidAlarm`; DTO and mapper round-trips
  for both schedule types.
- **`AlarmFormStateTest`** (unit): the defaults for new alarms (home station preselected); type
  switching resets severity and time; `isValid` at each rule; `isDirty` false after a no-op edit
  and back; `typeLocked` in edit; the severity options per type; `toDraft()`.
- **`AlarmSummaryTest`** (unit): both types; the day-range collapsing (every day, Mon–Fri, the
  weekend, a non-contiguous set); time formatting.
- **`AlarmsViewModelTest`** (`StandardTestDispatcher`, fakes):
  - `CAN_REQUEST` and `MUST_OPEN_SETTINGS` map to `PermissionRequired`, with no network call.
  - `ENABLED` leads to Loading, then Content.
  - Revoking returns to `PermissionRequired`.
  - `Error`, then retry.
  - Refresh keeps the alarms (`isRefreshing`, observed with a gated fake).
  - The toggle is optimistic and reverts on failure with an event.
  - `limitReached` at 10.
  - `PushUnavailable` gives `Error(pushUnavailable = true)`.
  - Resume triggers a reload.
- **`AlarmEditorViewModelTest`**: new versus edit loading; save success emits `Done` once; save
  failure keeps the form and shows `saveError`; `isSaving` is observable mid-flight; the discard
  dialog appears only when dirty; delete confirmation then `Done`; a load error for edit.
- **`TopLevelDestinationTest`**: updated order and names ("Alarms" in position 4), and
  `AlarmEditor` is not a tab, so the bar is hidden.
- **Fakes:** `FakeAlarmRepository` (gated, scriptable failures), `FakePushTokenProvider`,
  `FakeDeviceRegistrationRepository`, `FakeNotificationPermissionPreferences`.
- **Not automated (thin, checked manually):** the Android and iOS permission controllers, the
  `FirebaseMessagingService`, `FirebasePushTokenProvider`, channel creation, the DataStore-backed
  preferences, and the composables.
- **Prior art:** `HomeViewModelTest` (gated fake for intermediate states), `OnboardingViewModelTest`
  (one-shot events, permission result handed to the ViewModel), `AllStationsViewModelTest`
  (refresh semantics), the station and measurement repository tests with `MockEngine`.
- **Gates:** `./gradlew :composeApp:testDebugUnitTest :server:test` and
  `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64`.
