package uz.ekranoquvchi.mvp.content

/** Uzun matnni gap bo'laklariga ajratadi: birinchi bo'lak tez eshitilsin (monografiya 9.2, 9.8). */
object TextChunker {
    fun split(text: String, soft: Int = 160, hard: Int = 260): List<String> {
        if (text.length <= hard) return listOf(text)
        val out = ArrayList<String>()
        val sb = StringBuilder()
        for (word in text.split(' ')) {
            if (sb.isNotEmpty() && sb.length + 1 + word.length > hard) {
                out.add(sb.toString())
                sb.setLength(0)
            }
            if (sb.isNotEmpty()) sb.append(' ')
            sb.append(word)
            if (sb.length >= soft && word.isNotEmpty() && ".!?\u2026;:".indexOf(word.last()) >= 0) {
                out.add(sb.toString())
                sb.setLength(0)
            }
        }
        if (sb.isNotEmpty()) out.add(sb.toString())
        return out
    }
}
