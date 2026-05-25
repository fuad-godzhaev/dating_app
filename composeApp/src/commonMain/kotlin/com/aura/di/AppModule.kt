package com.aura.di

import com.arkivanov.mvikotlin.core.store.StoreFactory
import com.arkivanov.mvikotlin.main.store.DefaultStoreFactory
import com.aura.database.AppDatabase
import com.aura.database.RepositoryManager
import com.aura.database.appView.dao.IncomingLikesDao
import com.aura.database.appView.dao.MessageDao
import com.aura.domain.auth.AuthRepository
import com.aura.domain.auth.DefaultAuthRepository
import com.aura.domain.auth.SecureKeyStorage
import com.aura.domain.auth.SignatureVerifier
import com.aura.domain.auth.defaultSignatureVerifier
import com.aura.p2p.blob.BlobFetcher
import com.aura.p2p.blob.BlobStreamClient
import com.aura.p2p.blob.CascadingBlobFetcher
import com.aura.p2p.blob.Libp2pBlobStreamClient
import com.aura.p2p.blob.StreamBlobFetcher
import com.aura.p2p.discovery.DhtGossipDiscoveryService
import com.aura.p2p.discovery.DiscoveryPreferencesStore
import com.aura.p2p.discovery.DiscoveryService
import com.aura.p2p.discovery.GeohashLocator
import com.aura.p2p.discovery.PeerDirectory
import com.aura.p2p.discovery.PresenceAnnouncer
import com.aura.p2p.fetch.CascadingProfileFetcher
import com.aura.p2p.fetch.Libp2pProfileStreamClient
import com.aura.p2p.fetch.ProfileFetcher
import com.aura.p2p.fetch.ProfileStreamClient
import com.aura.p2p.fetch.StreamProfileFetcher
import com.aura.p2p.fetch.TransportHolderLocator
import com.aura.p2p.blob.PhotoUploader
import com.aura.p2p.feed.PeerProfileFeed
import com.aura.p2p.like.Libp2pLikeStreamClient
import com.aura.p2p.like.LikeService
import com.aura.p2p.like.LikeStreamClient
import com.aura.p2p.like.LikeStreamServer
import com.aura.p2p.messaging.EciesMessageCrypto
import com.aura.p2p.messaging.Libp2pMailboxStreamClient
import com.aura.p2p.messaging.Libp2pMessageStreamClient
import com.aura.p2p.messaging.MailboxHolder
import com.aura.p2p.messaging.MailboxHolderLocator
import com.aura.p2p.messaging.MailboxService
import com.aura.p2p.messaging.MailboxStreamClient
import com.aura.p2p.messaging.MailboxStreamServer
import com.aura.p2p.messaging.Libp2pReceiptStreamClient
import com.aura.p2p.messaging.MessageCrypto
import com.aura.p2p.messaging.MessageService
import com.aura.p2p.messaging.ReceiptStreamClient
import com.aura.p2p.messaging.ReceiptStreamServer
import com.aura.p2p.messaging.MessageStreamClient
import com.aura.p2p.messaging.MessageStreamServer
import com.aura.p2p.messaging.TransportMailboxHolderLocator
import com.aura.p2p.relay.CacheCipher
import com.aura.p2p.relay.CacheEncryption
import com.aura.p2p.relay.CacheHolderAdvertiser
import com.aura.p2p.relay.DefaultGossipSubInvalidator
import com.aura.p2p.relay.FixedCapacityScorer
import com.aura.p2p.relay.GossipSubInvalidator
import com.aura.p2p.relay.IngestRateLimiter
import com.aura.p2p.relay.RelayPolicy
import com.aura.p2p.relay.ReputationScorer
import com.aura.p2p.relay.SessionInteractionTokens
import com.aura.p2p.relay.SystemClock
import com.aura.p2p.relay.TransportCacheHolderAdvertiser
import com.aura.p2p.relay.TransportGossipChannel
import com.aura.p2p.transport.Libp2pConfig
import com.aura.p2p.transport.Libp2pTransport
import com.aura.p2p.transport.Transport
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
    single<IncomingLikesDao> { get<AppDatabase>().incomingLikeDao() }

    // ---- Phase C: discovery (transport runtime + presence over DHT/GossipSub) ----
    single<SignatureVerifier> { defaultSignatureVerifier() }
    // One shared libp2p host for the whole app: built from the encrypted-at-rest
    // Ed25519 transport seed. Lazy — only created when discovery first needs it.
    single {
        Libp2pTransport(Libp2pConfig(identityKey = get<SecureKeyStorage>().getOrCreateTransportSeed()))
    }
    // Expose the transport through its interface so seam consumers (and the simulation
    // harness) depend on Transport, not the concrete expect class.
    single<Transport> { get<Libp2pTransport>() }
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

    // ---- Part 3: blob (photo) fetch over /aura/blob/1.0.0 ----
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

    // ---- Part 5 / M4: online E2EE messaging over /aura/message/1.0.0 ----
    // ECIES message crypto (P-256 ECDH + HKDF + AES-GCM); KeyAgreement is platform.
    single<MessageCrypto> {
        val auth = get<AuthRepository>()
        EciesMessageCrypto(selfDid = { auth.getDid() }, keyAgreement = get())
    }
    single<MessageStreamClient> { Libp2pMessageStreamClient(get()) }
    single<ReceiptStreamClient> { Libp2pReceiptStreamClient(get()) }
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
            receiptClient = get(),
        )
    }
    single {
        val service = get<MessageService>()
        MessageStreamServer { service.handleIncoming(it) }
    }
    single {
        val service = get<MessageService>()
        ReceiptStreamServer { service.handleReceipt(it) }
    }

    // ---- Part 5 / M5: offline mailbox over /aura/mailbox/1.0.0 ----
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

    // ---- E: photo upload (image-pick -> downscale/recompress -> raw CID -> blob store -> profile) ----
    single { PhotoUploader(blobStore = get(), blobDao = get<AppDatabase>().blobDao(), transcoder = get()) }

    // ---- E: core match loop over /aura/like/1.0.0 ----
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
    single { PeerProfileFeed(get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get(), get()) }
}
