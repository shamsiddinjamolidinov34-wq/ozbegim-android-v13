package uz.ekranoquvchi.mvp.ui

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import uz.ekranoquvchi.mvp.R
import uz.ekranoquvchi.mvp.core.GestureAction
import uz.ekranoquvchi.mvp.core.GestureMap
import uz.ekranoquvchi.mvp.util.Prefs

/** Ishoralarni sozlash (FR-02, FR-21): profil (TalkBack-mos / VoiceOver-mos) va har bir ishora uchun amal. */
class GesturesActivity : AppCompatActivity() {
    private lateinit var container: LinearLayout
    private val actions = GestureAction.values().toList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        title = getString(R.string.btn_gestures)
        container = findViewById(R.id.container)
        render()
    }

    private fun text(s: String) {
        val tv = TextView(this)
        tv.text = s
        tv.textSize = 17f
        tv.setPadding(0, 16, 0, 8)
        tv.isFocusable = true
        container.addView(tv)
    }

    /** Dastlabki qiymatga qaytilganda ham, boshqa tanlanganda ham to'g'ri ishlaydi (boshlang'ich chaqiruv e'tiborga olinmaydi). */
    private fun spinner(description: String, options: List<String>, selected: Int, onChange: (Int) -> Unit): Spinner {
        val sp = Spinner(this)
        sp.contentDescription = description
        sp.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)
        sp.setSelection(selected)
        var current = selected
        sp.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position == current) return
                current = position
                onChange(position)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        return sp
    }

    private fun render() {
        container.removeAllViews()
        text(getString(R.string.gestures_intro))

        text(getString(R.string.gesture_profile))
        val profiles = listOf(getString(R.string.profile_talkback), getString(R.string.profile_voiceover))
        val currentProfile = if (Prefs.gestureProfile(this) == "voiceover") 1 else 0
        container.addView(
            spinner(getString(R.string.gesture_profile), profiles, currentProfile) { pos ->
                Prefs.get(this).edit().putString(Prefs.GESTURE_PROFILE, if (pos == 1) "voiceover" else "talkback").apply()
                GestureMap.reset(this) // profil almashganda shaxsiy o'zgartirishlar tozalanadi
                render()
            }
        )

        val reset = Button(this)
        reset.setText(R.string.btn_reset_gestures)
        reset.setOnClickListener {
            GestureMap.reset(this)
            render()
        }
        container.addView(reset)

        val labels = actions.map { it.label }
        for (item in GestureMap.items) {
            text(item.name)
            val idx = actions.indexOf(GestureMap.actionFor(this, item.id))
            container.addView(
                spinner(item.name, labels, if (idx >= 0) idx else 0) { pos -> GestureMap.save(this, item.id, actions[pos]) }
            )
        }
    }
}
