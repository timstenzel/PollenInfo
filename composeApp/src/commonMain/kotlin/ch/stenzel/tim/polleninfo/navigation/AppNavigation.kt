package ch.stenzel.tim.polleninfo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ch.stenzel.tim.polleninfo.feature.example.presentation.ExampleScreen
import ch.stenzel.tim.polleninfo.feature.onboarding.presentation.OnboardingScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Onboarding) {
        composable<Screen.Onboarding> {
            OnboardingScreen()
        }

        // The reference feature stays registered and unchanged; it is simply no longer the start
        // destination now that the app has a real first screen.
        composable<Screen.Example> {
            ExampleScreen()
        }
    }
}
