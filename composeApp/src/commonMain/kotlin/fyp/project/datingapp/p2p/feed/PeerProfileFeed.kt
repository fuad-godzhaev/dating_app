package fyp.project.datingapp.p2p.feed

import fyp.project.datingapp.p2p.discovery.DiscoveryFilters
import fyp.project.datingapp.p2p.discovery.DiscoveryService
import fyp.project.datingapp.p2p.fetch.ProfileFetcher
import fyp.project.datingapp.p2p.fetch.StreamProfileFetcher
import fyp.project.datingapp.p2p.transport.Libp2pTransport
import fyp.project.datingapp.records.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
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
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val startMutex = Mutex()
    private var started = false

    /** Bring up host + serving handler + LAN bootstrap + presence heartbeat. Idempotent. */
    suspend fun ensureStarted() {
        startMutex.withLock {
            if (started) return
            transport.start()
            streamServer.register(transport)
            lanBootstrap.start()
            discovery.announceSelf(scope)
            started = true
        }
    }

    /**
     * Verified peer profiles for the feed: each discovered [PresenceRecord] is
     * resolved to a full [UserProfile] via the fetch cascade. Cold flow; calling
     * [ensureStarted] first so collection always has a running host.
     */
    fun candidates(filters: DiscoveryFilters = DiscoveryFilters()): Flow<UserProfile> = channelFlow {
        ensureStarted()
        discovery.candidates(filters).collect { presence ->
            launch {
                fetchWithRetry(presence.did, presence.profileCid)?.let { trySend(it) }
            }
        }
    }

    private suspend fun fetchWithRetry(
        did: String,
        expectedCid: String,
        attempts: Int = FETCH_ATTEMPTS,
    ): UserProfile? {
        repeat(attempts) {
            runCatching { fetcher.fetch(did, expectedCid) }.getOrNull()?.let { return it }
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
