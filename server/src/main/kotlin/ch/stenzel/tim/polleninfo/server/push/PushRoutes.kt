package ch.stenzel.tim.polleninfo.server.push

import ch.stenzel.tim.polleninfo.server.push.model.PushSubscription
import ch.stenzel.tim.polleninfo.server.push.model.SubscriptionResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.pushRoutes(pushService: PushService) {
    route("/push") {
        post("/subscribe") {
            val subscription = call.receive<PushSubscription>()
            val id = pushService.subscribe(subscription)
            call.respond(
                HttpStatusCode.Created,
                SubscriptionResponse(subscriptionId = id, message = "Subscribed successfully"),
            )
        }

        delete("/subscribe/{id}") {
            val id = call.parameters["id"] ?: return@delete call.respond(HttpStatusCode.BadRequest)
            val removed = pushService.unsubscribe(id)
            if (removed) call.respond(HttpStatusCode.NoContent)
            else call.respond(HttpStatusCode.NotFound)
        }

        post("/alert") {
            val pollenType = call.request.queryParameters["pollenType"] ?: "Unknown"
            val location = call.request.queryParameters["location"] ?: "your area"
            val sent = pushService.broadcastHighPollenAlert(pollenType, location)
            call.respond(mapOf("notificationsSent" to sent))
        }

        get("/subscriptions") {
            call.respond(pushService.allSubscriptions())
        }
    }
}
