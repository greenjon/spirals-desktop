package llm.slop.spirals.database.repositories

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import llm.slop.spirals.MandalaDatabase
import llm.slop.spirals.database.entities.MandalaTag

/**
 * Repository for managing Mandala tags.
 * 
 * This repository handles operations for tagging mandalas, sets, and other entities.
 */
class TagRepository(private val database: MandalaDatabase) {
    private val tagDao = database.mandalaTagDao()
    
    /**
     * Gets a flow of all tags grouped by entity ID.
     * 
     * @return A Flow emitting a Map where keys are entity IDs and values are lists of tags
     */
    fun getAllTagsGroupedById(): Flow<Map<String, List<String>>> {
        return tagDao.getAllTags()
            .map { list -> list.groupBy({ tag -> tag.id }, { tag -> tag.tag }) }
    }
    
    /**
     * Gets all tags for a specific entity.
     * 
     * @param id The ID of the entity
     * @return A list of tag strings
     */
    suspend fun getTagsForId(id: String): List<String> {
        return tagDao.getAllTagsForId(id).map { it.tag }
    }
    
    /**
     * Toggles a tag for an entity. Adds the tag if it doesn't exist, removes it if it does.
     * 
     * @param id The ID of the entity
     * @param tag The tag to toggle
     */
    suspend fun toggleTag(id: String, tag: String) {
        val allForId = tagDao.getAllTagsForId(id)
        val existing = allForId.find { it.tag == tag }
        
        if (existing != null) {
            tagDao.deleteTag(existing)
        } else {
            val ratings = listOf("trash", "1", "2", "3")
            if (tag in ratings) {
                // Remove any existing rating tags before adding a new one
                allForId.filter { it.tag in ratings }.forEach { tagDao.deleteTag(it) }
            }
            tagDao.insertTag(MandalaTag(id, tag))
        }
    }
    
    /**
     * Adds a tag to an entity.
     * 
     * @param id The ID of the entity
     * @param tag The tag to add
     */
    suspend fun addTag(id: String, tag: String) {
        val allForId = tagDao.getAllTagsForId(id)
        val existing = allForId.find { it.tag == tag }
        
        if (existing == null) {
            val ratings = listOf("trash", "1", "2", "3")
            if (tag in ratings) {
                // Remove any existing rating tags before adding a new one
                allForId.filter { it.tag in ratings }.forEach { tagDao.deleteTag(it) }
            }
            tagDao.insertTag(MandalaTag(id, tag))
        }
    }
    
    /**
     * Removes a tag from an entity.
     * 
     * @param id The ID of the entity
     * @param tag The tag to remove
     */
    suspend fun removeTag(id: String, tag: String) {
        val allForId = tagDao.getAllTagsForId(id)
        val existing = allForId.find { it.tag == tag }
        
        if (existing != null) {
            tagDao.deleteTag(existing)
        }
    }
    
    /**
     * Sets the rating (1, 2, 3, or trash) for an entity.
     * 
     * @param id The ID of the entity
     * @param rating The rating to set (1, 2, 3, or "trash")
     */
    suspend fun setRating(id: String, rating: String) {
        if (rating !in listOf("trash", "1", "2", "3")) return
        
        val allForId = tagDao.getAllTagsForId(id)
        
        // Remove any existing rating tags
        allForId.filter { it.tag in listOf("trash", "1", "2", "3") }
            .forEach { tagDao.deleteTag(it) }
        
        // Add the new rating tag
        tagDao.insertTag(MandalaTag(id, rating))
    }
    
    /**
     * Exports all tags as a CSV string.
     * 
     * @return A CSV-formatted string containing all tags
     */
    suspend fun exportTagsAsCsv(): String {
        val tagsFlow = getAllTagsGroupedById()
        val tags = tagsFlow.first()
        
        if (tags.isEmpty()) return "No tags recorded."
        
        val sb = StringBuilder("ID,Tags\n")
        tags.forEach { (entityId, tagsList) -> 
            sb.append("$entityId,${tagsList.joinToString("|")}\n") 
        }
        
        return sb.toString()
    }
}