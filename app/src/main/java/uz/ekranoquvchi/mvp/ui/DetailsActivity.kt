package uz.ekranoquvchi.mvp.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.ListPreference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import uz.ekranoquvchi.mvp.R
import uz.ekranoquvchi.mvp.util.Prefs

/**
 * Tafsilotlar (TalkBack'dagi "Verbosity" kabi): elementlar o'qilganda nimalar aytilishi. Daraja (past, o'rta, yuqori)
 * tanlansa, hamma qiymat birdan o'rnatiladi; alohida qiymat o'zgartirilsa, daraja "maxsus" bo'ladi.
 * Bu yerda ovoz balandligi o'zgarganda o'qishni to'xtatmaslik ham yoqiladi va o'chiriladi.
 */
class DetailsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.details_title)
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, DetailsFragment())
                .commit()
        }
    }
}

class DetailsFragment : PreferenceFragmentCompat() {
    private val toggleKeys = listOf(Prefs.DET_TYPE_BTN, Prefs.DET_TYPE, Prefs.DET_POSITION, Prefs.DET_HINTS)

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val ctx = requireContext()
        Prefs.migrateDetails(ctx)
        // Sozlamalar xizmat bilan bir xil (qurilma-himoyalangan) faylga yoziladi
        preferenceManager.preferenceDataStore = SharedPrefsStore(ctx.applicationContext, Prefs.get(ctx))
        setPreferencesFromResource(R.xml.preferences_details, rootKey)

        findPreference<ListPreference>(Prefs.VERBOSITY)?.setOnPreferenceChangeListener { _, newValue ->
            val v = newValue as? String
            if (v != null && v != "custom") {
                Prefs.applyPreset(requireContext(), v)
                refreshToggles()
            }
            true
        }
        for (key in toggleKeys) {
            findPreference<SwitchPreferenceCompat>(key)?.setOnPreferenceChangeListener { _, _ ->
                findPreference<ListPreference>(Prefs.VERBOSITY)?.value = "custom"
                true
            }
        }
    }

    private fun refreshToggles() {
        val d = Prefs.details(requireContext())
        findPreference<SwitchPreferenceCompat>(Prefs.DET_TYPE)?.isChecked = d.typeOther
        findPreference<SwitchPreferenceCompat>(Prefs.DET_TYPE_BTN)?.isChecked = d.typeButtonImage
        findPreference<SwitchPreferenceCompat>(Prefs.DET_POSITION)?.isChecked = d.position
        findPreference<SwitchPreferenceCompat>(Prefs.DET_HINTS)?.isChecked = d.hints
    }
}
