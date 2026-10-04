package ch.stenzel.tim.polleninfo.server.alarm.push

import com.google.auth.oauth2.ServiceAccountCredentials
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped
import io.ktor.server.application.log
import java.nio.file.Files
import java.nio.file.Path
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Environment variable holding the path of the Firebase service-account key (JSON). */
const val FCM_CREDENTIALS_ENV = "FCM_CREDENTIALS"

/**
 * The production [PushSender]: FCM when [FCM_CREDENTIALS_ENV] names a service-account key, else a
 * [LoggingPushSender] with a startup warning, so the backend runs without credentials.
 *
 * The key is never part of the repository. A key that is configured but unreadable fails startup:
 * a backend that silently logs instead of sending would look healthy while delivering nothing.
 */
fun Application.pushSenderFromEnvironment(): PushSender {
    val path = System.getenv(FCM_CREDENTIALS_ENV)
    if (path.isNullOrBlank()) {
        log.warn("$FCM_CREDENTIALS_ENV is not set: push notifications are logged, not sent")
        return LoggingPushSender()
    }

    val key = Files.newInputStream(Path.of(path)).use(ServiceAccountCredentials::fromStream)
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
