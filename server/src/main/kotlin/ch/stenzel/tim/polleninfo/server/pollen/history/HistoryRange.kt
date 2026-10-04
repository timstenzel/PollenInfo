package ch.stenzel.tim.polleninfo.server.pollen.history

import java.time.LocalDate

/** How far back a station's history reaches, in whole days ending yesterday. */
enum class HistoryRange(val days: Int) {
    WEEK(7),
    MONTH(30),
    YEAR(365),
    ;

    /** The `range` query value: the lowercase name. */
    val wireName: String get() = name.lowercase()

    companion object {
        /**
         * Resolves a `range` query value — exactly `week`, `month` or `year`. Anything else,
         * including another casing or no value at all, is `null`, which the route answers with 400.
         */
        fun fromWireName(value: String?): HistoryRange? = entries.firstOrNull { it.wireName == value }
    }
}

/**
 * The dates a history over [range] covers on [today]: [HistoryRange.days] days ending **yesterday**.
 *
 * Today is never included: the publisher's daily average for a day exists only once that day is
 * over, so a window ending today would always end on a missing day.
 */
fun historyWindow(range: HistoryRange, today: LocalDate): ClosedRange<LocalDate> {
    val until = today.minusDays(1)
    return until.minusDays(range.days - 1L)..until
}
