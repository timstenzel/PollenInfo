package ch.stenzel.tim.polleninfo.core.language

import kotlin.test.Test
import kotlin.test.assertEquals

class AppLanguageTest {

    @Test
    fun `a bare language tag maps to its language`() {
        assertEquals(AppLanguage.DE, AppLanguage.fromTag("de"))
        assertEquals(AppLanguage.IT, AppLanguage.fromTag("it"))
        assertEquals(AppLanguage.EN, AppLanguage.fromTag("en"))
        assertEquals(AppLanguage.FR, AppLanguage.fromTag("fr"))
    }

    @Test
    fun `a tag with a region maps to its language`() {
        assertEquals(AppLanguage.DE, AppLanguage.fromTag("de-CH"))
        assertEquals(AppLanguage.FR, AppLanguage.fromTag("fr-CH"))
        assertEquals(AppLanguage.EN, AppLanguage.fromTag("en-GB"))
    }

    @Test
    fun `an unsupported language is the system default`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag("es"))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag("rm-CH"))
    }

    @Test
    fun `no tag is the system default`() {
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(null))
        assertEquals(AppLanguage.SYSTEM, AppLanguage.fromTag(""))
    }

    @Test
    fun `every language reads back from its own tag`() {
        AppLanguage.entries.forEach { assertEquals(it, AppLanguage.fromTag(it.tag)) }
    }
}
