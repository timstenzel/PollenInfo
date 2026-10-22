package ch.stenzel.tim.polleninfo.core.ui.severity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.stenzel.tim.polleninfo.core.measurement.domain.model.SpeciesReading
import ch.stenzel.tim.polleninfo.core.ui.layout.MeadowCard
import ch.stenzel.tim.polleninfo.core.ui.species.speciesIcon
import ch.stenzel.tim.polleninfo.core.ui.species.speciesName
import ch.stenzel.tim.polleninfo.resources.Res
import ch.stenzel.tim.polleninfo.resources.reading_all_species
import ch.stenzel.tim.polleninfo.resources.reading_concentration_unit
import ch.stenzel.tim.polleninfo.resources.reading_no_data
import ch.stenzel.tim.polleninfo.resources.reading_not_reported
import org.jetbrains.compose.resources.stringResource

/**
 * The unit is stated once here, so the numbers on the tiles can stay bare and scannable. [titleStyle]
 * lets Home set the heading in its serif.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SpeciesListHeading(unit: String, titleStyle: TextStyle = MaterialTheme.typography.titleMedium) {
    // The unit sits at the end of the heading's line; where both don't fit (a large font, a long
    // language) it wraps below rather than squeezing the heading.
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        itemVerticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = stringResource(Res.string.reading_all_species),
            style = titleStyle,
            modifier = Modifier.padding(end = 12.dp),
        )
        Text(
            text = stringResource(Res.string.reading_concentration_unit, unit),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/**
 * [species] as [SpeciesTile]s two to a row, in the order given, the pair in a row as tall as the
 * taller of the two. Home's grid and an expanded station on All stations.
 */
@Composable
fun SpeciesTileGrid(species: List<SpeciesReading>, unit: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(SPECIES_TILE_GAP)) {
        species.chunked(2).forEach { pair ->
            Row(Modifier.height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(SPECIES_TILE_GAP)) {
                pair.forEach { reading ->
                    SpeciesTile(reading, unit, Modifier.weight(1f).fillMaxHeight())
                }
                // An odd last tile keeps the width of a column.
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** The gap between the tiles of a [SpeciesTileGrid], across and down; skeletons use it too. */
val SPECIES_TILE_GAP = 13.dp

/**
 * One species as a card tile: the species' icon on a pale tile, the concentration with [unit] at the
 * top end, the name, the severity word and a compact bar.
 *
 * A taxon the station does not report says "No data" and "not reported" under a dash, with an
 * empty bar, never "None" with a 0 — those are a measurement of clean air, and a user who reacts to
 * this taxon must not mistake one for the other.
 *
 * Announced as one item in the order the list rows were — name, severity word, concentration — so
 * a screen reader hears the same as before the tile layout put the number first.
 */
@Composable
fun SpeciesTile(reading: SpeciesReading, unit: String, modifier: Modifier = Modifier) {
    val name = speciesName(reading.id, reading.name)
    val level = reading.severity?.label() ?: stringResource(Res.string.reading_no_data)
    val concentration = reading.concentration?.toString() ?: NO_VALUE
    MeadowCard(
        shape = MaterialTheme.shapes.medium,
        modifier = modifier.clearAndSetSemantics {
            contentDescription = listOfNotNull(name, level, reading.concentration?.toString()).joinToString(", ")
        },
    ) {
        Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = speciesIcon(reading.id),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp),
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Text(
                        text = concentration,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.End,
                    )
                    // The unit belongs to a number; under a dash, why there is none.
                    Text(
                        text = if (reading.concentration != null) unit else stringResource(Res.string.reading_not_reported),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(text = name, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(text = level, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(14.dp))
            SeverityBar(reading.severity, SeverityBarSize.Compact, Modifier.fillMaxWidth())
        }
    }
}

/** What stands in for the number of a taxon without a reading. */
private const val NO_VALUE = "–"
