package com.apiguave.tinderclonecompose

import android.app.Application
import android.util.Log
import com.apiguave.pds.PdsService
import com.apiguave.tinderclonecompose.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class MainApp: Application() {

    private lateinit var pdsService: PdsService

    override fun onCreate() {
        super.onCreate()
        startKoin{
            androidContext(this@MainApp)
            modules(appModule)
        }

        // Initialize and start PDS server
        initializePds()
    }

    private fun initializePds() {
        pdsService = PdsService(this)

        Log.i("MainApp", "Starting PDS server...")
        pdsService.start(
            onStarted = {
                Log.i("MainApp", "PDS server started at: ${pdsService.getServerUrl()}")
            },
            onError = { error ->
                Log.e("MainApp", "Failed to start PDS server: $error")
            }
        )
    }

    override fun onTerminate() {
        super.onTerminate()
        if (::pdsService.isInitialized) {
            pdsService.stop()
        }
    }
}