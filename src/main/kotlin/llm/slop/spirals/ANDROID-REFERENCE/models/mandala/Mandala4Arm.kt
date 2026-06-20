package llm.slop.spirals.models.mandala

data class Mandala4Arm(
    val id: String,
    val a: Int,
    val b: Int,
    val c: Int,
    val d: Int,
    val petals: Int,
    val shapeRatio: Float,
    val multiplicityClass: Int,
    val independentFreqCount: Int,
    val twoFoldLikely: Boolean,
    val hierarchyDepth: Int,
    val dominanceRatio: Float,
    val radialVariance: Float
)
