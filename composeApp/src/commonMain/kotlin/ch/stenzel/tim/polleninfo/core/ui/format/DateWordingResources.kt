package ch.stenzel.tim.polleninfo.core.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.date_day_month
import ch.stenzel.tim.polleninfo.resources.date_months_full
import ch.stenzel.tim.polleninfo.resources.date_months_short
import ch.stenzel.tim.polleninfo.resources.date_weekdays_full
import ch.stenzel.tim.polleninfo.resources.date_weekdays_short
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.getStringArray
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource

/** The [DateWording] of the app's current language, for composables. */
@Composable
fun rememberDateWording(): DateWording {
    val monthsFull = stringArrayResource(Res.array.date_months_full)
    val monthsShort = stringArrayResource(Res.array.date_months_short)
    val weekdaysFull = stringArrayResource(Res.array.date_weekdays_full)
    val weekdaysShort = stringArrayResource(Res.array.date_weekdays_short)
    val dayMonthPattern = stringResource(Res.string.date_day_month)
    return remember(monthsFull, monthsShort, weekdaysFull, weekdaysShort, dayMonthPattern) {
        DateWording(monthsFull, monthsShort, weekdaysFull, weekdaysShort, dayMonthPattern)
    }
}

/** The [DateWording] of the app's current language, for code outside Compose (the push service). */
suspend fun loadDateWording(): DateWording = DateWording(
    monthsFull = getStringArray(Res.array.date_months_full),
    monthsShort = getStringArray(Res.array.date_months_short),
    weekdaysFull = getStringArray(Res.array.date_weekdays_full),
    weekdaysShort = getStringArray(Res.array.date_weekdays_short),
    dayMonthPattern = getString(Res.string.date_day_month),
)
