package fyp.project.datingapp.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface RecordDao {

    //Insert or replace a record
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertRecord(record: RecordEntity)

    //Get a specific record by collection + key
    @Query("SELECT * FROM records WHERE collection = :collection AND rkey = :rkey")
    suspend fun getRecord(collection: String, rkey: String): RecordEntity?

    //List all records in a collection, newest first
    @Query("SELECT * FROM records WHERE collection = :collection ORDER BY createdAt DESC")
    suspend fun listRecords(collection: String): List<RecordEntity>

    //Delete a record
    @Query("DELETE FROM records WHERE collection = :collection AND rkey = :rkey")
    suspend fun deleteRecord(collection: String, rkey: String)

    //Get all records (all collections) for repository export
    @Query("SELECT * FROM records ORDER BY collection, rkey")
    suspend fun getAllRecords(): List<RecordEntity>

    //Count records in a collection
    @Query("SELECT COUNT(*) FROM records WHERE collection = :collection")
    suspend fun countRecords(collection: String): Int
}