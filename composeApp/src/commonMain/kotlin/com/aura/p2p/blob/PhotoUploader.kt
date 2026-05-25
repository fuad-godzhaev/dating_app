package com.aura.p2p.blob

import com.aura.database.pds.dao.BlobDao
import com.aura.database.pds.entities.BlobEntity
import com.aura.records.BlobRef
import com.aura.records.canonical.Cid

/** A photo just stored on this device: its profile [ref] plus the local [filePath] (for preview). */
data class UploadedPhoto(val ref: BlobRef, val filePath: String)

/**
 * Stores a picked image as a content-addressed blob (E: photo upload): first DOWNSCALES +
 * RECOMPRESSES the image via [ImageTranscoder] (phase-2 efficiency: full-res ~2 MB photos were the
 * binding holder-throughput + feed-browse + holder-data-plan cost), then computes the raw-leaf CID
 * over the transcoded bytes (so a fetcher can verify integrity, same scheme as the blob fetch
 * path), writes them to the on-device [BlobStore], records the [BlobEntity], and returns the
 * [BlobRef] to attach to the profile (+ the local path for an immediate preview).
 *
 * The CID is computed over the TRANSCODED bytes, so the on-wire/stored blob is the compressed one;
 * the transcoder always falls back to the original bytes on failure, so the path is never broken.
 */
class PhotoUploader(
    private val blobStore: BlobStore,
    private val blobDao: BlobDao,
    private val transcoder: ImageTranscoder,
) {
    suspend fun upload(bytes: ByteArray, mimeType: String): UploadedPhoto {
        val transcoded = transcoder.transcode(bytes, mimeType)
        val outBytes = transcoded.bytes
        val outMime = transcoded.mimeType
        val cid = Cid.cidV1Raw(outBytes)
        val filePath = blobStore.write(cid, outBytes)
        val size = outBytes.size.toLong()
        blobDao.upsertBlob(BlobEntity(cid = cid, mimeType = outMime, size = size, filePath = filePath))
        return UploadedPhoto(BlobRef(ref = cid, mimeType = outMime, size = size), filePath)
    }
}
