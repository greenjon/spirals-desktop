package llm.slop.spirals.cv

interface CvSignal {
    fun getValue(timeSeconds: Double): Float
}
