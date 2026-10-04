package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import ch.stenzel.tim.polleninfo.core.species.domain.model.Species
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmFormState

/** What the alarm editor shows. */
sealed interface AlarmEditorUiState {

    /** The stations and pollen types the fields offer are loading. */
    data object Loading : AlarmEditorUiState

    /**
     * The form. [stations] and [species] are what the station dropdown and the pollen-type chips
     * offer; [form] holds what is chosen. While [isSaving] the Save button shows progress and
     * ignores taps. [saveError] is the last failed save's message, kept until the next attempt, with
     * every field left as it was.
     */
    data class Editing(
        val form: AlarmFormState,
        val stations: List<Station>,
        val species: List<Species>,
        val isSaving: Boolean = false,
        val saveError: String? = null,
    ) : AlarmEditorUiState {

        /** Save is offered only for a complete alarm, and not twice. */
        val canSave: Boolean get() = form.isValid && !isSaving
    }

    /** The stations or pollen types could not be loaded, so there is nothing to choose from. */
    data class Error(val message: String) : AlarmEditorUiState
}
