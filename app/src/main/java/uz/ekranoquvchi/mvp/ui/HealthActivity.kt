package uz.ekranoquvchi.mvp.ui

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import uz.ekranoquvchi.mvp.R
import uz.ekranoquvchi.mvp.content.EngineProfiles
import uz.ekranoquvchi.mvp.content.Quirks
import uz.ekranoquvchi.mvp.diag.CrashGuard
import uz.ekranoquvchi.mvp.diag.Diag
import uz.ekranoquvchi.mvp.diag.LatencyTracker
import uz.ekranoquvchi.mvp.net.AiClient
import uz.ekranoquvchi.mvp.util.Prefs
import uz.ekranoquvchi.mvp.util.Secrets
import uz.ekranoquvchi.mvp.util.SystemUtil
import java.util.Locale

/** "Tizim tekshiruvi": xizmat, tarmoq ruxsati, batareya, o'zbek ovozi, RHVoice va kechikish (p95). */
class HealthActivity : AppCompatActivity() {
    private lateinit var container: LinearLayout
    private var tts: TextToSpeech? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        title = getString(R.string.btn_health)
        container = findViewById(R.id.container)
        Diag.init(this)
        runLocalChecks()
        runTtsCheck()
    }

    override fun onDestroy() {
        try {
            tts?.shutdown()
        } catch (_: Throwable) {
        }
        super.onDestroy()
    }

    private fun addText(text: String) {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 18f
        tv.setPadding(0, 20, 0, 20)
        tv.isFocusable = true
        container.addView(tv)
    }

    private fun line(ok: Boolean, text: String) {
        addText(getString(if (ok) R.string.health_ok else R.string.health_problem) + ": " + text)
    }

    private fun button(textRes: Int, action: () -> Unit) {
        val b = Button(this)
        b.setText(textRes)
        b.setOnClickListener { action() }
        container.addView(b)
    }

    private fun runLocalChecks() {
        val enabled = SystemUtil.isServiceEnabled(this)
        line(enabled, getString(if (enabled) R.string.health_service_on else R.string.health_service_off))

        // Ikki, uch va to'rt barmoqli ishoralar Android 10 (API 29) dan boshlab bor
        val multiFinger = Build.VERSION.SDK_INT >= 29
        line(multiFinger, getString(if (multiFinger) R.string.health_multifinger_ok else R.string.health_multifinger_no))

        // Onlayn imkoniyatlar ixtiyoriy: o'chiq bo'lsa, ma'lumot qurilmadan chiqmaydi (monografiya 11.2)
        val onlineOn = Prefs.onlineEnabled(this)
        line(true, getString(if (onlineOn) R.string.health_online_on else R.string.health_online_off))
        if (onlineOn) {
            val azure = Secrets.has(this, Prefs.AZURE_KEY)
            line(azure, getString(if (azure) R.string.health_azure_key_yes else R.string.health_azure_key_no))
            val ai = AiClient.activeProvider(this) != null
            line(ai, getString(if (ai) R.string.health_ai_key_yes else R.string.health_ai_key_no))
        }

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val ignoring = pm.isIgnoringBatteryOptimizations(packageName)
        line(ignoring, getString(if (ignoring) R.string.health_battery_ok else R.string.health_battery_bad))
        if (!ignoring) button(R.string.btn_battery) { SystemUtil.openBatterySettings(this) }

        val count = LatencyTracker.count()
        if (count == 0) {
            line(true, getString(R.string.health_latency_none))
        } else {
            val p95 = LatencyTracker.percentile(95) ?: 0L
            line(p95 <= 150L, getString(R.string.health_latency, LatencyTracker.mean() ?: 0L, p95, count))
        }
        val crashes = CrashGuard.recentCount(this)
        line(crashes == 0, getString(R.string.health_crashes, crashes))

        // Qurilma hisoboti va ishlab chiqaruvchiga xos maslahatlar (FR-22)
        addText(getString(R.string.health_device, Build.VERSION.RELEASE, Build.VERSION.SDK_INT, "${Build.MANUFACTURER} ${Build.MODEL}"))
        for (hint in Quirks.hintsFor(Build.MANUFACTURER, Build.BRAND)) addText(hint)
        addText(getString(R.string.health_return_talkback))
        button(R.string.btn_enable) { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
    }

    private fun runTtsCheck() {
        tts = TextToSpeech(this) { status ->
            runOnUiThread {
                val t = tts
                if (status != TextToSpeech.SUCCESS || t == null) {
                    line(false, getString(R.string.health_tts_failed))
                    return@runOnUiThread
                }
                addText(getString(R.string.health_tts_engine, t.defaultEngine ?: "?"))
                when (t.isLanguageAvailable(Locale.forLanguageTag("uz-UZ"))) {
                    TextToSpeech.LANG_AVAILABLE,
                    TextToSpeech.LANG_COUNTRY_AVAILABLE,
                    TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE -> line(true, getString(R.string.health_tts_lang_ok))
                    TextToSpeech.LANG_MISSING_DATA -> {
                        line(false, getString(R.string.health_tts_lang_missing))
                        button(R.string.btn_install_voice) { SystemUtil.openInstallVoiceData(this) }
                    }
                    else -> line(false, getString(R.string.health_tts_lang_unsupported))
                }
                val rhvoice = t.engines.any { it.name == EngineProfiles.RHVOICE }
                addText(getString(if (rhvoice) R.string.health_rhvoice_yes else R.string.health_rhvoice_no))
                if (!rhvoice) button(R.string.btn_install_rhvoice) { SystemUtil.openStorePage(this, EngineProfiles.RHVOICE) }
                button(R.string.btn_tts_settings) { SystemUtil.openTtsSettings(this) }
            }
        }
    }
}
