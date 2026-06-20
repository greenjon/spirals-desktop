package llm.slop.spirals.display

import android.opengl.GLSurfaceView
import android.util.Log
import javax.microedition.khronos.egl.*

object SharedContextManager {
    @Volatile
    var mainContext: EGLContext? = null
    @Volatile
    var mainDisplay: EGLDisplay? = null
    @Volatile
    var mainConfig: EGLConfig? = null
    
    var refCount = 0
    
    // Frame synchronization
    private val frameLock = Object()
    @Volatile
    var finalTextureId: Int = 0
    private var frameAvailable = false

    fun notifyFrameReady(textureId: Int) {
        finalTextureId = textureId
        synchronized(frameLock) {
            frameAvailable = true
            frameLock.notifyAll()
        }
    }

    fun waitForFrame(timeoutMs: Long): Boolean {
        synchronized(frameLock) {
            if (!frameAvailable) {
                frameLock.wait(timeoutMs)
            }
            val consumed = frameAvailable
            frameAvailable = false
            return consumed
        }
    }
}

class SharedEGLContextFactory : GLSurfaceView.EGLContextFactory {
    
    private val EGL_CONTEXT_CLIENT_VERSION = 0x3098

    override fun createContext(egl: EGL10, display: EGLDisplay, config: EGLConfig): EGLContext {
        val attribList = intArrayOf(EGL_CONTEXT_CLIENT_VERSION, 3, EGL10.EGL_NONE)
        
        synchronized(SharedContextManager) {
            if (SharedContextManager.mainContext == null) {
                val context = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, attribList)
                if (context == null || context == EGL10.EGL_NO_CONTEXT) {
                    Log.e("SharedEGL", "Failed to create PRIMARY context. Error: ${egl.eglGetError()}")
                    return context ?: EGL10.EGL_NO_CONTEXT
                }
                SharedContextManager.mainContext = context
                SharedContextManager.mainDisplay = display
                SharedContextManager.mainConfig = config
                SharedContextManager.refCount++
                Log.d("SharedEGL", "Primary context established for sharing (ID: $context)")
                return context
            }
            
            val mainCtx = SharedContextManager.mainContext!!
            val context = egl.eglCreateContext(display, config, mainCtx, attribList)
            
            if (context == null || context == EGL10.EGL_NO_CONTEXT) {
                val fallbackCtx = egl.eglCreateContext(display, config, EGL10.EGL_NO_CONTEXT, attribList)
                return fallbackCtx ?: EGL10.EGL_NO_CONTEXT
            }
            
            SharedContextManager.refCount++
            return context
        }
    }

    override fun destroyContext(egl: EGL10, display: EGLDisplay, context: EGLContext) {
        synchronized(SharedContextManager) {
            SharedContextManager.refCount--
            if (context == SharedContextManager.mainContext && SharedContextManager.refCount > 0) {
                return 
            }
            if (context == SharedContextManager.mainContext) {
                SharedContextManager.mainContext = null
                SharedContextManager.mainDisplay = null
                SharedContextManager.mainConfig = null
            }
            egl.eglDestroyContext(display, context)
        }
    }
}
