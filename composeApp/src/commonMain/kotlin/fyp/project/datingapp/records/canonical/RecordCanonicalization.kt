package fyp.project.datingapp.records.canonical

import fyp.project.datingapp.p2p.transport.wire.AgeRange
import fyp.project.datingapp.p2p.transport.wire.BlobFetchRequest
import fyp.project.datingapp.p2p.transport.wire.BlobFetchResponse
import fyp.project.datingapp.p2p.transport.wire.MailboxRequest
import fyp.project.datingapp.p2p.transport.wire.MailboxResponse
import fyp.project.datingapp.p2p.transport.wire.MessageEnvelope
import fyp.project.datingapp.p2p.transport.wire.PresenceRecord
import fyp.project.datingapp.p2p.transport.wire.ProfileFetchRequest
import fyp.project.datingapp.p2p.transport.wire.ProfileFetchResponse
import fyp.project.datingapp.p2p.transport.wire.ProfileInvalidation
import fyp.project.datingapp.p2p.transport.wire.SignedEnvelope
import fyp.project.datingapp.records.BlobRef
import fyp.project.datingapp.records.Geolocation
import fyp.project.datingapp.records.Like
import fyp.project.datingapp.records.Match
import fyp.project.datingapp.records.Message
import fyp.project.datingapp.records.UserProfile

/**
 * Record ↔ [CborValue] mappers used at sign and verify time.
 *
 * The shape of each map mirrors the `@Serializable` data class field names
 * one-to-one. Absent / null optional fields are *omitted* from the map (rather
 * than emitted as `null`) so that a record round-tripping through optional
 * defaults never changes its signed bytes. This matches how DAG-CBOR encodes
 * sparse structures and matches the JSON-with-defaults convention used in
 * the lexicons.
 *
 * Lat/long are intentionally encoded as text strings. Doubles / floats in
 * DAG-CBOR require deterministic float canonicalization (shortest
 * representation, NaN normalization) which we'd rather avoid for now; the
 * extra bytes from text encoding are negligible for signed profile payloads.
 */

// ---- encode -----------------------------------------------------------------

fun UserProfile.toCborValue(): CborValue = CborValue.CMap(buildMap {
    put("\$type", CborValue.CString(type))
    put("did", CborValue.CString(did))
    put("displayName", CborValue.CString(displayName))
    put("bio", CborValue.CString(bio))
    put("age", CborValue.CInt(age.toLong()))
    avatar?.let { put("avatar", it.toCborValue()) }
    photos?.let { put("photos", CborValue.CArray(it.map { p -> p.toCborValue() })) }
    put("signingKey", CborValue.CBytes(signingKey))
    put("interests", CborValue.CArray(interests.map { CborValue.CString(it) }))
    location?.let { put("location", it.toCborValue()) }
    put("createdAt", CborValue.CString(createdAt))
})

fun BlobRef.toCborValue(): CborValue = CborValue.CMap(buildMap {
    put("\$type", CborValue.CString(type))
    put("ref", CborValue.CString(ref))
    put("mimeType", CborValue.CString(mimeType))
    put("size", CborValue.CInt(this@toCborValue.size))
})

fun Geolocation.toCborValue(): CborValue = CborValue.CMap(buildMap {
    put("latitude", CborValue.CString(latitude.toString()))
    put("longitude", CborValue.CString(longitude.toString()))
    geohash?.let { put("geohash", CborValue.CString(it)) }
})

fun Like.toCborValue(): CborValue = CborValue.CMap(buildMap {
    put("\$type", CborValue.CString(type))
    put("subject", CborValue.CString(subject))
    put("createdAt", CborValue.CString(createdAt))
})

fun Match.toCborValue(): CborValue = CborValue.CMap(buildMap {
    put("\$type", CborValue.CString(type))
    put("subject", CborValue.CString(subject))
    put("createdAt", CborValue.CString(createdAt))
})

fun Message.toCborValue(): CborValue = CborValue.CMap(buildMap {
    put("\$type", CborValue.CString(type))
    put("recipient", CborValue.CString(recipient))
    put("cipherText", CborValue.CBytes(cipherText))
    put("createdAt", CborValue.CString(createdAt))
})

fun AgeRange.toCborValue(): CborValue = CborValue.CMap(buildMap {
    put("min", CborValue.CInt(min.toLong()))
    put("max", CborValue.CInt(max.toLong()))
})

/**
 * The **signable** projection of a [PresenceRecord]: every field except
 * [PresenceRecord.signature]. This is the byte sequence the owner signs and
 * that peers re-derive to verify (see [encodeCanonical] / [encodePresenceWire]).
 */
fun PresenceRecord.toSignableCborValue(): CborValue = CborValue.CMap(buildMap {
    put("\$type", CborValue.CString(type))
    put("did", CborValue.CString(did))
    put("peerId", CborValue.CString(peerId))
    put("multiaddrs", CborValue.CArray(multiaddrs.map { CborValue.CString(it) }))
    put("profileCid", CborValue.CString(profileCid))
    put("geohash", CborValue.CString(geohash))
    put("interests", CborValue.CArray(interests.map { CborValue.CString(it) }))
    put("gender", CborValue.CString(gender))
    put("lookingFor", CborValue.CArray(lookingFor.map { CborValue.CString(it) }))
    put("ageRange", ageRange.toCborValue())
    put("announcedAt", CborValue.CString(announcedAt))
    put("ttl", CborValue.CInt(ttl))
})

fun encodeCanonical(profile: UserProfile): ByteArray =
    CanonicalEncoder.encode(profile.toCborValue())

fun encodeCanonical(blob: BlobRef): ByteArray =
    CanonicalEncoder.encode(blob.toCborValue())

fun encodeCanonical(like: Like): ByteArray =
    CanonicalEncoder.encode(like.toCborValue())

fun encodeCanonical(match: Match): ByteArray =
    CanonicalEncoder.encode(match.toCborValue())

fun encodeCanonical(message: Message): ByteArray =
    CanonicalEncoder.encode(message.toCborValue())

/**
 * Canonical DAG-CBOR of a [PresenceRecord] **without** its signature — the exact
 * bytes the owner signs and that a verifier hashes. Deterministic: re-encoding
 * the same logical record always yields these bytes (within a wire version).
 */
fun encodeCanonical(presence: PresenceRecord): ByteArray =
    CanonicalEncoder.encode(presence.toSignableCborValue())

/**
 * Canonical DAG-CBOR of a [PresenceRecord] **with** its signature — the payload
 * published to the DHT value / GossipSub topic. A receiver decodes this via
 * [decodePresenceRecord], strips the signature, re-encodes via [encodeCanonical],
 * and verifies. Kept under the 1 KB §15.3 budget by carrying coarse metadata only.
 */
fun encodePresenceWire(presence: PresenceRecord): ByteArray {
    val signable = (presence.toSignableCborValue() as CborValue.CMap).v
    val withSig = buildMap<String, CborValue> {
        putAll(signable)
        put("signature", CborValue.CBytes(presence.signature))
    }
    return CanonicalEncoder.encode(CborValue.CMap(withSig))
}

// ---- decode -----------------------------------------------------------------

fun decodeUserProfile(bytes: ByteArray): UserProfile {
    val m = CanonicalDecoder.decode(bytes).asMap()
    return UserProfile(
        type = m["\$type"]?.asString() ?: "fyp.project.datingapp.records.profile",
        did = m.getValue("did").asString(),
        displayName = m.getValue("displayName").asString(),
        bio = m.getValue("bio").asString(),
        age = m.getValue("age").asInt(),
        avatar = m["avatar"]?.let(::blobRefFrom),
        photos = m["photos"]?.asArray()?.map(::blobRefFrom),
        signingKey = m.getValue("signingKey").asBytes(),
        interests = m.getValue("interests").asArray().map { it.asString() },
        location = m["location"]?.let(::geoFrom),
        createdAt = m.getValue("createdAt").asString(),
    )
}

/**
 * Decode a wire payload produced by [encodePresenceWire] back into a
 * [PresenceRecord]. A missing `signature` field decodes to an empty array (the
 * record then fails verification) rather than throwing, so a malformed peer
 * payload is dropped, not fatal.
 */
fun decodePresenceRecord(bytes: ByteArray): PresenceRecord {
    val m = CanonicalDecoder.decode(bytes).asMap()
    return PresenceRecord(
        type = m["\$type"]?.asString() ?: "fyp.project.datingapp.p2p.presence",
        did = m.getValue("did").asString(),
        peerId = m.getValue("peerId").asString(),
        multiaddrs = m.getValue("multiaddrs").asArray().map { it.asString() },
        profileCid = m.getValue("profileCid").asString(),
        geohash = m.getValue("geohash").asString(),
        interests = m.getValue("interests").asArray().map { it.asString() },
        gender = m.getValue("gender").asString(),
        lookingFor = m.getValue("lookingFor").asArray().map { it.asString() },
        ageRange = ageRangeFrom(m.getValue("ageRange")),
        announcedAt = m.getValue("announcedAt").asString(),
        ttl = m.getValue("ttl").asLong(),
        signature = m["signature"]?.asBytes() ?: ByteArray(0),
    )
}

private fun ageRangeFrom(v: CborValue): AgeRange {
    val m = v.asMap()
    return AgeRange(min = m.getValue("min").asInt(), max = m.getValue("max").asInt())
}

// ---- fetch wire messages (Phase D, /datingapp/profile/1.0.0) -----------------
// These are stream control messages, not signed records. They are encoded with
// the canonical encoder only for a deterministic, dependency-free wire form. The
// crucial invariant is that [SignedEnvelope.canonicalBytes] is carried as an
// opaque CBOR byte string and round-trips **verbatim** (never re-encoded), so the
// owner's signature still verifies after transit (§10 byte-preservation).

fun SignedEnvelope.toCborValue(): CborValue = CborValue.CMap(buildMap {
    put("\$type", CborValue.CString(type))
    put("collection", CborValue.CString(collection))
    put("rkey", CborValue.CString(rkey))
    put("ownerDid", CborValue.CString(ownerDid))
    put("cid", CborValue.CString(cid))
    put("canonicalBytes", CborValue.CBytes(canonicalBytes))
    put("signature", CborValue.CBytes(signature))
})

fun ProfileFetchRequest.toCborValue(): CborValue = CborValue.CMap(buildMap {
    put("\$type", CborValue.CString(type))
    put("targetDid", CborValue.CString(targetDid))
    ifNotCid?.let { put("ifNotCid", CborValue.CString(it)) }
})

fun ProfileFetchResponse.toCborValue(): CborValue = CborValue.CMap(buildMap {
    put("\$type", CborValue.CString(type))
    put("targetDid", CborValue.CString(targetDid))
    record?.let { put("record", it.toCborValue()) }
    put("cidMatch", CborValue.CBool(cidMatch))
    error?.let { put("error", CborValue.CString(it)) }
})

fun encodeCanonical(envelope: SignedEnvelope): ByteArray =
    CanonicalEncoder.encode(envelope.toCborValue())

fun encodeProfileFetchRequest(request: ProfileFetchRequest): ByteArray =
    CanonicalEncoder.encode(request.toCborValue())

fun encodeProfileFetchResponse(response: ProfileFetchResponse): ByteArray =
    CanonicalEncoder.encode(response.toCborValue())

fun decodeSignedEnvelope(bytes: ByteArray): SignedEnvelope =
    signedEnvelopeFrom(CanonicalDecoder.decode(bytes))

fun decodeProfileFetchRequest(bytes: ByteArray): ProfileFetchRequest {
    val m = CanonicalDecoder.decode(bytes).asMap()
    return ProfileFetchRequest(
        type = m["\$type"]?.asString() ?: "fyp.project.datingapp.p2p.profileRequest",
        targetDid = m.getValue("targetDid").asString(),
        ifNotCid = (m["ifNotCid"] as? CborValue.CString)?.v,
    )
}

fun decodeProfileFetchResponse(bytes: ByteArray): ProfileFetchResponse {
    val m = CanonicalDecoder.decode(bytes).asMap()
    return ProfileFetchResponse(
        type = m["\$type"]?.asString() ?: "fyp.project.datingapp.p2p.profileResponse",
        targetDid = m.getValue("targetDid").asString(),
        record = m["record"]?.let(::signedEnvelopeFrom),
        cidMatch = (m["cidMatch"] as? CborValue.CBool)?.v ?: false,
        error = (m["error"] as? CborValue.CString)?.v,
    )
}

// ---- mailbox wire (Part 5 / M5, /datingapp/mailbox/1.0.0) -------------------
// Envelopes are embedded as their full wire bytes (CBytes), reusing the
// MessageEnvelope codec, so the holder carries them verbatim (ciphertext intact).

fun encodeMailboxRequest(request: MailboxRequest): ByteArray =
    CanonicalEncoder.encode(
        CborValue.CMap(buildMap {
            put("\$type", CborValue.CString(request.type))
            put("op", CborValue.CString(request.op))
            put("recipientDid", CborValue.CString(request.recipientDid))
            request.envelope?.let { put("envelope", CborValue.CBytes(encodeMessageEnvelopeWire(it))) }
        }),
    )

fun decodeMailboxRequest(bytes: ByteArray): MailboxRequest {
    val m = CanonicalDecoder.decode(bytes).asMap()
    return MailboxRequest(
        type = m["\$type"]?.asString() ?: "fyp.project.datingapp.p2p.mailboxRequest",
        op = m.getValue("op").asString(),
        recipientDid = m.getValue("recipientDid").asString(),
        envelope = (m["envelope"] as? CborValue.CBytes)?.v?.let { decodeMessageEnvelope(it) },
    )
}

fun encodeMailboxResponse(response: MailboxResponse): ByteArray =
    CanonicalEncoder.encode(
        CborValue.CMap(buildMap {
            put("\$type", CborValue.CString(response.type))
            put("ok", CborValue.CBool(response.ok))
            put("envelopes", CborValue.CArray(response.envelopes.map { CborValue.CBytes(encodeMessageEnvelopeWire(it)) }))
            response.error?.let { put("error", CborValue.CString(it)) }
        }),
    )

fun decodeMailboxResponse(bytes: ByteArray): MailboxResponse {
    val m = CanonicalDecoder.decode(bytes).asMap()
    return MailboxResponse(
        type = m["\$type"]?.asString() ?: "fyp.project.datingapp.p2p.mailboxResponse",
        ok = (m["ok"] as? CborValue.CBool)?.v ?: false,
        envelopes = (m["envelopes"] as? CborValue.CArray)?.v
            ?.mapNotNull { (it as? CborValue.CBytes)?.v?.let(::decodeMessageEnvelope) }
            ?: emptyList(),
        error = (m["error"] as? CborValue.CString)?.v,
    )
}

// ---- message envelope wire (Part 5, /datingapp/message|mailbox/1.0.0) -------
// The signature is the SENDER's P-256 signature over the signable projection
// (every field except `signature`), mirroring PresenceRecord. `ciphertext` is the
// opaque ECIES payload (eph pubkey || nonce || AES-GCM), round-trips verbatim.

fun MessageEnvelope.toSignableCborValue(): CborValue = CborValue.CMap(buildMap {
    put("\$type", CborValue.CString(type))
    put("senderDid", CborValue.CString(senderDid))
    put("recipientDid", CborValue.CString(recipientDid))
    put("msgId", CborValue.CString(msgId))
    put("ciphertext", CborValue.CBytes(ciphertext))
    put("messageType", CborValue.CInt(messageType.toLong()))
    put("sentAt", CborValue.CString(sentAt))
})

/** Signable bytes (no signature) — what the sender signs and a verifier re-derives. */
fun encodeCanonical(envelope: MessageEnvelope): ByteArray =
    CanonicalEncoder.encode(envelope.toSignableCborValue())

/** Full wire payload (signable projection + signature). */
fun encodeMessageEnvelopeWire(envelope: MessageEnvelope): ByteArray {
    val signable = (envelope.toSignableCborValue() as CborValue.CMap).v
    val withSig = buildMap<String, CborValue> {
        putAll(signable)
        put("signature", CborValue.CBytes(envelope.signature))
    }
    return CanonicalEncoder.encode(CborValue.CMap(withSig))
}

fun decodeMessageEnvelope(bytes: ByteArray): MessageEnvelope {
    val m = CanonicalDecoder.decode(bytes).asMap()
    return MessageEnvelope(
        type = m["\$type"]?.asString() ?: "fyp.project.datingapp.p2p.message",
        senderDid = m.getValue("senderDid").asString(),
        recipientDid = m.getValue("recipientDid").asString(),
        msgId = m.getValue("msgId").asString(),
        ciphertext = m.getValue("ciphertext").asBytes(),
        messageType = m.getValue("messageType").asInt(),
        sentAt = m.getValue("sentAt").asString(),
        signature = m["signature"]?.asBytes() ?: ByteArray(0),
    )
}

// ---- blob fetch wire (Part 3, /datingapp/blob/1.0.0) ------------------------
// Stream control messages for binary blobs. Integrity is the raw-leaf CID
// (Cid.cidV1Raw), not a signature; `bytes` round-trips verbatim as a CBOR byte
// string. A photo can approach the 2 MB DataValidator cap, so the blob stream
// reads with a larger frame bound than the 1 MiB profile default.

fun BlobFetchRequest.toCborValue(): CborValue = CborValue.CMap(buildMap {
    put("\$type", CborValue.CString(type))
    put("cid", CborValue.CString(cid))
})

fun BlobFetchResponse.toCborValue(): CborValue = CborValue.CMap(buildMap {
    put("\$type", CborValue.CString(type))
    put("cid", CborValue.CString(cid))
    bytes?.let { put("bytes", CborValue.CBytes(it)) }
    mimeType?.let { put("mimeType", CborValue.CString(it)) }
    error?.let { put("error", CborValue.CString(it)) }
})

fun encodeBlobFetchRequest(request: BlobFetchRequest): ByteArray =
    CanonicalEncoder.encode(request.toCborValue())

fun encodeBlobFetchResponse(response: BlobFetchResponse): ByteArray =
    CanonicalEncoder.encode(response.toCborValue())

fun decodeBlobFetchRequest(bytes: ByteArray): BlobFetchRequest {
    val m = CanonicalDecoder.decode(bytes).asMap()
    return BlobFetchRequest(
        type = m["\$type"]?.asString() ?: "fyp.project.datingapp.p2p.blobRequest",
        cid = m.getValue("cid").asString(),
    )
}

fun decodeBlobFetchResponse(bytes: ByteArray): BlobFetchResponse {
    val m = CanonicalDecoder.decode(bytes).asMap()
    return BlobFetchResponse(
        type = m["\$type"]?.asString() ?: "fyp.project.datingapp.p2p.blobResponse",
        cid = m.getValue("cid").asString(),
        bytes = (m["bytes"] as? CborValue.CBytes)?.v,
        mimeType = (m["mimeType"] as? CborValue.CString)?.v,
        error = (m["error"] as? CborValue.CString)?.v,
    )
}

// ---- invalidation wire (Phase E, fyp.project.datingapp.invalidate/v1/<did>) --
// A ProfileInvalidation is, on the wire, an out-of-band SignedEnvelope plus a
// publish timestamp. Its `signature` is the owner's per-record P-256 signature
// over `canonicalBytes` (the same value the profile RecordEntity carries), so a
// holder both authenticates the push and can serve the replaced envelope with a
// signature that still verifies. `canonicalBytes` round-trips verbatim (§10).

fun ProfileInvalidation.toCborValue(): CborValue = CborValue.CMap(buildMap {
    put("\$type", CborValue.CString(type))
    put("ownerDid", CborValue.CString(ownerDid))
    put("collection", CborValue.CString(collection))
    put("rkey", CborValue.CString(rkey))
    put("cid", CborValue.CString(cid))
    put("canonicalBytes", CborValue.CBytes(canonicalBytes))
    put("signature", CborValue.CBytes(signature))
    put("publishedAt", CborValue.CString(publishedAt))
})

fun encodeInvalidationWire(invalidation: ProfileInvalidation): ByteArray =
    CanonicalEncoder.encode(invalidation.toCborValue())

fun decodeProfileInvalidation(bytes: ByteArray): ProfileInvalidation {
    val m = CanonicalDecoder.decode(bytes).asMap()
    return ProfileInvalidation(
        type = m["\$type"]?.asString() ?: "fyp.project.datingapp.p2p.invalidate",
        ownerDid = m.getValue("ownerDid").asString(),
        collection = m.getValue("collection").asString(),
        rkey = m.getValue("rkey").asString(),
        cid = m.getValue("cid").asString(),
        canonicalBytes = m.getValue("canonicalBytes").asBytes(),
        signature = m.getValue("signature").asBytes(),
        publishedAt = m.getValue("publishedAt").asString(),
    )
}

private fun signedEnvelopeFrom(v: CborValue): SignedEnvelope {
    val m = v.asMap()
    return SignedEnvelope(
        type = m["\$type"]?.asString() ?: "fyp.project.datingapp.p2p.envelope",
        collection = m.getValue("collection").asString(),
        rkey = m.getValue("rkey").asString(),
        ownerDid = m.getValue("ownerDid").asString(),
        cid = m.getValue("cid").asString(),
        canonicalBytes = m.getValue("canonicalBytes").asBytes(),
        signature = m.getValue("signature").asBytes(),
    )
}

private fun blobRefFrom(v: CborValue): BlobRef {
    val m = v.asMap()
    return BlobRef(
        type = m["\$type"]?.asString() ?: "blob",
        ref = m.getValue("ref").asString(),
        mimeType = m.getValue("mimeType").asString(),
        size = m.getValue("size").asLong(),
    )
}

private fun geoFrom(v: CborValue): Geolocation {
    val m = v.asMap()
    return Geolocation(
        latitude = m.getValue("latitude").asString().toDouble(),
        longitude = m.getValue("longitude").asString().toDouble(),
        geohash = (m["geohash"] as? CborValue.CString)?.v,
    )
}

// ---- private CborValue projection helpers ----------------------------------

private fun CborValue.asMap(): Map<String, CborValue> =
    (this as? CborValue.CMap)?.v ?: error("expected CBOR map")

private fun CborValue.asString(): String =
    (this as? CborValue.CString)?.v ?: error("expected CBOR text string")

private fun CborValue.asInt(): Int = asLong().toInt()

private fun CborValue.asLong(): Long =
    (this as? CborValue.CInt)?.v ?: error("expected CBOR integer")

private fun CborValue.asBytes(): ByteArray =
    (this as? CborValue.CBytes)?.v ?: error("expected CBOR byte string")

private fun CborValue.asArray(): List<CborValue> =
    (this as? CborValue.CArray)?.v ?: error("expected CBOR array")
