package uz.ekranoquvchi.mvp.ui

import android.accessibilityservice.AccessibilityService
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import uz.ekranoquvchi.mvp.R
import uz.ekranoquvchi.mvp.ReaderService
import uz.ekranoquvchi.mvp.core.Tutorial

/**
 * O'quv mashqlari (FR-18, 8.2): birinchi mashq chiqish yo'lini o'rgatadi. Keyingilari ishoralarni mashq qildiradi;
 * ishora bajarilganini xizmat shu ekranga xabar qiladi.
 */
class TutorialActivity : AppCompatActivity() {
    private class Lesson(val textRes: Int, val gesture: Int, val buttonRes: Int)

    private val lessons = listOf(
        Lesson(R.string.lesson_exit, -1, R.string.btn_understood),
        Lesson(R.string.lesson_1, AccessibilityService.GESTURE_SWIPE_RIGHT, 0),
        Lesson(R.string.lesson_2, AccessibilityService.GESTURE_SWIPE_LEFT, 0),
        Lesson(R.string.lesson_3, AccessibilityService.GESTURE_SWIPE_UP_AND_DOWN, 0),
        Lesson(R.string.lesson_4, AccessibilityService.GESTURE_2_FINGER_SINGLE_TAP, 0),
        Lesson(R.string.lesson_5, AccessibilityService.GESTURE_3_FINGER_SINGLE_TAP, 0),
        Lesson(R.string.lesson_6, -1, R.string.btn_practice),
    ).filter {
        // Ko'p barmoqli ishoralar Android 10 (API 29) dan boshlab bor. Android 9 da bu mashqlar hech qachon
        // bajarilmasdi va foydalanuvchi shu yerda tiqilib qolardi, shuning uchun o'tkazib yuboriladi.
        it.gesture < AccessibilityService.GESTURE_2_FINGER_SINGLE_TAP || Build.VERSION.SDK_INT >= 29
    }
    private var index = 0
    private lateinit var text: TextView
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = getString(R.string.btn_tutorial)
        val box = LinearLayout(this)
        box.orientation = LinearLayout.VERTICAL
        box.setPadding(32, 32, 32, 32)
        text = TextView(this)
        text.textSize = 20f
        text.isFocusable = true
        button = Button(this)
        button.setOnClickListener { onButton() }
        box.addView(text)
        box.addView(button)
        setContentView(box)
        show()
    }

    override fun onResume() {
        super.onResume()
        Tutorial.listener = { id -> onGesture(id) }
    }

    override fun onPause() {
        Tutorial.listener = null
        super.onPause()
    }

    private fun show() {
        val lesson = lessons[index]
        text.setText(lesson.textRes)
        if (lesson.buttonRes != 0) {
            button.setText(lesson.buttonRes)
            button.visibility = View.VISIBLE
        } else {
            button.visibility = View.GONE
        }
        ReaderService.instance?.announce(getString(lesson.textRes))
    }

    private fun onGesture(id: Int) {
        if (index < lessons.size && lessons[index].gesture == id) advance()
    }

    private fun onButton() {
        if (index < lessons.size && lessons[index].gesture == -1) advance()
    }

    private fun advance() {
        index++
        if (index >= lessons.size) {
            text.setText(R.string.lesson_done)
            button.visibility = View.GONE
            ReaderService.instance?.announce(getString(R.string.lesson_done))
        } else {
            ReaderService.instance?.announce(getString(R.string.lesson_ok))
            show()
        }
    }
}
