package com.aura.p2p.relay

import com.aura.p2p.transport.wire.ProfileInvalidation
import com.aura.p2p.transport.wire.SignedEnvelope
import com.aura.records.canonical.Cid
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * End-to-end coverage of the five defenses in [RelayPolicy.put], plus the
 * AAD binding on [RelayPolicy.get]. Each test isolates the defense it
 * targets by keeping the others slack (rate limit high, capacity large,
 * etc.) so the failure signal is unambiguous.
 */
class RelayPolicyTest {

    private class MutableClock(var t: Long = 0L) : EpochClock {
        override fun nowMs(): Long = t
    }

    private fun envelope(
        ownerDid: String = "did:key:peer1",
        cid: String = "bafyreipeer1cidvalueforunittestsonlydonotuseinproduction",
        body: ByteArray = byteArrayOf(1, 2, 3, 4),
        signature: ByteArray = byteArrayOf(9, 9, 9),
    ) = SignedEnvelope(
        collection = "com.aura.profile",
        rkey = "self",
        ownerDid = ownerDid,
        cid = cid,
        canonicalBytes = body,
        signature = signature,
    )

    private fun buildPolicy(
        selfDid: String = "did:key:self",
        capacity: Int = 20,
        perMinute: Int = 1_000,
        perDay: Int = 1_000_000,
        clock: MutableClock = MutableClock(0),
    ): Pair<RelayPolicy, Fixtures> {
        val dao = FakeDiscoveryDao()
        val cipher = FakeCacheEncryption()
        val limiter = IngestRateLimiter(perMinute, perDay, clock)
        val tokens = SessionInteractionTokens(ttlMs = 30_000L, clock = clock)
        val reputation = FixedCapacityScorer(capacity)
        val policy = RelayPolicy(
            selfDid = { selfDid },
            discoveryDao = dao,
            rateLimiter = limiter,
            sessionTokens = tokens,
            reputation = reputation,
            encryption = cipher,
            clock = clock,
        )
        return policy to Fixtures(dao, cipher, limiter, tokens, clock)
    }

    private data class Fixtures(
        val dao: FakeDiscoveryDao,
        val cipher: FakeCacheEncryption,
        val limiter: IngestRateLimiter,
        val tokens: SessionInteractionTokens,
        val clock: MutableClock,
    )

    // --- Defense 1: non-self ------------------------------------------------

    @Test
    fun selfSignedEnvelopeIsRejected() = runTest {
        val (policy, fx) = buildPolicy(selfDid = "did:key:me")
        val tok = fx.tokens.issue()
        assertFalse(policy.put(envelope(ownerDid = "did:key:me"), tok))
        assertEquals(0, fx.dao.countCached())
        // The rejected call must not burn the token either — non-self is the
        // first check and short-circuits before consume.
        assertEquals(1, fx.tokens.outstandingCount())
    }

    // --- Defense 2: rate limit ---------------------------------------------

    @Test
    fun rateLimiterDropsBurstSilently() = runTest {
        val (policy, fx) = buildPolicy(perMinute = 2, perDay = 1_000)
        repeat(4) {
            val tok = fx.tokens.issue()
            policy.put(envelope(ownerDid = "did:key:peer$it"), tok)
        }
        // Only the first two should have landed.
        assertEquals(2, fx.dao.countCached())
    }

    // --- Defense 3: session interaction token ------------------------------

    @Test
    fun missingTokenRejectsPut() = runTest {
        val (policy, fx) = buildPolicy()
        assertFalse(policy.put(envelope(), sessionToken = null))
        assertFalse(policy.put(envelope(), sessionToken = "not-a-real-token"))
        assertEquals(0, fx.dao.countCached())
    }

    @Test
    fun tokenIsSingleUse() = runTest {
        val (policy, fx) = buildPolicy()
        val tok = fx.tokens.issue()
        assertTrue(policy.put(envelope(ownerDid = "did:key:a"), tok))
        assertFalse(policy.put(envelope(ownerDid = "did:key:b"), tok), "same token must not ingest twice")
        assertEquals(1, fx.dao.countCached())
    }

    // --- Defense 4: capacity cap + LRU eviction ----------------------------

    @Test
    fun capacityExceededEvictsOldestByLastServed() = runTest {
        val (policy, fx) = buildPolicy(capacity = 2)

        // Fill to capacity.
        fx.clock.t = 10
        policy.put(envelope(ownerDid = "did:key:a"), fx.tokens.issue())
        fx.clock.t = 20
        policy.put(envelope(ownerDid = "did:key:b"), fx.tokens.issue())
        assertEquals(2, fx.dao.countCached())

        // Serve 'a' so 'b' becomes the oldest lastServedAt.
        fx.clock.t = 30
        policy.get("did:key:a")

        // Admit a third row — 'b' should be evicted (oldest lastServedAt).
        fx.clock.t = 40
        policy.put(envelope(ownerDid = "did:key:c"), fx.tokens.issue())
        assertEquals(2, fx.dao.countCached())
        assertNotNull(fx.dao.getProfileByDid("did:key:a"))
        assertNull(fx.dao.getProfileByDid("did:key:b"))
        assertNotNull(fx.dao.getProfileByDid("did:key:c"))
    }

    // --- Defense 5: AEAD AAD binding on read -------------------------------

    @Test
    fun tamperedRowIsTreatedAsCacheMiss() = runTest {
        val (policy, fx) = buildPolicy()
        val body = byteArrayOf(10, 20, 30)
        val tok = fx.tokens.issue()
        assertTrue(policy.put(envelope(ownerDid = "did:key:peer", body = body), tok))

        // Simulate an attacker swapping the row's profileCid → AAD changes,
        // ciphertext no longer decrypts → get returns null.
        val row = fx.dao.getProfileByDid("did:key:peer")!!
        fx.dao.upsertProfile(row.copy(profileCid = "bafyreiattackertamperedcidnotmatchingtheoneatsealtime"))

        assertNull(policy.get("did:key:peer"))
    }

    // --- Happy path round-trip --------------------------------------------

    @Test
    fun putThenGetRoundTripsBytes() = runTest {
        val (policy, fx) = buildPolicy()
        val body = byteArrayOf(42, 43, 44, 45)
        fx.clock.t = 100
        assertTrue(policy.put(envelope(ownerDid = "did:key:peer", body = body), fx.tokens.issue()))

        fx.clock.t = 200
        val got = policy.get("did:key:peer")
        assertNotNull(got)
        assertTrue(body.contentEquals(got.canonicalBytes))

        // lastServedAt got bumped to the get-time clock.
        assertEquals(200L, fx.dao.getProfileByDid("did:key:peer")?.lastServedAt)
    }

    @Test
    fun expiredRowReturnsNull() = runTest {
        val (policy, fx) = buildPolicy()
        fx.clock.t = 0
        policy.put(envelope(ownerDid = "did:key:peer"), fx.tokens.issue())
        fx.clock.t = RelayPolicy.DEFAULT_TTL_MS + 1
        assertNull(policy.get("did:key:peer"))
    }

    @Test
    fun onInvalidationDropsRow() = runTest {
        val (policy, fx) = buildPolicy()
        policy.put(envelope(ownerDid = "did:key:peer"), fx.tokens.issue())
        assertEquals(1, fx.dao.countCached())
        policy.onInvalidation("did:key:peer")
        assertEquals(0, fx.dao.countCached())
    }

    // --- Phase E: onInvalidationReplace ------------------------------------

    private fun invalidation(ownerDid: String, cid: String, body: ByteArray) =
        ProfileInvalidation(
            ownerDid = ownerDid,
            collection = "com.aura.records.profile",
            rkey = "self",
            cid = cid,
            canonicalBytes = body,
            signature = byteArrayOf(8, 8, 8),
            publishedAt = "2026-05-23T12:00:00Z",
        )

    @Test
    fun onInvalidationReplace_swapsCachedBodyInPlace() = runTest {
        val (policy, fx) = buildPolicy()
        val body1 = byteArrayOf(1, 1, 1)
        fx.clock.t = 100
        assertTrue(policy.put(envelope(ownerDid = "did:key:peer", cid = "cidone", body = body1), fx.tokens.issue()))

        val body2 = byteArrayOf(2, 2, 2, 2)
        val cid2 = Cid.cidV1DagCbor(body2)
        fx.clock.t = 200
        assertTrue(policy.onInvalidationReplace(invalidation("did:key:peer", cid2, body2)))

        val served = policy.get("did:key:peer")
        assertNotNull(served)
        assertTrue(body2.contentEquals(served.canonicalBytes))
        assertEquals(cid2, fx.dao.getProfileByDid("did:key:peer")?.profileCid)
    }

    @Test
    fun onInvalidationReplace_noOpWhenNotCached() = runTest {
        val (policy, _) = buildPolicy()
        val body = byteArrayOf(2, 2, 2)
        assertFalse(policy.onInvalidationReplace(invalidation("did:key:absent", Cid.cidV1DagCbor(body), body)))
    }

    @Test
    fun onInvalidationReplace_noOpWhenContentDoesNotMatchCid() = runTest {
        val (policy, fx) = buildPolicy()
        policy.put(envelope(ownerDid = "did:key:peer"), fx.tokens.issue())
        // cid is not the content address of body -> integrity guard rejects.
        assertFalse(
            policy.onInvalidationReplace(
                invalidation("did:key:peer", "bafyreinotmatchingcontentaddressforunittest", byteArrayOf(9, 9, 9)),
            ),
        )
    }

    @Test
    fun onInvalidationReplace_noOpWhenSameCidAlreadyHeld() = runTest {
        val (policy, fx) = buildPolicy()
        val body = byteArrayOf(3, 3, 3)
        val cid = Cid.cidV1DagCbor(body)
        assertTrue(policy.put(envelope(ownerDid = "did:key:peer", cid = cid, body = body), fx.tokens.issue()))
        // Same cid -> nothing to replace.
        assertFalse(policy.onInvalidationReplace(invalidation("did:key:peer", cid, body)))
    }

    // --- Phase F: cacheHolder advertise on put -----------------------------

    @Test
    fun put_advertisesCacheHolderForTheCachedOwner() = runTest {
        val dao = FakeDiscoveryDao()
        val clock = MutableClock(0)
        val tokens = SessionInteractionTokens(ttlMs = 30_000L, clock = clock)
        val advertised = mutableListOf<String>()
        val policy = RelayPolicy(
            selfDid = { "did:key:self" },
            discoveryDao = dao,
            rateLimiter = IngestRateLimiter(1_000, 1_000_000, clock),
            sessionTokens = tokens,
            reputation = FixedCapacityScorer(20),
            encryption = FakeCacheEncryption(),
            clock = clock,
            advertiser = {
                object : CacheHolderAdvertiser {
                    override suspend fun advertise(targetDid: String) { advertised += targetDid }
                }
            },
        )
        assertTrue(policy.put(envelope(ownerDid = "did:key:peer"), tokens.issue()))
        assertEquals(listOf("did:key:peer"), advertised)
    }
}
