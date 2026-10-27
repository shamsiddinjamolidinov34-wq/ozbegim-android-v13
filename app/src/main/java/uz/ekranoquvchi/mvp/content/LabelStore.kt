package uz.ekranoquvchi.mvp.content

/**
 * Maxsus yorliqlar: yorlig'i yo'q elementlarga foydalanuvchi o'zi nom beradi (FR-12, jamoa paketlariga asos).
 * Matn fayli, har qatorda: to'liq_id=yorliq   (masalan: com.ilova:id/send=Yuborish)
 */
object LabelStore {
    @Volatile
    private var map: Map<String, String> = emptyMap()

    fun load(text: String) {
        map = parse(text)
    }

    fun lookup(viewId: String?): String? = if (viewId == null) null else map[viewId]

    fun parse(text: String): Map<String, String> {
        val out = HashMap<String, String>()
        for (line in text.lineSequence()) {
            val l = line.trim()
            if (l.isEmpty() || l.startsWith("#")) continue
            val i = l.indexOf('=')
            if (i <= 0 || i == l.length - 1) continue
            out[l.substring(0, i).trim()] = l.substring(i + 1).trim()
        }
        return out
    }
}
