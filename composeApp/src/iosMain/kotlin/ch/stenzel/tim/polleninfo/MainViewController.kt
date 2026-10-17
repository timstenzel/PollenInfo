package ch.stenzel.tim.polleninfo

import androidx.compose.ui.window.ComposeUIViewController
import ch.stenzel.tim.polleninfo.core.di.appModules
import org.koin.core.context.startKoin

/**
 * The app's root view controller, created once by the `iosApp` wrapper. [apiBaseUrl] (no trailing
 * slash) is the wrapper's `API_BASE_URL` from its `Info.plist`, set per build configuration.
 */
fun MainViewController(apiBaseUrl: String) = ComposeUIViewController(
    configure = {
        startKoin { modules(appModules(apiBaseUrl)) }
    },
) { App() }
