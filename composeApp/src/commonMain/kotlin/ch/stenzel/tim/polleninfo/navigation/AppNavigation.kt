package ch.stenzel.tim.polleninfo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ch.stenzel.tim.polleninfo.feature.example.presentation.ExampleScreen
import ch.stenzel.tim.polleninfo.feature.home.presentation.HomeScreen
import ch.stenzel.tim.polleninfo.feature.onboarding.presentation.OnboardingScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Onboarding) {
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
