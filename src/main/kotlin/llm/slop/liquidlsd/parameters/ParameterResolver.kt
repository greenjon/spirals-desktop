package llm.slop.liquidlsd.parameters

import llm.slop.liquidlsd.rendering.Deck
import llm.slop.liquidlsd.rendering.Mandala
import llm.slop.liquidlsd.rendering.Mixer
import llm.slop.liquidlsd.rendering.DynamicVisualSource

object ParameterResolver {
    private val registeredDescriptors = mutableListOf<ParameterDescriptor>()

    fun register(vararg descriptors: ParameterDescriptor) {
        registeredDescriptors.addAll(descriptors)
    }

    fun getAllDescriptors(): List<ParameterDescriptor> = registeredDescriptors.toList()

    fun getAllParameterPaths(mixer: Mixer): List<Pair<String, ModulatableParameter>> {
        val list = mutableListOf<Pair<String, ModulatableParameter>>()
        
        // TODO: remove once all owners self-register
        // Mixer
        list.add("Mixer/crossfade" to mixer.crossfade)
        list.add("Mixer/masterAlpha" to mixer.masterAlpha)
        list.add("Mixer/bloom" to mixer.bloom)
        list.add("Mixer/xfadeSpeed" to mixer.xfadeSpeed)
        list.add("Mixer/queuePrev" to mixer.queuePrev)
        list.add("Mixer/queueNext" to mixer.queueNext)
        
        // Deck A, B, and C
        for (deckLabel in listOf("Deck A", "Deck B", "Deck C")) {
            val deck = when (deckLabel) {
                "Deck A" -> mixer.deckA
                "Deck B" -> mixer.deckB
                else -> mixer.deckC
            }
            val mandala = deck.source as? Mandala
            
            if (mandala != null) {
                // Geometry
                list.add("$deckLabel/Geometry/Lobes" to mandala.parameters.getOrElse("Lobes") { error("Unknown parameter path: $deckLabel/Geometry/Lobes — check registration") })
                list.add("$deckLabel/Geometry/Recipe" to mandala.parameters.getOrElse("Recipe Select") { error("Unknown parameter path: $deckLabel/Geometry/Recipe — check registration") })
                list.add("$deckLabel/Geometry/L1" to mandala.parameters.getOrElse("L1") { error("Unknown parameter path: $deckLabel/Geometry/L1 — check registration") })
                list.add("$deckLabel/Geometry/L2" to mandala.parameters.getOrElse("L2") { error("Unknown parameter path: $deckLabel/Geometry/L2 — check registration") })
                list.add("$deckLabel/Geometry/L3" to mandala.parameters.getOrElse("L3") { error("Unknown parameter path: $deckLabel/Geometry/L3 — check registration") })
                list.add("$deckLabel/Geometry/L4" to mandala.parameters.getOrElse("L4") { error("Unknown parameter path: $deckLabel/Geometry/L4 — check registration") })
                list.add("$deckLabel/Geometry/FreqOffset" to mandala.parameters.getOrElse("Freq Offset") { error("Unknown parameter path: $deckLabel/Geometry/FreqOffset — check registration") })
                list.add("$deckLabel/Geometry/HarmonicLock" to mandala.parameters.getOrElse("Harmonic Lock") { error("Unknown parameter path: $deckLabel/Geometry/HarmonicLock — check registration") })
                list.add("$deckLabel/Geometry/3DMode" to mandala.parameters.getOrElse("3D Mode") { error("Unknown parameter path: $deckLabel/Geometry/3DMode — check registration") })
                list.add("$deckLabel/Geometry/SphereWrapX" to mandala.parameters.getOrElse("Sphere Wrap X") { error("Unknown parameter path: $deckLabel/Geometry/SphereWrapX — check registration") })
                list.add("$deckLabel/Geometry/SphereWrapY" to mandala.parameters.getOrElse("Sphere Wrap Y") { error("Unknown parameter path: $deckLabel/Geometry/SphereWrapY — check registration") })
                list.add("$deckLabel/Geometry/MirrorGroup" to mandala.parameters.getOrElse("Mirror Group") { error("Unknown parameter path: $deckLabel/Geometry/MirrorGroup — check registration") })
                list.add("$deckLabel/Geometry/PermuteXY" to mandala.parameters.getOrElse("Permute XY") { error("Unknown parameter path: $deckLabel/Geometry/PermuteXY — check registration") })
                list.add("$deckLabel/Geometry/PermuteYZ" to mandala.parameters.getOrElse("Permute YZ") { error("Unknown parameter path: $deckLabel/Geometry/PermuteYZ — check registration") })
                list.add("$deckLabel/Geometry/PermuteZX" to mandala.parameters.getOrElse("Permute ZX") { error("Unknown parameter path: $deckLabel/Geometry/PermuteZX — check registration") })

                // View
                list.add("$deckLabel/View/Zoom" to mandala.parameters.getOrElse("Zoom") { error("Unknown parameter path: $deckLabel/View/Zoom — check registration") })
                list.add("$deckLabel/View/RotateZ" to mandala.parameters.getOrElse("Rotate Z") { error("Unknown parameter path: $deckLabel/View/RotateZ — check registration") })
                list.add("$deckLabel/View/RotateX" to mandala.parameters.getOrElse("Rotate X") { error("Unknown parameter path: $deckLabel/View/RotateX — check registration") })
                list.add("$deckLabel/View/RotateY" to mandala.parameters.getOrElse("Rotate Y") { error("Unknown parameter path: $deckLabel/View/RotateY — check registration") })
                list.add("$deckLabel/View/Persp" to mandala.parameters.getOrElse("3D Persp") { error("Unknown parameter path: $deckLabel/View/Persp — check registration") })
                
                // Color
                list.add("$deckLabel/Color/Thickness" to mandala.parameters.getOrElse("Thickness") { error("Unknown parameter path: $deckLabel/Color/Thickness — check registration") })
                list.add("$deckLabel/Color/HueOffset" to mandala.parameters.getOrElse("Hue Offset") { error("Unknown parameter path: $deckLabel/Color/HueOffset — check registration") })
                list.add("$deckLabel/Color/HueSweep" to mandala.parameters.getOrElse("Hue Sweep") { error("Unknown parameter path: $deckLabel/Color/HueSweep — check registration") })
                list.add("$deckLabel/Color/Depth" to mandala.parameters.getOrElse("Depth") { error("Unknown parameter path: $deckLabel/Color/Depth — check registration") })
                list.add("$deckLabel/Color/Gain" to mandala.globalAlpha)
                
                // Background
                list.add("$deckLabel/Background/Style" to mandala.parameters.getOrElse("Bg Style") { error("Unknown parameter path: $deckLabel/Background/Style — check registration") })
                list.add("$deckLabel/Background/Feedback" to mandala.parameters.getOrElse("Bg Feedback") { error("Unknown parameter path: $deckLabel/Background/Feedback — check registration") })
                list.add("$deckLabel/Background/Hue" to mandala.parameters.getOrElse("Bg Hue") { error("Unknown parameter path: $deckLabel/Background/Hue — check registration") })
                list.add("$deckLabel/Background/Sat" to mandala.parameters.getOrElse("Bg Sat") { error("Unknown parameter path: $deckLabel/Background/Sat — check registration") })
                list.add("$deckLabel/Background/Val" to mandala.parameters.getOrElse("Bg Val") { error("Unknown parameter path: $deckLabel/Background/Val — check registration") })
                list.add("$deckLabel/Background/Sweep" to mandala.parameters.getOrElse("Bg Sweep") { error("Unknown parameter path: $deckLabel/Background/Sweep — check registration") })
                list.add("$deckLabel/Background/Speed" to mandala.parameters.getOrElse("Bg Speed") { error("Unknown parameter path: $deckLabel/Background/Speed — check registration") })
                list.add("$deckLabel/Background/Zoom" to mandala.parameters.getOrElse("Bg Zoom") { error("Unknown parameter path: $deckLabel/Background/Zoom — check registration") })
            }

            val activeSource = deck.source
            if (activeSource is DynamicVisualSource) {
                activeSource.parameters.forEach { (name, param) ->
                    list.add("$deckLabel/${activeSource.displayName}/$name" to param)
                }
                list.add("$deckLabel/${activeSource.displayName}/Gain" to activeSource.globalAlpha)
            }
            
            // Feedback
            list.add("$deckLabel/FB/Decay" to deck.fbDecay)
            list.add("$deckLabel/FB/Gain" to deck.fbGain)
            list.add("$deckLabel/FB/Zoom" to deck.fbZoom)
            list.add("$deckLabel/FB/Rotate" to deck.fbRotate)
            list.add("$deckLabel/FB/HueShift" to deck.fbHueShift)
            list.add("$deckLabel/FB/Blur" to deck.fbBlur)
            list.add("$deckLabel/FB/Chroma" to deck.fbChroma)
            list.add("$deckLabel/FB/Mode" to deck.fbMode)
        }
        
        return list
    }

    fun findParameterByPath(mixer: Mixer, path: String): ModulatableParameter? {
        // Simple O(N) search against the list. We could optimize this by caching if performance is an issue,
        // but it is usually only called when restoring UI state or handling midi mappings occasionally.
        return getAllParameterPaths(mixer).find { it.first == path }?.second
    }
}
