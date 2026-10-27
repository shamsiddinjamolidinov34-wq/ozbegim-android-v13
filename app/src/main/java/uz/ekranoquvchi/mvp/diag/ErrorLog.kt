package uz.ekranoquvchi.mvp.diag

import android.content.Context
import android.os.Build
import android.os.SystemClock
import org.json.JSONArray
import org.json.JSONObject
import uz.ekranoquvchi.mvp.BuildConfig
import uz.ekranoquvchi.mvp.content.UzScripts
import uz.ekranoquvchi.mvp.util.SharedFiles
import java.io.File
import java.util.UUID
import java.util.concurrent.Executors

data class LogEntry(
    val timeMs: Long,
    val category: String,
    val severity: String,
    val code: String,
    val params: Map<String, String>,
)

/**
 * Xatolar jurnali (monografiya 15): JSON qatorlari, ekran matnisiz, faqat qurilmada.
 * Qurilma-himoyalangan xotirada saqlanadi, shuning uchun qulf ochilmasdan ham yoziladi.
 * Hajm chegarasi: 1 MB (to'lganda eski yarmi o'chadi). MVP'da shifrlash yo'q (keyingi qadam: Keystore).
 */
class ErrorLog(context: Context) {
    private val file = File(context.createDeviceProtectedStorageContext().filesDir, "errors.jsonl")
    private val session = UUID.randomUUID().toString().substring(0, 8)
    private val io = Executors.newSingleThreadExecutor { r -> Thread(r, "errorlog").apply { isDaemon = true } }
    private val lock = Any()

    fun log(code: ErrorCode, params: Map<String, Any> = emptyMap()) {
        val line = build(code, params, code.severity.ordinal <= Severity.P1.ordinal)
        io.execute { append(line) }
    }

    /** Qulash paytida: jarayon tugashidan oldin yozilishi shart. */
    fun logSync(code: ErrorCode, params: Map<String, Any> = emptyMap()) {
        append(build(code, params, true))
    }

    private fun build(code: ErrorCode, params: Map<String, Any>, withTrace: Boolean): String {
        val o = JSONObject()
        o.put("t", System.currentTimeMillis())
        o.put("s", session)
        o.put("build", BuildConfig.VERSION_NAME)
        o.put("api", Build.VERSION.SDK_INT)
        o.put("dev", "${Build.MANUFACTURER} ${Build.MODEL}".take(32))
        o.put("cat", code.category.name)
        o.put("sev", code.severity.name)
        o.put("code", code.name)
        o.put("packs", UzScripts.packs.joinToString(",") { "${it.id}@${it.version}" })
        val m = JSONObject()
        for ((k, v) in params) m.put(k, v.toString())
        o.put("m", m)
        if (withTrace) {
            val now = SystemClock.uptimeMillis()
            val arr = JSONArray()
            for (e in Diag.blackBox.snapshot()) {
                arr.put(JSONArray().put(e.t - now).put(e.kind).put(e.a).put(e.b))
            }
            o.put("trace", arr)
        }
        return o.toString()
    }

    private fun append(line: String) {
        synchronized(lock) {
            try {
                if (file.length() > MAX_BYTES) {
                    val lines = file.readLines()
                    file.writeText(lines.drop(lines.size / 2).joinToString("\n", postfix = "\n"))
                }
                file.appendText(line + "\n")
            } catch (_: Throwable) {
                // jurnal yozolmasa, ekran o'quvchi ishida to'siq bo'lmasligi kerak
            }
        }
    }

    fun readAll(limit: Int = 300): List<LogEntry> = synchronized(lock) {
        if (!file.exists()) emptyList()
        else file.readLines().asReversed().asSequence().mapNotNull { parse(it) }.take(limit).toList()
    }

    private fun parse(line: String): LogEntry? = try {
        val o = JSONObject(line)
        val map = HashMap<String, String>()
        val m = o.optJSONObject("m")
        if (m != null) {
            val keys = m.keys()
            while (keys.hasNext()) {
                val k = keys.next()
                map[k] = m.optString(k)
            }
        }
        LogEntry(o.optLong("t"), o.optString("cat"), o.optString("sev"), o.optString("code"), map)
    } catch (_: Throwable) {
        null
    }

    fun clear() {
        synchronized(lock) { file.delete() }
    }

    /** Ulashish uchun nusxa: tizim ulashish oynasi orqali, foydalanuvchi tasdig'i bilan. */
    fun exportText(cacheDir: File): File {
        val dir = SharedFiles.dir(cacheDir)
        val out = File(dir, "errors.txt")
        val sb = StringBuilder()
        sb.append("O\u2018zbegim xatolar jurnali (matnsiz)\n")
        sb.append("build=").append(BuildConfig.VERSION_NAME)
            .append(" api=").append(Build.VERSION.SDK_INT)
            .append(" dev=").append("${Build.MANUFACTURER} ${Build.MODEL}").append("\n\n")
        synchronized(lock) { if (file.exists()) sb.append(file.readText()) }
        out.writeText(sb.toString())
        return out
    }

    companion object {
        private const val MAX_BYTES = 1_000_000L
    }
}
