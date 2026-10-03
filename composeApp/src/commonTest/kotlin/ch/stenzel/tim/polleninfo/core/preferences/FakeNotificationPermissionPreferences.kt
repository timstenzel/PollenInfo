package ch.stenzel.tim.polleninfo.core.preferences

import ch.stenzel.tim.polleninfo.core.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-memory stand-in for [DataStoreNotificationPermissionPreferences]. */
class FakeNotificationPermissionPreferences(
    initial: Boolean = false,
) : NotificationPermissionPreferences {

    private val _askedBefore = MutableStateFlow(initial)
    override val askedBefore: Flow<Boolean> = _askedBefore.asStateFlow()

    override suspend fun markAsked(): Result<Unit> {
        _askedBefore.value = true
        return Result.Success(Unit)
    }

    /** The currently stored value, for assertions that do not want to collect the flow. */
    val stored: Boolean get() = _askedBefore.value
}
