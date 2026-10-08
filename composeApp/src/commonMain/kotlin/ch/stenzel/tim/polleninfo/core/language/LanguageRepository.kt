package ch.stenzel.tim.polleninfo.core.language

/**
 * The app's language setting. It is owned by the platform, not by our DataStore: on Android it is
 * the same setting as the system's per-app language screen, on iOS the app's `AppleLanguages`
 * default. Implementations are logic-free and checked by hand; consumers use `FakeLanguageRepository`.
 */
interface LanguageRepository {
    /** The language currently chosen. Read synchronously, so a screen can show it from the start. */
    val current: AppLanguage

    /** Chooses [language]. Applies at once when [appliesImmediately], otherwise at the next launch. */
    fun set(language: AppLanguage)

    /** `true` on Android, which re-creates the screen in the new language; `false` on iOS. */
    val appliesImmediately: Boolean
}
