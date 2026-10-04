package ch.stenzel.tim.polleninfo.core.diary.data.local

import ch.stenzel.tim.polleninfo.core.diary.domain.model.DiaryEntry
import ch.stenzel.tim.polleninfo.core.diary.domain.model.Feeling
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement

/**
 * The stored form of the diary: a JSON array of `{ "date": "2026-10-04", "feeling": "BAD" }`.
 *
 * Plain strings on purpose — ISO dates and enum names stay readable and portable, which is what a
 * later move to an account needs. Decoding never throws: an element it cannot read (an unknown
 * feeling from a newer version, a malformed date) is skipped, and input that is not an array at all
 * decodes to an empty diary rather than crashing the app on every start.
 */
object DiaryCodec {

    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class EntryDto(val date: String, val feeling: String)

    fun encode(entries: List<DiaryEntry>): String = json.encodeToString(
        entries.map { EntryDto(date = it.date.toString(), feeling = it.feeling.name) },
    )

    fun decode(text: String): List<DiaryEntry> {
        val array = try {
            json.parseToJsonElement(text) as? JsonArray
        } catch (e: IllegalArgumentException) {
            // SerializationException is an IllegalArgumentException.
            null
        } ?: return emptyList()

        return array.mapNotNull { element ->
            try {
                val dto = json.decodeFromJsonElement<EntryDto>(element)
                val feeling = Feeling.entries.firstOrNull { it.name == dto.feeling }
                feeling?.let { DiaryEntry(LocalDate.parse(dto.date), it) }
            } catch (e: IllegalArgumentException) {
                null
            }
        }
            .distinctBy { it.date }
            .sortedBy { it.date }
    }

    /** An ISO date as stored, or `null` if it cannot be read. */
    fun decodeDate(text: String): LocalDate? = try {
        LocalDate.parse(text)
    } catch (e: IllegalArgumentException) {
        null
    }
}
