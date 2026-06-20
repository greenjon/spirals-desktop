package llm.slop.spirals.database.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import llm.slop.spirals.MandalaDatabase
import llm.slop.spirals.database.entities.MandalaSetEntity
import llm.slop.spirals.models.set.MandalaSet
import llm.slop.spirals.models.set.SelectionPolicy

/**
 * Repository for managing Mandala Sets.
 * 
 * This repository handles CRUD operations for Mandala Sets, abstracting away
 * the database interactions from the ViewModels. It converts between the domain model
 * (MandalaSet) and the database entity (MandalaSetEntity).
 */
class SetRepository(private val database: MandalaDatabase) : Repository<MandalaSet, String> {
    private val setDao = database.mandalaSetDao()
    private val jsonConfiguration = Json { ignoreUnknownKeys = true }
    
    /**
     * Gets a flow of all mandala sets.
     * 
     * @return A Flow emitting a list of MandalaSet objects converted from database entities
     */
    override fun getAll(): Flow<List<MandalaSet>> {
        return setDao.getAllSets().map { entities ->
            entities.map { entity ->
                fromEntity(entity)
            }
        }
    }
    
    /**
     * Gets a mandala set by its ID.
     * 
     * Note: This implementation is inefficient as it needs to load all sets.
     * In a production environment, we would add a dedicated DAO method.
     * 
     * @param id The ID of the mandala set to get
     * @return The MandalaSet object if found, or null if not found
     */
    override suspend fun getById(id: String): MandalaSet? {
        // Since we don't have a direct DAO method for getting by ID, we need to get all and filter
        val allSets = setDao.getAllSets().first()
        val entity = allSets.find { it.id == id } ?: return null
        return fromEntity(entity)
    }
    
    /**
     * Saves a mandala set to the database.
     * 
     * @param entity The MandalaSet to save
     */
    override suspend fun save(entity: MandalaSet) {
        val jsonOrderedMandalaIds = jsonConfiguration.encodeToString(entity.orderedMandalaIds)
        setDao.insertSet(
            MandalaSetEntity(
                id = entity.id,
                name = entity.name,
                jsonOrderedMandalaIds = jsonOrderedMandalaIds,
                selectionPolicy = entity.selectionPolicy.name
            )
        )
    }
    
    /**
     * Deletes a mandala set by its ID.
     * 
     * @param id The ID of the mandala set to delete
     */
    override suspend fun deleteById(id: String) {
        setDao.deleteById(id)
    }
    
    /**
     * Renames a mandala set.
     * 
     * @param id The ID of the set to rename
     * @param newName The new name for the set
     * @return true if the set was found and renamed, false otherwise
     */
    suspend fun renameSet(id: String, newName: String): Boolean {
        val allSets = setDao.getAllSets().first()
        val entity = allSets.find { it.id == id } ?: return false
        
        setDao.insertSet(entity.copy(name = newName))
        return true
    }
    
    /**
     * Clones a mandala set.
     * 
     * @param id The ID of the mandala set to clone
     * @param newName The name for the cloned set
     * @param newId The ID for the cloned set (optional)
     * @return the ID of the cloned set if successful, null otherwise
     */
    suspend fun cloneSet(id: String, newName: String, newId: String = java.util.UUID.randomUUID().toString()): String? {
        val allSets = setDao.getAllSets().first()
        val entity = allSets.find { it.id == id } ?: return null
        
        val newEntity = entity.copy(
            id = newId,
            name = newName
        )
        
        setDao.insertSet(newEntity)
        return newId
    }
    
    /**
     * Convert a MandalaSetEntity to a MandalaSet domain object.
     */
    private fun fromEntity(entity: MandalaSetEntity): MandalaSet {
        val orderedMandalaIds = try {
            jsonConfiguration.decodeFromString<List<String>>(entity.jsonOrderedMandalaIds).toMutableList()
        } catch (e: Exception) {
            mutableListOf()
        }
        
        val selectionPolicy = try {
            SelectionPolicy.valueOf(entity.selectionPolicy)
        } catch (e: Exception) {
            SelectionPolicy.SEQUENTIAL
        }
        
        return MandalaSet(
            id = entity.id,
            name = entity.name,
            orderedMandalaIds = orderedMandalaIds,
            selectionPolicy = selectionPolicy
        )
    }
}