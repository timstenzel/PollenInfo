package ch.stenzel.tim.polleninfo.server.alarm.store

import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
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
    fun `V1 applies to an empty database and creates every table`() {
        val result = PollenInfoDatabase.migrate(config)

        assertEquals(listOf("1"), result.migrations.map { it.version })
        assertEquals(
            listOf("alarms", "devices", "flyway_schema_history", "notification_log"),
            asOwner { connection ->
                connection.query(
                    "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name",
                )
            },
        )
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
            connection.execute("INSERT INTO devices (id, fcm_token, created_at) VALUES ('d1', 't', now())")
            connection.execute(
                """
                INSERT INTO alarms (id, device_id, enabled, station_abbr, species, min_severity, days, type,
                                    at_time, created_at)
                VALUES ('00000000-0000-0000-0000-000000000001', 'd1', true, 'PZH', '{BIRCH}', 'NONE',
                        '{MONDAY}', 'daily', '08:00', now())
                """,
            )
            connection.execute(
                "INSERT INTO notification_log VALUES ('00000000-0000-0000-0000-000000000001', 'BIRCH', '2026-08-03')",
            )
            connection.execute("UPDATE devices SET fcm_token = 'u'")
            connection.execute("UPDATE alarms SET enabled = false")
            connection.execute("UPDATE notification_log SET species = 'ASH'")
            assertEquals(listOf("u"), connection.query("SELECT fcm_token FROM devices"))
            assertEquals(listOf("f"), connection.query("SELECT enabled FROM alarms"))
            assertEquals(listOf("ASH"), connection.query("SELECT species FROM notification_log"))
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
