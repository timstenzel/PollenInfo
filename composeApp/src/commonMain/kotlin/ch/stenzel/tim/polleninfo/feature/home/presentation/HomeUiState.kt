package ch.stenzel.tim.polleninfo.feature.home.presentation

import ch.stenzel.tim.polleninfo.feature.home.domain.model.PollenSeverity
import ch.stenzel.tim.polleninfo.feature.home.domain.model.SpeciesReading
import kotlinx.datetime.Instant

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
     *
     * [measuredAt] is carried as the raw instant rather than as a `ReadingAge`: whether a reading
     * is fresh depends on when the screen is looked at, not on when the state was built, so the
     * screen classifies it against the current time when it renders.
     *
     * [isRefreshing] is set while a pull-to-refresh runs, so the readings stay on screen until the
     * new ones replace them.
     */
    data class Content(
        override val stationName: String,
        val measuredAt: Instant,
        val overallSeverity: PollenSeverity,
        val drivenBy: String?,
        val unit: String,
        val species: List<SpeciesReading>,
        val isRefreshing: Boolean = false,
    ) : HomeUiState

    data class Error(
        override val stationName: String,
        val message: String,
    ) : HomeUiState
}
