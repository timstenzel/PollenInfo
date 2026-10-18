package ch.stenzel.tim.polleninfo.core.crashreporting

import ch.stenzel.tim.polleninfo.core.preferences.CrashReportingPreferences
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Hands the stored crash-report choice to the [reporter]: once at start and again after every
 * change, so switching it off in Settings is off at once. The preference is the single source of
 * truth; nothing else calls [CrashReporter.setEnabled].
 *
 * Started once per process by `PollenInfoApplication`, on a scope that lives as long as the process.
 * A failure to read the preference is swallowed — the reporter then keeps the value it persisted at
 * the last launch, which is still the user's last choice.
 */
class CrashReportingSync(
    private val preferences: CrashReportingPreferences,
    private val reporter: CrashReporter,
) {
    /** Applies the stored choice now and after every change; returns only if there is nothing to sync. */
    suspend fun run() {
        if (!reporter.isAvailable) return
        preferences.enabled
            .distinctUntilChanged()
            .catch { }
            .collect(reporter::setEnabled)
    }
}
