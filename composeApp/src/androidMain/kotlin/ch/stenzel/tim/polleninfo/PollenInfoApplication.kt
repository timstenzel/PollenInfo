package ch.stenzel.tim.polleninfo

import android.app.Application
import ch.stenzel.tim.polleninfo.core.di.appModules
import ch.stenzel.tim.polleninfo.core.push.createAlarmNotificationChannels
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Starts Koin once per process, and creates the alarm notification channels.
 *
 * Not in `MainActivity.onCreate`: the activity is recreated on every configuration change — a dark
 * mode switch, a rotation — and a second `startKoin` throws `KoinApplicationAlreadyStartedException`.
 * The channels must exist before the first push arrives, which may be in a process FCM started with
 * no activity at all.
 */
class PollenInfoApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@PollenInfoApplication)
            modules(appModules)
        }
        createAlarmNotificationChannels(this)
    }
}
