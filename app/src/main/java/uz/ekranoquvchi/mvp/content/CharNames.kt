package uz.ekranoquvchi.mvp.content

/**
 * Bitta belgini nutqqa moslab nomlaydi (yozilgan belgi, harf bo'yicha yurish, harflab o'qish).
 * Nomlar o'zbekcha; harf nomlari faqat harflab o'qishda ishlatiladi.
 */
object CharNames {
    /** Apostrof ko'rinishlari: ' ‘ ’ ʻ ʼ ` */
    private const val APOSTROPHES = "'\u2018\u2019\u02BB\u02BC\u0060"

    /** Lotin harflarining o'zbekcha nomlari (harflab o'qish uchun). */
    private val LATIN_NAMES: Map<Char, String> = mapOf(
        'a' to "a", 'b' to "be", 'c' to "se", 'd' to "de", 'e' to "e", 'f' to "ef", 'g' to "ge", 'h' to "he",
        'i' to "i", 'j' to "je", 'k' to "ka", 'l' to "el", 'm' to "em", 'n' to "en", 'o' to "o", 'p' to "pe",
        'q' to "qe", 'r' to "er", 's' to "es", 't' to "te", 'u' to "u", 'v' to "ve", 'w' to "dubl ve",
        'x' to "xe", 'y' to "ye", 'z' to "ze",
    )

    fun speakable(s: String): String {
        if (s.length != 1) return s
        return when (s[0]) {
            ' ', '\u00A0' -> "bo'shliq"
            '\n' -> "yangi qator"
            '\t' -> "tabulyatsiya"
            '.' -> "nuqta"
            ',' -> "vergul"
            '?' -> "so'roq belgisi"
            '!' -> "undov belgisi"
            ':' -> "ikki nuqta"
            ';' -> "nuqtali vergul"
            '-' -> "chiziqcha"
            '\u2013', '\u2014' -> "uzun chiziq"
            '\u2026' -> "ko'p nuqta"
            '\'', '\u2018', '\u2019', '\u02BB', '\u02BC', '\u0060' -> "apostrof"
            '"', '\u201C', '\u201D', '\u00AB', '\u00BB' -> "qo'shtirnoq"
            '(' -> "qavs ochildi"
            ')' -> "qavs yopildi"
            '[' -> "kvadrat qavs ochildi"
            ']' -> "kvadrat qavs yopildi"
            '{' -> "figurali qavs ochildi"
            '}' -> "figurali qavs yopildi"
            '@' -> "kuchukcha"
            '/' -> "qiya chiziq"
            '\\' -> "teskari qiya chiziq"
            '*' -> "yulduzcha"
            '#' -> "panjara"
            '%' -> "foiz"
            '&' -> "va belgisi"
            '_' -> "ostki chiziq"
            '=' -> "teng belgisi"
            '+' -> "qo'shish belgisi"
            '<' -> "kichik belgisi"
            '>' -> "katta belgisi"
            '|' -> "tik chiziq"
            '~' -> "tilda"
            '$' -> "dollar belgisi"
            '\u2116' -> "raqam belgisi"
            else -> s
        }
    }

    /** Harf bo'yicha yurishda aytiladigan nom: "o‘" va "g‘" bitta harf ("o apostrof"), qolganlari speakable(). */
    fun unitName(s: String): String {
        if (s.length == 2 && isOG(s[0]) && APOSTROPHES.indexOf(s[1]) >= 0) return "${s[0]} apostrof"
        return speakable(s)
    }

    /**
     * Harflab o'qishda bitta birlikning aytilishi: harf nomi ("b" -> "be"), katta harf oldidan "katta",
     * "o‘" va "g‘" -> "o apostrof", boshqa belgilar speakable() kabi.
     */
    fun spelled(s: String): String {
        if (s.isEmpty()) return s
        if (s.length == 2 && isOG(s[0]) && APOSTROPHES.indexOf(s[1]) >= 0) {
            val big = if (s[0].isUpperCase()) "katta " else ""
            return big + s[0].lowercaseChar() + " apostrof"
        }
        if (s.length != 1) return s
        val c = s[0]
        if (!c.isLetter()) return speakable(s)
        val big = if (c.isUpperCase()) "katta " else ""
        val low = c.lowercaseChar()
        return big + (LATIN_NAMES[low] ?: low.toString())
    }

    private fun isOG(c: Char): Boolean = c == 'o' || c == 'O' || c == 'g' || c == 'G'
}
