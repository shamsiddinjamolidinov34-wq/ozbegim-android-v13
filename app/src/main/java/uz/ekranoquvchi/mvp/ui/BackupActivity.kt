package uz.ekranoquvchi.mvp.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import org.json.JSONObject
import uz.ekranoquvchi.mvp.R
import uz.ekranoquvchi.mvp.core.GestureMap
import uz.ekranoquvchi.mvp.util.Prefs
import uz.ekranoquvchi.mvp.util.SharedFiles
import uz.ekranoquvchi.mvp.util.UserFiles
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Sozlamalarni eksport va import (FR-19): sozlamalar, ishoralar, lug'at, yorliqlar va profillar bitta JSON faylda.
 * Fayl versiyalangan ("v": 1).
 *
 * Xavfsizlik: import fayli ishonchsiz kirish deb hisoblanadi. Hajmi chegaralanadi (1 MB), har bir sozlama
 * turi va qiymat oralig'i bo'yicha tekshiriladi (Prefs.sanitize), faylda yo'q bo'limlar mavjud ma'lumotni
 * o'chirmaydi va hech narsa foydalanuvchi tasdiqlamaguncha qo'llanmaydi.
 */
class BackupActivity : AppCompatActivity() {
    private lateinit var container: LinearLayout

    /** Tekshirilgan, hali qo'llanmagan import ma'lumoti. */
    private class Parsed(
        val prefs: Map<String, Any>,
        val dictionary: String?,
        val labels: String?,
        val profiles: String?,
    )

    private val openDoc = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importFrom(uri)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)
        title = getString(R.string.btn_backup)
        container = findViewById(R.id.container)

        val intro = TextView(this)
        intro.setText(R.string.backup_intro)
        intro.textSize = 17f
        intro.isFocusable = true
        intro.setPadding(0, 0, 0, 16)
        container.addView(intro)

        val exportButton = Button(this)
        exportButton.setText(R.string.btn_export)
        exportButton.setOnClickListener { export() }
        container.addView(exportButton)

        val importButton = Button(this)
        importButton.setText(R.string.btn_import)
        importButton.setOnClickListener { openDoc.launch(arrayOf("*/*")) }
        container.addView(importButton)
    }

    private fun toast(res: Int) = Toast.makeText(this, res, Toast.LENGTH_LONG).show()

    private fun knownGestureIds(): Set<Int> = GestureMap.items.map { it.id }.toSet()

    // ------------------------------------------------------------------ eksport

    private fun export() {
        try {
            val ids = knownGestureIds()
            val o = JSONObject()
            o.put("v", 1)
            val prefs = JSONObject()
            for ((k, v) in Prefs.get(this).all) {
                // Buzilgan yoki noma'lum qiymat eksport fayliga o'tmaydi
                val clean = Prefs.sanitize(k, v, ids)
                if (clean != null) prefs.put(k, clean)
            }
            o.put("prefs", prefs)
            o.put("dictionary", UserFiles.read(this, UserFiles.DICTIONARY))
            o.put("labels", UserFiles.read(this, UserFiles.LABELS))
            o.put("profiles", UserFiles.read(this, UserFiles.PROFILES))
            val file = File(SharedFiles.dir(cacheDir), "ozbegim-sozlamalar.json")
            file.writeText(o.toString(2))
            val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(send, getString(R.string.btn_export)))
        } catch (_: Throwable) {
            toast(R.string.backup_failed)
        }
    }

    // ------------------------------------------------------------------ import

    /** Faylni 1 MB gacha o'qiydi; katta bo'lsa null (xotira to'lib ketmasligi uchun avval hajm tekshiriladi). */
    private fun readLimited(uri: Uri): String? =
        contentResolver.openInputStream(uri)?.use { input ->
            val out = ByteArrayOutputStream()
            val buf = ByteArray(8192)
            var total = 0
            var tooBig = false
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                total += n
                if (total > MAX_BYTES) {
                    tooBig = true
                    break
                }
                out.write(buf, 0, n)
            }
            if (tooBig) null else out.toString("UTF-8")
        }

    /** Matn bo'limi: yo'q bo'lsa null (mavjud fayl tegilmaydi); satr bo'lmasa yoki juda katta bo'lsa xato. */
    private fun textField(o: JSONObject, key: String): String? {
        if (!o.has(key)) return null
        val v = o.get(key) as? String ?: throw IllegalArgumentException(key)
        require(v.length <= UserFiles.MAX_CHARS) { key }
        return v
    }

    private fun parse(text: String): Parsed {
        val o = JSONObject(text)
        require(o.optInt("v", 0) == 1) { "v" }
        val ids = knownGestureIds()
        val clean = LinkedHashMap<String, Any>()
        val p = o.optJSONObject("prefs")
        if (p != null) {
            val keys = p.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                val v = Prefs.sanitize(k, p.opt(k), ids)
                if (v != null) clean[k] = v
            }
        }
        return Parsed(clean, textField(o, "dictionary"), textField(o, "labels"), textField(o, "profiles"))
    }

    /** Izoh bo'lmagan, bo'sh bo'lmagan qatorlar soni. */
    private fun entryCount(text: String?): Int =
        text?.lineSequence()?.count { val l = it.trim(); l.isNotEmpty() && !l.startsWith("#") } ?: 0

    private fun importFrom(uri: Uri) {
        val parsed = try {
            val text = readLimited(uri)
            if (text == null) null else parse(text)
        } catch (_: Throwable) {
            null
        }
        if (parsed == null) {
            toast(R.string.backup_failed)
            return
        }
        // Hech narsa tasdiqsiz qo'llanmaydi: ishonchsiz fayl profil yoki lug'atni jimgina almashtirmasin
        AlertDialog.Builder(this)
            .setTitle(R.string.backup_confirm_title)
            .setMessage(
                getString(
                    R.string.backup_confirm_message,
                    parsed.prefs.size,
                    entryCount(parsed.dictionary),
                    entryCount(parsed.labels),
                    entryCount(parsed.profiles),
                )
            )
            .setPositiveButton(R.string.btn_apply_import) { _, _ -> applyImport(parsed) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun applyImport(d: Parsed) {
        try {
            val edit = Prefs.get(this).edit()
            for ((k, v) in d.prefs) {
                when (v) {
                    is Boolean -> edit.putBoolean(k, v)
                    is Int -> edit.putInt(k, v)
                    is String -> edit.putString(k, v)
                }
            }
            edit.apply()
            d.dictionary?.let { UserFiles.write(this, UserFiles.DICTIONARY, it) }
            d.labels?.let { UserFiles.write(this, UserFiles.LABELS, it) }
            d.profiles?.let { UserFiles.write(this, UserFiles.PROFILES, it) }
            UserFiles.reloadAll(this)
            toast(R.string.backup_imported)
        } catch (_: Throwable) {
            toast(R.string.backup_failed)
        }
    }

    private companion object {
        const val MAX_BYTES = 1_000_000
    }
}
