package ch.stenzel.tim.polleninfo.core.preferences

import ch.stenzel.tim.polleninfo.core.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.io.IOException

/** In-memory stand-in for [DataStoreCrashReportingPreferences]; `true` when nothing is stored. */
class FakeCrashReportingPreferences(
    initial: Boolean = true,
) : CrashReportingPreferences {

    private val _enabled = MutableStateFlow(initial)
    override val enabled: Flow<Boolean> = _enabled.asStateFlow()

    /** Every value passed to [setEnabled], in order. */
    val writes = mutableListOf<Boolean>()

    /** When set, the next [setEnabled] fails with it and stores nothing; later writes succeed again. */
    var failNextWrite: Exception? = null

    override suspend fun setEnabled(enabled: Boolean): Result<Unit> {
        writes += enabled
        failNextWrite?.let { failure ->
            failNextWrite = null
            return Result.Failure(failure)
        }
        _enabled.value = enabled
        return Result.Success(Unit)
    }

    /** Makes the next write fail as a DataStore that cannot write its file would. */
    fun failNextWrite() {
        failNextWrite = IOException("disk full")
    }

    /** The currently stored value, for assertions that do not want to collect the flow. */
    val stored: Boolean get() = _enabled.value
}
