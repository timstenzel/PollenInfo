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
    fun `a week runs from seven days back to yesterday`() {
        val window = historyWindow(HistoryRange.WEEK, today)

        assertEquals(LocalDate.of(2026, 9, 27), window.start)
        assertEquals(LocalDate.of(2026, 10, 3), window.endInclusive)
    }

    @Test
    fun `a year runs from 365 days back to yesterday`() {
        val window = historyWindow(HistoryRange.YEAR, today)

        assertEquals(LocalDate.of(2025, 10, 4), window.start)
        assertEquals(LocalDate.of(2026, 10, 3), window.endInclusive)
    }

    @Test
    fun `on 1 January every range lies wholly in the year before`() {
        val newYear = LocalDate.of(2026, 1, 1)

        assertEquals(LocalDate.of(2025, 12, 25)..LocalDate.of(2025, 12, 31), historyWindow(HistoryRange.WEEK, newYear))
        assertEquals(LocalDate.of(2025, 1, 1)..LocalDate.of(2025, 12, 31), historyWindow(HistoryRange.YEAR, newYear))
    }

    @Test
    fun `on 2 January a week ends on 1 January and starts in the year before`() {
        val window = historyWindow(HistoryRange.WEEK, LocalDate.of(2026, 1, 2))

        assertEquals(LocalDate.of(2025, 12, 26)..LocalDate.of(2026, 1, 1), window)
    }

    @Test
    fun `a year ending on a leap day still covers 365 days`() {
        val window = historyWindow(HistoryRange.YEAR, LocalDate.of(2024, 3, 1))

        assertEquals(LocalDate.of(2023, 3, 2)..LocalDate.of(2024, 2, 29), window)
    }

    @Test
    fun `a year reaching back over a leap day starts on 1 March`() {
        // 28 February 2025 back 365 days crosses 29 February 2024, so the window starts on 1 March.
        val window = historyWindow(HistoryRange.YEAR, LocalDate.of(2025, 3, 1))

        assertEquals(LocalDate.of(2024, 3, 1)..LocalDate.of(2025, 2, 28), window)
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
