package ch.stenzel.tim.polleninfo.core.diary

import ch.stenzel.tim.polleninfo.core.diary.domain.repository.FeelingPromptPreferences
import ch.stenzel.tim.polleninfo.core.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate

/**
 * In-memory stand-in for `DataStoreFeelingPromptPreferences`. One instance outlives the ViewModels
 * built over it, as the DataStore file outlives the app's process. [failWrite] makes [dismiss] fail
 * without storing anything.
 */
class FakeFeelingPromptPreferences(
    initialDismissedOn: LocalDate? = null,
    var failWrite: Boolean = false,
) : FeelingPromptPreferences {

    private val _dismissedOn = MutableStateFlow(initialDismissedOn)
    override val dismissedOn: Flow<LocalDate?> = _dismissedOn.asStateFlow()

    override suspend fun dismiss(date: LocalDate): Result<Unit> {
        if (failWrite) return Result.Failure(RuntimeException("disk full"))
        _dismissedOn.value = date
        return Result.Success(Unit)
    }

    /** The stored date, for assertions that do not want to collect the flow. */
    val storedDismissedOn: LocalDate? get() = _dismissedOn.value
}
