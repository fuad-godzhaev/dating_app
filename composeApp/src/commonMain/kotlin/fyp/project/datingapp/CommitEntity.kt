package fyp.project.datingapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class CommitEntity(
    @PrimaryKey(autoGenerate = true) val key: Long = 0,
    val id: Int,
    val rootMstCid: String,
    val rev: String,
    val signature: ByteArray,
    val did: String
)