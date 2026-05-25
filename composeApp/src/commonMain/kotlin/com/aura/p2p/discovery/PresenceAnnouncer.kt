package com.aura.p2p.discovery

import com.aura.database.RepositoryManager
import com.aura.domain.auth.AuthRepository
import com.aura.p2p.relay.EpochClock
import com.aura.p2p.relay.SystemClock
import com.aura.p2p.transport.Transport
import com.aura.p2p.transport.wire.AgeRange
import com.aura.p2p.transport.wire.PresenceRecord
import com.aura.records.canonical.encodeCanonical
import com.aura.records.canonical.encodePresenceWire
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Instant

/**
 * Builds, signs, and publishes the local user's [PresenceRecord] heartbeat to
 * both the app's Kad-DHT (under the geohash-derived key) and the per-geohash
 * GossipSub topic (design §6.2). Re-publishes on a timer so peers see a fresh,
 * unexpired record; [ttlMs] bounds how long a sighting stays valid.
 *
 * Returns/announces nothing until the prerequisites exist: an identity DID, a
 * stored profile (for its CID + interests), a current geohash, and a dialable
 * multiaddr. Missing any of these makes [announceOnce] a no-op (returns false)
 * rather than publishing a useless record.
 */
class PresenceAnnouncer(
    private val transport: Transport,
    private val authRepository: AuthRepository,
    private val repositoryManager: RepositoryManager,
    private val preferencesStore: DiscoveryPreferencesStore,
    private val locator: GeohashLocator,
    private val localAddress: LocalAddressProvider,
    private val clock: EpochClock = SystemClock,
    private val ttlMs: Long = DEFAULT_TTL_MS,
    private val republishIntervalMs: Long = DEFAULT_REPUBLISH_INTERVAL_MS,
) {
    private var job: Job? = null

    /** Publish once now. Returns false if a prerequisite is missing (no-op). */
    suspend fun announceOnce(): Boolean {
        val record = buildSignedRecord() ?: return false
        val wire = encodePresenceWire(record)
        val cell = Geohash.geohash5(record.geohash)
        runCatching { transport.dhtPutValue(PresenceTopics.dhtKey(cell), wire) }
        runCatching { transport.gossipPublish(PresenceTopics.topic(cell), wire) }
        return true
    }

    /** Begin a re-publish loop on [scope]; idempotent while already running. */
    fun startAnnouncing(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch {
            var iteration = 0
            while (isActive) {
                runCatching { announceOnce() }
                // The first announce(s) race connection setup: a DHT put with no
                // connected peer fails and GossipSub has no mesh yet. Re-publish
                // rapidly at first so a peer that connects within the first minute
                // is discovered promptly, then settle to the steady interval (which
                // stays well under the TTL for liveness).
                val delayMs =
                    if (iteration < INITIAL_BURST_COUNT) INITIAL_BURST_INTERVAL_MS else republishIntervalMs
                iteration++
                delay(delayMs)
            }
        }
    }

    fun stopAnnouncing() {
        job?.cancel()
        job = null
    }

    private suspend fun buildSignedRecord(): PresenceRecord? {
        val did = authRepository.getDid() ?: return null
        val geohash = locator.currentGeohash() ?: return null
        val profileCid = repositoryManager.getMyProfileCid() ?: return null
        val peerId = transport.peerId
        val multiaddrs = buildMultiaddrs(peerId)
        if (multiaddrs.isEmpty()) return null

        val prefs = preferencesStore.current()
        val profile = repositoryManager.getMyProfile()
        val interests = profile?.interests ?: emptyList()
        // Advertise the user's OWN age band from their (persisted) profile so peers' age filters
        // match real values; fall back to the prefs default only when no profile age is available.
        // gender/lookingFor still come from DiscoveryPreferences (see the note there).
        val ownAge = profile?.age
        val ageRange = if (ownAge != null && ownAge in 18..120) AgeRange(ownAge, ownAge) else prefs.ageRange
        val unsigned = PresenceRecord(
            did = did,
            peerId = peerId,
            multiaddrs = multiaddrs,
            profileCid = profileCid,
            geohash = geohash,
            interests = interests,
            gender = prefs.gender,
            lookingFor = prefs.lookingFor,
            ageRange = ageRange,
            announcedAt = Instant.fromEpochMilliseconds(clock.nowMs()).toString(),
            ttl = ttlMs,
            signature = ByteArray(0),
        )
        val signature = authRepository.sign(encodeCanonical(unsigned))
        return unsigned.copy(signature = signature)
    }

    private suspend fun buildMultiaddrs(peerId: String): List<String> {
        val ip = localAddress.localIpv4() ?: return emptyList()
        val port = parseTcpPort(transport.listenAddrs) ?: return emptyList()
        return listOf("/ip4/$ip/tcp/$port/p2p/$peerId")
    }

    companion object {
        const val DEFAULT_TTL_MS: Long = 15 * 60 * 1000L                  // 15 min (§6.2)
        const val DEFAULT_REPUBLISH_INTERVAL_MS: Long = 10 * 60 * 1000L   // steady refresh before expiry
        // Initial rapid announces to win the connection race + snappy first discovery.
        const val INITIAL_BURST_COUNT: Int = 8
        const val INITIAL_BURST_INTERVAL_MS: Long = 8_000L

        /** Parse the TCP port from a libp2p listen multiaddr list ("/ip4/.../tcp/<port>"). */
        fun parseTcpPort(addrs: List<String>): Int? {
            val marker = "/tcp/"
            for (a in addrs) {
                val idx = a.indexOf(marker)
                if (idx >= 0) {
                    val port = a.substring(idx + marker.length).substringBefore('/').toIntOrNull()
                    if (port != null && port > 0) return port
                }
            }
            return null
        }
    }
}
