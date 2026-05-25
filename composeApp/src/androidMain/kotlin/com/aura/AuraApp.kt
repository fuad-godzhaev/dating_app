package com.aura

import android.app.Application
import com.aura.di.androidModule
import com.aura.di.appModule
import com.aura.domain.auth.AuthRepository
import com.aura.domain.auth.KeyMigration
import com.aura.p2p.background.BackgroundService
import com.aura.p2p.transport.AndroidTransportEnv
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin

/**
 * Application entry point (Phase B). Koin is started here rather than in [MainActivity] so
 * background components (the WorkManager serve-window worker, the Stay-online foreground
 * service) have a DI graph even when the process starts without an Activity.
 */
class AuraApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidTransportEnv.appContext = applicationContext

        startKoin {
            androidContext(this@AuraApp)
            modules(androidModule, appModule)
        }
        val koin = GlobalContext.get()

        // P-256 re-split rollout: drop any legacy non-P-256 identity before the graph reads it.
        runBlocking { KeyMigration(koin.get<AuthRepository>()).runIfNeeded() }

        // Schedule the cooperative background serve windows.
        runCatching { koin.get<BackgroundService>().scheduleBackgroundSync() }
    }
}
