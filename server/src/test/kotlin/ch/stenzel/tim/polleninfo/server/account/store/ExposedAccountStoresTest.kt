package ch.stenzel.tim.polleninfo.server.account.store

import ch.stenzel.tim.polleninfo.server.account.domain.AccountSpec
import ch.stenzel.tim.polleninfo.server.account.domain.UserId
import ch.stenzel.tim.polleninfo.server.account.domain.hash
import ch.stenzel.tim.polleninfo.server.account.domain.newSessionToken
import ch.stenzel.tim.polleninfo.server.alarm.store.TestPostgres
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.toJavaDuration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import kotlin.test.assertFailsWith

class ExposedAccountStoresTest {

    private val database = TestPostgres.cleanDatabase()
    private val users = ExposedUserStore(database)
    private val sessions = ExposedSessionStore(database)

    private val now = Instant.parse("2026-10-12T08:30:00Z")

    private fun spec(email: String = "tim@example.com") = AccountSpec(email, "correct horse")

    private suspend fun registered(email: String = "tim@example.com"): UserId =
        assertIs<RegisterResult.Registered>(users.register(spec(email), "hash-of-$email", now)).userId

    // --- Users ---

    @Test
    fun `a registered user is stored with its hash and the consent and creation time`() = runTest {
        val id = registered()

        val row = transaction(database) { UsersTable.selectAll().single() }
        assertEquals(id.value, row[UsersTable.id])
        assertEquals("tim@example.com", row[UsersTable.email])
        assertEquals("hash-of-tim@example.com", row[UsersTable.passwordHash])
        assertEquals(now, row[UsersTable.consentedAt].toInstant())
        assertEquals(now, row[UsersTable.createdAt].toInstant())
    }

    @Test
    fun `a second registration of one email is EmailTaken and stores nothing`() = runTest {
        registered()

        assertEquals(RegisterResult.EmailTaken, users.register(spec(), "another-hash", now.plusSeconds(60)))

        val rows = transaction(database) { UsersTable.selectAll().toList() }
        assertEquals(listOf("hash-of-tim@example.com"), rows.map { it[UsersTable.passwordHash] })
    }

    @Test
    fun `two different emails are two users`() = runTest {
        assertNotEquals(registered("tim@example.com"), registered("anna@example.com"))
    }

    @Test
    fun `two concurrent registrations of one email yield exactly one Registered`() = runTest {
        // Several emails, so a check-then-insert race has more than one chance to show.
        repeat(5) { round ->
            val email = "user$round@example.com"
            val start = CompletableDeferred<Unit>()

            val results = List(2) {
                async(Dispatchers.IO) {
                    start.await()
                    users.register(spec(email), "hash", now)
                }
            }.also { start.complete(Unit) }.awaitAll()

            assertEquals(1, results.count { it is RegisterResult.Registered }, "results: $results")
            assertEquals(1, results.count { it == RegisterResult.EmailTaken }, "results: $results")
        }
        assertEquals(5L, transaction(database) { UsersTable.selectAll().count() })
    }

    // --- Sessions ---

    @Test
    fun `a session row holds the token's hash and not the token`() = runTest {
        val id = registered()

        val token = sessions.create(id, now)

        val row = transaction(database) { SessionsTable.selectAll().single() }
        assertContentEquals(token.hash(), row[SessionsTable.tokenHash])
        assertEquals(id.value, row[SessionsTable.userId])
        assertEquals(now, row[SessionsTable.createdAt].toInstant())
        assertEquals(now, row[SessionsTable.lastUsedAt].toInstant())
        assertEquals(ZoneOffset.UTC, row[SessionsTable.createdAt].offset)
    }

    @Test
    fun `every session of a user gets its own token`() = runTest {
        val id = registered()

        val first = sessions.create(id, now)
        val second = sessions.create(id, now)

        assertNotEquals(first, second)
        assertEquals(
            2L,
            transaction(database) { SessionsTable.selectAll().where { SessionsTable.userId eq id.value }.count() },
        )
    }

    @Test
    fun `a session cannot be created for a user that does not exist`() = runTest {
        assertFailsWith<ExposedSQLException> { sessions.create(UserId(UUID.randomUUID()), now) }
    }

    @Test
    fun `credentials are found by the normalised email and are null for an unknown one`() = runTest {
        val id = registered()

        val stored = assertNotNull(users.credentials("tim@example.com"))
        assertEquals(id, stored.userId)
        assertEquals("hash-of-tim@example.com", stored.passwordHash)
        assertNull(users.credentials("anna@example.com"))
    }

    @Test
    fun `a session authenticates as its user and an unknown token as nobody`() = runTest {
        val id = registered()
        val token = sessions.create(id, now)

        assertEquals(id, sessions.authenticate(token, now))
        assertNull(sessions.authenticate(newSessionToken(), now))
    }

    @Test
    fun `a session unused for exactly 90 days is accepted and refused 1 ms later`() = runTest {
        val id = registered()
        val kept = sessions.create(id, now)
        val expired = sessions.create(id, now)

        assertEquals(id, sessions.authenticate(kept, now.plus(SESSION_EXPIRY.toJavaDuration())))
        assertNull(sessions.authenticate(expired, now.plus(SESSION_EXPIRY.toJavaDuration()).plusMillis(1)))
    }

    @Test
    fun `an expired session stays refused even though it is still stored`() = runTest {
        val token = sessions.create(registered(), now)
        val later = now.plus(SESSION_EXPIRY.toJavaDuration()).plusSeconds(1)

        assertNull(sessions.authenticate(token, later))
        assertNull(sessions.authenticate(token, later))
        assertEquals(1L, transaction(database) { SessionsTable.selectAll().count() })
    }

    @Test
    fun `last_used_at is written at most once per day of use`() = runTest {
        val token = sessions.create(registered(), now)

        sessions.authenticate(token, now.plus(Duration.ofHours(23)))
        assertEquals(now, lastUsedAt())

        val nextDay = now.plus(Duration.ofDays(1))
        sessions.authenticate(token, nextDay)
        assertEquals(nextDay, lastUsedAt())

        sessions.authenticate(token, nextDay.plus(Duration.ofHours(12)))
        assertEquals(nextDay, lastUsedAt())
    }

    @Test
    fun `regular use keeps a session valid well past 90 days after its creation`() = runTest {
        val id = registered()
        val token = sessions.create(id, now)

        sessions.authenticate(token, now.plus(Duration.ofDays(60)))

        assertEquals(id, sessions.authenticate(token, now.plus(Duration.ofDays(120))))
    }

    @Test
    fun `deleting a session ends only that one`() = runTest {
        val id = registered()
        val ended = sessions.create(id, now)
        val other = sessions.create(id, now)

        sessions.delete(ended)

        assertNull(sessions.authenticate(ended, now))
        assertEquals(id, sessions.authenticate(other, now))
        // An unknown token is nothing to delete.
        sessions.delete(newSessionToken())
        assertEquals(1L, transaction(database) { SessionsTable.selectAll().count() })
    }

    private fun lastUsedAt(): Instant =
        transaction(database) { SessionsTable.selectAll().single()[SessionsTable.lastUsedAt].toInstant() }
}
