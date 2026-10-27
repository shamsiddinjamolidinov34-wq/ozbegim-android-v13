package uz.ekranoquvchi.mvp.ui

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import uz.ekranoquvchi.mvp.R
import uz.ekranoquvchi.mvp.net.AiClient
import uz.ekranoquvchi.mvp.util.Prefs

/**
 * Avtomatik tarjimani bitta tugma bilan yoqish va o'chirish (bosh ekran va sozlamalar uchun).
 * Yoqishda: sun'iy intellekt kaliti bo'lmasa, kalit kiritish ekrani taklif qilinadi; onlayn imkoniyatlar yoqilmagan
 * bo'lsa, ekran matni tashqi xizmatga yuborilishi aytilib, bir marta rozilik olinadi. Yoqilgandan keyin hech narsa
 * so'ralmaydi: tarjima hamma joyda o'zi ishlaydi.
 */
object AutoTranslateUi {
    fun isOn(context: Context): Boolean = Prefs.autoTranslate(context)

    fun turnOff(context: Context) {
        Prefs.get(context).edit().putBoolean(Prefs.AUTO_TRANSLATE, false).apply()
    }

    /** Yoqishga urinadi. Natija (yoqildi yoki yo'q) onDone orqali asosiy oqimda xabar qilinadi. */
    fun requestEnable(context: Context, onDone: (Boolean) -> Unit) {
        if (AiClient.activeProvider(context) == null) {
            AlertDialog.Builder(context)
                .setTitle(R.string.auto_tr_dialog_title)
                .setMessage(R.string.auto_tr_need_key)
                .setPositiveButton(R.string.auto_tr_open_ai) { _, _ ->
                    context.startActivity(Intent(context, AiSettingsActivity::class.java))
                    onDone(false)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> onDone(false) }
                .setOnCancelListener { onDone(false) }
                .show()
            return
        }
        if (!Prefs.onlineEnabled(context)) {
            AlertDialog.Builder(context)
                .setTitle(R.string.auto_tr_dialog_title)
                .setMessage(R.string.auto_tr_consent_message)
                .setPositiveButton(R.string.auto_tr_consent_yes) { _, _ ->
                    enable(context)
                    onDone(true)
                }
                .setNegativeButton(android.R.string.cancel) { _, _ -> onDone(false) }
                .setOnCancelListener { onDone(false) }
                .show()
            return
        }
        enable(context)
        onDone(true)
    }

    private fun enable(context: Context) {
        Prefs.get(context).edit()
            .putBoolean(Prefs.ONLINE, true)
            .putBoolean(Prefs.AUTO_TRANSLATE, true)
            .apply()
    }
}
