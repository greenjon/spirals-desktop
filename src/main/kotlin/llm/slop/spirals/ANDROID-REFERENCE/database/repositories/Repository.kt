package llm.slop.spirals.database.repositories

import kotlinx.coroutines.flow.Flow

/**
 * Base Repository interface defining common operations for data access.
 * 
 * This interface provides a consistent pattern for accessing data across different domains
 * in the application. All domain-specific repositories should implement this interface.
 * 
 * @param T The entity type this repository manages
 * @param ID The type of the identifier for entities in this repository
 */
interface Repository<T, ID> {
    /**
     * Gets a flow of all entities of type T.
     * 
     * @return A Flow emitting a list of all entities whenever the underlying data changes
     */
    fun getAll(): Flow<List<T>>
    
    /**
     * Gets a single entity by its identifier.
     * 
     * @param id The identifier of the entity to get
     * @return The entity with the given ID, or null if not found
     */
    suspend fun getById(id: ID): T?
    
    /**
     * Saves an entity to the data store.
     * This operation should upsert (insert if not exists, update if exists).
     * 
     * @param entity The entity to save
     */
    suspend fun save(entity: T)
    
    /**
     * Deletes an entity from the data store.
     * 
     * @param id The identifier of the entity to delete
     */
    suspend fun deleteById(id: ID)
}