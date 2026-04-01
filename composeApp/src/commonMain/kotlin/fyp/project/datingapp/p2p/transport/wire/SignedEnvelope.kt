package fyp.project.datingapp.p2p.transport.wire

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Byte-exact carrier for an owner-signed record as it moves across the
 * network. The [canonicalBytes] field holds the DAG-CBOR encoding of the
 * record at the moment the owner signed it and MUST NOT be re-encoded by
 * intermediate relays — the signature only verifies against the original
 * byte sequence.
 *
 * A relay that receives a [SignedEnvelope] and wants to serve it to a
 * downstream peer simply copies [canonicalBytes] and [signature] out
 * unchanged. Re-serializing via kotlinx-serialization on any leg (encode ↔
 * decode ↔ encode) is forbidden by the byte-preservation rule spelled out
 * in `p2p-subsystem-design.md` §10.
 */
@Serializable
data class SignedEnvelope(
    @SerialName($$"$type") val type: String = "fyp.project.datingapp.p2p.envelope",
    val collection: String,
    val rkey: String,
    val ownerDid: String,
    val cid: String,
    val canonicalBytes: ByteArray,
    val signature: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SignedEnvelope) return false
        return collection == other.collection &&
                rkey == other.rkey &&
                ownerDid == other.ownerDid &&
                cid == other.cid &&
                canonicalBytes.contentEquals(other.canonicalBytes) &&
                signature.contentEquals(other.signature)
    }

    override fun hashCode(): Int {
        var h = collection.hashCode()
        h = 31 * h + rkey.hashCode()
        h = 31 * h + ownerDid.hashCode()
        h = 31 * h + cid.hashCode()
        h = 31 * h + canonicalBytes.contentHashCode()
        h = 31 * h + signature.contentHashCode()
        return h
    }
}
