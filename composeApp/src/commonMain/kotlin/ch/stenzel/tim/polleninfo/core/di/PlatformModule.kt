package ch.stenzel.tim.polleninfo.core.di

import org.koin.core.module.Module

/**
 * Koin bindings that can only be built with platform APIs: the `DataStore<Preferences>` backing
 * `SelectedStationRepository`, whose factory needs a file path and an IO dispatcher, and the
 * `CoarseLocationProvider` and `PushTokenProvider`, which are different platform classes on each
 * side.
 *
 * Keeping them here rather than in [appModules] means `AppModule.kt` stays free of
 * `expect`/`actual` noise, and a platform can add a binding without every other module having to
 * know. Both entry points already start Koin with [appModules], so this needs no wiring beyond
 * being part of that list.
 */
expect val platformModule: Module
