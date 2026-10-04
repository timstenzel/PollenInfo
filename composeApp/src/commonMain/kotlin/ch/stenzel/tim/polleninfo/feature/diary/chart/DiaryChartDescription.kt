package ch.stenzel.tim.polleninfo.feature.diary.chart

import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryRange

/**
 * What a screen reader announces for the chart, which is one node: "Graph of your diary and 7
 * pollen types, last 30 days". The period follows the [range]: "last 7 days", "last 30 days",
 * "last 12 months" — a year as "last 365 days" would make the listener do the arithmetic.
 */
fun diaryChartDescription(speciesCount: Int, range: HistoryRange): String {
    val types = if (speciesCount == 1) "1 pollen type" else "$speciesCount pollen types"
    val period = when (range) {
        HistoryRange.WEEK -> "last 7 days"
        HistoryRange.MONTH -> "last 30 days"
        HistoryRange.YEAR -> "last 12 months"
    }
    return "Graph of your diary and $types, $period"
}
