package fyp.project.datingapp.p2p.relay

import fyp.project.datingapp.p2p.transport.wire.ProfileInvalidation

/**
 * Injection point for GossipSub-driven cache invalidation.
 *
 * Phase A exposes the interface only — no libp2p transport is wired yet.
 * [RelayPolicy] accepts a nullable instance so tests and early-phase builds
 * can construct a policy without a transport stack, and later phases can
 * swap in the real implementation without touching policy code.
 *
 * When implemented (Phase E, per `p2p-subsystem-design.md` §11.3), a real
 * invalidator will:
 *   1. Subscribe to `fyp.project.datingapp.invalidate/v1/<did>` for every DID
 *      currently in the relay cache.
 *   2. Verify each [ProfileInvalidation]'s signature against the owner's key.
 *   3. Call [RelayPolicy.onInvalidation] to drop superseded rows.
 */
interface GossipSubInvalidator {
    /** Start listening for invalidations concerning [did]. Idempotent. */
    suspend fun subscribe(did: String)

    /** Stop listening for invalidations concerning [did]. Idempotent. */
    suspend fun unsubscribe(did: String)

    /**
     * Publish an invalidation for one of *our own* records. The signature is
     * carried inside [invalidation] (the owner's per-record signature over
     * `canonicalBytes`), so no separate signature argument is needed.
     */
    suspend fun publish(invalidation: ProfileInvalidation)
}
