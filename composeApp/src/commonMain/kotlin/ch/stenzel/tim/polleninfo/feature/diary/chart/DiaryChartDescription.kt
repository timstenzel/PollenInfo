package ch.stenzel.tim.polleninfo.feature.diary.chart

import androidx.compose.runtime.Composable
import ch.stenzel.tim.polleninfo.core.history.domain.model.HistoryRange
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.diary_chart_description_month
import ch.stenzel.tim.polleninfo.resources.diary_chart_description_week
import ch.stenzel.tim.polleninfo.resources.diary_chart_description_year
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.pluralStringResource

/**
 * The sentence a screen reader announces for the chart, still to be put into words: one plural per
 * range, chosen by [speciesCount], which is also its one argument.
 */
data class DiaryChartDescription(val resource: PluralStringResource, val speciesCount: Int)

/**
 * What a screen reader announces for the chart, which is one node: "Graph of your diary and 7
 * pollen types, last 30 days". The period follows the [range]: "last 7 days", "last 30 days",
 * "last 12 months" — a year as "last 365 days" would make the listener do the arithmetic.
 */
fun diaryChartDescription(speciesCount: Int, range: HistoryRange): DiaryChartDescription {
    val resource = when (range) {
        HistoryRange.WEEK -> Res.plurals.diary_chart_description_week
        HistoryRange.MONTH -> Res.plurals.diary_chart_description_month
        HistoryRange.YEAR -> Res.plurals.diary_chart_description_year
    }
    return DiaryChartDescription(resource, speciesCount)
}

@Composable
fun DiaryChartDescription.resolve(): String = pluralStringResource(resource, speciesCount, speciesCount)
