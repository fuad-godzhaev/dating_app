package com.aura.p2p.discovery

import com.aura.p2p.transport.wire.PresenceRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow

/**
 * Discovery facade (design §6): announce the local user's presence and stream
 * verified candidates from the surrounding geohash neighbourhood. Candidates are
 * lightweight [PresenceRecord]s — turning one into a full
 * [com.aura.records.UserProfile] is the Phase D fetch cascade.
 */
interface DiscoveryService {
    /**
     * Cold flow of verified, non-expired, deduplicated candidates matching
     * [filters]. Emits a one-shot DHT snapshot first, then live GossipSub
     * updates, until the collector cancels.
     */
    fun candidates(filters: DiscoveryFilters = DiscoveryFilters()): Flow<PresenceRecord>

    /** Start the periodic presence heartbeat on [scope]. */
    fun announceSelf(scope: CoroutineScope)

    /** Publish presence once now; returns false if prerequisites are missing. */
    suspend fun announceOnce(): Boolean

    /** Stop the heartbeat loop. */
    fun stopAnnouncing()
}
