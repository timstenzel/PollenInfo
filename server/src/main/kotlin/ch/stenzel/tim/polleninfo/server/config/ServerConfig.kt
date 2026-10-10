package ch.stenzel.tim.polleninfo.server.config

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * Everything the server reads from its environment, read once at start by [load].
 *
 * In [Environment.DEVELOPMENT] every value has a default matching `deploy/compose.dev.yaml`, so
 * `./gradlew :server:run` needs no exported variable. In [Environment.PRODUCTION] the database URL,
 * both password files and the FCM key are mandatory, and a missing or unreadable one stops the
 * server before it opens a port — with every problem named at once, not the first one found.
 */
data class ServerConfig(
    val environment: Environment,
    val port: Int,
    val database: DatabaseConfig,
    /** The Firebase service-account key; always set in production, optional in development. */
    val fcmCredentialsPath: Path?,
    val rateLimits: RateLimits = RateLimits(),
    /**
     * Whether the client address is taken from the proxy's `X-Forwarded-For`. Only a server that
     * nothing but its own proxy can reach may trust it, or every caller could pick its own bucket.
     */
    val trustedProxy: Boolean = false,
) {

    enum class Environment { PRODUCTION, DEVELOPMENT }

    companion object {
        const val ENVIRONMENT = "POLLENINFO_ENV"
        const val PORT = "PORT"
        const val DB_URL = "DB_URL"
        const val DB_OWNER_USER = "DB_OWNER_USER"
        const val DB_OWNER_PASSWORD_FILE = "DB_OWNER_PASSWORD_FILE"
        const val DB_APP_USER = "DB_APP_USER"
        const val DB_APP_PASSWORD_FILE = "DB_APP_PASSWORD_FILE"
        const val FCM_CREDENTIALS = "FCM_CREDENTIALS"
        const val RATE_LIMIT_REGISTER_PER_HOUR = "RATE_LIMIT_REGISTER_PER_HOUR"
        const val RATE_LIMIT_DEVICE_PER_MINUTE = "RATE_LIMIT_DEVICE_PER_MINUTE"
        const val RATE_LIMIT_POLLEN_PER_MINUTE = "RATE_LIMIT_POLLEN_PER_MINUTE"
        const val TRUSTED_PROXY = "TRUSTED_PROXY"

        const val DEFAULT_PORT = 8080
        const val DEFAULT_OWNER_USER = "polleninfo_owner"
        const val DEFAULT_APP_USER = "polleninfo_app"

        /** The development database of `deploy/compose.dev.yaml`. */
        const val DEV_DB_URL = "jdbc:postgresql://localhost:5432/polleninfo"

        /** Development-only passwords; the same values as `deploy/dev-secrets/`. */
        const val DEV_OWNER_PASSWORD = "polleninfo-dev-owner"
        const val DEV_APP_PASSWORD = "polleninfo-dev-app"

        /**
         * Reads the configuration from [env], reading password and key files through [readFile],
         * which returns `null` for a file that does not exist or cannot be read.
         *
         * @throws ConfigException naming every missing, unreadable or malformed item.
         */
        fun load(env: Map<String, String>, readFile: (Path) -> String?): ServerConfig {
            val problems = mutableListOf<String>()
            fun value(name: String): String? = env[name]?.takeIf { it.isNotBlank() }

            val environment = when (val name = value(ENVIRONMENT)?.lowercase()) {
                null, "development" -> Environment.DEVELOPMENT
                "production" -> Environment.PRODUCTION
                else -> {
                    problems += "$ENVIRONMENT must be 'production' or 'development', not '$name'"
                    Environment.PRODUCTION
                }
            }
            val production = environment == Environment.PRODUCTION

            val port = when (val text = value(PORT)) {
                null -> DEFAULT_PORT
                else -> text.toIntOrNull()?.takeIf { it in 1..65_535 } ?: run {
                    problems += "$PORT must be a port number between 1 and 65535, not '$text'"
                    DEFAULT_PORT
                }
            }

            val url = value(DB_URL) ?: if (production) {
                problems += "$DB_URL is not set"
                ""
            } else {
                DEV_DB_URL
            }

            /** The password in the file [variable] names, or [devDefault] in development. */
            fun password(variable: String, devDefault: String): String {
                val path = value(variable)
                if (path == null) {
                    if (production) problems += "$variable is not set"
                    return devDefault
                }
                val content = readFile(Path.of(path))?.trim()
                if (content.isNullOrEmpty()) {
                    problems += "$variable names $path, which cannot be read or is empty"
                    return ""
                }
                return content
            }

            val database = DatabaseConfig(
                url = url,
                ownerUser = value(DB_OWNER_USER) ?: DEFAULT_OWNER_USER,
                ownerPassword = password(DB_OWNER_PASSWORD_FILE, DEV_OWNER_PASSWORD),
                appUser = value(DB_APP_USER) ?: DEFAULT_APP_USER,
                appPassword = password(DB_APP_PASSWORD_FILE, DEV_APP_PASSWORD),
            )

            // A key that is configured but unreadable fails in development too: a server that
            // silently logged instead of sending would look healthy while delivering nothing.
            val fcmCredentialsPath = when (val path = value(FCM_CREDENTIALS)) {
                null -> {
                    if (production) problems += "$FCM_CREDENTIALS is not set"
                    null
                }
                else -> Path.of(path).also {
                    if (readFile(it).isNullOrBlank()) {
                        problems += "$FCM_CREDENTIALS names $path, which cannot be read or is empty"
                    }
                }
            }

            /** A positive whole number from [variable], or [default] when it is not set. */
            fun limit(variable: String, default: Int): Int = when (val text = value(variable)) {
                null -> default
                else -> text.toIntOrNull()?.takeIf { it > 0 } ?: run {
                    problems += "$variable must be a whole number above 0, not '$text'"
                    default
                }
            }

            val rateLimits = RateLimits(
                registerPerHour = limit(RATE_LIMIT_REGISTER_PER_HOUR, RateLimits.DEFAULT_REGISTER_PER_HOUR),
                devicePerMinute = limit(RATE_LIMIT_DEVICE_PER_MINUTE, RateLimits.DEFAULT_DEVICE_PER_MINUTE),
                pollenPerMinute = limit(RATE_LIMIT_POLLEN_PER_MINUTE, RateLimits.DEFAULT_POLLEN_PER_MINUTE),
            )

            // In production the server sits behind Caddy, the only peer that can reach it; a
            // development server is reached directly, where the header is whatever the caller says.
            val trustedProxy = when (val text = value(TRUSTED_PROXY)?.lowercase()) {
                null -> production
                "true" -> true
                "false" -> false
                else -> {
                    problems += "$TRUSTED_PROXY must be 'true' or 'false', not '$text'"
                    production
                }
            }

            if (problems.isNotEmpty()) throw ConfigException(problems)
            return ServerConfig(environment, port, database, fcmCredentialsPath, rateLimits, trustedProxy)
        }

        /** The [load] file reader for real files: `null` for one that is missing or unreadable. */
        fun readFileOrNull(path: Path): String? = try {
            Files.readString(path)
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }
    }
}

/**
 * How many requests a caller may make before it gets a `429`: registrations per client address and
 * hour, device calls per device and minute, pollen reads per client address and minute.
 *
 * The pollen limit leaves room for the All stations tab, which reads all fifteen stations at once:
 * eight full refreshes a minute from one address still pass.
 */
data class RateLimits(
    val registerPerHour: Int = DEFAULT_REGISTER_PER_HOUR,
    val devicePerMinute: Int = DEFAULT_DEVICE_PER_MINUTE,
    val pollenPerMinute: Int = DEFAULT_POLLEN_PER_MINUTE,
) {
    companion object {
        const val DEFAULT_REGISTER_PER_HOUR = 5
        const val DEFAULT_DEVICE_PER_MINUTE = 60
        const val DEFAULT_POLLEN_PER_MINUTE = 120
    }
}

/**
 * The two logins of the one database: [ownerUser] owns the schema and runs the migrations, and
 * [appUser] — the only login the running server keeps connections for — can read and write rows
 * but not create, alter or drop a table.
 */
data class DatabaseConfig(
    val url: String,
    val ownerUser: String,
    val ownerPassword: String,
    val appUser: String,
    val appPassword: String,
) {
    /** Never prints the passwords, so a logged config leaks nothing. */
    override fun toString() = "DatabaseConfig(url=$url, ownerUser=$ownerUser, appUser=$appUser)"
}

/** The configuration cannot be used; [problems] lists every reason. */
class ConfigException(val problems: List<String>) :
    RuntimeException("Invalid server configuration:\n" + problems.joinToString("\n") { "  - $it" })
