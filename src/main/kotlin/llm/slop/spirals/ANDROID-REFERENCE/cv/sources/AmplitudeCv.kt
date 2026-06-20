package llm.slop.spirals.cv.sources

import llm.slop.spirals.cv.processors.EnvelopeFollower
import llm.slop.spirals.cv.CvSignal

/**
 * A CV signal that provides a smoothed linear energy value from an audio stream.
 * It reports energy relative to a reference level.
 */
class AmplitudeCv(
    updateRate: Float = 60f,
    attackMs: Float = 15f,
    releaseMs: Float = 150f,
    var referenceLevel: Float = 0.1f // RMS level that maps to 1.0
) : CvSignal {

    private val follower = EnvelopeFollower(updateRate, attackMs, releaseMs)

    @Volatile
    private var latestValue: Float = 0f

    /**
     * Update the amplitude with a raw RMS value.
     */
    fun update(rawRms: Float) {
        val smoothed = follower.update(rawRms)
        // Linear mapping: 1.0 energy = referenceLevel RMS
        latestValue = smoothed / referenceLevel
    }

    /**
     * Adjust smoothing parameters in real-time.
     */
    fun setSmoothing(attackMs: Float, releaseMs: Float) {
        follower.attackMs = attackMs
        follower.releaseMs = releaseMs
        follower.updateCoefficients()
    }

    override fun getValue(timeSeconds: Double): Float {
        return latestValue
    }
}
