package uz.ekranoquvchi.mvp.content

/**
 * Rim raqamlari (XIX, II, IV...) ni o'zbekcha tartib sonli so'zga aylantiradi, toki asrlar va boblar
 * harf-harf ("iks ay iks") emas, to'g'ri o'qilsin: "XIX asr" -> "o'n to'qqizinchi asr".
 * Natijadagi so'zlar ko'rsatma so'zning yozuvida chiqadi (lotin yoki kirill).
 *
 * Faqat ma'nosi aniq joylarda ishlaydi, shuning uchun "M. Gorkiy", "Vitamin C", "DVD", "I think" kabi matnlarga tegilmaydi:
 *  1) o'ngda ma'no so'zi turganda: asr, jahon, bob, qism, jild, bo'lim, kurs, davr, bosqich... (kirillda ham);
 *     "XV-XVI asrlar", "XV, XVI va XVII asrlar" kabi zanjirlar to'liq o'qiladi;
 *  2) bosh harfli ismdan keyin, kichik son bo'lsa: "Napoleon III", "Karl XII".
 * Kirill yozuvida ko'pincha Rim raqami kirill o'xshash harflar bilan yoziladi (Х, І): ular ham taniladi.
 */
object RomanNumerals {
    // Kirill o'xshash harflar: Х (U+0425), І (U+0406), С (U+0421), М (U+041C)
    private const val LOOKALIKES = "\u0425\u0406\u0421\u041C"

    private val CANDIDATE = Regex(
        "(?<![\\p{L}\\p{N}])(?:[IVXLCDM" + LOOKALIKES + "]{1,12}|[ivx]{2,6})(?![\\p{L}\\p{N}])"
    )
    private val CANON = Regex("^M{0,3}(CM|CD|D?C{0,3})(XC|XL|L?X{0,3})(IX|IV|V?I{0,3})$")

    private const val SUFFIX_LAT = "(?:lar|ning|ni|ga|da|dan|dagi|gacha|imiz|ingiz|i|si|cha|dek)*"
    private const val SUFFIX_CYR = "(?:\u043B\u0430\u0440|\u043D\u0438\u043D\u0433|\u043D\u0438|\u0433\u0430|\u0434\u0430|\u0434\u0430\u043D|\u0434\u0430\u0433\u0438|\u0433\u0430\u0447\u0430|\u0438\u043C\u0438\u0437|\u0438\u043D\u0433\u0438\u0437|\u0438|\u0441\u0438|\u0447\u0430|\u0434\u0435\u043A)*"

    // asr, jahon, bob, qism, jild, bo'lim, kurs, davr, bosqich, chorak, kongress, qurultoy, sessiya, olimpiada
    private val CONTEXT_LAT = Regex(
        "^(?:asr|jahon|bob|qism|jild|bo'lim|kurs|davr|bosqich|chorak|kongress|qurultoy|sessiya|olimpiada)" + SUFFIX_LAT + "$"
    )
    private val CONTEXT_CYR = Regex(
        "^(?:\u0430\u0441\u0440|\u0436\u0430\u04B3\u043E\u043D|\u0431\u043E\u0431|\u049B\u0438\u0441\u043C|\u0436\u0438\u043B\u0434|" +
            "\u0431\u045E\u043B\u0438\u043C|\u043A\u0443\u0440\u0441|\u0434\u0430\u0432\u0440|\u0431\u043E\u0441\u049B\u0438\u0447|" +
            "\u0447\u043E\u0440\u0430\u043A|\u043A\u043E\u043D\u0433\u0440\u0435\u0441\u0441|\u049B\u0443\u0440\u0443\u043B\u0442\u043E\u0439|" +
            "\u0441\u0435\u0441\u0441\u0438\u044F|\u043E\u043B\u0438\u043C\u043F\u0438\u0430\u0434\u0430)" + SUFFIX_CYR + "$"
    )

    // Zanjirdagi bog'lovchilar: "XV va XVI asrlar"
    private val CONNECTORS = setOf("va", "yoki", "hamda", "\u0432\u0430", "\u0451\u043A\u0438", "\u04B3\u0430\u043C\u0434\u0430")

    /** Rim raqamini songa aylantiradi (1..3999); noto'g'ri yozilgan bo'lsa (IIII, VX...) null. */
    fun parse(token: String): Int? {
        if (token.isEmpty()) return null
        val sb = StringBuilder(token.length)
        for (ch in token) {
            val mapped = when (ch) {
                '\u0425' -> 'X'
                '\u0406' -> 'I'
                '\u0421' -> 'C'
                '\u041C' -> 'M'
                else -> ch.uppercaseChar()
            }
            sb.append(mapped)
        }
        val t = sb.toString()
        if (!CANON.matches(t)) return null
        var total = 0
        var prev = 0
        for (i in t.length - 1 downTo 0) {
            val v = when (t[i]) {
                'I' -> 1
                'V' -> 5
                'X' -> 10
                'L' -> 50
                'C' -> 100
                'D' -> 500
                else -> 1000
            }
            if (v < prev) {
                total -= v
            } else {
                total += v
                prev = v
            }
        }
        return if (total in 1..3999) total else null
    }

    /** Matndagi mos Rim raqamlarini o'zbekcha tartib sonlarga almashtiradi; boshqa matnga tegmaydi. */
    fun normalize(text: String): String {
        if (text.length < 2) return text
        val all = CANDIDATE.findAll(text).toList()
        if (all.isEmpty()) return text
        val sb = StringBuilder(text.length + 24)
        var last = 0
        for ((idx, m) in all.withIndex()) {
            val value = parse(m.value) ?: continue
            val cyrillic = contextScript(text, all, idx, value) ?: continue
            sb.append(text, last, m.range.first)
            val words = UzNumbers.ordinal(value)
            sb.append(if (cyrillic) UzScripts.latinToCyrillic(words) else words)
            last = m.range.last + 1
        }
        if (last == 0) return text
        sb.append(text, last, text.length)
        return sb.toString()
    }

    /**
     * Raqam almashtirilishi kerak bo'lsa, natija yozuvini qaytaradi (true: kirill, false: lotin); aks holda null.
     */
    private fun contextScript(text: String, all: List<MatchResult>, idx: Int, value: Int): Boolean? {
        val m = all[idx]
        val tok = m.value
        val lower = tok.all { it.isLowerCase() }
        val lookalike = tok.any { LOOKALIKES.indexOf(it) >= 0 }

        // 1) o'ngdagi ma'no so'zi; keyingi Rim raqamlari zanjirni davom ettiradi
        var j = idx
        var end = m.range.last + 1
        while (true) {
            end = skipChain(text, end)
            if (j + 1 < all.size && all[j + 1].range.first == end && parse(all[j + 1].value) != null) {
                j++
                end = all[j].range.last + 1
                continue
            }
            break
        }
        val word = wordAt(text, end)
        if (word != null) {
            val w = UzScripts.unifyApostrophes(word).lowercase()
            if (CONTEXT_LAT.matches(w)) return lookalike
            if (CONTEXT_CYR.matches(w)) return true
        }
        if (lower) return null

        // 2) bosh harfli ismdan keyin, kichik son: "Napoleon III". Bitta harf ("I", "C") va katta sonlar ("MIX") tegilmaydi.
        if (tok.length >= 2 && value <= 30) {
            val prev = prevWord(text, m.range.first)
            if (prev != null && prev.length >= 3 && prev[0].isUpperCase() && prev.drop(1).all { it.isLowerCase() || isApostrophe(it) }) {
                return prev.any { it in '\u0400'..'\u04FF' }
            }
        }
        return null
    }

    private fun isApostrophe(c: Char): Boolean =
        c == '\'' || c == '\u02BB' || c == '\u02BC' || c == '\u2018' || c == '\u2019' || c == '`'

    private fun skipChain(text: String, from: Int): Int {
        var i = from
        while (i < text.length) {
            val c = text[i]
            if (c == ' ' || c == '\u00A0' || c == '-' || c == '\u2013' || c == '\u2014' || c == ',') {
                i++
                continue
            }
            val w = wordAt(text, i)
            if (w != null && CONNECTORS.contains(w.lowercase())) {
                i += w.length
                continue
            }
            break
        }
        return i
    }

    private fun wordAt(text: String, from: Int): String? {
        var k = from
        while (k < text.length && (text[k].isLetter() || isApostrophe(text[k]))) k++
        return if (k > from) text.substring(from, k) else null
    }

    /** Raqamdan oldingi so'z, agar ular orasida aynan bitta bo'shliq bo'lsa. */
    private fun prevWord(text: String, start: Int): String? {
        if (start < 2) return null
        val sp = text[start - 1]
        if (sp != ' ' && sp != '\u00A0') return null
        var k = start - 1
        while (k > 0 && (text[k - 1].isLetter() || isApostrophe(text[k - 1]))) k--
        return if (k < start - 1) text.substring(k, start - 1) else null
    }
}
