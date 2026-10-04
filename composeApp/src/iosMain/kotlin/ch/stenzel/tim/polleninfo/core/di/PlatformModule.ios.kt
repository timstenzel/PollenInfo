package ch.stenzel.tim.polleninfo.core.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import ch.stenzel.tim.polleninfo.core.location.CoarseLocationProvider
import ch.stenzel.tim.polleninfo.core.location.IosCoarseLocationProvider
import ch.stenzel.tim.polleninfo.core.push.PushTokenProvider
import ch.stenzel.tim.polleninfo.core.push.UnavailablePushTokenProvider
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.Path.Companion.toPath
import org.koin.core.module.Module
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

actual val platformModule: Module = module {
    single<DataStore<Preferences>> { createDataStore() }
    single<CoarseLocationProvider> { IosCoarseLocationProvider() }
    single<PushTokenProvider> { UnavailablePushTokenProvider() }
}

/**
 * `Dispatchers.Default`, not `Dispatchers.IO`: the latter is `internal` in kotlinx-coroutines on
 * Kotlin/Native, so it cannot be used here. Each platform actual picks its own dispatcher for
 * exactly this reason — the choice is not portable.
 */
private fun createDataStore(): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        produceFile = { dataStorePath().toPath() },
    )

/**
 * The app's Documents directory — the iOS equivalent of Android's private files directory.
 *
 * Untested by construction: there is no iOS app wrapper in this project, so this path is
 * compile-verified only and will first run when the wrapper is created.
 */
@OptIn(ExperimentalForeignApi::class)
private fun dataStorePath(): String {
    val documentDirectory: NSURL? = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path) { "Documents directory is unavailable" } +
        "/$DATA_STORE_FILE_NAME"
}

private const val DATA_STORE_FILE_NAME = "polleninfo_preferences.preferences_pb"
