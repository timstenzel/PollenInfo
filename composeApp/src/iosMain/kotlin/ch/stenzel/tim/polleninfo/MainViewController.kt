package ch.stenzel.tim.polleninfo

import androidx.compose.ui.window.ComposeUIViewController
import ch.stenzel.tim.polleninfo.core.di.appModules
import org.koin.core.context.startKoin

/**
 * The iOS simulator shares the host machine's network stack, so the development server is simply
 * on `localhost`. Reaching it over cleartext needs an App Transport Security exception
 * (`NSAllowsLocalNetworking`) in the future iOS wrapper's `Info.plist` — see `CLAUDE.md`.
 */
private const val API_BASE_URL = "http://localhost:8080"

fun MainViewController() = ComposeUIViewController(
    configure = {
        startKoin { modules(appModules(API_BASE_URL)) }
    },
) { App() }
