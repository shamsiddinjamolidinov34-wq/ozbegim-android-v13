package uz.ekranoquvchi.mvp.content

/** Raqamlarni o'zbekcha so'zga aylantiradi: sonlar (milliardlargacha), tartib sonlar, soat. */
object UzNumbers {
    private val ONES = arrayOf("nol", "bir", "ikki", "uch", "to'rt", "besh", "olti", "yetti", "sakkiz", "to'qqiz")
    private val TENS = arrayOf("", "o'n", "yigirma", "o'ttiz", "qirq", "ellik", "oltmish", "yetmish", "sakson", "to'qson")

    fun words(n: Int): String = words(n.toLong())

    /** 0..999 999 999 999. Undan katta yoki manfiy bo'lsa raqamlar o'zgarishsiz qaytadi. */
    fun words(n: Long): String {
        if (n < 0L || n > 999_999_999_999L) return n.toString()
        if (n < 10L) return ONES[n.toInt()]
        if (n < 100L) {
            val r = (n % 10L).toInt()
            val t = TENS[(n / 10L).toInt()]
            return if (r == 0) t else t + " " + ONES[r]
        }
        if (n < 1000L) {
            val h = (n / 100L).toInt()
            val head = if (h == 1) "yuz" else ONES[h] + " yuz"
            val rest = n % 100L
            return if (rest == 0L) head else head + " " + words(rest)
        }
        val (unit, name) = when {
            n >= 1_000_000_000L -> 1_000_000_000L to "milliard"
            n >= 1_000_000L -> 1_000_000L to "million"
            else -> 1_000L to "ming"
        }
        val q = n / unit
        // "ming" oldidan "bir" aytilmaydi (1000 = "ming"); million va milliard oldidan aytiladi
        val head = if (unit == 1_000L && q == 1L) name else words(q) + " " + name
        val rest = n % unit
        return if (rest == 0L) head else head + " " + words(rest)
    }

    /** 1 -> "birinchi", 2 -> "ikkinchi", 20 -> "yigirmanchi", 21 -> "yigirma birinchi". */
    fun ordinal(n: Int): String {
        val w = words(n)
        val last = w.last()
        return w + if ("aeiou".indexOf(last) >= 0) "nchi" else "inchi"
    }

    /** 14:30 -> "o'n to'rt o'ttiz"; 9:05 -> "to'qqiz nol besh". */
    fun time(hour: Int, minute: Int): String {
        val h = words(hour)
        return when {
            minute == 0 -> h
            minute < 10 -> h + " nol " + words(minute)
            else -> h + " " + words(minute)
        }
    }
}
