package com.aura.p2p.discovery

import com.aura.database.sha256Digest

/**
 * Derives the DHT key and GossipSub topic a presence record is published under,
 * keyed by the ~5 km [Geohash.geohash5] cell (design §6.2/§6.3). A peer queries
 * its own cell plus the 8 [Geohash.neighbours] (9 keys/topics total).
 *
 * The `.v1` segment is the wire version: any change to the [PresenceRecord]
 * shape or these key formats must bump it so old and new peers don't collide.
 */
object PresenceTopics {
    const val DHT_KEY_PREFIX = "com.aura.presence.v1/"
    const val TOPIC_PREFIX = "com.aura.presence/v1/geohash5/"

    /** DHT key = sha256(prefix + geohash5) — a fixed-width, opaque key. */
    fun dhtKey(geohash5: String): ByteArray =
        sha256Digest((DHT_KEY_PREFIX + geohash5).encodeToByteArray())

    /** GossipSub topic for a geohash5 cell. */
    fun topic(geohash5: String): String = TOPIC_PREFIX + geohash5
}
