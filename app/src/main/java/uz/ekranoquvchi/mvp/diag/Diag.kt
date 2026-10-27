package uz.ekranoquvchi.mvp.diag

import android.content.Context
import uz.ekranoquvchi.mvp.util.Prefs
import java.util.concurrent.atomic.AtomicBoolean

/** Diagnostika markazi: qora quti va jurnal (bitta jarayonda yagona nusxa). */
object Diag {
    val blackBox = BlackBox()

    @Volatile
    private var instance: ErrorLog? = null

    val log: ErrorLog
        get() = instance ?: error("Diag.init() chaqirilmagan")

    fun init(context: Context) {
        if (instance != null) return
        synchronized(this) {
            if (instance == null) instance = ErrorLog(context.applicationContext)
        }
    }
}

/** Qulashlarni qayd etadi; 10 daqiqada 3 marta qulasa, keyingi ishga tushishda xavfsiz rejim yoqiladi. */
object CrashGuard {
    private const val KEY = "crash_times"
    private const val WINDOW_MS = 10 * 60_000L
    private const val THRESHOLD = 3

    /**
     * Xizmat o'chirilib-yoqilganda jarayon tirik qoladi va install() qayta chaqiriladi. Har safar yangi qo'riqchi
     * oldingisini o'rab olsa, bitta qulash bir necha marta sanalib, xavfsiz rejim erta yoqilib qolardi.
     */
    private val installed = AtomicBoolean(false)

    fun install(context: Context) {
        if (!installed.compareAndSet(false, true)) return
        val appContext = context.applicationContext
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, e ->
            try {
                // Xabar matni yozilmaydi (u ekran matnini o'z ichiga olishi mumkin): faqat sinf va joy.
                val top = e.stackTrace.firstOrNull()
                val where = top?.let { "${it.className.substringAfterLast('.')}.${it.methodName}:${it.lineNumber}" } ?: "?"
                Diag.log.logSync(ErrorCode.CRASH_UNCAUGHT, mapOf("exc" to e.javaClass.simpleName, "at" to where))
                record(appContext)
            } catch (_: Throwable) {
            }
            previous?.uncaughtException(thread, e)
        }
    }

    private fun times(context: Context): List<Long> =
        Prefs.get(context).getString(KEY, "").orEmpty().split(',').mapNotNull { it.toLongOrNull() }

    private fun record(context: Context) {
        val list = (times(context) + System.currentTimeMillis()).takeLast(10)
        Prefs.get(context).edit().putString(KEY, list.joinToString(",")).commit()
    }

    fun recentCount(context: Context): Int {
        val now = System.currentTimeMillis()
        return times(context).count { now - it <= WINDOW_MS }
    }

    fun shouldEnterSafeMode(context: Context): Boolean = recentCount(context) >= THRESHOLD
}
