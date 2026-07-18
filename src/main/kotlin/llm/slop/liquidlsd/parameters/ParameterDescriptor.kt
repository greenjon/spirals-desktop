package llm.slop.liquidlsd.parameters

data class ParameterDescriptor(
    val path: String,         // e.g. "deck.0.opacity"
    val displayName: String,  // e.g. "Deck 0 Opacity"
    val owner: String         // e.g. "Mixer", "Mandala", "Deck"
)
