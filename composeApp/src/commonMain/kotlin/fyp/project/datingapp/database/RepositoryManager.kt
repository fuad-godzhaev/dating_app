package fyp.project.datingapp.database

import fyp.project.datingapp.DataValidator
import fyp.project.datingapp.DataValidatorResult
import fyp.project.datingapp.records.UserProfile
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.time.TimeSource

//TODO: Split the logic into separate services
class RepositoryManager(
    private val db: AppDatabase,
    private val identity: UserProfile  // Holds DID + signing key
) {

    // TODO: JSON serializer for debugging — CBOR would be used in production
    private val json = Json {
        // TODO: Pretty print for debugging (disable in production for smaller payloads)
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    //NSID constants
    object Collections {
        const val PROFILE = "fyp.project.datingapp.records.profile"
        //const val LIKE = "fyp.project.datingapp.records.like"
        //const val MATCH = "fyp.project.datingapp.records.match"
        const val MESSAGE = "fyp.project.datingapp.records.message"
    }

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

        // Step 2: Serialize to bytes
        // TODO: use DAG-CBOR. JSON is easier to debug.
        val recordBytes = json.encodeToString(profile).encodeToByteArray()

        // Step 3: Compute CID
        // TODO: Production: CIDv1 with dag-cbor codec and sha2-256 multihash.
        val cid = computeCid(recordBytes)

        val entity = RecordEntity(
            collection = Collections.PROFILE,
            rkey = "self",
            cborBytes = recordBytes,
            cid = cid,
            createdAt = TimeSource.Monotonic.markNow()
        )
        db.recordDao().upsertRecord(entity)

        // Step 5: Update the signed commit
        updateCommit()

        return Result.success(Unit)
    }

    suspend fun getMyProfile(): UserProfile? {
        val entity = db.recordDao().getRecord(Collections.PROFILE, "self")
            ?: return null
        return json.decodeFromString<UserProfile>(entity.cborBytes.toString())
    }

    //-----Commit Management-----
    private suspend fun updateCommit() {
        val allRecords = db.recordDao().getAllRecords()
        val sortedCids = allRecords.sortedBy { it.cid }
        val rootHash = computeCid(sortedCids.joinToString("").encodeToByteArray())
        val rev = generateTid()
        val commitData = "${identity.did}|$rev|$rootHash|3"
        val signature = identity.sign(commitData.encodeToByteArray())

        val commit = CommitEntity(
            did = identity.did,
            rev = rev,
            rootMstCid = rootHash,
            signature = signature,
            version = 3
        )
        db.commitDao().upsertCommit(commit)
    }

    //-----Utility Functions-----
    // TODO: Replace with real CIDv1
    private fun computeCid(data: ByteArray): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(data)
        return "sha256:${hash.joinToString("") { "%02x".format(it) }}"
    }

    private fun generateTid(): String {
        val timestamp = Clock.System.currentTimeMillis() * 1000 // microseconds
        val clockId = (Math.random() * 1024).toInt()
        val combined = (timestamp shl 10) or clockId.toLong()

        // Encode as base32-sort (simplified: use base36 for PoC)
        return combined.toString(36).padStart(13, '0').takeLast(13)

        // TODO: Replace with proper base32-sort encoding per ATProto spec
    }

    private fun nowIso8601(): String {
        return java.time.Instant.now().toString()
    }

    class DataValidatorException(message: String) : Exception(message)
}