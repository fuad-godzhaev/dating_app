package fyp.project.datingapp.di

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import fyp.project.datingapp.database.AppDatabase
import fyp.project.datingapp.database.RepositoryManager
import fyp.project.datingapp.domain.auth.AuthRepository
import fyp.project.datingapp.domain.auth.DefaultAuthRepository
import fyp.project.datingapp.domain.auth.SecureKeyStorage
import fyp.project.datingapp.domain.auth.SignatureVerifier
import fyp.project.datingapp.domain.auth.defaultSignatureVerifier
import fyp.project.datingapp.p2p.discovery.DhtGossipDiscoveryService
import fyp.project.datingapp.p2p.discovery.DiscoveryPreferencesStore
import fyp.project.datingapp.p2p.discovery.DiscoveryService
import fyp.project.datingapp.p2p.discovery.GeohashLocator
import fyp.project.datingapp.p2p.discovery.PresenceAnnouncer
import fyp.project.datingapp.p2p.transport.Libp2pConfig
import fyp.project.datingapp.p2p.transport.Libp2pTransport
import org.koin.dsl.module

val appModule = module {
    single<StoreFactory> { DefaultStoreFactory() }
    single<AuthRepository> { DefaultAuthRepository(get<AppDatabase>().authSettingsDao(), get()) }
    single { RepositoryManager(get(), get()) }

    // ---- Phase C: discovery (transport runtime + presence over DHT/GossipSub) ----
    single<SignatureVerifier> { defaultSignatureVerifier() }
    // One shared libp2p host for the whole app: built from the encrypted-at-rest
    // Ed25519 transport seed. Lazy — only created when discovery first needs it.
    single {
        Libp2pTransport(Libp2pConfig(identityKey = get<SecureKeyStorage>().getOrCreateTransportSeed()))
    }
    single { DiscoveryPreferencesStore() }
    single { GeohashLocator(get()) }
    single { PresenceAnnouncer(get(), get(), get(), get(), get(), get()) }
    single<DiscoveryService> { DhtGossipDiscoveryService(get(), get(), get(), get(), get()) }
}
