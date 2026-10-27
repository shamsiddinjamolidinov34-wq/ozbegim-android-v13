package uz.ekranoquvchi.mvp.core

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.os.Handler
import android.view.accessibility.AccessibilityNodeInfo
import uz.ekranoquvchi.mvp.diag.Diag
import uz.ekranoquvchi.mvp.diag.ErrorCode

/**
 * Tizim sirtlari: ilovalar menyusi va barmoq bilan surish.
 *
 * Muammo: xizmat teginib o'rganish rejimida ishlaydi, ya'ni ekrandagi barcha barmoq harakatini tizim ushlab oladi.
 * Xizmatning o'zi dispatchGesture bilan "surish" yuborsa, u ham shu ushlagichdan o'tishi va bosh ekranga (launcher)
 * yetib bormasligi mumkin. Shuning uchun ilovalar menyusi bir necha yo'l bilan, har birini tekshirib ochiladi:
 *  1) rasmiy tizim amali GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS (Android 12 va yuqori);
 *  2) bosh ekrandagi "Ilovalar" tugmasini topib bosish;
 *  3) oxirgi chora: teginib o'rganishni qisqa muddatga o'chirib, ekranga barmoq bilan surishni yuborish.
 * Har qadamdan keyin ekran haqiqatan o'zgarganmi tekshiriladi; o'zgarmasa keyingi yo'lga o'tiladi.
 * Qaysi yo'l ishlagani xatolar jurnaliga yoziladi: telefonda ishlamasa, jurnal sababini ko'rsatadi.
 */
class SystemSurfaces(
    private val svc: AccessibilityService,
    private val main: Handler,
    /** Teginib o'rganish rejimini yoqadi yoki o'chiradi (ReaderService.setTouchExploration). */
    private val setTouchExploration: (Boolean) -> Unit,
    /** Foydalanuvchi xizmatni to'xtatib turgan bo'lsa, teginib o'rganish qayta yoqilmaydi. */
    private val isSuspended: () -> Boolean,
) {
    private class Step(val name: String, val action: () -> Boolean)

    private var busyUntil = 0L
    private var teSuspendedByUs = false
    private val restoreTask = Runnable { restoreTouchExploration() }

    /**
     * Ilovalar menyusini ochadi. onDone(true): ekran o'zgardi (menyu ochildi), onDone(false): hech bir yo'l ishlamadi.
     * Ikki marta ketma-ket chaqirilsa, ikkinchisi e'tiborga olinmaydi.
     */
    fun openApps(onDone: (Boolean) -> Unit = {}) {
        val now = android.os.SystemClock.uptimeMillis()
        if (now < busyUntil) return
        busyUntil = now + 4000L
        val before = screenSignature()
        val steps = ArrayList<Step>(3)
        if (Build.VERSION.SDK_INT >= 31) {
            steps.add(Step("global") { svc.performGlobalAction(AccessibilityService.GLOBAL_ACTION_ACCESSIBILITY_ALL_APPS) })
        }
        steps.add(Step("button") { clickAppsButton() })
        steps.add(Step("swipe") { injectSwipe(0.5f, 0.74f, 0.5f, 0.26f, 220L) })
        runStep(steps, 0, before, onDone)
    }

    private fun runStep(steps: List<Step>, index: Int, before: Int, onDone: (Boolean) -> Unit) {
        if (index >= steps.size) {
            busyUntil = 0L
            log("apps_failed", mapOf("steps" to steps.size))
            onDone(false)
            return
        }
        val step = steps[index]
        val started = try {
            step.action()
        } catch (_: Throwable) {
            false
        }
        if (!started) {
            log("apps_step_rejected", mapOf("how" to step.name))
            runStep(steps, index + 1, before, onDone)
            return
        }
        val wait = if (step.name == "swipe") SWIPE_VERIFY_MS else VERIFY_MS
        main.postDelayed({
            if (screenSignature() != before) {
                busyUntil = 0L
                log("apps_ok", mapOf("how" to step.name))
                onDone(true)
            } else {
                log("apps_step_no_effect", mapOf("how" to step.name))
                runStep(steps, index + 1, before, onDone)
            }
        }, wait)
    }

    /** Bosh ekrandagi "Ilovalar" tugmasini (id yoki yorlig'i bo'yicha) topib bosadi. */
    private fun clickAppsButton(): Boolean {
        val root = rootOrNull() ?: return false
        var visited = 0
        var target: AccessibilityNodeInfo? = null
        fun walk(n: AccessibilityNodeInfo) {
            if (target != null || ++visited > BUTTON_SCAN_NODES || !n.isVisibleToUser) return
            if (n.isClickable && looksLikeAppsButton(n)) {
                target = n
                return
            }
            for (i in 0 until n.childCount) {
                val c = n.getChild(i) ?: continue
                walk(c)
                if (target != null) return
            }
        }
        walk(root)
        val t = target ?: return false
        return t.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun looksLikeAppsButton(n: AccessibilityNodeInfo): Boolean {
        val id = n.viewIdResourceName?.lowercase().orEmpty()
        if (id.isNotEmpty() && APPS_ID_PARTS.any { id.contains(it) }) return true
        val label = (n.contentDescription ?: n.text)?.toString()?.trim()?.lowercase().orEmpty()
        return label.isNotEmpty() && label in APPS_LABELS
    }

    /**
     * Ekranga barmoq bilan surish yuboradi (koordinatalar ekran o'lchamining ulushi: 0..1).
     * Teginib o'rganish yoqiq bo'lsa, tizim yuborilgan surishni ham o'zi ushlab olmasligi uchun uni qisqa muddatga
     * o'chiramiz va surish tugagach (yoki xavfsizlik muddati o'tgach) qayta yoqamiz. true: yuborish rejalashtirildi.
     */
    fun injectSwipe(x1: Float, y1: Float, x2: Float, y2: Float, durationMs: Long): Boolean {
        val dm = svc.resources.displayMetrics
        val w = dm.widthPixels.toFloat()
        val h = dm.heightPixels.toFloat()
        val path = Path()
        path.moveTo(w * x1, h * y1)
        path.lineTo(w * x2, h * y2)
        val desc: GestureDescription = try {
            GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0L, durationMs))
                .build()
        } catch (_: Throwable) {
            return false
        }
        val teWasOn = touchExplorationOn()
        val settle = if (teWasOn) TE_SETTLE_MS else 0L
        if (teWasOn) {
            teSuspendedByUs = true
            setTouchExploration(false)
        }
        main.removeCallbacks(restoreTask)
        main.postDelayed(restoreTask, settle + durationMs + RESTORE_MARGIN_MS)
        main.postDelayed({
            val accepted = try {
                svc.dispatchGesture(
                    desc,
                    object : AccessibilityService.GestureResultCallback() {
                        override fun onCompleted(gestureDescription: GestureDescription?) {
                            restoreTouchExploration()
                        }

                        override fun onCancelled(gestureDescription: GestureDescription?) {
                            restoreTouchExploration()
                        }
                    },
                    main,
                )
            } catch (_: Throwable) {
                false
            }
            if (!accepted) restoreTouchExploration()
        }, settle)
        return true
    }

    private fun touchExplorationOn(): Boolean {
        val flags = svc.serviceInfo?.flags ?: return false
        return flags and AccessibilityServiceInfo.FLAG_REQUEST_TOUCH_EXPLORATION_MODE != 0
    }

    private fun restoreTouchExploration() {
        main.removeCallbacks(restoreTask)
        if (!teSuspendedByUs) return
        teSuspendedByUs = false
        if (!isSuspended()) setTouchExploration(true)
    }

    /** Xizmat o'chirilganda: kutayotgan ishlarni bekor qiladi. */
    fun release() {
        main.removeCallbacks(restoreTask)
        busyUntil = 0L
        teSuspendedByUs = false
    }

    private fun rootOrNull(): AccessibilityNodeInfo? = try {
        svc.rootInActiveWindow
    } catch (_: Throwable) {
        null
    }

    /**
     * Faol oynaning \"barmoq izi\": paket, oyna raqami va birinchi ko'rinadigan tugunlarning sinfi, matni.
     * Ilovalar menyusi ochilsa, bosh ekran daraxti butunlay almashadi va izi o'zgaradi.
     */
    private fun screenSignature(): Int {
        val root = rootOrNull() ?: return 0
        var h = 17
        h = h * 31 + (root.packageName?.toString()?.hashCode() ?: 0)
        h = h * 31 + root.windowId
        var visited = 0
        fun walk(n: AccessibilityNodeInfo) {
            if (visited >= SIGNATURE_NODES || !n.isVisibleToUser) return
            visited++
            h = h * 31 + (n.className?.toString()?.hashCode() ?: 0)
            h = h * 31 + (n.text?.toString()?.hashCode() ?: 0)
            h = h * 31 + (n.contentDescription?.toString()?.hashCode() ?: 0)
            for (i in 0 until n.childCount) {
                val c = n.getChild(i) ?: continue
                walk(c)
            }
        }
        walk(root)
        return h
    }

    private fun log(what: String, extra: Map<String, Any> = emptyMap()) {
        try {
            Diag.log.log(ErrorCode.GESTURE_PATH, mapOf<String, Any>("what" to what) + extra)
        } catch (_: Throwable) {
        }
    }

    private companion object {
        const val VERIFY_MS = 700L
        const val SWIPE_VERIFY_MS = 1400L
        const val TE_SETTLE_MS = 250L
        const val RESTORE_MARGIN_MS = 1500L
        const val SIGNATURE_NODES = 80
        const val BUTTON_SCAN_NODES = 600

        val APPS_ID_PARTS = listOf(
            "all_apps", "allapps", "apps_button", "app_drawer", "appdrawer", "home_allappsicon", "apps_view_icon",
        )
        val APPS_LABELS = setOf(
            "apps", "all apps", "app drawer", "apps list", "apps screen",
            "ilovalar", "barcha ilovalar", "ilovalar ro'yxati", "ilovalar ekrani",
            "приложения", "все приложения", "список приложений",
        )
    }
}
