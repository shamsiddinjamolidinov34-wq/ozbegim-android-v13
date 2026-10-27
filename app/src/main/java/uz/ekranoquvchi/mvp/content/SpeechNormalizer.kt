package uz.ekranoquvchi.mvp.content

/**
 * Normalizator (FR-06, monografiya 9.5): raqam, sana, vaqt, pul, foiz, telefon, URL, pochta va hashtag'larni
 * o'zbekcha so'zga aylantiradi. Nutq dvigatelidan mustaqil (har qanday dvigatel bilan ishlaydi).
 * Ehtiyotkor: aniq naqshga mos kelmagan raqamlarga (versiya raqami, kod, 13+ xonali son) tegilmaydi.
 */
object SpeechNormalizer {
    private val MONTHS = arrayOf(
        "yanvar", "fevral", "mart", "aprel", "may", "iyun", "iyul", "avgust", "sentabr", "oktabr", "noyabr", "dekabr"
    )

    private val EMAIL = Regex("""[\p{L}\p{N}._%+\-]+@[\p{L}\p{N}\-]+(?:\.[\p{L}\p{N}\-]+)+""")
    private val URL = Regex("""(?:https?://|www\.)[^\s]+""", RegexOption.IGNORE_CASE)
    private val DATE_DMY = Regex("""(?<!\d)(\d{1,2})[./](\d{1,2})[./](\d{4})(?!\d)""")
    private val DATE_ISO = Regex("""(?<!\d)(\d{4})-(\d{2})-(\d{2})(?!\d)""")
    private val TIME = Regex("""(?<![\d:])([01]?\d|2[0-3]):([0-5]\d)(?![\d:])""")
    private val UNIT = Regex(
        """(?<![\p{L}\p{N}_])(\d+(?:[.,]\d+)?)\s?(kg|km|mb|gb|kb|sm|mm)(?![\p{L}\p{N}_])""",
        RegexOption.IGNORE_CASE
    )
    private val DEGREE = Regex("""(?<![\p{L}\p{N}_])(\d+(?:[.,]\d+)?)\s?\u00B0\s?C""")
    // $ = \u0024, euro = \u20AC, funt = \u00A3, rubl = \u20BD
    private val CUR_BEFORE = Regex("""([\u0024\u20AC\u00A3\u20BD])\s?(\d+(?:[ \u00A0\u202F]\d{3})*(?:[.,]\d{1,2})?)""")
    private val CUR_AFTER = Regex("""(\d+(?:[ \u00A0\u202F]\d{3})*(?:[.,]\d{1,2})?)\s?([\u0024\u20AC\u00A3\u20BD])""")
    private val PERCENT = Regex("""(\d+(?:[.,]\d+)?)\s?%""")
    private val PHONE = Regex("""\+\d[\d\s\-()]{7,}\d""")
    private val HASHTAG = Regex("""#([\p{L}\p{N}_]+)""")
    private val DECIMAL = Regex("""(?<![\p{L}\p{N}_.,])(\d{1,12})[.,](\d{1,2})(?![\p{L}\p{N}_]|[.,]\d)""")
    private val GROUPED = Regex("""(?<![\p{L}\p{N}_.,+])\d{1,3}(?:[ \u00A0\u202F]\d{3})+(?![\p{L}\p{N}_]|[ \u00A0\u202F]\d|[.,]\d)""")
    private val PLAIN = Regex("""(?<![\p{L}\p{N}_.,+])\d{1,12}(?![\p{L}\p{N}_]|[.,]\d)""")

    private fun unitWord(u: String): String = when (u.lowercase()) {
        "kg" -> "kilogramm"
        "km" -> "kilometr"
        "mb" -> "megabayt"
        "gb" -> "gigabayt"
        "kb" -> "kilobayt"
        "sm" -> "santimetr"
        else -> "millimetr"
    }

    private fun currencyWord(c: String): String = when (c) {
        "\u0024" -> "dollar"
        "\u20AC" -> "yevro"
        "\u00A3" -> "funt"
        else -> "rubl"
    }

    private fun digitsToWords(s: String): String = s.filter { it.isDigit() }.map { UzNumbers.words(it - '0') }.joinToString(" ")

    private fun host(s: String): String =
        s.replace(".", " nuqta ").replace("-", " chiziqcha ").replace("_", " pastki chiziq ").replace(Regex("\\s+"), " ").trim()

    private fun urlToSpeech(raw: String): String {
        val core = raw.trimEnd('.', ',', ';', ':', '!', '?', ')', ']')
        val tail = raw.substring(core.length)
        var h = core.replaceFirst(Regex("^(?i)https?://"), "")
        h = h.removePrefix("www.").removePrefix("WWW.")
        val cut = h.indexOfAny(charArrayOf('/', '?', '#'))
        if (cut >= 0) h = h.substring(0, cut)
        return host(h) + tail
    }

    private fun phoneToSpeech(raw: String): String {
        val d = raw.filter { it.isDigit() }
        val groups = if (d.startsWith("998") && d.length == 12) {
            listOf(d.substring(0, 3), d.substring(3, 5), d.substring(5, 8), d.substring(8, 10), d.substring(10, 12))
        } else {
            d.chunked(3)
        }
        return groups.joinToString(", ") { digitsToWords(it) }
    }

    private fun dateToSpeech(d: Int, m: Int, y: Int): String? {
        if (d !in 1..31 || m !in 1..12 || y !in 1000..2999) return null
        return UzNumbers.ordinal(d) + " " + MONTHS[m - 1] + ", " + UzNumbers.words(y.toLong()) + " yil"
    }

    private fun numberToSpeech(digits: String, digitsMode: Boolean): String =
        if (digits.length > 1 && (digits.startsWith("0") || (digitsMode && digits.length in 4..8))) {
            digitsToWords(digits)
        } else {
            UzNumbers.words(digits.toLong())
        }

    /** @param digitsMode tasdiqlash kodi kabi matnlarda 4-8 xonali sonlarni raqamma-raqam o'qiydi. */
    fun normalize(text: String, digitsMode: Boolean = false): String {
        if (!text.any { it.isDigit() } && text.indexOf('@') < 0 && text.indexOf('#') < 0 &&
            !text.contains("http", ignoreCase = true) && !text.contains("www.", ignoreCase = true)
        ) return text

        var t = text
        t = EMAIL.replace(t) { m ->
            val parts = m.value.split('@', limit = 2)
            host(parts[0]) + " kuchukcha " + host(parts[1])
        }
        t = URL.replace(t) { m -> urlToSpeech(m.value) }
        t = DATE_DMY.replace(t) { m ->
            dateToSpeech(m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt()) ?: m.value
        }
        t = DATE_ISO.replace(t) { m ->
            dateToSpeech(m.groupValues[3].toInt(), m.groupValues[2].toInt(), m.groupValues[1].toInt()) ?: m.value
        }
        t = TIME.replace(t) { m -> UzNumbers.time(m.groupValues[1].toInt(), m.groupValues[2].toInt()) }
        t = UNIT.replace(t) { m -> m.groupValues[1] + " " + unitWord(m.groupValues[2]) }
        t = DEGREE.replace(t) { m -> m.groupValues[1] + " daraja" }
        t = CUR_BEFORE.replace(t) { m -> m.groupValues[2] + " " + currencyWord(m.groupValues[1]) }
        t = CUR_AFTER.replace(t) { m -> m.groupValues[1] + " " + currencyWord(m.groupValues[2]) }
        t = PERCENT.replace(t) { m -> m.groupValues[1] + " foiz" }
        t = PHONE.replace(t) { m -> phoneToSpeech(m.value) }
        t = HASHTAG.replace(t) { m -> "xeshteg " + m.groupValues[1] }
        t = DECIMAL.replace(t) { m ->
            val frac = m.groupValues[2]
            val den = if (frac.length == 1) "o'ndan" else "yuzdan"
            UzNumbers.words(m.groupValues[1].toLong()) + " butun " + den + " " + UzNumbers.words(frac.toLong())
        }
        t = GROUPED.replace(t) { m ->
            val digits = m.value.filter { it.isDigit() }
            // 12 xonadan uzun son (masalan, uzun hisob raqami) Long'ga sig'maydi: tegmaymiz, PLAIN uni guruh-guruh o'qiydi
            if (digits.length > 12) m.value else UzNumbers.words(digits.toLong())
        }
        t = PLAIN.replace(t) { m -> numberToSpeech(m.value, digitsMode) }
        return t
    }
}
