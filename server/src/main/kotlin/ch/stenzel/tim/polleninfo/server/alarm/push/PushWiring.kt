package ch.stenzel.tim.polleninfo.server.alarm.push

import ch.stenzel.tim.polleninfo.server.config.ServerConfig
import com.google.auth.oauth2.ServiceAccountCredentials
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.log
import java.nio.file.Files
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The server's [PushSender]: FCM with the service-account key [ServerConfig.fcmCredentialsPath]
 * names, else — in development only — a [LoggingPushSender] with a startup warning, so a local
 * backend runs without credentials.
 *
 * The key is never part of the repository. Production cannot get here without one
 * ([ServerConfig.load] refuses to start), so it never falls back to logging: a backend that silently
 * logs instead of sending would look healthy while delivering nothing.
 */
fun Application.pushSender(config: ServerConfig): PushSender {
    val path = config.fcmCredentialsPath
    if (path == null) {
        check(config.environment == ServerConfig.Environment.DEVELOPMENT) { "Production needs an FCM key" }
        log.warn("${ServerConfig.FCM_CREDENTIALS} is not set: push notifications are logged, not sent")
        return LoggingPushSender()
    }

    val key = Files.newInputStream(path).use(ServiceAccountCredentials::fromStream)
    val projectId = requireNotNull(key.projectId) { "$path holds no project_id" }
    val credentials = key.createScoped(FcmPushSender.SCOPE)

    val client = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = PUSH_TIMEOUT_MILLIS
            connectTimeoutMillis = PUSH_TIMEOUT_MILLIS
        }
    }
    monitor.subscribe(ApplicationStopped) { client.close() }
    log.info("Push notifications go through FCM project $projectId")

    return FcmPushSender(
        client = client,
        projectId = projectId,
        accessToken = {
            // Blocking network call when the token is missing or about to expire; a no-op otherwise.
            withContext(Dispatchers.IO) {
                credentials.refreshIfExpired()
                checkNotNull(credentials.accessToken) { "No FCM access token was issued" }.tokenValue
            }
        },
    )
}

private const val PUSH_TIMEOUT_MILLIS = 15_000L
