package uz.ekranoquvchi.mvp.content

/** Tasdiqlash kodi (OTP) yoki parolga o'xshash xabarni aniqlaydi: karnayda o'qilmasligi uchun. */
object SensitiveText {
    private val CODE = Regex("(?<!\\d)\\d{4,8}(?!\\d)")

    // Lotin, o'zbek va rus kirilli: "парол" (o'zbekcha ham, ruscha "пароль" ham), "тасдиқ/тасдик", "подтвержд".
    // Ilgari faqat ruscha "пароль" (yumshatish belgisi bilan) bor edi: o'zbekcha "парол" aniqlanmasdi.
    private val WORDS = Regex(
        "(?iu)(kod|code|\u043A\u043E\u0434|parol|\u043F\u0430\u0440\u043E\u043B|otp|tasdiq|" +
            "\u0442\u0430\u0441\u0434\u0438[\u049B\u043A]|\u043F\u043E\u0434\u0442\u0432\u0435\u0440\u0436\u0434|" +
            "pass(?:word|code)|verif|cvv|\\bpin\\b)"
    )

    fun looksSensitive(text: String): Boolean = CODE.containsMatchIn(text) && WORDS.containsMatchIn(text)
}
