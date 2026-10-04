package ch.stenzel.tim.polleninfo.core.preferences

import ch.stenzel.tim.polleninfo.core.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory stand-in for [DataStoreDeviceRegistrationRepository]. */
class FakeDeviceRegistrationRepository(
    initial: String? = null,
) : DeviceRegistrationRepository {

    private val _deviceId = MutableStateFlow(initial)
    override val deviceId: Flow<String?> = _deviceId.asStateFlow()

    /** Every id ever stored, in order, so a test can tell "reused" from "registered again". */
    val storedIds = mutableListOf<String>()

    var clearCount: Int = 0
        private set

    override suspend fun store(id: String): Result<Unit> {
        storedIds += id
        _deviceId.value = id
        return Result.Success(Unit)
    }

    override suspend fun clear(): Result<Unit> {
        clearCount++
        _deviceId.value = null
        return Result.Success(Unit)
    }

    /** The currently stored id, for assertions that do not want to collect the flow. */
    val stored: String? get() = _deviceId.value
}
