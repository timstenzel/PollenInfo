package ch.stenzel.tim.polleninfo.server.alarm.store

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import org.flywaydb.core.Flyway
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * The schema as Flyway builds it and the two roles as `deploy/postgres/init/01-roles.sh` sets them
 * up: each test runs on its own database straight out of the init script, never migrated before.
 */
class MigrationTest {

    private val config = TestPostgres.unmigratedDatabase()

    @Test
    fun `V1 and V2 apply to an empty database and create every table`() {
        val result = PollenInfoDatabase.migrate(config)

        assertEquals(listOf("1", "2"), result.migrations.map { it.version })
        assertEquals(
            listOf(
                "alarms", "devices", "diary_entries", "flyway_schema_history", "notification_log", "scheduler_state",
                "sessions", "users",
            ),
            asOwner { connection ->
                connection.query(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name",
                )
            },
        )
    }

    @Test
    fun `V2 adds the account tables to a database at V1 and keeps its devices and alarms`() {
        // Production went live on V1 alone, with devices and alarms already stored.
        Flyway.configure()
            .dataSource(config.url, config.ownerUser, config.ownerPassword)
            .locations("classpath:db/migration")
            .target("1")
            .load()
            .migrate()
        asApp { connection ->
            connection.execute(
                """
                INSERT INTO devices (id, token_hash, fcm_token, created_at, last_seen_at)
                VALUES ('00000000-0000-0000-0000-0000000000d1', '\x00', 't', now(), now())
                """,
            )
            connection.execute(
                """
                INSERT INTO alarms (id, device_id, enabled, station_abbr, species, min_severity, days, type,
                                    at_time, created_at)
                VALUES ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000d1', true, 'PZH', '{BIRCH}', 'NONE',
                        '{MONDAY}', 'daily', '08:00', now())
                """,
            )
        }

        val result = PollenInfoDatabase.migrate(config)

        assertEquals(listOf("2"), result.migrations.map { it.version })
        asApp { connection ->
            assertEquals(listOf("1"), connection.query("SELECT count(*) FROM alarms"))
            // The app login received its privileges on V2's tables too.
            connection.execute(
                """
                INSERT INTO users (id, email, password_hash, consented_at, created_at)
                VALUES ('00000000-0000-0000-0000-0000000000a1', 'tim@example.com', 'h', now(), now())
                """,
            )
            assertEquals(listOf("1"), connection.query("SELECT count(*) FROM users"))
        }
    }

    @Test
    fun `a second migrate applies nothing`() {
        PollenInfoDatabase.migrate(config)

        val second = PollenInfoDatabase.migrate(config)

        assertEquals(0, second.migrationsExecuted)
    }

    @Test
    fun `the app role can select insert update and delete in every table`() {
        PollenInfoDatabase.migrate(config)

        asApp { connection ->
            connection.execute(
                """
                INSERT INTO devices (id, token_hash, fcm_token, created_at, last_seen_at)
                VALUES ('00000000-0000-0000-0000-0000000000d1', '\x00', 't', now(), now())
                """,
            )
            connection.execute(
                """
                INSERT INTO alarms (id, device_id, enabled, station_abbr, species, min_severity, days, type,
                                    at_time, created_at)
                VALUES ('00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000d1', true, 'PZH', '{BIRCH}', 'NONE',
                        '{MONDAY}', 'daily', '08:00', now())
                """,
            )
            connection.execute(
                "INSERT INTO notification_log VALUES ('00000000-0000-0000-0000-000000000001', 'BIRCH', '2026-08-03')",
            )
            connection.execute("INSERT INTO scheduler_state (last_minute) VALUES (now())")
            connection.execute(
                """
                INSERT INTO users (id, email, password_hash, consented_at, created_at)
                VALUES ('00000000-0000-0000-0000-0000000000a1', 'tim@example.com', 'h', now(), now())
                """,
            )
            connection.execute(
                """
                INSERT INTO sessions (token_hash, user_id, created_at, last_used_at)
                VALUES ('\x01', '00000000-0000-0000-0000-0000000000a1', now(), now())
                """,
            )
            // 23505 unique_violation: one account per email.
            val duplicate = assertFailsWith<SQLException> {
                connection.execute(
                    """
                    INSERT INTO users (id, email, password_hash, consented_at, created_at)
                    VALUES ('00000000-0000-0000-0000-0000000000a2', 'tim@example.com', 'h', now(), now())
                    """,
                )
            }
            assertEquals("23505", duplicate.sqlState)
            connection.execute(
                """
                INSERT INTO diary_entries (user_id, date, feeling, created_at)
                VALUES ('00000000-0000-0000-0000-0000000000a1', '2026-08-03', 'GOOD', now())
                """,
            )
            // 23505 unique_violation: one answer per user and day.
            val secondAnswer = assertFailsWith<SQLException> {
                connection.execute(
                    """
                    INSERT INTO diary_entries (user_id, date, feeling, created_at)
                    VALUES ('00000000-0000-0000-0000-0000000000a1', '2026-08-03', 'BAD', now())
                    """,
                )
            }
            assertEquals("23505", secondAnswer.sqlState)
            connection.execute("UPDATE users SET password_hash = 'g'")
            connection.execute("UPDATE sessions SET last_used_at = '2026-08-03T06:00:00Z'")
            connection.execute("UPDATE diary_entries SET feeling = 'VERY_GOOD'")
            assertEquals(listOf("g"), connection.query("SELECT password_hash FROM users"))
            assertEquals(listOf("1"), connection.query("SELECT count(*) FROM sessions"))
            assertEquals(listOf("VERY_GOOD"), connection.query("SELECT feeling FROM diary_entries"))
            connection.execute("DELETE FROM diary_entries")
            connection.execute("DELETE FROM sessions")
            connection.execute("DELETE FROM users")
            connection.execute("UPDATE devices SET fcm_token = 'u'")
            connection.execute("UPDATE alarms SET enabled = false")
            connection.execute("UPDATE notification_log SET species = 'ASH'")
            assertEquals(listOf("u"), connection.query("SELECT fcm_token FROM devices"))
            assertEquals(listOf("f"), connection.query("SELECT enabled FROM alarms"))
            assertEquals(listOf("ASH"), connection.query("SELECT species FROM notification_log"))
            connection.execute("UPDATE scheduler_state SET last_minute = '2026-08-03T06:00:00Z'")
            assertEquals(listOf("1"), connection.query("SELECT count(*) FROM scheduler_state"))
            // 23514 check_violation: scheduler_state holds one row at most.
            val second = assertFailsWith<SQLException> {
                connection.execute("INSERT INTO scheduler_state (id, last_minute) VALUES (false, now())")
            }
            assertEquals("23514", second.sqlState)
            connection.execute("DELETE FROM scheduler_state")
            connection.execute("DELETE FROM notification_log")
            connection.execute("DELETE FROM alarms")
            connection.execute("DELETE FROM devices")
        }
    }

    @Test
    fun `the app role can neither create nor alter nor drop a table`() {
        PollenInfoDatabase.migrate(config)

        asApp { connection ->
            assertDenied { connection.execute("CREATE TABLE intruder (id int)") }
            assertDenied { connection.execute("ALTER TABLE alarms ADD COLUMN intruder int") }
            assertDenied { connection.execute("DROP TABLE alarms") }
            assertDenied { connection.execute("TRUNCATE devices CASCADE") }
            assertDenied { connection.execute("ALTER TABLE users ADD COLUMN intruder int") }
            assertDenied { connection.execute("DROP TABLE sessions") }
            assertDenied { connection.execute("ALTER TABLE diary_entries ADD COLUMN intruder int") }
        }
    }

    private fun assertDenied(statement: () -> Unit) {
        val error = assertFailsWith<SQLException>(block = statement)
        // 42501 insufficient_privilege: refused for the role, not for a mistake in the statement.
        assertTrue(error.sqlState == "42501", "expected a privilege error, got ${error.sqlState}: ${error.message}")
    }

    private fun <T> asOwner(block: (Connection) -> T): T = connect(config.ownerUser, config.ownerPassword, block)

    private fun <T> asApp(block: (Connection) -> T): T = connect(config.appUser, config.appPassword, block)

    private fun <T> connect(user: String, password: String, block: (Connection) -> T): T =
        DriverManager.getConnection(config.url, user, password).use(block)

    private fun Connection.execute(sql: String) {
        createStatement().use { it.execute(sql) }
    }

    private fun Connection.query(sql: String): List<String> = createStatement().use { statement ->
        statement.executeQuery(sql).use { rows -> buildList { while (rows.next()) add(rows.getString(1)) } }
    }
}
