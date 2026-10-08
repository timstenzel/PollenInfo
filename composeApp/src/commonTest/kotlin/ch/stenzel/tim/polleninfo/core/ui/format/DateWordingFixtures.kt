package ch.stenzel.tim.polleninfo.core.ui.format

/**
 * Literal [DateWording]s for the four app languages, copied from the string resources, so formatting
 * is tested per language without loading resources. Keep them in step with the `composeResources`
 * string files.
 */
val englishDates = DateWording(
    monthsFull = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December",
    ),
    monthsShort = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"),
    weekdaysFull = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"),
    weekdaysShort = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
    dayMonthPattern = "%1\$s %2\$s",
)

val germanDates = DateWording(
    monthsFull = listOf(
        "Januar", "Februar", "März", "April", "Mai", "Juni",
        "Juli", "August", "September", "Oktober", "November", "Dezember",
    ),
    monthsShort = listOf("Jan.", "Feb.", "März", "Apr.", "Mai", "Juni", "Juli", "Aug.", "Sep.", "Okt.", "Nov.", "Dez."),
    weekdaysFull = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag"),
    weekdaysShort = listOf("Mo", "Di", "Mi", "Do", "Fr", "Sa", "So"),
    dayMonthPattern = "%1\$s. %2\$s",
)

val frenchDates = DateWording(
    monthsFull = listOf(
        "janvier", "février", "mars", "avril", "mai", "juin",
        "juillet", "août", "septembre", "octobre", "novembre", "décembre",
    ),
    monthsShort = listOf(
        "janv.", "févr.", "mars", "avr.", "mai", "juin", "juil.", "août", "sept.", "oct.", "nov.", "déc.",
    ),
    weekdaysFull = listOf("lundi", "mardi", "mercredi", "jeudi", "vendredi", "samedi", "dimanche"),
    weekdaysShort = listOf("lun", "mar", "mer", "jeu", "ven", "sam", "dim"),
    dayMonthPattern = "%1\$s %2\$s",
)

val italianDates = DateWording(
    monthsFull = listOf(
        "gennaio", "febbraio", "marzo", "aprile", "maggio", "giugno",
        "luglio", "agosto", "settembre", "ottobre", "novembre", "dicembre",
    ),
    monthsShort = listOf("gen", "feb", "mar", "apr", "mag", "giu", "lug", "ago", "set", "ott", "nov", "dic"),
    weekdaysFull = listOf("lunedì", "martedì", "mercoledì", "giovedì", "venerdì", "sabato", "domenica"),
    weekdaysShort = listOf("lun", "mar", "mer", "gio", "ven", "sab", "dom"),
    dayMonthPattern = "%1\$s %2\$s",
)
