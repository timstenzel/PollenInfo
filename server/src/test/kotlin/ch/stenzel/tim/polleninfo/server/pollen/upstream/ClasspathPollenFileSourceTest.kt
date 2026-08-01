package ch.stenzel.tim.polleninfo.server.pollen.upstream

import ch.stenzel.tim.polleninfo.server.pollen.domain.PollenStation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ClasspathPollenFileSourceTest {

    private val source = ClasspathPollenFileSource()

    @Test
    fun `serves a checked-in sample for every station`() = runTest {
        PollenStation.entries.forEach { station ->
            assertTrue(
                source.hourlyNow(station).isNotEmpty(),
                "no sample bytes for ${station.abbr}",
            )
        }
    }

    @Test
    fun `resolves the sample by the station's own published path`() = runTest {
        val bytes = source.hourlyNow(PollenStation.ZUERICH)

        // The first field of every row is the abbreviation, so a mixed-up path is visible in the
        // content rather than only in a file name.
        assertTrue(decodePublished(bytes).lineSequence().drop(1).first().startsWith("PZH;"))
    }

    @Test
    fun `fails rather than returning empty content when no sample exists`() = runTest {
        val empty = ClasspathPollenFileSource(root = "fixtures/nothing-here")

        assertFailsWith<NoSuchElementException> { empty.hourlyNow(PollenStation.ZUERICH) }
    }
}
