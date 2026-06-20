package llm.slop.spirals.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import llm.slop.spirals.LayerType
import llm.slop.spirals.MandalaDatabase
import llm.slop.spirals.MandalaLayerContent
import llm.slop.spirals.MandalaVisualSource
import llm.slop.spirals.models.PatchData
import llm.slop.spirals.PatchMapper
import llm.slop.spirals.database.repositories.MandalaRepository
import llm.slop.spirals.database.repositories.TagRepository
import llm.slop.spirals.navigation.NavigationViewModel

/**
 * ViewModel for the Mandala Editor screen.
 * 
 * This ViewModel manages the state and logic specifically for editing individual Mandala patches.
 * It interacts with the NavigationViewModel for navigation and the MandalaRepository for data access.
 */
class MandalaEditorViewModel(
    application: Application,
    private val navigationViewModel: NavigationViewModel
) : AndroidViewModel(application) {
    private val database = MandalaDatabase.getDatabase(application)
    private val mandalaRepository = MandalaRepository(database)
    private val tagRepository = TagRepository(database)
    
    // The current patch being edited
    private val _currentPatch = MutableStateFlow<PatchData?>(null)
    val currentPatch: StateFlow<PatchData?> = _currentPatch.asStateFlow()
    
    // All available patches for selection
    val allPatches = mandalaRepository.getAll()
    
    // Tags for organizing mandalas
    val tags = tagRepository.getAllTagsGroupedById()
    
    /**
     * Sets the current patch being edited.
     * 
     * @param patch The patch to set as current
     */
    fun setCurrentPatch(patch: PatchData?) {
        _currentPatch.value = patch
    }
    
    /**
     * Updates the current patch from the visual source.
     * 
     * @param visualSource The source containing the current mandala state
     * @param isDirty Whether to mark the patch as dirty
     */
    fun updateFromVisualSource(visualSource: MandalaVisualSource, isDirty: Boolean = true) {
        val currentValue = _currentPatch.value ?: return
        val patchData = PatchMapper.fromVisualSource(currentValue.name, visualSource)
        
        // Update the current patch
        _currentPatch.value = patchData
        
        // Update the navigation layer data
        val navStack = navigationViewModel.navStack.value
        val index = navStack.indexOfLast { it.type == LayerType.MANDALA }
        if (index >= 0) {
            navigationViewModel.updateLayerData(index, MandalaLayerContent(patchData), isDirty)
        }
    }
    
    /**
     * Checks if the current state has unsaved changes compared to a reference patch.
     * 
     * @param visualSource The source containing the current mandala state
     * @param reference The reference patch to compare against
     * @return True if there are unsaved changes, false otherwise
     */
    fun isDirty(visualSource: MandalaVisualSource, reference: PatchData?): Boolean {
        return PatchMapper.isDirty(visualSource, reference)
    }
    
    /**
     * Saves the current patch to the database.
     * 
     * @param patch The patch to save
     */
    fun savePatch(patch: PatchData) {
        viewModelScope.launch {
            mandalaRepository.save(patch)
            
            // Update the navigation layer to mark it as not dirty
            val navStack = navigationViewModel.navStack.value
            val index = navStack.indexOfLast { it.type == LayerType.MANDALA }
            if (index >= 0) {
                navigationViewModel.updateLayerData(index, MandalaLayerContent(patch), false)
            }
        }
    }
    
    /**
     * Deletes a patch by name.
     * 
     * @param name The name of the patch to delete
     */
    fun deletePatch(name: String) {
        viewModelScope.launch {
            mandalaRepository.deleteById(name)
        }
    }
    
    /**
     * Renames a patch.
     * 
     * @param oldName The current name of the patch
     * @param newName The new name for the patch
     */
    fun renamePatch(oldName: String, newName: String) {
        viewModelScope.launch {
            // Get the current patch
            val patch = _currentPatch.value ?: return@launch
            
            // Create a new patch with the new name
            val newPatch = patch.copy(name = newName)
            
            // Delete the old patch and save the new one
            mandalaRepository.deleteById(oldName)
            mandalaRepository.save(newPatch)
            
            // Update the current patch
            _currentPatch.value = newPatch
        }
    }
    
    /**
     * Clones a patch.
     * 
     * @param name The name of the patch to clone
     * @param newName The name for the cloned patch
     */
    fun clonePatch(name: String, newName: String) {
        viewModelScope.launch {
            // Get the patch to clone
            val patchFlow = allPatches.first()
            val patch = patchFlow.find { it.name == name } ?: return@launch
            
            // Create a new patch with the new name
            val newPatch = patch.copy(name = newName)
            
            // Save the new patch
            mandalaRepository.save(newPatch)
        }
    }
    
    /**
     * Toggles a tag for the current patch.
     * 
     * @param tag The tag to toggle
     */
    fun toggleTag(tag: String) {
        viewModelScope.launch {
            val patch = _currentPatch.value ?: return@launch
            tagRepository.toggleTag(patch.name, tag)
        }
    }
}