package fyp.project.datingapp.database.pds.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fyp.project.datingapp.database.pds.entities.BlobEntity

@Dao
interface BlobDao {

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    suspend fun upsertBlob(blob: BlobEntity)

    @Query("SELECT * FROM blobs WHERE cid = :cid")
    suspend fun getBlob(cid: String): BlobEntity?

    @Query("SELECT * FROM blobs")
    suspend fun getAllBlobs(): List<BlobEntity>

    @Query("DELETE FROM blobs WHERE cid = :cid")
    suspend fun deleteBlob(cid: String)
}