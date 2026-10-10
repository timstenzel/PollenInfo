package ch.stenzel.tim.polleninfo.core.di

import org.koin.core.module.Module
import org.koin.core.qualifier.named

/**
 * Koin bindings that can only be built with platform APIs: the two `DataStore<Preferences>` — the
 * unqualified one backing the user's selections and the diary, and the [DEVICE_DATA_STORE] one
 * holding the device token alone — whose factory needs a file path and an IO dispatcher, and the
 * `CoarseLocationProvider`, `PushTokenProvider` and `AppInfo`, which are different platform classes
 * on each side.
 *
 * Keeping them here rather than in [appModules] means `AppModule.kt` stays free of
 * `expect`/`actual` noise, and a platform can add a binding without every other module having to
 * know. Both entry points already start Koin with [appModules], so this needs no wiring beyond
 * being part of that list.
 */
expect val platformModule: Module

/**
 * The qualifier of the DataStore that holds only the device token, in a file of its own
 * ([DEVICE_DATA_STORE_NAME]) so Android's backup rules can exclude it and nothing else.
 */
val DEVICE_DATA_STORE = named("deviceDataStore")

/** The device token's DataStore file name, without the `.preferences_pb` extension. */
const val DEVICE_DATA_STORE_NAME = "polleninfo_device"
