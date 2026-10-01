package ch.stenzel.tim.polleninfo.feature.home.presentation

import ch.stenzel.tim.polleninfo.feature.home.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.feature.home.domain.model.SpeciesReading

/**
 * What the home screen shows.
 *
 * [stationName] is declared on the interface, so **every** variant carries it. The name comes from
 * the stored selection rather than from the reading, which means the screen can identify itself
 * before any reading has arrived and after one has failed — a screen labelled with one station's
 * name beside another's data, or beside none at all, is the failure this prevents.
 */
sealed interface HomeUiState {

    val stationName: String

    data class Loading(override val stationName: String) : HomeUiState

    /**
     * [species] arrives already ordered by `GetStationMeasurementUseCase`; the screen renders it as
     * is. [drivenBy] names the taxon behind [overallSeverity] and is `null` only when nothing was
     * measured at all.
     */
    data class Content(
        override val stationName: String,
        val overallSeverity: PollenSeverity,
        val drivenBy: String?,
        val unit: String,
        val species: List<SpeciesReading>,
    ) : HomeUiState

    data class Error(
        override val stationName: String,
        val message: String,
    ) : HomeUiState
}
