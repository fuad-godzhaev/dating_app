package fyp.project.datingapp

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class BlobEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cid: String,
    val mimeType: String,
    val filePath: String,
    val size: Long
)