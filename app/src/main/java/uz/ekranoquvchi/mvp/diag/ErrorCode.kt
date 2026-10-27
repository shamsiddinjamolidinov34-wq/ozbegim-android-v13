package uz.ekranoquvchi.mvp.diag

enum class Category { CRASH, SILENCE, LATENCY, FOCUS, TTS, AUDIO, GESTURE, INPUT, LANG, SCRIPT, PERM, UPDATE }

enum class Severity { P0, P1, P2, P3 }

/** Xato kodlari: har biri toifa va og'irlikka ega (monografiya 15.2). Ekran matni hech qachon yozilmaydi. */
enum class ErrorCode(val category: Category, val severity: Severity) {
    CRASH_UNCAUGHT(Category.CRASH, Severity.P0),
    SILENCE_NO_AUDIO(Category.SILENCE, Severity.P0),
    SILENCE_RECOVERED(Category.SILENCE, Severity.P2),
    TTS_INIT_FAILED(Category.TTS, Severity.P0),
    TTS_LANG_MISSING(Category.TTS, Severity.P1),
    TTS_ENGINE_SWITCHED(Category.TTS, Severity.P2),
    TTS_ENGINE_ERROR(Category.TTS, Severity.P1),
    TTS_FALLBACK_SIGNAL(Category.TTS, Severity.P1),
    LATENCY_BUDGET(Category.LATENCY, Severity.P2),
    SLOW_HANDLER(Category.LATENCY, Severity.P2),
    FOCUS_NODE_FAILED(Category.FOCUS, Severity.P1),
    LANG_BRIDGE_FAILED(Category.LANG, Severity.P2),
    AUDIO_FOCUS_LOST(Category.AUDIO, Severity.P3),
    USER_MARKED(Category.GESTURE, Severity.P1),
    SAFE_MODE_ENTERED(Category.CRASH, Severity.P1),
    ONLINE_FAILED(Category.TTS, Severity.P2),
    AI_FAILED(Category.LANG, Severity.P2),
    DISPLAY_INFO(Category.GESTURE, Severity.P3),
    NODE_UNLABELED(Category.FOCUS, Severity.P3),
    FOCUS_FALLBACK(Category.FOCUS, Severity.P3),
    GESTURE_PATH(Category.GESTURE, Severity.P3),
}
