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
) {

    val stationAbbr: String get() = draft.stationAbbr
    val species: Set<String> get() = draft.species
    val minSeverity: PollenSeverity get() = draft.minSeverity
    val days: Set<DayOfWeek> get() = draft.days
    val schedule: AlarmSchedule get() = draft.schedule

    /**
     * Whether the backend would accept this alarm. An alarm without pollen types or without days
     * could never fire, so it cannot be saved.
     */
    val isValid: Boolean get() = draft.species.isNotEmpty() && draft.days.isNotEmpty()

    val isDirty: Boolean get() = draft != initial

    /** The minimum severities a daily report offers. [PollenSeverity.NONE] is "Any": always sent. */
    val severityOptions: List<PollenSeverity> get() = PollenSeverity.entries

    fun toDraft(): AlarmDraft = draft

    fun withStation(abbr: String) = edit { copy(stationAbbr = abbr) }

    fun toggleSpecies(id: String) = edit { copy(species = species.toggle(id)) }

    fun withMinSeverity(severity: PollenSeverity) = edit { copy(minSeverity = severity) }

    fun toggleDay(day: DayOfWeek) = edit { copy(days = days.toggle(day)) }

    /** The time of a daily report. */
    fun withTime(at: LocalTime) = edit { copy(schedule = AlarmSchedule.Daily(at)) }

    private inline fun edit(change: AlarmDraft.() -> AlarmDraft) = AlarmFormState(draft.change(), initial)

    override fun equals(other: Any?): Boolean =
        other is AlarmFormState && draft == other.draft && initial == other.initial

    override fun hashCode(): Int = 31 * draft.hashCode() + initial.hashCode()

    override fun toString(): String = "AlarmFormState(draft=$draft, initial=$initial)"

    companion object {
        val DEFAULT_DAILY_TIME = LocalTime(8, 0)

        /**
         * A new daily report: the home station, every pollen type in [speciesIds], every day,
         * "Any" severity, at [DEFAULT_DAILY_TIME] — so an alarm saved without changes reports on
         * everything, every morning.
         */
        fun newDailyReport(homeStationAbbr: String, speciesIds: Collection<String>): AlarmFormState {
            val draft = AlarmDraft(
                enabled = true,
                stationAbbr = homeStationAbbr,
                species = speciesIds.toSet(),
                minSeverity = PollenSeverity.NONE,
                days = DayOfWeek.entries.toSet(),
                schedule = AlarmSchedule.Daily(DEFAULT_DAILY_TIME),
            )
            return AlarmFormState(draft, initial = draft)
        }
    }
}

private fun <T> Set<T>.toggle(element: T): Set<T> = if (element in this) this - element else this + element
