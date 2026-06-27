package llm.slop.spirals.audio

import mu.KotlinLogging
import java.util.Locale

/**
 * Helper to query and set the system input level (default audio source) via native utilities.
 * Supports Linux (wpctl) and macOS (osascript). Gracefully disables on Windows or other OSes.
 */
object SystemAudioVolume {
    private val logger = KotlinLogging.logger {}
    
    val isSupported: Boolean
    private val os: OS

    private enum class OS { LINUX, MAC, WINDOWS, UNKNOWN }

    init {
        val osName = System.getProperty("os.name").lowercase(Locale.ENGLISH)
        os = when {
            osName.contains("mac") -> OS.MAC
            osName.contains("nix") || osName.contains("nux") || osName.contains("aix") -> OS.LINUX
            osName.contains("win") -> OS.WINDOWS
            else -> OS.UNKNOWN
        }
        isSupported = (os == OS.LINUX || os == OS.MAC)
    }

    @Volatile
    var systemInputVolume: Float = 1.0f
        private set

    @Volatile
    var isMuted: Boolean = false
        private set

    private var lastQueryTime = 0L
    private val queryIntervalMs = 2000L
    private var isQuerying = false

    fun updateSystemVolume(volume: Float) {
        if (!isSupported) return
        systemInputVolume = volume.coerceIn(0f, 1f)
        Thread {
            try {
                val pb = when (os) {
                    OS.LINUX -> ProcessBuilder("wpctl", "set-volume", "@DEFAULT_AUDIO_SOURCE@", "%.2f".format(volume))
                    OS.MAC -> {
                        val pct = (volume * 100).toInt()
                        ProcessBuilder("osascript", "-e", "set volume input volume $pct")
                    }
                    else -> return@Thread
                }
                val process = pb.start()
                process.waitFor()
            } catch (e: Exception) {
                logger.error(e) { "Failed to set system volume" }
            }
        }.start()
    }

    fun queryAsync() {
        if (!isSupported) return
        val now = System.currentTimeMillis()
        if (now - lastQueryTime < queryIntervalMs || isQuerying) return
        isQuerying = true
        Thread {
            try {
                val pb = when (os) {
                    OS.LINUX -> ProcessBuilder("wpctl", "get-volume", "@DEFAULT_AUDIO_SOURCE@")
                    OS.MAC -> ProcessBuilder("osascript", "-e", "input volume of (get volume settings)")
                    else -> return@Thread
                }
                val process = pb.start()
                val output = process.inputStream.bufferedReader().readText().trim()
                process.waitFor()
                when (os) {
                    OS.LINUX -> {
                        if (output.startsWith("Volume:")) {
                            val parts = output.removePrefix("Volume:").trim().split(" ")
                            val vol = parts.getOrNull(0)?.toFloatOrNull()
                            if (vol != null) {
                                systemInputVolume = vol
                            }
                            isMuted = output.contains("[MUTED]")
                        }
                    }
                    OS.MAC -> {
                        val vol = output.toFloatOrNull()
                        if (vol != null) {
                            systemInputVolume = vol / 100f
                            isMuted = (vol == 0f)
                        }
                    }
                    else -> {}
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to query system volume" }
            } finally {
                lastQueryTime = System.currentTimeMillis()
                isQuerying = false
            }
        }.start()
    }
}
