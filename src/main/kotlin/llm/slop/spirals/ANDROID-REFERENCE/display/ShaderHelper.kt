package llm.slop.spirals.display

import android.content.Context
import android.opengl.GLES30
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader

object ShaderHelper {
    private const val TAG = "ShaderHelper"

    fun loadShader(context: Context, resourceId: Int, type: Int): Int {
        val source = readTextFileFromResource(context, resourceId)
        return compileShader(type, source)
    }

    private fun readTextFileFromResource(context: Context, resourceId: Int): String {
        val inputStream = context.resources.openRawResource(resourceId)
        val reader = BufferedReader(InputStreamReader(inputStream))
        val body = StringBuilder()
        var line: String?
        try {
            while (reader.readLine().also { line = it } != null) {
                body.append(line).append('\n')
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not read resource: $resourceId", e)
        }
        return body.toString()
    }

    fun compileShader(type: Int, shaderCode: String): Int {
        val shaderObjectId = GLES30.glCreateShader(type)
        if (shaderObjectId == 0) {
            Log.e(TAG, "Could not create new shader.")
            return 0
        }

        GLES30.glShaderSource(shaderObjectId, shaderCode)
        GLES30.glCompileShader(shaderObjectId)

        val compileStatus = IntArray(1)
        GLES30.glGetShaderiv(shaderObjectId, GLES30.GL_COMPILE_STATUS, compileStatus, 0)

        if (compileStatus[0] == 0) {
            Log.e(TAG, "Results of compiling shader:\n$shaderCode\n:${GLES30.glGetShaderInfoLog(shaderObjectId)}")
            GLES30.glDeleteShader(shaderObjectId)
            return 0
        }

        return shaderObjectId
    }

    fun linkProgram(vertexShaderId: Int, fragmentShaderId: Int): Int {
        val programObjectId = GLES30.glCreateProgram()
        if (programObjectId == 0) {
            Log.e(TAG, "Could not create new program")
            return 0
        }

        GLES30.glAttachShader(programObjectId, vertexShaderId)
        GLES30.glAttachShader(programObjectId, fragmentShaderId)
        GLES30.glLinkProgram(programObjectId)

        val linkStatus = IntArray(1)
        GLES30.glGetProgramiv(programObjectId, GLES30.GL_LINK_STATUS, linkStatus, 0)

        if (linkStatus[0] == 0) {
            Log.e(TAG, "Results of linking program:\n${GLES30.glGetProgramInfoLog(programObjectId)}")
            GLES30.glDeleteProgram(programObjectId)
            return 0
        }

        return programObjectId
    }

    fun buildProgram(context: Context, vertexResourceId: Int, fragmentResourceId: Int): Int {
        val vertexShaderId = loadShader(context, vertexResourceId, GLES30.GL_VERTEX_SHADER)
        val fragmentShaderId = loadShader(context, fragmentResourceId, GLES30.GL_FRAGMENT_SHADER)

        if (vertexShaderId == 0 || fragmentShaderId == 0) {
            return 0
        }

        return linkProgram(vertexShaderId, fragmentShaderId)
    }
}
