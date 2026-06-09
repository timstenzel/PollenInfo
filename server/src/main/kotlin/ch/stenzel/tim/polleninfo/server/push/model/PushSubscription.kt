package ch.stenzel.tim.polleninfo.server.push.model

import kotlinx.serialization.Serializable

@Serializable
data class PushSubscription(
    val deviceToken: String,
    val platform: Platform,
    val latitude: Double,
    val longitude: Double,
)

@Serializable
enum class Platform { ANDROID, IOS }

@Serializable
data class PushNotificationRequest(
    val title: String,
    val body: String,
    val deviceTokens: List<String>,
)

@Serializable
data class SubscriptionResponse(val subscriptionId: String, val message: String)
