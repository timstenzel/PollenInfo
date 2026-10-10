package ch.stenzel.tim.polleninfo.server.config

import ch.stenzel.tim.polleninfo.server.config.ServerConfig.Environment
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ServerConfigTest {

    /** The files a test's [load] can read; every other path is unreadable. */
    private val files = mutableMapOf(
        "/run/secrets/db_owner_password" to "owner-secret\n",
        "/run/secrets/db_app_password" to "app-secret\n",
        "/run/secrets/fcm_credentials.json" to """{"type":"service_account"}""",
    )

    private fun load(env: Map<String, String>) = ServerConfig.load(env) { files[it.toString()] }

    private val production = mapOf(
        "POLLENINFO_ENV" to "production",
        "PORT" to "9090",
        "DB_URL" to "jdbc:postgresql://postgres:5432/polleninfo",
        "DB_OWNER_PASSWORD_FILE" to "/run/secrets/db_owner_password",
        "DB_APP_PASSWORD_FILE" to "/run/secrets/db_app_password",
        "FCM_CREDENTIALS" to "/run/secrets/fcm_credentials.json",
    )

    @Test
    fun `no variable at all is development with the defaults of the dev compose file`() {
        val config = load(emptyMap())

        assertEquals(Environment.DEVELOPMENT, config.environment)
        assertEquals(8080, config.port)
        assertEquals(
            DatabaseConfig(
                url = "jdbc:postgresql://localhost:5432/polleninfo",
                ownerUser = "polleninfo_owner",
                ownerPassword = "polleninfo-dev-owner",
                appUser = "polleninfo_app",
                appPassword = "polleninfo-dev-app",
            ),
            config.database,
        )
        assertNull(config.fcmCredentialsPath)
    }

    @Test
    fun `the development passwords are the ones in the dev compose secrets`() {
        // Relative to `server/`, the test task's working directory.
        val secrets = Path.of("../deploy/dev-secrets")

        assertEquals(ServerConfig.DEV_OWNER_PASSWORD, Files.readString(secrets.resolve("db_owner_password")).trim())
        assertEquals(ServerConfig.DEV_APP_PASSWORD, Files.readString(secrets.resolve("db_app_password")).trim())
    }

    @Test
    fun `development values can be overridden one by one`() {
        val config = load(mapOf("PORT" to "8181", "DB_APP_PASSWORD_FILE" to "/run/secrets/db_app_password"))

        assertEquals(8181, config.port)
        assertEquals("app-secret", config.database.appPassword)
        assertEquals(ServerConfig.DEV_OWNER_PASSWORD, config.database.ownerPassword)
    }

    @Test
    fun `production with everything present reads every value and trims the password files`() {
        val config = load(production)

        assertEquals(Environment.PRODUCTION, config.environment)
        assertEquals(9090, config.port)
        assertEquals(
            DatabaseConfig(
                url = "jdbc:postgresql://postgres:5432/polleninfo",
                ownerUser = "polleninfo_owner",
                ownerPassword = "owner-secret",
                appUser = "polleninfo_app",
                appPassword = "app-secret",
            ),
            config.database,
        )
        assertEquals(Path.of("/run/secrets/fcm_credentials.json"), config.fcmCredentialsPath)
    }

    @Test
    fun `production without the FCM key and both password files fails naming all three`() {
        val env = production - "FCM_CREDENTIALS" - "DB_OWNER_PASSWORD_FILE" - "DB_APP_PASSWORD_FILE"

        val error = assertFailsWith<ConfigException> { load(env) }

        assertEquals(
            listOf(
                "DB_OWNER_PASSWORD_FILE is not set",
                "DB_APP_PASSWORD_FILE is not set",
                "FCM_CREDENTIALS is not set",
            ),
            error.problems,
        )
        error.problems.forEach { assertTrue(it in error.message.orEmpty()) }
    }

    @Test
    fun `production without a database URL fails`() {
        val error = assertFailsWith<ConfigException> { load(production - "DB_URL") }

        assertEquals(listOf("DB_URL is not set"), error.problems)
    }

    @Test
    fun `an unreadable password file fails naming the variable and the path`() {
        files.remove("/run/secrets/db_owner_password")

        val error = assertFailsWith<ConfigException> { load(production) }

        assertEquals(
            listOf("DB_OWNER_PASSWORD_FILE names /run/secrets/db_owner_password, which cannot be read or is empty"),
            error.problems,
        )
    }

    @Test
    fun `an empty password file fails like an unreadable one`() {
        files["/run/secrets/db_app_password"] = " \n"

        val error = assertFailsWith<ConfigException> { load(production) }

        assertEquals(
            listOf("DB_APP_PASSWORD_FILE names /run/secrets/db_app_password, which cannot be read or is empty"),
            error.problems,
        )
    }

    @Test
    fun `an unreadable FCM key fails in development too`() {
        val error = assertFailsWith<ConfigException> { load(mapOf("FCM_CREDENTIALS" to "/missing.json")) }

        assertEquals(listOf("FCM_CREDENTIALS names /missing.json, which cannot be read or is empty"), error.problems)
    }

    @Test
    fun `a malformed PORT fails`() {
        listOf("eighty", "0", "65536", "-1").forEach { port ->
            val error = assertFailsWith<ConfigException> { load(mapOf("PORT" to port)) }

            assertEquals(listOf("PORT must be a port number between 1 and 65535, not '$port'"), error.problems)
        }
    }

    @Test
    fun `the lowest and highest ports are accepted`() {
        assertEquals(1, load(mapOf("PORT" to "1")).port)
        assertEquals(65_535, load(mapOf("PORT" to "65535")).port)
    }

    @Test
    fun `an unknown environment fails and is checked as strictly as production`() {
        val error = assertFailsWith<ConfigException> { load(mapOf("POLLENINFO_ENV" to "prod")) }

        assertEquals("POLLENINFO_ENV must be 'production' or 'development', not 'prod'", error.problems.first())
        assertTrue("FCM_CREDENTIALS is not set" in error.problems)
    }

    @Test
    fun `a printed database configuration shows no password`() {
        val printed = load(production).database.toString()

        assertTrue("owner-secret" !in printed && "app-secret" !in printed, printed)
    }

    @Test
    fun `the rate limits default to five registrations an hour and 60 device and 120 pollen calls a minute`() {
        listOf(load(emptyMap()), load(production)).forEach { config ->
            assertEquals(RateLimits(registerPerHour = 5, devicePerMinute = 60, pollenPerMinute = 120), config.rateLimits)
        }
    }

    @Test
    fun `each rate limit can be set on its own`() {
        val config = load(
            mapOf(
                "RATE_LIMIT_REGISTER_PER_HOUR" to "10",
                "RATE_LIMIT_DEVICE_PER_MINUTE" to "1",
                "RATE_LIMIT_POLLEN_PER_MINUTE" to "300",
            ),
        )

        assertEquals(RateLimits(registerPerHour = 10, devicePerMinute = 1, pollenPerMinute = 300), config.rateLimits)
    }

    @Test
    fun `malformed rate limits fail naming every one`() {
        val error = assertFailsWith<ConfigException> {
            load(
                mapOf(
                    "RATE_LIMIT_REGISTER_PER_HOUR" to "five",
                    "RATE_LIMIT_DEVICE_PER_MINUTE" to "0",
                    "RATE_LIMIT_POLLEN_PER_MINUTE" to "-1",
                ),
            )
        }

        assertEquals(
            listOf(
                "RATE_LIMIT_REGISTER_PER_HOUR must be a whole number above 0, not 'five'",
                "RATE_LIMIT_DEVICE_PER_MINUTE must be a whole number above 0, not '0'",
                "RATE_LIMIT_POLLEN_PER_MINUTE must be a whole number above 0, not '-1'",
            ),
            error.problems,
        )
    }

    @Test
    fun `the proxy is trusted in production and not in development by default`() {
        assertTrue(load(production).trustedProxy)
        assertFalse(load(emptyMap()).trustedProxy)
    }

    @Test
    fun `TRUSTED_PROXY overrides the default either way`() {
        assertFalse(load(production + ("TRUSTED_PROXY" to "false")).trustedProxy)
        assertTrue(load(mapOf("TRUSTED_PROXY" to "TRUE")).trustedProxy)
    }

    @Test
    fun `a malformed TRUSTED_PROXY fails`() {
        val error = assertFailsWith<ConfigException> { load(mapOf("TRUSTED_PROXY" to "yes")) }

        assertEquals(listOf("TRUSTED_PROXY must be 'true' or 'false', not 'yes'"), error.problems)
    }

    @Test
    fun `the real file reader returns null for a missing file`() {
        assertNull(ServerConfig.readFileOrNull(Path.of("/definitely/not/here")))
    }
}
