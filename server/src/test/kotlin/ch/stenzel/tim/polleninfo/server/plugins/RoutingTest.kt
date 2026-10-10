package ch.stenzel.tim.polleninfo.server.plugins

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class RoutingTest {

    @Test
    fun `health endpoint reports OK`() = testApplication {
        application {
            configureSerialization()
            configureSecurity()
            configureRouting()
        }

        val response = client.get("/health")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("OK", response.bodyAsText())
    }

    @Test
    fun `an unmapped path returns 404`() = testApplication {
        application {
            configureSerialization()
            configureSecurity()
            configureRouting()
        }

        assertEquals(HttpStatusCode.NotFound, client.get("/nope").status)
    }
}
