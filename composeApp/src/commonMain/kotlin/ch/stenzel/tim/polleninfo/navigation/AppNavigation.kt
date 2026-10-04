package ch.stenzel.tim.polleninfo.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ch.stenzel.tim.polleninfo.feature.alarms.presentation.AlarmEditorScreen
import ch.stenzel.tim.polleninfo.feature.alarms.presentation.AlarmsScreen
import ch.stenzel.tim.polleninfo.feature.allstations.presentation.AllStationsScreen
import ch.stenzel.tim.polleninfo.feature.diary.presentation.DiaryScreen
import ch.stenzel.tim.polleninfo.feature.example.presentation.ExampleScreen
import ch.stenzel.tim.polleninfo.feature.home.presentation.HomeScreen
import ch.stenzel.tim.polleninfo.feature.onboarding.presentation.OnboardingScreen

/**
 * [startDestination] is decided by the startup gate in `App()` — see its documentation for why the
 * graph is not built until that answer is known.
 *
 * The bottom bar is shown only while the current destination is a [TopLevelDestination], so it
 * never appears during setup or on the reference example screen.
 */
@Composable
fun AppNavigation(startDestination: Screen) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = backStackEntry?.destination?.let { destination ->
        TopLevelDestination.current { destination.hasRoute(it) }
    }

    Scaffold(
        // No insets here: each screen's own Scaffold handles the top, and NavigationBar applies the
        // navigation-bar inset itself. Applying system bars here as well would add a gap above
        // Home's top bar and pad Onboarding twice.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (currentTab != null) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == currentTab,
                            // Reselecting the current tab is a no-op, not a reload.
                            onClick = { if (tab != currentTab) navController.navigateToTab(tab) },
                            icon = { Icon(tab.icon, contentDescription = tab.contentDescription) },
                            alwaysShowLabel = false,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            startDestination = startDestination,
            // Consuming the padding tells the screens' own Scaffolds that the bottom inset is
            // already covered by the bar, so they do not add it a second time.
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        )
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    startDestination: Screen,
    modifier: Modifier,
) {
    NavHost(navController = navController, startDestination = startDestination, modifier = modifier) {
        composable<Screen.Onboarding> {
            OnboardingScreen(
                onOnboardingComplete = {
                    // `inclusive` drops onboarding off the back stack: its purpose is fulfilled, so
                    // the back gesture from Home leaves the app rather than reopening setup.
                    navController.navigate(Screen.Home) {
                        popUpTo<Screen.Onboarding> { inclusive = true }
                    }
                },
            )
        }

        composable<Screen.Home> {
            HomeScreen()
        }

        composable<Screen.AllStations> {
            AllStationsScreen()
        }

        composable<Screen.Diary> {
            DiaryScreen()
        }

        composable<Screen.Alarms> {
            AlarmsScreen(
                onCreateAlarm = { navController.navigate(Screen.AlarmEditor()) },
                onEditAlarm = { id -> navController.navigate(Screen.AlarmEditor(id)) },
            )
        }

        composable<Screen.AlarmEditor> { entry ->
            AlarmEditorScreen(
                alarmId = entry.toRoute<Screen.AlarmEditor>().alarmId,
                onDone = { navController.popBackStack() },
            )
        }

        // Placeholder tab. The title is the tab's accessibility name, so the two cannot disagree.
        composable<Screen.Feature5> {
            ComingSoonScreen(TopLevelDestination.FEATURE_5.contentDescription)
        }

        // The reference feature stays registered and unchanged; it is simply no longer the start
        // destination now that the app has a real first screen.
        composable<Screen.Example> {
            ExampleScreen()
        }
    }
}

/**
 * Switches to [tab] keeping exactly one tab above Home on the back stack, so back from any tab
 * lands on Home and back from Home leaves the app. The tab being left saves its state and the one
 * being entered restores it, so Home's readings survive a round trip without reloading.
 *
 * Pops up to [Screen.Home] rather than `graph.findStartDestination()`: after a fresh install the
 * start destination is Onboarding, which is already off the back stack, and a `popUpTo` on a
 * destination that is not there is ignored — history would grow with every switch.
 */
private fun NavHostController.navigateToTab(tab: TopLevelDestination) {
    navigate(tab.screen) {
        popUpTo<Screen.Home> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
