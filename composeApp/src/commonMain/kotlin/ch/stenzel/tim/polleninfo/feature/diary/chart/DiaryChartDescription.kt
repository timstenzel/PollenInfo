package ch.stenzel.tim.polleninfo.feature.diary.chart

import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryRange

/**
 * What a screen reader announces for the chart, which is one node: "Graph of your diary and 7
 * pollen types, last 30 days".
 */
fun diaryChartDescription(speciesCount: Int, range: HistoryRange): String {
    val types = if (speciesCount == 1) "1 pollen type" else "$speciesCount pollen types"
    return "Graph of your diary and $types, last ${range.days} days"
}
