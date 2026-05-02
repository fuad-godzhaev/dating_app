package fyp.project.datingapp.p2p.blob

import fyp.project.datingapp.p2p.transport.wire.BlobFetchRequest
import fyp.project.datingapp.p2p.transport.wire.BlobFetchResponse
import fyp.project.datingapp.records.canonical.decodeBlobFetchRequest
import fyp.project.datingapp.records.canonical.decodeBlobFetchResponse
import fyp.project.datingapp.records.canonical.encodeBlobFetchRequest
import fyp.project.datingapp.records.canonical.encodeBlobFetchResponse
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BlobFetchCanonicalTest {

    @Test fun request_roundTrip() {
        val req = BlobFetchRequest(cid = "bafkreiexampleblobcid")
        val back = decodeBlobFetchRequest(encodeBlobFetchRequest(req))
        assertEquals(req.type, back.type)
        assertEquals(req.cid, back.cid)
    }

    @Test fun response_withBytes_roundTrip() {
        val resp = BlobFetchResponse(
            cid = "bafkreiexampleblobcid",
            bytes = byteArrayOf(1, 2, 3, 4, 5),
            mimeType = "image/jpeg",
        )
        val back = decodeBlobFetchResponse(encodeBlobFetchResponse(resp))
        assertEquals(resp.cid, back.cid)
        assertContentEquals(resp.bytes, back.bytes)
        assertEquals(resp.mimeType, back.mimeType)
        assertNull(back.error)
    }

    @Test fun response_errorOnly_roundTrip() {
        val resp = BlobFetchResponse(cid = "bafkreimissing", error = "not_found")
        val back = decodeBlobFetchResponse(encodeBlobFetchResponse(resp))
        assertEquals(resp.cid, back.cid)
        assertNull(back.bytes)
        assertEquals("not_found", back.error)
    }

    @Test fun encoding_isDeterministic() {
        val resp = BlobFetchResponse(cid = "bafkreix", bytes = byteArrayOf(7, 7), mimeType = "image/png")
        assertContentEquals(encodeBlobFetchResponse(resp), encodeBlobFetchResponse(resp))
    }
}
