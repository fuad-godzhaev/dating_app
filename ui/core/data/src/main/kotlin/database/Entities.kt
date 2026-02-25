import androidx.room.vo.PrimaryKey

@Entity
data class RecordEntity(
    @PrimaryKey val id: String,
    val name: String,
    val value: String
)