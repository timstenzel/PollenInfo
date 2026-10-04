package ch.stenzel.tim.polleninfo.feature.alarms.domain.model

import ch.stenzel.tim.polleninfo.core.measurement.domain.model.PollenSeverity
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime

/**
 * What the alarm editor's fields hold, and the rules over them — kept free of Compose and of the
 * ViewModel so every rule is a plain unit test.
 *
 * Every change returns a new state. [isDirty] compares against the state the editor opened with,
 * so a change that is undone again is not a change.
 */
class AlarmFormState private constructor(
    private val draft: AlarmDraft,
    private val initial: AlarmDraft,
    /**
     * True when editing an existing alarm: its type is what the alarm *is*, so [withType] changes
     * nothing. A different type is a new alarm.
     */
    val typeLocked: Boolean,
) {

    val stationAbbr: String get() = draft.stationAbbr
    val species: Set<String> get() = draft.species
    val minSeverity: PollenSeverity get() = draft.minSeverity
    val days: Set<DayOfWeek> get() = draft.days
    val schedule: AlarmSchedule get() = draft.schedule

    val type: AlarmType
        get() = when (draft.schedule) {
            is AlarmSchedule.Daily -> AlarmType.DAILY
            is AlarmSchedule.Threshold -> AlarmType.THRESHOLD
        }

    /**
     * Whether the backend would accept this alarm. An alarm without pollen types or without days
     * could never fire, and a threshold window whose end is not after its start never opens (windows
     * across midnight are not supported), so none of them can be saved.
     */
    val isValid: Boolean
        get() = draft.species.isNotEmpty() && draft.days.isNotEmpty() && isWindowValid

    /** False only for a threshold window whose end is not after its start. */
    val isWindowValid: Boolean
        get() = when (val schedule = draft.schedule) {
            is AlarmSchedule.Daily -> true
            is AlarmSchedule.Threshold -> schedule.until > schedule.from
        }

    val isDirty: Boolean get() = draft != initial

    /**
     * The severities this type offers. A daily report starts at [PollenSeverity.NONE] ("Any":
     * always sent); a threshold alert at Low, since "Any" would alert on nothing at all.
     */
    val severityOptions: List<PollenSeverity>
        get() = when (type) {
            AlarmType.DAILY -> PollenSeverity.entries
            AlarmType.THRESHOLD -> PollenSeverity.entries - PollenSeverity.NONE
        }

    fun toDraft(): AlarmDraft = draft

    fun withStation(abbr: String) = edit { copy(stationAbbr = abbr) }

    fun toggleSpecies(id: String) = edit { copy(species = species.toggle(id)) }

    fun withMinSeverity(severity: PollenSeverity) = edit { copy(minSeverity = severity) }

    fun toggleDay(day: DayOfWeek) = edit { copy(days = days.toggle(day)) }

    /**
     * Switches between a daily report and a threshold alert. The severity and the time fields mean
     * something different in each, so both go back to the new type's defaults; choosing the current
     * type changes nothing, and neither does any choice while [typeLocked].
     */
    fun withType(type: AlarmType): AlarmFormState {
        if (type == this.type || typeLocked) return this
        return edit {
            when (type) {
                AlarmType.DAILY -> copy(
                    minSeverity = DEFAULT_DAILY_SEVERITY,
                    schedule = AlarmSchedule.Daily(DEFAULT_DAILY_TIME),
                )
                AlarmType.THRESHOLD -> copy(
                    minSeverity = DEFAULT_THRESHOLD_SEVERITY,
                    schedule = AlarmSchedule.Threshold(DEFAULT_WINDOW_FROM, DEFAULT_WINDOW_UNTIL),
                )
            }
        }
    }

    /** The time of a daily report; ignored for a threshold alert. */
    fun withTime(at: LocalTime) = editSchedule<AlarmSchedule.Daily> { AlarmSchedule.Daily(at) }

    /** The start of a threshold alert's window; ignored for a daily report. */
    fun withWindowStart(from: LocalTime) = editSchedule<AlarmSchedule.Threshold> { copy(from = from) }

    /** The end of a threshold alert's window; ignored for a daily report. */
    fun withWindowEnd(until: LocalTime) = editSchedule<AlarmSchedule.Threshold> { copy(until = until) }

    private inline fun <reified S : AlarmSchedule> editSchedule(change: S.() -> S): AlarmFormState {
        val schedule = draft.schedule as? S ?: return this
        return edit { copy(schedule = schedule.change()) }
    }

    private inline fun edit(change: AlarmDraft.() -> AlarmDraft) = AlarmFormState(draft.change(), initial, typeLocked)

    override fun equals(other: Any?): Boolean =
        other is AlarmFormState && draft == other.draft && initial == other.initial &&
            typeLocked == other.typeLocked

    override fun hashCode(): Int = 31 * (31 * draft.hashCode() + initial.hashCode()) + typeLocked.hashCode()

    override fun toString(): String = "AlarmFormState(draft=$draft, initial=$initial, typeLocked=$typeLocked)"

    companion object {
        val DEFAULT_DAILY_TIME = LocalTime(8, 0)
        val DEFAULT_DAILY_SEVERITY = PollenSeverity.NONE
        val DEFAULT_THRESHOLD_SEVERITY = PollenSeverity.HIGH
        val DEFAULT_WINDOW_FROM = LocalTime(7, 0)
        val DEFAULT_WINDOW_UNTIL = LocalTime(21, 0)

        /**
         * A new daily report: the home station, every pollen type in [speciesIds], every day,
         * "Any" severity, at [DEFAULT_DAILY_TIME] — so an alarm saved without changes reports on
         * everything, every morning. [withType] turns it into a threshold alert.
         */
        fun newDailyReport(homeStationAbbr: String, speciesIds: Collection<String>): AlarmFormState {
            val draft = AlarmDraft(
                enabled = true,
                stationAbbr = homeStationAbbr,
                species = speciesIds.toSet(),
                minSeverity = DEFAULT_DAILY_SEVERITY,
                days = DayOfWeek.entries.toSet(),
                schedule = AlarmSchedule.Daily(DEFAULT_DAILY_TIME),
            )
            return AlarmFormState(draft, initial = draft, typeLocked = false)
        }

        /**
         * [alarm] opened for editing: every field as stored, its type locked. Whether it is paused
         * is kept as it is — the list's switch owns that — so saving an edit never resumes it.
         */
        fun fromAlarm(alarm: Alarm): AlarmFormState {
            val draft = alarm.toDraft()
            return AlarmFormState(draft, initial = draft, typeLocked = true)
        }
    }
}

private fun <T> Set<T>.toggle(element: T): Set<T> = if (element in this) this - element else this + element

/** What an alarm does: a report at a fixed time, or an alert while a window is open. */
enum class AlarmType { DAILY, THRESHOLD }
