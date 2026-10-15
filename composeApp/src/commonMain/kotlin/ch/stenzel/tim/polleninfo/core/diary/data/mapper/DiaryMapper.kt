package ch.stenzel.tim.polleninfo.core.diary.data.mapper

import ch.stenzel.tim.polleninfo.core.diary.data.remote.dto.DiaryEntryDto
import ch.stenzel.tim.polleninfo.core.diary.domain.model.DiaryEntry
import ch.stenzel.tim.polleninfo.core.diary.domain.model.Feeling
import kotlinx.datetime.LocalDate

// A malformed date or an unknown feeling throws, which the repository turns into a `Failure`.
fun DiaryEntryDto.toDomain(): DiaryEntry = DiaryEntry(
    date = LocalDate.parse(date),
    feeling = Feeling.entries.first { it.name == feeling },
)

/** The wire name of a feeling: its enum name, which the backend's `DiaryFeeling` shares. */
internal fun Feeling.toWireName(): String = name
