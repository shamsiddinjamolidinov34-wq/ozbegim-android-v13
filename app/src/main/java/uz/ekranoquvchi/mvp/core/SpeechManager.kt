package uz.ekranoquvchi.mvp.core

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import uz.ekranoquvchi.mvp.R
import uz.ekranoquvchi.mvp.content.EngineProfiles
import uz.ekranoquvchi.mvp.content.SensitiveText
import uz.ekranoquvchi.mvp.content.TextChunker
import uz.ekranoquvchi.mvp.diag.Diag
import uz.ekranoquvchi.mvp.diag.ErrorCode
import uz.ekranoquvchi.mvp.diag.LatencyTracker
import uz.ekranoquvchi.mvp.net.AzureSpeaker
import uz.ekranoquvchi.mvp.net.Online
import uz.ekranoquvchi.mvp.util.Prefs
import java.util.Locale

/**
 * Nutq boshqaruvchisi (monografiya 9.1, 16.1): tizim TTS adapteri, navbat, to'xtatish (barge-in),
 * audio fokus, jimlik detektori va avtomatik tiklash:
 *   1-bosqich: dvigatelni qayta ishga tushirish; 2-bosqich: boshqa dvigatelga o'tish;
 *   3-bosqich: TTS'siz zaxira signal (ton va tebranish), chunki foydalanuvchi hech qachon jim qolmasligi kerak.
 * Barcha holat o'zgarishlari asosiy oqimda bajariladi.
 */
class SpeechManager(private val context: Context) {

    private data class Pending(val text: String, val interrupt: Boolean, val baseTime: Long, val local: Boolean = false)

    private val main = Handler(Looper.getMainLooper())
    private val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var tts: TextToSpeech? = null
    private var generation = 0
    private var ready = false
    private var engineArg: String? = null
    private var knownEngines: List<String> = emptyList()
    private var recoveryStage = 0
    private var triedRhvoice = false
    private var langWarned = false
    /** Mikrofon band (masalan, ovozli xabar yozilmoqda): avtomatik xabarlar o'qilmaydi (faqat "stop" siyosati). */
    private var micMuteAuto = false

    /** Mikrofon band: nutq past ovozda aytiladi ("stop" va "duck" siyosatlari). */
    private var micDuck = false

    /** Mikrofon band: boshqa ilovaning yozuviga xalaqit bermaslik uchun audio fokus so'ralmaydi. */
    private var micActive = false
    private var chunkSoft = 110
    private var chunkHard = 170
    private var counter = 0
    private var lastInitAt = 0L
    private var lastLatencyLog = 0L
    private var rearms = 0
    private var lastOutputErrorAt = 0L
    private var lastRequest: Pending? = null
    private var focusRequest: AudioFocusRequest? = null
    private var systemDefaultEngine: String? = null
    private var online: AzureSpeaker? = null
    private var azureKeyWarned = false

    /** shutdown() chaqirilgan: kechikib kelgan qaytarma chaqiruvlar TTS'ni qayta tiriltirmasligi uchun. */
    @Volatile
    private var closed = false
    private val waiting = ArrayDeque<Pending>()
    private val requested = HashMap<String, Long>()
    private val started = HashSet<String>()

    /** Hozir ishlatilayotgan dvigatel (paket nomi). Yozuv qoidalari shunga qarab tanlanadi. */
    var currentEngine: String? = null
        private set

    /** Matn tayyorlash uchun: onlayn Azure ovozi faol bo'lsa "azure", aks holda mahalliy dvigatel. */
    val effectiveEngine: String?
        get() = if (onlineActive()) EngineProfiles.AZURE else currentEngine

    private val abandonFocus = Runnable {
        focusRequest?.let {
            try {
                audio.abandonAudioFocusRequest(it)
            } catch (_: Throwable) {
            }
        }
    }

    private val progress = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            main.post { handleStart(utteranceId) }
        }

        override fun onDone(utteranceId: String?) {
            main.post { handleEnd(utteranceId) }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
            main.post { handleError(utteranceId, -1) }
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            main.post { handleError(utteranceId, errorCode) }
        }

        override fun onStop(utteranceId: String?, interrupted: Boolean) {
            main.post { handleEnd(utteranceId) }
        }
    }

    init {
        createTts(initialEngine())
    }

    // ------------------------------------------------------------------ ochiq metodlar

    /**
     * @param baseTime hodisa vaqti (uptimeMillis): kechikish shundan o'lchanadi; 0 bo'lsa hozirgi vaqt.
     * @param local true bo'lsa matn hech qachon tarmoqqa chiqmaydi (maxfiy ilova): faqat mahalliy dvigatel aytadi.
     */
    fun speak(text: String, interrupt: Boolean, baseTime: Long = 0L, local: Boolean = false, auto: Boolean = false) {
        if (text.isBlank()) return
        if (Looper.myLooper() == Looper.getMainLooper()) speakOnMain(text, interrupt, baseTime, local, auto)
        else main.post { speakOnMain(text, interrupt, baseTime, local, auto) }
    }

    /**
     * Mikrofon ochilganda (masalan, ovozli xabar yozilayotganda). Ilgari "stop" butun nutqni o'chirib qo'yardi:
     * foydalanuvchi yozishni to'xtatish va yuborish tugmalarini eshita olmasdi, ba'zi qurilmalarda esa mikrofon
     * holati yangilanmay qolib, ekran o'quvchi butunlay jim bo'lardi. Endi:
     *  - "stop": avtomatik xabarlar (auto=true) o'qilmaydi, foydalanuvchi bosib o'qiganlari past ovozda aytiladi;
     *  - "duck": hammasi past ovozda;
     *  - "continue": o'zgarishsiz.
     * Mikrofon band bo'lganda boshqa ilovaning yozuviga xalaqit bermaslik uchun audio fokus so'ralmaydi.
     */
    fun setMicActive(active: Boolean, policy: String) {
        micActive = active
        micMuteAuto = active && policy == "stop"
        micDuck = active && policy != "continue"
        if (micMuteAuto) stop()
    }

    /** Nutq hozir aytilmoqdami yoki navbatdami. */
    fun isBusy(): Boolean {
        if (started.isNotEmpty() || requested.isNotEmpty()) return true
        return try {
            tts?.isSpeaking == true
        } catch (_: Throwable) {
            false
        }
    }

    /** Davriy tekshiruv (barqarorlik): dvigatel uzoq vaqt tayyor bo'lmasa, uni qayta ishga tushiradi. */
    fun ensureAlive() {
        if (closed || ready) return
        if (SystemClock.uptimeMillis() - lastInitAt > ALIVE_RETRY_MS) createTts(engineArg)
    }

    fun stop() {
        requested.clear()
        started.clear()
        try {
            tts?.stop()
        } catch (_: Throwable) {
        }
        online?.stop()
        scheduleAbandon()
    }

    /** Audio fokusni qo'yib yuborishni rejalashtiradi; takror chaqiruvlar navbatda to'planib qolmaydi. */
    private fun scheduleAbandon() {
        main.removeCallbacks(abandonFocus)
        main.postDelayed(abandonFocus, 1000L)
    }

    fun applyPrefs() {
        val sp = SpeedProfile.current(context)
        chunkSoft = sp.chunkSoft
        chunkHard = sp.chunkHard
        val t = tts ?: return
        try {
            t.setSpeechRate(Prefs.rate(context) / 100f)
            t.setPitch(Prefs.pitch(context) / 100f)
        } catch (_: Throwable) {
        }
    }

    fun shutdown() {
        closed = true
        generation++
        main.removeCallbacksAndMessages(null)
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Throwable) {
        }
        tts = null
        ready = false
        try {
            online?.shutdown()
        } catch (_: Throwable) {
        }
        online = null
        abandonFocus.run()
    }

    // ------------------------------------------------------------------ ishga tushirish

    private fun targetLocale(): Locale =
        if (Locale.getDefault().language == "uz") Locale.forLanguageTag("uz-UZ") else Locale.getDefault()

    private fun createTts(engine: String?) {
        if (closed) return
        val gen = ++generation
        ready = false
        engineArg = engine
        lastInitAt = SystemClock.uptimeMillis()
        val old = tts
        tts = null
        try {
            old?.stop()
            old?.shutdown()
        } catch (_: Throwable) {
        }
        val listener = TextToSpeech.OnInitListener { status ->
            main.post { if (gen == generation) onInit(status) }
        }
        try {
            tts = if (engine == null) TextToSpeech(context, listener) else TextToSpeech(context, listener, engine)
        } catch (t: Throwable) {
            Diag.log.log(ErrorCode.TTS_INIT_FAILED, mapOf("exc" to t.javaClass.simpleName))
            // Konstruktor xato bersa, hech qanday qaytarma chaqiruv kelmaydi: tiklash zanjirini o'zimiz boshlaymiz
            // (oxirgi bosqichda zaxira signal beriladi), aks holda foydalanuvchi sababsiz jim qolardi.
            main.post { if (!closed && gen == generation) escalate() }
        }
    }

    private fun onInit(status: Int) {
        val t = tts ?: return
        if (status != TextToSpeech.SUCCESS) {
            Diag.log.log(ErrorCode.TTS_INIT_FAILED, mapOf("status" to status, "engine" to (engineArg ?: "default")))
            escalate()
            return
        }
        systemDefaultEngine = try {
            t.defaultEngine
        } catch (_: Throwable) {
            null
        }
        currentEngine = engineArg ?: systemDefaultEngine
        knownEngines = try {
            t.engines.map { it.name }
        } catch (_: Throwable) {
            emptyList()
        }
        try {
            t.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
        } catch (_: Throwable) {
        }
        val locale = targetLocale()
        val r = t.setLanguage(locale)
        if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) {
            // Tanlangan dvigatelda o'zbek tili yo'q: RHVoice o'rnatilgan bo'lsa, unga o'tamiz (bir marta).
            if (locale.language == "uz" && !triedRhvoice && engineArg != EngineProfiles.RHVOICE &&
                Prefs.engine(context) != "system" && knownEngines.contains(EngineProfiles.RHVOICE)
            ) {
                triedRhvoice = true
                Prefs.setAutoEngine(context, EngineProfiles.RHVOICE)
                Diag.log.log(ErrorCode.TTS_ENGINE_SWITCHED, mapOf("why" to "uz_missing", "to" to "rhvoice"))
                createTts(EngineProfiles.RHVOICE)
                return
            }
            if (!langWarned) {
                langWarned = true
                Diag.log.log(ErrorCode.TTS_LANG_MISSING, mapOf("lang" to locale.toLanguageTag(), "code" to r))
            }
        }
        applyVoice(t)
        applyPrefs()
        t.setOnUtteranceProgressListener(progress)
        ready = true
        Log.i(TAG, "ready engine=$currentEngine lang=$r")
        while (waiting.isNotEmpty()) {
            val p = waiting.removeFirst()
            speakNow(p.text, p.interrupt, p.baseTime)
        }
    }

    private fun isInstalled(pkg: String): Boolean = try {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(pkg, 0)
        true
    } catch (_: Throwable) {
        false
    }

    /** Sozlamadagi dvigatel: "rhvoice" tanlangan va o'rnatilgan bo'lsa RHVoice, aks holda tizim standarti. */
    private fun initialEngine(): String? {
        val pref = Prefs.engine(context)
        if (pref == "rhvoice" && isInstalled(EngineProfiles.RHVOICE)) return EngineProfiles.RHVOICE
        // Oldingi safar tizim dvigatelida o'zbek tili yo'qligi aniqlangan bo'lsa, darhol RHVoice bilan boshlaymiz
        // (ilgari har safar avval tizim dvigateli ishga tushirilib, keyin RHVoice'ga o'tilardi: bu bir necha soniya olardi)
        if (pref == "auto" && Prefs.autoEngine(context) == EngineProfiles.RHVOICE && isInstalled(EngineProfiles.RHVOICE)) {
            return EngineProfiles.RHVOICE
        }
        return null
    }

    /** Tanlangan ovoz (masalan, Islom, Dilnavoz yoki Sevinch) mavjud bo'lsa, shuni qo'yadi. */
    private fun applyVoice(t: TextToSpeech) {
        val saved = Prefs.voice(context)
        if (saved.isEmpty()) return
        try {
            val v = t.voices?.firstOrNull { it.name == saved }
            if (v != null) t.setVoice(v)
        } catch (_: Throwable) {
        }
    }

    /** Dvigatel yoki ovoz tanlovi o'zgarganda chaqiriladi. */
    fun reconfigure() {
        triedRhvoice = false
        azureKeyWarned = false
        online?.stop()
        createTts(initialEngine())
    }

    // ------------------------------------------------------------------ gapirish

    private fun speakOnMain(text: String, interrupt: Boolean, baseTime: Long, local: Boolean, auto: Boolean) {
        if (closed || (micMuteAuto && auto)) return
        val base = if (baseTime > 0) baseTime else SystemClock.uptimeMillis()
        val p = Pending(text.take(MAX_TEXT), interrupt, base, local)
        lastRequest = p
        // Onlayn ovoz (Microsoft Azure): faqat foydalanuvchi yoqgan bo'lsa; maxfiy matn hech qachon tarmoqqa chiqmaydi
        if (!local && onlineActive() && !SensitiveText.looksSensitive(p.text)) {
            speakOnline(p)
            return
        }
        if (!ready) {
            if (interrupt) waiting.clear()
            waiting.addLast(p)
            while (waiting.size > 6) waiting.removeFirst()
            // dvigatel ishga tushmagan bo'lsa, vaqti-vaqti bilan qayta urinib ko'ramiz
            if (SystemClock.uptimeMillis() - lastInitAt > RETRY_INIT_MS) createTts(engineArg)
            return
        }
        speakNow(p.text, p.interrupt, p.baseTime)
    }

    /** Uzun matn gap bo'laklariga ajratiladi: birinchi bo'lak tez eshitiladi (9.2). */
    private fun speakNow(text: String, interrupt: Boolean, baseTime: Long) {
        val chunks = TextChunker.split(text, chunkSoft, chunkHard)
        for ((i, c) in chunks.withIndex()) speakChunk(c, if (i == 0) interrupt else false, baseTime, i == 0)
    }

    private fun speakChunk(text: String, interrupt: Boolean, baseTime: Long, first: Boolean) {
        val t = tts ?: return
        val id = "u${++counter}"
        if (interrupt) {
            requested.clear()
            started.clear()
            online?.stop()
        }
        // Kechikish faqat darhol boshlanishi kerak bo'lgan (to'xtatib gapiriladigan) birinchi bo'lak uchun o'lchanadi
        requested[id] = if (first && interrupt) baseTime else -1L
        requestFocus()
        val result = try {
            val params = if (micDuck) Bundle().apply { putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 0.3f) } else null
            t.speak(text, if (interrupt) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD, params, id)
        } catch (_: Throwable) {
            TextToSpeech.ERROR
        }
        if (result != TextToSpeech.SUCCESS) {
            requested.remove(id)
            Diag.log.log(ErrorCode.TTS_ENGINE_ERROR, mapOf("speak" to result))
            escalate()
            return
        }
        Diag.blackBox.add("speak", counter, if (interrupt) 1 else 0)
        // Jimlik detektori: so'rov bor, lekin ovoz boshlanmasa (navbatda uzoq gap bo'lmasa) belgilaymiz
        // Jimlik detektori faqat birinchi bo'lak uchun: keyingi bo'laklar navbatda kutadi, bu jimlik emas
        if (first && (interrupt || started.isEmpty())) main.postDelayed({ checkStarted(id) }, WATCHDOG_MS)
    }

    private fun handleStart(id: String?) {
        if (id == null) return
        started.add(id)
        rearms = 0
        val base = requested.remove(id)
        if (base != null && base > 0L) {
            val now = SystemClock.uptimeMillis()
            val ms = now - base
            LatencyTracker.add(ms)
            if (ms > LATENCY_LOG_MS && now - lastLatencyLog > 10_000L) {
                lastLatencyLog = now
                Diag.log.log(ErrorCode.LATENCY_BUDGET, mapOf("ms" to ms))
            }
        }
        if (recoveryStage > 0) {
            Diag.log.log(ErrorCode.SILENCE_RECOVERED, mapOf("stage" to recoveryStage))
            recoveryStage = 0
        }
    }

    private fun handleEnd(id: String?) {
        if (id == null) return
        started.remove(id)
        requested.remove(id)
        if (started.isEmpty() && requested.isEmpty()) scheduleAbandon()
    }

    private fun handleError(id: String?, code: Int) {
        Diag.log.log(ErrorCode.TTS_ENGINE_ERROR, mapOf("code" to code))
        if (id != null) {
            requested.remove(id)
            started.remove(id)
        }
        // Chiqish xatosi (-5) odatda vaqtinchalik (masalan, juda tez-tez to'xtatib yangidan boshlash): dvigatelni
        // qayta yaratish soniyalab jimlikka olib keladi, shuning uchun avval oxirgi gapni bir marta qayta aytamiz.
        val now = SystemClock.uptimeMillis()
        if (code == TextToSpeech.ERROR_OUTPUT && now - lastOutputErrorAt > 3000L) {
            lastOutputErrorAt = now
            val last = lastRequest
            if (last != null) {
                main.postDelayed({
                    if (!closed && ready) speakNow(last.text, last.interrupt, last.baseTime)
                }, 150L)
            }
            return
        }
        escalate()
    }

    private fun checkStarted(id: String) {
        if (closed || !requested.containsKey(id)) return // boshlangan yoki to'xtatilgan
        // Dvigatel hozir gapiryapti (oldingi gap hali tugamagan) bo'lsa, bu jimlik emas: yana kutamiz. Ilgari bunday
        // holatda dvigatel keraksiz qayta ishga tushirilib, nutq yanada sekinlashardi.
        val speaking = try {
            tts?.isSpeaking == true
        } catch (_: Throwable) {
            false
        }
        if (speaking && rearms < MAX_REARMS) {
            rearms++
            main.postDelayed({ checkStarted(id) }, WATCHDOG_MS)
            return
        }
        requested.remove(id)
        Diag.log.log(ErrorCode.SILENCE_NO_AUDIO, mapOf("stage" to recoveryStage, "waitMs" to WATCHDOG_MS))
        escalate()
    }

    // ------------------------------------------------------------------ onlayn ovoz (Microsoft Azure)

    /** Tanlangan dvigatel "azure", onlayn yoqilgan, kalit bor va yaqinda xato bo'lmagan. */
    fun onlineActive(): Boolean =
        Prefs.engine(context) == EngineProfiles.AZURE && Online.speechReady(context) && !Online.coolingDown()

    private val onlineListener = object : AzureSpeaker.Listener {
        override fun onStart(id: String) {
            Diag.blackBox.add("online", 1)
        }

        override fun onEnd(id: String) {}

        override fun onIdle() {
            if (started.isEmpty() && requested.isEmpty()) scheduleAbandon()
        }

        override fun onFailed(id: String, text: String, reason: String) {
            if (closed) return
            Diag.log.log(ErrorCode.ONLINE_FAILED, mapOf("why" to reason))
            if (reason == "auth" && !azureKeyWarned) {
                azureKeyWarned = true
                fallbackLocal(context.getString(R.string.msg_azure_key_bad), false)
            }
            fallbackLocal(text, false)
        }
    }

    private fun onlineSpeaker(): AzureSpeaker {
        var o = online
        if (o == null) {
            o = AzureSpeaker(context, onlineListener)
            online = o
        }
        return o
    }

    private fun speakOnline(p: Pending) {
        if (p.interrupt) {
            requested.clear()
            started.clear()
            try {
                tts?.stop()
            } catch (_: Throwable) {
            }
        }
        requestFocus()
        val sp = onlineSpeaker()
        sp.volume = if (micDuck) 0.3f else 1f
        val chunks = TextChunker.split(p.text, chunkSoft, chunkHard)
        for ((i, c) in chunks.withIndex()) {
            sp.speak("o${++counter}", c, if (i == 0) p.interrupt else false)
        }
        Diag.blackBox.add("speak", counter, if (p.interrupt) 3 else 2)
    }

    /** Onlayn bo'lak aytilmasa (tarmoq yo'q, kalit xato), shu matn mahalliy dvigatelda aytiladi: foydalanuvchi jim qolmaydi. */
    private fun fallbackLocal(text: String, interrupt: Boolean) {
        val p = Pending(text, interrupt, SystemClock.uptimeMillis(), true)
        if (!ready) {
            waiting.addLast(p)
            while (waiting.size > 6) waiting.removeFirst()
            return
        }
        speakNow(p.text, p.interrupt, p.baseTime)
    }

    // ------------------------------------------------------------------ tiklash

    private fun escalate() {
        if (closed) return
        recoveryStage++
        val last = lastRequest
        if (last != null && waiting.none { it === last }) waiting.addFirst(last)
        when (recoveryStage) {
            1 -> {
                Diag.log.log(ErrorCode.TTS_ENGINE_SWITCHED, mapOf("step" to "restart"))
                createTts(engineArg)
            }
            2 -> {
                val other = fallbackEngine()
                Diag.log.log(ErrorCode.TTS_ENGINE_SWITCHED, mapOf("step" to "switch", "to" to (other ?: "default")))
                createTts(other)
            }
            else -> {
                recoveryStage = 0
                waiting.clear()
                fallbackSignal()
            }
        }
    }

    /**
     * 2-bosqich uchun zaxira dvigatel: faqat ishonchli ro'yxatdagi (RHVoice, Google, Samsung) yoki tizimning
     * standart dvigateli. Ekran matni foydalanuvchi bilmagan, noma'lum (ehtimol bulutli) dvigatelga yuborilmaydi.
     * Foydalanuvchi "faqat tizim dvigateli"ni tanlagan bo'lsa, boshqasiga o'tilmaydi.
     */
    private fun fallbackEngine(): String? {
        if (Prefs.engine(context) == "system") return null
        val candidates = listOfNotNull(systemDefaultEngine) + EngineProfiles.TRUSTED
        return candidates.firstOrNull { it != currentEngine && knownEngines.contains(it) }
    }

    /** TTS butunlay ishlamasa ham eshitiladigan signal: "ekran o'quvchi tirik, lekin ovoz yo'q". */
    private fun fallbackSignal() {
        Diag.log.log(ErrorCode.TTS_FALLBACK_SIGNAL)
        try {
            val tone = ToneGenerator(AudioManager.STREAM_ACCESSIBILITY, 80)
            tone.startTone(ToneGenerator.TONE_PROP_NACK, 300)
            main.postDelayed({ tone.release() }, 700L)
        } catch (_: Throwable) {
        }
        try {
            @Suppress("DEPRECATION")
            val v = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 200, 120, 200, 120, 400), -1))
        } catch (_: Throwable) {
        }
    }

    private fun requestFocus() {
        // Boshqa ilova mikrofonda yozayotgan bo'lsa (ovozli xabar), audio fokus so'rab uning yozuviga xalaqit bermaymiz
        if (micActive) return
        if (focusRequest == null) {
            focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setWillPauseWhenDucked(false)
                .setOnAudioFocusChangeListener { change ->
                    Diag.blackBox.add("audiofocus", change)
                    if (change == AudioManager.AUDIOFOCUS_LOSS) Diag.log.log(ErrorCode.AUDIO_FOCUS_LOST)
                }
                .build()
        }
        main.removeCallbacks(abandonFocus)
        try {
            audio.requestAudioFocus(focusRequest!!)
        } catch (_: Throwable) {
        }
    }

    companion object {
        private const val TAG = "SpeechManager"
        // Ilgari 2 soniya edi: sekin dvigatel (masalan, RHVoice birinchi gapda) keraksiz qayta ishga tushirilardi
        private const val WATCHDOG_MS = 4000L
        private const val MAX_REARMS = 2
        private const val LATENCY_LOG_MS = 400L
        private const val RETRY_INIT_MS = 5000L
        private const val ALIVE_RETRY_MS = 15000L
        private const val MAX_TEXT = 3900
    }
}
