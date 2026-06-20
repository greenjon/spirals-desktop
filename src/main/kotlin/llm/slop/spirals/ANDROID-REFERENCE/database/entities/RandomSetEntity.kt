package llm.slop.spirals.database.entities

import androidx.room.*

@Entity(tableName = "random_sets")
data class RandomSetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val jsonSettings: String // Serialized RandomSet
)
