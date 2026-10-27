package uz.ekranoquvchi.mvp.content

/** Tavsif tartibi (TalkBack: "Description order"): nom, tur va holatni qaysi ketma-ketlikda aytish. */
enum class DescOrder { NAME_TYPE_STATE, TYPE_NAME_STATE, STATE_NAME_TYPE }

/**
 * Tafsilotlar (TalkBack'dagi "Verbosity"): element tavsifiga nimalar qo'shilishi.
 * Standartda tugma va rasm turi aytilmaydi: faqat nomi o'qiladi ("Orqaga", "Orqaga, tugma" emas).
 * Tahrirlash maydoni, parol maydoni va o'tkazgich turi doim aytiladi: ularsiz element bilan ishlab bo'lmaydi.
 */
data class Details(
    /** Tugma va rasm bo'lmagan elementlarning turi: ro'yxat, sarlavha, havola, slayder va h.k. */
    val typeOther: Boolean = true,
    /** "tugma" va "rasm" so'zlarini aytish. */
    val typeButtonImage: Boolean = false,
    /** Ro'yxat va jadvaldagi o'rni: "10 tadan 3". */
    val position: Boolean = false,
    /** Foydalanish ko'rsatmalari: "faollashtirish uchun ikki marta bosing". */
    val hints: Boolean = false,
    val order: DescOrder = DescOrder.NAME_TYPE_STATE,
) {
    companion object {
        /** Past daraja: faqat nom va holat. */
        val LOW = Details(typeOther = false, typeButtonImage = false, position = false, hints = false)

        /** O'rta daraja (standart): nom, tugma/rasmdan boshqa elementlarning turi va holat. */
        val MEDIUM = Details(typeOther = true, typeButtonImage = false, position = false, hints = false)

        /** Yuqori daraja: hamma narsa, ko'rsatmalar ham. */
        val HIGH = Details(typeOther = true, typeButtonImage = true, position = true, hints = true)

        fun forPreset(v: Verbosity): Details = when (v) {
            Verbosity.SHORT -> LOW
            Verbosity.NORMAL -> MEDIUM
            Verbosity.FULL -> HIGH
        }
    }
}
