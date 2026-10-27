package uz.ekranoquvchi.mvp.core

import android.content.Context
import android.media.AudioManager
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Qo'ng'iroq boshqaruvi (FR-16): javob berish, tugatish, karnay. Qo'shimcha ruxsat so'ramaydi:
 * qo'ng'iroq ekranidagi tugmani nomi bo'yicha topib bosadi. Tugma nomlari ishlab chiqaruvchiga bog'liq,
 * shuning uchun topilmasa foydalanuvchiga aytiladi.
 *
 * Xavfsizlik: "Accept", "Reject", "Tugatish" kabi so'zlar boshqa ilovalarda ham bor (masalan, to'lovni
 * tasdiqlash, cookie rozilik oynasi). Shu sabab tugma faqat qo'ng'iroq oynasida qidiriladi: telefon
 * ilovalarida, ma'lum IP-telefoniya ilovalarida yoki oddiy qo'ng'iroq jiringlayotganda/davom
 * etayotganda tizim bildirishnoma oynasida. Boshqa ilovalardagi tugmalarga tegilmaydi.
 */
object CallActions {
    enum class Kind { ANSWER, END, SPEAKER }

    private val ANSWER = Regex("(?iu)(answer|accept|javob|qabul|ответ|принять)")
    private val END = Regex("(?iu)(end call|hang up|decline|reject|tugat|rad et|отбой|завершить|отклонить|сбросить)")
    private val SPEAKER = Regex("(?iu)(speaker|karnay|динамик|громк)")

    private const val SYSTEM_UI = "com.android.systemui"

    /** IP-telefoniya va video qo'ng'iroq ilovalari (qo'ng'iroq oynasi shu ilovaning o'zida). */
    private val VOIP_PACKAGES = setOf(
        "com.whatsapp", "com.whatsapp.w4b",
        "org.telegram.messenger", "org.telegram.messenger.web", "org.thunderdog.challegram",
        "com.viber.voip", "com.skype.raider", "com.google.android.apps.tachyon", "us.zoom.videomeetings",
    )

    /** Tizim telefon ilovalari: paket nomi ularda ishlab chiqaruvchiga qarab farq qiladi, shuning uchun nom bo'yicha. */
    fun isDialerPackage(pkg: String): Boolean =
        pkg.contains("incallui") || pkg.contains("dialer") || pkg.contains("telecom") ||
            pkg == "com.android.phone" || pkg == "com.android.contacts" || pkg == "com.huawei.contacts"

    /** Oddiy (uyali) qo'ng'iroq jiringlayapti yoki davom etyapti: bu rejimni faqat telefon xizmati o'rnatadi. */
    private fun cellularCallMode(context: Context): Boolean = try {
        val mode = (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).mode
        mode == AudioManager.MODE_RINGTONE || mode == AudioManager.MODE_IN_CALL
    } catch (_: Throwable) {
        false
    }

    /** Shu oyna qo'ng'iroq oynasi bo'lishi mumkinmi? */
    fun isCallWindow(pkg: String?, cellular: Boolean): Boolean {
        if (pkg == null) return false
        return isDialerPackage(pkg) || pkg in VOIP_PACKAGES || (pkg == SYSTEM_UI && cellular)
    }

    fun click(context: Context, roots: List<AccessibilityNodeInfo>, kind: Kind): Boolean {
        val re = when (kind) {
            Kind.ANSWER -> ANSWER
            Kind.END -> END
            Kind.SPEAKER -> SPEAKER
        }
        val cellular = cellularCallMode(context)
        for (root in roots) {
            if (!isCallWindow(root.packageName?.toString(), cellular)) continue
            val node = findFirst(root) { n -> matches(n, re) } ?: continue
            var x: AccessibilityNodeInfo? = node
            var guard = 0
            while (x != null && !x.isClickable && guard++ < 6) x = x.parent
            if (x != null && x.performAction(AccessibilityNodeInfo.ACTION_CLICK)) return true
        }
        return false
    }

    private fun matches(n: AccessibilityNodeInfo, re: Regex): Boolean {
        val t = n.text?.toString().orEmpty()
        val d = n.contentDescription?.toString().orEmpty()
        return (t.isNotEmpty() && re.containsMatchIn(t)) || (d.isNotEmpty() && re.containsMatchIn(d))
    }
}
