package ch.stenzel.tim.polleninfo.feature.alarms.presentation

import ch.stenzel.tim.polleninfo.core.species.domain.model.Species
import ch.stenzel.tim.polleninfo.core.station.domain.model.Station
import ch.stenzel.tim.polleninfo.feature.alarms.domain.model.AlarmFormState

/** What the alarm editor shows. */
sealed interface AlarmEditorUiState {

    /** The stations and pollen types the fields offer — and, when editing, the alarm — are loading. */
    data object Loading : AlarmEditorUiState

    /**
     * The form. [stations] and [species] are what the station dropdown and the pollen-type chips
     * offer; [form] holds what is chosen. While [isSaving] the Save button shows progress and
     * ignores taps, and [isDeleting] does the same for Delete. [saveError] is the last failed save's
     * or delete's message, kept until the next attempt, with every field left as it was.
     *
     * [canDelete] is true for an existing alarm. [showDiscardDialog] asks "Discard changes?" after
     * back with unsaved changes; [showDeleteDialog] asks to confirm a delete.
     */
    data class Editing(
        val form: AlarmFormState,
        val stations: List<Station>,
        val species: List<Species>,
        val canDelete: Boolean = false,
        val isSaving: Boolean = false,
        val isDeleting: Boolean = false,
        val saveError: String? = null,
        val showDiscardDialog: Boolean = false,
        val showDeleteDialog: Boolean = false,
    ) : AlarmEditorUiState {

        /** A save or a delete is running; the form takes no changes meanwhile. */
        val isBusy: Boolean get() = isSaving || isDeleting

        /** Save is offered only for a complete alarm, and not twice. */
        val canSave: Boolean get() = form.isValid && !isBusy
    }

    /**
     * The stations or pollen types could not be loaded — or, when editing, the alarm — so there is
     * nothing to choose from.
     */
    data class Error(val message: String) : AlarmEditorUiState
}
