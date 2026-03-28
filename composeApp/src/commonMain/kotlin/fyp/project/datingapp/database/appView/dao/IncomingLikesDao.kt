package fyp.project.datingapp.database.appView.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fyp.project.datingapp.database.appView.entities.IncomingLikeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface IncomingLikesDao {
    //Get all unmatched incoming likes
    @Query("""
        SELECT * FROM incoming_likes 
        WHERE isMatched = 0 
        ORDER BY receivedAt DESC 
        """)
    fun getUnmatchedLikes(): Flow<List<IncomingLikeEntity>>

    //Check if a peer has liked you
    @Query("SELECT COUNT(*) > 0 FROM incoming_likes WHERE fromDid = :did AND isMatched = 0")
    suspend fun hasLikeFrom(did: String): Boolean

    //Insert an incoming like after verifying its signature
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLike(like: IncomingLikeEntity)

    //Mark a like as matched
    @Query("UPDATE incoming_likes SET isMatched = 1 WHERE fromDid = :did")
    suspend fun markAsMatched(did: String)

    //Count unmatched incoming likes
    @Query("SELECT COUNT(*) FROM incoming_likes WHERE isMatched = 0")
    fun countUnmatchedLikes(): Flow<Int>
}