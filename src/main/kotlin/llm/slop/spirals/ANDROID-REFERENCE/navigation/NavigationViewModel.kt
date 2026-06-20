package llm.slop.spirals.navigation

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import llm.slop.spirals.AppConfig
import llm.slop.spirals.LayerType
import llm.slop.spirals.navigation.NavLayer
import llm.slop.spirals.StartupMode
import llm.slop.spirals.LayerContent
import llm.slop.spirals.MandalaLayerContent
import llm.slop.spirals.SetLayerContent
import llm.slop.spirals.MixerLayerContent
import llm.slop.spirals.ShowLayerContent
import llm.slop.spirals.RandomSetLayerContent
import java.util.UUID

/**
 * NavigationViewModel - Responsible for managing the navigation stack and breadcrumb cascade system.
 * 
 * This ViewModel is extracted from the original MandalaViewModel to separate navigation concerns
 * from data persistence and business logic. It manages the navigation stack, handles breadcrumb
 * navigation, and implements the cascade system for auto-linking children to parents.
 * 
 * KEY CONCEPTS:
 * 
 * Navigation Stack (_navStack):
 * - List<NavLayer> representing the editing path
 * - Example: [Show1, Mix001, Set001] = editing Set001 within Mix001 within Show1
 * - Each layer tracks its data, dirty state, and parent relationship
 * 
 * Breadcrumb Cascade System:
 * - When user clicks parent breadcrumb, cascade save/link happens
 * - Walks from current layer down to target, saving and linking each
 * - See popToLayer() and onLayerSaved() for implementation
 * - Enables fast workflow: create child, edit, click parent breadcrumb = auto-linked
 */
class NavigationViewModel(application: Application) : AndroidViewModel(application) {
    private val appConfig = AppConfig(application)
    
    // Navigation Stack - The heart of the breadcrumb system
    private val _navStack = MutableStateFlow<List<NavLayer>>(emptyList())
    val navStack = _navStack.asStateFlow()

    init {
        // Initialize navigation stack
        _navStack.value = initialStack()
    }

    private fun initialStack(): List<NavLayer> {
        val mode = appConfig.startupMode
        if (mode == StartupMode.LAST_WORKSPACE) {
            val saved = appConfig.loadNavStack()
            if (!saved.isNullOrEmpty()) return saved
        }
        
        val type = when (mode) {
            StartupMode.MIXER -> LayerType.MIXER
            StartupMode.SET -> LayerType.SET
            StartupMode.MANDALA -> LayerType.MANDALA
            StartupMode.SHOW -> LayerType.SHOW
            else -> LayerType.MIXER
        }
        
        return listOf(NavLayer(UUID.randomUUID().toString(), getGenericName(type), type, isDirty = false))
    }

    fun getGenericName(type: LayerType): String = when(type) {
        LayerType.MIXER -> "Mixer Editor"
        LayerType.SET -> "Set Editor"
        LayerType.MANDALA -> "Mandala Editor"
        LayerType.SHOW -> "Show Editor"
        LayerType.RANDOM_SET -> "RSet Editor"
    }

    fun pushLayer(layer: NavLayer) {
        _navStack.value += layer
        saveWorkspaceIfEnabled()
    }

    /**
     * Creates a new layer and pushes it onto the navigation stack.
     * 
     * This is the primary way to create new patches from within a parent context.
     * Example: User is editing Mix001 and clicks "Create new Set" on slot 2.
     * 
     * IMPORTANT BEHAVIORS:
     * - Creates initial layer with defaults
     * - Sets createdFromParent=true for auto-linking on breadcrumb navigation
     * - Stores parentSlotIndex for Mixer children (which slot to insert into)
     * 
     * @param type The layer type to create (SHOW, MIXER, SET, or MANDALA)
     * @param name The name to give the new layer
     * @param data The initial data for the layer
     * @param parentSlotIndex For Mixer children: which slot (0-3) this belongs to.
     *                        Null for non-Mixer parents or creating at root level.
     */
    fun createAndPushLayer(type: LayerType, name: String, data: LayerContent?, parentSlotIndex: Int? = null) {
        val id = UUID.randomUUID().toString()
        
        val newLayer = NavLayer(
            id = id, 
            name = name, 
            type = type, 
            isDirty = true, 
            data = data,
            parentSlotIndex = parentSlotIndex,
            createdFromParent = true  // Mark as created from parent for auto-linking
        )
        
        pushLayer(newLayer)
    }

    fun createAndResetStack(type: LayerType, openedFromMenu: Boolean = false) {
        val name = getGenericName(type)
        val id = UUID.randomUUID().toString()
        _navStack.value = listOf(NavLayer(id, name, type, isDirty = false, openedFromMenu = openedFromMenu))
        saveWorkspaceIfEnabled()
    }

    /**
     * Updates the data associated with a specific layer in the stack.
     * 
     * ⚠️ IMPORTANT: This is IN-MEMORY ONLY - does NOT save to database!
     * 
     * @param index Index of the layer in navStack to update
     * @param data The new LayerContent (Mandala/Set/Mixer/Show data)
     * @param isDirty Optional dirty flag override
     */
    fun updateLayerData(index: Int, data: LayerContent?, isDirty: Boolean? = null) {
        if (index < 0 || index >= _navStack.value.size) return
        val current = _navStack.value.toMutableList()
        val updatedLayer = current[index]
        val newLayer = updatedLayer.copy(
            data = data,
            isDirty = isDirty ?: updatedLayer.isDirty
        )
        current[index] = newLayer
        _navStack.value = current
    }

    fun updateLayerName(index: Int, newName: String) {
        if (index < 0 || index >= _navStack.value.size) return
        val current = _navStack.value.toMutableList()
        current[index] = current[index].copy(name = newName)
        _navStack.value = current
        saveWorkspaceIfEnabled()
    }
    
    fun clearOpenedFromMenuFlag(index: Int) {
        if (index < 0 || index >= _navStack.value.size) return
        val current = _navStack.value.toMutableList()
        current[index] = current[index].copy(openedFromMenu = false)
        _navStack.value = current
    }

    /**
     * Pops the navigation stack to a specific layer.
     * 
     * This is the CORE of the breadcrumb cascade system. When a user clicks a parent
     * breadcrumb, this function:
     * 1. Walks from current layer DOWN to target layer
     * 2. Triggers onLayerSaved for each layer (which repositories will implement)
     * 3. Pops everything above the target
     * 
     * @param index The target layer index to pop to (0-based)
     * @param save If true, performs cascade save. If false, just pops (for "Discard")
     * @param onLayerSaved Callback for saving each layer as we walk back up. This will be
     *                    implemented by repositories to handle actual data persistence.
     * @param onLinkChildToParent Callback to handle linking child to parent. This will be
     *                          implemented by repositories.
     */
    fun popToLayer(
        index: Int, 
        save: Boolean = true,
        onLayerSaved: (NavLayer) -> Unit = {},
        onLinkChildToParent: (child: NavLayer, parentIndex: Int) -> Unit = { _, _ -> }
    ) {
        if (index < -1) return
        if (index >= _navStack.value.size) return
        
        if (save) {
            // Process layers from current down to target+1, saving and linking
            // IMPORTANT: Walk backwards so children are saved before parents are updated
            for (i in _navStack.value.lastIndex downTo index + 1) {
                val child = _navStack.value[i]
                
                // Save the child layer to database
                if (child.isDirty || child.data != null) {
                    onLayerSaved(child)
                }
                
                // If created from parent, link it back to parent's collection
                if (child.createdFromParent && i > 0) {
                    val parentIndex = i - 1
                    onLinkChildToParent(child, parentIndex)
                }
            }
        }
        
        val newStack = if (index == -1) {
            emptyList()
        } else {
            _navStack.value.take(index + 1)
        }
        
        _navStack.value = if (newStack.isEmpty()) {
            // If the stack is emptied, default back to a Mixer hub
            val id = UUID.randomUUID().toString()
            listOf(NavLayer(id, getGenericName(LayerType.MIXER), LayerType.MIXER, isDirty = false))
        } else {
            newStack
        }
        saveWorkspaceIfEnabled()
    }

    fun clearDirtyFlag(index: Int) {
        if (index < 0 || index >= _navStack.value.size) return
        val current = _navStack.value.toMutableList()
        current[index] = current[index].copy(isDirty = false)
        _navStack.value = current
    }

    private fun saveWorkspaceIfEnabled() {
        if (appConfig.startupMode == StartupMode.LAST_WORKSPACE) {
            appConfig.saveNavStack(_navStack.value)
        }
    }

    fun getStartupMode(): StartupMode = appConfig.startupMode
    
    fun setStartupMode(mode: StartupMode) {
        appConfig.startupMode = mode
        saveWorkspaceIfEnabled()
    }
}