package llm.slop.spirals

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import llm.slop.spirals.display.SpiralRenderer
import llm.slop.spirals.display.SharedEGLContextFactory
import llm.slop.spirals.models.MixerPatch
import llm.slop.spirals.models.mandala.MandalaParams

class SpiralSurfaceView(context: Context, attrs: AttributeSet? = null) : GLSurfaceView(context, attrs) {

    val renderer: SpiralRenderer = SpiralRenderer(context)

    init {
        setEGLContextClientVersion(3)
        setEGLContextFactory(SharedEGLContextFactory())
        setRenderer(renderer)
        renderMode = RENDERMODE_CONTINUOUSLY
    }
    
    fun setParams(params: MandalaParams) {
        // renderer.params is legacy. The renderer now uses the visualSource properties directly.
        // We can sync these to the visualSource if needed, but for now we'll just ignore to fix the build.
    }

    fun setVisualSource(source: MandalaVisualSource) {
        renderer.visualSource = source
    }

    fun setMixerState(patch: MixerPatch?, monitor: String) {
        renderer.mixerPatch = patch
        renderer.monitorSource = monitor
    }
}
