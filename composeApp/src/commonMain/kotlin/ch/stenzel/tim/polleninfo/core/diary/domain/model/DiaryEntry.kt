package ch.stenzel.tim.polleninfo.core.diary.domain.model

import kotlinx.datetime.LocalDate

/**
 * The user's answer for one Swiss calendar [date]. There is at most one per date, and it is
 * deliberately stored without a station: it describes the user, not a place.
 */
data class DiaryEntry(
    val date: LocalDate,
    val feeling: Feeling,
)
