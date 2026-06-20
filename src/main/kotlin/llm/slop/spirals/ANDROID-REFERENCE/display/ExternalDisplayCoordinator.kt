package llm.slop.spirals.display

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.PowerManager
import android.view.Display
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import llm.slop.spirals.defaults.DefaultsConfig

class ExternalDisplayCoordinator(private val context: Context) {

    private val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private var wakeLock: PowerManager.WakeLock? = null
    private var presentation: SpiralsPresentation? = null
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) {
            updatePresentation()
        }

        override fun onDisplayRemoved(displayId: Int) {
            updatePresentation()
        }

        override fun onDisplayChanged(displayId: Int) {
            updatePresentation()
        }
    }

    fun start() {
        displayManager.registerDisplayListener(displayListener, null)
        updatePresentation()
    }

    fun stop() {
        displayManager.unregisterDisplayListener(displayListener)
        dismissPresentation()
        releaseWakeLock()
    }

    fun updatePresentation() {
        val defaults = DefaultsConfig.getInstance(context)
        if (!defaults.isHdmiEnabled()) {
            dismissPresentation()
            _isConnected.value = false
            return
        }

        val displays = displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)
        val presentationDisplay = displays.firstOrNull { 
            (it.flags and Display.FLAG_PRESENTATION) != 0 
        }

        if (presentationDisplay != null) {
            _isConnected.value = true
            if (presentation == null || presentation?.display?.displayId != presentationDisplay.displayId) {
                showPresentation(presentationDisplay)
            }
        } else {
            _isConnected.value = false
            dismissPresentation()
        }
    }

    private fun showPresentation(display: Display) {
        dismissPresentation()
        Log.d("ExternalDisplay", "Showing presentation on display: ${display.name}")
        presentation = SpiralsPresentation(context, display)
        try {
            presentation?.show()
            acquireWakeLock()
        } catch (e: Exception) {
            Log.e("ExternalDisplay", "Failed to show presentation", e)
            presentation = null
        }
    }

    private fun dismissPresentation() {
        presentation?.let {
            Log.d("ExternalDisplay", "Dismissing presentation")
            it.dismiss()
            presentation = null
            releaseWakeLock()
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "Spirals:ExternalDisplayWakeLock"
            )
        }
        if (wakeLock?.isHeld == false) {
            wakeLock?.acquire()
            Log.d("ExternalDisplay", "WakeLock acquired")
        }
    }

    private fun releaseWakeLock() {
        if (wakeLock?.isHeld == true) {
            wakeLock?.release()
            Log.d("ExternalDisplay", "WakeLock released")
        }
    }
}
