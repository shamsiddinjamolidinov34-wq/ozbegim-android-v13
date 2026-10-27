package uz.ekranoquvchi.mvp.ui

import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import uz.ekranoquvchi.mvp.R
import uz.ekranoquvchi.mvp.content.ContentPipeline
import uz.ekranoquvchi.mvp.content.EngineProfiles
import uz.ekranoquvchi.mvp.diag.Diag
import uz.ekranoquvchi.mvp.util.Prefs
import uz.ekranoquvchi.mvp.util.SystemUtil

/**
 * Ovoz tanlash: o'zbek tilidagi ovozlar (masalan, RHVoice'dagi Islom, Dilnavoz, Sevinch) ro'yxati.
 * Ovozlar ilovaga qo'shilmagan (litsenziya: CC BY-NC-SA 4.0); ular RHVoice orqali o'rnatiladi.
 */
class VoicesActivity : AppCompatActivity() {
    private lateinit var container: LinearLayout
    private var tts: TextToSpeech? = null
    private var engineName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        title = getString(R.string.btn_voices)
        container = findViewById(R.id.container)
        Diag.init(this)

        addText(getString(R.string.voices_intro))
        addText(getString(R.string.voices_license))
        addButton(getString(R.string.btn_install_rhvoice)) { SystemUtil.openStorePage(this, EngineProfiles.RHVOICE) }
        addButton(getString(R.string.btn_open_rhvoice)) { launchRhvoice() }
        addButton(getString(R.string.btn_tts_settings)) { SystemUtil.openTtsSettings(this) }
        addButton(getString(R.string.btn_default_voice)) { saveVoice("", null) }
        addAzureSection()
        startTts()
    }

    /** Microsoft Azure ovozlari (onlayn): Madina, Sardor, shuningdek ruscha va inglizcha. Tanlansa, dvigatel "azure" bo'ladi. */
    private fun addAzureSection() {
        addText(getString(R.string.azure_voices_intro))
        val labels = resources.getStringArray(R.array.azure_voice_entries)
        val ids = resources.getStringArray(R.array.azure_voice_values)
        for (i in ids.indices) {
            val id = ids[i]
            val label = if (i < labels.size) labels[i] else id
            addButton(getString(R.string.voice_choose, label)) { saveAzureVoice(id, label) }
        }
        addButton(getString(R.string.btn_back_to_local)) { backToLocal() }
    }

    private fun saveAzureVoice(id: String, label: String) {
        Prefs.get(this).edit()
            .putString(Prefs.AZURE_VOICE, id)
            .putString(Prefs.ENGINE, EngineProfiles.AZURE)
            .apply()
        Toast.makeText(this, getString(R.string.azure_voice_saved, label), Toast.LENGTH_LONG).show()
    }

    private fun backToLocal() {
        Prefs.get(this).edit().putString(Prefs.ENGINE, "auto").apply()
        Toast.makeText(this, R.string.local_voice_restored, Toast.LENGTH_SHORT).show()
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
        tv.textSize = 17f
        tv.setPadding(0, 16, 0, 16)
        tv.isFocusable = true
        container.addView(tv)
    }

    private fun addButton(text: String, action: () -> Unit) {
        val b = Button(this)
        b.text = text
        b.setOnClickListener { action() }
        container.addView(b)
    }

    private fun launchRhvoice() {
        val intent = packageManager.getLaunchIntentForPackage(EngineProfiles.RHVOICE)
        if (intent == null) {
            Toast.makeText(this, R.string.rhvoice_missing, Toast.LENGTH_LONG).show()
        } else {
            startActivity(intent)
        }
    }

    private fun startTts() {
        val engine = if (Prefs.engine(this) == "rhvoice" && SystemUtil.isInstalled(this, EngineProfiles.RHVOICE)) {
            EngineProfiles.RHVOICE
        } else {
            null
        }
        engineName = engine
        val listener = TextToSpeech.OnInitListener { status -> runOnUiThread { showVoices(status) } }
        tts = if (engine == null) TextToSpeech(this, listener) else TextToSpeech(this, listener, engine)
    }

    private fun showVoices(status: Int) {
        val t = tts
        if (status != TextToSpeech.SUCCESS || t == null) {
            addText(getString(R.string.health_tts_failed))
            return
        }
        if (engineName == null) engineName = t.defaultEngine
        val all: List<Voice> = try {
            t.voices?.toList().orEmpty()
        } catch (_: Throwable) {
            emptyList()
        }
        val uz = all.filter { it.locale.language == "uz" || it.locale.language == "uzb" }.sortedBy { it.name }
        if (uz.isEmpty()) addText(getString(R.string.voices_none))
        for (v in uz) {
            addText(v.name)
            addButton(getString(R.string.voice_listen, v.name)) { listen(v) }
            addButton(getString(R.string.voice_choose, v.name)) { saveVoice(v.name, engineName) }
        }
    }

    private fun listen(v: Voice) {
        val t = tts ?: return
        try {
            t.setVoice(v)
            val opts = Prefs.pipelineOptions(this, EngineProfiles.readsCyrillic(engineName))
            val sample = ContentPipeline.process(getString(R.string.voice_sample), opts).text
            t.speak(sample, TextToSpeech.QUEUE_FLUSH, null, "sample")
        } catch (_: Throwable) {
        }
    }

    private fun saveVoice(name: String, engine: String?) {
        val e = Prefs.get(this).edit().putString(Prefs.VOICE, name)
        if (name.isNotEmpty()) {
            // Ilgari "rhvoice" qolib ketsa, tizim dvigatelidan tanlangan ovoz hech qachon qo'llanmasdi
            e.putString(Prefs.ENGINE, if (engine == EngineProfiles.RHVOICE) "rhvoice" else "system")
        }
        e.apply()
        Toast.makeText(this, R.string.voice_saved, Toast.LENGTH_SHORT).show()
    }
}
