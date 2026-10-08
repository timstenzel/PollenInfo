package ch.stenzel.tim.polleninfo.core.language

/** Records what was [set] and reports it as [current] from then on. */
class FakeLanguageRepository(
    override var current: AppLanguage = AppLanguage.SYSTEM,
    override val appliesImmediately: Boolean = true,
) : LanguageRepository {
    val setCalls = mutableListOf<AppLanguage>()

    override fun set(language: AppLanguage) {
        setCalls += language
        current = language
    }
}
