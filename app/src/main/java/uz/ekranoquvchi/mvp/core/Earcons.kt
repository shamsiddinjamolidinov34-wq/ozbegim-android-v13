package uz.ekranoquvchi.mvp.core

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import uz.ekranoquvchi.mvp.util.Prefs

/** Tovush signallari (8.4, 18.2): nutqsiz qisqa tonlar, kod bilan hosil qilinadi (fayl kerak emas). Standartda o'chiq. */
object Earcons {
    enum class Kind { FOCUS, EDGE, WINDOW }

    private const val RATE = 48000
    private val cache = HashMap<Kind, ShortArray>()
    private val main = Handler(Looper.getMainLooper())

    private fun tone(parts: List<Pair<Int, Int>>): ShortArray {
        var total = 0
        for (p in parts) total += RATE * p.second / 1000
        val out = ShortArray(total)
        var pos = 0
        for (p in parts) {
            val n = RATE * p.second / 1000
            for (i in 0 until n) {
                // 4 ms boshlanish va tugash silliqligi: "chiqillash" bo'lmasligi uchun
                val env = minOf(1.0, minOf(i, n - i) / (RATE * 0.004))
                out[pos + i] = (Math.sin(2.0 * Math.PI * p.first * i / RATE) * env * 9000.0).toInt().toShort()
            }
            pos += n
        }
        return out
    }

    private fun build(kind: Kind): ShortArray = when (kind) {
        Kind.FOCUS -> tone(listOf(1200 to 18))
        Kind.EDGE -> tone(listOf(330 to 70, 260 to 90))
        Kind.WINDOW -> tone(listOf(660 to 50, 880 to 70))
    }

    fun play(context: Context, kind: Kind) {
        if (!Prefs.earcons(context)) return
        val pcm = cache.getOrPut(kind) { build(kind) }
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(pcm, 0, pcm.size)
            track.play()
            main.postDelayed({
                try {
                    track.release()
                } catch (_: Throwable) {
                }
            }, pcm.size * 1000L / RATE + 150L)
        } catch (_: Throwable) {
        }
    }
}
