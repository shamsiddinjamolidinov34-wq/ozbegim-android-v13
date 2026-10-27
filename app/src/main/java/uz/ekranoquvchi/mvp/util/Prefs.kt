package uz.ekranoquvchi.mvp.util

import android.content.Context
import android.content.SharedPreferences
import uz.ekranoquvchi.mvp.content.BridgeMode
import uz.ekranoquvchi.mvp.content.DescOrder
import uz.ekranoquvchi.mvp.content.Details
import uz.ekranoquvchi.mvp.content.NewLatinMode
import uz.ekranoquvchi.mvp.content.PipelineOptions
import uz.ekranoquvchi.mvp.content.ScriptMode
import uz.ekranoquvchi.mvp.content.Verbosity
import uz.ekranoquvchi.mvp.core.GestureAction

/**
 * Sozlamalar qurilma-himoyalangan xotirada: qayta yoqilgandan keyin, qulf ochilmasdan (PIN ekranida)
 * ham o'qiladi (Direct Boot). Xizmat ham, sozlamalar ekrani ham shu bitta faylni ishlatadi.
 *
 * Barcha o'qish funksiyalari xatoga chidamli: faylda noto'g'ri tur yoki ruxsat etilmagan qiymat bo'lsa
 * (masalan, buzilgan zaxira fayli), standart qiymat qaytariladi. Ilgari bunday holat ClassCastException
 * bilan xizmatni qulatishi mumkin edi.
 */
object Prefs {
    const val FILE = "settings"
    const val RATE = "speech_rate"
    const val PITCH = "speech_pitch"
    const val ECHO_MODE = "echo_mode"
    const val MIC_POLICY = "mic_policy"
    const val GESTURE_PROFILE = "gesture_profile"
    const val SCRIPT = "script_mode"
    const val NEW_LATIN = "new_latin_mode"
    const val APOSTROPHE = "apostrophe"
    const val BRIDGE = "bridge_mode"
    const val PRIVATE_NOTIF = "private_notifications"
    const val ENGINE = "engine_pref"
    const val VOICE = "voice_name"
    const val VERBOSITY = "verbosity"
    const val EARCONS = "earcons"
    const val OPEN_TTS = "open_tts_settings"
    const val OPEN_VOICES = "open_voices"
    const val OPEN_GESTURES = "open_gestures"
    const val OPEN_AI = "open_ai_settings"
    const val OPEN_CONTACT = "open_contact"
    const val OPEN_DETAILS = "open_details"
    const val OPEN_RATE = "open_rate"
    const val OPEN_PITCH = "open_pitch"

    /** Nutq tezligi va toni ishoralar bilan o'zgartirilganda bir surishdagi qadam (foiz): 1, 2, 5, 10 yoki 20. */
    const val RATE_STEP = "rate_step"

    // Tafsilotlar (TalkBack'dagi "Verbosity" kabi): nimalar aytilishi. VERBOSITY: short, normal, full yoki custom (maxsus).
    const val DET_TYPE = "det_type"
    const val DET_TYPE_BTN = "det_type_btn"
    const val DET_POSITION = "det_position"
    const val DET_HINTS = "det_hints"
    const val DET_ORDER = "det_order"
    const val DET_WINDOW_TITLES = "det_window_titles"
    const val DET_SCREEN_OFF = "det_screen_off"
    const val DET_VOLUME_KEEP = "det_volume_keep"
    private const val DET_MIGRATED = "det_migrated"

    // Tezlashtirish va avtomatik tarjima
    const val SPEED = "response_speed"
    const val AUTO_TRANSLATE = "auto_translate"
    const val TRANSLATE_SCRIPT = "translate_script"

    /** Ikki barmoqli ishorani ilovaga o'tkazib yuborish: "keyguard" (faqat qulf ekranida), "always", "off". */
    const val TWO_FINGER = "two_finger_pass"

    // Onlayn imkoniyatlar (ixtiyoriy). Kalitlar (AZURE_KEY, AI_KEY) Secrets orqali shifrlab saqlanadi.
    const val ONLINE = "online_enabled"
    const val AZURE_KEY = "azure_key"
    const val AZURE_REGION = "azure_region"
    const val AZURE_VOICE = "azure_voice"
    const val AZURE_QUALITY = "azure_quality"
    const val AI_KEY = "ai_key"
    const val AI_MODEL = "ai_model"
    const val AI_PROVIDER = "ai_provider"
    const val GEMINI_KEY = "gemini_key"
    const val GEMINI_MODEL = "gemini_model"
    const val OPENAI_KEY = "openai_key"
    const val OPENAI_MODEL = "openai_model"
    const val CUSTOM_KEY = "custom_key"
    const val CUSTOM_MODEL = "custom_model"
    const val CUSTOM_URL = "custom_url"
    const val TRANSLATE_FROM = "translate_from"
    const val TRANSLATE_TO = "translate_to"
    const val TYPING_MODE = "typing_mode"

    const val DEFAULT_AZURE_REGION = "westeurope"
    const val DEFAULT_AZURE_VOICE = "uz-UZ-MadinaNeural"
    const val DEFAULT_AI_MODEL = "claude-haiku-5-5"
    const val DEFAULT_GEMINI_MODEL = "gemini-flash-latest"
    const val DEFAULT_OPENAI_MODEL = "gpt-5.4-mini"

    private val REGION_RE = Regex("^[a-z0-9]{3,30}$")
    private val MODEL_RE = Regex("^[A-Za-z0-9._-]{3,80}$")
    private val CUSTOM_MODEL_RE = Regex("^[A-Za-z0-9._:/-]{3,100}$")
    private val URL_RE = Regex("^https://[A-Za-z0-9.-]+(:[0-9]{1,5})?(/[A-Za-z0-9._~%/-]*)?$")

    /** Tarjima tillari (strings.xml dagi translate_values massivi bilan bir xil). Manba tilida qo'shimcha "auto" (avtomatik). */
    val TRANSLATE_LANGS = listOf(
        "uz", "ru", "en", "tr", "ar", "fa", "tg", "kk", "ky", "tk", "zh", "ko", "ja", "de", "fr", "es", "it",
    )

    /** Ishora sozlamalari kaliti: "g_" + ishora identifikatori. */
    const val GESTURE_PREFIX = "g_"

    /** Ovoz nomining eng katta uzunligi. */
    const val MAX_VOICE_LENGTH = 200

    /** Ichki xotira (eksport qilinmaydi): o'zbek tili tizim dvigatelida yo'qligi sababli tanlangan dvigatel. */
    private const val AUTO_ENGINE = "auto_engine_memo"

    /**
     * Qiymat chegaralari (foiz). Sozlamalarda aniq son yoziladi yoki 1 foizlik qadam bilan o'zgartiriladi (NumberDialog),
     * shuning uchun chegaralar keng: tezlik 20-500, ton 20-300. Onlayn (Azure) ovozda tezlik 50-200, ton 50-150
     * oralig'idan tashqarisi shu chegaraga keltiriladi (xizmat ruxsat bergan oraliq).
     */
    val RATE_RANGE = 20..500
    val PITCH_RANGE = 20..300

    /** Matnli sozlamalarning ruxsat etilgan qiymatlari (strings.xml dagi *_values massivlari bilan bir xil). */
    val CHOICES: Map<String, Set<String>> = mapOf(
        ECHO_MODE to setOf("chars", "words", "both", "off"),
        MIC_POLICY to setOf("stop", "duck", "continue"),
        GESTURE_PROFILE to setOf("talkback", "voiceover"),
        SCRIPT to setOf("auto", "latin", "keep"),
        NEW_LATIN to setOf("auto", "uz", "off"),
        APOSTROPHE to setOf("ascii", "modifier"),
        BRIDGE to setOf("adapt", "off"),
        ENGINE to setOf("auto", "rhvoice", "system", "azure"),
        VERBOSITY to setOf("short", "normal", "full", "custom"),
        DET_ORDER to setOf("name_type_state", "type_name_state", "state_name_type"),
        SPEED to setOf("normal", "fast", "turbo"),
        TRANSLATE_SCRIPT to setOf("latin", "cyrl", "new"),
        TWO_FINGER to setOf("keyguard", "always", "off"),
        AZURE_VOICE to setOf(
            "uz-UZ-MadinaNeural", "uz-UZ-SardorNeural",
            "ru-RU-SvetlanaNeural", "ru-RU-DmitryNeural",
            "en-US-JennyNeural", "en-US-GuyNeural",
        ),
        AZURE_QUALITY to setOf("mp3_192", "mp3_96"),
        AI_PROVIDER to setOf("auto", "claude", "gemini", "openai", "custom"),
        TRANSLATE_FROM to (setOf("auto") + TRANSLATE_LANGS),
        TRANSLATE_TO to (setOf("uz-cyrl") + TRANSLATE_LANGS),
        TYPING_MODE to setOf("standard", "touch"),
        RATE_STEP to setOf("1", "2", "5", "10", "20"),
    )

    private val BOOLEANS = setOf(
        PRIVATE_NOTIF, EARCONS, AUTO_TRANSLATE,
        DET_TYPE, DET_TYPE_BTN, DET_POSITION, DET_HINTS, DET_WINDOW_TITLES, DET_SCREEN_OFF, DET_VOLUME_KEEP,
    )

    @Volatile
    private var cached: SharedPreferences? = null

    /** Bitta umumiy nusxa: har chaqiruvda yangi Context o'rami yaratilmaydi (hodisalar ko'p, kechikish muhim). */
    fun get(context: Context): SharedPreferences {
        val existing = cached
        if (existing != null) return existing
        return synchronized(this) {
            cached ?: run {
                val base = context.applicationContext ?: context
                base.createDeviceProtectedStorageContext()
                    .getSharedPreferences(FILE, Context.MODE_PRIVATE)
                    .also { cached = it }
            }
        }
    }

    // ------------------------------------------------------------------ xavfsiz o'qish

    private fun readString(c: Context, key: String, def: String): String =
        try {
            get(c).getString(key, def) ?: def
        } catch (_: ClassCastException) {
            def
        }

    private fun readInt(c: Context, key: String, def: Int): Int =
        try {
            get(c).getInt(key, def)
        } catch (_: ClassCastException) {
            def
        }

    private fun readBool(c: Context, key: String, def: Boolean): Boolean =
        try {
            get(c).getBoolean(key, def)
        } catch (_: ClassCastException) {
            def
        }

    private fun choice(c: Context, key: String, def: String): String {
        val v = readString(c, key, def)
        return if (CHOICES[key]?.contains(v) == true) v else def
    }

    /** Ixtiyoriy matnli kalit uchun xavfsiz o'qish (tur mos kelmasa null). */
    fun string(c: Context, key: String): String? =
        try {
            get(c).getString(key, null)
        } catch (_: ClassCastException) {
            null
        }

    fun rate(c: Context): Int = readInt(c, RATE, 100).coerceIn(RATE_RANGE.first, RATE_RANGE.last)

    fun pitch(c: Context): Int = readInt(c, PITCH, 100).coerceIn(PITCH_RANGE.first, PITCH_RANGE.last)

    /** Tezlik va tonni ishora bilan o'zgartirishdagi qadam (foiz). */
    fun rateStep(c: Context): Int = choice(c, RATE_STEP, "10").toIntOrNull() ?: 10

    /** Yozilgan matn e'loni: "chars" (harflar), "words" (so'zlar), "both" (ikkalasi), "off". */
    fun echoMode(c: Context): String = choice(c, ECHO_MODE, "chars")

    /**
     * Mikrofon ochiq bo'lganda (masalan, ovozli xabar yozilayotganda): "stop" (avtomatik xabarlar o'qilmaydi,
     * bosib o'qiganlaringiz past ovozda aytiladi), "duck" (hammasi past ovozda), "continue" (o'zgarishsiz).
     */
    fun micPolicy(c: Context): String = choice(c, MIC_POLICY, "stop")

    /** Ishora profili: "talkback" (TalkBack-mos) yoki "voiceover" (VoiceOver-mos). */
    fun gestureProfile(c: Context): String = choice(c, GESTURE_PROFILE, "talkback")

    fun privateNotifications(c: Context): Boolean = readBool(c, PRIVATE_NOTIF, true)

    fun earcons(c: Context): Boolean = readBool(c, EARCONS, false)

    /** "auto": tizim dvigateli, o'zbek tili bo'lmasa RHVoice; "rhvoice": RHVoice; "system": faqat tizim dvigateli. */
    fun engine(c: Context): String = choice(c, ENGINE, "auto")

    fun autoEngine(c: Context): String = readString(c, AUTO_ENGINE, "")

    /** Onlayn imkoniyatlar asosiy kaliti: standartda o'chiq, foydalanuvchi tasdig'i bilan yoqiladi. */
    fun onlineEnabled(c: Context): Boolean = readBool(c, ONLINE, false)

    fun azureRegion(c: Context): String {
        val v = readString(c, AZURE_REGION, DEFAULT_AZURE_REGION).trim().lowercase()
        return if (REGION_RE.matches(v)) v else DEFAULT_AZURE_REGION
    }

    fun azureVoice(c: Context): String = choice(c, AZURE_VOICE, DEFAULT_AZURE_VOICE)

    /** "mp3_192": 48 kHz, 192 kbit/s; "mp3_96": 48 kHz, 96 kbit/s. */
    fun azureQuality(c: Context): String = choice(c, AZURE_QUALITY, "mp3_192")

    fun aiModel(c: Context): String {
        val v = readString(c, AI_MODEL, DEFAULT_AI_MODEL).trim()
        return if (MODEL_RE.matches(v)) v else DEFAULT_AI_MODEL
    }

    /** Sun'iy intellekt xizmati: "auto" (kaliti kiritilgan birinchisi), "claude", "gemini", "openai" yoki "custom". */
    fun aiProvider(c: Context): String = choice(c, AI_PROVIDER, "auto")

    fun geminiModel(c: Context): String {
        val v = readString(c, GEMINI_MODEL, DEFAULT_GEMINI_MODEL).trim()
        return if (MODEL_RE.matches(v)) v else DEFAULT_GEMINI_MODEL
    }

    fun openaiModel(c: Context): String {
        val v = readString(c, OPENAI_MODEL, DEFAULT_OPENAI_MODEL).trim()
        return if (CUSTOM_MODEL_RE.matches(v)) v else DEFAULT_OPENAI_MODEL
    }

    /** Boshqa (OpenAI bilan mos) xizmat modeli; kiritilmagan yoki noto'g'ri bo'lsa bo'sh. */
    fun customModel(c: Context): String {
        val v = readString(c, CUSTOM_MODEL, "").trim()
        return if (CUSTOM_MODEL_RE.matches(v)) v else ""
    }

    /** Boshqa xizmat manzili (faqat https); kiritilmagan yoki noto'g'ri bo'lsa bo'sh. */
    fun customUrl(c: Context): String {
        val v = readString(c, CUSTOM_URL, "").trim()
        return if (URL_RE.matches(v)) v else ""
    }

    /** Tarjima manba tili: "auto" (tilni o'zi aniqlaydi, standart) yoki til kodi ("uz", "ru", "en" va h.k.). */
    fun translateFrom(c: Context): String = choice(c, TRANSLATE_FROM, "auto")

    /** Tarjima maqsad tili: standart "uz" (o'zbekcha, lotin); "uz-cyrl" o'zbekcha kirill. */
    fun translateTo(c: Context): String = choice(c, TRANSLATE_TO, "uz")

    /** Klaviatura rejimi: "standard" (harf ikki marta bosilganda yoziladi) yoki "touch" (barmoq ko'tarilganda yoziladi). */
    fun typingMode(c: Context): String = choice(c, TYPING_MODE, "standard")

    fun setAutoEngine(c: Context, engine: String) {
        try {
            get(c).edit().putString(AUTO_ENGINE, engine).apply()
        } catch (_: Throwable) {
        }
    }

    fun voice(c: Context): String = readString(c, VOICE, "").take(MAX_VOICE_LENGTH)

    fun verbosity(c: Context): Verbosity = when (choice(c, VERBOSITY, "normal")) {
        "short" -> Verbosity.SHORT
        "full" -> Verbosity.FULL
        else -> Verbosity.NORMAL
    }

    /** Tafsilot darajasi nomi: "short", "normal", "full" yoki "custom". */
    fun verbosityPreset(c: Context): String = choice(c, VERBOSITY, "normal")

    /** Hozirgi tafsilotlar: hamma qiymat alohida kalitlarda saqlanadi (daraja tanlansa, ular birdan o'rnatiladi). */
    fun details(c: Context): Details = Details(
        typeOther = readBool(c, DET_TYPE, true),
        typeButtonImage = readBool(c, DET_TYPE_BTN, false),
        position = readBool(c, DET_POSITION, false),
        hints = readBool(c, DET_HINTS, false),
        order = when (choice(c, DET_ORDER, "name_type_state")) {
            "type_name_state" -> DescOrder.TYPE_NAME_STATE
            "state_name_type" -> DescOrder.STATE_NAME_TYPE
            else -> DescOrder.NAME_TYPE_STATE
        },
    )

    /** Tafsilot darajasini tanlaydi va unga mos hamma qiymatni o'rnatadi (TalkBack'dagi "preset" kabi). */
    fun applyPreset(c: Context, preset: String) {
        try {
            val d = when (preset) {
                "short" -> Details.LOW
                "full" -> Details.HIGH
                else -> Details.MEDIUM
            }
            val name = if (preset == "short" || preset == "full") preset else "normal"
            get(c).edit()
                .putString(VERBOSITY, name)
                .putBoolean(DET_TYPE, d.typeOther)
                .putBoolean(DET_TYPE_BTN, d.typeButtonImage)
                .putBoolean(DET_POSITION, d.position)
                .putBoolean(DET_HINTS, d.hints)
                .apply()
        } catch (_: Throwable) {
        }
    }

    /** Eski versiyadan o'tish: saqlangan daraja (qisqa, oddiy, batafsil) alohida qiymatlarga bir marta yoziladi. */
    fun migrateDetails(c: Context) {
        try {
            if (readBool(c, DET_MIGRATED, false)) return
            val preset = choice(c, VERBOSITY, "normal")
            val d = if (preset == "custom") Details.MEDIUM else Details.forPreset(verbosity(c))
            get(c).edit()
                .putBoolean(DET_TYPE, d.typeOther)
                .putBoolean(DET_TYPE_BTN, d.typeButtonImage)
                .putBoolean(DET_POSITION, d.position)
                .putBoolean(DET_HINTS, d.hints)
                .putBoolean(DET_MIGRATED, true)
                .apply()
        } catch (_: Throwable) {
        }
    }

    fun windowTitles(c: Context): Boolean = readBool(c, DET_WINDOW_TITLES, true)

    /** Ekran o'chiq bo'lganda ham avtomatik xabarlarni (bildirishnoma, jonli matn) aytish. */
    fun speakWhenScreenOff(c: Context): Boolean = readBool(c, DET_SCREEN_OFF, true)

    /** Ovoz tugmalari bosilganda hozirgi o'qish to'xtamasligi va fokus ovoz paneliga o'tmasligi. */
    fun volumeKeep(c: Context): Boolean = readBool(c, DET_VOLUME_KEEP, true)

    /** Javob tezligi: "normal", "fast" (standart) yoki "turbo". */
    fun speedLevel(c: Context): String = choice(c, SPEED, "fast")

    fun autoTranslate(c: Context): Boolean = readBool(c, AUTO_TRANSLATE, false)

    /** Avtomatik tarjima qaysi yozuvda tayyorlanadi: "latin" (hozirgi), "cyrl" (kirill) yoki "new" (yangi alifbo). */
    fun translateScript(c: Context): String = choice(c, TRANSLATE_SCRIPT, "latin")

    fun twoFingerPass(c: Context): String = choice(c, TWO_FINGER, "keyguard")

    fun pipelineOptions(c: Context, engineReadsCyrillic: Boolean): PipelineOptions =
        PipelineOptions(
            bridge = if (choice(c, BRIDGE, "adapt") == "off") BridgeMode.OFF else BridgeMode.ADAPT,
            scriptMode = when (choice(c, SCRIPT, "auto")) {
                "latin" -> ScriptMode.LATIN
                "keep" -> ScriptMode.KEEP
                else -> ScriptMode.AUTO
            },
            newLatin = when (choice(c, NEW_LATIN, "auto")) {
                "uz" -> NewLatinMode.UZ
                "off" -> NewLatinMode.OFF
                else -> NewLatinMode.AUTO
            },
            engineReadsCyrillic = engineReadsCyrillic,
            apostrophe = if (choice(c, APOSTROPHE, "ascii") == "modifier") '\u02BB' else '\'',
        )

    // ------------------------------------------------------------------ eksport va import tekshiruvi

    /**
     * Eksport va importda bitta sozlamani tekshiradi. To'g'ri bo'lsa toza qiymatni (Int, Boolean yoki String),
     * noto'g'ri turdagi, chegaradan tashqaridagi yoki noma'lum kalit bo'lsa null qaytaradi.
     * Tekshirilmagan qiymat SharedPreferences'ga tushsa, keyingi o'qishda xizmat qulashi yoki nutq
     * butunlay jim bo'lib qolishi (masalan, tezlik 0) mumkin edi.
     *
     * @param gestureIds ma'lum ishora identifikatorlari (faqat shularga "g_" kaliti qabul qilinadi).
     */
    fun sanitize(key: String, value: Any?, gestureIds: Set<Int>): Any? = when {
        key == RATE -> (value as? Number)?.toInt()?.takeIf { it in RATE_RANGE }
        key == PITCH -> (value as? Number)?.toInt()?.takeIf { it in PITCH_RANGE }
        key in BOOLEANS -> value as? Boolean
        key == VOICE -> (value as? String)?.takeIf { it.length <= MAX_VOICE_LENGTH }
        key == AZURE_REGION -> (value as? String)?.takeIf { REGION_RE.matches(it) }
        key == AI_MODEL -> (value as? String)?.takeIf { MODEL_RE.matches(it) }
        key == GEMINI_MODEL -> (value as? String)?.takeIf { MODEL_RE.matches(it) }
        key == OPENAI_MODEL || key == CUSTOM_MODEL -> (value as? String)?.takeIf { CUSTOM_MODEL_RE.matches(it) }
        key == CUSTOM_URL -> (value as? String)?.takeIf { URL_RE.matches(it) }
        key in CHOICES -> (value as? String)?.takeIf { it in CHOICES.getValue(key) }
        key.startsWith(GESTURE_PREFIX) -> sanitizeGesture(key, value, gestureIds)
        else -> null
    }

    private fun sanitizeGesture(key: String, value: Any?, gestureIds: Set<Int>): String? {
        val id = key.removePrefix(GESTURE_PREFIX).toIntOrNull() ?: return null
        if (id !in gestureIds) return null
        val name = value as? String ?: return null
        return if (GestureAction.values().any { it.name == name }) name else null
    }
}
