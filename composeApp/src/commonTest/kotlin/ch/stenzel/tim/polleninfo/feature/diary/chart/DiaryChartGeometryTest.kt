package ch.stenzel.tim.polleninfo.feature.diary.chart

import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryDay
import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryRange
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlin.test.Test
import kotlin.test.assertEquals

class DiaryChartGeometryTest {

    private val start = LocalDate(2026, 9, 4)

    /** Days in order from [start]; each entry is that day's birch level, `null` for no value. */
    private fun days(vararg birch: PollenSeverity?): List<HistoryDay> =
        birch.mapIndexed { index, level -> HistoryDay(start.plus(DatePeriod(days = index)), mapOf("BIRCH" to level)) }

    private fun month(level: PollenSeverity? = PollenSeverity.LOW) = days(*Array(30) { level })

    @Test
    fun `the first day sits at the left edge`() {
        val run = diaryChartGeometry(month(), listOf("BIRCH"), width = 290f, height = 100f).lines.single().runs.single()

        assertEquals(0f, run.first().x)
    }

    @Test
    fun `the last day sits at the right edge`() {
        val run = diaryChartGeometry(month(), listOf("BIRCH"), width = 290f, height = 100f).lines.single().runs.single()

        assertEquals(290f, run.last().x)
    }

    @Test
    fun `a middle day sits at its share of the width`() {
        // 30 days give 29 equal steps of 10 px across 290 px.
        val run = diaryChartGeometry(month(), listOf("BIRCH"), width = 290f, height = 100f).lines.single().runs.single()

        assertEquals(150f, run[15].x)
        assertEquals(10f, run[1].x)
    }

    @Test
    fun `a single day is centred`() {
        val run = diaryChartGeometry(days(PollenSeverity.LOW), listOf("BIRCH"), width = 200f, height = 100f)
            .lines.single().runs.single()

        assertEquals(100f, run.single().x)
    }

    @Test
    fun `a higher level is drawn higher with very high at the top and none at the bottom`() {
        assertEquals(100f, levelY(PollenSeverity.NONE, 100f))
        assertEquals(75f, levelY(PollenSeverity.LOW, 100f))
        assertEquals(50f, levelY(PollenSeverity.MODERATE, 100f))
        assertEquals(25f, levelY(PollenSeverity.HIGH, 100f))
        assertEquals(0f, levelY(PollenSeverity.VERY_HIGH, 100f))
    }

    @Test
    fun `points take the height of their level`() {
        val geometry = diaryChartGeometry(
            days(PollenSeverity.VERY_HIGH, PollenSeverity.NONE),
            listOf("BIRCH"),
            width = 100f,
            height = 80f,
        )

        assertEquals(listOf(0f, 80f), geometry.lines.single().runs.single().map { it.y })
    }

    @Test
    fun `a missing day splits the line`() {
        val geometry = diaryChartGeometry(
            days(PollenSeverity.LOW, PollenSeverity.HIGH, null, PollenSeverity.MODERATE, PollenSeverity.LOW),
            listOf("BIRCH"),
            width = 400f,
            height = 100f,
        )

        val runs = geometry.lines.single().runs
        assertEquals(2, runs.size)
        assertEquals(listOf(0f, 100f), runs[0].map { it.x })
        assertEquals(listOf(300f, 400f), runs[1].map { it.x })
    }

    @Test
    fun `an isolated day between two gaps is kept as a single point`() {
        val geometry = diaryChartGeometry(
            days(null, PollenSeverity.HIGH, null),
            listOf("BIRCH"),
            width = 200f,
            height = 100f,
        )

        assertEquals(listOf(listOf(ChartPoint(100f, 25f))), geometry.lines.single().runs)
    }

    @Test
    fun `a species without any value has a line with no runs`() {
        val geometry = diaryChartGeometry(days(null, null), listOf("BIRCH", "ASH"), width = 100f, height = 100f)

        assertEquals(listOf("BIRCH", "ASH"), geometry.lines.map { it.speciesId })
        assertEquals(listOf(emptyList(), emptyList()), geometry.lines.map { it.runs })
    }

    @Test
    fun `there is one level tick per severity`() {
        val geometry = diaryChartGeometry(month(), listOf("BIRCH"), width = 290f, height = 100f)

        assertEquals(PollenSeverity.entries, geometry.levelTicks.map { it.level })
        assertEquals(listOf(100f, 75f, 50f, 25f, 0f), geometry.levelTicks.map { it.y })
    }

    @Test
    fun `a month has a date tick every seven days ending on the last day`() {
        val geometry = diaryChartGeometry(month(), listOf("BIRCH"), width = 290f, height = 100f)

        assertEquals(
            listOf(LocalDate(2026, 9, 5), LocalDate(2026, 9, 12), LocalDate(2026, 9, 19), LocalDate(2026, 9, 26), LocalDate(2026, 10, 3)),
            geometry.dateTicks.map { it.date },
        )
        assertEquals(listOf(10f, 80f, 150f, 220f, 290f), geometry.dateTicks.map { it.x })
    }

    @Test
    fun `the description names the species count and the period`() {
        assertEquals("Graph of your diary and 7 pollen types, last 30 days", diaryChartDescription(7, HistoryRange.MONTH))
        assertEquals("Graph of your diary and 1 pollen type, last 30 days", diaryChartDescription(1, HistoryRange.MONTH))
    }
}
