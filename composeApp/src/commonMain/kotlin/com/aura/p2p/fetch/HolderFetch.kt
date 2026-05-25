package com.aura.p2p.fetch

import com.aura.p2p.relay.CacheHolderKeys
import com.aura.p2p.transport.Transport
import kotlinx.coroutines.flow.Flow

/**
 * Requester side of Phase F: find the peerIds of cacheHolders advertising a given
 * target DID, via DHT provider records on [CacheHolderKeys.cacheHolderKey]. The
 * cascade ([CascadingProfileFetcher] step 4) asks each holder over
 * [PROFILE_PROTOCOL_ID] and verifies the served envelope against the *owner's* key,
 * so a holder can withhold but never forge.
 *
 * A narrow seam (the `expect` [Libp2pTransport] can't be faked in commonTest);
 * [TransportHolderLocator] is the production adapter. `dhtFindProviders` also seeds
 * the peerstore with each holder's addrs, so a follow-up `openStream(peerId, ...)`
 * can auto-dial without an explicit multiaddr.
 */
interface HolderLocator {
    fun findHolders(targetDid: String): Flow<String>
}

/** Production adapter over the go-libp2p host's DHT provider lookup. */
class TransportHolderLocator(private val transport: Transport) : HolderLocator {
    override fun findHolders(targetDid: String): Flow<String> =
        transport.dhtFindProviders(CacheHolderKeys.cacheHolderKey(targetDid))
}
