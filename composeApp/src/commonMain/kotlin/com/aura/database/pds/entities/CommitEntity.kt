package com.aura.database.pds.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "commits")
data class CommitEntity(
    @PrimaryKey val id: Int = 1,    // Always 1 — latest commit only
    val did: String,
    val rev: String,                // TID-format revision string
    val rootMstCid: String,         // CID of the MST root (or simplified hash)
    val signature: ByteArray,       // 64-byte ECDSA compact signature
    val version: Int = 3            // Commit format version
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CommitEntity) return false
        return did == other.did && rev == other.rev
    }
    override fun hashCode(): Int = 31 * did.hashCode() + rev.hashCode()
}
