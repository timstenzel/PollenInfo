package ch.stenzel.tim.polleninfo.feature.home.presentation

import ch.stenzel.tim.polleninfo.feature.home.domain.model.ReadingAge
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class ReadingAgeLabelTest {

    @Test
    fun `a fresh reading states its update time on a 24-hour clock`() {
        assertEquals("Updated 09:00", ReadingAge.Fresh(LocalTime(9, 0)).label())
        assertEquals("Updated 17:05", ReadingAge.Fresh(LocalTime(17, 5)).label())
    }

    @Test
    fun `a stale reading names its date rather than a time`() {
        assertEquals("Data from 29 July", ReadingAge.Stale(LocalDate(2026, 7, 29)).label())
        assertEquals("Data from 1 August", ReadingAge.Stale(LocalDate(2026, 8, 1)).label())
    }
}
