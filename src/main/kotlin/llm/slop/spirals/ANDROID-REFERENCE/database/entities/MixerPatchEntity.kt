package llm.slop.spirals.database.entities

import androidx.room.*

@Entity(tableName = "mixer_patches")
data class MixerPatchEntity(
    @PrimaryKey val id: String,
    val name: String,
    val jsonSettings: String // Serialized MixerPatch
)
