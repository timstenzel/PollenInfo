package ch.stenzel.tim.polleninfo

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import ch.stenzel.tim.polleninfo.core.push.createAlarmNotificationChannels

/**
 * An [AppCompatActivity] because the app language goes through `AppCompatDelegate`: below API 33
 * only AppCompat's activities apply it, and changing it re-creates them in the new language.
 */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // A language change re-creates this activity, so this is where the channel names follow it.
        createAlarmNotificationChannels(this)

        setContent { App() }
    }
}
