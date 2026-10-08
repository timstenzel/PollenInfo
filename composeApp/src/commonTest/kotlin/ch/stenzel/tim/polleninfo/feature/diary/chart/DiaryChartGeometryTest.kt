package ch.stenzel.tim.polleninfo.feature.diary.chart

import ch.stenzel.tim.polleninfo.core.diary.domain.model.DiaryEntry
import ch.stenzel.tim.polleninfo.core.diary.domain.model.Feeling
import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryDay
import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryRange
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.diary_chart_description_month
import ch.stenzel.tim.polleninfo.resources.diary_chart_description_week
import ch.stenzel.tim.polleninfo.resources.diary_chart_description_year
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
        val run = diaryChartGeometry(month(), listOf("BIRCH"), HistoryRange.MONTH, width = 290f, height = 100f).lines.single().runs.single()

        assertEquals(0f, run.first().x)
    }

    @Test
    fun `the last day sits at the right edge`() {
        val run = diaryChartGeometry(month(), listOf("BIRCH"), HistoryRange.MONTH, width = 290f, height = 100f).lines.single().runs.single()

        assertEquals(290f, run.last().x)
    }

    @Test
    fun `a middle day sits at its share of the width`() {
        // 30 days give 29 equal steps of 10 px across 290 px.
        val run = diaryChartGeometry(month(), listOf("BIRCH"), HistoryRange.MONTH, width = 290f, height = 100f).lines.single().runs.single()

        assertEquals(150f, run[15].x)
        assertEquals(10f, run[1].x)
    }

    @Test
    fun `a single day is centred`() {
        val run = diaryChartGeometry(days(PollenSeverity.LOW), listOf("BIRCH"), HistoryRange.MONTH, width = 200f, height = 100f)
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
            HistoryRange.MONTH,
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
            HistoryRange.MONTH,
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
            HistoryRange.MONTH,
            width = 200f,
            height = 100f,
        )

        assertEquals(listOf(listOf(ChartPoint(100f, 25f))), geometry.lines.single().runs)
    }

    @Test
    fun `a species without any value has no line`() {
        val geometry = diaryChartGeometry(days(null, null), listOf("BIRCH", "ASH"), HistoryRange.MONTH, width = 100f, height = 100f)

        assertEquals(emptyList(), geometry.lines)
    }

    @Test
    fun `an unchecked species produces no line`() {
        // Birch and grasses both have values; only grasses is among the checked ids passed in.
        val days = month().map { it.copy(levels = mapOf("BIRCH" to PollenSeverity.LOW, "GRASSES" to PollenSeverity.HIGH)) }

        val geometry = diaryChartGeometry(days, listOf("GRASSES"), HistoryRange.MONTH, width = 290f, height = 100f)

        assertEquals(listOf("GRASSES"), geometry.lines.map { it.speciesId })
    }

    @Test
    fun `lines follow the order of the ids passed in`() {
        val days = month().map { it.copy(levels = mapOf("BIRCH" to PollenSeverity.LOW, "GRASSES" to PollenSeverity.HIGH)) }

        val geometry = diaryChartGeometry(days, listOf("GRASSES", "BIRCH"), HistoryRange.MONTH, width = 290f, height = 100f)

        assertEquals(listOf("GRASSES", "BIRCH"), geometry.lines.map { it.speciesId })
    }

    @Test
    fun `there is one level tick per severity`() {
        val geometry = diaryChartGeometry(month(), listOf("BIRCH"), HistoryRange.MONTH, width = 290f, height = 100f)

        assertEquals(PollenSeverity.entries, geometry.levelTicks.map { it.level })
        assertEquals(listOf(100f, 75f, 50f, 25f, 0f), geometry.levelTicks.map { it.y })
    }

    @Test
    fun `a month has a date tick every seven days ending on the last day`() {
        val geometry = diaryChartGeometry(month(), listOf("BIRCH"), HistoryRange.MONTH, width = 290f, height = 100f)

        assertEquals(
            listOf(LocalDate(2026, 9, 5), LocalDate(2026, 9, 12), LocalDate(2026, 9, 19), LocalDate(2026, 9, 26), LocalDate(2026, 10, 3)),
            geometry.dateTicks.map { it.date },
        )
        assertEquals(listOf(10f, 80f, 150f, 220f, 290f), geometry.dateTicks.map { it.x })
    }

    @Test
    fun `a week has a date tick on every day`() {
        val week = days(*Array(7) { PollenSeverity.LOW })

        val geometry = diaryChartGeometry(week, listOf("BIRCH"), HistoryRange.WEEK, width = 300f, height = 100f)

        assertEquals(week.map { it.date }, geometry.dateTicks.map { it.date })
        assertEquals(listOf(0f, 50f, 100f, 150f, 200f, 250f, 300f), geometry.dateTicks.map { it.x })
    }

    @Test
    fun `a year has a date tick on the first of every month`() {
        // 4 October 2025 to 3 October 2026, as the backend sends it on 4 October 2026.
        val from = LocalDate(2025, 10, 4)
        val year = (0 until 365).map { HistoryDay(from.plus(DatePeriod(days = it)), mapOf("BIRCH" to PollenSeverity.LOW)) }

        val geometry = diaryChartGeometry(year, listOf("BIRCH"), HistoryRange.YEAR, width = 364f, height = 100f)

        assertEquals(
            listOf(
                LocalDate(2025, 11, 1), LocalDate(2025, 12, 1), LocalDate(2026, 1, 1), LocalDate(2026, 2, 1),
                LocalDate(2026, 3, 1), LocalDate(2026, 4, 1), LocalDate(2026, 5, 1), LocalDate(2026, 6, 1),
                LocalDate(2026, 7, 1), LocalDate(2026, 8, 1), LocalDate(2026, 9, 1), LocalDate(2026, 10, 1),
            ),
            geometry.dateTicks.map { it.date },
        )
        // One px per day: 1 November is day 28, 1 October day 362.
        assertEquals(28f, geometry.dateTicks.first().x)
        assertEquals(362f, geometry.dateTicks.last().x)
    }

    @Test
    fun `a year starting on the first of a month labels its first day`() {
        val from = LocalDate(2025, 11, 1)
        val year = (0 until 365).map { HistoryDay(from.plus(DatePeriod(days = it)), mapOf("BIRCH" to PollenSeverity.LOW)) }

        val geometry = diaryChartGeometry(year, listOf("BIRCH"), HistoryRange.YEAR, width = 364f, height = 100f)

        assertEquals(DateTick(from, 0f), geometry.dateTicks.first())
        assertEquals(12, geometry.dateTicks.size)
    }

    /** An entry on the day [index] days after [start]. */
    private fun entry(index: Int, feeling: Feeling) = DiaryEntry(start.plus(DatePeriod(days = index)), feeling)

    @Test
    fun `a very bad answer sits level with very high pollen`() {
        val geometry = diaryChartGeometry(
            days(PollenSeverity.VERY_HIGH),
            listOf("BIRCH"),
            HistoryRange.MONTH,
            width = 100f,
            height = 80f,
            entries = listOf(entry(0, Feeling.VERY_BAD)),
        )

        assertEquals(geometry.lines.single().runs.single().single(), geometry.feeling.dots.single())
        assertEquals(levelY(PollenSeverity.VERY_HIGH, 80f), geometry.feeling.dots.single().y)
    }

    @Test
    fun `a very good answer sits level with low pollen`() {
        val geometry = diaryChartGeometry(
            days(PollenSeverity.LOW),
            listOf("BIRCH"),
            HistoryRange.MONTH,
            width = 100f,
            height = 80f,
            entries = listOf(entry(0, Feeling.VERY_GOOD)),
        )

        assertEquals(geometry.lines.single().runs.single().single(), geometry.feeling.dots.single())
        assertEquals(levelY(PollenSeverity.LOW, 80f), geometry.feeling.dots.single().y)
    }

    @Test
    fun `every feeling takes the height of its level`() {
        val geometry = diaryChartGeometry(
            days(*Array(4) { PollenSeverity.LOW }),
            listOf("BIRCH"),
            HistoryRange.WEEK,
            width = 300f,
            height = 100f,
            entries = listOf(
                entry(0, Feeling.VERY_BAD),
                entry(1, Feeling.BAD),
                entry(2, Feeling.GOOD),
                entry(3, Feeling.VERY_GOOD),
            ),
        )

        assertEquals(listOf(listOf(ChartPoint(0f, 0f), ChartPoint(100f, 25f), ChartPoint(200f, 50f), ChartPoint(300f, 75f))), geometry.feeling.runs)
    }

    @Test
    fun `an unanswered day splits the feeling line with a dot on each answered day`() {
        // Monday and Wednesday answered and Tuesday not: two single-point runs and no line across.
        val geometry = diaryChartGeometry(
            days(*Array(3) { PollenSeverity.LOW }),
            listOf("BIRCH"),
            HistoryRange.WEEK,
            width = 200f,
            height = 100f,
            entries = listOf(entry(0, Feeling.BAD), entry(2, Feeling.GOOD)),
        )

        assertEquals(listOf(listOf(ChartPoint(0f, 25f)), listOf(ChartPoint(200f, 50f))), geometry.feeling.runs)
        assertEquals(listOf(ChartPoint(0f, 25f), ChartPoint(200f, 50f)), geometry.feeling.dots)
    }

    @Test
    fun `a run of answers is one polyline and each of its days still has a dot`() {
        val geometry = diaryChartGeometry(
            days(*Array(5) { PollenSeverity.LOW }),
            listOf("BIRCH"),
            HistoryRange.WEEK,
            width = 400f,
            height = 100f,
            entries = listOf(entry(0, Feeling.BAD), entry(1, Feeling.BAD), entry(3, Feeling.GOOD)),
        )

        assertEquals(listOf(2, 1), geometry.feeling.runs.map { it.size })
        assertEquals(listOf(0f, 100f, 300f), geometry.feeling.dots.map { it.x })
    }

    @Test
    fun `an isolated answer between two unanswered days is kept as a dot`() {
        val geometry = diaryChartGeometry(
            days(*Array(3) { PollenSeverity.LOW }),
            listOf("BIRCH"),
            HistoryRange.WEEK,
            width = 200f,
            height = 100f,
            entries = listOf(entry(1, Feeling.VERY_BAD)),
        )

        assertEquals(listOf(listOf(ChartPoint(100f, 0f))), geometry.feeling.runs)
        assertEquals(listOf(ChartPoint(100f, 0f)), geometry.feeling.dots)
    }

    @Test
    fun `the feeling line ignores answers outside the days shown`() {
        val geometry = diaryChartGeometry(
            days(PollenSeverity.LOW, PollenSeverity.LOW),
            listOf("BIRCH"),
            HistoryRange.WEEK,
            width = 100f,
            height = 100f,
            entries = listOf(entry(-1, Feeling.BAD), entry(2, Feeling.BAD)),
        )

        assertEquals(emptyList(), geometry.feeling.runs)
    }

    @Test
    fun `no answers give a feeling line with no runs`() {
        val geometry = diaryChartGeometry(month(), listOf("BIRCH"), HistoryRange.MONTH, width = 290f, height = 100f)

        assertEquals(emptyList(), geometry.feeling.dots)
    }

    @Test
    fun `the feeling words sit at the heights of their levels`() {
        val geometry = diaryChartGeometry(month(), listOf("BIRCH"), HistoryRange.MONTH, width = 290f, height = 100f)

        assertEquals(
            listOf(
                FeelingTick(Feeling.VERY_BAD, 0f),
                FeelingTick(Feeling.BAD, 25f),
                FeelingTick(Feeling.GOOD, 50f),
                FeelingTick(Feeling.VERY_GOOD, 75f),
            ),
            geometry.feelingTicks,
        )
    }

    @Test
    fun `the description passes the species count as its quantity and argument`() {
        assertEquals(DiaryChartDescription(Res.plurals.diary_chart_description_month, 7), diaryChartDescription(7, HistoryRange.MONTH))
        assertEquals(DiaryChartDescription(Res.plurals.diary_chart_description_month, 1), diaryChartDescription(1, HistoryRange.MONTH))
        assertEquals(DiaryChartDescription(Res.plurals.diary_chart_description_month, 0), diaryChartDescription(0, HistoryRange.MONTH))
    }

    @Test
    fun `the description follows the range`() {
        assertEquals(DiaryChartDescription(Res.plurals.diary_chart_description_week, 7), diaryChartDescription(7, HistoryRange.WEEK))
        assertEquals(DiaryChartDescription(Res.plurals.diary_chart_description_month, 7), diaryChartDescription(7, HistoryRange.MONTH))
        assertEquals(DiaryChartDescription(Res.plurals.diary_chart_description_year, 7), diaryChartDescription(7, HistoryRange.YEAR))
    }
}
