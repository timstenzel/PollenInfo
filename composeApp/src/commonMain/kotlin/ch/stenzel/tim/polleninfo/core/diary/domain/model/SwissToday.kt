package ch.stenzel.tim.polleninfo.core.diary.domain.model

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

private val SWISS_ZONE = TimeZone.of("Europe/Zurich")

/**
 * The app's single definition of "today": the current calendar date in Switzerland, whatever zone
 * the device is in. Diary answers are compared with Swiss daily pollen means, so they have to be
 * filed under the same dates the backend's history uses.
 */
fun swissToday(clock: Clock): LocalDate = clock.todayIn(SWISS_ZONE)
