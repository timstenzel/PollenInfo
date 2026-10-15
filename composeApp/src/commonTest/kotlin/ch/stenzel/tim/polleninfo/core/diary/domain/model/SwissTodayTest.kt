package ch.stenzel.tim.polleninfo.core.diary.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.LocalDate

class SwissTodayTest {

    @Test
    fun `swiss today follows the Swiss date across UTC midnight`() {
        // 22:30 UTC on 3 October is already 00:30 on 4 October in Zürich (CEST, UTC+2).
        assertEquals(LocalDate(2026, 10, 4), swissToday(clockAt("2026-10-03T22:30:00Z")))
        assertEquals(LocalDate(2026, 10, 3), swissToday(clockAt("2026-10-03T21:59:59Z")))
    }

    private fun clockAt(instant: String) = object : Clock {
        override fun now() = Instant.parse(instant)
    }
}
