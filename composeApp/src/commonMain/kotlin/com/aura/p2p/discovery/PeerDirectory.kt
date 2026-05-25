package com.aura.p2p.discovery

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** A peer's libp2p reachability, as advertised in its PresenceRecord. */
data class PeerContact(val peerId: String, val multiaddrs: List<String>)

/**
 * In-memory `DID -> PeerContact` map bridging discovery and fetch. The fetch
 * cascade's direct-P2P step needs the owner's libp2p peerId + multiaddrs, which
 * are **not** derivable from the `did:key` (the signing key and the Ed25519
 * transport key are different). Discovery learns them from each verified
 * `PresenceRecord`; the fetcher reads them here.
 *
 * Transient and lock-free (atomic CAS via [MutableStateFlow.update]) — consistent
 * with discovery state being in-memory only (design §9.4). Last writer wins, so a
 * fresher presence (new multiaddrs after an IP change) overwrites the old contact.
 */
class PeerDirectory {
    private val contacts = MutableStateFlow<Map<String, PeerContact>>(emptyMap())

    fun record(did: String, peerId: String, multiaddrs: List<String>) {
        contacts.update { it + (did to PeerContact(peerId, multiaddrs)) }
    }

    fun get(did: String): PeerContact? = contacts.value[did]
}
