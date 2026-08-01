package ch.stenzel.tim.polleninfo.core.di

import ch.stenzel.tim.polleninfo.core.network.apiBaseUrl
import ch.stenzel.tim.polleninfo.core.network.createHttpClient
import ch.stenzel.tim.polleninfo.core.preferences.DataStoreSelectedStationRepository
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStationRepository
import ch.stenzel.tim.polleninfo.core.startup.StartupViewModel
import ch.stenzel.tim.polleninfo.feature.example.data.remote.ExampleApiService
import ch.stenzel.tim.polleninfo.feature.example.data.repository.ExampleRepositoryImpl
import ch.stenzel.tim.polleninfo.feature.example.domain.repository.ExampleRepository
import ch.stenzel.tim.polleninfo.feature.example.domain.usecase.GetPollenSnapshotUseCase
import ch.stenzel.tim.polleninfo.feature.example.presentation.ExampleViewModel
import ch.stenzel.tim.polleninfo.feature.home.data.remote.StationMeasurementApiService
import ch.stenzel.tim.polleninfo.feature.home.data.repository.StationMeasurementRepositoryImpl
import ch.stenzel.tim.polleninfo.feature.home.domain.repository.StationMeasurementRepository
import ch.stenzel.tim.polleninfo.feature.home.domain.usecase.GetStationMeasurementUseCase
import ch.stenzel.tim.polleninfo.feature.home.presentation.HomeViewModel
import ch.stenzel.tim.polleninfo.feature.onboarding.data.remote.StationApiService
import ch.stenzel.tim.polleninfo.feature.onboarding.data.repository.StationRepositoryImpl
import ch.stenzel.tim.polleninfo.feature.onboarding.domain.repository.StationRepository
import ch.stenzel.tim.polleninfo.feature.onboarding.domain.usecase.FindNearestStationUseCase
import ch.stenzel.tim.polleninfo.feature.onboarding.presentation.OnboardingViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val networkModule = module {
    single { createHttpClient() }
}

val dataModule = module {
    single { ExampleApiService(get()) }
    single<ExampleRepository> { ExampleRepositoryImpl(get()) }

    // The backend address is handed to the service here rather than read inside it, so tests can
    // construct one against any host.
    single { StationApiService(get(), apiBaseUrl) }
    single<StationRepository> { StationRepositoryImpl(get()) }

    single { StationMeasurementApiService(get(), apiBaseUrl) }
    single<StationMeasurementRepository> { StationMeasurementRepositoryImpl(get()) }

    // The DataStore itself comes from platformModule; only the thin repository over it is common.
    single<SelectedStationRepository> { DataStoreSelectedStationRepository(get()) }
}

val domainModule = module {
    factory { GetPollenSnapshotUseCase(get()) }
    factory { FindNearestStationUseCase() }
    factory { GetStationMeasurementUseCase(get()) }
}

val presentationModule = module {
    viewModel { StartupViewModel(get()) }
    viewModel { ExampleViewModel(get()) }
    // CoarseLocationProvider comes from platformModule; the ViewModel only knows the interface.
    viewModel { OnboardingViewModel(get(), get(), get(), get()) }
    viewModel { HomeViewModel(get(), get()) }
}

val appModules = listOf(platformModule, networkModule, dataModule, domainModule, presentationModule)
