package llm.slop.liquidlsd.audio

import java.nio.FloatBuffer
import javax.sound.sampled.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Fallback audio capture client that uses the Java Sound API (TargetDataLine).
 * This works natively on macOS, Windows, and Linux without native JACK/PipeWire dependencies.
 */
class JavaSoundClient(
    val onProcess: (FloatBuffer, Int, Float) -> Unit // (buffer, nframes, sampleRate)
) {
    @Volatile
    var isConnected = false
        private set

    private var line: TargetDataLine? = null
    private var thread: Thread? = null
    @Volatile
    private var running = false

    /**
     * Starts audio capture from the system's default input line.
     */
    fun start(): Boolean {
        try {
            logger.info { "Starting Java Sound Audio client..." }
            val format = AudioFormat(44100f, 16, 1, true, false) // 44.1kHz, 16-bit, Mono, Signed, Little-Endian
            val info = DataLine.Info(TargetDataLine::class.java, format)
            
            val targetLine = if (AudioSystem.isLineSupported(info)) {
                AudioSystem.getLine(info) as TargetDataLine
            } else {
                // Try 48kHz if 44.1kHz is not supported
                val altFormat = AudioFormat(48000f, 16, 1, true, false)
                val altInfo = DataLine.Info(TargetDataLine::class.java, altFormat)
                if (AudioSystem.isLineSupported(altInfo)) {
                    AudioSystem.getLine(altInfo) as TargetDataLine
                } else {
                    logger.warn { "No supported audio input TargetDataLine found." }
                    return false
                }
            }

            val bufferSize = 512 // 512 samples per read chunk (approx. 11.6ms at 44.1kHz)
            try {
                targetLine.open(targetLine.format, bufferSize * 2) // buffer size in bytes
            } catch (e: LineUnavailableException) {
                logger.warn { "Failed to open TargetDataLine with buffer size ${bufferSize * 2}: ${e.message}. Trying default buffer size." }
                targetLine.open(targetLine.format)
            }
            
            targetLine.start()
            line = targetLine
            isConnected = true
            running = true

            val sampleRate = targetLine.format.sampleRate

            thread = Thread({
                val byteBuffer = ByteArray(bufferSize * 2)
                val floatArray = FloatArray(bufferSize)
                val floatBuffer = FloatBuffer.wrap(floatArray)

                try {
                    while (running) {
                        val currentLine = line ?: break
                        val bytesRead = currentLine.read(byteBuffer, 0, byteBuffer.size)
                        if (bytesRead <= 0) continue

                        val samplesRead = convertPcmToFloat(byteBuffer, bytesRead, floatArray)

                        floatBuffer.position(0)
                        floatBuffer.limit(samplesRead)

                        onProcess(floatBuffer, samplesRead, sampleRate)
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Error in Java Sound capture loop" }
                    isConnected = false
                }
            }, "JavaSoundClient-Capture").apply { isDaemon = true }

            thread?.start()
            logger.info { "Java Sound Audio client started successfully." }
            return true
        } catch (e: Throwable) {
            logger.warn { "Failed to start Java Sound audio: ${e.message}" }
            stop()
            return false
        }
    }

    /**
     * Stops capture and releases system resources.
     */
    fun stop() {
        running = false
        isConnected = false
        try {
            thread?.interrupt()
            thread = null
        } catch (e: Exception) {
            // Ignore
        }
        try {
            line?.stop()
            line?.close()
            line = null
        } catch (e: Exception) {
            // Ignore
        }
    }

    companion object {
        /**
         * Converts 16-bit signed little-endian PCM byte data into floats in range [-1.0, 1.0].
         * Returns the number of samples successfully written to floatArray.
         */
        fun convertPcmToFloat(byteBuffer: ByteArray, bytesRead: Int, floatArray: FloatArray): Int {
            val samplesRead = bytesRead / 2
            val limit = minOf(samplesRead, floatArray.size)
            for (i in 0 until limit) {
                val low = byteBuffer[i * 2].toInt() and 0xff
                val high = byteBuffer[i * 2 + 1].toInt()
                val sample = ((high shl 8) or low).toShort()
                floatArray[i] = sample.toFloat() / 32768f
            }
            return limit
        }
    }
}
