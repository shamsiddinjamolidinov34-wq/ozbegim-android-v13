package uz.ekranoquvchi.mvp.content

enum class Script { LATIN, CYRILLIC, MIXED, OTHER }

/**
 * Faqat o'qish uchun normalizatsiya: foydalanuvchining matni o'zgartirilmaydi (monografiya 14.4).
 * NFC, ko'rinmas belgilar, chiziqcha variantlari va ko'rinishda o'xshash belgilar (kirill "а" va lotin "a").
 */
object TextNormalizer {
    private val INVISIBLE = Regex("[\u200B\u200E\u200F\u202A-\u202E\u2060-\u2064\uFEFF\u00AD]")
    private val DASHES = Regex("[\u2010\u2011\u2012]")

    // Kirill -> lotin, ko'rinishda bir xil harflar (\u kodlari: tahrirlashda adashmaslik uchun)
    private val CYR_TO_LAT: Map<Char, Char> = mapOf(
        '\u0430' to 'a', '\u0435' to 'e', '\u043E' to 'o', '\u0440' to 'p', '\u0441' to 'c',
        '\u0445' to 'x', '\u0443' to 'y',
        '\u0410' to 'A', '\u0412' to 'B', '\u0415' to 'E', '\u041A' to 'K', '\u041C' to 'M',
        '\u041D' to 'H', '\u041E' to 'O', '\u0420' to 'P', '\u0421' to 'C', '\u0422' to 'T',
        '\u0425' to 'X', '\u0423' to 'Y',
    )
    private val LAT_TO_CYR: Map<Char, Char> = CYR_TO_LAT.entries.associate { (k, v) -> v to k }

    fun normalize(s: String): String {
        val nfc = java.text.Normalizer.normalize(s, java.text.Normalizer.Form.NFC)
        return DASHES.replace(INVISIBLE.replace(nfc, ""), "-")
    }

    fun isLatin(c: Char): Boolean = c in 'a'..'z' || c in 'A'..'Z' || c in '\u00C0'..'\u024F'

    fun isCyrillic(c: Char): Boolean = c in '\u0400'..'\u04FF'

    fun dominantScript(w: String): Script {
        var lat = 0
        var cyr = 0
        for (c in w) {
            if (isLatin(c)) lat++ else if (isCyrillic(c)) cyr++
        }
        return when {
            lat > 0 && cyr == 0 -> Script.LATIN
            cyr > 0 && lat == 0 -> Script.CYRILLIC
            lat > 0 && cyr > 0 -> Script.MIXED
            else -> Script.OTHER
        }
    }

    /** Bir so'zda ikki yozuv aralashsa, ko'pchilik yozuvga keltiradi (faqat o'xshash belgilar). */
    fun repairMixedScript(w: String): String {
        if (dominantScript(w) != Script.MIXED) return w
        val lat = w.count { isLatin(it) }
        val cyr = w.count { isCyrillic(it) }
        return when {
            lat > cyr -> w.map { CYR_TO_LAT[it] ?: it }.joinToString("")
            cyr > lat -> w.map { LAT_TO_CYR[it] ?: it }.joinToString("")
            else -> w
        }
    }
}
