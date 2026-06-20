package llm.slop.spirals.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import llm.slop.spirals.LayerType
import llm.slop.spirals.MandalaDatabase
import llm.slop.spirals.MixerLayerContent
import llm.slop.spirals.database.repositories.MandalaRepository
import llm.slop.spirals.database.repositories.MixerRepository
import llm.slop.spirals.database.repositories.RandomSetRepository
import llm.slop.spirals.database.repositories.SetRepository
import llm.slop.spirals.models.MixerPatch
import llm.slop.spirals.models.MixerSlotData
import llm.slop.spirals.models.VideoSourceType
import llm.slop.spirals.navigation.NavigationViewModel

/**
 * ViewModel for the Mixer Editor screen.
 * 
 * This ViewModel manages the state and logic specifically for editing Mixer patches.
 * It interacts with the NavigationViewModel for navigation and various repositories for data access.
 */
class MixerEditorViewModel(
    application: Application,
    private val navigationViewModel: NavigationViewModel
) : AndroidViewModel(application) {
    private val database = MandalaDatabase.getDatabase(application)
    private val mixerRepository = MixerRepository(database)
    private val setRepository = SetRepository(database)
    private val mandalaRepository = MandalaRepository(database)
    private val randomSetRepository = RandomSetRepository(database)
    
    // The current mixer being edited
    private val _currentMixer = MutableStateFlow<MixerPatch?>(null)
    val currentMixer: StateFlow<MixerPatch?> = _currentMixer.asStateFlow()
    
    // All available mixers for selection
    val allMixers = mixerRepository.getAll()
    
    // All available mandala sets that can be assigned to slots
    val allSets = setRepository.getAll()
    
    // All available mandalas that can be assigned to slots
    val allMandalas = mandalaRepository.getAll()
    
    // All available random sets that can be assigned to slots
    val allRandomSets = randomSetRepository.getAll()
    
    /**
     * Sets the current mixer being edited.
     * 
     * @param mixer The mixer to edit
     */
    fun setCurrentMixer(mixer: MixerPatch?) {
        _currentMixer.value = mixer
    }
    
    /**
     * Updates the current mixer and the navigation layer.
     * 
     * @param mixer The updated mixer
     * @param isDirty Whether to mark the mixer as dirty
     */
    fun updateMixer(mixer: MixerPatch, isDirty: Boolean = true) {
        _currentMixer.value = mixer
        
        // Update the navigation layer data
        val navStack = navigationViewModel.navStack.value
        val index = navStack.indexOfLast { it.type == LayerType.MIXER }
        if (index >= 0) {
            navigationViewModel.updateLayerData(index, MixerLayerContent(mixer), isDirty)
        }
    }
    
    /**
     * Updates a specific slot in the mixer.
     * 
     * @param slotIndex The index of the slot to update (0-3)
     * @param slotData The new slot data
     */
    fun updateSlot(slotIndex: Int, slotData: MixerSlotData) {
        val mixer = _currentMixer.value ?: return
        if (slotIndex < 0 || slotIndex >= mixer.slots.size) return
        
        val updatedSlots = mixer.slots.toMutableList()
        updatedSlots[slotIndex] = slotData
        
        val updatedMixer = mixer.copy(slots = updatedSlots)
        updateMixer(updatedMixer)
    }
    
    /**
     * Assigns a mandala set to a slot.
     * 
     * @param slotIndex The index of the slot (0-3)
     * @param setId The ID of the mandala set
     */
    fun assignSetToSlot(slotIndex: Int, setId: String) {
        val mixer = _currentMixer.value ?: return
        if (slotIndex < 0 || slotIndex >= mixer.slots.size) return
        
        val updatedSlots = mixer.slots.toMutableList()
        updatedSlots[slotIndex] = updatedSlots[slotIndex].copy(
            mandalaSetId = setId,
            sourceType = VideoSourceType.MANDALA_SET
        )
        
        val updatedMixer = mixer.copy(slots = updatedSlots)
        updateMixer(updatedMixer)
    }
    
    /**
     * Assigns a mandala to a slot.
     * 
     * @param slotIndex The index of the slot (0-3)
     * @param mandalaName The name of the mandala
     */
    fun assignMandalaToSlot(slotIndex: Int, mandalaName: String) {
        val mixer = _currentMixer.value ?: return
        if (slotIndex < 0 || slotIndex >= mixer.slots.size) return
        
        val updatedSlots = mixer.slots.toMutableList()
        updatedSlots[slotIndex] = updatedSlots[slotIndex].copy(
            selectedMandalaId = mandalaName,
            sourceType = VideoSourceType.MANDALA
        )
        
        val updatedMixer = mixer.copy(slots = updatedSlots)
        updateMixer(updatedMixer)
    }
    
    /**
     * Assigns a random set to a slot.
     * 
     * @param slotIndex The index of the slot (0-3)
     * @param randomSetId The ID of the random set
     */
    fun assignRandomSetToSlot(slotIndex: Int, randomSetId: String) {
        val mixer = _currentMixer.value ?: return
        if (slotIndex < 0 || slotIndex >= mixer.slots.size) return
        
        val updatedSlots = mixer.slots.toMutableList()
        updatedSlots[slotIndex] = updatedSlots[slotIndex].copy(
            randomSetId = randomSetId,
            sourceType = VideoSourceType.RANDOM_SET
        )
        
        val updatedMixer = mixer.copy(slots = updatedSlots)
        updateMixer(updatedMixer)
    }
    
    /**
     * Clears a slot, removing any assigned content.
     * 
     * @param slotIndex The index of the slot (0-3)
     */
    fun clearSlot(slotIndex: Int) {
        val mixer = _currentMixer.value ?: return
        if (slotIndex < 0 || slotIndex >= mixer.slots.size) return
        
        val updatedSlots = mixer.slots.toMutableList()
        updatedSlots[slotIndex] = MixerSlotData() // Reset to default
        
        val updatedMixer = mixer.copy(slots = updatedSlots)
        updateMixer(updatedMixer)
    }
    
    /**
     * Saves the current mixer to the database.
     */
    fun saveMixer() {
        viewModelScope.launch {
            val mixer = _currentMixer.value ?: return@launch
            mixerRepository.save(mixer)
            
            // Update the navigation layer to mark it as not dirty
            val navStack = navigationViewModel.navStack.value
            val index = navStack.indexOfLast { it.type == LayerType.MIXER }
            if (index >= 0) {
                navigationViewModel.updateLayerData(index, MixerLayerContent(mixer), false)
            }
        }
    }
    
    /**
     * Deletes a mixer by ID.
     * 
     * @param id The ID of the mixer to delete
     */
    fun deleteMixer(id: String) {
        viewModelScope.launch {
            mixerRepository.deleteById(id)
        }
    }
    
    /**
     * Renames a mixer.
     * 
     * @param id The ID of the mixer to rename
     * @param newName The new name for the mixer
     */
    fun renameMixer(id: String, newName: String) {
        viewModelScope.launch {
            val mixer = _currentMixer.value ?: return@launch
            if (mixer.id == id) {
                val updatedMixer = mixer.copy(name = newName)
                mixerRepository.save(updatedMixer)
                _currentMixer.value = updatedMixer
                
                // Update the navigation layer
                val navStack = navigationViewModel.navStack.value
                val index = navStack.indexOfLast { it.type == LayerType.MIXER }
                if (index >= 0) {
                    navigationViewModel.updateLayerName(index, newName)
                    navigationViewModel.updateLayerData(index, MixerLayerContent(updatedMixer), true)
                }
            } else {
                mixerRepository.renamePatch(id, newName)
            }
        }
    }
    
    /**
     * Clones a mixer.
     * 
     * @param id The ID of the mixer to clone
     * @param newName The name for the cloned mixer
     */
    fun cloneMixer(id: String, newName: String) {
        viewModelScope.launch {
            val newId = mixerRepository.clonePatch(id, newName)
            if (newId != null) {
                // If we cloned the current mixer, load the new one
                if (id == _currentMixer.value?.id) {
                    mixerRepository.getById(newId)?.let { newMixer ->
                        _currentMixer.value = newMixer
                    }
                }
            }
        }
    }
}