package ch.stenzel.tim.polleninfo.core.language

/**
 * The languages the app can be set to. [SYSTEM] has no [tag]: it follows the device's language when
 * that is German, French or Italian and falls back to English otherwise — resource fallback does that
 * on its own, so nothing has to decide it here.
 */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    EN("en"),
    DE("de"),
    FR("fr"),
    IT("it"),
    ;

    companion object {
        /**
         * The language a stored or platform-reported BCP 47 [tag] names, judged by its primary subtag
         * alone, so `de-CH` is [DE]. A missing, empty or unsupported tag is [SYSTEM].
         */
        fun fromTag(tag: String?): AppLanguage {
            val primary = tag?.substringBefore('-')?.substringBefore('_')?.lowercase()
            if (primary.isNullOrEmpty()) return SYSTEM
            return entries.firstOrNull { it.tag == primary } ?: SYSTEM
        }
    }
}
