package llm.slop.spirals.parameters

fun calculateWaveform(waveform: Waveform, phase: Double, slope: Float): Float {
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
