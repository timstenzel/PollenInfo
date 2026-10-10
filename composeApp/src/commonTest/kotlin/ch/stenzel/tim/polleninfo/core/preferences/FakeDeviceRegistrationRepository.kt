package ch.stenzel.tim.polleninfo.core.preferences

import ch.stenzel.tim.polleninfo.core.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory stand-in for [DataStoreDeviceRegistrationRepository]. */
class FakeDeviceRegistrationRepository(
    initial: String? = null,
) : DeviceRegistrationRepository {

    private val _deviceToken = MutableStateFlow(initial)
    override val deviceToken: Flow<String?> = _deviceToken.asStateFlow()

    /** Every token ever stored, in order, so a test can tell "reused" from "registered again". */
    val storedTokens = mutableListOf<String>()

    var clearCount: Int = 0
        private set

    override suspend fun store(token: String): Result<Unit> {
        storedTokens += token
        _deviceToken.value = token
        return Result.Success(Unit)
    }

    override suspend fun clear(): Result<Unit> {
        clearCount++
        _deviceToken.value = null
        return Result.Success(Unit)
    }

    /** The currently stored token, for assertions that do not want to collect the flow. */
    val stored: String? get() = _deviceToken.value
}
