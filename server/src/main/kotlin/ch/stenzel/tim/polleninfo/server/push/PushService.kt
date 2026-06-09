package ch.stenzel.tim.polleninfo.server.push

import ch.stenzel.tim.polleninfo.server.push.model.PushNotificationRequest
import ch.stenzel.tim.polleninfo.server.push.model.PushSubscription
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PushService {

    private val subscriptions = ConcurrentHashMap<String, PushSubscription>()

    fun subscribe(subscription: PushSubscription): String {
        val id = UUID.randomUUID().toString()
        subscriptions[id] = subscription
        return id
    }

    fun unsubscribe(subscriptionId: String): Boolean = subscriptions.remove(subscriptionId) != null

    fun allSubscriptions(): Map<String, PushSubscription> = subscriptions.toMap()

    fun sendNotification(request: PushNotificationRequest): Int {
        // In production: integrate FCM (Android) and APNS (iOS) here.
        // For now we log and return the targeted device count.
        println("[PushService] Sending '${request.title}' to ${request.deviceTokens.size} device(s)")
        return request.deviceTokens.size
    }

    fun broadcastHighPollenAlert(pollenType: String, location: String): Int {
        val targetTokens = subscriptions.values.map { it.deviceToken }
        if (targetTokens.isEmpty()) return 0
        return sendNotification(
            PushNotificationRequest(
                title = "⚠️ High Pollen Alert",
                body = "High $pollenType pollen levels detected near $location",
                deviceTokens = targetTokens,
            ),
        )
    }
}
