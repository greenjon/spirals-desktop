package llm.slop.spirals.database.entities

import androidx.room.*

/**
 * Room Entity for storing a Mandala Patch.
 */
@Entity(tableName = "mandala_patches")
data class MandalaPatchEntity(
    @PrimaryKey val name: String,
    val recipeId: String,
    val jsonSettings: String // Serialized ParameterSettings
)
