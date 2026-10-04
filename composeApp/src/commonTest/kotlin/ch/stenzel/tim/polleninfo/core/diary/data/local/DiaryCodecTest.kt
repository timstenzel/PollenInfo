package ch.stenzel.tim.polleninfo.core.diary.data.local

import ch.stenzel.tim.polleninfo.core.diary.domain.model.DiaryEntry
import ch.stenzel.tim.polleninfo.core.diary.domain.model.Feeling
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.datetime.LocalDate

class DiaryCodecTest {

    private val entries = listOf(
        DiaryEntry(LocalDate(2025, 12, 31), Feeling.VERY_BAD),
        DiaryEntry(LocalDate(2026, 1, 1), Feeling.BAD),
        DiaryEntry(LocalDate(2026, 2, 28), Feeling.GOOD),
        DiaryEntry(LocalDate(2026, 10, 4), Feeling.VERY_GOOD),
    )

    @Test
    fun `entries survive a round trip unchanged`() {
        assertEquals(entries, DiaryCodec.decode(DiaryCodec.encode(entries)))
    }

    @Test
    fun `an empty diary survives a round trip`() {
        assertEquals(emptyList(), DiaryCodec.decode(DiaryCodec.encode(emptyList())))
    }

    @Test
    fun `stores ISO dates and feeling names`() {
        assertEquals(
            """[{"date":"2026-10-04","feeling":"VERY_GOOD"}]""",
            DiaryCodec.encode(listOf(DiaryEntry(LocalDate(2026, 10, 4), Feeling.VERY_GOOD))),
        )
    }

    @Test
    fun `corrupt input decodes to an empty diary`() {
        assertEquals(emptyList(), DiaryCodec.decode("not json"))
        assertEquals(emptyList(), DiaryCodec.decode("""[{"date":"2026-10-04""""))
        assertEquals(emptyList(), DiaryCodec.decode("""{"date":"2026-10-04","feeling":"BAD"}"""))
        assertEquals(emptyList(), DiaryCodec.decode(""))
    }

    @Test
    fun `an unknown feeling is skipped and the rest kept`() {
        val decoded = DiaryCodec.decode(
            """[{"date":"2026-10-03","feeling":"MEH"},{"date":"2026-10-04","feeling":"BAD"}]""",
        )

        assertEquals(listOf(DiaryEntry(LocalDate(2026, 10, 4), Feeling.BAD)), decoded)
    }

    @Test
    fun `malformed elements are skipped and the rest kept`() {
        val decoded = DiaryCodec.decode(
            """[{"date":"4.10.2026","feeling":"BAD"},{"feeling":"BAD"},42,""" +
                """{"date":"2026-10-04","feeling":"GOOD"}]""",
        )

        assertEquals(listOf(DiaryEntry(LocalDate(2026, 10, 4), Feeling.GOOD)), decoded)
    }

    @Test
    fun `decoding sorts by date and keeps the first answer for a repeated date`() {
        val decoded = DiaryCodec.decode(
            """[{"date":"2026-10-04","feeling":"GOOD"},{"date":"2026-10-03","feeling":"BAD"},""" +
                """{"date":"2026-10-04","feeling":"VERY_BAD"}]""",
        )

        assertEquals(
            listOf(
                DiaryEntry(LocalDate(2026, 10, 3), Feeling.BAD),
                DiaryEntry(LocalDate(2026, 10, 4), Feeling.GOOD),
            ),
            decoded,
        )
    }

    @Test
    fun `a stored dismissal date decodes and a malformed one is null`() {
        assertEquals(LocalDate(2026, 10, 4), DiaryCodec.decodeDate("2026-10-04"))
        assertNull(DiaryCodec.decodeDate("yesterday"))
    }
}
