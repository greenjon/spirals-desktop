package llm.slop.spirals.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import llm.slop.spirals.LayerType
import llm.slop.spirals.MandalaDatabase
import llm.slop.spirals.RandomSetLayerContent
import llm.slop.spirals.database.repositories.MandalaRepository
import llm.slop.spirals.database.repositories.RandomSetRepository
import llm.slop.spirals.models.RandomSet
import llm.slop.spirals.models.*
import llm.slop.spirals.navigation.NavigationViewModel

/**
 * ViewModel for the Random Set Editor screen.
 * 
 * This ViewModel manages the state and logic specifically for editing RandomSet templates.
 * It interacts with the NavigationViewModel for navigation and repositories for data access.
 */
class RandomSetEditorViewModel(
    application: Application,
    private val navigationViewModel: NavigationViewModel
) : AndroidViewModel(application) {
    private val database = MandalaDatabase.getDatabase(application)
    private val randomSetRepository = RandomSetRepository(database)
    private val mandalaRepository = MandalaRepository(database)
    
    // The current random set being edited
    private val _currentRandomSet = MutableStateFlow<RandomSet?>(null)
    val currentRandomSet: StateFlow<RandomSet?> = _currentRandomSet.asStateFlow()
    
    // All available random sets for selection
    val allRandomSets = randomSetRepository.getAll()
    
    // All available mandalas for reference recipes
    val allMandalas = mandalaRepository.getAll()
    
    /**
     * Sets the current random set being edited.
     * 
     * @param randomSet The random set to edit
     */
    fun setCurrentRandomSet(randomSet: RandomSet?) {
        _currentRandomSet.value = randomSet
    }
    
    /**
     * Updates the current random set and the navigation layer.
     * 
     * @param randomSet The updated random set
     * @param isDirty Whether to mark the random set as dirty
     */
    fun updateRandomSet(randomSet: RandomSet, isDirty: Boolean = true) {
        _currentRandomSet.value = randomSet
        
        // Update the navigation layer data
        val navStack = navigationViewModel.navStack.value
        val index = navStack.indexOfLast { it.type == LayerType.RANDOM_SET }
        if (index >= 0) {
            navigationViewModel.updateLayerData(index, RandomSetLayerContent(randomSet), isDirty)
        }
    }
    
    /**
     * Updates the recipe filter mode.
     * 
     * @param filter The new filter mode
     */
    fun updateRecipeFilter(filter: RecipeFilter) {
        val randomSet = _currentRandomSet.value ?: return
        val updatedSet = randomSet.copy(recipeFilter = filter)
        updateRandomSet(updatedSet)
    }
    
    /**
     * Updates the petal count for PETALS_EXACT filter mode.
     * 
     * @param count The exact petal count to filter for
     */
    fun updatePetalCount(count: Int) {
        val randomSet = _currentRandomSet.value ?: return
        val updatedSet = randomSet.copy(petalCount = count)
        updateRandomSet(updatedSet)
    }
    
    /**
     * Updates the petal range for PETALS_RANGE filter mode.
     * 
     * @param min The minimum petal count
     * @param max The maximum petal count
     */
    fun updatePetalRange(min: Int, max: Int) {
        val randomSet = _currentRandomSet.value ?: return
        val updatedSet = randomSet.copy(petalMin = min, petalMax = max)
        updateRandomSet(updatedSet)
    }
    
    /**
     * Updates the specific recipe IDs for SPECIFIC_IDS filter mode.
     * 
     * @param recipeIds The list of specific recipe IDs to use
     */
    fun updateSpecificRecipeIds(recipeIds: List<String>) {
        val randomSet = _currentRandomSet.value ?: return
        val updatedSet = randomSet.copy(specificRecipeIds = recipeIds)
        updateRandomSet(updatedSet)
    }
    
    /**
     * Updates the auto hue sweep setting.
     * 
     * @param enabled Whether to automatically set hue sweep to match petal count
     */
    fun updateAutoHueSweep(enabled: Boolean) {
        val randomSet = _currentRandomSet.value ?: return
        val updatedSet = randomSet.copy(autoHueSweep = enabled)
        updateRandomSet(updatedSet)
    }
    
    /**
     * Updates arm constraints for a specific arm.
     * 
     * @param armIndex Which arm to update (1-4)
     * @param constraints The new constraints for the arm
     */
    fun updateArmConstraints(armIndex: Int, constraints: ArmConstraints) {
        val randomSet = _currentRandomSet.value ?: return
        
        var updatedSet = when (armIndex) {
            1 -> randomSet.copy(l1Constraints = constraints)
            2 -> randomSet.copy(l2Constraints = constraints)
            3 -> randomSet.copy(l3Constraints = constraints)
            4 -> randomSet.copy(l4Constraints = constraints)
            else -> return
        }
        
        // If linked and updating L1, update all others
        if (randomSet.linkArms && armIndex == 1) {
            updatedSet = updatedSet.copy(
                l2Constraints = constraints,
                l3Constraints = constraints,
                l4Constraints = constraints
            )
        }
        
        updateRandomSet(updatedSet)
    }
    
    /**
     * Updates the link arms setting.
     * 
     * @param linked Whether to link all arms to L1
     */
    fun updateLinkArms(linked: Boolean) {
        val randomSet = _currentRandomSet.value ?: return
        var updatedSet = randomSet.copy(linkArms = linked)
        
        // If just linked, copy L1 to all others
        if (linked) {
            val l1 = randomSet.l1Constraints
            updatedSet = updatedSet.copy(
                l2Constraints = l1,
                l3Constraints = l1,
                l4Constraints = l1
            )
        }
        
        updateRandomSet(updatedSet)
    }
    
    /**
     * Updates rotation constraints.
     * 
     * @param constraints The new rotation constraints
     */
    fun updateRotationConstraints(constraints: RotationConstraints) {
        val randomSet = _currentRandomSet.value ?: return
        val updatedSet = randomSet.copy(rotationConstraints = constraints)
        updateRandomSet(updatedSet)
    }
    
    /**
     * Updates hue offset constraints.
     * 
     * @param constraints The new hue offset constraints
     */
    fun updateHueOffsetConstraints(constraints: HueOffsetConstraints) {
        val randomSet = _currentRandomSet.value ?: return
        val updatedSet = randomSet.copy(hueOffsetConstraints = constraints)
        updateRandomSet(updatedSet)
    }
    
    /**
     * Saves the current random set to the database.
     */
    fun saveRandomSet() {
        viewModelScope.launch {
            val randomSet = _currentRandomSet.value ?: return@launch
            randomSetRepository.save(randomSet)
            
            // Update the navigation layer to mark it as not dirty
            val navStack = navigationViewModel.navStack.value
            val index = navStack.indexOfLast { it.type == LayerType.RANDOM_SET }
            if (index >= 0) {
                navigationViewModel.updateLayerData(index, RandomSetLayerContent(randomSet), false)
            }
        }
    }
    
    /**
     * Deletes a random set by ID.
     * 
     * @param id The ID of the random set to delete
     */
    fun deleteRandomSet(id: String) {
        viewModelScope.launch {
            randomSetRepository.deleteById(id)
        }
    }
    
    /**
     * Renames a random set.
     * 
     * @param id The ID of the random set to rename
     * @param newName The new name for the random set
     */
    fun renameRandomSet(id: String, newName: String) {
        viewModelScope.launch {
            val randomSet = _currentRandomSet.value ?: return@launch
            if (randomSet.id == id) {
                val updatedRandomSet = randomSet.copy(name = newName)
                randomSetRepository.save(updatedRandomSet)
                _currentRandomSet.value = updatedRandomSet
                
                // Update the navigation layer
                val navStack = navigationViewModel.navStack.value
                val index = navStack.indexOfLast { it.type == LayerType.RANDOM_SET }
                if (index >= 0) {
                    navigationViewModel.updateLayerName(index, newName)
                    navigationViewModel.updateLayerData(index, RandomSetLayerContent(updatedRandomSet), true)
                }
            } else {
                randomSetRepository.renamePatch(id, newName)
            }
        }
    }
    
    /**
     * Clones a random set.
     * 
     * @param id The ID of the random set to clone
     * @param newName The name for the cloned random set
     */
    fun cloneRandomSet(id: String, newName: String) {
        viewModelScope.launch {
            val newId = randomSetRepository.clonePatch(id, newName, java.util.UUID.randomUUID().toString())
            if (newId != null) {
                // If we cloned the current random set, load the new one
                if (id == _currentRandomSet.value?.id) {
                    randomSetRepository.getById(newId)?.let { newRandomSet ->
                        _currentRandomSet.value = newRandomSet
                    }
                }
            }
        }
    }
}