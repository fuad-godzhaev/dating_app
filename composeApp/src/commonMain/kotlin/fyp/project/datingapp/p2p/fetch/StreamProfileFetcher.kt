package fyp.project.datingapp.p2p.fetch

import fyp.project.datingapp.p2p.relay.RelayPolicy
import fyp.project.datingapp.p2p.transport.Libp2pStream
import fyp.project.datingapp.p2p.transport.Libp2pTransport
import fyp.project.datingapp.p2p.transport.StreamHandler
import fyp.project.datingapp.p2p.transport.wire.ProfileFetchRequest
import fyp.project.datingapp.p2p.transport.wire.ProfileFetchResponse
import fyp.project.datingapp.p2p.transport.wire.SignedEnvelope
import fyp.project.datingapp.records.canonical.decodeProfileFetchRequest
import fyp.project.datingapp.records.canonical.encodeProfileFetchResponse

/**
 * Owner-side handler for [PROFILE_PROTOCOL_ID]. Answers a [ProfileFetchRequest]
 * with this device's own signed profile envelope, or — when acting as a
 * cacheHolder for someone else — a relay-cached envelope via [RelayPolicy.get].
 *
 * Profiles are public-by-design, so there is no authorization: the only failure
 * surfaced is `not_found` (and, later, `rate_limited`). `ifNotCid` lets a
 * requester that already holds a CID skip the body (`cidMatch=true`, no record).
 */
class StreamProfileFetcher(
    private val selfDid: suspend () -> String?,
    private val ownEnvelope: suspend () -> SignedEnvelope?,
    private val relay: RelayPolicy? = null,
) {
    /** Register the handler on [transport]. Call once after the host starts. */
    fun register(transport: Libp2pTransport) {
        transport.registerStreamHandler(PROFILE_PROTOCOL_ID, object : StreamHandler {
            override suspend fun handle(stream: Libp2pStream) {
                try {
                    val request = decodeProfileFetchRequest(stream.readFrame())
                    stream.writeFrame(encodeProfileFetchResponse(respond(request)))
                } catch (_: Throwable) {
                    // Malformed frame / dropped peer: nothing safe to send; just close.
                } finally {
                    runCatching { stream.close() }
                }
            }
        })
    }

    private suspend fun respond(request: ProfileFetchRequest): ProfileFetchResponse {
        val target = request.targetDid
        if (target == selfDid()) {
            val env = ownEnvelope()
                ?: return ProfileFetchResponse(targetDid = target, error = "not_found")
            if (request.ifNotCid != null && request.ifNotCid == env.cid) {
                return ProfileFetchResponse(targetDid = target, cidMatch = true)
            }
            return ProfileFetchResponse(targetDid = target, record = env)
        }
        // Not us: serve a relay-cached envelope if this device holds one.
        val cached = relay?.get(target)
        return if (cached != null) {
            ProfileFetchResponse(targetDid = target, record = cached)
        } else {
            ProfileFetchResponse(targetDid = target, error = "not_found")
        }
    }
}
