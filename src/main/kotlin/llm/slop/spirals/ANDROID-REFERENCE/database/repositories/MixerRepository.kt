package llm.slop.spirals.database.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import llm.slop.spirals.MandalaDatabase
import llm.slop.spirals.database.entities.MixerPatchEntity
import llm.slop.spirals.models.MixerPatch

/**
 * Repository for managing Mixer patches.
 * 
 * This repository handles CRUD operations for Mixer patches, abstracting away
 * the database interactions from the ViewModels. It converts between the domain model
 * (MixerPatch) and the database entity (MixerPatchEntity).
 */
class MixerRepository(private val database: MandalaDatabase) : Repository<MixerPatch, String> {
    private val mixerDao = database.mixerPatchDao()
    private val jsonConfiguration = Json { ignoreUnknownKeys = true; prettyPrint = true }
    
    /**
     * Gets a flow of all mixer patches.
     * 
     * @return A Flow emitting a list of MixerPatch objects converted from database entities
     */
    override fun getAll(): Flow<List<MixerPatch>> {
        return mixerDao.getAllMixerPatches().map { entities ->
            entities.mapNotNull { entity ->
                fromEntity(entity)
            }
        }
    }
    
    /**
     * Gets a mixer patch by its ID.
     * 
     * Note: This implementation is inefficient as it needs to load all mixer patches.
     * In a production environment, we would add a dedicated DAO method.
     * 
     * @param id The ID of the mixer patch to get
     * @return The MixerPatch object if found, or null if not found
     */
    override suspend fun getById(id: String): MixerPatch? {
        val allMixers = mixerDao.getAllMixerPatches().first()
        val entity = allMixers.find { it.id == id } ?: return null
        return fromEntity(entity)
    }
    
    /**
     * Gets a mixer patch by its name.
     * 
     * @param name The name of the mixer patch to get
     * @return The MixerPatch object if found, or null if not found
     */
    suspend fun getByName(name: String): MixerPatch? {
        val allMixers = mixerDao.getAllMixerPatches().first()
        val entity = allMixers.find { it.name == name } ?: return null
        return fromEntity(entity)
    }
    
    /**
     * Saves a mixer patch to the database.
     * 
     * @param entity The MixerPatch to save
     */
    override suspend fun save(entity: MixerPatch) {
        val json = jsonConfiguration.encodeToString(entity)
        mixerDao.insertMixerPatch(MixerPatchEntity(entity.id, entity.name, json))
    }
    
    /**
     * Deletes a mixer patch by its ID.
     * 
     * @param id The ID of the mixer patch to delete
     */
    override suspend fun deleteById(id: String) {
        mixerDao.deleteById(id)
    }
    
    /**
     * Renames a mixer patch.
     * 
     * @param id The ID of the mixer patch to rename
     * @param newName The new name for the mixer patch
     * @return true if the mixer patch was found and renamed, false otherwise
     */
    suspend fun renamePatch(id: String, newName: String): Boolean {
        val allMixers = mixerDao.getAllMixerPatches().first()
        val entity = allMixers.find { it.id == id } ?: return false
        
        // Update the entity with the new name
        mixerDao.insertMixerPatch(entity.copy(name = newName))
        return true
    }
    
    /**
     * Clones a mixer patch.
     * 
     * @param id The ID of the mixer patch to clone
     * @param newName The name for the cloned mixer patch
     * @param newId The ID for the cloned mixer patch (optional)
     * @return the ID of the cloned mixer patch if successful, null otherwise
     */
    suspend fun clonePatch(id: String, newName: String, newId: String = java.util.UUID.randomUUID().toString()): String? {
        val allMixers = mixerDao.getAllMixerPatches().first()
        val entity = allMixers.find { it.id == id } ?: return null
        
        // Create a new entity with new ID and name but same settings
        mixerDao.insertMixerPatch(entity.copy(id = newId, name = newName))
        return newId
    }
    
    /**
     * Convert a MixerPatchEntity to a MixerPatch domain object.
     */
    private fun fromEntity(entity: MixerPatchEntity): MixerPatch? {
        return try {
            val mixerPatch = jsonConfiguration.decodeFromString<MixerPatch>(entity.jsonSettings)
            
            // Ensure the name and ID match the entity
            // This handles cases where the JSON serialization might have different values
            mixerPatch.copy(id = entity.id, name = entity.name)
        } catch (e: Exception) {
            null
        }
    }
}