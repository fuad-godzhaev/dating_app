package fyp.project.datingapp.records.canonical

import fyp.project.datingapp.p2p.transport.wire.AgeRange
import fyp.project.datingapp.p2p.transport.wire.PresenceRecord
import fyp.project.datingapp.p2p.transport.wire.ProfileFetchRequest
import fyp.project.datingapp.p2p.transport.wire.ProfileFetchResponse
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
    put("signalPreKeyBundle", CborValue.CBytes(signalPreKeyBundle))
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
        signalPreKeyBundle = m.getValue("signalPreKeyBundle").asBytes(),
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
