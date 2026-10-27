package uz.ekranoquvchi.mvp.content

/** "Quirks" registri (FR-22, 12.2): ishlab chiqaruvchiga xos muammolar va aylanma yo'llar, deklarativ ma'lumot sifatida. */
object Quirks {
    private val RULES: List<Pair<Regex, String>> = listOf(
        Regex("xiaomi|redmi|poco") to
            "Xiaomi, Redmi, POCO: ilovaning batareya rejimini «Cheklovsiz» qiling va avto-ishga tushirishga ruxsat bering, aks holda tizim xizmatni to'xtatib qo'yishi mumkin.",
        Regex("huawei|honor") to
            "Huawei va Honor: ilovani qo'lda boshqarishga o'tkazing (avto-ishga tushirish va fonda ishlash yoqilgan bo'lsin). Google xizmatlari bo'lmasa, o'zbek ovozi uchun RHVoice o'rnating.",
        Regex("oppo|realme|oneplus") to
            "OPPO, Realme, OnePlus: batareya optimizatsiyasini o'chiring va avto-ishga tushirishga ruxsat bering.",
        Regex("vivo|iqoo") to
            "vivo, iQOO: fonda ishlashga ruxsat bering va yuqori quvvat sarfi cheklovini o'chiring.",
        Regex("samsung") to
            "Samsung: batareya sozlamalarida O‘zbegimni uxlayotgan ilovalar ro'yxatiga qo'shmang. Samsungning o'z ekran o'quvchisi Voice Assistant deb ataladi.",
        Regex("tecno|infinix|itel|transsion") to
            "Tecno, Infinix, itel: telefon menejerida avto-ishga tushirishga ruxsat bering va batareya cheklovini o'chiring.",
    )

    fun hintsFor(vararg names: String): List<String> {
        val key = names.joinToString(" ").lowercase()
        return RULES.filter { it.first.containsMatchIn(key) }.map { it.second }
    }
}
