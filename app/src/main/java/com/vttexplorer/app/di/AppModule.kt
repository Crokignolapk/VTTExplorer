package com.vttexplorer.app.di

import android.content.Context
import androidx.room.Room
import com.vttexplorer.app.data.database.AppDatabase
import com.vttexplorer.app.data.location.LocationRepositoryImpl
import com.vttexplorer.app.data.maps.MapProvider
import com.vttexplorer.app.data.maps.MapLibreProvider
import com.vttexplorer.app.data.repository.RideRepositoryImpl
import com.vttexplorer.app.data.routing.GraphHopperRoutingEngine
import com.vttexplorer.app.domain.repository.RoutingEngine
import com.vttexplorer.app.domain.repository.LocationRepository
import com.vttexplorer.app.domain.repository.RideRepository
import com.vttexplorer.app.domain.usecase.GenerateLoopUseCase
import com.vttexplorer.app.domain.usecase.GetRouteUseCase
import com.vttexplorer.app.presentation.map.MapViewModel
import com.vttexplorer.app.presentation.navigation.NavigationViewModel
import com.vttexplorer.app.presentation.route_generator.RouteGeneratorViewModel
import com.vttexplorer.app.presentation.history.HistoryViewModel
import com.vttexplorer.app.presentation.settings.SettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
    // Database
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "vtt_explorer.db"
        ).fallbackToDestructiveMigration().build()
    }
    single { get<AppDatabase>().rideDao() }

    // Repositories
    single<LocationRepository> { LocationRepositoryImpl(androidContext()) }
    single<RideRepository> { RideRepositoryImpl(get()) }
    single<MapProvider> { MapLibreProvider() }
    single<RoutingEngine> { GraphHopperRoutingEngine() }

    // Use cases
    factory { GenerateLoopUseCase(get(), get()) }
    factory { GetRouteUseCase(get()) }

    // ViewModels
    viewModel { MapViewModel(get(), get(), get()) }
    viewModel { RouteGeneratorViewModel(get(), get()) }
    viewModel { NavigationViewModel(get(), get()) }
    viewModel { HistoryViewModel(get()) }
    viewModel { SettingsViewModel(androidContext()) }
}
