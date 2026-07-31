package ch.stenzel.tim.polleninfo.core.di

import ch.stenzel.tim.polleninfo.core.network.apiBaseUrl
import ch.stenzel.tim.polleninfo.core.network.createHttpClient
import ch.stenzel.tim.polleninfo.feature.example.data.remote.ExampleApiService
import ch.stenzel.tim.polleninfo.feature.example.data.repository.ExampleRepositoryImpl
import ch.stenzel.tim.polleninfo.feature.example.domain.repository.ExampleRepository
import ch.stenzel.tim.polleninfo.feature.example.domain.usecase.GetPollenSnapshotUseCase
import ch.stenzel.tim.polleninfo.feature.example.presentation.ExampleViewModel
import ch.stenzel.tim.polleninfo.feature.onboarding.data.remote.StationApiService
import ch.stenzel.tim.polleninfo.feature.onboarding.data.repository.StationRepositoryImpl
import ch.stenzel.tim.polleninfo.feature.onboarding.domain.repository.StationRepository
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
}

val domainModule = module {
    factory { GetPollenSnapshotUseCase(get()) }
}

val presentationModule = module {
    viewModel { ExampleViewModel(get()) }
    viewModel { OnboardingViewModel(get()) }
}

val appModules = listOf(networkModule, dataModule, domainModule, presentationModule)
