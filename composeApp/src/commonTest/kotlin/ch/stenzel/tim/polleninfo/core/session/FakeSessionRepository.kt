package ch.stenzel.tim.polleninfo.core.session

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.session.domain.model.Session
import ch.stenzel.tim.polleninfo.core.session.domain.repository.SessionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** A logged-in session for tests that do not care which. */
val testSession = Session(token = "session-token-1", email = "tim@example.com")

/**
 * In-memory [SessionRepository]: a settable [current] session, gates to hold a registration or a
 * login in flight and scripted results. A successful registration or login logs in; [logout] logs
 * out and counts the calls.
 */
class FakeSessionRepository(initial: Session? = null) : SessionRepository {

    private val _session = MutableStateFlow(initial)
    override val session: Flow<Session?> = _session.asStateFlow()

    /** The session as stored; set it to log in or out from a test. */
    var current: Session?
        get() = _session.value
        set(value) {
            _session.value = value
        }

    var registerResult: Result<Unit> = Result.Success(Unit)
    var registerGate: CompletableDeferred<Unit>? = null

    /** Every registration asked for, as email and password. */
    val registrations = mutableListOf<Pair<String, String>>()

    override suspend fun register(email: String, password: String): Result<Unit> {
        registrations += email to password
        registerGate?.await()
        return registerResult.also {
            if (it is Result.Success) _session.value = Session(token = "registered-token", email = email)
        }
    }

    var loginResult: Result<Unit> = Result.Success(Unit)
    var loginGate: CompletableDeferred<Unit>? = null

    /** Every login asked for, as email and password. */
    val logins = mutableListOf<Pair<String, String>>()

    override suspend fun login(email: String, password: String): Result<Unit> {
        logins += email to password
        loginGate?.await()
        return loginResult.also {
            if (it is Result.Success) _session.value = Session(token = "login-token", email = email)
        }
    }

    var logoutCount = 0
        private set

    override suspend fun logout(): Result<Unit> {
        logoutCount++
        _session.value = null
        return Result.Success(Unit)
    }

    override suspend fun clear() {
        _session.value = null
    }
}
