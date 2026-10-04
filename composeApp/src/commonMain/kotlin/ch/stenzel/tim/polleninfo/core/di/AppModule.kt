package ch.stenzel.tim.polleninfo.core.di

import ch.stenzel.tim.polleninfo.core.measurement.data.remote.StationMeasurementApiService
import ch.stenzel.tim.polleninfo.core.measurement.data.repository.StationMeasurementRepositoryImpl
import ch.stenzel.tim.polleninfo.core.measurement.domain.repository.StationMeasurementRepository
import ch.stenzel.tim.polleninfo.core.measurement.domain.usecase.GetStationMeasurementUseCase
import ch.stenzel.tim.polleninfo.core.network.apiBaseUrl
import ch.stenzel.tim.polleninfo.core.network.createHttpClient
import ch.stenzel.tim.polleninfo.core.preferences.DataStoreDeviceRegistrationRepository
import ch.stenzel.tim.polleninfo.core.preferences.DataStoreNotificationPermissionPreferences
import ch.stenzel.tim.polleninfo.core.preferences.DeviceRegistrationRepository
import ch.stenzel.tim.polleninfo.core.preferences.DataStoreSelectedStationRepository
import ch.stenzel.tim.polleninfo.core.preferences.NotificationPermissionPreferences
import ch.stenzel.tim.polleninfo.core.preferences.SelectedStationRepository
import ch.stenzel.tim.polleninfo.core.species.data.remote.SpeciesApiService
import ch.stenzel.tim.polleninfo.core.species.data.repository.SpeciesRepositoryImpl
import ch.stenzel.tim.polleninfo.core.species.domain.repository.SpeciesRepository
import ch.stenzel.tim.polleninfo.core.startup.StartupViewModel
import ch.stenzel.tim.polleninfo.core.station.data.remote.StationApiService
import ch.stenzel.tim.polleninfo.core.station.data.repository.StationRepositoryImpl
import ch.stenzel.tim.polleninfo.core.station.domain.repository.StationRepository
import ch.stenzel.tim.polleninfo.feature.alarms.data.remote.AlarmApiService
import ch.stenzel.tim.polleninfo.feature.alarms.data.repository.AlarmRepositoryImpl
import ch.stenzel.tim.polleninfo.feature.alarms.domain.repository.AlarmRepository
import ch.stenzel.tim.polleninfo.feature.alarms.presentation.AlarmEditorViewModel
import ch.stenzel.tim.polleninfo.feature.alarms.presentation.AlarmsViewModel
import ch.stenzel.tim.polleninfo.feature.allstations.domain.usecase.GetAllStationReadingsUseCase
import ch.stenzel.tim.polleninfo.feature.allstations.presentation.AllStationsViewModel
import ch.stenzel.tim.polleninfo.feature.example.data.remote.ExampleApiService
import ch.stenzel.tim.polleninfo.feature.example.data.repository.ExampleRepositoryImpl
import ch.stenzel.tim.polleninfo.feature.example.domain.repository.ExampleRepository
import ch.stenzel.tim.polleninfo.feature.example.domain.usecase.GetPollenSnapshotUseCase
import ch.stenzel.tim.polleninfo.feature.example.presentation.ExampleViewModel
import ch.stenzel.tim.polleninfo.feature.home.presentation.HomeViewModel
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

    single { SpeciesApiService(get(), apiBaseUrl) }
    single<SpeciesRepository> { SpeciesRepositoryImpl(get()) }

    single { StationMeasurementApiService(get(), apiBaseUrl) }
    single<StationMeasurementRepository> { StationMeasurementRepositoryImpl(get()) }

    // The DataStore itself comes from platformModule; only the thin repository over it is common.
    single<SelectedStationRepository> { DataStoreSelectedStationRepository(get()) }
    single<NotificationPermissionPreferences> { DataStoreNotificationPermissionPreferences(get()) }
    single<DeviceRegistrationRepository> { DataStoreDeviceRegistrationRepository(get()) }

    // PushTokenProvider comes from platformModule.
    single { AlarmApiService(get(), apiBaseUrl) }
    single<AlarmRepository> { AlarmRepositoryImpl(get(), get(), get()) }
}

val domainModule = module {
    factory { GetPollenSnapshotUseCase(get()) }
    factory { FindNearestStationUseCase() }
    factory { GetStationMeasurementUseCase(get()) }
    factory { GetAllStationReadingsUseCase(get()) }
}

val presentationModule = module {
    viewModel { StartupViewModel(get()) }
    viewModel { ExampleViewModel(get()) }
    // CoarseLocationProvider comes from platformModule; the ViewModel only knows the interface.
    viewModel { OnboardingViewModel(get(), get(), get(), get()) }
    viewModel { HomeViewModel(get(), get()) }
    viewModel { AllStationsViewModel(get(), get()) }
    viewModel { AlarmsViewModel(get(), get(), get(), get()) }
    viewModel { AlarmEditorViewModel(get(), get(), get(), get()) }
}

val appModules = listOf(platformModule, networkModule, dataModule, domainModule, presentationModule)
