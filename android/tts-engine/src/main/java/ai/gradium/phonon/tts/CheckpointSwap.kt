package ai.gradium.phonon.tts

import java.io.File

/** Called only under the model installation/load lock. Backup is the crash recovery marker. */
object CheckpointSwap {
    /** Close the candidate before touching its files; restore only after successful recovery. */
    fun restoreActivation(error: Throwable, close: () -> Unit, recover: () -> Unit, restore: () -> Unit): Nothing {
        try {
            close()
            recover()
            restore()
        } catch (recoveryError: Throwable) {
            if (recoveryError !== error) error.addSuppressed(recoveryError)
        }
        throw error
    }

    fun recover(target: File) {
        val backup = File(target.parentFile, target.name + ".backup")
        if (backup.exists()) {
            check(!target.exists() || target.deleteRecursively()) { "Cannot clear interrupted install" }
            check(backup.renameTo(target)) { "Restore failed; previous checkpoint remains in $backup" }
        }
    }

    fun <T> replace(staging: File, target: File, activate: () -> T): T {
        val backup = File(target.parentFile, target.name + ".backup")
        check(!backup.exists()) { "Recover previous install before replacing" }
        val hadPrevious = target.exists()
        if (hadPrevious) check(target.renameTo(backup)) { "Cannot back up installed model" }
        val result = try {
            check(staging.renameTo(target)) { "Cannot activate checkpoint" }
            activate()
        } catch (t: Throwable) {
            check(!target.exists() || target.deleteRecursively()) { "Cannot clear failed install; backup retained" }
            if (hadPrevious) check(backup.renameTo(target)) { "Restore failed; previous checkpoint retained in $backup" }
            throw t
        }
        // Failure to remove the marker must not report success: a restart would restore it.
        check(!backup.exists() || backup.deleteRecursively()) { "Loaded model, but backup cleanup failed; restart will restore previous model" }
        return result
    }
}
