package ch.stenzel.tim.polleninfo.core.language

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate

/**
 * A context whose resources are in the app's chosen language, for text built outside an activity —
 * notification channel names, and notifications the push service writes.
 *
 * On API 33+ the system applies the per-app language to the application context itself. Below that
 * only AppCompat's activities are localized, so this wraps the application context in the chosen
 * locales; with none chosen ("System default"), or before AppCompat has loaded its stored choice
 * (no activity created yet in this process), it is the application context in the device language.
 */
fun Context.appLanguageContext(): Context {
    val app = applicationContext
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return app
    val locales = AppCompatDelegate.getApplicationLocales()
    if (locales.isEmpty) return app
    val configuration = Configuration(app.resources.configuration)
    configuration.setLocales(LocaleList.forLanguageTags(locales.toLanguageTags()))
    return app.createConfigurationContext(configuration)
}
