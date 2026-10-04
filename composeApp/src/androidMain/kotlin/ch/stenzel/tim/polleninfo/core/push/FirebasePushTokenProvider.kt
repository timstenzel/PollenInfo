package ch.stenzel.tim.polleninfo.core.push

import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * The FCM registration token.
 *
 * FCM is the one place the app depends on Google Play services. Without them the token task fails,
 * which surfaces as an ordinary error with Retry rather than as "unavailable": push on devices
 * without Play services is out of scope, and telling the two apart would need Play services too.
 */
class FirebasePushTokenProvider : PushTokenProvider {

    override suspend fun token(): PushTokenResult = suspendCancellableCoroutine { continuation ->
        // The task itself cannot be cancelled; a late answer after cancellation is simply dropped.
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!continuation.isActive) return@addOnCompleteListener
            if (task.isSuccessful) {
                continuation.resume(PushTokenResult.Available(task.result))
            } else {
                continuation.resumeWithException(
                    task.exception ?: IllegalStateException("No FCM token was issued"),
                )
            }
        }
    }
}
