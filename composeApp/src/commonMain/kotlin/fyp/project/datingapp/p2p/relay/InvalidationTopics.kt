package fyp.project.datingapp.p2p.relay

/**
 * Derives the per-DID GossipSub topic a profile owner publishes
 * [fyp.project.datingapp.p2p.transport.wire.ProfileInvalidation]s on, and that a
 * cacheHolder subscribes to for every DID it currently relay-caches
 * (`p2p-subsystem-design.md` §8.4 / §11.3).
 *
 * The `.v1` segment is the wire version: any change to the invalidation shape or
 * this topic format must bump it so old and new peers don't collide. Mirrors
 * [fyp.project.datingapp.p2p.discovery.PresenceTopics].
 */
object InvalidationTopics {
    const val TOPIC_PREFIX = "fyp.project.datingapp.invalidate/v1/"

    /** GossipSub topic carrying invalidations for [did]. */
    fun topic(did: String): String = TOPIC_PREFIX + did
}
