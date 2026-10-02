package com.example.backgroundaudio

import android.content.Context
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin

/**
 * Generates a gentle, seamlessly looping 8-second pad (A-major-ish chord with slow pulsing) as a
 * WAV file in the app cache, so the project needs no binary audio asset to be testable.
 * All frequencies have a whole number of cycles in 8 s, so the loop point is click-free.
 */
object ToneGenerator {
    private const val SAMPLE_RATE = 22050
    private const val SECONDS = 8
    private const val FILE_NAME = "background_tone_v1.wav"

    fun ensureToneFile(context: Context): File {
        val file = File(context.cacheDir, FILE_NAME)
        if (file.exists() && file.length() > 44) return file

        val sampleCount = SAMPLE_RATE * SECONDS
        val dataLen = sampleCount * 2
        val buf = ByteBuffer.allocate(44 + dataLen).order(ByteOrder.LITTLE_ENDIAN)

        buf.put("RIFF".toByteArray()).putInt(36 + dataLen).put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
        buf.putInt(SAMPLE_RATE).putInt(SAMPLE_RATE * 2).putShort(2).putShort(16)
        buf.put("data".toByteArray()).putInt(dataLen)

        val freqs = doubleArrayOf(220.0, 277.125, 329.625)
        for (i in 0 until sampleCount) {
            val t = i.toDouble() / SAMPLE_RATE
            var s = 0.0
            for (f in freqs) s += sin(2 * PI * f * t)
            s /= freqs.size
            val pulse = 0.65 + 0.35 * sin(2 * PI * 0.5 * t)
            buf.putShort((s * pulse * 0.4 * Short.MAX_VALUE).toInt().toShort())
        }

        file.writeBytes(buf.array())
        return file
    }
}
