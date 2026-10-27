package uz.ekranoquvchi.mvp.util

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * API kalitlari uchun xotira. Kalit Android Keystore'dagi AES-GCM kaliti bilan shifrlanib saqlanadi;
 * sozlamalar fayliga ochiq matn tushmaydi. Eksport va importga kalitlar kirmaydi (Prefs.sanitize ularni tanimaydi).
 * Shifrlab bo'lmasa (qurilma Keystore'si ishlamasa), kalit saqlanmaydi: ochiq matn zaxirasi yo'q.
 */
object Secrets {
    private const val ALIAS = "ozbegim_secret_v1"
    private const val PREFIX = "sec_"
    private const val IV_BYTES = 12

    private val cache = ConcurrentHashMap<String, String>()

    fun isSecret(name: String): Boolean =
        name == Prefs.AZURE_KEY || name == Prefs.AI_KEY || name == Prefs.GEMINI_KEY ||
            name == Prefs.OPENAI_KEY || name == Prefs.CUSTOM_KEY

    /** Kalit qiymati; yo'q yoki ochib bo'lmasa null. */
    fun get(context: Context, name: String): String? {
        val hit = cache[name]
        if (hit != null) return hit
        val enc = Prefs.string(context, PREFIX + name) ?: return null
        val plain = decrypt(enc) ?: return null
        cache[name] = plain
        return plain
    }

    fun has(context: Context, name: String): Boolean = !get(context, name).isNullOrEmpty()

    /** Bo'sh qiymat kalitni o'chiradi. @return saqlandimi (o'chirish doim true). */
    fun put(context: Context, name: String, value: String?): Boolean {
        cache.remove(name)
        val v = value?.trim().orEmpty()
        val edit = Prefs.get(context).edit()
        if (v.isEmpty()) {
            edit.remove(PREFIX + name).apply()
            return true
        }
        val enc = try {
            encrypt(v)
        } catch (_: Throwable) {
            return false
        }
        edit.putString(PREFIX + name, enc).apply()
        cache[name] = v
        return true
    }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore")
        ks.load(null)
        val existing = ks.getKey(ALIAS, null)
        if (existing is SecretKey) return existing
        val gen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        gen.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return gen.generateKey()
    }

    private fun encrypt(plain: String): String {
        val c = Cipher.getInstance("AES/GCM/NoPadding")
        c.init(Cipher.ENCRYPT_MODE, key())
        val iv = c.iv
        val body = c.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(iv + body, Base64.NO_WRAP)
    }

    private fun decrypt(enc: String): String? = try {
        val raw = Base64.decode(enc, Base64.NO_WRAP)
        if (raw.size <= IV_BYTES) {
            null
        } else {
            val c = Cipher.getInstance("AES/GCM/NoPadding")
            c.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, raw, 0, IV_BYTES))
            String(c.doFinal(raw, IV_BYTES, raw.size - IV_BYTES), Charsets.UTF_8)
        }
    } catch (_: Throwable) {
        null
    }
}
