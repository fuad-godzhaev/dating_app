package com.aura.database

import com.aura.DataValidator
import com.aura.DataValidatorResult
import com.aura.database.pds.entities.CommitEntity
import com.aura.database.pds.entities.RecordEntity
import com.aura.domain.auth.AuthRepository
import com.aura.p2p.relay.GossipSubInvalidator
import com.aura.p2p.transport.wire.ProfileInvalidation
import com.aura.p2p.transport.wire.SignedEnvelope
import com.aura.records.Like
import com.aura.records.Match
import com.aura.records.UserProfile
import com.aura.records.canonical.CanonicalEncoder
import com.aura.records.canonical.Cid
import com.aura.records.canonical.CborValue
import com.aura.records.canonical.decodeUserProfile
import com.aura.records.canonical.encodeCanonical
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Instant

//TODO: Split the logic into separate services
class RepositoryManager(
    private val db: AppDatabase,
    private val authRepository: AuthRepository,
    // Phase E: nullable seam so the DB layer can announce a profile update over
    // GossipSub without a hard transport dependency (and so tests construct a
    // manager with no networking). Injected lazily via Koin getOrNull().
    private val invalidator: GossipSubInvalidator? = null,
) {

    // Records are canonical DAG-CBOR on the wire and at rest; see
    // com.aura.records.canonical.CanonicalEncoder. No JSON
    // serializer is kept here — keeping one around invites a "serialize with
    // json, sign with cbor" mismatch bug.

    //NSID constants
    object Collections {
        const val PROFILE = "com.aura.records.profile"
        const val LIKE = "com.aura.records.like"
        const val MATCH = "com.aura.records.match"
        const val MESSAGE = "com.aura.records.message"
    }

    //Create or update User Profile
    suspend fun putProfile(profile: UserProfile): Result<Unit> {
        // Messaging E2EE (ECIES) seals to the recipient's DID key directly, so the
        // profile carries no separate encryption prekey bundle.

        // Step 1: Validate against Lexicon schema
        val validation = DataValidator.validate(profile)
        if (validation is DataValidatorResult.Invalid) {
            return Result.failure(
                DataValidatorException(
                    "Profile validation failed: ${validation.errors.joinToString("; ")}"
                )
            )
        }

        // Step 2: Serialize to canonical DAG-CBOR bytes (exactly what's signed).
        val recordBytes = encodeCanonical(profile)

        // Step 3: Compute CIDv1 (dag-cbor + sha2-256 multihash, base32-lower).
        val cid = Cid.cidV1DagCbor(recordBytes)

        // Step 3b: Per-record P-256 signature over the canonical bytes, so a peer
        // can verify this profile standalone via the owner's did:key on fetch
        // (Phase D), independent of the MST commit signature.
        val recordSignature = authRepository.sign(recordBytes)

        // Step 4: Store the record
        val entity = RecordEntity(
            collection = Collections.PROFILE,
            rkey = "self",
            cborBytes = recordBytes,
            cid = cid,
            createdAt = timeZoneNow().toEpochMilliseconds(),
            signature = recordSignature,
        )
        db.recordDao().upsertRecord(entity)

        // Step 5: Update the signed commit
        updateCommit()

        // Step 6 (Phase E): announce the new version on this DID's invalidation
        // topic so peers caching an older copy refresh in place instead of waiting
        // out the TTL. Best-effort: a null invalidator (tests / no transport) or a
        // missing identity is a silent no-op. The invalidation reuses the record's
        // own CID + per-record signature, so a holder can serve the replacement
        // with a signature that still verifies.
        invalidator?.let { inv ->
            val did = authRepository.getDid()
            if (did != null) {
                val invalidation = ProfileInvalidation(
                    ownerDid = did,
                    collection = Collections.PROFILE,
                    rkey = "self",
                    cid = cid,
                    canonicalBytes = recordBytes,
                    signature = recordSignature,
                    publishedAt = timeZoneNow().toString(),
                )
                runCatching { inv.publish(invalidation) }
            }
        }

        return Result.success(Unit)
    }

    //Fetch users profile
    suspend fun getMyProfile(): UserProfile? {
        val entity = db.recordDao().getRecord(Collections.PROFILE, "self")
            ?: return null
        return decodeUserProfile(entity.cborBytes)
    }

    // CID of the stored profile record (CIDv1 dag-cbor) — what a PresenceRecord
    // advertises so a peer can fetch the full profile by content address (Phase D).
    suspend fun getMyProfileCid(): String? =
        db.recordDao().getRecord(Collections.PROFILE, "self")?.cid

    // The owner-signed envelope for this user's profile, served over the fetch
    // stream (Phase D). Returns null if there is no profile or it predates v4
    // per-record signing (re-save the profile to populate the signature).
    suspend fun getMyProfileEnvelope(): SignedEnvelope? {
        val entity = db.recordDao().getRecord(Collections.PROFILE, "self") ?: return null
        val signature = entity.signature ?: return null
        val did = authRepository.getDid() ?: return null
        return SignedEnvelope(
            collection = Collections.PROFILE,
            rkey = "self",
            ownerDid = did,
            cid = entity.cid,
            canonicalBytes = entity.cborBytes,
            signature = signature,
        )
    }

    // Local on-disk path for a stored blob [cid] (a profile photo) so the UI can
    // preview it, or null if this device doesn't hold the blob. Self photos are
    // always held (they were written here on upload); peer photos resolve once
    // the blob fetch lands.
    suspend fun blobFilePath(cid: String): String? =
        db.blobDao().getBlob(cid)?.filePath

    //Check if user has a profile
    suspend fun hasProfile(userId: String): Boolean {
        return db.recordDao().getRecord(
            collection = Collections.PROFILE,
            rkey = "self"
        ) != null
    }

    // ---- Likes / matches (E: core match loop) -------------------------------

    /**
     * Record an outgoing like for [targetDid] (signed, stored under the LIKE
     * collection keyed by the target DID, so a reciprocal check is a point lookup),
     * and return the [SignedEnvelope] to deliver to the target over the like stream.
     */
    suspend fun putLike(targetDid: String): Result<SignedEnvelope> {
        val myDid = authRepository.getDid()
            ?: return Result.failure(IllegalStateException("No identity — cannot like"))
        val like = Like(subject = targetDid, createdAt = timeZoneNow().toString())
        val bytes = encodeCanonical(like)
        val cid = Cid.cidV1DagCbor(bytes)
        val signature = authRepository.sign(bytes)
        db.recordDao().upsertRecord(
            RecordEntity(
                collection = Collections.LIKE,
                rkey = targetDid,
                cborBytes = bytes,
                cid = cid,
                createdAt = timeZoneNow().toEpochMilliseconds(),
                signature = signature,
            )
        )
        updateCommit()
        return Result.success(
            SignedEnvelope(
                collection = Collections.LIKE,
                rkey = targetDid,
                ownerDid = myDid,
                cid = cid,
                canonicalBytes = bytes,
                signature = signature,
            )
        )
    }

    /** True if this user has already liked [targetDid] (a reciprocal like ⇒ match). */
    suspend fun hasOutgoingLike(targetDid: String): Boolean =
        db.recordDao().getRecord(Collections.LIKE, targetDid) != null

    /** Record a mutual match with [peerDid] (signed, stored under the MATCH collection). */
    suspend fun putMatch(peerDid: String): Result<Unit> {
        val match = Match(subject = peerDid, createdAt = timeZoneNow().toString())
        val bytes = encodeCanonical(match)
        val cid = Cid.cidV1DagCbor(bytes)
        val signature = authRepository.sign(bytes)
        db.recordDao().upsertRecord(
            RecordEntity(
                collection = Collections.MATCH,
                rkey = peerDid,
                cborBytes = bytes,
                cid = cid,
                createdAt = timeZoneNow().toEpochMilliseconds(),
                signature = signature,
            )
        )
        updateCommit()
        return Result.success(Unit)
    }

    //-----Commit Management-----
    private suspend fun updateCommit() {
        val allRecords = db.recordDao().getAllRecords()
        // Canonical root: DAG-CBOR array of CID text strings, sorted ascending.
        // Hashing over canonical bytes avoids the "joinToString(\"\")" ambiguity
        // where "abc" + "d" and "ab" + "cd" would collide.
        val sortedCids = allRecords.map { it.cid }.sorted()
        val rootCborBytes = CanonicalEncoder.encode(
            CborValue.CArray(sortedCids.map { CborValue.CString(it) })
        )
        val rootHash = Cid.cidV1DagCbor(rootCborBytes)
        val rev = generateTid()
        val did = authRepository.getDid() ?: error("No identity — cannot commit")
        val commitData = "$did|$rev|$rootHash|3"
        val signature = authRepository.sign(commitData.encodeToByteArray())

        val commit = CommitEntity(
            did = did,
            rev = rev,
            rootMstCid = rootHash,
            signature = signature,
            version = 3
        )
        db.commitDao().upsertCommit(commit)
    }

    //-----Utility Functions-----
    // ATProto TID: a 13-char, base32-SORTABLE, strictly-monotonic record key. It encodes a
    // microsecond timestamp in the high bits + a 10-bit clock id in the low bits. The timestamp is
    // kept strictly increasing per process, so two records can never collide on an rkey - the old
    // impl used Kotlin `toString(32)` (not the sortable alphabet) and a random clock id with no
    // monotonicity, which could duplicate a TID for two records written in the same millisecond and
    // broke lexical == chronological ordering. (Addresses the two prior TODOs here.)
    private val tidAlphabet = "234567abcdefghijklmnopqrstuvwxyz" // base32-sortable
    private var lastTidMicros = 0L

    private fun generateTid(): String {
        var micros = timeZoneNow().toEpochMilliseconds() * 1000
        if (micros <= lastTidMicros) micros = lastTidMicros + 1 // strictly monotonic
        lastTidMicros = micros
        val combined = (micros shl 10) or Random.nextInt(1024).toLong()
        // 13 big-endian 5-bit groups over the sortable alphabet (covers the 63-bit value).
        val out = CharArray(13)
        var n = combined
        for (i in 12 downTo 0) { out[i] = tidAlphabet[(n and 0x1f).toInt()]; n = n ushr 5 }
        return out.concatToString()
    }

    private fun timeZoneNow(): Instant = Clock.System.now()

    class DataValidatorException(message: String) : Exception(message)
}