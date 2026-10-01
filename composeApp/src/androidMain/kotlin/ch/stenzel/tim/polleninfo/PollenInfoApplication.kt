package ch.stenzel.tim.polleninfo

import android.app.Application
import ch.stenzel.tim.polleninfo.core.di.appModules
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Starts Koin once per process.
 *
 * Not in `MainActivity.onCreate`: the activity is recreated on every configuration change — a dark
 * mode switch, a rotation — and a second `startKoin` throws `KoinApplicationAlreadyStartedException`.
 */
class PollenInfoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@PollenInfoApplication)
            modules(appModules)
        }
    }
}
