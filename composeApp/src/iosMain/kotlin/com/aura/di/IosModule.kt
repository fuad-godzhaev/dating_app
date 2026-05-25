package com.aura.di

import com.aura.database.getRoomDatabase
import com.aura.domain.auth.SecureKeyStorage
import com.aura.domain.location.IosLocationProvider
import com.aura.domain.location.LocationProvider
import com.aura.getDatabaseBuilder
import com.aura.p2p.ble.BleProximity
import com.aura.p2p.ble.IosBleProximity
import com.aura.p2p.background.BackgroundService
import com.aura.p2p.background.NoopBackgroundService
import com.aura.p2p.blob.BlobStore
import com.aura.p2p.blob.ImageTranscoder
import com.aura.p2p.blob.IosBlobStore
import com.aura.p2p.blob.IosImageTranscoder
import com.aura.p2p.messaging.IosKeyAgreement
import com.aura.p2p.messaging.KeyAgreement
import com.aura.p2p.discovery.FeedFilterStore
import com.aura.p2p.discovery.IosFeedFilterStore
import com.aura.p2p.discovery.IosLocalAddressProvider
import com.aura.p2p.discovery.LocalAddressProvider
import com.aura.p2p.feed.IosLanBootstrap
import com.aura.p2p.feed.LanBootstrap
import com.aura.p2p.transport.IosReachabilityProbe
import com.aura.p2p.transport.ReachabilityProbe
import org.koin.dsl.module

val iosModule = module {
    single { getRoomDatabase(getDatabaseBuilder()) }
    single { SecureKeyStorage() }
    single<LocationProvider> { IosLocationProvider() }
    single<LocalAddressProvider> { IosLocalAddressProvider() }
    single<BleProximity> { IosBleProximity() }
    single<LanBootstrap> { IosLanBootstrap() }
    single<BlobStore> { IosBlobStore() }
    single<ImageTranscoder> { IosImageTranscoder() }
    single<KeyAgreement> { IosKeyAgreement() }
    single<BackgroundService> { NoopBackgroundService() }
    single<ReachabilityProbe> { IosReachabilityProbe() }
    single<FeedFilterStore> { IosFeedFilterStore() }
}
