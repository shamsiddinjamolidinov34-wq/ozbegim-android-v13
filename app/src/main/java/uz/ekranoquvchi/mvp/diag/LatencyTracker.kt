package uz.ekranoquvchi.mvp.diag

/** Hodisadan nutq boshlanishigacha kechikish (ms). Oxirgi 200 o'lchov, faqat qurilmada. */
object LatencyTracker {
    private const val MAX = 200
    private val samples = ArrayList<Long>()

    @Synchronized
    fun add(ms: Long) {
        samples.add(ms)
        if (samples.size > MAX) samples.removeAt(0)
    }

    @Synchronized
    fun count(): Int = samples.size

    @Synchronized
    fun mean(): Long? = if (samples.isEmpty()) null else samples.sum() / samples.size

    /** Eng yaqin tartib (nearest-rank) usuli, butun sonlarda. */
    @Synchronized
    fun percentile(p: Int): Long? {
        if (samples.isEmpty()) return null
        val s = samples.sorted()
        val rank = (p * s.size + 99) / 100
        return s[rank.coerceIn(1, s.size) - 1]
    }

    @Synchronized
    fun clear() = samples.clear()
}
