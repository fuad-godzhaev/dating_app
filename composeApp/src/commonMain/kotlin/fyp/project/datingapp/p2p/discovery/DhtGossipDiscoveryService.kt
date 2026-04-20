package fyp.project.datingapp.p2p.discovery

import fyp.project.datingapp.domain.auth.AuthRepository
import fyp.project.datingapp.domain.auth.SignatureVerifier
import fyp.project.datingapp.p2p.relay.EpochClock
import fyp.project.datingapp.p2p.relay.SystemClock
import fyp.project.datingapp.p2p.transport.Libp2pTransport
import fyp.project.datingapp.p2p.transport.wire.PresenceRecord
import fyp.project.datingapp.records.canonical.decodePresenceRecord
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Instant

/**
 * [DiscoveryService] over the app's Kad-DHT + GossipSub (design §6). Announcing
 * is delegated to [PresenceAnnouncer]; discovery merges a one-shot DHT lookup of
 * the 9-cell geohash neighbourhood with the continuous GossipSub streams on the
 * same cells, then verifies, de-duplicates, drops expired, and filters.
 *
 * State is in-memory and per-collection (design §9.4) — no persistence.
 */
class DhtGossipDiscoveryService(
    private val transport: Libp2pTransport,
    private val announcer: PresenceAnnouncer,
    private val locator: GeohashLocator,
    private val verifier: SignatureVerifier,
    private val authRepository: AuthRepository,
    private val peerDirectory: PeerDirectory,
    private val clock: EpochClock = SystemClock,
    private val maxDhtResults: Int = DEFAULT_MAX_DHT_RESULTS,
) : DiscoveryService {

    override fun announceSelf(scope: kotlinx.coroutines.CoroutineScope) = announcer.startAnnouncing(scope)
    override suspend fun announceOnce(): Boolean = announcer.announceOnce()
    override fun stopAnnouncing() = announcer.stopAnnouncing()

    override fun candidates(filters: DiscoveryFilters): Flow<PresenceRecord> = channelFlow {
        val cell = locator.currentGeohash5() ?: return@channelFlow
        val cells = (listOf(cell) + Geohash.neighbours(cell)).distinct()
        // Drop our own presence: the DHT can return the value we just published, and
        // discovering yourself is never useful. (GossipSub already doesn't echo to the
        // publisher; this also covers the DHT path.)
        val selfDid = authRepository.getDid()

        // did -> newest announcedAt seen, for cross-source dedup (DHT + 9 topics).
        val newest = HashMap<String, String>()
        val mutex = Mutex()

        suspend fun accept(payload: ByteArray): PresenceRecord? {
            val record = runCatching { decodePresenceRecord(payload) }.getOrNull() ?: return null
            if (record.did == selfDid) return null
            if (isExpired(record)) return null
            if (!PresenceVerifier.verify(record, verifier)) return null
            // Record reachability for every verified peer (even if filtered out),
            // so the fetch cascade can dial it later via PeerDirectory.
            peerDirectory.record(record.did, record.peerId, record.multiaddrs)
            if (!filters.matches(record)) return null
            return mutex.withLock {
                val prev = newest[record.did]
                if (prev != null && prev >= record.announcedAt) {
                    null
                } else {
                    newest[record.did] = record.announcedAt
                    record
                }
            }
        }

        // 1) One-shot DHT snapshot across the neighbourhood.
        for (c in cells) {
            val values = runCatching { transport.dhtGetValues(PresenceTopics.dhtKey(c), maxDhtResults) }
                .getOrDefault(emptyList())
            for (payload in values) accept(payload)?.let { trySend(it) }
        }

        // 2) Continuous GossipSub updates; one collector per cell.
        for (c in cells) {
            launch {
                transport.gossipSubscribe(PresenceTopics.topic(c)).collect { msg ->
                    accept(msg.payload)?.let { trySend(it) }
                }
            }
        }

        awaitClose { /* child collectors cancel with the channelFlow scope */ }
    }

    private fun isExpired(record: PresenceRecord): Boolean {
        val announcedMs = runCatching { Instant.parse(record.announcedAt).toEpochMilliseconds() }
            .getOrNull() ?: return true // unparseable timestamp -> drop
        return announcedMs + record.ttl < clock.nowMs()
    }

    companion object {
        const val DEFAULT_MAX_DHT_RESULTS = 16
    }
}
