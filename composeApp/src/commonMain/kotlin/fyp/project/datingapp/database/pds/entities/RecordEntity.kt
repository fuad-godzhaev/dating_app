package fyp.project.datingapp.database.pds.entities

import androidx.room.Entity
import androidx.room.Index

//Generic record to store in PDS
@Entity(
    tableName = "records",
    primaryKeys = ["collection", "rkey"],
    indices = [
        Index(value = ["collection"]),
        Index(value = ["cid"], unique = true), // CID is globally unique
        Index(value = ["createdAt"])
    ]
)
data class RecordEntity(
    val collection: String,    // NSID: "fyp.project.datingapp.records.profile"
    val rkey: String,          // Record key: "self" or TID
    val cborBytes: ByteArray,
    val cid: String,           // CIDv1 of the CBOR bytes
    val createdAt: Long,
    // v4: per-record ECDSA P-256 signature over [cborBytes] (the exact bytes that
    // were signed). Lets a peer verify this record standalone via the owner's
    // did:key, independent of the MST commit. Nullable for pre-v4 / legacy rows.
    val signature: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is RecordEntity) return false
        return collection == other.collection && rkey == other.rkey
    }
    override fun hashCode(): Int = 31 * collection.hashCode() + rkey.hashCode()
}