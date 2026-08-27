package com.vttexplorer.app

import android.app.Application
import com.vttexplorer.app.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.maplibre.android.MapLibre

class VTTExplorerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // MapLibre DOIT être initialisé avant toute création de MapView
        try {
            MapLibre.getInstance(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@VTTExplorerApp)
            modules(appModule)
        }
    }
}
