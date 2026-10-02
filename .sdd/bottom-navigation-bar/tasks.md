# Tasks: Bottom Navigation Bar

Requirements: [requirements.md](requirements.md)

Notes that apply to every task:

- **iOS runtime (UAT 15) cannot be verified here** — there is no `iosApp` project and only Xcode
  Command Line Tools are installed. Only compiling the iOS sources is checkable; the on-device iOS
  run is deferred until the wrapper exists.
- **Deliberate deviation from the annex snippet:** tab navigation pops up to `Screen.Home`, not to
  `graph.findStartDestination()`. On a fresh install the graph's start destination is Onboarding,
  which has already been popped; `popUpTo` on a destination not in the back stack is ignored, so
  history would pile up with every tab switch.
- **Insets:** the outer `Scaffold` must not apply system-bar insets itself (default
  `contentWindowInsets = systemBars` would add a top inset above Home's `TopAppBar` and both insets
  to Onboarding). Use `contentWindowInsets = WindowInsets(0)`, let `NavigationBar` apply its own
  `navigationBars` inset, and give the `NavHost` `Modifier.padding(innerPadding)
  .consumeWindowInsets(innerPadding)` so inner Scaffolds do not re-apply the bottom inset. Only
  touch Home's own `Scaffold` if a gap is still observed.
- **The tested visibility rule is the shipped rule.** Express it over `KClass<out Screen>` (or over
  the `TopLevelDestination` entries) and call that same function from the composable via
  `destination.hasRoute(entry.screen::class)` — no second, untested copy.

## Task [01-bottom-bar-frame-with-home-tab]

Once setup is done, the home dashboard is shown inside an app frame with a bottom navigation bar
holding a single tab: Home (location pin icon, no text label, accessibility name "Home"),
highlighted. The bar never appears on the setup screen or the reference example screen. The tab set
is defined in one place (`TopLevelDestination`, only `HOME` for now) together with the pure rule
that decides when the bar is shown. The home dashboard itself behaves exactly as before, and the
layout has neither a double gap nor an overlap above the system navigation area.

### Implementation steps

- [x] Add the `TopLevelDestination` enum in `navigation/` with `screen`, `icon`
      (`Icons.Default.LocationOn`) and `contentDescription`, containing only `HOME` → `Screen.Home`
- [x] Add the pure visibility / selection rule on its companion, expressed so the composable calls
      the same function the tests call
- [x] Wrap the `NavHost` in an outer `Scaffold` (`contentWindowInsets = WindowInsets(0)`) whose
      `bottomBar` renders a `NavigationBar` only when the current back-stack destination is a tab;
      items icon-only, `contentDescription` on the `Icon`, `selected` from the rule
- [x] Pass `Modifier.padding(innerPadding).consumeWindowInsets(innerPadding)` to the `NavHost`;
      adjust Home's `Scaffold` insets only if a gap remains
- [x] Correct the stale "Placeholder main screen" KDoc on `Screen.Home`
- [x] Add `TopLevelDestinationTest` in `commonTest`
- [x] Update the `CLAUDE.md` Navigation section: the bar, `TopLevelDestination` as the tab list,
      the visibility rule, and the inset handling

### Acceptance criteria

- [x] Automated test: `HOME` is the first entry and maps to `Screen.Home` with content description "Home"
- [x] Automated test: the visibility rule is true for `Screen.Home` and false for `Screen.Onboarding` and `Screen.Example`
- [x] Runtime (Android emulator, fresh install): the setup screen shows no bottom bar and its layout (top bar, bottom content) is unchanged from before; after completing setup, Home shows the bar
- [ ] ~~Runtime (returning user): Home opens with the bar and the Home tab highlighted; TalkBack announces the tab as "Home"~~ *(skipped: the bar and highlighted tab were verified on a returning-user launch, and the accessibility tree shows the selected tab holding an icon described as "Home", but TalkBack's spoken output could not be captured — the emulator runs without audio and TalkBack writes no speech log)*
- [x] Runtime: Home's station title, readings, reading-age captions, pull-to-refresh and retry behave as before; the bar sits directly above the system navigation area with no extra gap and no content hidden behind it, and the top bar has no extra gap above it
- [x] `CLAUDE.md` Navigation section names `TopLevelDestination`, states the visibility rule and describes the inset handling

### Quality gates

- [x] `./gradlew :composeApp:testDebugUnitTest` passes, including all previously existing tests
- [x] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [x] The build output contains no `w:` lines referencing files touched by this task
- [x] New test names are backtick sentences containing no commas

## Task [02-placeholder-tabs]

The bar grows to five tabs: Home plus "Feature 2" … "Feature 5", all with the location pin icon and
no text labels. Each placeholder tab opens a shared "Coming soon" screen with a top bar titled with
its tab name and a centred pin icon above the text "Coming soon". Switching tabs keeps each tab's
state (Home's readings stay loaded), does not build up history, the back gesture from any
placeholder returns to Home, back from Home leaves the app, and tapping the already-selected tab
does nothing — on both the fresh-install and the returning-user launch path.

### Implementation steps

- [ ] Add `@Serializable data object Feature2 … Feature5 : Screen`
- [ ] Add the stateless `ComingSoonScreen(title)` composable (`Scaffold` + `TopAppBar(title)`,
      centred pin icon and "Coming soon"), not re-applying the bottom inset
- [ ] Extend `TopLevelDestination` with `FEATURE_2 … FEATURE_5` ("Feature 2" … "Feature 5")
- [ ] Register `composable<Screen.FeatureN>` for all four, each rendering `ComingSoonScreen`
- [ ] Navigate on tab click with `popUpTo<Screen.Home> { saveState = true }`,
      `launchSingleTop = true`, `restoreState = true`; skip navigation when the tab is already selected
- [ ] Extend `TopLevelDestinationTest`
- [ ] Update the `CLAUDE.md` Navigation section: the five tabs, per-tab state, back behaviour and
      how to replace a placeholder with a real feature

### Acceptance criteria

- [ ] Automated test: exactly five entries in the order `HOME`, `FEATURE_2` … `FEATURE_5`, with pairwise distinct content descriptions and pairwise distinct screens, and the visibility rule is true for all five screens (and still false for `Screen.Onboarding` and `Screen.Example`)
- [ ] Runtime: after completing setup on a fresh install, Home shows a bar of five tabs, each a location pin icon with no text label; tapping tabs 2–5 opens a screen titled "Feature N" showing the pin and "Coming soon", with that tab highlighted, and the bar sits directly above the system navigation area with no gap or overlap
- [ ] Runtime: with Home's readings loaded, switching Home → Feature 2 → Home shows the readings immediately with no loading indicator
- [ ] Runtime, on both a fresh install (after setup) and a returning-user launch: Home → Feature 2 → Feature 3 → Feature 4, then back once shows Home; back again leaves the app
- [ ] Runtime: tapping the already-selected tab changes nothing, both on Home and on a placeholder tab
- [ ] Runtime: TalkBack announces the tabs as "Home", "Feature 2", "Feature 3", "Feature 4", "Feature 5"
- [ ] `CLAUDE.md` Navigation section describes per-tab state, back behaviour and how to replace a placeholder with a real feature

### Quality gates

- [ ] `./gradlew :composeApp:testDebugUnitTest` passes, including all previously existing tests
- [ ] `./gradlew :composeApp:compileTestKotlinIosSimulatorArm64` passes
- [ ] The build output contains no `w:` lines referencing files touched by this task
- [ ] New test names are backtick sentences containing no commas
