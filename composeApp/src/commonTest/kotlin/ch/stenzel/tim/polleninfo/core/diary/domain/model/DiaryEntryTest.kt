package ch.stenzel.tim.polleninfo.core.diary.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

class DiaryEntryTest {

    private val oct3 = LocalDate(2026, 10, 3)
    private val oct4 = LocalDate(2026, 10, 4)

    @Test
    fun `recording adds an entry for a new date in date order`() {
        val entries = listOf(DiaryEntry(oct4, Feeling.GOOD)).recording(oct3, Feeling.BAD)

        assertEquals(listOf(DiaryEntry(oct3, Feeling.BAD), DiaryEntry(oct4, Feeling.GOOD)), entries)
    }

    @Test
    fun `recording never replaces the answer already stored for a date`() {
        val entries = listOf(DiaryEntry(oct4, Feeling.GOOD)).recording(oct4, Feeling.VERY_BAD)

        assertEquals(listOf(DiaryEntry(oct4, Feeling.GOOD)), entries)
    }

    @Test
    fun `swiss today follows the Swiss date across UTC midnight`() {
        // 22:30 UTC on 3 October is already 00:30 on 4 October in Zürich (CEST, UTC+2).
        assertEquals(oct4, swissToday(clockAt("2026-10-03T22:30:00Z")))
        assertEquals(oct3, swissToday(clockAt("2026-10-03T21:59:59Z")))
    }

    private fun clockAt(instant: String) = object : Clock {
        override fun now() = Instant.parse(instant)
    }
}
