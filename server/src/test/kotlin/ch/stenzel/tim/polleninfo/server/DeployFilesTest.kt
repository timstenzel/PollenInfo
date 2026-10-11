package ch.stenzel.tim.polleninfo.server

import ch.stenzel.tim.polleninfo.server.alarm.scheduler.TICK_STOP_TIMEOUT
import ch.stenzel.tim.polleninfo.server.alarm.store.TestPostgres
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Pins the values the compose files share with the server's code, so a version bump or a longer
 * shutdown on one side cannot silently leave the other behind. Reads the files as text; no Docker.
 */
class DeployFilesTest {

    private fun deployFile(name: String): String = Files.readString(Path.of("../deploy", name))

    private fun postgresImages(file: String): List<String> =
        Regex("""^\s*image:\s*(postgres:\S+)""", RegexOption.MULTILINE)
            .findAll(deployFile(file))
            .map { it.groupValues[1] }
            .toList()

    @Test
    fun `the production stack runs the Postgres version the database tests run`() {
        assertEquals(listOf(TestPostgres.IMAGE), postgresImages("compose.yaml"))
    }

    @Test
    fun `the development database runs the Postgres version the database tests run`() {
        assertEquals(listOf(TestPostgres.IMAGE), postgresImages("compose.dev.yaml"))
    }

    @Test
    fun `the server's stop grace period covers a running tick plus the Netty and application shutdown`() {
        val gracePeriod = Regex("""stop_grace_period:\s*(\d+)s""")
            .find(deployFile("compose.yaml"))
            ?.groupValues?.get(1)?.toInt()?.seconds
        val shutdownAroundTheTick = 12.seconds // Netty: 2 s grace + 5 s timeout; application: 5 s
        assertTrue(
            gracePeriod != null && gracePeriod > TICK_STOP_TIMEOUT + shutdownAroundTheTick,
            "stop_grace_period $gracePeriod must exceed ${TICK_STOP_TIMEOUT + shutdownAroundTheTick}",
        )
    }
}
