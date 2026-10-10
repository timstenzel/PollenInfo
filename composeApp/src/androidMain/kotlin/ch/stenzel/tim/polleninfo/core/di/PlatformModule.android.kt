package ch.stenzel.tim.polleninfo.core.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import ch.stenzel.tim.polleninfo.core.appinfo.AndroidAppInfo
import ch.stenzel.tim.polleninfo.core.appinfo.AppInfo
import ch.stenzel.tim.polleninfo.core.language.AndroidLanguageRepository
import ch.stenzel.tim.polleninfo.core.language.LanguageRepository
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
    single<DataStore<Preferences>> { createDataStore(get(), DATA_STORE_NAME) }
    single<DataStore<Preferences>>(DEVICE_DATA_STORE) { createDataStore(get(), DEVICE_DATA_STORE_NAME) }
    single<CoarseLocationProvider> { AndroidCoarseLocationProvider(get()) }
    single<PushTokenProvider> { FirebasePushTokenProvider() }
    single<AppInfo> { AndroidAppInfo(get()) }
    single<LanguageRepository> { AndroidLanguageRepository() }
}

/**
 * The application `Context` comes from Koin — `MainActivity` installs it with `androidContext()`.
 * `preferencesDataStoreFile` puts the file in the app's private `datastore/` directory, as
 * `datastore/<name>.preferences_pb` — the path `androidApp`'s backup rules name.
 */
private fun createDataStore(context: Context, name: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.create(
        scope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
        produceFile = { context.preferencesDataStoreFile(name) },
    )

private const val DATA_STORE_NAME = "polleninfo_preferences"
