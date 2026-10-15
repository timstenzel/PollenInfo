package ch.stenzel.tim.polleninfo.core.diary.data.repository

import ch.stenzel.tim.polleninfo.core.diary.data.mapper.toDomain
import ch.stenzel.tim.polleninfo.core.diary.data.mapper.toWireName
import ch.stenzel.tim.polleninfo.core.diary.data.remote.DiaryApiService
import ch.stenzel.tim.polleninfo.core.diary.domain.model.DiaryEntry
import ch.stenzel.tim.polleninfo.core.diary.domain.model.Feeling
import ch.stenzel.tim.polleninfo.core.diary.domain.repository.DiaryRepository
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.result.safeCall
import ch.stenzel.tim.polleninfo.core.session.domain.model.SignedOutException
import ch.stenzel.tim.polleninfo.core.session.domain.repository.SessionRepository
import kotlinx.coroutines.flow.first

/**
 * [DiaryRepository] over the backend, with the token of [sessions]' current session. No session
 * fails at once, without a request; a `401` forgets the session, so every screen observing it
 * returns to its signed-out state.
 */
class DiaryRepositoryImpl(
    private val api: DiaryApiService,
    private val sessions: SessionRepository,
) : DiaryRepository {

    override suspend fun record(feeling: Feeling): Result<DiaryEntry> =
        withSession { token -> api.record(token, feeling.toWireName()).toDomain() }

    override suspend fun today(): Result<DiaryEntry?> =
        withSession { token -> api.today(token)?.toDomain() }

    private suspend fun <T> withSession(call: suspend (token: String) -> T): Result<T> {
        val session = sessions.session.first() ?: return Result.Failure(SignedOutException())
        return safeCall { call(session.token) }.also { result ->
            // Only if it is still the session that was refused: one logged in meanwhile stays.
            if (result is Result.Failure && result.exception is SignedOutException &&
                sessions.session.first() == session
            ) {
                sessions.clear()
            }
        }
    }
}
