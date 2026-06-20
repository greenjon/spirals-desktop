package llm.slop.spirals.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import llm.slop.spirals.LayerType
import llm.slop.spirals.MandalaDatabase
import llm.slop.spirals.models.set.MandalaSet
import llm.slop.spirals.models.set.SelectionPolicy
import llm.slop.spirals.SetLayerContent
import llm.slop.spirals.database.repositories.MandalaRepository
import llm.slop.spirals.database.repositories.SetRepository
import llm.slop.spirals.navigation.NavigationViewModel

/**
 * ViewModel for the Set Editor screen.
 * 
 * This ViewModel manages the state and logic specifically for editing Mandala Sets.
 * It interacts with the NavigationViewModel for navigation and the SetRepository for data access.
 */
class SetEditorViewModel(
    application: Application,
    private val navigationViewModel: NavigationViewModel
) : AndroidViewModel(application) {
    private val database = MandalaDatabase.getDatabase(application)
    private val setRepository = SetRepository(database)
    private val mandalaRepository = MandalaRepository(database)
    
    // The current set being edited
    private val _currentSet = MutableStateFlow<MandalaSet?>(null)
    val currentSet: StateFlow<MandalaSet?> = _currentSet.asStateFlow()
    
    // All available sets for selection
    val allSets = setRepository.getAll()
    
    // All available mandalas that can be added to the set
    val allMandalas = mandalaRepository.getAll()
    
    /**
     * Sets the current set being edited.
     * 
     * @param set The set to edit
     */
    fun setCurrentSet(set: MandalaSet?) {
        _currentSet.value = set
    }
    
    /**
     * Updates the current set and the navigation layer.
     * 
     * @param set The updated set
     * @param isDirty Whether to mark the set as dirty
     */
    fun updateSet(set: MandalaSet, isDirty: Boolean = true) {
        _currentSet.value = set
        
        // Update the navigation layer data
        val navStack = navigationViewModel.navStack.value
        val index = navStack.indexOfLast { it.type == LayerType.SET }
        if (index >= 0) {
            navigationViewModel.updateLayerData(index, SetLayerContent(set), isDirty)
        }
    }
    
    /**
     * Adds a mandala to the set.
     * 
     * @param mandalaName The name of the mandala to add
     */
    fun addMandalaToSet(mandalaName: String) {
        val set = _currentSet.value ?: return
        if (!set.orderedMandalaIds.contains(mandalaName)) {
            val updatedIds = set.orderedMandalaIds.toMutableList()
            updatedIds.add(mandalaName)
            
            val updatedSet = set.copy(orderedMandalaIds = updatedIds)
            updateSet(updatedSet)
        }
    }
    
    /**
     * Removes a mandala from the set.
     * 
     * @param mandalaName The name of the mandala to remove
     */
    fun removeMandalaFromSet(mandalaName: String) {
        val set = _currentSet.value ?: return
        val updatedIds = set.orderedMandalaIds.toMutableList()
        if (updatedIds.remove(mandalaName)) {
            val updatedSet = set.copy(orderedMandalaIds = updatedIds)
            updateSet(updatedSet)
        }
    }
    
    /**
     * Reorders mandalas in the set.
     * 
     * @param fromIndex The original index
     * @param toIndex The target index
     */
    fun reorderMandalas(fromIndex: Int, toIndex: Int) {
        val set = _currentSet.value ?: return
        val updatedIds = set.orderedMandalaIds.toMutableList()
        
        if (fromIndex < updatedIds.size && toIndex < updatedIds.size) {
            val item = updatedIds.removeAt(fromIndex)
            updatedIds.add(toIndex, item)
            val updatedSet = set.copy(orderedMandalaIds = updatedIds)
            updateSet(updatedSet)
        }
    }
    
    /**
     * Changes the selection policy for the set.
     * 
     * @param policy The new selection policy
     */
    fun setSelectionPolicy(policy: SelectionPolicy) {
        val set = _currentSet.value ?: return
        val updatedSet = set.copy(selectionPolicy = policy)
        updateSet(updatedSet)
    }
    
    /**
     * Saves the current set to the database.
     */
    fun saveSet() {
        viewModelScope.launch {
            val set = _currentSet.value ?: return@launch
            setRepository.save(set)
            
            // Update the navigation layer to mark it as not dirty
            val navStack = navigationViewModel.navStack.value
            val index = navStack.indexOfLast { it.type == LayerType.SET }
            if (index >= 0) {
                navigationViewModel.updateLayerData(index, SetLayerContent(set), false)
            }
        }
    }
    
    /**
     * Deletes a set by ID.
     * 
     * @param id The ID of the set to delete
     */
    fun deleteSet(id: String) {
        viewModelScope.launch {
            setRepository.deleteById(id)
        }
    }
    
    /**
     * Renames a set.
     * 
     * @param id The ID of the set to rename
     * @param newName The new name for the set
     */
    fun renameSet(id: String, newName: String) {
        viewModelScope.launch {
            val set = _currentSet.value ?: return@launch
            if (set.id == id) {
                val updatedSet = set.copy(name = newName)
                setRepository.save(updatedSet)
                _currentSet.value = updatedSet
                
                // Update the navigation layer
                val navStack = navigationViewModel.navStack.value
                val index = navStack.indexOfLast { it.type == LayerType.SET }
                if (index >= 0) {
                    navigationViewModel.updateLayerName(index, newName)
                    navigationViewModel.updateLayerData(index, SetLayerContent(updatedSet), true)
                }
            } else {
                setRepository.renameSet(id, newName)
            }
        }
    }
    
    /**
     * Clones a set.
     * 
     * @param id The ID of the set to clone
     * @param newName The name for the cloned set
     */
    fun cloneSet(id: String, newName: String) {
        viewModelScope.launch {
            val newId = setRepository.cloneSet(id, newName)
            if (newId != null) {
                // If we cloned the current set, load the new one
                if (id == _currentSet.value?.id) {
                    setRepository.getById(newId)?.let { newSet ->
                        _currentSet.value = newSet
                    }
                }
            }
        }
    }
}