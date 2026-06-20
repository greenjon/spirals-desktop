package llm.slop.spirals.cv.processors

import android.content.Context
import android.content.Intent
import android.media.*
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build

enum class AudioSourceType {
    MIC, 
    UNPROCESSED, // Bypasses system AGC/Noise suppression, ideal for CV analysis
    INTERNAL     // Captures audio from other apps via MediaProjection
}

/**
 * Manages audio source configuration and MediaProjection flow.
 */
class AudioSourceManager(context: Context) {
    private val appContext = context.applicationContext
    private val projectionManager = appContext.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager

    fun createProjectionIntent(): Intent = projectionManager.createScreenCaptureIntent()

    fun getMediaProjection(resultCode: Int, data: Intent): MediaProjection? {
        return projectionManager.getMediaProjection(resultCode, data)
    }

    /**
     * Builds an AudioRecord based on the selected type.
     */
    fun buildAudioRecord(
        type: AudioSourceType,
        sampleRate: Int,
        encoding: Int,
        channelConfig: Int,
        bufferSize: Int,
        mediaProjection: MediaProjection? = null
    ): AudioRecord? {
        return try {
            when (type) {
                AudioSourceType.MIC -> {
                    AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        sampleRate,
                        channelConfig,
                        encoding,
                        bufferSize
                    )
                }
                AudioSourceType.UNPROCESSED -> {
                    AudioRecord(
                        MediaRecorder.AudioSource.UNPROCESSED,
                        sampleRate,
                        channelConfig,
                        encoding,
                        bufferSize
                    )
                }
                AudioSourceType.INTERNAL -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && mediaProjection != null) {
                        val config = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                            .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                            .addMatchingUsage(AudioAttributes.USAGE_GAME)
                            .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                            .build()

                        AudioRecord.Builder()
                            .setAudioFormat(
                                AudioFormat.Builder()
                                    .setEncoding(encoding)
                                    .setSampleRate(sampleRate)
                                    .setChannelMask(channelConfig)
                                    .build()
                            )
                            .setAudioPlaybackCaptureConfig(config)
                            .setBufferSizeInBytes(bufferSize)
                            .build()
                    } else {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
