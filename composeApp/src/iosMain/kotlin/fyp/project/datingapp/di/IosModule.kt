package fyp.project.datingapp.di

import fyp.project.datingapp.database.getRoomDatabase
import fyp.project.datingapp.domain.auth.SecureKeyStorage
import fyp.project.datingapp.domain.location.IosLocationProvider
import fyp.project.datingapp.domain.location.LocationProvider
import fyp.project.datingapp.getDatabaseBuilder
import fyp.project.datingapp.p2p.ble.BleProximity
import fyp.project.datingapp.p2p.ble.IosBleProximity
import fyp.project.datingapp.p2p.background.BackgroundService
import fyp.project.datingapp.p2p.background.NoopBackgroundService
import fyp.project.datingapp.p2p.blob.BlobStore
import fyp.project.datingapp.p2p.blob.IosBlobStore
import fyp.project.datingapp.p2p.messaging.IosKeyAgreement
import fyp.project.datingapp.p2p.messaging.KeyAgreement
import fyp.project.datingapp.p2p.discovery.IosLocalAddressProvider
import fyp.project.datingapp.p2p.discovery.LocalAddressProvider
import fyp.project.datingapp.p2p.feed.IosLanBootstrap
import fyp.project.datingapp.p2p.feed.LanBootstrap
import org.koin.dsl.module

val iosModule = module {
    single { getRoomDatabase(getDatabaseBuilder()) }
    single { SecureKeyStorage() }
    single<LocationProvider> { IosLocationProvider() }
    single<LocalAddressProvider> { IosLocalAddressProvider() }
    single<BleProximity> { IosBleProximity() }
    single<LanBootstrap> { IosLanBootstrap() }
    single<BlobStore> { IosBlobStore() }
    single<KeyAgreement> { IosKeyAgreement() }
    single<BackgroundService> { NoopBackgroundService() }
}
