package ch.stenzel.tim.polleninfo

import android.app.Application
import ch.stenzel.tim.polleninfo.core.crashreporting.CrashReportingSync
import ch.stenzel.tim.polleninfo.core.di.appModules
import ch.stenzel.tim.polleninfo.core.push.createAlarmNotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

/**
 * Starts Koin once per process, creates the alarm notification channels and keeps crash reporting in
 * step with the user's choice.
 *
 * Not in `MainActivity.onCreate`: the activity is recreated on every configuration change — a dark
 * mode switch, a rotation — and a second `startKoin` throws `KoinApplicationAlreadyStartedException`.
 * The channels must exist before the first push arrives, which may be in a process FCM started with
 * no activity at all. `MainActivity` creates them again, which renames them after a language change.
 * The crash-report choice must likewise hold in such a process, so its sync starts here too.
 */
class PollenInfoApplication : Application() {

    /** Lives as long as the process; nothing cancels it. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        val koin = startKoin {
            androidContext(this@PollenInfoApplication)
            // The deployed backend for a release, the development server for a debug build.
            modules(appModules(BuildConfig.API_BASE_URL))
        }.koin
        createAlarmNotificationChannels(this)
        val crashReportingSync = koin.get<CrashReportingSync>()
        applicationScope.launch { crashReportingSync.run() }
    }
}
