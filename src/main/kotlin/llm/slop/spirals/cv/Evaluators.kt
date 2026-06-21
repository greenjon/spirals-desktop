package llm.slop.spirals.cv

import llm.slop.spirals.parameters.CvModulator
import llm.slop.spirals.parameters.LfoSpeedMode
import llm.slop.spirals.parameters.calculateWaveform

fun evaluateModulator(modulator: CvModulator): Float {
    return when (modulator.sourceId) {
        "beatPhase" -> {
            val beats = CVRegistry.getSynchronizedTotalBeats()
            val localPhase = ((beats / modulator.subdivision) + modulator.phaseOffset) % 1.0
            val positivePhase = if (localPhase < 0.0) localPhase + 1.0 else localPhase
            calculateWaveform(modulator.waveform, positivePhase, modulator.slope)
        }
        "sampleAndHold" -> {
            val beats = CVRegistry.getSynchronizedTotalBeats()
            val subdivisionD = modulator.subdivision.toDouble().coerceAtLeast(0.01)
            
            val cyclePosition = (beats / subdivisionD) + modulator.phaseOffset
            val phase = cyclePosition % 1.0
            val positivePhase = if (phase < 0.0) phase + 1.0 else phase

            val currentCycle = kotlin.math.floor(cyclePosition).toInt()
            val previousCycle = currentCycle - 1

            val seed = subdivisionD.hashCode() xor modulator.phaseOffset.hashCode()

            val currentValue = kotlin.random.Random((currentCycle + seed).toLong()).nextFloat()
            val previousValue = kotlin.random.Random((previousCycle + seed).toLong()).nextFloat()

            val glideAmount = if (positivePhase < modulator.slope) {
                (positivePhase / modulator.slope).toFloat().coerceIn(0f, 1f)
            } else {
                1.0f
            }

            previousValue + (currentValue - previousValue) * glideAmount
        }
        "lfo" -> {
            val seconds = CVRegistry.getElapsedRealtimeSec()
            val period = when (modulator.lfoSpeedMode) {
                LfoSpeedMode.FAST -> modulator.subdivision * 10.0
                LfoSpeedMode.MEDIUM -> modulator.subdivision * 900.0
                LfoSpeedMode.SLOW -> modulator.subdivision * 86400.0
            }.coerceAtLeast(0.001)

            val localPhase = ((seconds / period) + modulator.phaseOffset) % 1.0
            val positivePhase = if (localPhase < 0.0) localPhase + 1.0 else localPhase
            calculateWaveform(modulator.waveform, positivePhase, modulator.slope)
        }
        else -> {
            CVRegistry.get(modulator.sourceId)
        }
    }
}
