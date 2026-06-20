package llm.slop.spirals.database.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import llm.slop.spirals.MandalaDatabase
import llm.slop.spirals.database.entities.ShowPatchEntity
import llm.slop.spirals.models.ShowPatch

/**
 * Repository for managing Show patches.
 * 
 * This repository handles CRUD operations for Show patches, abstracting away
 * the database interactions from the ViewModels. It converts between the domain model
 * (ShowPatch) and the database entity (ShowPatchEntity).
 */
class ShowRepository(private val database: MandalaDatabase) : Repository<ShowPatch, String> {
    private val showDao = database.showPatchDao()
    private val jsonConfiguration = Json { ignoreUnknownKeys = true; prettyPrint = true }
    
    /**
     * Gets a flow of all show patches.
     * 
     * @return A Flow emitting a list of ShowPatch objects converted from database entities
     */
    override fun getAll(): Flow<List<ShowPatch>> {
        return showDao.getAllShowPatches().map { entities ->
            entities.mapNotNull { entity ->
                fromEntity(entity)
            }
        }
    }
    
    /**
     * Gets a show patch by its ID.
     * 
     * Note: This implementation is inefficient as it needs to load all show patches.
     * In a production environment, we would add a dedicated DAO method.
     * 
     * @param id The ID of the show patch to get
     * @return The ShowPatch object if found, or null if not found
     */
    override suspend fun getById(id: String): ShowPatch? {
        val allShows = showDao.getAllShowPatches().first()
        val entity = allShows.find { it.id == id } ?: return null
        return fromEntity(entity)
    }
    
    /**
     * Gets a show patch by its name.
     * 
     * @param name The name of the show patch to get
     * @return The ShowPatch object if found, or null if not found
     */
    suspend fun getByName(name: String): ShowPatch? {
        val allShows = showDao.getAllShowPatches().first()
        val entity = allShows.find { it.name == name } ?: return null
        return fromEntity(entity)
    }
    
    /**
     * Saves a show patch to the database.
     * 
     * @param entity The ShowPatch to save
     */
    override suspend fun save(entity: ShowPatch) {
        val json = jsonConfiguration.encodeToString(entity)
        showDao.insertShowPatch(ShowPatchEntity(entity.id, entity.name, json))
    }
    
    /**
     * Deletes a show patch by its ID.
     * 
     * @param id The ID of the show patch to delete
     */
    override suspend fun deleteById(id: String) {
        showDao.deleteById(id)
    }
    
    /**
     * Deletes a show patch by its name.
     * 
     * @param name The name of the show patch to delete
     */
    suspend fun deleteByName(name: String) {
        showDao.deleteByName(name)
    }
    
    /**
     * Renames a show patch.
     * 
     * @param id The ID of the show patch to rename
     * @param newName The new name for the show patch
     * @return true if the show patch was found and renamed, false otherwise
     */
    suspend fun renamePatch(id: String, newName: String): Boolean {
        val allShows = showDao.getAllShowPatches().first()
        val entity = allShows.find { it.id == id } ?: return false
        
        // Update the entity with the new name
        showDao.insertShowPatch(entity.copy(name = newName))
        return true
    }
    
    /**
     * Clones a show patch.
     * 
     * @param id The ID of the show patch to clone
     * @param newName The name for the cloned show patch
     * @param newId The ID for the cloned show patch (optional)
     * @return the ID of the cloned show patch if successful, null otherwise
     */
    suspend fun clonePatch(id: String, newName: String, newId: String = java.util.UUID.randomUUID().toString()): String? {
        val allShows = showDao.getAllShowPatches().first()
        val entity = allShows.find { it.id == id } ?: return null
        
        // Create a new entity with new ID and name but same settings
        showDao.insertShowPatch(entity.copy(id = newId, name = newName))
        return newId
    }
    
    /**
     * Convert a ShowPatchEntity to a ShowPatch domain object.
     */
    private fun fromEntity(entity: ShowPatchEntity): ShowPatch? {
        return try {
            val showPatch = jsonConfiguration.decodeFromString<ShowPatch>(entity.jsonSettings)
            
            // Ensure the name and ID match the entity
            // This handles cases where the JSON serialization might have different values
            showPatch.copy(id = entity.id, name = entity.name)
        } catch (e: Exception) {
            null
        }
    }
}