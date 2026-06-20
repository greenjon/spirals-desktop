package llm.slop.spirals.database.entities

import androidx.room.Entity

@Entity(tableName = "mandala_tags", primaryKeys = ["id", "tag"])
data class MandalaTag(
    val id: String,
    val tag: String
)
