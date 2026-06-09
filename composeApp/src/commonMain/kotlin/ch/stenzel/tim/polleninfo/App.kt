package ch.stenzel.tim.polleninfo

import androidx.compose.runtime.Composable
import ch.stenzel.tim.polleninfo.navigation.AppNavigation
import ch.stenzel.tim.polleninfo.theme.PollenInfoTheme

@Composable
fun App() {
    PollenInfoTheme {
        AppNavigation()
    }
}
