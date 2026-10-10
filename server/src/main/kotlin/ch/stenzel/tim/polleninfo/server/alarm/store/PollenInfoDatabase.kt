package ch.stenzel.tim.polleninfo.server.alarm.store

import ch.stenzel.tim.polleninfo.server.config.DatabaseConfig
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.flywaydb.core.api.output.MigrateResult
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.TransactionManager

/**
 * The server's PostgreSQL database: alarms, device registrations and the threshold notification
 * log, which unlike the reading cache must survive a restart.
 *
 * [open] first brings the schema up to date — Flyway, as the schema owner, over a connection of its
 * own that is closed again — and only then opens the pool every store uses, as the app login, which
 * can read and write rows but not change a table. The schema is the migrations in
 * `db/migration/`; nothing creates tables from the Kotlin definitions.
 */
class PollenInfoDatabase private constructor(
    /** What the Exposed stores run their transactions on. */
    val database: Database,
    private val pool: HikariDataSource,
) : AutoCloseable {

    /** Closes the pool; the stores built on [database] cannot be used afterwards. */
    override fun close() {
        TransactionManager.closeAndUnregister(database)
        pool.close()
    }

    companion object {

        /** Migrates, then connects as the app login. */
        fun open(config: DatabaseConfig, poolSize: Int = POOL_SIZE): PollenInfoDatabase {
            migrate(config)
            return connect(config, poolSize)
        }

        /** Applies every pending migration as the owner; a schema already up to date is left alone. */
        fun migrate(config: DatabaseConfig): MigrateResult =
            Flyway.configure()
                .dataSource(config.url, config.ownerUser, config.ownerPassword)
                .locations(MIGRATIONS)
                .load()
                .migrate()

        /** A pool of [poolSize] connections as the app login, without touching the schema. */
        fun connect(config: DatabaseConfig, poolSize: Int = POOL_SIZE): PollenInfoDatabase {
            val pool = HikariDataSource(
                HikariConfig().apply {
                    jdbcUrl = config.url
                    username = config.appUser
                    password = config.appPassword
                    maximumPoolSize = poolSize
                    poolName = "polleninfo"
                },
            )
            return PollenInfoDatabase(Database.connect(pool), pool)
        }

        /** The routes and the scheduler together rarely hold more than a couple of connections. */
        const val POOL_SIZE = 5

        private const val MIGRATIONS = "classpath:db/migration"
    }
}
