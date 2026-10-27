package uz.ekranoquvchi.mvp.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import uz.ekranoquvchi.mvp.R
import uz.ekranoquvchi.mvp.util.Contact
import uz.ekranoquvchi.mvp.util.SystemUtil
import java.util.Locale

/**
 * Aloqa va murojaat: foydalanuvchi elektron pochta yoki Telegram orqali yozadi. "Xato haqida xat yozish" tugmasi xatga
 * ilova, Android va qurilma versiyasini o'zi qo'shadi (shaxsiy ma'lumot yo'q; xatni yuborishdan oldin foydalanuvchi ko'radi).
 * Matn va tugmalar ketma-ket joylashgan, hammasini ekran o'quvchi bilan surib o'qish mumkin.
 */
class ContactActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.contact_title)
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        val pad = (16 * resources.displayMetrics.density).toInt()
        box.setPadding(pad, pad, pad, pad)

        addText(box, getString(R.string.contact_intro), 16f)
        addText(box, getString(R.string.contact_email_line, Contact.EMAIL), 18f)
        addButton(box, R.string.contact_btn_mail) { sendMail(getString(R.string.contact_mail_subject, version()), "") }
        addButton(box, R.string.contact_btn_bug) { sendMail(getString(R.string.contact_bug_subject, version()), bugBody()) }
        addButton(box, R.string.contact_btn_copy_mail) { copy(Contact.EMAIL) }
        addText(box, getString(R.string.contact_telegram_line, Contact.TELEGRAM), 18f)
        addButton(box, R.string.contact_btn_telegram) { openTelegram() }
        addButton(box, R.string.contact_btn_copy_telegram) { copy("@" + Contact.TELEGRAM) }
        addButton(box, R.string.contact_btn_log) { startActivity(Intent(this, LogActivity::class.java)) }

        val scroll = ScrollView(this)
        scroll.addView(box)
        setContentView(scroll)
    }

    private fun addText(parent: LinearLayout, text: String, size: Float) {
        val tv = TextView(this)
        tv.text = text
        tv.textSize = size
        tv.isFocusable = true
        tv.setPadding(0, 0, 0, (12 * resources.displayMetrics.density).toInt())
        parent.addView(tv)
    }

    private fun addButton(parent: LinearLayout, textRes: Int, onClick: () -> Unit) {
        val b = Button(this)
        b.setText(textRes)
        b.setOnClickListener { onClick() }
        parent.addView(b)
    }

    private fun version(): String = try {
        packageManager.getPackageInfo(packageName, 0).versionName ?: ""
    } catch (_: Throwable) {
        ""
    }

    private fun bugBody(): String {
        val on = SystemUtil.isServiceEnabled(this)
        return getString(
            R.string.contact_bug_body,
            version(),
            Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")",
            Build.MANUFACTURER + " " + Build.MODEL,
            Locale.getDefault().toLanguageTag(),
            getString(if (on) R.string.contact_on else R.string.contact_off),
        )
    }

    private fun sendMail(subject: String, body: String) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(Contact.EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            if (body.isNotEmpty()) putExtra(Intent.EXTRA_TEXT, body)
        }
        try {
            startActivity(intent)
        } catch (_: Throwable) {
            copy(Contact.EMAIL, false)
            Toast.makeText(this, R.string.contact_no_mail, Toast.LENGTH_LONG).show()
        }
    }

    private fun openTelegram() {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Contact.TELEGRAM_URL)))
        } catch (_: Throwable) {
            copy("@" + Contact.TELEGRAM, false)
            Toast.makeText(this, R.string.contact_no_telegram, Toast.LENGTH_LONG).show()
        }
    }

    private fun copy(text: String, announce: Boolean = true) {
        try {
            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            cm.setPrimaryClip(ClipData.newPlainText("O'zbegim", text))
            if (announce) Toast.makeText(this, R.string.msg_copied, Toast.LENGTH_SHORT).show()
        } catch (_: Throwable) {
        }
    }
}
