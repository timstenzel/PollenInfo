package ch.stenzel.tim.polleninfo.core.preferences

import ch.stenzel.tim.polleninfo.core.result.Result
import kotlinx.coroutines.flow.Flow

/**
 * The user's station choice, persisted on the device.
 *
 * Lives in `core/` rather than in the onboarding slice because it already has three consumers:
 * onboarding writes it, the startup gate reads it to decide whether onboarding is needed at all,
 * and the home screen reads it to label itself.
 */
interface SelectedStationRepository {

    /** Emits `null` while no station has been chosen. */
    val selectedStation: Flow<SelectedStation?>

    /**
     * Persists [station]. Returns [Result] rather than throwing or returning `Unit`: the write can
     * fail on an IO error, and the caller has to stay on the onboarding screen when it does instead
     * of navigating on to a screen with nothing to read.
     */
    suspend fun select(station: SelectedStation): Result<Unit>
}
