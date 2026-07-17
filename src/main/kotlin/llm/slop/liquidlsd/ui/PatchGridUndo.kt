package llm.slop.liquidlsd.ui

import llm.slop.liquidlsd.rendering.Mixer
import llm.slop.liquidlsd.parameters.CvModulator
import llm.slop.liquidlsd.parameters.ParameterResolver

object PatchGridUndo {
    fun createUndoSnapshot(mixer: Mixer): PatchGridUndoSnapshot {
        val mods = mutableMapOf<String, List<CvModulator>>()
        ParameterResolver.getAllParameterPaths(mixer).forEach { (path, p) ->
            mods[path] = p.modulators.map { it.copy() }
        }
        return PatchGridUndoSnapshot(mods)
    }

    fun pushUndoState(state: PatchGridState, mixer: Mixer) {
        state.pushUndoState(createUndoSnapshot(mixer))
    }

    fun performUndo(state: PatchGridState, mixer: Mixer) {
        val snapshot = state.popUndoState() ?: return
        ParameterResolver.getAllParameterPaths(mixer).forEach { (path, p) ->
            snapshot.modulatorsByParamKey[path]?.let { savedMods ->
                p.modulators.clear()
                p.modulators.addAll(savedMods.map { it.copy() })
            }
        }
    }
}
