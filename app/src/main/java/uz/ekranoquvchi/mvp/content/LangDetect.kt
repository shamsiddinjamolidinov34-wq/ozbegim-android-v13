package uz.ekranoquvchi.mvp.content

/**
 * Matn tilini tez va tarmoqsiz taxmin qiladi: avtomatik tarjima faqat o'zbekcha bo'lmagan matnni
 * tashqi xizmatga yuborishi uchun. Aniq til emas, "tarjima kerakmi" degan savolga javob beradi.
 * Shubhali matn (noma'lum so'z) tarjimaga yuboriladi: xizmat o'zbekcha matn va nomlarni o'zgartirmay qaytaradi,
 * natija keshlanadi, shuning uchun xato ogohlantirish arzon.
 */
object LangDetect {
    enum class Lang { UZ_LATN, UZ_CYRL, RU, EN, OTHER, NEUTRAL }

    private val WORD = Regex("[\\p{L}']+")
    private val URL_OR_MAIL = Regex("^(https?://|www\\.)\\S+$|^\\S+@\\S+\\.\\S+$", RegexOption.IGNORE_CASE)

    // Kirill: faqat o'zbekcha va faqat ruscha harflar
    private const val UZ_ONLY = "\u045E\u049B\u0493\u04B3" // ў қ ғ ҳ
    private const val RU_ONLY = "\u044B\u0449\u044C"       // ы щ ь

    private val UZ_LATIN_WORDS: Set<String> = setOf(
        "va", "bu", "bilan", "uchun", "ham", "emas", "bor", "yo'q", "lekin", "men", "sen", "biz", "siz", "ular",
        "shu", "ana", "mana", "juda", "ko'p", "kam", "katta", "kichik", "yangi", "eski", "yaxshi", "yomon", "kerak",
        "mumkin", "qanday", "nima", "qayerda", "qachon", "kim", "nega", "haqida", "orqali", "bo'yicha", "ichida",
        "keyin", "oldin", "hozir", "bugun", "ertaga", "kecha", "ha", "yoki", "agar", "chunki", "ammo", "endi",
        "sozlamalar", "sozlama", "ilova", "ilovalar", "orqaga", "oldinga", "bosh", "ekran", "qidirish", "qidiruv",
        "yuborish", "yuboring", "saqlash", "bekor", "qilish", "o'chirish", "yoqish", "o'chirilgan", "yoqilgan",
        "tanlash", "ochish", "yopish", "davom", "etish", "tasdiqlash", "kirish", "chiqish", "ro'yxat", "xabar",
        "xabarlar", "qo'ng'iroq", "qo'ng'iroqlar", "kontaktlar", "rasm", "rasmlar", "musiqa", "fayl", "fayllar",
        "papka", "hujjat", "tarmoq", "parol", "foydalanuvchi", "hisob", "til", "tillar", "vaqt", "sana", "soat",
        "daqiqa", "soniya", "kun", "hafta", "oy", "yil", "bildirishnoma", "bildirishnomalar", "ovoz", "yorqinlik",
        "batareya", "xotira", "qurilma", "yordam", "ma'lumot", "o'zbekcha", "o'zbek", "qurilmalar", "ulanish",
        "ulangan", "yuklab", "yuklash", "yangilash", "yangilanish", "tugma", "belgilangan", "tanlangan",
        "kechirasiz", "rahmat", "salom", "xayr", "iltimos", "boshlash", "tugatish", "keyingi", "oldingi",
    )

    private val UZ_CYR_WORDS: Set<String> = setOf(
        "\u0432\u0430", "\u0431\u0443", "\u0431\u0438\u043B\u0430\u043D", "\u0443\u0447\u0443\u043D", "\u044D\u043C\u0430\u0441",
        "\u0431\u043E\u0440", "\u043B\u0435\u043A\u0438\u043D", "\u043C\u0435\u043D", "\u0441\u0435\u043D", "\u0431\u0438\u0437",
        "\u0441\u0438\u0437", "\u0443\u043B\u0430\u0440", "\u043A\u0435\u0440\u0430\u043A", "\u0451\u043A\u0438",
        "\u0441\u043E\u0437\u043B\u0430\u043C\u0430\u043B\u0430\u0440", "\u0438\u043B\u043E\u0432\u0430",
        "\u043E\u0440\u049B\u0430\u0433\u0430", "\u0445\u0430\u0431\u0430\u0440",
    )

    private val EN_WORDS: Set<String> = setOf(
        "the", "and", "of", "to", "in", "is", "are", "was", "were", "for", "on", "with", "as", "at", "by", "from",
        "this", "that", "it", "be", "or", "not", "you", "your", "all", "new", "more", "no", "yes", "back", "next",
        "previous", "home", "settings", "search", "open", "close", "cancel", "ok", "save", "delete", "edit",
        "share", "send", "message", "messages", "call", "calls", "contacts", "menu", "help", "about", "login",
        "log", "sign", "in", "up", "password", "username", "email", "notifications", "notification", "update",
        "updates", "download", "install", "uninstall", "camera", "photo", "photos", "gallery", "music", "video",
        "file", "files", "folder", "battery", "storage", "network", "language", "text", "image", "button",
        "page", "list", "item", "items", "select", "selected", "done", "continue", "skip", "loading", "error",
        "please", "try", "again", "settings", "account", "profile", "chat", "chats", "channel", "channels",
        "group", "groups", "name", "time", "date", "today", "yesterday", "tomorrow", "have", "has", "will",
        "can", "could", "would", "should", "what", "when", "where", "who", "why", "how", "which", "there", "here",
    )

    private val RU_WORDS: Set<String> = setOf(
        "\u0438", "\u0432", "\u043D\u0435", "\u043D\u0430", "\u0447\u0442\u043E", "\u044F", "\u0441", "\u043E\u043D",
        "\u043A\u0430\u043A", "\u044D\u0442\u043E", "\u043F\u043E", "\u043D\u043E", "\u043E\u043D\u0438", "\u043C\u044B",
        "\u0432\u044B", "\u0432\u0441\u0435", "\u043E\u043D\u0430", "\u0442\u0430\u043A", "\u0435\u0433\u043E",
        "\u0442\u043E\u043B\u044C\u043A\u043E", "\u0431\u044B\u043B\u043E", "\u0431\u044B", "\u043E\u0442", "\u0438\u0437",
        "\u0437\u0430", "\u0434\u043B\u044F", "\u0442\u043E", "\u0436\u0435", "\u0438\u043B\u0438", "\u0435\u0449\u0451",
        "\u043D\u0435\u0442", "\u0434\u0430", "\u0432\u043E\u0442", "\u0435\u0441\u043B\u0438", "\u0443\u0436\u0435",
        "\u043A\u043E\u0433\u0434\u0430", "\u043C\u043D\u0435", "\u0442\u044B", "\u0435\u0441\u0442\u044C",
        "\u043D\u0430\u0441\u0442\u0440\u043E\u0439\u043A\u0438", "\u043D\u0430\u0437\u0430\u0434",
        "\u043F\u043E\u0438\u0441\u043A", "\u043E\u0442\u043C\u0435\u043D\u0430", "\u0441\u043E\u0445\u0440\u0430\u043D\u0438\u0442\u044C",
        "\u0443\u0434\u0430\u043B\u0438\u0442\u044C", "\u0441\u043E\u043E\u0431\u0449\u0435\u043D\u0438\u0435",
        "\u0441\u043E\u043E\u0431\u0449\u0435\u043D\u0438\u044F", "\u043A\u043E\u043D\u0442\u0430\u043A\u0442\u044B",
        "\u043C\u0435\u043D\u044E", "\u043F\u043E\u043C\u043E\u0449\u044C", "\u0433\u043B\u0430\u0432\u043D\u0430\u044F",
    )

    /** Matn tarjima qilinishi kerakmi: o'zbekcha emas, raqam/belgi emas va maxfiy kod emas. */
    fun needsTranslation(text: String): Boolean {
        if (text.length < 2) return false
        if (SensitiveText.looksSensitive(text)) return false
        return when (detect(text)) {
            Lang.RU, Lang.EN, Lang.OTHER -> true
            else -> false
        }
    }

    fun detect(text: String): Lang {
        val t = text.trim()
        if (t.isEmpty() || URL_OR_MAIL.matches(t)) return Lang.NEUTRAL

        // Harflar yozuvlar bo'yicha sanaladi
        var latin = 0
        var cyr = 0
        var other = 0
        var i = 0
        while (i < t.length) {
            val cp = t.codePointAt(i)
            i += Character.charCount(cp)
            if (!Character.isLetter(cp)) continue
            when (Character.UnicodeScript.of(cp)) {
                Character.UnicodeScript.LATIN -> latin++
                Character.UnicodeScript.CYRILLIC -> cyr++
                else -> other++
            }
        }
        val letters = latin + cyr + other
        if (letters == 0) return Lang.NEUTRAL
        if (other * 2 > letters) return Lang.OTHER

        var uzLat = 0
        var uzCyr = 0
        var en = 0
        var ru = 0
        var unknown = 0
        val norm = UzScripts.unifyApostrophes(t).lowercase()
        for (m in WORD.findAll(norm)) {
            val w = m.value.trim('\'')
            if (w.length < 2) continue
            val script = wordScript(w)
            when (script) {
                1 -> { // lotin
                    if (UZ_LATIN_WORDS.contains(w)) uzLat += 2
                    else if (hasUzbekLatinCue(w)) uzLat += 1
                    if (EN_WORDS.contains(w)) en += 2
                    if (!UZ_LATIN_WORDS.contains(w) && !EN_WORDS.contains(w) && !hasUzbekLatinCue(w)) unknown++
                }
                2 -> { // kirill
                    if (w.any { UZ_ONLY.indexOf(it) >= 0 } || UZ_CYR_WORDS.contains(w)) uzCyr += 2
                    else if (w.any { RU_ONLY.indexOf(it) >= 0 } || RU_WORDS.contains(w)) ru += 2
                    else unknown++
                }
                else -> unknown++
            }
        }
        val uz = uzLat + uzCyr
        val foreign = en + ru
        if (uz > 0 && uz >= foreign && (uz >= 2 || unknown == 0)) {
            return if (uzCyr > uzLat) Lang.UZ_CYRL else Lang.UZ_LATN
        }
        if (ru > 0 && ru >= en) return Lang.RU
        if (en > 0) return Lang.EN
        if (uz > 0) return if (uzCyr > uzLat) Lang.UZ_CYRL else Lang.UZ_LATN
        return if (unknown > 0) Lang.OTHER else Lang.NEUTRAL
    }

    /** 1: lotin, 2: kirill, 0: boshqa yoki aralash. */
    private fun wordScript(w: String): Int {
        var lat = 0
        var cyr = 0
        for (c in w) {
            if (!c.isLetter()) continue
            when (Character.UnicodeScript.of(c.code)) {
                Character.UnicodeScript.LATIN -> lat++
                Character.UnicodeScript.CYRILLIC -> cyr++
                else -> return 0
            }
        }
        return when {
            lat > 0 && cyr == 0 -> 1
            cyr > 0 && lat == 0 -> 2
            else -> 0
        }
    }

    /** O'zbek lotin imlosining ishonchli belgilari: o' / g' va "q" harfi (inglizcha "qu" emas). */
    private fun hasUzbekLatinCue(w: String): Boolean {
        if (w.contains("o'") || w.contains("g'")) return true
        val q = w.indexOf('q')
        if (q >= 0 && (q == w.length - 1 || w[q + 1] != 'u')) return true
        return false
    }
}
