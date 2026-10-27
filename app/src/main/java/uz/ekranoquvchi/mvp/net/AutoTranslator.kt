package uz.ekranoquvchi.mvp.net

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.accessibility.AccessibilityNodeInfo
import uz.ekranoquvchi.mvp.content.LangDetect
import uz.ekranoquvchi.mvp.content.UzScripts
import java.util.concurrent.Executors

/**
 * Avtomatik tarjima (telefon tili o'zbekcha bo'lganda): o'zbekcha bo'lmagan matn tilni o'zi aniqlab o'zbekchaga
 * tarjima qilinadi. Tezlik uchun uch narsa qilinadi:
 *  1) tarjimalar keshlanadi (bir xil matn qayta so'ralmaydi);
 *  2) ekrandagi ko'rinadigan matnlar oldindan, bitta so'rovda tarjima qilinadi (prefetchTree);
 *  3) fokus bo'lgan matn keshda yo'q bo'lsa, alohida shoshilinch so'rov yuboriladi.
 * Keshda o'zbekcha lotin (hozirgi imlo) saqlanadi; kirill yoki yangi alifboga toScript() o'tkazadi.
 * Holat faqat asosiy oqimda o'zgaradi (qulf kerak emas); tarmoq ishi orqa oqimda.
 * Xizmat ketma-ket xato bersa, tarmoqqa bosim tushmasligi uchun biroz vaqt so'rov yuborilmaydi.
 */
object AutoTranslator {
    private const val MAX_CACHE = 800
    private const val BATCH_ITEMS = 24
    private const val BATCH_CHARS = 6000
    private const val MAX_PREFETCH = 60
    private const val PROBLEM_GAP_MS = 60_000L

    /** Bitta matnning eng uzun hajmi: undan uzun matn tarjima qilinmaydi (asl matn o'qiladi). */
    const val MAX_ITEM_CHARS = 3000

    private val main = Handler(Looper.getMainLooper())
    private val io = Executors.newFixedThreadPool(2) { r -> Thread(r, "ozbegim-translate").apply { isDaemon = true } }

    private val cache = object : LinkedHashMap<String, String>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean = size > MAX_CACHE
    }
    private val waiters = HashMap<String, MutableList<() -> Unit>>()
    private val queued = LinkedHashSet<String>()
    private var queueRunning = false
    private var failures = 0
    private var blockedUntil = 0L
    private var lastProblemAt = 0L

    /** Xizmat javob bermaganda (ko'pi bilan daqiqada bir marta) asosiy oqimda chaqiriladi. */
    @Volatile
    var problemListener: (() -> Unit)? = null

    /** Keshdagi tarjima (o'zbekcha lotin); yo'q bo'lsa null. Faqat asosiy oqimda. */
    fun lookup(text: String): String? = cache[text]

    fun isBlocked(): Boolean = SystemClock.elapsedRealtime() < blockedUntil

    /** Xotira kam bo'lganda yoki sozlama o'zgarganda keshni tozalash. */
    fun clearCache() {
        cache.clear()
    }

    /** Tarjimani tanlangan yozuvga o'tkazadi: "latin" (hozirgi), "cyrl" (kirill) yoki "new" (yangi alifbo). */
    fun toScript(latin: String, choice: String): String = when (choice) {
        "cyrl" -> UzScripts.latinToCyrillic(latin)
        "new" -> UzScripts.latinToNewLatin(latin)
        else -> latin
    }

    /**
     * Keshda yo'q matnlarni tarjima qiladi. done har bir so'rov uchun bir marta (muvaffaqiyat, xato yoki
     * keshda bor bo'lsa ham) asosiy oqimda chaqiriladi. urgent: darhol alohida so'rov; aks holda navbat bilan.
     */
    fun request(ctx: Context, texts: Collection<String>, urgent: Boolean, done: (() -> Unit)?) {
        val todo = ArrayList<String>(texts.size)
        for (t in texts) {
            if (t.isBlank() || t.length > MAX_ITEM_CHARS || cache.containsKey(t) || todo.contains(t)) continue
            todo.add(t)
        }
        if (todo.isEmpty() || isBlocked()) {
            done?.invoke()
            return
        }
        if (done != null) {
            val left = intArrayOf(todo.size)
            for (t in todo) {
                val cb: () -> Unit = {
                    left[0]--
                    if (left[0] == 0) done()
                }
                waiters.getOrPut(t) { ArrayList() }.add(cb)
            }
        }
        val app = ctx.applicationContext
        if (urgent) {
            runBatch(app, split(todo), false)
        } else {
            for (t in todo) queued.add(t)
            pump(app)
        }
    }

    /** Ekran daraxtidagi ko'rinadigan matnlarni orqa oqimda yig'ib, oldindan tarjima qiladi. */
    fun prefetchTree(ctx: Context, root: AccessibilityNodeInfo, maxNodes: Int) {
        val app = ctx.applicationContext
        io.execute {
            val found = LinkedHashSet<String>()
            try {
                collect(root, found, 0, IntArray(1), maxNodes)
            } catch (_: Throwable) {
            }
            val need = ArrayList<String>()
            for (s in found) if (LangDetect.needsTranslation(s)) need.add(s)
            if (need.isNotEmpty()) main.post { request(app, need, false, null) }
        }
    }

    private fun collect(n: AccessibilityNodeInfo, out: MutableSet<String>, depth: Int, visited: IntArray, max: Int) {
        if (visited[0]++ >= max || depth > 14 || out.size >= MAX_PREFETCH) return
        if (!n.isVisibleToUser) return
        if (!n.isPassword) {
            val t = n.text?.toString()?.trim().orEmpty()
            val d = n.contentDescription?.toString()?.trim().orEmpty()
            val h = n.hintText?.toString()?.trim().orEmpty()
            val tip = n.tooltipText?.toString()?.trim().orEmpty()
            // NodeDescriber.labelOf bilan bir xil ustuvorlik: matn, tavsif, maslahat, ko'rsatma
            val pick = when {
                t.isNotEmpty() -> t
                d.isNotEmpty() -> d
                h.isNotEmpty() -> h
                else -> tip
            }
            if (pick.isNotEmpty() && pick.length <= MAX_ITEM_CHARS) out.add(pick)
        }
        for (i in 0 until n.childCount) {
            val c = n.getChild(i) ?: continue
            collect(c, out, depth + 1, visited, max)
        }
    }

    // ------------------------------------------------------------------ navbat va so'rovlar

    private fun pump(app: Context) {
        if (queueRunning || queued.isEmpty()) return
        val batch = ArrayList<String>()
        var chars = 0
        val it = queued.iterator()
        while (it.hasNext() && batch.size < BATCH_ITEMS) {
            val s = it.next()
            if (batch.isNotEmpty() && chars + s.length > BATCH_CHARS) break
            batch.add(s)
            chars += s.length
            it.remove()
        }
        if (batch.isEmpty()) return
        queueRunning = true
        runBatch(app, batch, true)
    }

    /** Shoshilinch so'rovdagi matnlar hajmi chegaradan oshmasligi uchun (odatda 1-3 ta matn). */
    private fun split(todo: List<String>): List<String> {
        val out = ArrayList<String>()
        var chars = 0
        for (s in todo) {
            if (out.size >= BATCH_ITEMS || (out.isNotEmpty() && chars + s.length > BATCH_CHARS)) break
            out.add(s)
            chars += s.length
        }
        return out
    }

    private fun runBatch(app: Context, batch: List<String>, fromQueue: Boolean) {
        io.execute {
            val result: List<String>? = try {
                AiClient.translateBatch(app, batch)
            } catch (_: Throwable) {
                null
            }
            main.post { onBatch(app, batch, result, fromQueue) }
        }
    }

    private fun onBatch(app: Context, batch: List<String>, result: List<String>?, fromQueue: Boolean) {
        if (fromQueue) queueRunning = false
        if (result == null || result.size != batch.size) {
            noteFailure()
        } else {
            failures = 0
            blockedUntil = 0L
        }
        for ((i, t) in batch.withIndex()) {
            if (result != null && result.size == batch.size) {
                val out = result[i].trim()
                cache[t] = if (out.isEmpty()) t else out
            }
            val list = waiters.remove(t)
            if (list != null) for (cb in list) cb()
        }
        if (fromQueue) pump(app)
    }

    private fun noteFailure() {
        failures = (failures + 1).coerceAtMost(5)
        val now = SystemClock.elapsedRealtime()
        blockedUntil = now + minOf(300_000L, 20_000L * (1L shl (failures - 1)))
        if (now - lastProblemAt > PROBLEM_GAP_MS) {
            lastProblemAt = now
            problemListener?.invoke()
        }
    }
}
