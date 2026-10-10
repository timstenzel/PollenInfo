package ch.stenzel.tim.polleninfo.feature.alarms.data.repository

import ch.stenzel.tim.polleninfo.core.preferences.DeviceRegistrationRepository
import ch.stenzel.tim.polleninfo.core.push.PushTokenProvider
import ch.stenzel.tim.polleninfo.core.push.PushTokenResult
import ch.stenzel.tim.polleninfo.core.push.PushTokenUpdater
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.result.safeCall
import ch.stenzel.tim.polleninfo.feature.alarms.data.mapper.toDomain
import ch.stenzel.tim.polleninfo.feature.alarms.data.mapper.toInputDto
import ch.stenzel.tim.polleninfo.feature.alarms.data.remote.AlarmApiService
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.Alarm
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmDraft
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmNotFoundException
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.PushUnavailableException
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.UnknownDeviceException
import ch.stenzel.tim.polleninfo.feature.alarms.domain.repository.AlarmRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Registers lazily: no device token is requested until a call needs one, and the token is stored and
 * reused from then on. If the backend no longer accepts it (`401`), the call re-registers once and is
 * retried once; every call goes through [withDevice] so none can forget to.
 *
 * It is also the [PushTokenUpdater], since a rotated token belongs to the registration it keeps.
 */
class AlarmRepositoryImpl(
    private val api: AlarmApiService,
    private val registration: DeviceRegistrationRepository,
    private val pushTokens: PushTokenProvider,
) : AlarmRepository, PushTokenUpdater {

    /** Serialises registration, so two calls on a fresh install cannot register two devices. */
    private val registrationLock = Mutex()

    override suspend fun alarms(): Result<List<Alarm>> = safeCall {
        withDevice { deviceToken -> api.getAlarms(deviceToken).toDomain() }
    }

    override suspend fun create(draft: AlarmDraft): Result<Alarm> = safeCall {
        withDevice { deviceToken -> api.createAlarm(deviceToken, draft.toInputDto()).toDomain() }
    }

    /** There is no single-alarm endpoint; the list is small and is what the backend offers. */
    override suspend fun alarm(id: String): Result<Alarm> = safeCall {
        withDevice { deviceToken -> api.getAlarms(deviceToken).firstOrNull { it.id == id }?.toDomain() }
            ?: throw AlarmNotFoundException()
    }

    override suspend fun update(id: String, draft: AlarmDraft): Result<Alarm> = safeCall {
        withDevice { deviceToken -> api.updateAlarm(deviceToken, id, draft.toInputDto()).toDomain() }
    }

    override suspend fun delete(id: String): Result<Unit> = safeCall {
        withDevice { deviceToken -> api.deleteAlarm(deviceToken, id) }
    }

    /**
     * Never registers: on an install that has not registered yet the first registration sends the
     * push address current then. The lock makes a rotation during that first registration wait for
     * its device token rather than be lost. If the backend no longer accepts the device token (`401`),
     * it is dropped — only if it is still the one used — so the next alarm call registers afresh,
     * with the current push address.
     */
    override suspend fun updateToken(token: String): Result<Unit> = safeCall {
        val deviceToken = registrationLock.withLock { registration.deviceToken.first() } ?: return@safeCall
        try {
            api.updateToken(deviceToken, token)
        } catch (e: UnknownDeviceException) {
            registrationLock.withLock {
                if (registration.deviceToken.first() == deviceToken) registration.clear().orThrow()
            }
        }
    }

    private suspend fun <T> withDevice(call: suspend (deviceToken: String) -> T): T {
        val deviceToken = ensureRegistered()
        return try {
            call(deviceToken)
        } catch (e: UnknownDeviceException) {
            call(replaceRegistration(stale = deviceToken))
        }
    }

    private suspend fun ensureRegistered(): String = registrationLock.withLock {
        registration.deviceToken.first() ?: register()
    }

    /**
     * Drops [stale] and registers again — unless a concurrent call has already done so, in which
     * case its token is used rather than replaced a second time.
     */
    private suspend fun replaceRegistration(stale: String): String = registrationLock.withLock {
        val current = registration.deviceToken.first()
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
        val deviceToken = api.registerDevice(token).deviceToken
        // A token that was not stored is lost: fail this call rather than carry on with it, and the
        // next call registers again.
        registration.store(deviceToken).orThrow()
        return deviceToken
    }
}

private fun <T> Result<T>.orThrow(): T = when (this) {
    is Result.Success -> data
    is Result.Failure -> throw exception
}
