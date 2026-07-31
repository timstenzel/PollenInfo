package ch.stenzel.tim.polleninfo.core.di

import ch.stenzel.tim.polleninfo.core.network.createHttpClient
import ch.stenzel.tim.polleninfo.feature.example.data.remote.ExampleApiService
import ch.stenzel.tim.polleninfo.feature.example.data.repository.ExampleRepositoryImpl
import ch.stenzel.tim.polleninfo.feature.example.domain.repository.ExampleRepository
import ch.stenzel.tim.polleninfo.feature.example.domain.usecase.GetPollenSnapshotUseCase
import ch.stenzel.tim.polleninfo.feature.example.presentation.ExampleViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val networkModule = module {
    single { createHttpClient() }
}

val dataModule = module {
    single { ExampleApiService(get()) }
    single<ExampleRepository> { ExampleRepositoryImpl(get()) }
}

val domainModule = module {
    factory { GetPollenSnapshotUseCase(get()) }
}

val presentationModule = module {
    viewModel { ExampleViewModel(get()) }
}

val appModules = listOf(networkModule, dataModule, domainModule, presentationModule)
