package llm.slop.spirals.database.entities

import androidx.room.*

@Entity(tableName = "mandala_sets")
data class MandalaSetEntity(
    @PrimaryKey val id: String,
    val name: String,
    val jsonOrderedMandalaIds: String, // Serialized List<String>
    val selectionPolicy: String
)
