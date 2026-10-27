package uz.ekranoquvchi.mvp.core

import android.content.Context
import uz.ekranoquvchi.mvp.util.Prefs

/**
 * Javob tezligi (tezlashtirish): harakatlanish va javob berishdagi sekinlikni kamaytiradigan kutish vaqtlari.
 * "normal" (oldingi qiymatlar), "fast" (standart), "turbo" (eng tezkor).
 */
class SpeedProfile(
    /** Oyna hodisalari ketma-ket kelganda faqat oxirgisi uchun ishlash kutish vaqti (ms). */
    val windowDebounceMs: Long,
    /** Mazmun o'zgarishi hodisalarini qayta ishlash orasidagi eng kichik oraliq (ms). */
    val contentGapMs: Long,
    /** Ro'yxatni aylantirgandan keyin yangi elementga fokus berishdan oldingi kutish (ms). */
    val scrollWaitMs: Long,
    /** Uzun matnni gap bo'laklariga ajratish: birinchi bo'lak qancha qisqa bo'lsa, ovoz shuncha tez boshlanadi. */
    val chunkSoft: Int,
    val chunkHard: Int,
    /** Avtomatik tarjimani kutishning eng uzun vaqti (ms): shundan keyin asl matn o'qiladi. */
    val translateWaitMs: Long,
    /** Yengil tavsif: yorlig'i yo'q konteynerning ichki matnlarini kamroq yig'adi. */
    val light: Boolean,
) {
    companion object {
        val NORMAL = SpeedProfile(250L, 300L, 450L, 160, 260, 1600L, false)
        val FAST = SpeedProfile(120L, 450L, 280L, 110, 170, 1200L, false)
        val TURBO = SpeedProfile(60L, 700L, 200L, 80, 130, 900L, true)

        fun of(level: String): SpeedProfile = when (level) {
            "normal" -> NORMAL
            "turbo" -> TURBO
            else -> FAST
        }

        fun current(context: Context): SpeedProfile = of(Prefs.speedLevel(context))
    }
}
