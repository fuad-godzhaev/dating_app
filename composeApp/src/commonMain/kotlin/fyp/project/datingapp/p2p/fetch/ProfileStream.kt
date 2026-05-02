package fyp.project.datingapp.p2p.fetch

import fyp.project.datingapp.p2p.discovery.PeerContact
import fyp.project.datingapp.p2p.transport.LIBP2P_MAX_FRAME
import fyp.project.datingapp.p2p.transport.Libp2pStream
import fyp.project.datingapp.p2p.transport.Libp2pTransport
import fyp.project.datingapp.p2p.transport.wire.ProfileFetchRequest
import fyp.project.datingapp.p2p.transport.wire.ProfileFetchResponse
import fyp.project.datingapp.records.canonical.decodeProfileFetchResponse
import fyp.project.datingapp.records.canonical.encodeProfileFetchRequest

/** libp2p stream protocol for profile fetch (replaces the design doc's legacy `/dateable`). */
const val PROFILE_PROTOCOL_ID: String = "/datingapp/profile/1.0.0"

/**
 * One round-trip on [PROFILE_PROTOCOL_ID]: dial the peer, send a request frame,
 * read a response frame. Behind an interface so the cascade can be unit-tested
 * without the (expect-class, un-fakeable) [Libp2pTransport].
 */
interface ProfileStreamClient {
    /** Request from a known [PeerContact] (owner, learned from discovery). */
    suspend fun request(contact: PeerContact, request: ProfileFetchRequest): ProfileFetchResponse?

    /**
     * Request from a bare peerId (a cacheHolder learned from DHT provider records,
     * Phase F). No multiaddr is supplied: `dhtFindProviders` already seeded the
     * peerstore with the holder's addrs, so `openStream` can auto-dial.
     */
    suspend fun requestFromPeerId(peerId: String, request: ProfileFetchRequest): ProfileFetchResponse?
}

/** Real client over the go-libp2p host. */
class Libp2pProfileStreamClient(private val transport: Libp2pTransport) : ProfileStreamClient {
    override suspend fun request(contact: PeerContact, request: ProfileFetchRequest): ProfileFetchResponse? {
        // Best-effort dial via the advertised multiaddr (no-op if already connected),
        // then open the stream by peerId.
        contact.multiaddrs.firstOrNull()?.let { runCatching { transport.connect(it) } }
        return roundTrip(contact.peerId, request)
    }

    override suspend fun requestFromPeerId(peerId: String, request: ProfileFetchRequest): ProfileFetchResponse? =
        roundTrip(peerId, request)

    private suspend fun roundTrip(peerId: String, request: ProfileFetchRequest): ProfileFetchResponse? {
        val stream = transport.openStream(peerId, PROFILE_PROTOCOL_ID)
        return try {
            stream.writeFrame(encodeProfileFetchRequest(request))
            decodeProfileFetchResponse(stream.readFrame())
        } finally {
            runCatching { stream.close() }
        }
    }
}

// ---- length-prefixed framing (shared by client + owner-side handler) ----------
// 4-byte big-endian length header + payload. libp2p stream reads can return short,
// so reads loop until the full frame is in hand.

internal suspend fun Libp2pStream.writeFrame(payload: ByteArray) {
    val n = payload.size
    val framed = ByteArray(4 + n)
    framed[0] = (n ushr 24).toByte()
    framed[1] = (n ushr 16).toByte()
    framed[2] = (n ushr 8).toByte()
    framed[3] = n.toByte()
    payload.copyInto(framed, 4)
    writeBytes(framed)
}

internal suspend fun Libp2pStream.readFrame(maxLen: Int = LIBP2P_MAX_FRAME): ByteArray {
    val header = readExactly(4)
    val len = ((header[0].toInt() and 0xFF) shl 24) or
        ((header[1].toInt() and 0xFF) shl 16) or
        ((header[2].toInt() and 0xFF) shl 8) or
        (header[3].toInt() and 0xFF)
    require(len in 0..maxLen) { "profile frame length $len out of bounds (max $maxLen)" }
    return readExactly(len)
}

private suspend fun Libp2pStream.readExactly(n: Int): ByteArray {
    if (n == 0) return ByteArray(0)
    val out = ByteArray(n)
    var off = 0
    while (off < n) {
        val chunk = readBytes(n - off)
        if (chunk.isEmpty()) error("stream EOF after $off/$n bytes")
        chunk.copyInto(out, off)
        off += chunk.size
    }
    return out
}
