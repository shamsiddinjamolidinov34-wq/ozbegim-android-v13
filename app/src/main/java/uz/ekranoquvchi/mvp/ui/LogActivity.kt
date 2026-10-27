package uz.ekranoquvchi.mvp.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import uz.ekranoquvchi.mvp.R
import uz.ekranoquvchi.mvp.diag.Diag
import uz.ekranoquvchi.mvp.diag.LatencyTracker
import uz.ekranoquvchi.mvp.diag.LogEntry
import uz.ekranoquvchi.mvp.util.Contact
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Xatolar jurnali: har yozuv bitta gap bilan o'qiladi. Tugmalar ro'yxatdan oldin turadi. */
class LogActivity : AppCompatActivity() {
    private lateinit var container: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        title = getString(R.string.log_title)
        container = findViewById(R.id.container)
        Diag.init(this)
        render()
    }

    private fun addText(text: String) {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = 17f
        tv.setPadding(0, 16, 0, 16)
        tv.isFocusable = true
        container.addView(tv)
    }

    private fun addButton(textRes: Int, action: () -> Unit) {
        val b = Button(this)
        b.setText(textRes)
        b.setOnClickListener { action() }
        container.addView(b)
    }

    private fun render() {
        container.removeAllViews()
        addText(getString(R.string.log_privacy))
        val n = LatencyTracker.count()
        if (n > 0) {
            addText(
                getString(
                    R.string.log_latency_header,
                    LatencyTracker.percentile(50) ?: 0L,
                    LatencyTracker.percentile(95) ?: 0L,
                    n
                )
            )
        }
        addButton(R.string.btn_share_log) { share() }
        addButton(R.string.btn_clear_log) {
            Diag.log.clear()
            render()
        }
        val entries = Diag.log.readAll()
        if (entries.isEmpty()) addText(getString(R.string.log_empty))
        val fmt = SimpleDateFormat("dd.MM HH:mm:ss", Locale.getDefault())
        for (e in entries) addText("${fmt.format(Date(e.timeMs))}, ${e.severity}: ${describe(e)}")
    }

    private fun describe(e: LogEntry): String = when (e.code) {
        "SILENCE_NO_AUDIO" -> getString(R.string.log_silence_no_audio)
        "SILENCE_RECOVERED" -> getString(R.string.log_silence_recovered)
        "TTS_INIT_FAILED" -> getString(R.string.log_tts_init_failed)
        "TTS_LANG_MISSING" -> getString(R.string.log_tts_lang_missing)
        "TTS_ENGINE_SWITCHED" -> getString(R.string.log_tts_engine_switched)
        "TTS_FALLBACK_SIGNAL" -> getString(R.string.log_tts_fallback_signal)
        "TTS_ENGINE_ERROR" -> getString(R.string.log_tts_engine_error)
        "LATENCY_BUDGET" -> getString(R.string.log_latency_budget, e.params["ms"] ?: "?")
        "SLOW_HANDLER" -> getString(R.string.log_slow_handler, e.params["kind"] ?: "?", e.params["what"] ?: "?", e.params["ms"] ?: "?")
        "FOCUS_NODE_FAILED" -> getString(R.string.log_focus_node_failed)
        "LANG_BRIDGE_FAILED" -> getString(R.string.log_bridge_failed)
        "CRASH_UNCAUGHT" -> getString(R.string.log_crash_uncaught)
        "USER_MARKED" -> getString(R.string.log_user_marked)
        "SAFE_MODE_ENTERED" -> getString(R.string.log_safe_mode)
        "AUDIO_FOCUS_LOST" -> getString(R.string.log_audio_focus_lost)
        "ONLINE_FAILED" -> getString(R.string.log_online_failed, e.params["why"] ?: "?")
        "AI_FAILED" -> getString(R.string.log_ai_failed, e.params["what"] ?: "?")
        else -> getString(R.string.log_unknown)
    }

    /** Tizim ulashish oynasi orqali: foydalanuvchi o'zi tanlaydi va tasdiqlaydi. */
    private fun share() {
        try {
            val file = Diag.log.exportText(cacheDir)
            val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_EMAIL, arrayOf(Contact.EMAIL))
                putExtra(Intent.EXTRA_SUBJECT, getString(R.string.log_mail_subject))
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(send, getString(R.string.btn_share_log)))
        } catch (_: Throwable) {
        }
    }
}
