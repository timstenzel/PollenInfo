# Bottom Navigation Bar

## Problem Statement

PollenInfo has a single main screen: the home dashboard showing the pollen readings for the station
the user picked during setup. There is no way to reach any other part of the app, and no place in
the layout where further features can be added. Several more features are planned, and each would
otherwise have to invent its own way of being reached. Users need one consistent, always-available
way to move between the main areas of the app.

## Solution

The main screens of the app gain a bottom navigation bar with five icon-only tabs. The first tab is
the existing home dashboard ("current location"), behaving exactly as today. The other four tabs
are reserved for features that will be defined later; until then each opens a simple "Coming soon"
screen. All five tabs use the location pin icon for now.

The bar appears on the five tab screens only — never during first-time setup. Each tab remembers
where the user left it, switching tabs does not pile up history, and the back gesture from any
placeholder tab returns to the home dashboard, while back from the home dashboard leaves the app.

## User Stories

1. As an app user, I want a bottom navigation bar on the main screens, so that I can move between the app's main areas with one tap.
2. As an app user, I want the bar to hold five tabs, so that I can see the full set of main areas the app offers.
3. As an app user, I want the first tab to be the home dashboard I already know, so that my usual view stays the first thing I reach.
4. As an app user, I want the home dashboard to behave exactly as before, so that adding the bar does not change how I read my station's pollen levels.
5. As an app user, I want the tabs to show icons only, so that the bar stays compact.
6. As an app user, I want the currently selected tab to be visibly highlighted, so that I know where I am.
7. As an app user, I want every placeholder tab to open a screen that says "Coming soon", so that I understand the feature is planned rather than broken.
8. As an app user, I want each placeholder screen to carry a title naming its tab ("Feature 2" … "Feature 5"), so that I can tell which tab I am on even though the tabs have no visible labels.
9. As an app user, I want the bar to appear only once setup is complete, so that I cannot skip choosing my station.
10. As a first-time user, I want the bar to appear when setup finishes and I land on the home dashboard, so that the app's structure becomes available the moment I can use it.
11. As an app user, I want switching away from the home dashboard and back to keep my readings on screen, so that I do not wait for a reload or see a loading spinner again.
12. As an app user, I want each tab to keep its own state, so that returning to a tab shows it as I left it.
13. As an app user, I want switching between tabs not to build up a long history, so that the back gesture does not walk me through every tab I visited.
14. As an app user, I want the back gesture on any placeholder tab to return me to the home dashboard, so that the dashboard is always my base.
15. As an app user, I want the back gesture on the home dashboard to leave the app, so that it behaves as it does today.
16. As an app user, I want tapping the tab I am already on to do nothing, so that I do not trigger unexpected reloads or jumps.
17. As an app user, I want the bar not to overlap the screen content or leave a double gap above it, so that the layout looks right on phones with gesture navigation and on iPhones with a home indicator.
18. As a screen-reader user, I want every tab to be announced by a distinct name ("Home", "Feature 2" … "Feature 5"), so that I can tell five identical-looking icons apart.
19. As an app user on iOS, I want the same bar and behaviour as on Android, so that the app works the same on both platforms.
20. As a developer, I want the five tabs defined in one place, so that replacing a placeholder with a real feature means changing one entry and one screen.
21. As a developer, I want the rule for when the bar is visible to be tested automatically, so that it cannot silently start appearing during setup.
22. As a developer, I want the project documentation to describe the bar, so that the next feature knows how to plug into it.

## User Acceptance Tests

1. Given a first-time user, when the app opens on the setup screen, then no bottom navigation bar is shown.
2. Given a first-time user on the setup screen, when they complete setup, then the home dashboard opens with a bottom navigation bar showing five tabs.
3. Given a returning user, when the app opens, then the home dashboard is shown with the bottom navigation bar and the first tab highlighted.
4. Given the bottom navigation bar is shown, when the user looks at it, then all five tabs show a location pin icon and no text labels.
5. Given the user is on the home dashboard, when they look at the screen, then the station name, readings, reading-age captions, pull-to-refresh and retry behave as before the change.
6. Given the user is on the home dashboard, when they tap the second tab, then a screen titled "Feature 2" opens showing a pin icon and the text "Coming soon", and the second tab is highlighted.
7. Given the user is on the home dashboard, when they tap the third, fourth and fifth tab in turn, then screens titled "Feature 3", "Feature 4" and "Feature 5" open, each showing "Coming soon".
8. Given the home dashboard has loaded its readings, when the user switches to another tab and back to the first tab, then the same readings are shown immediately without a loading indicator.
9. Given the user has visited the second, third and fourth tab in turn starting from the home dashboard, when they use the back gesture once, then the home dashboard is shown.
10. Given the user is on the home dashboard, when they use the back gesture, then the app is left.
11. Given the user is on the home dashboard, when they tap the first tab again, then nothing changes on screen.
12. Given the user is on a placeholder tab, when they tap that same tab again, then nothing changes on screen.
13. Given a device with gesture navigation or an on-screen navigation bar, when any tab screen is shown, then the bottom bar sits directly above the system area and the screen content is neither hidden behind it nor separated from it by an extra gap.
14. Given a screen reader is enabled, when the user moves focus across the bottom bar, then the tabs are announced as "Home", "Feature 2", "Feature 3", "Feature 4" and "Feature 5".
15. Given the app runs on iOS, when the user performs tests 3–12, then the results are the same as on Android.

## Definition of Done

- All user acceptance tests pass on the Android emulator.
- The shared code compiles for iOS.
- Automated tests covering the tab set and the bar-visibility rule exist and pass.
- All previously existing automated tests still pass.
- No regression in the setup flow or the home dashboard.
- Project documentation describes the bottom bar, its tabs, when it is shown and how back navigation behaves.

## Out of Scope

- The actual features behind tabs 2–5, their names and their final icons.
- Visible text labels on the tabs.
- Changing the home dashboard to follow the device's live location; it keeps showing the station chosen during setup.
- Changing the station from the home dashboard or any tab (belongs to the planned settings feature).
- "Tap the current tab again to scroll to top / refresh" behaviour.
- Animations for showing or hiding the bar.
- Showing the bar on the setup screen or the reference example screen.
- Automated UI tests of the bar's rendering or tab switching (no UI test infrastructure exists).
- Navigation-rail or tablet-specific layouts.

## Further Notes

- All five tabs deliberately share the location pin icon for now, at the product owner's request. Visually the tabs are distinguishable only by position and by the placeholder screens' titles; this is accepted as temporary until each feature gets its own icon.
- The first tab is internally named "Home" rather than "Current location" because it shows the station stored during setup, not the device's live position.

---

## Technical Annex
> Written against codebase as of: 2026-10-02

### Architectural Decisions

- **`navigation/TopLevelDestination`** — new enum, the single source of truth for the tabs, in
  declaration order:

  ```kotlin
  enum class TopLevelDestination(
      val screen: Screen,
      val icon: ImageVector,          // Icons.Default.LocationOn for all five today
      val contentDescription: String, // "Home", "Feature 2" … "Feature 5"
  ) { HOME, FEATURE_2, FEATURE_3, FEATURE_4, FEATURE_5 }
  ```

  Plus pure helpers on the companion (exact names to be settled in implementation) that answer
  "is the bar visible for this destination" and "which tab is selected", expressed over `Screen`
  values / route classes so they are testable without a `NavController`. If `ImageVector` makes the
  enum awkward to unit-test, the icon may move to the composable layer — the testable contract is
  order, `screen`, `contentDescription` and the visibility rule.
- **`navigation/Screen`** — add `@Serializable data object Feature2 … Feature5 : Screen`. Separate
  objects (not one `Placeholder(slot: Int)`) so each tab is its own route with its own saved state,
  and a placeholder can be renamed to a real feature without touching the others. Correct the stale
  KDoc on `Screen.Home` ("Placeholder main screen").
- **`ComingSoonScreen(title: String)`** — one stateless composable shared by all four placeholder
  destinations: `Scaffold` + `TopAppBar(title)`, centred `Icons.Default.LocationOn` + "Coming soon".
  No ViewModel, no feature slice. Location: `navigation/` or a small `feature/placeholder/presentation/`.
- **`AppNavigation`** — wrap the `NavHost` in an outer `Scaffold` whose `bottomBar` is a Material 3
  `NavigationBar`, rendered only when the current back-stack destination is a `TopLevelDestination`
  (read via `currentBackStackEntryAsState()` + `destination.hasRoute(...)`). Items are icon-only
  (`label = null` / `alwaysShowLabel = false`), with `contentDescription` set on the `Icon`.
  Tab clicks use the standard pattern:

  ```kotlin
  navController.navigate(dest.screen) {
      popUpTo(navController.graph.findStartDestination().id) { saveState = true }
      launchSingleTop = true
      restoreState = true
  }
  ```

  Because the start destination may be `Onboarding` (popped on completion) or `Home`, the
  `popUpTo` target must resolve to Home's base entry in both cases — verify that back from a
  placeholder lands on Home and back from Home exits, for both launch paths (fresh install and
  returning user). Reselecting the current tab is a no-op. Register `composable<Screen.FeatureN>`
  for all four. `Onboarding` and `Example` are unchanged and show no bar.
- **Insets** — the outer `Scaffold` consumes the bottom system inset; the `NavHost` gets its
  `innerPadding`. `HomeScreen`'s inner `Scaffold` (and `ComingSoonScreen`'s) must not re-apply
  bottom insets (e.g. `contentWindowInsets = WindowInsets(0)` or `.only(Top + Horizontal)`) to
  avoid a double gap. Home's behaviour otherwise unchanged.
- **Back behaviour** — no custom `BackHandler`; the `popUpTo`/`saveState` pattern yields
  placeholder → Home → exit.
- **Multiplatform** — all code in `commonMain`; uses only `compose.material3` and the already
  explicit `compose.materialIconsExtended`. No `expect`/`actual`. Navigation Compose 2.9.0
  (`org.jetbrains.androidx.navigation`) already present.
- **Documentation** — `CLAUDE.md` "Navigation" section: describe the bar, `TopLevelDestination` as
  the tab list, the visibility rule, per-tab state and back behaviour, and how to replace a
  placeholder with a real feature.

### Automated Testing Decisions

- Good tests here assert the externally visible contract of the tab list — what tabs exist, in
  what order, with which names, and where the bar shows — not how the composable draws it.
- **Tested:** `TopLevelDestination` only, as plain unit tests in
  `composeApp/src/commonTest/.../navigation/TopLevelDestinationTest.kt`:
  - exactly five entries, `HOME` first and mapped to `Screen.Home`;
  - all `contentDescription`s distinct; all `screen`s distinct;
  - visibility rule true for each of the five tab screens, false for `Screen.Onboarding` and
    `Screen.Example`.
- **Not automated:** `NavigationBar` rendering, tab switching, state restoration, insets, back
  behaviour — no Compose UI test infrastructure exists; covered by the manual acceptance tests.
- Test names are backtick sentences **without commas** (Kotlin/Native restriction).
- Prior art: pure-logic tests such as `ReadingAgeTest` / use-case tests under
  `commonTest/.../feature/home/`.
- Verification commands: `./gradlew :composeApp:testDebugUnitTest` and
  `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64`.
