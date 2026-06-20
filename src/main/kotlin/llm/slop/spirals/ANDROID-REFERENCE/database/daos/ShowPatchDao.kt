package llm.slop.spirals.database.daos

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import llm.slop.spirals.database.entities.ShowPatchEntity

@Dao
interface ShowPatchDao {
    @Query("SELECT * FROM show_patches")
    fun getAllShowPatches(): Flow<List<ShowPatchEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShowPatch(patch: ShowPatchEntity)

    @Query("DELETE FROM show_patches WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM show_patches WHERE name = :name")
    suspend fun deleteByName(name: String)
}
