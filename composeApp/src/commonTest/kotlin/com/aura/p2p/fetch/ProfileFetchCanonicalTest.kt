package com.aura.p2p.fetch

import com.aura.p2p.transport.wire.ProfileFetchRequest
import com.aura.p2p.transport.wire.ProfileFetchResponse
import com.aura.p2p.transport.wire.SignedEnvelope
import com.aura.records.canonical.decodeProfileFetchRequest
import com.aura.records.canonical.decodeProfileFetchResponse
import com.aura.records.canonical.decodeSignedEnvelope
import com.aura.records.canonical.encodeCanonical
import com.aura.records.canonical.encodeProfileFetchRequest
import com.aura.records.canonical.encodeProfileFetchResponse
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProfileFetchCanonicalTest {

    private fun envelope(sig: ByteArray = byteArrayOf(9, 8, 7)) = SignedEnvelope(
        collection = "com.aura.records.profile",
        rkey = "self",
        ownerDid = "did:key:zDnExampleOwner",
        cid = "bafyreiexamplecid",
        canonicalBytes = byteArrayOf(1, 2, 3, 4, 5, 0, -1, 127),
        signature = sig,
    )

    @Test fun envelope_roundTrip_preservesBytesVerbatim() {
        val e = envelope()
        val back = decodeSignedEnvelope(encodeCanonical(e))
        assertEquals(e.collection, back.collection)
        assertEquals(e.rkey, back.rkey)
        assertEquals(e.ownerDid, back.ownerDid)
        assertEquals(e.cid, back.cid)
        assertContentEquals(e.canonicalBytes, back.canonicalBytes)
        assertContentEquals(e.signature, back.signature)
        // Deterministic, and re-encoding the decoded form is byte-identical.
        assertContentEquals(encodeCanonical(e), encodeCanonical(back))
    }

    @Test fun request_roundTrip_withAndWithoutIfNotCid() {
        val r = ProfileFetchRequest(targetDid = "did:key:zDnX", ifNotCid = "bafyreiabc")
        val back = decodeProfileFetchRequest(encodeProfileFetchRequest(r))
        assertEquals(r.targetDid, back.targetDid)
        assertEquals(r.ifNotCid, back.ifNotCid)

        val r2 = ProfileFetchRequest(targetDid = "did:key:zDnY")
        val back2 = decodeProfileFetchRequest(encodeProfileFetchRequest(r2))
        assertEquals("did:key:zDnY", back2.targetDid)
        assertNull(back2.ifNotCid)
    }

    @Test fun response_withRecord_roundTrip() {
        val res = ProfileFetchResponse(targetDid = "did:key:zDnX", record = envelope(), cidMatch = false)
        val back = decodeProfileFetchResponse(encodeProfileFetchResponse(res))
        assertEquals(res.targetDid, back.targetDid)
        assertEquals(false, back.cidMatch)
        assertNull(back.error)
        assertContentEquals(envelope().canonicalBytes, back.record!!.canonicalBytes)
        assertContentEquals(envelope().signature, back.record!!.signature)
    }

    @Test fun response_error_roundTrip() {
        val res = ProfileFetchResponse(targetDid = "did:key:zDnX", error = "not_found")
        val back = decodeProfileFetchResponse(encodeProfileFetchResponse(res))
        assertNull(back.record)
        assertEquals("not_found", back.error)
    }

    @Test fun response_cidMatch_noBody() {
        val res = ProfileFetchResponse(targetDid = "did:key:zDnX", cidMatch = true)
        val back = decodeProfileFetchResponse(encodeProfileFetchResponse(res))
        assertTrue(back.cidMatch)
        assertNull(back.record)
    }
}
