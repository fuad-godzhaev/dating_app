package fyp.project.datingapp.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "blobs",
    indices = [Index(value = ["cid"], unique = true)]
)
data class BlobEntity(
    @PrimaryKey val cid: String,   // CID of the blob bytes
    val mimeType: String,
    val size: Long,
    val filePath: String           // Absolute path on device filesystem
)
