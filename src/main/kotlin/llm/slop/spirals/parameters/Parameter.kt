package llm.slop.spirals.parameters

import kotlinx.serialization.Serializable
import llm.slop.spirals.cv.CVRegistry
import llm.slop.spirals.cv.CvHistoryBuffer
import java.util.concurrent.CopyOnWriteArrayList

@Serializable
enum class ModulationOperator {
    ADD, MUL
}

@Serializable
enum class Waveform {
    SINE, TRIANGLE, SQUARE
}

@Serializable
enum class LfoSpeedMode {
    SLOW, MEDIUM, FAST
}

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
    val lfoSpeedMode: LfoSpeedMode = LfoSpeedMode.FAST
)

/**
 * A parameter that can be modulated by a base value and multiple CV sources.
 * Keeps a sliding history of its evaluated values.
 */
class ModulatableParameter(
    var baseValue: Float = 0.0f,
    val historySize: Int = 200
) {
    val modulators = CopyOnWriteArrayList<CvModulator>()
    val history = CvHistoryBuffer(historySize)

    var value: Float = baseValue
        private set

    /**
     * Calculates the final value by combining the base value with all active modulators.
     * Called once per frame prior to rendering.
     */
    fun evaluate(): Float {
        var result = baseValue

        for (mod in modulators) {
            if (mod.bypassed) continue

            val finalCv = when (mod.sourceId) {
                "beatPhase" -> {
                    val beats = CVRegistry.getSynchronizedTotalBeats()
                    val localPhase = ((beats / mod.subdivision) + mod.phaseOffset) % 1.0
                    val positivePhase = if (localPhase < 0.0) localPhase + 1.0 else localPhase
                    calculateWaveform(mod.waveform, positivePhase, mod.slope)
                }
                "sampleAndHold" -> {
                    val beats = CVRegistry.getSynchronizedTotalBeats()
                    val subdivision = mod.subdivision.toDouble().coerceAtLeast(0.01)
                    
                    val cyclePosition = (beats / subdivision) + mod.phaseOffset
                    val phase = cyclePosition % 1.0
                    val positivePhase = if (phase < 0.0) phase + 1.0 else phase

                    val currentCycle = kotlin.math.floor(cyclePosition).toInt()
                    val previousCycle = currentCycle - 1

                    // Deterministic seed based on subdivision and phase offset hash codes
                    val seed = subdivision.hashCode() xor mod.phaseOffset.hashCode()

                    val currentValue = kotlin.random.Random((currentCycle + seed).toLong()).nextFloat()
                    val previousValue = kotlin.random.Random((previousCycle + seed).toLong()).nextFloat()

                    val glideAmount = if (positivePhase < mod.slope) {
                        (positivePhase / mod.slope).toFloat().coerceIn(0f, 1f)
                    } else {
                        1.0f
                    }

                    previousValue + (currentValue - previousValue) * glideAmount
                }
                "lfo" -> {
                    val seconds = CVRegistry.getElapsedRealtimeSec()
                    val period = when (mod.lfoSpeedMode) {
                        LfoSpeedMode.FAST -> mod.subdivision * 10.0 // 0 to 10s
                        LfoSpeedMode.MEDIUM -> mod.subdivision * 900.0 // 0 to 15m
                        LfoSpeedMode.SLOW -> mod.subdivision * 86400.0 // 0 to 24h
                    }.coerceAtLeast(0.001)

                    val localPhase = ((seconds / period) + mod.phaseOffset) % 1.0
                    val positivePhase = if (localPhase < 0.0) localPhase + 1.0 else localPhase
                    calculateWaveform(mod.waveform, positivePhase, mod.slope)
                }
                else -> {
                    // Query the global CVRegistry for audio signals or other modules
                    CVRegistry.get(mod.sourceId)
                }
            }

            val modAmount = finalCv * mod.weight

            result = when (mod.operator) {
                ModulationOperator.ADD -> result + modAmount
                ModulationOperator.MUL -> result * (1.0f + modAmount)
            }
        }

        // Clamp the final parameter output to standard unit range [0.0, 1.0]
        value = result.coerceIn(0f, 1f)
        history.add(value)
        return value
    }

    /**
     * Directly updates the base value (e.g. from UI sliders).
     */
    fun set(newValue: Float) {
        baseValue = newValue
    }

    private fun calculateWaveform(waveform: Waveform, phase: Double, slope: Float): Float {
        return when (waveform) {
            Waveform.SINE -> (kotlin.math.sin(phase * 2.0 * Math.PI).toFloat() * 0.5f) + 0.5f
            Waveform.TRIANGLE -> {
                val s = slope.toDouble()
                if (s <= 0.001) (1.0 - phase).toFloat()
                else if (s >= 0.999) phase.toFloat()
                else if (phase < s) (phase / s).toFloat()
                else ((1.0 - phase) / (1.0 - s)).toFloat()
            }
            Waveform.SQUARE -> if (phase < slope) 1.0f else 0.0f
        }
    }
}
