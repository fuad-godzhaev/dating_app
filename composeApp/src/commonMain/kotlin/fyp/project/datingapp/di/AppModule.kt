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
import fyp.project.datingapp.p2p.discovery.PeerDirectory
import fyp.project.datingapp.p2p.discovery.PresenceAnnouncer
import fyp.project.datingapp.p2p.fetch.CascadingProfileFetcher
import fyp.project.datingapp.p2p.fetch.Libp2pProfileStreamClient
import fyp.project.datingapp.p2p.fetch.ProfileFetcher
import fyp.project.datingapp.p2p.fetch.ProfileStreamClient
import fyp.project.datingapp.p2p.fetch.StreamProfileFetcher
import fyp.project.datingapp.p2p.feed.PeerProfileFeed
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
    single { PeerDirectory() }
    single { PresenceAnnouncer(get(), get(), get(), get(), get(), get()) }
    single<DiscoveryService> { DhtGossipDiscoveryService(get(), get(), get(), get(), get(), get()) }

    // ---- Phase D: fetch (turn a discovered DID + CID into a verified profile) ----
    single<ProfileStreamClient> { Libp2pProfileStreamClient(get()) }
    single<ProfileFetcher> {
        val auth = get<AuthRepository>()
        val repo = get<RepositoryManager>()
        CascadingProfileFetcher(
            selfDid = { auth.getDid() },
            ownEnvelope = { repo.getMyProfileEnvelope() },
            cache = get<AppDatabase>().discoveryDao(),
            peerDirectory = get(),
            streamClient = get(),
            verifier = get(),
        )
    }
    // Owner-side serving handler; register() it on the transport after start.
    single {
        val auth = get<AuthRepository>()
        val repo = get<RepositoryManager>()
        StreamProfileFetcher(selfDid = { auth.getDid() }, ownEnvelope = { repo.getMyProfileEnvelope() })
    }

    // ---- Phase G.2: real peer feed (discovery + fetch behind one facade) ----
    single { PeerProfileFeed(get(), get(), get(), get(), get()) }
}
