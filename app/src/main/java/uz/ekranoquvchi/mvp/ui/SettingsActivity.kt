package uz.ekranoquvchi.mvp.ui

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.Preference
import androidx.preference.PreferenceDataStore
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import uz.ekranoquvchi.mvp.R
import uz.ekranoquvchi.mvp.util.Contact
import uz.ekranoquvchi.mvp.util.Prefs
import uz.ekranoquvchi.mvp.util.Secrets
import uz.ekranoquvchi.mvp.util.SystemUtil

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.btn_settings)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, SettingsFragment())
                .commit()
        }
    }
}

class SettingsFragment : PreferenceFragmentCompat() {
    /** Qisqa matnlarni (hozirgi qiymat) yangilovchilar: ekranga qaytganda ham yangilanadi. */
    private val refreshers = mutableListOf<() -> Unit>()

    override fun onResume() {
        super.onResume()
        for (r in refreshers) r()
    }

    /**
     * Nutq tezligi va toni: aniq son yoziladi yoki 1 foizlik qadam bilan o'zgartiriladi (ilgari surgich edi: ekran
     * o'quvchi bilan u 5 foizdan kichik qadam bermas edi).
     */
    private fun bindNumber(openKey: String, valueKey: String, range: IntRange, titleRes: Int, read: (Context) -> Int) {
        val pref = findPreference<Preference>(openKey) ?: return
        val refresh = {
            pref.summary = getString(R.string.pref_value_summary, read(requireContext()))
        }
        refresh()
        refreshers.add(refresh)
        pref.setOnPreferenceClickListener {
            NumberDialog.show(requireContext(), titleRes, valueKey, range, 100) { refresh() }
            true
        }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Sozlamalar xizmat bilan bir xil (qurilma-himoyalangan) faylga yoziladi
        preferenceManager.preferenceDataStore =
            SharedPrefsStore(requireContext().applicationContext, Prefs.get(requireContext()))
        setPreferencesFromResource(R.xml.preferences, rootKey)
        refreshers.clear()
        bindNumber(Prefs.OPEN_RATE, Prefs.RATE, Prefs.RATE_RANGE, R.string.pref_rate) { Prefs.rate(it) }
        bindNumber(Prefs.OPEN_PITCH, Prefs.PITCH, Prefs.PITCH_RANGE, R.string.pref_pitch) { Prefs.pitch(it) }
        findPreference<Preference>(Prefs.OPEN_TTS)?.setOnPreferenceClickListener {
            SystemUtil.openTtsSettings(requireContext())
            true
        }
        findPreference<Preference>(Prefs.OPEN_VOICES)?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), VoicesActivity::class.java))
            true
        }
        findPreference<Preference>(Prefs.OPEN_GESTURES)?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), GesturesActivity::class.java))
            true
        }
        findPreference<Preference>(Prefs.OPEN_DETAILS)?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), DetailsActivity::class.java))
            true
        }
        // Avtomatik tarjima: bitta tugma. Yoqishda kalit va rozilik tekshiriladi (AutoTranslateUi)
        findPreference<SwitchPreferenceCompat>(Prefs.AUTO_TRANSLATE)?.setOnPreferenceChangeListener { pref, newValue ->
            if (newValue == true) {
                AutoTranslateUi.requestEnable(requireContext()) { ok -> (pref as SwitchPreferenceCompat).isChecked = ok }
                false
            } else {
                true
            }
        }
        findPreference<Preference>(Prefs.OPEN_AI)?.setOnPreferenceClickListener {
            startActivity(Intent(requireContext(), AiSettingsActivity::class.java))
            true
        }
        findPreference<Preference>(Prefs.OPEN_CONTACT)?.let { p ->
            p.summary = Contact.EMAIL + ", Telegram: @" + Contact.TELEGRAM
            p.setOnPreferenceClickListener {
                startActivity(Intent(requireContext(), ContactActivity::class.java))
                true
            }
        }
    }
}

internal class SharedPrefsStore(private val ctx: Context, private val sp: SharedPreferences) : PreferenceDataStore() {
    // API kalitlari ochiq matn bo'lib sozlamalar fayliga tushmaydi: Secrets orqali Keystore bilan shifrlanadi
    override fun putString(key: String, value: String?) {
        if (Secrets.isSecret(key)) {
            Secrets.put(ctx, key, value)
            return
        }
        sp.edit().putString(key, value).apply()
    }

    override fun getString(key: String, defValue: String?): String? =
        if (Secrets.isSecret(key)) Secrets.get(ctx, key) ?: defValue else sp.getString(key, defValue)

    override fun putInt(key: String, value: Int) {
        sp.edit().putInt(key, value).apply()
    }

    override fun getInt(key: String, defValue: Int): Int = sp.getInt(key, defValue)

    override fun putBoolean(key: String, value: Boolean) {
        sp.edit().putBoolean(key, value).apply()
    }

    override fun getBoolean(key: String, defValue: Boolean): Boolean = sp.getBoolean(key, defValue)
}
