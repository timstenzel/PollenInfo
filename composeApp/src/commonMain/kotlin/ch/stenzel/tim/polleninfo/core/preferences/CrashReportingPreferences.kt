package ch.stenzel.tim.polleninfo.core.preferences

import ch.stenzel.tim.polleninfo.core.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * Whether the user lets the app send crash reports. On until switched off; an ordinary preference,
 * not a secret, so it travels with the device backup like the selected station.
 *
 * Settings writes it; `CrashReportingSync` passes every value on to the platform's reporter.
 */
interface CrashReportingPreferences {

    /** `true` while nothing is stored. */
    val enabled: Flow<Boolean>

    suspend fun setEnabled(enabled: Boolean): Result<Unit>
}
