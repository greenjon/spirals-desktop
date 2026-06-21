package llm.slop.spirals.parameters

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
data class CvModulator(
    val sourceId: String,
    val operator: ModulationOperator = ModulationOperator.ADD,
    val weight: Float = 0.0f,
    val bypassed: Boolean = false,
    // Beat synchronization/shape settings
    val waveform: Waveform = Waveform.SINE,
    val subdivision: Float = 1.0f,
    val phaseOffset: Float = 0.0f,
    val slope: Float = 0.5f,
    // LFO speed bounds setting
    val lfoSpeedMode: LfoSpeedMode = LfoSpeedMode.FAST,

    // Range fields
    val weightMin: Float = weight,
    val weightMax: Float = weight,
    val subdivisionMin: Float = subdivision,
    val subdivisionMax: Float = subdivision,
    val phaseOffsetMin: Float = phaseOffset,
    val phaseOffsetMax: Float = phaseOffset,
    val slopeMin: Float = slope,
    val slopeMax: Float = slope,

    val id: String = UUID.randomUUID().toString()
) {
    fun randomizeActiveValues(random: kotlin.random.Random = kotlin.random.Random.Default): CvModulator {
        val newWeight = if (weightMin == weightMax) weightMin else random.nextFloat() * (weightMax - weightMin) + weightMin
        val newPhase = if (phaseOffsetMin == phaseOffsetMax) phaseOffsetMin else random.nextFloat() * (phaseOffsetMax - phaseOffsetMin) + phaseOffsetMin
        val newSlope = if (slopeMin == slopeMax) slopeMin else random.nextFloat() * (slopeMax - slopeMin) + slopeMin

        val newSubdiv = if (subdivisionMin == subdivisionMax) {
            subdivisionMin
        } else {
            if (sourceId == "beatPhase" || sourceId == "sampleAndHold") {
                val options = floatArrayOf(0.125f, 0.25f, 0.5f, 1f, 2f, 4f, 8f, 16f, 32f, 64f, 128f, 256f)
                val valid = options.filter { it in subdivisionMin..subdivisionMax }
                if (valid.isNotEmpty()) valid.random(random) else subdivisionMin
            } else {
                // LFO Speed is continuous. 0.1s to 10s.
                random.nextFloat() * (subdivisionMax - subdivisionMin) + subdivisionMin
            }
        }

        return this.copy(
            weight = newWeight,
            subdivision = newSubdiv,
            phaseOffset = newPhase,
            slope = newSlope
        )
    }
}
