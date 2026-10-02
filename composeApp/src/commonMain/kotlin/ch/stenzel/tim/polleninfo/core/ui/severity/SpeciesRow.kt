package ch.stenzel.tim.polleninfo.core.ui.severity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.SpeciesReading

/** The unit is stated once here, so the numbers on the rows can stay bare and scannable. */
@Composable
fun SpeciesListHeading(unit: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(
            text = "All species",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "concentration in $unit",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * Two lines: the name, then the bar, the severity word and the concentration.
 *
 * A taxon the station does not report says "No data" with a dash, never "None" with a 0 — those
 * are a measurement of clean air, and a user who reacts to this taxon must not mistake one for the
 * other.
 */
@Composable
fun SpeciesRow(reading: SpeciesReading) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = reading.name, style = MaterialTheme.typography.bodyLarge)
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            SeverityBar(reading.severity, SeverityBarSize.Compact, Modifier.weight(1f))
            Spacer(Modifier.width(16.dp))
            // Fixed widths, so the bars all end at the same x and the words and numbers each form
            // a column down the list.
            Text(
                text = reading.severity?.label() ?: "No data",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.width(SEVERITY_WORD_WIDTH),
            )
            Text(
                text = reading.concentration?.toString() ?: "–",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End,
                modifier = Modifier.width(CONCENTRATION_WIDTH),
            )
        }
    }
}

private val SEVERITY_WORD_WIDTH = 88.dp
private val CONCENTRATION_WIDTH = 56.dp
