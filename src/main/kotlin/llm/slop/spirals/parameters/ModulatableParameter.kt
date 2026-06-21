package llm.slop.spirals.parameters

import llm.slop.spirals.cv.CvHistoryBuffer
import llm.slop.spirals.cv.evaluateModulator
import java.util.concurrent.CopyOnWriteArrayList

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

    var baseMin: Float = baseValue
    var baseMax: Float = baseValue

    var value: Float = baseValue
        private set

    /**
     * Randomizes the static baseValue within the [baseMin, baseMax] range.
     */
    fun randomizeBaseValue(random: kotlin.random.Random = kotlin.random.Random.Default) {
        baseValue = if (baseMin == baseMax) baseMin else random.nextFloat() * (baseMax - baseMin) + baseMin
    }

    /**
     * Calculates the final value by combining the base value with all active modulators.
     * Called once per frame prior to rendering.
     */
    fun evaluate(): Float {
        val activeMods = modulators.filter { !it.bypassed }
        if (activeMods.isEmpty()) {
            value = baseValue.coerceIn(0f, 1f)
            history.add(value)
            return value
        }

        var result = baseValue

        for (mod in activeMods) {
            val finalCv = evaluateModulator(mod)
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
        baseMin = newValue
        baseMax = newValue
    }
}
