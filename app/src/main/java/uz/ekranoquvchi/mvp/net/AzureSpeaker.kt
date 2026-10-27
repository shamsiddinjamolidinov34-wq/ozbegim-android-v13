package uz.ekranoquvchi.mvp.net

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaDataSource
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import uz.ekranoquvchi.mvp.util.Prefs
import uz.ekranoquvchi.mvp.util.Secrets
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Microsoft Azure Speech (REST) orqali onlayn ovoz: o'zbekcha Madina va Sardor, shuningdek ruscha va inglizcha ovozlar.
 *
 * Audio sifati: 48 kHz, MP3. Azure 48 kHz uchun faqat 96 va 192 kbit/s MP3 beradi (128 kbit/s faqat 16 kHz'da bor),
 * shuning uchun standart 192 kbit/s (so'ralgan 128 dan past emas), tejash uchun 96 kbit/s tanlanadi.
 *
 * Matn bo'laklari parallel (ikkitadan) yuklanadi, lekin qat'iy tartibda ijro etiladi. Xato bo'lsa, shu bo'lak
 * onFailed orqali qaytariladi (chaqiruvchi mahalliy dvigatelda aytadi). Hamma metodlar faqat asosiy oqimdan chaqiriladi;
 * qaytarma chaqiruvlar ham asosiy oqimda keladi.
 */
class AzureSpeaker(private val context: Context, private val listener: Listener) {

    interface Listener {
        fun onStart(id: String)

        fun onEnd(id: String)

        /** reason: "auth" (kalit rad etildi), "net" (tarmoq yoki xizmat xatosi), "play" (ijro xatosi). */
        fun onFailed(id: String, text: String, reason: String)

        /** Navbat to'liq tugadi va hech narsa ijro etilmayapti. */
        fun onIdle()
    }

    private class Item(val id: String, val text: String) {
        @Volatile
        var bytes: ByteArray? = null

        @Volatile
        var done = false

        @Volatile
        var reason = ""
    }

    private class AuthException : IOException("auth")

    private class BytesSource(private val data: ByteArray) : MediaDataSource() {
        override fun readAt(position: Long, buffer: ByteArray, offset: Int, size: Int): Int {
            if (position >= data.size) return -1
            val n = minOf(size.toLong(), data.size - position).toInt()
            System.arraycopy(data, position.toInt(), buffer, offset, n)
            return n
        }

        override fun getSize(): Long = data.size.toLong()

        override fun close() {}
    }

    private val main = Handler(Looper.getMainLooper())
    private val pool: ExecutorService = Executors.newFixedThreadPool(2) { r -> Thread(r, "azure-tts").apply { isDaemon = true } }
    private val queue = ArrayDeque<Item>()
    private val cache = object : LruCache<String, ByteArray>(CACHE_BYTES) {
        override fun sizeOf(key: String, value: ByteArray): Int = value.size
    }
    private var player: MediaPlayer? = null
    private var playing: Item? = null
    private var generation = 0

    @Volatile
    private var closed = false

    /** Ovoz balandligi (0..1): mikrofon siyosati "past ovozda" bo'lganda pasaytiriladi. */
    var volume: Float = 1f

    fun speak(id: String, text: String, interrupt: Boolean) {
        if (closed || text.isBlank()) return
        if (interrupt) stopInternal()
        val item = Item(id, text)
        queue.addLast(item)
        val gen = generation
        try {
            pool.execute { fetchInto(item, gen) }
        } catch (_: Throwable) {
            item.reason = "net"
            item.done = true
        }
        pump()
    }

    fun stop() {
        if (closed) return
        stopInternal()
    }

    fun shutdown() {
        closed = true
        stopInternal()
        try {
            pool.shutdownNow()
        } catch (_: Throwable) {
        }
    }

    // ------------------------------------------------------------------ navbat

    private fun stopInternal() {
        generation++
        queue.clear()
        releasePlayer()
    }

    private fun releasePlayer() {
        val p = player
        player = null
        playing = null
        if (p == null) return
        try {
            p.stop()
        } catch (_: Throwable) {
        }
        try {
            p.release()
        } catch (_: Throwable) {
        }
    }

    private fun onFetched(gen: Int) {
        if (closed || gen != generation) return
        pump()
    }

    /** Navbat boshidagi bo'lak tayyor bo'lsa ijro etadi; xato bo'lsa chaqiruvchiga qaytaradi va keyingisiga o'tadi. */
    private fun pump() {
        if (closed || playing != null) return
        while (queue.isNotEmpty()) {
            val head = queue.first()
            if (!head.done) return
            queue.removeFirst()
            val bytes = head.bytes
            if (bytes == null || bytes.isEmpty()) {
                listener.onFailed(head.id, head.text, head.reason.ifEmpty { "net" })
                continue
            }
            if (play(head, bytes)) return
            listener.onFailed(head.id, head.text, "play")
        }
        listener.onIdle()
    }

    private fun play(item: Item, bytes: ByteArray): Boolean {
        return try {
            val mp = MediaPlayer()
            mp.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            mp.setDataSource(BytesSource(bytes))
            mp.setVolume(volume, volume)
            mp.setOnPreparedListener { p ->
                if (player === p) {
                    listener.onStart(item.id)
                    p.start()
                }
            }
            mp.setOnCompletionListener { p -> finish(p, item, true) }
            mp.setOnErrorListener { p, _, _ ->
                finish(p, item, false)
                true
            }
            player = mp
            playing = item
            mp.prepareAsync()
            true
        } catch (_: Throwable) {
            releasePlayer()
            false
        }
    }

    private fun finish(p: MediaPlayer, item: Item, ok: Boolean) {
        if (player !== p) {
            try {
                p.release()
            } catch (_: Throwable) {
            }
            return
        }
        releasePlayer()
        if (ok) listener.onEnd(item.id) else listener.onFailed(item.id, item.text, "play")
        pump()
    }

    // ------------------------------------------------------------------ yuklash (orqa oqim)

    private fun fetchInto(item: Item, gen: Int) {
        var data: ByteArray? = null
        var reason = ""
        try {
            val voice = Prefs.azureVoice(context)
            val format = formatFor(Prefs.azureQuality(context))
            val rate = Prefs.rate(context)
            val pitch = Prefs.pitch(context)
            val cacheKey = "$voice|$format|$rate|$pitch|${item.text}"
            val cacheable = item.text.length <= CACHE_MAX_TEXT
            data = if (cacheable) cache.get(cacheKey) else null
            if (data == null) {
                data = request(item.text, voice, format, rate, pitch)
                if (cacheable) cache.put(cacheKey, data)
            }
            Online.noteSuccess()
        } catch (_: AuthException) {
            reason = "auth"
            Online.noteFailure()
        } catch (_: Throwable) {
            reason = "net"
            Online.noteFailure()
        }
        item.bytes = data
        item.reason = reason
        item.done = true
        main.post { onFetched(gen) }
    }

    private fun request(text: String, voice: String, format: String, rate: Int, pitch: Int): ByteArray {
        val key = Secrets.get(context, Prefs.AZURE_KEY)
        if (key.isNullOrEmpty()) throw AuthException()
        val url = "https://" + Prefs.azureRegion(context) + ".tts.speech.microsoft.com/cognitiveservices/v1"
        val headers = mapOf(
            "Ocp-Apim-Subscription-Key" to key,
            "Content-Type" to "application/ssml+xml",
            "X-Microsoft-OutputFormat" to format,
            "User-Agent" to "Ozbegim",
        )
        val ssml = buildSsml(text, voice, rate, pitch)
        val res = Http.post(url, headers, ssml.toByteArray(Charsets.UTF_8), CONNECT_MS, READ_MS, MAX_AUDIO_BYTES)
        if (res.code == 401 || res.code == 403) throw AuthException()
        if (res.code != 200 || res.body.isEmpty()) throw IOException("http ${res.code}")
        return res.body
    }

    companion object {
        /** Azure 48 kHz MP3 formatlari (rasmiy ro'yxat): faqat 96 va 192 kbit/s mavjud. */
        const val FORMAT_192 = "audio-48khz-192kbitrate-mono-mp3"
        const val FORMAT_96 = "audio-48khz-96kbitrate-mono-mp3"

        private const val CONNECT_MS = 3000
        private const val READ_MS = 9000
        private const val MAX_AUDIO_BYTES = 4_000_000
        private const val CACHE_BYTES = 3 * 1024 * 1024
        private const val CACHE_MAX_TEXT = 80

        fun formatFor(quality: String): String = if (quality == "mp3_96") FORMAT_96 else FORMAT_192

        private fun signed(v: Int): String = if (v >= 0) "+$v" else v.toString()

        private fun escape(s: String): String {
            val sb = StringBuilder(s.length + 16)
            for (c in s) {
                when {
                    c == '&' -> sb.append("&amp;")
                    c == '<' -> sb.append("&lt;")
                    c == '>' -> sb.append("&gt;")
                    c == '\'' -> sb.append("&apos;")
                    c == '"' -> sb.append("&quot;")
                    c < ' ' && c != '\n' && c != '\t' -> sb.append(' ')
                    else -> sb.append(c)
                }
            }
            return sb.toString()
        }

        /** SSML: tezlik -50..+100 foiz, balandlik -50..+50 foiz (Azure ruxsat etgan oraliq). */
        fun buildSsml(text: String, voice: String, rate: Int, pitch: Int): String {
            val parts = voice.split('-')
            val lang = if (parts.size >= 3) parts[0] + "-" + parts[1] else "uz-UZ"
            val r = (rate - 100).coerceIn(-50, 100)
            val p = (pitch - 100).coerceIn(-50, 50)
            return "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='$lang'>" +
                "<voice name='$voice'><prosody rate='${signed(r)}%' pitch='${signed(p)}%'>" +
                escape(text) +
                "</prosody></voice></speak>"
        }
    }
}
