package uz.ekranoquvchi.mvp.util

import android.content.Context
import uz.ekranoquvchi.mvp.content.BridgeDictionary
import uz.ekranoquvchi.mvp.content.LabelStore
import uz.ekranoquvchi.mvp.content.ProfileStore
import java.io.File

/**
 * Foydalanuvchi ma'lumot fayllari (oddiy matn; qurilma-himoyalangan xotirada, qulf ochilmasdan ham o'qiladi).
 * Faqat uchta ma'lum fayl nomi qabul qilinadi (yo'l almashtirish hujumidan himoya), hajm chegaralangan,
 * yozish esa vaqtinchalik fayl orqali bajariladi: qurilma o'chib qolsa ham fayl yarim yozilgan holda qolmaydi.
 */
object UserFiles {
    const val DICTIONARY = "user_dictionary.txt"
    const val LABELS = "labels.txt"
    const val PROFILES = "profiles.txt"

    /** Bitta fayldagi eng ko'p belgi soni. Tahrirlagich ham, import ham shu chegarani tekshiradi. */
    const val MAX_CHARS = 200_000

    private val NAMES = setOf(DICTIONARY, LABELS, PROFILES)

    fun isKnown(name: String): Boolean = name in NAMES

    private fun dir(context: Context): File = context.createDeviceProtectedStorageContext().filesDir

    /** Fayl mazmuni; bo'lmasa yoki o'qib bo'lmasa bo'sh qator. MAX_CHARS dan uzun qismi o'qilmaydi. */
    fun read(context: Context, name: String): String {
        if (name !in NAMES) return ""
        return try {
            val f = File(dir(context), name)
            if (f.exists()) readLimited(f) else ""
        } catch (_: Throwable) {
            ""
        }
    }

    private fun readLimited(f: File): String {
        val sb = StringBuilder()
        f.bufferedReader().use { r ->
            val buf = CharArray(4096)
            while (sb.length < MAX_CHARS) {
                val n = r.read(buf, 0, minOf(buf.size, MAX_CHARS - sb.length))
                if (n < 0) break
                sb.append(buf, 0, n)
            }
        }
        return sb.toString()
    }

    /** @return saqlandimi. Noma'lum nom yoki juda katta matn rad etiladi. */
    fun write(context: Context, name: String, text: String): Boolean {
        if (name !in NAMES || text.length > MAX_CHARS) return false
        return try {
            val target = File(dir(context), name)
            val tmp = File(dir(context), "$name.tmp")
            tmp.writeText(text)
            if (!tmp.renameTo(target)) {
                target.writeText(text)
                tmp.delete()
            }
            true
        } catch (_: Throwable) {
            false
        }
    }

    fun reloadAll(context: Context) {
        BridgeDictionary.setUserEntries(read(context, DICTIONARY))
        LabelStore.load(read(context, LABELS))
        ProfileStore.load(read(context, PROFILES))
    }
}
