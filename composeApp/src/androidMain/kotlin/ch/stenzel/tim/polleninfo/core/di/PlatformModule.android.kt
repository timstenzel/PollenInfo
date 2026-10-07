package ch.stenzel.tim.polleninfo.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import ch.stenzel.tim.polleninfo.core.appinfo.AndroidAppInfo
import ch.stenzel.tim.polleninfo.core.appinfo.AppInfo
import ch.stenzel.tim.polleninfo.core.location.AndroidCoarseLocationProvider
import ch.stenzel.tim.polleninfo.core.location.CoarseLocationProvider
import ch.stenzel.tim.polleninfo.core.push.FirebasePushTokenProvider
import ch.stenzel.tim.polleninfo.core.push.PushTokenProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.core.module.Module
import org.koin.dsl.module

actual val platformModule: Module = module {
    single<DataStore<Preferences>> { createDataStore(get()) }
    single<CoarseLocationProvider> { AndroidCoarseLocationProvider(get()) }
    single<PushTokenProvider> { FirebasePushTokenProvider() }
    single<AppInfo> { AndroidAppInfo(get()) }
}

/**
 * The application `Context` comes from Koin — `MainActivity` installs it with `androidContext()`.
 * `preferencesDataStoreFile` puts the file in the app's private `datastore/` directory.
 */
private fun createDataStore(context: Context): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        produceFile = { context.preferencesDataStoreFile(DATA_STORE_NAME) },
    )

private const val DATA_STORE_NAME = "polleninfo_preferences"
