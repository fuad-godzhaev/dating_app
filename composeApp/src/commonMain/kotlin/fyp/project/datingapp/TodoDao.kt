package fyp.project.datingapp

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Insert
    suspend fun insert(item: RecordEntity)

    @Query("SELECT count(*) FROM RecordEntity")
    suspend fun count(): Int

    @Query("SELECT * FROM RecordEntity")
    fun getAllAsFlow(): Flow<List<RecordEntity>>
}