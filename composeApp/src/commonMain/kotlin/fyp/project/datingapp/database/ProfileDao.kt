package fyp.project.datingapp.database

import androidx.room.Dao
import androidx.room.Query

@Dao
interface ProfileDao {

    //Get the local user's profile.
    @Query("""
        SELECT * FROM records 
        WHERE collection = 'app.dateable.actor.profile' AND rkey = 'self'
    """)
    suspend fun getMyProfile(): RecordEntity?

    //Check if a profile exists
    @Query("""
        SELECT COUNT(*) FROM records 
        WHERE collection = 'app.dateable.actor.profile' AND rkey = 'self'
    """)
    suspend fun hasProfile(): Boolean
}