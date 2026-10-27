package uz.ekranoquvchi.mvp.util

import java.io.File

/**
 * Ulashish uchun vaqtinchalik fayllar (cache/shared): zaxira nusxa va xatolar jurnali.
 * Fayllar ulashilgandan keyin ham keshda qolib ketmasligi uchun, eskirganlari (1 soatdan eski)
 * papka har ochilganda o'chiriladi.
 */
object SharedFiles {
    private const val MAX_AGE_MS = 60 * 60_000L

    /** Papkani yaratadi, eskirgan fayllarni o'chiradi va papkani qaytaradi. */
    fun dir(cacheDir: File): File {
        val d = File(cacheDir, "shared")
        d.mkdirs()
        val now = System.currentTimeMillis()
        d.listFiles()?.forEach { f ->
            if (f.isFile && now - f.lastModified() > MAX_AGE_MS) f.delete()
        }
        return d
    }
}
