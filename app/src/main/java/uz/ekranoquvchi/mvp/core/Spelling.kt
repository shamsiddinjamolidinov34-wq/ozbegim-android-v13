package uz.ekranoquvchi.mvp.core

import uz.ekranoquvchi.mvp.content.CharNames

/**
 * Harflab o'qish: matnni harf nomlari bilan, vergul bilan ajratib beradi ("Salom" -> "katta es, a, el, o, em").
 * Vergul nutqda qisqa to'xtash beradi, shuning uchun har bir harf aniq eshitiladi.
 */
object Spelling {
    /** Bir marta harflab o'qiladigan eng ko'p birlik soni (uzun matn nutqni uzoq band qilmasligi uchun). */
    const val MAX_UNITS = 300

    fun spell(text: String, maxUnits: Int = MAX_UNITS): String {
        val sb = StringBuilder()
        var count = 0
        var lastBlank = false
        for (seg in TextSegments.split(text, TextGran.CHAR)) {
            if (count >= maxUnits) break
            val unit = text.substring(seg.start, seg.end)
            val blank = unit.isBlank()
            // ketma-ket bo'shliqlar bitta "bo'shliq" deb aytiladi
            if (blank && lastBlank) continue
            lastBlank = blank
            if (sb.isNotEmpty()) sb.append(", ")
            sb.append(CharNames.spelled(unit))
            count++
        }
        return sb.toString()
    }
}
