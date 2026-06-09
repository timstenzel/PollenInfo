package ch.stenzel.tim.polleninfo

import androidx.compose.ui.window.ComposeUIViewController
import ch.stenzel.tim.polleninfo.core.di.appModules
import org.koin.core.context.startKoin

fun MainViewController() = ComposeUIViewController(
    configure = {
        startKoin { modules(appModules) }
    },
) { App() }
