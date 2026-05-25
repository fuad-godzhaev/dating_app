package com.aura.p2p.blob

import com.aura.database.pds.dao.BlobDao
import com.aura.database.pds.entities.BlobEntity
import com.aura.p2p.discovery.PeerDirectory
import com.aura.p2p.transport.wire.BlobFetchRequest
import com.aura.records.canonical.Cid

/**
 * Resolves a blob CID (a profile photo) to a local file path, fetching it from the
 * network if it isn't cached. Integrity is content-addressed: a fetched blob is
 * accepted only if `Cid.cidV1Raw(bytes) == cid` (a peer can withhold but never
 * substitute). Mirrors the profile fetch cascade, minus signatures.
 */
interface BlobFetcher {
    /** Local file path for [blobCid], or null if it can't be resolved. */
    suspend fun fetch(profileDid: String, blobCid: String): String?
}

/**
 * Cascade: (1) local [BlobDao] + on-disk file; (2) direct P2P to the profile owner
 * (peer from [PeerDirectory]). cacheHolder fallback for blobs is deferred (owner-only
 * in v1; see MASTER-PLAN-EXECUTION-NOTES).
 */
class CascadingBlobFetcher(
    private val blobDao: BlobDao,
    private val blobStore: BlobStore,
    private val peerDirectory: PeerDirectory,
    private val streamClient: BlobStreamClient,
) : BlobFetcher {

    override suspend fun fetch(profileDid: String, blobCid: String): String? {
        // 1. Local: a cached row whose file is still on disk.
        blobDao.getBlob(blobCid)?.let { entity ->
            if (blobStore.exists(entity.filePath)) return entity.filePath
        }

        // 2. Direct P2P to the owner.
        val contact = peerDirectory.get(profileDid) ?: return null
        val response = runCatching {
            streamClient.request(contact, BlobFetchRequest(cid = blobCid))
        }.getOrNull() ?: return null
        val bytes = response.bytes ?: return null

        // Integrity gate: the bytes must hash to the requested raw-leaf CID.
        if (Cid.cidV1Raw(bytes) != blobCid) return null

        val path = blobStore.write(blobCid, bytes)
        blobDao.upsertBlob(
            BlobEntity(
                cid = blobCid,
                mimeType = response.mimeType ?: "image/jpeg",
                size = bytes.size.toLong(),
                filePath = path,
            ),
        )
        return path
    }
}
