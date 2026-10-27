package uz.ekranoquvchi.mvp.core

/**
 * O'zbegim menyusi (TalkBack'dagi "TalkBack menyusi" o'rnida): hamma amal bir joyda, ishorani yodlash shart emas.
 * Tez-tez kerak bo'ladiganlar tepada. Ishoralarga biriktirib bo'ladigan har qanday amalni shu yerdan ham bajarish mumkin.
 * Nutq tezligi bu yerda yo'q: u o'qish boshqaruvlarida (yuqoriga/pastga surish) va sozlamalarda o'zgaradi.
 * Tizim amallari (orqaga, bosh ekran, so'nggi ilovalar va h.k.) alohida "Tizim amallari" ichki menyusida.
 */
object GlobalMenu {
    val actions: List<GestureAction> = listOf(
        GestureAction.ACTIONS_MENU,
        GestureAction.READING_MENU,
        GestureAction.SPELL_FOCUSED,
        GestureAction.SPELL_LAST,
        GestureAction.AUTO_TRANSLATE,
        GestureAction.READ_FROM_TOP,
        GestureAction.READ_FROM_NEXT,
        GestureAction.SCREEN_SEARCH,
        GestureAction.LINKS_MENU,
        GestureAction.DESCRIBE_IMAGE,
        GestureAction.DESCRIBE_SCREEN,
        GestureAction.TRANSLATE,
        GestureAction.DETAILS_MENU,
        GestureAction.OPEN_SETTINGS,
        GestureAction.TTS_SETTINGS,
        GestureAction.SYSTEM_ACTIONS,
        GestureAction.STATUS,
        GestureAction.REPEAT_LAST,
        GestureAction.COPY_LAST,
        GestureAction.COPY_TEXT,
        GestureAction.SELECT_ALL,
        GestureAction.CUT_TEXT,
        GestureAction.PASTE_TEXT,
        GestureAction.CURTAIN,
        GestureAction.MEDIA_PLAY_PAUSE,
        GestureAction.CALL_ANSWER,
        GestureAction.CALL_END,
        GestureAction.CALL_SPEAKER,
        GestureAction.READ_PRIVATE,
        GestureAction.EXPLAIN_BRIDGE,
        GestureAction.ADD_WORD,
        GestureAction.ADD_LABEL,
        GestureAction.MARK_PROBLEM,
        GestureAction.FIRST_ITEM,
        GestureAction.JUMP_LAST,
        GestureAction.NEXT_WINDOW,
        GestureAction.PREV_WINDOW,
        GestureAction.PRACTICE,
        GestureAction.SUSPEND,
    )

    /** "Tizim amallari" ichki menyusi: ilgari bosh menyuda alohida turgan tizim amallari. */
    val systemActions: List<GestureAction> = listOf(
        GestureAction.BACK,
        GestureAction.HOME,
        GestureAction.RECENTS,
        GestureAction.OPEN_APPS,
        GestureAction.NOTIFICATIONS,
        GestureAction.QUICK_SETTINGS,
        GestureAction.LOCK_SCREEN,
        GestureAction.SCREENSHOT,
        GestureAction.POWER_DIALOG,
        GestureAction.SPLIT_SCREEN,
    )
}
