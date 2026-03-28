package fyp.project.datingapp.database

import androidx.room.*

@Entity(tableName = "auth_settings")
data class AuthSettingsEntity(
    @PrimaryKey val id: Int = 1,
    val pinHash: String? = null,
    val pinSalt: String? = null,
    val hasIdentity: Boolean = false,
    val didKey: String? = null,
    val createdAt: Long = 0
)

@Dao
interface AuthSettingsDao {

    @Query("SELECT * FROM auth_settings WHERE id = 1")
    suspend fun get(): AuthSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(settings: AuthSettingsEntity)

    @Query("DELETE FROM auth_settings")
    suspend fun clear()
}
