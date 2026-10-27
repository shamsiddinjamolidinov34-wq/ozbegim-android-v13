package uz.ekranoquvchi.mvp.content

enum class PackStatus { ACTIVE, PROJECT }

data class ScriptPackInfo(val id: String, val version: String, val status: PackStatus)

/**
 * O'zbek yozuvlari: kirill, hozirgi lotin (1995) va yangilangan lotin (2026, loyiha).
 * Qoida ("uch yozuv, bir talaffuz", monografiya 14.4 va 14.6): bir xil matn qaysi yozuvda kelmasin,
 * nutq dvigateliga bir xil ko'rinishda boradi. MVP'da markaz (pivot) hozirgi lotin;
 * har yozuv faqat markazga o'tkazgich beradi, shuning uchun n yozuv uchun n ta o'tkazgich yetadi.
 */
object UzScripts {
    val packs = listOf(
        ScriptPackInfo("uz-Cyrl", "1.0", PackStatus.ACTIVE),
        ScriptPackInfo("uz-Latn-1995", "1.0", PackStatus.ACTIVE),
        ScriptPackInfo("uz-Latn-2026", "0.1", PackStatus.PROJECT),
    )

    private const val CYR_VOWELS = "аеёиоуэюяўыАЕЁИОУЭЮЯЎЫ"
    private const val UZ_ONLY = "ўқғҳЎҚҒҲ"
    private const val RU_ONLY = "ыщЫЩ"
    private const val APOS_VARIANTS = "\u02BB\u02BC\u2018\u2019\u0060\u00B4\u2032\u2035\u02B9"

    // Kirill -> hozirgi lotin. "е" harfi kontekstga bog'liq, alohida ishlanadi.
    private val CYR_MAP: Map<Char, String> = mapOf(
        'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d", 'ё' to "yo", 'ж' to "j", 'з' to "z",
        'и' to "i", 'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n", 'о' to "o", 'п' to "p",
        'р' to "r", 'с' to "s", 'т' to "t", 'у' to "u", 'ф' to "f", 'х' to "x", 'ц' to "ts", 'ч' to "ch",
        'ш' to "sh", 'щ' to "sh", 'ъ' to "'", 'ь' to "", 'ы' to "i", 'э' to "e", 'ю' to "yu", 'я' to "ya",
        'ў' to "o'", 'қ' to "q", 'ғ' to "g'", 'ҳ' to "h",
    )

    // Yangilangan lotin -> hozirgi lotin: Ş=sh, Ç=ch, Ö=o', Ğ=g'.
    // Ş/ş (U+015E/015F) va ko'rinishda o'xshash Ș/ș (U+0218/0219) ikkalasi ham qabul qilinadi.
    private val NEW_LATIN: Map<Char, String> = mapOf(
        '\u015F' to "sh", '\u0219' to "sh", '\u00E7' to "ch", '\u00F6' to "o'", '\u011F' to "g'",
        '\u015E' to "Sh", '\u0218' to "Sh", '\u00C7' to "Ch", '\u00D6' to "O'", '\u011E' to "G'",
    )

    // Ishonchli turkcha belgilar: ı İ ü Ü (yangilangan o'zbek alifbosida yo'q)
    private const val TURKISH_MARKERS = "\u0131\u0130\u00FC\u00DC"

    fun hasUzbekOnlyLetters(w: String): Boolean = w.any { UZ_ONLY.indexOf(it) >= 0 }

    fun hasRussianOnlyLetters(w: String): Boolean = w.any { RU_ONLY.indexOf(it) >= 0 }

    fun hasNewLatinLetters(w: String): Boolean = w.any { NEW_LATIN.containsKey(it) }

    fun looksTurkish(text: String): Boolean = text.any { TURKISH_MARKERS.indexOf(it) >= 0 }

    private fun isCyrVowel(c: Char) = CYR_VOWELS.indexOf(c) >= 0

    private fun capitalize(s: String) = if (s.isEmpty()) s else s.substring(0, 1).uppercase() + s.substring(1)

    /** Kirill so'zni hozirgi lotinga o'tkazadi (ruscha so'zlar uchun ham ishlaydi, monografiya 10.4). */
    fun cyrillicToLatin(word: String): String {
        val letters = word.filter { it.isLetter() }
        val allCaps = letters.length > 1 && letters.all { it.isUpperCase() }
        val sb = StringBuilder(word.length + 4)
        var prev = ' '
        for (ch in word) {
            val lower = ch.lowercaseChar()
            val piece: String? = if (lower == '\u0435') {
                // "е": so'z boshida, unlidan va ъ/ь dan keyin "ye", boshqa joyda "e"
                if (!prev.isLetter() || isCyrVowel(prev) || prev == '\u044A' || prev == '\u042A' ||
                    prev == '\u044C' || prev == '\u042C'
                ) "ye" else "e"
            } else {
                CYR_MAP[lower]
            }
            when {
                piece == null -> sb.append(ch)
                ch.isUpperCase() -> sb.append(if (allCaps) piece.uppercase() else capitalize(piece))
                else -> sb.append(piece)
            }
            prev = ch
        }
        return sb.toString()
    }

    /** Yangilangan lotinni hozirgi lotinga o'tkazadi: şahar -> shahar, Özbekiston -> O'zbekiston. */
    fun newLatinToCurrent(word: String): String {
        val letters = word.filter { it.isLetter() }
        val allCaps = letters.length > 1 && letters.all { it.isUpperCase() }
        val sb = StringBuilder(word.length + 4)
        for (c in word) {
            val piece = NEW_LATIN[c]
            when {
                piece == null -> sb.append(c)
                c.isUpperCase() && allCaps -> sb.append(piece.uppercase())
                else -> sb.append(piece)
            }
        }
        return sb.toString()
    }

    /** Apostrof variantlarini (o', o‘, oʻ, o`, o’ ...) bitta ko'rinishga keltiradi. */
    fun unifyApostrophes(s: String): String {
        if (s.none { APOS_VARIANTS.indexOf(it) >= 0 }) return s
        val sb = StringBuilder(s.length)
        for (c in s) sb.append(if (APOS_VARIANTS.indexOf(c) >= 0) '\'' else c)
        return sb.toString()
    }

    /** Dvigatel qaysi apostrofni yaxshi o'qisa, shuni qo'yadi (sozlama). */
    fun applyApostrophe(s: String, style: Char): String = if (style == '\'') s else s.replace('\'', style)

    // Hozirgi lotin -> kirill (bir harfli moslar). Ikki harfli (sh, ch, o', g', yo, yu, ya, ye) va "e" alohida ishlanadi.
    private val LAT_TO_CYR: Map<Char, String> = mapOf(
        'a' to "\u0430", 'b' to "\u0431", 'c' to "\u043A", 'd' to "\u0434", 'f' to "\u0444", 'g' to "\u0433",
        'h' to "\u04B3", 'i' to "\u0438", 'j' to "\u0436", 'k' to "\u043A", 'l' to "\u043B", 'm' to "\u043C",
        'n' to "\u043D", 'o' to "\u043E", 'p' to "\u043F", 'q' to "\u049B", 'r' to "\u0440", 's' to "\u0441",
        't' to "\u0442", 'u' to "\u0443", 'v' to "\u0432", 'w' to "\u0432", 'x' to "\u0445", 'y' to "\u0439",
        'z' to "\u0437",
    )

    /**
     * Hozirgi o'zbek lotinini kirillga o'tkazadi (sh=ш, ch=ч, o'=ў, g'=ғ, h=ҳ, q=қ, x=х, yo=ё, yu=ю, ya=я, ye=е;
     * so'z boshidagi e=э; harflar orasidagi apostrof=ъ). Avtomatik tarjima natijasini kirillda o'qish uchun.
     */
    fun latinToCyrillic(text: String): String {
        val t = unifyApostrophes(text)
        val sb = StringBuilder(t.length + 8)
        var i = 0
        var inWord = false
        while (i < t.length) {
            val c = t[i]
            if (c == '\'') {
                val nextLetter = i + 1 < t.length && t[i + 1].isLetter()
                if (inWord && nextLetter) sb.append('\u044A') else sb.append(c)
                i++
                continue
            }
            if (!c.isLetter()) {
                sb.append(c)
                inWord = false
                i++
                continue
            }
            val upper = c.isUpperCase()
            val lc = c.lowercaseChar()
            val n = if (i + 1 < t.length) t[i + 1].lowercaseChar() else '\u0000'
            val n2 = if (i + 2 < t.length) t[i + 2] else '\u0000'
            var step = 1
            val piece: String = when {
                lc == 'o' && n == '\'' -> {
                    step = 2
                    "\u045E"
                }
                lc == 'g' && n == '\'' -> {
                    step = 2
                    "\u0493"
                }
                lc == 's' && n == 'h' -> {
                    step = 2
                    "\u0448"
                }
                lc == 'c' && n == 'h' -> {
                    step = 2
                    "\u0447"
                }
                lc == 'y' && n == 'o' && n2 != '\'' -> {
                    step = 2
                    "\u0451"
                }
                lc == 'y' && n == 'u' -> {
                    step = 2
                    "\u044E"
                }
                lc == 'y' && n == 'a' -> {
                    step = 2
                    "\u044F"
                }
                lc == 'y' && n == 'e' -> {
                    step = 2
                    "\u0435"
                }
                lc == 'e' -> if (inWord) "\u0435" else "\u044D"
                else -> LAT_TO_CYR[lc] ?: c.toString()
            }
            sb.append(if (upper) piece.uppercase() else piece)
            inWord = true
            i += step
        }
        return sb.toString()
    }

    /**
     * Hozirgi lotinni yangilangan lotinga (2026, loyiha) o'tkazadi: sh=ş, ch=ç, o'=ö, g'=ğ.
     * Dvigatel uchun matn baribir quvurda hozirgi lotinga qaytariladi ("uch yozuv, bir talaffuz").
     */
    fun latinToNewLatin(text: String): String {
        val t = unifyApostrophes(text)
        val sb = StringBuilder(t.length)
        var i = 0
        while (i < t.length) {
            val c = t[i]
            val lc = c.lowercaseChar()
            val n = if (i + 1 < t.length) t[i + 1] else '\u0000'
            val ln = n.lowercaseChar()
            val upper = c.isUpperCase()
            if (lc == 's' && ln == 'h') {
                sb.append(if (upper) '\u015E' else '\u015F')
                i += 2
            } else if (lc == 'c' && ln == 'h') {
                sb.append(if (upper) '\u00C7' else '\u00E7')
                i += 2
            } else if (lc == 'o' && n == '\'') {
                sb.append(if (upper) '\u00D6' else '\u00F6')
                i += 2
            } else if (lc == 'g' && n == '\'') {
                sb.append(if (upper) '\u011E' else '\u011F')
                i += 2
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }
}
