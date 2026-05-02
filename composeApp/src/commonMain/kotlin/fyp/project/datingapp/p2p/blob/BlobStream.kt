package fyp.project.datingapp.p2p.blob

import fyp.project.datingapp.p2p.discovery.PeerContact
import fyp.project.datingapp.p2p.fetch.readFrame
import fyp.project.datingapp.p2p.fetch.writeFrame
import fyp.project.datingapp.p2p.transport.Libp2pTransport
import fyp.project.datingapp.p2p.transport.wire.BlobFetchRequest
import fyp.project.datingapp.p2p.transport.wire.BlobFetchResponse
import fyp.project.datingapp.records.canonical.decodeBlobFetchResponse
import fyp.project.datingapp.records.canonical.encodeBlobFetchRequest

/** libp2p stream protocol for blob (photo) fetch. */
const val BLOB_PROTOCOL_ID: String = "/datingapp/blob/1.0.0"

/**
 * Max blob-stream frame: 4 MiB. The DataValidator caps a blob at 2 MB; the extra
 * headroom covers the CBOR envelope around the bytes. Larger than the 1 MiB
 * profile-stream default, so a single photo fits in one frame (no chunking in v1).
 */
const val BLOB_MAX_FRAME: Int = 4 * 1024 * 1024

/**
 * One round-trip on [BLOB_PROTOCOL_ID]: dial, send a [BlobFetchRequest], read a
 * [BlobFetchResponse]. Behind an interface so the cascade is unit-testable without
 * the (expect-class) transport. Mirrors `ProfileStreamClient`.
 */
interface BlobStreamClient {
    suspend fun request(contact: PeerContact, request: BlobFetchRequest): BlobFetchResponse?
}

/** Real client over the go-libp2p host. */
class Libp2pBlobStreamClient(private val transport: Libp2pTransport) : BlobStreamClient {
    override suspend fun request(contact: PeerContact, request: BlobFetchRequest): BlobFetchResponse? {
        contact.multiaddrs.firstOrNull()?.let { runCatching { transport.connect(it) } }
        val stream = transport.openStream(contact.peerId, BLOB_PROTOCOL_ID)
        return try {
            stream.writeFrame(encodeBlobFetchRequest(request))
            decodeBlobFetchResponse(stream.readFrame(BLOB_MAX_FRAME))
        } finally {
            runCatching { stream.close() }
        }
    }
}
