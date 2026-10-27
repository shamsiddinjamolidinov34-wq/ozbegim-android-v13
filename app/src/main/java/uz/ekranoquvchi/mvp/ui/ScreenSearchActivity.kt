package uz.ekranoquvchi.mvp.ui

import android.os.Bundle
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import uz.ekranoquvchi.mvp.R
import uz.ekranoquvchi.mvp.ReaderService

/**
 * Ekranda qidirish (TalkBack'dagi "Ekranni qidirish"): kichik oyna, matn kiritiladi, so'ng xizmat ekrandagi mos
 * elementga fokusni o'tkazadi. Oyna dialog uslubida, shuning uchun orqadagi ilova ekranda qoladi.
 */
class ScreenSearchActivity : AppCompatActivity() {
    private lateinit var input: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.search_title)
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(48, 32, 48, 32)

        val label = TextView(this)
        label.setText(R.string.search_title)
        label.textSize = 20f
        label.isFocusable = true
        box.addView(label)

        input = EditText(this)
        input.setHint(R.string.search_hint)
        input.contentDescription = getString(R.string.search_hint)
        input.setSingleLine(true)
        input.imeOptions = EditorInfo.IME_ACTION_SEARCH
        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                submit()
                true
            } else {
                false
            }
        }
        box.addView(input)

        val go = Button(this)
        go.setText(R.string.search_go)
        go.setOnClickListener { submit() }
        box.addView(go)

        val close = Button(this)
        close.setText(R.string.btn_close)
        close.setOnClickListener { finish() }
        box.addView(close)

        setContentView(box)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
        input.requestFocus()
    }

    private fun submit() {
        val query = input.text?.toString()?.trim().orEmpty()
        finish()
        if (query.isNotEmpty()) ReaderService.instance?.searchScreen(query)
    }
}
