package ch.stenzel.tim.polleninfo.core.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * The app language through AppCompat: on API 33+ it is the system's per-app language (so the
 * system's own screen and Settings can never disagree), below that AppCompat stores it itself
 * (`autoStoreLocales` in the manifest). [set] re-creates the running activities in the new
 * language; their ViewModels survive, as on any configuration change.
 *
 * Below API 33 the stored choice is only loaded once an `AppCompatActivity` has been created, so
 * [current] is meant for screens, not for a process FCM started without one.
 */
class AndroidLanguageRepository : LanguageRepository {
    override val current: AppLanguage
        get() = AppLanguage.fromTag(AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag())

    override fun set(language: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag ?: ""))
    }

    override val appliesImmediately: Boolean = true
}
