package fyp.project.datingapp.p2p.blob

import fyp.project.datingapp.database.pds.dao.BlobDao
import fyp.project.datingapp.p2p.fetch.readFrame
import fyp.project.datingapp.p2p.fetch.writeFrame
import fyp.project.datingapp.p2p.transport.Libp2pStream
import fyp.project.datingapp.p2p.transport.Libp2pTransport
import fyp.project.datingapp.p2p.transport.StreamHandler
import fyp.project.datingapp.p2p.transport.wire.BlobFetchRequest
import fyp.project.datingapp.p2p.transport.wire.BlobFetchResponse
import fyp.project.datingapp.records.canonical.decodeBlobFetchRequest
import fyp.project.datingapp.records.canonical.encodeBlobFetchResponse

/**
 * Owner-side handler for [BLOB_PROTOCOL_ID]: serves this device's own blobs from
 * [BlobDao] + [BlobStore]. Blobs are public, so there is no authorization; the
 * only failure is `not_found`. Mirrors `StreamProfileFetcher`.
 */
class StreamBlobFetcher(
    private val blobDao: BlobDao,
    private val blobStore: BlobStore,
) {
    fun register(transport: Libp2pTransport) {
        transport.registerStreamHandler(BLOB_PROTOCOL_ID, object : StreamHandler {
            override suspend fun handle(stream: Libp2pStream) {
                try {
                    val request = decodeBlobFetchRequest(stream.readFrame())
                    stream.writeFrame(encodeBlobFetchResponse(respond(request)))
                } catch (_: Throwable) {
                    // Malformed frame / dropped peer: nothing safe to send; just close.
                } finally {
                    runCatching { stream.close() }
                }
            }
        })
    }

    private suspend fun respond(request: BlobFetchRequest): BlobFetchResponse {
        val entity = blobDao.getBlob(request.cid)
            ?: return BlobFetchResponse(cid = request.cid, error = "not_found")
        val bytes = blobStore.read(entity.filePath)
            ?: return BlobFetchResponse(cid = request.cid, error = "not_found")
        return BlobFetchResponse(cid = request.cid, bytes = bytes, mimeType = entity.mimeType)
    }
}
