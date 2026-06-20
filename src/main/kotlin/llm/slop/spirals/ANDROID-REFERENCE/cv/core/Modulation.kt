package llm.slop.spirals.cv.core

import llm.slop.spirals.cv.visualizers.CvHistoryBuffer
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.serialization.Serializable
import llm.slop.spirals.cv.sources.SampleAndHoldCv

enum class ModulationOperator {
    ADD, MUL
}

enum class Waveform {
    SINE, TRIANGLE, SQUARE
}

enum class LfoSpeedMode {
    SLOW, MEDIUM, FAST
}

@Serializable
data class CvModulator(
    val sourceId: String,
    val operator: ModulationOperator = ModulationOperator.ADD,
    val weight: Float = 0.0f,
    val bypassed: Boolean = false,
    // Beat expansion fields
    val waveform: Waveform = Waveform.SINE,
    val subdivision: Float = 1.0f,
    val phaseOffset: Float = 0.0f,
    val slope: Float = 0.5f,
    // LFO expansion fields
    val lfoSpeedMode: LfoSpeedMode = LfoSpeedMode.FAST
)

/**
 * A parameter that can be controlled by a base value and multiple CV modulators.
 */
class ModulatableParameter(
    var baseValue: Float = 0.0f,
    val historySize: Int = 200
) {
    val modulators = CopyOnWriteArrayList<CvModulator>()
    val history = CvHistoryBuffer(historySize)
    
    var value: Float = baseValue
        private set

    // Pulse logic for manual triggering via UI
    @Volatile
    private var pulseCountdown = 0
    
    /**
     * Triggers a manual pulse (dip then spike) to ensure a rising edge 
     * is detected by trigger logic even if currently modulated high.
     */
    fun triggerPulse() {
        pulseCountdown = 12 // 6 frames low, 6 frames high (~100ms at 120Hz)
    }

    /**
     * Calculates the final value. Called at 120Hz from the Renderer.
     */
    fun evaluate(): Float {
        var result = baseValue
        
        for (mod in modulators) {
            if (mod.bypassed) continue
            
            val finalCv = when (mod.sourceId) {
                "beatPhase" -> {
                    val beats = ModulationRegistry.getSynchronizedTotalBeats()
                    val localPhase = ((beats / mod.subdivision) + mod.phaseOffset) % 1.0
                    val positivePhase = if (localPhase < 0) (localPhase + 1.0) else localPhase
                    calculateWaveform(mod.waveform, positivePhase, mod.slope)
                }
                "sampleAndHold" -> {
                    val beats = ModulationRegistry.getSynchronizedTotalBeats()
                    val subdivision = mod.subdivision.toDouble().coerceAtLeast(0.01)
                    
                    // The S&H implementation now handles its own phase/cycle calculation internally
                    // using the phaseOffset to shift the random seed and the sampling points.
                    ModulationRegistry.sampleAndHold.getValue(
                        totalBeats = beats,
                        subdivision = subdivision,
                        phaseOffset = mod.phaseOffset,
                        slope = mod.slope
                    )
                }
                "lfo" -> {
                    val seconds = ModulationRegistry.getElapsedRealtimeSec()
                    val period = when (mod.lfoSpeedMode) {
                        LfoSpeedMode.FAST -> mod.subdivision * 10.0 // 0 to 10s
                        LfoSpeedMode.MEDIUM -> mod.subdivision * 900.0 // 0 to 15m
                        LfoSpeedMode.SLOW -> mod.subdivision * 86400.0 // 0 to 24h
                    }.coerceAtLeast(0.001)
                    
                    val localPhase = ((seconds / period) + mod.phaseOffset) % 1.0
                    val positivePhase = if (localPhase < 0) (localPhase + 1.0) else localPhase
                    calculateWaveform(mod.waveform, positivePhase, mod.slope)
                }
                else -> ModulationRegistry.get(mod.sourceId)
            }
            
            val modAmount = finalCv * mod.weight
            
            result = when (mod.operator) {
                ModulationOperator.ADD -> result + modAmount
                ModulationOperator.MUL -> result * (1.0f + modAmount)
            }
        }
        
        // Apply pulse override
        val pulse = pulseCountdown
        if (pulse > 0) {
            value = if (pulse > 6) 0.0f else 1.0f
            pulseCountdown = pulse - 1
        } else {
            value = result.coerceIn(0f, 1f)
        }
        
        history.add(value)
        return value
    }

    private fun calculateWaveform(waveform: Waveform, phase: Double, slope: Float): Float {
        return when(waveform) {
            Waveform.SINE -> (sin(phase * 2.0 * Math.PI).toFloat() * 0.5f) + 0.5f
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
