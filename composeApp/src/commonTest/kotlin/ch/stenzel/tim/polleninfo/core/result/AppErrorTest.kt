package ch.stenzel.tim.polleninfo.core.result

import ch.stenzel.tim.polleninfo.core.network.HttpStatusException
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.io.IOException
import kotlinx.serialization.SerializationException

class AppErrorTest {

    @Test
    fun `an IO exception is a Network error`() {
        assertEquals(AppError.Network, IOException("Connection refused").toAppError())
    }

    @Test
    fun `a request timeout is a Network error`() {
        assertEquals(AppError.Network, HttpRequestTimeoutException("http://backend.test", 15_000).toAppError())
    }

    @Test
    fun `5xx statuses are ServerUnavailable`() {
        listOf(500, 502, 503, 599).forEach { status ->
            assertEquals(AppError.ServerUnavailable, HttpStatusException(status).toAppError(), "status $status")
        }
    }

    @Test
    fun `a 404 is NotFound`() {
        assertEquals(AppError.NotFound, HttpStatusException(404).toAppError())
    }

    @Test
    fun `statuses next to the mapped ones are Unknown`() {
        listOf(400, 403, 405, 499, 600).forEach { status ->
            assertEquals(AppError.Unknown, HttpStatusException(status).toAppError(), "status $status")
        }
    }

    @Test
    fun `any other exception is Unknown`() {
        assertEquals(AppError.Unknown, RuntimeException("boom").toAppError())
        assertEquals(AppError.Unknown, SerializationException("not json").toAppError())
        assertEquals(AppError.Unknown, IllegalStateException().toAppError())
    }
}
