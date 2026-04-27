package fyp.project.datingapp.di

import fyp.project.datingapp.database.getRoomDatabase
import fyp.project.datingapp.domain.auth.SecureKeyStorage
import fyp.project.datingapp.domain.location.AndroidLocationProvider
import fyp.project.datingapp.domain.location.LocationProvider
import fyp.project.datingapp.getDatabaseBuilder
import fyp.project.datingapp.p2p.ble.AndroidBleProximity
import fyp.project.datingapp.p2p.ble.BleProximity
import fyp.project.datingapp.p2p.discovery.AndroidLocalAddressProvider
import fyp.project.datingapp.p2p.discovery.LocalAddressProvider
import fyp.project.datingapp.p2p.feed.AndroidLanBootstrap
import fyp.project.datingapp.p2p.feed.LanBootstrap
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidModule = module {
    single { getRoomDatabase(getDatabaseBuilder(androidContext())) }
    single { SecureKeyStorage(androidContext()) }
    single<LocationProvider> { AndroidLocationProvider(androidContext()) }
    single<LocalAddressProvider> { AndroidLocalAddressProvider(androidContext()) }
    single<BleProximity> { AndroidBleProximity(androidContext()) }
    single<LanBootstrap> { AndroidLanBootstrap(androidContext(), get()) }
}
