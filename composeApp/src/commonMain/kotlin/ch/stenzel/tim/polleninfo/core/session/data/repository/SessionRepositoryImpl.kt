package ch.stenzel.tim.polleninfo.core.session.data.repository

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.result.safeCall
import ch.stenzel.tim.polleninfo.core.session.data.local.SessionStorage
import ch.stenzel.tim.polleninfo.core.session.data.remote.AccountApiService
import ch.stenzel.tim.polleninfo.core.session.domain.model.Session
import ch.stenzel.tim.polleninfo.core.session.domain.model.SignedOutException
import ch.stenzel.tim.polleninfo.core.session.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class SessionRepositoryImpl(
    private val api: AccountApiService,
    private val storage: SessionStorage,
) : SessionRepository {

    override val session: Flow<Session?> = storage.session

    override suspend fun register(email: String, password: String): Result<Unit> = safeCall {
        val response = api.register(email.trim(), password)
        storage.store(Session(token = response.sessionToken, email = normalizeEmail(email)))
    }

    override suspend fun login(email: String, password: String): Result<Unit> = safeCall {
        val response = api.login(email.trim(), password)
        storage.store(Session(token = response.sessionToken, email = normalizeEmail(email)))
    }

    override suspend fun logout(): Result<Unit> {
        val session = storage.session.first() ?: return Result.Success(Unit)
        // Its outcome does not matter: a 401 means the backend has ended the session already, and
        // after any other failure it expires there unused.
        safeCall { api.logout(session.token) }
        return safeCall { storage.clear() }
    }

    override suspend fun deleteAccount(password: String): Result<Unit> {
        val session = storage.session.first() ?: return Result.Failure(SignedOutException())
        val result = safeCall { api.deleteAccount(session.token, password) }
        // Deleted, or the backend no longer knows the session: either way it is gone. Only clear
        // it if it is still the one used, so a newer login is never thrown away.
        val gone = result is Result.Success ||
            (result is Result.Failure && result.exception is SignedOutException)
        if (gone && storage.session.first()?.token == session.token) {
            val cleared = safeCall { storage.clear() }
            if (result is Result.Success) return cleared
        }
        return result
    }

    override suspend fun clear() {
        storage.clear()
    }

    /** The address as the backend stores it, so Settings shows the account's own spelling. */
    private fun normalizeEmail(email: String) = email.trim().lowercase()
}
