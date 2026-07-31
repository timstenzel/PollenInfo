package ch.stenzel.tim.polleninfo

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.stenzel.tim.polleninfo.core.startup.StartupState
import ch.stenzel.tim.polleninfo.core.startup.StartupViewModel
import ch.stenzel.tim.polleninfo.navigation.AppNavigation
import ch.stenzel.tim.polleninfo.navigation.Screen
import ch.stenzel.tim.polleninfo.theme.PollenInfoTheme
import org.koin.compose.viewmodel.koinViewModel

/**
 * The startup gate.
 *
 * `AppNavigation` is composed **only once [StartupState] has resolved**, so its `startDestination`
 * is right the first time. That is what makes "no onboarding flash" a structural property rather
 * than a timing accident: while the answer is unknown there is no navigation graph at all, so
 * onboarding cannot appear and then be navigated away from. It also means no `popUpTo` bookkeeping
 * on launch and no splash destination the user could reach with the back gesture.
 *
 * While unresolved the app renders an empty themed [Surface], not a spinner: the read is a
 * milliseconds-long local file access, and a progress indicator that appears for one frame reads as
 * a glitch rather than as feedback.
 */
@Composable
fun App(startupViewModel: StartupViewModel = koinViewModel()) {
    val startupState by startupViewModel.state.collectAsStateWithLifecycle()

    PollenInfoTheme {
        when (startupState) {
            is StartupState.Loading -> Surface(modifier = Modifier.fillMaxSize()) {}

            is StartupState.NeedsOnboarding -> AppNavigation(startDestination = Screen.Onboarding)

            is StartupState.Ready -> AppNavigation(startDestination = Screen.Home)
        }
    }
}
