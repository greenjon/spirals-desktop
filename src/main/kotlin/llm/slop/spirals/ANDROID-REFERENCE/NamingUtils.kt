package llm.slop.spirals

object NamingUtils {
    fun generateCloneName(originalName: String, existingNames: List<String>): String {
        val base = if (originalName.contains("-clone")) {
            originalName.substringBeforeLast("-clone")
        } else {
            originalName
        }
        
        val cloneRegex = Regex("-clone(\\d+)$")
        
        // Find highest existing clone index for this base
        val highestIndex = existingNames
            .filter { it.startsWith(base) }
            .mapNotNull { name ->
                val match = cloneRegex.find(name)
                match?.groupValues?.get(1)?.toIntOrNull()
            }
            .maxOrNull() ?: -1
        
        var nextIndex = highestIndex + 1
        var candidate = "$base-clone${nextIndex.toString().padStart(2, '0')}"
        
        // Safety check for collisions
        while (existingNames.contains(candidate)) {
            nextIndex++
            candidate = "$base-clone${nextIndex.toString().padStart(2, '0')}"
        }
        
        return candidate
    }
}
