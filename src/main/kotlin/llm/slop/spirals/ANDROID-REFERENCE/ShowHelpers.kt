package llm.slop.spirals

import llm.slop.spirals.models.*
import java.util.UUID

/**
 * Creates a temporary mixer patch from a RandomSet.
 * This allows Shows to work with RandomSets directly while reusing existing Mixer rendering.
 */
fun createTemporaryMixerFromRandomSet(randomSet: RandomSet): MixerPatch {
    return MixerPatch(
        id = UUID.randomUUID().toString(),
        name = "Temp_${randomSet.name}",
        slots = List(4) { index ->
            if (index == 0) {
                MixerSlotData(
                    randomSetId = randomSet.id,
                    sourceType = VideoSourceType.RANDOM_SET,
                    enabled = true
                )
            } else {
                MixerSlotData()
            }
        },
        // Default mixer settings
        mixerA = MixerGroupData(),
        mixerB = MixerGroupData(),
        mixerF = MixerGroupData(),
        finalGain = ModulatableParameterData(1.0f),
        masterAlpha = 1.0f,
        effects = MixerFXData()
    )
}