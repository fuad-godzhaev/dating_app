package fyp.project.datingapp.p2p.transport.wire

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request one blob (a profile photo / avatar) by its raw-leaf CID over
 * `/datingapp/blob/1.0.0`. Blobs are opaque binary, not signed records: integrity
 * comes from the CID itself (`Cid.cidV1Raw(bytes) == cid`), so there is no
 * signature or owner field here.
 */
@Serializable
data class BlobFetchRequest(
    @SerialName($$"$type") val type: String = "fyp.project.datingapp.p2p.blobRequest",
    val cid: String,
)

/**
 * Response carrying the blob bytes, or an [error] string (e.g. "not_found") with
 * [bytes] null. The requester MUST verify `Cid.cidV1Raw(bytes) == cid` before
 * trusting [bytes] — a holder can withhold but not substitute content.
 */
@Serializable
data class BlobFetchResponse(
    @SerialName($$"$type") val type: String = "fyp.project.datingapp.p2p.blobResponse",
    val cid: String,
    val bytes: ByteArray? = null,
    val mimeType: String? = null,
    val error: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BlobFetchResponse) return false
        return type == other.type &&
            cid == other.cid &&
            (bytes?.contentEquals(other.bytes) ?: (other.bytes == null)) &&
            mimeType == other.mimeType &&
            error == other.error
    }

    override fun hashCode(): Int {
        var h = type.hashCode()
        h = 31 * h + cid.hashCode()
        h = 31 * h + (bytes?.contentHashCode() ?: 0)
        h = 31 * h + (mimeType?.hashCode() ?: 0)
        h = 31 * h + (error?.hashCode() ?: 0)
        return h
    }
}
