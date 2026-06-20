package llm.slop.spirals.display

import android.opengl.GLES30
import android.util.Log
import android.view.Surface
import llm.slop.spirals.display.ShaderHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.microedition.khronos.egl.*

class PresentationRenderThread(private val surface: Surface) : Thread("PresentationRenderThread") {

    private var egl: EGL10? = null
    private var eglDisplay: EGLDisplay? = EGL10.EGL_NO_DISPLAY
    private var eglContext: EGLContext? = EGL10.EGL_NO_CONTEXT
    private var eglSurface: EGLSurface? = EGL10.EGL_NO_SURFACE
    
    @Volatile
    private var running = true

    private var program = 0
    private var vao = 0
    private var vbo = 0
    private var uTextureLoc = -1

    fun quit() {
        running = false
        interrupt()
    }

    override fun run() {
        initGL()
        if (eglContext == EGL10.EGL_NO_CONTEXT) return

        setupQuad()
        
        while (running) {
            // Wait for frame with timeout to allow thread check
            if (SharedContextManager.waitForFrame(33)) {
                drawFrame()
            }
        }

        releaseGL()
    }

    private fun initGL() {
        egl = EGLContext.getEGL() as EGL10
        val egl = egl!!
        
        eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        egl.eglInitialize(eglDisplay, version)

        val config = SharedContextManager.mainConfig ?: return
        val mainContext = SharedContextManager.mainContext ?: return

        val attribList = intArrayOf(0x3098, 3, EGL10.EGL_NONE)
        eglContext = egl.eglCreateContext(eglDisplay, config, mainContext, attribList)
        
        if (eglContext == EGL10.EGL_NO_CONTEXT) {
            Log.e("PresentationThread", "Failed to create shared context")
            return
        }

        eglSurface = egl.eglCreateWindowSurface(eglDisplay, config, surface, null)
        if (!egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            Log.e("PresentationThread", "eglMakeCurrent failed")
            return
        }

        val vertexShader = """
            #version 300 es
            layout(location = 0) in vec2 aPosition;
            layout(location = 1) in vec2 aTexCoord;
            out vec2 vTexCoord;
            void main() {
                vTexCoord = aTexCoord;
                gl_Position = vec4(aPosition, 0.0, 1.0);
            }
        """.trimIndent()

        val fragmentShader = """
            #version 300 es
            precision mediump float;
            uniform sampler2D uTexture;
            in vec2 vTexCoord;
            out vec4 fragColor;
            void main() {
                fragColor = texture(uTexture, vTexCoord);
            }
        """.trimIndent()

        val vs = ShaderHelper.compileShader(GLES30.GL_VERTEX_SHADER, vertexShader)
        val fs = ShaderHelper.compileShader(GLES30.GL_FRAGMENT_SHADER, fragmentShader)
        program = ShaderHelper.linkProgram(vs, fs)
        uTextureLoc = GLES30.glGetUniformLocation(program, "uTexture")
    }

    private fun setupQuad() {
        val vertices = floatArrayOf(
            -1f, -1f,   0f, 0f,
             1f, -1f,   1f, 0f,
            -1f,  1f,   0f, 1f,
             1f,  1f,   1f, 1f
        )
        val buffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        buffer.put(vertices).position(0)

        val vaos = IntArray(1)
        GLES30.glGenVertexArrays(1, vaos, 0)
        vao = vaos[0]
        
        val vbos = IntArray(1)
        GLES30.glGenBuffers(1, vbos, 0)
        vbo = vbos[0]

        GLES30.glBindVertexArray(vao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * 4, buffer, GLES30.GL_STATIC_DRAW)

        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 2, GLES30.GL_FLOAT, false, 16, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 2, GLES30.GL_FLOAT, false, 16, 8)
    }

    private fun drawFrame() {
        val textureId = SharedContextManager.finalTextureId
        if (textureId == 0) return

        GLES30.glClearColor(0f, 0f, 0f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)

        GLES30.glUseProgram(program)
        GLES30.glBindVertexArray(vao)
        
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0)
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, textureId)
        GLES30.glUniform1i(uTextureLoc, 0)

        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP, 0, 4)
        
        egl?.eglSwapBuffers(eglDisplay, eglSurface)
    }

    private fun releaseGL() {
        egl?.let {
            it.eglMakeCurrent(eglDisplay, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_SURFACE, EGL10.EGL_NO_CONTEXT)
            it.eglDestroySurface(eglDisplay, eglSurface)
            it.eglDestroyContext(eglDisplay, eglContext)
        }
    }
}
