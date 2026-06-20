package llm.slop.spirals

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import llm.slop.spirals.database.entities.*
import llm.slop.spirals.models.*
import llm.slop.spirals.models.set.MandalaSet
import llm.slop.spirals.navigation.NavLayer
import java.util.UUID

/**
 * MandalaViewModel - The central hub for navigation, data persistence, and business logic.
 */
class MandalaViewModel(application: Application) : AndroidViewModel(application) {
    private val db = MandalaDatabase.getDatabase(application)
    private val tagDao = db.mandalaTagDao()
    private val patchDao = db.mandalaPatchDao()
    private val setDao = db.mandalaSetDao()
    private val mixerDao = db.mixerPatchDao()
    private val showDao = db.showPatchDao()
    private val randomSetDao = db.randomSetDao()
    private val appConfig = AppConfig(application)

    val allPatches = patchDao.getAllPatches().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val allSets = setDao.getAllSets().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val allMixerPatches = mixerDao.getAllMixerPatches().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val allShowPatches = showDao.getAllShowPatches().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val allRandomSets = randomSetDao.getAllRandomSets().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _currentPatch = MutableStateFlow<PatchData?>(null)
    val currentPatch: StateFlow<PatchData?> = _currentPatch.asStateFlow()

    // Show state
    private val _currentShowIndex = MutableStateFlow(0)
    val currentShowIndex = _currentShowIndex.asStateFlow()

    private val _showGenerationTrigger = MutableStateFlow(0)
    val showGenerationTrigger = _showGenerationTrigger.asStateFlow()

    private val _navStack = MutableStateFlow<List<NavLayer>>(emptyList())
    val navStack = _navStack.asStateFlow()

    init {
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

    fun generateNextName(type: LayerType): String {
        val (prefix, list) = when (type) {
            LayerType.MIXER -> "Mix" to allMixerPatches.value.map { it.name }
            LayerType.SET -> "Set" to allSets.value.map { it.name }
            LayerType.MANDALA -> "Man" to allPatches.value.map { it.name }
            LayerType.SHOW -> "Show" to allShowPatches.value.map { it.name }
            LayerType.RANDOM_SET -> "Rand" to allRandomSets.value.map { it.name }
        }

        val regex = Regex("${prefix}(\\d+)")
        val maxNum = list.mapNotNull {
            regex.find(it)?.groupValues?.get(1)?.toIntOrNull()
        }.maxOrNull() ?: 0

        return "$prefix${(maxNum + 1).toString().padStart(3, '0')}"
    }

    fun pushLayer(layer: NavLayer) {
        _navStack.value += layer
        saveWorkspaceIfEnabled()
    }

    fun createAndPushLayer(type: LayerType, parentSlotIndex: Int? = null) {
        val name = generateNextName(type)
        val id = UUID.randomUUID().toString()

        val data: LayerContent = when(type) {
            LayerType.MIXER -> MixerLayerContent(MixerPatch(id = id, name = name, slots = List(4) { MixerSlotData() }))
            LayerType.SET -> SetLayerContent(MandalaSet(id = id, name = name, orderedMandalaIds = mutableListOf()))
            LayerType.MANDALA -> MandalaLayerContent(PatchData(name = name, recipeId = MandalaLibrary.MandalaRatios.first().id, parameters = emptyList()))
            LayerType.SHOW -> ShowLayerContent(ShowPatch(id = id, name = name))
            LayerType.RANDOM_SET -> RandomSetLayerContent(RandomSet(id = id, name = name))
        }

        val newLayer = NavLayer(
            id = id,
            name = name,
            type = type,
            isDirty = true,
            data = data,
            parentSlotIndex = parentSlotIndex,
            createdFromParent = true
        )

        pushLayer(newLayer)
        saveLayer(newLayer)

        if (type == LayerType.MANDALA) {
            _currentPatch.value = (data as MandalaLayerContent).patch
        }
    }

    fun createAndResetStack(type: LayerType, openedFromMenu: Boolean = false) {
        val name = getGenericName(type)
        val id = UUID.randomUUID().toString()
        _navStack.value = listOf(NavLayer(id, name, type, isDirty = false, openedFromMenu = openedFromMenu))
        saveWorkspaceIfEnabled()
    }

    fun startNewPatch(type: LayerType) {
        val name = generateNextName(type)
        val id = UUID.randomUUID().toString()
        val data: LayerContent? = when(type) {
            LayerType.MIXER -> MixerLayerContent(MixerPatch(id = id, name = name, slots = List(4) { MixerSlotData() }))
            LayerType.SET -> SetLayerContent(MandalaSet(id = id, name = name, orderedMandalaIds = mutableListOf()))
            LayerType.SHOW -> ShowLayerContent(ShowPatch(id = id, name = name))
            LayerType.MANDALA -> MandalaLayerContent(PatchData(name = name, recipeId = MandalaLibrary.MandalaRatios.first().id, parameters = emptyList()))
            LayerType.RANDOM_SET -> RandomSetLayerContent(RandomSet(id = id, name = name))
        }

        val newLayer = NavLayer(id, name, type, isDirty = true, data = data)

        if (_navStack.value.size == 1 && _navStack.value[0].type == type && _navStack.value[0].data == null) {
            _navStack.value = listOf(newLayer)
        } else {
            pushLayer(newLayer)
        }

        if (type == LayerType.MANDALA) {
            _currentPatch.value = (data as? MandalaLayerContent)?.patch
        }
    }

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

    fun popToLayer(index: Int, save: Boolean = true) {
        if (index < -1) return
        if (index >= _navStack.value.size) return

        if (save) {
            for (i in _navStack.value.lastIndex downTo index + 1) {
                val child = _navStack.value[i]
                if (child.isDirty || child.data != null) {
                    saveLayer(child)
                }
                if (child.createdFromParent && i > 0) {
                    val parentIndex = i - 1
                    linkChildToParent(child, parentIndex)
                }
            }
        }

        val newStack = if (index == -1) {
            emptyList()
        } else {
            _navStack.value.take(index + 1)
        }

        _navStack.value = if (newStack.isEmpty()) {
            val id = UUID.randomUUID().toString()
            listOf(NavLayer(id, getGenericName(LayerType.MIXER), LayerType.MIXER, isDirty = false))
        } else {
            newStack
        }
        saveWorkspaceIfEnabled()
    }

    private fun linkChildToParent(child: NavLayer, parentIndex: Int) {
        if (parentIndex < 0 || parentIndex >= _navStack.value.size) return
        val parent = _navStack.value[parentIndex]
        val parentData = parent.data ?: return

        when (parent.type) {
            LayerType.SHOW -> {
                val show = (parentData as? ShowLayerContent)?.show ?: return
                val randomSet = (child.data as? RandomSetLayerContent)?.randomSet ?: return

                if (!show.randomSetIds.contains(randomSet.id)) {
                    val updatedShow = show.copy(randomSetIds = show.randomSetIds + randomSet.id)
                    updateLayerData(parentIndex, ShowLayerContent(updatedShow), isDirty = true)
                    saveLayer(_navStack.value[parentIndex])
                }
            }
            LayerType.MIXER -> {
                val mixer = (parentData as? MixerLayerContent)?.mixer ?: return
                val slotIndex = child.parentSlotIndex ?: return

                when (child.type) {
                    LayerType.SET -> {
                        val set = (child.data as? SetLayerContent)?.set ?: return
                        val newSlots = mixer.slots.toMutableList()
                        if (newSlots[slotIndex].mandalaSetId != set.id) {
                            newSlots[slotIndex] = newSlots[slotIndex].copy(
                                mandalaSetId = set.id,
                                sourceType = VideoSourceType.MANDALA_SET
                            )
                            val updatedMixer = mixer.copy(slots = newSlots)
                            updateLayerData(parentIndex, MixerLayerContent(updatedMixer), isDirty = true)
                            saveLayer(_navStack.value[parentIndex])
                        }
                    }
                    LayerType.MANDALA -> {
                        val mandala = (child.data as? MandalaLayerContent)?.patch ?: return
                        val newSlots = mixer.slots.toMutableList()
                        if (newSlots[slotIndex].selectedMandalaId != mandala.name) {
                            newSlots[slotIndex] = newSlots[slotIndex].copy(
                                selectedMandalaId = mandala.name,
                                sourceType = VideoSourceType.MANDALA
                            )
                            val updatedMixer = mixer.copy(slots = newSlots)
                            updateLayerData(parentIndex, MixerLayerContent(updatedMixer), isDirty = true)
                            saveLayer(_navStack.value[parentIndex])
                        }
                    }
                    LayerType.RANDOM_SET -> {
                        val randomSet = (child.data as? RandomSetLayerContent)?.randomSet ?: return
                        val newSlots = mixer.slots.toMutableList()
                        if (newSlots[slotIndex].randomSetId != randomSet.id) {
                            newSlots[slotIndex] = newSlots[slotIndex].copy(
                                randomSetId = randomSet.id,
                                sourceType = VideoSourceType.RANDOM_SET
                            )
                            val updatedMixer = mixer.copy(slots = newSlots)
                            updateLayerData(parentIndex, MixerLayerContent(updatedMixer), isDirty = true)
                            saveLayer(_navStack.value[parentIndex])
                        }
                    }
                    else -> {}
                }
            }
            LayerType.SET -> {
                val set = (parentData as? SetLayerContent)?.set ?: return
                val mandala = (child.data as? MandalaLayerContent)?.patch ?: return
                if (!set.orderedMandalaIds.contains(mandala.name)) {
                    val updatedSet = set.copy(orderedMandalaIds = (set.orderedMandalaIds + mandala.name).toMutableList())
                    updateLayerData(parentIndex, SetLayerContent(updatedSet), isDirty = true)
                    saveLayer(_navStack.value[parentIndex])
                }
            }
            else -> {}
        }
    }

    fun saveCurrentLayer(): Boolean {
        val currentLayer = _navStack.value.lastOrNull() ?: return false
        if (currentLayer.data == null) return false
        saveLayer(currentLayer)
        return true
    }

    fun saveLayer(layer: NavLayer) {
        val data = layer.data ?: return
        viewModelScope.launch {
            when (data) {
                is MandalaLayerContent -> {
                    savePatch(data.patch.copy(name = layer.name))
                }
                is SetLayerContent -> {
                    saveSet(data.set.copy(name = layer.name))
                }
                is MixerLayerContent -> {
                    saveMixerPatch(data.mixer.copy(name = layer.name))
                }
                is ShowLayerContent -> {
                    saveShowPatch(data.show.copy(name = layer.name))
                }
                is RandomSetLayerContent -> {
                    saveRandomSet(data.randomSet.copy(name = layer.name))
                }
            }
            val index = _navStack.value.indexOfFirst { it.id == layer.id }
            if (index != -1) {
                _navStack.update { stack ->
                    stack.toMutableList().apply {
                        this[index] = this[index].copy(isDirty = false)
                    }
                }
            }
        }
    }

    fun renameLayer(index: Int, oldName: String, newName: String) {
        if (index < 0 || index >= _navStack.value.size) return
        val layer = _navStack.value[index]

        viewModelScope.launch {
            when (val data = layer.data) {
                is MandalaLayerContent -> deletePatch(oldName)
                is SetLayerContent -> deleteSet(data.set.id)
                is MixerLayerContent -> deleteMixerPatch(data.mixer.id)
                is ShowLayerContent -> deleteShowPatch(data.show.id)
                is RandomSetLayerContent -> deleteRandomSet(data.randomSet.id)
                null -> {}
            }

            val updatedData: LayerContent? = when (val data = layer.data) {
                is MandalaLayerContent -> MandalaLayerContent(data.patch.copy(name = newName))
                is SetLayerContent -> SetLayerContent(data.set.copy(name = newName))
                is MixerLayerContent -> MixerLayerContent(data.mixer.copy(name = newName))
                is ShowLayerContent -> ShowLayerContent(data.show.copy(name = newName))
                is RandomSetLayerContent -> RandomSetLayerContent(data.randomSet.copy(name = newName))
                null -> null
            }

            val current = _navStack.value.toMutableList()
            current[index] = current[index].copy(name = newName, data = updatedData)
            _navStack.value = current

            if (layer.type == LayerType.MANDALA && updatedData is MandalaLayerContent) {
                _currentPatch.value = updatedData.patch
            }

            saveLayer(_navStack.value[index])
            saveWorkspaceIfEnabled()
        }
    }

    fun cloneLayer(index: Int) {
        if (index < 0 || index >= _navStack.value.size) return
        val layer = _navStack.value[index]
        val data = layer.data ?: return

        val newName = NamingUtils.generateCloneName(layer.name, getExistingNames(layer.type))
        val newId = UUID.randomUUID().toString()

        val newData: LayerContent = when (data) {
            is MandalaLayerContent -> MandalaLayerContent(data.patch.copy(name = newName))
            is SetLayerContent -> SetLayerContent(data.set.copy(id = newId, name = newName))
            is MixerLayerContent -> MixerLayerContent(data.mixer.copy(id = newId, name = newName))
            is ShowLayerContent -> ShowLayerContent(data.show.copy(id = newId, name = newName))
            is RandomSetLayerContent -> RandomSetLayerContent(data.randomSet.copy(id = newId, name = newName))
        }

        val newLayer = NavLayer(newId, newName, layer.type, isDirty = true, data = newData)
        pushLayer(newLayer)
        saveLayer(newLayer)
    }

    private fun getExistingNames(type: LayerType): List<String> {
        return when (type) {
            LayerType.MIXER -> allMixerPatches.value.map { it.name }
            LayerType.SET -> allSets.value.map { it.name }
            LayerType.MANDALA -> allPatches.value.map { it.name }
            LayerType.SHOW -> allShowPatches.value.map { it.name }
            LayerType.RANDOM_SET -> allRandomSets.value.map { it.name }
        }
    }

    fun deleteLayerAndPop(index: Int) {
        if (index < 0 || index >= _navStack.value.size) return
        val layer = _navStack.value[index]

        viewModelScope.launch {
            when (val data = layer.data) {
                is MandalaLayerContent -> deletePatch(layer.name)
                is SetLayerContent -> deleteSet(data.set.id)
                is MixerLayerContent -> deleteMixerPatch(data.mixer.id)
                is ShowLayerContent -> deleteShowPatch(data.show.id)
                is RandomSetLayerContent -> deleteRandomSet(data.randomSet.id)
                null -> {}
            }
            popToLayer(index - 1, save = false)
        }
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

    fun setCurrentPatch(patch: PatchData?) {
        _currentPatch.value = patch
    }

    val tags: StateFlow<Map<String, List<String>>> = tagDao.getAllTags()
        .map { list -> list.groupBy({ it.id }, { it.tag }) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    fun savePatch(patchData: PatchData) {
        viewModelScope.launch {
            val json = PatchMapper.toJson(patchData)
            patchDao.insertPatch(MandalaPatchEntity(patchData.name, patchData.recipeId, json))
        }
    }

    fun deletePatch(name: String) {
        viewModelScope.launch { patchDao.deleteByName(name) }
    }

    fun saveSet(mandalaSet: MandalaSet) {
        viewModelScope.launch {
            val entity = MandalaSetEntity(
                id = mandalaSet.id,
                name = mandalaSet.name,
                jsonOrderedMandalaIds = Json.encodeToString(mandalaSet.orderedMandalaIds),
                selectionPolicy = mandalaSet.selectionPolicy.name
            )
            setDao.insertSet(entity)
        }
    }

    fun deleteSet(id: String) {
        viewModelScope.launch { setDao.deleteById(id) }
    }

    fun saveMixerPatch(mixerPatch: MixerPatch) {
        viewModelScope.launch {
            val json = Json.encodeToString(mixerPatch)
            mixerDao.insertMixerPatch(MixerPatchEntity(mixerPatch.id, mixerPatch.name, json))
        }
    }

    fun deleteMixerPatch(id: String) {
        viewModelScope.launch { mixerDao.deleteById(id) }
    }

    fun saveShowPatch(showPatch: ShowPatch) {
        viewModelScope.launch {
            val json = Json.encodeToString(showPatch)
            showDao.insertShowPatch(ShowPatchEntity(showPatch.id, showPatch.name, json))
        }
    }

    fun deleteShowPatch(id: String) {
        viewModelScope.launch { showDao.deleteById(id) }
    }

    fun saveRandomSet(randomSet: RandomSet) {
        viewModelScope.launch {
            val json = Json.encodeToString(randomSet)
            randomSetDao.insertRandomSet(RandomSetEntity(randomSet.id, randomSet.name, json))
        }
    }

    fun deleteRandomSet(id: String) {
        viewModelScope.launch { randomSetDao.deleteById(id) }
    }

    fun toggleTag(id: String, tag: String) {
        viewModelScope.launch {
            val allForId = tagDao.getAllTagsForId(id)
            val existing = allForId.find { it.tag == tag }
            if (existing != null) {
                tagDao.deleteTag(existing)
            } else {
                val ratings = listOf("trash", "1", "2", "3")
                if (tag in ratings) {
                    allForId.filter { it.tag in ratings }.forEach { tagDao.deleteTag(it) }
                }
                tagDao.insertTag(MandalaTag(id, tag))
            }
        }
    }

    fun getExportData(): String {
        val currentTags = tags.value
        if (currentTags.isEmpty()) return "No tags recorded."
        val sb = StringBuilder("ID,Tags\n")
        currentTags.forEach { (id, tagsList) -> sb.append("$id,${tagsList.joinToString("|")}\n") }
        return sb.toString()
    }

    fun renameSavedPatch(type: LayerType, id: String, newName: String) {
        viewModelScope.launch {
            when (type) {
                LayerType.MANDALA -> { // Special case: id is the name
                    val entity = allPatches.value.find { it.name == id }
                    if (entity != null) {
                        patchDao.deleteByName(id)
                        val patch = PatchMapper.fromJson(entity.jsonSettings)?.copy(name = newName)
                        if (patch != null) {
                            val newJson = PatchMapper.toJson(patch)
                            patchDao.insertPatch(entity.copy(name = newName, jsonSettings = newJson))
                        }
                    }
                }
                LayerType.SET -> {
                    val entity = allSets.value.find { it.id == id }
                    if (entity != null) {
                        setDao.insertSet(entity.copy(name = newName))
                    }
                }
                LayerType.MIXER -> {
                    val entity = allMixerPatches.value.find { it.id == id }
                    if (entity != null) {
                        val mixer = Json.decodeFromString<MixerPatch>(entity.jsonSettings)
                        val newMixer = mixer.copy(name = newName)
                        mixerDao.insertMixerPatch(entity.copy(name = newName, jsonSettings = Json.encodeToString(newMixer)))
                    }
                }
                LayerType.SHOW -> {
                    val entity = allShowPatches.value.find { it.id == id }
                    if (entity != null) {
                        val show = Json.decodeFromString<ShowPatch>(entity.jsonSettings)
                        val newShow = show.copy(name = newName)
                        showDao.insertShowPatch(entity.copy(name = newName, jsonSettings = Json.encodeToString(newShow)))
                    }
                }
                LayerType.RANDOM_SET -> {
                    val entity = allRandomSets.value.find { it.id == id }
                    if (entity != null) {
                        val randomSet = Json.decodeFromString<RandomSet>(entity.jsonSettings)
                        val newRandomSet = randomSet.copy(name = newName)
                        randomSetDao.insertRandomSet(entity.copy(name = newName, jsonSettings = Json.encodeToString(newRandomSet)))
                    }
                }
            }
        }
    }

    fun cloneSavedPatch(type: LayerType, id: String) {
        viewModelScope.launch {
            val (name, existingNames) = when (type) {
                LayerType.MANDALA -> allPatches.value.find { it.name == id }?.name to allPatches.value.map { it.name }
                LayerType.SET -> allSets.value.find { it.id == id }?.name to allSets.value.map { it.name }
                LayerType.MIXER -> allMixerPatches.value.find { it.id == id }?.name to allMixerPatches.value.map { it.name }
                LayerType.SHOW -> allShowPatches.value.find { it.id == id }?.name to allShowPatches.value.map { it.name }
                LayerType.RANDOM_SET -> allRandomSets.value.find { it.id == id }?.name to allRandomSets.value.map { it.name }
            }
            if (name == null) return@launch

            val newName = NamingUtils.generateCloneName(name, existingNames)
            val newId = UUID.randomUUID().toString()

            when (type) {
                LayerType.MANDALA -> { // id is name
                    val entity = allPatches.value.find { it.name == id }
                    if (entity != null) {
                        val patch = PatchMapper.fromJson(entity.jsonSettings)?.copy(name = newName)
                        if (patch != null) {
                            val newJson = PatchMapper.toJson(patch)
                            patchDao.insertPatch(entity.copy(name = newName, jsonSettings = newJson))
                        }
                    }
                }
                LayerType.SET -> {
                    val entity = allSets.value.find { it.id == id }
                    if (entity != null) {
                        setDao.insertSet(entity.copy(id = newId, name = newName))
                    }
                }
                LayerType.MIXER -> {
                    val entity = allMixerPatches.value.find { it.id == id }
                    if (entity != null) {
                        val mixer = Json.decodeFromString<MixerPatch>(entity.jsonSettings)
                        val newMixer = mixer.copy(id = newId, name = newName)
                        mixerDao.insertMixerPatch(MixerPatchEntity(newId, newName, Json.encodeToString(newMixer)))
                    }
                }
                LayerType.SHOW -> {
                    val entity = allShowPatches.value.find { it.id == id }
                    if (entity != null) {
                        val show = Json.decodeFromString<ShowPatch>(entity.jsonSettings)
                        val newShow = show.copy(id = newId, name = newName)
                        showDao.insertShowPatch(ShowPatchEntity(newId, newName, Json.encodeToString(newShow)))
                    }
                }
                LayerType.RANDOM_SET -> {
                    val entity = allRandomSets.value.find { it.id == id }
                    if (entity != null) {
                        val randomSet = Json.decodeFromString<RandomSet>(entity.jsonSettings)
                        val newRandomSet = randomSet.copy(id = newId, name = newName)
                        randomSetDao.insertRandomSet(RandomSetEntity(newId, newName, Json.encodeToString(newRandomSet)))
                    }
                }
            }
        }
    }

    fun deleteSavedPatch(type: LayerType, id: String) {
        viewModelScope.launch {
            when (type) {
                LayerType.MANDALA -> patchDao.deleteByName(id) // id is name
                LayerType.SET -> setDao.deleteById(id)
                LayerType.MIXER -> mixerDao.deleteById(id)
                LayerType.SHOW -> showDao.deleteById(id)
                LayerType.RANDOM_SET -> randomSetDao.deleteById(id)
            }
        }
    }

    fun jumpToShowIndex(index: Int) {
        _currentShowIndex.value = index
    }

    fun triggerNextMixer(size: Int) {
        if (size == 0) return
        _currentShowIndex.value = (_currentShowIndex.value + 1) % size
    }

    fun triggerPrevMixer(size: Int) {
        if (size == 0) return
        _currentShowIndex.value = if (_currentShowIndex.value <= 0) size - 1 else _currentShowIndex.value - 1
    }

    fun triggerShowGenerate() {
        _showGenerationTrigger.value++
    }
}
