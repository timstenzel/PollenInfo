package ch.stenzel.tim.polleninfo.feature.example.data.repository

import ch.stenzel.tim.polleninfo.core.result.Result
import ch.stenzel.tim.polleninfo.core.result.safeCall
import ch.stenzel.tim.polleninfo.feature.example.data.mapper.toDomain
import ch.stenzel.tim.polleninfo.feature.example.data.remote.ExampleApiService
import ch.stenzel.tim.polleninfo.feature.example.domain.model.PollenSnapshot
import ch.stenzel.tim.polleninfo.feature.example.domain.repository.ExampleRepository

class ExampleRepositoryImpl(
    private val apiService: ExampleApiService,
) : ExampleRepository {

    override suspend fun getPollenSnapshot(
        latitude: Double,
        longitude: Double,
    ): Result<PollenSnapshot> = safeCall {
        apiService.getSnapshot(latitude, longitude).toDomain()
    }
}
