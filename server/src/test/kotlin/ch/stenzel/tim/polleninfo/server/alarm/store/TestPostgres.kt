package ch.stenzel.tim.polleninfo.server.alarm.store

import ch.stenzel.tim.polleninfo.server.config.DatabaseConfig
import ch.stenzel.tim.polleninfo.server.config.ServerConfig
import java.nio.file.Path
import java.sql.DriverManager
import java.util.UUID
import org.jetbrains.exposed.v1.jdbc.Database
import org.testcontainers.DockerClientFactory
import org.testcontainers.images.builder.Transferable
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.MountableFile

/**
 * The database tests' PostgreSQL: one container per test JVM, started on first use, initialised by
 * the same `deploy/postgres/init/01-roles.sh` as development and production, so the tests run
 * against the production version with the production roles.
 *
 * Store, route and scheduler tests share the database `polleninfo`, migrated once and emptied by
 * [cleanDatabase] for every test. Migration tests need a database that has never been migrated;
 * [unmigratedDatabase] copies one from a template taken right after the init script ran.
 */
object TestPostgres {

    /** Keep in step with `deploy/compose.dev.yaml` (and the production compose file). */
    const val IMAGE = "postgres:18.6"

    const val OWNER_PASSWORD = "test-owner-password"
    const val APP_PASSWORD = "test-app-password"

    private const val DATABASE = "polleninfo"
    private const val TEMPLATE = "polleninfo_unmigrated"

    private val container: PostgreSQLContainer by lazy(::start)

    /** The app login's pool on the shared database, open for the life of the test JVM. */
    private val shared: PollenInfoDatabase by lazy {
        PollenInfoDatabase.migrate(config(DATABASE))
        PollenInfoDatabase.connect(config(DATABASE))
    }

    /**
     * The shared, migrated database with every table emptied, as the app login — what the server's
     * stores run on.
     */
    fun cleanDatabase(): Database {
        val database = shared.database
        execute(DATABASE, ServerConfig.DEFAULT_OWNER_USER, OWNER_PASSWORD) {
            "TRUNCATE devices, alarms, notification_log, scheduler_state"
        }
        return database
    }

    /** The configuration of the shared database. */
    fun sharedConfig(): DatabaseConfig = config(DATABASE)

    /**
     * A new database exactly as the init script leaves `polleninfo` — roles, ownership and default
     * privileges in place, no migration applied.
     */
    fun unmigratedDatabase(): DatabaseConfig {
        val name = "migration_" + UUID.randomUUID().toString().replace("-", "")
        execute("postgres", container.username, container.password) {
            "CREATE DATABASE $name TEMPLATE $TEMPLATE OWNER ${ServerConfig.DEFAULT_OWNER_USER}"
        }
        return config(name)
    }

    private fun config(database: String) = DatabaseConfig(
        url = "jdbc:postgresql://${container.host}:${container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)}/$database",
        ownerUser = ServerConfig.DEFAULT_OWNER_USER,
        ownerPassword = OWNER_PASSWORD,
        appUser = ServerConfig.DEFAULT_APP_USER,
        appPassword = APP_PASSWORD,
    )

    private fun execute(database: String, user: String, password: String, sql: () -> String) {
        DriverManager.getConnection(config(database).url, user, password).use { connection ->
            connection.createStatement().use { it.execute(sql()) }
        }
    }

    private fun start(): PostgreSQLContainer {
        check(DockerClientFactory.instance().isDockerAvailable) {
            "The database tests need Docker. Start Docker Desktop (or OrbStack / Colima) and run them again."
        }
        val container = PostgreSQLContainer(IMAGE)
            .withEnv("DB_OWNER_PASSWORD_FILE", "/run/secrets/db_owner_password")
            .withEnv("DB_APP_PASSWORD_FILE", "/run/secrets/db_app_password")
            .withCopyToContainer(Transferable.of(OWNER_PASSWORD), "/run/secrets/db_owner_password")
            .withCopyToContainer(Transferable.of(APP_PASSWORD), "/run/secrets/db_app_password")
            .withCopyFileToContainer(
                // Relative to `server/`, the test task's working directory.
                MountableFile.forHostPath(Path.of("../deploy/postgres/init/01-roles.sh"), 0b111_101_101),
                "/docker-entrypoint-initdb.d/01-roles.sh",
            )
        container.start()
        // Nothing has connected to `polleninfo` yet, so it can serve as a template.
        DriverManager.getConnection(container.jdbcUrl, container.username, container.password).use { connection ->
            connection.createStatement().use { it.execute("CREATE DATABASE $TEMPLATE TEMPLATE $DATABASE") }
        }
        return container
    }
}
