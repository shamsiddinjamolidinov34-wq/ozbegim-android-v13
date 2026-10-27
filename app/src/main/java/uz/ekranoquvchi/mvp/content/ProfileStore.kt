package uz.ekranoquvchi.mvp.content

data class AppProfile(
    val verbosity: Verbosity? = null,
    val echo: Boolean? = null,
    val bridge: BridgeMode? = null,
    val notify: Boolean? = null,
    val chat: Boolean? = null,
    val sensitive: Boolean? = null,
)

/**
 * Ilova bo'yicha profillar (FR-12, FR-17). Matn fayli, har qatorda:
 * paket=verbosity:short;echo:off;bridge:off;notify:off;chat:on;sensitive:on
 * sensitive:on - maxfiy ilova (bank, to'lov): qora quti yozmaydi, suhbat rejimi o'chadi (11.2).
 */
object ProfileStore {
    @Volatile
    private var map: Map<String, AppProfile> = emptyMap()

    fun load(text: String) {
        map = parse(text)
    }

    fun get(pkg: String?): AppProfile? = if (pkg == null) null else map[pkg]

    fun parse(text: String): Map<String, AppProfile> {
        val out = HashMap<String, AppProfile>()
        for (line in text.lineSequence()) {
            val l = line.trim()
            if (l.isEmpty() || l.startsWith("#")) continue
            val i = l.indexOf('=')
            if (i <= 0) continue
            val pkg = l.substring(0, i).trim()
            var p = AppProfile()
            for (part in l.substring(i + 1).split(';')) {
                val kv = part.split(':', limit = 2)
                if (kv.size != 2) continue
                val k = kv[0].trim().lowercase()
                val v = kv[1].trim().lowercase()
                p = when (k) {
                    "verbosity" -> p.copy(
                        verbosity = when (v) {
                            "short" -> Verbosity.SHORT
                            "normal" -> Verbosity.NORMAL
                            "full" -> Verbosity.FULL
                            else -> null
                        }
                    )
                    "echo" -> p.copy(echo = onOff(v))
                    "bridge" -> p.copy(
                        bridge = when (v) {
                            "off" -> BridgeMode.OFF
                            "on", "adapt" -> BridgeMode.ADAPT
                            else -> null
                        }
                    )
                    "notify" -> p.copy(notify = onOff(v))
                    "chat" -> p.copy(chat = onOff(v))
                    "sensitive" -> p.copy(sensitive = onOff(v))
                    else -> p
                }
            }
            out[pkg] = p
        }
        return out
    }

    private fun onOff(v: String): Boolean? = when (v) {
        "on", "true", "1" -> true
        "off", "false", "0" -> false
        else -> null
    }
}
