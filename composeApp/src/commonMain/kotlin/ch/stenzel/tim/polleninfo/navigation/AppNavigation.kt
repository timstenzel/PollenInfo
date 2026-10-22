package ch.stenzel.tim.polleninfo.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import ch.stenzel.tim.polleninfo.core.ui.layout.LocalBottomBarPadding
import ch.stenzel.tim.polleninfo.feature.account.domain.model.AccountMode
import ch.stenzel.tim.polleninfo.feature.account.presentation.AccountScreen
import ch.stenzel.tim.polleninfo.feature.alarms.presentation.AlarmEditorScreen
import ch.stenzel.tim.polleninfo.feature.alarms.presentation.AlarmsScreen
import ch.stenzel.tim.polleninfo.feature.allstations.presentation.AllStationsScreen
import ch.stenzel.tim.polleninfo.feature.diary.presentation.DiaryScreen
import ch.stenzel.tim.polleninfo.feature.example.presentation.ExampleScreen
import ch.stenzel.tim.polleninfo.feature.home.presentation.HomeScreen
import ch.stenzel.tim.polleninfo.feature.onboarding.presentation.OnboardingScreen
import ch.stenzel.tim.polleninfo.feature.settings.presentation.ChangeStationScreen
import ch.stenzel.tim.polleninfo.feature.settings.presentation.SettingsScreen
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * [startDestination] is decided by the startup gate in `App()` — see its documentation for why the
 * graph is not built until that answer is known.
 *
 * The bottom bar is shown only while the current destination is a [TopLevelDestination], so it
 * never appears during setup or on the reference example screen.
 *
 * Screens draw behind the glass bar: the `NavHost` is not padded for it but is the bar's blur
 * source, and the bar's height reaches the tab screens as [LocalBottomBarPadding] (zero while the
 * bar is hidden).
 */
@Composable
fun AppNavigation(startDestination: Screen) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentTab = backStackEntry?.destination?.let { destination ->
        TopLevelDestination.current { destination.hasRoute(it) }
    }
    val hazeState = rememberHazeState()

    Scaffold(
        // No insets here: tab screens pad for the status bar and the glass bar themselves, the bar
        // applies the navigation-bar inset, and the other screens' own Scaffolds handle theirs.
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (currentTab != null) {
                GlassNavigationBar(
                    currentTab = currentTab,
                    // Reselecting the current tab is a no-op, not a reload.
                    onTabSelected = { tab -> if (tab != currentTab) navController.navigateToTab(tab) },
                    hazeState = hazeState,
                )
            }
        },
    ) { innerPadding ->
        // Only the bar's height: with no insets of its own, the Scaffold's padding is exactly it.
        CompositionLocalProvider(LocalBottomBarPadding provides innerPadding.calculateBottomPadding()) {
            AppNavHost(
                navController = navController,
                startDestination = startDestination,
                modifier = Modifier.fillMaxSize().hazeSource(hazeState),
            )
        }
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
            HomeScreen(onOpenAccount = { navController.navigate(Screen.Account(AccountMode.LOGIN)) })
        }

        composable<Screen.AllStations> {
            AllStationsScreen()
        }

        composable<Screen.Diary> {
            DiaryScreen(
                onLogIn = { navController.navigate(Screen.Account(AccountMode.LOGIN)) },
                onCreateAccount = { navController.navigate(Screen.Account(AccountMode.REGISTER)) },
            )
        }

        composable<Screen.Account>(typeMap = Screen.Account.typeMap) { entry ->
            AccountScreen(
                initialMode = entry.toRoute<Screen.Account>().mode,
                onDone = { navController.popBackStack() },
            )
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

        composable<Screen.Settings> {
            SettingsScreen(
                onChangeStation = { navController.navigate(Screen.ChangeStation) },
                onLogIn = { navController.navigate(Screen.Account(AccountMode.LOGIN)) },
            )
        }

        composable<Screen.ChangeStation> {
            ChangeStationScreen(onDone = { navController.popBackStack() })
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
