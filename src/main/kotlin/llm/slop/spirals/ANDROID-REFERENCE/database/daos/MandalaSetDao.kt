package llm.slop.spirals.database.daos

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import llm.slop.spirals.database.entities.MandalaSetEntity

@Dao
interface MandalaSetDao {
    @Query("SELECT * FROM mandala_sets")
    fun getAllSets(): Flow<List<MandalaSetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: MandalaSetEntity)

    @Delete
    suspend fun deleteSet(set: MandalaSetEntity)

    @Query("DELETE FROM mandala_sets WHERE id = :id")
    suspend fun deleteById(id: String)
}
