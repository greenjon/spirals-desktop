package llm.slop.spirals

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import llm.slop.spirals.cv.core.CvModulator
import llm.slop.spirals.cv.core.ModulationOperator
import llm.slop.spirals.cv.core.Waveform
import llm.slop.spirals.models.ModulatorData
import llm.slop.spirals.models.ParameterData
import llm.slop.spirals.models.PatchData

/**
 * Robust mapper to handle serialization and deserialization of patches.
 * Excludes global mixing parameters (Alpha/Scale) to allow for higher-level mixing later.
 */
object PatchMapper {
    private val jsonConfiguration = Json { ignoreUnknownKeys = true; prettyPrint = true }

    private val legacyMigrations = mapOf(
        "arm1" to "L1",
        "arm2" to "L2",
        "arm3" to "L3",
        "arm4" to "L4"
    )

    fun fromVisualSource(name: String, source: MandalaVisualSource): PatchData {
        val parameters = mutableListOf<ParameterData>()

        // Only include internal geometry and color parameters in the individual patch
        source.parameters.forEach { (id, param) ->
            parameters.add(ParameterData(
                id = id,
                baseValue = param.baseValue,
                modulators = param.modulators.map {
                    ModulatorData(
                        sourceId = it.sourceId,
                        operator = it.operator.name,
                        weight = it.weight,
                        bypassed = it.bypassed,
                        waveform = it.waveform.name,
                        subdivision = it.subdivision,
                        phaseOffset = it.phaseOffset,
                        slope = it.slope
                    )
                }
            ))
        }

        return PatchData(name, source.recipe.id, parameters)
    }

    fun applyToVisualSource(patchData: PatchData, source: MandalaVisualSource) {
        val ratio = MandalaLibrary.MandalaRatios.find { it.id == patchData.recipeId }
        if (ratio != null) source.recipe = ratio

        patchData.parameters.forEach { paramData ->
            val currentId = legacyMigrations[paramData.id] ?: paramData.id

            // We ignore globalAlpha and globalScale here so they aren't overwritten by individual patches
            val param = source.parameters[currentId]

            param?.apply {
                baseValue = paramData.baseValue

                val validModulators = mutableListOf<CvModulator>()
                paramData.modulators.forEach { modData ->
                    try {
                        val op = when(modData.operator.uppercase()) {
                            "MUL" -> ModulationOperator.MUL
                            else -> ModulationOperator.ADD
                        }

                        val wave = try {
                            Waveform.valueOf(modData.waveform.uppercase())
                        } catch (e: Exception) {
                            Waveform.SINE
                        }

                        validModulators.add(CvModulator(
                            sourceId = modData.sourceId,
                            operator = op,
                            weight = modData.weight,
                            bypassed = modData.bypassed,
                            waveform = wave,
                            subdivision = modData.subdivision,
                            phaseOffset = modData.phaseOffset,
                            slope = modData.slope
                        ))
                    } catch (e: Exception) {
                        // Suppress or log exception
                    }
                }

                modulators.clear()
                modulators.addAll(validModulators)
            }
        }
    }

    /**
     * Checks if the visual source has unsaved changes relative to a reference patch.
     */
    fun isDirty(source: MandalaVisualSource, reference: PatchData?): Boolean {
        if (reference == null) return true // Unsaved "New Patch" is dirty

        val currentData = fromVisualSource(reference.name, source)
        // Compare recipe
        if (currentData.recipeId != reference.recipeId) return true

        // Compare parameters (simplified check)
        if (currentData.parameters.size != reference.parameters.size) return true

        val refMap = reference.parameters.associateBy { it.id }
        for (param in currentData.parameters) {
            val refParam = refMap[param.id] ?: return true
            if (param.baseValue != refParam.baseValue) return true
            if (param.modulators.size != refParam.modulators.size) return true
            for (i in param.modulators.indices) {
                if (param.modulators[i] != refParam.modulators[i]) return true
            }
        }

        return false
    }

    fun toJson(patchData: PatchData): String = jsonConfiguration.encodeToString(patchData)

    fun fromJson(jsonStr: String?): PatchData? {
        if (jsonStr == null) return null
        return try {
            jsonConfiguration.decodeFromString<PatchData>(jsonStr)
        } catch (e: Exception) {
            null
        }
    }

    fun allToJson(patches: List<PatchData>): String = jsonConfiguration.encodeToString(patches)
}