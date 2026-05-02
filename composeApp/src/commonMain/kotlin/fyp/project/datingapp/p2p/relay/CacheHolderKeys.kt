package fyp.project.datingapp.p2p.relay

import fyp.project.datingapp.database.sha256Digest

/**
 * DHT key under which cacheHolders advertise (via *provider records*) that they
 * serve a given target DID's profile (`p2p-subsystem-design.md` §7.2). A holder
 * calls `dhtProvide(cacheHolderKey(targetDid))`; a requester calls
 * `dhtFindProviders(cacheHolderKey(targetDid))` to get holder peerIds.
 *
 * Provider records (not `PutValue`) are used deliberately: the transport DHT is
 * single-value-per-key (last-writer-wins), so a list of holders can only be
 * represented as the provider set, not a value. Mirrors [PresenceTopics.dhtKey].
 */
object CacheHolderKeys {
    const val KEY_PREFIX = "fyp.project.datingapp.cache-holders.v1/"

    /** Opaque, fixed-width DHT key = sha256(prefix + targetDid). */
    fun cacheHolderKey(targetDid: String): ByteArray =
        sha256Digest((KEY_PREFIX + targetDid).encodeToByteArray())
}
