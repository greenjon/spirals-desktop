package llm.slop.spirals.database.daos

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import llm.slop.spirals.database.entities.RandomSetEntity

@Dao
interface RandomSetDao {
    @Query("SELECT * FROM random_sets")
    fun getAllRandomSets(): Flow<List<RandomSetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRandomSet(randomSet: RandomSetEntity)

    @Delete
    suspend fun deleteRandomSet(randomSet: RandomSetEntity)

    @Query("DELETE FROM random_sets WHERE id = :id")
    suspend fun deleteById(id: String)
    
    @Query("SELECT * FROM random_sets WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): RandomSetEntity?
}
