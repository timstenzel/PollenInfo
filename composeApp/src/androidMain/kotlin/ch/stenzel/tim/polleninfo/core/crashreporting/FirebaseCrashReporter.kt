package ch.stenzel.tim.polleninfo.core.crashreporting

import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Crashlytics' collection switch. Firebase persists the value, so a crash in the first moments of
 * the next launch — before the stored choice has been read again — still honours it.
 *
 * [collectionAllowed] is `false` in a debug build. The debug manifest already turns collection off,
 * but a runtime call takes precedence over the manifest, so this must never turn it back on: it then
 * disables collection whatever it is given. Checked by hand (see `deploy/README.md`).
 */
class FirebaseCrashReporter(private val collectionAllowed: Boolean) : CrashReporter {

    override val isAvailable: Boolean = true

    override fun setEnabled(enabled: Boolean) {
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(enabled && collectionAllowed)
    }
}
