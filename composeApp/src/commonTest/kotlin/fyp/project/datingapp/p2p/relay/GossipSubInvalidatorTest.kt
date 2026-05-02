package fyp.project.datingapp.p2p.relay

import fyp.project.datingapp.domain.auth.SignatureVerifier
import fyp.project.datingapp.p2p.transport.GossipMessage
import fyp.project.datingapp.p2p.transport.wire.ProfileInvalidation
import fyp.project.datingapp.p2p.transport.wire.SignedEnvelope
import fyp.project.datingapp.records.canonical.Cid
import fyp.project.datingapp.records.canonical.encodeInvalidationWire
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Drives [DefaultGossipSubInvalidator] over a fake [GossipChannel]: a verified
 * invalidation replaces the cached row in place; a rejected one is ignored; and
 * publish writes the wire form to the owner's topic.
 */
class GossipSubInvalidatorTest {

    private val did = "did:key:zDnaembgSGUhZULN2Caob4HLJPaxBh92N7rtH21TErzqf8HQo"

    private class MutableClock(var t: Long = 0L) : EpochClock {
        override fun nowMs(): Long = t
    }

    private class AcceptingVerifier(private val result: Boolean = true) : SignatureVerifier {
        override fun verify(publicKey: ByteArray, data: ByteArray, signature: ByteArray, algorithm: String) = result
    }

    private class FakeGossipChannel : GossipChannel {
        private val flows = mutableMapOf<String, MutableSharedFlow<GossipMessage>>()
        val published = mutableListOf<Pair<String, ByteArray>>()
        val unsubscribed = mutableListOf<String>()

        private fun flow(topic: String) =
            flows.getOrPut(topic) { MutableSharedFlow(replay = 1, extraBufferCapacity = 16) }

        override fun subscribe(topic: String): Flow<GossipMessage> = flow(topic)
        override suspend fun unsubscribe(topic: String) { unsubscribed += topic }
        override suspend fun publish(topic: String, payload: ByteArray) { published += topic to payload }

        suspend fun deliver(topic: String, payload: ByteArray) =
            flow(topic).emit(GossipMessage(topic = topic, from = "peer", payload = payload))
    }

    private class Fixture(
        val dao: FakeDiscoveryDao,
        val policy: RelayPolicy,
        val tokens: SessionInteractionTokens,
        val clock: MutableClock,
    )

    private fun fixture(): Fixture {
        val dao = FakeDiscoveryDao()
        val clock = MutableClock(1_000)
        val tokens = SessionInteractionTokens(ttlMs = 30_000L, clock = clock)
        val policy = RelayPolicy(
            selfDid = { "did:key:self" },
            discoveryDao = dao,
            rateLimiter = IngestRateLimiter(1_000, 1_000_000, clock),
            sessionTokens = tokens,
            reputation = FixedCapacityScorer(20),
            encryption = FakeCacheEncryption(),
            clock = clock,
        )
        return Fixture(dao, policy, tokens, clock)
    }

    private fun envelope(cid: String, body: ByteArray) = SignedEnvelope(
        collection = "fyp.project.datingapp.profile",
        rkey = "self",
        ownerDid = did,
        cid = cid,
        canonicalBytes = body,
        signature = byteArrayOf(7, 7, 7),
    )

    private fun invalidation(cid: String, body: ByteArray) = ProfileInvalidation(
        ownerDid = did,
        collection = "fyp.project.datingapp.records.profile",
        rkey = "self",
        cid = cid,
        canonicalBytes = body,
        signature = byteArrayOf(8, 8, 8),
        publishedAt = "2026-05-23T12:00:00Z",
    )

    @Test
    fun verifiedInvalidation_replacesCachedRowInPlace() = runTest(UnconfinedTestDispatcher()) {
        val fx = fixture()
        val channel = FakeGossipChannel()
        val invalidator = DefaultGossipSubInvalidator(channel, AcceptingVerifier(true), fx.policy, backgroundScope)

        // Seed an initial relay-cached version.
        val body1 = byteArrayOf(1, 1, 1, 1)
        val cid1 = Cid.cidV1DagCbor(body1)
        assertTrue(fx.policy.put(envelope(cid1, body1), fx.tokens.issue()))
        assertEquals(cid1, fx.dao.getProfileByDid(did)?.profileCid)

        invalidator.subscribe(did)
        advanceUntilIdle()

        // Owner publishes a new version on the DID's topic.
        val body2 = byteArrayOf(2, 2, 2, 2, 2)
        val cid2 = Cid.cidV1DagCbor(body2)
        channel.deliver(InvalidationTopics.topic(did), encodeInvalidationWire(invalidation(cid2, body2)))
        advanceUntilIdle()

        // The cached row is now the new version, served straight from cache.
        assertEquals(cid2, fx.dao.getProfileByDid(did)?.profileCid)
        val served = fx.policy.get(did)
        assertNotNull(served)
        assertContentEquals(body2, served.canonicalBytes)
    }

    @Test
    fun rejectedInvalidation_leavesRowUnchanged() = runTest(UnconfinedTestDispatcher()) {
        val fx = fixture()
        val channel = FakeGossipChannel()
        val invalidator = DefaultGossipSubInvalidator(channel, AcceptingVerifier(false), fx.policy, backgroundScope)

        val body1 = byteArrayOf(1, 1, 1, 1)
        val cid1 = Cid.cidV1DagCbor(body1)
        fx.policy.put(envelope(cid1, body1), fx.tokens.issue())

        invalidator.subscribe(did)
        advanceUntilIdle()

        val body2 = byteArrayOf(2, 2, 2, 2, 2)
        val cid2 = Cid.cidV1DagCbor(body2)
        channel.deliver(InvalidationTopics.topic(did), encodeInvalidationWire(invalidation(cid2, body2)))
        advanceUntilIdle()

        // Signature rejected -> the row keeps its original version.
        assertEquals(cid1, fx.dao.getProfileByDid(did)?.profileCid)
    }

    @Test
    fun publish_writesWireFormToOwnerTopic() = runTest(UnconfinedTestDispatcher()) {
        val fx = fixture()
        val channel = FakeGossipChannel()
        val invalidator = DefaultGossipSubInvalidator(channel, AcceptingVerifier(true), fx.policy, backgroundScope)

        val body = byteArrayOf(5, 6, 7, 8)
        val inv = invalidation(Cid.cidV1DagCbor(body), body)
        invalidator.publish(inv)

        assertEquals(1, channel.published.size)
        assertEquals(InvalidationTopics.topic(did), channel.published[0].first)
        assertContentEquals(encodeInvalidationWire(inv), channel.published[0].second)
    }

    @Test
    fun unsubscribe_recordsTopic() = runTest(UnconfinedTestDispatcher()) {
        val fx = fixture()
        val channel = FakeGossipChannel()
        val invalidator = DefaultGossipSubInvalidator(channel, AcceptingVerifier(true), fx.policy, backgroundScope)

        invalidator.subscribe(did)
        advanceUntilIdle()
        invalidator.unsubscribe(did)

        assertTrue(channel.unsubscribed.contains(InvalidationTopics.topic(did)))
    }
}
