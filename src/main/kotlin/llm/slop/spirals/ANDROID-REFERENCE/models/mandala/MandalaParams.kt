package llm.slop.spirals.models.mandala

data class MandalaParams(
    val omega1: Int = 1,
    val omega2: Int = -2,
    val omega3: Int = 4,
    val omega4: Int = 0,
    val l1: Float = 0.4f,
    val l2: Float = 0.3f,
    val l3: Float = 0.2f,
    val l4: Float = 0.0f,
    val phi1: Float = 0f,
    val phi2: Float = 0f,
    val phi3: Float = 0f,
    val phi4: Float = 0f,
    val thickness: Float = 0.01f
)
