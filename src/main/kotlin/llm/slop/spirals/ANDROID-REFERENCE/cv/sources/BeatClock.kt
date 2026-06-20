package llm.slop.spirals.cv.sources

import llm.slop.spirals.cv.CvSignal

/**
 * Logic for tracking beat phase based on BPM.
 */
class BeatClock(var bpm: Float = 120f) {
    /**
     * Calculates the phase (0.0 to 1.0) based on the provided time.
     * This implementation provides a deterministic phase for any given time.
     */
    fun getPhase(timeSeconds: Double): Float {
        val beatsPerSecond = bpm / 60.0
        val totalBeats = timeSeconds * beatsPerSecond
        return (totalBeats % 1.0).toFloat()
    }
}

/**
 * A CV signal that wraps a BeatClock and returns the current phase.
 */
class BeatPhaseCv(private val beatClock: BeatClock) : CvSignal {
    override fun getValue(timeSeconds: Double): Float {
        return beatClock.getPhase(timeSeconds)
    }
}
