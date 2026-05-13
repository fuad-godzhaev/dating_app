package fyp.project.datingapp.p2p.feed

import fyp.project.datingapp.p2p.blob.BlobFetcher
import fyp.project.datingapp.p2p.blob.StreamBlobFetcher
import fyp.project.datingapp.p2p.discovery.DiscoveryFilters
import fyp.project.datingapp.p2p.discovery.DiscoveryService
import fyp.project.datingapp.p2p.fetch.ProfileFetcher
import fyp.project.datingapp.p2p.fetch.StreamProfileFetcher
import fyp.project.datingapp.p2p.like.LikeStreamServer
import fyp.project.datingapp.p2p.messaging.MailboxService
import fyp.project.datingapp.p2p.messaging.MailboxStreamServer
import fyp.project.datingapp.p2p.messaging.MessageStreamServer
import fyp.project.datingapp.p2p.transport.Libp2pTransport
import fyp.project.datingapp.p2p.transport.wire.SignedEnvelope
import fyp.project.datingapp.records.UserProfile
import fyp.project.datingapp.records.canonical.decodeUserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * The single facade the UI consumes for a real peer feed: it owns the discovery
 * + fetch lifecycle and emits fully-resolved, signature-verified [UserProfile]s.
 *
 * [ensureStarted] (idempotent) brings the libp2p host up, registers the profile
 * serving handler so this device answers fetches, and starts the presence
 * heartbeat. [candidates] streams discovered peers and resolves each to a full
 * profile via the fetch cascade, fetching concurrently so one slow/unreachable
 * peer doesn't stall the others.
 *
 * Deferred (G.2 follow-ups): relay-cache sighting hook (session token on card
 * render), real photo-blob fetch, and sign-out/WorkManager teardown. The
 * announce loop runs on an app-lifetime scope here.
 */
class PeerProfileFeed(
    private val transport: Libp2pTransport,
    private val discovery: DiscoveryService,
    private val fetcher: ProfileFetcher,
    private val streamServer: StreamProfileFetcher,
    private val lanBootstrap: LanBootstrap,
    private val blobFetcher: BlobFetcher,
    private val blobServer: StreamBlobFetcher,
    private val messageServer: MessageStreamServer,
    private val mailboxServer: MailboxStreamServer,
    private val mailboxService: MailboxService,
    private val likeServer: LikeStreamServer,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val startMutex = Mutex()
    private var started = false

    /** Whether the host/feed is currently up. Lock-free read (benign race) so a background
     *  worker can avoid tearing down a feed the foreground is already using. */
    val isRunning: Boolean get() = started

    /** Bring up host + serving handler + LAN bootstrap + presence heartbeat. Idempotent. */
    suspend fun ensureStarted() {
        startMutex.withLock {
            if (started) return
            transport.start()
            streamServer.register(transport)
            blobServer.register(transport)
            messageServer.register(transport)
            mailboxServer.register(transport)
            likeServer.register(transport)
            lanBootstrap.start()
            discovery.announceSelf(scope)
            started = true
        }
        // Collect any mail parked while we were offline (M5). Best-effort, off the
        // start lock so a slow DHT lookup doesn't block the feed coming up.
        scope.launch { runCatching { mailboxService.pullOwnMail() } }
    }

    /**
     * Resolve a profile photo by its raw-leaf CID to a local file path, fetching
     * from [profileDid]'s owner over `/datingapp/blob/1.0.0` if not already cached.
     * Null if it can't be resolved (offline owner, integrity failure, iOS stub).
     */
    suspend fun loadPhoto(profileDid: String, blobCid: String): String? =
        blobFetcher.fetch(profileDid, blobCid)

    /**
     * Tear down the feed lifecycle (Part 4 §C): stop the presence heartbeat, the LAN
     * bootstrap, and the libp2p host, and cancel in-flight candidate/fetch work.
     * Idempotent and safe to call from a sign-out path. After [stop], a later
     * [ensureStarted] brings everything back up (the scope is reused, not killed).
     *
     * Note: the GossipSub invalidator owns a separate DI-created scope; its
     * subscription collectors unwind on their own when [transport] stops (their
     * flows error and are runCatching-swallowed). Fully cancelling that scope on
     * sign-out is a follow-up tied to the DI-owned lifecycle.
     */
    suspend fun stop() {
        startMutex.withLock {
            if (!started) return
            discovery.stopAnnouncing()
            lanBootstrap.stop()
            runCatching { transport.stop() }
            scope.coroutineContext.cancelChildren()
            started = false
        }
    }

    /**
     * Verified peer profiles for the feed: each discovered [PresenceRecord] is
     * resolved via the fetch cascade to a [FeedCandidate] carrying both the
     * decoded [UserProfile] and the signed envelope it came from. The envelope
     * is surfaced (not just the profile) so the Home sighting hook can hand it
     * to the relay cache without re-fetching. Cold flow; [ensureStarted] runs
     * first so collection always has a running host.
     */
    fun candidates(filters: DiscoveryFilters = DiscoveryFilters()): Flow<FeedCandidate> = channelFlow {
        ensureStarted()
        discovery.candidates(filters).collect { presence ->
            launch {
                val envelope = fetchSignedWithRetry(presence.did, presence.profileCid) ?: return@launch
                val profile = runCatching { decodeUserProfile(envelope.canonicalBytes) }.getOrNull()
                    ?: return@launch
                trySend(FeedCandidate(profile, envelope))
            }
        }
    }

    private suspend fun fetchSignedWithRetry(
        did: String,
        expectedCid: String,
        attempts: Int = FETCH_ATTEMPTS,
    ): SignedEnvelope? {
        repeat(attempts) {
            runCatching { fetcher.fetchSigned(did, expectedCid) }.getOrNull()?.let { return it }
            delay(FETCH_RETRY_MS)
        }
        return null
    }

    companion object {
        // A first sighting can race connection setup; retry a few times.
        private const val FETCH_ATTEMPTS = 4
        private const val FETCH_RETRY_MS = 3000L
    }
}

/**
 * A discovered peer resolved to a verified profile plus the [SignedEnvelope] it
 * was fetched from. The envelope is the byte-exact, owner-signed record; the
 * Home relay-cache sighting hook feeds it to `RelayPolicy.put` so this device
 * can later serve it as a cacheHolder (Phase F) without re-fetching.
 */
data class FeedCandidate(
    val profile: UserProfile,
    val envelope: SignedEnvelope,
)
