package uz.ekranoquvchi.mvp.ui

import android.content.Context
import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import uz.ekranoquvchi.mvp.R
import uz.ekranoquvchi.mvp.ReaderService
import uz.ekranoquvchi.mvp.util.Prefs

/**
 * Aniq raqamli sozlash oynasi (nutq tezligi va ovoz toni uchun): qiymat to'g'ridan-to'g'ri yoziladi yoki
 * 1 va 10 foizlik tugmalar bilan o'zgartiriladi. Har o'zgarishdan keyin yangi qiymat darhol qo'llanadi va
 * aytiladi, shuning uchun foydalanuvchi natijani o'sha zahoti eshitadi. "Bekor qilish" eski qiymatni qaytaradi.
 */
object NumberDialog {
    /**
     * @param titleRes oyna sarlavhasi (masalan, "Nutq tezligi (foiz)")
     * @param key sozlama kaliti (Prefs.RATE yoki Prefs.PITCH), qiymati Int
     * @param range ruxsat etilgan chegaralar (foiz)
     * @param def standart qiymat
     * @param onDone oyna yopilgach chaqiriladi (sozlamalar ekranidagi qisqa matnni yangilash uchun)
     */
    fun show(context: Context, titleRes: Int, key: String, range: IntRange, def: Int, onDone: () -> Unit) {
        val prefs = Prefs.get(context)
        val original = try {
            prefs.getInt(key, def)
        } catch (_: ClassCastException) {
            def
        }
        val start = original.coerceIn(range.first, range.last)
        val density = context.resources.displayMetrics.density
        val pad = (20 * density).toInt()

        val box = LinearLayout(context)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(pad, pad / 2, pad, 0)

        val hint = TextView(context)
        hint.text = context.getString(R.string.number_dialog_hint, range.first, range.last)
        box.addView(hint)

        val input = EditText(context)
        input.inputType = InputType.TYPE_CLASS_NUMBER
        input.imeOptions = EditorInfo.IME_ACTION_DONE
        input.setSingleLine(true)
        input.hint = context.getString(titleRes)
        input.setText(start.toString())
        input.setSelectAllOnFocus(true)
        box.addView(input)

        fun current(): Int = input.text.toString().trim().toIntOrNull()?.coerceIn(range.first, range.last) ?: start

        fun setValue(value: Int, say: Boolean) {
            val v = value.coerceIn(range.first, range.last)
            input.setText(v.toString())
            input.setSelection(input.text.length)
            // Xizmat shu faylni tinglaydi: yangi qiymat nutqqa darhol qo'llanadi
            prefs.edit().putInt(key, v).apply()
            if (say) ReaderService.instance?.announce(context.getString(R.string.number_dialog_value, v))
        }

        fun stepButton(label: String, description: String, delta: Int): Button {
            val b = Button(context)
            b.text = label
            b.contentDescription = description
            b.setOnClickListener { setValue(current() + delta, true) }
            return b
        }

        fun cell() = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)

        val row = LinearLayout(context)
        row.orientation = LinearLayout.HORIZONTAL
        row.addView(stepButton("\u221210", context.getString(R.string.number_dialog_minus, 10), -10), cell())
        row.addView(stepButton("\u22121", context.getString(R.string.number_dialog_minus, 1), -1), cell())
        row.addView(stepButton("+1", context.getString(R.string.number_dialog_plus, 1), 1), cell())
        row.addView(stepButton("+10", context.getString(R.string.number_dialog_plus, 10), 10), cell())
        box.addView(row)

        val test = Button(context)
        test.text = context.getString(R.string.number_dialog_test)
        test.setOnClickListener {
            setValue(current(), false)
            ReaderService.instance?.speakTest()
        }
        box.addView(test)

        val reset = Button(context)
        reset.text = context.getString(R.string.number_dialog_default, def)
        reset.setOnClickListener { setValue(def, true) }
        box.addView(reset)

        input.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                setValue(current(), true)
                true
            } else {
                false
            }
        }

        AlertDialog.Builder(context)
            .setTitle(titleRes)
            .setView(box)
            .setPositiveButton(R.string.number_dialog_save) { _, _ ->
                setValue(current(), false)
                onDone()
            }
            .setNegativeButton(R.string.number_dialog_cancel) { _, _ ->
                prefs.edit().putInt(key, original).apply()
                onDone()
            }
            .setOnCancelListener {
                prefs.edit().putInt(key, original).apply()
                onDone()
            }
            .show()
    }
}
