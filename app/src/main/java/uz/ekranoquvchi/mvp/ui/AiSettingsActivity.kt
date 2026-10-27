package uz.ekranoquvchi.mvp.ui

import android.os.Bundle
import android.text.InputType
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import uz.ekranoquvchi.mvp.R
import uz.ekranoquvchi.mvp.util.Prefs

/**
 * Sun'iy intellekt va onlayn imkoniyatlar: Gemini, ChatGPT, Claude va boshqa xizmatlar kalitlari, Microsoft ovozi.
 * Bosh ekrandagi alohida tugma orqali ochiladi. Hammasi ixtiyoriy va standartda o'chiq.
 */
class AiSettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.ai_settings_title)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, AiSettingsFragment())
                .commit()
        }
    }
}

class AiSettingsFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        // Sozlamalar xizmat bilan bir xil (qurilma-himoyalangan) faylga yoziladi; kalitlar shifrlanadi
        preferenceManager.preferenceDataStore =
            SharedPrefsStore(requireContext().applicationContext, Prefs.get(requireContext()))
        setPreferencesFromResource(R.xml.preferences_ai, rootKey)

        // Onlayn imkoniyatlarni yoqishdan oldin nima yuborilishini aytib, tasdiq so'raymiz
        findPreference<SwitchPreferenceCompat>(Prefs.ONLINE)?.setOnPreferenceChangeListener { pref, newValue ->
            if (newValue == true) {
                AlertDialog.Builder(requireContext())
                    .setTitle(R.string.online_consent_title)
                    .setMessage(R.string.online_consent_message)
                    .setPositiveButton(R.string.btn_online_accept) { _, _ ->
                        (pref as SwitchPreferenceCompat).isChecked = true
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
                false
            } else {
                true
            }
        }

        // Kalit matni ko'rinadi (ekran o'quvchi bilan tekshirish mumkin), klaviatura eslab qolmaydi
        val secretKeys = listOf(Prefs.GEMINI_KEY, Prefs.OPENAI_KEY, Prefs.AI_KEY, Prefs.CUSTOM_KEY, Prefs.AZURE_KEY)
        for (key in secretKeys) {
            val p = findPreference<EditTextPreference>(key) ?: continue
            p.setOnBindEditTextListener { edit ->
                edit.inputType = InputType.TYPE_CLASS_TEXT or
                    InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD or
                    InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
            }
            p.summaryProvider = Preference.SummaryProvider<EditTextPreference> { pref ->
                if (pref.text.isNullOrEmpty()) getString(R.string.pref_secret_empty) else getString(R.string.pref_secret_saved)
            }
        }
    }
}
