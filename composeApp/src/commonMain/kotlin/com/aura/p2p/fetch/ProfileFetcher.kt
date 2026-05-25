package com.aura.p2p.fetch

import com.aura.p2p.transport.wire.SignedEnvelope
import com.aura.records.UserProfile

/**
 * Resolves a DID (+ optional expected CID from a `PresenceRecord`) to that user's
 * current, signature-verified profile (`p2p-subsystem-design.md` §7). Every path
 * passes through the verification gate; an unverifiable record is never returned.
 */
interface ProfileFetcher {
    /** Verified [UserProfile], or null if all cascade sources miss. */
    suspend fun fetch(did: String, expectedCid: String? = null): UserProfile?

    /** As [fetch] but returns the raw signed envelope (for relay-cache insertion). */
    suspend fun fetchSigned(did: String, expectedCid: String? = null): SignedEnvelope?
}
