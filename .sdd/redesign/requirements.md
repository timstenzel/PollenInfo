# Redesign ("Meadow")

## Problem Statement

PollenInfo works, but it looks like a stock Material app: grey-green default colours (or, on newer
Android phones, whatever colours the user's wallpaper produces), the system font, a title bar on
every screen that takes space without adding information, a plain bottom bar and plain lists. Nothing
about the app says "pollen", "plants" or "nature", and it does not feel as modern or as carefully
made as comparable pollen apps (aha! Pollen, Pollenflug & Allergie Alarm). Loading shows a bare
spinner and errors are a line of text with a button, so the moments where the app waits or fails
feel unfinished.

## Solution

Every screen of the app gets a new visual design, called "Meadow", as drawn and approved in the
Figma file *PollenInfo Redesign* (pages "Direction & Foundations", "Components", "Screens"). The
data, the behaviour, the texts and the navigation stay exactly as they are; only the look changes.

- **Natural, calm colours:** meadow-cream backgrounds with soft sage washes, deep forest green as
  the brand colour and pollen gold for the single main action of a screen. A matching dark theme.
  The app always uses its own colours — it no longer adopts the wallpaper colours on Android.
- **Fancier type:** an organic serif (Fraunces) for screen titles and the big severity word, a
  modern sans serif (Plus Jakarta Sans) for everything else. Both fonts ship with the app.
- **No top bar:** each screen opens with a small overline and a large serif title inside the
  content. Screens that are not tabs show a round, floating back button instead.
- **Glass bottom bar:** the tab bar has rounded upper corners, is partly see-through with a blur, and
  content scrolls visibly behind it. Screens that are not tabs show their main action (Continue,
  Save) in a matching glass bar at the bottom.
- **One hero per screen:** Home leads with the worst severity as a large word inside a half-circle
  gauge of four segments; the pollen types become a two-column grid of tiles.
- **Cards, tiles and chips** with generous rounded corners, pale-sage icon tiles, pill-shaped chips
  and segmented toggles throughout, and restyled dialogs, text fields, the time picker and snackbars.
- **Designed waiting and failure:** loading shows a skeleton of the screen's layout with a moving
  shimmer; errors show a card with an illustration, the existing wording and "Retry"; partial and
  action errors use banners and snackbars in the new style.

Severity keeps its rules: the same five levels and fill stops, colour never alone (every bar and dot
sits beside its word), and the same severity and species colours — except a slightly darker
"Moderate" in the light theme so it stays readable on the new backgrounds.

## User Stories

### Look and feel

1. As an app user, I want the app to use calm, natural greens and creams, so that it feels connected to plants and pollen.
2. As an app user, I want the app to keep its own colours on every phone, so that it looks the same regardless of my wallpaper.
3. As an app user, I want a dark theme in the same style, so that the app is comfortable to use at night.
4. As an app user, I want the dark theme to follow my phone's setting as today, so that I do not have to choose it.
5. As an app user, I want large, elegant titles, so that I immediately know which screen I am on.
6. As an app user, I want the same fonts on Android and iPhone, so that the app looks the same on both.
7. As an app user, I want rounded cards and soft backgrounds, so that the app feels modern and friendly.
8. As an app user, I want the main action of a screen to stand out, so that I know what to do next.
9. As an app user, I want the screens to use the full height of my phone instead of a title bar, so that I see more content at once.

### Navigation

10. As an app user, I want the bottom bar to keep its five icon-only tabs in the same order, so that I find everything where it was.
11. As an app user, I want the bottom bar to have rounded upper corners and let content show through softly, so that the screen feels open and light.
12. As an app user, I want the selected tab highlighted as a green pill, so that I can see where I am at a glance.
13. As an app user, I want to scroll content behind the bottom bar and still reach the last item, so that nothing is hidden for good.
14. As an app user, I want a back button on every screen that is not a tab, so that I can always leave it without a title bar.
15. As an app user, I want the system back gesture to behave as today, so that the redesign does not change how I move through the app.
16. As a screen-reader user, I want the tabs and the back button announced with the same names as today, so that the redesign does not make the app harder to use.

### Home

17. As an app user, I want my station's worst severity as a large word inside a half-circle gauge, so that I grasp today's pollen load at once.
18. As an app user, I want the gauge to fill one to four segments for Low to Very high and none for None, so that the level is readable even at a glance.
19. As an app user, I want the gauge to say which pollen type drives the level, so that I know what to watch.
20. As an app user, I want the reading time and the refresh time shown under the gauge, so that I know how current the data is.
21. As an app user, I want an outdated reading to be marked with a warning banner and a warning-coloured time, so that I do not trust old data by mistake.
22. As an app user, I want the "How do you feel today?" question as a green card with four face buttons and "Not today", so that answering is quick and inviting.
23. As an app user, I want every pollen type as a tile with its icon, name, level word, concentration and bar, so that I can scan all seven types quickly.
24. As an app user, I want types my station does not report to say so in their tile, so that I do not mistake them for "None".
25. As an app user, I want pull-to-refresh to keep working with a visible indicator, so that I can update the reading.

### All stations

26. As an app user, I want a map card of Switzerland with a coloured dot per station and a legend naming every level, so that I see the national picture.
27. As an app user, I want the selected station highlighted on the map with its name and level, so that I know which dot I picked.
28. As an app user, I want the station list as rounded rows with a severity dot, the level and the driving type, so that I can compare stations.
29. As an app user, I want rows that are still loading or have no reading to say so with a grey dot, so that I can tell them from real levels.
30. As an app user, I want outdated rows to keep their warning icon, so that I see which readings are not current.
31. As an app user, I want an expanded station row to show that station's readings in the new style, so that the details look like the rest of the app.

### Diary

32. As a logged-out user, I want the diary to explain in a friendly card why it needs an account, with "Create account" and "Log in", so that I understand the next step.
33. As a diary user, I want the station dropdown and the Week / Month / Year toggle above the chart, so that I can change what I compare.
34. As a diary user, I want the chart in a rounded card with my feeling as a dashed green line and each pollen type in its own colour, so that I can compare them.
35. As a diary user, I want the pollen types as tick chips in their line colour, with "Not measured here" where it applies, so that I can choose the lines.
36. As a diary user, I want the medical disclaimer kept, so that I am not misled.

### Alarms and the alarm editor

37. As an app user, I want each alarm as a card with an icon, its summary, its station, weekday circles, its criteria and a switch, so that I can understand an alarm at a glance.
38. As an app user, I want paused alarms to look dimmed and say "Paused", so that I can tell them apart.
39. As an app user, I want the number of alarms out of ten shown, so that I know how many I can still create.
40. As an app user, I want "Create alarm" as a gold floating button, so that creating an alarm is easy to find.
41. As an app user, I want the "notifications are disabled" and "no alarms yet" situations shown as illustrated cards with their button, so that I know what to do.
42. As an app user, I want to choose between "Daily report" and "Threshold alert" as two large cards, so that the choice is clear.
43. As an app user editing an existing alarm, I want the other type shown locked, so that I understand the type cannot change.
44. As an app user, I want stations, pollen types, minimum levels, days and times in the new field, chip and circle styles, so that the editor matches the rest of the app.
45. As an app user, I want the time picker in the new style with the 24-hour dial, so that picking a time feels consistent.
46. As an app user, I want "Save" in a glass bar at the bottom of the editor and a save error shown right above it, so that the result of saving is where I look.
47. As an app user, I want "Delete alarm" as a red outlined button and the discard and delete questions as restyled dialogs, so that destructive actions are clearly marked.
48. As an app user, I want a failed switch to flip back and explain itself in a snackbar, so that I know the alarm did not change.

### Onboarding, change station, account and settings

49. As a first-time user, I want a welcoming first screen with a Swiss map illustration, the station dropdown, "Use my location" and "Continue", so that setup feels inviting.
50. As a first-time user, I want a failed station list shown as an error card with "Retry", so that I can recover.
51. As an app user, I want "Default station" in the new style with its explanation, picker and location note, so that changing it matches the rest of the app.
52. As an app user, I want the log-in and create-account forms in the new field style, with the password eye, hints, errors, the no-recovery warning and the consent checkbox, so that signing up is clear.
53. As an app user, I want a failed log-in explained in a banner under the fields, so that I see why it failed.
54. As an app user, I want Settings as an account card followed by grouped rows with icon tiles, so that settings are easy to scan.
55. As an app user, I want the log-out and delete-account confirmations as restyled dialogs, the latter with its password field, so that I confirm consciously.
56. As an app user, I want a failed crash-report setting explained in a snackbar, so that I know my choice was not saved.
57. As an app user, I want the MeteoSwiss attribution, the disclaimer, the impressum and the version line kept, so that the legal information stays available.

### Waiting and errors

58. As an app user, I want every screen that loads data to show a skeleton of its layout with a moving shimmer, so that I see what is coming instead of a blank spinner.
59. As an app user, I want a screen that failed to load to keep its title and show an illustrated card with the reason and "Retry", so that I understand what happened and can try again.
60. As an app user, I want the same error wording as today, so that messages stay accurate and translated.

### Accessibility and quality

61. As a user with low vision, I want every severity and pollen-type colour to stay clearly visible against the backgrounds it appears on, so that bars, dots and lines are readable.
62. As a user with colour-vision deficiency, I want every bar, dot and gauge to sit beside its level word, so that colour never carries the meaning alone.
63. As a screen-reader user, I want the gauge, tiles, map and chart to keep their spoken descriptions, so that the redesign adds nothing I cannot hear.
64. As a user who enlarges system text, I want the new layouts to grow with the text without clipping, so that I can still read everything.
65. As an app user in German, French or Italian, I want the longer texts to fit the new cards and buttons, so that my language is not cut off.
66. As an app user on an older Android phone, I want the bottom bar to still look right without the blur, so that the app works on my phone.
67. As an iPhone user, I want the same design as on Android, so that both apps feel the same.
68. As the maintainer, I want the colour contrasts checked by an automated test, so that a later colour change cannot silently make severity unreadable.
69. As the maintainer, I want the gauge's filling rule tested automatically, so that the hero never shows a wrong level.
70. As the maintainer, I want the project documentation to describe the new theme, bars and screen layout, so that future features follow the design.

## User Acceptance Tests

### Foundations

1. Given an Android 12 or newer phone with a colourful wallpaper, when the app opens, then it shows the Meadow forest green and cream colours, not wallpaper colours.
2. Given the phone is set to light mode, when any screen opens, then it shows the light Meadow colours; given dark mode, then the dark Meadow colours.
3. Given any screen, when the user looks at its title, then it is set in the serif font, and all other text is in the sans-serif font, on Android and on iOS.
4. Given any tab screen, when it opens, then there is no title bar; a small overline and a large title sit at the top of the content.
5. Given any non-tab screen (onboarding excepted), when it opens, then a round back button floats at the top left, and tapping it does what the old back arrow did.

### Bottom bar and action bar

6. Given a tab screen with more content than fits, when the user scrolls down, then the content passes visibly and softly blurred behind the bottom bar, and the last item can be scrolled fully above the bar.
7. Given the bottom bar, when the user looks at it, then its upper corners are rounded, it shows the same five icons in the same order, and the current tab sits on a green pill.
8. Given an Android phone older than Android 12, when the user scrolls behind the bottom bar, then the bar is see-through without blur and remains readable.
9. Given onboarding, change station or the alarm editor, when the screen opens, then the main action (Continue / Save) sits in a glass bar at the bottom with rounded upper corners.

### Home

10. Given a reading with severity High driven by Birch, when Home shows it, then the gauge shows "High" with three of four segments filled in the High colour and "Driven by Birch" beneath.
11. Given a reading with severity None, when Home shows it, then the gauge shows "None" with no segment filled.
12. Given a reading older than three hours, when Home shows it, then a banner says "These readings are not current." and the reading-time pill is shown in warning colours.
13. Given the feeling question is due and the user is logged in, when Home shows it, then a green card offers four face buttons labelled Very good, Good, Bad, Very bad, and "Not today".
14. Given a reading, when Home lists the pollen types, then every type appears as a tile in a two-column grid, worst first, each with its level word, concentration and bar; a type the station does not report shows "No data" and an empty bar.
15. Given Home is loading, when the user looks at it, then a skeleton of the gauge and tiles with a moving shimmer is shown; given it failed, then a card with the reason and "Retry" is shown below the station title.

### All stations

16. Given all stations, when the user opens the tab, then a map card shows one coloured dot per station and a legend with all five level words.
17. Given the user selects Zürich, when the map shows it, then its dot is enlarged with a halo and a label "Zürich · High".
18. Given some stations are still loading and one has no reading, when the list shows them, then those rows show "Loading…" or "No reading" with a grey dot, and the other rows show their levels.
19. Given every station failed, when the tab shows the result, then a card with "No station's reading could be loaded…" and "Retry" is shown.

### Diary

20. Given a logged-out user, when they open the Diary tab, then a card explains the account with four points and offers "Create account" and "Log in".
21. Given a logged-in user with entries, when the Diary shows a week, then the chart card shows a dashed green feeling line with dots, one coloured line per checked type, and severity words on the vertical axis.
22. Given a type not measured at the selected station, when the user looks at the chips, then that chip is dimmed and says "Not measured here".

### Alarms and editor

23. Given three alarms, one paused, when the Alarms tab opens, then three cards show their summaries, stations, weekday circles, criteria and switches, the paused card is dimmed and marked "Paused", and "3 of 10" is shown.
24. Given notifications are off, when the Alarms tab opens, then an illustrated card says "Notifications are disabled" with its button; given no alarms, then an illustrated card says "No alarms yet" with a gold "Create alarm" button.
25. Given the user flips a switch and the server cannot be reached, when the request fails, then the switch returns to its previous position and a snackbar explains the failure above the "Create alarm" button.
26. Given a new alarm, when the editor opens, then "Daily report" and "Threshold alert" are two large cards, and choosing one shows its description below.
27. Given an existing alarm, when the editor opens, then the other type's card is dimmed with a lock.
28. Given the user taps the report time, when the time picker opens, then it shows the new style with large hour and minute boxes and a 24-hour dial.
29. Given the user saves and the server cannot be reached, when saving fails, then the error appears in red directly above "Save" in the bottom bar.
30. Given the user changed an alarm and goes back, when the discard question appears, then it is a restyled dialog offering "Discard" (red) and "Keep editing"; given the user deletes an alarm, then the restyled "Delete alarm?" dialog appears.

### Onboarding, account, settings

31. Given a first launch, when onboarding opens, then it shows the Swiss map illustration, "Welcome to PollenInfo", the station dropdown, "Use my location" and "Continue" in the bottom bar.
32. Given the station list cannot be loaded during onboarding, when the screen shows it, then an error card with "Retry" replaces the dropdown and "Continue" is disabled.
33. Given the create-account form with mismatching passwords, when the user looks at it, then the repeat-password field is outlined in red with "The passwords don't match." below it, followed by the no-recovery warning and the consent checkbox.
34. Given a wrong password at log-in, when log-in fails, then a banner under the fields says "Couldn't log in. The email address or password is incorrect."
35. Given a logged-in user, when Settings opens, then an account card shows the email with a "Log out" button, followed by grouped rows for station, language, privacy, data source, impressum and account deletion, and the version line.
36. Given the user taps "Log out" or "Delete account", when the confirmation appears, then it is a restyled dialog; the delete dialog contains a password field.
37. Given saving the crash-report choice fails, when the switch flips back, then a snackbar says "Could not save your choice. Please try again."

### Accessibility and languages

38. Given the app in German, French and Italian, when every screen and dialog is opened, then no text is cut off and no button label is truncated.
39. Given the largest system font size, when Home, Alarms and the alarm editor are opened, then all text is readable without overlapping.
40. Given TalkBack or VoiceOver, when the user moves through Home, then the station, severity, driving type, reading time and every pollen tile are announced as before.
41. Given the severity and species colours, when they are measured against every background they appear on, then each reaches at least 3:1 (species lines 3.5:1), and body text reaches at least 4.5:1, in both themes.

## Definition of Done

- All user acceptance tests pass on an Android phone (light and dark, Android 12+ and one older version) and the iOS sources compile.
- Every screen, dialog, loading, error and partial state matches the approved Figma file in layout, colours, type and shapes; deviations are agreed with the maintainer.
- Behaviour, data, navigation and wording are unchanged: every existing app test still passes unchanged except tests that pin a colour value that this feature deliberately changes.
- The new automated tests (palette contrast, gauge filling) pass.
- No text in German, French or Italian is clipped on any screen; translations are unchanged except where a new string is genuinely needed.
- Accessibility: no new element without a spoken description where the old one had one; touch targets stay at least 48 dp.
- All quality gates pass: app tests, iOS compile, translation check, Android Lint, no compiler warning, debug and release builds.
- The fonts' licences are included in the repository next to the font files.
- The project documentation (CLAUDE.md, README) describes the new theme, the glass bars, the screen layout without a top bar, the removed dynamic colour and the new palette rules.

## Out of Scope

- Any change to data, server, API, ViewModels' behaviour, UI state shapes, navigation structure or texts (beyond new strings a component genuinely needs).
- New features shown in the reference apps: forecasts, weather, air quality, plant photos, widgets, symptom statistics, PDF export.
- A user-selectable theme or colour scheme; the app follows the system light/dark setting as today.
- Animations beyond the loading shimmer and the existing standard transitions (no animated gauge, no page transitions).
- App icon, splash screen and store graphics.
- Tablet- or landscape-specific layouts.
- The iOS wrapper project (still not created); iOS is compile-verified as before.
- The fictional `feature/example` screen keeps its look.

## Further Notes

- **Design source:** Figma file *PollenInfo Redesign*, <https://www.figma.com/design/naeELzkiFSdMuXsnmZVKnW>. Page "Screens" lists 36 screens in usage order (light row on top, dark below); "Components" holds the reusable pieces; "Direction & Foundations" holds the reasoning, colour tokens and type scale. Because the Figma plan allows one mode per collection, light and dark tokens are two collections ("Light", "Dark") with identical names.
- **References:** aha! Pollen (sage background, white cards, icon tiles, level bars) and Pollenflug & Allergie Alarm (gauge hero, frosted surfaces, faces row, segmented toggle). Their blue/purple palettes, photos, weather and forecasts were deliberately not taken.
- **Decided during the design:** "Low" stays green although the brand is green, because every level always appears with its word. The bottom bar's corners are rounded (28 dp), not cut.
- **Not drawn as separate frames** but covered by the same components: the expanded station row's details, button "Saving…"/"Deleting…" labels, other error wordings, and the outdated-row warning icon.
- The Moderate colour change and any other colour change must be re-validated by the new contrast test; the species palette's colour-vision distances (documented in `SpeciesPalette.kt`) are unaffected because species colours do not change.

---

## Technical Annex
> Written against codebase as of: 2026-10-19

### Architectural Decisions

**Theme foundation (`:theme`)**

- `ColorSchemes.kt` / `Color.kt` get the Meadow values. Mapping of the Figma tokens onto Material 3 roles (light / dark):
  - `background` = `bg/canvas` (`#F1F0E2` / `#0E1611`)
  - `surface` = `surface/default` (`#FBFAF3` / `#16211A`)
  - `surfaceContainerLowest` = `surface/raised` (`#FFFFFF` / `#1F2C23`), used for cards
  - `surfaceContainerHighest` = `surface/sunken` (`#E7E8D6` / `#0A110C`), used for tracks, skeletons and segmented backgrounds
  - `primary` / `onPrimary` = `#2E5B3B` / `#FFFFFF` (dark `#A6D5A0` / `#0D2614`)
  - `primaryContainer` / `onPrimaryContainer` = `#CFE3BF` / `#15301E` (dark `#25442E` / `#CFE9C8`)
  - `onSurface` = `text/primary` (`#17241B` / `#E8EEDF`), `onSurfaceVariant` = `text/secondary` (`#556356` / `#A7B4A5`)
  - `outline` / `outlineVariant` from `outline/default` (`#CBD0BA` / `#34443A`)
  - `error` = `#B3261E` / `#F2B8B5`
  - `inverseSurface` / `inverseOnSurface` (snackbar) = `text/primary` / `surface/default`
  The remaining roles (secondary, tertiary, the other surface containers) are derived to fit; exact values are a task decision recorded beside each colour with its contrast.
- **New extended tokens** that Material 3 has no role for, in a new `MeadowColors` data class exposed through `staticCompositionLocalOf` (`LocalMeadowColors`) and provided by `PollenInfoTheme`: `wash` (`#D6E6C2` / `#1C3022`), `glass` (`surface/default` at 72 % / 70 %), `accent` + `onAccent` (pollen gold `#E2B93B` / `#F0CB5C`, on `#2A2207`), `warningContainer` + `onWarningContainer` (`#F6E3C4` / `#3B2A10`, `#5B3A06` / `#F5D9A8`), `mapLand` (`#E3EBD2` / `#1A281E`). `MapPalette.kt` keeps the lake colours (re-tuned to `#9CC3D6` / `#2D5466` if the contrast test allows).
- **Dynamic colour removed:** `Theme.android.kt` stops using `dynamicLight/DarkColorScheme`; Android and iOS both use `lightScheme` / `darkScheme`. The "luminance of `colorScheme.surface`" rule for picking light/dark palette values stays.
- **Severity palette:** values unchanged except `severityModerateLight` `#B08000` → `#A07400` (3.2:1 against `wash`, 3.39:1 against `surface/sunken`, 4.21:1 against `surface/raised`; the old value fell to 2.85:1 on the new track). `SeverityColorsTest` is updated for this one value. The species palette is unchanged.
- **Fonts:** Fraunces (SemiBold, static instance) and Plus Jakarta Sans (Regular, Medium, SemiBold, Bold), both SIL Open Font License, as Compose resources in `:theme` (`theme/src/commonMain/composeResources/font/`, `packageOfResClass = "ch.stenzel.tim.polleninfo.theme.resources"`; `:theme` gains `compose-components-resources`). OFL texts committed beside the fonts. `PollenInfoTypography` becomes a `@Composable` factory because `Font(Res.font.…)` is composable. Type scale from the Figma text styles:
  - Display/Hero Fraunces SemiBold 56/60 −1.5 → `displayLarge`
  - Display/Large Fraunces 36/42 −0.8 → `displayMedium` / screen title
  - Headline/Title Fraunces 28/34 −0.4 → `headlineMedium`
  - Headline/Card Jakarta Bold 18/24 → `titleMedium`
  - Body/Large Jakarta Medium 16/24 → `bodyLarge`
  - Body/Default Jakarta Regular 14/20 → `bodyMedium`
  - Label/Strong Jakarta SemiBold 14/18 → `labelLarge`
  - Label/Small Jakarta SemiBold 12/16 → `labelMedium`
  - Label/Overline Jakarta Bold 11/14 +1.2 uppercase → `labelSmall`
  - Number/Value Jakarta Bold 20/24 → `titleLarge`
- **Shapes:** `Shapes(small = 12.dp, medium = 20.dp, large = 28.dp, extraLarge = 36.dp)`; pills use `CircleShape` / 50 %.

**Palette contrast check (deep, pure module)**

- `contrastRatio(a: Color, b: Color): Double` (WCAG 2.1 relative luminance), plus a declarative table of required pairs (foreground token, background token, minimum: 3.0 graphics, 3.5 species lines, 4.5 text) for both schemes. Lives in `:composeApp` `commonTest` (or a small pure file in `:theme` if `:theme` gains a test source set), so the test can see both the theme values and the app's severity/species mapping.

**Glass bottom bar**

- `AppNavigation` replaces `NavigationBar` / `NavigationBarItem` with a custom `GlassNavigationBar` driven by the existing `TopLevelDestination` entries (same `contentDescription`s, `Role.Tab`, selection semantics, `navigateToTab` logic untouched). Shape: `RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)`, `glass` fill, 1 dp top hairline in `outline` at 70 %, selected item a 60×44 pill in `primary` with `onPrimary` icon, unselected icons `onSurfaceVariant`.
- **Blur:** Haze (`dev.chrisbanes.haze:haze`, latest stable 2.0.1 as of this date — verify compatibility with Compose Multiplatform 1.12.2 / Kotlin 2.4.21 before adding; pin in `libs.versions.toml`). The `NavHost` content is the `hazeSource`, the bar the `hazeEffect` (blur ≈ 24 dp, tint = `glass`). Where blur is unsupported (Android < 12), Haze's fallback tint is used; acceptance test 8 verifies readability.
- **Insets change:** content now draws behind the bar. The outer `Scaffold` no longer pads the `NavHost` for the bar; each screen gets the bar height through a `LocalBottomBarPadding` (or the `innerPadding` passed as content padding to the screen's list) so lists add it to their bottom `contentPadding`. The rule "outer Scaffold has `contentWindowInsets = WindowInsets(0)`" and the Home rule "list bottom padding grows by the card's measured height" are re-stated in CLAUDE.md accordingly.

**Screen frame without a top bar**

- `core/ui/layout/MeadowScreen` (name to be decided in tasks): a `Box` with the `background` colour and the decorative `wash` / `accent` blobs (blurred circles, decorative, `clearAndSetSemantics {}`), a scrollable content slot whose first item is `ScreenHeader(overline, title)` (`labelSmall` uppercase + serif title, `semantics { heading() }`), an optional floating `BackButton` (44 dp glass circle, `common_back` description, 48 dp touch target, top-left under the status bar inset), and an optional `GlassActionBar` slot at the bottom (same shape and glass as the tab bar, hosting full-width buttons). All `TopAppBar`s are removed from the screens.
- A top scrim (gradient from `background`) behind the status bar and back button when content scrolls under them.

**Shared components (`core/ui`)**

- `SeverityGauge(severity: PollenSeverity?, drivenBy: String?)`: 4 arc segments over 180°, gap ≈ 4°, inner radius 84 %, filled segments in `severity.color()`, rest `surfaceContainerHighest`, soft glow behind; centre word `displayLarge`. Filling comes from a pure `gaugeFilledSegments(severity: PollenSeverity?): Int` (`null`, `NONE` → 0; `LOW` 1 … `VERY_HIGH` 4). Decorative to accessibility; the word and the existing semantics carry the meaning.
- `SeverityBar`: same `severityFillFraction` stops (unchanged and still tested); track becomes `surfaceContainerHighest`; "no reading" stays a muted empty track.
- `SpeciesTile` (replaces `SpeciesRow` on Home and in the expanded station row): icon tile (tree or grasses icon), concentration + unit, name, level word, compact `SeverityBar`; same semantics as `SpeciesRow` today.
- `StationRow` style in All stations: severity dot, name, "level · driving type", the reserved stale-icon slot, chevron/expand affordance; loading / no-reading rows use a grey dot.
- Chips (`FilterChip` restyle) and a `SegmentedToggle` (pill track in `surfaceContainerHighest`, selected pill `primary`) for Week/Month/Year; `FeelingPicker` face buttons (four icons, labels via `Feeling.label()`).
- `Skeleton` blocks + a `shimmer` modifier (infinite linear gradient sweep, respects reduced-motion where the platform offers it); per-screen skeleton layouts mirror the drawn ones.
- `StatusCard(emblem, title, body, actions)` for error, empty and gate states (warning-tinted emblem for errors, sage for empty/gate); `Banner` (warning container, icon + text); snackbar colours via `inverseSurface`.
- Restyled Material components through the theme where possible (dialog shape `extraLarge` with centred icon, title, text, stacked full-width buttons; `OutlinedTextField` colours/shape `medium`; buttons pill-shaped with Primary, Tonal, Outline, Text, Danger and Accent variants; `TimePicker` colours).
- Icons stay Material icons (`material-icons-extended`); the Stations tab keeps `SwissOutlineIcon`. Faces for the feeling picker use Material "sentiment" icons or small vectors in `:composeApp` resources.

**Per-screen restyle**

- Onboarding, Home, All stations (map card with legend, enlarged selected dot with halo and label), Diary (gate, controls, chart card, chips), Alarms (cards, count chip, gold extended FAB), Alarm editor (type cards, lock on existing alarms, sections, glass Save bar, inline save error), Account (log in / create account), Settings (account card, grouped rows), Change station, all dialogs and the time picker, and every Loading / Error / partial state, as in Figma.
- No ViewModel, UI-state, use-case, repository or navigation-graph change. The only new strings, if any, are for new spoken descriptions; none are expected.

### Automated Testing Decisions

- Good tests here pin **observable rules**, not pixels: which colours meet which contrast, how many gauge segments a severity fills. Visual fidelity is checked by hand against Figma (acceptance tests), as the project has no screenshot tests.
- **Palette contrast test** (new, `commonTest`): for both schemes, every severity colour against `surface`, `surfaceContainerLowest`, `surfaceContainerHighest`, `background`, `wash` and `mapLand` ≥ 3:1; every species colour against the chart card surface ≥ 3.5:1 (the documented requirement); `onSurface` and `onSurfaceVariant` against `background`, `surface` and `surfaceContainerLowest` ≥ 4.5:1; `onPrimary`/`primary`, `onAccent`/`accent`, `onWarningContainer`/`warningContainer` ≥ 4.5:1. Each pair is pinned from both sides by also asserting the computed ratio of a known pair (black/white = 21, identical colours = 1), following `SpeciesThresholdsTest`'s boundary style.
- **Gauge filling test** (new, `commonTest`): `gaugeFilledSegments` for `null`, every `PollenSeverity`, and monotonic increase; mirrors `SeverityBarTest`.
- **Existing tests stay:** `SeverityBarTest` (fill stops), `SeverityColorsTest` (updated only for the new Moderate light value), `TopLevelDestinationTest`, all ViewModel and repository tests unchanged.
- Test names are backtick sentences without commas (Kotlin/Native); no mocking framework; tests run with `./gradlew :composeApp:testAndroidHostTest` and compile for iOS with `:composeApp:compileTestKotlinIosSimulatorArm64`.
