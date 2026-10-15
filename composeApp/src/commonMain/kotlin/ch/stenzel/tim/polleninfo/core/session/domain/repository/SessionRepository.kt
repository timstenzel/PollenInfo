package ch.stenzel.tim.polleninfo.core.session.domain.repository

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.session.domain.model.Session
import kotlinx.coroutines.flow.Flow

/**
 * Whether, and to which account, this phone is logged in. The app works without an account; only
 * the diary needs one.
 */
interface SessionRepository {

    /** `null` while nobody is logged in. */
    val session: Flow<Session?>

    /**
     * Creates an account and logs in to it. Consent to storing the diary is implied by the form,
     * which cannot be submitted without it, and is sent as `true`.
     */
    suspend fun register(email: String, password: String): Result<Unit>

    /** Logs in to an existing account; a wrong email or password is `InvalidCredentialsException`. */
    suspend fun login(email: String, password: String): Result<Unit>

    /**
     * Ends this phone's session on the backend and forgets it here. It is forgotten even when the
     * request fails — the user asked to be logged out of this phone, and a session the backend still
     * holds expires on its own — so this fails only if the phone cannot clear its own storage.
     */
    suspend fun logout(): Result<Unit>

    /**
     * Deletes the account with every answer on the backend, once [password] confirms it, and then
     * logs out. A wrong password is `InvalidCredentialsException` and keeps the session; a backend
     * that no longer knows the session is `SignedOutException`, and the session is forgotten.
     */
    suspend fun deleteAccount(password: String): Result<Unit>

    /**
     * Forgets the stored session on this phone, without asking the backend — what any repository
     * does when the backend answers `401` to the session (`SignedOutException`).
     */
    suspend fun clear()
}
