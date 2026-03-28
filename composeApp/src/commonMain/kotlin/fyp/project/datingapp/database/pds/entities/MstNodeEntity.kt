package fyp.project.datingapp.database.pds.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class MstNodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val cid: String,
    val cborBytes: ByteArray
)