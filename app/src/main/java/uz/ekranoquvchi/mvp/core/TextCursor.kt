package uz.ekranoquvchi.mvp.core

import android.view.accessibility.AccessibilityNodeInfo
import java.text.BreakIterator
import java.util.Locale

/** Matn bo'lagi: [start, end). */
class TextSeg(val start: Int, val end: Int)

/** Matn bo'ylab yurish birligi: harf, so'z, qator (gap) yoki xatboshi. rank: kichikdan kattaga tartib. */
enum class TextGran(val granularity: Int, val rank: Int) {
    CHAR(AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER, 0),
    WORD(AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD, 1),
    LINE(AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE, 2),
    PARAGRAPH(AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH, 3);

    companion object {
        fun of(granularity: Int): TextGran? = values().firstOrNull { it.granularity == granularity }
    }
}

/**
 * Matnni harf, so'z, qator va xatboshi bo'laklariga ajratadi. Tizim tugunining "harakat birliklari"ni qo'llab-quvvatlashiga
 * bog'liq emas: Telegram xabari kabi matni faqat contentDescription'da bo'lgan elementlarda ham ishlaydi.
 *
 * - Harf: tizim belgi chegaralari (emoji ham bitta); o'zbekcha "o‘" va "g‘" (har xil apostrof bilan) bitta harf.
 * - So'z: harf va raqamlar ketma-ketligi; harflar orasidagi apostrof so'zni bo'lmaydi (bo'lib, tug'ilgan); 3.14 va 12:30 butun.
 * - Qator: ekrandagi qator tizim tomonidan berilmagani uchun gaplar (nuqta, undov, so'roq) va yangi qatorlar bo'yicha.
 * - Xatboshi: yangi qator bilan ajratilgan bo'laklar.
 */
object TextSegments {
    /** Gap tugamasa ham shu uzunlikdan keyingi birinchi bo'shliqda qator bo'linadi. */
    const val MAX_LINE = 140

    private const val APOSTROPHES = "'\u2018\u2019\u02BB\u02BC\u0060\u00B4"
    private const val TERMINATORS = ".!?\u2026"
    private const val CLOSERS = "\"'\u201D\u2019\u00BB\u203A)]}"

    fun isApostrophe(c: Char): Boolean = APOSTROPHES.indexOf(c) >= 0

    fun split(text: String, gran: TextGran): List<TextSeg> = when (gran) {
        TextGran.CHAR -> chars(text)
        TextGran.WORD -> words(text)
        TextGran.LINE -> lines(text)
        TextGran.PARAGRAPH -> paragraphs(text)
    }

    // ------------------------------------------------------------------ harflar

    private fun chars(text: String): List<TextSeg> {
        val out = ArrayList<TextSeg>(text.length)
        if (text.isEmpty()) return out
        val bi = BreakIterator.getCharacterInstance(Locale.ROOT)
        bi.setText(text)
        var s = bi.first()
        var e = bi.next()
        while (e != BreakIterator.DONE) {
            if (e > s) out.add(TextSeg(s, e))
            s = e
            e = bi.next()
        }
        return mergeApostrophes(text, out)
    }

    /** o‘ va g‘ (har qanday apostrof bilan) bitta harf: "o", "apostrof" deb ikkiga bo'linmaydi. */
    private fun mergeApostrophes(text: String, segs: List<TextSeg>): List<TextSeg> {
        val out = ArrayList<TextSeg>(segs.size)
        var i = 0
        while (i < segs.size) {
            val a = segs[i]
            if (i + 1 < segs.size && a.end - a.start == 1) {
                val b = segs[i + 1]
                val c = text[a.start]
                val letter = c == 'o' || c == 'O' || c == 'g' || c == 'G'
                if (letter && b.end - b.start == 1 && isApostrophe(text[b.start])) {
                    out.add(TextSeg(a.start, b.end))
                    i += 2
                    continue
                }
            }
            out.add(a)
            i++
        }
        return out
    }

    // ------------------------------------------------------------------ so'zlar

    private fun isWordChar(cp: Int): Boolean {
        if (Character.isLetterOrDigit(cp)) return true
        val t = Character.getType(cp)
        return t == Character.NON_SPACING_MARK.toInt() ||
            t == Character.COMBINING_SPACING_MARK.toInt() ||
            t == Character.ENCLOSING_MARK.toInt()
    }

    private fun isSymbol(cp: Int): Boolean {
        val t = Character.getType(cp)
        return t == Character.MATH_SYMBOL.toInt() ||
            t == Character.CURRENCY_SYMBOL.toInt() ||
            t == Character.MODIFIER_SYMBOL.toInt() ||
            t == Character.OTHER_SYMBOL.toInt()
    }

    /** Harflar orasidagi apostrof va raqamlar orasidagi . , : so'zni bo'lmaydi. */
    private fun isJoiner(c: Int, prev: Int, next: Int): Boolean {
        if (!isWordChar(next)) return false
        if (c < 0x10000 && isApostrophe(c.toChar())) return isWordChar(prev)
        val sep = c == '.'.code || c == ','.code || c == ':'.code
        return sep && Character.isDigit(prev) && Character.isDigit(next)
    }

    private fun words(text: String): List<TextSeg> {
        val out = ArrayList<TextSeg>()
        val n = text.length
        var bi: BreakIterator? = null
        var i = 0
        while (i < n) {
            val cp = text.codePointAt(i)
            val len = Character.charCount(cp)
            if (isWordChar(cp)) {
                var j = i + len
                while (j < n) {
                    val c2 = text.codePointAt(j)
                    val l2 = Character.charCount(c2)
                    if (isWordChar(c2)) {
                        j += l2
                        continue
                    }
                    val next = j + l2
                    if (next < n && isJoiner(c2, text.codePointBefore(j), text.codePointAt(next))) {
                        j = next
                        continue
                    }
                    break
                }
                out.add(TextSeg(i, j))
                i = j
            } else if (isSymbol(cp)) {
                // emoji va boshqa belgilar: har biri alohida "so'z" (teri rangi, ZWJ ketma-ketligi bilan birga)
                val iter = bi ?: BreakIterator.getCharacterInstance(Locale.ROOT).also { it.setText(text) }
                bi = iter
                var j = iter.following(i)
                if (j == BreakIterator.DONE || j <= i) j = i + len
                out.add(TextSeg(i, j))
                i = j
            } else {
                i += len
            }
        }
        return out
    }

    // ------------------------------------------------------------------ qatorlar va xatboshilar

    private fun isNewline(c: Char): Boolean = c == '\n' || c == '\r' || c == '\u2028' || c == '\u2029'

    private fun trimmedSeg(text: String, a: Int, b: Int): TextSeg? {
        var s = a
        var e = b
        while (s < e && text[s].isWhitespace()) s++
        while (e > s && text[e - 1].isWhitespace()) e--
        return if (e > s) TextSeg(s, e) else null
    }

    private fun paragraphs(text: String): List<TextSeg> {
        val out = ArrayList<TextSeg>()
        val n = text.length
        var start = 0
        var i = 0
        while (i <= n) {
            if (i == n || isNewline(text[i])) {
                val seg = trimmedSeg(text, start, i)
                if (seg != null) out.add(seg)
                start = i + 1
            }
            i++
        }
        return out
    }

    private fun lines(text: String): List<TextSeg> {
        val out = ArrayList<TextSeg>()
        for (p in paragraphs(text)) splitSentences(text, p.start, p.end, out)
        return out
    }

    private fun splitSentences(text: String, from: Int, to: Int, out: MutableList<TextSeg>) {
        var start = from
        var i = from
        while (i < to) {
            val c = text[i]
            var cut = -1
            if (TERMINATORS.indexOf(c) >= 0) {
                var j = i + 1
                while (j < to && (TERMINATORS.indexOf(text[j]) >= 0 || CLOSERS.indexOf(text[j]) >= 0)) j++
                if ((j >= to || text[j].isWhitespace()) && !isInitialOrNumber(text, start, i, c)) cut = j
            } else if (c.isWhitespace() && i - start >= MAX_LINE) {
                cut = i
            }
            if (cut >= 0) {
                val seg = trimmedSeg(text, start, cut)
                if (seg != null) out.add(seg)
                start = cut
                i = if (cut > i) cut else i + 1
            } else {
                i++
            }
        }
        val last = trimmedSeg(text, start, to)
        if (last != null) out.add(last)
    }

    /** "I. Turayeva" (bosh harf) va "1. Birinchi" (raqamli ro'yxat) nuqtasi gapni tugatmaydi. */
    private fun isInitialOrNumber(text: String, start: Int, i: Int, c: Char): Boolean {
        if (c != '.') return false
        if (i - 1 >= start && text[i - 1].isLetter() && (i - 2 < start || text[i - 2].isWhitespace())) return true
        var k = start
        while (k < i && text[k].isWhitespace()) k++
        if (k >= i) return false
        for (m in k until i) if (!text[m].isDigit()) return false
        return true
    }
}

/**
 * Matn ichidagi virtual kursor. Tizim "harf bo'yicha yurish"ni qo'llamaydigan elementlar (masalan, Telegram xabari)
 * uchun O'zbegimning o'zi matn bo'ylab yuradi. Kursor oxirgi aytilgan bo'lakning [from, to) joyini eslab qoladi.
 * Birlik almashganda (harf <-> so'z <-> qator) kursor joyi saqlanadi: so'zni o'qib, "harflar"ga o'tilsa, shu so'zning
 * birinchi harfidan boshlanadi.
 */
class TextCursor {
    private var text = ""
    private var key = ""
    private var from = 0
    private var to = 0
    private var last: TextGran? = null

    /** Kursor qaysi elementga bog'langan (element almashganda boshidan boshlanadi). */
    val boundKey: String
        get() = key

    fun reset() {
        text = ""
        key = ""
        from = 0
        to = 0
        last = null
    }

    /** Boshqa element yoki matni o'zgargan bo'lsa, kursorni boshiga qaytaradi. */
    fun bind(newKey: String, newText: String) {
        if (newKey == key && newText == text) return
        key = newKey
        text = newText
        from = 0
        to = 0
        last = null
    }

    /** Keyingi bo'lak; matn tugagan bo'lsa null. */
    fun next(gran: TextGran): String? = move(gran, true)

    /** Oldingi bo'lak; matn boshiga yetilgan bo'lsa null. */
    fun prev(gran: TextGran): String? = move(gran, false)

    private fun move(gran: TextGran, forward: Boolean): String? {
        if (text.isEmpty()) return null
        val segs = TextSegments.split(text, gran)
        if (segs.isEmpty()) return null
        rebase(gran, segs)
        var target: TextSeg? = null
        if (forward) {
            for (s in segs) {
                if (s.start >= to) {
                    target = s
                    break
                }
            }
        } else {
            for (k in segs.indices.reversed()) {
                if (segs[k].end <= from) {
                    target = segs[k]
                    break
                }
            }
        }
        val t = target ?: return null
        from = t.start
        to = t.end
        return text.substring(t.start, t.end)
    }

    private fun rebase(gran: TextGran, segs: List<TextSeg>) {
        val before = last
        last = gran
        if (before == null || before == gran) return
        if (gran.rank < before.rank) {
            // kattadan kichikka (so'zdan harfga): oxirgi bo'lakning boshidan boshlaymiz
            to = from
        } else {
            // kichikdan kattaga (harfdan so'zga): kursor turgan so'zdan (qatordan) boshlaymiz
            for (s in segs) {
                if (s.start <= from && from < s.end) {
                    from = s.start
                    to = s.start
                    break
                }
            }
        }
    }
}
