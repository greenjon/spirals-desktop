package llm.slop.spirals.cv.core

import androidx.compose.runtime.mutableStateMapOf
import llm.slop.spirals.cv.visualizers.CvHistoryBuffer
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.*
import llm.slop.spirals.cv.sources.SampleAndHoldCv

/**
 * Central registry for all Control Voltage signals.
 */
object ModulationRegistry {
    val sampleAndHold = SampleAndHoldCv()

    private val rawSignalData = ConcurrentHashMap<String, Float>().apply {
        put("amp", 0f)
        put("bass", 0f)
        put("mid", 0f)
        put("high", 0f)
        put("bassFlux", 0f)
        put("onset", 0f)
        put("accent", 0f)
        put("beatPhase", 0f)
        put("totalBeats", 0f)
        put("bpm", 120f)
    }

    private val startTimeNs = System.nanoTime()

    // Anchor State for Precision Clock
    private var anchorBeats = 0.0
    private var anchorBpm = 120f
    private var anchorTimeNs = System.nanoTime()

    fun updateBeatAnchor(beats: Double, bpm: Float, timeNs: Long) {
        anchorBeats = beats
        anchorBpm = bpm
        anchorTimeNs = timeNs
        update("totalBeats", beats.toFloat())
        update("bpm", bpm)
    }

    fun getSynchronizedTotalBeats(): Double {
        val now = System.nanoTime()
        val elapsedSec = (now - anchorTimeNs) / 1_000_000_000.0
        val beatDelta = elapsedSec * (anchorBpm / 60.0)
        return anchorBeats + beatDelta
    }

    fun getElapsedRealtimeSec(): Double {
        return (System.nanoTime() - startTimeNs) / 1_000_000_000.0
    }

    // Diagnostic History Buffers (Accessible anywhere)
    val history = ConcurrentHashMap<String, CvHistoryBuffer>().apply {
        rawSignalData.keys.forEach { put(it, CvHistoryBuffer(200)) }
    }

    val signals = mutableStateMapOf<String, Float>().apply {
        putAll(rawSignalData)
    }

    private var syncJob: Job? = null

    /**
     * Starts background sync for both UI state and Diagnostic history.
     * Guarded to ensure only one active job exists.
     */
    fun startSync(scope: CoroutineScope) {
        if (syncJob?.isActive == true) return
        
        syncJob = scope.launch(Dispatchers.Default) {
            try {
                while (isActive) {
                    val start = System.currentTimeMillis()
                    
                    // 1. Update History (Always running)
                    rawSignalData.forEach { (k, v) ->
                        history[k]?.add(v)
                    }

                    // 2. Sync to UI State (Main Thread)
                    withContext(Dispatchers.Main) {
                        rawSignalData.forEach { (k, v) ->
                            if (signals[k] != v) {
                                signals[k] = v
                            }
                        }
                    }

                    // Aim for ~60Hz (16ms)
                    val elapsed = System.currentTimeMillis() - start
                    delay((16 - elapsed).coerceAtLeast(1))
                }
            } finally {
                // Ensure the reference is cleared if the scope is cancelled
                if (syncJob?.isActive == false) {
                    syncJob = null
                }
            }
        }
    }

    fun update(name: String, value: Float) {
        rawSignalData[name] = value
    }

    fun get(name: String): Float = rawSignalData[name] ?: 0f
}
