package ch.stenzel.tim.polleninfo.feature.example.domain.repository

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.feature.example.domain.model.PollenSnapshot

/**
 * Reference feature, not a product feature — see `feature/example` in CLAUDE.md.
 *
 * This slice exists to demonstrate the layering (DTO → mapper → domain → repository → use case →
 * ViewModel → UI) with tests at each level. Mirror its structure when adding a real feature; don't
 * extend it.
 */
interface ExampleRepository {
    suspend fun getPollenSnapshot(latitude: Double, longitude: Double): Result<PollenSnapshot>
}
