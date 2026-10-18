package ch.stenzel.tim.polleninfo.core.preferences

import ch.stenzel.tim.polleninfo.core.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory stand-in for [DataStoreCrashReportingPreferences]; `true` when nothing is stored. */
class FakeCrashReportingPreferences(
    initial: Boolean = true,
) : CrashReportingPreferences {

    private val _enabled = MutableStateFlow(initial)
    override val enabled: Flow<Boolean> = _enabled.asStateFlow()

    /** Every value passed to [setEnabled], in order. */
    val writes = mutableListOf<Boolean>()

    override suspend fun setEnabled(enabled: Boolean): Result<Unit> {
        writes += enabled
        _enabled.value = enabled
        return Result.Success(Unit)
    }

    /** The currently stored value, for assertions that do not want to collect the flow. */
    val stored: Boolean get() = _enabled.value
}
