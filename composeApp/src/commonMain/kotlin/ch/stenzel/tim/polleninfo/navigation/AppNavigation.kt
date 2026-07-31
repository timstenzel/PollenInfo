package ch.stenzel.tim.polleninfo.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ch.stenzel.tim.polleninfo.feature.example.presentation.ExampleScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Screen.Example) {
        composable<Screen.Example> {
            ExampleScreen()
        }
    }
}
