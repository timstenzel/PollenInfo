package ch.stenzel.tim.polleninfo.feature.alarms.data.repository

import ch.stenzel.tim.polleninfo.core.preferences.DeviceRegistrationRepository
import ch.stenzel.tim.polleninfo.core.push.PushTokenProvider
import ch.stenzel.tim.polleninfo.core.push.PushTokenResult
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.result.safeCall
import ch.stenzel.tim.polleninfo.feature.alarms.data.mapper.toDomain
import ch.stenzel.tim.polleninfo.feature.alarms.data.remote.AlarmApiService
import ch.stenzel.tim.polleninfo.feature.alarms.data.remote.UnknownDeviceException
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.Alarm
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.PushUnavailableException
import ch.stenzel.tim.polleninfo.feature.alarms.domain.repository.AlarmRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Registers lazily: no device id is requested until a call needs one, and the id is stored and
 * reused from then on. If the backend has forgotten the id, the call re-registers once and is
 * retried once; every call goes through [withDevice] so none can forget to.
 */
class AlarmRepositoryImpl(
    private val api: AlarmApiService,
    private val registration: DeviceRegistrationRepository,
    private val pushTokens: PushTokenProvider,
) : AlarmRepository {

    /** Serialises registration, so two calls on a fresh install cannot register two devices. */
    private val registrationLock = Mutex()

    override suspend fun alarms(): Result<List<Alarm>> = safeCall {
        withDevice { deviceId -> api.getAlarms(deviceId).toDomain() }
    }

    private suspend fun <T> withDevice(call: suspend (deviceId: String) -> T): T {
        val deviceId = ensureRegistered()
        return try {
            call(deviceId)
        } catch (e: UnknownDeviceException) {
            call(replaceRegistration(stale = deviceId))
        }
    }

    private suspend fun ensureRegistered(): String = registrationLock.withLock {
        registration.deviceId.first() ?: register()
    }

    /**
     * Drops [stale] and registers again — unless a concurrent call has already done so, in which
     * case its id is used rather than replaced a second time.
     */
    private suspend fun replaceRegistration(stale: String): String = registrationLock.withLock {
        val current = registration.deviceId.first()
        if (current != null && current != stale) return@withLock current
        registration.clear().orThrow()
        register()
    }

    /** Must be called holding [registrationLock]. */
    private suspend fun register(): String {
        val token = when (val result = pushTokens.token()) {
            is PushTokenResult.Available -> result.token
            PushTokenResult.Unavailable -> throw PushUnavailableException()
        }
        val deviceId = api.registerDevice(token).deviceId
        // An id that was not stored is lost: fail this call rather than carry on with it, and the
        // next call registers again.
        registration.store(deviceId).orThrow()
        return deviceId
    }
}

private fun <T> Result<T>.orThrow(): T = when (this) {
    is Result.Success -> data
    is Result.Failure -> throw exception
}
