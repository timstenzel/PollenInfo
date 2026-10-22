package ch.stenzel.tim.polleninfo

import androidx.compose.ui.window.ComposeUIViewController
import ch.stenzel.tim.polleninfo.core.di.appModules
import org.koin.core.context.startKoin

/**
 * The iOS entry point, hosted by the SwiftUI app in `iosApp/`. [apiBaseUrl] (no trailing slash)
 * comes from the app's `Info.plist`, which takes it from the build configuration (`iosApp/Config/`):
 * the local backend in Debug, production in Release.
 */
fun MainViewController(apiBaseUrl: String) = ComposeUIViewController(
    configure = {
        startKoin { modules(appModules(apiBaseUrl)) }
    },
) { App() }
