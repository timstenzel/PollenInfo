package ch.stenzel.tim.polleninfo.core.diary.data.remote.dto

import kotlinx.serialization.Serializable

/** Body of `POST /users/me/diary`: a `Feeling` name. */
@Serializable
data class DiaryAnswerRequestDto(val feeling: String)

/** One answer as the backend sends it: an ISO date (`2026-10-15`) and a `Feeling` name. */
@Serializable
data class DiaryEntryDto(val date: String, val feeling: String)
