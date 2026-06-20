package llm.slop.spirals.database.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import llm.slop.spirals.MandalaDatabase
import llm.slop.spirals.database.entities.RandomSetEntity
import llm.slop.spirals.models.RandomSet

/**
 * Repository for managing Random Sets.
 * 
 * This repository handles CRUD operations for Random Sets, abstracting away
 * the database interactions from the ViewModels. It converts between the domain model
 * (RandomSet) and the database entity (RandomSetEntity).
 */
class RandomSetRepository(private val database: MandalaDatabase) : Repository<RandomSet, String> {
    private val randomSetDao = database.randomSetDao()
    private val jsonConfiguration = Json { ignoreUnknownKeys = true; prettyPrint = true }
    
    /**
     * Gets a flow of all random sets.
     * 
     * @return A Flow emitting a list of RandomSet objects converted from database entities
     */
    override fun getAll(): Flow<List<RandomSet>> {
        return randomSetDao.getAllRandomSets().map { entities ->
            entities.mapNotNull { entity ->
                fromEntity(entity)
            }
        }
    }
    
    /**
     * Gets a random set by its ID.
     * 
     * @param id The ID of the random set to get
     * @return The RandomSet object if found, or null if not found
     */
    override suspend fun getById(id: String): RandomSet? {
        val entity = randomSetDao.getById(id) ?: return null
        return fromEntity(entity)
    }
    
    /**
     * Saves a random set to the database.
     * 
     * @param entity The RandomSet to save
     */
    override suspend fun save(entity: RandomSet) {
        val json = jsonConfiguration.encodeToString(entity)
        randomSetDao.insertRandomSet(RandomSetEntity(entity.id, entity.name, json))
    }
    
    /**
     * Deletes a random set by its ID.
     * 
     * @param id The ID of the random set to delete
     */
    override suspend fun deleteById(id: String) {
        randomSetDao.deleteById(id)
    }
    
    /**
     * Renames a random set.
     * 
     * @param id The ID of the random set to rename
     * @param newName The new name for the random set
     * @return true if the random set was found and renamed, false otherwise
     */
    suspend fun renamePatch(id: String, newName: String): Boolean {
        val entity = randomSetDao.getById(id) ?: return false
        
        try {
            val randomSet = fromEntity(entity)?.copy(name = newName) ?: return false
            save(randomSet)
            return true
        } catch (e: Exception) {
            return false
        }
    }
    
    /**
     * Clones a random set.
     * 
     * @param id The ID of the random set to clone
     * @param newName The name for the cloned random set
     * @param newId The ID for the cloned random set (optional)
     * @return the ID of the cloned random set if successful, null otherwise
     */
    suspend fun clonePatch(id: String, newName: String, newId: String = java.util.UUID.randomUUID().toString()): String? {
        val entity = randomSetDao.getById(id) ?: return null
        
        try {
            val randomSet = fromEntity(entity)?.copy(id = newId, name = newName) ?: return null
            save(randomSet)
            return newId
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Convert a RandomSetEntity to a RandomSet domain object.
     */
    private fun fromEntity(entity: RandomSetEntity): RandomSet? {
        return try {
            val randomSet = jsonConfiguration.decodeFromString<RandomSet>(entity.jsonSettings)
            
            // Ensure the name and ID match the entity
            // This handles cases where the JSON serialization might have different values
            randomSet.copy(id = entity.id, name = entity.name)
        } catch (e: Exception) {
            null
        }
    }
}