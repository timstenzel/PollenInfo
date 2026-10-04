package ch.stenzel.tim.polleninfo.server.alarm.store

import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import java.util.UUID
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.DatabaseConfig
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction

/**
 * The server's SQLite database: alarms, device registrations and the threshold notification log,
 * which unlike the reading cache must survive a restart.
 *
 * Every way of opening one switches foreign keys on for **every** connection — SQLite defaults them
 * off per connection, and Exposed opens a connection per transaction — and creates any missing
 * tables. There is no migration tool yet; a schema change to an existing table needs one.
 */
object PollenInfoDatabase {

    /** Environment variable holding the database file's path. */
    const val PATH_ENV = "POLLENINFO_DB"

    /** Relative to the working directory, which is `server/` under `./gradlew :server:run`. */
    const val DEFAULT_PATH = "./data/polleninfo.db"

    /** The production database: [PATH_ENV] if set, else [DEFAULT_PATH]. */
    fun fromEnvironment(): Database = file(Path.of(System.getenv(PATH_ENV) ?: DEFAULT_PATH))

    /** A file-backed database, creating the file and its directory if they do not exist. */
    fun file(path: Path): Database {
        path.toAbsolutePath().parent?.let(Files::createDirectories)
        return open("jdbc:sqlite:$path")
    }

    /**
     * A private in-memory database for tests and for the routing defaults. A shared-cache memory
     * database lives only while a connection to it is open, and Exposed closes its connection after
     * each transaction, so one extra connection is held open for the life of the process.
     */
    fun inMemory(): Database {
        val url = "jdbc:sqlite:file:${UUID.randomUUID()}?mode=memory&cache=shared"
        keepAlive += DriverManager.getConnection(url)
        return open(url)
    }

    private val keepAlive = mutableListOf<Connection>()

    private fun open(url: String): Database {
        val database = Database.connect(
            url = url,
            driver = "org.sqlite.JDBC",
            setupConnection = { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("PRAGMA foreign_keys = ON")
                    // The scheduler and the routes write from different connections; wait for a
                    // lock instead of failing at once with SQLITE_BUSY.
                    statement.execute("PRAGMA busy_timeout = $BUSY_TIMEOUT_MILLIS")
                }
            },
            // SQLite supports only SERIALIZABLE and READ_UNCOMMITTED.
            databaseConfig = DatabaseConfig { defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE },
        )
        transaction(database) { SchemaUtils.create(DevicesTable, AlarmsTable, NotificationLogTable) }
        return database
    }

    private const val BUSY_TIMEOUT_MILLIS = 5_000
}
