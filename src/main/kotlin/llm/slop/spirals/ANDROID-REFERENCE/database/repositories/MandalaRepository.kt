package llm.slop.spirals.database.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.firstOrNull
import llm.slop.spirals.database.entities.MandalaPatchEntity
import llm.slop.spirals.MandalaDatabase
import llm.slop.spirals.models.PatchData
import llm.slop.spirals.PatchMapper

/**
 * Repository for managing Mandala patches.
 *
 * This repository handles CRUD operations for Mandala patches, abstracting away
 * the database interactions from the ViewModels. It converts between the domain model
 * (PatchData) and the database entity (MandalaPatchEntity).
 */
class MandalaRepository(private val database: MandalaDatabase) : Repository<PatchData, String> {
    private val patchDao = database.mandalaPatchDao()

    /**
     * Gets a flow of all mandala patches.
     *
     * @return A Flow emitting a list of PatchData objects converted from database entities
     */
    override fun getAll(): Flow<List<PatchData>> {
        return patchDao.getAllPatches().map { entities ->
            entities.mapNotNull { entity ->
                PatchMapper.fromJson(entity.jsonSettings)?.let { patch ->
                    // Ensure name and recipeId match the entity
                    PatchData(
                        name = entity.name,
                        recipeId = entity.recipeId,
                        parameters = patch.parameters,
                        version = patch.version
                    )
                }
            }
        }
    }

    /**
     * Gets a mandala patch by its name (which serves as its ID).
     *
     * Note: This implementation is inefficient as it needs to load all patches.
     * In a production environment, we would add a dedicated DAO method.
     *
     * @param id The name of the patch to get
     * @return The PatchData object if found, or null if not found
     */
    override suspend fun getById(id: String): PatchData? {
        // Since we don't have a direct DAO method for getting by name, we need to get all and filter
        val allPatches = patchDao.getAllPatches().map { entities ->
            entities.find { it.name == id }
        }.firstOrNull()

        val entity = allPatches ?: return null

        return PatchMapper.fromJson(entity.jsonSettings)?.let { patch ->
            PatchData(
                name = entity.name,
                recipeId = entity.recipeId,
                parameters = patch.parameters,
                version = patch.version
            )
        }
    }

    /**
     * Saves a mandala patch to the database.
     *
     * @param entity The PatchData to save
     */
    override suspend fun save(entity: PatchData) {
        val json = PatchMapper.toJson(entity)
        patchDao.insertPatch(MandalaPatchEntity(entity.name, entity.recipeId, json))
    }

    /**
     * Deletes a mandala patch by its name.
     *
     * @param id The name of the patch to delete
     */
    override suspend fun deleteById(id: String) {
        patchDao.deleteByName(id)
    }

    /**
     * Renames a mandala patch.
     *
     * @param oldName The current name of the patch
     * @param newName The new name for the patch
     */
    suspend fun renamePatch(oldName: String, newName: String) {
        // Get the entity by old name
        val entity = patchDao.getAllPatches().map { entities ->
            entities.find { it.name == oldName }
        }.firstOrNull()

        if (entity != null) {
            // Delete the old entity
            patchDao.deleteByName(oldName)

            // Create a new entity with the new name
            val newEntity = MandalaPatchEntity(
                name = newName,
                recipeId = entity.recipeId,
                jsonSettings = entity.jsonSettings
            )

            // Insert the new entity
            patchDao.insertPatch(newEntity)
        }
    }

    /**
     * Clones a mandala patch.
     *
     * @param name The name of the patch to clone
     * @param newName The name for the cloned patch
     */
    suspend fun clonePatch(name: String, newName: String) {
        // Get the entity by name
        val entity = patchDao.getAllPatches().map { entities ->
            entities.find { it.name == name }
        }.firstOrNull()

        if (entity != null) {
            // Create a new entity with the new name
            val newEntity = MandalaPatchEntity(
                name = newName,
                recipeId = entity.recipeId,
                jsonSettings = entity.jsonSettings
            )

            // Insert the new entity
            patchDao.insertPatch(newEntity)
        }
    }
}