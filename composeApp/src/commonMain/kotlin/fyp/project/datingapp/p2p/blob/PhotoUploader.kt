package fyp.project.datingapp.p2p.blob

import fyp.project.datingapp.database.pds.dao.BlobDao
import fyp.project.datingapp.database.pds.entities.BlobEntity
import fyp.project.datingapp.records.BlobRef
import fyp.project.datingapp.records.canonical.Cid

/** A photo just stored on this device: its profile [ref] plus the local [filePath] (for preview). */
data class UploadedPhoto(val ref: BlobRef, val filePath: String)

/**
 * Stores a picked image as a content-addressed blob (E: photo upload): computes the raw-leaf
 * CID over the bytes (so a fetcher can verify integrity, same scheme as the blob fetch path),
 * writes the bytes to the on-device [BlobStore], records the [BlobEntity], and returns the
 * [BlobRef] to attach to the profile (+ the local path for an immediate preview).
 */
class PhotoUploader(
    private val blobStore: BlobStore,
    private val blobDao: BlobDao,
) {
    suspend fun upload(bytes: ByteArray, mimeType: String): UploadedPhoto {
        val cid = Cid.cidV1Raw(bytes)
        val filePath = blobStore.write(cid, bytes)
        val size = bytes.size.toLong()
        blobDao.upsertBlob(BlobEntity(cid = cid, mimeType = mimeType, size = size, filePath = filePath))
        return UploadedPhoto(BlobRef(ref = cid, mimeType = mimeType, size = size), filePath)
    }
}
