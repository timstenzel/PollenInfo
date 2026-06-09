package ch.stenzel.tim.polleninfo.feature.pollenforecast.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.stenzel.tim.polleninfo.feature.pollenforecast.domain.model.PollenForecast
import ch.stenzel.tim.polleninfo.feature.pollenforecast.domain.model.PollenLevel
import ch.stenzel.tim.polleninfo.feature.pollenforecast.domain.model.PollenReading
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun PollenForecastScreen(viewModel: PollenForecastViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PollenForecastContent(
        uiState = uiState,
        onRefresh = viewModel::loadForecast,
        onRetry = viewModel::retry,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PollenForecastContent(
    uiState: PollenForecastUiState,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("PollenInfo") })
        },
    ) { innerPadding ->
        when (uiState) {
            is PollenForecastUiState.Loading -> LoadingView(Modifier.padding(innerPadding))

            is PollenForecastUiState.Error -> ErrorView(
                message = uiState.message,
                onRetry = onRetry,
                modifier = Modifier.padding(innerPadding),
            )

            is PollenForecastUiState.Content -> PullToRefreshBox(
                isRefreshing = uiState.isRefreshing,
                onRefresh = onRefresh,
                modifier = Modifier.padding(innerPadding),
            ) {
                ForecastView(forecast = uiState.forecast)
            }
        }
    }
}

@Composable
private fun LoadingView(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorView(message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "Something went wrong", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(text = message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Retry") }
    }
}

@Composable
private fun ForecastView(forecast: PollenForecast) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Current pollen levels",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = forecast.location,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
        }

        items(forecast.currentReadings) { reading ->
            PollenReadingCard(reading)
        }
    }
}

@Composable
private fun PollenReadingCard(reading: PollenReading) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(reading.type.displayName, style = MaterialTheme.typography.titleSmall)
                Text(
                    "${reading.valueGrainsPerM3} grains/m³",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(16.dp))
            PollenLevelBadge(reading.level)
        }
    }
}

@Composable
private fun PollenLevelBadge(level: PollenLevel) {
    val (color, label) = when (level) {
        PollenLevel.NONE -> Color(0xFF9E9E9E) to "None"
        PollenLevel.LOW -> Color(0xFF4CAF50) to "Low"
        PollenLevel.MODERATE -> Color(0xFFFFEB3B) to "Moderate"
        PollenLevel.HIGH -> Color(0xFFFF9800) to "High"
        PollenLevel.VERY_HIGH -> Color(0xFFF44336) to "Very High"
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = Modifier.size(width = 90.dp, height = 32.dp),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.White)
        }
    }
}
