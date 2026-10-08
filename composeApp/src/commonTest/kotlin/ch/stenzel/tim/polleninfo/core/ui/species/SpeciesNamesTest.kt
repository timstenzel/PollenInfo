package ch.stenzel.tim.polleninfo.core.ui.species

import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.species_alder
import ch.stenzel.tim.polleninfo.resources.species_ash
import ch.stenzel.tim.polleninfo.resources.species_beech
import ch.stenzel.tim.polleninfo.resources.species_birch
import ch.stenzel.tim.polleninfo.resources.species_grasses
import ch.stenzel.tim.polleninfo.resources.species_hazel
import ch.stenzel.tim.polleninfo.resources.species_oak
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SpeciesNamesTest {

    @Test
    fun `every species the backend knows has its own name resource`() {
        assertEquals(
            listOf(
                Res.string.species_alder, Res.string.species_birch, Res.string.species_hazel, Res.string.species_beech,
                Res.string.species_ash, Res.string.species_oak, Res.string.species_grasses,
            ),
            listOf("ALDER", "BIRCH", "HAZEL", "BEECH", "ASH", "OAK", "GRASSES").map { speciesNameResource(it) },
        )
    }

    @Test
    fun `an unknown species id has no name resource`() {
        assertNull(speciesNameResource("MUGWORT"))
        assertNull(speciesNameResource("birch"))
    }

    @Test
    fun `a known species is named through its resource`() {
        val name = speciesName("BIRCH", fallback = "Birch") { if (it == Res.string.species_birch) "Birke" else "?" }

        assertEquals("Birke", name)
    }

    @Test
    fun `an unknown species id falls back to the server's name`() {
        val name = speciesName("MUGWORT", fallback = "Mugwort") { error("nothing to resolve") }

        assertEquals("Mugwort", name)
    }
}
