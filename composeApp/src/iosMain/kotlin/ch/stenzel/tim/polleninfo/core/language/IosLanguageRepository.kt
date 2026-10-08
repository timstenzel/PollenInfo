package ch.stenzel.tim.polleninfo.core.language

import platform.Foundation.NSBundle
import platform.Foundation.NSUserDefaults

/**
 * The app language as the app's own `AppleLanguages` default, which iOS reads at launch — so a
 * change applies the next time the app is opened. [AppLanguage.SYSTEM] removes the key, and the
 * device's language list applies again.
 *
 * [current] reads the app's own domain only: `standardUserDefaults` would also answer from the
 * global domain, where `AppleLanguages` is the device's list, and "System default" would then read
 * back as the device language. Compile-verified only — there is no iOS app wrapper yet.
 */
class IosLanguageRepository : LanguageRepository {
    private val defaults = NSUserDefaults.standardUserDefaults

    override val current: AppLanguage
        get() {
            val domain = NSBundle.mainBundle.bundleIdentifier?.let { defaults.persistentDomainForName(it) }
            val languages = domain?.get(APPLE_LANGUAGES) as? List<*>
            return AppLanguage.fromTag(languages?.firstOrNull() as? String)
        }

    override fun set(language: AppLanguage) {
        val tag = language.tag
        if (tag == null) defaults.removeObjectForKey(APPLE_LANGUAGES) else defaults.setObject(listOf(tag), APPLE_LANGUAGES)
    }

    override val appliesImmediately: Boolean = false
}

private const val APPLE_LANGUAGES = "AppleLanguages"
