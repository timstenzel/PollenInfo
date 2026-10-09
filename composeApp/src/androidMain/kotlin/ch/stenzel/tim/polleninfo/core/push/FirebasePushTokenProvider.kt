package ch.stenzel.tim.polleninfo.core.push

import com.google.android.gms.tasks.Task
import com.google.firebase.installations.FirebaseInstallations
import com.google.firebase.messaging.FirebaseMessaging
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * This install's push address: its Firebase Installation ID (FID), registered with FCM.
 *
 * firebase-messaging 26 addresses an app instance by its FID rather than by a registration token
 * (the manifest's `firebase_messaging_installation_id_enabled`). [FirebaseMessaging.register] makes
 * sure the FID is registered with FCM; the FID itself is read from Firebase Installations, which is
 * the same id `onRegistered` reports to [PollenFirebaseMessagingService].
 *
 * FCM is the one place the app depends on Google Play services. Without them a task fails, which
 * surfaces as an ordinary error with Retry rather than as "unavailable": push on devices without
 * Play services is out of scope, and telling the two apart would need Play services too.
 */
class FirebasePushTokenProvider : PushTokenProvider {

    override suspend fun token(): PushTokenResult {
        FirebaseMessaging.getInstance().register().await()
        return PushTokenResult.Available(FirebaseInstallations.getInstance().id.await())
    }
}

/** The task's result; the task itself cannot be cancelled, so a late answer is simply dropped. */
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (!continuation.isActive) return@addOnCompleteListener
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: IllegalStateException("The Firebase task failed"))
        }
    }
}
