package fyp.project.datingapp.database

import fyp.project.datingapp.DataValidator
import fyp.project.datingapp.DataValidatorResult
import fyp.project.datingapp.database.pds.entities.CommitEntity
import fyp.project.datingapp.database.pds.entities.RecordEntity
import fyp.project.datingapp.domain.auth.AuthRepository
import fyp.project.datingapp.records.UserProfile
import fyp.project.datingapp.records.canonical.CanonicalEncoder
import fyp.project.datingapp.records.canonical.Cid
import fyp.project.datingapp.records.canonical.CborValue
import fyp.project.datingapp.records.canonical.decodeUserProfile
import fyp.project.datingapp.records.canonical.encodeCanonical
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.Instant

//TODO: Split the logic into separate services
class RepositoryManager(
    private val db: AppDatabase,
    private val authRepository: AuthRepository,
) {

    // Records are canonical DAG-CBOR on the wire and at rest; see
    // fyp.project.datingapp.records.canonical.CanonicalEncoder. No JSON
    // serializer is kept here — keeping one around invites a "serialize with
    // json, sign with cbor" mismatch bug.

    //NSID constants
    object Collections {
        const val PROFILE = "fyp.project.datingapp.records.profile"
        const val LIKE = "fyp.project.datingapp.records.like"
        const val MATCH = "fyp.project.datingapp.records.match"
        const val MESSAGE = "fyp.project.datingapp.records.message"
    }

    //Create or update User Profile
    suspend fun putProfile(profile: UserProfile): Result<Unit> {
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

        // Step 4: Store the record
        val entity = RecordEntity(
            collection = Collections.PROFILE,
            rkey = "self",
            cborBytes = recordBytes,
            cid = cid,
            createdAt = timeZoneNow().toEpochMilliseconds()
        )
        db.recordDao().upsertRecord(entity)

        // Step 5: Update the signed commit
        updateCommit()

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

    //Check if user has a profile
    suspend fun hasProfile(userId: String): Boolean {
        return db.recordDao().getRecord(
            collection = Collections.PROFILE,
            rkey = "self"
        ) != null
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
    // TODO: Replace with proper base32-sort encoding per ATProto spec
    private fun generateTid(): String {
        val timestamp = timeZoneNow().toEpochMilliseconds() * 1000 // microseconds
        val clockId = Random.nextInt(1024)
        val combined = (timestamp shl 10) or clockId.toLong()

        // Encode as base36
        return combined.toString(32).padStart(13, '0').takeLast(13)
    }

    //TODO: make sure TID's always increment and are not reused or duplicated with the same collection in a given repo
    private fun timeZoneNow(): Instant {
        //val now: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val now: Instant = Clock.System.now()
        return now
    }

    class DataValidatorException(message: String) : Exception(message)
}