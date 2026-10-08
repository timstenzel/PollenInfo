package ch.stenzel.tim.polleninfo.core.network

import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess

/**
 * Our backend answered with a non-2xx [status]. Every API service checks the status before `body()`
 * through [checkSuccess], so an error response is never read as if it were the expected payload, and
 * `toAppError()` can tell a server problem (5xx) from a missing resource (404).
 */
class HttpStatusException(val status: Int) : Exception("The backend answered $status")

/** This response, or [HttpStatusException] if its status is not 2xx. */
fun HttpResponse.checkSuccess(): HttpResponse {
    if (!status.isSuccess()) throw HttpStatusException(status.value)
    return this
}
