package llm.slop.liquidlsd.rendering

import llm.slop.liquidlsd.parameters.ModulatableParameter

data class Vector3(val x: Float, val y: Float, val z: Float)

fun lerp(start: Float, stop: Float, amount: Float): Float {
    return start + (stop - start) * amount
}

fun lerp(v1: Vector3, v2: Vector3, amount: Float): Vector3 {
    return Vector3(
        lerp(v1.x, v2.x, amount),
        lerp(v1.y, v2.y, amount),
        lerp(v1.z, v2.z, amount)
    )
}

class Kifs(
    id: String,
    displayName: String,
    shader: Shader,
    parameters: LinkedHashMap<String, ModulatableParameter>,
    hasFeedback: Boolean = false,
    ownsShader: Boolean = false
) : DynamicVisualSource(id, displayName, shader, parameters, hasFeedback = hasFeedback, ownsShader = ownsShader) {

    override fun setupUniforms(shader: Shader) {
        // Retrieve current states from your app's parameter map
        val currentMorph = parameters["Shape Morph"]?.value ?: 0f
        val currentFoldX = parameters["Fold Angle X"]?.value ?: 0f
        val currentFoldY = parameters["Fold Angle Y"]?.value ?: 0f
        val currentFoldZ = parameters["Fold Angle Z"]?.value ?: 0f

        // Calculate the combined angles
        val finalAngles = calculateDynamicFoldAngles(
            currentMorph,
            currentFoldX,
            currentFoldY,
            currentFoldZ
        )

        parameters.forEach { (name, param) ->
            val uniformName = "u" + name.replace(" ", "")
            when (name) {
                "Fold Angle X" -> shader.setUniform(uniformName, finalAngles.x)
                "Fold Angle Y" -> shader.setUniform(uniformName, finalAngles.y)
                "Fold Angle Z" -> shader.setUniform(uniformName, finalAngles.z)
                else -> shader.setUniform(uniformName, param.value)
            }
        }
    }

    private fun calculateDynamicFoldAngles(
        shapeMorph: Float,
        manualX: Float,
        manualY: Float,
        manualZ: Float
    ): Vector3 {
        // Multiply by 4.0 to match the shader's 4-stage shape mapping logic
        val m = shapeMorph.coerceIn(0f, 1f) * 4f

        // Define the ideal symmetry angles for the core solids (in radians)
        val cubeAngles = Vector3(0f, 0.7854f, 0f)
        val tetraAngles = Vector3(0f, 1.231f, 2.0944f)
        val dodecAngles = Vector3(2.0344f, 0f, 2.4119f)

        // The sphere is a transitional geometry state between Cube and Tetra in the shader.
        // Its KIFS layout is the halfway interpolation between their respective grids.
        val sphereAngles = lerp(cubeAngles, tetraAngles, 0.5f)

        // Determine the base geometric angle based on the current morph state
        val baseAngles = when {
            m < 1f -> lerp(cubeAngles, sphereAngles, m)
            m < 2f -> lerp(sphereAngles, tetraAngles, m - 1f)
            m < 3f -> lerp(tetraAngles, dodecAngles, m - 2f)
            // Soccer ball uses the exact same Icosahedral symmetry angles as the Dodecahedron
            else -> dodecAngles
        }

        // Add the user's manual UI controls to the base calculated angles
        return Vector3(
            baseAngles.x + manualX,
            baseAngles.y + manualY,
            baseAngles.z + manualZ
        )
    }

    override fun clone(): Kifs {
        val clonedParams = LinkedHashMap<String, ModulatableParameter>()
        this.parameters.forEach { (name, param) ->
            clonedParams[name] = param.clone()
        }
        return Kifs(
            id = this.id,
            displayName = this.displayName,
            shader = this.shader,
            parameters = clonedParams,
            hasFeedback = this.hasFeedback,
            ownsShader = false
        )
    }
}
