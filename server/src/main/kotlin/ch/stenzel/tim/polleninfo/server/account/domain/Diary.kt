package ch.stenzel.tim.polleninfo.server.account.domain

import java.time.LocalDate

/**
 * How the user felt on a day — the answer to the app's "How do you feel today?". The names are the
 * wire format and the stored value, and equal the app's `Feeling` names.
 */
enum class DiaryFeeling {
    VERY_BAD,
    BAD,
    GOOD,
    VERY_GOOD;

    companion object {
        /** The feeling named exactly [name], or `null` for anything else. */
        fun fromNameOrNull(name: String): DiaryFeeling? = entries.firstOrNull { it.name == name }
    }
}

/** One answer: [feeling] on the Swiss calendar day [date]. */
data class DiaryEntry(val date: LocalDate, val feeling: DiaryFeeling)
