# Tasks — settings-and-localization

> Generated from `.sdd/settings-and-localization/requirements.md` against the codebase at `e9eb597` (2026-10-07).
>
> **Annex deviation (resolved with the user — "adapt to the codebase"):** the alarm failure exceptions
> (`PushUnavailableException`, `InvalidAlarmException`, `AlarmLimitReachedException`,
> `AlarmNotFoundException`, `UnknownDeviceException`) stay in `feature/alarms`, because `core/` may not
> import a feature. `core/result` owns the `AppError` type and a generic `toAppError()` (transport,
> HTTP status, unknown); the generic status exception moves from `feature/alarms`
> (`BackendStatusException`) to `core/network` as `HttpStatusException(status)` and every API service
> uses it; `feature/alarms` has its own `toAlarmAppError()` that maps its exceptions and delegates the
> rest.
>
> **Refinements found while slicing** (within the annex's intent): `AppError` gains the
> app-produced conditions the annex list did not name — `NoStationSelected` (Home, Diary),
> `NoReadings` (All stations, every station unavailable), `NoStations` (empty station list) —
> `AlarmNotFoundException` maps to `NotFound` and `UnknownDeviceException` (after the repository's
> one retry) to `Unknown`. Home's `feelingSaveError` is a local storage failure, not a network one,
> and becomes a `Boolean` like onboarding's `saveError`. The manifest theme is already
> `Theme.AppCompat.DayNight.NoActionBar`, so `AppCompatActivity` needs no theme change.
>
> **Common quality gates** apply to every task in addition to its own:
> `./gradlew :composeApp:testDebugUnitTest :server:test` green;
> `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` green;
> `./gradlew :composeApp:checkTranslations` green (from task 01 on);
> no new compiler warnings in touched modules; test names are backtick sentences without commas;
> CLAUDE.md updated for every behaviour or structure the task changes.
>
> **Hard-coded text grep** (used by tasks 06–08), run from the repo root:
> `grep -rnE '"[A-Z][a-z]+( [a-z]+)+' composeApp/src/commonMain composeApp/src/androidMain composeApp/src/iosMain --include=*.kt`
> with comment/KDoc lines and `feature/example` excluded; every remaining hit must be on a reviewed
> allowlist kept in the task's notes (ids, URLs, log/exception text that is never shown).

## Task 01-settings-tab-with-about-sections

The fifth tab becomes Settings: a scrolling screen titled "Settings" with the Impressum, the
MeteoSwiss data-source section and the app version — the first screen whose text comes from string
resources, in all four languages, guarded by the translation check. The Default station and Language
rows come in later tasks.

### Implementation steps

- [x] Add the Compose resources directories (`values`, `values-de`, `values-fr`, `values-it`) and load strings with `stringResource`.
- [x] Add the `checkTranslations` Gradle task over `composeResources/values*/strings.xml` **and** `androidMain/res/values*/strings.xml`: key parity in both directions, placeholder parity per key, no blank values, no "ß" in any `values-de` file; `example_` keys and `translatable="false"` exempt. Make `check` depend on it; add it to the CLAUDE.md Commands table.
- [x] Rename `FEATURE_5` / `Screen.Feature5` to `SETTINGS` / `Screen.Settings` with the settings icon and a resource-backed content description; update `TopLevelDestinationTest`.
- [x] Add `AppInfo` / `AppVersion` with Android (`PackageManager`) and iOS (`NSBundle`) actuals bound in `platformModule`.
- [x] Build `SettingsViewModel` (version only for now) and `SettingsScreen`: top bar "Settings", section titles as headings, Impressum ("Developed by Tim Stenzel", tappable `developer.mobile.t3s@gmail.com` → `mailto:` with subject "PollenInfo"), Data source (prescribed attribution per language, per-language MeteoSwiss link, independence disclaimer), version caption "Version {name} ({code})".
- [x] Write the de/fr/it translations for these strings (Swiss Standard German with "du", formal fr/it, French typography).
- [x] Delete `ComingSoonScreen` if nothing references it any more; update CLAUDE.md (bottom navigation, a new "Localization" section skeleton, `core/appinfo`, `feature/settings`).

### Acceptance criteria

- [x] `TopLevelDestinationTest` asserts five tabs with `SETTINGS` → `Screen.Settings` last.
- [x] `SettingsViewModelTest` asserts the version comes from a fake `AppInfo` and is `null` (line hidden) when `AppInfo` has none.
- [x] On the emulator: the fifth tab opens a screen titled "Settings" showing Impressum, Data source (attribution, link and the independence statement) and "Version 1.0.0-debug (1)"; back from Settings shows Home; leaving and returning to the tab keeps the scroll position.
- [x] Tapping the email opens a mail draft to `developer.mobile.t3s@gmail.com` with subject "PollenInfo"; tapping the link opens `www.meteoswiss.admin.ch`; with the emulator's system language German the attribution reads "Quelle: MeteoSchweiz" and the link opens `www.meteoschweiz.admin.ch` (French "Source: MétéoSuisse" / `meteosuisse`, Italian "Fonte: MeteoSvizzera" / `meteosvizzera` likewise).
  *(Verified 2026-10-07 on API 36: the tap sends `VIEW mailto:developer.mobile.t3s@gmail.com?subject=PollenInfo` to Gmail's `ComposeActivityGmailExternal`; the emulator's Gmail has no account and shows its welcome tour instead of the draft. Languages verified by switching the emulator's system language to de-CH, fr-CH, it-CH.)*
- [x] `./gradlew :composeApp:assembleRelease` (or the release variant's merged manifest/`PackageManager` values) shows `versionName` "1.0.0" and `versionCode` 1, so the release line reads "Version 1.0.0 (1)".
- [x] Removing one key from `values-it`, introducing a mismatched placeholder, or adding an "ß" to a `values-de` value each makes `checkTranslations` fail naming the problem (verified by hand, then reverted).

### Quality gates

- [x] Common quality gates.
- [x] `./gradlew :composeApp:assembleDebug` succeeds with the configuration cache enabled (the new task is configuration-cache compatible).

## Task 02-typed-app-errors

Every screen's error message becomes a translated, human sentence chosen by the kind of failure —
no connection, server problem, not found, no station chosen, no readings, no stations, push
unavailable, alarm limit, invalid alarm, unknown. UI states carry an `AppError` instead of a message
string, so no raw technical text reaches the screen anymore. Comes before the station picker so the
picker is built on `AppError` from the start.

### Implementation steps

- [x] Add `AppError` (`Network`, `ServerUnavailable`, `NotFound`, `NoStationSelected`, `NoReadings`, `NoStations`, `PushUnavailable`, `AlarmLimitReached`, `InvalidAlarm`, `Unknown`) and `Throwable.toAppError()` (IO/transport/timeout → `Network`, `HttpStatusException` 5xx → `ServerUnavailable`, 404 → `NotFound`, else `Unknown`) in `core/result`.
- [x] Add `HttpStatusException(status)` in `core/network`, replacing `BackendStatusException`, and make every API service check the status before `body()` (station, measurement, history, species, alarms; history already checks — switch it to the new exception).
- [x] Add `toAlarmAppError()` in `feature/alarms`: `PushUnavailableException` → `PushUnavailable`, `InvalidAlarmException` → `InvalidAlarm`, `AlarmLimitReachedException` → `AlarmLimitReached`, `AlarmNotFoundException` → `NotFound`, `UnknownDeviceException` → `Unknown`, everything else delegated.
- [x] Replace message strings with `AppError` in Home (`NO_STATION_MESSAGE` → `NoStationSelected`; `feelingSaveError` → `Boolean`), All stations (`NO_READINGS_MESSAGE` → `NoReadings`), Diary, Alarms (`Error(message, pushUnavailable)` → `Error(AppError)`, `ToggleFailed(message)` → `ToggleFailed(error, enabling: Boolean)`), Alarm editor (`saveError: AppError?`, "No stations available" → `NoStations`) and Onboarding; remove the `DEFAULT_*_ERROR` constants.
- [x] Add `AppError.message(context)` in `core/ui/error` with load / save / delete / toggle wording, translated into all four languages; screens map `AppError` (and Home's feeling-save flag) to text.
- [x] Update CLAUDE.md "Error handling" and the UI-state descriptions that mention messages.

### Acceptance criteria

- [x] `AppErrorTest` pins: an IO exception and a request timeout → `Network`; `HttpStatusException` 500, 502 and 503 → `ServerUnavailable`; 404 → `NotFound`; any other exception → `Unknown`.
- [x] An alarms test pins `toAlarmAppError()` for each of the five alarm exceptions and delegation for any other.
- [x] `MockEngine` tests for the station, measurement and species services pin that a `502` response fails with `HttpStatusException(502)` rather than a deserialization error.
- [x] The existing error tests of `HomeViewModelTest`, `AllStationsViewModelTest`, `DiaryViewModelTest`, `AlarmsViewModelTest`, `AlarmEditorViewModelTest` and `OnboardingViewModelTest` assert an `AppError` kind (or Home's feeling-save flag) instead of a message.
- [x] `grep -rnE '(message|[eE]rror): String' --include='*UiState.kt' --include='*Event.kt' composeApp/src/commonMain` returns nothing outside `feature/example`, and `grep -rn 'exception.message' composeApp/src/commonMain` returns nothing under any `presentation/` outside `feature/example`.
- [x] On the emulator: in airplane mode Home shows the translated "couldn't reach the server" sentence; with a backend answering `502` (stop upstream access or stub) it shows the "service is having problems" sentence instead; saving an eleventh alarm (ten seeded through the API for the same device id) shows the translated limit message; no host name, status code or library text appears in any of them.
  *(Verified 2026-10-08 on API 36 with the app locale set to de-CH: airplane mode → "Der Server ist nicht erreichbar. Prüfe deine Internetverbindung …"; a stub on :8080 answering `502` → "Der Dienst hat gerade Probleme. …"; nine alarms seeded through the API, editor opened, a tenth seeded, Save → "Der Alarm konnte nicht gespeichert werden. Du hast die maximale Anzahl Alarme erreicht. …". The screens' headings and "Retry" are still English until task 06. Seeded alarms deleted afterwards.)*

### Quality gates

- [x] Common quality gates.

## Task 03-change-default-station

From Settings the user can change the default station on a new screen that works like onboarding.
The picker (station list, use-my-location with timeout and cancellation, dropdown, location errors)
moves from onboarding into `core/stationpicker`, so onboarding and the new screen share one tested
implementation; onboarding's text moves to string resources in all four languages on the way.

### Implementation steps

- [x] Move `FindNearestStationUseCase` (and its test), `LocationError` and the selection/location logic out of `OnboardingViewModel` into `core/stationpicker` as a picker state holder that takes an initial selection and reports failures as `AppError`.
- [x] Move the picker composables (location button, dropdown, location error text) into `core/stationpicker/presentation` and embed them in onboarding; move all onboarding strings to resources and translate them.
- [x] Add `Screen.ChangeStation` (no tab, bottom bar hidden), `ChangeStationViewModel` (reads the stored station once for the preselection, `canSave` only for a different station and not while saving, `Done` on a buffered channel, `saveError` on failure, `Error(AppError)` + retry when the list fails) and `ChangeStationScreen` (top bar "Default station" with back, Save with progress).
- [x] Add the Default station section as the first section of Settings: observes `SelectedStationRepository`, resolves the name from `StationRepository`, falls back to the stored name; tapping navigates to `ChangeStation`.
- [x] Translate all new strings; update CLAUDE.md (onboarding, `core/stationpicker`, navigation, Settings).

### Acceptance criteria

- [x] Picker tests (moved from `OnboardingViewModelTest`, virtual time) pin: initial selection present / absent / not listed; a manual pick cancels a running lookup; timeout → `UNAVAILABLE`; denial → `PERMISSION_DENIED`; both errors clear on a pick; a failed list is `Error(AppError)` and retry reloads it.
- [x] `OnboardingViewModelTest` still pins save → `Completed` exactly once and a failed save.
- [x] `ChangeStationViewModelTest` pins: stored station preselected; `canSave` false when unchanged, true after picking another, false while a gated save runs; success → `Done` exactly once and the fake repository holds the new station; failure → `saveError` and no `Done`.
- [x] `SettingsViewModelTest` pins the station row: stored name before the list arrives, the list's name after, the new name after the selection changes; `TopLevelDestinationTest` asserts `Screen.ChangeStation` maps to no tab.
- [x] On the emulator: Zürich → Bern from Settings returns to Settings showing Bern and Home shows Bern's readings; back without saving keeps Zürich; an existing Lugano alarm still shows Lugano; a new alarm starts on Bern; a Diary showing Lugano (picked in its dropdown) still shows Lugano.
- [x] On the emulator with the backend stopped, ChangeStation shows a translated error with Retry, and Retry loads the list once the backend runs again.
  *(Verified 2026-10-08 on API 36. Zürich → Bern: Save disabled until Bern was picked, Settings then showed Bern and Home loaded Bern (backend logged `PBE/measurements`); back without saving kept Zürich; the Lugano alarm (created for the test) still read Lugano; a new alarm opened on Bern; the Diary set to Lugano still showed Lugano. Backend stopped: "The station list could not be loaded." + "Couldn’t reach the server. …" + Retry, and in de-CH "Die Stationsliste konnte nicht geladen werden." + "Der Server ist nicht erreichbar. …" + "Erneut versuchen"; Retry after restarting the backend loaded the list with Bern preselected. Default station, app locale and alarms restored afterwards.)*

### Quality gates

- [x] Common quality gates.
- [x] `grep -rn 'feature.onboarding' composeApp/src/commonMain/kotlin/ch/stenzel/tim/polleninfo/feature/settings` and the reverse return nothing.

## Task 04-app-language-switching

The Settings Language row lets the user choose System default, English, Deutsch, Français or
Italiano. On Android the choice applies immediately and is the same setting as the system's per-app
language screen; on iOS it is written for the next launch with a note saying so. Everything
translated so far (Settings, change station, onboarding, error messages) and the Android notification
channel names follow the choice.

### Implementation steps

- [x] Add `core/language`: `AppLanguage` with tags and a pure `fromTag`, the `LanguageRepository` interface, and `FakeLanguageRepository` in `commonTest`.
- [x] Android: add `androidx.appcompat` explicitly in `androidMain`, make `MainActivity` an `AppCompatActivity`, add `locales_config.xml` + `android:localeConfig`, the AppCompat locales metadata service with `autoStoreLocales`, and `AndroidLanguageRepository`; bind in `platformModule`.
- [x] Android: add one helper that returns a context localized to the app's chosen language (the application context on API 33+, a `createConfigurationContext` wrapper from `AppCompatDelegate.getApplicationLocales()` below) — task 09's push service reuses it.
- [x] Move the notification channel names to Android string resources (`values`, `-de`, `-fr`, `-it`), create the channels through the localized context, and re-create them after a language change so their names update.
- [x] iOS: `IosLanguageRepository` writing/removing `AppleLanguages`; bind in `platformModule`.
- [x] Settings: the Language section (second, after Default station) shows the current choice; a radio dialog lists the five options, each named in its own language, with the current one marked; the "applies the next time you open the app" note shows when the platform does not apply immediately.
- [x] Update CLAUDE.md: "Localization" (mechanism per platform, System default fallback, style rules), "Persisted user selections" (the language is owned by the platform, not DataStore), "iOS wrapper configuration" (`CFBundleLocalizations` en/de/fr/it, `CFBundleDevelopmentRegion` en).

### Acceptance criteria

- [x] `AppLanguageTest` pins `fromTag`: `de`, `de-CH`, `fr-CH`, `it`, `en-GB` map to their language; `es`, `null` and `""` map to `SYSTEM`.
  *(AppLanguageTest: 5 tests, 0 failures — also `rm-CH` → SYSTEM and every language round-trips its own tag.)*
- [x] `SettingsViewModelTest` pins: the current language comes from the repository; selecting one calls `set`; the restart note is shown only when the repository does not apply immediately.
  *(SettingsViewModelTest: 15 tests, 0 failures — 7 new: language from the repository, dialog open/close, select → `set` and close, current language → no `set`, no note when immediate, note only after a change when not immediate, re-read on resume.)*
- [x] On an Android 13+ emulator: the dialog marks the current choice; choosing Français switches Settings to French at once, still on Settings, and returning to Home shows its readings without a full-screen reload; the system per-app language screen then shows Français, and changing it there to Deutsch is reflected in the app's Language row.
  *(Verified 2026-10-08 on API 36: the dialog marked "System default"; Français switched Settings to French at once, still on Settings (`cmd locale get-app-locales` → `[fr]`); Home then showed its readings with "Refreshed 21:02" unchanged — no reload; the system "App language" screen had Français selected and listed exactly Deutsch / English / Français / Italiano; choosing Deutsch (Schweiz) there gave `[de-CH]` and the app's Language row read "Deutsch".)*
- [x] On a fresh install with the emulator in German, onboarding appears in German; with the emulator in Spanish, in English.
  *(Verified 2026-10-08 on API 36: system `de-CH`, uninstall + install → "Willkommen bei PollenInfo … Weiter"; system `es-US,en-US`, uninstall + install → "Welcome to PollenInfo … Continue". Emulator restored to `en-US` and re-onboarded on Basel; the uninstalls wiped the emulator's diary answers and device id.)*
- [ ] ~~After switching the app to German, Android's notification settings list the two channels with German names — verified on an API 33+ and an API < 33 emulator.~~ *(skipped: only an API 36 system image is installed and there is no `sdkmanager` to fetch an API < 33 one. The API 33+ half passed on 2026-10-08: with the app in German, Android's notification settings listed "Tagesberichte" and "Grenzwert-Warnungen".)*
- [x] On Settings the sections read top to bottom: Default station, Language, Impressum, Data source, version.
  *(Verified on API 36: Default station, Language, Impressum, Data source, then "Version 1.0.0-debug (1)".)*

### Quality gates

- [x] Common quality gates.
  *(`./gradlew :composeApp:testDebugUnitTest :server:test :composeApp:compileTestKotlinIosSimulatorArm64 :composeApp:checkTranslations :composeApp:assembleDebug --rerun-tasks` → BUILD SUCCESSFUL; 576 app + 340 server tests, 0 failed; no compiler warnings besides the pre-existing KLIB resolver notes; new test names have no commas; CLAUDE.md updated.)*
- [x] The merged debug manifest contains `android:localeConfig` and the AppCompat locales metadata service with `autoStoreLocales` set to `true`.
  *(`build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml` has `android:localeConfig="@xml/locales_config"` and `AppLocalesMetadataHolderService` with `autoStoreLocales` = `true`.)*

## Task 05-localized-vocabulary-and-dates

Pollen-type names, severity words, feeling words, month and weekday names and date formats appear
in the app's language everywhere they are shown — Home, All stations, Diary (filters, axis labels,
chart description), alarm list summaries and the alarm editor's chips — while times stay 24-hour and
station names stay as published. Each lookup also has a non-composable form so notifications can use
it later.

### Implementation steps

- [x] Add species-name resources keyed by species id with a composable lookup that falls back to the server's name for an unknown id, plus `speciesNameResource(id): StringResource?` for non-composable callers; use it everywhere a species name is shown.
- [x] Make `PollenSeverity.label()`, `minimumLabel()` and `Feeling.label()` resource-backed, each with a `StringResource` accessor usable outside Compose.
- [x] Add `core/ui/format`: a pure `DateWording` (month and weekday names full/short, per-language date forms) with pure formatting functions, built both by a composable (`rememberDateWording()`) and by a `suspend` builder using `getString` (for the push service).
- [x] Use it in `ReadingAgeLabel` (full date), `DiaryChart` (short date, month only), `AlarmSummary` / `daysSummary` and the editor's day chips (short labels, spoken full names), replacing the English name tables.
- [x] Translate all of it; update CLAUDE.md "Localization" (vocabulary lookups, `DateWording`).

### Acceptance criteria

- [x] `DateWordingTest` pins, for literal en/de/fr/it fixtures: "29 July" / "29. Juli" / "29 juillet" / "29 luglio"; "4 Sep" / "4. Sep." / "4 sept." / "4 set"; the month-only form; the weekday range ("Mon–Fri" / "Mo–Fr" / "lun–ven" / "lun–ven") and short list ("Sat, Sun" / "Sa, So").
  *(DateWordingTest: 14 tests, 0 failures — full, short, month-only, weekday short/full, ranges "Mon–Fri" / "Mo–Fr" / "lun–ven" / "lun–ven", lists "Sat, Sun" / "Sa, So" / "sam, dim" / "sab, dom", `HH:mm`, and the 12/7-name guard.)*
- [x] `AlarmSummaryTest` and `ReadingAgeLabelTest` pass against `DateWording` and the label lookups, and every time they produce is `HH:mm`.
  *(AlarmSummaryTest 19, ReadingAgeLabelTest 8 tests, 0 failures — English, German, Italian and French fixtures; every time `HH:mm`.)*
- [x] A test pins that an unknown species id falls back to the given server name.
  *(SpeciesNamesTest: 4 tests, 0 failures — `MUGWORT` → "Mugwort" without resolving anything.)*
- [x] On the emulator in German: Home shows "Birke", "Gräser" and "Mässig"; the Diary year view shows German month labels; in French an alarm summary shows the French weekday range and severity words; times everywhere are 24-hour; station names read e.g. "Genève", "Zürich" in every language.
  *(Verified 2026-10-08 on API 36. German: Home showed "Erle", "Birke", "Gräser", "Keine" and "Driven by Erle"; no station had anything above None today, so "Mässig" was checked on the Diary's severity axis, drawn by the same `PollenSeverity.label()`; the Diary year axis read "Dez. Feb. Apr. Juni Aug. Okt." — the German labels first overlapped, fixed with `dateLabelStep`. French, two alarms seeded through the API: "Threshold alert 07:00–21:30 · lun–ven · Bouleau, Graminées ≥ Élevé" (Genève) and "Daily report at 08:00 · sam, dim · Aulne ≥ Modéré" (Zürich); the editor's day chips read lun…dim, announced lundi…dimanche. Times 24-hour throughout. Alarms deleted, app language and notification permission reset afterwards.)*
- [x] `grep -rnE 'MonthNames\.ENGLISH|DayOfWeekNames\.ENGLISH' composeApp/src/commonMain` returns nothing.
  *(No output, exit 1.)*

### Quality gates

- [x] Common quality gates.
  *(`./gradlew :composeApp:testDebugUnitTest :server:test :composeApp:compileTestKotlinIosSimulatorArm64 :composeApp:checkTranslations :composeApp:assembleDebug --rerun-tasks` → BUILD SUCCESSFUL; 602 app + 340 server tests, 0 failed; no compiler warnings besides the KLIB resolver notes; no test name has a comma; CLAUDE.md updated. `checkTranslations` now also covers `string-array` item counts — verified by deleting one Italian weekday: "'date_weekdays_short' has 6 items, English has 7", then reverted.)*

## Task 06-translate-home-all-stations-and-navigation

Home (including the feeling prompt and reading-age captions), All stations (including the map's
spoken description and the stale icon) and the bottom navigation bar are fully in the app's
language, with all accessibility text translated.

### Implementation steps

- [ ] Move every user-visible and spoken string of `feature/home`, `feature/allstations`, `core/ui/severity`, `core/ui/feeling` and `navigation/` to resources (plurals where a count appears).
- [ ] Translate them (Swiss Standard German with "du", formal fr/it, French typography).
- [ ] Update CLAUDE.md where it quotes these strings as English-only facts.

### Acceptance criteria

- [ ] On the emulator in German the feeling prompt asks "Wie fühlst du dich heute?"; in French it uses "vous" with a narrow non-breaking space before "?".
- [ ] With TalkBack in Italian, the five tabs, the map and a stale row's icon are announced in Italian; the map's sentence is the Italian translation of "Map of 15 pollen stations. Select a station in the list below." with the count 15.
- [ ] Home's stale warning and "Refreshed" caption appear in each of the four languages with the localized date.
- [ ] The hard-coded text grep (see header), restricted to `feature/home`, `feature/allstations`, `core/ui` and `navigation`, has no hits outside the allowlist.

### Quality gates

- [ ] Common quality gates.

## Task 07-translate-diary

The Diary screen — station dropdown, range buttons, intro line, chart (axis words, empty-state hint,
spoken description), pollen-type filters ("Not measured here") and the medical note — is fully in the
app's language.

### Implementation steps

- [ ] Move every user-visible and spoken string of `feature/diary` to resources, with plurals for counts.
- [ ] Keep `diaryChartDescription` pure: it returns a resource key with arguments (count, range) or takes its wording as a parameter; adapt its tests in `DiaryChartGeometryTest`.
- [ ] Translate them.

### Acceptance criteria

- [ ] The `diaryChartDescription` tests in `DiaryChartGeometryTest` assert the key and arguments (count, range) for week, month and year instead of English sentences.
- [ ] On the emulator in French the Diary's texts, buttons, filters, empty-state hint and medical note are French.
- [ ] With TalkBack in German the chart is announced in German with the correct count and range.
- [ ] The hard-coded text grep (see header), restricted to `feature/diary`, has no hits outside the allowlist.

### Quality gates

- [ ] Common quality gates.

## Task 08-translate-alarms-and-example

The alarm list (permission gate, rows, limit hint, snackbar, FAB) and the alarm editor (type toggle,
sections, time pickers, dialogs, hints) are fully in the app's language. The reference example
feature reads its (English-only) text from resources so it shows the pattern. After this task no
hard-coded user-facing string remains anywhere in the app.

### Implementation steps

- [ ] Move every user-visible and spoken string of `feature/alarms` (incl. `summaryOf` wording) and `core/notifications` to resources and translate them.
- [ ] Move `feature/example` strings to `example_`-prefixed keys in `values/` only.
- [ ] Run the hard-coded text grep over `commonMain`, `androidMain` and `iosMain`; convert every remaining user-facing hit and record the reviewed allowlist.
- [ ] Complete CLAUDE.md "Localization" (what is and is not translated, key naming, the `feature/example` exemption, the grep and its allowlist).

### Acceptance criteria

- [ ] `AlarmSummaryTest` asserts keys/arguments or a fake wording, not English sentences, for "Paused", "Every day", "All pollen types" and both alarm types.
- [ ] On the emulator in Italian, the permission explanation, the list, the editor, its dialogs, the limit hint and the toggle-failed snackbar are Italian, and TalkBack announces the switch and the day chips in Italian.
- [ ] The hard-coded text grep (see header) over `commonMain`, `androidMain` and `iosMain` has no hits outside the allowlist.
- [ ] `checkTranslations` passes with the `example_` keys present only in English.

### Quality gates

- [ ] Common quality gates.

## Task 09-localized-push-notifications

Alarm notifications are written on the phone in the app's language. The backend sends a data-only
FCM message with what happened (kind, station, alarm, ordered levels, reading time) instead of text;
the app parses it, builds title and body from the same resources as the screens, and posts it the
same way whether the app is open or not. Anything it does not understand becomes a generic
"Pollen in <station>" notification.

### Implementation steps

- [ ] Server: replace `PushMessage.title/body` with `kind` (`report`, `no_pollen`, `not_reported`, `no_current_reading`, `unavailable`, `alert`), station, alarm id, ordered `levels` and optional `measuredAt`; `AlarmRules` keeps every decision rule and drops all wording; `LoggingPushSender` logs the structure.
- [ ] Server: `FcmPushSender` sends data-only (no `notification` block), Android priority high, string data keys `kind`, `channel`, `stationAbbr`, `stationName`, `alarmId`, `levels` (`BIRCH:HIGH,GRASSES:MODERATE`), `measuredAt`.
- [ ] App: pure `parseAlarmPayload(data)` → `AlarmNotificationContent` (any unknown element → `Generic`), and a pure notification-text builder over keys/arguments using `DateWording` in `Europe/Zurich` relative to `swissToday`.
- [ ] App: `PollenFirebaseMessagingService` renders from `message.data` through the localized context from task 04 and the non-composable lookups from task 05, and posts on the payload's channel; one path for foreground and background, same icon and tap.
- [ ] Translate the notification strings; update CLAUDE.md (Firebase, Alarm delivery: payload table, device rendering, delivery trade-off, removed server wording).

### Acceptance criteria

- [ ] `AlarmRulesTest` pins each kind in exactly the cases the old bodies covered, the `levels` order (worst first, then `PollenSpecies` order) and `measuredAt` only for `no_current_reading`; all existing timing, staleness, minimum and per-day tests still pass.
- [ ] `AlarmSchedulerTest` pins that the scheduler delivers the structured message, with notification-log and token handling unchanged.
- [ ] `FcmPushSenderTest` pins a request with no `notification`, `android.priority` high and the data keys above as strings; the `Unregistered` / `Failed` mapping is unchanged.
- [ ] `parseAlarmPayload` tests pin every kind and the `Generic` fallback for an unknown kind, species or severity, malformed `levels` and a missing `measuredAt`; notification-text tests pin title and body keys/arguments per kind and "today" / "yesterday" / earlier date at the Swiss midnight edge from both sides.
- [ ] On the emulator with the app in German, a daily report and a threshold alert — delivered by `:server:run` with `FCM_CREDENTIALS` set, or by a `curl` to FCM HTTP v1 with the new data-only payload (the Firebase console cannot send data-only) — appear in German, identical in foreground and background, and tapping opens the app; a payload with an unknown `kind` shows the generic German notification.

### Quality gates

- [ ] Common quality gates.
- [ ] `grep -rnE '"[^"]*(Pollen in|No current reading|yesterday|No pollen of)' server/src/main/kotlin/ch/stenzel/tim/polleninfo/server/alarm` returns nothing.
