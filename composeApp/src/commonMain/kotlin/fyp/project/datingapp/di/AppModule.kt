package fyp.project.datingapp.di

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import fyp.project.datingapp.database.AppDatabase
import fyp.project.datingapp.database.RepositoryManager
import fyp.project.datingapp.database.appView.dao.MessageDao
import fyp.project.datingapp.domain.auth.AuthRepository
import fyp.project.datingapp.domain.auth.DefaultAuthRepository
import fyp.project.datingapp.domain.auth.SecureKeyStorage
import fyp.project.datingapp.domain.auth.SignatureVerifier
import fyp.project.datingapp.domain.auth.defaultSignatureVerifier
import fyp.project.datingapp.p2p.blob.BlobFetcher
import fyp.project.datingapp.p2p.blob.BlobStreamClient
import fyp.project.datingapp.p2p.blob.CascadingBlobFetcher
import fyp.project.datingapp.p2p.blob.Libp2pBlobStreamClient
import fyp.project.datingapp.p2p.blob.StreamBlobFetcher
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
import fyp.project.datingapp.p2p.fetch.TransportHolderLocator
import fyp.project.datingapp.p2p.blob.PhotoUploader
import fyp.project.datingapp.p2p.feed.PeerProfileFeed
import fyp.project.datingapp.p2p.like.Libp2pLikeStreamClient
import fyp.project.datingapp.p2p.like.LikeService
import fyp.project.datingapp.p2p.like.LikeStreamClient
import fyp.project.datingapp.p2p.like.LikeStreamServer
import fyp.project.datingapp.p2p.messaging.EciesMessageCrypto
import fyp.project.datingapp.p2p.messaging.Libp2pMailboxStreamClient
import fyp.project.datingapp.p2p.messaging.Libp2pMessageStreamClient
import fyp.project.datingapp.p2p.messaging.MailboxHolder
import fyp.project.datingapp.p2p.messaging.MailboxHolderLocator
import fyp.project.datingapp.p2p.messaging.MailboxService
import fyp.project.datingapp.p2p.messaging.MailboxStreamClient
import fyp.project.datingapp.p2p.messaging.MailboxStreamServer
import fyp.project.datingapp.p2p.messaging.MessageCrypto
import fyp.project.datingapp.p2p.messaging.MessageService
import fyp.project.datingapp.p2p.messaging.MessageStreamClient
import fyp.project.datingapp.p2p.messaging.MessageStreamServer
import fyp.project.datingapp.p2p.messaging.TransportMailboxHolderLocator
import fyp.project.datingapp.p2p.relay.CacheCipher
import fyp.project.datingapp.p2p.relay.CacheEncryption
import fyp.project.datingapp.p2p.relay.CacheHolderAdvertiser
import fyp.project.datingapp.p2p.relay.DefaultGossipSubInvalidator
import fyp.project.datingapp.p2p.relay.FixedCapacityScorer
import fyp.project.datingapp.p2p.relay.GossipSubInvalidator
import fyp.project.datingapp.p2p.relay.IngestRateLimiter
import fyp.project.datingapp.p2p.relay.RelayPolicy
import fyp.project.datingapp.p2p.relay.ReputationScorer
import fyp.project.datingapp.p2p.relay.SessionInteractionTokens
import fyp.project.datingapp.p2p.relay.SystemClock
import fyp.project.datingapp.p2p.relay.TransportCacheHolderAdvertiser
import fyp.project.datingapp.p2p.relay.TransportGossipChannel
import fyp.project.datingapp.p2p.transport.Libp2pConfig
import fyp.project.datingapp.p2p.transport.Libp2pTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

val appModule = module {
    single<StoreFactory> { DefaultStoreFactory() }
    single<AuthRepository> { DefaultAuthRepository(get<AppDatabase>().authSettingsDao(), get()) }
    // invalidator via getOrNull so the DB layer publishes profile-update
    // invalidations (Phase E) when the relay stack is present; no-op in tests.
    single { RepositoryManager(get(), get(), getOrNull()) }
    single<MessageDao> { get<AppDatabase>().conversationDao() }

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

    // ---- Relay cache (the five defenses; populated via the Home sighting hook) ----
    single { IngestRateLimiter() }
    single { SessionInteractionTokens() }
    single<ReputationScorer> { FixedCapacityScorer() }
    single<CacheCipher> { CacheEncryption() }
    single {
        val auth = get<AuthRepository>()
        RelayPolicy(
            selfDid = { auth.getDid() },
            discoveryDao = get<AppDatabase>().discoveryDao(),
            rateLimiter = get(),
            sessionTokens = get(),
            reputation = get(),
            encryption = get<CacheCipher>(),
            clock = SystemClock,
            // Lazy provider breaks the RelayPolicy <-> invalidator construction
            // cycle: the lambda is only invoked on the first put(), by which point
            // the invalidator singleton is built.
            invalidator = { getOrNull<GossipSubInvalidator>() },
            // Phase F: advertise this device as a cacheHolder on each successful cache.
            advertiser = { getOrNull<CacheHolderAdvertiser>() },
        )
    }
    single<CacheHolderAdvertiser> { TransportCacheHolderAdvertiser(get()) }
    // Phase E: per-DID GossipSub invalidation. App-lifetime scope owns the
    // subscription collectors (cancelled on sign-out teardown, Part 4 §C).
    single<GossipSubInvalidator> {
        DefaultGossipSubInvalidator(
            channel = TransportGossipChannel(get()),
            verifier = get(),
            relayPolicy = get(),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )
    }

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
            // Phase F: cacheHolder fallback via DHT provider records.
            holderLocator = TransportHolderLocator(get()),
        )
    }
    // Owner-side serving handler; register() it on the transport after start.
    // relay = the RelayPolicy so this device also answers fetches for peers it
    // has relay-cached (cacheHolder role; the foundation for Phase F).
    single {
        val auth = get<AuthRepository>()
        val repo = get<RepositoryManager>()
        StreamProfileFetcher(
            selfDid = { auth.getDid() },
            ownEnvelope = { repo.getMyProfileEnvelope() },
            relay = get(),
        )
    }

    // ---- Part 3: blob (photo) fetch over /datingapp/blob/1.0.0 ----
    single<BlobStreamClient> { Libp2pBlobStreamClient(get()) }
    single<BlobFetcher> {
        CascadingBlobFetcher(
            blobDao = get<AppDatabase>().blobDao(),
            blobStore = get(),
            peerDirectory = get(),
            streamClient = get(),
        )
    }
    single { StreamBlobFetcher(blobDao = get<AppDatabase>().blobDao(), blobStore = get()) }

    // ---- Part 5 / M4: online E2EE messaging over /datingapp/message/1.0.0 ----
    // ECIES message crypto (P-256 ECDH + HKDF + AES-GCM); KeyAgreement is platform.
    single<MessageCrypto> {
        val auth = get<AuthRepository>()
        EciesMessageCrypto(selfDid = { auth.getDid() }, keyAgreement = get())
    }
    single<MessageStreamClient> { Libp2pMessageStreamClient(get()) }
    single {
        val auth = get<AuthRepository>()
        MessageService(
            selfDid = { auth.getDid() },
            crypto = get(),
            sign = { auth.sign(it) },
            verifier = get(),
            fetcher = get(),
            peerDirectory = get(),
            streamClient = get(),
            messageDao = get<AppDatabase>().conversationDao(),
            // Lazy: parks undeliverable messages at the recipient's mailbox holders
            // (M5). getOrNull breaks the MessageService <-> MailboxService cycle.
            offlineDeposit = { env -> getOrNull<MailboxService>()?.deposit(env.recipientDid, env) ?: false },
        )
    }
    single {
        val service = get<MessageService>()
        MessageStreamServer { service.handleIncoming(it) }
    }

    // ---- Part 5 / M5: offline mailbox over /datingapp/mailbox/1.0.0 ----
    // Persistent (survives holder restart) + sealed at rest with the cache AEAD.
    single { MailboxHolder(dao = get<AppDatabase>().mailboxDao(), cache = get<CacheCipher>(), clock = SystemClock) }
    single<MailboxHolderLocator> { TransportMailboxHolderLocator(get()) }
    single<MailboxStreamClient> { Libp2pMailboxStreamClient(get()) }
    // Verifies the recipient's signed token on pull (authenticated retrieval).
    single { MailboxStreamServer(holder = get(), verifier = get(), clock = SystemClock) }
    single {
        val auth = get<AuthRepository>()
        MailboxService(
            selfDid = { auth.getDid() },
            locator = get(),
            streamClient = get(),
            onEnvelope = { get<MessageService>().handleIncoming(it) },
            sign = { auth.sign(it) },
            clock = SystemClock,
        )
    }

    // ---- E: photo upload (image-pick -> raw CID -> blob store -> profile) ----
    single { PhotoUploader(blobStore = get(), blobDao = get<AppDatabase>().blobDao()) }

    // ---- E: core match loop over /datingapp/like/1.0.0 ----
    single<LikeStreamClient> { Libp2pLikeStreamClient(get()) }
    single {
        val auth = get<AuthRepository>()
        LikeService(
            selfDid = { auth.getDid() },
            repo = get(),
            incomingLikes = get<AppDatabase>().incomingLikeDao(),
            messageDao = get<AppDatabase>().conversationDao(),
            fetcher = get(),
            verifier = get(),
            peerDirectory = get(),
            streamClient = get(),
            clock = SystemClock,
        )
    }
    single { LikeStreamServer { get<LikeService>().handleIncomingLike(it) } }

    // ---- Phase G.2: real peer feed (discovery + fetch behind one facade) ----
    single { PeerProfileFeed(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
}
