package ch.stenzel.tim.polleninfo.feature.example.domain.usecase

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.feature.example.domain.model.PollenSnapshot
import ch.stenzel.tim.polleninfo.feature.example.domain.repository.ExampleRepository

class GetPollenSnapshotUseCase(
    private val repository: ExampleRepository,
) {
    suspend operator fun invoke(
        latitude: Double = 47.3769,
        longitude: Double = 8.5417,
    ): Result<PollenSnapshot> = repository.getPollenSnapshot(latitude, longitude)
}
