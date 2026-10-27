package uz.ekranoquvchi.mvp.content

/** Nutq dvigatellarining yozuv imkoniyatlari (monografiya 14.4: "TTS bilan moslash"). */
object EngineProfiles {
    const val RHVOICE = "com.github.olga_yakovleva.rhvoice.android"

    /** Onlayn Microsoft Azure ovozi (SpeechManager.effectiveEngine): kirill yozuvi lotinga o'tkaziladi. */
    const val AZURE = "azure"

    /**
     * Zaxira (tiklash) paytida ekran matni faqat shu ma'lum dvigatellarga yoki tizimning standart
     * dvigateliga beriladi. Boshqa (noma'lum, ehtimol bulutli) dvigatelga o'zboshimchalik bilan o'tilmaydi.
     */
    val TRUSTED = listOf(RHVOICE, "com.google.android.tts", "com.samsung.SMT")

    /**
     * Foydalanuvchi kuzatuvi: RHVoice o'zbek lotin va kirill yozuvini birdek o'qiydi.
     * Shuning uchun u bilan kirill matnga tegilmaydi; faqat uchinchi (yangi lotin) yozuv o'tkaziladi.
     * Noma'lum dvigatellar uchun faqat lotin kutiladi.
     */
    fun readsCyrillic(enginePackage: String?): Boolean = enginePackage == RHVOICE
}
