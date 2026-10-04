package ch.stenzel.tim.polleninfo.core.diary.domain.model

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import kotlin.test.Test
import kotlin.test.assertEquals

class FeelingTest {

    @Test
    fun `each feeling sits on the pollen scale with higher meaning worse`() {
        assertEquals(PollenSeverity.VERY_HIGH, Feeling.VERY_BAD.level)
        assertEquals(PollenSeverity.HIGH, Feeling.BAD.level)
        assertEquals(PollenSeverity.MODERATE, Feeling.GOOD.level)
        assertEquals(PollenSeverity.LOW, Feeling.VERY_GOOD.level)
    }

    @Test
    fun `feelings are declared from very bad to very good`() {
        assertEquals(
            listOf(Feeling.VERY_BAD, Feeling.BAD, Feeling.GOOD, Feeling.VERY_GOOD),
            Feeling.entries,
        )
    }
}
