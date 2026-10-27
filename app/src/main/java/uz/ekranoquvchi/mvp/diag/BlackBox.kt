package uz.ekranoquvchi.mvp.diag

import android.os.SystemClock

/** "Qora quti": oxirgi 30 soniyadagi hodisalar, MATNSIZ (monografiya 15.4). */
class BlackBox(private val windowMs: Long = 30_000L, private val capacity: Int = 600) {
    data class Entry(val t: Long, val kind: String, val a: Int, val b: Int)

    private val buf = ArrayDeque<Entry>()

    @Synchronized
    fun add(kind: String, a: Int = 0, b: Int = 0) {
        val now = SystemClock.uptimeMillis()
        buf.addLast(Entry(now, kind, a, b))
        while (buf.size > capacity || (buf.isNotEmpty() && now - buf.first().t > windowMs)) buf.removeFirst()
    }

    @Synchronized
    fun snapshot(): List<Entry> = buf.toList()
}
