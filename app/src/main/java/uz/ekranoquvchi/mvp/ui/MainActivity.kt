package uz.ekranoquvchi.mvp.ui

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import uz.ekranoquvchi.mvp.R
import uz.ekranoquvchi.mvp.ReaderService
import uz.ekranoquvchi.mvp.diag.Diag
import uz.ekranoquvchi.mvp.util.SharedFiles
import uz.ekranoquvchi.mvp.util.SystemUtil
import uz.ekranoquvchi.mvp.util.UserFiles

class MainActivity : AppCompatActivity() {
    private fun click(id: Int, action: () -> Unit) {
        findViewById<Button>(id).setOnClickListener { action() }
    }

    private fun open(cls: Class<*>) = startActivity(Intent(this, cls))

    private fun editor(file: String, titleRes: Int, helpRes: Int) {
        startActivity(
            Intent(this, TextFileEditorActivity::class.java)
                .putExtra(TextFileEditorActivity.EXTRA_FILE, file)
                .putExtra(TextFileEditorActivity.EXTRA_TITLE, titleRes)
                .putExtra(TextFileEditorActivity.EXTRA_HELP, helpRes)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Diag.init(this)
        SharedFiles.dir(cacheDir) // eskirgan ulashish fayllarini (zaxira nusxa, jurnal) o'chiradi
        click(R.id.btn_enable) { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        click(R.id.btn_settings) { open(SettingsActivity::class.java) }
        click(R.id.btn_details) { open(DetailsActivity::class.java) }
        // Avtomatik tarjima: bitta tugma yoqadi va o'chiradi; yoqilgandan keyin hamma joyda o'zi ishlaydi
        click(R.id.btn_auto_translate) {
            if (AutoTranslateUi.isOn(this)) {
                AutoTranslateUi.turnOff(this)
                refreshAutoTranslateButton()
            } else {
                AutoTranslateUi.requestEnable(this) { refreshAutoTranslateButton() }
            }
        }
        click(R.id.btn_voices) { open(VoicesActivity::class.java) }
        click(R.id.btn_ai) { open(AiSettingsActivity::class.java) }
        click(R.id.btn_gestures) { open(GesturesActivity::class.java) }
        click(R.id.btn_tutorial) { open(TutorialActivity::class.java) }
        click(R.id.btn_test) {
            val service = ReaderService.instance
            if (service == null) {
                Toast.makeText(this, R.string.test_not_running, Toast.LENGTH_LONG).show()
            } else {
                service.speakTest()
            }
        }
        click(R.id.btn_health) { open(HealthActivity::class.java) }
        click(R.id.btn_log) { open(LogActivity::class.java) }
        click(R.id.btn_dictionary) { editor(UserFiles.DICTIONARY, R.string.btn_dictionary, R.string.help_dictionary) }
        click(R.id.btn_labels) { editor(UserFiles.LABELS, R.string.btn_labels, R.string.help_labels) }
        click(R.id.btn_profiles) { editor(UserFiles.PROFILES, R.string.btn_profiles, R.string.help_profiles) }
        click(R.id.btn_backup) { open(BackupActivity::class.java) }
        click(R.id.btn_contact) { open(ContactActivity::class.java) }
    }

    private fun refreshAutoTranslateButton() {
        findViewById<Button>(R.id.btn_auto_translate).setText(
            if (AutoTranslateUi.isOn(this)) R.string.auto_tr_btn_on else R.string.auto_tr_btn_off
        )
    }

    override fun onResume() {
        super.onResume()
        refreshAutoTranslateButton()
        findViewById<TextView>(R.id.status).setText(
            if (SystemUtil.isServiceEnabled(this)) R.string.main_status_on else R.string.main_status_off
        )
    }
}
