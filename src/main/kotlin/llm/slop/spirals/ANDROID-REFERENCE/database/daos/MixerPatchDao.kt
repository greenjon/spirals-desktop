package llm.slop.spirals.database.daos

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import llm.slop.spirals.database.entities.MixerPatchEntity

@Dao
interface MixerPatchDao {
    @Query("SELECT * FROM mixer_patches")
    fun getAllMixerPatches(): Flow<List<MixerPatchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMixerPatch(patch: MixerPatchEntity)

    @Query("DELETE FROM mixer_patches WHERE id = :id")
    suspend fun deleteById(id: String)
}
