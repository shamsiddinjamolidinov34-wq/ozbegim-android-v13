package uz.ekranoquvchi.mvp.net

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import uz.ekranoquvchi.mvp.util.Prefs
import uz.ekranoquvchi.mvp.util.Secrets
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Onlayn imkoniyatlar (Microsoft Azure ovozi, tarjima, sun'iy intellekt) uchun umumiy holat.
 * Hammasi ixtiyoriy: asosiy kalit "Onlayn imkoniyatlarni yoqish" sozlamasi va tegishli kalit bo'lmasa, tarmoqqa
 * hech narsa yuborilmaydi. Tarmoq uzilsa, ovoz avtomatik mahalliy dvigatelga o'tadi (to'xtash-kutish jadvali bilan).
 */
object Online {
    private val io = Executors.newFixedThreadPool(2) { r -> Thread(r, "ozbegim-net").apply { isDaemon = true } }
    private val main = Handler(Looper.getMainLooper())
    private val failures = AtomicInteger(0)

    @Volatile
    private var blockedUntil = 0L

    fun enabled(context: Context): Boolean = Prefs.onlineEnabled(context)

    fun speechReady(context: Context): Boolean = enabled(context) && Secrets.has(context, Prefs.AZURE_KEY)

    /** Onlayn yoqilgan va kamida bitta sun'iy intellekt xizmati (Claude, Gemini, ChatGPT yoki boshqa) sozlangan. */
    fun aiReady(context: Context): Boolean = enabled(context) && AiClient.activeProvider(context) != null

    /** Yaqinda xato bo'lgan bo'lsa, onlayn ovoz birozdan keyin qayta sinab ko'riladi (30 soniyadan 5 daqiqagacha). */
    fun coolingDown(): Boolean = SystemClock.elapsedRealtime() < blockedUntil

    fun noteFailure() {
        val n = failures.incrementAndGet().coerceIn(1, 5)
        blockedUntil = SystemClock.elapsedRealtime() + minOf(300_000L, 30_000L * (1L shl (n - 1)))
    }

    fun noteSuccess() {
        failures.set(0)
        blockedUntil = 0L
    }

    /** work orqa oqimda bajariladi, done asosiy oqimda chaqiriladi (xatoda null bilan). */
    fun <T> async(work: () -> T?, done: (T?) -> Unit) {
        io.execute {
            val result: T? = try {
                work()
            } catch (_: Throwable) {
                null
            }
            main.post { done(result) }
        }
    }
}
