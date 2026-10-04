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

/**
 * These entries with [feeling] recorded for [date], sorted by date. A date that already has an
 * entry keeps it — answers are immutable, so a second tap (or a second device-side write racing the
 * first) never replaces the first answer.
 */
fun List<DiaryEntry>.recording(date: LocalDate, feeling: Feeling): List<DiaryEntry> =
    if (any { it.date == date }) this else (this + DiaryEntry(date, feeling)).sortedBy { it.date }
