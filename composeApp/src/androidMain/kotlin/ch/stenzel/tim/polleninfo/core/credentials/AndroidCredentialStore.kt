package ch.stenzel.tim.polleninfo.core.credentials

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPasswordOption
import androidx.credentials.PasswordCredential
import java.lang.ref.WeakReference
import kotlin.coroutines.cancellation.CancellationException

/**
 * Credential Manager: Google Password Manager (through `credentials-play-services-auth` on
 * Android 13 and below) or whichever password manager the user has chosen.
 *
 * Its sheets need an activity, which a Koin single holding the application cannot be handed. So the
 * store follows the app's own activity through [Application.ActivityLifecycleCallbacks] — only
 * `ComponentActivity`s, which leaves out the Play services provider's hidden activity — and holds
 * it weakly, so a re-created activity is never leaked or used. It is bound with
 * `createdAtStart`, so the callbacks are registered before `MainActivity` exists.
 *
 * Every failure is logged by its type only — never with the email or password — and swallowed.
 * Checked by hand.
 */
class AndroidCredentialStore(application: Application) : CredentialStore {

    private val credentialManager = CredentialManager.create(application)
    private var activity: WeakReference<Activity>? = null

    init {
        application.registerActivityLifecycleCallbacks(ActivityTracker())
    }

    override suspend fun save(email: String, password: String) {
        val activity = currentActivity() ?: return
        try {
            credentialManager.createCredential(activity, CreatePasswordRequest(id = email, password = password))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.i(TAG, "The login was not saved: ${e.javaClass.simpleName}")
        }
    }

    override suspend fun offer(): SavedCredential? {
        val activity = currentActivity() ?: return null
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetPasswordOption())
            // No sheet at all when nothing is saved, rather than one offering to add a login.
            .setPreferImmediatelyAvailableCredentials(true)
            .build()
        return try {
            val credential = credentialManager.getCredential(activity, request).credential
            (credential as? PasswordCredential)?.let { SavedCredential(it.id, it.password) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.i(TAG, "No saved login was offered: ${e.javaClass.simpleName}")
            null
        }
    }

    private fun currentActivity(): Activity? {
        val current = activity?.get()?.takeUnless { it.isFinishing || it.isDestroyed }
        if (current == null) Log.w(TAG, "No activity to show the password manager on")
        return current
    }

    private inner class ActivityTracker : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = track(activity)
        override fun onActivityStarted(activity: Activity) = track(activity)
        override fun onActivityResumed(activity: Activity) = track(activity)
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) {
            if (this@AndroidCredentialStore.activity?.get() === activity) this@AndroidCredentialStore.activity = null
        }

        private fun track(activity: Activity) {
            if (activity is ComponentActivity) this@AndroidCredentialStore.activity = WeakReference(activity)
        }
    }

    private companion object {
        const val TAG = "CredentialStore"
    }
}
