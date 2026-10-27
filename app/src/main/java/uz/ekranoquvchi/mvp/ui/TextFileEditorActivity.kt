package uz.ekranoquvchi.mvp.ui

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import uz.ekranoquvchi.mvp.R
import uz.ekranoquvchi.mvp.util.UserFiles

/**
 * Oddiy matn tahrirlagichi: talaffuz lug'ati, maxsus yorliqlar va ilova profillari fayllari uchun.
 * Saqlash tugmasi tahrirlash maydonidan oldin turadi (ekran o'quvchi bilan qulay).
 */
class TextFileEditorActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_FILE = "file"
        const val EXTRA_TITLE = "title"
        const val EXTRA_HELP = "help"
        const val EXTRA_APPEND = "append"
    }

    private lateinit var edit: EditText
    private lateinit var fileName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Fayl nomi faqat ma'lum uchta fayldan biri bo'lishi mumkin (yo'l almashtirishdan himoya)
        val requested = intent.getStringExtra(EXTRA_FILE)
        fileName = if (requested != null && UserFiles.isKnown(requested)) requested else UserFiles.DICTIONARY
        title = getString(intent.getIntExtra(EXTRA_TITLE, R.string.btn_dictionary))

        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(32, 32, 32, 32)

        val help = TextView(this)
        help.setText(intent.getIntExtra(EXTRA_HELP, R.string.help_dictionary))
        help.isFocusable = true
        help.setPadding(0, 0, 0, 16)

        val save = Button(this)
        save.setText(R.string.btn_save)
        save.setOnClickListener { saveAndClose() }

        edit = EditText(this)
        // Doimiy identifikator: ekran aylanganda yoki tizim sozlamasi o'zgarganda matn avtomatik tiklanadi
        edit.id = R.id.editor_text
        edit.setText(UserFiles.read(this, fileName))
        edit.gravity = Gravity.TOP or Gravity.START
        edit.minLines = 8
        edit.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
        val extra = intent.getStringExtra(EXTRA_APPEND)
        // Qayta yaratilganda (savedInstanceState != null) matn tizim tomonidan tiklanadi: ikkinchi marta qo'shilmaydi
        if (extra != null && savedInstanceState == null) {
            val cur = edit.text.toString()
            val sep = if (cur.isNotEmpty() && !cur.endsWith("\n")) "\n" else ""
            edit.setText(cur + sep + extra)
            edit.setSelection(edit.text.length)
        }

        box.addView(help)
        box.addView(save)
        box.addView(edit)
        val scroll = ScrollView(this)
        scroll.addView(box)
        setContentView(scroll)
    }

    private fun saveAndClose() {
        val text = edit.text.toString()
        if (text.length > UserFiles.MAX_CHARS) {
            Toast.makeText(this, R.string.msg_too_large, Toast.LENGTH_LONG).show()
            return
        }
        if (!UserFiles.write(this, fileName, text)) {
            Toast.makeText(this, R.string.msg_save_failed, Toast.LENGTH_LONG).show()
            return
        }
        UserFiles.reloadAll(this)
        Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show()
        finish()
    }
}
