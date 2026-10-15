package ch.stenzel.tim.polleninfo.core.session.data.repository

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.result.safeCall
import ch.stenzel.tim.polleninfo.core.session.data.local.SessionStorage
import ch.stenzel.tim.polleninfo.core.session.data.remote.AccountApiService
import ch.stenzel.tim.polleninfo.core.session.domain.model.Session
import ch.stenzel.tim.polleninfo.core.session.domain.repository.SessionRepository
import kotlinx.coroutines.flow.Flow

class SessionRepositoryImpl(
    private val api: AccountApiService,
    private val storage: SessionStorage,
) : SessionRepository {

    override val session: Flow<Session?> = storage.session

    override suspend fun register(email: String, password: String): Result<Unit> = safeCall {
        val response = api.register(email.trim(), password)
        storage.store(Session(token = response.sessionToken, email = normalizeEmail(email)))
    }

    override suspend fun clear() {
        storage.clear()
    }

    /** The address as the backend stores it, so Settings shows the account's own spelling. */
    private fun normalizeEmail(email: String) = email.trim().lowercase()
}
