package ch.stenzel.tim.polleninfo.core.history.domain.model

/** How far back a station's history reaches: [days] whole days, ending yesterday. */
enum class HistoryRange(val days: Int) {
    WEEK(7),
    MONTH(30),
    YEAR(365),
}
