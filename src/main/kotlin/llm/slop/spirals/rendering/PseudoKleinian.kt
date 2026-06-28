package llm.slop.spirals.rendering

import llm.slop.spirals.parameters.ModulatableParameter
import llm.slop.spirals.parameters.MeterType

/**
 * Encapsulates the state and parameters of a 3D Pseudo-Kleinian fractal.
 * Implements VisualSource for desktop rendering.
 */
class PseudoKleinian : VisualSource {
    override val parameters = linkedMapOf(
        "Scale" to ModulatableParameter(1.5f, minClamp = 0.5f, maxClamp = 3.0f),
        "Radius" to ModulatableParameter(1.0f, minClamp = 0.5f, maxClamp = 1.2f),
        "CX" to ModulatableParameter(0.5f, minClamp = -3.0f, maxClamp = 3.0f),
        "CY" to ModulatableParameter(0.5f, minClamp = -3.0f, maxClamp = 3.0f),
        "CZ" to ModulatableParameter(0.5f, minClamp = -3.0f, maxClamp = 3.0f),
        "Rot X" to ModulatableParameter(0.0f, minClamp = -3.14f, maxClamp = 3.14f, meterType = MeterType.ENDLESS),
        "Rot Y" to ModulatableParameter(0.0f, minClamp = -3.14f, maxClamp = 3.14f, meterType = MeterType.ENDLESS),
        "Rot Z" to ModulatableParameter(0.0f, minClamp = -3.14f, maxClamp = 3.14f, meterType = MeterType.ENDLESS),
        "Iterations" to ModulatableParameter(8.0f, minClamp = 1.0f, maxClamp = 15.0f),
        "Zoom" to ModulatableParameter(1.0f, minClamp = 0.1f, maxClamp = 5.0f),
        "Color Shift" to ModulatableParameter(0.0f, minClamp = 0.0f, maxClamp = 1.0f, meterType = MeterType.ENDLESS),
        "Yaw" to ModulatableParameter(0.0f, minClamp = -3.14f, maxClamp = 3.14f, meterType = MeterType.ENDLESS),
        "Pitch" to ModulatableParameter(0.0f, minClamp = -3.14f, maxClamp = 3.14f, meterType = MeterType.ENDLESS),
        "Glow" to ModulatableParameter(0.5f, minClamp = 0.0f, maxClamp = 1.0f)
    )

    override val globalAlpha = ModulatableParameter(1.0f)
}
