package uz.ekranoquvchi.mvp.core

/** O'quv mashqlari (FR-18): xizmat ishorani shu yerga xabar qiladi, mashq ekrani esa unga javob beradi. */
object Tutorial {
    @Volatile
    var listener: ((Int) -> Unit)? = null
}
