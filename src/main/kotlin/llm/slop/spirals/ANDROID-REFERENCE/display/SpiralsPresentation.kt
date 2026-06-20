package llm.slop.spirals.display

import android.app.Presentation
import android.content.Context
import android.os.Bundle
import android.view.Display
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.WindowManager
import llm.slop.spirals.R

class SpiralsPresentation(outerContext: Context, display: Display) : Presentation(outerContext, display) {

    private var renderThread: PresentationRenderThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        window?.run {
            setType(WindowManager.LayoutParams.TYPE_PRIVATE_PRESENTATION)
            addFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED)
            // Ensure no system UI or other distractions on HDMI
            setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }

        val surfaceView = SurfaceView(context)
        surfaceView.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                renderThread = PresentationRenderThread(holder.surface)
                renderThread?.start()
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                renderThread?.quit()
                renderThread = null
            }
        })
        
        setContentView(surfaceView)
    }

    override fun onStop() {
        super.onStop()
        renderThread?.quit()
        renderThread = null
    }
}
