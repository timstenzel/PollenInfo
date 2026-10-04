package ch.stenzel.tim.polleninfo.feature.alarms.domain.repository

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.Alarm
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmDraft

/**
 * This device's alarms on the backend.
 *
 * The device registers itself the first time any call needs it, so callers never see a
 * registration step. A call fails with
 * [PushUnavailableException][ch.stenzel.tim.polleninfo.feature.alarms.domain.model.PushUnavailableException]
 * on a platform without push.
 */
interface AlarmRepository {

    /** The device's alarms in creation order. */
    suspend fun alarms(): Result<List<Alarm>>

    /**
     * Stores [draft] as a new alarm and returns it as the backend stored it. A refusal fails with
     * [InvalidAlarmException][ch.stenzel.tim.polleninfo.feature.alarms.domain.model.InvalidAlarmException].
     */
    suspend fun create(draft: AlarmDraft): Result<Alarm>
}
