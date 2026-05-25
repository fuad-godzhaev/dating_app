package com.aura.di

import com.aura.database.getRoomDatabase
import com.aura.domain.auth.SecureKeyStorage
import com.aura.domain.location.AndroidLocationProvider
import com.aura.domain.location.LocationProvider
import com.aura.getDatabaseBuilder
import com.aura.p2p.ble.AndroidBleProximity
import com.aura.p2p.ble.BleProximity
import com.aura.p2p.background.AndroidBackgroundService
import com.aura.p2p.background.BackgroundService
import com.aura.p2p.background.NotificationHelper
import com.aura.p2p.blob.AndroidBlobStore
import com.aura.p2p.blob.AndroidImageTranscoder
import com.aura.p2p.blob.BlobStore
import com.aura.p2p.blob.ImageTranscoder
import com.aura.p2p.messaging.JcaKeyAgreement
import com.aura.p2p.messaging.KeyAgreement
import com.aura.p2p.discovery.AndroidFeedFilterStore
import com.aura.p2p.discovery.AndroidLocalAddressProvider
import com.aura.p2p.discovery.FeedFilterStore
import com.aura.p2p.discovery.LocalAddressProvider
import com.aura.p2p.feed.AndroidLanBootstrap
import com.aura.p2p.feed.LanBootstrap
import com.aura.p2p.transport.AndroidReachabilityProbe
import com.aura.p2p.transport.ReachabilityProbe
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single { getRoomDatabase(getDatabaseBuilder(androidContext())) }
    single { SecureKeyStorage(androidContext()) }
    single<LocationProvider> { AndroidLocationProvider(androidContext()) }
    single<LocalAddressProvider> { AndroidLocalAddressProvider(androidContext()) }
    single<BleProximity> { AndroidBleProximity(androidContext()) }
    single<LanBootstrap> { AndroidLanBootstrap(androidContext(), get()) }
    single<BlobStore> { AndroidBlobStore(androidContext()) }
    single<ImageTranscoder> { AndroidImageTranscoder() }
    single<KeyAgreement> { JcaKeyAgreement(get()) }
    single { NotificationHelper(androidContext()) }
    single<BackgroundService> { AndroidBackgroundService(androidContext()) }
    // Idea C: reachability signal for gating the opt-in serving role (AutoNAT-backed after .aar rebuild).
    single<ReachabilityProbe> { AndroidReachabilityProbe(get()) }
    single<FeedFilterStore> { AndroidFeedFilterStore(androidContext()) }
}
