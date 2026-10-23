package ai.gradium.phonon.tts

/** One explicit user action, consumed before invoking work. Cancellation never touches storage. */
internal class ReplacementGate {
    private var action: (() -> Unit)? = null
    val pending get() = action != null
    fun request(installed: Boolean, busy: Boolean, work: () -> Unit) {
        if (busy || pending) return
        if (installed) action = work else work()
    }
    fun cancel() { action = null }
    fun accept(busy: Boolean) {
        val work = action
        action = null
        if (!busy) work?.invoke()
    }
}
