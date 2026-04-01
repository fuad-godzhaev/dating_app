package fyp.project.datingapp.p2p.transport.wire

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Messages exchanged on the `/dateable/profile/1.0.0` libp2p stream protocol.
 * The requester sends a [ProfileFetchRequest]; the responder (owner or a
 * cacheHolder) returns a [ProfileFetchResponse] containing the signed
 * envelope. The requester verifies the signature against the owner's public
 * key before trusting the body.
 *
 * `ifNotCid` lets a client elide the bulk payload when the responder already
 * has the same version the client does (useful when polling an owner for
 * refreshes while the client already has a recent cached copy).
 */
@Serializable
data class ProfileFetchRequest(
    @SerialName($$"$type") val type: String = "fyp.project.datingapp.p2p.profileRequest",
    val targetDid: String,
    val ifNotCid: String? = null,
)

@Serializable
data class ProfileFetchResponse(
    @SerialName($$"$type") val type: String = "fyp.project.datingapp.p2p.profileResponse",
    val targetDid: String,
    val record: SignedEnvelope? = null,
    val cidMatch: Boolean = false,
    /** One of: "not_found", "rate_limited", "unauthorized". Null on success. */
    val error: String? = null,
)
