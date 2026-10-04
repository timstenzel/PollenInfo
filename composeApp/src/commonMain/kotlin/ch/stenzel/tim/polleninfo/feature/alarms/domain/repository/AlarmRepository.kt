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

    /**
     * One of the device's alarms. One that does not exist (any more) fails with
     * [AlarmNotFoundException][ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmNotFoundException].
     */
    suspend fun alarm(id: String): Result<Alarm>

    /**
     * Replaces the settings of alarm [id] with [draft] and returns it as the backend stored it. Fails
     * like [create], and like [alarm] for an alarm that no longer exists.
     */
    suspend fun update(id: String, draft: AlarmDraft): Result<Alarm>

    /** Deletes alarm [id]. Fails like [alarm] for an alarm that no longer exists. */
    suspend fun delete(id: String): Result<Unit>
}
