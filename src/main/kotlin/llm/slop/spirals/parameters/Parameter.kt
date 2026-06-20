package llm.slop.spirals.parameters

/**
 * A parameter that can be modulated.
 * In Phase 1, it simply evaluates to its base value.
 * In Phase 3, this will integrate CV modulators.
 */
class ModulatableParameter(
    var baseValue: Float = 0.0f
) {
    var value: Float = baseValue
        private set

    /**
     * Evaluates the parameter value.
     */
    fun evaluate(): Float {
        value = baseValue
        return value
    }

    /**
     * Directly set the parameter value.
     */
    fun set(newValue: Float) {
        baseValue = newValue
        value = newValue
    }
}
