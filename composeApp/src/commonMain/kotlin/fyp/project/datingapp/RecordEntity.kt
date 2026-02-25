package fyp.project.datingapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class RecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val collection: String,
    val rkey: String,
    val cborBytes: ByteArray,
    val cid: String,
    val createdAt: Long
)