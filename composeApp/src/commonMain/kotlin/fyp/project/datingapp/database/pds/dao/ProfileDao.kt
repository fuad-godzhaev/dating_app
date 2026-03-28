package fyp.project.datingapp.database.pds.dao

import androidx.room.Dao
import androidx.room.Query
import fyp.project.datingapp.database.pds.entities.RecordEntity

@Dao
interface ProfileDao {

    //Get the local user's profile.
    @Query("""
        SELECT * FROM records 
        WHERE collection = 'fyp.project.datingapp.records.profile' AND rkey = 'self'
    """)
    suspend fun getMyProfile(): RecordEntity?

    //Check if a profile exists
    @Query("""
        SELECT COUNT(*) FROM records 
        WHERE collection = 'fyp.project.datingapp.records.profile' AND rkey = 'self'
    """)
    suspend fun hasProfile(): Boolean
}

//TODO: Delete this file?