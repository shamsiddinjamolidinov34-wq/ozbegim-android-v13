package uz.ekranoquvchi.mvp.net

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/** Eng sodda HTTPS POST (qo'shimcha kutubxonasiz). Faqat orqa oqimda chaqiriladi. */
object Http {
    class Result(val code: Int, val body: ByteArray)

    /**
     * @param maxBytes javob hajmi chegarasi; oshsa IOException.
     * Muvaffaqiyatda ulanish yopilmaydi (keyingi so'rov uchun qayta ishlatiladi: TLS qo'l siqish takrorlanmaydi).
     */
    fun post(
        url: String,
        headers: Map<String, String>,
        body: ByteArray,
        connectMs: Int,
        readMs: Int,
        maxBytes: Int,
    ): Result {
        require(url.startsWith("https://")) { "https kerak" }
        val conn = URL(url).openConnection() as HttpURLConnection
        try {
            conn.requestMethod = "POST"
            conn.connectTimeout = connectMs
            conn.readTimeout = readMs
            conn.doOutput = true
            conn.useCaches = false
            conn.instanceFollowRedirects = false
            for ((k, v) in headers) conn.setRequestProperty(k, v)
            conn.outputStream.use { it.write(body) }
            val code = conn.responseCode
            val stream: InputStream? = if (code in 200..299) conn.inputStream else conn.errorStream
            val data = if (stream == null) ByteArray(0) else stream.use { readLimited(it, maxBytes) }
            return Result(code, data)
        } catch (t: Throwable) {
            try {
                conn.disconnect()
            } catch (_: Throwable) {
            }
            throw t
        }
    }

    private fun readLimited(input: InputStream, maxBytes: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(8192)
        var total = 0
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            total += n
            if (total > maxBytes) throw IOException("javob juda katta")
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }
}
