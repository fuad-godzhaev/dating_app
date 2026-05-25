package com.aura.p2p.feed

/**
 * Brings up local-network peer bootstrap so the DHT / GossipSub mesh has peers to
 * talk to. Without it, two freshly-started hosts on the same LAN never connect and
 * discovery returns nothing. Android backs this with NsdManager (the working LAN
 * path from Phase B); cross-network bootstrap (peerstore / IPFS rendezvous) is a
 * later phase. Called once from [PeerProfileFeed.ensureStarted] after the host is up.
 */
interface LanBootstrap {
    /** Start advertising + discovering on the LAN and dialling found peers. Idempotent. */
    fun start()
    fun stop()
}
