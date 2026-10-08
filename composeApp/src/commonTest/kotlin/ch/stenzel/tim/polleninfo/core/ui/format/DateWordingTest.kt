package ch.stenzel.tim.polleninfo.core.ui.format

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class DateWordingTest {

    private val all = listOf(englishDates, germanDates, frenchDates, italianDates)

    private val weekend = setOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

    private val workingWeek =
        setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)

    // --- Dates ---

    @Test
    fun `the full date reads naturally in each language`() {
        val date = LocalDate(2026, 7, 29)

        assertEquals(listOf("29 July", "29. Juli", "29 juillet", "29 luglio"), all.map { it.fullDate(date) })
    }

    @Test
    fun `the day of the month has no leading zero`() {
        val date = LocalDate(2026, 8, 1)

        assertEquals(listOf("1 August", "1. August", "1 août", "1 agosto"), all.map { it.fullDate(date) })
    }

    @Test
    fun `the short date uses each language's abbreviation`() {
        val date = LocalDate(2026, 9, 4)

        assertEquals(listOf("4 Sep", "4. Sep.", "4 sept.", "4 set"), all.map { it.shortDate(date) })
    }

    @Test
    fun `the month-only form is the abbreviated month`() {
        val date = LocalDate(2026, 10, 1)

        assertEquals(listOf("Oct", "Okt.", "oct.", "ott"), all.map { it.monthOnly(date) })
    }

    @Test
    fun `January and December are the first and last names`() {
        assertEquals("Jan", englishDates.monthOnly(LocalDate(2026, 1, 31)))
        assertEquals("31. Dezember", germanDates.fullDate(LocalDate(2025, 12, 31)))
    }

    // --- Weekdays ---

    @Test
    fun `weekday names are Monday first`() {
        assertEquals(listOf("Mon", "Mo", "lun", "lun"), all.map { it.weekdayShort(DayOfWeek.MONDAY) })
        assertEquals(listOf("Sunday", "Sonntag", "dimanche", "domenica"), all.map { it.weekdayFull(DayOfWeek.SUNDAY) })
    }

    @Test
    fun `the working week is a range in each language`() {
        assertEquals(listOf("Mon–Fri", "Mo–Fr", "lun–ven", "lun–ven"), all.map { it.weekdays(workingWeek) })
    }

    @Test
    fun `the weekend is a short list in each language`() {
        assertEquals(listOf("Sat, Sun", "Sa, So", "sam, dim", "sab, dom"), all.map { it.weekdays(weekend) })
    }

    @Test
    fun `three consecutive days are a range and two are not`() {
        assertEquals("Mo–Mi", germanDates.weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY)))
        assertEquals("Mo, Di", germanDates.weekdays(setOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY)))
    }

    @Test
    fun `ranges and single days combine in week order`() {
        assertEquals(
            "lun–gio, sab",
            italianDates.weekdays(setOf(DayOfWeek.SATURDAY, DayOfWeek.THURSDAY, DayOfWeek.WEDNESDAY, DayOfWeek.TUESDAY, DayOfWeek.MONDAY)),
        )
        assertEquals("lun, mer, ven", frenchDates.weekdays(setOf(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY)))
    }

    @Test
    fun `all seven days are one range`() {
        assertEquals("Mon–Sun", englishDates.weekdays(DayOfWeek.entries.toSet()))
    }

    @Test
    fun `a single day is its abbreviation`() {
        assertEquals("Do", germanDates.weekdays(setOf(DayOfWeek.THURSDAY)))
    }

    // --- Times and shape ---

    @Test
    fun `times are HH colon mm on a 24-hour clock`() {
        assertEquals("06:05", formatTime(LocalTime(6, 5)))
        assertEquals("00:00", formatTime(LocalTime(0, 0)))
        assertEquals("23:59", formatTime(LocalTime(23, 59)))
    }

    @Test
    fun `a wording without twelve months or seven weekdays is rejected`() {
        assertFailsWith<IllegalArgumentException> { englishDates.copy(monthsShort = englishDates.monthsShort.drop(1)) }
        assertFailsWith<IllegalArgumentException> { englishDates.copy(weekdaysFull = englishDates.weekdaysFull + "Funday") }
    }
}
