package ch.stenzel.tim.polleninfo.core.di

import ch.stenzel.tim.polleninfo.core.network.createHttpClient
import ch.stenzel.tim.polleninfo.feature.pollenforecast.data.remote.PollenApiService
import ch.stenzel.tim.polleninfo.feature.pollenforecast.data.repository.PollenForecastRepositoryImpl
import ch.stenzel.tim.polleninfo.feature.pollenforecast.domain.repository.PollenForecastRepository
import ch.stenzel.tim.polleninfo.feature.pollenforecast.domain.usecase.GetPollenForecastUseCase
import ch.stenzel.tim.polleninfo.feature.pollenforecast.presentation.PollenForecastViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val networkModule = module {
    single { createHttpClient() }
}

val dataModule = module {
    single { PollenApiService(get()) }
    single<PollenForecastRepository> { PollenForecastRepositoryImpl(get()) }
}

val domainModule = module {
    factory { GetPollenForecastUseCase(get()) }
}

val presentationModule = module {
    viewModel { PollenForecastViewModel(get()) }
}

val appModules = listOf(networkModule, dataModule, domainModule, presentationModule)
