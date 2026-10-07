# Settings and Localization

## Problem Statement

PollenInfo is used across Switzerland, but it speaks only English. A user in Lausanne, Lugano or
Bern sees English screens, English pollen-type names, English dates and English push notifications,
and has no way to change that. Error messages are worse still: some of them are raw technical text
("Failed to connect to …") that no user should ever see, in any language.

Once onboarding is finished, the user's home station is fixed. Someone who moves, or who picked the
wrong station at first, has no way to change it short of clearing the app's data — which also throws
away their alarms and diary.

The app also has nowhere to say who made it, how to reach them, which version is installed, or
where its data comes from. MeteoSwiss's terms of use require every user of their open data to name
them as the source and to avoid appearing endorsed by them; the app currently does neither.

The fifth tab of the bottom navigation is still a "Coming soon" placeholder.

## Solution

The fifth tab becomes **Settings**, a single scrolling screen with five sections:

1. **Default station** — shows the current home station. Tapping it opens a screen that works like
   onboarding (use my location, or pick from the list), starts on the current station, and saves the
   new choice. Home follows the new station at once.
2. **Language** — System default, English, Deutsch, Français, Italiano. "System default" follows the
   phone's language when it is German, French or Italian and uses English otherwise. On Android the
   change applies immediately and is the same setting as the phone's own per-app language screen; on
   iOS it applies the next time the app is opened.
3. **Impressum** — "Developed by Tim Stenzel" and a tappable contact email address.
4. **Data source** — the MeteoSwiss attribution in the wording their terms of use prescribe for the
   current language, a link to their website in that language, and a statement that PollenInfo is
   independent and not endorsed by MeteoSwiss.
5. **Version** — the installed app version.

Behind the language setting, the **whole app is translated** into German (Swiss Standard German,
informal "du"), French and Italian (both formal): every screen, every accessibility description,
pollen-type names, severity words, month and weekday names, date formats, error messages and the
Android notification channel names. Station names stay as MeteoSwiss publishes them.

**Push notifications are translated too.** The backend stops writing notification text; it sends
what happened (which station, which pollen types at which level, how old the reading is) and the
phone writes the notification in the app's language, using exactly the same words as the screens.

Error messages become friendly, translated sentences chosen by the kind of failure ("Couldn't reach
the server. Check your connection and try again.") instead of whatever text a library or the
backend produced.

## User Stories

### Settings tab

1. As a user, I want a Settings tab in the bottom navigation bar, so that I can find the app's options in one place.
2. As a user, I want the Settings tab to show a recognisable settings icon, so that I can tell it apart from the other tabs.
3. As a screen-reader user, I want the Settings tab announced as "Settings" in my language, so that I know where it leads.
4. As a user, I want Settings to keep its scroll position when I switch tabs and come back, so that it behaves like every other tab.
5. As a user, I want back from Settings to return me to Home, so that navigation stays consistent with the other tabs.
6. As a user, I want the Settings screen grouped into clearly headed sections, so that I can scan it quickly.
7. As a screen-reader user, I want each section title announced as a heading, so that I can jump between sections.

### Changing the default station

8. As a user, I want to see my current default station on the Settings screen, so that I know which station Home shows.
9. As a user, I want to tap my default station to change it, so that I can switch after moving or after a wrong first choice.
10. As a user, I want the change-station screen to open with my current station already selected, so that I see what I am changing from.
11. As a user, I want to use my location on the change-station screen, so that the nearest station is proposed without searching the list.
12. As a user, I want the location shortcut to only propose a station and let me confirm it, so that the app never changes my station without my say.
13. As a user, I want to pick a station directly from the dropdown, so that I can choose a station that is not the nearest one.
14. As a user, I want picking a station manually to cancel a location lookup still in progress, so that a late location result cannot overwrite my choice.
15. As a user, I want a clear message when location permission is denied or my position cannot be found, so that I know to pick from the list instead.
16. As a user, I want the Save button disabled while the selection is still my current station, so that I cannot "save" a non-change.
17. As a user, I want saving to return me to Settings showing the new station, so that I see the change took effect.
18. As a user, I want Home to show the new station's readings after I change it, so that my dashboard follows my choice without restarting the app.
19. As a user, I want to leave the change-station screen with back without saving, so that I can abandon a change freely.
20. As a user, I want a failed save to keep me on the screen with an error, so that I know the change did not happen and can try again.
21. As a user, I want a retry when the station list cannot be loaded, so that a temporary network problem does not strand me.
22. As a user, I want my existing alarms to keep their stations when I change my default, so that changing Home does not silently change what my alarms watch.
23. As a user, I want new alarms to start on my new default station, so that the editor proposes the station I care about now.
24. As a user, I want the Diary to keep the station I am currently comparing during this session, so that changing my default does not jump my chart.

### Language

25. As a user, I want to see which language the app is currently using, so that I know what the setting is.
26. As a user, I want to choose between System default, English, German, French and Italian, so that I can use the app in my preferred Swiss national language or in English.
27. As a user, I want each language listed in its own name (Deutsch, Français, Italiano), so that I can find my language even if the app is currently in one I cannot read.
28. As a new user, I want the app to start in my phone's language when it is German, French or Italian, so that I do not have to find Settings first.
29. As a user whose phone is in another language, I want the app to use English, so that I always get a language the app supports.
30. As an Android user, I want a language change to take effect immediately without losing my place, so that switching feels instant.
31. As an Android 13+ user, I want the app's language in Android's own per-app language settings to be the same setting as in the app, so that the two can never disagree.
32. As an iOS user, I want to be told that a language change applies the next time I open the app, so that I am not confused when nothing changes right away.
33. As a German-speaking Swiss user, I want Swiss Standard German spelling (ss, not ß) and the informal "du", so that the app reads like a Swiss app.
34. As a French- or Italian-speaking user, I want the formal "vous" / "Lei" and correct typography, so that the app reads naturally and politely.
35. As a user, I want pollen-type names in my language (Birke, Bouleau, Betulla), so that I recognise the plants.
36. As a user, I want severity words in my language (Gering, Mässig, Hoch, Sehr hoch …), so that I understand the levels at a glance.
37. As a user, I want dates in my language's form ("29. Juli", "29 juillet", "29 luglio"), so that dates read naturally.
38. As a user, I want weekday names and ranges in my language ("Mo–Fr", "lun–ven"), so that alarm summaries and day chips make sense.
39. As a user, I want month labels on the Diary chart in my language, so that the axis reads naturally.
40. As a user, I want times shown in 24-hour format in every language, so that they match Swiss conventions.
41. As a user, I want station names shown as MeteoSwiss publishes them (Genève, Zürich, Lugano), so that they match official sources.
42. As a screen-reader user, I want every spoken description (chart descriptions, button names, states) in my language, so that the app is accessible in my language.
43. As an Android user, I want the notification categories in Android's notification settings named in my language, so that I can manage them.

### Notifications

44. As a user with alarms, I want my daily reports in the app's language, so that notifications match the app.
45. As a user with alarms, I want my threshold alerts in the app's language, so that I understand them immediately.
46. As a user, I want a notification to use the same pollen-type and severity words as the app's screens, so that the two never disagree.
47. As a user, I want a notification to look the same whether the app is open or not, so that its behaviour is predictable.
48. As a user, I want a notification to name the time of the last reading in my language when no current reading exists, so that I understand how old the data is.
49. As a user, I want to still receive a sensible notification if the app does not understand some detail sent by the server, so that an alarm is never silently lost.
50. As a user, I want tapping a notification to open the app, so that I can see the details.
51. As a user who changes the app's language, I want later notifications in the new language, so that the change applies everywhere.

### Errors

52. As a user, I want error messages written for people and in my language, so that I understand what went wrong.
53. As a user, I want a different message for "no connection" than for "the server is having problems", so that I know whether to check my own network.
54. As a user, I want a clear message when I hit the alarm limit, so that I know why I cannot add another alarm.
55. As a user, I never want to see raw technical error text, so that the app feels trustworthy.

### Impressum

56. As a user, I want to see who developed the app, so that I know who is responsible for it.
57. As a user, I want to tap the developer's email address to write to them, so that I can report a problem or give feedback easily.
58. As a user, I want the email draft to come pre-filled with the subject "PollenInfo", so that the developer knows what my message is about.

### Data source

59. As a user, I want to see that the pollen data comes from MeteoSwiss, so that I know the data's origin and can trust it.
60. As a user, I want the source named in my language (MeteoSchweiz, MétéoSuisse, MeteoSvizzera), so that it reads naturally.
61. As a user, I want to open MeteoSwiss's website in my language from the app, so that I can learn more from the official source.
62. As the app owner, I want the attribution to use exactly the wording MeteoSwiss's terms of use prescribe, so that the app complies with them.
63. As the app owner, I want a statement that the app is independent and not endorsed by MeteoSwiss, so that the app complies with their terms.

### Version

64. As a user, I want to see the installed app version, so that I can mention it when reporting a problem.
65. As a tester, I want debug builds marked as such in the version line, so that I can tell builds apart.

### Maintainer

66. As a maintainer, I want the build to fail when a translation is missing, empty or has mismatched placeholders, so that no language silently falls back to English.
67. As a maintainer, I want the reference example feature to show how text is loaded from resources, so that new features follow the pattern.
68. As a maintainer, I want the project documentation to describe localization, the new error model, the notification format and the Settings screens, so that the next change starts from accurate docs.

## User Acceptance Tests

### Settings tab and screen

1. Given the app is past onboarding, when I look at the bottom bar, then the fifth tab shows a settings icon and opening it shows a screen titled "Settings".
2. Given TalkBack is on, when I focus the fifth tab, then it is announced as "Settings" (or its translation in the app's language).
3. Given I am on Settings, when I press back, then Home is shown; pressing back again leaves the app.
4. Given I am on Settings, when I read the screen top to bottom, then I see the sections Default station, Language, Impressum, Data source and the version line, in that order.

### Default station

5. Given my default station is Zürich, when I open Settings, then the Default station row shows "Zürich".
6. Given my default station is Zürich, when I tap the Default station row, then a screen opens with Zürich preselected and Save disabled, and no bottom bar is shown.
7. Given the change-station screen, when I pick Bern from the dropdown, then Save becomes enabled; when I pick Zürich again, then Save is disabled again.
8. Given the change-station screen and location permission granted, when I tap "Use my location", then the nearest station is selected and nothing is saved until I tap Save.
9. Given a location lookup is in progress, when I pick a station from the dropdown, then the lookup stops and my pick is not replaced when the lookup would have finished.
10. Given location permission is denied, when I tap "Use my location", then a message tells me to pick a station from the list, and the dropdown stays usable.
11. Given I picked Bern, when I tap Save, then I return to Settings, the row shows "Bern", and Home shows Bern's readings.
12. Given I picked Bern but did not save, when I press back, then I return to Settings and the default is still Zürich.
13. Given I have an alarm for Lugano, when I change my default station to Bern, then the alarm still shows Lugano.
14. Given I changed my default to Bern, when I create a new alarm, then the editor starts on Bern.
15. Given the Diary is showing Lugano (chosen in its dropdown), when I change my default station to Bern and return to the Diary, then it still shows Lugano.
16. Given the backend is unreachable, when I open the change-station screen, then an error with Retry is shown, and Retry loads the list once the backend is back.

### Language

17. Given a fresh install on a phone set to German, when I open the app, then onboarding and every later screen are in German.
18. Given a fresh install on a phone set to Spanish, when I open the app, then it is in English.
19. Given Settings, when I tap the Language row, then a dialog lists System default, English, Deutsch, Français, Italiano with the current choice marked.
20. Given an Android phone, when I choose Français, then the app switches to French immediately, I am still on Settings, and Home's readings reappear without a full reload when I go back.
21. Given an Android 13+ phone with the app set to Italiano, when I open Android's per-app language settings for PollenInfo, then Italiano is selected there; when I change it there to Deutsch, then the app is in German and its Language row shows Deutsch.
22. Given an iOS device, when I choose Deutsch, then a note says the change applies the next time the app is opened; after reopening, the app is in German.
23. Given the app is in German, when I look at Home, then pollen types read e.g. "Birke", "Gräser", severities read e.g. "Mässig", and no "ß" appears anywhere in the app.
24. Given the app is in German, when I see the feeling prompt, then it asks "Wie fühlst du dich heute?".
25. Given the app is in French, when I see the feeling prompt, then it uses "vous" and a non-breaking space before the question mark.
26. Given the app is in Italian and a reading is from an earlier day, when I look at Home's warning, then the date reads e.g. "29 luglio".
27. Given the app is in German, when I look at the Diary in month view, then the date labels read e.g. "4. Sep." and in year view the month names are German.
28. Given the app is in French, when I look at an alarm for Monday to Friday, then its summary shows the French weekday range and French severity words.
29. Given any language, when I look at times anywhere in the app, then they are shown as 24-hour "HH:mm".
30. Given any language, when I look at station names, then they read exactly as MeteoSwiss publishes them (e.g. "Genève", "Zürich").
31. Given the app is in German and TalkBack is on, when I focus the Diary chart, then its description is read in German.
32. Given an Android phone with the app in German, when I open Android's notification settings for PollenInfo, then the two categories are named in German.

### Notifications

33. Given the app is set to German and I have a daily report for Zürich at 08:00, when 08:00 passes, then the notification reads "Pollen in Zürich" with German pollen-type and severity words, worst first.
34. Given the app is set to Italian and a threshold alert fires, when the notification appears, then its text is Italian and uses the same words as the app's screens.
35. Given a daily report with "Any" and only an old reading, when it fires, then the notification says in the app's language that there is no current reading and names the time (or "yesterday" / the date) of the latest one.
36. Given a daily report with "Any" and no reading at all, when it fires, then the notification says readings are currently unavailable.
37. Given a daily report where every selected type is at "None", when it fires, then the notification says there is no pollen of the selected types.
38. Given a daily report where the station reports none of the selected types, when it fires, then the notification says there is no reading for the selected types.
39. Given a notification arrives while the app is open and another while it is closed, when I compare them, then they look the same and tapping either opens the app.
40. Given the app was in English and I switch it to French, when the next alarm fires, then its notification is in French.
41. Given the server sends a notification type the app does not know, when it arrives, then a generic notification "Pollen in <station>" asking me to open the app is shown in the app's language.

### Errors

42. Given airplane mode, when I open Home, then the error message says the server could not be reached and suggests checking my connection, in the app's language.
43. Given the backend answers with a server error, when I open Home, then the message says the service is having problems, not that my connection is at fault.
44. Given I already have ten alarms (created from another install sharing the id), when I try to save an eleventh, then a translated message explains the limit.
45. Given any error anywhere in the app, when it is shown, then it never contains raw technical text such as host names, status codes or library messages.

### Impressum, data source, version

46. Given Settings, when I look at the Impressum section, then it reads "Developed by Tim Stenzel" and shows developer.mobile.t3s@gmail.com.
47. Given Settings, when I tap the email address, then my mail app opens a new message to developer.mobile.t3s@gmail.com with the subject "PollenInfo".
48. Given the app is in each of the four languages in turn, when I look at the Data source section, then it reads "Source: MeteoSwiss", "Quelle: MeteoSchweiz", "Source: MétéoSuisse" or "Fonte: MeteoSvizzera" respectively.
49. Given the app is in German, when I tap the MeteoSwiss link, then the browser opens www.meteoschweiz.admin.ch (and the matching site for the other languages).
50. Given Settings, when I look at the Data source section, then it states that PollenInfo is independent and not affiliated with or endorsed by MeteoSwiss, in the app's language.
51. Given a release build of version 1.0.0 (code 1), when I look at the bottom of Settings, then it reads "Version 1.0.0 (1)"; on a debug build "Version 1.0.0-debug (1)".

### Maintainer

52. Given a text exists in English but not in Italian, when the project's checks run, then the build fails and names the missing text.
53. Given a translation lacks a placeholder its English text has, when the project's checks run, then the build fails and names it.

## Definition of Done

- All user acceptance tests pass on an Android emulator; the iOS-specific ones are verified as far as the iOS sources compile (there is no iOS app project).
- Every user-visible text in the app, including accessibility descriptions and Android notification channel names, is available in English, German, French and Italian, except station names, the app name "PollenInfo" and the reference example feature (English only, by decision).
- The translation completeness check passes and is part of the project's standard checks.
- No screen shows raw technical error text.
- Push notifications are written by the app in its current language; the backend sends no notification text.
- All automated tests listed in the Technical Annex exist and pass; all app and server unit tests pass and the iOS sources compile.
- No regression in onboarding, Home, All stations, Diary or Alarms.
- The project documentation (CLAUDE.md) describes localization, the error model, the notification format, the Settings and change-station screens and the new iOS wrapper keys.

## Out of Scope

- Translating station names.
- Translating texts the backend returns in its responses (they remain English and serve only as fallbacks or logs).
- Any further languages (e.g. Romansh) or right-to-left support.
- Localized number formatting beyond what the screens already show (concentrations are whole numbers).
- 12-hour time format.
- Opening a specific screen or station from a notification (the tap opens the app as today).
- Supporting older app versions or a deployed backend through the notification format change — there is a clean cut, with no dual format.
- An iOS app project and its Info.plist (only documented, as today).
- Push notifications on iOS.
- Any other settings (theme, units, notification preferences beyond what Android provides).
- Postal address or other Impressum details beyond name and email.
- Crediting the map data source (Natural Earth) — deliberately left out.
- Professional translation review; translations are produced as part of this feature and reviewed in code review.

## Further Notes

- **Notification delivery trade-off.** Notifications written on the phone require the app's process to start when a message arrives. This is reliable in normal use, but a user who has force-stopped the app, or whose phone has put it into the most restrictive battery mode, may receive alarms late or not until they next open the app. With the previous format the system would have shown them anyway. Accepted in exchange for notifications in the user's language.
- **Swiss legal context.** Full Impressum identification (including a postal address) is required in Switzerland only for commercial online offerings; for a free app without in-app sales, name and email are considered sufficient. Revisit if the app becomes commercial.
- **MeteoSwiss terms of use** (opendatadocs.meteoswiss.ch, "Terms of use") require the source in the form "Quelle: MeteoSchweiz; Source: MétéoSuisse; Fonte: MeteoSvizzera; Source: MeteoSwiss" and forbid giving the impression of MeteoSwiss support. They do not require a URL; the link is a courtesy.
- **Translation style**: Swiss Standard German (no ß) with "du"; French and Italian formal ("vous" / "Lei"); French typography with a narrow non-breaking space before ? : ! ; and « » quotes.
- The version shown is whatever the build declares; there is no separate "about" or licence screen.

---

## Technical Annex
> Written against codebase as of: 2026-10-07 (branch `feature/diary`, HEAD `e9eb597`)

### Architectural Decisions

#### Navigation

- `TopLevelDestination.FEATURE_5` → `SETTINGS` (`Screen.Settings`, `Icons.Default.Settings`,
  `contentDescription` from a string resource). `Screen.Feature5` is renamed `Screen.Settings`;
  `ComingSoonScreen` stays in the codebase but is no longer routed (keep it — it is the documented
  placeholder pattern — or delete it if nothing else references it; decide at implementation and
  update CLAUDE.md accordingly). Update `TopLevelDestinationTest` (order and names).
- New `@Serializable data object Screen.ChangeStation`, not a tab: `TopLevelDestination.current`
  returns `null` for it, so the bottom bar is hidden. Settings navigates to it; its `Done` event pops
  back. `contentDescription` of `TopLevelDestination` becomes a `StringResource` (the tested rule
  stays a pure function of the route).

#### `core/stationpicker` (extracted from `feature/onboarding`)

The rule "a feature never imports another feature" forces the move; "code moves to `core/` once a
second feature needs it" decides where.

- Moves: `FindNearestStationUseCase` (`feature/onboarding/domain/usecase` →
  `core/stationpicker/domain/usecase`), `LocationError`, and the selection/location logic now in
  `OnboardingViewModel` (`loadStations`, `onStationSelected` cancelling `locationJob`,
  `onPermissionResult`, `resolveNearestStation` with `LOCATION_TIMEOUT`).
- Shape: a plain state holder, not a ViewModel, owned by each screen's ViewModel and driven by its
  `viewModelScope`:

  ```kotlin
  class StationPicker(
      stationRepository: StationRepository,
      coarseLocationProvider: CoarseLocationProvider,
      findNearestStation: FindNearestStationUseCase,
      scope: CoroutineScope,
      initialAbbr: () -> String?,   // onboarding: null; change-station: the stored station
  ) {
      val state: StateFlow<StationPickerState>   // Loading | Content(stations, selected, isLocating, locationError) | Error(AppError)
      fun retry()
      fun onStationSelected(station: Station)
      fun onPermissionResult(granted: Boolean)
  }
  ```

  The exact type split (holder vs. a base ViewModel) may be adjusted at implementation, provided
  the logic lives once and is tested once.
- Composables move to `core/stationpicker/presentation`: `UseMyLocationButton`, `StationDropdown`,
  the location-error text, and a `StationPickerContent(state, …)` the two screens embed.
- `OnboardingViewModel` keeps the save + `OnboardingEvent.Completed`; `OnboardingUiState` wraps the
  picker state plus `saveError`.
- If a stored abbreviation is no longer in the list, the change-station screen starts with nothing
  selected (as onboarding).

#### `feature/settings`

- `presentation/SettingsViewModel(selectedStationRepository, stationRepository, languageRepository, appInfo)`:
  - Observes `SelectedStationRepository.selectedStation` (so the row updates after a save) and
    resolves the display name from `StationRepository`; until the list arrives, or if it fails,
    the row shows the stored name/abbreviation from `SelectedStation` — no full-screen error for the
    Settings screen.
  - Exposes `language: AppLanguage`, `showLanguageDialog`, `languageAppliesOnRestart` (iOS after a
    change), and `version: AppVersion?`.
  - `onLanguageSelected(AppLanguage)` calls `LanguageRepository.set`.
- `presentation/ChangeStationViewModel(selectedStationRepository, stationPicker deps…)`:
  - Reads the stored station **once** for the initial selection.
  - `canSave = selected != null && selected.abbr != storedAbbr && !isSaving`.
  - `save()` → `SelectedStationRepository.select(...)`; success → `ChangeStationEvent.Done` on a
    buffered `Channel`; failure → `Content.saveError`.
- Screens: `SettingsScreen` (sections with `semantics { heading() }`; language as an `AlertDialog`
  with radio rows; the email and MeteoSwiss link via `LocalUriHandler`), `ChangeStationScreen`
  (`TopAppBar` with back, `StationPickerContent`, Save with progress).
- Constants: developer name "Tim Stenzel", email `developer.mobile.t3s@gmail.com`, `mailto:` with
  `?subject=PollenInfo`. The MeteoSwiss URL is a string resource per language:
  `https://www.meteoswiss.admin.ch` / `https://www.meteoschweiz.admin.ch` /
  `https://www.meteosuisse.admin.ch` / `https://www.meteosvizzera.admin.ch`.

#### `core/appinfo`

- `data class AppVersion(val name: String, val code: Long)`; `interface AppInfo { val version: AppVersion? }`,
  bound in `platformModule`: Android via `PackageManager.getPackageInfo` (`versionName`,
  `PackageInfoCompat.getLongVersionCode`), iOS via `NSBundle.mainBundle` `CFBundleShortVersionString`
  / `CFBundleVersion`, `null` if either is missing (line hidden). Displayed as
  "Version {name} ({code})"; the debug `versionNameSuffix = "-debug"` shows through on its own.

#### `core/language`

- ```kotlin
  enum class AppLanguage(val tag: String?) { SYSTEM(null), EN("en"), DE("de"), FR("fr"), IT("it") }
  interface LanguageRepository {
      val current: AppLanguage            // read synchronously at screen start
      fun set(language: AppLanguage)
      val appliesImmediately: Boolean     // Android true, iOS false
  }
  ```
  Pure helper `AppLanguage.fromTag(tag: String?)` (primary subtag, unknown → `SYSTEM`) is tested.
- **Android** — `AndroidLanguageRepository` over `AppCompatDelegate.getApplicationLocales()` /
  `setApplicationLocales(LocaleListCompat.forLanguageTags(tag ?: ""))`. Requires:
  `androidx.appcompat` in `androidMain`; `MainActivity : AppCompatActivity` (with a
  `Theme.AppCompat`/`Theme.Material3`-compatible manifest theme, e.g. a `Theme.AppCompat.DayNight.NoActionBar` parent,
  since AppCompat refuses non-AppCompat themes); `res/xml/locales_config.xml` listing `en`, `de`,
  `fr`, `it` and `android:localeConfig` on `<application>`; the
  `AppLocalesMetadataHolderService` with `autoStoreLocales=true` in the manifest so API < 33 persists
  the choice. Logic-free, checked by hand, like `DataStore*` repositories.
- **iOS** — `IosLanguageRepository` writes `NSUserDefaults.standardUserDefaults` `AppleLanguages`
  (`[tag]`, or removes the key for `SYSTEM`) and reads it back; `appliesImmediately = false`.
  Compile-verified only. CLAUDE.md "iOS wrapper configuration" gains `CFBundleLocalizations`
  (`en`, `de`, `fr`, `it`) and `CFBundleDevelopmentRegion = en`.
- "System default" relies on resource fallback: Compose resources and Android pick
  `values-de|fr|it` for a matching device locale, `values/` (English) otherwise.

#### String resources

- Compose Multiplatform resources (`compose.components.resources`, already a dependency):
  `composeApp/src/commonMain/composeResources/values/strings.xml` (English, default) and
  `values-de/`, `values-fr/`, `values-it/`. Composables use `stringResource(Res.string.x)` /
  `pluralStringResource`; non-composable suspend code (the push service) uses `getString(...)`.
- **ViewModels hold no user-facing text.** UI states carry ids, enums and `AppError`; screens map
  them to resources.
- Android-only resources in `composeApp/src/androidMain/res/values{,-de,-fr,-it}/strings.xml`:
  notification channel names (`daily_report`, `threshold_alert`) and `app_name` (same "PollenInfo"
  everywhere — declared once in `values/` only, `translatable="false"`).
  `PollenInfoApplication` creates the channels on start as today; it also re-creates them (which
  updates the names) after a language change — `AppCompatDelegate.setApplicationLocales` recreates
  activities, so `MainActivity.onCreate` or the `LanguageRepository.set` path calls the channel
  setup again.
- Key naming: `<area>_<thing>` (`home_feeling_question`, `settings_section_language`,
  `severity_moderate`, `species_birch`, `error_network`, `example_…`).
- `feature/example` moves to resources with `example_`-prefixed keys in `values/` only; the check
  ignores that prefix.

#### Vocabulary lookups (replacing server names and hard-coded words)

- `core/ui/species`: `speciesName(id: String, fallback: String): String` @Composable (and a
  non-composable `speciesNameResource(id): StringResource?` for the push service) keyed by the
  server's species ids (`ALDER`, `BIRCH`, `HAZEL`, `BEECH`, `ASH`, `OAK`, `GRASSES`); unknown id →
  the server's English `name`. Used by Home, All stations, Diary filters and chart description,
  Alarm summaries and the editor.
- `core/ui/severity`: `PollenSeverity.label()` and `minimumLabel()` ("Any") become resource-backed.
- `core/ui/feeling`: `Feeling.label()` resource-backed.

#### `core/ui/format` — localized dates

- Per language, string resources (or string arrays) for 12 full and 12 abbreviated month names and
  7 full and 7 abbreviated weekday names, fed into `kotlinx.datetime.format.MonthNames(list)` /
  `DayOfWeekNames(list)`; a per-language format choice for the date forms:

  | Form | en | de | fr | it |
  |---|---|---|---|---|
  | full (`ReadingAge.Stale.Earlier`, notification "d MMMM") | 29 July | 29. Juli | 29 juillet | 29 luglio |
  | short (Diary month/week axis) | 4 Sep | 4. Sep. | 4 sept. | 4 set |
  | month only (Diary year axis) | Oct | Okt. | oct. | ott |
  | weekday short (alarm summaries, day chips) | Mon | Mo | lun | lun |

- Shape: a pure `DateWording` value (`monthsFull`, `monthsShort`, `weekdaysFull`,
  `weekdaysShort`, `dayMonthOrder`/separator flags) with pure formatting functions, built in a
  composable from resources (`rememberDateWording()`) and passed to `ReadingAgeLabel`, `DiaryChart`
  (axis labels) and `AlarmSummary` (`daysSummary`). The pure functions are tested with literal
  `DateWording` fixtures for each language — no resource loading in tests. Time stays `HH:mm`
  (`LocalTime.Format { hour(); char(':'); minute() }`) in every language.
- Replaces `MonthNames.ENGLISH_FULL` (`ReadingAgeLabel.kt:58`), `MonthNames.ENGLISH_ABBREVIATED`
  (`DiaryChart.kt:140,145`) and the English weekday names in `AlarmSummary` / the editor's day chips
  (whose spoken full day name also comes from `weekdaysFull`).

#### `AppError` — typed errors instead of message strings

- ```kotlin
  sealed interface AppError {
      data object Network : AppError            // no connection, DNS, timeout (IOException family, HttpRequestTimeoutException)
      data object ServerUnavailable : AppError  // 5xx incl. 502
      data object NotFound : AppError           // 404 where the screen treats it as "not available"
      data object PushUnavailable : AppError    // PushUnavailableException
      data object AlarmLimitReached : AppError  // AlarmLimitReachedException
      data object InvalidAlarm : AppError       // InvalidAlarmException (server text kept only in the exception)
      data object Unknown : AppError
  }
  fun Throwable.toAppError(): AppError
  ```
  Located in `core/result` (next to `Result`), pure and tested. The API services already map status
  codes to typed exceptions in places (`AlarmApiService`); where they do not, the mapping needs the
  HTTP status, so services throw a small `HttpStatusException(status)` (or Ktor's
  `ResponseException` via `expectSuccess`) that `toAppError` reads. Exact exception plumbing is an
  implementation choice; the contract is the `AppError` each screen ends up with.
- Every `Error(message: String)` and `saveError: String?` becomes `Error(error: AppError)` /
  `saveError: AppError?`: `HomeUiState`, `AllStationsUiState`, `DiaryUiState`, `AlarmsUiState`
  (`pushUnavailable` folds into `AppError.PushUnavailable`), `AlarmEditorUiState`,
  `OnboardingUiState` (and the new picker/settings states). `AlarmsEvent.ToggleFailed(message)` →
  `ToggleFailed(error: AppError)`. `DEFAULT_*_ERROR` constants disappear.
- `core/ui/error/AppErrorText.kt`: `@Composable fun AppError.message(context: ErrorContext): String`
  where the context picks wording that differs per action (load vs. save vs. delete:
  "Couldn't save the alarm." vs. "Couldn't load your alarms."). Kept small; most kinds share one
  sentence.

#### Push notifications — data-only, rendered on the device

**Server** (`alarm/domain`, `alarm/push`):

- `PushMessage` loses `title`/`body`; it becomes structured:

  ```kotlin
  data class PushMessage(
      val kind: PushKind,
      val channel: PushChannel,
      val station: PollenStation,
      val alarmId: AlarmId,
      val levels: List<Pair<PollenSpecies, PollenSeverity>> = emptyList(), // already ordered: worst first, then PollenSpecies order
      val measuredAt: Instant? = null,                                       // only for NO_CURRENT_READING
  )
  enum class PushKind(val wire: String) {
      REPORT("report"),                       // daily, current reading, some selected type > NONE
      NO_POLLEN("no_pollen"),                 // daily, current reading, all selected types at NONE
      NOT_REPORTED("not_reported"),           // daily, current reading, station reports none of the selected types
      NO_CURRENT_READING("no_current_reading"),// daily "Any", reading older than current
      UNAVAILABLE("unavailable"),             // daily "Any", no reading at all
      ALERT("alert"),                         // threshold
  }
  ```
- `AlarmRules.evaluateDaily` / `evaluateThreshold` keep **every** decision rule (due, active,
  current, minimum, per-day exclusion, ordering) and return the structured message; `title`,
  `readingBody`, `severityList`'s text, `noCurrentReadingBody`, `unavailableBody` and the private
  `PollenSeverity.label()` are removed.
- `FcmPushSender` sends a **data-only** message — no `notification` block — with
  `android.priority = "HIGH"` (needed to start the app's process promptly) and the data map:

  | Key | Value |
  |---|---|
  | `kind` | `PushKind.wire` |
  | `channel` | `PushChannel.id` (`daily_report` / `threshold_alert`) |
  | `stationAbbr` | e.g. `PZH` (existing `DATA_STATION`) |
  | `stationName` | `PollenStation.displayName`, e.g. `Zürich` — the service may run with no station list loaded |
  | `alarmId` | existing `DATA_ALARM` |
  | `levels` | `BIRCH:HIGH,GRASSES:MODERATE` — species and severity enum names, comma-separated, in display order; empty/absent when not applicable |
  | `measuredAt` | ISO-8601 instant, only for `no_current_reading` |

  `FcmAndroidNotification(channel_id)` goes; the channel travels in `data`. All values are strings
  (an FCM data constraint). `LoggingPushSender` logs the structured message.
- **Clean cut**: no `notification` block, no version field, no dual format.

**App**:

- `core/push/AlarmNotificationContent.kt` (`commonMain`, pure):

  ```kotlin
  sealed interface AlarmNotificationContent {
      val stationName: String; val channel: NotificationChannelId
      data class Levels(…, val levels: List<Pair<String, PollenSeverity>>, val isAlert: Boolean)
      data class NoPollen(…); data class NotReported(…)
      data class NoCurrentReading(…, val measuredAt: Instant)
      data class Unavailable(…)
      data class Generic(…)   // unknown kind, malformed levels, unknown species/severity, missing measuredAt
  }
  fun parseAlarmPayload(data: Map<String, String>): AlarmNotificationContent
  ```
  Rule: any element the app does not understand degrades the **whole** message to `Generic` —
  never a partial list that drops a type silently. Missing `stationName` falls back to
  `stationAbbr`; missing channel → daily report channel. A payload missing even `stationAbbr` still
  produces `Generic` with an empty station (title "Pollen").
- Rendering text: title "Pollen in {station}" (`notification_title`); bodies from resources, levels
  joined with " · " as "{species}: {severity}" using the same species/severity resources as the
  UI; "Latest from {time}" / "{time} yesterday" / "{d MMMM}" computed with `DateWording` in
  **`Europe/Zurich`** (alarm times are Swiss time, matching the server's former output), relative to
  `swissToday(clock)`. A pure `notificationText(content, wording, strings, now)` is tested with a
  fake string lookup so tests assert on keys + arguments, not on English sentences.
- `PollenFirebaseMessagingService.onMessageReceived`: no longer reads `message.notification`; parses
  `message.data`, resolves strings with the **app's** locale (API 33+: application context already
  carries the per-app locale; below 33: wrap the context with
  `AppCompatDelegate.getApplicationLocales()` via `createConfigurationContext`, since AppCompat
  applies it only to activities), and posts through the existing `NotificationCompat` path, same
  icon, same tap intent, unique id per message. One path for foreground and background.
- Accepted: data-only delivery misses force-stopped apps and is throttled in the "restricted"
  standby bucket (documented in CLAUDE.md "Firebase").

#### Build — `:composeApp:checkTranslations`

- A Gradle task (in `composeApp/build.gradle.kts` or a small `buildSrc`/convention file), wired as a
  dependency of `check`, configuration-cache compatible (inputs declared as files). It parses
  `composeResources/values*/strings.xml` and `androidMain/res/values*/strings.xml` and fails with a
  list of problems when: a key is in one language and not another (either direction, keys with
  `example_` prefix and `translatable="false"` exempt); placeholders (`%s`, `%d`, `%1$s` …) differ
  as a multiset per key; a value is blank. Added to the CLAUDE.md Commands table; also runnable
  standalone.

#### Documentation (CLAUDE.md)

- New "Localization" section (resources layout, language mechanism per platform, style rules,
  `DateWording`, vocabulary lookups, the check, what is not translated).
- Update: Error handling (`AppError`, no message strings in UI state), Bottom navigation bar
  (Settings tab, `ChangeStation`), feature/package tables (`feature/settings`,
  `core/stationpicker`, `core/language`, `core/appinfo`, `core/ui/format`, `core/ui/error`), Firebase
  + Alarm delivery (data-only payload, keys table, rendering on device, delivery trade-off, removal
  of server wording), Persisted user selections (language is not in DataStore — the platform owns
  it), iOS wrapper configuration (`CFBundleLocalizations`, `CFBundleDevelopmentRegion`), Commands
  (`checkTranslations`), Multiplatform gotchas if any new trap is found.

### Automated Testing Decisions

**What makes a good test here:** assert observable behaviour at a module's interface — the state a
ViewModel exposes, the payload a sender posts, the content a parser returns, the text a formatter
produces for given inputs — never private helpers or call order. Tests must not depend on the
English wording where a resource key is the contract (assert keys/arguments or enums, not
sentences), except the date formatter, whose output *is* the contract and is tested per language
with literal fixtures. Hand-written fakes only; app tests in `commonTest`; backtick names without
commas (Kotlin/Native). Pin boundaries from both sides.

| Module | Type | What is pinned | Prior art |
|---|---|---|---|
| `AlarmRules` (server) | unit | each `PushKind` is chosen in exactly the cases the old bodies were; `levels` ordering (worst first, then `PollenSpecies` order); `measuredAt` only for `NO_CURRENT_READING`; every existing timing, staleness, minimum and per-day rule unchanged | `AlarmRulesTest` (rewrite assertions from text to structure) |
| `AlarmScheduler` (server) | unit | delivers the structured message; notification log and token handling unchanged | `AlarmSchedulerTest` |
| `FcmPushSender` (server) | unit, `MockEngine` | request has no `notification`, has `android.priority = HIGH`, the data keys of the table, string values; `Unregistered`/`Failed` mapping unchanged | `FcmPushSenderTest` |
| `parseAlarmPayload` (app) | unit | every kind; `levels` parsing and order preserved; unknown kind / species / severity / malformed `levels` / missing `measuredAt` → `Generic`; missing `stationName` → abbr | `DiaryCodecTest` |
| `notificationText` (app) | unit | title and body keys + arguments per kind; "today" / "yesterday" / earlier date in `Europe/Zurich` at the midnight edge from both sides | `ReadingAgeTest`, `AlarmRulesTest` (staleness forms) |
| `StationPicker` | unit (virtual time) | initial selection from `initialAbbr` (present / absent / not listed); manual pick cancels lookup; timeout → `UNAVAILABLE`; denied → `PERMISSION_DENIED`; errors clear on pick; retry | `OnboardingViewModelTest` (moved/split), `FakeCoarseLocationProvider` |
| `OnboardingViewModel` | unit | still saves and emits `Completed` once; save failure | `OnboardingViewModelTest` |
| `ChangeStationViewModel` | unit | preselects stored station; `canSave` false when unchanged, true after a different pick, false while saving; save → `Done` once; failure → `saveError`; station list failure → `Error(AppError)` + retry | `AlarmEditorViewModelTest` (Done once, gated save) |
| `SettingsViewModel` | unit | station row name resolves from list, falls back to stored name before/without it, updates when the selection changes; language choice delegates to a `FakeLanguageRepository` and sets `languageAppliesOnRestart` only when `appliesImmediately` is false; version passthrough / `null` | `HomeViewModelTest` |
| `AppLanguage.fromTag` | unit | `de`, `de-CH`, `fr-CH`, `it`, `en-GB` → matching; `es`, `null`, `""` → `SYSTEM` | `TopLevelDestinationTest` style |
| `Throwable.toAppError` | unit | transport/timeout → `Network`; 500/502/503 → `ServerUnavailable`; 404 → `NotFound`; each alarm exception → its kind; anything else → `Unknown` | `ResultTest` |
| ViewModels with errors (Home, All stations, Diary, Alarms, Alarm editor) | unit | existing error tests now assert the `AppError` kind instead of a message | their existing tests |
| `DateWording` formatting | unit | for en/de/fr/it fixtures: full date, short date, month-only, weekday short and ranges ("Mo–Fr"), list form ("Sa, So"); `HH:mm` unchanged | `ReadingAgeTest`, `AlarmSummaryTest` |
| `AlarmSummary` | unit | wording driven by keys/`DateWording`; species/severity via lookup | `AlarmSummaryTest` (adapt) |
| `TopLevelDestination` | unit | five tabs, fifth is `SETTINGS` → `Screen.Settings`; `ChangeStation` → no tab | `TopLevelDestinationTest` |
| `checkTranslations` | build check | runs in `check`; verified once by hand with a deliberately removed key and a mismatched placeholder | — |

**Checked by hand (no automated test):** `AndroidLanguageRepository`, `IosLanguageRepository`, the
`AppInfo` actuals, all composables (Settings, language dialog, change-station screen), the push
service's locale-aware rendering and posting, channel renaming after a language change, the
per-app language system screen integration, and the iOS "applies on next launch" behaviour
(compile-verified only).

Run before considering a task done: `./gradlew :composeApp:testDebugUnitTest :server:test`,
`./gradlew :composeApp:compileTestKotlinIosSimulatorArm64`, `./gradlew :composeApp:checkTranslations`.
