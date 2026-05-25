package com.aura.p2p.fetch

import com.aura.domain.auth.SignatureVerifier
import com.aura.p2p.discovery.PeerContact
import com.aura.p2p.discovery.PeerDirectory
import com.aura.p2p.relay.EpochClock
import com.aura.p2p.relay.FakeDiscoveryDao
import com.aura.p2p.transport.wire.ProfileFetchRequest
import com.aura.p2p.transport.wire.ProfileFetchResponse
import com.aura.p2p.transport.wire.SignedEnvelope
import com.aura.records.UserProfile
import com.aura.records.canonical.Cid
import com.aura.records.canonical.encodeCanonical
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CascadingProfileFetcherTest {

    // Valid P-256 did:key so ProfileEnvelopeVerifier.p256FromDidKey() succeeds.
    private val ownerDid = "did:key:zDnaembgSGUhZULN2Caob4HLJPaxBh92N7rtH21TErzqf8HQo"
    private val selfDid = "did:key:zDnaeSelfTestAccount000000000000000000000000000"

    private val profileBytes = encodeCanonical(
        UserProfile(
            did = ownerDid,
            displayName = "Owner",
            bio = "hi",
            age = 30,
            signingKey = byteArrayOf(1, 2, 3),
            interests = listOf("books"),
            createdAt = "2026-04-12T10:00:00Z",
        ),
    )
    private val profileCid = Cid.cidV1DagCbor(profileBytes)

    private fun goodEnvelope() = SignedEnvelope(
        collection = "com.aura.records.profile",
        rkey = "self",
        ownerDid = ownerDid,
        cid = profileCid,
        canonicalBytes = profileBytes,
        signature = byteArrayOf(7, 7, 7),
    )

    private val clock = EpochClock { 1_000_000L }

    private class FakeStreamClient(
        var response: ProfileFetchResponse?,
        var holderResponse: ProfileFetchResponse? = null,
    ) : ProfileStreamClient {
        var calls = 0
        var holderCalls = 0
        override suspend fun request(contact: PeerContact, request: ProfileFetchRequest): ProfileFetchResponse? {
            calls++
            return response
        }
        override suspend fun requestFromPeerId(peerId: String, request: ProfileFetchRequest): ProfileFetchResponse? {
            holderCalls++
            return holderResponse
        }
    }

    private class FakeHolderLocator(private val peerIds: List<String>) : HolderLocator {
        override fun findHolders(targetDid: String): Flow<String> = peerIds.asFlow()
    }

    private class FakeVerifier(var result: Boolean) : SignatureVerifier {
        override fun verify(publicKey: ByteArray, data: ByteArray, signature: ByteArray, algorithm: String) = result
    }

    private fun fetcher(
        dao: FakeDiscoveryDao,
        directory: PeerDirectory,
        client: ProfileStreamClient,
        verifier: SignatureVerifier,
        own: SignedEnvelope? = null,
        holderLocator: HolderLocator? = null,
    ) = CascadingProfileFetcher(
        selfDid = { selfDid },
        ownEnvelope = { own },
        cache = dao,
        peerDirectory = directory,
        streamClient = client,
        verifier = verifier,
        clock = clock,
        holderLocator = holderLocator,
    )

    @Test fun ownPds_returnsOwnEnvelopeWithoutNetwork() = runTest {
        val client = FakeStreamClient(null)
        val own = goodEnvelope().copy(ownerDid = selfDid)
        val f = fetcher(FakeDiscoveryDao(), PeerDirectory(), client, FakeVerifier(false), own = own)
        val got = f.fetchSigned(selfDid)
        assertEquals(own.cid, got?.cid)
        assertEquals(0, client.calls, "own PDS must not hit the network")
    }

    @Test fun directP2p_verifiedHit_returnedAndCached_secondFetchIsCacheHit() = runTest {
        val dao = FakeDiscoveryDao()
        val directory = PeerDirectory().apply { record(ownerDid, "12D3KooWpeer", listOf("/ip4/10.0.0.2/tcp/4001/p2p/12D3KooWpeer")) }
        val client = FakeStreamClient(ProfileFetchResponse(targetDid = ownerDid, record = goodEnvelope()))
        val f = fetcher(dao, directory, client, FakeVerifier(true))

        val first = f.fetchSigned(ownerDid, expectedCid = profileCid)
        assertEquals(profileCid, first?.cid)
        assertEquals(1, client.calls)
        assertTrue(dao.getProfileByDid(ownerDid) != null, "verified hit must be cached")

        // Second fetch should be served from the local cache, not the network.
        val second = f.fetchSigned(ownerDid, expectedCid = profileCid)
        assertEquals(profileCid, second?.cid)
        assertEquals(1, client.calls, "second fetch must hit the local cache")
    }

    @Test fun directP2p_forgedEnvelope_rejected_andNotCached() = runTest {
        val dao = FakeDiscoveryDao()
        val directory = PeerDirectory().apply { record(ownerDid, "12D3KooWpeer", listOf("/ip4/10.0.0.2/tcp/4001/p2p/12D3KooWpeer")) }
        val client = FakeStreamClient(ProfileFetchResponse(targetDid = ownerDid, record = goodEnvelope()))
        val f = fetcher(dao, directory, client, FakeVerifier(false)) // signature fails

        assertNull(f.fetchSigned(ownerDid, expectedCid = profileCid))
        assertNull(dao.getProfileByDid(ownerDid), "a forged envelope must not be cached")
    }

    @Test fun cidMismatch_rejectsDirectHit() = runTest {
        val directory = PeerDirectory().apply { record(ownerDid, "12D3KooWpeer", listOf("/ip4/10.0.0.2/tcp/4001/p2p/12D3KooWpeer")) }
        val client = FakeStreamClient(ProfileFetchResponse(targetDid = ownerDid, record = goodEnvelope()))
        val f = fetcher(FakeDiscoveryDao(), directory, client, FakeVerifier(true))
        // expectedCid differs from the envelope's cid -> verification gate fails.
        assertNull(f.fetchSigned(ownerDid, expectedCid = "bafyreiwrongcidwrongcidwrongcidwrongcidwrongcid"))
    }

    @Test fun noPeerKnown_returnsNull() = runTest {
        val client = FakeStreamClient(ProfileFetchResponse(targetDid = ownerDid, record = goodEnvelope()))
        val f = fetcher(FakeDiscoveryDao(), PeerDirectory(), client, FakeVerifier(true))
        // No PeerDirectory entry and no holder locator -> both network steps skipped.
        assertNull(f.fetchSigned(ownerDid, expectedCid = profileCid))
        assertEquals(0, client.calls)
    }

    // --- Phase F: cacheHolder fallback (step 4) -----------------------------

    @Test fun holderFallback_servesVerifiedEnvelopeWhenOwnerOffline() = runTest {
        val dao = FakeDiscoveryDao()
        // Owner offline: no PeerDirectory entry -> step 3 skipped, fall to holders.
        val client = FakeStreamClient(
            response = null,
            holderResponse = ProfileFetchResponse(targetDid = ownerDid, record = goodEnvelope()),
        )
        val locator = FakeHolderLocator(listOf("12D3KooWholder"))
        val f = fetcher(dao, PeerDirectory(), client, FakeVerifier(true), holderLocator = locator)

        val got = f.fetchSigned(ownerDid, expectedCid = profileCid)
        assertEquals(profileCid, got?.cid)
        assertEquals(0, client.calls, "owner direct step must be skipped")
        assertEquals(1, client.holderCalls, "first verified holder hit wins")
        assertTrue(dao.getProfileByDid(ownerDid) != null, "holder hit must be cached")
    }

    @Test fun holderFallback_rejectsForgedEnvelopeAndDoesNotCache() = runTest {
        val dao = FakeDiscoveryDao()
        val client = FakeStreamClient(
            response = null,
            holderResponse = ProfileFetchResponse(targetDid = ownerDid, record = goodEnvelope()),
        )
        val locator = FakeHolderLocator(listOf("12D3KooWholder1", "12D3KooWholder2"))
        val f = fetcher(dao, PeerDirectory(), client, FakeVerifier(false), holderLocator = locator)

        // Verifier rejects every holder's envelope -> no result, nothing cached.
        assertNull(f.fetchSigned(ownerDid, expectedCid = profileCid))
        assertNull(dao.getProfileByDid(ownerDid))
        assertEquals(2, client.holderCalls, "both holders tried before giving up")
    }
}
