package ch.stenzel.tim.polleninfo.feature.pollenforecast.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.stenzel.tim.polleninfo.core.result.AppResult
import ch.stenzel.tim.polleninfo.feature.pollenforecast.domain.usecase.GetPollenForecastUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PollenForecastViewModel(
    private val getPollenForecast: GetPollenForecastUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<PollenForecastUiState>(PollenForecastUiState.Loading)
    val uiState: StateFlow<PollenForecastUiState> = _uiState.asStateFlow()

    init {
        loadForecast()
    }

    fun loadForecast(latitude: Double = 47.3769, longitude: Double = 8.5417) {
        val isRefresh = _uiState.value is PollenForecastUiState.Content
        if (!isRefresh) _uiState.value = PollenForecastUiState.Loading
        else _uiState.update { (it as PollenForecastUiState.Content).copy(isRefreshing = true) }

        viewModelScope.launch {
            when (val result = getPollenForecast(latitude, longitude)) {
                is AppResult.Success -> _uiState.value = PollenForecastUiState.Content(result.data)
                is AppResult.Failure -> _uiState.value = PollenForecastUiState.Error(result.exception)
            }
        }
    }

    fun retry() = loadForecast()
}
