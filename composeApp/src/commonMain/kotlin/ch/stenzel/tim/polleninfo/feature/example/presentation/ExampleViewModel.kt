package ch.stenzel.tim.polleninfo.feature.example.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.feature.example.domain.usecase.GetPollenSnapshotUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ExampleViewModel(
    private val getPollenSnapshot: GetPollenSnapshotUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ExampleUiState>(ExampleUiState.Loading)
    val uiState: StateFlow<ExampleUiState> = _uiState.asStateFlow()

    init {
        loadSnapshot()
    }

    fun loadSnapshot(latitude: Double = 47.3769, longitude: Double = 8.5417) {
        val isRefresh = _uiState.value is ExampleUiState.Content
        if (!isRefresh) _uiState.value = ExampleUiState.Loading
        else _uiState.update { (it as ExampleUiState.Content).copy(isRefreshing = true) }

        viewModelScope.launch {
            when (val result = getPollenSnapshot(latitude, longitude)) {
                is Result.Success -> _uiState.value = ExampleUiState.Content(result.data)
                is Result.Failure -> _uiState.value = ExampleUiState.Error(result.exception)
            }
        }
    }

    fun retry() = loadSnapshot()
}
