package llm.slop.spirals.navigation

import kotlinx.serialization.Serializable
import llm.slop.spirals.LayerContent
import llm.slop.spirals.LayerType

/**
 * NavLayer represents one level in the navigation/editing stack.
 * 
 * The stack represents the user's "editing path" through the hierarchy.
 * Example: [Show1, Mix001, Set001] means the user is editing Set001 within Mix001 within Show1.
 * 
 * KEY CONCEPT: Breadcrumb Cascade System
 * When a user clicks a parent breadcrumb, the system walks from the current layer down to the
 * target, saving each layer and linking children to parents. This creates a seamless workflow
 * where navigation completes the "add to parent" action.
 * 
 * @property id Unique identifier for this layer instance
 * @property name Display name (may differ from data.name due to timing during renames)
 * @property type The level type (SHOW, MIXER, SET, or MANDALA)
 * @property isDirty Has unsaved changes (though we auto-save, this tracks user modifications)
 * @property data The actual patch content (null for generic/empty editors)
 * @property parentSlotIndex For Mixer children: which slot (0-3) to insert into when linking.
 *                           Null for non-Mixer parents or root layers.
 * @property createdFromParent If true, clicking parent breadcrumb will auto-link this to parent.
 *                            Set to false when editing existing items (vs creating new ones).
 * @property openedFromMenu If true, the Manage overlay should be shown initially.
 *                          Set to true when opening via menu, false when navigating from parent.
 * 
 * NOTE TO FUTURE AI: The breadcrumb cascade system is central to the UX. When modifying
 * navigation logic, ensure that:
 * 1. createdFromParent correctly tracks creation vs editing
 * 2. parentSlotIndex is set when creating from Mixer slots
 * 3. linkChildToParent checks for duplicates before adding
 * See DESIGN.md for full explanation of the cascade system.
 */
@Serializable
data class NavLayer(
    val id: String,
    val name: String,
    val type: LayerType,
    val isDirty: Boolean = false,
    val data: LayerContent? = null,
    val parentSlotIndex: Int? = null,
    val createdFromParent: Boolean = false,
    val openedFromMenu: Boolean = false
)
