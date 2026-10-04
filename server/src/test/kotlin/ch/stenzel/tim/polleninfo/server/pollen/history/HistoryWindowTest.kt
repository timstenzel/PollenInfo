package ch.stenzel.tim.polleninfo.server.pollen.history

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HistoryWindowTest {

    private val today = LocalDate.of(2026, 10, 4)

    @Test
    fun `every range ends yesterday`() {
        HistoryRange.entries.forEach { range ->
            assertEquals(LocalDate.of(2026, 10, 3), historyWindow(range, today).endInclusive, range.name)
        }
    }

    @Test
    fun `every range covers exactly its number of days`() {
        HistoryRange.entries.forEach { range ->
            val window = historyWindow(range, today)
            val days = ChronoUnit.DAYS.between(window.start, window.endInclusive) + 1

            assertEquals(range.days.toLong(), days, range.name)
        }
    }

    @Test
    fun `a month runs from thirty days back to yesterday`() {
        val window = historyWindow(HistoryRange.MONTH, today)

        assertEquals(LocalDate.of(2026, 9, 4), window.start)
        assertEquals(LocalDate.of(2026, 10, 3), window.endInclusive)
    }

    @Test
    fun `range values are the exact lowercase names`() {
        assertEquals(HistoryRange.WEEK, HistoryRange.fromWireName("week"))
        assertEquals(HistoryRange.MONTH, HistoryRange.fromWireName("month"))
        assertEquals(HistoryRange.YEAR, HistoryRange.fromWireName("year"))
    }

    @Test
    fun `any other range value is not a range`() {
        assertNull(HistoryRange.fromWireName(null))
        assertNull(HistoryRange.fromWireName(""))
        assertNull(HistoryRange.fromWireName("Month"))
        assertNull(HistoryRange.fromWireName("day"))
        assertNull(HistoryRange.fromWireName(" month"))
    }
}
