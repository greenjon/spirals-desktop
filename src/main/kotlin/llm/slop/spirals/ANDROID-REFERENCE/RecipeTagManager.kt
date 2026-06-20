package llm.slop.spirals

import android.content.Context
import android.content.SharedPreferences

/**
 * RecipeTagManager - Simple tagging system for Mandala recipes.
 * 
 * Manages two types of tags using SharedPreferences:
 * - Favorites (star) - Recipes the user likes
 * - Trash (delete) - Recipes marked for deletion
 * 
 * USE CASE: The recipe library has hundreds of recipes. This helps users pare down
 * the library while performing or experimenting. Quick workflow:
 * 1. Browse recipes with arrow keys
 * 2. Star/trash with buttons on right side of preview
 * 3. Use sort modes to group favorites/trash
 * 4. Eventually delete recipes marked as trash
 * 
 * UI INTEGRATION:
 * - Star/trash buttons on right side of video preview in Mandala editor
 * - Recipe picker dialog shows color-coded text (green=favorite, red=trash)
 * - Sort modes to group tagged recipes
 * 
 * NOTE TO FUTURE AI: This is intentionally simple (SharedPreferences, not database).
 * Tags are per-device and don't sync. If you need sync/backup, consider moving to
 * database or implementing export/import functionality.
 */
class RecipeTagManager(context: Context) {
    private val prefs: SharedPreferences = context.applicationContext
        .getSharedPreferences("recipe_tags", Context.MODE_PRIVATE)
    
    private val favoritesKey = "favorites"
    private val trashKey = "trash"
    
    fun toggleFavorite(recipeId: String) {
        val favorites = getFavorites().toMutableSet()
        if (favorites.contains(recipeId)) {
            favorites.remove(recipeId)
        } else {
            favorites.add(recipeId)
        }
        prefs.edit().putStringSet(favoritesKey, favorites).apply()
    }
    
    fun toggleTrash(recipeId: String) {
        val trash = getTrash().toMutableSet()
        if (trash.contains(recipeId)) {
            trash.remove(recipeId)
        } else {
            trash.add(recipeId)
        }
        prefs.edit().putStringSet(trashKey, trash).apply()
    }
    
    fun isFavorite(recipeId: String): Boolean {
        return getFavorites().contains(recipeId)
    }
    
    fun isTrash(recipeId: String): Boolean {
        return getTrash().contains(recipeId)
    }
    
    fun getFavorites(): Set<String> {
        return prefs.getStringSet(favoritesKey, emptySet()) ?: emptySet()
    }
    
    fun getTrash(): Set<String> {
        return prefs.getStringSet(trashKey, emptySet()) ?: emptySet()
    }
}
