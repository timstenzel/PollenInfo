package ch.stenzel.tim.polleninfo.feature.home.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStationRepository
import org.koin.compose.koinInject

/**
 * **Placeholder — a stand-in for the real dashboard**, which is a future feature.
 *
 * It exists so onboarding has somewhere to lead, and it shows only the name of the station the user
 * selected. There is no ViewModel and no network call because there is nothing yet to hold state
 * for or to fetch; pollen readings, refresh and any dashboard content replace this wholesale.
 *
 * There is also deliberately no way to change the station from here — that arrives with the planned
 * settings feature. Until then, clearing the app's data is the only route back to onboarding.
 */
@Composable
fun HomeScreen(selectedStationRepository: SelectedStationRepository = koinInject()) {
    val selectedStation by selectedStationRepository.selectedStation
        .collectAsStateWithLifecycle(initialValue = null)

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                text = "Selected station: ${selectedStation?.name.orEmpty()}",
                style = MaterialTheme.typography.titleMedium,
            )
        }
    }
}
