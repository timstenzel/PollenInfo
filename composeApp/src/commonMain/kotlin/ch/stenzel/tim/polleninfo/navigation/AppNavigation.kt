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
                            // Home is the only tab and is always the selected one, so there is
                            // nowhere to navigate to yet.
                            onClick = {},
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

        // The reference feature stays registered and unchanged; it is simply no longer the start
        // destination now that the app has a real first screen.
        composable<Screen.Example> {
            ExampleScreen()
        }
    }
}
