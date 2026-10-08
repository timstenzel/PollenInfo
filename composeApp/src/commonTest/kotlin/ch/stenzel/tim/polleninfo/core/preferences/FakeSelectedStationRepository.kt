package ch.stenzel.tim.polleninfo.core.preferences

import ch.stenzel.tim.polleninfo.core.result.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-memory stand-in for the DataStore-backed repository. Hand-written — the project adds no
 * mocking framework.
 *
 * [failWrite] makes [select] fail so the save-error path can be driven without a broken filesystem.
 * [writeGate], when set, holds every [select] until it is completed, so a test can observe a save
 * in flight.
 */
class FakeSelectedStationRepository(
    initial: SelectedStation? = null,
    var failWrite: Boolean = false,
) : SelectedStationRepository {

    private val _selectedStation = MutableStateFlow(initial)
    override val selectedStation: Flow<SelectedStation?> = _selectedStation.asStateFlow()

    /** Everything handed to [select], in order — including writes that were made to fail. */
    val writes = mutableListOf<SelectedStation>()

    var writeGate: CompletableDeferred<Unit>? = null

    override suspend fun select(station: SelectedStation): Result<Unit> {
        writes += station
        writeGate?.await()
        if (failWrite) return Result.Failure(RuntimeException("disk full"))
        _selectedStation.value = station
        return Result.Success(Unit)
    }

    /** The currently stored value, for assertions that do not want to collect the flow. */
    val stored: SelectedStation? get() = _selectedStation.value
}
